package hexlet.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class UrlsPage extends BasePage{
    private List<Url> urls;
    private Map<Long, Integer> lastCheckCode;
}
