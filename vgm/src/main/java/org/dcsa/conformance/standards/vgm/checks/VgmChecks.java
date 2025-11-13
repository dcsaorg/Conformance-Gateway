package org.dcsa.conformance.standards.vgm.checks;

import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.*;

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
        "At least one VGM Declaration must demonstrate the correct use of the '%s' object"
            .formatted(VGM),
        (body, contextPath) -> {
          var vgmDeclarations = body.path(VGM_DECLARATIONS);
          var allValidationIssues = new ArrayList<String>();
          int declarationIndex = 0;

          for (var declaration : vgmDeclarations) {
            var validationErrors = new ArrayList<String>();
            var vgm = declaration.path(VGM);

            // Check if VGM object exists
            if (JsonUtil.isMissingOrEmpty(vgm)) {
              validationErrors.add("'%s' object is missing or null".formatted(VGM));
            } else {
              // Check weight object exists
              var weight = vgm.path(WEIGHT);
              if (JsonUtil.isMissingOrEmpty(weight)) {
                validationErrors.add("'%s' object is missing or null".formatted(WEIGHT));
              } else {
                // Check weight.value is a positive number
                var weightValue = weight.path(VALUE);
                if (weightValue.isMissingNode()) {
                  validationErrors.add("'%s.%s' is missing".formatted(WEIGHT, VALUE));
                } else if (!weightValue.isNumber()) {
                  validationErrors.add("'%s.%s' is not a number".formatted(WEIGHT, VALUE));
                } else if (weightValue.asDouble() <= 0) {
                  validationErrors.add("'%s.%s' must be positive".formatted(WEIGHT, VALUE));
                }

                // Check weight.unit is 'KGM' or 'LBR'
                var weightUnit = weight.path(UNIT).asText("");
                if (weightUnit.isBlank()) {
                  validationErrors.add("'%s.%s' is missing or blank".formatted(WEIGHT, UNIT));
                } else if (!VgmDataSets.VGM_WEIGHT_UNIT.contains(weightUnit)) {
                  validationErrors.add("'%s.%s' must be 'KGM' or 'LBR'".formatted(WEIGHT, UNIT));
                }
              }

              // Check method is 'SM1' or 'SM2'
              var method = vgm.path(METHOD).asText("");
              if (method.isBlank()) {
                validationErrors.add("'%s' is missing or blank".formatted(METHOD));
              } else if (!VgmDataSets.VGM_METHOD.contains(method)) {
                validationErrors.add("'%s' must be 'SM1' or 'SM2'".formatted(METHOD));
              }
            }

            if (validationErrors.isEmpty()) {
              return ConformanceCheckResult.simple(Set.of());
            }

            // Add each validation error with declaration index
            for (var error : validationErrors) {
              allValidationIssues.add("VGM Declaration [%d]: %s".formatted(declarationIndex, error));
            }
            declarationIndex++;
          }

          return ConformanceCheckResult.simple(Set.copyOf(allValidationIssues));
        });
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithEquipmentDetailsCheck() {
    return JsonAttribute.customValidator(
        "At least one VGM Declaration must demonstrate the correct use of the '%s' object"
            .formatted(EQUIPMENT_DETAILS),
        (body, contextPath) -> {
          var vgmDeclarations = body.path(VGM_DECLARATIONS);

          for (var declaration : vgmDeclarations) {
            var equipmentReference =
                declaration.path(EQUIPMENT_DETAILS).path(EQUIPMENT_REFERENCE).asText("");

            if (!equipmentReference.isBlank()) {
              return ConformanceCheckResult.simple(Set.of());
            }
          }

          return ConformanceCheckResult.simple(
              Set.of(
                  "At least one VGM Declaration must have an '%s' object with a non-empty and non-blank '%s' attribute"
                      .formatted(EQUIPMENT_DETAILS, EQUIPMENT_REFERENCE)));
        });
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithShipmentDetailsCheck() {
    return JsonAttribute.customValidator(
        "At least one VGM Declaration must demonstrate the correct use of the '%s' object"
            .formatted(SHIPMENT_DETAILS),
        (body, contextPath) -> {
          var vgmDeclarations = body.path(VGM_DECLARATIONS);

          for (var declaration : vgmDeclarations) {
            var carrierBookingReference =
                declaration.path(SHIPMENT_DETAILS).path(CARRIER_BOOKING_REFERENCE).asText("");
            var transportDocumentReference =
                declaration.path(SHIPMENT_DETAILS).path(TRANSPORT_DOCUMENT_REFERENCE).asText("");

            if (!carrierBookingReference.isBlank() || !transportDocumentReference.isBlank()) {
              return ConformanceCheckResult.simple(Set.of());
            }
          }

          return ConformanceCheckResult.simple(
              Set.of(
                  "At least one VGM Declaration must have a '%s' object with a non-empty and non-blank '%s' or '%s' attribute"
                      .formatted(
                          SHIPMENT_DETAILS,
                          CARRIER_BOOKING_REFERENCE,
                          TRANSPORT_DOCUMENT_REFERENCE)));
        });
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithResponsiblePartyCheck() {
    return JsonAttribute.customValidator(
        "At least one VGM Declaration must demonstrate the correct use of the '%s' object"
            .formatted(RESPONSIBLE_PARTY),
        (body, contextPath) -> {
          var vgmDeclarations = body.path(VGM_DECLARATIONS);

          for (var declaration : vgmDeclarations) {
            var partyName = declaration.path(RESPONSIBLE_PARTY).path(PARTY_NAME).asText("");
            var contactDetailsName =
                declaration.path(RESPONSIBLE_PARTY).path(CONTACT_DETAILS).path(NAME).asText("");

            if (!partyName.isBlank() || !contactDetailsName.isBlank()) {
              return ConformanceCheckResult.simple(Set.of());
            }
          }

          return ConformanceCheckResult.simple(
              Set.of(
                  "At least one VGM Declaration must have a '%s' object with a non-empty and non-blank '%s' or '%s.%s' attribute"
                      .formatted(RESPONSIBLE_PARTY, PARTY_NAME, CONTACT_DETAILS, NAME)));
        });
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithAuthorizedPersonSignatoryCheck() {
    return JsonAttribute.customValidator(
        "At least one VGM Declaration must demonstrate the correct use of the '%s' attribute (not empty or blank)"
            .formatted(AUTHORIZED_PERSON_SIGNATORY),
        (body, contextPath) -> {
          var vgmDeclarations = body.path(VGM_DECLARATIONS);

          for (var declaration : vgmDeclarations) {
            var authorizedPersonSignatory =
                declaration.path(AUTHORIZED_PERSON_SIGNATORY).asText("");
            if (!authorizedPersonSignatory.isBlank()) {
              return ConformanceCheckResult.simple(Set.of());
            }
          }

          return ConformanceCheckResult.simple(
              Set.of(
                  "At least one VGM Declaration must have a non-empty and non-blank '%s' attribute"
                      .formatted(AUTHORIZED_PERSON_SIGNATORY)));
        });
  }
}
