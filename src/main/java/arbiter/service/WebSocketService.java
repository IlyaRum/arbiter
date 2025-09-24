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
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class WebSocketService extends ABaseService {
  private final WebSocketClient webSocketClient;
  private final AtomicReference<WebSocket> currentWebSocket = new AtomicReference<>();
  private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
  private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
  private final AtomicInteger maxReconnectAttempts = new AtomicInteger(100);
  private final AtomicInteger reconnectDelayMs = new AtomicInteger(10000);
  private String currentChannelId;
  private boolean pongReceived = false;
  private String currentToken;
  private long reconnectTimerId = -1;


  private static final EventFormat JSON_FORMAT = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
  private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

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

  private Future<JsonObject> connectToWebSocketServer(String token, RoutingContext context) {
    System.out.println("connectToWebSocketServer: token " + token);

    if (token == null || token.isEmpty()) {
      String errorMsg = "Token is required";
      logger.error("error: " + errorMsg);
      if (context != null) {
        handleError(context, new IllegalArgumentException(errorMsg));
      }
      return Future.failedFuture(errorMsg);
    }

    reconnectAttempts.set(0);
    currentToken = token;

    Promise<JsonObject> promise = Promise.promise();

    WebSocketConnectOptions options = createWebSocketConnectOptions(token);

    webSocketClient
      .connect(options)
      .onComplete(res -> {
        if (res.succeeded()) {
          WebSocket webSocket = res.result();
          currentWebSocket.set(webSocket);
          setupWebSocketHandlers(webSocket, promise);

          // Таймаут на установление соединения и получение первого сообщения
          vertx.setTimer(30000, timerId -> {
            if (!promise.future().isComplete()) {
              promise.tryFail("WebSocket connection timeout - no opening message received");
            }
          });
        } else {
          handleConnectionError(res.cause(), promise, context, options);
        }
      });

    return promise.future();
  }

  private void setupWebSocketHandlers(WebSocket webSocket, Promise<JsonObject> promise) {
    webSocket.textMessageHandler(handleTextMessage(promise, webSocket));

    webSocket.closeHandler(v -> {
      logger.info("WebSocket connection closed");
      if (!promise.future().isComplete()) {
        promise.tryFail("WebSocket connection closed unexpectedly");
      }
      // Здесь можно добавить логику переподключения
      scheduleReconnect();
    });

    webSocket.pongHandler(pong -> {
      pongReceived = true;
      logger.debug("PONG received");
    });

    // Обработка ошибок
    webSocket.exceptionHandler(error -> {
      logger.error("WebSocket ошибка: " + error.getMessage());
      if (!promise.future().isComplete()) {
        promise.tryFail("WebSocket ошибка: " + error.getMessage());
      }
      // Запускаем переподключение при ошибке
      scheduleReconnect();
    });
  }

  private static WebSocketConnectOptions createWebSocketConnectOptions(String token) {
    WebSocketConnectOptions options = new WebSocketConnectOptions()
      .setAbsoluteURI("wss://ia-oc-w-aiptst.cdu.so/api/public/core/v2.1/channels/open")
      .addSubProtocol(AppConfig.CLOUDEVENTS_PROTOCOL)
      .setPort(443)
      .addHeader("authorization", "Bearer " + token);
    return options;
  }

  private void reconnect() {
    if (isReconnecting.get() || reconnectAttempts.get() >= maxReconnectAttempts.get()) {
      logger.warn(String.format("Reconnection stopped: attempts=%d, max=%d, reconnecting=%s",
        reconnectAttempts.get(), maxReconnectAttempts.get(), isReconnecting.get()));
      return;
    }

    if (currentToken == null) {
      logger.error("Cannot reconnect: no token available");
      return;
    }

    isReconnecting.set(true);
    int attempt = reconnectAttempts.incrementAndGet();

    logger.info(String.format("))) попытка переподключения к ОИК %d из %d", attempt, maxReconnectAttempts.get()));

    WebSocketConnectOptions options = createWebSocketConnectOptions(currentToken);

    webSocketClient
      .connect(options)
      .onComplete(res -> {
        isReconnecting.set(false);

        if (res.succeeded()) {
          WebSocket webSocket = res.result();
          currentWebSocket.set(webSocket);
          reconnectAttempts.set(0); // Сбрасываем счетчик при успешном подключении

          logger.info("))) успешное переподключение к ОИК");

          // Настраиваем обработчики для нового соединения
          setupWebSocketHandlers(webSocket, Promise.promise());

        } else {
          logger.error(String.format("((( ошибка переподключения к ОИК %d из %d", attempt, maxReconnectAttempts.get()));

          // Планируем следующую попытку переподключения
          if (attempt < maxReconnectAttempts.get()) {
            scheduleReconnect();
          } else {
            logger.error(String.format("Достигнуто максимальное количество попыток переподключения: %d", maxReconnectAttempts.get()));
            // Здесь можно уведомить о критической ошибке
          }
        }
      });
  }

  /**
   * Планирование переподключения с задержкой
   */
  private void scheduleReconnect() {
    if (reconnectTimerId != -1) {
      vertx.cancelTimer(reconnectTimerId);
    }

    if (reconnectAttempts.get() >= maxReconnectAttempts.get()) {
      logger.error("Превышено максимальное количество попыток переподключения");
      return;
    }

    int delay = reconnectDelayMs.get();
    logger.info(String.format("Планируем переподключение через %d мс", delay));

    reconnectTimerId = vertx.setTimer(delay, timerId -> {
      reconnectTimerId = -1;
      reconnect();
    });
  }

  /**
   * Принудительное переподключение
   */
  public void forceReconnect(RoutingContext context) {
    logger.info("Принудительное переподключение");
    reconnectAttempts.set(0);

    WebSocket existingWebSocket = currentWebSocket.get();
    if (existingWebSocket != null) {
      existingWebSocket.close((short) 1000, "Force reconnect");
    } else {
      reconnect();
    }
  }

  /**
   * Остановка всех попыток переподключения
   */
  public void stopReconnecting(RoutingContext context) {
    if (reconnectTimerId != -1) {
      vertx.cancelTimer(reconnectTimerId);
      reconnectTimerId = -1;
    }
    isReconnecting.set(false);
    logger.info("Переподключение остановлено");
  }

  /**
   * Установка максимального количества попыток переподключения
   */
  public void setMaxReconnectAttempts(int maxAttempts) {
    this.maxReconnectAttempts.set(maxAttempts);
    logger.info(String.format("Установлено максимальное количество попыток переподключения: %d", maxAttempts));
  }

  /**
   * Установка задержки между попытками переподключения
   */
  public void setReconnectDelay(int delayMs) {
    this.reconnectDelayMs.set(delayMs);
    logger.info(String.format("Установлена задержка переподключения: %d мс", delayMs));
  }

  /**
   * Проверка состояния соединения
   */
  public boolean isConnected() {
    WebSocket webSocket = currentWebSocket.get();
    return webSocket != null && !webSocket.isClosed();
  }

  /**
   * Получение статистики переподключений
   */
  public void getReconnectionStats(RoutingContext context) {
    JsonObject stats = new JsonObject()
      .put("reconnectAttempts", reconnectAttempts.get())
      .put("maxReconnectAttempts", maxReconnectAttempts.get())
      .put("isReconnecting", isReconnecting.get())
      .put("isConnected", isConnected())
      .put("reconnectDelayMs", reconnectDelayMs.get());

    context.response()
      .putHeader("content-type", "application/json")
      .end(stats.encode());
  }

  public void connectToWebSocketServer(RoutingContext context) {
    String token = context.get("authToken");
    connectToWebSocketServer(token, context);
  }

  public Future<JsonObject> connectToWebSocketServer(String token) {
    return connectToWebSocketServer(token, null);
  }

  //для решения WARNING: Thread vert.x-eventloop-thread-1 has been blocked for 769173 ms, time limit is 2000 ms
  private void logAsync(String message) {
    vertx.executeBlocking(() -> {
        System.out.println(message);
        System.out.println("----");
        return null;
      }, false)
      .onFailure(err -> {
        System.err.println("Logging failed: " + err.getMessage());
      });
  }

  private static Handler<Throwable> closeWebSocket(Promise<JsonObject> promise, WebSocket webSocket) {
    return error -> {
      logger.error("WebSocket ошибка: " + error.getMessage());
      if (!promise.future().isComplete()) {
        promise.tryFail("WebSocket ошибка: " + error.getMessage());
      }
      webSocket.close((short) 1011, "Server error");
    };
  }

  private Handler<String> handleTextMessage(Promise<JsonObject> promise, WebSocket webSocket) {
    return message -> {
      try {
        EventFormat format = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
        CloudEvent event = format.deserialize(message.getBytes());

        //logCloudEvent(event);

        String eventType = event.getType();
        switch (eventType) {
          case "ru.monitel.ck11.channel.opened.v2":
            handleChannelOpened(event);

            // Завершаем promise только при получении сообщения об открытии
            if (!promise.future().isComplete()) {
              String data = cloudEventToString(event);
              JsonObject jsonData = new JsonObject(data);
              promise.tryComplete(jsonData);
            }
            break;

          case "ru.monitel.ck11.measurement-values.data.v2":
            handleMeasurementData(event);
            break;

          case "ru.monitel.ck11.events.stream-started.v2":
            logger.info("подписка на события стартовала");
            break;

          case "ru.monitel.ck11.events.stream-broken.v2":
            logger.info("подписка на события остановлена");
            closeWebSocket(promise, webSocket);
            break;

          default:
            if (eventType != null && eventType.startsWith("ru.monitel.ck11.rt-events.")) {
              System.out.println(eventType);
            }
            break;
        }
      } catch (Exception e) {
        logger.error("Ошибка парсинга CloudEvent: " + e.getMessage());
        logger.info("Полученное сообщение: " + message);
        promise.tryFail(e);
      }
    };
  }

  private void handleChannelOpened(CloudEvent event) {
    currentChannelId = event.getSubject();
    logAsync("currentChannelId: " + currentChannelId);
  }

  private void handleMeasurementData(CloudEvent event) {
    CloudEventData cloudEventData = event.getData();
    //logAsync("Data: " + cloudEventData);

    assert cloudEventData != null;
    String jsonData = cloudEventData.toString();
    String jsonStr = jsonData
      .replace("JsonCloudEventData{node=", "")
      .replace("}}", "}");

    JsonObject data = new JsonObject(jsonStr);
    JsonArray dataArray = data.getJsonArray("data");

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < dataArray.size(); i++) {
      JsonObject item = dataArray.getJsonObject(i);
      String uid = item.getString("uid");
      double value = item.getDouble("value");

      if (i > 0) {
        result.append("; ");
      }
      result.append(String.format("%s = %f", uid, value));
    }

    logAsync("Result: " + result);
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

  // Конвертация CloudEvent в строку JSON
  public static String cloudEventToString(CloudEvent event) {
    byte[] bytes = JSON_FORMAT.serialize(event);
    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
  }

  private void handleConnectionError(Throwable cause, Promise<JsonObject> promise,
                                     RoutingContext context, WebSocketConnectOptions options) {
    String fullUri = buildUriFromOptions(options);
    String errorMsg = "Ошибка подключения к " + fullUri + ": " + cause.getMessage();
    logger.error(errorMsg);

    if (context != null) {
      handleError(context, new IllegalArgumentException(errorMsg));
    }

    promise.tryFail("Ошибка подключения: " + cause.getMessage());

    // Запускаем переподключение при ошибке соединения
    scheduleReconnect();
  }
}
