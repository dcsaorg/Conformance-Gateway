package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.standards.ebl.checks.EblCheckTestFixtures.dsp;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;
import org.junit.jupiter.api.Test;

class CarrierTdNotificationPayloadRequestConformanceCheckTest {
  @Test
  void notificationWithoutReferenceRequirementOmitsReferenceEqualityCheck() {
    var check =
        new CarrierTdNotificationPayloadRequestConformanceCheck(
            UUID.randomUUID(), List.of(TransportDocumentStatus.TD_DRAFT), false, () -> dsp(null));
    assertTrue(check.createSubChecks().noneMatch(c -> c.getTitle().contains("Must equal 'TDR'")));
  }
}
