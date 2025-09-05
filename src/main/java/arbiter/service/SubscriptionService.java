package arbiter.service;

import arbiter.config.AppConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.util.List;

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

  public void handleChangeSubscription(RoutingContext ctx) {
    try {
      String channelId = ctx.pathParam("channelId");
      String subscriptionId = ctx.pathParam("subscriptionId");
      String token = ctx.get("authToken");
      JsonObject requestBody = ctx.body().asJsonObject();

      if (requestBody == null) {
        sendError(ctx, 400, "Request body must be JSON");
        return;
      }

      List<String> addUids = extractUidsFromJsonArray(
        requestBody.getJsonArray("measurementValueToAddUids")
      );

      List<String> removeUids = extractUidsFromJsonArray(
        requestBody.getJsonArray("measurementValueToRemoveUids")
      );

      changeSubscription(channelId, subscriptionId, addUids, removeUids, token)
        .onSuccess(response -> {

          handleSuccess(ctx, 204, "Subscription updated successfully");
        })
        .onFailure(error -> {
          JsonObject errorResponse = new JsonObject()
            .put("error", "Failed to update subscription")
            .put("message", error.getMessage());

          System.err.println(errorResponse.encodePrettily());

          ctx.response()
                  .setStatusCode(500)
                  .putHeader("Content-Type", "application/json")
                  .end(errorResponse.encode());
        });

    } catch (Exception e) {
      sendError(ctx, 400, "Invalid request format: " + e.getMessage());
    }
  }

  public Future<JsonObject> changeSubscription(String channelId, String subscriptionId, List<String> add, List<String> remove, String token) {

    String url = String.format(AppConfig.getSubscriptionsChangeUrl(), channelId, subscriptionId);

    JsonObject requestBody = new JsonObject();
    JsonArray addArray = new JsonArray();
    JsonArray removeArray = new JsonArray();

    if (add != null) {
      for (String uid : add) {
        addArray.add(uid);
      }
    }

    if (remove != null) {
      for (String uid : remove) {
        removeArray.add(uid);
      }
    }

    requestBody.put("measurementValueToAddUids", addArray);
    requestBody.put("measurementValueToRemoveUids", removeArray);

    return webClient.patchAbs(url)
      .putHeader("Content-Type", "application/json")
      .putHeader("Authorization", "Bearer " + token)
      .sendJsonObject(requestBody)
      .compose(response -> {
        if (response.statusCode() == 204) {
          return Future.succeededFuture(new JsonObject());
        } else {
          System.err.println("responseBody PATCH statusCode: " + response.statusCode());
          return Future.failedFuture(String.format("HTTP %d: %s", response.statusCode(), url));
        }
      });
  }
}
