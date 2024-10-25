package org.dcsa.conformance.standards.tnt.checks;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.party.SuppliedScenarioParameters;
import org.dcsa.conformance.standards.tnt.party.TntFilterParameter;
import org.dcsa.conformance.standards.tnt.party.TntRole;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.dcsa.conformance.standards.tnt.checks.TntDataSets.VALID_DOCUMENT_TYPE_CODES;
import static org.dcsa.conformance.standards.tnt.checks.TntDataSets.VALID_EQUIPMENT_EVENT_TYPES;
import static org.dcsa.conformance.standards.tnt.checks.TntDataSets.VALID_EVENT_TYPES;
import static org.dcsa.conformance.standards.tnt.checks.TntDataSets.VALID_SHIPMENT_EVENT_TYPES;

@UtilityClass
public class TntChecks {
  private static final String SHIPMENT_EVENT_TYPE = "SHIPMENT";

  public static ActionCheck responseContentChecks(UUID matched, String standardVersion,
                                                  Supplier<SuppliedScenarioParameters> sspSupplier) {

    var checks = new ArrayList<JsonContentCheck>();

    checks.add(JsonAttribute.customValidator(
      "Validate eventIds are unique",
      body -> {
        Set<String> uniqueEventIds = StreamSupport.stream(body.spliterator(), false)
          .map(node -> node.path("eventID").asText())
          .collect(Collectors.toSet());
        if (uniqueEventIds.size() != body.size()) {
          return Set.of("Event Ids are not unique");
        }
        return Set.of();
      }
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate eventCreatedDateTime parameter conditions are met",
      body -> {
        Set<OffsetDateTime> eventCreatedDateTimes = StreamSupport.stream(body.spliterator(), false)
          .map(node -> OffsetDateTime.parse(node.path("eventCreatedDateTime").asText()))
          .collect(Collectors.toSet());

        Optional<Map.Entry<TntFilterParameter, String>> eventCreatedDateTimeParam =
          sspSupplier.get().getMap().entrySet().stream()
          .filter(e -> e.getKey().equals(TntFilterParameter.EVENT_CREATED_DATE_TIME))
          .findFirst();

        if (eventCreatedDateTimeParam.isPresent()) {
          String[] eventCreatedDateTimeParamKey = eventCreatedDateTimeParam.get().getKey()
            .getQueryParamName().split(":");
          OffsetDateTime eventCreatedDateTimeParamValue =
            OffsetDateTime.parse(eventCreatedDateTimeParam.get().getValue());

          if (eventCreatedDateTimeParamKey.length == 2) {
            return validateEventCreatedDateTime(eventCreatedDateTimes, eventCreatedDateTimeParamKey,
              eventCreatedDateTimeParamValue);
          }
        }

        return Set.of();
      }
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate eventTypes are valid",
      body -> {
        Set<String> invalidEventTypes = StreamSupport.stream(body.spliterator(), false)
          .map(node -> node.path("eventType").asText())
          .filter(eventType -> !VALID_EVENT_TYPES.contains(eventType))
          .collect(Collectors.toSet());

        if (!invalidEventTypes.isEmpty()) {
          return invalidEventTypes.stream()
            .map(eventType -> "Invalid eventType: " + eventType)
            .collect(Collectors.toSet());
        }
        return Set.of();
      }
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate shipmentEventTypeCode exists and matches in JSON response " +
        "if request parameter has shipmentEventTypeCode",
      body -> {
        Optional<Map.Entry<TntFilterParameter, String>> shipmentEventTypeCodeParam =
          sspSupplier.get().getMap().entrySet().stream()
          .filter(e -> e.getKey().equals(TntFilterParameter.SHIPMENT_EVENT_TYPE_CODE))
          .findFirst();

        if (shipmentEventTypeCodeParam.isPresent()) {
          Set<String> expectedShipmentEventTypeCodes =
            Arrays.stream(shipmentEventTypeCodeParam.get().getValue().split(","))
            .map(String::trim)
            .collect(Collectors.toSet());

          Set<String> errors = filterNodesByEventType(body, SHIPMENT_EVENT_TYPE)
            .filter(node -> {
              JsonNode shipmentEventTypeCodeNode = node.path("shipmentEventTypeCode");
              return !(shipmentEventTypeCodeNode.isMissingNode() && shipmentEventTypeCodeNode.isNull())
                && !expectedShipmentEventTypeCodes.contains(shipmentEventTypeCodeNode.asText())
                && !VALID_SHIPMENT_EVENT_TYPES.contains(shipmentEventTypeCodeNode.asText());
            })
            .map(node -> "Missing or mismatched shipmentEventTypeCode " +
              "for eventType SHIPMENT at " + node.path("eventId").asText())
            .collect(Collectors.toSet());

          return errors.isEmpty() ? Set.of() : errors;
        }

        return Set.of();
      }
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate documentTypeCode exists and matches in JSON response if request parameter has documentTypeCode",
      body -> {
        Optional<Map.Entry<TntFilterParameter, String>> documentTypeCodeParam =
          sspSupplier.get().getMap().entrySet().stream()
          .filter(e -> e.getKey().equals(TntFilterParameter.DOCUMENT_TYPE_CODE))
          .findFirst();

        if (documentTypeCodeParam.isPresent()) {
          Set<String> expectedDocumentTypeCodes = Arrays.stream(documentTypeCodeParam.get().getValue().split(","))
            .map(String::trim)
            .collect(Collectors.toSet());

          Set<String> errors = Stream.concat(
              filterNodesByEventType(body, "TRANSPORT"),
              filterNodesByEventType(body, SHIPMENT_EVENT_TYPE)
            )
            .filter(node -> {
              JsonNode documentTypeCodeNode = node.path("documentTypeCode");
              return !(documentTypeCodeNode.isMissingNode() || documentTypeCodeNode.isNull())
                && !expectedDocumentTypeCodes.contains(documentTypeCodeNode.asText())
                && !VALID_DOCUMENT_TYPE_CODES.contains(documentTypeCodeNode.asText());
            })
            .map(node -> "Missing or mismatched documentTypeCode for eventType " +
              node.path("eventType").asText() + " at " + node.path("eventId").asText())
            .collect(Collectors.toSet());

          return errors.isEmpty() ? Set.of() : errors;
        }

        return Set.of();
      }
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate transportEventTypeCode exists and matches in JSON response " +
        "if request parameter has transportEventTypeCode",
      body -> {
        Optional<Map.Entry<TntFilterParameter, String>> transportEventTypeCodeParam =
          sspSupplier.get().getMap().entrySet().stream()
          .filter(e -> e.getKey().equals(TntFilterParameter.TRANSPORT_EVENT_TYPE_CODE))
          .findFirst();

        if (transportEventTypeCodeParam.isPresent()) {
          Set<String> expectedTransportEventTypeCodes =
            Arrays.stream(transportEventTypeCodeParam.get().getValue().split(","))
            .map(String::trim)
            .collect(Collectors.toSet());

          Set<String> errors = filterNodesByEventType(body, "TRANSPORT")
            .filter(node -> {
              JsonNode transportEventTypeCodeNode = node.path("transportEventTypeCode");
              return transportEventTypeCodeNode.isMissingNode()
                || transportEventTypeCodeNode.isNull()
                || !expectedTransportEventTypeCodes.contains(transportEventTypeCodeNode.asText());
            })
            .map(node -> "Missing or mismatched transportEventTypeCode for eventType TRANSPORT at "
              + node.path("eventId").asText())
            .collect(Collectors.toSet());

          return errors.isEmpty() ? Set.of() : errors;
        }

        return Set.of();
      }
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate transportCallID exists and matches in JSON response if request parameter has transportCallID",
      body -> validateParameter(
        body,
        sspSupplier,
        TntFilterParameter.TRANSPORT_CALL_ID,
        "/transportCall/transportCallID",
        "TRANSPORT", "EQUIPMENT")
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate vesselIMONumber exists and matches in JSON response if request parameter has vesselIMONumber",
      body -> validateParameter(
        body,
        sspSupplier,
        TntFilterParameter.VESSEL_IMO_NUMBER,
        "/transportCall/vessel/vesselIMONumber",
        "TRANSPORT",
        "EQUIPMENT"
      )
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate exportVoyageNumber exists and matches in JSON response if request parameter has exportVoyageNumber",
      body -> validateParameter(
        body,
        sspSupplier,
        TntFilterParameter.EXPORT_VOYAGE_NUMBER,
        "/transportCall/exportVoyageNumber",
        "TRANSPORT",
        "EQUIPMENT"
      )
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate carrierServiceCode exists and matches in JSON response if request parameter has carrierServiceCode",
      body -> validateParameter(
        body,
        sspSupplier,
        TntFilterParameter.CARRIER_SERVICE_CODE,
        "/transportCall/carrierServiceCode",
        "TRANSPORT",
        "EQUIPMENT"
      )
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate UNLocationCode exists and matches in JSON response if request parameter has UNLocationCode",
      body -> validateParameter(
        body,
        sspSupplier,
        TntFilterParameter.UN_LOCATION_CODE,
        "/transportCall/location/UNLocationCode",
        "TRANSPORT",
        "EQUIPMENT"
      )
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate equipmentEventTypeCode exists and matches in JSON response if request parameter " +
        "has equipmentEventTypeCode",
      body -> {
        Optional<Map.Entry<TntFilterParameter, String>> equipmentEventTypeCodeParam =
          sspSupplier.get().getMap().entrySet().stream()
            .filter(e -> e.getKey().equals(TntFilterParameter.EQUIPMENT_EVENT_TYPE_CODE))
            .findFirst();

        if (equipmentEventTypeCodeParam.isPresent()) {
          Set<String> expectedEquipmentEventTypeCodes =
            Arrays.stream(equipmentEventTypeCodeParam.get().getValue().split(","))
              .map(String::trim)
              .collect(Collectors.toSet());

          Set<String> errors = filterNodesByEventType(body, "EQUIPMENT")
            .filter(node -> {
              JsonNode equipmentEventTypeCodeNode = node.path("equipmentEventTypeCode");
              return equipmentEventTypeCodeNode.isMissingNode() || equipmentEventTypeCodeNode.isNull()
                || !expectedEquipmentEventTypeCodes.contains(equipmentEventTypeCodeNode.asText())
                || !VALID_EQUIPMENT_EVENT_TYPES.contains(equipmentEventTypeCodeNode.asText());
            })
            .map(node -> "Missing or mismatched equipmentEventTypeCode for eventType EQUIPMENT at "
              + node.path("eventId").asText())
            .collect(Collectors.toSet());

          return errors.isEmpty() ? Set.of() : errors;
        }

        return Set.of();
      }
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate limit parameter is met",
      body -> {
        Optional<Map.Entry<TntFilterParameter, String>> limitParam =
          sspSupplier.get().getMap().entrySet().stream()
            .filter(e -> e.getKey().equals(TntFilterParameter.LIMIT))
            .findFirst();

        if (limitParam.isPresent()) {
          int expectedLimit = Integer.parseInt(limitParam.get().getValue().trim());
          if (body.size() > expectedLimit) {
            return Set.of("The number of events exceeds the limit parameter: " + expectedLimit);
          }
        }

        return Set.of();
      }
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate equipmentReference exists and matches in JSON response " +
        "if request parameter has equipmentReference",
      body -> validateEquipmentReference(
        body,
        sspSupplier)
    ));

    checks.add(JsonAttribute.customValidator(
      "Validate carrierBookingReference exists and matches in JSON response " +
        "if request parameter has carrierBookingReference",
      body -> validateCarrierBookingReference(
        body,
        sspSupplier)
    ));
    return JsonAttribute.contentChecks(
      TntRole::isPublisher,
      matched,
      HttpMessageType.RESPONSE,
      standardVersion,
      checks
    );
  }

