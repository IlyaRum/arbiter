package arbiter.controller;

import arbiter.config.AppConfig;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthCheckControllerTest {

  @Mock
  private Vertx vertx;

  @Mock
  private Router router;

  @Mock
  private Route route;

  @Mock
  private RoutingContext routingContext;

  @Mock
  private HttpServerResponse httpServerResponse;

  private HealthCheckController healthCheckController;

  @BeforeEach
  void setUp() {
    healthCheckController = new HealthCheckController(vertx);
  }

  @Test
  void registerRoutes_ShouldRegisterHealthCheckRoute() {
    when(router.get(AppConfig.HEALTH)).thenReturn(route);
    when(route.handler(any())).thenReturn(route);

    healthCheckController.registerRoutes(router);

    verify(router, times(1)).get(AppConfig.HEALTH);
    verify(route, times(1)).handler(any());
  }

  @Test
  void handleHealthCheckRequest_ShouldReturn200WithUpStatus() {
    when(routingContext.response()).thenReturn(httpServerResponse);
    when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
    when(httpServerResponse.putHeader(anyString(), anyString())).thenReturn(httpServerResponse);

    invokePrivateHealthCheckMethod(routingContext);

    verify(routingContext, times(1)).response();
    verify(httpServerResponse, times(1)).setStatusCode(200);
    verify(httpServerResponse, times(1)).putHeader("content-type", "text/plain");
    verify(httpServerResponse, times(1)).end("UP");
  }

  private void invokePrivateHealthCheckMethod(RoutingContext routingContext) {
    try {
      java.lang.reflect.Method method = HealthCheckController.class.getDeclaredMethod(
        "handleHealthCheckRequest", RoutingContext.class);
      method.setAccessible(true);
      method.invoke(healthCheckController, routingContext);
    } catch (Exception e) {
      throw new RuntimeException("Failed to invoke private method", e);
    }
  }
}
