package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.ebl.checks.EblCheckTestFixtures.dsp;
import static org.dcsa.conformance.standards.ebl.checks.EblCheckTestFixtures.exchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.dcsa.conformance.core.check.ConformanceResult;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.junit.jupiter.api.Test;

class CarrierSiNotificationPayloadRequestConformanceCheckTest {
  @Test
  void notificationChecksIncludeUpdatedPayloadWhenUpdateStatusIsUnconstrainedOrExpected() {
    for (ShippingInstructionsStatus updated :
        Arrays.asList(
            null,
            ShippingInstructionsStatus.SI_ANY,
            ShippingInstructionsStatus.SI_UPDATE_RECEIVED)) {
      var check =
          new CarrierSiNotificationPayloadRequestConformanceCheck(
              UUID.randomUUID(),
              ShippingInstructionsStatus.SI_RECEIVED,
              updated,
              () -> dsp("REGULAR_SWB"));
      var checks = check.createSubChecks().toList();
      assertEquals(
          updated != null,
          checks.stream().anyMatch(c -> c.getTitle().contains("[Updated Shipping Instructions]")));
      ObjectNode payload = OBJECT_MAPPER.createObjectNode();
      ObjectNode data = payload.putObject("data");
      data.put("shippingInstructionsStatus", "RECEIVED");
      if (updated == ShippingInstructionsStatus.SI_UPDATE_RECEIVED) {
        data.put("updatedShippingInstructionsStatus", "UPDATE_RECEIVED");
      }
      for (String field : List.of("shippingInstructions", "updatedShippingInstructions")) {
        ObjectNode nested = data.putObject(field);
        nested
            .put("shippingInstructionsReference", "SIR")
            .put("transportDocumentTypeCode", "SWB")
            .put("isToOrder", false)
            .put("shippingInstructionsStatus", "RECEIVED");
        nested.withObject("/documentParties/consignee/address").put("street", "Road");
        if (updated == ShippingInstructionsStatus.SI_UPDATE_RECEIVED) {
          nested.put("updatedShippingInstructionsStatus", "UPDATE_RECEIVED");
        }
      }
      var exchange = exchange(payload, payload);
      checks.forEach(
          c -> {
            c.check(id -> exchange);
            assertEquals(1, c.resultsStream().count(), c.getTitle());
            assertTrue(c.resultsStream().allMatch(ConformanceResult::isConformant), c.getTitle());
          });
    }
  }

  @Test
  void notificationNestedReferencesMustMatchTheScenario() {
    for (ShippingInstructionsStatus updated :
        List.of(ShippingInstructionsStatus.SI_ANY, ShippingInstructionsStatus.SI_UPDATE_RECEIVED)) {
      for (String field : List.of("shippingInstructions", "updatedShippingInstructions")) {
        for (String reference : List.of("SIR", "UNRELATED-SIR")) {
          ObjectNode payload = OBJECT_MAPPER.createObjectNode();
          ObjectNode data = payload.putObject("data");
          data.put("shippingInstructionsStatus", "RECEIVED");
          data.put("updatedShippingInstructionsStatus", "UPDATE_RECEIVED");
          ObjectNode nested = data.putObject(field);
          nested
              .put("shippingInstructionsReference", reference)
              .put("shippingInstructionsStatus", "RECEIVED")
              .put("updatedShippingInstructionsStatus", "UPDATE_RECEIVED")
              .put("transportDocumentTypeCode", "SWB")
              .put("isToOrder", false);
          nested.withObject("/documentParties/consignee/address").put("street", "Road");
          assertNotificationConformance(payload, updated, reference.equals("SIR"));
        }
      }
    }
  }

  @Test
  void notificationSiAnyStillRejectsUnknownUpdatedStatuses() {
    for (String field : List.of("data", "shippingInstructions", "updatedShippingInstructions")) {
      for (String status : List.of("UPDATE_RECEIVED", "FOO")) {
        ObjectNode payload = OBJECT_MAPPER.createObjectNode();
        ObjectNode data = payload.putObject("data");
        data.put("shippingInstructionsStatus", "RECEIVED");
        ObjectNode target = field.equals("data") ? data : data.putObject(field);
        target
            .put("shippingInstructionsReference", "SIR")
            .put("shippingInstructionsStatus", "RECEIVED")
            .put("updatedShippingInstructionsStatus", status)
            .put("transportDocumentTypeCode", "SWB")
            .put("isToOrder", false);
        target.withObject("/documentParties/consignee/address").put("street", "Road");
        assertNotificationConformance(
            payload, ShippingInstructionsStatus.SI_ANY, status.equals("UPDATE_RECEIVED"));
      }
    }
  }

  @Test
  void notificationSiAnyValidatesSuppliedUpdateCodeListProviders() {
    for (String provider : List.of("DCSA", "ZZZ", "UNKNOWN")) {
      ObjectNode payload = OBJECT_MAPPER.createObjectNode();
      ObjectNode data = payload.putObject("data");
      data.put("shippingInstructionsStatus", "RECEIVED");
      ObjectNode updated = data.putObject("updatedShippingInstructions");
      updated
          .put("shippingInstructionsReference", "SIR")
          .put("shippingInstructionsStatus", "RECEIVED")
          .put("transportDocumentTypeCode", "SWB")
          .put("isToOrder", false);
      updated
          .withObject("/documentParties/consignee")
          .putArray("identifyingCodes")
          .addObject()
          .put("codeListProvider", provider)
          .put("partyCode", "PARTY");
      assertNotificationConformance(
          payload, ShippingInstructionsStatus.SI_ANY, !provider.equals("UNKNOWN"));
    }
  }

  @Test
  void notificationSiAnyAllowsAbsentOrEmptyUpdatePayload() {
    ObjectNode payload = OBJECT_MAPPER.createObjectNode();
    ObjectNode data = payload.putObject("data");
    data.put("shippingInstructionsStatus", "RECEIVED");
    assertNotificationConformance(payload, ShippingInstructionsStatus.SI_ANY, true);
    data.putObject("updatedShippingInstructions");
    assertNotificationConformance(payload, ShippingInstructionsStatus.SI_ANY, true);
  }

  private static void assertNotificationConformance(
      ObjectNode payload, ShippingInstructionsStatus updated, boolean expected) {
    var exchange = exchange(payload, payload);
    var check =
        new CarrierSiNotificationPayloadRequestConformanceCheck(
            exchange.getUuid(),
            ShippingInstructionsStatus.SI_RECEIVED,
            updated,
            () -> dsp("REGULAR_SWB"));
    var checks = check.createSubChecks().toList();
    checks.forEach(c -> c.check(id -> exchange));
    assertEquals(
        expected,
        checks.stream()
            .flatMap(c -> c.resultsStream())
            .allMatch(ConformanceResult::isConformant),
        payload.toString());
  }
}
