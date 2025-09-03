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
    String token = ctx.get("authToken");



    if (token == null) {
      String errorMsg = "Token is required";
      System.err.println("error: " + errorMsg);
      handleError(ctx, new IllegalArgumentException(errorMsg));
    }

    createSubscription(ctx, channelId, token)
      .onSuccess(response -> {

        ctx.response()
          .setStatusCode(201)
          .putHeader("Content-Type", "application/json")
          .end(response.encode());
      })
      .onFailure(error -> {
        JsonObject errorResponse = new JsonObject()
          .put("error", "Failed to add subscription")
          .put("message", error.getMessage());

        System.err.println(errorResponse.encodePrettily());

        ctx.response()
          .setStatusCode(500)
          .putHeader("Content-Type", "application/json")
          .end(errorResponse.encode());
      });
  }

  private Future<JsonObject> createSubscription(RoutingContext ctx, String channelId, String token) {
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
          return Future.succeededFuture(responseBody);
        } else {
          //sendError(ctx, response.statusCode(), "Add subscriptions to " + url + " failed");
          return Future.failedFuture(String.format("HTTP %d: %s", response.statusCode(), url));
        }
      });
  }
}
