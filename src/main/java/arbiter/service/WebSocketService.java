package arbiter.service;

import arbiter.config.AppConfig;
import arbiter.constants.CloudEventStrings;
import arbiter.data.MemoryData;
import arbiter.di.DependencyInjector;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class WebSocketService extends ABaseService {
  private final WebSocketClient webSocketClient;
  private final AtomicReference<WebSocket> currentWebSocket = new AtomicReference<>();
  private final Map<String, MemoryData> store = new HashMap<>();
  private final DependencyInjector dependencyInjector;


  //TODO[IER] Пересмотреть обработчики событий для внешнего управления
  private Runnable closeHandler; // Изменено с Consumer<Void> на Runnable
  private Consumer<Throwable> exceptionHandler;

  // Поля для ping-pong логики
  private volatile boolean pongReceived = true;
  private volatile Instant lastPingTime;
  private Long pingTimerId = null;
  private Long pongTimeoutTimerId = null;
  private static final int DEFAULT_PING_INTERVAL_SECONDS = 30;
  private static final int PONG_TIMEOUT_SECONDS = 30; // Таймаут ожидания pong
  private Runnable reconnectHandler;
  // Настройки ping-pong из конфигурации
  private int pingIntervalSeconds = DEFAULT_PING_INTERVAL_SECONDS;


  private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

  public WebSocketService(Vertx vertx, DependencyInjector dependencyInjector) {
    super(vertx);
    this.dependencyInjector = dependencyInjector;

    loadPingPongConfig();



    this.webSocketClient = vertx.createWebSocketClient(new WebSocketClientOptions()
        .setSsl(false)
        .setTrustAll(true)
        .setVerifyHost(false)
        .setLogActivity(true)
        .setMaxFrameSize(1024 * 1024) //TODO[IER] Вынести в конфиг файл. Увеличение фрейма до 1MB(по умолчанию 64KB)
        .setMaxMessageSize(1024 * 1024)
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
      //TODO[IER] Для разработки. Удалить.
      dependencyInjector.getHandleDataService().handleTextMessage(promise)
        .handle(CloudEventStrings.MEASUREMENT_VALUES_DATA_V2);
    } else {
      WebSocketConnectOptions options = createWebSocketConnectOptions(token);

      webSocketClient
        .connect(options)
        .onComplete(res -> {
          if (res.succeeded()) {
            WebSocket webSocket = res.result();
            currentWebSocket.set(webSocket);
            setupWebSocketHandlers(webSocket, promise);

            if(AppConfig.isEnablePing()){
              startPingPongTimer(webSocket);
            }

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

  /**
   * Загрузка настроек ping-pong из конфигурации
   */
  private void loadPingPongConfig() {
    try {
      String pingIntervalStr = AppConfig.getPingInterval();
      if (pingIntervalStr != null && !pingIntervalStr.isEmpty()) {
        pingIntervalSeconds = Integer.parseInt(pingIntervalStr);
        logger.info("Ping-pong interval set to '" + pingIntervalSeconds + "' seconds");
      }
    } catch (Exception e) {
      logger.warn("Failed to load ping interval from config, using default: '" + DEFAULT_PING_INTERVAL_SECONDS + "' seconds");
    }
  }

  /**
   * Запуск периодической отправки ping
   */
  private void startPingPongTimer(WebSocket webSocket) {
    stopPingPongTimer();

    this.lastPingTime = Instant.now();

    pingTimerId = vertx.setPeriodic(pingIntervalSeconds * 1000, timerId -> {
      if (webSocket != null && !webSocket.isClosed()) {
        sendPing(webSocket);
      } else {
        stopPingPongTimer();
      }
    });
    logger.debug("Ping-pong timer started with interval: '" + pingIntervalSeconds + "' seconds");
  }

  /**
   * Остановка ping-pong таймера
   */
  private void stopPingPongTimer() {
    if (pingTimerId != null) {
      vertx.cancelTimer(pingTimerId);
      pingTimerId = null;
      logger.info("Ping-pong timer stopped");
    }
  }

  /**
   * Отправка ping и проверка предыдущего pong
   */
  private void sendPing(WebSocket webSocket) {
    try {
      Instant now = Instant.now();
      long secondsSinceLastPing = java.time.Duration.between(lastPingTime, now).getSeconds();

      if (secondsSinceLastPing >= pingIntervalSeconds) {
        lastPingTime = now;

        if (!pongReceived) {
          logger.warn("PONG not received within '" + PONG_TIMEOUT_SECONDS + "' seconds, closing connection");
          closeConnectionWithError("PONG timeout - no response from server");
        }
        else {
          logger.info("Sending PING to server");
          webSocket.writeFrame(WebSocketFrame.pingFrame(io.vertx.core.buffer.Buffer.buffer("ping")));
          pongReceived = false;

          cancelPongTimeoutTimer();

          pongTimeoutTimerId = vertx.setTimer(PONG_TIMEOUT_SECONDS * 1000, timeoutId -> {
            if (!pongReceived) {
              logger.error("PONG not received after ping timeout");
              closeConnectionWithError("PONG timeout - no response from server");
            }
            pongTimeoutTimerId = null;
          });
        }
      }
    } catch (Exception e) {
      logger.error("Error in ping logic", e);
      closeConnectionWithError("Ping error: " + e.getMessage());
    }
  }

  private void cancelPongTimeoutTimer() {
    if (pongTimeoutTimerId != null) {
      vertx.cancelTimer(pongTimeoutTimerId);
      pongTimeoutTimerId = null;
    }
  }

  private void closeConnectionWithError(String errorMsg) {
    logger.error(errorMsg);
    cancelPongTimeoutTimer();
    stopPingPongTimer();

    WebSocket webSocket = currentWebSocket.get();
    if (webSocket != null && !webSocket.isClosed()) {
      webSocket.close((short) 1011, errorMsg);
    }

    if (exceptionHandler != null) {
      exceptionHandler.accept(new RuntimeException(errorMsg));
    }

    if (reconnectHandler != null) {
      reconnectHandler.run();
    }
  }

  private void setupWebSocketHandlers(WebSocket webSocket, Promise<JsonObject> promise) {
    webSocket.textMessageHandler(dependencyInjector.getHandleDataService().handleTextMessage(promise));

    webSocket.closeHandler(v -> {
      logger.info("WebSocket connection closed");

      stopPingPongTimer();
      pongReceived = true;
      lastPingTime = Instant.now();

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
      logger.info("PONG received");
      cancelPongTimeoutTimer();
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

  private static Handler<Throwable> closeWebSocket(Promise<JsonObject> promise, WebSocket webSocket) {
    return error -> {
      logger.error("WebSocket ошибка: " + error.getMessage());
      if (!promise.future().isComplete()) {
        promise.tryFail("WebSocket ошибка: " + error.getMessage());
      }
      webSocket.close((short) 1011, "Server error");
    };
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
