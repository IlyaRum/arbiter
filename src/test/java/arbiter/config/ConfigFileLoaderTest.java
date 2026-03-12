package arbiter.config;

import arbiter.data.UnitCollection;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigFileLoaderTest {

  @Mock
  private Vertx vertx;

  @Mock
  private ConfigFileManager fileManager;

  @Mock
  private ConfigPasswordEncoder passwordEncoder;

  @Mock
  private UnitCollection collection;

  private ConfigFileLoader configFileLoader;

  private static final String TEST_CONFIG_FILE = "test-config.json";
  private static final JsonObject TEST_CONFIG = new JsonObject()
    .put("oik", new JsonObject()
      .put("host", "localhost")
      .put("user", "login")
      .put("password", "encrypt_me"))
    ;

  private static final JsonObject ENCODED_CONFIG = new JsonObject()
    .put("oik", new JsonObject()
      .put("host", "localhost")
      .put("user", "login")
      .put("password", "ENCRYPTED_PASSWORD"))
    ;

  private static final Buffer TEST_BUFFER = Buffer.buffer("{\"config\":\"test\"}");

  @BeforeEach
  void setUp() {
   configFileLoader = new ConfigFileLoader(vertx, fileManager, passwordEncoder);
  }

  @Test
  void loadConfigFileAsync_SuccessfulLoad_ShouldCompleteSuccessfully() {
    setupFlowStub();
    setupExecuteBlockingStub();

    CompletableFuture<Void> future = configFileLoader.loadConfigFileAsync(TEST_CONFIG_FILE, collection);

    assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
    verify(fileManager).readConfigFile(TEST_CONFIG_FILE);
    verify(passwordEncoder).processPasswordEncoding(anyString(), any(JsonObject.class));
    verify(fileManager).configToBuffer(ENCODED_CONFIG);
    verify(collection).loadConfigData(TEST_BUFFER);
    verify(vertx, never()).close();
  }

  @Test
  void loadConfigFileAsync_WhenReadConfigFails_ShouldCompleteExceptionallyAndCloseVertx() {
    RuntimeException expectedException = new RuntimeException("File not found: config.json");
    when(fileManager.readConfigFile(anyString())).thenThrow(expectedException);

    setupExecuteBlockingStub();
    setupVertxCloseStub();

    CompletableFuture<Void> future = configFileLoader.loadConfigFileAsync(TEST_CONFIG_FILE, collection);

    assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    verify(fileManager).readConfigFile(TEST_CONFIG_FILE);
    verify(passwordEncoder, never()).processPasswordEncoding(anyString(), any());
    verify(fileManager, never()).configToBuffer(any());
    verify(collection, never()).loadConfigData(any());
    verify(vertx).close();
  }

  @Test
  void loadConfigFileAsync_WhenConfigToBufferFails_ShouldCompleteExceptionallyAndCloseVertx() {
    RuntimeException expectedException = new RuntimeException("Buffer conversion failed");
    when(fileManager.readConfigFile(anyString())).thenReturn(TEST_CONFIG);
    when(passwordEncoder.processPasswordEncoding(anyString(), any(JsonObject.class))).thenReturn(ENCODED_CONFIG);
    when(fileManager.configToBuffer(any(JsonObject.class))).thenThrow(expectedException);

    setupExecuteBlockingStub();

    setupVertxCloseStub();

    CompletableFuture<Void> future = configFileLoader.loadConfigFileAsync(TEST_CONFIG_FILE, collection);

    assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    verify(fileManager).readConfigFile(TEST_CONFIG_FILE);
    verify(passwordEncoder).processPasswordEncoding(anyString(), any(JsonObject.class));
    verify(fileManager).configToBuffer(ENCODED_CONFIG);
    verify(collection, never()).loadConfigData(any());
    verify(vertx).close();
  }

  @Test
  void loadConfigFileAsync_WhenLoadConfigDataFails_ShouldCompleteExceptionallyAndCloseVertx() {
    RuntimeException expectedException = new RuntimeException("Failed to load config data");
    setupFlowStub();
    doThrow(expectedException).when(collection).loadConfigData(TEST_BUFFER);

    setupExecuteBlockingStub();
    setupVertxCloseStub();

    CompletableFuture<Void> future = configFileLoader.loadConfigFileAsync(TEST_CONFIG_FILE, collection);

    assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    verify(fileManager).readConfigFile(TEST_CONFIG_FILE);
    verify(passwordEncoder).processPasswordEncoding(anyString(), any(JsonObject.class));
    verify(fileManager).configToBuffer(ENCODED_CONFIG);
    verify(collection).loadConfigData(TEST_BUFFER);
    verify(vertx).close();
  }

  @Test
  void loadConfigFileAsync_WhenConfigFileIsNull_ShouldHandleGracefully() {
    String nullConfigFile = null;
    NullPointerException expectedException = new NullPointerException("Config file path cannot be null");
    when(fileManager.readConfigFile(null)).thenThrow(expectedException);

    setupExecuteBlockingStub();
    setupVertxCloseStub();

    CompletableFuture<Void> future = configFileLoader.loadConfigFileAsync(nullConfigFile, collection);

    assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    verify(vertx).close();
  }

  @Test
  void loadConfigFileAsync_WhenConfigFileIsEmpty_ShouldHandleGracefully() {
    String emptyConfigFile = "";
    RuntimeException expectedException = new RuntimeException("Config file path is empty");
    when(fileManager.readConfigFile(emptyConfigFile)).thenThrow(expectedException);

    setupExecuteBlockingStub();
    setupVertxCloseStub();

    CompletableFuture<Void> future = configFileLoader.loadConfigFileAsync(emptyConfigFile, collection);

    assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    verify(vertx).close();
  }

  @Test
  void loadConfigFileAsync_WhenVertxExecuteBlockingFails_ShouldCompleteExceptionallyAndCloseVertx() {
    RuntimeException vertxError = new RuntimeException("Vert.x execution failed");
    when(vertx.executeBlocking(any(Callable.class), eq(false)))
      .thenReturn(Future.failedFuture(vertxError));
    setupVertxCloseStub();

    CompletableFuture<Void> future = configFileLoader.loadConfigFileAsync(TEST_CONFIG_FILE, collection);

    ExecutionException exception = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    assertEquals(vertxError, exception.getCause());
    verify(vertx).close();
  }

  @Test
  void loadConfigFileAsync_VerifyOperationsOrder() throws Exception {
    setupFlowStub();

    InOrder inOrder = inOrder(fileManager, passwordEncoder, collection);

    setupExecuteBlockingStub();

    CompletableFuture<Void> future = configFileLoader.loadConfigFileAsync(TEST_CONFIG_FILE, collection);
    future.get(5, TimeUnit.SECONDS);

    inOrder.verify(fileManager).readConfigFile(TEST_CONFIG_FILE);
    inOrder.verify(passwordEncoder).processPasswordEncoding(anyString(), any(JsonObject.class));
    inOrder.verify(fileManager).configToBuffer(ENCODED_CONFIG);
    inOrder.verify(collection).loadConfigData(TEST_BUFFER);
  }

  @Test
  void loadConfigFileAsync_WithLargeConfig_ShouldHandleSuccessfully() {
    JsonObject largeConfig = new JsonObject();
    for (int i = 0; i < 1000; i++) {
      largeConfig.put("key" + i, "value" + i);
    }

    JsonObject largeEncodedConfig = largeConfig.copy();
    Buffer largeBuffer = Buffer.buffer(largeConfig.encode());

    when(fileManager.readConfigFile(anyString())).thenReturn(largeConfig);
    when(passwordEncoder.processPasswordEncoding(TEST_CONFIG_FILE, largeConfig)).thenReturn(largeEncodedConfig);
    when(fileManager.configToBuffer(largeEncodedConfig)).thenReturn(largeBuffer);

    setupExecuteBlockingStub();

    CompletableFuture<Void> future = configFileLoader.loadConfigFileAsync(TEST_CONFIG_FILE, collection);

    assertDoesNotThrow(() -> future.get(10, TimeUnit.SECONDS));
    verify(collection).loadConfigData(largeBuffer);
  }

  @Test
  void loadConfigFileAsync_WhenCloseVertxFails_ShouldStillCompleteExceptionally() {
    RuntimeException expectedException = new RuntimeException("File error");

    setupVertxCloseStub();

    when(vertx.executeBlocking(any(Callable.class), eq(false)))
      .thenAnswer(invocation -> Future.failedFuture(expectedException));

    CompletableFuture<Void> future = configFileLoader.loadConfigFileAsync(TEST_CONFIG_FILE, collection);

    ExecutionException exception = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    assertEquals(expectedException, exception.getCause());
    verify(vertx).close();
  }

  @Test
  void loadConfigFileAsync_ShouldExecuteInBlockingMode() {
    setupFlowStub();
    setupExecuteBlockingStub();

    CompletableFuture<Void> future = configFileLoader.loadConfigFileAsync(TEST_CONFIG_FILE, collection);

    assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
    verify(vertx).executeBlocking(any(Callable.class), eq(false));
  }

  private void setupFlowStub() {
    when(fileManager.readConfigFile(anyString())).thenReturn(TEST_CONFIG);
    when(passwordEncoder.processPasswordEncoding(anyString(), any(JsonObject.class))).thenReturn(ENCODED_CONFIG);
    when(fileManager.configToBuffer(any(JsonObject.class))).thenReturn(TEST_BUFFER);
  }


  private void setupVertxCloseStub() {
    Future<Void> closeFuture = mock(Future.class);
    when(vertx.close()).thenReturn(closeFuture);
  }

  private void setupExecuteBlockingStub() {
    when(vertx.executeBlocking(any(Callable.class), anyBoolean()))
      .thenAnswer(invocation -> {
        Callable<Void> callable = invocation.getArgument(0);
        try {
          callable.call();
          return Future.succeededFuture();
        } catch (Exception e) {
          return Future.failedFuture(e);
        }
      });
  }
}
