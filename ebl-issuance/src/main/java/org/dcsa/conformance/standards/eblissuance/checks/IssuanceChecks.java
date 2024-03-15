package org.dcsa.conformance.standards.eblissuance.checks;

import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.genericTDContentChecks;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonPointer;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheckRebaser;
import org.dcsa.conformance.core.check.JsonRebaseableContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;
import org.dcsa.conformance.standards.eblissuance.action.EblType;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

public class IssuanceChecks {

  private static JsonRebaseableContentCheck hasEndorseeScenarioCheck(EblType eblType) {
    return JsonAttribute.customValidator(
      "[Scenario] Validate END party presence is correct",
      JsonAttribute.path("document", JsonAttribute.path("documentParties",
        (documentParties, contextPath) -> {
          var hadEndorsee = false;
          if (!eblType.isToOrder()) {
            return Set.of();
          }
          for (var party : documentParties) {
            if (party.path("partyFunction").asText("").equals("END")) {
              hadEndorsee = true;
              break;
            }
          }

          if (eblType.isBlankEbl() && hadEndorsee) {
            return Set.of("The EBL should have been blank endorsed, but it has an END party");
          }
          if (!eblType.isBlankEbl() && !hadEndorsee) {
            return Set.of("The EBL should have had a named endorsee, but it is missing the END party");
          }
          return Set.of();
        }
      ))
    );
  }

  public static ActionCheck tdScenarioChecks(UUID matched, EblType eblType) {
    return JsonAttribute.contentChecks(
      "Complex validations of transport document",
      EblIssuanceRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      JsonAttribute.mustEqual(
        "[Scenario] The 'document.isToOrder' attribute must match the scenario requirements",
        JsonPointer.compile("/document/isToOrder"),
        eblType.isToOrder()
      ),
      hasEndorseeScenarioCheck(eblType)
    );
  }

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
