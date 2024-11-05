package org.dcsa.conformance.standards.tnt.party;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.dcsa.conformance.standards.tnt.checks.TntDataSets.VALID_DOCUMENT_TYPE_CODES;
import static org.dcsa.conformance.standards.tnt.checks.TntDataSets.VALID_EQUIPMENT_EVENT_TYPES;
import static org.dcsa.conformance.standards.tnt.checks.TntDataSets.VALID_EVENT_TYPES;
import static org.dcsa.conformance.standards.tnt.checks.TntDataSets.VALID_SHIPMENT_EVENT_TYPES;

public class QueryParameterSpecificRule implements QueryParamRule{

  private static final String EVENT_TYPE = "eventType";

  private static final Map<String, Set<String>> allowedQueryParamForEventTypeMap = Map.of(
      "SHIPMENT", Set.of("shipmentEventTypeCode", "documentTypeCode", "carrierBookingReference",
      "transportDocumentID", "transportDocumentReference", "equipmentReference", "eventCreatedDateTime"),
      "TRANSPORT", Set.of("transportDocumentReference", "transportEventTypeCode", "transportCallID", "vesselIMONumber",
      "exportVoyageNumber", "carrierServiceCode", "UNLocationCode", "equipmentReference", "eventCreatedDateTime", "carrierBookingReference"),
      "EQUIPMENT", Set.of("carrierBookingReference", "transportDocumentReference", "transportCallID",
      "vesselIMONumber", "exportVoyageNumber", "carrierServiceCode", "UNLocationCode", "equipmentEventTypeCode", "equipmentReference", "eventCreatedDateTime")
    );
  private static final Set<String> excludedQueryParams = Set.of("eventType", "cursor", "limit", "eventCreatedDateTime:gte",
    "eventCreatedDateTime:gt", "eventCreatedDateTime:lt", "eventCreatedDateTime:lte", "eventCreatedDateTime:eq", "eventCreatedDateTime");

  @Override
  public boolean validate(Map<String, ? extends Collection<String>> queryParams) {
    boolean areParamsValid = true;

    if (queryParams.containsKey(EVENT_TYPE)) {
      Set<String> eventTypes = queryParams.get("eventType").stream()
        .flatMap(eventType -> Arrays.stream(eventType.split(",")))
        .collect(Collectors.toSet());
      Set<String> allowedParams = allowedQueryParamForEventTypeMap.entrySet().stream()
        .filter(entry -> eventTypes.contains(entry.getKey()))
        .flatMap(entry -> entry.getValue().stream())
        .collect(Collectors.toSet());

      areParamsValid = queryParams.keySet().stream()
        .filter(Predicate.not(excludedQueryParams::contains))
        .allMatch(allowedParams::contains);
    }

    if (queryParams.containsKey(EVENT_TYPE)) {
      return areParamsValid && validateEventType(queryParams.get(EVENT_TYPE));
    }
    if (queryParams.containsKey("shipmentEventTypeCode")) {
      return areParamsValid && validateShipmentEventTypeCode(queryParams.get("shipmentEventTypeCode"));
    }
    if (queryParams.containsKey("documentTypeCode")) {
      return areParamsValid && validateDocumentTypeCode(queryParams.get("documentTypeCode"));
    }
    if (queryParams.containsKey("equipmentEventTypeCode")) {
      return areParamsValid && validateEquipmentEventTypeCode(queryParams.get("equipmentEventTypeCode"));
    }

    return areParamsValid;
  }


  private boolean validateEventType(Collection<String> eventTypes) {
    return eventTypes.stream()
      .flatMap(eventType -> Arrays.stream(eventType.split(",")))
      .allMatch(VALID_EVENT_TYPES::contains);
  }

  private boolean validateShipmentEventTypeCode(Collection<String> shipmentEventTypeCodes) {
    return shipmentEventTypeCodes.stream()
      .flatMap(eventType -> Arrays.stream(eventType.split(",")))
      .allMatch(VALID_SHIPMENT_EVENT_TYPES::contains);
  }
  private boolean validateDocumentTypeCode(Collection<String> documentTypeCodes) {
    return documentTypeCodes.stream()
      .flatMap(eventType -> Arrays.stream(eventType.split(",")))
      .allMatch(VALID_DOCUMENT_TYPE_CODES::contains);
  }
  private boolean validateEquipmentEventTypeCode(Collection<String> equipmentTypeCodes) {
    return equipmentTypeCodes.stream()
      .flatMap(eventType -> Arrays.stream(eventType.split(",")))
      .allMatch(VALID_EQUIPMENT_EVENT_TYPES::contains);
  }

}


