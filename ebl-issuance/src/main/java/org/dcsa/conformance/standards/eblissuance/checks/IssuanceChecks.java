package org.dcsa.conformance.standards.eblissuance.checks;

import static org.dcsa.conformance.core.check.JsonAttribute.concatContextPath;
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

  private static JsonRebaseableContentCheck hasEndorseeScenarioCheck(String standardsVersion, EblType eblType) {
    if (standardsVersion.startsWith("2.") || standardsVersion.equals("3.0.0-Beta-1")) {
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
    return JsonAttribute.customValidator(
      "[Scenario] Validate endorsee party presence is correct",
      JsonAttribute.path("document", JsonAttribute.path("documentParties",
        (documentParties, contextPath) -> {
          if (!eblType.isToOrder()) {
            return Set.of();
          }
          var hadEndorsee = documentParties.has("endorsee");
          var endorseePath = concatContextPath(contextPath, "documentParties.endorsee");
          if (eblType.isBlankEbl() && hadEndorsee) {
            return Set.of("The EBL should have been blank endorsed, but it has an '%s' attribute".formatted(endorseePath));
          }
          if (!eblType.isBlankEbl() && !hadEndorsee) {
            return Set.of("The EBL should have had a named endorsee, but it is missing the '%s' attribute".formatted(endorseePath));
          }
          return Set.of();
        }
      ))
    );
  }

  public static ActionCheck tdScenarioChecks(UUID matched, String standardsVersion, EblType eblType) {
    return JsonAttribute.contentChecks(
      "Complex validations of transport document",
      EblIssuanceRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      standardsVersion,
      JsonAttribute.mustEqual(
        "[Scenario] The 'document.isToOrder' attribute must match the scenario requirements",
        JsonPointer.compile("/document/isToOrder"),
        eblType.isToOrder()
      ),
      hasEndorseeScenarioCheck(standardsVersion, eblType)
    );
  }

  public static ActionCheck tdContentChecks(UUID matched, String standardsVersion) {
    var checks = genericTDContentChecks(TransportDocumentStatus.TD_ISSUED, standardsVersion, null);
    return JsonAttribute.contentChecks(
      "Complex validations of transport document",
      EblIssuanceRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      standardsVersion,
      JsonContentCheckRebaser.of("document"),
      checks
    );
  }
}
