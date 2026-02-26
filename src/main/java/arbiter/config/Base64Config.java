package arbiter.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Base64Config {

  /**
   * Кодирует строку в формат Base64
   * @param credentials простая строка
   * @return закодированное значение
   */
  public static String encodeToBase64(String credentials){
    return Base64.getEncoder()
      .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Декодирует строку из формата Base64
   * @param encodedCredentials закодированная строка
   * @return декодированное значение
   */
  public static String decodeFromBase64(String encodedCredentials){
    byte[] decodedBytes = Base64.getDecoder().decode(encodedCredentials);
    return new String(decodedBytes, StandardCharsets.UTF_8);
  }
}
