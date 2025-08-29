package arbiter.di;

import arbiter.controller.WebSocketController;
import arbiter.service.WebSocketService;
import io.vertx.core.Vertx;

public class DependencyInjector {
  private final Vertx vertx;
  private WebSocketService webSocketService;
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
    webSocketController = new WebSocketController(vertx, webSocketService);
  }

  // Геттеры для сервисов
  public WebSocketService getWebSocketService() { return webSocketService; }

  // Геттеры для контроллеров
  public WebSocketController getWebSocketController() { return webSocketController; }
}
