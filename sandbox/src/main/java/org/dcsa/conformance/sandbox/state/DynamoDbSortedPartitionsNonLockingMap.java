package org.dcsa.conformance.sandbox.state;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.dcsa.conformance.core.state.SortedPartitionsNonLockingMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

public class DynamoDbSortedPartitionsNonLockingMap implements SortedPartitionsNonLockingMap {
  private final DynamoDbClient dynamoDbClient;
  private final String tableName;

  public DynamoDbSortedPartitionsNonLockingMap(DynamoDbClient dynamoDbClient, String tableName) {
    this.dynamoDbClient = dynamoDbClient;
    this.tableName = tableName;
  }

  @Override
  public void setItemValue(String partitionKey, String sortKey, JsonNode value) {
    dynamoDbClient.putItem(
        PutItemRequest.builder()
            .tableName(tableName)
            .item(
                Map.ofEntries(
                    Map.entry("PK", AttributeValue.fromS(partitionKey)),
                    Map.entry("SK", AttributeValue.fromS(sortKey)),
                    Map.entry("value", AttributeValue.fromS(value.toString()))))
            .build());
  }

  @Override
  public JsonNode getItemValue(String partitionKey, String sortKey) {
    return JsonToolkit.stringToJsonNode(
        dynamoDbClient
            .getItem(
                GetItemRequest.builder()
                    .tableName(tableName)
                    .key(
                        Map.ofEntries(
                            Map.entry("PK", AttributeValue.fromS(partitionKey)),
                            Map.entry("SK", AttributeValue.fromS(sortKey))))
                    .build())
            .item()
            .getOrDefault("value", AttributeValue.fromS(""))
            .s());
  }

  @Override
  public JsonNode getFirstItemValue(String partitionKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public JsonNode getLastItemValue(String partitionKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<JsonNode> getPartitionValues(String partitionKey) {
    ArrayList<JsonNode> values = new ArrayList<>();

    Map<String, AttributeValue> lastEvaluatedKey = Collections.emptyMap();
    do {
      QueryRequest.Builder queryRequestBuilder =
          QueryRequest.builder()
              .tableName(tableName)
              .keyConditionExpression("#k = :v")
              .expressionAttributeNames(Map.of("#k", "PK"))
              .expressionAttributeValues(Map.of(":v", AttributeValue.fromS(partitionKey)));
      if (!lastEvaluatedKey.isEmpty()) {
        queryRequestBuilder.exclusiveStartKey(lastEvaluatedKey);
      }

      QueryResponse queryResponse = dynamoDbClient.query(queryRequestBuilder.build());
      queryResponse.items().stream()
          .filter(item -> item.containsKey("value"))
          .map(item -> JsonToolkit.stringToJsonNode(item.get("value").s()))
          .forEach(values::add);

      lastEvaluatedKey = queryResponse.lastEvaluatedKey();
    } while (!lastEvaluatedKey.isEmpty());

    return values;
  }
}
