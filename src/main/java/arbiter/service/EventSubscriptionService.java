package arbiter.service;

import arbiter.config.AppConfig;
import arbiter.di.DependencyInjector;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class EventSubscriptionService extends ABaseService{

  private final DependencyInjector dependencyInjector;
  private final WebClient webClient;
  private static final Logger logger = LoggerFactory.getLogger(EventSubscriptionService.class);


  public EventSubscriptionService(Vertx vertx, DependencyInjector dependencyInjector) {
    super(vertx);
    this.dependencyInjector = dependencyInjector;

    WebClientOptions options = new WebClientOptions()
      .setKeepAlive(true)
      .setConnectTimeout(5000)
      .setSsl(true)
      .setTrustAll(true) //отключает проверку сертификатов
      .setVerifyHost(false); //Отключает проверку hostname

    this.webClient = WebClient.wrap(vertx.createHttpClient(options));
  }

  public void handleAddEventSubscription(RoutingContext context) {

    String channelId = context.pathParam("channelId");
    String token = context.get("authToken");

    JsonObject requestBody = context.body().asJsonObject();

    if (requestBody == null) {
      sendError(context, 400, "Request body must be JSON");
      return;
    }

    List<String> types = extractFromJsonArray(requestBody.getJsonArray("eventTypes"));

    addEventSubscription(channelId, types, token)
    .onSuccess(response -> {
      handleSuccess(context, 204, "Event Subscription added successfully");
    })
      .onFailure(error -> {
        JsonObject errorResponse = new JsonObject()
          .put("error", "Failed to add event subscription")
          .put("message", error.getMessage());

        System.err.println(errorResponse.encodePrettily());

        context.response()
          .setStatusCode(500)
          .putHeader("Content-Type", "application/json")
          .end(errorResponse.encode());
      });
  }

  public CompletionStage<Object> addEventSubscription(DependencyInjector dependencyInjector, String token, JsonObject subscriptionResult) {
    String channelId = subscriptionResult.getString("channelId");
    String eventUID = dependencyInjector.getUnitCollection().getEventUID();
    List<String> eventTypes = new ArrayList<>();
    eventTypes.add("ru.monitel.ck11.rt-events." + eventUID + ".v1");
    logger.debug("channelId: " + channelId + ", eventTypes: " + eventTypes);
    return dependencyInjector.getEventSubscriptionService().addEventSubscription(channelId, eventTypes, token).toCompletionStage();
  }


  private Future<Object> addEventSubscription(String channelId, List<String> types, String token) {
    String url = String.format(AppConfig.getEventSubscriptionsAddUrl(), channelId);

    logger.debug(url);

    JsonArray eventTypes = new JsonArray();
    JsonObject requestBody = new JsonObject();

    if (types != null) {
      for (String type : types) {
        eventTypes.add(type);
      }
    }

    requestBody.put("eventTypes", eventTypes);

    return webClient.postAbs(url)
      .putHeader("Content-Type", "application/json")
      .putHeader("Authorization", "Bearer " + token)
      .sendJsonObject(requestBody)
      .compose(response -> {
        if (response.statusCode() == 204) {
          return Future.succeededFuture(new JsonObject());
        } else {
          logger.error("responseBody POST statusCode: " + response.statusCode());
          return Future.failedFuture(String.format("HTTP %d: %s", response.statusCode(), url));
        }
      });
  }

  }
