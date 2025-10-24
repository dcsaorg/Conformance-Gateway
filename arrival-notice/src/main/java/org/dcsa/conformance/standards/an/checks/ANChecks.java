package org.dcsa.conformance.standards.an.checks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.KeywordDataset;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.an.party.ANRole;
import org.dcsa.conformance.standards.an.party.DynamicScenarioParameters;

public class ANChecks {

  public static ActionCheck getANPostPayloadChecks(
      UUID matchedExchangeUuid, String expectedApiVersion, String scenarioType) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    checks.add(
        runIfBodyNotEmpty(
            "Payload checks (only when body is not empty)", () -> payloadChecks(scenarioType)));
    return JsonAttribute.contentChecks(
        "",
        "The Publisher has correctly demonstrated the use of functionally required attributes in the payload",
        ANRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        checks);
  }

  private static List<JsonContentCheck> payloadChecks(String scenarioType) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.addAll(validateBasicFields());
    checks.add(validateCarrierContactInformation());
    checks.add(validateDocumentParties());
    checks.addAll(validateDataSetFields());
    checks.add(validateTransport());
    checks.add(validateUtilizedTransportEquipments());
    checks.add(validateConsignmentItems());
    if (scenarioType != null) {
      checks.addAll(getScenarioRelatedChecks(scenarioType));
    }
    return checks;
  }

  private static JsonContentCheck runIfBodyNotEmpty(
      String description, Supplier<List<JsonContentCheck>> checksSupplier) {
    return JsonAttribute.customValidator(
        description,
        body -> {
          if (body == null || body.isEmpty()) {
            return Set.of();
          }
          var issues = new LinkedHashSet<String>();
          for (var check : checksSupplier.get()) {
            issues.addAll(check.validate(body));
          }
          return issues;
        });
  }

  public static List<JsonContentCheck> validateBasicFields() {
    return List.of(
        validateBasicFieldWithLabel("carrierCode", "arrivalNotices.*"),
        validateBasicFieldWithLabel("transportDocumentReference", "arrivalNotices.*"));
  }

  private static JsonContentCheck validateBasicFieldWithLabel(String field, String path) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the '" + field + "' attribute",
        mav -> mav.submitAllMatching(path),
        (node, contextPath) -> {
          if (!node.hasNonNull(field)) {
            return Set.of(
                contextPath + "." + field + " must be functionally present in the payload");
          }
          if (node.get(field).asText().isBlank()) {
            return Set.of(contextPath + "." + field + " must not be empty or blank in the payload");
          }

          return Set.of();
        });
  }

  public static List<JsonContentCheck> validateDataSetFields() {
    return List.of(
        validateDataSetFields(
            "carrierCodeListProvider", ANDatasets.CARRIER_CODE_LIST_PROVIDER, "arrivalNotices.*"),
        validateDataSetFields(
            "deliveryTypeAtDestination",
            ANDatasets.DELIVERY_TYPE_AT_DESTINATION,
            "arrivalNotices.*"),
        validateDataSetFields(
            "partyFunction", ANDatasets.PARTY_FUNCTION, "arrivalNotices.*.documentParties.*"),
        validateDataSetFields(
            "facilityCodeListProvider",
            ANDatasets.FACILITY_CODE_LIST_PROVIDER,
            "arrivalNotices.*.transport.portOfDischarge.facility"),
        validateDataSetFields(
            "unit",
            ANDatasets.CARGO_GROSS_WEIGHT_UNIT,
            "arrivalNotices.*.consignmentItems.*.cargoItems.*.cargoGrossWeight"));
  }

  public static JsonContentCheck validateDataSetFields(
      String attribute, KeywordDataset dataset, String path) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the '" + attribute + "' attribute",
        mav -> mav.submitAllMatching(path),
        (node, contextPath) -> {
          final var fieldNode = node.path(attribute);
          final String fieldPath = contextPath + "." + attribute;

          if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return Set.of(fieldPath + " must be functionally present in the payload");
          }
          if (fieldNode.asText().isBlank()) {
            return Set.of(fieldPath + " must not be empty or blank in the payload");
          }
          var validator = JsonAttribute.matchedMustBeDatasetKeywordIfPresent(dataset);

          return validator.validate(fieldNode, fieldPath);
        });
  }

  public static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE =
      JsonAttribute.customValidator(
          "Every response received during a conformance test must not be empty",
          body -> body.isEmpty() ? Set.of("The response body must not be empty") : Set.of());

  public static List<JsonContentCheck> getScenarioRelatedChecks(String scenarioType) {
    var checks = new ArrayList<JsonContentCheck>();

    if (scenarioType.equals(ScenarioType.FREE_TIME.name())) {
      checks.add(validateFreeTimeObjectStructure(scenarioType));
      checks.add(
          validateDataSetFields(
              "timeUnit", ANDatasets.FREE_TIME_TIME_UNIT, "arrivalNotices.*.freeTimes.*"));
    }

    if (scenarioType.equals(ScenarioType.FREIGHTED.name())) {
      checks.add(validateChargesStructure());
      checks.add(
          validateDataSetFields(
              "paymentTermCode", ANDatasets.PAYMENT_TERM_CODE, "arrivalNotices.*.charges.*"));
    }
    return checks;
  }

  public static ActionCheck getANGetResponseChecks(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    checks.addAll(validateTransportDocumentReferences(dspSupplier));
    checks.addAll(payloadChecks(dspSupplier.get().scenarioType()));
    return JsonAttribute.contentChecks(
        ANRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }

  public static ActionCheck getANNPostPayloadChecks(
      UUID matchedExchangeUuid, String expectedApiVersion) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    checks.add(
        validateBasicFieldWithLabel("transportDocumentReference", "arrivalNoticeNotifications.*"));
    checks.add(validateANNEquipmentReference());
    checks.add(validateTransportETA("arrivalNoticeNotifications.*"));
    checks.add(validatePODAdrressANN());
    checks.add(validatePortOfDischargeLocation("arrivalNoticeNotifications.*"));
    checks.add(
        validatePortOfDischargeFacilityFields(
            "facilityCode", "arrivalNoticeNotifications.*.portOfDischarge"));
    checks.add(
        validateDataSetFields(
            "facilityCodeListProvider",
            ANDatasets.FACILITY_CODE_LIST_PROVIDER,
            "arrivalNoticeNotifications.*.portOfDischarge.facility"));

    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    return JsonAttribute.contentChecks(
        ANRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        checks);
  }

  private static JsonContentCheck validateANNEquipmentReference() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'equipmentReferences' field",
        mav -> mav.submitAllMatching("arrivalNoticeNotifications.*"),
        (notifications, contextPath) -> {
          var node = notifications.get("equipmentReferences");
          if (node == null || !node.isArray() || node.isEmpty()) {
            return Set.of(
                contextPath
                    + ".equipmentReferences must functionally be a non-empty array of strings");
          }
          boolean allEmpty = true;
          for (var item : node) {
            if (item != null && item.isTextual() && !item.asText().trim().isEmpty()) {
              allEmpty = false;
              break;
            }
          }

          if (allEmpty) {
            return Set.of(
                contextPath
                    + ".equipmentReferences must contain at least one non-empty string value");
          }
          return Set.of();
        });
  }

  public static List<JsonContentCheck> validateTransportDocumentReferences(
      Supplier<DynamicScenarioParameters> dsp) {
    return List.of(
        JsonAttribute.customValidator(
            "[Scenario] Validate that all 'transportDocumentReference's in the response match the query parameters, and none are missing",
            body -> {
              var issues = new LinkedHashSet<String>();
              var arrivalNotices = body.get("arrivalNotices");

              if (arrivalNotices == null || !arrivalNotices.isArray()) {
                issues.add("Missing or invalid 'arrivalNotices' array in payload.");
                return issues;
              }

              Set<String> expectedTDRs = new HashSet<>(dsp.get().transportDocumentReferences());
              Set<String> foundTDRs = new HashSet<>();

              for (int i = 0; i < arrivalNotices.size(); i++) {
                var notice = arrivalNotices.get(i);
                var tdrNode = notice.get("transportDocumentReference");

                if (tdrNode == null || !tdrNode.isTextual()) {
                  issues.add(
                      String.format("ArrivalNotice %d: missing 'transportDocumentReference'", i));
                  continue;
                }

                String tdr = tdrNode.asText();
                foundTDRs.add(tdr);

                if (!expectedTDRs.contains(tdr)) {
                  issues.add(
                      String.format(
                          "ArrivalNotice %d: transportDocumentReference '%s' was not requested in query parameters",
                          i, tdr));
                }
              }

              Set<String> missingTDRs = new HashSet<>(expectedTDRs);
              missingTDRs.removeAll(foundTDRs);
              for (String missingTdr : missingTDRs) {
                issues.add(
                    String.format(
                        "No ArrivalNotice was returned for transportDocumentReference '%s' specified in query parameters",
                        missingTdr));
              }

              return issues;
            }));
  }

  public static JsonContentCheck validateCarrierContactInformation() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'carrierContactInformation' object",
        body -> {
          var issues = new LinkedHashSet<String>();
          issues.addAll(validateCarrierContactName().validate(body));
          issues.addAll(validateCarrierContactEmailOrPhone().validate(body));
          return issues;
        });
  }

  private static JsonContentCheck validateCarrierContactName() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'name' attribute in 'carrierContactInformation'",
        mav -> mav.submitAllMatching("arrivalNotices.*.carrierContactInformation.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull("name")) {
            return Set.of(contextPath + ".name must be functionally present");
          } else if (node.get("name").asText().isBlank()) {
            return Set.of(contextPath + ".name must not be empty or blank in the payload");
          }
          return Set.of();
        });
  }


  private static JsonContentCheck validateCarrierContactEmailOrPhone() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of either the 'email' or 'phone' attribute in 'carrierContactInformation'",
        mav -> mav.submitAllMatching("arrivalNotices.*.carrierContactInformation.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull("email") && !node.hasNonNull("phone")) {
            return Set.of(contextPath + " must functionally include either 'email' or 'phone'");
          }
          if (node.hasNonNull("email") && node.get("email").asText().isBlank()) {
            return Set.of(contextPath + ".'email' must not be empty or blank in the payload");
          }
          if (node.hasNonNull("phone") && node.get("phone").asText().isBlank()) {
            return Set.of(contextPath + ".'phone' must not be empty or blank in the payload");
          }
          return Set.of();
        });
  }

  public static JsonContentCheck validateDocumentParties() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'documentParties' object",
        body -> {
          var issues = new LinkedHashSet<String>();
          issues.addAll(validateDocumentPartyField("partyName").validate(body));
          issues.addAll(validateDocumentPartyField("partyContactDetails").validate(body));
          issues.addAll(validatePartyContactName().validate(body));
          issues.addAll(validatePartyContactEmailOrPhone().validate(body));
          issues.addAll(validateDocumentPartyAddress().validate(body));
          return issues;
        });
  }

  public static JsonContentCheck validatePartyContactName() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the '"
            + "name"
            + "' attribute in 'carrierContactInformation'",
        mav -> mav.submitAllMatching("arrivalNotices.*.documentParties.*.partyContactDetails.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull("name")) {
            return Set.of(contextPath + ". 'name' must be functionally present");
          } else if (node.get("name").asText().isBlank()) {
            return Set.of(contextPath + ". 'name' must not be empty or blank in the payload");
          }

          return Set.of();
        });
  }

  public static JsonContentCheck validatePartyContactEmailOrPhone() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of either the 'email' or 'phone' attribute in 'partyContactDetails'",
        mav -> mav.submitAllMatching("arrivalNotices.*.documentParties.*.partyContactDetails.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull("email") && !node.hasNonNull("phone")) {
            return Set.of(contextPath + " must functionally include either 'email' or 'phone'");
          }
          if (node.hasNonNull("email") && node.get("email").asText().isBlank()) {
            return Set.of(contextPath + ". 'email' must not be empty or blank in the payload");
          }
          if (node.hasNonNull("phone") && node.get("phone").asText().isBlank()) {
            return Set.of(contextPath + ". 'phone' must not be empty or blank in the payload");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateDocumentPartyField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the '"
            + field
            + "' attribute in 'documentParties'",
        mav -> mav.submitAllMatching("arrivalNotices.*.documentParties.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull(field)) {
            return Set.of(contextPath + "." + field + " must be functionally  present");
          }
          if (field.equals("partyName") && node.get(field).asText().isBlank()) {
            return Set.of(contextPath + "." + field + " must not be empty or blank in the payload");
          }
          return Set.of();
        });
  }

  public static JsonContentCheck validateAddress(String field, String path) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'address' object in " + field,
        mav -> mav.submitAllMatching(path),
        (node, contextPath) -> {
          var address = node.get("address");
          if (address != null && !address.isEmpty()) {
            boolean hasNonEmpty =
                ADDRESS_FIELDS.stream()
                    .anyMatch(
                        f -> {
                          var fieldNode = address.get(f);
                          return fieldNode != null
                              && fieldNode.isTextual()
                              && !fieldNode.asText().isBlank();
                        });

            if (!hasNonEmpty) {
              return Set.of(
                  contextPath
                      + ".address must contain at least one non-empty value among: "
                      + String.join(", ", ADDRESS_FIELDS));
            }
          }
          return Set.of();
        });
  }

  private static final List<String> ADDRESS_FIELDS =
      List.of(
          "street",
          "streetNumber",
          "floor",
          "postCode",
          "POBox",
          "city",
          "stateRegion",
          "countryCode");

  public static JsonContentCheck validateTransport() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'transport' object",
        body -> {
          var issues = new LinkedHashSet<String>();
          issues.addAll(validateTransportETA("arrivalNotices.*.transport").validate(body));
          issues.addAll(validatePortOfDischargePresence().validate(body));
          issues.addAll(
              validatePortOfDischargeLocation("arrivalNotices.*.transport").validate(body));
          issues.addAll(validatePODAdrressAN().validate(body));
          issues.addAll(
              validatePortOfDischargeFacilityFields(
                      "facilityCode", "arrivalNotices.*.transport.portOfDischarge")
                  .validate(body));
          issues.addAll(validateVesselVoyage().validate(body));
          issues.addAll(validateVesselVoyageField("vesselName").validate(body));
          issues.addAll(validateVesselVoyageField("carrierImportVoyageNumber").validate(body));
          return issues;
        });
  }

  private static JsonContentCheck validateTransportETA(String path) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of transport ETA fields",
        mav -> mav.submitAllMatching(path),
        (node, contextPath) -> {
          boolean hasPortOfDischargeValue =
              node.hasNonNull("portOfDischargeArrivalDate")
                  && node.path("portOfDischargeArrivalDate").hasNonNull("value");

          boolean hasPlaceOfDeliveryValue =
              node.hasNonNull("placeOfDeliveryArrivalDate")
                  && node.path("placeOfDeliveryArrivalDate").hasNonNull("value");

          if (!hasPortOfDischargeValue && !hasPlaceOfDeliveryValue) {
            return Set.of(
                contextPath
                    + ": must functionally include either 'portOfDischargeArrivalDate.value' "
                    + "or 'placeOfDeliveryArrivalDate.value'");
          }
          return Set.of();
        });
  }


  private static JsonContentCheck validatePortOfDischargePresence() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'portOfDischarge' object",
        mav -> mav.submitAllMatching("arrivalNotices.*.transport"),
        (node, contextPath) -> {
          var pod = node.get("portOfDischarge");
          if (pod == null) {
            return Set.of(contextPath + ".portOfDischarge must be functionally present");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validatePortOfDischargeLocation(String path) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of location information in 'portOfDischarge'",
        mav -> mav.submitAllMatching(path),
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
            if (pod.hasNonNull("UNLocationCode") && pod.get("UNLocationCode").asText().isBlank()) {
              return Set.of(
                  contextPath
                      + ".portOfDischarge.UNLocationCode must not be empty or blank in the payload");
            }
            if (pod.hasNonNull("address")
                && pod.get("address").isObject()
                && pod.get("address").isEmpty()) {
              return Set.of(
                  contextPath
                      + ".portOfDischarge.address if present must contain at least one field");
            }
          }
          return Set.of();
        });
  }

  public static JsonContentCheck validateDocumentPartyAddress() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of location information in 'portOfDischarge'",
        mav -> mav.submitAllMatching("arrivalNotices.*.documentParties.*"),
        (node, contextPath) -> {
          if (node.get("address") == null) {
            return Set.of(contextPath + ".address must be functionally present");
          }
          if (node.hasNonNull("address")
              && node.get("address").isObject()
              && node.get("address").isEmpty()) {
            return Set.of(
                contextPath
                    + ".address must be functionally present and contain at least one field");
          } else {
            boolean hasNonEmpty =
                ADDRESS_FIELDS.stream()
                    .anyMatch(
                        f -> {
                          var fieldNode = node.get("address").get(f);
                          return fieldNode != null
                              && fieldNode.isTextual()
                              && !fieldNode.asText().isBlank();
                        });

            if (!hasNonEmpty) {
              return Set.of(
                  contextPath
                      + ".address must contain at least one non-empty value among: "
                      + String.join(", ", ADDRESS_FIELDS));
            }
          }
          return Set.of();
        });
  }

  public static JsonContentCheck validatePODAdrressAN() {
    return validateAddress("portOfDischarge", "arrivalNotices.*.transport.portOfDischarge");
  }

  public static JsonContentCheck validatePODAdrressANN() {
    return validateAddress("portOfDischarge", "arrivalNoticeNotifications.*.portOfDischarge");
  }

  public static JsonContentCheck validatePortOfDischargeFacilityFields(String field, String path) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of facility location "
            + field
            + " in 'portOfDischarge'",
        mav -> mav.submitAllMatching(path),
        (node, contextPath) -> {
          var facility = node.get("facility");
          if (facility != null && facility.isObject()) {

            if (!facility.hasNonNull(field)) {
              return Set.of(contextPath + ".facility must functionally contain " + field);
            } else if (facility.get(field).asText().isBlank()) {
              return Set.of(
                  contextPath
                      + ".facility."
                      + field
                      + " must not be empty or blank in the payload");
            }
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateVesselVoyage() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'vesselVoyages' array",
        mav -> mav.submitAllMatching("arrivalNotices.*.transport.legs.*"),
        (node, contextPath) -> {
          var voyage = node.get("vesselVoyage");
          if (voyage == null || voyage.isEmpty()) {
            return Set.of(contextPath + ".vesselVoyage must be functionally present");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateVesselVoyageField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of '" + field + "' in vesselVoyages",
        mav -> mav.submitAllMatching("arrivalNotices.*.transport.legs.*.vesselVoyage"),
        (node, contextPath) -> {
          if (!node.hasNonNull(field)) {
            return Set.of(contextPath + "." + field + " must be functionally present");
          } else if (node.get(field).asText().isBlank()) {
            return Set.of(contextPath + "." + field + " must not be empty or blank in the payload");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateFreeTimeAttribute(String attributeName) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'freeTime' '"
            + attributeName
            + "' attribute",
        mav -> mav.submitAllMatching("arrivalNotices.*.freeTimes.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull(attributeName)) {
            return Set.of(
                String.format("%s.%s must be functionally present", contextPath, attributeName));
          }
          return Set.of();
        });
  }

  public static JsonContentCheck validateFreeTimeObjectStructure(String scenarioType) {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'freeTime' object",
        body -> {
          var issues = new LinkedHashSet<String>();
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
          issues.addAll(validateFreeTimeArrayAttribute("typeCodes").validate(body));
          issues.addAll(validateFreeTimeArrayAttribute("ISOEquipmentCodes").validate(body));
          issues.addAll(validateFreeTimeArrayAttribute("equipmentReferences").validate(body));
          issues.addAll(validateFreeTimeAttribute("duration").validate(body));
          issues.addAll(validateFreeTimeAttribute("timeUnit").validate(body));

          return issues;
        });
  }


  private static JsonContentCheck validateFreeTimeArrayAttribute(String attributeName) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'freeTime' '"
            + attributeName
            + "' attribute",
        mav -> mav.submitAllMatching("arrivalNotices.*.freeTimes.*"),
        (node, contextPath) -> {
          var arrayNode = node.get(attributeName);
          if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) {
            return Set.of(
                String.format(
                    "%s.%s must be functionally present and be a non-empty array",
                    contextPath, attributeName));
          }

          boolean allEmpty = true;
          for (var element : arrayNode) {
            if (element != null && element.isTextual() && !element.asText().trim().isEmpty()) {
              allEmpty = false;
              break;
            }
          }
          if (allEmpty) {
            return Set.of(
                String.format(
                    "%s.%s must contain at least one non-empty value", contextPath, attributeName));
          }

          if (attributeName.equals("typeCodes")) {
            var issues = new LinkedHashSet<String>();
            for (var element : arrayNode) {
              var validator =
                  JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                      ANDatasets.FREE_TIME_TYPE_CODES);
              issues.addAll(validator.validate(element, contextPath + "." + attributeName));
            }
            return issues;
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateChargeAttribute(String attributeName) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'charges' '"
            + attributeName
            + "' attribute",
        mav -> mav.submitAllMatching("arrivalNotices.*.charges.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull(attributeName)) {
            return Set.of(
                String.format("%s.%s must be functionally present", contextPath, attributeName));
          } else if (node.get(attributeName).asText().isBlank()) {
            return Set.of(
                String.format(
                    "%s.%s must not be empty or blank in the payload", contextPath, attributeName));
          }
          return Set.of();
        });
  }

  public static JsonContentCheck validateChargesStructure() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'charges' object",
        body -> {
          var issues = new LinkedHashSet<String>();
          var arrivalNotices = body.path("arrivalNotices");
          for (int i = 0; i < arrivalNotices.size(); i++) {
            var freeTimes = arrivalNotices.get(i).path("charges");
            if (freeTimes == null || !freeTimes.isArray() || freeTimes.isEmpty()) {
              issues.add(
                  "ArrivalNotice "
                      + i
                      + ": 'charges' must be functionally present and contain at least one item for FREIGHTED scenario");
            }
          }
          issues.addAll(validateChargeAttribute("chargeName").validate(body));
          issues.addAll(validateChargeAttribute("currencyAmount").validate(body));
          issues.addAll(validateChargeAttribute("currencyCode").validate(body));
          issues.addAll(validateChargeAttribute("unitPrice").validate(body));
          issues.addAll(validateChargeAttribute("quantity").validate(body));
          return issues;
        });
  }

  public static JsonContentCheck validateUtilizedTransportEquipments() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'utilizedTransportEquipments' object",
        body -> {
          var issues = new LinkedHashSet<String>();

          issues.addAll(validateUTEEquipmentPresence().validate(body));
          issues.addAll(validateUTEEquipmentField("equipmentReference").validate(body));
          issues.addAll(validateUTEEquipmentField("ISOEquipmentCode").validate(body));
          issues.addAll(validateUTESealsPresence().validate(body));
          issues.addAll(validateUTESealNumber().validate(body));

          return issues;
        });
  }

  private static JsonContentCheck validateUTEEquipmentField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'utilizedTransportEquipments.equipment."
            + field
            + "' attribute",
        mav -> mav.submitAllMatching("arrivalNotices.*.utilizedTransportEquipments.*.equipment"),
        (node, contextPath) -> {
          if (!node.hasNonNull(field)) {
            return Set.of(contextPath + "." + field + " must be functionally present");
          } else if (node.get(field).asText().isBlank()) {
            return Set.of(contextPath + "." + field + " must not be empty or blank in the payload");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateUTEEquipmentPresence() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'utilizedTransportEquipments.equipment' object",
        mav -> mav.submitAllMatching("arrivalNotices.*.utilizedTransportEquipments.*"),
        (ute, contextPath) -> {
          if (ute.get("equipment") == null || ute.get("equipment").isNull()) {
            return Set.of(contextPath + ".equipment must be functionally present");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateUTESealsPresence() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'utilizedTransportEquipments.seals' array",
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

  private static JsonContentCheck validateUTESealNumber() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'utilizedTransportEquipments.seals.number' attribute",
        mav -> mav.submitAllMatching("arrivalNotices.*.utilizedTransportEquipments.*.seals.*"),
        (seal, contextPath) -> {
          if (!seal.hasNonNull("number")) {
            return Set.of(contextPath + ".number must be functionally present");
          } else if (seal.get("number").asText().isBlank())
            return Set.of(contextPath + ".number must not be empty or blank in the payload");
          {
          }
          return Set.of();
        });
  }

  public static JsonContentCheck validateConsignmentItems() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'consignmentItem' object",
        body -> {
          var issues = new LinkedHashSet<String>();

          issues.addAll(validateConsignmentItemsDescriptionOfGoods().validate(body));
          issues.addAll(validateCargoItemPresence().validate(body));
          issues.addAll(validateCargoItemField("equipmentReference").validate(body));
          issues.addAll(validateCargoItemField("cargoGrossWeight").validate(body));
          issues.addAll(validateCargoGrossWeightField("value").validate(body));
          issues.addAll(validateCargoGrossWeightField("unit").validate(body));
          issues.addAll(validateOuterPackagingStructure().validate(body));
          issues.addAll(validateOuterPackagingFields().validate(body));

          return issues;
        });
  }
  private static JsonContentCheck validateConsignmentItemsDescriptionOfGoods() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'descriptionOfGoods' field",
        mav -> mav.submitAllMatching("arrivalNotices.*.consignmentItems.*"),
        (ci, contextPath) -> {
          var node = ci.get("descriptionOfGoods");
          if (node == null || !node.isArray() || node.isEmpty()) {
            return Set.of(
                contextPath
                    + ".descriptionOfGoods must functionally be a non-empty array of strings");
          }
          boolean allEmpty = true;
          for (var item : node) {
            if (item != null && item.isTextual() && !item.asText().trim().isEmpty()) {
              allEmpty = false;
              break;
            }
          }

          if (allEmpty) {
            return Set.of(
                contextPath
                    + ".descriptionOfGoods must contain at least one non-empty string value");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateCargoItemPresence() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'cargoItems' array",
        mav -> mav.submitAllMatching("arrivalNotices.*.consignmentItems.*"),
        (ci, contextPath) -> {
          var items = ci.get("cargoItems");
          if (items == null || !items.isArray() || items.isEmpty()) {
            return Set.of(contextPath + ".cargoItems must functionally be a non-empty array");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateCargoItemField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'cargoItems."
            + field
            + "' attribute",
        mav -> mav.submitAllMatching("arrivalNotices.*.consignmentItems.*.cargoItems.*"),
        (item, contextPath) -> {
          if (!item.hasNonNull(field)) {
            return Set.of(contextPath + "." + field + " must be functionally present");
          } else if (!field.equals("cargoGrossWeight") && item.get(field).asText().isBlank()) {
            return Set.of(contextPath + "." + field + " must not be empty or blank in the payload");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateCargoGrossWeightField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'cargoGrossWeight."
            + field
            + "' attribute",
        mav ->
            mav.submitAllMatching(
                "arrivalNotices.*.consignmentItems.*.cargoItems.*.cargoGrossWeight"),
        (item, contextPath) -> {
          if (!item.hasNonNull(field)) {
            return Set.of(contextPath + "." + field + " must be functionally present");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateOuterPackagingStructure() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'outerPackaging' object",
        mav -> mav.submitAllMatching("arrivalNotices.*.consignmentItems.*.cargoItems.*"),
        (item, contextPath) -> {
          var op = item.get("outerPackaging");
          if (op == null || !op.isObject() || op.isEmpty()) {
            return Set.of(contextPath + ".outerPackaging must functionally be a non-empty object");
          }
          return Set.of();
        });
  }

  private static JsonContentCheck validateOuterPackagingFields() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'outerPackaging' required fields",
        mav ->
            mav.submitAllMatching(
                "arrivalNotices.*.consignmentItems.*.cargoItems.*.outerPackaging."),
        (op, contextPath) -> {
          var issues = new LinkedHashSet<String>();

          if (!op.hasNonNull("packageCode")
              && !op.hasNonNull("IMOPackagingCode")
              && !op.hasNonNull("description")) {
            issues.add(
                contextPath
                    + " must contain at least one of 'packageCode', 'IMOPackagingCode', or 'description'");
          }

          if (op.get("description") != null && op.get("description").asText().isBlank()) {
            issues.add(contextPath + ".description must not be empty or blank in the payload");
          }
          if (op.get("packageCode") != null && op.get("packageCode").asText().isBlank()) {
            issues.add(contextPath + ".packageCode must not be empty or blank in the payload");
          }
          if (op.get("IMOPackagingCode") != null && op.get("IMOPackagingCode").asText().isBlank()) {
            issues.add(contextPath + ".IMOPackagingCode must not be empty or blank in the payload");
          }
          if (!op.hasNonNull("numberOfPackages")) {
            issues.add(contextPath + ".numberOfPackages must be functionally present");
          }

          return issues;
        });
  }


}