  private Set<String> validateParameter(JsonNode body, Supplier<SuppliedScenarioParameters> sspSupplier,
                                        TntFilterParameter parameter, String jsonPath,
                                        String eventType1, String eventType2) {
    Optional<Map.Entry<TntFilterParameter, String>> param = sspSupplier.get().getMap().entrySet().stream()
      .filter(e -> e.getKey().equals(parameter))
      .findFirst();

    if (param.isPresent()) {
      Set<String> expectedValues = Arrays.stream(param.get().getValue().split(","))
        .map(String::trim)
        .collect(Collectors.toSet());

      Set<String> errors = Stream.concat(
          filterNodesByEventType(body, eventType1),
          filterNodesByEventType(body, eventType2)
        )
        .filter(node -> {
          JsonNode valueNode = node.at(jsonPath);
          return !(valueNode.isMissingNode()
            || valueNode.isNull())
            && !expectedValues.contains(valueNode.asText());
        })
        .map(node -> "Missing or mismatched " + jsonPath + " for eventType "
          + node.path("eventType").asText() + " at " + node.path("eventId").asText())
        .collect(Collectors.toSet());

      return errors.isEmpty() ? Set.of() : errors;
    }

    return Set.of();
  }

  private Set<String> validateEquipmentReference(JsonNode body, Supplier<SuppliedScenarioParameters> sspSupplier) {
    Optional<Map.Entry<TntFilterParameter, String>> equipmentReferenceParam =
      sspSupplier.get().getMap().entrySet().stream()
      .filter(e -> e.getKey().equals(TntFilterParameter.EQUIPMENT_REFERENCE))
      .findFirst();

    if (equipmentReferenceParam.isPresent()) {
      Set<String> expectedEquipmentReferences = Arrays.stream(equipmentReferenceParam.get().getValue().split(","))
        .map(String::trim)
        .collect(Collectors.toSet());

      Set<String> errors = Stream.concat(
          filterNodesByEventType(body, SHIPMENT_EVENT_TYPE),
          filterNodesByEventType(body, "TRANSPORT")
        )
        .filter(node -> {
          JsonNode referencesNode = node.path("references");
          boolean isValid = StreamSupport.stream(referencesNode.spliterator(), false)
            .anyMatch(refNode -> !(refNode.path("referenceType").asText().equals("EQ") &&
              expectedEquipmentReferences.contains(refNode.path("referenceValue").asText())));
          return !isValid;
        })
        .map(node -> "Missing or mismatched equipmentReference for eventType "
          + node.path("eventType").asText() + " at " + node.path("eventId").asText())
        .collect(Collectors.toSet());

      errors.addAll(
        filterNodesByEventType(body, "EQUIPMENT")
          .filter(node -> {
            JsonNode equipmentReferenceNode = node.path("equipmentReference");
            return !(equipmentReferenceNode.isMissingNode() || equipmentReferenceNode.isNull())
              && !expectedEquipmentReferences.contains(equipmentReferenceNode.asText());
          })
          .map(node -> "Missing or mismatched equipmentReference for eventType EQUIPMENT at "
            + node.path("eventId").asText())
          .collect(Collectors.toSet())
      );

      return errors.isEmpty() ? Set.of() : errors;
    }

    return Set.of();
  }


