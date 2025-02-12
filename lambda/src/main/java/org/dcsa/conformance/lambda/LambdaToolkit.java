package org.dcsa.conformance.lambda;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;
import org.dcsa.conformance.sandbox.state.DynamoDbSortedPartitionsLockingMap;
import org.dcsa.conformance.sandbox.state.DynamoDbSortedPartitionsNonLockingMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LambdaToolkit {
  private static final Logger logger = LoggerFactory.getLogger(LambdaToolkit.class);

  public static ConformancePersistenceProvider createPersistenceProvider() {
    DynamoDbClient dynamoDbClient =
        DynamoDbClient.builder()
            .region(Region.EU_NORTH_1)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build();
    String tableName = System.getenv("TABLE_NAME");
    if (tableName == null || tableName.isEmpty()) {
      logger.error("Environment variable TABLE_NAME is not set");
      throw new IllegalStateException("Environment variable TABLE_NAME is not set");
    }
    return new ConformancePersistenceProvider(
        new DynamoDbSortedPartitionsNonLockingMap(dynamoDbClient, tableName),
        new DynamoDbSortedPartitionsLockingMap(dynamoDbClient, tableName));
  }

  public static String getDbConfigValue(
      ConformancePersistenceProvider persistenceProvider, String key) {
    return persistenceProvider.getNonLockingMap().getItemValue("configuration", key).asText();
  }

  public static Consumer<JsonNode> createDeferredSandboxTaskConsumer(
      ConformancePersistenceProvider persistenceProvider) {
    return jsonNode -> {
      try (LambdaAsyncClient client = LambdaAsyncClient.create()) {
        InvokeRequest invokeRequest =
            InvokeRequest.builder()
                .invocationType(InvocationType.EVENT)
                .functionName(getDbConfigValue(persistenceProvider, "sandboxTaskLambdaArn"))
                .payload(SdkBytes.fromUtf8String(jsonNode.toPrettyString()))
                .build();
        client.invoke(invokeRequest).join();
      } catch (Exception e) {
        logger.error("Failed to invoke Lambda function", e);
      }
    };
  }
}
