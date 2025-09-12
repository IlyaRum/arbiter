//package arbiter.controller;
//
//import arbiter.config.AppConfig;
//import arbiter.service.WebSocketService;
//import io.vertx.core.Vertx;
//import io.vertx.ext.web.Router;
//import io.vertx.ext.web.RoutingContext;
//
//public class WebSocketController extends ABaseController {
//
//  private final WebSocketService webSocketService;
//
//  public WebSocketController(Vertx vertx, WebSocketService webSocketService) {
//    super(vertx);
//    this.webSocketService = webSocketService;
//  }
//
//  @Override
//  public void registerRoutes(Router router) {
//    router.get(AppConfig.CORE_PREFIX + AppConfig.CHANNELS_OPEN)
//      .handler(this::getAndValidateToken)
//      .handler(this::connectToWebSocket);
//  }
//
//  public void connectToWebSocket(RoutingContext context) {
//    webSocketService.connectToWebSocketServer(context);
//  }
//
//  private void getAndValidateToken(RoutingContext context) {
//    webSocketService.getAndValidateToken(context);
//  }
//}
