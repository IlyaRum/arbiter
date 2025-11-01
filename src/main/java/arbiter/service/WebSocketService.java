package arbiter.service;

import arbiter.config.AppConfig;
import arbiter.constants.CloudEventStrings;
import arbiter.data.MemoryData;
import arbiter.di.DependencyInjector;
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
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class WebSocketService extends ABaseService {
  private final WebSocketClient webSocketClient;
  private final AtomicReference<WebSocket> currentWebSocket = new AtomicReference<>();
  private final Map<String, MemoryData> store = new HashMap<>();
  private boolean pongReceived = false;
  private final DependencyInjector dependencyInjector;


  //TODO[IER] Пересмотреть обработчики событий для внешнего управления
  private Runnable closeHandler; // Изменено с Consumer<Void> на Runnable
  private Consumer<Throwable> exceptionHandler;


  private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

  public WebSocketService(Vertx vertx, DependencyInjector dependencyInjector) {
    super(vertx);
    this.dependencyInjector = dependencyInjector;
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
      dependencyInjector.getHandleDataService().handleTextMessage(promise)
        .handle(CloudEventStrings.MEASUREMENT_VALUES_DATA_V2_ONE_SECTION_PARAMETERS);
    } else {
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
    webSocket.textMessageHandler(dependencyInjector.getHandleDataService().handleTextMessage(promise));

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
