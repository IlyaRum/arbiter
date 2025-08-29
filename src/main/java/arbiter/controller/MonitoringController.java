package arbiter.controller;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MonitoringController extends ABaseController{

  public MonitoringController(Vertx vertx) {
    super(vertx);
  }

  @Override
  public void registerRoutes(Router router) {
    // Добавляем обработчик маршрута GET /
    //curl -v http://localhost:8080
    router.get("/").handler(this::handleRootRequest);
    router.get("/metrics").handler(this::handleMetricsRequest);
    router.get("/info").handler(this::handleInfoRequest);
  }

  private void handleRootRequest(RoutingContext ctx) {
    JsonObject metrics = new JsonObject()
      .put("message","Service running...")
      .put("timestamp", java.time.Instant.now().toString());

    ctx.response()
        .putHeader("content-type", "application/json")
        .end(metrics.encode());
  }

  private void handleMetricsRequest(RoutingContext ctx) {
    Runtime runtime = Runtime.getRuntime();

    JsonObject metrics = new JsonObject()
      .put("memory", new JsonObject()
        .put("free", runtime.freeMemory())
        .put("total", runtime.totalMemory())
        .put("max", runtime.maxMemory())
        .put("used", runtime.totalMemory() - runtime.freeMemory()))
      .put("processors", runtime.availableProcessors())
      .put("timestamp", java.time.Instant.now().toString());

    ctx.response()
      .putHeader("content-type", "application/json")
      .end(metrics.encode());
  }

  private void handleInfoRequest(RoutingContext ctx) {
    JsonObject info = new JsonObject()
      .put("application", "Vert.x 5 Web Application")
      .put("java", new JsonObject()
        .put("version", System.getProperty("java.version"))
        .put("vendor", System.getProperty("java.vendor")))
      .put("os", new JsonObject()
        .put("name", System.getProperty("os.name"))
        .put("version", System.getProperty("os.version")))
      .put("timestamp", java.time.Instant.now().toString());

    ctx.response()
      .putHeader("content-type", "application/json")
      .end(info.encode());
  }
}
