package org.dcsa.conformance.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.sandbox.ConformanceSandbox;
import org.dcsa.conformance.sandbox.ConformanceWebRequest;
import org.dcsa.conformance.sandbox.ConformanceWebResponse;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;

@Slf4j
public class ApiLambda
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public APIGatewayProxyResponseEvent handleRequest(
      final APIGatewayProxyRequestEvent event, final Context context) {
    try {
      System.out.println("event = " + event + ", context = " + context);
      JsonNode jsonEvent = new ObjectMapper().valueToTree(event);
      log.info("jsonEvent = " + jsonEvent.toPrettyString());

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
              LambdaToolkit.createAsyncWebClient(persistenceProvider));

      Map<String, List<String>> responseHeaders = conformanceWebResponse.getValueListHeaders();
      responseHeaders.put("Content-Type", List.of(conformanceWebResponse.contentType()));
      return new APIGatewayProxyResponseEvent()
          .withMultiValueHeaders(responseHeaders)
          .withStatusCode(conformanceWebResponse.statusCode())
          .withBody(conformanceWebResponse.body());
    } catch (RuntimeException | Error e) {
      log.error("Unhandled exception: " + e, e);
      throw e;
    }
  }

  public static void main(String[] args) {} // unused
}
