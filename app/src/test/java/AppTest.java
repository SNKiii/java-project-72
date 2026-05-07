import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.App;
import hexlet.code.BaseRepository;
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
        BaseRepository.dataSource = dataSource;

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
            try (var response = client.post("/urls", "url=https://example.com")) {
                assertThat(response.code()).isEqualTo(200);
            }
        });
    }

    @Test
    void testCreateUrlDuplicate() {
        JavalinTest.test(app, (server, client) -> {
            client.post("/urls", "url=https://duplicate.com");
            try (var response = client.post("/urls", "url=https://duplicate.com")) {
                assertThat(response.code()).isEqualTo(200);
            }
        });
    }

    @Test
    void testCreateUrlInvalid() {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.post("/urls", "url=invalid")) {
                assertThat(response.code()).isEqualTo(422);
                assertThat(response.body().string()).contains("Некорректный URL");
            }
        });
    }

    @Test
    void testGetUrlById() {
        JavalinTest.test(app, (server, client) -> {
            client.post("/urls", "url=https://test.com");
            try (var response = client.get("/urls/1")) {
                assertThat(response.code()).isEqualTo(200);
            }
        });
    }

    @Test
    void testGetUrlNotFound() {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.get("/urls/99999")) {
                assertThat(response.code()).isEqualTo(404);
            }
        });
    }

    @Test
    void testCheckUrlSuccess() {
        String mockHtml = """
            <html>
                <head><title>Test Title</title></head>
                <body><h1>Test H1</h1>
                <meta name="description" content="Test Description"></body>
            </html>
            """;
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(mockHtml));

        JavalinTest.test(app, (server, client) -> {
            client.post("/urls", "url=" + mockServerUrl);
            try (var response = client.post("/urls/1/checks", "")) {
                assertThat(response.code()).isEqualTo(200);
            }
        });
    }

    @Test
    void testCheckUrlClientError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        JavalinTest.test(app, (server, client) -> {
            client.post("/urls", "url=" + mockServerUrl);
            try (var response = client.post("/urls/1/checks", "")) {
                assertThat(response.code()).isEqualTo(200);
            }
        });
    }

    @Test
    void testUrlPageShowsDataTestAttributes() {
        JavalinTest.test(app, (server, client) -> {
            client.post("/urls", "url=https://example.com");
            try (var response = client.get("/urls/1")) {
                String body = response.body().string();
                assertThat(response.code()).isEqualTo(200);
                assertThat(body).contains("data-test=\"url\"");
                assertThat(body).contains("method=\"post\"");
                assertThat(body).contains("action=\"/urls/1/checks\"");
                assertThat(body).contains("data-test=\"checks\"");
            }
        });
    }
}