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
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
  private WebClient webClient;

    @Mock
  private HttpRequest<Buffer> httpRequest;

  @Mock
  private HttpResponse<Buffer> httpResponse;

  @InjectMocks
  private HandleDataService handleDataService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    handleDataService = spy(new HandleDataService(vertx, dependencyInjector));
    handleDataService.setMeasurementDataProcessor(measurementDataProcessor);
  }

  @Test
  void testHandleTextMessage_ChannelOpenedEvent(VertxTestContext testContext) {
    Promise<JsonObject> promise = Promise.promise();
    Handler<String> handler = handleDataService.handleTextMessage(promise);


    Promise<Object> executeBlockingPromise = Promise.promise();
    Future<Object> mockFuture = executeBlockingPromise.future();
    when(vertx.executeBlocking(any(), eq(false))).thenReturn(mockFuture);

    String eventId = UUID.randomUUID().toString();
    CloudEvent event = CloudEventBuilder.v1()
      .withId(eventId)
      .withSource(URI.create("urn:source"))
      .withType("ru.monitel.ck11.channel.opened.v2")
      .withSubject("channel-123")
      .withData("application/json", "{\"channelId\": \"channel-123\"}".getBytes())
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
        assertEquals("channel-123", handleDataService.getCurrentChannelId());
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

    byte[] eventBytes = io.cloudevents.core.provider.EventFormatProvider.getInstance()
      .resolveFormat(io.cloudevents.jackson.JsonFormat.CONTENT_TYPE)
      .serialize(event);
    String message = new String(eventBytes, StandardCharsets.UTF_8);

    doNothing().when(measurementDataProcessor).onDataReceived(any(MeasurementList.class));

    handler.handle(message);

    verify(measurementDataProcessor, times(1)).onDataReceived(any(MeasurementList.class));
    assertTrue(promise.future().result() == null);
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

    byte[] eventBytes = io.cloudevents.core.provider.EventFormatProvider.getInstance()
      .resolveFormat(io.cloudevents.jackson.JsonFormat.CONTENT_TYPE)
      .serialize(event);
    String message = new String(eventBytes, StandardCharsets.UTF_8);

    handler.handle(message);

    assertFalse(promise.future().failed());
  }

  @Test
  void testHandleMeasurementData() throws Exception {
    JsonObject data = new JsonObject()
      .put("data", new JsonArray()
        .add(new JsonObject().put("uid", "sensor-1").put("value", 25.5).put("timestamp", "2024-01-01T10:00:00Z").put("qCode", 1))
        .add(new JsonObject().put("uid", "sensor-2").put("value", 30.0).put("timestamp", "2024-01-01T10:00:00Z").put("qCode", 1)));

    CloudEvent event = CloudEventBuilder.v1()
      .withId(UUID.randomUUID().toString())
      .withSource(URI.create("urn:source"))
      .withType("ru.monitel.ck11.measurement-values.data.v2")
      .withData(JsonCloudEventData.wrap(objectMapper.readTree(String.valueOf(data))))
      .build();

    doNothing().when(measurementDataProcessor).onDataReceived(any(MeasurementList.class));

    invokeHandleMeasurementData(event);

    verify(measurementDataProcessor, times(1)).onDataReceived(any(MeasurementList.class));
  }

  @Test
  void testHandleChannelOpened() {
    Promise<Object> executeBlockingPromise = Promise.promise();
    Future<Object> mockFuture = executeBlockingPromise.future();
    when(vertx.executeBlocking(any(), eq(false))).thenReturn(mockFuture);

    CloudEvent event = CloudEventBuilder.v1()
      .withId(UUID.randomUUID().toString())
      .withSource(URI.create("urn:source"))
      .withType("ru.monitel.ck11.channel.opened.v2")
      .withSubject("test-channel-id")
      .build();

    invokeHandleChannelOpened(event);

    assertEquals("test-channel-id", handleDataService.getCurrentChannelId());
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

  @Disabled
  @Test
  void testSendPutRequest_Success(VertxTestContext testContext) {
    String jsonData = "{\"test\": \"data\"}";
    String unitId = "unit-123";

    //when(vertx.createHttpClient()).thenReturn((HttpClientAgent) mock(HttpClient.class));

    when(WebClient.create(vertx)).thenReturn(webClient);
    when(webClient.putAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.sendBuffer(any())).thenReturn(Future.succeededFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsString()).thenReturn("OK");

//    handleDataService.sendPutRequest(jsonData, unitId);
    invokeSendPutRequest(jsonData, unitId);

    vertx.setTimer(100, id -> testContext.completeNow());
  }

  @Disabled
  @Test
  void testSendPostRequest_Success(VertxTestContext testContext) {
    String jsonData = "{\"test\": \"data\"}";

    when(vertx.createHttpClient()).thenReturn((HttpClientAgent) mock(HttpClient.class));
    when(WebClient.create(vertx)).thenReturn(webClient);
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.sendBuffer(any())).thenReturn(Future.succeededFuture(httpResponse));
    when(httpResponse.statusCode()).thenReturn(201);
    when(httpResponse.bodyAsString()).thenReturn("Created");

//    handleDataService.sendPostRequest(jsonData);
    invokeSendPostRequest(jsonData);

    vertx.setTimer(100, id -> testContext.completeNow());
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
  void testHandleProcessedData_FirstTime() {
    handleDataService.setFirstTime(true);
    StoreData storeData = mock(StoreData.class);
    UnitDto unitDto = mock(UnitDto.class);

    when(storeData.size()).thenReturn(1);
    when(storeData.getUnitDataList()).thenReturn(List.of(unitDto));

    doNothing().when(handleDataService).sendPostRequestAsync(anyString());

    invokeHandleProcessedData(storeData, "unit-123");

    verify(handleDataService, times(1)).sendPostRequestAsync(anyString());
    verify(handleDataService, never()).sendPutRequestAsync(anyString(), anyString());
    assertFalse(handleDataService.isFirstTime());
  }

  @Test
  void testHandleProcessedData_NotFirstTime() {
    handleDataService.setFirstTime(false);
    StoreData storeData = mock(StoreData.class);
    UnitDto unitDto = mock(UnitDto.class);

    when(storeData.size()).thenReturn(1);
    when(storeData.getUnitDataList()).thenReturn(List.of(unitDto));

    doNothing().when(handleDataService).sendPutRequestAsync(anyString(), anyString());

    invokeHandleProcessedData(storeData, "unit-123");

    verify(handleDataService, times(1)).sendPutRequestAsync(anyString(), eq("unit-123"));
    verify(handleDataService, never()).sendPostRequestAsync(anyString());
  }

  @Test
  void testHandleProcessedData_EmptyData() {
    StoreData storeData = mock(StoreData.class);
    when(storeData.size()).thenReturn(0);

    invokeHandleProcessedData(storeData, "unit-123");

    verify(handleDataService, never()).sendPostRequestAsync(anyString());
    verify(handleDataService, never()).sendPutRequestAsync(anyString(), anyString());
  }

  public void invokeHandleMeasurementData(CloudEvent event) {
    try {
      java.lang.reflect.Method method = HandleDataService.class.getDeclaredMethod(
        "handleMeasurementData", CloudEvent.class);
      method.setAccessible(true);
      method.invoke(handleDataService, event);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void invokeHandleChannelOpened(CloudEvent event) {
    try {
      java.lang.reflect.Method method = HandleDataService.class.getDeclaredMethod(
        "handleChannelOpened", CloudEvent.class);
      method.setAccessible(true);
      method.invoke(handleDataService, event);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void invokeHandleProcessedData(StoreData data, String unitId) {
    try {
      java.lang.reflect.Method method = HandleDataService.class.getDeclaredMethod(
        "handleProcessedData", StoreData.class, String.class);
      method.setAccessible(true);
      method.invoke(handleDataService, data, unitId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String invokeConvertStoreDataToJson(Object objects) {
    try {
      java.lang.reflect.Method method = HandleDataService.class.getDeclaredMethod(
        "convertStoreDataToJson", Object.class);
      method.setAccessible(true);
      return (String) method.invoke(handleDataService, objects);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void invokeSendPostRequestAsync(String jsonData) {
    try {
      java.lang.reflect.Method method = HandleDataService.class.getDeclaredMethod(
        "sendPostRequestAsync", String.class);
      method.setAccessible(true);
      method.invoke(handleDataService, jsonData);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void invokeSendPostRequest(String jsonData) {
    try {
      java.lang.reflect.Method method = HandleDataService.class.getDeclaredMethod(
        "sendPostRequest", String.class);
      method.setAccessible(true);
      method.invoke(handleDataService, jsonData);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void invokeSendPutRequest(String jsonData, String unitId) {
    try {
      java.lang.reflect.Method method = HandleDataService.class.getDeclaredMethod(
        "sendPutRequest", String.class, String.class);
      method.setAccessible(true);
      method.invoke(handleDataService, jsonData, unitId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
