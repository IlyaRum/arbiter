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
    String token = "xAY0rJSauPYT7opuNOSBszuuM84y6ODgFuIU9kueMAQ1z2q5-MfxonFGLXfKoa2DP4Fp3_qwI9Fd1nlI0MkUwauqRoiyUkLS0Rndty5nTcNeQHGV_J6lmQkpGillng_52-BGrmwUZruW5zx7K_Vpj-iztZsbWon3RhiNVQnmUfcbD_bv0ltAQ4X2JCNBGwNRvdkaIydteA1v5Xb0ma34pzXGRXRi2RJW7UZoEURDv0fGj2EhgTZ2RDepjR4YP2mtu1Ej6jWFT_n84LP0TL6FjbYtUkUwC0tXcMKy6J98t_iwhj7lk5ptX2Yh7XD9bdx2qRZjj1TlFM-ZKSZa3aGNow";

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
