package org.dcsa.conformance.standards.tnt.v300.checks;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.tnt.v300.action.TntEventType;
import org.dcsa.conformance.standards.tnt.v300.party.TntRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.DOCUMENT_REFERENCE;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.EQUIPMENT_DETAILS;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.EQUIPMENT_EVENT_TYPE;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.EQUIPMENT_REFERENCE;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.EVENTS;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.EVENT_CLASSIFICATION;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.EVENT_CLASSIFIER;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.EVENT_DATE_TIME;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.EVENT_ID;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.EVENT_LOCATION;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.EVENT_TYPE;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.EVENT_UPDATED_DATE_TIME;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.IOT_EVENT_TYPE;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.ISO_EQUIPMENT_CODE;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.MODE_OF_TRANSPORT;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.RAIL_TRANSPORT;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.REEFER_EVENT_TYPE;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.REFERENCE;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.SHIPMENT_DETAILS;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.SHIPMENT_EVENT_TYPE;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.TRANSPORT_CALL;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.TRANSPORT_CALL_REFERENCE;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.TRANSPORT_DETAILS;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.TRANSPORT_EVENT_TYPE;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.TRUCK_TRANSPORT;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.TYPE;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventAttributes.VESSEL_TRANSPORT;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventValues.BARGE;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventValues.RAIL;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventValues.TRUCK;
import static org.dcsa.conformance.standards.tnt.v300.checks.TntEventValues.VESSEL;

@UtilityClass
public class TntChecks {

  private static final Map<TntEventType, Supplier<List<JsonContentCheck>>> EVENT_TYPE_CHECKS = Map.of(
    TntEventType.SHIPMENT, TntChecks::shipmentEventChecks,
    TntEventType.TRANSPORT, TntChecks::transportEventChecks,
    TntEventType.EQUIPMENT, TntChecks::equipmentEventChecks,
    TntEventType.IOT, TntChecks::iotEventChecks,
    TntEventType.REEFER, TntChecks::reeferEventChecks
  );

  public static ActionCheck getTntGetResponseChecks(
      UUID matched, String standardVersion, TntEventType eventType) {
    List<JsonContentCheck> checks = getJsonContentChecks(eventType);
    return JsonAttribute.contentChecks(
        TntRole::isProducer, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  public static ActionCheck getTntPostPayloadChecks(
      UUID matched, String standardVersion, TntEventType eventType) {
    List<JsonContentCheck> checks = getJsonContentChecks(eventType);
    return JsonAttribute.contentChecks(
        TntRole::isProducer, matched, HttpMessageType.REQUEST, standardVersion, checks);
  }

  private static List<JsonContentCheck> getJsonContentChecks(TntEventType eventType) {
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(atLeastOneEventInMessageCheck());
    checks.add(everyEventHasEventIdCheck());
    checks.add(everyEventHasEventDateTimeCheck());
    checks.add(everyEventHasEventUpdatedDateTimeCheck());
    checks.add(everyEventHasNonEmptyEventClassificationCheck());
    checks.add(everyEventHasValidEventClassifierCheck());

    if (eventType != null) {
      checks.add(atLeastOneEventHasEventTypeCheck(eventType));
      checks.addAll(EVENT_TYPE_CHECKS.get(eventType).get());
    } else {
      EVENT_TYPE_CHECKS.values().forEach(supplier -> checks.addAll(supplier.get()));
    }

    return checks;
  }

  private static JsonContentCheck atLeastOneEventInMessageCheck() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "At least one event must be included in the tested message.",
        mav -> mav.submitAllMatching(EVENTS),
        JsonAttribute.matchedMinLength(1));
  }

