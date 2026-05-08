package hexlet.code;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UrlCheckRepository extends BaseRepository {

    public static void save(UrlCheck check) throws SQLException {
        String sql = "INSERT INTO url_checks (url_id, status_code, title,"
                +
                " h1, description, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, check.getUrlId());
            stmt.setInt(2, check.getStatusCode());
            stmt.setString(3, check.getTitle());
            stmt.setString(4, check.getH1());
            stmt.setString(5, check.getDescription());
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));

            stmt.executeUpdate();

            var generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                check.setId(generatedKeys.getLong(1));
            }
        }
    }

    public static List<UrlCheck> findByUrlId(Long urlId) throws SQLException {
        String sql = "SELECT * FROM url_checks WHERE url_id = ? ORDER BY id DESC";

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, urlId);
            var rs = stmt.executeQuery();

            var checks = new ArrayList<UrlCheck>();
            while (rs.next()) {
                var check = new UrlCheck(
                        rs.getLong("id"),
                        rs.getLong("url_id"),
                        rs.getInt("status_code"),
                        rs.getString("title"),
                        rs.getString("h1"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                checks.add(check);
            }
            return checks;
        }
    }

    public static Optional<UrlCheck> getLastCheckByUrlId(Long urlId) throws SQLException {
        String sql = "SELECT * FROM url_checks WHERE url_id = ? ORDER BY id DESC LIMIT 1";

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, urlId);
            var rs = stmt.executeQuery();

            if (rs.next()) {
                var check = new UrlCheck(
                        rs.getLong("id"),
                        rs.getLong("url_id"),
                        rs.getInt("status_code"),
                        rs.getString("title"),
                        rs.getString("h1"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                return Optional.of(check);
            }
            return Optional.empty();
        }
    }
}
