package arbiter.controller;

import arbiter.config.AppConfig;
import arbiter.service.SubscriptionService;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class SubscriptionController extends ABaseController {

  SubscriptionService subscriptionService;

  public SubscriptionController(Vertx vertx, SubscriptionService subscriptionService) {
    super(vertx);
    this.subscriptionService = subscriptionService;
  }

  @Override
  public void registerRoutes(Router router) {
    router.route().handler(BodyHandler.create()
      .setHandleFileUploads(false)
      .setMergeFormAttributes(false));
    router.route().handler(ctx -> {
      System.out.println("Received " + ctx.request().method() + " " + ctx.request().path());
      System.out.println("Body: " + ctx.body().asString());
      ctx.next();
    });
//    router.post(AppConfig.MEASUREMENT_PREFIX + AppConfig.ADD_SUBSCRIPTION_BY_CHANNELID)
//      .handler(this::getAndValidateToken)
//      .handler(this::handleCreateSubscription);
//    router.patch(AppConfig.MEASUREMENT_PREFIX + AppConfig.CHANGE_SUBSCRIPTION)
//      .handler(this::getAndValidateToken)
//      .handler(this::handleChangeSubscription);
    router.delete(AppConfig.MEASUREMENT_PREFIX + AppConfig.DELETE_SUBSCRIPTION)
      .handler(this::getAndValidateToken)
      .handler(this::handleDeleteSubscription);
  }

//  private void handleCreateSubscription(RoutingContext routingContext) {
//    subscriptionService.handleCreateSubscription(routingContext);
//  }

  private void getAndValidateToken(RoutingContext context) {
    subscriptionService.getAndValidateToken(context);
  }

//  private void handleChangeSubscription(RoutingContext routingContext) {
//    subscriptionService.handleChangeSubscription(routingContext);
//  }

  private void handleDeleteSubscription(RoutingContext routingContext) {
    subscriptionService.handleDeleteSubscription(routingContext);
  }
}
