package arbiter.initialization;

import arbiter.di.DependencyInjector;
import io.vertx.core.internal.logging.Logger;

import java.util.concurrent.CompletableFuture;

public class WebSocketManager {
  private final DependencyInjector dependencyInjector;
  private final Logger logger;
  private String channelId;

  public WebSocketManager(DependencyInjector dependencyInjector, Logger logger) {
    this.dependencyInjector = dependencyInjector;
    this.logger = logger;
  }

  public CompletableFuture<String> connect(String token) {
    return dependencyInjector.getWebSocketService()
      .connectToWebSocketServer(token)
      .toCompletionStage()
      .toCompletableFuture()
      .thenApply(jsonObject -> {
        this.channelId = jsonObject.getString("subject");
        logger.info("channelId: " + channelId);
        return channelId;
      });
  }

  public String getChannelId() {
    return channelId;
  }
}
