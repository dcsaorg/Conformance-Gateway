package org.dcsa.conformance.standards.tnt.v300.checks;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonContentMatchedValidation;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.v300.action.TntEventType;
import org.dcsa.conformance.standards.tnt.v300.party.TntRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

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
    checks.add(everyEventHasValidEventClassifierCheck());
    checks.add(everyEventHasValidEventSubtypeCheck());

    if (eventType != null) {
      checks.add(atLeastOneEventHasEventTypeCheck(eventType));
    }

    checks.addAll(shipmentEventChecks());
    checks.addAll(transportEventChecks());
    checks.addAll(commonEquipmentDetailsChecks());

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
      "Every event must demonstrate the correct use of the `%s` attribute: it must be present and not empty or blank."
        .formatted(EVENT_ID),
      mav -> mav.submitAllMatching(EVENTS + ".*"),
      JsonAttribute.path(EVENT_ID, JsonAttribute.matchedMustBeNonEmpty()));
  }

  private static JsonContentCheck everyEventHasEventDateTimeCheck() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
      "Every event must demonstrate the correct use of the `%s` attribute: it must be present and not empty or blank."
        .formatted(EVENT_DATE_TIME),
      mav -> mav.submitAllMatching(EVENTS + ".*"),
      JsonAttribute.path(EVENT_DATE_TIME, JsonAttribute.matchedMustBeNonEmpty()));
  }

  private static JsonContentCheck everyEventHasEventUpdatedDateTimeCheck() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
      "Every event must demonstrate the correct use of the `%s` attribute: it must be present and not empty or blank."
        .formatted(EVENT_UPDATED_DATE_TIME),
      mav -> mav.submitAllMatching(EVENTS + ".*"),
      JsonAttribute.path(EVENT_UPDATED_DATE_TIME, JsonAttribute.matchedMustBeNonEmpty()));
  }

  private static JsonContentCheck everyEventHasValidEventClassifierCheck() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
      "The `%s.%s` attribute within every event must be one of `ACTUAL`, `ESTIMATED`, or `PLANNED`."
        .formatted(EVENT_CLASSIFICATION, EVENT_CLASSIFIER),
      mav -> mav.submitAllMatching(EVENTS + ".*"),
      JsonAttribute.path(
        EVENT_CLASSIFICATION,
        JsonAttribute.path(
          EVENT_CLASSIFIER,
          JsonAttribute.matchedMustBeOneOf(TntDataSets.VALID_EVENT_CLASSIFIERS))));
  }

  private static JsonContentCheck everyEventHasValidEventSubtypeCheck() {
    return JsonAttribute.allIndividualMatchesMustBeValid(
      "For every event, the event-type-specific subtype attribute applicable to its `eventClassification.eventType` must contain a value allowed by the corresponding OpenAPI schema property: `shipmentEventType`, `transportEventType`, `equipmentEventType`, `iotEventType`, or `reeferEventType`.",
      mav -> mav.submitAllMatching(EVENTS + ".*"),
      JsonAttribute.ifMatchedThenElse(
        isEventOfType(TntEventType.SHIPMENT),
        eventSubtypeValidation(SHIPMENT_EVENT_TYPE, TntDataSets.VALID_SHIPMENT_EVENT_TYPES),
        JsonAttribute.ifMatchedThenElse(
          isEventOfType(TntEventType.TRANSPORT),
          eventSubtypeValidation(TRANSPORT_EVENT_TYPE, TntDataSets.VALID_TRANSPORT_EVENT_TYPES),
          JsonAttribute.ifMatchedThenElse(
            isEventOfType(TntEventType.EQUIPMENT),
            eventSubtypeValidation(EQUIPMENT_EVENT_TYPE, TntDataSets.VALID_EQUIPMENT_EVENT_TYPES),
            JsonAttribute.ifMatchedThenElse(
              isEventOfType(TntEventType.IOT),
              eventSubtypeValidation(IOT_EVENT_TYPE, TntDataSets.VALID_IOT_EVENT_TYPES),
              JsonAttribute.ifMatchedThen(
                isEventOfType(TntEventType.REEFER),
                eventSubtypeValidation(
                  REEFER_EVENT_TYPE, TntDataSets.VALID_REEFER_EVENT_TYPES)))))));
  }

  private static JsonContentMatchedValidation eventSubtypeValidation(
    String subtypeAttribute, Set<String> validValues) {
    return JsonAttribute.path(
      EVENT_CLASSIFICATION,
      JsonAttribute.path(subtypeAttribute, JsonAttribute.matchedMustBeOneOf(validValues)));
  }

  private static JsonContentCheck atLeastOneEventHasEventTypeCheck(TntEventType eventType) {
    return JsonAttribute.atLeastOneIndividualMatchMustBeValid(
      "For a scenario targeting `%s`, at least one event must have `%s.%s` equal to `%s`."
        .formatted(eventType, EVENT_CLASSIFICATION, EVENT_TYPE, eventType),
      mav -> mav.submitAllMatching(EVENTS + ".*." + EVENT_CLASSIFICATION + "." + EVENT_TYPE),
      JsonAttribute.matchedMustEqual(eventType::name));
  }

  private static Predicate<JsonNode> isEventOfType(TntEventType eventType) {
    return JsonAttribute.isEqualTo(EVENT_CLASSIFICATION + "." + EVENT_TYPE, eventType.name());
  }

  private static Predicate<JsonNode> isEquipmentRelatedEvent() {
    return JsonAttribute.isOneOf(
      EVENT_CLASSIFICATION + "." + EVENT_TYPE,
      Set.of(TntEventType.EQUIPMENT.name(), TntEventType.IOT.name(), TntEventType.REEFER.name()));
  }

  private static List<JsonContentCheck> commonEquipmentDetailsChecks() {
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(
      JsonAttribute.allIndividualMatchesMustBeValid(
        "The `%s.%s` attribute within every applicable event must be present and not empty or blank."
          .formatted(EQUIPMENT_DETAILS, EQUIPMENT_REFERENCE),
        mav -> mav.submitAllMatching(EVENTS + ".*"),
        JsonAttribute.ifMatchedThen(
          isEquipmentRelatedEvent(),
          JsonAttribute.path(
            EQUIPMENT_DETAILS,
            JsonAttribute.path(
              EQUIPMENT_REFERENCE, JsonAttribute.matchedMustBeNonEmpty())))));

    checks.add(
      JsonAttribute.allIndividualMatchesMustBeValid(
        "The `%s.%s` attribute within every applicable event must be present and not empty or blank."
          .formatted(EQUIPMENT_DETAILS, ISO_EQUIPMENT_CODE),
        mav -> mav.submitAllMatching(EVENTS + ".*"),
        JsonAttribute.ifMatchedThen(
          isEquipmentRelatedEvent(),
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
        "The `%s.%s.%s` attribute within every Shipment event must contain a value allowed by `DocumentReference.type` in the T&T 3.0.0 OpenAPI specification."
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
        "The `%s.%s.%s` attribute within every Shipment event must be present and not empty or blank."
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
        "Every Transport event must demonstrate the correct use of the `%s` object: it must be present and not empty"
          .formatted(EVENT_LOCATION),
        mav -> mav.submitAllMatching(EVENTS + ".*"),
        JsonAttribute.ifMatchedThen(
          isEventOfType(TntEventType.TRANSPORT),
          JsonAttribute.path(EVENT_LOCATION, JsonAttribute.matchedMustBeNonEmpty()))));

    checks.add(
      JsonAttribute.allIndividualMatchesMustBeValid(
        "The `%s.%s.%s` attribute within every Transport event must be present and not empty or blank."
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

    checks.add(transportObjectCheck(
      "When `transportDetails.transportCall.modeOfTransport` is `VESSEL` or `BARGE`, every applicable Transport event must demonstrate the correct use of `transportDetails.transportCall.vesselTransport`: it must be present and not empty",
      Set.of(VESSEL, BARGE),
      VESSEL_TRANSPORT));
    checks.add(transportObjectCheck(
      "When `transportDetails.transportCall.modeOfTransport` is `TRUCK`, every applicable Transport event must demonstrate the correct use of `transportDetails.transportCall.truckTransport`: it must be present and not empty",
      Set.of(TRUCK),
      TRUCK_TRANSPORT));
    checks.add(transportObjectCheck(
      "When `transportDetails.transportCall.modeOfTransport` is `RAIL`, every applicable Transport event must demonstrate the correct use of `transportDetails.transportCall.railTransport`: it must be present and not empty",
      Set.of(RAIL),
      RAIL_TRANSPORT));

    return checks;
  }

  private static JsonContentCheck transportObjectCheck(
    String description, Set<String> modesOfTransport, String transportObject) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
      description,
      mav -> mav.submitAllMatching(EVENTS + ".*"),
      JsonAttribute.ifMatchedThen(
        isEventOfType(TntEventType.TRANSPORT),
        JsonAttribute.path(
          TRANSPORT_DETAILS,
          JsonAttribute.path(
            TRANSPORT_CALL,
            JsonAttribute.ifMatchedThen(
              JsonAttribute.isOneOf(MODE_OF_TRANSPORT, modesOfTransport),
              JsonAttribute.path(transportObject, JsonAttribute.matchedMustBeNonEmpty()))))));
  }
}
