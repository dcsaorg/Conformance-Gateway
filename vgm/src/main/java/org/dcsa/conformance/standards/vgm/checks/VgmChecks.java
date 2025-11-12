package org.dcsa.conformance.standards.vgm.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.vgm.party.VgmRole;

public class VgmChecks {

  private static final String VGM_DECLARATIONS = "VGMDeclarations";

  private VgmChecks() {}

  public static ActionCheck getVGMGetPayloadChecks(UUID matched, String standardVersion) {
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(atLeastOneVgmDeclarationInMessageCheck());
    checks.add(atLeastOneVgmDeclarationWithVgmObjectCheck());
    checks.add(atLeastOneVgmDeclarationWithEquipmentDetailsCheck());
    checks.add(atLeastOneVgmDeclarationWithShipmentDetailsCheck());
    checks.add(atLeastOneVgmDeclarationWithResponsiblePartyCheck());
    checks.add(atLeastOneVgmDeclarationWithAuthorizedPersonSignatoryCheck());

    return JsonAttribute.contentChecks(
        VgmRole::isProducer, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  public static ActionCheck getVGMPostPayloadChecks(UUID matched, String standardVersion) {
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(atLeastOneVgmDeclarationInMessageCheck());
    checks.add(atLeastOneVgmDeclarationWithVgmObjectCheck());
    checks.add(atLeastOneVgmDeclarationWithEquipmentDetailsCheck());
    checks.add(atLeastOneVgmDeclarationWithShipmentDetailsCheck());
    checks.add(atLeastOneVgmDeclarationWithResponsiblePartyCheck());
    checks.add(atLeastOneVgmDeclarationWithAuthorizedPersonSignatoryCheck());

    return JsonAttribute.contentChecks(
        VgmRole::isProducer, matched, HttpMessageType.REQUEST, standardVersion, checks);
  }

  public static JsonContentCheck atLeastOneVgmDeclarationInMessageCheck() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "At least one VGM Declaration must be included in a message sent to the sandbox during conformance testing",
        mav -> mav.submitAllMatching(VGM_DECLARATIONS),
        JsonAttribute.matchedMinLength(1));
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithVgmObjectCheck() {
    return JsonAttribute.customValidator(
        "At least one VGM Declaration must demonstrate the correct use of the 'VGM' object",
        (body, contextPath) -> {
          var vgmDeclarations = body.path(VGM_DECLARATIONS);

          for (var declaration : vgmDeclarations) {
            var validationErrors = new ArrayList<String>();
            var vgm = declaration.path("VGM");

            // Check if VGM object exists
            if (JsonUtil.isMissingOrEmpty(vgm)) {
              validationErrors.add("VGM object is missing or null");
            } else {
              // Check weight object exists
              var weight = vgm.path("weight");
              if (JsonUtil.isMissingOrEmpty(weight)) {
                validationErrors.add("weight object is missing or null");
              } else {
                // Check weight.value is a positive number
                var weightValue = weight.path("value");
                if (weightValue.isMissingNode()) {
                  validationErrors.add("weight.value is missing");
                } else if (!weightValue.isNumber()) {
                  validationErrors.add("weight.value is not a number");
                } else if (weightValue.asDouble() <= 0) {
                  validationErrors.add("weight.value must be positive");
                }

                // Check weight.unit is 'KGM' or 'LBR'
                var weightUnit = weight.path("unit").asText("");
                if (weightUnit.isBlank()) {
                  validationErrors.add("weight.unit is missing or blank");
                } else if (!weightUnit.equals("KGM") && !weightUnit.equals("LBR")) {
                  validationErrors.add("weight.unit must be 'KGM' or 'LBR'");
                }
              }

              // Check method is 'SM1' or 'SM2'
              var method = vgm.path("method").asText("");
              if (method.isBlank()) {
                validationErrors.add("method is missing or blank");
              } else if (!method.equals("SM1") && !method.equals("SM2")) {
                validationErrors.add("method must be 'SM1' or 'SM2'");
              }
            }

            // If no validation errors, we found a valid VGM object
            if (validationErrors.isEmpty()) {
              return ConformanceCheckResult.simple(Set.of());
            }
          }

          return ConformanceCheckResult.simple(
              Set.of(
                  "At least one VGM Declaration must have a 'VGM' object with: a 'weight' object containing a positive 'value' and valid 'unit' ('KGM' or 'LBR'), and a valid 'method' ('SM1' or 'SM2')"));
        });
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithEquipmentDetailsCheck() {
    return JsonAttribute.customValidator(
        "At least one VGM Declaration must demonstrate the correct use of the 'equipmentDetails' object",
        (body, contextPath) -> {
          var vgmDeclarations = body.path(VGM_DECLARATIONS);

          for (var declaration : vgmDeclarations) {
            var equipmentReference =
                declaration.path("equipmentDetails").path("equipmentReference").asText("");

            if (!equipmentReference.isBlank()) {
              return ConformanceCheckResult.simple(Set.of());
            }
          }

          return ConformanceCheckResult.simple(
              Set.of(
                  "At least one VGM Declaration must have an 'equipmentDetails' object with a non-empty and non-blank 'equipmentReference' attribute"));
        });
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithShipmentDetailsCheck() {
    return JsonAttribute.customValidator(
        "At least one VGM Declaration must demonstrate the correct use of the 'shipmentDetails' object",
        (body, contextPath) -> {
          var vgmDeclarations = body.path(VGM_DECLARATIONS);

          for (var declaration : vgmDeclarations) {
            var carrierBookingReference =
                declaration.path("shipmentDetails").path("carrierBookingReference").asText("");
            var transportDocumentReference =
                declaration.path("shipmentDetails").path("transportDocumentReference").asText("");

            if (!carrierBookingReference.isBlank() || !transportDocumentReference.isBlank()) {
              return ConformanceCheckResult.simple(Set.of());
            }
          }

          return ConformanceCheckResult.simple(
              Set.of(
                  "At least one VGM Declaration must have a 'shipmentDetails' object with a non-empty and non-blank 'carrierBookingReference' or 'transportDocumentReference' attribute"));
        });
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithResponsiblePartyCheck() {
    return JsonAttribute.customValidator(
        "At least one VGM Declaration must demonstrate the correct use of the 'responsibleParty' object",
        (body, contextPath) -> {
          var vgmDeclarations = body.path(VGM_DECLARATIONS);

          for (var declaration : vgmDeclarations) {
            var partyName = declaration.path("responsibleParty").path("partyName").asText("");
            var contactDetailsName =
                declaration.path("responsibleParty").path("contactDetails").path("name").asText("");

            if (!partyName.isBlank() || !contactDetailsName.isBlank()) {
              return ConformanceCheckResult.simple(Set.of());
            }
          }

          return ConformanceCheckResult.simple(
              Set.of(
                  "At least one VGM Declaration must have a 'responsibleParty' object with a non-empty and non-blank 'partyName' or 'contactDetails.name' attribute"));
        });
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithAuthorizedPersonSignatoryCheck() {
    return JsonAttribute.customValidator(
        "At least one VGM Declaration must demonstrate the correct use of the 'authorizedPersonSignatory' attribute (not empty or blank)",
        (body, contextPath) -> {
          var vgmDeclarations = body.path(VGM_DECLARATIONS);

          for (var declaration : vgmDeclarations) {
            var authorizedPersonSignatory =
                declaration.path("authorizedPersonSignatory").asText("");
            if (!authorizedPersonSignatory.isBlank()) {
              return ConformanceCheckResult.simple(Set.of());
            }
          }

          return ConformanceCheckResult.simple(
              Set.of(
                  "At least one VGM Declaration must have a non-empty and non-blank 'authorizedPersonSignatory' attribute"));
        });
  }
}
