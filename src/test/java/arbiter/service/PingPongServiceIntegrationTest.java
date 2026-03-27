package arbiter.service;

import arbiter.config.AppConfig;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(VertxExtension.class)
@Disabled
class PingPongServiceIntegrationTest {

  private Vertx vertx;
  private HttpServer server;
  private WebSocket clientWebSocket;
  private PingPongService pingPongService;
  private TestPingPongHandler testHandler;
  private MockedStatic<AppConfig> appConfigMock;
  private AtomicLong serverReceivedPingId;
  private AtomicInteger serverReceivedPingCount;
  private AtomicInteger serverSentPongCount;
  private AtomicInteger clientReceivedPongCount;
  private AtomicLong clientLastReceivedPongId;
  private AtomicBoolean serverSentPong;

  private static final int TEST_PORT = 8081;
  private AtomicBoolean shouldSendEmptyPong = new AtomicBoolean(false);


  @BeforeEach
  void setUp(Vertx vertx, VertxTestContext testContext) {
    this.vertx = vertx;
    this.testHandler = new TestPingPongHandler(testContext);
    this.pingPongService = new PingPongService(vertx, testHandler);

    this.serverSentPong = new AtomicBoolean(false);
    this.serverReceivedPingId = new AtomicLong(0);
    this.serverReceivedPingCount = new AtomicInteger(0);
    this.serverSentPongCount = new AtomicInteger(0);

    this.clientReceivedPongCount = new AtomicInteger(0);
    this.clientLastReceivedPongId = new AtomicLong();

    appConfigMock = mockStatic(AppConfig.class);
    appConfigMock.when(AppConfig::isEnablePing).thenReturn(true);
    appConfigMock.when(AppConfig::getPingInterval).thenReturn("2");
    appConfigMock.when(AppConfig::getPongInterval).thenReturn("5");

    pingPongService.loadPingConfig();
    pingPongService.loadPongConfig();

    HttpServerOptions serverOptions = new HttpServerOptions()
      .setPort(TEST_PORT)
      .setHost("localhost");

    server = vertx.createHttpServer(serverOptions);

    server.webSocketHandler(serverWebSocket -> {
      serverWebSocket.frameHandler(frame -> {
        if (frame.isPing()) {
          serverReceivedPingCount.incrementAndGet();
          if (shouldSendEmptyPong.get()) {
            Buffer emptyPong = Buffer.buffer();
            serverWebSocket.writeFrame(WebSocketFrame.pongFrame(emptyPong));
            System.out.println("Server: Sent PONG without data.");
          } else {
            Buffer pingData = frame.binaryData();
            if (pingData != null && pingData.length() >= 8) {
              long pingId = pingData.getLong(0);
              serverReceivedPingId.set(pingId);
              serverReceivedPingCount.incrementAndGet();
              Buffer pongBuffer = Buffer.buffer().appendLong(pingId);
              serverWebSocket.writeFrame(WebSocketFrame.pongFrame(pongBuffer));
              System.out.println("Server: Sent PONG with id: " + pingId);
            }
          }
          serverSentPong.set(true);
          serverSentPongCount.incrementAndGet();
        }
      });
      serverWebSocket.closeHandler(v -> {
        System.out.println("Server: WebSocket closed");
      });
    });
    server.listen()
      .onSuccess(v -> testContext.completeNow())
      .onFailure(testContext::failNow);
  }

  @AfterEach
  void tearDown(VertxTestContext testContext) {
    if (appConfigMock != null) {
      appConfigMock.close();
    }

    // Stop ping service
    if (pingPongService != null) {
      pingPongService.stop();
    }

    // Close client
    if (clientWebSocket != null && !clientWebSocket.isClosed()) {
      clientWebSocket.close();
    }

    // Close server
    if (server != null) {
      server.close()
        .onComplete(v -> testContext.completeNow())
        .onFailure(testContext::failNow);
    } else {
      testContext.completeNow();
    }
  }

  @Test
  void testCompletePingPongFlow(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
    WebSocketConnectOptions options = new WebSocketConnectOptions()
      .setPort(TEST_PORT)
      .setHost("localhost")
      .setURI("/");

    vertx.createWebSocketClient()
      .connect(options)
      .onSuccess(webSocket -> {
        clientWebSocket = webSocket;
        pingPongService.startPingTimer(clientWebSocket);

        vertx.setTimer(5000, timerId -> {
          testContext.verify(() ->
            assertTrue(serverReceivedPingCount.get() >= 1,
              "Server should receive at least 1 PING")
          );
          testContext.completeNow();
        });
      })
      .onFailure(testContext::failNow);

    assertTrue(testContext.awaitCompletion(10, TimeUnit.SECONDS));
  }

