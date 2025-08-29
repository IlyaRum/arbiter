package arbiter.controller;

import arbiter.config.AppConfig;
import arbiter.service.WebSocketService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class WebSocketController extends ABaseController{

  private final WebSocketService webSocketService;

  public WebSocketController(Vertx vertx, WebSocketService webSocketService) {
    super(vertx);
    this.webSocketService = webSocketService;
  }

  @Override
  public void registerRoutes(Router router) {
    router.post(AppConfig.API_PREFIX + "/channels/connect").handler(this::connectToWebSocket);
  }

  public void connectToWebSocket(RoutingContext ctx) {
    JsonObject requestBody = ctx.body().asJsonObject();
    if (requestBody == null) {
      handleError(ctx, new IllegalArgumentException("Request body is required"));
      return;
    }

    String apiUrl = requestBody.getString("apiUrl");
    String token = requestBody.getString("token");

    if (apiUrl == null || token == null) {
      handleError(ctx, new IllegalArgumentException("apiUrl and token are required"));
      return;
    }

    webSocketService.connectToWebSocketServer(apiUrl, token)
      .onSuccess(webSocket -> {
        JsonObject response = new JsonObject()
          .put("status", "connected")
          .put("channelId", webSocketService.getCurrentChannelId())
          .put("message", "WebSocket connection established successfully");

        handleSuccess(ctx, response);
      })
      .onFailure(throwable -> {
        handleError(ctx, throwable);
      });
  }
}
