package arbiter.service;

import arbiter.data.StoreData;
import arbiter.data.dto.UnitDto;
import arbiter.di.DependencyInjector;
import arbiter.measurement.MeasurementDataProcessor;
import arbiter.measurement.MeasurementList;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonCloudEventData;
import io.cloudevents.jackson.JsonFormat;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class HandleDataServiceTest {

  @Mock
  private Vertx vertx;

  @Mock
  private DependencyInjector dependencyInjector;

  @Mock
  private MeasurementDataProcessor measurementDataProcessor;

  @Mock
  private ExecutorService executorMock;

  @Mock
  private CalculationServiceClient calculationServiceClient;

  @Captor
  private ArgumentCaptor<Runnable> runnableCaptor;

  @Captor
  private ArgumentCaptor<MeasurementList> measurementListCaptor;

  @Captor
  private ArgumentCaptor<String> stringCaptor;

  private HandleDataService handleDataService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();

    handleDataService = new HandleDataService(
      vertx,
      dependencyInjector,
      executorMock,
      calculationServiceClient
    );

    handleDataService.setMeasurementDataProcessor(measurementDataProcessor);
  }

  @Test
  void testHandleTextMessage_ChannelOpenedEvent(VertxTestContext testContext) {
    Promise<JsonObject> promise = Promise.promise();
    Handler<String> handler = handleDataService.handleTextMessage(promise);

    String eventId = UUID.randomUUID().toString();
    String channelId = "channel-123";

    CloudEvent event = CloudEventBuilder.v1()
      .withId(eventId)
      .withSource(URI.create("urn:source"))
      .withType("ru.monitel.ck11.channel.opened.v2")
      .withSubject(channelId)
      .withData("application/json", ("{\"channelId\": \"" + channelId + "\"}").getBytes())
      .withTime(OffsetDateTime.now())
      .build();

    byte[] eventBytes = Objects.requireNonNull(EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE))
      .serialize(event);
    String message = new String(eventBytes, StandardCharsets.UTF_8);

    handler.handle(message);

    promise.future().onComplete(testContext.succeeding(json -> {
      testContext.verify(() -> {
        assertNotNull(json);
        assertTrue(json.containsKey("id"));
        assertEquals(eventId, json.getString("id"));
        assertEquals(channelId, handleDataService.getCurrentChannelId());
      });
      testContext.completeNow();
    }));
  }

  @Test
  void testHandleTextMessage_MeasurementDataEvent() throws Exception {
    Promise<JsonObject> promise = Promise.promise();
    Handler<String> handler = handleDataService.handleTextMessage(promise);

    JsonObject data = new JsonObject()
      .put("data", new JsonArray()
        .add(new JsonObject().put("uid", "sensor-1").put("value", 25.5).put("timestamp", "2024-01-01T10:00:00Z").put("qCode", 1))
        .add(new JsonObject().put("uid", "sensor-2").put("value", 30.0).put("timestamp", "2024-01-01T10:00:00Z").put("qCode", 1)));

    CloudEvent event = CloudEventBuilder.v1()
      .withId(UUID.randomUUID().toString())
      .withSource(URI.create("urn:source"))
      .withType("ru.monitel.ck11.measurement-values.data.v2")
      .withData(JsonCloudEventData.wrap(objectMapper.readTree(data.toString())))
      .build();

    byte[] eventBytes = EventFormatProvider.getInstance()
      .resolveFormat(JsonFormat.CONTENT_TYPE)
      .serialize(event);
    String message = new String(eventBytes, StandardCharsets.UTF_8);

    doNothing().when(measurementDataProcessor).onDataReceived(any(MeasurementList.class));

    handler.handle(message);

    verify(measurementDataProcessor, times(1)).onDataReceived(measurementListCaptor.capture());
    MeasurementList capturedList = measurementListCaptor.getValue();

    assertNotNull(capturedList);
    assertEquals(2, capturedList.size());

    assertNull(promise.future().result());
  }

  @Test
  void testHandleTextMessage_InvalidJson() {
    Promise<JsonObject> promise = Promise.promise();
    Handler<String> handler = handleDataService.handleTextMessage(promise);
    String invalidMessage = "{invalid json";

    handler.handle(invalidMessage);

    assertTrue(promise.future().failed());
    promise.future().onFailure(error -> {
      assertNotNull(error);
      assertInstanceOf(Exception.class, error);
    });
  }

  @Test
  void testHandleTextMessage_RTEvents() {
    Promise<JsonObject> promise = Promise.promise();
    Handler<String> handler = handleDataService.handleTextMessage(promise);

    CloudEvent event = CloudEventBuilder.v1()
      .withId(UUID.randomUUID().toString())
      .withSource(URI.create("urn:source"))
      .withType("ru.monitel.ck11.rt-events.test")
      .withData("application/json", "{\"event\": \"test\"}".getBytes())
      .build();

    byte[] eventBytes = Objects.requireNonNull(EventFormatProvider.getInstance()
        .resolveFormat(JsonFormat.CONTENT_TYPE))
      .serialize(event);
    String message = new String(eventBytes, StandardCharsets.UTF_8);

    handler.handle(message);

    assertFalse(promise.future().failed());
    assertNull(promise.future().result());
  }

  @Test
  void testCloudEventToString() {
    CloudEvent event = CloudEventBuilder.v1()
      .withId("test-id")
      .withSource(URI.create("urn:test"))
      .withType("test.type")
      .build();

    String result = HandleDataService.cloudEventToString(event);

    assertNotNull(result);
    assertTrue(result.contains("test-id"));
    assertTrue(result.contains("test.type"));
  }

  @Test
  void testConvertStoreDataToJson() {
    Map<String, Object> testData = new HashMap<>();
    testData.put("key", "value");
    testData.put("number", 123);

    String result = invokeConvertStoreDataToJson(testData);

    assertNotNull(result);
    assertTrue(result.contains("key"));
    assertTrue(result.contains("value"));
    assertTrue(result.contains("123"));
  }

  @Test
  void testHandleProcessedData_WithNullUnitId_ShouldSendPostRequest() {
    StoreData storeData = mock(StoreData.class);
    when(storeData.size()).thenReturn(1);

    invokeHandleProcessedData(storeData, null);

    verify(executorMock, times(1)).submit(runnableCaptor.capture());

    Runnable capturedTask = runnableCaptor.getValue();
    capturedTask.run();

    verify(calculationServiceClient, times(1)).sendPostRequestAsync(stringCaptor.capture());
    verify(calculationServiceClient, never()).sendPutRequestAsync(anyString(), anyString());

    String capturedJson = stringCaptor.getValue();
    assertNotNull(capturedJson);
  }

  @Test
  void testHandleProcessedData_WithUnitId_ShouldSendPutRequest() {
    String unitId = "test-unit-123";
    StoreData storeData = mock(StoreData.class);
    UnitDto unitDto = mock(UnitDto.class);

    when(storeData.size()).thenReturn(1);
    when(storeData.getUnitDataList()).thenReturn(List.of(unitDto));

    invokeHandleProcessedData(storeData, unitId);

    verify(executorMock, times(1)).submit(runnableCaptor.capture());

    Runnable capturedTask = runnableCaptor.getValue();
    capturedTask.run();

    verify(calculationServiceClient, times(1)).sendPutRequestAsync(stringCaptor.capture(), eq(unitId));
    verify(calculationServiceClient, never()).sendPostRequestAsync(anyString());

    String capturedJson = stringCaptor.getValue();
    assertNotNull(capturedJson);
  }

  @Test
  void testHandleProcessedData_EmptyData_ShouldNotSendAnyRequest() {
    StoreData storeData = mock(StoreData.class);
    when(storeData.size()).thenReturn(0);

    invokeHandleProcessedData(storeData, "unit-123");
    invokeHandleProcessedData(storeData, null);

    verify(executorMock, never()).submit(any(Runnable.class));
    verify(calculationServiceClient, never()).sendPostRequestAsync(anyString());
    verify(calculationServiceClient, never()).sendPutRequestAsync(anyString(), anyString());
  }

  @Test
  void testHandleProcessedData_ExceptionInProcessing_ShouldNotBreak() {
    StoreData storeData = mock(StoreData.class);
    when(storeData.size()).thenReturn(1);

    HandleDataService realService = new HandleDataService(
      vertx,
      dependencyInjector,
      executorMock,
      calculationServiceClient
    );
    HandleDataService spyService = spy(realService);

    assertDoesNotThrow(() -> {
      java.lang.reflect.Method method = HandleDataService.class.getDeclaredMethod(
        "handleProcessedData", StoreData.class, String.class);
      method.setAccessible(true);
      method.invoke(spyService, storeData, "unit-123");
    });

    verify(executorMock, times(1)).submit(any(Runnable.class));
  }

  @Test
  void testHandleProcessedData_ExceptionInProcessing_ShouldLogErrorButNotBreak() {
    StoreData storeData = mock(StoreData.class);
    when(storeData.size()).thenReturn(1);

    doThrow(new RuntimeException("Test exception"))
      .when(calculationServiceClient).sendPostRequestAsync(anyString());

    invokeHandleProcessedData(storeData, null);

    verify(executorMock, times(1)).submit(runnableCaptor.capture());

    Runnable capturedTask = runnableCaptor.getValue();

    assertDoesNotThrow(() -> {
      capturedTask.run();
    });

    verify(calculationServiceClient, times(1)).sendPostRequestAsync(anyString());
  }

  @Test
  void testHandleProcessedData_MultipleCalls_ShouldMaintainOrder() {
    StoreData storeData1 = mock(StoreData.class);
    StoreData storeData2 = mock(StoreData.class);
    UnitDto unitDto = mock(UnitDto.class);

    when(storeData1.size()).thenReturn(1);
    when(storeData2.size()).thenReturn(1);
    when(storeData2.getUnitDataList()).thenReturn(List.of(unitDto));

    List<String> callOrder = new ArrayList<>();

    doAnswer(invocation -> {
      Runnable task = invocation.getArgument(0);
      callOrder.add("submit");
      task.run();
      return null;
    }).when(executorMock).submit(any(Runnable.class));

    doAnswer(invocation -> {
      callOrder.add("post");
      return null;
    }).when(calculationServiceClient).sendPostRequestAsync(anyString());

    doAnswer(invocation -> {
      callOrder.add("put:" + invocation.getArgument(1));
      return null;
    }).when(calculationServiceClient).sendPutRequestAsync(anyString(), anyString());

    invokeHandleProcessedData(storeData1, null);
    invokeHandleProcessedData(storeData2, "unit-123");

    verify(executorMock, times(2)).submit(any(Runnable.class));

    assertEquals(4, callOrder.size());
    assertEquals("submit", callOrder.get(0));
    assertEquals("post", callOrder.get(1));
    assertEquals("submit", callOrder.get(2));
    assertTrue(callOrder.get(3).startsWith("put:"));
  }

  @Test
  void testHandleTextMessage_StreamStartedEvent() {
    Promise<JsonObject> promise = Promise.promise();
    Handler<String> handler = handleDataService.handleTextMessage(promise);

    CloudEvent event = CloudEventBuilder.v1()
      .withId(UUID.randomUUID().toString())
      .withSource(URI.create("urn:source"))
      .withType("ru.monitel.ck11.events.stream-started.v2")
      .build();

    byte[] eventBytes = Objects.requireNonNull(EventFormatProvider.getInstance()
        .resolveFormat(JsonFormat.CONTENT_TYPE))
      .serialize(event);
    String message = new String(eventBytes, StandardCharsets.UTF_8);

    handler.handle(message);

    assertFalse(promise.future().failed());
    assertNull(promise.future().result());
  }

  @Test
  void testHandleTextMessage_StreamBrokenEvent() {
    Promise<JsonObject> promise = Promise.promise();
    Handler<String> handler = handleDataService.handleTextMessage(promise);

    CloudEvent event = CloudEventBuilder.v1()
      .withId(UUID.randomUUID().toString())
      .withSource(URI.create("urn:source"))
      .withType("ru.monitel.ck11.events.stream-broken.v2")
      .build();

    byte[] eventBytes = Objects.requireNonNull(EventFormatProvider.getInstance()
        .resolveFormat(JsonFormat.CONTENT_TYPE))
      .serialize(event);
    String message = new String(eventBytes, StandardCharsets.UTF_8);

    handler.handle(message);

    assertFalse(promise.future().failed());
    assertNull(promise.future().result());
  }

  @Test
  void testGettersAndSetters() {
    CalculationServiceClient newClient = mock(CalculationServiceClient.class);
    MeasurementDataProcessor newProcessor = mock(MeasurementDataProcessor.class);

    handleDataService.setCalculationClient(newClient);
    handleDataService.setMeasurementDataProcessor(newProcessor);

    assertEquals(newClient, handleDataService.getCalculationClient());
  }

  private void invokeHandleProcessedData(StoreData data, String unitId) {
    try {
      java.lang.reflect.Method method = HandleDataService.class.getDeclaredMethod(
        "handleProcessedData", StoreData.class, String.class);
      method.setAccessible(true);
      method.invoke(handleDataService, data, unitId);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke handleProcessedData", e);
    }
  }

  private String invokeConvertStoreDataToJson(Object objects) {
    try {
      java.lang.reflect.Method method = HandleDataService.class.getDeclaredMethod(
        "convertStoreDataToJson", Object.class);
      method.setAccessible(true);
      return (String) method.invoke(handleDataService, objects);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke convertStoreDataToJson", e);
    }
  }
}
