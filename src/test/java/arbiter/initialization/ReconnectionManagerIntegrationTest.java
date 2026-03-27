package arbiter.initialization;

import static org.junit.jupiter.api.Assertions.*;

import arbiter.di.DependencyInjector;
import arbiter.service.EventSubscriptionService;
import arbiter.service.WebSocketService;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconnectionManagerIntegrationTest {

  @Mock
  private DependencyInjector dependencyInjector;

  @Mock
  private WebSocketService webSocketService;

  @Mock
  private SubscriptionManager subscriptionManager;

  @Mock
  private EventSubscriptionService eventSubscriptionService;

  @Mock
  private Vertx vertx;

  private ReconnectionManager reconnectionManager;

  @BeforeEach
  void setUp() {
    reconnectionManager = new ReconnectionManager(dependencyInjector);
  }

  @Test
  void reconnect_successfulFlow_shouldReconnectAndRestoreSubscription() {
    String currentToken = "test-token";
    String channelId = "test-channel-123";
    String subscriptionId = "sub-456";

    JsonObject webSocketResponse = new JsonObject().put("subject", channelId);

    JsonObject subscriptionResponse = new JsonObject()
      .put("value", new JsonObject().put("subscriptionId", subscriptionId));

    JsonObject changeSubscriptionResponse = new JsonObject().put("status", "ok");

    JsonObject addEventResponse = new JsonObject().put("status", "success");

    when(webSocketService.connectToWebSocketServer(currentToken))
      .thenReturn(Future.succeededFuture(webSocketResponse));

    when(subscriptionManager.createSubscription(channelId, currentToken))
      .thenReturn(CompletableFuture.completedFuture(subscriptionResponse));

    when(subscriptionManager.changeSubscription(channelId, subscriptionId, currentToken))
      .thenReturn(CompletableFuture.completedFuture(changeSubscriptionResponse));

    when(eventSubscriptionService.addEventSubscription(any(DependencyInjector.class), any(String.class), any(JsonObject.class))).thenReturn(CompletableFuture.completedFuture(addEventResponse));
    when(dependencyInjector.getWebSocketService()).thenReturn(webSocketService);
    when(dependencyInjector.getSubscriptionManager()).thenReturn(subscriptionManager);
    when(dependencyInjector.getEventSubscriptionService()).thenReturn(eventSubscriptionService);

    CompletableFuture<JsonObject> resultFuture = reconnectionManager.reconnect(currentToken);
    JsonObject result = resultFuture.join();

    assertNotNull(result);
    assertEquals(channelId, result.getString("channelId"));

    verify(webSocketService, times(1)).connectToWebSocketServer(currentToken);
    verify(subscriptionManager, times(1)).createSubscription(channelId, currentToken);
    verify(subscriptionManager, times(1)).changeSubscription(channelId, subscriptionId, currentToken);
    verify(eventSubscriptionService, times(1)).addEventSubscription(
      eq(dependencyInjector), eq(currentToken), any(JsonObject.class));

    assertFalse(reconnectionManager.isReconnecting());
    assertEquals(0, reconnectionManager.getReconnectAttempts());
  }

  @Test
  void reconnect_whenAlreadyReconnecting_shouldFail() throws InterruptedException {
    String currentToken = "test-token";

    Promise<JsonObject> promise = Promise.promise();

    when(dependencyInjector.getWebSocketService()).thenReturn(webSocketService);

    when(webSocketService.connectToWebSocketServer(currentToken))
      .thenReturn(promise.future());

    CompletableFuture<JsonObject> firstAttempt = reconnectionManager.reconnect(currentToken);
    Thread.sleep(100);
    CompletableFuture<JsonObject> secondAttempt = reconnectionManager.reconnect(currentToken);

    assertTrue(secondAttempt.isCompletedExceptionally());
    assertTrue(reconnectionManager.isReconnecting());

    reconnectionManager.stopReconnecting();
    promise.fail(new RuntimeException("Переподключение остановлено"));
  }

  @Test
  void reconnect_whenTokenIsNull_shouldFail() {
    CompletableFuture<JsonObject> result = reconnectionManager.reconnect(null);

    assertTrue(result.isCompletedExceptionally());

    verify(webSocketService, never()).connectToWebSocketServer((RoutingContext) any());
    assertFalse(reconnectionManager.isReconnecting());
  }

  @Test
  void reconnect_whenWebSocketConnectionFails_shouldScheduleReconnect() {
    String currentToken = "test-token";
    RuntimeException connectionError = new RuntimeException("Connection refused");

    when(webSocketService.connectToWebSocketServer(currentToken))
      .thenReturn(Future.failedFuture(connectionError));

    when(vertx.setTimer(anyLong(), any())).thenReturn(123L);
    when(dependencyInjector.getWebSocketService()).thenReturn(webSocketService);
    when(dependencyInjector.getVertx()).thenReturn(vertx);
    CompletableFuture<JsonObject> result = reconnectionManager.reconnect(currentToken);

    assertTrue(result.isCompletedExceptionally());

    verify(vertx, times(1)).setTimer(eq(10000L), any());
    assertFalse(reconnectionManager.isReconnecting());
    assertEquals(1, reconnectionManager.getReconnectAttempts());

    reconnectionManager.stopReconnecting();
  }
}
