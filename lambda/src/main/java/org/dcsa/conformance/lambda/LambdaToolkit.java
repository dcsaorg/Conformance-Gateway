package org.dcsa.conformance.lambda;

import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.sandbox.ConformanceWebRequest;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;
import org.dcsa.conformance.sandbox.state.DynamoDbSortedPartitionsLockingMap;
import org.dcsa.conformance.sandbox.state.DynamoDbSortedPartitionsNonLockingMap;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LambdaToolkit {
  public static ConformancePersistenceProvider createPersistenceProvider() {
    DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
    String tableName = System.getenv("TABLE_NAME");
    return new ConformancePersistenceProvider(
        new DynamoDbSortedPartitionsNonLockingMap(dynamoDbClient, tableName),
        new DynamoDbSortedPartitionsLockingMap(dynamoDbClient, tableName));
  }

  public static String getDbConfigValue(
      ConformancePersistenceProvider persistenceProvider, String key) {
    return persistenceProvider.getNonLockingMap().getItemValue("configuration", key).asText();
  }

  public static Consumer<ConformanceWebRequest> createAsyncWebClient(
      ConformancePersistenceProvider persistenceProvider) {
    return conformanceWebRequest -> {
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
                  .withFunctionName(getDbConfigValue(persistenceProvider, "httpClientLambdaArn"))
                  .withPayload(httpClientLambdaEvent.toPrettyString()));
    };
  }

  public static BiConsumer<String, ConformanceRequest> createAsyncSandboxOutboundRequestHandler(
      ConformancePersistenceProvider persistenceProvider) {
    return (sandboxId, conformanceRequest) -> {
      throw new UnsupportedOperationException("TODO");
    };
  }
}
