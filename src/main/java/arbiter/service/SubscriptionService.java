package arbiter.service;

import arbiter.config.AppConfig;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.util.List;

public class SubscriptionService extends ABaseService {

  private final WebClient webClient;
  private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);


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
      logger.error("error: " + errorMsg);
      handleError(ctx, new IllegalArgumentException(errorMsg));
    }

    createSubscription(channelId, token)
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

        logger.error(errorResponse.encodePrettily());

        ctx.response()
          .setStatusCode(500)
          .putHeader("Content-Type", "application/json")
          .end(errorResponse.encode());
      });
  }

  public Future<JsonObject> createSubscription(String channelId, String token) {
    String url = String.format(AppConfig.getSubscriptionsAddUrl(), channelId);

    logger.debug(url);

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

      List<String> addUids = extractFromJsonArray(
        requestBody.getJsonArray("measurementValueToAddUids")
      );

      List<String> removeUids = extractFromJsonArray(
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

    logger.debug(url);

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
          logger.error("responseBody PATCH statusCode: " + response.statusCode());
          return Future.failedFuture(String.format("HTTP %d: %s", response.statusCode(), url));
        }
      });
  }

  public void handleDeleteSubscription(RoutingContext ctx) {
    String channelId = ctx.pathParam("channelId");
    String subscriptionId = ctx.pathParam("subscriptionId");
    String token = ctx.get("authToken");

    if (channelId == null || channelId.isEmpty() ||
      subscriptionId == null || subscriptionId.isEmpty()) {
      sendBadRequest(ctx, "Channel ID is required");
      return;
    }

    deleteSubscription(channelId, subscriptionId, token)
      .onSuccess(voidResult -> {
        ctx.response()
          .setStatusCode(204)
          .end();
        System.out.println("Subscription deleted successfully for channel: " + channelId);
      })
      .onFailure(error -> {
        JsonObject errorResponse = new JsonObject()
          .put("error", "Failed to delete subscription")
          .put("message", error.getMessage());

        logger.error(errorResponse.encodePrettily());

        ctx.response()
          .setStatusCode(500)
          .putHeader("Content-Type", "application/json")
          .end(errorResponse.encode());
      });
  }

  public Future<Void> deleteSubscription(String channelId, String subscriptionId, String token) {

    String url = String.format(AppConfig.getSubscriptionsDeleteUrl(), channelId, subscriptionId);

    logger.debug(url);

    return webClient.deleteAbs(url)
      .putHeader("Content-Type", "application/json")
      .putHeader("Authorization", "Bearer " + token)
      .send()
      .compose(response -> {
        if (response.statusCode() == 204) {
          return Future.succeededFuture();
        } else {
          logger.error("responseBody DELETE statusCode: " + response.statusCode());
          return Future.failedFuture(String.format("HTTP %d: %s", response.statusCode(), url));
        }
      });
  }

  private void sendBadRequest(RoutingContext ctx, String message) {
    ctx.response()
      .setStatusCode(400)
      .end(new JsonObject()
        .put("error", message)
        .encode());
  }
}
