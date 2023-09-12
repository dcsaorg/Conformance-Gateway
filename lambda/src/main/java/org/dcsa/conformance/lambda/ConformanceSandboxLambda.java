package org.dcsa.conformance.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class ConformanceSandboxLambda
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  public APIGatewayProxyResponseEvent handleRequest(
      final APIGatewayProxyRequestEvent input, final Context context) {
    System.out.println("input = " + input + ", context = " + context);

    return new APIGatewayProxyResponseEvent()
        .withHeaders(Map.of("Content-Type", "application/json"))
        .withStatusCode(200)
        .withBody(new ObjectMapper().createObjectNode().put("message", "OK").toPrettyString());
  }

  public static void main(String[] args) {} // unused
}
