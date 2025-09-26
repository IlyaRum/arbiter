package arbiter.di;

import arbiter.config.AppConfig;
import arbiter.controller.MonitoringController;
import arbiter.controller.SubscriptionController;
import arbiter.controller.WebSocketController;
import arbiter.initialization.SubscriptionManager;
import arbiter.initialization.WebSocketManager;
import arbiter.service.SubscriptionService;
import arbiter.service.TokenService;
import arbiter.service.WebSocketService;
import data.UnitCollection;
import io.vertx.core.Vertx;

public class DependencyInjector {
  private final Vertx vertx;
  private WebSocketManager webSocketManager;
  private SubscriptionManager subscriptionManager;
  private MonitoringController monitoringController;
  private WebSocketController webSocketController;
  private WebSocketService webSocketService;
  private SubscriptionController subscriptionController;
  private SubscriptionService subscriptionService;
  private TokenService tokenService;
  private UnitCollection unitCollection;


  public DependencyInjector(Vertx vertx) {
    this.vertx = vertx;
    initializeManagers();
    initializeServices();
    initializeControllers();
  }

  private void initializeManagers(){
    unitCollection = new UnitCollection(vertx, AppConfig.ARBITER_CONFIG_FILE, "1.0.0");
    webSocketManager = new WebSocketManager(this);
    subscriptionManager = new SubscriptionManager(this);
  }

  private void initializeServices() {
    webSocketService = new WebSocketService(vertx, this);
    subscriptionService = new SubscriptionService(vertx);
    tokenService = new TokenService(vertx, AppConfig.getAuthTokenUrl(), AppConfig.getAuthBasicCredentials());
  }

  private void initializeControllers() {
    monitoringController = new MonitoringController(vertx, this);
    webSocketController = new WebSocketController(vertx, this);
    subscriptionController = new SubscriptionController(vertx, this);

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

  public Vertx getVertx() {
    return vertx;
  }

  public WebSocketManager getWebSocketManager() {
    return webSocketManager;
  }

  public SubscriptionManager getSubscriptionManager() {
    return subscriptionManager;
  }

  public UnitCollection getUnitCollection() {
    return unitCollection;
  }
}
