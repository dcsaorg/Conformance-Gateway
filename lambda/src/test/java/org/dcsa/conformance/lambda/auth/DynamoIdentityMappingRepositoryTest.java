package org.dcsa.conformance.lambda.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

class DynamoIdentityMappingRepositoryTest {
  final DynamoDbClient db = mock(DynamoDbClient.class);
  final DynamoIdentityMappingRepository mappings =
      new DynamoIdentityMappingRepository(db, "dtConformance", "key-arn");

  static AttributeValue s(String value) {
    return AttributeValue.fromS(value);
  }

  @Test
  void transactionClaimsForwardAndReverseAndChecksInventoryAndLegacy() {
    mappings.link(
        "identity",
        "legacy",
        "version#email",
        new LegacyIdentityMapping("legacy", false),
        "version");
    var capture = ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
    verify(db).transactWriteItems(capture.capture());
    var items = capture.getValue().transactItems();
    assertEquals(4, items.size());
    assertEquals("attribute_not_exists(PK)", items.get(0).put().conditionExpression());
    assertEquals("attribute_not_exists(PK)", items.get(1).put().conditionExpression());
    assertEquals(s("auth#owner#legacy"), items.get(1).put().item().get("PK"));
    assertEquals(s("auth#legacy#version#email"), items.get(2).conditionCheck().key().get("PK"));
    assertEquals(
        s("eligible"), items.get(2).conditionCheck().expressionAttributeValues().get(":s"));
    assertEquals(s("version"), items.get(3).conditionCheck().expressionAttributeValues().get(":v"));
    assertEquals(s("key-arn"), items.get(3).conditionCheck().expressionAttributeValues().get(":k"));
    assertEquals(s("v1"), items.get(0).put().item().get("SK"));
    assertEquals(s("auth#identity#identity"), items.get(0).put().item().get("PK"));
  }

  @Test
  void reservedAndMismatchedOwnersCannotBeUsed() {
    for (var owner :
        java.util.List.of(
            Map.of("identity", s("other")),
            Map.of("identity", s("identity"), "status", s("reserved")))) {
      when(db.getItem(any(GetItemRequest.class)))
          .thenReturn(
              GetItemResponse.builder()
                  .item(Map.of("status", s("active"), "environmentId", s("legacy")))
                  .build())
          .thenReturn(GetItemResponse.builder().item(owner).build());
      assertEquals(
          409, assertThrows(WebuiIdentity.Failure.class, () -> mappings.find("identity")).status());
    }
  }

  @Test
  void newUserTransactionRequiresAbsentLegacyLookup() {
    mappings.link("identity", "new", "version#email", null, "version");
    var capture = ArgumentCaptor.forClass(TransactWriteItemsRequest.class);
    verify(db).transactWriteItems(capture.capture());
    assertEquals(
        "attribute_not_exists(PK)",
        capture.getValue().transactItems().get(2).conditionCheck().conditionExpression());
  }

  @Test
  void retriesConcurrentWinnerButDoesNotReassignDifferentIdentity() {
    when(db.transactWriteItems(any(TransactWriteItemsRequest.class)))
        .thenThrow(TransactionCanceledException.builder().message("conflict").build());
    when(db.getItem(any(GetItemRequest.class)))
        .thenReturn(GetItemResponse.builder().item(Map.of()).build());
    assertEquals(
        409,
        assertThrows(
                WebuiIdentity.Failure.class,
                () -> mappings.link("other", "legacy", "email", null, "v"))
            .status());
    when(db.getItem(any(GetItemRequest.class)))
        .thenReturn(
            GetItemResponse.builder()
                .item(Map.of("status", s("active"), "environmentId", s("winner")))
                .build())
        .thenReturn(GetItemResponse.builder().item(Map.of("identity", s("same"))).build());
    assertEquals("winner", mappings.link("same", "loser", "email", null, "v"));
  }

  @Test
  void missingReadinessKeyMismatchAndRevokedMappingsFailClosed() {
    when(db.getItem(any(GetItemRequest.class)))
        .thenReturn(GetItemResponse.builder().item(Map.of()).build());
    assertThrows(WebuiIdentity.Failure.class, mappings::migrationVersion);
    when(db.getItem(any(GetItemRequest.class)))
        .thenReturn(
            GetItemResponse.builder()
                .item(Map.of("status", s("ready"), "keyArn", s("wrong-key"), "version", s("v")))
                .build());
    assertThrows(WebuiIdentity.Failure.class, mappings::migrationVersion);
    when(db.getItem(any(GetItemRequest.class)))
        .thenReturn(
            GetItemResponse.builder()
                .item(Map.of("status", s("revoked"), "environmentId", s("legacy")))
                .build());
    assertThrows(WebuiIdentity.Failure.class, () -> mappings.find("identity"));
    var capture = ArgumentCaptor.forClass(GetItemRequest.class);
    verify(db, atLeastOnce()).getItem(capture.capture());
    assertTrue(capture.getAllValues().stream().allMatch(GetItemRequest::consistentRead));
  }
}
