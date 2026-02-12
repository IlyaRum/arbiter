package arbiter;

import arbiter.initialization.WebSocketManager;
import arbiter.service.HandleDataService;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

public class ShutdownManager {
  private static final Logger logger = LoggerFactory.getLogger(ShutdownManager.class);

  private HandleDataService handleDataService;
  private final WebSocketManager webSocketManager;

  public ShutdownManager(WebSocketManager webSocketManager,
                         HandleDataService handleDataService) {
    this.handleDataService = handleDataService;
    this.webSocketManager = webSocketManager;
  }

  public void shutdown() {
    logger.info("Завершение работы...");

    if (handleDataService != null) {
      try {
        handleDataService.shutdown();
        logger.info("CalculationServiceClient успешно остановлен");
      } catch (Exception e) {
        logger.error("Ошибка при остановке CalculationServiceClient", e);
      }
    }

//    if (webSocketManager != null) {
//      try {
//        webSocketManager.close();
//        logger.info("WebSocket соединение зарыто");
//      } catch (Exception e) {
//        logger.error("Ошибка закрытия соединения WebSocket", e);
//      }
//    }

    logger.info("Завершение работы завершено");
  }
}
