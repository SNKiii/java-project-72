package hexlet.code.dataBase;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.stream.Collectors;

@Slf4j
public class ConfigDB {

    public static HikariDataSource getJDBCUrl() {
        String jdbcUrl = System.getProperty("TEST_DATABASE_URL");

        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            jdbcUrl = System.getenv().getOrDefault("JDBC_DATABASE_URL",
                    "jdbc:h2:mem:project;DB_CLOSE_DELAY=-1;");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        HikariDataSource dataSource = new HikariDataSource(config);

        createTablesFromSchema(dataSource);

        return dataSource;
    }

    private static void createTablesFromSchema(HikariDataSource dataSource) {
        try {
            InputStream inputStream = ConfigDB.class.getClassLoader()
                    .getResourceAsStream("schema.sql");

            if (inputStream == null) {
                log.error("schema.sql not found in resources");
                return;
            }

            String sql = new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.joining("\n"));

            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement()) {

                for (String command : sql.split(";")) {
                    String trimmed = command.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            stmt.execute(trimmed);
                        } catch (SQLException e) {
                            if (!e.getMessage().contains("already exists")) {
                                log.error("SQL Error: {}", e.getMessage());
                            }
                        }
                    }
                }
                log.info("Database schema initialized successfully");
            }
        } catch (Exception e) {
            log.error("Failed to initialize database schema: {}", e.getMessage(), e);
        }
    }
}
