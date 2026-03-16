package arbiter.config;

import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Класс для настройки опции WEB клиентов
 */
public class WebClientConfiguration {

  private static final PemTrustOptions trustOptions = new PemTrustOptions()
    .addCertPath(AppConfig.getOikCertCrt());

  private static final boolean TRUST_ALL = AppConfig.isTrustAll();

  public static WebClientOptions createWebClientOptions() {
    WebClientOptions options = new WebClientOptions()
      .setKeepAlive(true)
      .setConnectTimeout(5000);
    if (TRUST_ALL) {
      options
        .setSsl(true)
        .setTrustAll(true)
        .setVerifyHost(false);
    } else {
      options
        .setSsl(true)
        .setTrustAll(false)
        .setTrustOptions(trustOptions)
        .setVerifyHost(false);
    }

    return options;
  }
}
