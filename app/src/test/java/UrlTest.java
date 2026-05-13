import hexlet.code.model.Url;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UrlTest {

    @Test
    void testUrlConstructorWithName() {
        String name = "https://example.com";
        Url url = new Url(name);

        assertThat(url.getName()).isEqualTo(name);
        assertThat(url.getId()).isNull(); // id не установлен
        assertThat(url.getCreatedAt()).isNull(); // createdAt не инициализируется в этом конструкторе
    }

    @Test
    void testUrlConstructorWithAllFields() {
        Long id = 1L;
        String name = "https://example.com";
        LocalDateTime createdAt = LocalDateTime.now();

        Url url = new Url(id, name, createdAt);

        assertThat(url.getId()).isEqualTo(id);
        assertThat(url.getName()).isEqualTo(name);
        assertThat(url.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void testUrlSetters() {
        Url url = new Url("initial.com");
        url.setId(100L);
        url.setName("updated.com");
        LocalDateTime now = LocalDateTime.now();
        url.setCreatedAt(now);

        assertThat(url.getId()).isEqualTo(100L);
        assertThat(url.getName()).isEqualTo("updated.com");
        assertThat(url.getCreatedAt()).isEqualTo(now);
    }
}