  @Test
  void testHandlePongWithoutData_Simplified(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
    shouldSendEmptyPong.set(true);

    WebSocketConnectOptions options = new WebSocketConnectOptions()
      .setPort(TEST_PORT)
      .setHost("localhost")
      .setURI("/");

    vertx.createWebSocketClient()
      .connect(options)
      .onSuccess(webSocket -> {
        clientWebSocket = webSocket;

        serverReceivedPingCount.set(0);
        serverSentPongCount.set(0);

        webSocket.frameHandler(frame -> {
          if (frame.type() == WebSocketFrameType.PONG) {
            Buffer pongData = frame.binaryData();
            if (pongData != null && pongData.length() > 0) {
              System.out.println("Client: Received PONG with data, length: " + pongData.length());
              clientReceivedPongCount.incrementAndGet();
              if (pongData.length() >= 8) {
                clientLastReceivedPongId.set(pongData.getLong(0));
                System.out.println("Client: PONG id: " + pongData.getLong(0));
              }
            } else {
              System.out.println("Client: Received PONG without data");
              clientReceivedPongCount.incrementAndGet();
            }
          }
        });

        // Запускаем ping сервис
        pingPongService.startPingTimer(clientWebSocket);
//        vertx.setTimer(1000, timerId -> {
//          // Проверяем, что сервер получил PING
//          if (serverReceivedPingCount.get() >= 1) {
//            System.out.println("PING received by server, pongReceived should be false now");
//            // Теперь pongReceived = false, и сервер отправит PONG без данных
//            // PONG должен прийти и попасть в нужную ветку
//          }
//        });

        // Ждем отправки PING и получения PONG без данных
        vertx.setTimer(5000, timerId -> {
          testContext.verify(() -> {
            System.out.println("📊 Server received PING count: " + serverReceivedPingCount.get());
            System.out.println("📊 Server sent PONG count: " + serverSentPongCount.get());
            System.out.println("📊 Client received PONG count: " + clientReceivedPongCount.get());
            System.out.println("📊 Test handler error: " + testHandler.getErrorMessage());
            // Проверяем, что сервер получил хотя бы один PING
            assertTrue(serverReceivedPingCount.get() >= 1,
              "Server should receive at least 1 PING. Actual: " + serverReceivedPingCount.get());

            // Проверяем, что клиент получил PONG (с данными или без)
            assertTrue(serverSentPongCount.get() >= 1,
              "Server should send PONG response. Actual: " + serverSentPongCount.get());

            assertTrue(clientReceivedPongCount.get() >= 1,
              "Client should receive at least 1 PONG. Actual: " + clientReceivedPongCount.get());
            if (clientLastReceivedPongId.get() != 0) {
              System.out.println("⚠️ Warning: Received PONG with data, id: " + clientLastReceivedPongId.get());
            }

            // Проверяем, что ошибка таймаута не произошла
            assertNull(testHandler.getErrorMessage(),
              "No timeout error should occur");

          });
          testContext.completeNow();
          shouldSendEmptyPong.set(false);
        });
      })
      .onFailure(testContext::failNow);

    assertTrue(testContext.awaitCompletion(100, TimeUnit.SECONDS));
  }

  @Test
  void testHandlePongWithoutData(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
    shouldSendEmptyPong.set(true);

    WebSocketConnectOptions options = new WebSocketConnectOptions()
      .setPort(TEST_PORT)
      .setHost("localhost")
      .setURI("/");

    vertx.createWebSocketClient()
      .connect(options)
      .onSuccess(webSocket -> {
        clientWebSocket = webSocket;
        System.out.println("✅ Client: WebSocket connected successfully");

        // Добавляем обработчики для отладки
        webSocket.exceptionHandler(e -> {
          System.err.println("❌ Client WebSocket error: " + e.getMessage());
          testContext.failNow(e);
        });

        webSocket.closeHandler(v -> {
          System.out.println("🔌 Client: WebSocket closed");
        });

        // Сбрасываем счетчики
        serverReceivedPingCount.set(0);
        serverSentPongCount.set(0);

        // Запускаем ping сервис
        pingPongService.startPingTimer(clientWebSocket);
        System.out.println("🚀 Ping service started");

        // Ждем несколько циклов
        vertx.setTimer(7000, timerId -> {
          testContext.verify(() -> {
            System.out.println("📊 Server received PING count: " + serverReceivedPingCount.get());
            System.out.println("📊 Server sent PONG count: " + serverSentPongCount.get());
            System.out.println("📊 Test handler error: " + testHandler.getErrorMessage());

            assertTrue(serverReceivedPingCount.get() >= 2,
              "Server should receive at least 2 PINGs. Actual: " + serverReceivedPingCount.get());
            assertNull(testHandler.getErrorMessage(),
              "No timeout error should occur");
          });
          testContext.completeNow();
          shouldSendEmptyPong.set(false);
        });
      })
      .onFailure(cause -> {
        System.err.println("❌ Client connection failed: " + cause.getMessage());
        cause.printStackTrace();
        testContext.failNow(cause);
      });

    assertTrue(testContext.awaitCompletion(10, TimeUnit.SECONDS));
  }

  private static class TestPingPongHandler implements PingPongService.PingPongHandler {
    private final AtomicReference<String> errorMessage = new AtomicReference<>();
    private final VertxTestContext testContext;

    TestPingPongHandler(VertxTestContext testContext) {
      this.testContext = testContext;
    }

    @Override
    public void onPongTimeout(String errorMsg) {
      errorMessage.set(errorMsg);
      testContext.failNow(new AssertionError("Unexpected timeout: " + errorMsg));
    }

    public String getErrorMessage() {
      return errorMessage.get();
    }
  }


}
