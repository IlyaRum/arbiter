package arbiter.controller;

import arbiter.config.AppConfig;
import arbiter.di.DependencyInjector;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class WebSocketController extends ABaseController {

  private final DependencyInjector dependencyInjector;

  public WebSocketController(Vertx vertx, DependencyInjector dependencyInjector) {
    super(vertx);
    this.dependencyInjector = dependencyInjector;
  }

  @Override
  public void registerRoutes(Router router) {
    router.get(AppConfig.CORE_PREFIX + AppConfig.CHANNELS_OPEN)
      .handler(this::getAndValidateToken)
      .handler(this::connectToWebSocket);

    router.get(AppConfig.CORE_PREFIX + AppConfig.FORCE_RECONNECT)
      .handler(this::forceReconnect);

    router.get(AppConfig.CORE_PREFIX + AppConfig.STOP_RECONNECTING)
      .handler(this::stopReconnecting);

    router.get(AppConfig.CORE_PREFIX + AppConfig.CLOSE_WEBSOCKET_CONNECT)
      .handler(this::closeWebSocketManual);
  }

  public void connectToWebSocket(RoutingContext context) {
    dependencyInjector.getWebSocketService().connectToWebSocketServer(context);
  }

  public void forceReconnect(RoutingContext context) {
    dependencyInjector.getWebSocketManager().forceReconnect(context);
  }

  public void stopReconnecting(RoutingContext context) {
    dependencyInjector.getWebSocketManager().stopReconnecting(context);
  }

  private void getAndValidateToken(RoutingContext context) {
    dependencyInjector.getTokenService().getTokenForContext(context);
  }

  private void closeWebSocketManual(RoutingContext context) {
    dependencyInjector.getWebSocketService().closeWebSocketManual(context);
  }
}
