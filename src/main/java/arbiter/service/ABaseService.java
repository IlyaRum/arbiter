package arbiter.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public abstract class ABaseService {

  protected final Vertx vertx;

  protected ABaseService(Vertx vertx) {
    this.vertx = vertx;
  }

  protected <T> Future<T> failedFuture(Throwable throwable) {
    return Future.failedFuture(throwable);
  }

  protected <T> Future<T> succeededFuture(T result) {
    return Future.succeededFuture(result);
  }

  public void handleSuccess(RoutingContext ctx, Object result) {
    ctx.response()
      .setStatusCode(200)
      .putHeader("Content-Type", "application/json")
      .end(io.vertx.core.json.JsonObject.mapFrom(result).encode());
  }

  public void handleSuccess(RoutingContext ctx, String message) {
    ctx.response()
      .setStatusCode(200)
      .putHeader("Content-Type", "application/json")
      .end(message);
  }

  public void handleError(RoutingContext ctx, Throwable throwable) {
    ctx.response()
      .setStatusCode(500)
      .putHeader("Content-Type", "application/json")
      .end(new JsonObject()
        .put("error", throwable.getMessage())
        .encode());
  }

  public void sendError(RoutingContext context, int statusCode, String message) {
    context.response()
      .setStatusCode(statusCode)
      .putHeader("Content-Type", "application/json")
      .end("{\"error\": \"" + message + "\"}");
  }

  public abstract void stop();
}
