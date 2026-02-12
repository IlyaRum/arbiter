package arbiter.service;

import arbiter.config.AppConfig;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Сервис для отправки HTTP-запросов к расчетному сервису
 */
public class CalculationServiceClient {

  private static final Logger logger = LoggerFactory.getLogger(CalculationServiceClient.class);

  private final WebClient webClient;
  private final ExecutorService executor;

  private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private final Object shutdownLock = new Object();

  public CalculationServiceClient(WebClient webClient, ExecutorService executor) {
    this.webClient = webClient;
    this.executor = executor;
  }

  /**
   * Отправка POST-запроса к расчетному сервису
   */
  private void sendPostRequest(String jsonData) {
    if (isShuttingDown.get() || isClosed.get()) {
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
      .onFailure(err -> {
        if (!isShuttingDown.get() && !isClosed.get()) {
          logger.error("Ошибка при отправке POST запроса: " + err.getMessage());
        } else {
          logger.debug("POST запрос прерван из-за завершения работы: " + err.getMessage());
        }
      });
  }

  /**
   * Отправка PUT-запроса к расчетному сервису
   */
  private void sendPutRequest(String jsonData, String unitId) {
    if (isShuttingDown()) {
      logger.warn("Пропускаем отправку запроса, т.к. идет завершение работы");
      return;
    }

    logger.info(String.format("Отправляем PUT запрос для сечения '%s' с данными %s", unitId, jsonData));

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
      .onFailure(err -> {
          if (!isShuttingDown.get() && !isClosed.get()) {
            logger.error("Ошибка при отправке PUT запроса: " + err.getMessage());
          } else {
            logger.debug("PUT запрос прерван из-за завершения работы: " + err.getMessage());
          }
        }
      );
  }

  public void sendPostRequestAsync(String jsonData) {
    if (isShuttingDown()) {
      logger.warn("Пропускаем отправку POST запроса, т.к. идет завершение работы");
      return;
    }

    executor.submit(() -> {
      try {
        sendPostRequest(jsonData);
      } catch (Exception e) {
        if (!isShuttingDown.get() && !isClosed.get()) {
          logger.error("Ошибка в sendPostRequestAsync: ", e);
        }
      }
    });
  }

  public void sendPutRequestAsync(String jsonData, String unitId) {
    if (isShuttingDown.get() || isClosed.get()) {
      logger.warn("Пропускаем отправку PUT запроса, т.к. идет завершение работы");
      return;
    }

    executor.submit(() -> {
      try {
        if (isShuttingDown()) return;
        sendPutRequest(jsonData, unitId);
      } catch (Exception e) {
        if (!isShuttingDown.get() && !isClosed.get()) {
          logger.error("Ошибка в sendPutRequestAsync для unitId: " + unitId, e);
        }
      }
    });
  }

  public void shutdown() {
    synchronized (shutdownLock) {
      if (isClosed.get()) {
        return;
      }

      isShuttingDown.set(true);
      logger.info("Начинаем завершение CalculationServiceClient");

      executor.shutdown();

      try {
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
          logger.warn("Executor service не завершился за 10 секунд, принудительно прерываем");
          executor.shutdownNow();

          if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            logger.error("Executor service не удалось завершить принудительно");
          }
        }
      } catch (InterruptedException e) {
        logger.warn("Прерывание при ожидании завершения executor");
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      } finally {
        isClosed.set(true);
        isShuttingDown.set(false);
        logger.info("CalculationServiceClient завершен");
      }
    }
  }

  public boolean isShuttingDown() {
    return isShuttingDown.get() || isClosed.get();
  }
}
