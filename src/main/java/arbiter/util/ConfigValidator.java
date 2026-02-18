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

  private ConfigValidator() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * Валидирует наличие и непустое значение поля
   */
  public static Object validateFieldNameAndValue(Object jsonObject, String fieldName) {
    if (jsonObject == null) {
      throw new IllegalStateException("Критическая ошибка: отсутствует обязательное поле '"
        + fieldName + "' в конфигурационном файле: " + AppConfig.getArbiterConfigJsonFile());
    }
    if (jsonObject instanceof String && ((String) jsonObject).isEmpty()) {
      throw new IllegalStateException("Критическая ошибка: значение поля '"
        + fieldName + "' в конфигурационном файле " + AppConfig.getArbiterConfigJsonFile()
        + " не может быть пустым.");
    }
    return jsonObject;
  }

  /**
   * Валидирует наличие поля (значение может быть пустым)
   */
  public static Object validateFieldName(Object jsonObject, String fieldName) {
    if (jsonObject == null) {
      throw new IllegalStateException("Критическая ошибка: отсутствует обязательное поле '"
        + fieldName + "' в конфигурационном файле: " + AppConfig.getArbiterConfigJsonFile());
    }
    return jsonObject;
  }

  /**
   * Валидирует наличие поля (значение может быть пустым) в секции
   */
  public static Object validateFieldNameInSection(Object jsonObject, String fieldName, String sectionName) {
    if (jsonObject == null) {
      throw new IllegalStateException("Критическая ошибка: отсутствует обязательное поле '"
        + fieldName + "' в секции '"+ sectionName + "' в конфигурационном файле: " + AppConfig.getArbiterConfigJsonFile());
    }
    return jsonObject;
  }

  /**
   * Валидирует имя поля в секции и возвращает это имя
   */
  public static Object validateFieldName(Object jsonObject, String fieldName, String sectionName) {
    if (jsonObject == null) {
      throw new IllegalStateException("Критическая ошибка: отсутствует обязательное поле '"
        + fieldName + "' в секции '"+ sectionName + "' в конфигурационном файле: " + AppConfig.getArbiterConfigJsonFile());
    }
    return fieldName;
  }

  /**
   * Валидирует наличие и непустое значение поля в секции
   */
  public static Object validateFieldNameAndValueInSection(Object jsonObject, String fieldName, String sectionName) {
    if (jsonObject == null) {
      throw new IllegalStateException("Критическая ошибка: отсутствует обязательное поле '"
        + fieldName + "' в секции '"+ sectionName + "' в конфигурационном файле: " + AppConfig.getArbiterConfigJsonFile());
    }
    if (jsonObject instanceof String && ((String) jsonObject).isEmpty()) {
      throw new IllegalStateException("Критическая ошибка: значение поля '"
        + fieldName + "' в секции '"+ sectionName + "' в конфигурационном файле " + AppConfig.getArbiterConfigJsonFile()
        + " не может быть пустым.");
    }
    return jsonObject;
  }

  /**
   * Валидирует наличие целочисленного поля в JsonObject
   */
  public static Integer validateFieldName(JsonObject config, String fieldName, Integer value) {
    if (!config.containsKey(fieldName)) {
      throw new IllegalStateException("Критическая ошибка: отсутствует обязательное поле '"
        + fieldName + "' в конфигурационном файле: " + AppConfig.getArbiterConfigJsonFile());
    }
    return value;
  }

  /**
   * Валидирует наличие целочисленного поля в JsonObject в секции
   */
  public static Integer validateFieldNameInSection(JsonObject config, String fieldName, Integer value, String sectionName) {
    if (!config.containsKey(fieldName)) {
      throw new IllegalStateException("Критическая ошибка: отсутствует обязательное поле '"
        + fieldName + "' в секции '"+ sectionName + "' в конфигурационном файле: " + AppConfig.getArbiterConfigJsonFile());
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
        + fieldName + "' в конфигурационном файле: " + AppConfig.getArbiterConfigJsonFile());
    }
    String uuid = config.getString(fieldName);
    if(uuid != null && !uuid.isEmpty()) {
      if(!UUID_PATTERN.matcher(uuid).matches()){
        throw new IllegalArgumentException("Критическая ошибка: не валидный uuid '" + uuid + "' в поле '"
          + fieldName + "' в конфигурационном файле: " + AppConfig.getArbiterConfigJsonFile());
      }
      return uuid;
    }
    else {
      throw new IllegalStateException("Критическая ошибка: значение поля '"
        + fieldName + "' в конфигурационном файле " + AppConfig.getArbiterConfigJsonFile()
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
        + fieldName + "' в секции '"+ sectionName + "' в конфигурационном файле: " + AppConfig.getArbiterConfigJsonFile());
    }
    String uuid = config.getString(fieldName);
    if(uuid != null && !uuid.isEmpty()) {
      if(!UUID_PATTERN.matcher(uuid).matches()){
        throw new IllegalArgumentException("Критическая ошибка: не валидный uuid '" + uuid + "' в поле '"
          + fieldName + "' в секции '"+ sectionName + "' в конфигурационном файле: " + AppConfig.getArbiterConfigJsonFile());
      }
      return uuid;
    }
    else {
      throw new IllegalStateException("Критическая ошибка: значение поля '"
        + fieldName + "' в секции '"+ sectionName + "' в конфигурационном файле " + AppConfig.getArbiterConfigJsonFile()
        + " не может быть пустым.");
    }
  }

  /**
   * Валидирует переданное значение равно "да" или "нет" (без учёта регистра)
   */
  public static String validateBooleanValue(String value, String fieldName) {
    if (!value.equalsIgnoreCase("да") && !value.equalsIgnoreCase("нет")) {
      throw new IllegalStateException("Критическая ошибка: значение '" + value + "' для поля '"
        + fieldName + "' в конфигурационном файле " + AppConfig.getArbiterConfigJsonFile() + " не валидно.");
    }
    return value;
  }
}
