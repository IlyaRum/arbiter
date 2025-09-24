package arbiter.di;

import arbiter.config.AppConfig;
import arbiter.controller.MonitoringController;
import arbiter.controller.SubscriptionController;
import arbiter.controller.WebSocketController;
import arbiter.service.SubscriptionService;
import arbiter.service.TokenService;
import arbiter.service.WebSocketService;
import io.vertx.core.Vertx;

public class DependencyInjector {
  private final Vertx vertx;
  private MonitoringController monitoringController;
  private WebSocketController webSocketController;
  private WebSocketService webSocketService;
  private SubscriptionController subscriptionController;
  private SubscriptionService subscriptionService;
  private TokenService tokenService;


  public DependencyInjector(Vertx vertx) {
    this.vertx = vertx;
    initializeServices();
    initializeControllers();
  }

  private void initializeServices() {
    webSocketService = new WebSocketService(vertx);
    subscriptionService = new SubscriptionService(vertx);
    tokenService = new TokenService(vertx, AppConfig.getAuthTokenUrl(), AppConfig.getAuthBasicCredentials());
  }

  private void initializeControllers() {
    monitoringController = new MonitoringController(vertx, this);
    webSocketController = new WebSocketController(vertx, webSocketService, tokenService);
    subscriptionController = new SubscriptionController(vertx, subscriptionService, tokenService);

  }

  public MonitoringController getMonitoringController() {
    return monitoringController;
  }

  public WebSocketService getWebSocketService() { return webSocketService; }

  public WebSocketController getWebSocketController() { return webSocketController; }

  public SubscriptionController getSubscriptionController() { return subscriptionController; }

  public TokenService getTokenService() {
    return tokenService;
  }

  public SubscriptionService getSubscriptionService() {
    return subscriptionService;
  }
}
