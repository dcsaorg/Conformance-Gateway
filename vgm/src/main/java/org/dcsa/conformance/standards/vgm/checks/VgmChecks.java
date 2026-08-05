package org.dcsa.conformance.standards.vgm.checks;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.vgm.party.VgmRole;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.AUTHORIZED_PERSON_SIGNATORY;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.CARRIER_BOOKING_REFERENCE;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.CONTACT_DETAILS;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.EQUIPMENT_DETAILS;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.EQUIPMENT_REFERENCE;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.METHOD;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.NAME;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.PARTY_NAME;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.RESPONSIBLE_PARTY;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.SHIPMENT_DETAILS;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.TRANSPORT_DOCUMENT_REFERENCE;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.UNIT;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.VALUE;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.VGM;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.VGM_DECLARATIONS;
import static org.dcsa.conformance.standards.vgm.checks.VgmAttributes.WEIGHT;

public class VgmChecks {

  private VgmChecks() {
  }

  public static ActionCheck getVGMGetPayloadChecks(UUID matched, String standardVersion) {
    return payloadChecks(matched, standardVersion, HttpMessageType.RESPONSE);
  }

  public static ActionCheck getVGMPostPayloadChecks(UUID matched, String standardVersion) {
    return payloadChecks(matched, standardVersion, HttpMessageType.REQUEST);
  }

  private static ActionCheck payloadChecks(UUID matched, String standardVersion, HttpMessageType messageType) {
    return JsonAttribute.contentChecks(
      VgmRole::isProducer,
      matched,
      messageType,
      standardVersion,
      List.of(
        atLeastOneVgmDeclarationInMessageCheck(),
        atLeastOneVgmDeclarationWithVgmObjectCheck(),
        atLeastOneVgmDeclarationWithWeightObjectCheck(),
        atLeastOneVgmDeclarationWithPositiveWeightValueCheck(),
        atLeastOneVgmDeclarationWithValidWeightUnitCheck(),
        atLeastOneVgmDeclarationWithValidMethodCheck(),
        atLeastOneVgmDeclarationWithEquipmentDetailsCheck(),
        atLeastOneVgmDeclarationWithEquipmentReferenceCheck(),
        atLeastOneVgmDeclarationWithShipmentDetailsCheck(),
        atLeastOneVgmDeclarationWithShipmentReferenceCheck(),
        atLeastOneVgmDeclarationWithResponsiblePartyCheck(),
        atLeastOneVgmDeclarationWithResponsiblePartyNameCheck(),
        atLeastOneVgmDeclarationWithAuthorizedPersonSignatoryCheck()
      )
    );
  }

