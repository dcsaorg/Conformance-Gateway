package org.dcsa.conformance.standards.an.checks;

import com.fasterxml.jackson.databind.JsonNode;
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

  private static final JsonContentCheck CHECK_PRESENCE_OF_REQUIRED_FIELDS =
      JsonAttribute.customValidator(
          "The Publisher has correctly demonstrated the use of functionally required attributes in the payload",
          body -> {
            var issues = new LinkedHashSet<String>();
            validateBasicFields(issues);
            validateCarrierContactInformation(issues);
            validateDocumentParties(issues);
            validateTransport(issues);
            return issues;
          });

  private static void validateCarrierContactInformation(Set<String> issues) {
    JsonAttribute.allIndividualMatchesMustBeValid(
        "Carrier contact info must include name and either email or phone",
        mav -> mav.path("arrivalNotices").all().path("carrierContactInformation").submitPath(),
        (node, contextPath) -> {
          if (!node.hasNonNull("name")) {
            issues.add(contextPath + ".name must be present in carrierContactInformation");
          }
          if (!node.hasNonNull("email") && !node.hasNonNull("phone")) {
            issues.add(contextPath + " must include either 'email' or 'phone'");
          }
          return issues;
        });
  }

  private static void validateDocumentParties(Set<String> issues) {
    JsonAttribute.allIndividualMatchesMustBeValid(
        "Validate presence and fields in 'documentParties'",
        mav -> mav.path("arrivalNotices").all().path("documentParties").all(),
        (node, contextPath) -> {
          if (!node.hasNonNull("partyFunction")) {
            issues.add(contextPath + ".partyFunction must be present");
          }
          if (!node.hasNonNull("partyName")) {
            issues.add(contextPath + ".partyName must be present");
          }
          if (!node.hasNonNull("partyContactDetails")) {
            issues.add(contextPath + ".partyContactDetails must be present");
          }
          var address = node.get("address");
          if (address == null || !address.isObject() || address.isEmpty()) {
            issues.add(contextPath + ".address must be present and contain at least one field");
          }
          return issues;
        });
  }

  private static void validateTransport(Set<String> issues) {
    JsonAttribute.allIndividualMatchesMustBeValid(
        "Validate transport fields and nested vesselVoyages",
        mav -> mav.path("arrivalNotices").all().path("transport").submitPath(),
        (transport, contextPath) -> {
          if (!transport.hasNonNull("etaAtPortOfDischargeDate")
              && !transport.hasNonNull("etaAtPlaceOfDeliveryDate")) {
            issues.add(
                contextPath
                    + ": must have either 'etaAtPortOfDischargeDate' or 'etaAtPlaceOfDeliveryDate'");
          }
          if (!transport.hasNonNull("portOfDischarge")) {
            issues.add(contextPath + ".portOfDischarge must be present");
          }
          if (!transport.hasNonNull("address")
              && !transport.hasNonNull("UNLocationCode")
              && !transport.hasNonNull("facility")) {
            issues.add(
                contextPath
                    + ": One of 'address', 'UNLocationCode', or 'facility' must be present");
          }

          var voyages = transport.get("vesselVoyages");
          if (voyages == null || !voyages.isArray() || voyages.isEmpty()) {
            issues.add(contextPath + ".vesselVoyages must be a non-empty array");
          } else {
            for (int i = 0; i < voyages.size(); i++) {
              var voyage = voyages.get(i);
              if (!voyage.hasNonNull("typeCode")) {
                issues.add(contextPath + ".vesselVoyages[" + i + "].typeCode must be present");
              }
              if (!voyage.hasNonNull("vesselName")) {
                issues.add(contextPath + ".vesselVoyages[" + i + "].vesselName must be present");
              }
              if (!voyage.hasNonNull("carrierVoyageNumber")) {
                issues.add(
                    contextPath + ".vesselVoyages[" + i + "].carrierVoyageNumber must be present");
              }
            }
          }

          return issues;
        });
  }

  private static void validateBasicFields(Set<String> issues) {
    JsonAttribute.allIndividualMatchesMustBeValid(
        "Basic required fields for ArrivalNotice",
        mav -> mav.path("arrivalNotices").all(),
        (node, contextPath) -> {
          if (!node.hasNonNull("carrierCode")) {
            issues.add(contextPath + ".carrierCode must be present in the payload");
          }
          if (!node.hasNonNull("carrierCodeListProvider")) {
            issues.add(contextPath + ".carrierCodeListProvider must be present in the payload");
          }
          if (!node.hasNonNull("deliveryTypeAtDestination")) {
            issues.add(contextPath + ".deliveryTypeAtDestination must be present in the payload");
          }
          return issues;
        });
  }

  static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE =
      JsonAttribute.customValidator(
          "Every response received during a conformance test must not be empty",
          body -> body.isEmpty() ? Set.of("The response body must not be empty") : Set.of());

  private static List<JsonContentCheck> getScenarioRelatedChecks(String scenarioType) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(
        JsonAttribute.customValidator(
            "[Scenario] Validate required fields based on scenario type",
            body -> {
              var issues = new LinkedHashSet<String>();
              var arrivalNotices = body.get("arrivalNotices");

              for (int i = 0; i < arrivalNotices.size(); i++) {
                var arrivalNotice = arrivalNotices.get(i);

                switch (scenarioType) {
                  case "FREIGHTED":
                    checkFieldNotEmpty(
                        arrivalNotice,
                        "charges",
                        i,
                        "'charges' must be present and not empty for 'Arrival Notice [Freighted]' scenario",
                        issues);
                    break;

                  case "FREE_TIME":
                    checkFieldNotEmpty(
                        arrivalNotice,
                        "freeTimes",
                        i,
                        "'freeTimes' must be present and not empty for 'Arrival Notice [Free Time]' scenario",
                        issues);
                    break;
                }
              }

              return issues;
            }));

    return checks;
  }

  private static void checkFieldNotEmpty(
      JsonNode node, String fieldName, int index, String message, Set<String> issues) {
    var field = node.get(fieldName);
    if (field == null || field.isEmpty()) {
      issues.add(String.format("ArrivalNotice %d: %s", index, message));
    }
  }

  public static ActionCheck getANGetResponseChecks(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    checks.add(CHECK_PRESENCE_OF_REQUIRED_FIELDS);
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

  public static final List<JsonContentCheck> COMMON_PAYLOAD_CHECKS =
      Arrays.asList(VALIDATE_NON_EMPTY_RESPONSE, CHECK_PRESENCE_OF_REQUIRED_FIELDS);
}
