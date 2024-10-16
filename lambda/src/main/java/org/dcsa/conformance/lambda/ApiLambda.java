package org.dcsa.conformance.lambda;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.dcsa.conformance.sandbox.ConformanceSandbox;
import org.dcsa.conformance.sandbox.ConformanceWebRequest;
import org.dcsa.conformance.sandbox.ConformanceWebResponse;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;
import software.amazon.lambda.powertools.logging.Logging;

@Log4j2
public class ApiLambda
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  @Logging
  public APIGatewayProxyResponseEvent handleRequest(
      final APIGatewayProxyRequestEvent event, final Context context) {
    try {
      log.info("event = {}", event);
      JsonNode jsonEvent = OBJECT_MAPPER.valueToTree(event);
      log.debug("jsonEvent = {}", jsonEvent);

      ConformancePersistenceProvider persistenceProvider =
          LambdaToolkit.createPersistenceProvider();

      ConformanceWebResponse conformanceWebResponse =
          ConformanceSandbox.handleRequest(
              persistenceProvider,
              new ConformanceWebRequest(
                  event.getHttpMethod(),
                  LambdaToolkit.getDbConfigValue(persistenceProvider, "environmentBaseUrl")
                      + event.getPath(),
                  Objects.requireNonNullElse(
                      event.getMultiValueQueryStringParameters(), Collections.emptyMap()),
                  event.getMultiValueHeaders(),
                  event.getBody()),
              LambdaToolkit.createDeferredSandboxTaskConsumer(persistenceProvider));

      Map<String, List<String>> responseHeaders = conformanceWebResponse.getValueListHeaders();
      responseHeaders.put("Content-Type", List.of(conformanceWebResponse.contentType()));
      return new APIGatewayProxyResponseEvent()
          .withMultiValueHeaders(responseHeaders)
          .withStatusCode(conformanceWebResponse.statusCode())
          .withBody(conformanceWebResponse.body());
    } catch (RuntimeException | Error e) {
      log.error("Unhandled exception", e);
      throw e;
    }
  }

  public static void main(String[] args) {} // unused
}
