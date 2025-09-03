package arbiter.controller;

import arbiter.config.AppConfig;
import arbiter.service.SubscriptionService;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class SubscriptionController extends ABaseController {

  SubscriptionService subscriptionService;

  public SubscriptionController(Vertx vertx, SubscriptionService subscriptionService) {
    super(vertx);
    this.subscriptionService = subscriptionService;
  }

  @Override
  public void registerRoutes(Router router) {
    router.post(AppConfig.MEASUREMENT_PREFIX + "/subscriptions/:channelId")
      .handler(this::handleCreateSubscription);
  }

  private void handleCreateSubscription(RoutingContext routingContext) {
    subscriptionService.handleCreateSubscription(routingContext);
  }


}
