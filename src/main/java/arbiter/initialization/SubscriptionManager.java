package arbiter.initialization;

import arbiter.di.DependencyInjector;
import data.UnitCollection;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

public class SubscriptionManager {
  private final DependencyInjector dependencyInjector;
  private final Logger logger;
  private final UnitCollection data;

  public SubscriptionManager(DependencyInjector dependencyInjector, Logger logger, UnitCollection data) {
    this.dependencyInjector = dependencyInjector;
    this.logger = logger;
    this.data = data;
  }

  public CompletableFuture<JsonObject> createSubscription(String channelId, String token) {
    return dependencyInjector.getSubscriptionService()
      .createSubscription(channelId, token)
      .toCompletionStage()
      .toCompletableFuture();
  }

  public CompletableFuture<JsonObject> changeSubscription(String channelId, String subscriptionId, String token) {
    return dependencyInjector.getSubscriptionService()
      .changeSubscription(channelId, subscriptionId, data.getUIDs(), null, token)
      .toCompletionStage()
      .toCompletableFuture();
  }
}
