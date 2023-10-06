package org.dcsa.conformance.lambda;

import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.sandbox.ConformanceSandbox;
import org.dcsa.conformance.sandbox.ConformanceWebRequest;
import org.dcsa.conformance.sandbox.ConformanceWebResponse;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;
import org.dcsa.conformance.sandbox.state.DynamoDbSortedPartitionsLockingMap;
import org.dcsa.conformance.sandbox.state.DynamoDbSortedPartitionsNonLockingMap;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Slf4j
public class ConformanceSandboxLambda
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public APIGatewayProxyResponseEvent handleRequest(
      final APIGatewayProxyRequestEvent input, final Context context) {
    try {
      System.out.println("input = " + input + ", context = " + context);
      log.info("input.getPath() = " + input.getPath());

      // empty home page
      if ("/".equals(input.getPath())) {
        return new APIGatewayProxyResponseEvent()
            .withHeaders(Map.of("Content-Type", "text/plain"))
            .withStatusCode(200)
            .withBody("");
      }

      if (input.getPath().startsWith("/conformance")) {
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();

        ConformancePersistenceProvider persistenceProvider =
            new ConformancePersistenceProvider(
                new DynamoDbSortedPartitionsNonLockingMap(dynamoDbClient, "conformance"),
                new DynamoDbSortedPartitionsLockingMap(dynamoDbClient, "conformance"));

        String baseUrl =
            persistenceProvider
                .getNonLockingMap()
                .getItemValue("configuration", "baseUrl")
                .asText();
        String httpClientLambdaArn =
            persistenceProvider
                .getNonLockingMap()
                .getItemValue("configuration", "httpClientLambdaArn")
                .asText();

        ConformanceWebResponse conformanceWebResponse =
            ConformanceSandbox.handleRequest(
                persistenceProvider,
                new ConformanceWebRequest(
                    input.getHttpMethod(),
                    baseUrl + input.getPath(),
                    Objects.requireNonNullElse(
                        input.getMultiValueQueryStringParameters(), Collections.emptyMap()),
                    input.getMultiValueHeaders(),
                    input.getBody()),
                conformanceWebRequest -> {
                  ObjectNode httpClientLambdaEvent = new ObjectMapper().createObjectNode();
                  httpClientLambdaEvent.put("url", conformanceWebRequest.url());
                  conformanceWebRequest
                      .headers()
                      .forEach(
                          (name, values) ->
                              values.forEach(
                                  value -> {
                                    httpClientLambdaEvent.put("authHeaderName", name);
                                    httpClientLambdaEvent.put("authHeaderValue", value);
                                  }));
                  AWSLambdaAsyncClientBuilder.defaultClient()
                      .invoke(
                          new InvokeRequest()
                              .withInvocationType(InvocationType.Event)
                              .withFunctionName(httpClientLambdaArn)
                              .withPayload(httpClientLambdaEvent.toPrettyString()));
                });

        Map<String, List<String>> responseHeaders = conformanceWebResponse.getValueListHeaders();
        responseHeaders.put("Content-Type", List.of(conformanceWebResponse.contentType()));
        return new APIGatewayProxyResponseEvent()
            .withMultiValueHeaders(responseHeaders)
            .withStatusCode(conformanceWebResponse.statusCode())
            .withBody(conformanceWebResponse.body());
      }

      log.error("Unsupported request path");
      return new APIGatewayProxyResponseEvent()
          .withHeaders(Map.of("Content-Type", "text/plain"))
          .withStatusCode(404)
          .withBody("");
    } catch (RuntimeException | Error e) {
      log.error("Unhandled exception: " + e, e);
      throw e;
    }
  }

  public static void main(String[] args) {} // unused
}
