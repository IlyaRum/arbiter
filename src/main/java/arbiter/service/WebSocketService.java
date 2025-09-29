package arbiter.service;

import arbiter.config.AppConfig;
import arbiter.constants.CloudEventStrings;
import arbiter.di.DependencyInjector;
import com.fasterxml.jackson.databind.JsonNode;
import data.*;
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
import measurement.MeasurementList;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class WebSocketService extends ABaseService {
  private final WebSocketClient webSocketClient;
  private final AtomicReference<WebSocket> currentWebSocket = new AtomicReference<>();
  private final Map<String, MemoryData> store = new HashMap<>();
  private String currentChannelId;
  private boolean pongReceived = false;
  private final DependencyInjector dependencyInjector;


  //TODO[IER] Пересмотреть обработчики событий для внешнего управления
  private Runnable closeHandler; // Изменено с Consumer<Void> на Runnable
  private Consumer<Throwable> exceptionHandler;

  private static final EventFormat JSON_FORMAT = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
  private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

  public WebSocketService(Vertx vertx, DependencyInjector dependencyInjector) {
    super(vertx);
    this.dependencyInjector = dependencyInjector;
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

    Promise<JsonObject> promise = Promise.promise();

    if (Objects.equals(AppConfig.getDevFlag(), "local")) {
      handleTextMessage(promise).handle(CloudEventStrings.MEASUREMENT_VALUES_DATA_V2);
    }
    else {
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
    }
    return promise.future();
  }

  private void setupWebSocketHandlers(WebSocket webSocket, Promise<JsonObject> promise) {
    webSocket.textMessageHandler(handleTextMessage(promise));

    webSocket.closeHandler(v -> {
      logger.info("WebSocket connection closed");
      if (!promise.future().isComplete()) {
        promise.tryFail("WebSocket connection closed unexpectedly");
      }
      // Вызываем внешний обработчик закрытия
      if (closeHandler != null) {
        closeHandler.run();
      }
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
      // Вызываем внешний обработчик ошибок
      if (exceptionHandler != null) {
        exceptionHandler.accept(error);
      }
    });
  }

  /**
   * Закрытие соединения
   */
  public void closeConnection() {
    WebSocket webSocket = currentWebSocket.get();
    if (webSocket != null) {
      webSocket.close((short) 1000, "Manual closure");
      currentWebSocket.set(null);
    }
  }

  /**
   * Закрытие соединения вручную
   */
  public void closeWebSocketManual(RoutingContext context) {
    this.closeConnection();
    logger.info("Manual webSocket connection closed");
    handleSuccess(context, 200, "Manual webSocket connection closed");
  }

  /**
   * Установка обработчика закрытия соединения
   */
  public void setCloseHandler(Runnable closeHandler) {
    this.closeHandler = closeHandler;
  }

  /**
   * Установка обработчика ошибок
   */
  public void setExceptionHandler(Consumer<Throwable> exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
  }

  /**
   * Проверка состояния соединения
   */
  public boolean isConnected() {
    WebSocket webSocket = currentWebSocket.get();
    return webSocket != null && !webSocket.isClosed();
  }

  private static WebSocketConnectOptions createWebSocketConnectOptions(String token) {
    return new WebSocketConnectOptions()
      .setAbsoluteURI("wss://ia-oc-w-aiptst.cdu.so/api/public/core/v2.1/channels/open")
      .addSubProtocol(AppConfig.CLOUDEVENTS_PROTOCOL)
      .setPort(443)
      .addHeader("authorization", "Bearer " + token);
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

  private Handler<String> handleTextMessage(Promise<JsonObject> promise) {
    return message -> {
      try {
        EventFormat format = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE);
        CloudEvent event = format.deserialize(message.getBytes(StandardCharsets.UTF_8));

        //logCloudEvent(event);

        String eventType = event.getType();
        //logger.debug("eventType: " + eventType);
        if (eventType.equals("ru.monitel.ck11.channel.opened.v2")) {
          handleChannelOpened(event);
          // Завершаем promise только при получении сообщения об открытии
          if (!promise.future().isComplete()) {
            String data = cloudEventToString(event);
            JsonObject jsonData = new JsonObject(data);
            promise.tryComplete(jsonData);
          }
        } else if (eventType.equals("ru.monitel.ck11.measurement-values.data.v2")) {
          //тип для подписки на актуальные данные: ru.monitel.ck11.measurement-values.data.v2;
          handleMeasurementData(event);
          //TODO[IER] здесь нужно будет сохранить в объект полученные данные
        } else if (eventType.startsWith("ru.monitel.ck11.rt-events.")) {
          //события реального времени
          CloudEventData cloudEventData = event.getData();
          logger.info("[rt-events]CloudEventData: " + cloudEventData);
          //TODO[IER] здесь нужно реализовать полученные данные из эвента
        } else if (eventType.equals("ru.monitel.ck11.events.stream-started.v2")) {
          logger.info("подписка на события стартовала");
        } else if (eventType.equals("ru.monitel.ck11.events.stream-broken.v2")) {
          logger.info("подписка на события остановлена");
          //close();
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
    logAsync("Channel Id: " + currentChannelId);
  }

  private void handleMeasurementData(CloudEvent event) {
    CloudEventData cloudEventData = event.getData();
    //logAsync("[data.v2]CloudEventData: " + cloudEventData);
    logAsync("[data.v2]event: " + event);

    assert cloudEventData != null;
    String jsonData = cloudEventData.toString();
    String jsonStr = jsonData
      .replace("JsonCloudEventData{node=", "")
      .replace("}}", "}");

    JsonObject data = new JsonObject(jsonStr);
    JsonArray dataArray = data.getJsonArray("data");
    MeasurementList measurementList = new MeasurementList();
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < dataArray.size(); i++) {
      JsonObject item = dataArray.getJsonObject(i);
      JsonNode jsonNode = item.mapTo(JsonNode.class);
      measurementList.add(jsonNode);
      String uid = item.getString("uid");
      double value = item.getDouble("value");

      if (i > 0) {
        result.append("; ");
      }
      result.append(String.format("%s = %f", uid, value));
    }

    dependencyInjector.getUnitCollection().onDataReceived(measurementList);
    //TODO[IER]
    //logAsync("Result: " + result);
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
  }
}
