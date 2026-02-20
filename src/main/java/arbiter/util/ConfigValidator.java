package arbiter.util;

import io.vertx.core.json.JsonObject;
import arbiter.config.AppConfig;

import java.util.regex.Pattern;

/**
 * Утилитарный класс для валидации полей конфигурации
 */
public final class ConfigValidator {

  private static final String UUID_REGEX =
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

  private static final Pattern UUID_PATTERN = Pattern.compile(UUID_REGEX);

  private static final String ARBITER_CONFIG_JSON_FILE = AppConfig.getArbiterConfigJsonFile();

  private ConfigValidator() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  private static void checkNotNull(Object jsonObject, String fieldName, String sectionName) {
    if (jsonObject == null) {
      String message = (sectionName != null)
              ? "Критическая ошибка: отсутствует обязательное поле '" + fieldName
              + "' в секции '" + sectionName + "' в конфигурационном файле: "
              + ARBITER_CONFIG_JSON_FILE
              : "Критическая ошибка: отсутствует обязательное поле '" + fieldName
              + "' в конфигурационном файле: " + ARBITER_CONFIG_JSON_FILE;
      throw new IllegalStateException(message);
    }
  }

  private static void checkNotEmpty(Object value, String fieldName, String sectionName) {
    if (value instanceof String && ((String) value).isEmpty()) {
      String message = (sectionName != null)
              ? "Критическая ошибка: значение поля '" + fieldName
              + "' в секции '" + sectionName + "' в конфигурационном файле "
              + ARBITER_CONFIG_JSON_FILE + " не может быть пустым."
              : "Критическая ошибка: значение поля '" + fieldName
              + "' в конфигурационном файле " + ARBITER_CONFIG_JSON_FILE
              + " не может быть пустым.";
      throw new IllegalStateException(message);
    }
  }

  /**
   * Валидирует наличие и непустое значение поля
   */
  public static Object validateFieldNameAndValue(Object jsonObject, String fieldName) {
    checkNotNull(jsonObject, fieldName, null);
    Object existsFieldName = validateFieldName(jsonObject, fieldName);
    checkNotEmpty(existsFieldName, fieldName, null);
    return existsFieldName;
  }

  /**
   * Валидирует наличие поля (значение может быть пустым)
   */
  public static Object validateFieldName(Object jsonObject, String fieldName) {
    checkNotNull(jsonObject, fieldName, null);
    return jsonObject;
  }

  /**
   * Валидирует имя поля в секции и возвращает это имя
   */
  public static Object validateFieldNameInSection(Object jsonObject, String fieldName, String sectionName) {
    checkNotNull(jsonObject, fieldName, sectionName);
    return fieldName;
  }

  /**
   * Валидирует наличие и непустое значение поля в секции
   */
  public static Object validateFieldNameAndValueInSection(Object jsonObject, String fieldName, String sectionName) {
    checkNotNull(jsonObject, fieldName, sectionName);
    checkNotEmpty(jsonObject, fieldName, sectionName);
    return jsonObject;
  }

  /**
   * Валидирует наличие целочисленного поля в JsonObject
   */
  public static Integer validateFieldName(JsonObject config, String fieldName, Integer value) {
    if (!config.containsKey(fieldName)) {
      throw new IllegalStateException("Критическая ошибка: отсутствует обязательное поле '"
        + fieldName + "' в конфигурационном файле: " + ARBITER_CONFIG_JSON_FILE);
    }
    return value;
  }

  /**
   * Валидирует наличие целочисленного поля в JsonObject в секции
   */
  public static Integer validateFieldNameInSection(JsonObject config, String fieldName, Integer value, String sectionName) {
    if (!config.containsKey(fieldName)) {
      throw new IllegalStateException("Критическая ошибка: отсутствует обязательное поле '"
        + fieldName + "' в секции '"+ sectionName + "' в конфигурационном файле: " + ARBITER_CONFIG_JSON_FILE);
    }
    return value;
  }

  /**
   * Валидирует наличие и непустое значение поля
   * Валидирует UUID, проверяя его наличие и соответствие формату UUID
   */
  public static String validateFieldNameAndValueUuid(String fieldName, JsonObject config) {
    if (!config.containsKey(fieldName)) {
      throw new IllegalStateException("Критическая ошибка: отсутствует обязательное поле '"
        + fieldName + "' в конфигурационном файле: " + ARBITER_CONFIG_JSON_FILE);
    }
    String uuid = config.getString(fieldName);
    if(uuid != null && !uuid.isEmpty()) {
      if(!UUID_PATTERN.matcher(uuid).matches()){
        throw new IllegalArgumentException("Критическая ошибка: не валидный uuid '" + uuid + "' в поле '"
          + fieldName + "' в конфигурационном файле: " + ARBITER_CONFIG_JSON_FILE);
      }
      return uuid;
    }
    else {
      throw new IllegalStateException("Критическая ошибка: значение поля '"
        + fieldName + "' в конфигурационном файле " + ARBITER_CONFIG_JSON_FILE
        + " не может быть пустым.");
    }
  }

  /**
   * Валидирует наличие и непустое значение поля в секции
   * Валидирует UUID, проверяя его наличие и соответствие формату UUID в секции
   */
  public static String validateFieldNameAndValueUuidInSection(String fieldName, JsonObject config, String sectionName) {

    if (!config.containsKey(fieldName)) {
      throw new IllegalStateException("Критическая ошибка: отсутствует обязательное поле '"
        + fieldName + "' в секции '"+ sectionName + "' в конфигурационном файле: " + ARBITER_CONFIG_JSON_FILE);
    }
    String uuid = config.getString(fieldName);
    if(uuid != null && !uuid.isEmpty()) {
      if(!UUID_PATTERN.matcher(uuid).matches()){
        throw new IllegalArgumentException("Критическая ошибка: не валидный uuid '" + uuid + "' в поле '"
          + fieldName + "' в секции '"+ sectionName + "' в конфигурационном файле: " + ARBITER_CONFIG_JSON_FILE);
      }
      return uuid;
    }
    else {
      throw new IllegalStateException("Критическая ошибка: значение поля '"
        + fieldName + "' в секции '"+ sectionName + "' в конфигурационном файле " + ARBITER_CONFIG_JSON_FILE
        + " не может быть пустым.");
    }
  }

  /**
   * Валидирует переданное значение равно "да" или "нет" (без учёта регистра)
   */
  public static String validateBooleanValue(String value, String fieldName) {
    if (!value.equalsIgnoreCase("да") && !value.equalsIgnoreCase("нет")) {
      throw new IllegalStateException("Критическая ошибка: значение '" + value + "' для поля '"
        + fieldName + "' в конфигурационном файле " + ARBITER_CONFIG_JSON_FILE + " не валидно.");
    }
    return value;
  }
}
