import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.model.Url;
import hexlet.code.repository.BaseRepository;
import hexlet.code.repository.UrlRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class UrlRepositoryTest {

    private HikariDataSource dataSource;

    @BeforeEach
    void beforeEach() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;");
        dataSource = new HikariDataSource(config);
        BaseRepository.dataSource = dataSource;

        createTables();
    }

    @AfterEach
    void afterEach() {
        if (dataSource != null) {
            dataSource.close();
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
        }
    }

    @Test
    void testSave() throws SQLException {
        Url url = new Url("https://test.com");
        UrlRepository.save(url);

        assertThat(url.getId()).isNotNull();

        var found = UrlRepository.findById(url.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("https://test.com");
    }

    @Test
    void testFindByName() throws SQLException {
        Url url = new Url("https://find.com");
        UrlRepository.save(url);

        var found = UrlRepository.findByName("https://find.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("https://find.com");

        var notFound = UrlRepository.findByName("https://not-exist.com");
        assertThat(notFound).isEmpty();
    }

    @Test
    void testFindByIdNotFound() throws SQLException {
        var found = UrlRepository.findById(99999L);
        assertThat(found).isEmpty();
    }

    @Test
    void testGetEntities() throws SQLException {
        Url url1 = new Url("https://first.com");
        Url url2 = new Url("https://second.com");
        UrlRepository.save(url1);
        UrlRepository.save(url2);

        var urls = UrlRepository.getEntities();
        assertThat(urls).hasSize(2);
        assertThat(urls.get(0).getName()).isEqualTo("https://second.com");
        assertThat(urls.get(1).getName()).isEqualTo("https://first.com");
    }
}