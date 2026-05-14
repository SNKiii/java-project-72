import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.App;
import hexlet.code.util.NamedRoutes;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {

    private Javalin app;
    private HikariDataSource dataSource;
    private MockWebServer mockWebServer;
    private String mockServerUrl;

    @BeforeEach
    void beforeEach() throws IOException, SQLException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        mockServerUrl = mockWebServer.url("/").toString();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;");
        dataSource = new HikariDataSource(config);
        hexlet.code.repository.BaseRepository.dataSource = dataSource;

        System.setProperty("TEST_DATABASE_URL", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;");

        createTables();

        app = App.getApp();
    }

    @AfterEach
    void afterEach() throws IOException {
        System.clearProperty("TEST_DATABASE_URL");
        if (app != null) {
            app.stop();
        }
        if (dataSource != null) {
            dataSource.close();
        }
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    private void createTables() throws SQLException {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS url_checks");
            stmt.execute("DROP TABLE IF EXISTS urls");

            stmt.execute("""
                CREATE TABLE urls (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL UNIQUE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

            stmt.execute("""
                CREATE TABLE url_checks (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    url_id BIGINT NOT NULL,
                    status_code INT,
                    title VARCHAR(255),
                    h1 VARCHAR(255),
                    description TEXT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        }
    }

    @Test
    void testCreateUrlSuccess() {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=https://example.com")) {
                assertThat(response.code()).isEqualTo(200);
            }

            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT COUNT(*) FROM urls WHERE name = 'https://example.com'")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(1);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testCreateUrlDuplicate() {
        JavalinTest.test(app, (server, client) -> {
            client.post(NamedRoutes.urlsPath(), "url=https://duplicate.com");

            try (var response = client.post(NamedRoutes.urlsPath(), "url=https://duplicate.com")) {
                assertThat(response.code()).isEqualTo(200);
            }

            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT COUNT(*) FROM urls WHERE name = 'https://duplicate.com'")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(1);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testCreateUrlInvalid() {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=invalid")) {
                assertThat(response.code()).isEqualTo(422);
                assertThat(response.body().string()).contains("Некорректный URL");
            }
        });
    }

    @Test
    void testGetUrlById() {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=https://test.com")) {
                assertThat(response.code()).isEqualTo(200);
            }

            Long urlId = null;
            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT id FROM urls WHERE name = 'https://test.com'")) {
                if (rs.next()) {
                    urlId = rs.getLong("id");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            assertThat(urlId).isNotNull();

            try (var response = client.get(NamedRoutes.urlPath(urlId.toString()))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string()).contains("https://test.com");
            }
        });
    }

    @Test
    void testGetUrlNotFound() {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.get(NamedRoutes.urlPath("99999"))) {
                assertThat(response.code()).isEqualTo(404);
            }
        });
    }

    @Test
    void testCheckUrlSuccess() {
        String mockHtml = """
            <html>
                <head><title>Test Title</title></head>
                <body>
                    <h1>Test H1</h1>
                    <meta name="description" content="Test Description">
                </body>
            </html>
            """;
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(mockHtml));

        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=" + mockServerUrl)) {
                assertThat(response.code()).isEqualTo(200);
            }

            Long urlId = null;
            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT id FROM urls")) {
                if (rs.next()) {
                    urlId = rs.getLong("id");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            assertThat(urlId).isNotNull();

            try (var response = client.post(NamedRoutes.urlChecksPath(urlId.toString()), "")) {
                assertThat(response.code()).isEqualTo(200);
            }

            try (var conn = dataSource.getConnection();
                 var stmt = conn.prepareStatement(
                         "SELECT status_code, title, h1, description FROM url_checks WHERE url_id = ?")) {
                stmt.setLong(1, urlId);
                var rs = stmt.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt("status_code")).isEqualTo(200);
                assertThat(rs.getString("title")).isEqualTo("Test Title");
                assertThat(rs.getString("h1")).isEqualTo("Test H1");
                assertThat(rs.getString("description")).isEqualTo("Test Description");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testCheckUrlTruncatesLongText() {
        String longText = "a".repeat(300);
        String truncatedText = "a".repeat(200) + "...";

        String mockHtml = """
            <html>
                <head><title>%s</title></head>
                <body>
                    <h1>%s</h1>
                    <meta name="description" content="%s">
                </body>
            </html>
            """.formatted(longText, longText, longText);
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(mockHtml));

        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=" + mockServerUrl)) {
                assertThat(response.code()).isEqualTo(200);
            }

            Long urlId = null;
            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT id FROM urls")) {
                if (rs.next()) {
                    urlId = rs.getLong("id");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            assertThat(urlId).isNotNull();

            try (var response = client.post(NamedRoutes.urlChecksPath(urlId.toString()), "")) {
                assertThat(response.code()).isEqualTo(200);
            }

            try (var conn = dataSource.getConnection();
                 var stmt = conn.prepareStatement(
                         "SELECT title, h1, description FROM url_checks WHERE url_id = ?")) {
                stmt.setLong(1, urlId);
                var rs = stmt.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("title")).isEqualTo(truncatedText);
                assertThat(rs.getString("h1")).isEqualTo(truncatedText);
                assertThat(rs.getString("description")).isEqualTo(truncatedText);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testCheckUrlClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=" + mockServerUrl)) {
                assertThat(response.code()).isEqualTo(200);
            }

            Long urlId = null;
            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT id FROM urls")) {
                if (rs.next()) {
                    urlId = rs.getLong("id");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            assertThat(urlId).isNotNull();

            try (var response = client.post(NamedRoutes.urlChecksPath(urlId.toString()), "")) {
                assertThat(response.code()).isEqualTo(200);
            }

            try (var conn = dataSource.getConnection();
                 var stmt = conn.prepareStatement("SELECT COUNT(*) FROM url_checks WHERE url_id = ?")) {
                stmt.setLong(1, urlId);
                var rs = stmt.executeQuery();
                rs.next();
                assertThat(rs.getInt(1)).isZero();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testUrlPageShowsDataTestAttributes() {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=https://example.com")) {
                assertThat(response.code()).isEqualTo(200);
            }

            Long urlId = null;
            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT id FROM urls WHERE name = 'https://example.com'")) {
                if (rs.next()) {
                    urlId = rs.getLong("id");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            assertThat(urlId).isNotNull();

            try (var response = client.get(NamedRoutes.urlPath(urlId.toString()))) {
                String body = response.body().string();
                assertThat(response.code()).isEqualTo(200);
                assertThat(body).contains("data-test=\"url\"");
                assertThat(body).contains("method=\"post\"");
                assertThat(body).contains("action=\"/urls/" + urlId + "/checks\"");
                assertThat(body).contains("data-test=\"checks\"");
            }
        });
    }
    @Test
    void testCheckUrlWithEmptyHtml() {
        String mockHtml = """
            <html>
                <head></head>
                <body></body>
            </html>
            """;
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(mockHtml));

        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=" + mockServerUrl)) {
                assertThat(response.code()).isEqualTo(200);
            }

            Long urlId = null;
            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT id FROM urls")) {
                if (rs.next()) {
                    urlId = rs.getLong("id");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            assertThat(urlId).isNotNull();

            try (var response = client.post(NamedRoutes.urlChecksPath(urlId.toString()), "")) {
                assertThat(response.code()).isEqualTo(200);
            }

            try (var conn = dataSource.getConnection();
                 var stmt = conn.prepareStatement(
                         "SELECT title, h1, description FROM url_checks WHERE url_id = ?")) {
                stmt.setLong(1, urlId);
                var rs = stmt.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("title")).isEmpty();
                assertThat(rs.getString("h1")).isNull();
                assertThat(rs.getString("description")).isNull();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testCreateUrlWithPort() {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=http://localhost:8080")) {
                assertThat(response.code()).isEqualTo(200);
            }

            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT name FROM urls WHERE name = 'http://localhost:8080'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("http://localhost:8080");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testCreateUrlWithHttps() {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=https://secure.com")) {
                assertThat(response.code()).isEqualTo(200);
            }

            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT name FROM urls WHERE name = 'https://secure.com'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("https://secure.com");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
