package arbiter.service;

import arbiter.config.AppConfig;
import arbiter.data.UnitCollection;
import arbiter.di.DependencyInjector;
import arbiter.initialization.WebSocketManager;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
public class WebSocketMessageTimeoutIntegrationTest {

  private Vertx vertx;
  private WebSocketService webSocketService;
  private TestWebSocketServer testServer;

  @Mock
  private DependencyInjector dependencyInjector;

  @Mock
  private WebSocketManager webSocketManager;

  @Mock
  private UnitCollection unitCollection;

  @Mock
  private HandleDataService handleDataService;

  private MockedStatic<AppConfig> mockedAppConfig;

  private static final String TEST_TOKEN = "test-token-123";
  private static final String TEST_CHANNEL_ID = "channel-123";
  private static final int MESSAGE_TIMEOUT_SECONDS = 2;
  private static final int CHANNEL_OPEN_TIMEOUT_SECONDS = 5;

  @BeforeEach
  public void setUp(Vertx vertx, VertxTestContext testContext) {
    this.vertx = vertx;
    MockitoAnnotations.openMocks(this);
    mockedAppConfig = Mockito.mockStatic(AppConfig.class);
    mockedAppConfig.when(AppConfig::isEnablePing).thenReturn(true);

    when(dependencyInjector.getVertx()).thenReturn(vertx);
    when(dependencyInjector.getWebSocketManager()).thenReturn(webSocketManager);
    when(dependencyInjector.getHandleDataService()).thenReturn(handleDataService);

    when(handleDataService.handleTextMessage(any(Promise.class)))
      .thenAnswer(invocation -> {
        Promise<JsonObject> promise = invocation.getArgument(0);
        return (Handler<String>) message -> {
          System.out.println("HandleDataService received: " + message);
          try {
            JsonObject json = new JsonObject(message);

            if ("channel.opened".equals(json.getString("type"))) {
              promise.complete(json);
            }
          } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            promise.tryFail(e);
          }
        };
      });

    when(unitCollection.getPingIntervalSeconds()).thenReturn(30);
    when(unitCollection.getPongTimeoutSeconds()).thenReturn(10);
    when(unitCollection.getWebsocketReadDataTimeout()).thenReturn(MESSAGE_TIMEOUT_SECONDS);
    when(unitCollection.getOpenChanelTimeout()).thenReturn(CHANNEL_OPEN_TIMEOUT_SECONDS);
    when(dependencyInjector.getUnitCollection()).thenReturn(unitCollection);

    webSocketService = new WebSocketService(vertx, dependencyInjector);
    webSocketService.setEnableSubprotocol(false);

    testServer = new TestWebSocketServer(vertx);

