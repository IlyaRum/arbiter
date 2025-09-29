package arbiter.initialization;

import arbiter.di.DependencyInjector;
import arbiter.data.UnitCollection;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

public class SubscriptionManager {
  private final DependencyInjector dependencyInjector;
  private UnitCollection data;

  public SubscriptionManager(DependencyInjector dependencyInjector) {
    this.dependencyInjector = dependencyInjector;
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

  public void setData(UnitCollection data) {
    this.data = data;
  }
}
