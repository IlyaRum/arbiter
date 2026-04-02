package arbiter;

import arbiter.service.CalculationServiceClient;
import arbiter.initialization.WebSocketManager;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

public class ShutdownManager {
  private static final Logger logger = LoggerFactory.getLogger(ShutdownManager.class);

  private final CalculationServiceClient calculationServiceClient;
  private final WebSocketManager webSocketManager;

  public ShutdownManager(CalculationServiceClient calculationServiceClient,
                         WebSocketManager webSocketManager) {
    this.calculationServiceClient = calculationServiceClient;
    this.webSocketManager = webSocketManager;
  }

  public void shutdown() {
    logger.info("Завершение работы...");

    if (calculationServiceClient != null) {
      try {
        calculationServiceClient.shutdown();
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

    logger.info("Работа завершена");
  }
}
