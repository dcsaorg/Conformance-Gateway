package org.dcsa.conformance.sandbox.state;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    AttributeValue attributeValue =
        dynamoDbClient
            .getItem(
                GetItemRequest.builder()
                    .tableName(tableName)
                    .key(
                        Map.ofEntries(
                            Map.entry("PK", AttributeValue.fromS(partitionKey)),
                            Map.entry("SK", AttributeValue.fromS(sortKey))))
                    .consistentRead(true)
                    .build())
            .item()
            .get("value");
    return attributeValue == null ? null : JsonToolkit.stringToJsonNode(attributeValue.s());
  }

  @Override
  public LinkedHashMap<String, JsonNode> getPartitionValuesBySortKey(
      String partitionKey, String sortKeyPrefix) {
    List<Map<String, AttributeValue>> allItems = new ArrayList<>();
    Map<String, AttributeValue> lastEvaluatedKey = Collections.emptyMap();
    do {
      // "The AttributeValue for a key attribute cannot contain an empty string value."
      QueryRequest.Builder queryRequestBuilder =
          sortKeyPrefix.isEmpty()
              ? QueryRequest.builder()
                  .tableName(tableName)
                  .keyConditionExpression("#pk = :pkv")
                  .expressionAttributeNames(Map.ofEntries(Map.entry("#pk", "PK")))
                  .expressionAttributeValues(
                      Map.ofEntries(Map.entry(":pkv", AttributeValue.fromS(partitionKey))))
                  .consistentRead(true)
              : QueryRequest.builder()
                  .tableName(tableName)
                  .keyConditionExpression("#pk = :pkv AND begins_with(#sk, :skp)")
                  .expressionAttributeNames(
                      Map.ofEntries(Map.entry("#pk", "PK"), Map.entry("#sk", "SK")))
                  .expressionAttributeValues(
                      Map.ofEntries(
                          Map.entry(":pkv", AttributeValue.fromS(partitionKey)),
                          Map.entry(":skp", AttributeValue.fromS(sortKeyPrefix))))
                  .consistentRead(true);
      if (!lastEvaluatedKey.isEmpty()) {
        queryRequestBuilder.exclusiveStartKey(lastEvaluatedKey);
      }

      QueryResponse queryResponse = dynamoDbClient.query(queryRequestBuilder.build());
      queryResponse.items().stream()
          .filter(item -> item.containsKey("value"))
          .forEach(allItems::add);

      lastEvaluatedKey = queryResponse.lastEvaluatedKey();
    } while (!lastEvaluatedKey.isEmpty());

    return allItems.stream()
        .collect(
            Collectors.toMap(
                item -> item.get("SK").s(),
                item -> JsonToolkit.stringToJsonNode(item.get("value").s()),
                (existing, replacement) -> existing,
                LinkedHashMap::new));
  }
}
