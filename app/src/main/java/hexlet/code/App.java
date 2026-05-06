package hexlet.code;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;
import io.javalin.rendering.template.JavalinJte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private static TemplateEngine createTemplateEngine() {
        ClassLoader classLoader = App.class.getClassLoader();
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
        return TemplateEngine.create(codeResolver, ContentType.Html);
    }

    public static Javalin getApp() {
        BaseRepository.dataSource = ConfigDB.getJDBCUrl();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte(createTemplateEngine()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            System.err.println("=== ERROR ===");
            e.printStackTrace(System.err);
            ctx.status(500).result("Error: " + e.getMessage());
        });

        app.get("/", ctx -> {
            var page = new BasePage();
            page.setFlash(ctx.consumeSessionAttribute("flash"));
            page.setFlashType(ctx.consumeSessionAttribute("flash-type"));
            ctx.render("index.jte", Collections.singletonMap("page", page));
        });

        app.post("/urls", ctx -> {
            String inputUrl = ctx.formParam("url");
            String normalizedUrl;

            try {
                var uri = new URI(inputUrl);
                var urlObj = uri.toURL();
                normalizedUrl = String.format("%s://%s%s",
                        urlObj.getProtocol(),
                        urlObj.getHost(),
                        urlObj.getPort() == -1 ? "" : ":" + urlObj.getPort()
                ).toLowerCase();
            } catch (Exception e) {
                var page = new BasePage();
                page.setError("Некорректный URL");
                ctx.status(422);
                ctx.render("index.jte", Collections.singletonMap("page", page));
                return;
            }

            try {
                var existingUrl = UrlRepository.findByName(normalizedUrl);
                if (existingUrl.isPresent()) {
                    ctx.sessionAttribute("flash", "Страница уже существует");
                    ctx.sessionAttribute("flash-type", "info");
                    ctx.redirect("/urls/" + existingUrl.get().getId());
                } else {
                    var url = new Url(normalizedUrl);
                    UrlRepository.save(url);
                    ctx.sessionAttribute("flash", "Страница успешно добавлена");
                    ctx.sessionAttribute("flash-type", "success");
                    ctx.redirect("/urls/" + url.getId());
                }
            } catch (SQLException e) {
                log.error("Database error", e);
                ctx.status(500).result("Database Error");
            }
        });

        app.get("/urls", ctx -> {
            try {
                var urls = UrlRepository.getEntities();
                Map<Long, Integer> lastCheckCode = new HashMap<>();
                Map<Long, LocalDateTime> lastCheckDate = new HashMap<>();  // Добавить

                for (var url : urls) {
                    var lastCheck = UrlCheckRepository.getLastCheckByUrlId(url.getId());
                    lastCheck.ifPresent(check -> {
                        lastCheckCode.put(url.getId(), check.getStatusCode());
                        lastCheckDate.put(url.getId(), check.getCreatedAt());  // Добавить
                    });
                }

                var page = new UrlsPage(urls, lastCheckCode, lastCheckDate);

                page.setFlash(ctx.consumeSessionAttribute("flash"));
                page.setFlashType(ctx.consumeSessionAttribute("flash-type"));
                ctx.render("urls/index.jte", Collections.singletonMap("page", page));
            } catch (SQLException e) {
                log.error("Database error", e);
                ctx.status(500).result("Database Error");
            }
        });

        app.get("/urls/{id}", ctx -> {
            var id = ctx.pathParamAsClass("id", Long.class).get();

            try {
                var url = UrlRepository.findById(id)
                        .orElseThrow(() -> new NotFoundResponse("Url not found"));

                var checks = UrlCheckRepository.findByUrlId(id);
                var page = new UrlPage(url, checks);
                page.setFlash(ctx.consumeSessionAttribute("flash"));
                page.setFlashType(ctx.consumeSessionAttribute("flash-type"));
                ctx.render("urls/show.jte", Collections.singletonMap("page", page));
            } catch (SQLException e) {
                log.error("Database error", e);
                ctx.status(500).result("Database Error");
            }
        });

        app.post("/urls/{id}/checks", ctx -> {
            var id = ctx.pathParamAsClass("id", Long.class).get();

            try {
                var url = UrlRepository.findById(id)
                        .orElseThrow(() -> new NotFoundResponse("Url not found"));
                var httpClient = java.net.http.HttpClient.newHttpClient();
                var request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(url.getName()))
                        .timeout(java.time.Duration.ofSeconds(10))
                        .GET()
                        .build();

                var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                if (statusCode >= 400) {
                    ctx.sessionAttribute("flash", "Произошла ошибка при проверке");
                    ctx.sessionAttribute("flash-type", "danger");
                    ctx.redirect("/urls/" + id);
                    return;
                }

                String html = response.body();
                org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);

                String title = doc.title();
                String h1 = doc.select("h1").first() != null ? doc.select("h1").first().text() : null;
                String description = doc.select("meta[name=description]").first() != null
                        ? doc.select("meta[name=description]").first().attr("content") : null;

                var check = new UrlCheck(id, statusCode, title, h1, description);
                UrlCheckRepository.save(check);

                ctx.sessionAttribute("flash", "Страница успешно проверена");
                ctx.sessionAttribute("flash-type", "success");
                ctx.redirect("/urls/" + id);
            } catch (Exception e) {
                log.error("Check failed", e);
                ctx.sessionAttribute("flash", "Произошла ошибка при проверке");
                ctx.sessionAttribute("flash-type", "danger");
                ctx.redirect("/urls/" + id);
            }
        });
        return app;
    }
    public static void main(String[] args) {
        Javalin app = getApp();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            if (BaseRepository.dataSource != null) {
                BaseRepository.dataSource.close();
            }
        }));

        app.start(port);
    }
}
