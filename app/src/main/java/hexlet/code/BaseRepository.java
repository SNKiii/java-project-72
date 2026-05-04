package hexlet.code;

import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public class BaseRepository{
    public HikariDataSource dataSource;
}