  private Set<String> validateCarrierBookingReference(JsonNode body, Supplier<SuppliedScenarioParameters> sspSupplier) {
    Optional<Map.Entry<TntFilterParameter, String>> carrierBookingReferenceParam =
      sspSupplier.get().getMap().entrySet().stream()
      .filter(e -> e.getKey().equals(TntFilterParameter.CARRIER_BOOKING_REFERENCE))
      .findFirst();

    if (carrierBookingReferenceParam.isPresent()) {
      Set<String> expectedCarrierBookingReferences =
        Arrays.stream(carrierBookingReferenceParam.get().getValue().split(","))
        .map(String::trim)
        .collect(Collectors.toSet());

      Set<String> errors = Stream.concat(
          filterNodesByEventType(body, "TRANSPORT"),
          filterNodesByEventType(body, "EQUIPMENT")
        )
        .filter(node -> {
          JsonNode documentReferencesNode = node.path("documentReferences");
          boolean isValid = StreamSupport.stream(documentReferencesNode.spliterator(), false)
            .anyMatch(refNode -> !(refNode.path("documentReferenceType").asText().equals("BKG") &&
              expectedCarrierBookingReferences.contains(refNode.path("documentReferenceValue").asText())));
          return !isValid;
        })
        .map(node -> "Missing or mismatched carrierBookingReference for eventType " + node.path("eventType").asText()
          + " at " + node.path("eventId").asText())
        .collect(Collectors.toSet());

      errors.addAll(
        filterNodesByEventType(body, SHIPMENT_EVENT_TYPE)
          .filter(node -> {
            JsonNode documentTypeCodeNode = node.path("documentTypeCode");
            JsonNode documentIDNode = node.path("documentID");
            return documentTypeCodeNode.asText().equals("BKG")
              && !expectedCarrierBookingReferences.contains(documentIDNode.asText());
          })
          .map(node -> "Missing or mismatched carrierBookingReference for eventType SHIPMENT at "
            + node.path("eventId").asText())
          .collect(Collectors.toSet())
      );

      return errors.isEmpty() ? Set.of() : errors;
    }

    return Set.of();
  }



