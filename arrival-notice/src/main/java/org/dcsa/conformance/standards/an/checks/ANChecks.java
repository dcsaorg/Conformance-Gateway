package org.dcsa.conformance.standards.an.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonRebasableContentCheck;
import org.dcsa.conformance.core.check.KeywordDataset;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.an.party.ANRole;
import org.dcsa.conformance.standards.an.party.DynamicScenarioParameters;

public class ANChecks {

  public static ActionCheck getANPostPayloadChecks(
      UUID matchedExchangeUuid, String expectedApiVersion, String scenarioType) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    var payloadChecks = payloadChecks(scenarioType);
    checks.addAll(guardEachWithBodyPresent(payloadChecks, "arrivalNotices"));
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

  public static ActionCheck getANGetResponseChecks(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    checks.addAll(guardEachWithBodyPresent(getResponseChecks(dspSupplier), "arrivalNotices"));
    return JsonAttribute.contentChecks(
        ANRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }

  private static List<JsonContentCheck> getResponseChecks(
      Supplier<DynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.addAll(validateTransportDocumentReferences(dspSupplier));
    checks.addAll(payloadChecks(dspSupplier.get().scenarioType()));
    return checks;
  }

  public static ActionCheck getANNPostPayloadChecks(
      UUID matchedExchangeUuid, String expectedApiVersion) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE_NOTIFICATION);
    var notificationPayloadChecks = notificationPayloadChecks();
    checks.addAll(
        guardEachWithBodyPresent(notificationPayloadChecks, "arrivalNoticeNotifications"));
    return JsonAttribute.contentChecks(
        ANRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        checks);
  }

  private static List<JsonContentCheck> notificationPayloadChecks() {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(
        validateBasicFieldWithLabel("transportDocumentReference", "arrivalNoticeNotifications.*"));
    checks.add(validateANNEquipmentReference());
    checks.add(validateTransportETA("arrivalNoticeNotifications.*"));
    checks.add(validatePortOfDischarge("arrivalNoticeNotifications.*"));
    return checks;
  }

  public static List<JsonContentCheck> validateBasicFields() {
    return List.of(
        validateBasicFieldWithLabel("carrierCode", "arrivalNotices.*"),
        validateBasicFieldWithLabel("transportDocumentReference", "arrivalNotices.*"));
  }

  private static JsonContentCheck validateBasicFieldWithLabel(String field, String path) {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the '" + field + "' attribute",
        mav -> mav.submitAllMatching(path),
        (node, contextPath) -> {
          var fieldNode = node.get(field);

          if (JsonUtil.isMissingOrEmpty(fieldNode)) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + "." + field + " must be functionally present in the payload"));
          }

          if (fieldNode.isArray()) {
            if (fieldNode.isEmpty()) {
              return ConformanceCheckResult.simple(
                  Set.of(contextPath + "." + field + " must not be an empty array in the payload"));
            }
            return ConformanceCheckResult.simple(Set.of());
          }

          if (fieldNode.isTextual() && fieldNode.asText().isBlank()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + "." + field + " must not be empty or blank in the payload"));
          }

          if (fieldNode.isObject() && JsonUtil.isMissingOrEmpty(fieldNode)) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + "." + field + " must not be empty in the payload"));
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
            "arrivalNotices.*"));
  }

  public static JsonContentCheck validateDataSetFields(
      String attribute, KeywordDataset dataset, String path) {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the '" + attribute + "' attribute",
        mav -> mav.submitAllMatching(path),
        (node, contextPath) -> {
          final var fieldNode = node.path(attribute);
          final String fieldPath = contextPath + "." + attribute;

          if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return ConformanceCheckResult.simple(
                Set.of(fieldPath + " must be functionally present in the payload"));
          }
          if (fieldNode.asText().isBlank()) {
            return ConformanceCheckResult.simple(
                Set.of(fieldPath + " must not be empty or blank in the payload"));
          }
          var validator = JsonAttribute.matchedMustBeDatasetKeywordIfPresent(dataset);

          return validator.validate(fieldNode, fieldPath);
        });
  }

  public static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE =
      JsonAttribute.customValidator(
          "Every response received during a conformance test must not be empty",
          body ->
              ConformanceCheckResult.simple(
                  (body.path("arrivalNotices").isEmpty())
                      ? Set.of("The response body must not be empty")
                      : Set.of()));

  public static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE_NOTIFICATION =
      JsonAttribute.customValidator(
          "Every response received during a conformance test must not be empty",
          body ->
              ConformanceCheckResult.simple(
                  (body.path("arrivalNoticeNotifications").isEmpty())
                      ? Set.of("The response body must not be empty")
                      : Set.of()));

  public static List<JsonContentCheck> getScenarioRelatedChecks(String scenarioType) {
    var checks = new ArrayList<JsonContentCheck>();

    if (scenarioType.equals(ScenarioType.FREE_TIME.name())) {
      checks.add(validateFreeTimeObjectStructure());
    }

    if (scenarioType.equals(ScenarioType.FREIGHTED.name())) {
      checks.add(validateChargesStructure());
    }
    return checks;
  }



  private static JsonContentCheck validateANNEquipmentReference() {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the 'equipmentReferences' field",
        mav -> mav.submitAllMatching("arrivalNoticeNotifications.*"),
        (notifications, contextPath) -> {
          var node = notifications.get("equipmentReferences");
          if (JsonUtil.isMissingOrEmpty(node)) {
            return ConformanceCheckResult.simple(
                Set.of(
                    contextPath
                        + ".equipmentReferences must functionally be present and a non-empty array of strings"));
          }
          boolean allEmpty = true;
          for (var item : node) {
            if (item != null && item.isTextual() && !item.asText().trim().isEmpty()) {
              allEmpty = false;
              break;
            }
          }

          if (allEmpty) {
            return ConformanceCheckResult.simple(
                Set.of(
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
            "Validate that all 'transportDocumentReference's in the response match the query parameters, and none are missing",
            body -> {
              var issues = new LinkedHashSet<String>();
              var arrivalNotices = body.get("arrivalNotices");

              if (arrivalNotices == null || !arrivalNotices.isArray()) {
                issues.add("Missing or invalid 'arrivalNotices' array in payload.");
                return ConformanceCheckResult.simple(issues);
              }

              if (dsp.get().transportDocumentReferences() == null
                  || dsp.get().transportDocumentReferences().isEmpty()) {
                issues.add("No 'transportDocumentReferences' were specified in the payload");
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
          var basicResult =
              validateBasicFieldWithLabel("carrierContactInformation", "arrivalNotices.*")
                  .validate(body);
          if (!basicResult.getErrorMessages().isEmpty()) {
            return basicResult;
          }
          var issues = new LinkedHashSet<String>();
          issues.addAll(validateCarrierContactName().validate(body).getErrorMessages());
          issues.addAll(validateCarrierContactEmailOrPhone().validate(body).getErrorMessages());
          return ConformanceCheckResult.simple(issues);
        });
  }

  private static JsonContentCheck validateCarrierContactName() {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the 'name' attribute in 'carrierContactInformation'",
        mav -> mav.submitAllMatching("arrivalNotices.*.carrierContactInformation.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull("name")) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ".name must be functionally present"));
          } else if (node.get("name").asText().isBlank()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ".name must not be empty or blank in the payload"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }


  private static JsonContentCheck validateCarrierContactEmailOrPhone() {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of either the 'email' or 'phone' attribute in 'carrierContactInformation'",
        mav -> mav.submitAllMatching("arrivalNotices.*.carrierContactInformation.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull("email") && !node.hasNonNull("phone")) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + " must functionally include either 'email' or 'phone'"));
          }
          if (node.hasNonNull("email") && node.get("email").asText().isBlank()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ".'email' must not be empty or blank in the payload"));
          }
          if (node.hasNonNull("phone") && node.get("phone").asText().isBlank()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ".'phone' must not be empty or blank in the payload"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck validateDocumentParties() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'documentParties' object",
        body -> {
          var basicResult =
              validateBasicFieldWithLabel("documentParties", "arrivalNotices.*").validate(body);
          if (!basicResult.getErrorMessages().isEmpty()) {
            return basicResult;
          }
          var results = new LinkedHashSet<ConformanceCheckResult>();
          results.add(validateDocumentPartyField("partyFunction").validate(body));
          results.add(validateDocumentPartyField("partyName").validate(body));
          results.add(validateDocumentPartyField("partyContactDetails").validate(body));
          results.add(validatePartyContactName().validate(body));
          results.add(validatePartyContactEmailOrPhone().validate(body));
          results.add(validateDocumentPartyAddress().validate(body));
          return ConformanceCheckResult.from(results);
        });
  }

  public static JsonContentCheck validatePartyContactName() {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the '"
            + "name"
            + "' attribute in 'carrierContactInformation'",
        mav -> mav.submitAllMatching("arrivalNotices.*.documentParties.*.partyContactDetails.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull("name")) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ". 'name' must be functionally present"));
          } else if (node.get("name").asText().isBlank()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ". 'name' must not be empty or blank in the payload"));
          }

          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck validatePartyContactEmailOrPhone() {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of either the 'email' or 'phone' attribute in 'partyContactDetails'",
        mav -> mav.submitAllMatching("arrivalNotices.*.documentParties.*.partyContactDetails.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull("email") && !node.hasNonNull("phone")) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + " must functionally include either 'email' or 'phone'"));
          }
          if (node.hasNonNull("email") && node.get("email").asText().isBlank()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ". 'email' must not be empty or blank in the payload"));
          }
          if (node.hasNonNull("phone") && node.get("phone").asText().isBlank()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ". 'phone' must not be empty or blank in the payload"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck validateDocumentPartyField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the '"
            + field
            + "' attribute in 'documentParties'",
        mav -> mav.submitAllMatching("arrivalNotices.*.documentParties.*"),
        (node, contextPath) -> {
          var value = node.get(field);
          var fieldPath = contextPath + "." + field;

          if (value == null || value.isNull()) {
            return ConformanceCheckResult.simple(
                Set.of(fieldPath + " must be functionally present"));
          }
          if (JsonUtil.isMissingOrEmpty(value)) {
            return ConformanceCheckResult.simple(
                Set.of(fieldPath + " must not be empty in the payload"));
          }

          if (field.equals("partyFunction")) {
              var validator =
                  JsonAttribute.matchedMustBeDatasetKeywordIfPresent(ANDatasets.PARTY_FUNCTION);
              return validator.validate(value, fieldPath);

          }

          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck validateAddress(String field, String path) {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
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
              return ConformanceCheckResult.simple(
                  Set.of(
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
          var basicResult =
              validateBasicFieldWithLabel("transport", "arrivalNotices.*").validate(body);
          if (!basicResult.getErrorMessages().isEmpty()) {
            return basicResult;
          }
          var checkResults = new LinkedHashSet<ConformanceCheckResult>();
          checkResults.add(validateTransportETA("arrivalNotices.*.transport").validate(body));
          checkResults.add(validatePortOfDischarge("arrivalNotices.*.transport").validate(body));
          checkResults.add(validatePODAdrressAN().validate(body));
          checkResults.add(validateLegs().validate(body));
          checkResults.add(validateVesselVoyage().validate(body));
          checkResults.add(validateVesselVoyageField("vesselName").validate(body));
          checkResults.add(validateVesselVoyageField("carrierImportVoyageNumber").validate(body));
          return ConformanceCheckResult.from(checkResults);
        });
  }

  private static JsonContentCheck validateLegs() {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of transport 'Legs' array",
        mav -> mav.submitAllMatching("arrivalNotices.*.transport"),
        (node, contextPath) -> {
          node.get("legs");
          if (JsonUtil.isMissingOrEmpty(node.get("legs"))) {
            return ConformanceCheckResult.simple(
                Set.of(
                    contextPath + ".legs must be functionally present and be a non-empty array"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static JsonContentCheck validateTransportETA(String path) {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
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
            return ConformanceCheckResult.simple(
                Set.of(
                    contextPath
                        + ": must functionally include either 'portOfDischargeArrivalDate.value' "
                        + "or 'placeOfDeliveryArrivalDate.value'"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck validatePortOfDischarge(String path) {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated correct use of 'portOfDischarge' and its location fields",
        mav -> mav.submitAllMatching(path),
        (node, contextPath) -> {
          var pod = node.get("portOfDischarge");

          if (JsonUtil.isMissingOrEmpty(pod)) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ".portOfDischarge must be functionally present"));
          }

          Set<String> messages = new HashSet<>();

          boolean hasAddress = pod.hasNonNull("address");
          boolean hasUNLocationCode = pod.hasNonNull("UNLocationCode");
          boolean hasFacility = pod.hasNonNull("facility");

          if (!hasAddress && !hasUNLocationCode && !hasFacility) {
            messages.add(
                contextPath
                    + ".portOfDischarge must contain at least one of 'address', 'UNLocationCode', or 'facility'");
          }

          if (hasUNLocationCode) {
            var unLoc = pod.get("UNLocationCode");
            if (unLoc.asText().isBlank()) {
              messages.add(
                  contextPath + ".portOfDischarge.UNLocationCode must not be empty or blank");
            }
          }

          if (hasAddress) {
            var address = pod.get("address");

            if (address.isObject() && address.isEmpty()) {
              messages.add(
                  contextPath
                      + ".portOfDischarge.address if present must contain at least one field");
            } else {
              boolean hasNonEmptyField =
                  ADDRESS_FIELDS.stream()
                      .anyMatch(
                          f -> {
                            var v = address.get(f);
                            return v != null && v.isTextual() && !v.asText().isBlank();
                          });

              if (!hasNonEmptyField) {
                messages.add(
                    contextPath
                        + ".portOfDischarge.address must contain at least one non-empty field among: "
                        + String.join(", ", ADDRESS_FIELDS));
              }
            }
          }

          if (hasFacility) {
            var facility = pod.get("facility");

            if (facility.isObject()) {

              boolean hasCode = facility.hasNonNull("facilityCode");
              boolean hasListProvider = facility.hasNonNull("facilityCodeListProvider");

              if (!hasCode) {
                messages.add(contextPath + ".portOfDischarge.facility must contain 'facilityCode'");
              } else if (facility.get("facilityCode").asText().isBlank()) {
                messages.add(
                    contextPath
                        + ".portOfDischarge.facility.facilityCode must not be empty or blank");
              }

              if (!hasListProvider) {
                messages.add(
                    contextPath
                        + ".portOfDischarge.facility must contain 'facilityCodeListProvider'");
              } else {
                var providerNode = facility.get("facilityCodeListProvider");
                var providerValue = providerNode.asText();

                if (providerValue.isBlank()) {
                  messages.add(
                      contextPath
                          + ".portOfDischarge.facility.facilityCodeListProvider must not be empty or blank");
                } else {
                  var datasetCheck =
                      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                              ANDatasets.FACILITY_CODE_LIST_PROVIDER)
                          .validate(
                              providerNode,
                              contextPath + ".portOfDischarge.facility.facilityCodeListProvider");

                  messages.addAll(datasetCheck.getErrorMessages());
                }
              }
            }
          }

          return ConformanceCheckResult.simple(messages);
        });
  }


  public static JsonContentCheck validateDocumentPartyAddress() {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of location information in 'portOfDischarge'",
        mav -> mav.submitAllMatching("arrivalNotices.*.documentParties.*"),
        (node, contextPath) -> {
          if (node.get("address") == null) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ".address must be functionally present"));
          }
          if (node.hasNonNull("address")
              && node.get("address").isObject()
              && node.get("address").isEmpty()) {
            return ConformanceCheckResult.simple(
                Set.of(
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
              return ConformanceCheckResult.simple(
                  Set.of(
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
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the 'vesselVoyages' array",
        mav -> mav.submitAllMatching("arrivalNotices.*.transport.legs.*"),
        (node, contextPath) -> {
          var voyage = node.get("vesselVoyage");
          if (voyage == null || voyage.isEmpty()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ".vesselVoyage must be functionally present and not empty"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static JsonContentCheck validateVesselVoyageField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of '" + field + "' in vesselVoyages",
        mav -> mav.submitAllMatching("arrivalNotices.*.transport.legs.*.vesselVoyage"),
        (node, contextPath) -> {
          if (!node.hasNonNull(field)) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + "." + field + " must be functionally present"));
          } else if (node.get(field).asText().isBlank()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + "." + field + " must not be empty or blank in the payload"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static JsonContentCheck validateFreeTimeAttribute(String attributeName) {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the 'freeTime' '"
            + attributeName
            + "' attribute",
        mav -> mav.submitAllMatching("arrivalNotices.*.freeTimes.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull(attributeName)) {
            return ConformanceCheckResult.simple(
                Set.of(
                    String.format(
                        "%s.%s must be functionally present", contextPath, attributeName)));
          } else if (node.get(attributeName).asText().isBlank()) {
            return ConformanceCheckResult.simple(
                Set.of(
                    String.format(
                        "%s.%s must not be empty or blank in the payload",
                        contextPath, attributeName)));
          }
          if (attributeName.equals("timeUnit")) {
            var validator =
                JsonAttribute.matchedMustBeDatasetKeywordIfPresent(ANDatasets.FREE_TIME_TIME_UNIT);
            return validator.validate(node.get(attributeName), contextPath + "." + attributeName);
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
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
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
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the 'charges' '"
            + attributeName
            + "' attribute",
        mav -> mav.submitAllMatching("arrivalNotices.*.charges.*"),
        (node, contextPath) -> {
          if (!node.hasNonNull(attributeName)) {
            return ConformanceCheckResult.simple(
                Set.of(
                    String.format(
                        "%s.%s must be functionally present", contextPath, attributeName)));
          } else if (node.get(attributeName).asText().isBlank()) {
            return ConformanceCheckResult.simple(
                Set.of(
                    String.format(
                        "%s.%s must not be empty or blank in the payload",
                        contextPath, attributeName)));
          }
          if (attributeName.equals("paymentTermCode")) {
            var validator =
                JsonAttribute.matchedMustBeDatasetKeywordIfPresent(ANDatasets.PAYMENT_TERM_CODE);
            return validator.validate(node.get(attributeName), contextPath + "." + attributeName);
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
          results.add(validateChargeAttribute("paymentTermCode").validate(body));
          return ConformanceCheckResult.from(results);
        });
  }

  public static JsonContentCheck validateUtilizedTransportEquipments() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'utilizedTransportEquipments' object",
        body -> {
          var basicResult =
              validateBasicFieldWithLabel("utilizedTransportEquipments", "arrivalNotices.*")
                  .validate(body);
          if (!basicResult.getErrorMessages().isEmpty()) {
            return basicResult;
          }
          var issues = new LinkedHashSet<String>();
          issues.addAll(validateUTEEquipmentPresence().validate(body).getErrorMessages());
          issues.addAll(
              validateUTEEquipmentField("equipmentReference").validate(body).getErrorMessages());
          issues.addAll(
              validateUTEEquipmentField("ISOEquipmentCode").validate(body).getErrorMessages());
          issues.addAll(validateUTESealsPresence().validate(body).getErrorMessages());
          issues.addAll(validateUTESealNumber().validate(body).getErrorMessages());

          return ConformanceCheckResult.simple(issues);
        });
  }

  private static JsonContentCheck validateUTEEquipmentField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the 'utilizedTransportEquipments.equipment."
            + field
            + "' attribute",
        mav -> mav.submitAllMatching("arrivalNotices.*.utilizedTransportEquipments.*.equipment"),
        (node, contextPath) -> {
          if (!node.hasNonNull(field)) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + "." + field + " must be functionally present"));
          } else if (node.get(field).asText().isBlank()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + "." + field + " must not be empty or blank in the payload"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static JsonContentCheck validateUTEEquipmentPresence() {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the 'utilizedTransportEquipments.equipment' object",
        mav -> mav.submitAllMatching("arrivalNotices.*.utilizedTransportEquipments.*"),
        (ute, contextPath) -> {
          if (ute.get("equipment") == null || ute.get("equipment").isNull()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ".equipment must be functionally present"));
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
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the 'utilizedTransportEquipments.seals.number' attribute",
        mav -> mav.submitAllMatching("arrivalNotices.*.utilizedTransportEquipments.*.seals.*"),
        (seal, contextPath) -> {
          if (!seal.hasNonNull("number")) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ".number must be functionally present"));
          } else if (seal.get("number").asText().isBlank())
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ".number must not be empty or blank in the payload"));
          {
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck validateConsignmentItems() {
    return JsonAttribute.customValidator(
        "The publisher has demonstrated the correct use of the 'consignmentItem' object",
        body -> {
          var basicResult =
              validateBasicFieldWithLabel("consignmentItems", "arrivalNotices.*").validate(body);
          if (!basicResult.getErrorMessages().isEmpty()) {
            return basicResult;
          }
          var issues = new LinkedHashSet<String>();
          issues.addAll(
              validateConsignmentItemsDescriptionOfGoods().validate(body).getErrorMessages());
          issues.addAll(validateCargoItemPresence().validate(body).getErrorMessages());
          issues.addAll(
              validateCargoItemField("equipmentReference").validate(body).getErrorMessages());
          issues.addAll(
              validateCargoItemField("cargoGrossWeight").validate(body).getErrorMessages());
          issues.addAll(validateCargoGrossWeightField("value").validate(body).getErrorMessages());
          issues.addAll(validateCargoGrossWeightField("unit").validate(body).getErrorMessages());
          issues.addAll(validateOuterPackagingStructure().validate(body).getErrorMessages());
          issues.addAll(validateOuterPackagingFields().validate(body).getErrorMessages());

          return ConformanceCheckResult.simple(issues);
        });
  }
  private static JsonContentCheck validateConsignmentItemsDescriptionOfGoods() {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the 'descriptionOfGoods' field",
        mav -> mav.submitAllMatching("arrivalNotices.*.consignmentItems.*"),
        (ci, contextPath) -> {
          var node = ci.get("descriptionOfGoods");
          if (node == null || !node.isArray() || node.isEmpty()) {
            return ConformanceCheckResult.simple(
                Set.of(
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
            return ConformanceCheckResult.simple(
                Set.of(
                    contextPath
                        + ".descriptionOfGoods must contain at least one non-empty string value"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static JsonContentCheck validateCargoItemPresence() {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the 'cargoItems' array",
        mav -> mav.submitAllMatching("arrivalNotices.*.consignmentItems.*"),
        (ci, contextPath) -> {
          var items = ci.get("cargoItems");
          if (items == null || !items.isArray() || items.isEmpty()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ".cargoItems must functionally be a non-empty array"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static JsonContentCheck validateCargoItemField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the 'cargoItems."
            + field
            + "' attribute",
        mav -> mav.submitAllMatching("arrivalNotices.*.consignmentItems.*.cargoItems.*"),
        (item, contextPath) -> {
          if (!item.hasNonNull(field)) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + "." + field + " must be functionally present"));
          } else if (!field.equals("cargoGrossWeight") && item.get(field).asText().isBlank()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + "." + field + " must not be empty or blank in the payload"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static JsonContentCheck validateCargoGrossWeightField(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the 'cargoGrossWeight."
            + field
            + "' attribute",
        mav ->
            mav.submitAllMatching(
                "arrivalNotices.*.consignmentItems.*.cargoItems.*.cargoGrossWeight"),
        (item, contextPath) -> {
          if (!item.hasNonNull(field)) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + "." + field + " must be functionally present"));
          }
          if (field.equals("unit")) {
            if (item.get(field).asText().isBlank()) {
              return ConformanceCheckResult.simple(
                  Set.of(contextPath + "." + field + " must not be empty or blank in the payload"));
            } else {
              final var fieldNode = item.path(field);
              final String fieldPath = contextPath + "." + field;
              var validator =
                  JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                      ANDatasets.CARGO_GROSS_WEIGHT_UNIT);

              return validator.validate(fieldNode, fieldPath);
            }
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static JsonContentCheck validateOuterPackagingStructure() {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
        "The publisher has demonstrated the correct use of the 'outerPackaging' object",
        mav -> mav.submitAllMatching("arrivalNotices.*.consignmentItems.*.cargoItems.*"),
        (item, contextPath) -> {
          var op = item.get("outerPackaging");
          if (op == null || !op.isObject() || op.isEmpty()) {
            return ConformanceCheckResult.simple(
                Set.of(contextPath + ".outerPackaging must functionally be a non-empty object"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static JsonContentCheck validateOuterPackagingFields() {
    return JsonAttribute.allIndividualMatchesMustBeValidWithoutRelevance(
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

  public static List<JsonRebasableContentCheck> guardEachWithBodyPresent(
      List<JsonContentCheck> checks, String payload) {

    Predicate<JsonNode> bodyPresent = body -> !JsonUtil.isMissingOrEmpty(body.path(payload));

    return checks.stream()
        .map(
            ch -> {
              JsonRebasableContentCheck rebasable =
                  JsonAttribute.customValidator(ch.description(), (node, ctx) -> ch.validate(node));

              return JsonAttribute.ifThen(ch.description(), bodyPresent, rebasable);
            })
        .toList();
  }
}
