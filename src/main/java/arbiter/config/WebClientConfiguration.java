package arbiter.config;

import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Класс для настройки опции WEB клиентов
 */
public class WebClientConfiguration {

  private static final PemTrustOptions trustOptions = new PemTrustOptions()
    .addCertPath(AppConfig.getOikSertCrt());

  public static WebClientOptions createWebClientOptions() {
    return new WebClientOptions()
      .setKeepAlive(true)
      .setConnectTimeout(5000)
      .setSsl(true)
      .setTrustAll(false)
      .setTrustOptions(trustOptions)
      .setVerifyHost(false);
  }
}
