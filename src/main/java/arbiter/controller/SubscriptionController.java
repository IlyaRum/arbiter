package arbiter.controller;

import arbiter.config.AppConfig;
import arbiter.di.DependencyInjector;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class SubscriptionController extends ABaseController {

  private final DependencyInjector dependencyInjector;

  public SubscriptionController(Vertx vertx, DependencyInjector dependencyInjector) {
    super(vertx);
    this.dependencyInjector = dependencyInjector;
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
    router.post(AppConfig.MEASUREMENT_PREFIX + AppConfig.ADD_SUBSCRIPTION_BY_CHANNELID)
      .handler(this::getAndValidateToken)
      .handler(this::handleCreateSubscription);
    router.patch(AppConfig.MEASUREMENT_PREFIX + AppConfig.CHANGE_SUBSCRIPTION)
      .handler(this::getAndValidateToken)
      .handler(this::handleChangeSubscription);
    router.delete(AppConfig.MEASUREMENT_PREFIX + AppConfig.DELETE_SUBSCRIPTION)
      .handler(this::getAndValidateToken)
      .handler(this::handleDeleteSubscription);
  }

  private void handleCreateSubscription(RoutingContext routingContext) {
    dependencyInjector.getSubscriptionService().handleCreateSubscription(routingContext);
  }

  private void getAndValidateToken(RoutingContext context) {
    dependencyInjector.getTokenService().getTokenForContext(context);
  }

  private void handleChangeSubscription(RoutingContext routingContext) {
    dependencyInjector.getSubscriptionService().handleChangeSubscription(routingContext);
  }

  private void handleDeleteSubscription(RoutingContext routingContext) {
    dependencyInjector.getSubscriptionService().handleDeleteSubscription(routingContext);
  }
}
