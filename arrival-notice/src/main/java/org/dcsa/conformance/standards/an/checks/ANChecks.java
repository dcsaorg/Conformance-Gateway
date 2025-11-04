package org.dcsa.conformance.standards.an.checks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
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
    checks.addAll(payloadChecks(scenarioType));
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
    var checks = new ArrayList<>(validateBasicFields());
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
            return ConformanceCheckResult.simple(Set.of(
                contextPath + "." + field + " must be functionally present in the payload"));
          }
          if (node.get(field).asText().isBlank()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + "." + field + " must not be empty or blank in the payload"));
          }

          return ConformanceCheckResult.simple(Set.of());
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
            return ConformanceCheckResult.simple(Set.of(fieldPath + " must be functionally present in the payload"));
          }
          if (fieldNode.asText().isBlank()) {
            return ConformanceCheckResult.simple(Set.of(fieldPath + " must not be empty or blank in the payload"));
          }
          var validator = JsonAttribute.matchedMustBeDatasetKeywordIfPresent(dataset);

          return validator.validate(fieldNode, fieldPath);
        });
  }

  public static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE =
      JsonAttribute.customValidator(
          "Every response received during a conformance test must not be empty",
          body -> ConformanceCheckResult.simple(body.isEmpty() ? Set.of("The response body must not be empty") : Set.of()));

  public static List<JsonContentCheck> getScenarioRelatedChecks(String scenarioType) {
    var checks = new ArrayList<JsonContentCheck>();

    if (scenarioType.equals(ScenarioType.FREE_TIME.name())) {
      checks.add(validateFreeTimeObjectStructure());
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
            return ConformanceCheckResult.simple(Set.of(
                contextPath
                    + ".equipmentReferences must functionally be a non-empty array of strings"));
          }
          boolean allEmpty = true;
          for (var item : node) {
            if (item != null && item.isTextual() && !item.asText().trim().isEmpty()) {
              allEmpty = false;
              break;
            }
          }

          if (allEmpty) {
            return ConformanceCheckResult.simple(Set.of(
                contextPath
                    + ".equipmentReferences must contain at least one non-empty string value"));
          }
          return ConformanceCheckResult.simple(Set.of());
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
                return ConformanceCheckResult.simple(issues);
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

              return ConformanceCheckResult.simple(issues);
            }));
  }

  public static JsonContentCheck validateCarrierContactInformation() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'carrierContactInformation' object",
        body -> {
          var issues = new LinkedHashSet<String>();
          issues.addAll(validateCarrierContactName().validate(body).getErrorMessages());
          issues.addAll(validateCarrierContactEmailOrPhone().validate(body).getErrorMessages());
          return ConformanceCheckResult.simple(issues);
        });
  }

  private static JsonContentCheck validateCarrierContactName() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'name' attribute in 'carrierContactInformation'",
        mav -> mav.submitAllMatching("arrivalNotices.*.carrierContactInformation.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull("name")) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ".name must be functionally present"));
          } else if (node.get("name").asText().isBlank()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ".name must not be empty or blank in the payload"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }


  private static JsonContentCheck validateCarrierContactEmailOrPhone() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of either the 'email' or 'phone' attribute in 'carrierContactInformation'",
        mav -> mav.submitAllMatching("arrivalNotices.*.carrierContactInformation.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull("email") && !node.hasNonNull("phone")) {
            return ConformanceCheckResult.simple(Set.of(contextPath + " must functionally include either 'email' or 'phone'"));
          }
          if (node.hasNonNull("email") && node.get("email").asText().isBlank()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ".'email' must not be empty or blank in the payload"));
          }
          if (node.hasNonNull("phone") && node.get("phone").asText().isBlank()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ".'phone' must not be empty or blank in the payload"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck validateDocumentParties() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'documentParties' object",
        body -> {
          var results = new LinkedHashSet<ConformanceCheckResult>();
          results.add(validateDocumentPartyField("partyName").validate(body));
          results.add(validateDocumentPartyField("partyContactDetails").validate(body));
          results.add(validatePartyContactName().validate(body));
          results.add(validatePartyContactEmailOrPhone().validate(body));
          results.add(validateDocumentPartyAddress().validate(body));
          return ConformanceCheckResult.from(results);
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
            return ConformanceCheckResult.simple(Set.of(contextPath + ". 'name' must be functionally present"));
          } else if (node.get("name").asText().isBlank()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ". 'name' must not be empty or blank in the payload"));
          }

          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck validatePartyContactEmailOrPhone() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of either the 'email' or 'phone' attribute in 'partyContactDetails'",
        mav -> mav.submitAllMatching("arrivalNotices.*.documentParties.*.partyContactDetails.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull("email") && !node.hasNonNull("phone")) {
            return ConformanceCheckResult.simple(Set.of(contextPath + " must functionally include either 'email' or 'phone'"));
          }
          if (node.hasNonNull("email") && node.get("email").asText().isBlank()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ". 'email' must not be empty or blank in the payload"));
          }
          if (node.hasNonNull("phone") && node.get("phone").asText().isBlank()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ". 'phone' must not be empty or blank in the payload"));
          }
          return ConformanceCheckResult.simple(Set.of());
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
            return ConformanceCheckResult.simple(Set.of(contextPath + "." + field + " must be functionally  present"));
          }
          if (field.equals("partyName") && node.get(field).asText().isBlank()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + "." + field + " must not be empty or blank in the payload"));
          }
          return ConformanceCheckResult.simple(Set.of());
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
              return ConformanceCheckResult.simple(Set.of(
                  contextPath
                      + ".address must contain at least one non-empty value among: "
                      + String.join(", ", ADDRESS_FIELDS)));
            }
          }
          return ConformanceCheckResult.simple(Set.of());
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
          var checkResults = new LinkedHashSet<ConformanceCheckResult>();
          checkResults.add(validateTransportETA("arrivalNotices.*.transport").validate(body));
          checkResults.add(validatePortOfDischargePresence().validate(body));
          checkResults.add(
              validatePortOfDischargeLocation("arrivalNotices.*.transport").validate(body));
          checkResults.add(validatePODAdrressAN().validate(body));
          checkResults.add(validateVesselVoyage().validate(body));
          checkResults.add(validateVesselVoyageField("vesselName").validate(body));
          checkResults.add(validateVesselVoyageField("carrierImportVoyageNumber").validate(body));
          return ConformanceCheckResult.from(checkResults);
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
            return ConformanceCheckResult.simple(Set.of(
                contextPath
                    + ": must functionally include either 'portOfDischargeArrivalDate.value' "
                    + "or 'placeOfDeliveryArrivalDate.value'"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }


  private static JsonContentCheck validatePortOfDischargePresence() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'portOfDischarge' object",
        mav -> mav.submitAllMatching("arrivalNotices.*.transport"),
        (node, contextPath) -> {
          var pod = node.get("portOfDischarge");
          if (pod == null) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ".portOfDischarge must be functionally present"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck validatePortOfDischargeLocation(String path) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of location information in 'portOfDischarge'",
        mav -> mav.submitAllMatching(path),
        (node, contextPath) -> {
          var pod = node.get("portOfDischarge");
          if (pod != null && pod.isObject()) {
            boolean hasAddress = pod.hasNonNull("address");
            boolean hasUNLocationCode = pod.hasNonNull("UNLocationCode");
            boolean hasFacility = pod.hasNonNull("facility");

            if (!hasAddress && !hasUNLocationCode && !hasFacility) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      contextPath
                          + ".portOfDischarge must functionally contain at least one of 'address', 'UNLocationCode', or 'facility'"));
            }
            Set<String> messages = new HashSet<>();

            if (hasUNLocationCode && pod.get("UNLocationCode").asText().isBlank()) {
              messages.add(
                  contextPath
                      + ".portOfDischarge.UNLocationCode must not be empty or blank in the payload");
            }

            if (hasAddress && pod.get("address").isObject() && pod.get("address").isEmpty()) {
              messages.add(
                  contextPath
                      + ".portOfDischarge.address if present must contain at least one field");
            }

            if (hasFacility) {
              var facility = pod.get("facility");
              if (facility != null && facility.isObject()) {
                boolean hasFacilityCode = facility.hasNonNull("facilityCode");
                boolean hasFacilityCodeListProvider =
                    facility.hasNonNull("facilityCodeListProvider");

                if (!hasFacilityCode || !hasFacilityCodeListProvider) {
                  messages.add(
                      contextPath
                          + ".portOfDischarge.facility must functionally contain 'facilityCode' and 'facilityCodeListProvider'");
                } else {
                  if (facility.get("facilityCode").asText().isBlank()
                      || facility.get("facilityCodeListProvider").asText().isBlank()) {
                    messages.add(
                        contextPath
                            + ".portOfDischarge.facility.facilityCode and "
                            + contextPath
                            + ".portOfDischarge.facility.facilityCodeListProvider must not be empty or blank in the payload");
                  } else if (facility.hasNonNull("facilityCodeListProvider")) {
                    var result =
                        JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                                ANDatasets.FACILITY_CODE_LIST_PROVIDER)
                            .validate(
                                facility.get("facilityCodeListProvider"),
                                contextPath + ".portOfDischarge.facility.facilityCodeListProvider");

                    messages.addAll(result.getErrorMessages());
                  }
                }
              }
            }

            return ConformanceCheckResult.simple(messages);
          }

          // No portOfDischarge object — no messages
          return ConformanceCheckResult.simple(Set.of());
        });
  }


  public static JsonContentCheck validateDocumentPartyAddress() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of location information in 'portOfDischarge'",
        mav -> mav.submitAllMatching("arrivalNotices.*.documentParties.*"),
        (node, contextPath) -> {
          if (node.get("address") == null) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ".address must be functionally present"));
          }
          if (node.hasNonNull("address")
              && node.get("address").isObject()
              && node.get("address").isEmpty()) {
            return ConformanceCheckResult.simple(Set.of(
                contextPath
                    + ".address must be functionally present and contain at least one field"));
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
              return ConformanceCheckResult.simple(Set.of(
                  contextPath
                      + ".address must contain at least one non-empty value among: "
                      + String.join(", ", ADDRESS_FIELDS)));
            }
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck validatePODAdrressAN() {
    return validateAddress("portOfDischarge", "arrivalNotices.*.transport.portOfDischarge");
  }

  public static JsonContentCheck validatePODAdrressANN() {
    return validateAddress("portOfDischarge", "arrivalNoticeNotifications.*.portOfDischarge");
  }

  private static JsonContentCheck validateVesselVoyage() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'vesselVoyages' array",
        mav -> mav.submitAllMatching("arrivalNotices.*.transport.legs.*"),
        (node, contextPath) -> {
          var voyage = node.get("vesselVoyage");
          if (voyage == null || voyage.isEmpty()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ".vesselVoyage must be functionally present"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static JsonContentCheck validateVesselVoyageField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of '" + field + "' in vesselVoyages",
        mav -> mav.submitAllMatching("arrivalNotices.*.transport.legs.*.vesselVoyage"),
        (node, contextPath) -> {
          if (!node.hasNonNull(field)) {
            return ConformanceCheckResult.simple(Set.of(contextPath + "." + field + " must be functionally present"));
          } else if (node.get(field).asText().isBlank()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + "." + field + " must not be empty or blank in the payload"));
          }
          return ConformanceCheckResult.simple(Set.of());
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
            return ConformanceCheckResult.simple(Set.of(
                String.format("%s.%s must be functionally present", contextPath, attributeName)));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck validateFreeTimeObjectStructure() {
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
          issues.addAll(validateFreeTimeArrayAttribute("typeCodes").validate(body).getErrorMessages());
          issues.addAll(validateFreeTimeArrayAttribute("ISOEquipmentCodes").validate(body).getErrorMessages());
          issues.addAll(validateFreeTimeArrayAttribute("equipmentReferences").validate(body).getErrorMessages());
          issues.addAll(validateFreeTimeAttribute("duration").validate(body).getErrorMessages());
          issues.addAll(validateFreeTimeAttribute("timeUnit").validate(body).getErrorMessages());

          return ConformanceCheckResult.simple(issues);
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
            return ConformanceCheckResult.simple(
                Set.of(
                    String.format(
                        "%s.%s must be functionally present and be a non-empty array",
                        contextPath, attributeName)));
          }

          boolean allEmpty = true;
          for (var element : arrayNode) {
            if (element != null && element.isTextual() && !element.asText().trim().isEmpty()) {
              allEmpty = false;
              break;
            }
          }
          if (allEmpty) {
            return ConformanceCheckResult.simple(
                Set.of(
                    String.format(
                        "%s.%s must contain at least one non-empty value",
                        contextPath, attributeName)));
          }

          if (attributeName.equals("typeCodes")) {
            var results = new LinkedHashSet<ConformanceCheckResult>();
            for (var element : arrayNode) {
              var validator =
                  JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                      ANDatasets.FREE_TIME_TYPE_CODES);
              results.add(validator.validate(element, contextPath + "." + attributeName));
            }
            return ConformanceCheckResult.from(results);
          }
          return ConformanceCheckResult.simple(Set.of());
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
            return ConformanceCheckResult.simple(Set.of(
                String.format("%s.%s must be functionally present", contextPath, attributeName)));
          } else if (node.get(attributeName).asText().isBlank()) {
            return ConformanceCheckResult.simple(Set.of(
                String.format(
                    "%s.%s must not be empty or blank in the payload", contextPath, attributeName)));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck validateChargesStructure() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'charges' object",
        body -> {
          var results = new LinkedHashSet<ConformanceCheckResult>();
          var arrivalNotices = body.path("arrivalNotices");
          for (int i = 0; i < arrivalNotices.size(); i++) {
            var freeTimes = arrivalNotices.get(i).path("charges");
            if (freeTimes == null || !freeTimes.isArray() || freeTimes.isEmpty()) {
              results.add(
                  ConformanceCheckResult.simple(
                      Set.of(
                          "ArrivalNotice "
                              + i
                              + ": 'charges' must be functionally present and contain at least one item for FREIGHTED scenario")));
            }
          }
          results.add(validateChargeAttribute("chargeName").validate(body));
          results.add(validateChargeAttribute("currencyAmount").validate(body));
          results.add(validateChargeAttribute("currencyCode").validate(body));
          results.add(validateChargeAttribute("unitPrice").validate(body));
          results.add(validateChargeAttribute("quantity").validate(body));
          return ConformanceCheckResult.from(results);
        });
  }

  public static JsonContentCheck validateUtilizedTransportEquipments() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'utilizedTransportEquipments' object",
        body -> {
          var issues = new LinkedHashSet<String>();

          issues.addAll(validateUTEEquipmentPresence().validate(body).getErrorMessages());
          issues.addAll(validateUTEEquipmentField("equipmentReference").validate(body).getErrorMessages());
          issues.addAll(validateUTEEquipmentField("ISOEquipmentCode").validate(body).getErrorMessages());
          issues.addAll(validateUTESealsPresence().validate(body).getErrorMessages());
          issues.addAll(validateUTESealNumber().validate(body).getErrorMessages());

          return ConformanceCheckResult.simple(issues);
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
            return ConformanceCheckResult.simple(Set.of(contextPath + "." + field + " must be functionally present"));
          } else if (node.get(field).asText().isBlank()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + "." + field + " must not be empty or blank in the payload"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static JsonContentCheck validateUTEEquipmentPresence() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'utilizedTransportEquipments.equipment' object",
        mav -> mav.submitAllMatching("arrivalNotices.*.utilizedTransportEquipments.*"),
        (ute, contextPath) -> {
          if (ute.get("equipment") == null || ute.get("equipment").isNull()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ".equipment must be functionally present"));
          }
          return ConformanceCheckResult.simple(Set.of());
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

          return ConformanceCheckResult.simple(issues);
        });
  }

  private static JsonContentCheck validateUTESealNumber() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'utilizedTransportEquipments.seals.number' attribute",
        mav -> mav.submitAllMatching("arrivalNotices.*.utilizedTransportEquipments.*.seals.*"),
        (seal, contextPath) -> {
          if (!seal.hasNonNull("number")) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ".number must be functionally present"));
          } else if (seal.get("number").asText().isBlank())
            return ConformanceCheckResult.simple(Set.of(contextPath + ".number must not be empty or blank in the payload"));
          {
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck validateConsignmentItems() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'consignmentItem' object",
        body -> {
          var issues = new LinkedHashSet<String>();

          issues.addAll(validateConsignmentItemsDescriptionOfGoods().validate(body).getErrorMessages());
          issues.addAll(validateCargoItemPresence().validate(body).getErrorMessages());
          issues.addAll(validateCargoItemField("equipmentReference").validate(body).getErrorMessages());
          issues.addAll(validateCargoItemField("cargoGrossWeight").validate(body).getErrorMessages());
          issues.addAll(validateCargoGrossWeightField("value").validate(body).getErrorMessages());
          issues.addAll(validateCargoGrossWeightField("unit").validate(body).getErrorMessages());
          issues.addAll(validateOuterPackagingStructure().validate(body).getErrorMessages());
          issues.addAll(validateOuterPackagingFields().validate(body).getErrorMessages());

          return ConformanceCheckResult.simple(issues);
        });
  }
  private static JsonContentCheck validateConsignmentItemsDescriptionOfGoods() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'descriptionOfGoods' field",
        mav -> mav.submitAllMatching("arrivalNotices.*.consignmentItems.*"),
        (ci, contextPath) -> {
          var node = ci.get("descriptionOfGoods");
          if (node == null || !node.isArray() || node.isEmpty()) {
            return ConformanceCheckResult.simple(Set.of(
                contextPath
                    + ".descriptionOfGoods must functionally be a non-empty array of strings"));
          }
          boolean allEmpty = true;
          for (var item : node) {
            if (item != null && item.isTextual() && !item.asText().trim().isEmpty()) {
              allEmpty = false;
              break;
            }
          }

          if (allEmpty) {
            return ConformanceCheckResult.simple(Set.of(
                contextPath
                    + ".descriptionOfGoods must contain at least one non-empty string value"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static JsonContentCheck validateCargoItemPresence() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'cargoItems' array",
        mav -> mav.submitAllMatching("arrivalNotices.*.consignmentItems.*"),
        (ci, contextPath) -> {
          var items = ci.get("cargoItems");
          if (items == null || !items.isArray() || items.isEmpty()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ".cargoItems must functionally be a non-empty array"));
          }
          return ConformanceCheckResult.simple(Set.of());
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
            return ConformanceCheckResult.simple(Set.of(contextPath + "." + field + " must be functionally present"));
          } else if (!field.equals("cargoGrossWeight") && item.get(field).asText().isBlank()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + "." + field + " must not be empty or blank in the payload"));
          }
          return ConformanceCheckResult.simple(Set.of());
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
            return ConformanceCheckResult.simple(Set.of(contextPath + "." + field + " must be functionally present"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static JsonContentCheck validateOuterPackagingStructure() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The publisher has demonstrated the correct use of the 'outerPackaging' object",
        mav -> mav.submitAllMatching("arrivalNotices.*.consignmentItems.*.cargoItems.*"),
        (item, contextPath) -> {
          var op = item.get("outerPackaging");
          if (op == null || !op.isObject() || op.isEmpty()) {
            return ConformanceCheckResult.simple(Set.of(contextPath + ".outerPackaging must functionally be a non-empty object"));
          }
          return ConformanceCheckResult.simple(Set.of());
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

          return ConformanceCheckResult.simple(issues);
        });
  }


}
