package arbiter.router;

import arbiter.controller.MonitoringController;
import arbiter.controller.WebSocketController;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class MainRouter {

  private final Vertx vertx;
  private final WebSocketController webSocketController;
  private final MonitoringController monitoringController;

  public MainRouter(Vertx vertx, WebSocketController webSocketController, MonitoringController monitoringController) {
    this.vertx = vertx;
    this.monitoringController = monitoringController;
    this.webSocketController = webSocketController;
  }

  public Router createRouter() {
    Router router = Router.router(vertx);
    monitoringController.registerRoutes(router);
    webSocketController.registerRoutes(router);
    return router;
  }
}
