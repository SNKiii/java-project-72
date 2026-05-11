package hexlet.code.Controller;

import hexlet.code.dto.BasePage;
import hexlet.code.dto.UrlPage;
import hexlet.code.dto.UrlsPage;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import hexlet.code.util.NamedRoutes;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import kong.unirest.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;

import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class UrlController {

    public static void homePage(Context ctx) {
        BasePage page = new BasePage();
        page.setFlash(ctx.consumeSessionAttribute("flash"));
        page.setFlashType(ctx.consumeSessionAttribute("flash-type"));
        ctx.render("index.jte", Map.of("page", page));
    }

    public static void createUrl(Context ctx) throws SQLException {
        String inputUrl = ctx.formParam("url");

        if (inputUrl == null || inputUrl.trim().isEmpty()) {
            log.warn("Empty URL submitted");
            BasePage page = new BasePage();
            page.setError("Некорректный URL");
            ctx.status(422);
            ctx.render("index.jte", Map.of("page", page));
            return;
        }

        URI parsedUrl;
        try {
            parsedUrl = new URI(inputUrl);
        } catch (Exception e) {
            log.warn("Invalid URL: {}", inputUrl);
            BasePage page = new BasePage();
            page.setError("Некорректный URL");
            ctx.status(422);
            ctx.render("index.jte", Map.of("page", page));
            return;
        }

        if (parsedUrl.getScheme() == null || parsedUrl.getHost() == null) {
            log.warn("Invalid URL (missing scheme or host): {}", inputUrl);
            BasePage page = new BasePage();
            page.setError("Некорректный URL");
            ctx.status(422);
            ctx.render("index.jte", Map.of("page", page));
            return;
        }

        String normalizedUrl = String.format(
                "%s://%s%s",
                parsedUrl.getScheme().toLowerCase(),
                parsedUrl.getHost().toLowerCase(),
                parsedUrl.getPort() == -1 ? "" : ":" + parsedUrl.getPort()
        );

        var existingUrl = UrlRepository.findByName(normalizedUrl).orElse(null);

        if (existingUrl != null) {
            log.info("URL already exists: {}", normalizedUrl);
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "info");
            ctx.redirect(NamedRoutes.urlPath(existingUrl.getId()));
        } else {
            var newUrl = new Url(normalizedUrl);
            UrlRepository.save(newUrl);
            log.info("URL saved: {}", normalizedUrl);
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flash-type", "success");
            ctx.redirect(NamedRoutes.urlPath(newUrl.getId()));
        }
    }

    public static void listUrls(Context ctx) throws SQLException {
        var urls = UrlRepository.getEntities();
        Map<Long, Integer> lastCheckCode = new HashMap<>();
        Map<Long, LocalDateTime> lastCheckDate = new HashMap<>();

        for (var url : urls) {
            var lastCheck = UrlCheckRepository.getLastCheckByUrlId(url.getId());
            lastCheck.ifPresent(check -> {
                lastCheckCode.put(url.getId(), check.getStatusCode());
                lastCheckDate.put(url.getId(), check.getCreatedAt());
            });
        }

        var page = new UrlsPage(urls, lastCheckCode, lastCheckDate);
        page.setFlash(ctx.consumeSessionAttribute("flash"));
        page.setFlashType(ctx.consumeSessionAttribute("flash-type"));
        ctx.render("urls/index.jte", Map.of("page", page));
    }

    public static void showUrl(Context ctx) throws SQLException {
        var id = ctx.pathParamAsClass("id", Long.class).get();

        var url = UrlRepository.findById(id)
                .orElseThrow(() -> new NotFoundResponse("Url not found"));

        var checks = UrlCheckRepository.findByUrlId(id);
        var page = new UrlPage(url, checks);
        page.setFlash(ctx.consumeSessionAttribute("flash"));
        page.setFlashType(ctx.consumeSessionAttribute("flash-type"));
        ctx.render("urls/show.jte", Map.of("page", page));
    }

    public static void runCheck(Context ctx) throws SQLException {
        var id = ctx.pathParamAsClass("id", Long.class).get();
        var url = UrlRepository.findById(id)
                .orElseThrow(() -> new NotFoundResponse("Url not found"));

        log.info("Starting check for URL: {}", url.getName());

        var response = Unirest.get(url.getName()).asString();
        int statusCode = response.getStatus();

        log.info("HTTP status code for {}: {}", url.getName(), statusCode);

        if (statusCode >= 400) {
            log.warn("Check failed with status {} for {}", statusCode, url.getName());
            ctx.sessionAttribute("flash", "Произошла ошибка при проверке");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect(NamedRoutes.urlPath(id));
            return;
        }

        String html = response.getBody();
        var doc = Jsoup.parse(html);

        String title = doc.title();
        String h1 = doc.select("h1").first() != null ? doc.select("h1").first().text() : null;
        String description = doc.select("meta[name=description]").first() != null
                ? doc.select("meta[name=description]").first().attr("content") : null;

        log.info("Parsed data - Title: {}, H1: {}, Description length: {}",
                title, h1, description != null ? description.length() : 0);

        var check = new UrlCheck(id, statusCode, title, h1, description);
        UrlCheckRepository.save(check);

        log.info("Check saved successfully for URL id={}", id);
        ctx.sessionAttribute("flash", "Страница успешно проверена");
        ctx.sessionAttribute("flash-type", "success");
        ctx.redirect(NamedRoutes.urlPath(id));
    }
}
