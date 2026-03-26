package arbiter.initialization;

import arbiter.di.DependencyInjector;
import arbiter.service.WebSocketService;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ReconnectionManager {

  private static final Logger logger = LoggerFactory.getLogger(ReconnectionManager.class);

  private final DependencyInjector dependencyInjector;

  private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
  private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
  private final AtomicInteger maxReconnectAttempts = new AtomicInteger(100);
  //TODO[IER] Вынести Задержку между попытками в configData.json
  private final AtomicInteger reconnectDelayMs = new AtomicInteger(10000);
  private long reconnectTimerId = -1;
  private String channelId;

  public ReconnectionManager(DependencyInjector dependencyInjector) {
    this.dependencyInjector = dependencyInjector;
  }

  /**
   * Выполняет переподключение с бесконечным числом попыток
   */
//  public CompletableFuture<JsonObject> reconnect(String currentToken) {
//    if (isReconnecting.get()) {
//      logger.warn("Reconnection already in progress");
//      return CompletableFuture.failedFuture(new RuntimeException("Переподключение уже выполняется"));
//    }
//
//    if (currentToken == null) {
//      logger.error("Cannot reconnect: no token available");
//      return CompletableFuture.failedFuture(new RuntimeException("Token not available"));
//    }
//
//    isReconnecting.set(true);
//    int attempt = reconnectAttempts.incrementAndGet();
//
//    logger.info(String.format("Попытка переподключения к ОИК %d", attempt));
//
//    WebSocketService webSocketService = dependencyInjector.getWebSocketService();
//
//    return webSocketService.connectToWebSocketServer(currentToken)
//      .toCompletionStage()
//      .toCompletableFuture()
//      .thenApply(jsonObject -> {
//        isReconnecting.set(false);
//        reconnectAttempts.set(0);
//        String channelId = jsonObject.getString("subject");
//        logger.info("[reconnect] channelId: " + channelId);
//        return jsonObject;
//      })
//      .thenCompose(jsonObject -> {
//        String channelId = jsonObject.getString("subject");
//        return dependencyInjector.getSubscriptionManager()
//          .createSubscription(channelId, currentToken)
//          .thenApply(subscriptionResult -> {
//            JsonObject valueObject = subscriptionResult.getJsonObject("value");
//            String subscriptionId = valueObject.getString("subscriptionId");
//            logger.info("[reconnect] subscriptionId: " + subscriptionId);
//            return new JsonObject()
//              .put("channelId", channelId)
//              .put("subscriptionId", subscriptionId)
//              .put("subscriptionResult", subscriptionResult);
//          });
//      })
//      .thenCompose(result -> {
//        String channelId = result.getString("channelId");
//        String subscriptionId = result.getString("subscriptionId");
//        return dependencyInjector.getSubscriptionManager()
//          .changeSubscription(channelId, subscriptionId, currentToken)
//          .thenApply(finalResult -> result);
//      })
//      .thenCompose(result -> {
//        String channelId = result.getString("channelId");
//        JsonObject subscriptionResult = result.getJsonObject("subscriptionResult");
//        return dependencyInjector.getEventSubscriptionService()
//          .addEventSubscription(dependencyInjector, currentToken, subscriptionResult)
//          .thenApply(eventResult -> new JsonObject().put("channelId", channelId));
//      })
//      .exceptionally(throwable -> {
//        isReconnecting.set(false);
//        logger.error(String.format("Ошибка переподключения к ОИК (попытка %d): %s",
//          attempt, throwable.getMessage()));
//        scheduleReconnect(currentToken);
//        throw new RuntimeException(throwable);
//      });
//  }

  public CompletableFuture<JsonObject> reconnect1(String currentToken) {
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
        channelId = jsonObject.getString("subject");
        logger.info("[reconnect] channelId: " + channelId);
        return channelId;
      })
      .thenCompose(channelId -> dependencyInjector.getSubscriptionManager().createSubscription(channelId, currentToken))
      .thenCompose(subscriptionResult -> {
        JsonObject valueObject = subscriptionResult.getJsonObject("value");
        String subscriptionId = valueObject.getString("subscriptionId");
        logger.info("[reconnect] subscriptionId: " + subscriptionId);
        return dependencyInjector.getSubscriptionManager()
          .changeSubscription(channelId, subscriptionId, currentToken)
          .thenApply(finalResult -> new JsonObject().put("channelId", channelId));
      })
      .thenCompose(subscriptionResult -> dependencyInjector.getEventSubscriptionService().addEventSubscription(dependencyInjector, currentToken, subscriptionResult)
        .thenApply(eventResult -> subscriptionResult))
      .exceptionally(throwable -> {
        isReconnecting.set(false);
        logger.error(String.format("Ошибка переподключения к ОИК %d из %d", attempt, maxReconnectAttempts.get()));

        if (attempt < maxReconnectAttempts.get()) {
          scheduleReconnect1(currentToken);
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
//  public void scheduleReconnect(String currentToken) {
//    if (reconnectTimerId != -1) {
//      dependencyInjector.getVertx().cancelTimer(reconnectTimerId);
//    }
//
//    int delay = reconnectDelayMs.get();
//    logger.info(String.format("Планируем переподключение через %d мс", delay));
//
//    reconnectTimerId = dependencyInjector.getVertx().setTimer(delay, timerId -> {
//      reconnectTimerId = -1;
//      reconnect(currentToken);
//    });
//  }

  /**
   * Планирование переподключения с задержкой
   */
  private void scheduleReconnect1(String currentToken) {
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
      reconnect1(currentToken);
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

  /**
   * Проверка, выполняется ли переподключение
   */
  public boolean isReconnecting() {
    return isReconnecting.get();
  }

}
