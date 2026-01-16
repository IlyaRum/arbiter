package arbiter.service;

import arbiter.config.AppConfig;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Сервис для отправки HTTP-запросов к расчетному сервису
 */
public class CalculationServiceClient {

  private static final Logger logger = LoggerFactory.getLogger(CalculationServiceClient.class);

  private volatile boolean isShuttingDown = false;
  private final WebClient webClient;
  private final ExecutorService executor;

  public CalculationServiceClient(WebClient webClient, ExecutorService executor) {
    this.webClient = webClient;
    this.executor = executor;
  }

  /**
   * Отправка POST-запроса к расчетному сервису
   */
  private void sendPostRequest(String jsonData) {
    if (isShuttingDown) {
      logger.warn("Пропускаем отправку запроса, т.к. идет завершение работы");
      return;
    }

    logger.info("Отправляем POST запрос в арбитр расчетов с данными: " + jsonData);

    webClient.postAbs(AppConfig.getCalcSrvAbsoluteUrl())
      .putHeader("Content-Type", "application/json")
      .sendBuffer(Buffer.buffer(jsonData))
      .compose(response -> {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          logger.debug("Данные успешно отправлены. Ответ: " + response.bodyAsString());
          return Future.succeededFuture();
        } else {
          return Future.failedFuture("HTTP error: " + response.statusCode() + " - " + response.bodyAsString());
        }
      })
      .onSuccess(v -> logger.debug("POST запрос выполнен успешно"))
      .onFailure(err -> logger.error("Ошибка при отправке POST запроса: " + err.getMessage()));
  }

  /**
   * Отправка PUT-запроса к расчетному сервису
   */
  private void sendPutRequest(String jsonData, String unitId) {
    if (isShuttingDown) {
      logger.warn("Пропускаем отправку запроса, т.к. идет завершение работы");
      return;
    }

    logger.info(String.format("Отправляем PUT запрос для сечения '%s'", unitId));

    webClient.putAbs(AppConfig.getCalcSrvAbsoluteUrl())
      .putHeader("Content-Type", "application/json")
      .sendBuffer(Buffer.buffer(jsonData))
      .compose(response -> {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          logger.debug("Данные успешно обновлены. Ответ: " + response.bodyAsString());
          return Future.succeededFuture();
        } else {
          return Future.failedFuture("HTTP error: " + response.statusCode() + " - " + response.bodyAsString());
        }
      })
      .onSuccess(v -> logger.debug("PUT запрос выполнен успешно"))
      .onFailure(err -> logger.error("Ошибка при отправке PUT запроса: " + err.getMessage()));
  }

  public void sendPostRequestAsync(String jsonData) {
    executor.submit(() -> {
      try {
        if (isShuttingDown) return;
        sendPostRequest(jsonData);
      } catch (Exception e) {
        logger.error("Ошибка в sendPostRequestAsync: ", e);
      }
    });
  }

  public void sendPutRequestAsync(String jsonData, String unitId) {
    executor.submit(() -> {
      try {
        if (isShuttingDown) return;
        sendPutRequest(jsonData, unitId);
      } catch (Exception e) {
        logger.error("Ошибка в sendPutRequestAsync для unitId: " + unitId, e);
      }
    });
  }

  public void shutdown() {
    isShuttingDown = true;
    executor.shutdown();

    try {
      if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
        executor.shutdownNow();
        logger.warn("Executor service не завершился корректно");
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
