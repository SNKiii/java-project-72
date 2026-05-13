import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.BaseRepository;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UrlCheckRepositoryTest {

    private Url testUrl;

    @BeforeEach
    void setUp() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;");
        BaseRepository.dataSource = new HikariDataSource(config);

        try (var conn = BaseRepository.dataSource.getConnection();
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

        testUrl = new Url("https://test.com");
        UrlRepository.save(testUrl);
    }

    @Test
    void testSave() throws SQLException {
        UrlCheck check = new UrlCheck(
                testUrl.getId(),
                200,
                "Test Title",
                "Test H1",
                "Test Description"
        );

        UrlCheckRepository.save(check);

        assertThat(check.getId()).isNotNull();

        List<UrlCheck> checks = UrlCheckRepository.findByUrlId(testUrl.getId());
        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatusCode()).isEqualTo(200);
        assertThat(checks.get(0).getTitle()).isEqualTo("Test Title");
        assertThat(checks.get(0).getH1()).isEqualTo("Test H1");
        assertThat(checks.get(0).getDescription()).isEqualTo("Test Description");
    }

    @Test
    void testSaveWithTruncation() throws SQLException {
        String longText = "a".repeat(300);
        String expectedTruncated = "a".repeat(200) + "...";

        UrlCheck check = new UrlCheck(
                testUrl.getId(),
                200,
                longText,
                longText,
                longText
        );

        assertThat(check.getTitle()).isEqualTo(expectedTruncated);
        assertThat(check.getH1()).isEqualTo(expectedTruncated);
        assertThat(check.getDescription()).isEqualTo(expectedTruncated);

        UrlCheckRepository.save(check);

        List<UrlCheck> checks = UrlCheckRepository.findByUrlId(testUrl.getId());
        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getTitle()).isEqualTo(expectedTruncated);
        assertThat(checks.get(0).getH1()).isEqualTo(expectedTruncated);
        assertThat(checks.get(0).getDescription()).isEqualTo(expectedTruncated);
    }

    @Test
    void testFindByUrlId() throws SQLException {
        UrlCheck check1 = new UrlCheck(testUrl.getId(), 200, "Title1", "H1_1", "Desc1");
        UrlCheck check2 = new UrlCheck(testUrl.getId(), 404, "Title2", "H1_2", "Desc2");

        UrlCheckRepository.save(check1);
        UrlCheckRepository.save(check2);

        List<UrlCheck> checks = UrlCheckRepository.findByUrlId(testUrl.getId());

        assertThat(checks).hasSize(2);
        assertThat(checks).extracting(UrlCheck::getStatusCode).containsExactlyInAnyOrder(200, 404);
    }

    @Test
    void testFindByUrlIdEmpty() throws SQLException {
        List<UrlCheck> checks = UrlCheckRepository.findByUrlId(99999L);
        assertThat(checks).isEmpty();
    }

    @Test
    void testGetLastCheckByUrlId() throws SQLException {
        UrlCheck check1 = new UrlCheck(testUrl.getId(), 200, "Title1", "H1_1", "Desc1");
        UrlCheck check2 = new UrlCheck(testUrl.getId(), 404, "Title2", "H1_2", "Desc2");

        UrlCheckRepository.save(check1);
        UrlCheckRepository.save(check2);

        Optional<UrlCheck> lastCheck = UrlCheckRepository.getLastCheckByUrlId(testUrl.getId());

        assertThat(lastCheck).isPresent();
        assertThat(lastCheck.get().getStatusCode()).isEqualTo(404);
        assertThat(lastCheck.get().getTitle()).isEqualTo("Title2");
    }

    @Test
    void testGetLastCheckByUrlIdNotFound() throws SQLException {
        Optional<UrlCheck> lastCheck = UrlCheckRepository.getLastCheckByUrlId(99999L);
        assertThat(lastCheck).isEmpty();
    }

    @Test
    void testFindLatestChecks() throws SQLException {
        Url url2 = new Url("https://test2.com");
        UrlRepository.save(url2);

        UrlCheck check1Url1 = new UrlCheck(testUrl.getId(), 200, "Title1", "H1_1", "Desc1");
        UrlCheck check2Url1 = new UrlCheck(testUrl.getId(), 404, "Title2", "H1_2", "Desc2");

        UrlCheck check1Url2 = new UrlCheck(url2.getId(), 500, "Title3", "H1_3", "Desc3");

        UrlCheckRepository.save(check1Url1);
        UrlCheckRepository.save(check2Url1);
        UrlCheckRepository.save(check1Url2);

        Map<Long, UrlCheck> latestChecks = UrlCheckRepository.findLatestChecks();

        assertThat(latestChecks).hasSize(2);
        assertThat(latestChecks.get(testUrl.getId()).getStatusCode()).isEqualTo(404);
        assertThat(latestChecks.get(testUrl.getId()).getTitle()).isEqualTo("Title2");
        assertThat(latestChecks.get(url2.getId()).getStatusCode()).isEqualTo(500);
        assertThat(latestChecks.get(url2.getId()).getTitle()).isEqualTo("Title3");
    }
}
