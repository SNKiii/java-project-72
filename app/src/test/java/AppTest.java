import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
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
import java.net.URI;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {

    private Javalin app;
    private HikariDataSource dataSource;
    private MockWebServer mockWebServer;
    private String mockServerUrl;
    private String normalizedMockServerUrl;

    @BeforeEach
    void beforeEach() throws IOException, SQLException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String rawUrl = mockWebServer.url("/").toString();
        URI uri = URI.create(rawUrl);
        mockServerUrl = rawUrl;
        normalizedMockServerUrl = String.format("%s://%s%s",
                uri.getScheme(),
                uri.getHost(),
                uri.getPort() == -1 ? "" : ":" + uri.getPort()
        );

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
    void testCreateUrlSuccess() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=https://example.com")) {
                assertThat(response.code()).isEqualTo(200);
            }

            var maybeUrl = UrlRepository.findByName("https://example.com");
            assertThat(maybeUrl).isPresent();
        });
    }

    @Test
    void testCreateUrlDuplicate() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            client.post(NamedRoutes.urlsPath(), "url=https://duplicate.com");

            try (var response = client.post(NamedRoutes.urlsPath(), "url=https://duplicate.com")) {
                assertThat(response.code()).isEqualTo(200);
            }

            var maybeUrl = UrlRepository.findByName("https://duplicate.com");
            assertThat(maybeUrl).isPresent();
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
    void testGetUrlById() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            client.post(NamedRoutes.urlsPath(), "url=https://test.com");

            var maybeUrl = UrlRepository.findByName("https://test.com");
            assertThat(maybeUrl).isPresent();

            long id = maybeUrl.get().getId();
            try (var response = client.get(NamedRoutes.urlPath(String.valueOf(id)))) {
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
    void testCheckUrlSuccess() throws SQLException {
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
            // Используем нормализованный URL для POST запроса
            client.post(NamedRoutes.urlsPath(), "url=" + normalizedMockServerUrl);

            var maybeUrl = UrlRepository.findByName(normalizedMockServerUrl);
            assertThat(maybeUrl).isPresent();

            long id = maybeUrl.get().getId();
            try (var response = client.post(NamedRoutes.urlChecksPath(String.valueOf(id)), "")) {
                assertThat(response.code()).isEqualTo(200);
            }

            var checks = UrlCheckRepository.findByUrlId(id);
            assertThat(checks).isNotEmpty();

            var check = checks.get(0);
            assertThat(check.getStatusCode()).isEqualTo(200);
            assertThat(check.getTitle()).isEqualTo("Test Title");
            assertThat(check.getH1()).isEqualTo("Test H1");
            assertThat(check.getDescription()).isEqualTo("Test Description");
        });
    }

    @Test
    void testCheckUrlTruncatesLongText() throws SQLException {
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
            client.post(NamedRoutes.urlsPath(), "url=" + normalizedMockServerUrl);

            var maybeUrl = UrlRepository.findByName(normalizedMockServerUrl);
            assertThat(maybeUrl).isPresent();

            long id = maybeUrl.get().getId();
            try (var response = client.post(NamedRoutes.urlChecksPath(String.valueOf(id)), "")) {
                assertThat(response.code()).isEqualTo(200);
            }

            var checks = UrlCheckRepository.findByUrlId(id);
            assertThat(checks).isNotEmpty();

            var check = checks.get(0);
            assertThat(check.getTitle()).isEqualTo(truncatedText);
            assertThat(check.getH1()).isEqualTo(truncatedText);
            assertThat(check.getDescription()).isEqualTo(truncatedText);
        });
    }

    @Test
    void testCheckUrlClientError() throws SQLException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        JavalinTest.test(app, (server, client) -> {
            client.post(NamedRoutes.urlsPath(), "url=" + normalizedMockServerUrl);

            var maybeUrl = UrlRepository.findByName(normalizedMockServerUrl);
            assertThat(maybeUrl).isPresent();

            long id = maybeUrl.get().getId();
            try (var response = client.post(NamedRoutes.urlChecksPath(String.valueOf(id)), "")) {
                assertThat(response.code()).isEqualTo(200);
            }

            // Проверка не должна быть сохранена при статусе 404
            var checks = UrlCheckRepository.findByUrlId(id);
            assertThat(checks).isEmpty();
        });
    }

    @Test
    void testUrlPageShowsDataTestAttributes() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            client.post(NamedRoutes.urlsPath(), "url=https://example.com");

            var maybeUrl = UrlRepository.findByName("https://example.com");
            assertThat(maybeUrl).isPresent();

            long id = maybeUrl.get().getId();
            try (var response = client.get(NamedRoutes.urlPath(String.valueOf(id)))) {
                String body = response.body().string();
                assertThat(response.code()).isEqualTo(200);
                assertThat(body).contains("data-test=\"url\"");
                assertThat(body).contains("method=\"post\"");
                assertThat(body).contains("action=\"/urls/" + id + "/checks\"");
                assertThat(body).contains("data-test=\"checks\"");
            }
        });
    }

    @Test
    void testCheckUrlWithEmptyHtml() throws SQLException {
        String mockHtml = """
            <html>
                <head></head>
                <body></body>
            </html>
            """;
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(mockHtml));

        JavalinTest.test(app, (server, client) -> {
            client.post(NamedRoutes.urlsPath(), "url=" + normalizedMockServerUrl);

            var maybeUrl = UrlRepository.findByName(normalizedMockServerUrl);
            assertThat(maybeUrl).isPresent();

            long id = maybeUrl.get().getId();
            try (var response = client.post(NamedRoutes.urlChecksPath(String.valueOf(id)), "")) {
                assertThat(response.code()).isEqualTo(200);
            }

            var checks = UrlCheckRepository.findByUrlId(id);
            assertThat(checks).isNotEmpty();

            var check = checks.get(0);
            assertThat(check.getTitle()).isEmpty();
            assertThat(check.getH1()).isNull();
            assertThat(check.getDescription()).isNull();
        });
    }

    @Test
    void testCreateUrlWithPort() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=http://localhost:8080")) {
                assertThat(response.code()).isEqualTo(200);
            }

            var maybeUrl = UrlRepository.findByName("http://localhost:8080");
            assertThat(maybeUrl).isPresent();
            assertThat(maybeUrl.get().getName()).isEqualTo("http://localhost:8080");
        });
    }

    @Test
    void testCreateUrlWithHttps() throws SQLException {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post(NamedRoutes.urlsPath(), "url=https://secure.com")) {
                assertThat(response.code()).isEqualTo(200);
            }

            var maybeUrl = UrlRepository.findByName("https://secure.com");
            assertThat(maybeUrl).isPresent();
            assertThat(maybeUrl.get().getName()).isEqualTo("https://secure.com");
        });
    }
}
