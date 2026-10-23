package org.dcsa.conformance.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.lambda.auth.WebuiAuthentication;
import org.dcsa.conformance.lambda.auth.WebuiIdentity;
import org.dcsa.conformance.sandbox.ConformanceAccessException;
import org.dcsa.conformance.sandbox.ConformanceErrorResponses;
import org.dcsa.conformance.sandbox.ConformanceWebuiHandler;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;

import java.util.List;
import java.util.Map;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@Slf4j
public class WebuiLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private WebuiIdentity identity;

  public WebuiLambda() {
  }

  public WebuiLambda(WebuiIdentity identity) {
    this.identity = identity;
  }

  public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
    try {
      JsonNode jsonEvent = OBJECT_MAPPER.valueToTree(event);
      JsonNode claims = jsonEvent.path("requestContext").path("authorizer").path("jwt").path("claims");
      if (!claims.isObject()) return error(401, "Authentication required");
      if (identity == null) {
        try {
          identity = WebuiAuthentication.fromEnvironment();
        } catch (IllegalArgumentException e) {
          log.error("Web UI authentication configuration is incomplete or invalid");
          return error(503, "Authentication is not configured");
        }
      }
      String authorization = event.getHeaders() == null
        ? null
        : event.getHeaders().entrySet().stream()
        .filter(entry -> entry.getKey().equalsIgnoreCase("Authorization"))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
      String environmentId = identity.resolve(claims, authorization);
      JsonNode request = JsonToolkit.stringToJsonNode(event.getBody());

      if ("checkAccess".equals(request.path("operation").asText())) {
        return new APIGatewayProxyResponseEvent()
          .withStatusCode(200)
          .withHeaders(Map.of("Content-Type", JsonToolkit.JSON_UTF_8, "Cache-Control", "no-store"))
          .withBody("{}");
      }

      ConformancePersistenceProvider persistenceProvider = LambdaToolkit.createPersistenceProvider();

      ConformanceWebuiHandler webuiHandler = new ConformanceWebuiHandler(
        new WebuiAccessChecker(persistenceProvider),
        LambdaToolkit.getDbConfigValue(persistenceProvider, "environmentBaseUrl"),
        persistenceProvider,
        LambdaToolkit.createDeferredSandboxTaskConsumer(persistenceProvider)
      );

      String responseBody;
      try {
        responseBody = webuiHandler._doHandleRequest(environmentId, request).toPrettyString();
      } catch (ConformanceAccessException e) {
        return error(403, "Sandbox access denied");
      }

      return new APIGatewayProxyResponseEvent()
        .withMultiValueHeaders(
          Map.of(
            "Content-Type",
            List.of(JsonToolkit.JSON_UTF_8),
            "Cache-Control",
            List.of("no-store")))
        .withStatusCode(200)
        .withBody(responseBody);
    } catch (WebuiIdentity.Failure e) {
      return error(e.status(), e.getMessage());
    } catch (software.amazon.awssdk.core.exception.SdkException e) {
      return error(503, "Authentication storage temporarily unavailable");
    } catch (RuntimeException e) {
      return new APIGatewayProxyResponseEvent()
        .withMultiValueHeaders(
          Map.of(
            "Content-Type",
            List.of(JsonToolkit.JSON_UTF_8),
            "Cache-Control",
            List.of("no-store")))
        .withStatusCode(200)
        .withBody(
          ConformanceErrorResponses.webuiResponse(log, "handling the Web UI Lambda request", e)
            .toPrettyString());
    }
  }

  private static APIGatewayProxyResponseEvent error(int status, String message) {
    return new APIGatewayProxyResponseEvent()
      .withStatusCode(status)
      .withHeaders(Map.of("Content-Type", JsonToolkit.JSON_UTF_8, "Cache-Control", "no-store"))
      .withBody(OBJECT_MAPPER.createObjectNode().put("error", message).toString());
  }

  public static void main(String[] args) {
  } // unused
}
