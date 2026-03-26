package arbiter.initialization;

import arbiter.di.DependencyInjector;
import arbiter.service.WebSocketService;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.concurrent.CompletableFuture;

public class WebSocketManager {
  private final DependencyInjector dependencyInjector;
  private final ReconnectionManager reconnectionManager;
  private String channelId;
  private String currentToken;

  private static final Logger logger = LoggerFactory.getLogger(WebSocketManager.class);

  public WebSocketManager(DependencyInjector dependencyInjector) {
    this.dependencyInjector = dependencyInjector;
    reconnectionManager = new ReconnectionManager(dependencyInjector);
  }

  public CompletableFuture<String> connectWebSocket(String token) {
    this.currentToken = token;
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
        //TODO[IER] Нужно реализовать и добавить метод для переподключения веб сокета,
        //если канал не открылся по какой-то причине
        //reconnectToWebSocketServer();
        throw new RuntimeException(throwable);
      });
  }

  /**
   * Принудительное переподключение
   */
  public CompletableFuture<JsonObject> forceReconnect(RoutingContext context) {
    logger.info("Принудительное переподключение");
    reconnectionManager.reconnectAttempts();

    WebSocketService webSocketService = dependencyInjector.getWebSocketService();
    webSocketService.closeConnection(); // Закрываем текущее соединение

    context.response()
      .setStatusCode(200)
      .putHeader("Content-Type", "application/json")
      .end("Принудительное переподключение");

    return reconnectionManager.reconnect1(currentToken);
  }

  public void forceReconnect(String errorMsg) {
    logger.error(errorMsg);
    reconnectionManager.reconnectAttempts();
    dependencyInjector.getWebSocketService().closeConnection();
    reconnectionManager.reconnect1(currentToken);
  }

  /**
   * Остановка всех попыток переподключения
   */
  public void stopReconnecting(RoutingContext context) {
    reconnectionManager.stopReconnecting();

    context.response()
      .setStatusCode(200)
      .putHeader("Content-Type", "application/json")
      .end("Переподключение остановлено");
  }

  /**
   * Проверка состояния соединения
   */
  public boolean isConnected() {
    return dependencyInjector.getWebSocketService().isConnected();
  }

  /**
   * Получение статистики переподключений
   */
  public void getReconnectionStats(RoutingContext context) {
    JsonObject stats = new JsonObject()
      .put("reconnectAttempts", reconnectionManager.getReconnectAttempts())
      .put("isReconnecting", reconnectionManager.isReconnecting())
      .put("isConnected", isConnected());

    context.response()
      .putHeader("content-type", "application/json")
      .end(stats.encode());
  }

  public String getChannelId() {
    return channelId;
  }
}
