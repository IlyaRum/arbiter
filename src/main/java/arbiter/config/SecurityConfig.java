package arbiter.config;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class SecurityConfig {

  /**
   * Признак зашифрованного пароля
   */
  private static final String CIPHER_MARK = "{cipher}";

  private static final int ENCODE = -1;
  private static final int DECODE = 1;

  /**
   * Код, задающий смещения для шифрования
   */
  private static final String SECRET = "WXRCPOZUO_NR_Y";

  /**
   * Расчитанные смещения
   */
  private static final int[] SHIFT = new int[SECRET.length()];

  /**
   * Кодировка фиксированная - 1251
   */
  private static final Charset CHARSET = Charset.forName("cp1251");

  /**
   * Смещение для кода
   */
  private static final int SECRET_SHIFT = 63;

  static {
    char[] secretArray = SECRET.toCharArray();
    for (int i = 0; i < secretArray.length; i++) {
      SHIFT[i] = (int)secretArray[i] - SECRET_SHIFT;
    }
  }

  /**
   * Декодировать значение пароля
   *
   * @param password пароль
   * @return раскодированное значение или исходное
   */
  public static String decodePassword(String password) {
    return isEncoded(password) ? decode(password.replace(CIPHER_MARK, "")) : password;
  }

  /**
   * Закодировать значение пароля
   *
   * @param password пароль
   * @return закодированное значение или исходное
   */
  public static String encodePassword(String password) {
    return isEncoded(password) ? password : CIPHER_MARK + encode(password);
  }

  /**
   * Проверить признак закодированного значения пароля
   *
   * @param value значение
   * @return наличие признака закодированного значения
   */
  public static boolean isEncoded(String value) {
    return value.startsWith(CIPHER_MARK);
  }

  /**
   * Раскодировать шифр в значение
   * @param value шифр
   * @return значение
   */
  public static String decode(String value)
  {
    return coding(value, DECODE);
  }

  /**
   * Закодировать значение
   * @param value значение
   * @return закодированный шифр
   */
  public static String encode(String value)
  {
    return coding(value, ENCODE);
  }

  private static String coding(String value, int direction)
  {
    if (value == null || value.trim().isEmpty()) {
      return value;
    }
    byte[] bytes = CHARSET.encode(value).array();
    byte[] resultBytes = new byte[bytes.length];
    for (int i = 0; i < bytes.length; i++)
    {
      resultBytes[i] = (byte)(bytes[i] + direction * SHIFT[i % SECRET.length()]);
    }
    CharBuffer decode = CHARSET.decode(ByteBuffer.wrap(resultBytes));
    return decode.toString();
  }

}
