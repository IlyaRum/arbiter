package arbiter.service;

import arbiter.config.AppConfig;
import arbiter.constants.CloudEventStrings;
import arbiter.di.DependencyInjector;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class WebSocketService extends ABaseService {
  private final WebSocketClient webSocketClient;
  private final AtomicReference<WebSocket> currentWebSocket = new AtomicReference<>();
  private final AtomicBoolean channelOpened = new AtomicBoolean(false);
  private final DependencyInjector dependencyInjector;
  private final PingPongService pingPongService;
  private Long messageTimeoutTimerId = null;
  private Long connectionTimeoutTimerId;
  private Long channelOpenTimeoutTimerId;
  private static final long DEFAULT_MESSAGE_TIMEOUT_SECONDS = 20;
  private static final long DEFAULT_CONNECTION_TIMEOUT_SECONDS = 30;
  private static final long DEFAULT_CHANNEL_OPEN_TIMEOUT_SECONDS = 20;
  private long messageTimeoutSeconds = DEFAULT_MESSAGE_TIMEOUT_SECONDS;
  private long channelOpenTimeoutSeconds = DEFAULT_CHANNEL_OPEN_TIMEOUT_SECONDS;


  //TODO[IER] Пересмотреть обработчики событий для внешнего управления
  private Runnable closeHandler; // Изменено с Consumer<Void> на Runnable
  private Consumer<Throwable> exceptionHandler;
  private Runnable reconnectHandler;
  private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);

  private String webSocketUrl = "wss://ia-oc-w-aiptst.cdu.so/api/public/core/v2.1/channels/open";
  private boolean enableSubprotocol = true;

  public WebSocketService(Vertx vertx, DependencyInjector dependencyInjector) {
    super(vertx);
    this.dependencyInjector = dependencyInjector;
    this.pingPongService = createPingPongService(vertx);
    pingPongService.loadPingConfig(dependencyInjector.getUnitCollection().getPingIntervalSeconds());
    pingPongService.loadPongConfig(dependencyInjector.getUnitCollection().getPongTimeoutSeconds());
    loadMessageTimeoutConfig(dependencyInjector.getUnitCollection().getWebsocketReadDataTimeout());
    loadChannelOpenTimeoutConfig(dependencyInjector.getUnitCollection().getOpenChanelTimeout());

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

  private PingPongService createPingPongService(Vertx vertx) {
    PingPongService.PingPongHandler handler = new PingPongService.PingPongHandler() {
      @Override
      public void onPongTimeout(String errorMsg) {
        pingPongService.cancelPongTimeoutTimer();
        pingPongService.cancelPingTimer();
        dependencyInjector.getWebSocketManager().forceReconnect(errorMsg);
      }
    };

    return  new PingPongService(vertx, handler);
  }

  public void setEnableSubprotocol(boolean enable) {
    this.enableSubprotocol = enable;
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
    channelOpened.set(false);
    cancelAllTimeouts();

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
            startChannelOpenTimeoutTimer(promise);

            if(AppConfig.isEnablePing()){
              pingPongService.startPingTimer(webSocket);
            }else {
              logger.warn("Ping is disabled");
            }

            connectionTimeoutTimerId = vertx.setTimer(DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000, timerId -> {
              if (!promise.future().isComplete()) {
                promise.tryFail("WebSocket connection timeout - no opening message received");
                cancelAllTimeouts();
              }
            });
          } else {
            handleConnectionError(res.cause(), promise, context, options);
          }
        });
    }
    return promise.future();
  }


