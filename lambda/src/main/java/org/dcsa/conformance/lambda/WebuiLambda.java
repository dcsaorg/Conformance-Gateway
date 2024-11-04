package org.dcsa.conformance.lambda;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.sandbox.ConformanceAccessException;
import org.dcsa.conformance.sandbox.ConformanceWebuiHandler;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;

@Slf4j
public class WebuiLambda
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public APIGatewayProxyResponseEvent handleRequest(
      final APIGatewayProxyRequestEvent event, final Context context) {
    try {
      System.out.println("event = " + event + ", context = " + context);
      log.info("event.getPath() = {}", event.getPath());

      JsonNode jsonEvent = OBJECT_MAPPER.valueToTree(event);
      log.info("JSON event = {}", jsonEvent.toString());

      String cognitoIdAsEnvironmentId =
          jsonEvent
              .get("requestContext")
              .get("authorizer")
              .get("jwt")
              .get("claims")
              .get("cognito:username")
              .asText();
      log.info("cognitoIdAsEnvironmentId='{}'", cognitoIdAsEnvironmentId);

      ConformancePersistenceProvider persistenceProvider =
          LambdaToolkit.createPersistenceProvider();

      ConformanceWebuiHandler webuiHandler =
          new ConformanceWebuiHandler(
              new WebuiAccessChecker(persistenceProvider),
              LambdaToolkit.getDbConfigValue(persistenceProvider, "environmentBaseUrl"),
              persistenceProvider,
              LambdaToolkit.createDeferredSandboxTaskConsumer(persistenceProvider));

      String responseBody;
      try {
        responseBody =
            webuiHandler
                .handleRequest(
                    cognitoIdAsEnvironmentId, JsonToolkit.stringToJsonNode(event.getBody()))
                .toPrettyString();
      } catch (ConformanceAccessException e) {
        return new APIGatewayProxyResponseEvent()
            .withMultiValueHeaders(Map.of("Content-Type", List.of(JsonToolkit.JSON_UTF_8)))
            .withStatusCode(403)
            .withBody("Access denied");
      }

      return new APIGatewayProxyResponseEvent()
          .withMultiValueHeaders(Map.of("Content-Type", List.of(JsonToolkit.JSON_UTF_8)))
          .withStatusCode(200)
          .withBody(responseBody);
    } catch (RuntimeException | Error e) {
      log.error("Unhandled exception: {}", e, e);
      throw e;
    }
  }

  public static void main(String[] args) {} // unused
}
