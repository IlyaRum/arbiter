package arbiter.controller;

import arbiter.config.AppConfig;
import arbiter.service.TokenService;
import arbiter.service.WebSocketService;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class WebSocketController extends ABaseController {

  private final WebSocketService webSocketService;
  private final TokenService tokenService;

  public WebSocketController(Vertx vertx, WebSocketService webSocketService, TokenService tokenService) {
    super(vertx);
    this.webSocketService = webSocketService;
    this.tokenService = tokenService;
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
    webSocketService.connectToWebSocketServer(context);
  }

  public void forceReconnect(RoutingContext context) {
    webSocketService.forceReconnect(context);
  }

  public void stopReconnecting(RoutingContext context) {
    webSocketService.stopReconnecting(context);
  }



  private void getAndValidateToken(RoutingContext context) {
    tokenService.getTokenForContext(context);
  }
}