//  private void closeConnectionWithError(String errorMsg) {
//    logger.error(errorMsg);
//    pingPongService.cancelPongTimeoutTimer();
//    pingPongService.cancelPingTimer();
//
//    WebSocket webSocket = currentWebSocket.get();
//    if (webSocket != null && !webSocket.isClosed()) {
//      webSocket.close((short) 1011, errorMsg);
//    }
//
//    if (exceptionHandler != null) {
//      exceptionHandler.accept(new RuntimeException(errorMsg));
//    }
//
//    if (reconnectHandler != null) {
//      reconnectHandler.run();
//    }
//  }

  private void setupWebSocketHandlers(WebSocket webSocket, Promise<JsonObject> promise) {
    webSocket.textMessageHandler(dependencyInjector.getHandleDataService().handleTextMessage(promise));

    webSocket.closeHandler(v -> {
      logger.info("WebSocket connection closed");
      logger.info("Checking connect after close: " + isConnected());

      pingPongService.stop();
      cancelAllTimeouts();

      if (!promise.future().isComplete()) {
        promise.tryFail("WebSocket connection closed unexpectedly");
      }
      if (closeHandler != null) {
        closeHandler.run();
      }
    });

    webSocket.exceptionHandler(error -> {
      logger.error("WebSocket ошибка: " + error.getMessage());
      pingPongService.stop();
      cancelAllTimeouts();

      if (!promise.future().isComplete()) {
        promise.tryFail("WebSocket ошибка: " + error.getMessage());
      }

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
      publishEvent("websocket.closed", "Websocket connection closed");
    }
  }

  private void publishEvent(String address, String str) {
    try {
      Vertx vertx = dependencyInjector.getVertx();
      if (vertx != null) {
        vertx.eventBus().publish(address, str);
        logger.debug("Опубликовано событие " + address + " : " + str);
      }
    } catch (Exception e) {
      logger.error("Ошибка при публикации события: " + address + " : " + e.getMessage());
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

  private WebSocketConnectOptions createWebSocketConnectOptions(String token) {
    WebSocketConnectOptions options = new WebSocketConnectOptions()
      .setAbsoluteURI(webSocketUrl)
      .setPort(443);

    if (enableSubprotocol) {
      options.addSubProtocol(AppConfig.CLOUDEVENTS_PROTOCOL);
    }

    return options
      .addHeader("authorization", "Bearer " + token);
  }

  public void setWebSocketUrl(String webSocketUrl) {
    this.webSocketUrl = webSocketUrl;
  }

  public void connectToWebSocketServer(RoutingContext context) {
    String token = context.get("authToken");
    connectToWebSocketServer(token, context);
  }

  public Future<JsonObject> connectToWebSocketServer(String token) {
    return connectToWebSocketServer(token, null);
  }

//  private static Handler<Throwable> closeWebSocket(Promise<JsonObject> promise, WebSocket webSocket) {
//    return error -> {
//      logger.error("WebSocket ошибка: " + error.getMessage());
//      if (!promise.future().isComplete()) {
//        promise.tryFail("WebSocket ошибка: " + error.getMessage());
//      }
//      webSocket.close((short) 1011, "Server error");
//    };
//  }

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

  /**
   * Запуск таймаута ожидания сообщений
   */
  private void startMessageTimeout() {
    cancelMessageTimeout();

    if (isChannelOpened()) {
      messageTimeoutTimerId = vertx.setTimer(messageTimeoutSeconds, timerId -> {
        cancelAllTimeouts();
        channelOpened.set(false);
        String errorMsg = "Message timeout: no data received for '" + messageTimeoutSeconds + "' seconds";
        dependencyInjector.getWebSocketManager().forceReconnect(errorMsg);
        messageTimeoutTimerId = null;
      });
    }
  }

  /**
   * Запуск таймаута на открытие канала (получение channel.opened)
   */
  private void startChannelOpenTimeoutTimer(Promise<JsonObject> promise) {
    cancelChannelOpenTimeout();

    channelOpenTimeoutTimerId = vertx.setTimer(channelOpenTimeoutSeconds * 1000, timerId -> {

      if (!channelOpened.get()) {
        String errorMsg = "Channel open timeout: no 'channel.opened' event received within " + channelOpenTimeoutSeconds + " seconds";
        if (!promise.future().isComplete()) {
          promise.tryFail(errorMsg);
        }
        cancelAllTimeouts();
        dependencyInjector.getWebSocketManager().forceReconnect(errorMsg);
      }
      channelOpenTimeoutTimerId = null;
    });
    logger.info("Channel open timer started with interval: '" + channelOpenTimeoutSeconds + "' seconds");
  }

  /**
   * Сброс таймаута при получении каждого сообщения из СК-11
   */
  public void resetMessageTimeout() {
    WebSocket webSocket = currentWebSocket.get();
    if (webSocket != null && !webSocket.isClosed() && channelOpened.get()) {
//      cancelMessageTimeout();
      startMessageTimeout();
    }
  }

  public void cancelMessageTimeout() {
    if (messageTimeoutTimerId != null) {
      vertx.cancelTimer(messageTimeoutTimerId);
      messageTimeoutTimerId = null;
    }
  }

  public void cancelAllTimeouts() {
    //cancelConnectionTimeout();
    cancelChannelOpenTimeout();
    cancelMessageTimeout();
  }

  public boolean isChannelOpened() {
    return channelOpened.get();
  }

  /**
   * Уведомление об успешном открытии канала
   */
  public void onChannelOpened(String channelId) {
    logger.info("Channel opened successfully: " + channelId);
    channelOpened.set(true);

    cancelChannelOpenTimeout();
    if(isConnected()){
      startMessageTimeout();
      logger.info("Message timeout timer started with interval: '" + messageTimeoutSeconds + "' seconds");
    }
  }

  private void cancelChannelOpenTimeout() {
    if (channelOpenTimeoutTimerId != null) {
      vertx.cancelTimer(channelOpenTimeoutTimerId);
      channelOpenTimeoutTimerId = null;
    }
  }

  private void cancelConnectionTimeout() {
    if (connectionTimeoutTimerId != null) {
      vertx.cancelTimer(connectionTimeoutTimerId);
      connectionTimeoutTimerId = null;
    }
  }

  private void loadMessageTimeoutConfig(Integer websocketReadDataTimeout){
    this.messageTimeoutSeconds = websocketReadDataTimeout;
    logger.info("Read data interval set to '" + messageTimeoutSeconds + "' seconds");
  }

  private void loadChannelOpenTimeoutConfig(Integer openChanelTimeout){
    this.channelOpenTimeoutSeconds = openChanelTimeout;
    logger.info("Open channel interval set to '" + channelOpenTimeoutSeconds + "' seconds");
  }
}
