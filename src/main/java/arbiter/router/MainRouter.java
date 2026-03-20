package arbiter.router;

import arbiter.controller.*;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class MainRouter {

  private final Vertx vertx;
  private final WebSocketController webSocketController;
  private final MonitoringController monitoringController;
  private final SubscriptionController subscriptionController;
  private final EventSubscriptionController eventSubscriptionController;
  private HealthCheckController healthCheckController;

  public MainRouter(Vertx vertx,
                    WebSocketController webSocketController,
                    MonitoringController monitoringController,
                    SubscriptionController subscriptionController,
                    EventSubscriptionController eventSubscriptionController,
                    HealthCheckController healthCheckController) {
    this.vertx = vertx;
    this.monitoringController = monitoringController;
    this.webSocketController = webSocketController;
    this.subscriptionController = subscriptionController;
    this.eventSubscriptionController = eventSubscriptionController;
    this.healthCheckController = healthCheckController;
  }

  public Router createRouter() {
    Router router = Router.router(vertx);
    monitoringController.registerRoutes(router);
    webSocketController.registerRoutes(router);
    subscriptionController.registerRoutes(router);
    eventSubscriptionController.registerRoutes(router);
    healthCheckController.registerRoutes(router);
    return router;
  }
}
