package arbiter.router;

import arbiter.config.AppConfig;
import arbiter.controller.WebSocketController;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class MainRouter {

  private final Vertx vertx;
  private final WebSocketController webSocketController;

  public MainRouter(Vertx vertx, WebSocketController webSocketController) {
    this.vertx = vertx;
    this.webSocketController = webSocketController;
  }

  public Router createRouter() {
    Router router = Router.router(vertx);

    // Глобальные middleware
    router.route().handler(BodyHandler.create());
    router.route().handler(ctx -> {
      ctx.response().putHeader("Content-Type", "application/json");
      ctx.next();
    });

    // Health check
    router.get("/health").handler(ctx -> {
      ctx.json(new JsonObject().put("status", "OK"));
    });

    webSocketController.registerRoutes(router);
    return router;
  }
}
