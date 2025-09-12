package arbiter.di;

import arbiter.controller.MonitoringController;
import arbiter.controller.SubscriptionController;
//import arbiter.controller.WebSocketController;
import arbiter.service.SubscriptionService;
import arbiter.service.WebSocketService;
import io.vertx.core.Vertx;

public class DependencyInjector {
  private final Vertx vertx;
  private MonitoringController monitoringController;
  //private WebSocketController webSocketController;
  private WebSocketService webSocketService;
  private SubscriptionController subscriptionController;
  private SubscriptionService subscriptionService;


  public DependencyInjector(Vertx vertx) {
    this.vertx = vertx;
    initializeServices();
    initializeControllers();
  }

  private void initializeServices() {
    webSocketService = new WebSocketService(vertx);
    subscriptionService = new SubscriptionService(vertx);
  }

  private void initializeControllers() {
    monitoringController = new MonitoringController(vertx);
    //webSocketController = new WebSocketController(vertx, webSocketService);
    subscriptionController = new SubscriptionController(vertx, subscriptionService);

  }

  public MonitoringController getMonitoringController() {
    return monitoringController;
  }

  public WebSocketService getWebSocketService() { return webSocketService; }

  //public WebSocketController getWebSocketController() { return webSocketController; }

  public SubscriptionController getSubscriptionController() { return subscriptionController; }
}
