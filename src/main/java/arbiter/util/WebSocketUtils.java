package arbiter.util;

import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

public class WebSocketUtils {

  private static final Logger logger = LoggerFactory.getLogger(WebSocketUtils.class);

  public static String buildWebSocketUrl(WebSocketConnectOptions options) {
    String protocol = options.isSsl() ? "wss" : "ws";
    String host = options.getHost();
    int port = options.getPort();
    String uri = options.getURI();

    // Стандартные порты можно не указывать в URL
    String portPart = "";
    if ((options.isSsl() && port != 443) || (!options.isSsl() && port != 80)) {
      portPart = ":" + port;
    }

    return protocol + "://" + host + portPart + uri;
  }

  public static void logConnectionDetails(WebSocketConnectOptions options) {
    String url = buildWebSocketUrl(options);
    //String protocols = String.join(", ", options.getSubProtocols());

    logger.info("WebSocket connection details:");
    logger.info("URL: " + url);
    logger.info("Host: " + options.getHost());
    logger.info("Port: " + options.getPort());
    logger.info("SSL: " + options.isSsl());
    logger.info("URI: " + options.getURI());
    //logger.info("Protocols: " + protocols);
    logger.info("Headers: " + options.getHeaders());
  }
}
