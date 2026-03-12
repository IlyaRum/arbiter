package arbiter.config;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigFileManagerTest {

  @Mock
  private Vertx vertx;

  @Mock
  private FileSystem fileSystem;

  private ConfigFileManager configFileManager;

  private static final String TEST_CONFIG_FILE = "test-config.json";
  private static final String CONFIG_CONTENT = "{\"key\":\"value\",\"number\":123}";

  @BeforeEach
  void setUp() {
    lenient().when(vertx.fileSystem()).thenReturn(fileSystem);
    configFileManager = new ConfigFileManager(vertx);
  }

  @Test
  void readConfigFile_Success_ReturnsJsonObject() {
    Buffer buffer = Buffer.buffer(CONFIG_CONTENT);
    when(fileSystem.readFileBlocking(TEST_CONFIG_FILE)).thenReturn(buffer);

    JsonObject result = configFileManager.readConfigFile(TEST_CONFIG_FILE);

    assertNotNull(result);
    assertEquals("value", result.getString("key"));
    assertEquals(123, result.getInteger("number"));
    verify(fileSystem).readFileBlocking(TEST_CONFIG_FILE);
  }

  @Test
  void readConfigFile_FileNotFound_ThrowsException() {
    when(fileSystem.readFileBlocking(TEST_CONFIG_FILE))
      .thenThrow(new RuntimeException("File not found"));


    RuntimeException exception = assertThrows(RuntimeException.class,
      () -> configFileManager.readConfigFile(TEST_CONFIG_FILE));

    assertTrue(exception.getMessage().contains("Config loading failed"));
    verify(fileSystem).readFileBlocking(TEST_CONFIG_FILE);
  }

  @Test
  void readConfigFile_InvalidJson_ThrowsException() {

    Buffer invalidBuffer = Buffer.buffer("invalid json");
    when(fileSystem.readFileBlocking(TEST_CONFIG_FILE)).thenReturn(invalidBuffer);

    assertThrows(RuntimeException.class,
      () -> configFileManager.readConfigFile(TEST_CONFIG_FILE));

    verify(fileSystem).readFileBlocking(TEST_CONFIG_FILE);
  }

  @Test
  void writeConfigFile_Success_WritesFile() {
    JsonObject config = new JsonObject()
      .put("key", "value")
      .put("number", 123);

    when(fileSystem.writeFileBlocking(eq(TEST_CONFIG_FILE), any(Buffer.class)))
      .thenReturn(fileSystem);

    configFileManager.writeConfigFile(TEST_CONFIG_FILE, config);

    verify(fileSystem).writeFileBlocking(eq(TEST_CONFIG_FILE), any(Buffer.class));
  }

  @Test
  void writeConfigFile_WriteError_ThrowsException() {
    JsonObject config = new JsonObject().put("key", "value");

    when(fileSystem.writeFileBlocking(eq(TEST_CONFIG_FILE), any(Buffer.class)))
      .thenThrow(new RuntimeException("Write failed"));

    RuntimeException exception = assertThrows(RuntimeException.class,
      () -> configFileManager.writeConfigFile(TEST_CONFIG_FILE, config));

    assertTrue(exception.getMessage().contains("Config file writing failed"));
    verify(fileSystem).writeFileBlocking(eq(TEST_CONFIG_FILE), any(Buffer.class));
  }

  @Test
  void writeConfigFile_NullConfig_ThrowsException() {

    assertThrows(RuntimeException.class,
      () -> configFileManager.writeConfigFile(TEST_CONFIG_FILE, null));
  }

  @Test
  void configToBuffer_ReturnsCorrectBuffer() {
    JsonObject config = new JsonObject()
      .put("key", "value")
      .put("number", 123);

    Buffer result = configFileManager.configToBuffer(config);

    assertNotNull(result);
    String expectedJson = config.encodePrettily();
    assertEquals(expectedJson, result.toString());
  }

  @Test
  void configToBuffer_EmptyConfig_ReturnsEmptyJsonBuffer() {
    JsonObject emptyConfig = new JsonObject();

    Buffer result = configFileManager.configToBuffer(emptyConfig);

    assertNotNull(result);
    assertEquals(emptyConfig.encodePrettily(), result.toString());
  }
}
