package org.dcsa.conformance.lambda.auth;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionCheck;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Auth records live in a reserved namespace, never in existing environment/sandbox partitions.
 */
public final class DynamoIdentityMappingRepository implements IdentityMappingRepository {

  private final DynamoDbClient db;
  private final String table;
  private final String keyArn;

  public DynamoIdentityMappingRepository(DynamoDbClient db, String table, String keyArn) {
    this.db = db;
    this.table = table;
    // Match the inventory's key, without giving the repository responsibility for hashing.
    this.keyArn = keyArn;
  }

  private static AttributeValue s(String value) {
    return AttributeValue.fromS(value);
  }

  public static Map<String, AttributeValue> key(String suffix) {
    return Map.of("PK", s("auth#" + suffix), "SK", s("v1"));
  }

  private Map<String, AttributeValue> read(String suffix) {
    return db.getItem(GetItemRequest.builder().tableName(table).key(key(suffix)).consistentRead(true).build()).item();
  }

  @Override
  public String find(String identity) {
    var item = read("identity#" + identity);
    if (item.isEmpty()) return null;
    if (!s("active").equals(item.get("status")) || !item.containsKey("environmentId")) {
      throw new WebuiIdentity.Failure(409, "Identity reconciliation requires support");
    }
    String environmentId = item.get("environmentId").s();
    var owner = read("owner#" + environmentId);
    if (!s(identity).equals(owner.get("identity")) || s("reserved").equals(owner.get("status"))) {
      throw new WebuiIdentity.Failure(409, "Identity reconciliation requires support");
    }
    return environmentId;
  }

  @Override
  public String migrationVersion() {
    var meta = read("migration");
    if (!s("ready").equals(meta.get("status"))
      || !s(keyArn).equals(meta.get("keyArn"))
      || !meta.containsKey("version")) {
      throw new WebuiIdentity.Failure(503, "Identity migration is not ready");
    }
    return meta.get("version").s();
  }

  @Override
  public LegacyIdentityMapping legacy(String emailKey) {
    var item = read("legacy#" + emailKey);
    if (item.isEmpty()) return null;
    boolean blocked =
      !s("eligible").equals(item.get("status")) || !item.containsKey("environmentId");
    return new LegacyIdentityMapping(blocked ? "" : item.get("environmentId").s(), blocked);
  }

  @Override
  public String link(String identity, String environmentId, String emailKey, LegacyIdentityMapping legacy, String version) {
    // All four assertions succeed together, or nothing is written. Never replace existing owners.
    var transaction = List.of(
      createIdentityIfAbsent(identity, environmentId),
      claimOwnerIfAbsent(identity, environmentId),
      requireUnchangedLegacyLookup(emailKey, environmentId, legacy),
      requireActiveInventory(version)
    );

    try {
      db.transactWriteItems(TransactWriteItemsRequest.builder().transactItems(transaction).build());
      return environmentId;
    } catch (TransactionCanceledException e) {
      // Only the SAME identity may reuse a concurrent winner. No retry can claim another owner.
      String winner = find(identity);
      if (winner != null) return winner;
      throw new WebuiIdentity.Failure(409, "Identity reconciliation requires support; retry or contact support");
    }
  }

  private TransactWriteItem createIdentityIfAbsent(String identity, String environmentId) {
    var forward = new HashMap<>(key("identity#" + identity));
    forward.putAll(
      Map.of(
        "environmentId",
        s(environmentId),
        "status",
        s("active"),
        "createdAt",
        s(Instant.now().toString())));
    return insertIfAbsent(forward);
  }

  private TransactWriteItem claimOwnerIfAbsent(String identity, String environmentId) {
    var reverse = new HashMap<>(key("owner#" + environmentId));
    reverse.put("identity", s(identity));
    return insertIfAbsent(reverse);
  }

  private TransactWriteItem insertIfAbsent(Map<String, AttributeValue> item) {
    return TransactWriteItem.builder()
      .put(Put.builder()
        .tableName(table)
        .item(item)
        .conditionExpression("attribute_not_exists(PK)")
        .build())
      .build();
  }

  private TransactWriteItem requireUnchangedLegacyLookup(
    String emailKey, String environmentId, LegacyIdentityMapping legacy) {
    var legacyCheck = ConditionCheck.builder().tableName(table).key(key("legacy#" + emailKey));
    if (legacy == null) {
      legacyCheck.conditionExpression("attribute_not_exists(PK)");
    } else {
      legacyCheck
        .conditionExpression("#s = :s AND environmentId = :e")
        .expressionAttributeNames(Map.of("#s", "status"))
        .expressionAttributeValues(Map.of(":s", s("eligible"), ":e", s(environmentId)));
    }
    return TransactWriteItem.builder().conditionCheck(legacyCheck.build()).build();
  }

  private TransactWriteItem requireActiveInventory(String version) {
    return TransactWriteItem.builder()
      .conditionCheck(ConditionCheck.builder()
        .tableName(table)
        .key(key("migration"))
        .conditionExpression("#s = :s AND version = :v AND keyArn = :k")
        .expressionAttributeNames(Map.of("#s", "status"))
        .expressionAttributeValues(Map.of(":s", s("ready"), ":v", s(version), ":k", s(keyArn)))
        .build())
      .build();
  }
}
