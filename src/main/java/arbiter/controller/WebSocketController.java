package arbiter.controller;

import arbiter.config.AppConfig;
import arbiter.service.WebSocketService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class WebSocketController extends ABaseController {

  private final WebSocketService webSocketService;

  public WebSocketController(Vertx vertx, WebSocketService webSocketService) {
    super(vertx);
    this.webSocketService = webSocketService;
  }

  @Override
  public void registerRoutes(Router router) {
    router.get(AppConfig.CORE_PREFIX + "/channels/open")
      .handler(this::getAndValidateToken)
      .handler(this::connectToWebSocket);
  }

  public void connectToWebSocket(RoutingContext context) {
    String token = context.get("authToken");

    System.out.println("access_token: " + token);

    if (token == null) {
      webSocketService.handleError(context, new IllegalArgumentException("token are required"));
      return;
    }

    webSocketService.connectToWebSocketServer(token)
      .onSuccess(webSocket -> {
        JsonObject response = new JsonObject()
          .put("status", "connected")
          .put("channelId", webSocketService.getCurrentChannelId())
          .put("message", "WebSocket connection established successfully");

        webSocketService.handleSuccess(context, response);
      })
      .onFailure(throwable -> {
        webSocketService.handleError(context, throwable);
      });
  }

  private void getAndValidateToken(RoutingContext context) {
    webSocketService.getAndValidateToken(context);
  }
}
