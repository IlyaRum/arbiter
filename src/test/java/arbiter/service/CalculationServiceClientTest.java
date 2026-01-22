package arbiter.service;

import arbiter.config.AppConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class CalculationServiceClientTest {

  @Mock
  private WebClient webClient;

  @InjectMocks
  private CalculationServiceClient calculationServiceClient;

  @Mock
  private ExecutorService executorService;

  @Mock
  private HttpRequest<Buffer> mockHttpRequest;

  @Mock
  private HttpResponse<Buffer> mockHttpResponse;

  private ObjectMapper objectMapper;

  private MockedStatic<AppConfig> mockedAppConfig;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    calculationServiceClient = spy(new CalculationServiceClient(webClient, executorService));
    mockedAppConfig = Mockito.mockStatic(AppConfig.class);
  }

  @AfterEach
  void tearDown() {
    if (mockedAppConfig != null) {
      mockedAppConfig.close();
    }
  }

  @Test
  void sendPostRequest_SuccessfulResponse_ShouldSendRequest() {
    String jsonData = "{\"test\": \"data\"}";
    String testUrl = "http://localhost:8080/api/calculate";

    when(AppConfig.getCalcSrvAbsoluteUrl()).thenReturn(testUrl);
    when(webClient.postAbs(testUrl)).thenReturn(mockHttpRequest);
    when(mockHttpRequest.putHeader(anyString(), anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.sendBuffer(any())).thenReturn(Future.succeededFuture(mockHttpResponse));
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.bodyAsString()).thenReturn("Success");

    invokeSendPostRequest( jsonData);

    verify(webClient).postAbs(testUrl);
    verify(mockHttpRequest).putHeader("Content-Type", "application/json");
    verify(mockHttpRequest).sendBuffer(Buffer.buffer(jsonData));
  }

  @Test
  void sendPostRequest_ClientError_ShouldLogError() throws Exception {
    String jsonData = "{\"test\": \"data\"}";
    String testUrl = "http://localhost:8080/api/calculate";
    RuntimeException exception = new RuntimeException("Connection failed");

    when(AppConfig.getCalcSrvAbsoluteUrl()).thenReturn(testUrl);
    when(webClient.postAbs(testUrl)).thenReturn(mockHttpRequest);
    when(mockHttpRequest.putHeader(anyString(), anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.sendBuffer(any())).thenReturn(Future.failedFuture(exception));

    invokeSendPostRequest( jsonData);

    verify(webClient).postAbs(testUrl);
    verify(mockHttpRequest).sendBuffer(any());
  }

  @Test
  void sendPostRequest_FailedResponseWithStatusCode400_ShouldLogError() throws Exception {
    String jsonData = "{\"test\": \"data\"}";
    String testUrl = "http://localhost:8080/api/calculate";
    when(AppConfig.getCalcSrvAbsoluteUrl()).thenReturn(testUrl);
    when(webClient.postAbs(testUrl)).thenReturn(mockHttpRequest);
    when(mockHttpRequest.putHeader(anyString(), anyString())).thenReturn(mockHttpRequest);
    when(mockHttpResponse.statusCode()).thenReturn(400);
    when(mockHttpResponse.bodyAsString()).thenReturn("Bad Request: Invalid data");

    ArgumentCaptor<Function<HttpResponse<Buffer>, Future<Void>>> composeFunctionCaptor =
      ArgumentCaptor.forClass(Function.class);

    @SuppressWarnings("unchecked")
    Future<HttpResponse<Buffer>> mockFuture = mock(Future.class);
    when(mockHttpRequest.sendBuffer(any())).thenReturn(mockFuture);
    when(mockFuture.compose(composeFunctionCaptor.capture())).thenReturn(Future.succeededFuture());

    invokeSendPostRequest( jsonData);

    verify(webClient).postAbs(testUrl);

    Function<HttpResponse<Buffer>, Future<Void>> composeFunction = composeFunctionCaptor.getValue();
    Future<?> resultFuture = composeFunction.apply(mockHttpResponse);

    assertTrue(resultFuture.failed());
    resultFuture.onFailure(error -> {
      assertTrue(error.getMessage().contains("HTTP error: 400"));
      assertTrue(error.getMessage().contains("Bad Request"));
    });
  }

  @Test
  void sendPutRequest_ClientError_ShouldLogError() throws Exception {
    String jsonData = "{\"test\": \"data\"}";
    String testUrl = "http://localhost:8080/api/calculate";
    RuntimeException exception = new RuntimeException("Connection failed");

    when(AppConfig.getCalcSrvAbsoluteUrl()).thenReturn(testUrl);
    when(webClient.putAbs(testUrl)).thenReturn(mockHttpRequest);
    when(mockHttpRequest.putHeader(anyString(), anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.sendBuffer(any())).thenReturn(Future.failedFuture(exception));

    invokeSendPutRequest( jsonData, "unitId");

    verify(webClient).putAbs(testUrl);
    verify(mockHttpRequest).sendBuffer(any());
  }

  @Test
  void sendPutRequest_FailedResponseWithStatusCode400_ShouldLogError() throws Exception {
    String jsonData = "{\"test\": \"data\"}";
    String testUrl = "http://localhost:8080/api/calculate";
    when(AppConfig.getCalcSrvAbsoluteUrl()).thenReturn(testUrl);
    when(webClient.putAbs(testUrl)).thenReturn(mockHttpRequest);
    when(mockHttpRequest.putHeader(anyString(), anyString())).thenReturn(mockHttpRequest);
    when(mockHttpResponse.statusCode()).thenReturn(400);
    when(mockHttpResponse.bodyAsString()).thenReturn("Bad Request: Invalid data");

    ArgumentCaptor<Function<HttpResponse<Buffer>, Future<Void>>> composeFunctionCaptor =
      ArgumentCaptor.forClass(Function.class);

    @SuppressWarnings("unchecked")
    Future<HttpResponse<Buffer>> mockFuture = mock(Future.class);
    when(mockHttpRequest.sendBuffer(any())).thenReturn(mockFuture);
    when(mockFuture.compose(composeFunctionCaptor.capture())).thenReturn(Future.succeededFuture());

    invokeSendPutRequest( jsonData, "unitId");

    verify(webClient).putAbs(testUrl);

    Function<HttpResponse<Buffer>, Future<Void>> composeFunction = composeFunctionCaptor.getValue();
    Future<?> resultFuture = composeFunction.apply(mockHttpResponse);

    assertTrue(resultFuture.failed());
    resultFuture.onFailure(error -> {
      assertTrue(error.getMessage().contains("HTTP error: 400"));
      assertTrue(error.getMessage().contains("Bad Request"));
    });
  }

  @Test
  void sendPutRequest_SuccessfulResponse_ShouldSendRequest() {
    String jsonData = "{\"test\": \"data\"}";
    String testUrl = "http://localhost:8080/api/calculate";

    when(AppConfig.getCalcSrvAbsoluteUrl()).thenReturn(testUrl);
    when(webClient.putAbs(testUrl)).thenReturn(mockHttpRequest);
    when(mockHttpRequest.putHeader(anyString(), anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.sendBuffer(any())).thenReturn(Future.succeededFuture(mockHttpResponse));
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.bodyAsString()).thenReturn("Success");

    invokeSendPutRequest(jsonData, "unitId");

    verify(webClient).putAbs(testUrl);
    verify(mockHttpRequest).putHeader("Content-Type", "application/json");
    verify(mockHttpRequest).sendBuffer(Buffer.buffer(jsonData));
  }

  @Test
  void sendPostRequestAsync_shouldSubmitTaskToExecutor() throws Exception {

    String testJson = "{\"test\": \"data\"}";
//    ExecutorService spyExecutor = setExecutorField(handleDataService);

    invokeSendPostRequestAsync(testJson);

    verify(executorService, timeout(1000).times(1)).submit(any(Runnable.class));
  }

  @Test
  void sendPutRequestAsync_shouldSubmitTaskToExecutor() throws Exception {

    String testJson = "{\"test\": \"data\"}";
//    ExecutorService spyExecutor = setExecutorField(handleDataService);

    invokeSendPutRequestAsync(testJson, "unitId");

    verify(executorService, timeout(1000).times(1)).submit(any(Runnable.class));
  }

  private static ExecutorService setExecutorField(HandleDataService service) throws NoSuchFieldException, IllegalAccessException {
    java.lang.reflect.Field executorField = HandleDataService.class.getDeclaredField("executor");
    executorField.setAccessible(true);

    ExecutorService spyExecutor = spy((ExecutorService) executorField.get(service));
    executorField.set(service, spyExecutor);
    return spyExecutor;
  }

  public void invokeSendPostRequestAsync(String jsonData) {
    try {
      java.lang.reflect.Method method = CalculationServiceClient.class.getDeclaredMethod(
        "sendPostRequestAsync", String.class);
      method.setAccessible(true);
      method.invoke(calculationServiceClient, jsonData);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void invokeSendPutRequestAsync(String jsonData, String unitId) {
    try {
      java.lang.reflect.Method method = CalculationServiceClient.class.getDeclaredMethod(
        "sendPutRequestAsync", String.class, String.class);
      method.setAccessible(true);
      method.invoke(calculationServiceClient, jsonData, unitId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void invokeSendPutRequest(String jsonData, String unitId) {
    try {
      java.lang.reflect.Method method = CalculationServiceClient.class.getDeclaredMethod(
        "sendPutRequest", String.class, String.class);
      method.setAccessible(true);
      method.invoke(calculationServiceClient, jsonData, unitId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void invokeSendPostRequest(String jsonData) {
    try {
      java.lang.reflect.Method method = CalculationServiceClient.class.getDeclaredMethod(
        "sendPostRequest", String.class);
      method.setAccessible(true);
      method.invoke(calculationServiceClient, jsonData);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
