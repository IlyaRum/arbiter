package arbiter.initialization;

import arbiter.di.DependencyInjector;
import arbiter.service.WebSocketService;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketManager {
  private final DependencyInjector dependencyInjector;
  private final Logger logger;
  private String channelId;
  private String currentToken;
  private long reconnectTimerId = -1;

  private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
  private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
  private final AtomicInteger maxReconnectAttempts = new AtomicInteger(100);
  private final AtomicInteger reconnectDelayMs = new AtomicInteger(10000);


  public WebSocketManager(DependencyInjector dependencyInjector, Logger logger) {
    this.dependencyInjector = dependencyInjector;
    this.logger = logger;
  }

  public CompletableFuture<String> connect(String token) {
    return dependencyInjector.getWebSocketService()
      .connectToWebSocketServer(token)
      .toCompletionStage()
      .toCompletableFuture()
      .thenApply(jsonObject -> {
        this.channelId = jsonObject.getString("subject");
        logger.info("[connect] channelId: " + channelId);
        return channelId;
      }).exceptionally(throwable -> {
        logger.error("Ошибка подключения: " + throwable.getMessage());
        scheduleReconnect();
        throw new RuntimeException(throwable);
      });
  }

  public CompletableFuture<String> reconnect() {
    if (isReconnecting.get() || reconnectAttempts.get() >= maxReconnectAttempts.get()) {
      logger.warn(String.format("Reconnection stopped: attempts=%d, max=%d, reconnecting=%s",
        reconnectAttempts.get(), maxReconnectAttempts.get(), isReconnecting.get()));
      return CompletableFuture.failedFuture(new RuntimeException("Превышено максимальное количество попыток переподключения"));
    }

    if (currentToken == null) {
      logger.error("Cannot reconnect: no token available");
      return CompletableFuture.failedFuture(new RuntimeException("Token not available"));
    }

    isReconnecting.set(true);
    int attempt = reconnectAttempts.incrementAndGet();

    logger.info(String.format("))) попытка переподключения к ОИК %d из %d", attempt, maxReconnectAttempts.get()));

    WebSocketService webSocketService = dependencyInjector.getWebSocketService();
    return webSocketService.connectToWebSocketServer(currentToken)
      .toCompletionStage()
      .toCompletableFuture()
      .thenApply(jsonObject -> {
        isReconnecting.set(false);
        this.channelId = jsonObject.getString("subject");
        reconnectAttempts.set(0);
        logger.info("[reconnect] channelId: " + channelId);
        return channelId;
      }).exceptionally(throwable -> {
        isReconnecting.set(false);
        logger.error(String.format("((( ошибка переподключения к ОИК %d из %d",
          attempt, maxReconnectAttempts.get()));

        if (attempt < maxReconnectAttempts.get()) {
          scheduleReconnect();
        } else {
          logger.error(String.format("Достигнуто максимальное количество попыток переподключения: %d",
            maxReconnectAttempts.get()));
        }
        throw new RuntimeException(throwable);
      });


//    WebSocketConnectOptions options = createWebSocketConnectOptions(currentToken);
//
//    webSocketClient
//      .connect(options)
//      .onComplete(res -> {
//        isReconnecting.set(false);
//
//        if (res.succeeded()) {
//          WebSocket webSocket = res.result();
//          currentWebSocket.set(webSocket);
//          reconnectAttempts.set(0); // Сбрасываем счетчик при успешном подключении
//
//          logger.info("))) успешное переподключение к ОИК");
//
//          // Настраиваем обработчики для нового соединения
//          setupWebSocketHandlers(webSocket, Promise.promise());
//
//        } else {
//          logger.error(String.format("((( ошибка переподключения к ОИК %d из %d", attempt, maxReconnectAttempts.get()));
//
//          // Планируем следующую попытку переподключения
//          if (attempt < maxReconnectAttempts.get()) {
//            scheduleReconnect();
//          } else {
//            logger.error(String.format("Достигнуто максимальное количество попыток переподключения: %d", maxReconnectAttempts.get()));
//            // Здесь можно уведомить о критической ошибке
//          }
//        }
//      });
  }

  /**
   * Установка обработчиков событий WebSocket
   */
  public void setupWebSocketHandlers() {
    WebSocketService webSocketService = dependencyInjector.getWebSocketService();

    // Устанавливаем обработчик закрытия соединения
    webSocketService.setCloseHandler(() -> {
      logger.info("WebSocket connection closed - scheduling reconnect");
      scheduleReconnect();
    });

    // Устанавливаем обработчик ошибок
    webSocketService.setExceptionHandler(throwable -> {
      logger.error("WebSocket error - scheduling reconnect: " + throwable.getMessage());
      scheduleReconnect();
    });
  }

  /**
   * Планирование переподключения с задержкой
   */
  private void scheduleReconnect() {
    if (reconnectTimerId != -1) {
      dependencyInjector.getVertx().cancelTimer(reconnectTimerId);
    }

    if (reconnectAttempts.get() >= maxReconnectAttempts.get()) {
      logger.error("Превышено максимальное количество попыток переподключения");
      return;
    }

    int delay = reconnectDelayMs.get();
    logger.info(String.format("Планируем переподключение через %d мс", delay));

    reconnectTimerId = dependencyInjector.getVertx().setTimer(delay, timerId -> {
      reconnectTimerId = -1;
      reconnect();
    });
  }

  /**
   * Принудительное переподключение
   */
  public CompletableFuture<String> forceReconnect(RoutingContext context) {
    logger.info("Принудительное переподключение");
    reconnectAttempts.set(0);

    WebSocketService webSocketService = dependencyInjector.getWebSocketService();
    webSocketService.closeConnection(); // Закрываем текущее соединение

    return reconnect();
  }

  /**
   * Остановка всех попыток переподключения
   */
  public void stopReconnecting(RoutingContext context) {
    if (reconnectTimerId != -1) {
      dependencyInjector.getVertx().cancelTimer(reconnectTimerId);
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
    WebSocketService webSocketService = dependencyInjector.getWebSocketService();
    return webSocketService.isConnected();
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

  public String getChannelId() {
    return channelId;
  }
}
