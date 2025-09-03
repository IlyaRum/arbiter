package arbiter.service;

import arbiter.config.AppConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class SubscriptionService extends ABaseService {

  private final WebClient webClient;

  public SubscriptionService(Vertx vertx) {
    super(vertx);
    WebClientOptions options = new WebClientOptions()
      .setKeepAlive(true)
      .setConnectTimeout(5000)
      .setSsl(true)
      .setTrustAll(true) //отключает проверку сертификатов
      .setVerifyHost(false); //Отключает проверку hostname

    this.webClient = WebClient.wrap(vertx.createHttpClient(options));
  }

  public void handleCreateSubscription(RoutingContext ctx) {
    String channelId = ctx.pathParam("channelId");
    String token = /*ctx.get("authToken")*/ "LfoTeu5U1qelbjAvzFEehCba6FkZzfUyL2sXO_NBAs8JkHQa8LzN_1hxs8tlO7Jv03_5qXtYw9SGMXHkzshDMfYsY-AS0RdvQ1mEpmZ0CSW8GarRzBN2XW5XEKhJ48MGqOSeqwokBzOoz98VYyna2Cj3J423xQAqFExPWrYAs_ODkGwCuCow7Sou7-zIRFQVUqvfCcJzD6nloYa3VV9FVNqtt2wZTbB5vendk1LYU0NK3kkUuJQYsjj5kLm9EGMWPVGJIKspmgbAj5mROtESDOmya4sq3uvmjAx3PtCS98vZ6L8J7o1sqasEIrqxx4BnE64KZYNBd8HBrst9F_Vz5A";

    createSubscription(ctx, channelId, token)
      .onSuccess(subscriptionId -> {
        JsonObject response = new JsonObject()
          .put("subscriptionId", subscriptionId)
          .put("channelId", channelId)
          .put("status", "created");

        System.out.println(response.encodePrettily());

        ctx.response()
          .setStatusCode(201)
          .putHeader("Content-Type", "application/json")
          .end(response.encode());
      })
      .onFailure(error -> {
        JsonObject errorResponse = new JsonObject()
          .put("error", "Failed to create subscription")
          .put("message", error.getMessage())
          .put("channelId", channelId);

        System.err.println(errorResponse.encodePrettily());

        ctx.response()
          .setStatusCode(500)
          .putHeader("Content-Type", "application/json")
          .end(errorResponse.encode());
      });
  }

  private Future<String> createSubscription(RoutingContext ctx, String channelId, String token) {
    String url = String.format(AppConfig.getSubscriptionsAddUrl(), channelId);

    System.out.println(url);

    JsonObject json = new JsonObject()
      .put("subscriptionType", "actual")
      .put("valueType", "numeric");

    return webClient.postAbs(url)
      .putHeader("Content-Type", "application/json")
      .putHeader("Authorization", "Bearer " + token)
      .sendJsonObject(json)
      .compose(response -> {
        if (response.statusCode() == 201) {
          JsonObject responseBody = response.bodyAsJsonObject();
          System.out.println(responseBody.encodePrettily());
          JsonObject valueObject = responseBody.getJsonObject("value");
          String subscriptionId = valueObject.getString("subscriptionId");
          return Future.succeededFuture(subscriptionId);
        } else {
          sendError(ctx, response.statusCode(), "Add subscriptions to " + url + " failed");
          return Future.failedFuture(String.format("HTTP %d: %s", response.statusCode(), response.bodyAsString()));
        }
      });
  }
}
