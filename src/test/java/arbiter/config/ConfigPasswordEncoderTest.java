package arbiter.config;

import static org.junit.jupiter.api.Assertions.*;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static arbiter.constants.UnitCollectionConstants.CONFIG_KEY_OIK;
import static arbiter.constants.UnitCollectionConstants.CONFIG_KEY_PASSWORD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigPasswordEncoderTest {

  @Mock
  private ConfigFileManager fileManager;

  private ConfigPasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    passwordEncoder = new ConfigPasswordEncoder(fileManager);
  }

  @Test
  void processPasswordEncoding_WhenOikFieldIsNull_ShouldThrowException() {
    String configFile = "test-config.json";
    JsonObject originalConfig = new JsonObject()
      .put("someField", "someValue");

    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> passwordEncoder.processPasswordEncoding(configFile, originalConfig)
    );

    assertTrue(exception.getMessage().contains("отсутствует обязательное поле"));
    verify(fileManager, never()).writeConfigFile(anyString(), any(JsonObject.class));
  }

  @Test
  void processPasswordEncoding_WhenPasswordIsNull_ShouldThrowException() {
    String configFile = "test-config.json";
    JsonObject oikField = new JsonObject()
      .put("otherField", "value");
    JsonObject originalConfig = new JsonObject()
      .put(CONFIG_KEY_OIK, oikField);

    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> passwordEncoder.processPasswordEncoding(configFile, originalConfig)
    );

    assertTrue(exception.getMessage().contains("отсутствует обязательное поле"));
    verify(fileManager, never()).writeConfigFile(anyString(), any(JsonObject.class));
  }

  @Test
  void processPasswordEncoding_WhenPasswordIsEmpty_ShouldReturnOriginalConfig() {
    String configFile = "test-config.json";
    JsonObject oikField = new JsonObject()
      .put(CONFIG_KEY_PASSWORD, "");
    JsonObject originalConfig = new JsonObject()
      .put(CONFIG_KEY_OIK, oikField);

    JsonObject result = passwordEncoder.processPasswordEncoding(configFile, originalConfig);

    assertSame(originalConfig, result);
    assertEquals("", oikField.getString(CONFIG_KEY_PASSWORD));
    verify(fileManager, never()).writeConfigFile(anyString(), any(JsonObject.class));
  }

  @Test
  void processPasswordEncoding_WhenPasswordIsAlreadyEncoded_ShouldReturnOriginalConfig() {
    String configFile = "test-config.json";
    String encodedPassword = SecurityConfig.encodePassword("testPassword");
    JsonObject oikField = new JsonObject()
      .put(CONFIG_KEY_PASSWORD, encodedPassword);
    JsonObject originalConfig = new JsonObject()
      .put(CONFIG_KEY_OIK, oikField);

    JsonObject result = passwordEncoder.processPasswordEncoding(configFile, originalConfig);

    assertSame(originalConfig, result);
    assertEquals(encodedPassword, oikField.getString(CONFIG_KEY_PASSWORD));
    verify(fileManager, never()).writeConfigFile(anyString(), any(JsonObject.class));
  }

  @Test
  void processPasswordEncoding_WhenPlainPasswordProvided_ShouldEncodeAndSave() {
    String configFile = "test-config.json";
    String plainPassword = "mySecretPassword";

    JsonObject oikField = new JsonObject()
      .put(CONFIG_KEY_PASSWORD, plainPassword);
    JsonObject originalConfig = new JsonObject()
      .put(CONFIG_KEY_OIK, oikField);

    JsonObject result = passwordEncoder.processPasswordEncoding(configFile, originalConfig);

    assertSame(originalConfig, result);

    String storedPassword = oikField.getString(CONFIG_KEY_PASSWORD);
    assertNotEquals(plainPassword, storedPassword);
    assertTrue(SecurityConfig.isEncoded(storedPassword));

    verify(fileManager, times(1)).writeConfigFile(eq(configFile), eq(originalConfig));
  }

  @Test
  void processPasswordEncoding_WhenMultipleCalls_ShouldNotReEncodeAlreadyEncodedPassword() {
    String configFile = "test-config.json";
    String plainPassword = "mySecretPassword";

    JsonObject oikField = new JsonObject()
      .put(CONFIG_KEY_PASSWORD, plainPassword);
    JsonObject config = new JsonObject()
      .put(CONFIG_KEY_OIK, oikField);

    JsonObject result1 = passwordEncoder.processPasswordEncoding(configFile, config);
    String encodedPassword = oikField.getString(CONFIG_KEY_PASSWORD);

    JsonObject result2 = passwordEncoder.processPasswordEncoding(configFile, config);

    assertSame(config, result1);
    assertSame(config, result2);
    assertEquals(encodedPassword, oikField.getString(CONFIG_KEY_PASSWORD));

    verify(fileManager, times(1)).writeConfigFile(eq(configFile), eq(config));
  }

  @Test
  void encodeAndSavePassword_WhenCalled_ShouldEncodeAndWriteToFile() {
    String configFile = "test-config.json";
    String plainPassword = "testPassword";

    JsonObject oikField = new JsonObject();
    JsonObject config = new JsonObject()
      .put(CONFIG_KEY_OIK, oikField);

      JsonObject result = invokeEncodeAndSavePassword(configFile, config, oikField, plainPassword);

      assertSame(config, result);
      assertTrue(SecurityConfig.isEncoded(oikField.getString(CONFIG_KEY_PASSWORD)));
      verify(fileManager, times(1)).writeConfigFile(eq(configFile), eq(config));
  }

  private JsonObject invokeEncodeAndSavePassword(String configFile, JsonObject config, JsonObject oikField, String plainPassword) {
    try {
      java.lang.reflect.Method method = ConfigPasswordEncoder.class.getDeclaredMethod(
        "encodeAndSavePassword", String.class, JsonObject.class, JsonObject.class, String.class);
      method.setAccessible(true);
      return (JsonObject) method.invoke(passwordEncoder, configFile, config, oikField, plainPassword);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke private method: " + e.getMessage());
    }
  }
}