  private Set<String> validateEventCreatedDateTime(
    Set<OffsetDateTime> eventCreatedDateTimes,
    String[] eventCreatedDateTimeParamKey,
    OffsetDateTime eventCreatedDateTimeParamValue
  ) {
    switch (eventCreatedDateTimeParamKey[1]) {
      case "gte":
        if (eventCreatedDateTimes.stream()
          .anyMatch(dateTime -> dateTime.isBefore(eventCreatedDateTimeParamValue))) {
          return Set.of("Event Created Date Time is not greater than or equal to " + eventCreatedDateTimeParamValue);
        }
        break;
      case "gt":
        if (eventCreatedDateTimes.stream()
          .anyMatch(dateTime -> dateTime.compareTo(eventCreatedDateTimeParamValue) <= 0)) {
          return Set.of("Event Created Date Time is not greater than " + eventCreatedDateTimeParamValue);
        }
        break;
      case "lte":
        if (eventCreatedDateTimes.stream()
          .anyMatch(dateTime -> dateTime.compareTo(eventCreatedDateTimeParamValue) > 0)) {
          return Set.of("Event Created Date Time is not less than or equal to " + eventCreatedDateTimeParamValue);
        }
        break;
      case "lt":
        if (eventCreatedDateTimes.stream()
          .anyMatch(dateTime -> dateTime.compareTo(eventCreatedDateTimeParamValue) >= 0)) {
          return Set.of("Event Created Date Time is not less than " + eventCreatedDateTimeParamValue);
        }
        break;
      case "eq":
        if (eventCreatedDateTimes.stream()
          .anyMatch(dateTime -> dateTime.compareTo(eventCreatedDateTimeParamValue) != 0)) {
          return Set.of("Event Created Date Time is not equal to " + eventCreatedDateTimeParamValue);
        }
        break;
    }
    return Set.of();
  }

  private Stream<JsonNode> filterNodesByEventType(JsonNode body, String eventType) {
    return StreamSupport.stream(body.spliterator(), false)
      .filter(node -> node.path("eventType").asText().equals(eventType));
  }
}
