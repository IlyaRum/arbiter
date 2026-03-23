package arbiter.service;

import arbiter.config.AppConfig;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public class PingPongService {

  private static final Logger logger = LoggerFactory.getLogger(PingPongService.class);

  private final Vertx vertx;
  private PingPongHandler pingPongHandler;

  // Поля для ping-pong логики
  private volatile boolean pongReceived = true;
  //private final AtomicBoolean pongReceived = new AtomicBoolean(true);
  private static final int PONG_TIMEOUT_SECONDS = 30; // Таймаут ожидания pong
  private volatile Instant lastPingTime;
  private Long pingTimerId = null;
  private Long pongTimeoutTimerId = null;
  private static final int DEFAULT_PING_INTERVAL_SECONDS = 30;

  // Настройки ping-pong из конфигурации
  private int pingIntervalSeconds = DEFAULT_PING_INTERVAL_SECONDS;

  public PingPongService(Vertx vertx) {
    this.vertx = vertx;
  }

  public interface PingPongHandler {
    void onPongTimeout(String errorMsg);
  }

  public PingPongService(Vertx vertx, PingPongHandler pingPongHandler) {
    this.vertx = vertx;
    this.pingPongHandler = pingPongHandler;
  }

  /**
   * Загрузка настроек ping-pong из конфигурации
   */
  public void loadPingPongConfig() {
    try {
      String pingIntervalStr = AppConfig.getPingInterval();
      if (pingIntervalStr != null && !pingIntervalStr.isEmpty()) {
        pingIntervalSeconds = Integer.parseInt(pingIntervalStr);
        logger.info("Ping-pong interval set to '" + pingIntervalSeconds + "' seconds");
      }
    } catch (Exception e) {
      logger.warn("Failed to load ping interval from config, using default: '" + DEFAULT_PING_INTERVAL_SECONDS + "' seconds");
    }
  }

  /**
   * Отправка ping и проверка предыдущего pong
   */
  private void sendPing(WebSocket webSocket) {
    try {
      Instant now = Instant.now();
      long secondsSinceLastPing = java.time.Duration.between(lastPingTime, now).getSeconds();

      if (secondsSinceLastPing >= pingIntervalSeconds) {
        lastPingTime = now;

        if (!pongReceived) {
          logger.warn("PONG not received within '" + PONG_TIMEOUT_SECONDS + "' seconds, closing connection");
          //closeConnectionWithError("PONG timeout - no response from server");
          if (pingPongHandler != null) {
            pingPongHandler.onPongTimeout("PONG timeout - no response from server");
          }
        }
        else {
          logger.info("Sending PING to server");
          webSocket.writeFrame(WebSocketFrame.pingFrame(io.vertx.core.buffer.Buffer.buffer("ping")));
          pongReceived = false;

          startPongTimeoutTimer();
        }
      }
    } catch (Exception e) {
      logger.error("Error in ping logic", e);
      //closeConnectionWithError("Ping error: " + e.getMessage());
      if (pingPongHandler != null) {
        pingPongHandler.onPongTimeout("Ping error: " + e.getMessage());
      }
    }
  }

  private void startPongTimeoutTimer() {
    cancelPongTimeoutTimer();

    pongTimeoutTimerId = vertx.setTimer(PONG_TIMEOUT_SECONDS * 1000, timeoutId -> {
      if (!pongReceived) {
        logger.error("PONG not received after ping timeout");
        //closeConnectionWithError("PONG timeout - no response from server");
        if (pingPongHandler != null) {
          pingPongHandler.onPongTimeout("PONG timeout - no response from server");
        }
      }
      pongTimeoutTimerId = null;
    });
  }

  /**
   * Запуск периодической отправки ping
   */
  public void startPingPongTimer(WebSocket webSocket) {
    if (webSocket == null) {
      throw new IllegalArgumentException("WebSocket cannot be null");
    }

    stopPingPongTimer();

    this.lastPingTime = Instant.now();

    webSocket.pongHandler(pong -> {
      pongReceived = true;
      logger.info("PONG received");
      cancelPongTimeoutTimer();
    });

    pingTimerId = vertx.setPeriodic(pingIntervalSeconds * 1000, timerId -> {
      sendPing(webSocket);
    });
    logger.debug("Ping-pong timer started with interval: '" + pingIntervalSeconds + "' seconds");
  }

  /**
   * Остановка ping-pong таймера
   */
  public void stopPingPongTimer() {
    if (pingTimerId != null) {
      vertx.cancelTimer(pingTimerId);
      pingTimerId = null;
      logger.info("Ping-pong timer stopped");
    }
  }

  public void reset(){
    pongReceived = true;
    lastPingTime = Instant.now();
  }

  public void cancelPongTimeoutTimer() {
    if (pongTimeoutTimerId != null) {
      vertx.cancelTimer(pongTimeoutTimerId);
      pongTimeoutTimerId = null;
    }
  }

  public void setPingPongHandler(PingPongHandler pingPongHandler) {
    this.pingPongHandler = pingPongHandler;
  }
}
