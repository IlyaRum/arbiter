package arbiter.service;

import arbiter.config.AppConfig;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class WebSocketService extends ABaseService {
  private final WebSocketClient webSocketClient;
  private final AtomicReference<WebSocket> currentWebSocket = new AtomicReference<>();
  private CompletableFuture<JsonObject> connectionResponseFuture;
  private String currentChannelId;
  private boolean pongReceived = false;
  private Instant dataReceived;


  public WebSocketService(Vertx vertx) {
    super(vertx);
    this.webSocketClient = vertx.createWebSocketClient(new WebSocketClientOptions()
        .setSsl(false)
        .setTrustAll(true)
        .setVerifyHost(false)
        .setLogActivity(true)
      //.setTryUsePerFrameCompression(true)
      //.setTryUsePerMessageCompression(true)
    );
  }

  public Future<JsonObject> connectToWebSocketServer(RoutingContext context) {
    String token = context.get("authToken");

    if (token == null) {
      String errorMsg = "Token is required";
      System.err.println("error: " + errorMsg);
      handleError(context, new IllegalArgumentException(errorMsg));
      return Future.failedFuture(errorMsg);
    }


    Promise<JsonObject> promise = Promise.promise();

    WebSocketConnectOptions options = new WebSocketConnectOptions()
      .setAbsoluteURI("wss://ia-oc-w-aiptst.cdu.so/api/public/core/v2.1/channels/open")
      .addSubProtocol(AppConfig.CLOUDEVENTS_PROTOCOL)
      .setPort(443)
      .addHeader("authorization", "Bearer " + token);

    webSocketClient
      .connect(options)
      .onComplete(res -> {
        if (res.succeeded()) {
          WebSocket webSocket = res.result();
          webSocket.textMessageHandler(handleTextMessage(context, promise, webSocket));

          webSocket.closeHandler(v -> {
            logAsync("WebSocket connection closed");
            if (!promise.future().isComplete()) {
              promise.tryFail("WebSocket connection closed unexpectedly");
            }
          });

//          webSocket.pongHandler(pong -> {
//            pongReceived = true;
//            System.out.println("xxx PONG xxx");
//          });

          // Обработка ошибок
          webSocket.exceptionHandler(closeWebSocket(promise, webSocket));

          //promise.complete();
        } else {
          String fullUri = buildUriFromOptions(options);
          System.err.println("Ошибка подключения к " + fullUri + ": " + res.cause().getMessage());
          handleError(context, new IllegalArgumentException("Ошибка подключения к " + fullUri + ": " + res.cause().getMessage()));
          promise.tryFail("Ошибка подключения: " + res.cause().getMessage());
        }
      });

    return promise.future();
  }

  //для решения WARNING: Thread vert.x-eventloop-thread-1 has been blocked for 769173 ms, time limit is 2000 ms
  private void logAsync(String message) {
    vertx.executeBlocking(() -> {
        System.out.println(message);
        return null;
      }, false)
      .onFailure(err -> {
        System.err.println("Logging failed: " + err.getMessage());
      });
  }

  private static Handler<Throwable> closeWebSocket(Promise<JsonObject> promise, WebSocket webSocket) {
    return error -> {
      System.err.println("WebSocket ошибка: " + error.getMessage());
      if (!promise.future().isComplete()) {
        promise.tryFail("WebSocket ошибка: " + error.getMessage());
      }
      webSocket.close((short) 1011, "Server error");
    };
  }

  private Handler<String> handleTextMessage(RoutingContext context, Promise<JsonObject> promise, WebSocket webSocket) {
    return message -> {

      try {
        EventFormat format = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
        CloudEvent event = format.deserialize(message.getBytes());

        //logCloudEvent(event);

        String eventType = event.getType();
        switch (eventType) {
          case "ru.monitel.ck11.channel.opened.v2":
            handleChannelOpened(event);
            break;

          case "ru.monitel.ck11.measurement-values.data.v2":
            handleMeasurementData(event);
            break;

          case "ru.monitel.ck11.events.stream-started.v2":
            System.out.println("подписка на события стартовала");
            break;

          case "ru.monitel.ck11.events.stream-broken.v2":
            System.out.println("подписка на события остановлена");
            closeWebSocket(promise, webSocket);
            break;

          default:
            if (eventType != null && eventType.startsWith("ru.monitel.ck11.rt-events.")) {
              System.out.println(eventType);
            }
            break;
        }

        handleSuccess(context, 200, message);

      } catch (Exception e) {
        System.err.println("Ошибка парсинга CloudEvent: " + e.getMessage());
        System.err.println("Полученное сообщение: " + message);
        promise.tryFail(e);
      }
    };
  }

  private void handleChannelOpened(CloudEvent event) {
    currentChannelId = event.getSubject();
    logAsync("websocket currentChannelId: " + currentChannelId);
  }

  private void handleMeasurementData(CloudEvent event) {
    CloudEventData cloudEventData = event.getData();
    logAsync("Data: " + cloudEventData);

    assert cloudEventData != null;
    String jsonData = cloudEventData.toString();
    String jsonStr = jsonData
      .replace("JsonCloudEventData{node=", "")
      .replace("}}", "}");
    JsonObject data = new JsonObject(jsonStr);
    JsonObject firstDataItem = data.getJsonArray("data").getJsonObject(0);
    double value = firstDataItem.getDouble("value");
    logAsync("Input value: " + value);
  }

  private String buildUriFromOptions(WebSocketConnectOptions options) {
    String protocol = "wss";
    String host = options.getHost();
    int port = options.getPort();
    String path = options.getURI();

    // Стандартные порты можно не указывать
    if ((port == 443) || (port == 80)) {
      return protocol + "://" + host + path;
    } else {
      return protocol + "://" + host + ":" + port + path;
    }
  }

}
