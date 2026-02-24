package arbiter.service;

import arbiter.config.AppConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculationServiceClientTest {

  @Mock
  private WebClient webClient;

  @Mock
  private Vertx vertx;

  @Mock
  private ExecutorService executorService;

  @Mock
  private HttpRequest<Buffer> mockHttpRequest;

  @Mock
  private HttpResponse<Buffer> mockHttpResponse;

  private CalculationServiceClient calculationServiceClient;
  private MockedStatic<AppConfig> mockedAppConfig;
  private MockedStatic<WebClient> mockedWebClient;

  @BeforeEach
  void setUp() {
    mockedAppConfig = Mockito.mockStatic(AppConfig.class);
    mockedWebClient = Mockito.mockStatic(WebClient.class);
    mockedWebClient.when(() -> WebClient.wrap(any())).thenReturn(webClient);

    calculationServiceClient = new CalculationServiceClient(vertx, executorService);
  }

  @AfterEach
  void tearDown() {
    if (mockedAppConfig != null) {
      mockedAppConfig.close();
    }
    if (mockedWebClient != null) {
      mockedWebClient.close();
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
    when(mockHttpRequest.timeout(anyLong())).thenReturn(mockHttpRequest);

    invokeSendPostRequest(jsonData);

    verify(webClient).postAbs(testUrl);
    verify(mockHttpRequest).putHeader("Content-Type", "application/json");
    verify(mockHttpRequest).sendBuffer(Buffer.buffer(jsonData));
  }

  @Test
  void sendPostRequest_WhenShuttingDown_ShouldNotSendRequest() {
    String jsonData = "{\"test\": \"data\"}";

    setShuttingDownFlag(true);

    invokeSendPostRequest(jsonData);

    verify(webClient, never()).postAbs(anyString());
    verify(mockHttpRequest, never()).sendBuffer(any());
  }

  @Test
  void sendPostRequest_ClientError_ShouldLogError() {
    String jsonData = "{\"test\": \"data\"}";
    String testUrl = "http://localhost:8080/api/calculate";
    RuntimeException exception = new RuntimeException("Connection failed");

    when(AppConfig.getCalcSrvAbsoluteUrl()).thenReturn(testUrl);
    when(webClient.postAbs(testUrl)).thenReturn(mockHttpRequest);
    when(mockHttpRequest.putHeader(anyString(), anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.sendBuffer(any())).thenReturn(Future.failedFuture(exception));
    when(mockHttpRequest.timeout(anyLong())).thenReturn(mockHttpRequest);

    invokeSendPostRequest(jsonData);

    verify(webClient).postAbs(testUrl);
    verify(mockHttpRequest).sendBuffer(any());
  }

  @Test
  void sendPostRequest_FailedResponseWithStatusCode400_ShouldLogError() {
    String jsonData = "{\"test\": \"data\"}";
    String testUrl = "http://localhost:8080/api/calculate";

    when(AppConfig.getCalcSrvAbsoluteUrl()).thenReturn(testUrl);
    when(webClient.postAbs(testUrl)).thenReturn(mockHttpRequest);
    when(mockHttpRequest.putHeader(anyString(), anyString())).thenReturn(mockHttpRequest);
    when(mockHttpResponse.statusCode()).thenReturn(400);
    when(mockHttpResponse.bodyAsString()).thenReturn("Bad Request: Invalid data");

    @SuppressWarnings("unchecked")
    Future<HttpResponse<Buffer>> mockFuture = mock(Future.class);
    when(mockHttpRequest.sendBuffer(any())).thenReturn(mockFuture);
    ArgumentCaptor<Function<HttpResponse<Buffer>, Future<Void>>> composeCaptor =
      ArgumentCaptor.forClass(Function.class);
    when(mockFuture.compose(composeCaptor.capture())).thenReturn(Future.succeededFuture());
    when(mockHttpRequest.timeout(anyLong())).thenReturn(mockHttpRequest);

    invokeSendPostRequest(jsonData);

    verify(webClient).postAbs(testUrl);

    Function<HttpResponse<Buffer>, Future<Void>> composeFunction = composeCaptor.getValue();
    Future<Void> resultFuture = composeFunction.apply(mockHttpResponse);

    assertTrue(resultFuture.failed());
    resultFuture.onFailure(error -> {
      assertTrue(error.getMessage().contains("HTTP error: 400"));
      assertTrue(error.getMessage().contains("Bad Request"));
    });
  }

  @Test
  void sendPostRequest_SuccessfulResponseWithStatusCode201_ShouldSucceed() {
    String jsonData = "{\"test\": \"data\"}";
    String testUrl = "http://localhost:8080/api/calculate";

    when(AppConfig.getCalcSrvAbsoluteUrl()).thenReturn(testUrl);
    when(webClient.postAbs(testUrl)).thenReturn(mockHttpRequest);
    when(mockHttpRequest.putHeader(anyString(), anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.sendBuffer(any())).thenReturn(Future.succeededFuture(mockHttpResponse));
    when(mockHttpResponse.statusCode()).thenReturn(201);
    when(mockHttpResponse.bodyAsString()).thenReturn("Created");
    when(mockHttpRequest.timeout(anyLong())).thenReturn(mockHttpRequest);

    invokeSendPostRequest(jsonData);

    verify(webClient).postAbs(testUrl);
    verify(mockHttpRequest).sendBuffer(Buffer.buffer(jsonData));
  }

  @Test
  void sendPutRequest_SuccessfulResponse_ShouldSendRequest() {
    String jsonData = "{\"test\": \"data\"}";
    String unitId = "unit123";
    String testUrl = "http://localhost:8080/api/calculate";

    when(AppConfig.getCalcSrvAbsoluteUrl()).thenReturn(testUrl);
    when(webClient.putAbs(testUrl)).thenReturn(mockHttpRequest);
    when(mockHttpRequest.putHeader(anyString(), anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.sendBuffer(any())).thenReturn(Future.succeededFuture(mockHttpResponse));
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.bodyAsString()).thenReturn("Success");
    when(mockHttpRequest.timeout(anyLong())).thenReturn(mockHttpRequest);

    invokeSendPutRequest(jsonData, unitId);

    verify(webClient).putAbs(testUrl);
    verify(mockHttpRequest).putHeader("Content-Type", "application/json");
    verify(mockHttpRequest).sendBuffer(Buffer.buffer(jsonData));
  }

  @Test
  void sendPutRequest_WhenShuttingDown_ShouldNotSendRequest() {
    String jsonData = "{\"test\": \"data\"}";
    String unitId = "unit123";

    setShuttingDownFlag(true);

    invokeSendPutRequest(jsonData, unitId);

    verify(webClient, never()).putAbs(anyString());
    verify(mockHttpRequest, never()).sendBuffer(any());
  }

  @Test
  void sendPutRequest_ClientError_ShouldLogError() {
    String jsonData = "{\"test\": \"data\"}";
    String unitId = "unit123";
    String testUrl = "http://localhost:8080/api/calculate";
    RuntimeException exception = new RuntimeException("Connection failed");

    when(AppConfig.getCalcSrvAbsoluteUrl()).thenReturn(testUrl);
    when(webClient.putAbs(testUrl)).thenReturn(mockHttpRequest);
    when(mockHttpRequest.putHeader(anyString(), anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.sendBuffer(any())).thenReturn(Future.failedFuture(exception));
    when(mockHttpRequest.timeout(anyLong())).thenReturn(mockHttpRequest);

    invokeSendPutRequest(jsonData, unitId);

    verify(webClient).putAbs(testUrl);
    verify(mockHttpRequest).sendBuffer(any());
  }

  @Test
  void sendPutRequest_FailedResponseWithStatusCode500_ShouldLogError() {
    String jsonData = "{\"test\": \"data\"}";
    String unitId = "unit123";
    String testUrl = "http://localhost:8080/api/calculate";

    when(AppConfig.getCalcSrvAbsoluteUrl()).thenReturn(testUrl);
    when(webClient.putAbs(testUrl)).thenReturn(mockHttpRequest);
    when(mockHttpRequest.putHeader(anyString(), anyString())).thenReturn(mockHttpRequest);
    when(mockHttpResponse.statusCode()).thenReturn(500);
    when(mockHttpResponse.bodyAsString()).thenReturn("Internal Server Error");
    when(mockHttpRequest.timeout(anyLong())).thenReturn(mockHttpRequest);

    @SuppressWarnings("unchecked")
    Future<HttpResponse<Buffer>> mockFuture = mock(Future.class);
    when(mockHttpRequest.sendBuffer(any())).thenReturn(mockFuture);

    ArgumentCaptor<Function<HttpResponse<Buffer>, Future<Void>>> composeCaptor =
      ArgumentCaptor.forClass(Function.class);
    when(mockFuture.compose(composeCaptor.capture())).thenReturn(Future.succeededFuture());

    invokeSendPutRequest(jsonData, unitId);

    verify(webClient).putAbs(testUrl);

    Function<HttpResponse<Buffer>, Future<Void>> composeFunction = composeCaptor.getValue();
    Future<Void> resultFuture = composeFunction.apply(mockHttpResponse);

    assertTrue(resultFuture.failed());
    resultFuture.onFailure(error -> {
      assertTrue(error.getMessage().contains("HTTP error: 500"));
      assertTrue(error.getMessage().contains("Internal Server Error"));
    });
  }

  @Test
  void sendPostRequestAsync_ShouldSubmitTaskToExecutor() {
    String jsonData = "{\"test\": \"data\"}";

    calculationServiceClient.sendPostRequestAsync(jsonData);

    verify(executorService).submit(any(Runnable.class));
  }

  @Test
  void sendPutRequestAsync_ShouldSubmitTaskToExecutor() {
    String jsonData = "{\"test\": \"data\"}";
    String unitId = "unit123";

    calculationServiceClient.sendPutRequestAsync(jsonData, unitId);

    verify(executorService).submit(any(Runnable.class));
  }

  @Test
  void sendPutRequestAsync_WhenShuttingDown_ShouldSubmitTaskButNotSendRequest() {
    String jsonData = "{\"test\": \"data\"}";
    String unitId = "unit123";

    setShuttingDownFlag(true);

    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

    calculationServiceClient.sendPutRequestAsync(jsonData, unitId);

    verify(executorService).submit(runnableCaptor.capture());

    Runnable capturedRunnable = runnableCaptor.getValue();
    capturedRunnable.run();

    verify(webClient, never()).putAbs(anyString());
  }

  @Test
  void shutdown_ShouldSetShuttingDownFlagAndShutdownExecutor() throws InterruptedException {
    when(executorService.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(true);

    calculationServiceClient.shutdown();

    assertTrue(getShuttingDownFlag());

    verify(executorService).shutdown();
    verify(executorService).awaitTermination(5, TimeUnit.SECONDS);
    verify(executorService, never()).shutdownNow();
  }

  @Test
  void shutdown_WhenAwaitTerminationTimesOut_ShouldCallShutdownNow() throws InterruptedException {
    when(executorService.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(false);

    calculationServiceClient.shutdown();

    verify(executorService).shutdown();
    verify(executorService).awaitTermination(5, TimeUnit.SECONDS);
    verify(executorService).shutdownNow();
  }

  @Test
  void shutdown_WhenAwaitTerminationInterrupted_ShouldCallShutdownNowAndRestoreInterrupt() throws InterruptedException {
    when(executorService.awaitTermination(5, TimeUnit.SECONDS))
      .thenThrow(new InterruptedException("Test interruption"));

    calculationServiceClient.shutdown();

    verify(executorService).shutdown();
    verify(executorService).awaitTermination(5, TimeUnit.SECONDS);
    verify(executorService).shutdownNow();

    assertTrue(Thread.currentThread().isInterrupted());

    Thread.interrupted();
  }

  private void setShuttingDownFlag(boolean value) {
    try {
      java.lang.reflect.Field field = CalculationServiceClient.class.getDeclaredField("isShuttingDown");
      field.setAccessible(true);
      field.set(calculationServiceClient, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean getShuttingDownFlag() {
    try {
      java.lang.reflect.Field field = CalculationServiceClient.class.getDeclaredField("isShuttingDown");
      field.setAccessible(true);
      return (boolean) field.get(calculationServiceClient);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void invokeSendPostRequest(String jsonData) {
    try {
      java.lang.reflect.Method method = CalculationServiceClient.class.getDeclaredMethod(
        "sendPostRequest", String.class);
      method.setAccessible(true);
      method.invoke(calculationServiceClient, jsonData);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void invokeSendPutRequest(String jsonData, String unitId) {
    try {
      java.lang.reflect.Method method = CalculationServiceClient.class.getDeclaredMethod(
        "sendPutRequest", String.class, String.class);
      method.setAccessible(true);
      method.invoke(calculationServiceClient, jsonData, unitId);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
