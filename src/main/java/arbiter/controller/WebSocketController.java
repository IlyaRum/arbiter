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


  }

  public void connectToWebSocket(RoutingContext context) {
    dependencyInjector.getWebSocketService().connectToWebSocketServer(context);
  }

  public void forceReconnect(RoutingContext context) {
    //TODO[IER] переделать вызов
    //dependencyInjector.getWebSocketService().forceReconnect(context);
  }

  public void stopReconnecting(RoutingContext context) {
    //TODO[IER] переделать вызов
    //dependencyInjector.getWebSocketService().stopReconnecting(context);
  }



  private void getAndValidateToken(RoutingContext context) {
    dependencyInjector.getTokenService().getTokenForContext(context);
  }
}
