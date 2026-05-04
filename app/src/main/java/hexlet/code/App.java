package hexlet.code;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;

import java.io.IOException;

public class App {

    private static TemplateEngine createTemplateEngine() {
        ClassLoader classLoader = App.class.getClassLoader();
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
        TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Html);
        return templateEngine;
    }

    public static Javalin getApp() {

        var hikariDataSource = ConfigDB.getJDBCUrl();
        var baseConfig = new BaseRepository(hikariDataSource);

         Javalin app = Javalin.create(config -> {
             config.bundledPlugins.enableDevLogging();
             config.fileRenderer(new JavalinJte(createTemplateEngine()));
         });

        app.get("/", ctx -> {
            ctx.contentType("text/html");
            ctx.result("<h1>Hello World</h1>");
        });

        return app;
    }

   public static void main(String[] args) throws IOException {
        Javalin app = getApp();
       int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7070"));
       app.start(port);
    }
}
