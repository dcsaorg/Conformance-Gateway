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
  void notificationChecksIncludeUpdatedPayloadOnlyForUpdateReceived() {
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
          updated == ShippingInstructionsStatus.SI_UPDATE_RECEIVED,
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
}
