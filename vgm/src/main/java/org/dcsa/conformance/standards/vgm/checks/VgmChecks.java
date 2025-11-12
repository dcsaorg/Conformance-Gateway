package org.dcsa.conformance.standards.vgm.checks;

import java.util.UUID;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.standards.vgm.party.SuppliedScenarioParameters;

public class VgmChecks {

  public static ActionCheck getANPostPayloadChecks(
      UUID matchedExchangeUuid, String expectedApiVersion) {
    return null;
  }

  public static ActionCheck getANGetResponseChecks(
      UUID matchedExchangeUuid, String expectedApiVersion, SuppliedScenarioParameters ssp) {
    return null;
  }
}