  private static JsonContentCheck everyEventHasEventIdCheck() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "Every event must demonstrate the correct use of the '%s' attribute: it must be present and not empty or blank."
            .formatted(EVENT_ID),
        mav -> mav.submitAllMatching(EVENTS + ".*"),
        JsonAttribute.path(EVENT_ID, JsonAttribute.matchedMustBeNonEmpty()));
  }

  private static JsonContentCheck everyEventHasEventDateTimeCheck() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "Every event must have a non-empty (not blank) '%s' attribute.".formatted(EVENT_DATE_TIME),
        mav -> mav.submitAllMatching(EVENTS + ".*"),
        JsonAttribute.path(EVENT_DATE_TIME, JsonAttribute.matchedMustBeNonEmpty()));
  }

  private static JsonContentCheck everyEventHasEventUpdatedDateTimeCheck() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "Every event must demonstrate the correct use of the '%s' attribute.".formatted(EVENT_UPDATED_DATE_TIME),
        mav -> mav.submitAllMatching(EVENTS + ".*"),
        JsonAttribute.path(EVENT_UPDATED_DATE_TIME, JsonAttribute.matchedMustBeNonEmpty()));
  }

  private static JsonContentCheck everyEventHasNonEmptyEventClassificationCheck() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "Every event must demonstrate the correct use of the '%s' object.".formatted(EVENT_CLASSIFICATION),
        mav -> mav.submitAllMatching(EVENTS + ".*"),
        JsonAttribute.path(EVENT_CLASSIFICATION, JsonAttribute.matchedMustBeNonEmpty()));
  }

  private static JsonContentCheck everyEventHasValidEventClassifierCheck() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The '%s.%s' attribute within every event must be one of ACTUAL, ESTIMATED, or PLANNED."
          .formatted(EVENT_CLASSIFICATION, EVENT_CLASSIFIER),
        mav -> mav.submitAllMatching(EVENTS + ".*"),
        JsonAttribute.path(
            EVENT_CLASSIFICATION,
            JsonAttribute.path(
                EVENT_CLASSIFIER,
                JsonAttribute.matchedMustBeOneOf(TntDataSets.VALID_EVENT_CLASSIFIERS))));
  }

  private static JsonContentCheck atLeastOneEventHasEventTypeCheck(TntEventType eventType) {
    return JsonAttribute.atLeastOneIndividualMatchMustBeValid(
        "For a scenario targeting %s, at least one event must have '%s.%s' equal to %s."
            .formatted(eventType, EVENT_CLASSIFICATION, EVENT_TYPE, eventType),
        mav -> mav.submitAllMatching(EVENTS + ".*." + EVENT_CLASSIFICATION + "." + EVENT_TYPE),
        JsonAttribute.matchedMustEqual(eventType::name));
  }

  private static Predicate<JsonNode> isEventOfType(TntEventType eventType) {
    return JsonAttribute.isEqualTo(EVENT_CLASSIFICATION + "." + EVENT_TYPE, eventType.name());
  }

  private static List<JsonContentCheck> commonEquipmentDetailsChecks(TntEventType eventType) {
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "Every applicable event must demonstrate the correct use of the '%s' object."
                .formatted(EQUIPMENT_DETAILS),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(eventType),
                JsonAttribute.path(EQUIPMENT_DETAILS, JsonAttribute.matchedMustBeNonEmpty()))));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "The '%s.%s' attribute within every applicable event must be present and not empty or blank."
                .formatted(EQUIPMENT_DETAILS, EQUIPMENT_REFERENCE),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(eventType),
                JsonAttribute.path(
                    EQUIPMENT_DETAILS,
                    JsonAttribute.path(
                        EQUIPMENT_REFERENCE, JsonAttribute.matchedMustBeNonEmpty())))));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "The '%s.%s' attribute within every applicable event must be present and not empty or blank."
                .formatted(EQUIPMENT_DETAILS, ISO_EQUIPMENT_CODE),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(eventType),
                JsonAttribute.path(
                    EQUIPMENT_DETAILS,
                    JsonAttribute.path(
                        ISO_EQUIPMENT_CODE, JsonAttribute.matchedMustBeNonEmpty())))));

    return checks;
  }

  private static List<JsonContentCheck> shipmentEventChecks() {
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "For every event, the event-type-specific subtype attribute must contain a value allowed by the corresponding OpenAPI schema property ('%s')."
                .formatted(SHIPMENT_EVENT_TYPE),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.SHIPMENT),
                JsonAttribute.path(
                    EVENT_CLASSIFICATION,
                    JsonAttribute.path(
                        SHIPMENT_EVENT_TYPE,
                        JsonAttribute.matchedMustBeOneOf(
                            TntDataSets.VALID_SHIPMENT_EVENT_TYPES))))));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "Every Shipment event must demonstrate the correct use of the '%s' object."
                .formatted(SHIPMENT_DETAILS),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.SHIPMENT),
                JsonAttribute.path(SHIPMENT_DETAILS, JsonAttribute.matchedMustBeNonEmpty()))));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "Every Shipment event must demonstrate the correct use of the '%s.%s' object."
                .formatted(SHIPMENT_DETAILS, DOCUMENT_REFERENCE),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.SHIPMENT),
                JsonAttribute.path(
                    SHIPMENT_DETAILS,
                    JsonAttribute.path(
                        DOCUMENT_REFERENCE, JsonAttribute.matchedMustBeNonEmpty())))));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "The '%s.%s.%s' attribute within every Shipment event must contain a value allowed by DocumentReference.type in the T&T 3.0.0 OpenAPI specification."
                .formatted(SHIPMENT_DETAILS, DOCUMENT_REFERENCE, TYPE),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.SHIPMENT),
                JsonAttribute.path(
                    SHIPMENT_DETAILS,
                    JsonAttribute.path(
                        DOCUMENT_REFERENCE,
                        JsonAttribute.path(
                            TYPE,
                            JsonAttribute.matchedMustBeOneOf(
                                TntDataSets.VALID_DOCUMENT_REFERENCE_TYPES)))))));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "The '%s.%s.%s' attribute within every Shipment event must be present and not empty or blank."
                .formatted(SHIPMENT_DETAILS, DOCUMENT_REFERENCE, REFERENCE),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.SHIPMENT),
                JsonAttribute.path(
                    SHIPMENT_DETAILS,
                    JsonAttribute.path(
                        DOCUMENT_REFERENCE,
                        JsonAttribute.path(REFERENCE, JsonAttribute.matchedMustBeNonEmpty()))))));

    return checks;
  }

  private static List<JsonContentCheck> transportEventChecks() {
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "For every event, the event-type-specific subtype attribute must contain a value allowed by the corresponding OpenAPI schema property ('%s')."
                .formatted(TRANSPORT_EVENT_TYPE),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.TRANSPORT),
                JsonAttribute.path(
                    EVENT_CLASSIFICATION,
                    JsonAttribute.path(
                        TRANSPORT_EVENT_TYPE,
                        JsonAttribute.matchedMustBeOneOf(
                            TntDataSets.VALID_TRANSPORT_EVENT_TYPES))))));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "Every Transport event must demonstrate the correct use of the '%s' object."
                .formatted(EVENT_LOCATION),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.TRANSPORT),
                JsonAttribute.path(EVENT_LOCATION, JsonAttribute.matchedMustBeNonEmpty()))));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "Every Transport event must demonstrate the correct use of the '%s' object."
                .formatted(TRANSPORT_DETAILS),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.TRANSPORT),
                JsonAttribute.path(TRANSPORT_DETAILS, JsonAttribute.matchedMustBeNonEmpty()))));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "Every Transport event must demonstrate the correct use of the '%s.%s' object."
                .formatted(TRANSPORT_DETAILS, TRANSPORT_CALL),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.TRANSPORT),
                JsonAttribute.path(
                    TRANSPORT_DETAILS,
                    JsonAttribute.path(TRANSPORT_CALL, JsonAttribute.matchedMustBeNonEmpty())))));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "The '%s.%s.%s' attribute within every Transport event must be present and not empty or blank."
                .formatted(TRANSPORT_DETAILS, TRANSPORT_CALL, TRANSPORT_CALL_REFERENCE),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.TRANSPORT),
                JsonAttribute.path(
                    TRANSPORT_DETAILS,
                    JsonAttribute.path(
                        TRANSPORT_CALL,
                        JsonAttribute.path(
                            TRANSPORT_CALL_REFERENCE, JsonAttribute.matchedMustBeNonEmpty()))))));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "When '%s.%s.%s' is VESSEL or BARGE / TRUCK / RAIL, every applicable Transport event must demonstrate the correct use of the corresponding transport sub-object."
                .formatted(TRANSPORT_DETAILS, TRANSPORT_CALL, MODE_OF_TRANSPORT),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.TRANSPORT),
                JsonAttribute.path(
                    TRANSPORT_DETAILS,
                    JsonAttribute.path(
                        TRANSPORT_CALL,
                        (transportCall, contextPath) -> {
                          String modeOfTransport =
                              transportCall.path(MODE_OF_TRANSPORT).asText(null);
                          boolean hasVessel =
                              !JsonUtil.isMissing(transportCall.path(VESSEL_TRANSPORT));
                          boolean hasRail = !JsonUtil.isMissing(transportCall.path(RAIL_TRANSPORT));
                          boolean hasTruck =
                              !JsonUtil.isMissing(transportCall.path(TRUCK_TRANSPORT));

                          if (modeOfTransport == null || modeOfTransport.isEmpty()) {
                            return ConformanceCheckResult.simple(
                                Set.of(
                                    "The '%s' object must contain a '%s' attribute"
                                        .formatted(contextPath, MODE_OF_TRANSPORT)));
                          }

                          switch (modeOfTransport) {
                            case VESSEL, BARGE:
                              if (!hasVessel) {
                                return ConformanceCheckResult.simple(
                                    Set.of(
                                        "When '%s.%s.%s' is VESSEL or BARGE, every applicable Transport event must demonstrate the correct use of '%s.%s.%s'."
                                            .formatted(
                                                TRANSPORT_DETAILS, TRANSPORT_CALL, MODE_OF_TRANSPORT,
                                                TRANSPORT_DETAILS, TRANSPORT_CALL, VESSEL_TRANSPORT)));
                              }
                              break;
                            case RAIL:
                              if (!hasRail) {
                                return ConformanceCheckResult.simple(
                                    Set.of(
                                        "When '%s.%s.%s' is RAIL, every applicable Transport event must demonstrate the correct use of '%s.%s.%s'."
                                            .formatted(
                                                TRANSPORT_DETAILS, TRANSPORT_CALL, MODE_OF_TRANSPORT,
                                                TRANSPORT_DETAILS, TRANSPORT_CALL, RAIL_TRANSPORT)));
                              }
                              break;
                            case TRUCK:
                              if (!hasTruck) {
                                return ConformanceCheckResult.simple(
                                    Set.of(
                                        "When '%s.%s.%s' is TRUCK, every applicable Transport event must demonstrate the correct use of '%s.%s.%s'."
                                            .formatted(
                                                TRANSPORT_DETAILS, TRANSPORT_CALL, MODE_OF_TRANSPORT,
                                                TRANSPORT_DETAILS, TRANSPORT_CALL, TRUCK_TRANSPORT)));
                              }
                              break;
                            default:
                              break;
                          }
                          return ConformanceCheckResult.simple(Collections.emptySet());
                        })))));


    return checks;
  }

  private static List<JsonContentCheck> equipmentEventChecks() {
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "For every event, the event-type-specific subtype attribute must contain a value allowed by the corresponding OpenAPI schema property ('%s')."
                .formatted(EQUIPMENT_EVENT_TYPE),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.EQUIPMENT),
                JsonAttribute.path(
                    EVENT_CLASSIFICATION,
                    JsonAttribute.path(
                        EQUIPMENT_EVENT_TYPE,
                        JsonAttribute.matchedMustBeOneOf(
                            TntDataSets.VALID_EQUIPMENT_EVENT_TYPES))))));

    checks.addAll(commonEquipmentDetailsChecks(TntEventType.EQUIPMENT));

    return checks;
  }

  private static List<JsonContentCheck> iotEventChecks() {
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "For every event, the event-type-specific subtype attribute must contain a value allowed by the corresponding OpenAPI schema property ('%s')."
                .formatted(IOT_EVENT_TYPE),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.IOT),
                JsonAttribute.path(
                    EVENT_CLASSIFICATION,
                    JsonAttribute.path(
                        IOT_EVENT_TYPE,
                        JsonAttribute.matchedMustBeOneOf(
                            TntDataSets.VALID_IOT_EVENT_TYPES))))));

    checks.addAll(commonEquipmentDetailsChecks(TntEventType.IOT));

    return checks;
  }

  private static List<JsonContentCheck> reeferEventChecks() {
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "For every event, the event-type-specific subtype attribute must contain a value allowed by the corresponding OpenAPI schema property ('%s')."
                .formatted(REEFER_EVENT_TYPE),
            mav -> mav.submitAllMatching(EVENTS + ".*"),
            JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.REEFER),
                JsonAttribute.path(
                    EVENT_CLASSIFICATION,
                    JsonAttribute.path(
                        REEFER_EVENT_TYPE,
                        JsonAttribute.matchedMustBeOneOf(
                            TntDataSets.VALID_REEFER_EVENT_TYPES))))));

    checks.addAll(commonEquipmentDetailsChecks(TntEventType.REEFER));

    return checks;
  }
}