    testServer.start()
      .onComplete(ar -> {
        if (ar.succeeded()) {
          webSocketService.setWebSocketUrl("ws://localhost:" + testServer.getPort());
          testContext.completeNow();
        } else {
          testContext.failNow(ar.cause());
        }
      });
  }

  @AfterEach
  public void tearDown() {
    if (mockedAppConfig != null) {
      mockedAppConfig.close();
    }
    if (testServer != null) {
      testServer.stop();
    }
    if (webSocketService != null) {
      webSocketService.closeConnection();
    }
  }

  @Test
  public void testMessageTimeoutTriggersReconnection(VertxTestContext testContext) throws Exception {
    AtomicBoolean reconnectCalled = new AtomicBoolean(false);
    AtomicReference<String> reconnectErrorMessage = new AtomicReference<>();
    CountDownLatch connectionEstablished = new CountDownLatch(1);
    CountDownLatch serverStarted = new CountDownLatch(1);

    doAnswer(invocation -> {
      String errorMsg = invocation.getArgument(0);
      reconnectCalled.set(true);
      reconnectErrorMessage.set(errorMsg);
      System.out.println("Force reconnect called with: " + errorMsg);
      return null;
    }).when(webSocketManager).forceReconnect(anyString());

    testServer.start()
      .onComplete(ar -> {
      if (ar.succeeded()) {
        webSocketService.setWebSocketUrl("ws://localhost:" + testServer.getPort());
        serverStarted.countDown();

        webSocketService.connectToWebSocketServer(TEST_TOKEN)
          .onComplete(connectAr -> {
            if (connectAr.succeeded()) {
              JsonObject result = connectAr.result();
              String channelId = result.getString("subject");
              webSocketService.onChannelOpened(channelId);
              connectionEstablished.countDown();
            } else {
              testContext.failNow(connectAr.cause());
            }
          });
      } else {
        testContext.failNow(ar.cause());
      }
    });

    assertTrue(serverStarted.await(5, TimeUnit.SECONDS));
    assertTrue(connectionEstablished.await(5, TimeUnit.SECONDS));

    vertx.setTimer((MESSAGE_TIMEOUT_SECONDS * 1000) + 500, timerId -> {
      testContext.verify(() -> {
        assertTrue(reconnectCalled.get(), "Reconnect should be called after message timeout");
        assertNotNull(reconnectErrorMessage.get());
        assertTrue(reconnectErrorMessage.get().contains("Message timeout"),"Error message should contain 'Message timeout'");
        assertTrue(reconnectErrorMessage.get().contains(String.valueOf(MESSAGE_TIMEOUT_SECONDS)),"Error message should contain timeout value");
        testContext.completeNow();
      });
    });
  }

