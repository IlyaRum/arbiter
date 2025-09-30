package arbiter.controller;

import arbiter.config.AppConfig;
import arbiter.di.DependencyInjector;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class EventSubscriptionController  extends ABaseController{

  private final DependencyInjector dependencyInjector;

  public EventSubscriptionController(Vertx vertx, DependencyInjector dependencyInjector) {
    super(vertx);
    this.dependencyInjector = dependencyInjector;
  }

  @Override
  public void registerRoutes(Router router) {
    router.post(AppConfig.CORE_PREFIX + AppConfig.ADD_EVENT_SUBSCRIPTION)
      .handler(this::getAndValidateToken)
      .handler(this::handleAddEventSubscription);
  }

  private void getAndValidateToken(RoutingContext context) {
    dependencyInjector.getTokenService().getTokenForContext(context);
  }

  private void handleAddEventSubscription(RoutingContext context) {
    dependencyInjector.getEventSubscriptionService().handleAddEventSubscription(context);
  }

}
