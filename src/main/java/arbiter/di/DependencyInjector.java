package arbiter.di;

import arbiter.controller.MonitoringController;
import arbiter.controller.WebSocketController;
import arbiter.service.WebSocketService;
import io.vertx.core.Vertx;

public class DependencyInjector {
  private final Vertx vertx;
  private WebSocketService webSocketService;
  private MonitoringController monitoringController;
  private WebSocketController webSocketController;

  public DependencyInjector(Vertx vertx) {
    this.vertx = vertx;
    initializeServices();
    initializeControllers();
  }

  private void initializeServices() {
    webSocketService = new WebSocketService(vertx);
  }

  private void initializeControllers() {
    monitoringController = new MonitoringController(vertx);
    webSocketController = new WebSocketController(vertx, webSocketService);
  }

  public MonitoringController getMonitoringController() {
    return monitoringController;
  }

  public WebSocketService getWebSocketService() { return webSocketService; }

  public WebSocketController getWebSocketController() { return webSocketController; }
}
