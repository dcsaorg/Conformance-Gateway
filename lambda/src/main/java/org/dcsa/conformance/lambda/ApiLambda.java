package org.dcsa.conformance.lambda;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.sandbox.ConformanceSandbox;
import org.dcsa.conformance.sandbox.ConformanceWebRequest;
import org.dcsa.conformance.sandbox.ConformanceWebResponse;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;

@Slf4j
public class ApiLambda
    implements RequestHandler<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent> {

  public ApplicationLoadBalancerResponseEvent handleRequest(
      final ApplicationLoadBalancerRequestEvent event, final Context context) {
    try {
      System.out.println("event = " + event + ", context = " + context);
      JsonNode jsonEvent = OBJECT_MAPPER.valueToTree(event);
      log.info("jsonEvent = {}", jsonEvent.toPrettyString());

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

      ApplicationLoadBalancerResponseEvent response = new ApplicationLoadBalancerResponseEvent();
      response.setStatusCode(conformanceWebResponse.statusCode());
      response.setMultiValueHeaders(responseHeaders);
      response.setBody(conformanceWebResponse.body());
      response.setIsBase64Encoded(false);
      return response;
    } catch (RuntimeException | Error e) {
      log.error("Unhandled exception: {}", e, e);
      throw e;
    }
  }

  static void main(String[] ignoredArgs) {} // unused
}
