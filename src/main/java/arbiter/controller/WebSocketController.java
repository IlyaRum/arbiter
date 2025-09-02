package arbiter.controller;

import arbiter.config.AppConfig;
import arbiter.service.WebSocketService;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketConnectOptions;
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
    router.get(AppConfig.API_PREFIX + "/channels/open").handler(this::connectToWebSocket);
  }

  public void connectToWebSocket(RoutingContext ctx) {
//    JsonObject requestBody = ctx.body().asJsonObject();
//    if (requestBody == null) {
//      handleError(ctx, new IllegalArgumentException("Request body is required"));
//      return;
//    }

//    String apiUrl = requestBody.getString("apiUrl");
//    String token = requestBody.getString("token");
    String token = "ifxWuOiZjN00HLwgu20k9yYfYynvAkQOaO1YpcedliIvMJteTkHeiLibr-K99NPSwV7YONAav-iQSe_LcUzUFgHpuPXUTP-ozionoPmoWBJqJw68WYdt8ObcOhZYQnIrzKyu-HPP3DQr05CyiCgi_uK_soPoBp40C9fBbz08t4U4e_ikyTZioBg2UULaC-zdEv_MiTDToZ0P0iOZZmXQfL8OekaypTI9Wzu__TELHYFSpoIlabcDiAeYzie-iQRiElAaSfrbp3GGQt6ifk2ZgS9LxJ8dihm8Agc9E5EyKmFD6QSlcA4XvGiOi4WZ6pmlwLNia0W2sl9TA1Cw1Fy8WQ";

//    if ( token == null) {
//      handleError(ctx, new IllegalArgumentException("apiUrl and token are required"));
//      return;
//    }


    Future<JsonObject> jsonObjectFuture = webSocketService.connectToWebSocketServer(token);


    jsonObjectFuture
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

//  private static void attemptReconnect(Vertx vertx) {
//    if (retryCount < MAX_RETRIES) {
//      retryCount++;
//      long delay = (long) (1000 * Math.pow(2, retryCount)); // Экспоненциальная задержка
//
//      System.out.println("Reconnecting in " + delay + "ms (attempt " + retryCount + ")");
//
//      vertx.setTimer(delay, id -> {
//        connectWebSocket(vertx);
//      });
//    } else {
//      System.out.println("Max reconnection attempts reached");
//      vertx.close();
//    }
//  }
}
