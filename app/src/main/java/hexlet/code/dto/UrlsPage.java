package hexlet.code.dto;

import hexlet.code.model.Url;
import lombok.Getter;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UrlsPage extends BasePage {
    private List<Url> urls;
    private Map<Long, Integer> lastCheckCode;
    private Map<Long, LocalDateTime> lastCheckDate;
}
