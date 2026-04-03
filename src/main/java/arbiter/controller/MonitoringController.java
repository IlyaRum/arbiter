package arbiter.controller;

import arbiter.config.AppConfig;
import arbiter.di.DependencyInjector;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

public class MonitoringController extends ABaseController{

  private final DependencyInjector dependencyInjector;

  public MonitoringController(Vertx vertx, DependencyInjector dependencyInjector) {
    super(vertx);
    this.dependencyInjector = dependencyInjector;
  }

  @Override
  public void registerRoutes(Router router) {
    router.get("/").handler(this::handleRootRequest);
    router.get(AppConfig.METRICS).handler(this::handleMetricsRequest);
    router.get(AppConfig.INFO).handler(this::handleInfoRequest);
    router.get(AppConfig.RECONNECTION_STATS).handler(this::getReconnectionStats);

    SockJSBridgeOptions options = new SockJSBridgeOptions()
      .addInboundPermitted(new PermittedOptions().setAddress("api.inbound"))
      .addOutboundPermitted(new PermittedOptions().setAddress("api.outbound"));

    router.route("/api-eventbus/*").subRouter(SockJSHandler.create(vertx).bridge(options));

    //TODO[IER] Для Тестов сокета с UI. Удалить
    vertx.setPeriodic(3000, id -> {
      vertx.eventBus().publish("api.outbound", "A message from the arbiter-api at " + System.currentTimeMillis());
    });
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

  public void getReconnectionStats(RoutingContext context) {
    dependencyInjector.getWebSocketManager().getReconnectionStats(context);
  }
}
