package arbiter.controller;

import arbiter.MainVerticle;
import arbiter.config.AppConfig;
import arbiter.service.WebSocketService;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;

public class WebSocketController extends ABaseController {

  private final WebSocketService webSocketService;

  public WebSocketController(Vertx vertx, WebSocketService webSocketService) {
    super(vertx);
    this.webSocketService = webSocketService;
  }

  @Override
  public void registerRoutes(Router router) {
    router.get(AppConfig.API_PREFIX + "/channels/open")
      .handler(this::getAndValidateToken)
      .handler(this::connectToWebSocket);
  }

  public void connectToWebSocket(RoutingContext context) {
    String token = context.get("authToken");

    System.out.println("access_token: " + token);

    if (token == null) {
      handleError(context, new IllegalArgumentException("token are required"));
      return;
    }

    webSocketService.connectToWebSocketServer(token)
      .onSuccess(webSocket -> {
        JsonObject response = new JsonObject()
          .put("status", "connected")
          .put("channelId", webSocketService.getCurrentChannelId())
          .put("message", "WebSocket connection established successfully");

        handleSuccess(context, response);
      })
      .onFailure(throwable -> {
        handleError(context, throwable);
      });
  }

  private void getAndValidateToken(RoutingContext context) {
    // Специальные настройки для обхода SSL
    HttpClientOptions options = new HttpClientOptions()
      .setSsl(true)
      .setTrustAll(true) //отключает проверку сертификатов
      .setVerifyHost(false); //Отключает проверку hostname

    WebClient insecureClient = WebClient.wrap(context.vertx().createHttpClient(options));

    // Вызываем эндпоинт для получения токена
    insecureClient.postAbs("https://ia-oc-w-aiptst.cdu.so:9443/auth/app/token")
      .putHeader("Authorization", "Basic " + MainVerticle.getAuthBasicCredentials())
      .putHeader("Content-Type", "application/json")
      .send()
      .onSuccess(response -> {
        if (response.statusCode() == 200) {
          try {
            JsonObject tokenResponse = response.bodyAsJsonObject();
            String token = tokenResponse.getString("access_token"); // предполагаемое поле с токеном

            if (token != null && !token.isEmpty()) {
              context.put("authToken", token);
              context.next();
            } else {
              sendError(context, 401, "Token not found in response");
            }
          } catch (Exception e) {
            sendError(context, 500, "Invalid token response format");
          }
        } else {
          sendError(context, response.statusCode(), "Token request failed: " + response.bodyAsString());
        }
      })
      .onFailure(err -> {
        sendError(context, 500, "Token service unavailable: " + err.getMessage());
      });
  }

  private void sendError(RoutingContext context, int statusCode, String message) {
    context.response()
      .setStatusCode(statusCode)
      .putHeader("Content-Type", "application/json")
      .end("{\"error\": \"" + message + "\"}");
  }
}
