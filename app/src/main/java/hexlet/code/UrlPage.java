package hexlet.code;

import lombok.Getter;
import lombok.AllArgsConstructor;
import java.util.List;

@Getter
@AllArgsConstructor
public class UrlPage extends BasePage {
    private Url url;
    private List<UrlCheck> checks;
}