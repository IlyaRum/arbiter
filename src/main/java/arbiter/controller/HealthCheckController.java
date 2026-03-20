package arbiter.controller;

import arbiter.config.AppConfig;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HealthCheckController extends ABaseController {

  public HealthCheckController(Vertx vertx) {
    super(vertx);
  }

  @Override
  public void registerRoutes(Router router) {
    router.get(AppConfig.HEALTH).handler(this::handleHealthCheckRequest);
  }

  private void handleHealthCheckRequest(RoutingContext ctx) {
    ctx.response()
      .setStatusCode(200)
      .putHeader("content-type", "text/plain")
      .end("UP");
  }
}
