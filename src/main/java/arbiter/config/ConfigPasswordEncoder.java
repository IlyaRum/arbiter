package arbiter.config;

import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

import static arbiter.constants.UnitCollectionConstants.CONFIG_KEY_OIK;
import static arbiter.constants.UnitCollectionConstants.CONFIG_KEY_PASSWORD;
import static arbiter.util.ConfigValidator.validateFieldName;

/**
 * Обработчик кодирования паролей в конфигурационных файлах.
 */
public class ConfigPasswordEncoder {
  private static final Logger logger = LoggerFactory.getLogger(ConfigPasswordEncoder.class);
  private final ConfigFileManager fileManager;

  public ConfigPasswordEncoder(ConfigFileManager fileManager) {
    this.fileManager = fileManager;
  }

  public JsonObject processPasswordEncoding(String configFile, JsonObject config) {
    JsonObject oikField = config.getJsonObject(CONFIG_KEY_OIK);
    if (oikField != null) {
      String password = (String) validateFieldName(oikField.getString(CONFIG_KEY_PASSWORD), CONFIG_KEY_PASSWORD);;
      if (password != null && !password.isEmpty() && !SecurityConfig.isEncoded(password)) {
        return encodeAndSavePassword(configFile, config, oikField, password);
      }
    }
    return config;
  }

  private JsonObject encodeAndSavePassword(String configFile, JsonObject config, JsonObject oikField, String plainPassword) {
    String encodedPassword = SecurityConfig.encodePassword(plainPassword);
    logger.info("Encoded password: '" + encodedPassword + "'");
    oikField.put(CONFIG_KEY_PASSWORD, encodedPassword);
    fileManager.writeConfigFile(configFile, config);
    logger.info("Password in " + configFile + " has been overwritten successfully");
    return config;
  }
}
