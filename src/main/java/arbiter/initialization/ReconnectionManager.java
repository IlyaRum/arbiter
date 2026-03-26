package arbiter.initialization;

import arbiter.config.AppConfig;
import arbiter.di.DependencyInjector;
import arbiter.service.WebSocketService;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ReconnectionManager {

  private static final Logger logger = LoggerFactory.getLogger(ReconnectionManager.class);

  private final DependencyInjector dependencyInjector;

  private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
  private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
  private final AtomicInteger maxReconnectAttempts = new AtomicInteger(100);
  private final AtomicInteger websocketReconnectInterval = new AtomicInteger(10);
  private long reconnectTimerId = -1;

  public ReconnectionManager(DependencyInjector dependencyInjector) {
    this.dependencyInjector = dependencyInjector;
  }

  /**
   * Выполняет переподключение с бесконечным числом попыток
   */
  public CompletableFuture<JsonObject> reconnect(String currentToken) {
      if (currentToken == null) {
          logger.error("Cannot reconnect: no token available");
          return CompletableFuture.failedFuture(new RuntimeException("Token not available"));
      }

      isReconnecting.set(true);
      int attempt = reconnectAttempts.incrementAndGet();

      logger.info(String.format("Попытка переподключения к ОИК %d", attempt));

      WebSocketService webSocketService = dependencyInjector.getWebSocketService();
      return webSocketService.connectToWebSocketServer(currentToken)
              .toCompletionStage()
              .toCompletableFuture()
              .thenApply(jsonObject -> {
                  isReconnecting.set(false);
                  reconnectAttempts.set(0);
                  String channelId = jsonObject.getString("subject");
                  logger.info("[reconnect] channelId: " + channelId);
                  return jsonObject;
              })
              .thenCompose(jsonObject -> {
                  String channelId = jsonObject.getString("subject");
                  logger.info("[reconnect] channelId: " + channelId);
                  return dependencyInjector.getSubscriptionManager()
                          .createSubscription(channelId, currentToken)
                          .thenApply(subscriptionResult -> {
                              JsonObject valueObject = subscriptionResult.getJsonObject("value");
                              String subscriptionId = valueObject.getString("subscriptionId");
                              logger.info("[reconnect] subscriptionId: " + subscriptionId);
                              return new JsonObject()
                                      .put("channelId", channelId)
                                      .put("subscriptionId", subscriptionId)
                                      .put("subscriptionResult", subscriptionResult);
                          });
              })
              .thenCompose(subscriptionResult -> {
                  String channelId = subscriptionResult.getString("channelId");
                  String subscriptionId = subscriptionResult.getString("subscriptionId");
                  return dependencyInjector.getSubscriptionManager()
                          .changeSubscription(channelId, subscriptionId, currentToken)
                          .thenApply(finalResult -> new JsonObject().put("channelId", channelId));
              })
              .thenCompose(subscriptionResult ->
                      dependencyInjector.getEventSubscriptionService()
                              .addEventSubscription(dependencyInjector, currentToken, subscriptionResult)
                              .thenApply(eventResult -> subscriptionResult))
              .exceptionally(throwable -> {
                  isReconnecting.set(false);
                  logger.error(String.format("Ошибка переподключения к ОИК (попытка %d): %s",
                          attempt, throwable.getMessage()));
                  scheduleReconnect(currentToken);
                  throw new RuntimeException(throwable);
              });
  }

    /**
     * Выполняет переподключение с определенным числом попыток
     */
    public CompletableFuture<JsonObject> reconnectWithAttempts(String currentToken) {
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

        logger.info(String.format("Попытка переподключения к ОИК %d из %d", attempt, maxReconnectAttempts.get()));

        WebSocketService webSocketService = dependencyInjector.getWebSocketService();
        return webSocketService.connectToWebSocketServer(currentToken)
                .toCompletionStage()
                .toCompletableFuture()
                .thenApply(jsonObject -> {
                    isReconnecting.set(false);
                    reconnectAttempts.set(0);
                    String channelId = jsonObject.getString("subject");
                    logger.info("[reconnect] channelId: " + channelId);
                    return jsonObject;
                })
                .thenCompose(jsonObject -> {
                    String channelId = jsonObject.getString("subject");
                    logger.info("[reconnect] channelId: " + channelId);
                    return dependencyInjector.getSubscriptionManager()
                            .createSubscription(channelId, currentToken)
                            .thenApply(subscriptionResult -> {
                                JsonObject valueObject = subscriptionResult.getJsonObject("value");
                                String subscriptionId = valueObject.getString("subscriptionId");
                                logger.info("[reconnect] subscriptionId: " + subscriptionId);
                                return new JsonObject()
                                        .put("channelId", channelId)
                                        .put("subscriptionId", subscriptionId)
                                        .put("subscriptionResult", subscriptionResult);
                            });
                })
                .thenCompose(subscriptionResult -> {
                    String channelId = subscriptionResult.getString("channelId");
                    String subscriptionId = subscriptionResult.getString("subscriptionId");
                    return dependencyInjector.getSubscriptionManager()
                            .changeSubscription(channelId, subscriptionId, currentToken)
                            .thenApply(finalResult -> new JsonObject().put("channelId", channelId));
                })
                .thenCompose(subscriptionResult ->
                        dependencyInjector.getEventSubscriptionService()
                                .addEventSubscription(dependencyInjector, currentToken, subscriptionResult)
                                .thenApply(eventResult -> subscriptionResult))
                .exceptionally(throwable -> {
                    isReconnecting.set(false);
                    logger.error(String.format("Ошибка переподключения к ОИК %d из %d", attempt, maxReconnectAttempts.get()));

                    if (attempt < maxReconnectAttempts.get()) {
                        scheduleReconnectWithAttempts(currentToken);
                    } else {
                        logger.error(String.format("Достигнуто максимальное количество попыток переподключения: %d",
                                maxReconnectAttempts.get()));
                    }
                    throw new RuntimeException(throwable);
                });
    }


  /**
   * Планирование переподключения с задержкой
   */
  public void scheduleReconnect(String currentToken) {
    if (reconnectTimerId != -1) {
      dependencyInjector.getVertx().cancelTimer(reconnectTimerId);
    }

    int delay = websocketReconnectInterval.get();
    logger.info(String.format("Планируем переподключение через %d секунд", delay));

    reconnectTimerId = dependencyInjector.getVertx().setTimer(delay * 1000L, timerId -> {
      reconnectTimerId = -1;
      reconnect(currentToken);
    });
  }

  /**
   * Планирование переподключения с задержкой
   */
  private void scheduleReconnectWithAttempts(String currentToken) {
    if (reconnectTimerId != -1) {
      dependencyInjector.getVertx().cancelTimer(reconnectTimerId);
    }

    if (reconnectAttempts.get() >= maxReconnectAttempts.get()) {
      logger.error("Превышено максимальное количество попыток переподключения");
      return;
    }

    int delay = websocketReconnectInterval.get();
    logger.info(String.format("Планируем переподключение через %d мс", delay));

    reconnectTimerId = dependencyInjector.getVertx().setTimer(delay, timerId -> {
      reconnectTimerId = -1;
      reconnectWithAttempts(currentToken);
    });
  }

  /**
   * Остановка всех попыток переподключения
   */
  public void stopReconnecting() {
    if (reconnectTimerId != -1) {
      dependencyInjector.getVertx().cancelTimer(reconnectTimerId);
      reconnectTimerId = -1;
    }
    isReconnecting.set(false);
    logger.info("Переподключение остановлено");
  }

  public void reconnectAttempts() {
    reconnectAttempts.set(0);
  }

  /**
   * Получение количества попыток переподключения
   */
  public int getReconnectAttempts() {
    return reconnectAttempts.get();
  }

    public int getWebsocketReconnectInterval() {
        return websocketReconnectInterval.get();
    }

    /**
   * Проверка, выполняется ли переподключение
   */
  public boolean isReconnecting() {
    return isReconnecting.get();
  }

    /**
     * Загрузка настройки задержки между попытками рекконекта из конфигурации
     */
    public void loadWebsocketReconnectIntervalConfig() {
        try {
            String reconnectIntervalStr = AppConfig.getWebsocketReconnectInterval();
            if (reconnectIntervalStr != null && !reconnectIntervalStr.isEmpty()) {
                websocketReconnectInterval.set(Integer.parseInt(reconnectIntervalStr));
                    logger.info("Websocket reconnect interval set to '" + websocketReconnectInterval + "' seconds");
            }
        } catch (Exception e) {
            logger.warn("Failed to load websocket reconnect interval from config, using default: '" + websocketReconnectInterval + "' seconds");
        }
    }

}
