package hexlet.code;

import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;

import java.io.IOException;

public class App {
    public static Javalin getApp() {
         Javalin app = Javalin.create(config -> {
             config.bundledPlugins.enableDevLogging();
             config.fileRenderer(new JavalinJte());
         });

        app.get("/", ctx -> {
            ctx.contentType("text/html");
            ctx.result("<h1>Hello Hexlet</h1>");
        });

        return app;
    }

   public static void main(String[] args) throws IOException {
        Javalin app = getApp();
        app.start(7070);
    }
}
