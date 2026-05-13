import hexlet.code.model.Url;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UrlTest {

    @Test
    void testUrlConstructorAndGetters() {
        String name = "https://example.com";
        Url url = new Url(name);

        assertThat(url.getName()).isEqualTo(name);
        assertThat(url.getCreatedAt()).isNotNull();
    }

    @Test
    void testUrlWithIdAndTimestamp() {
        Long id = 1L;
        String name = "https://example.com";
        LocalDateTime createdAt = LocalDateTime.now();

        Url url = new Url(id, name, createdAt);

        assertThat(url.getId()).isEqualTo(id);
        assertThat(url.getName()).isEqualTo(name);
        assertThat(url.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void testSetters() {
        Url url = new Url("https://example.com");
        url.setId(10L);

        assertThat(url.getId()).isEqualTo(10L);
    }
}