//  @Test
//  public void testResetMessageTimeoutPreventsTimeout(VertxTestContext testContext) throws Exception {
//    // Arrange
//    AtomicBoolean reconnectCalled = new AtomicBoolean(false);
//
//    doAnswer(invocation -> {
//      reconnectCalled.set(true);
//      return null;
//    }).when(webSocketManager).forceReconnect(anyString());
//
//    CountDownLatch connectionEstablished = new CountDownLatch(1);
//
//    // Act
//    webSocketService.connectToWebSocketServer(TEST_TOKEN)
//      .onComplete(ar -> {
//        if (ar.succeeded()) {
//          JsonObject result = ar.result();
//          String channelId = result.getString("subject");
//          webSocketService.onChannelOpened(channelId);
//          connectionEstablished.countDown();
//        }
//      });
//
//    assertTrue(connectionEstablished.await(5, TimeUnit.SECONDS));
//
//    // Регулярно сбрасываем таймаут (каждую секунду)
//    long timerId = vertx.setPeriodic(1000, periodicId -> {
//      webSocketService.resetMessageTimeout();
//      System.out.println("Message timeout reset at: " + System.currentTimeMillis());
//    });
//
//    // Assert - ждем время больше таймаута
//    vertx.setTimer((MESSAGE_TIMEOUT_SECONDS * 1000) + 1000, timerId2 -> {
//      testContext.verify(() -> {
//        assertFalse(reconnectCalled.get(), "Reconnect should not be called when message timeout is reset");
//        vertx.cancelTimer(timerId);
//        testContext.completeNow();
//      });
//    });
//  }
//
//  @Test
//  public void testChannelNotOpenedPreventsMessageTimeout(VertxTestContext testContext) throws Exception {
//    // Arrange
//    AtomicBoolean reconnectCalled = new AtomicBoolean(false);
//
//    doAnswer(invocation -> {
//      reconnectCalled.set(true);
//      return null;
//    }).when(webSocketManager).forceReconnect(anyString());
//
//    CountDownLatch connectionEstablished = new CountDownLatch(1);
//
//    // Act - подключаемся, НО НЕ вызываем onChannelOpened()
//    webSocketService.connectToWebSocketServer(TEST_TOKEN)
//      .onComplete(ar -> {
//        if (ar.succeeded()) {
//          // Не вызываем onChannelOpened() - канал считается не открытым
//          connectionEstablished.countDown();
//        } else {
//          testContext.failNow(ar.cause());
//        }
//      });
//
//    assertTrue(connectionEstablished.await(5, TimeUnit.SECONDS));
//
//    // Assert - ждем время больше таймаута сообщений
//    vertx.setTimer((MESSAGE_TIMEOUT_SECONDS * 1000) + 1000, timerId -> {
//      testContext.verify(() -> {
//        // Таймаут сообщений не должен сработать, так как канал не открыт
//        assertFalse(reconnectCalled.get(),
//          "Message timeout should not start when channel is not opened");
//        testContext.completeNow();
//      });
//    });
//  }
//
//  @Test
//  public void testMessageTimeoutStopsAfterConnectionClose(VertxTestContext testContext) throws Exception {
//    // Arrange
//    AtomicInteger reconnectCallCount = new AtomicInteger(0);
//
//    doAnswer(invocation -> {
//      reconnectCallCount.incrementAndGet();
//      return null;
//    }).when(webSocketManager).forceReconnect(anyString());
//
//    CountDownLatch connectionEstablished = new CountDownLatch(1);
//
//    // Act
//    webSocketService.connectToWebSocketServer(TEST_TOKEN)
//      .onComplete(ar -> {
//        if (ar.succeeded()) {
//          JsonObject result = ar.result();
//          String channelId = result.getString("subject");
//          webSocketService.onChannelOpened(channelId);
//          connectionEstablished.countDown();
//        }
//      });
//
//    assertTrue(connectionEstablished.await(5, TimeUnit.SECONDS));
//
//    // Закрываем соединение до срабатывания таймаута
//    vertx.setTimer(1000, timerId -> {
//      webSocketService.closeConnection();
//    });
//
//    // Assert - ждем время больше таймаута
//    vertx.setTimer((MESSAGE_TIMEOUT_SECONDS * 1000) + 1000, timerId -> {
//      testContext.verify(() -> {
//        assertEquals(0, reconnectCallCount.get(),
//          "Reconnect should not be called after connection is closed");
//        testContext.completeNow();
//      });
//    });
//  }
//
//  @Test
//  public void testMultipleMessagesResetTimeoutRepeatedly(VertxTestContext testContext) throws Exception {
//    // Arrange
//    AtomicBoolean reconnectCalled = new AtomicBoolean(false);
//
//    doAnswer(invocation -> {
//      reconnectCalled.set(true);
//      return null;
//    }).when(webSocketManager).forceReconnect(anyString());
//
//    CountDownLatch connectionEstablished = new CountDownLatch(1);
//    AtomicInteger messageCount = new AtomicInteger(0);
//
//    // Act
//    webSocketService.connectToWebSocketServer(TEST_TOKEN)
//      .onComplete(ar -> {
//        if (ar.succeeded()) {
//          JsonObject result = ar.result();
//          String channelId = result.getString("subject");
//          webSocketService.onChannelOpened(channelId);
//          connectionEstablished.countDown();
//
//          // Имитируем получение сообщений каждые 1.5 секунды
//          long periodicId = vertx.setPeriodic(1500, timerId -> {
//            if (messageCount.incrementAndGet() <= 3) {
//              webSocketService.resetMessageTimeout();
//              System.out.println("Message received and timeout reset: " + messageCount.get());
//            } else {
//              vertx.cancelTimer(timerId);
//            }
//          });
//        }
//      });
//
//    assertTrue(connectionEstablished.await(5, TimeUnit.SECONDS));
//
//    // Assert - ждем время, достаточное для срабатывания таймаута (4 секунды без сообщений после последнего)
//    vertx.setTimer(6000, timerId -> {
//      testContext.verify(() -> {
//        assertFalse(reconnectCalled.get(),
//          "Reconnect should not be called when messages are received regularly");
//        testContext.completeNow();
//      });
//    });
//  }
//
//  @Test
//  public void testMessageTimeoutTimerCancellation(VertxTestContext testContext) throws Exception {
//    // Arrange
//    CountDownLatch connectionEstablished = new CountDownLatch(1);
//
//    // Act
//    webSocketService.connectToWebSocketServer(TEST_TOKEN)
//      .onComplete(ar -> {
//        if (ar.succeeded()) {
//          JsonObject result = ar.result();
//          String channelId = result.getString("subject");
//          webSocketService.onChannelOpened(channelId);
//          connectionEstablished.countDown();
//        }
//      });
//
//    assertTrue(connectionEstablished.await(5, TimeUnit.SECONDS));
//
//    // Проверяем, что таймер запущен
//    assertTrue(webSocketService.isChannelOpened());
//
//    // Отменяем таймаут
//    webSocketService.cancelMessageTimeout();
//
//    // Ждем время больше таймаута
//    vertx.setTimer((MESSAGE_TIMEOUT_SECONDS * 1000) + 1000, timerId -> {
//      testContext.verify(() -> {
//        // Проверяем, что reconnect не был вызван (через мок)
//        verify(webSocketManager, never()).forceReconnect(anyString());
//        testContext.completeNow();
//      });
//    });
//  }
//
//  @Test
//  public void testConsecutiveMessageTimeouts(VertxTestContext testContext) throws Exception {
//    // Arrange
//    AtomicInteger reconnectCallCount = new AtomicInteger(0);
//
//    doAnswer(invocation -> {
//      int callNumber = reconnectCallCount.incrementAndGet();
//      System.out.println("Reconnect called " + callNumber + " time");
//      return null;
//    }).when(webSocketManager).forceReconnect(anyString());
//
//    CountDownLatch firstConnectionEstablished = new CountDownLatch(1);
//
//    // Act & Assert
//    webSocketService.connectToWebSocketServer(TEST_TOKEN)
//      .onComplete(ar -> {
//        if (ar.succeeded()) {
//          JsonObject result = ar.result();
//          String channelId = result.getString("subject");
//          webSocketService.onChannelOpened(channelId);
//          firstConnectionEstablished.countDown();
//        }
//      });
//
//    assertTrue(firstConnectionEstablished.await(5, TimeUnit.SECONDS));
//
//    // Ждем первый таймаут
//    vertx.setTimer((MESSAGE_TIMEOUT_SECONDS * 1000) + 500, timerId1 -> {
//      testContext.verify(() -> {
//        assertEquals(1, reconnectCallCount.get(), "First timeout should trigger reconnect");
//
//        // Имитируем переподключение и открытие нового канала
//        webSocketService.onChannelOpened(TEST_CHANNEL_ID + "-new");
//
//        // Ждем второй таймаут
//        vertx.setTimer((MESSAGE_TIMEOUT_SECONDS * 1000) + 500, timerId2 -> {
//          testContext.verify(() -> {
//            assertEquals(2, reconnectCallCount.get(), "Second timeout should trigger another reconnect");
//            testContext.completeNow();
//          });
//        });
//      });
//    });
//  }
//
//  @Test
//  @DisplayName("Тест для проверки последовательности таймеров")
//  public void testCompleteTimerSequence(VertxTestContext testContext) throws Exception {
//    // Arrange
//    AtomicInteger eventSequence = new AtomicInteger(0);
//    AtomicReference<String> lastEvent = new AtomicReference<>();
//
//    doAnswer(invocation -> {
//      int seq = eventSequence.incrementAndGet();
//      lastEvent.set("forceReconnect called: " + seq);
//      System.out.println(lastEvent.get());
//      return null;
//    }).when(webSocketManager).forceReconnect(anyString());
//
//    CountDownLatch connectionEstablished = new CountDownLatch(1);
//
//    // Act - подключаемся
//    webSocketService.connectToWebSocketServer(TEST_TOKEN)
//      .onComplete(ar -> {
//        if (ar.succeeded()) {
//          JsonObject result = ar.result();
//          String channelId = result.getString("subject");
//
//          // Шаг 1: Открываем канал
//          webSocketService.onChannelOpened(channelId);
//          eventSequence.incrementAndGet();
//          lastEvent.set("Channel opened");
//          connectionEstablished.countDown();
//        }
//      });
//
//    assertTrue(connectionEstablished.await(5, TimeUnit.SECONDS));
//
//    // Шаг 2: Ждем, пока запустится таймер сообщений
//    vertx.setTimer(500, timerId1 -> {
//      eventSequence.incrementAndGet();
//      lastEvent.set("Message timeout timer started");
//
//      // Шаг 3: Имитируем получение сообщения до таймаута
//      vertx.setTimer(1000, timerId2 -> {
//        webSocketService.resetMessageTimeout();
//        eventSequence.incrementAndGet();
//        lastEvent.set("Message received, timeout reset");
//      });
//
//      // Шаг 4: Ждем, когда таймаут не сработает из-за сброса
//      vertx.setTimer(MESSAGE_TIMEOUT_SECONDS * 1000 + 1000, timerId3 -> {
//        testContext.verify(() -> {
//          // Должен быть вызван только один раз (если был)
//          assertTrue(eventSequence.get() >= 3, "At least 3 events should occur");
//          testContext.completeNow();
//        });
//      });
//    });
//  }
//
//  @Test
//  @DisplayName("Тест для проверки состояния таймера через рефлексию")
//  public void testMessageTimeoutTimerState(VertxTestContext testContext) throws Exception {
//    // Arrange
//    CountDownLatch connectionEstablished = new CountDownLatch(1);
//
//    // Act
//    webSocketService.connectToWebSocketServer(TEST_TOKEN)
//      .onComplete(ar -> {
//        if (ar.succeeded()) {
//          JsonObject result = ar.result();
//          String channelId = result.getString("subject");
//          webSocketService.onChannelOpened(channelId);
//          connectionEstablished.countDown();
//        }
//      });
//
//    assertTrue(connectionEstablished.await(5, TimeUnit.SECONDS));
//
//    // Используем рефлексию для проверки состояния таймера
//    java.lang.reflect.Field timerField = WebSocketService.class.getDeclaredField("messageTimeoutTimerId");
//    timerField.setAccessible(true);
//    Long timerId = (Long) timerField.get(webSocketService);
//
//    assertNotNull(timerId, "Message timeout timer should be set after channel opened");
//    assertTrue(timerId > 0, "Timer ID should be positive");
//
//    // Проверяем, что таймер активен
//    vertx.setTimer(100, v -> {
//      boolean timerExists = vertx.cancelTimer(timerId);
//      testContext.verify(() -> {
//        assertTrue(timerExists, "Timer should be active and cancellable");
//        testContext.completeNow();
//      });
//    });
//  }

  /**
   * Тестовый WebSocket сервер с возможностью эмуляции различных сценариев
   */
    private static class TestWebSocketServer {
      private final Vertx vertx;
      private HttpServer server;
      private int port;

      public TestWebSocketServer(Vertx vertx) {
        this.vertx = vertx;
      }

      public io.vertx.core.Future<Void> start() {
        return vertx.createHttpServer()
          .webSocketHandler(webSocket -> {
            System.out.println("Test server: WebSocket connected");

            JsonObject channelOpened = new JsonObject()
              .put("type", "channel.opened")
              .put("subject", TEST_CHANNEL_ID);
            webSocket.writeTextMessage(channelOpened.encode());

            System.out.println("Test server: Sent : " + channelOpened.encode());

            webSocket.closeHandler(v -> {
              System.out.println("Test server: WebSocket closed");
            });

            webSocket.textMessageHandler(msg -> {
              System.out.println("Test server received: " + msg);
            });
          })
          .listen(443)
          .onSuccess(httpServer -> {
            server = httpServer;
            port = httpServer.actualPort();
            System.out.println("Test server started on port: " + port);
          })
          .onFailure(cause -> {
            System.err.println("Test server failed to start: " + cause.getMessage());
          })
          .mapEmpty();
      }


      public void stop() {
      if (server != null) {
        server.close();
      }
    }

    public int getPort() {
      return port;
    }
  }
}
