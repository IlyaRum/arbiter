package arbiter.controller;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public abstract class ABaseController {

  protected final Vertx vertx;

  public ABaseController(Vertx vertx) {
    this.vertx = vertx;
  }

  public abstract void registerRoutes(Router router);

  protected void handleSuccess(RoutingContext ctx, Object result) {
    ctx.response()
      .setStatusCode(200)
      .putHeader("Content-Type", "application/json")
      .end(io.vertx.core.json.JsonObject.mapFrom(result).encode());
  }

  protected void handleError(RoutingContext ctx, Throwable throwable) {
    ctx.response()
      .setStatusCode(500)
      .putHeader("Content-Type", "application/json")
      .end(new JsonObject()
        .put("error", throwable.getMessage())
        .encode());
  }
}
