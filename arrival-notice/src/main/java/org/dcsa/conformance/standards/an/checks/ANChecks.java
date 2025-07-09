package org.dcsa.conformance.standards.an.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.an.party.ANRole;
import org.dcsa.conformance.standards.an.party.DynamicScenarioParameters;

public class ANChecks {

  public static ActionCheck getANPostPayloadChecks(
      UUID matchedExchangeUuid, String expectedApiVersion, String scenarioType) {
    var checks = new ArrayList<>(COMMON_PAYLOAD_CHECKS);
    checks.addAll(getScenarioRelatedChecks(scenarioType));
    return JsonAttribute.contentChecks(
        ANRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        checks);
  }

  private static JsonContentCheck validateBasicFields() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the basic ArrivalNotice fields",
        body -> {
          var issues = new LinkedHashSet<String>();
          issues.addAll(checkBasicField("carrierCode").validate(body));
          issues.addAll(checkBasicField("carrierCodeListProvider").validate(body));
          issues.addAll(checkBasicField("deliveryTypeAtDestination").validate(body));
          return issues;
        });
  }

  private static JsonContentCheck checkBasicField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"" + field + "\" attribute",
        mav -> mav.path("arrivalNotices").all(),
        (node, contextPath) -> {
          if (!node.hasNonNull(field)) {
            return Set.of(
                contextPath + "." + field + " must be functionally present in the payload");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateCarrierContactInformation() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the \"carrierInformationForCargoRelease\" object",
        body -> {
          var issues = new LinkedHashSet<String>();
          issues.addAll(checkCarrierField("name").validate(body));
          issues.addAll(checkCarrierEmailOrPhone().validate(body));
          return issues;
        });
  }

  private static JsonContentCheck checkCarrierField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \""
            + field
            + "\" attribute in \"carrierInformationForCargoRelease\"",
        mav -> mav.path("arrivalNotices").all().path("carrierInformationForCargoRelease").all(),
        (node, contextPath) -> {
          if (!node.hasNonNull(field)) {
            return Set.of(contextPath + "." + field + " must be functionally present");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck checkCarrierEmailOrPhone() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of either the \"email\" or \"phone\" attribute in \"carrierInformationForCargoRelease\"",
        mav -> mav.path("arrivalNotices").all().path("carrierInformationForCargoRelease").all(),
        (node, contextPath) -> {
          if (!node.hasNonNull("email") && !node.hasNonNull("phone")) {
            return Set.of(contextPath + " must functionally include either 'email' or 'phone'");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateDocumentParties() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the \"documentParties\" object",
        body -> {
          var issues = new LinkedHashSet<String>();
          issues.addAll(checkDocumentPartyField("partyFunction").validate(body));
          issues.addAll(checkDocumentPartyField("partyName").validate(body));
          issues.addAll(checkDocumentPartyField("partyContactDetails").validate(body));
          issues.addAll(checkDocumentPartyAddress().validate(body));
          return issues;
        });
  }

  private static JsonContentCheck checkDocumentPartyField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \""
            + field
            + "\" attribute in \"documentParties\"",
        mav -> mav.path("arrivalNotices").all().path("documentParties").all(),
        (node, contextPath) -> {
          if (!node.hasNonNull(field)) {
            return Set.of(contextPath + "." + field + " must be functionally  present");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck checkDocumentPartyAddress() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"address\" object in \"documentParties\"",
        mav -> mav.path("arrivalNotices").all().path("documentParties").all(),
        (node, contextPath) -> {
          var address = node.get("address");
          if (address == null || !address.isObject() || address.isEmpty()) {
            return Set.of(
                contextPath
                    + ".address must be functionally present and contain at least one field");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateTransport() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the \"transport\" object",
        body -> {
          var issues = new LinkedHashSet<String>();
          issues.addAll(checkTransportETA().validate(body));
          issues.addAll(checkPortOfDischargePresence().validate(body));
          issues.addAll(checkPortOfDischargeLocation().validate(body));
          issues.addAll(checkVesselVoyagesArray().validate(body));
          issues.addAll(checkVesselVoyageField("typeCode").validate(body));
          issues.addAll(checkVesselVoyageField("vesselName").validate(body));
          issues.addAll(checkVesselVoyageField("carrierVoyageNumber").validate(body));
          return issues;
        });
  }

  private static JsonContentCheck checkTransportETA() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of transport ETA fields",
        mav -> mav.path("arrivalNotices").all().path("transport").submitPath(),
        (node, contextPath) -> {
          if (!node.hasNonNull("etaAtPortOfDischargeDate")
              && !node.hasNonNull("etaAtPlaceOfDeliveryDate")) {
            return Set.of(
                contextPath
                    + ": must functionally include either 'etaAtPortOfDischargeDate' or 'etaAtPlaceOfDeliveryDate'");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck checkPortOfDischargePresence() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"portOfDischarge\" object",
        mav -> mav.path("arrivalNotices").all().path("transport").submitPath(),
        (node, contextPath) -> {
          var pod = node.get("portOfDischarge");
          if (pod == null) {
            return Set.of(contextPath + ".portOfDischarge must be functionally present");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck checkPortOfDischargeLocation() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of location information in \"portOfDischarge\"",
        mav -> mav.path("arrivalNotices").all().path("transport").submitPath(),
        (node, contextPath) -> {
          var pod = node.get("portOfDischarge");
          if (pod != null && pod.isObject()) {
            if (!pod.hasNonNull("address")
                && !pod.hasNonNull("UNLocationCode")
                && !pod.hasNonNull("facility")) {
              return Set.of(
                  contextPath
                      + ".portOfDischarge must functionally contain at least one of 'address', 'UNLocationCode', or 'facility'");
            }
          }
          return Set.of();
        });
  }

  private static JsonContentCheck checkVesselVoyagesArray() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"vesselVoyages\" array",
        mav -> mav.path("arrivalNotices").all().path("transport").submitPath(),
        (node, contextPath) -> {
          var voyages = node.get("vesselVoyages");
          if (voyages == null || !voyages.isArray() || voyages.isEmpty()) {
            return Set.of(contextPath + ".vesselVoyages must be functionally a non-empty array");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck checkVesselVoyageField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of \"" + field + "\" in vesselVoyages",
        mav -> mav.path("arrivalNotices").all().path("transport").all().path("vesselVoyages").all(),
        (node, contextPath) -> {
          if (!node.hasNonNull(field)) {
            return Set.of(contextPath + "." + field + " must be functionally present");
          }
          return Set.of();
        });
  }

  static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE =
      JsonAttribute.customValidator(
          "Every response received during a conformance test must not be empty",
          body -> body.isEmpty() ? Set.of("The response body must not be empty") : Set.of());

  private static JsonContentCheck checkFreeTimeAttribute(String attributeName) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"freeTime\" \""
            + attributeName
            + "\" attribute",
        mav -> mav.path("arrivalNotices").all().path("freeTimes").all(),
        (node, contextPath) -> {
          if (!node.hasNonNull(attributeName)) {
            return Set.of(
                String.format("%s.%s must be functionally present", contextPath, attributeName));
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateFreeTimeObjectStructure(String scenarioType) {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the \"freeTime\" object",
        body -> {
          var issues = new LinkedHashSet<String>();
          if ("FREE_TIME".equalsIgnoreCase(scenarioType)) {
            var arrivalNotices = body.path("arrivalNotices");
            for (int i = 0; i < arrivalNotices.size(); i++) {
              var freeTimes = arrivalNotices.get(i).path("freeTimes");
              if (!freeTimes.isArray() || freeTimes.isEmpty()) {
                issues.add(
                    "ArrivalNotice "
                        + i
                        + ": 'freeTimes' must be functionally present and contain at least one item for FREE_TIME scenario");
              }
            }
          }
          issues.addAll(checkFreeTimeAttribute("typeCode").validate(body));
          issues.addAll(checkFreeTimeAttribute("isoEquipmentCode").validate(body));
          issues.addAll(checkFreeTimeAttribute("duration").validate(body));
          issues.addAll(checkFreeTimeAttribute("timeUnit").validate(body));

          return issues;
        });
  }

  private static List<JsonContentCheck> getScenarioRelatedChecks(String scenarioType) {
    var checks = new ArrayList<JsonContentCheck>();

    switch (scenarioType) {
      case "FREE_TIME" -> checks.add(validateFreeTimeObjectStructure(scenarioType));
      case "FREIGHTED" -> checks.add(validateChargesStructure());
    }

    return checks;
  }

  private static JsonContentCheck checkChargeAttribute(String attributeName) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"charges\" \""
            + attributeName
            + "\" attribute",
        mav -> mav.path("arrivalNotices").all().path("charges").all(),
        (node, contextPath) -> {
          if (!node.hasNonNull(attributeName)) {
            return Set.of(
                String.format("%s.%s must be functionally present", contextPath, attributeName));
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateChargesStructure() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the \"charges\" object",
        body -> {
          var issues = new LinkedHashSet<String>();
          issues.addAll(checkChargeAttribute("chargeName").validate(body));
          issues.addAll(checkChargeAttribute("currencyAmount").validate(body));
          issues.addAll(checkChargeAttribute("currencyCode").validate(body));
          issues.addAll(checkChargeAttribute("paymentTermCode").validate(body));
          issues.addAll(checkChargeAttribute("unitPrice").validate(body));
          issues.addAll(checkChargeAttribute("quantity").validate(body));
          issues.addAll(checkChargeAttribute("carrierRateOfExchange").validate(body));
          return issues;
        });
  }

  public static ActionCheck getANGetResponseChecks(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    checks.addAll(checkValidTransportDocumentReferences(dspSupplier));
    return JsonAttribute.contentChecks(
        ANRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }

  private static List<JsonContentCheck> checkValidTransportDocumentReferences(
      Supplier<DynamicScenarioParameters> dsp) {
    var checks = new ArrayList<JsonContentCheck>();

    checks.add(
        JsonAttribute.customValidator(
            "[Scenario] Validate that each 'transportDocumentReference' in the response is valid",
            body -> {
              var issues = new LinkedHashSet<String>();

              var arrivalNotices = body.get("arrivalNotices");
              if (arrivalNotices == null || !arrivalNotices.isArray()) {
                issues.add("Missing or invalid 'arrivalNotices' array in payload.");
                return issues;
              }

              Set<String> validReferences = new HashSet<>(dsp.get().transportDocumentReferences());

              for (int i = 0; i < arrivalNotices.size(); i++) {
                var notice = arrivalNotices.get(i);
                var tdr = notice.get("transportDocumentReference");

                if (!validReferences.contains(tdr.asText())) {
                  issues.add(
                      String.format(
                          "ArrivalNotice %d: 'transportDocumentReference' '%s' is not a valid one",
                          i, tdr.asText()));
                }
              }

              return issues;
            }));

    return checks;
  }

  private static JsonContentCheck validateUtilizedTransportEquipments() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the \"utilizedTransportEquipments\" object",
        body -> {
          var issues = new LinkedHashSet<String>();

          issues.addAll(checkUTEEquipmentPresence().validate(body));
          issues.addAll(checkUTEEquipmentField("equipmentReference").validate(body));
          issues.addAll(checkUTEEquipmentField("ISOEquipmentCode").validate(body));
          issues.addAll(checkUTESealsPresence().validate(body));
          issues.addAll(checkUTESealNumber().validate(body));

          return issues;
        });
  }

  private static JsonContentCheck checkUTEEquipmentField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"utilizedTransportEquipments.equipment."
            + field
            + "\" attribute",
        mav ->
            mav.path("arrivalNotices")
                .all()
                .path("utilizedTransportEquipments")
                .all()
                .path("equipment")
                .submitPath(),
        (node, contextPath) -> {
          if (!node.hasNonNull(field)) {
            return Set.of(contextPath + "." + field + " must be functionally present");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck checkUTEEquipmentPresence() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"utilizedTransportEquipments.equipment\" object",
        mav -> mav.path("arrivalNotices").all().path("utilizedTransportEquipments").all(),
        (ute, contextPath) -> {
          if (ute.get("equipment") == null || ute.get("equipment").isNull()) {
            return Set.of(contextPath + ".equipment must be functionally present");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck checkUTESealsPresence() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the \"utilizedTransportEquipments.seals\" array",
        body -> {
          var issues = new LinkedHashSet<String>();
          var arrivalNotices = body.path("arrivalNotices");

          for (int i = 0; i < arrivalNotices.size(); i++) {
            var utes = arrivalNotices.get(i).path("utilizedTransportEquipments");
            for (int j = 0; j < utes.size(); j++) {
              var seals = utes.get(j).path("seals");
              if (!seals.isArray() || seals.isEmpty()) {
                issues.add(
                    String.format(
                        "arrivalNotices[%d].utilizedTransportEquipments[%d].seals must functionally be a non-empty array",
                        i, j));
              }
            }
          }

          return issues;
        });
  }

  private static JsonContentCheck checkUTESealNumber() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"utilizedTransportEquipments.seals.number\" attribute",
        mav ->
            mav.path("arrivalNotices")
                .all()
                .path("utilizedTransportEquipments")
                .all()
                .path("seals")
                .all(),
        (seal, contextPath) -> {
          if (!seal.hasNonNull("number")) {
            return Set.of(contextPath + ".number must be functionally present");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateConsignmentItems() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the \"consignmentItem\" object",
        body -> {
          var issues = new LinkedHashSet<String>();

          issues.addAll(checkConsignmentDescriptionOfGoods().validate(body));
          issues.addAll(checkCargoItemPresence().validate(body));
          issues.addAll(checkCargoItemField("equipmentReference").validate(body));
          issues.addAll(checkCargoItemField("cargoGrossWeight").validate(body));
          issues.addAll(checkOuterPackagingStructure().validate(body));
          issues.addAll(checkOuterPackagingFields().validate(body));

          return issues;
        });
  }

  private static JsonContentCheck checkConsignmentDescriptionOfGoods() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"descriptionOfGoods\" field",
        mav -> mav.path("arrivalNotices").all().path("consignmentItem").all(),
        (ci, contextPath) -> {
          var node = ci.get("descriptionOfGoods");
          if (node == null || !node.isArray() || node.isEmpty()) {
            return Set.of(
                contextPath
                    + ".descriptionOfGoods must functionally be a non-empty array of strings");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck checkCargoItemPresence() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"cargoItems\" array",
        mav -> mav.path("arrivalNotices").all().path("consignmentItem").all(),
        (ci, contextPath) -> {
          var items = ci.get("cargoItems");
          if (items == null || !items.isArray() || items.isEmpty()) {
            return Set.of(contextPath + ".cargoItems must functionally be a non-empty array");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck checkCargoItemField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"cargoItems."
            + field
            + "\" attribute",
        mav ->
            mav.path("arrivalNotices").all().path("consignmentItem").all().path("cargoItems").all(),
        (item, contextPath) -> {
          if (!item.hasNonNull(field)) {
            return Set.of(contextPath + "." + field + " must be functionally present");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck checkOuterPackagingStructure() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"outerPackaging\" object",
        mav ->
            mav.path("arrivalNotices").all().path("consignmentItem").all().path("cargoItems").all(),
        (item, contextPath) -> {
          var op = item.get("outerPackaging");
          if (op == null || !op.isObject() || op.isEmpty()) {
            return Set.of(contextPath + ".outerPackaging must functionally be a non-empty object");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck checkOuterPackagingFields() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the \"outerPackaging\" required fields",
        mav ->
            mav.path("arrivalNotices")
                .all()
                .path("consignmentItem")
                .all()
                .path("cargoItems")
                .all()
                .path("outerPackaging")
                .submitPath(),
        (op, contextPath) -> {
          var issues = new LinkedHashSet<String>();

          if (!op.hasNonNull("packageCode")
              && !op.hasNonNull("imoPackagingCode")
              && !op.hasNonNull("description")) {
            issues.add(
                contextPath
                    + " must contain at least one of 'packageCode', 'imoPackagingCode', or 'description'");
          }

          if (!op.hasNonNull("numberOfPackages")) {
            issues.add(contextPath + ".numberOfPackages must be functionally present");
          }

          return issues;
        });
  }

  private static final JsonContentCheck CHECK_PRESENCE_OF_REQUIRED_FIELDS =
      JsonAttribute.customValidator(
          "The Publisher has correctly demonstrated the use of functionally required attributes in the payload",
          body -> {
            Set<String> allIssues = new LinkedHashSet<>();
            allIssues.addAll(validateBasicFields().validate(body));
            allIssues.addAll(validateCarrierContactInformation().validate(body));
            allIssues.addAll(validateDocumentParties().validate(body));
            allIssues.addAll(validateTransport().validate(body));
            allIssues.addAll(validateUtilizedTransportEquipments().validate(body));
            allIssues.addAll(validateConsignmentItems().validate(body));

            return allIssues;
          });

  public static final List<JsonContentCheck> COMMON_PAYLOAD_CHECKS =
      Arrays.asList(VALIDATE_NON_EMPTY_RESPONSE, CHECK_PRESENCE_OF_REQUIRED_FIELDS);
}
