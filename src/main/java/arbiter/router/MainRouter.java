package arbiter.router;

import arbiter.controller.EventSubscriptionController;
import arbiter.controller.MonitoringController;
import arbiter.controller.SubscriptionController;
import arbiter.controller.WebSocketController;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class MainRouter {

  private final Vertx vertx;
  private final WebSocketController webSocketController;
  private final MonitoringController monitoringController;
  private final SubscriptionController subscriptionController;
  private final EventSubscriptionController eventSubscriptionController;

  public MainRouter(Vertx vertx,
                    WebSocketController webSocketController,
                    MonitoringController monitoringController,
                    SubscriptionController subscriptionController,
                    EventSubscriptionController eventSubscriptionController) {
    this.vertx = vertx;
    this.monitoringController = monitoringController;
    this.webSocketController = webSocketController;
    this.subscriptionController = subscriptionController;
    this.eventSubscriptionController = eventSubscriptionController;
  }

  public Router createRouter() {
    Router router = Router.router(vertx);
    monitoringController.registerRoutes(router);
    webSocketController.registerRoutes(router);
    subscriptionController.registerRoutes(router);
    eventSubscriptionController.registerRoutes(router);
    return router;
  }
}
