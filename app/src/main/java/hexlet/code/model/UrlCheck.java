package hexlet.code.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class UrlCheck {
    private Long id;
    private Long urlId;
    private Integer statusCode;
    private String title;
    private String h1;
    private String description;
    private LocalDateTime createdAt;

    public UrlCheck(Long urlId, Integer statusCode, String title, String h1, String description) {
        this.urlId = urlId;
        this.statusCode = statusCode;
        this.title = truncate(title);
        this.h1 = truncate(h1);
        this.description = truncate(description);
    }

    private static String truncate(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() <= 200) {
            return text;
        }
        return text.substring(0, 200) + "...";
    }
}
