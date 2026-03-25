package arbiter.service;

import arbiter.config.AppConfig;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

public class PingPongService {

  private static final Logger logger = LoggerFactory.getLogger(PingPongService.class);

  private final Vertx vertx;
  private PingPongHandler pingPongHandler;

  private volatile boolean pongReceived = true;
  private Long pingTimerId = null;
  private Long pongTimeoutTimerId = null;
  private static final int DEFAULTS_PONG_TIMEOUT_SECONDS = 30;
  private static final int DEFAULT_PING_INTERVAL_SECONDS = 30;
  private int pingIntervalSeconds = DEFAULT_PING_INTERVAL_SECONDS;
  private int pongTimeoutSeconds = DEFAULTS_PONG_TIMEOUT_SECONDS;

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
   * Загрузка настроек ping из конфигурации
   */
  public void loadPingConfig() {
    try {
      String pingIntervalStr = AppConfig.getPingInterval();
      if (pingIntervalStr != null && !pingIntervalStr.isEmpty()) {
        pingIntervalSeconds = Integer.parseInt(pingIntervalStr);
        if(AppConfig.isEnablePing()) {
          logger.info("Ping interval set to '" + pingIntervalSeconds + "' seconds");
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to load ping interval from config, using default: '" + DEFAULT_PING_INTERVAL_SECONDS + "' seconds");
    }
  }

  /**
   * Загрузка настроек pong из конфигурации
   */
  public void loadPongConfig() {
    try {
      String pongIntervalStr = AppConfig.getPongInterval();
      if (pongIntervalStr != null && !pongIntervalStr.isEmpty()) {
        pongTimeoutSeconds = Integer.parseInt(pongIntervalStr);
        if(AppConfig.isEnablePing()) {
          logger.info("Waiting for pong within '" + pongTimeoutSeconds + "' seconds");
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to load pong timeout from config, using default: '" + DEFAULTS_PONG_TIMEOUT_SECONDS + "' seconds");
    }
  }

  /**
   * Отправка ping и проверка предыдущего pong
   */
  private void sendPing(WebSocket webSocket) {
    if (webSocket == null || webSocket.isClosed()) {
      logger.warn("WebSocket is closed, cannot send PING");
      return;
    }

    try {
      if (!pongReceived) {
        logger.warn("PONG not received within '" + pongTimeoutSeconds + "' seconds, closing connection");
        //closeConnectionWithError("PONG timeout - no response from server");
        if (pingPongHandler != null) {
          pingPongHandler.onPongTimeout("PONG timeout - no response from server");
        }
      } else {
        logger.info("Sending PING to server");
        webSocket.writeFrame(WebSocketFrame.pingFrame(io.vertx.core.buffer.Buffer.buffer("ping")));
        pongReceived = false;

        startPongTimeoutTimer();
      }
    } catch (Exception e) {
      logger.error("Error in ping logic", e);
      pongReceived = true;
      //closeConnectionWithError("Ping error: " + e.getMessage());
      if (pingPongHandler != null) {
        pingPongHandler.onPongTimeout("Ping error: " + e.getMessage());
      }
    }
  }

  private void startPongTimeoutTimer() {
    cancelPongTimeoutTimer();

    pongTimeoutTimerId = vertx.setTimer(pongTimeoutSeconds * 1000, timeoutId -> {
      if (!pongReceived) {
        logger.error("PONG not received after timeout '" + pongTimeoutSeconds + "' seconds ");
        pongReceived = true;
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
  public void startPingTimer(WebSocket webSocket) {
    if (webSocket == null) {
      throw new IllegalArgumentException("WebSocket cannot be null");
    }

    stop();

    webSocket.pongHandler(pong -> handlePong());

    pingTimerId = vertx.setPeriodic(pingIntervalSeconds * 1000, timerId -> {
      sendPing(webSocket);
    });
    logger.info("Ping timer started with interval: '" + pingIntervalSeconds + "' seconds");
  }

  private void handlePong() {
    if (pongReceived) {
      logger.debug("Received unsolicited PONG");
      return;
    }

    try {
      pongReceived = true;
      logger.info("PONG received");
      cancelPongTimeoutTimer();
    } catch (Exception e) {
      logger.error("Error handling PONG", e);
    }
  }

  public void stop() {
    boolean hadActiveTimers = pingTimerId != null || pongTimeoutTimerId != null;
    cancelPingTimer();
    cancelPongTimeoutTimer();
    pongReceived = true;
    if (hadActiveTimers) {
      logger.info("PingPongService stopped");
    }
  }

  /**
   * Отмена периодического ping таймера
   */
  public void cancelPingTimer() {
    if (pingTimerId != null) {
      vertx.cancelTimer(pingTimerId);
      pingTimerId = null;
      logger.info("Ping timer stopped");
    }
  }

  /**
   * Сброс состояния для переподключения
   */
  public void reset(){
    pongReceived = true;
    cancelPongTimeoutTimer();
    logger.debug("PingPongService reset");
  }

  /**
   * Отмена таймера ожидания pong
   */
  public void cancelPongTimeoutTimer() {
    if (pongTimeoutTimerId != null) {
      vertx.cancelTimer(pongTimeoutTimerId);
      pongTimeoutTimerId = null;
    }
  }
}
