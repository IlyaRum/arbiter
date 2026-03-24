package arbiter.service;

import arbiter.config.AppConfig;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(VertxExtension.class)
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
    appConfigMock.when(AppConfig::getPingInterval).thenReturn("2"); // 2 seconds for faster test
    appConfigMock.when(AppConfig::getPongInterval).thenReturn("5"); // 5 seconds timeout

    pingPongService.loadPingConfig();
    pingPongService.loadPongConfig();

    HttpServerOptions serverOptions = new HttpServerOptions()
      .setPort(TEST_PORT)
      .setHost("localhost");

    server = vertx.createHttpServer(serverOptions);

    server.webSocketHandler(serverWebSocket -> {
      serverWebSocket.frameHandler(frame -> {
        if (frame.isPing()) {
          Buffer pingData = frame.binaryData();
          if (pingData != null && pingData.length() >= 8) {
            long pingId = pingData.getLong(0);
            serverReceivedPingId.set(pingId);
            serverReceivedPingCount.incrementAndGet();

            Buffer pongBuffer = Buffer.buffer().appendLong(pingId);
            serverWebSocket.writeFrame(WebSocketFrame.pongFrame(pongBuffer));
            serverSentPong.set(true);
            serverSentPongCount.incrementAndGet();

            System.out.println("Server: Sent PONG with id: " + pingId);
          }
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
        System.out.println("Client: Connected to server");

        clientWebSocket.pongHandler(pongBuffer -> {
          if (pongBuffer != null && pongBuffer.length() >= 8) {
            long receivedPongId = pongBuffer.getLong(0);
            clientReceivedPongCount.incrementAndGet();
            clientLastReceivedPongId.set(receivedPongId);
            System.out.println("Client: Received PONG with id: " + receivedPongId);
          } else {
            System.out.println("Client: Received PONG without data");
            clientReceivedPongCount.incrementAndGet();
          }
        });

        pingPongService.startPingTimer(clientWebSocket);

        System.out.println("Client: PingPongService started");

        vertx.setTimer(5000, timerId -> {
          testContext.verify(() -> {
            assertTrue(serverReceivedPingCount.get() >= 1,"Server should receive at least 1 PING. Actual: " + serverReceivedPingCount.get());
            assertTrue(serverSentPong.get(),"Server should send PONG response");
            assertNull(testHandler.getErrorMessage(),"PingPongService should not report timeout. Error: " + testHandler.getErrorMessage());
          });
          testContext.completeNow();
        });
      })
      .onFailure(testContext::failNow);
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
