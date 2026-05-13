package hexlet.code;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import hexlet.code.controller.UrlController;
import hexlet.code.dataBase.ConfigDB;
import hexlet.code.repository.BaseRepository;
import hexlet.code.util.NamedRoutes;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.rendering.template.JavalinJte;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

@Slf4j
public class App {

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

        app.exception(SQLException.class, (e, ctx) -> {
            log.error("Database error: {}", e.getMessage(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Database Error");
        });

        app.exception(Exception.class, (e, ctx) -> {
            log.error("Unexpected error for {} {}: {}", ctx.method(), ctx.path(), e.getMessage(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Internal Server Error");
        });

        app.exception(NotFoundResponse.class, (e, ctx) -> {
            log.warn("Not found: {}", ctx.path());
            ctx.status(HttpStatus.NOT_FOUND).result("Page not found");
        });

        app.get(NamedRoutes.rootPath(), UrlController::homePage);
        app.post(NamedRoutes.urlsPath(), UrlController::createUrl);
        app.get(NamedRoutes.urlsPath(), UrlController::listUrls);
        app.get(NamedRoutes.urlPath("{id}"), UrlController::showUrl);
        app.post(NamedRoutes.urlChecksPath("{id}"), UrlController::runCheck);

        return app;
    }

    public static void main(String[] args) {
        Javalin app = getApp();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down application...");
            app.stop();
            if (BaseRepository.dataSource != null) {
                BaseRepository.dataSource.close();
                log.info("Database connection pool closed");
            }
        }));

        app.start(port);
        log.info("Application started on port {}", port);
    }
}
