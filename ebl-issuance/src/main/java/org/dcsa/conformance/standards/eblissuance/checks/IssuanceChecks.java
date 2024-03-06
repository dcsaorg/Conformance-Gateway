package org.dcsa.conformance.standards.eblissuance.checks;

import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.genericTDContentChecks;

import java.util.UUID;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheckRebaser;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

public class IssuanceChecks {

  public static ActionCheck tdContentChecks(UUID matched) {
    var checks = genericTDContentChecks(TransportDocumentStatus.TD_ISSUED, null);
    return JsonAttribute.contentChecks(
      "Complex validations of transport document",
      EblIssuanceRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      JsonContentCheckRebaser.of("document"),
      checks
    );
  }
}
