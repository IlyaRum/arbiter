package arbiter.util;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConfigValidatorTest {

  private JsonObject config;
  private String validUuid;

  @BeforeEach
  void setUp() {
    validUuid = UUID.randomUUID().toString().toUpperCase();
    config = new JsonObject();
  }

  @Test
  @DisplayName("Валидация с непустым строковым значением")
  void validateFieldNameAndValue_WithNonEmptyString_ShouldPass() {
    String value = "testValue";
    Object result = ConfigValidator.validateFieldNameAndValue(value, "fieldName");
    assertEquals(value, result);
  }

  @Test
  @DisplayName("Исключение при null значении")
  void validateFieldNameAndValue_WithNull_ShouldThrowException() {
    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> ConfigValidator.validateFieldNameAndValue(null, "fieldName")
    );
    assertTrue(exception.getMessage().contains("отсутствует обязательное поле"));
  }

  @Test
  @DisplayName("Исключение при пустой строке")
  void validateFieldNameAndValue_WithEmptyString_ShouldThrowException() {
    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> ConfigValidator.validateFieldNameAndValue("", "fieldName")
    );
    assertTrue(exception.getMessage().contains("не может быть пустым"));
  }

  @Test
  @DisplayName("Валидация с существующим целочисленным полем")
  void validateFieldName_WithExistingInteger_ShouldPass() {
    config.put("fieldName", 42);
    Integer result = ConfigValidator.validateFieldName(config, "fieldName", 42);
    assertEquals(42, result);
  }

  @Test
  @DisplayName("Исключение при отсутствии целочисленного поля")
  void validateFieldName_WithMissingInteger_ShouldThrowException() {
    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> ConfigValidator.validateFieldName(config, "missingInt", 42)
    );
    assertTrue(exception.getMessage().contains("отсутствует обязательное поле"));
  }

  @Test
  @DisplayName("Валидация с существующим целочисленным полем")
  void validateFieldNameInSection_WithExistingInteger_ShouldPass() {
    config.put("fieldName", 42);
    Integer result = ConfigValidator.validateFieldNameInSection(config, "fieldName", 42, "sectionName");
    assertEquals(42, result);
  }

  @Test
  @DisplayName("Исключение при отсутствии целочисленного поля")
  void validateFieldNameInSection_WithMissingInteger_ShouldThrowException() {
    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> ConfigValidator.validateFieldNameInSection(config, "missingInt", 42, "sectionName")
    );
    assertTrue(exception.getMessage().contains("отсутствует обязательное поле"));
  }

  @Test
  @DisplayName("Валидация с непустым строковым значением поля в секции")
  void validateFieldNameAndValueInSection_WithNonEmptyString_ShouldPass() {
    config.put("fieldName", "testValue");
    Object result = ConfigValidator.validateFieldNameAndValueInSection(config, "fieldName", "sectionName");
    assertEquals(config, result);
  }

  @Test
  @DisplayName("Исключение при null значении поля в секции")
  void validateFieldNameAndValueInSection_WithNull_ShouldThrowException() {
    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> ConfigValidator.validateFieldNameAndValueInSection(null, "fieldName", "sectionName")
    );
    assertTrue(exception.getMessage().contains("отсутствует обязательное поле"));
  }

  @Test
  @DisplayName("Исключение при пустой строке")
  void validateFieldNameAndValueInSection_WithEmptyString_ShouldThrowException() {
    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> ConfigValidator.validateFieldNameAndValueInSection("", "fieldName", "sectionName")
    );
    assertTrue(exception.getMessage().contains("не может быть пустым"));
  }

  @Test
  @DisplayName("Валидация наличия имени поля в секции")
  void testValidateFieldNameInSection_WithNonEmptyString_ShouldPass() {
    config.put("fieldName", "value");
    Object result = ConfigValidator.validateFieldNameInSection(config, "fieldName", "sectionName");
    assertEquals("fieldName", result);
  }

  @Test
  @DisplayName("Исключение, если имя поля не найдено в секции")
  void testValidateFieldNameInSection_WithNull_ShouldThrowException() {
    String fieldName = "fieldName";
    String sectionName = "sectionName";
    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> ConfigValidator.validateFieldNameInSection(null, fieldName, sectionName)
    );
    assertTrue(exception.getMessage().contains("Критическая ошибка: отсутствует обязательное поле '" + fieldName + "' в секции '" + sectionName));
  }

  @Test
  @DisplayName("Валидация с корректным UUID")
  void validateFieldNameAndValueUuid_WithValidUuid_ShouldPass() {
    config.put("fieldName", validUuid);
    String result = ConfigValidator.validateFieldNameAndValueUuid("fieldName", config);
    assertEquals(validUuid, result);
  }

  @Test
  @DisplayName("Исключение при отсутствии поля")
  void validateFieldNameAndValueUuid_WithMissingField_ShouldThrowException() {
    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> ConfigValidator.validateFieldNameAndValueUuid("missingField", config)
    );
    assertTrue(exception.getMessage().contains("Критическая ошибка: отсутствует обязательное поле"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "invalid-uuid", "12345", "not-a-uuid-format"})
  @DisplayName("Исключение при невалидном UUID")
  void validateFieldNameAndValueUuid_WithInvalidUuid_ShouldThrowException(String invalidUuid) {
    config.put("fieldName", invalidUuid);

    if (invalidUuid.isEmpty()) {
      IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> ConfigValidator.validateFieldNameAndValueUuid("fieldName", config)
      );
      assertTrue(exception.getMessage().contains("не может быть пустым"));
    } else {
      IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ConfigValidator.validateFieldNameAndValueUuid("fieldName", config)
      );
      assertTrue(exception.getMessage().contains("Критическая ошибка: не валидный uuid"));
    }
  }

  @Test
  @DisplayName("Валидация с корректным UUID поля в секции")
  void validateFieldNameAndValueUuidInSection_WithValidUuid_ShouldPass() {
    config.put("fieldName", validUuid);
    String result = ConfigValidator.validateFieldNameAndValueUuidInSection("fieldName", config, "sectionName");
    assertEquals(validUuid, result);
  }

  @Test
  @DisplayName("Исключение при отсутствии для поля в секции")
  void validateFieldNameAndValueUuidInSection_WithMissingField_ShouldThrowException() {
    config.put("", validUuid);
    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> ConfigValidator.validateFieldNameAndValueUuidInSection("missingField", config, "sectionName")
    );
    assertTrue(exception.getMessage().contains("Критическая ошибка: отсутствует обязательное поле"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "invalid-uuid", "12345", "not-a-uuid-format"})
  @DisplayName("Исключение при невалидном UUID для поля в секции")
  void validateFieldNameAndValueUuidInSection_WithInvalidUuid_ShouldThrowException(String invalidUuid) {
    config.put("fieldName", invalidUuid);

    if (invalidUuid.isEmpty()) {
      IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> ConfigValidator.validateFieldNameAndValueUuidInSection("fieldName", config, "sectionName")
      );
      assertTrue(exception.getMessage().contains("не может быть пустым"));
    } else {
      IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> ConfigValidator.validateFieldNameAndValueUuidInSection("fieldName", config, "sectionName")
      );
      assertTrue(exception.getMessage().contains("Критическая ошибка: не валидный uuid"));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"да", "Да", "ДА", "нет", "Нет", "НЕТ"})
  @DisplayName("Валидация с корректными значениями 'да'/'нет'")
  void validateBooleanValue_WithValidValues_ShouldPass(String value) {
    String result = ConfigValidator.validateBooleanValue(value, "fieldName");
    assertEquals(value, result);
  }

  @ParameterizedTest
  @ValueSource(strings = {"yes", "no", "true", "false", "1", "0", "maybe"})
  @DisplayName("Исключение при некорректных значениях")
  void validateBooleanValue_WithInvalidValues_ShouldThrowException(String invalidValue) {
    IllegalStateException exception = assertThrows(
      IllegalStateException.class,
      () -> ConfigValidator.validateBooleanValue(invalidValue, "fieldName")
    );
    assertTrue(exception.getMessage().contains("не валидно"));
  }

  @Test
  @DisplayName("Валидация наличия не нулевого поля")
  void checkValueProperty_WithNoNullValue_ShouldPass() {
    Properties prop = new Properties();
    prop.setProperty("prop1", "value1");
    String result = ConfigValidator.checkValueProperty(prop, "prop1", "configFile");
    assertEquals("value1", result);
  }

  @ParameterizedTest
  @ValueSource(strings = {"prop1", "prop2"})
  @DisplayName("Исключение если значение для свойства null или пустое")
  void checkValueProperty_WithNullValue_ShouldThrowException(String key) {
    Properties properties = new Properties();
    properties.setProperty("prop1", "");
    IllegalStateException exception = assertThrows(IllegalStateException.class,
      () -> ConfigValidator.checkValueProperty(properties, key, "configFile"));
    assertTrue(exception.getMessage().contains("Отсутствует обязательное свойство"));
  }
}