  public static JsonContentCheck atLeastOneVgmDeclarationInMessageCheck() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
      "At least one VGM Declaration must be included in a message sent to the sandbox during conformance testing.",
      mav -> mav.submitAllMatching(VGM_DECLARATIONS),
      JsonAttribute.matchedMinLength(1));
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithVgmObjectCheck() {
    return atLeastOneDeclarationMatches(
      "At least one VGM Declaration must demonstrate the correct use of the %s object."
        .formatted(VGM),
      declaration -> isPresentObject(declaration.path(VGM)));
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithWeightObjectCheck() {
    return atLeastOneDeclarationMatches(
      "The %s object within at least one VGM Declaration must demonstrate the correct use of the %s object."
        .formatted(VGM, WEIGHT),
      declaration -> isPresentObject(declaration.path(VGM).path(WEIGHT)));
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithPositiveWeightValueCheck() {
    return atLeastOneDeclarationMatches(
      "The %s.%s object within at least one VGM Declaration must demonstrate the correct use of the %s attribute (positive number)."
        .formatted(VGM, WEIGHT, VALUE),
      declaration -> {
        JsonNode value = declaration.path(VGM).path(WEIGHT).path(VALUE);
        return value.isNumber() && value.asDouble() > 0;
      });
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithValidWeightUnitCheck() {
    return atLeastOneDeclarationMatches(
      "The %s.%s object within at least one VGM Declaration must demonstrate the correct use of the %s attribute ('KGM' or 'LBR')."
        .formatted(VGM, WEIGHT, UNIT),
      declaration ->
        VgmDataSets.VGM_WEIGHT_UNIT.contains(
          declaration.path(VGM).path(WEIGHT).path(UNIT).asText("")));
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithValidMethodCheck() {
    return atLeastOneDeclarationMatches(
      "The %s object within at least one VGM Declaration must demonstrate the correct use of the %s attribute ('SM1' or 'SM2')."
        .formatted(VGM, METHOD),
      declaration ->
        VgmDataSets.VGM_METHOD.contains(declaration.path(VGM).path(METHOD).asText("")));
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithEquipmentDetailsCheck() {
    return atLeastOneDeclarationMatches(
      "At least one VGM Declaration must demonstrate the correct use of the %s object."
        .formatted(EQUIPMENT_DETAILS),
      declaration -> isPresentObject(declaration.path(EQUIPMENT_DETAILS)));
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithEquipmentReferenceCheck() {
    return atLeastOneDeclarationMatches(
      "The %s object within at least one VGM Declaration must demonstrate the correct use of the %s attribute (not empty or blank)."
        .formatted(EQUIPMENT_DETAILS, EQUIPMENT_REFERENCE),
      declaration ->
        isNotBlank(declaration.path(EQUIPMENT_DETAILS).path(EQUIPMENT_REFERENCE)));
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithShipmentDetailsCheck() {
    return atLeastOneDeclarationMatches(
      "At least one VGM Declaration must demonstrate the correct use of the %s object."
        .formatted(SHIPMENT_DETAILS),
      declaration -> isPresentObject(declaration.path(SHIPMENT_DETAILS)));
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithShipmentReferenceCheck() {
    return atLeastOneDeclarationMatches(
      "The %s object within at least one VGM Declaration must demonstrate the correct use of the %s or %s attribute (not empty or blank)."
        .formatted(
          SHIPMENT_DETAILS, CARRIER_BOOKING_REFERENCE, TRANSPORT_DOCUMENT_REFERENCE),
      declaration -> {
        JsonNode shipmentDetails = declaration.path(SHIPMENT_DETAILS);
        return isNotBlank(shipmentDetails.path(CARRIER_BOOKING_REFERENCE))
          || isNotBlank(shipmentDetails.path(TRANSPORT_DOCUMENT_REFERENCE));
      });
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithResponsiblePartyCheck() {
    return atLeastOneDeclarationMatches(
      "At least one VGM Declaration must demonstrate the correct use of the %s object."
        .formatted(RESPONSIBLE_PARTY),
      declaration -> isPresentObject(declaration.path(RESPONSIBLE_PARTY)));
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithResponsiblePartyNameCheck() {
    return atLeastOneDeclarationMatches(
      "The %s object within at least one VGM Declaration must demonstrate the correct use of the %s or %s.%s attribute (not empty or blank)."
        .formatted(RESPONSIBLE_PARTY, PARTY_NAME, CONTACT_DETAILS, NAME),
      declaration -> {
        JsonNode responsibleParty = declaration.path(RESPONSIBLE_PARTY);
        return isNotBlank(responsibleParty.path(PARTY_NAME))
          || isNotBlank(responsibleParty.path(CONTACT_DETAILS).path(NAME));
      });
  }

  public static JsonContentCheck atLeastOneVgmDeclarationWithAuthorizedPersonSignatoryCheck() {
    return atLeastOneDeclarationMatches(
      "At least one VGM Declaration must demonstrate the correct use of the %s attribute (not empty or blank)."
        .formatted(AUTHORIZED_PERSON_SIGNATORY),
      declaration -> isNotBlank(declaration.path(AUTHORIZED_PERSON_SIGNATORY)));
  }

  private static JsonContentCheck atLeastOneDeclarationMatches(
    String description, Predicate<JsonNode> predicate) {
    return JsonAttribute.customValidator(
      description,
      (body, contextPath) -> {
        JsonNode declarations = body.path(VGM_DECLARATIONS);
        if (declarations.isArray()) {
          for (JsonNode declaration : declarations) {
            if (predicate.test(declaration)) {
              return ConformanceCheckResult.simple(Set.of());
            }
          }
        }
        return ConformanceCheckResult.simple(Set.of(description));
      });
  }

  private static boolean isPresentObject(JsonNode node) {
    return node.isObject() && !JsonUtil.isMissingOrEmpty(node);
  }

  private static boolean isNotBlank(JsonNode node) {
    return node.isTextual() && !node.asText().isBlank();
  }
}
