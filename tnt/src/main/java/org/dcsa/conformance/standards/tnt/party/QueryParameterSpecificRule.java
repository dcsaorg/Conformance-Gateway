package org.dcsa.conformance.standards.tnt.party;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.dcsa.conformance.standards.tnt.checks.TntDataSets.VALID_DOCUMENT_TYPE_CODES;
import static org.dcsa.conformance.standards.tnt.checks.TntDataSets.VALID_EQUIPMENT_EVENT_TYPES;
import static org.dcsa.conformance.standards.tnt.checks.TntDataSets.VALID_SHIPMENT_EVENT_TYPES;

public class QueryParameterSpecificRule implements QueryParamRule{

  private final Map<String, Set<String>> queryParamMap = Map.of(
      "SHIPMENT", Set.of("shipmentEventTypeCode", "documentTypeCode", "carrierBookingReference",
      "transportDocumentID", "transportDocumentReference", "equipmentReference", "eventCreatedDateTime"),
      "TRANSPORT", Set.of("transportDocumentReference", "transportEventTypeCode", "transportCallID", "vesselIMONumber",
      "exportVoyageNumber", "carrierServiceCode", "UNLocationCode", "equipmentReference", "eventCreatedDateTime"),
      "EQUIPMENT", Set.of("carrierBookingReference", "transportDocumentReference", "transportCallID",
      "vesselIMONumber", "exportVoyageNumber", "carrierServiceCode", "UNLocationCode", "equipmentEventTypeCode", "equipmentReference", "eventCreatedDateTime")
    );
    private final Set<String> excludedQueryParams = Set.of("eventType", "cursor", "limit", "eventCreatedDateTime:gte",
      "eventCreatedDateTime:gt", "eventCreatedDateTime:lt", "eventCreatedDateTime:lte", "eventCreatedDateTime:eq", "eventCreatedDateTime");

    @Override
    public boolean validate(Map<String, ? extends Collection<String>> queryParams) {
      boolean areParamsValid = true;

      if (queryParams.containsKey("eventType")) {
        Set<String> eventTypes = new HashSet<>(queryParams.get("eventType"));
        Set<String> allowedParams = queryParamMap.entrySet().stream()
          .filter(entry -> eventTypes.contains(entry.getKey()))
          .flatMap(entry -> entry.getValue().stream())
          .collect(Collectors.toSet());

        areParamsValid = queryParams.keySet().stream()
          .filter(Predicate.not(excludedQueryParams::contains))
          .allMatch(allowedParams::contains);
      }

      if (queryParams.containsKey("shipmentEventTypeCode")) {
        return areParamsValid && validateShipmentEventTypeCode(queryParams.get("shipmentEventTypeCode"));
      }
      if (queryParams.containsKey("documentTypeCode")) {
        return areParamsValid && validateDocumentTypeCode(queryParams.get("documentTypeCode"));
      }
      if (queryParams.containsKey("equipmentTypeCode")) {
        return areParamsValid && validateEquipmentEventTypeCode(queryParams.get("equipmentTypeCode"));
      }

      return areParamsValid;
    }

  private boolean validateShipmentEventTypeCode(Collection<String> shipmentEventTypeCodes) {
    return shipmentEventTypeCodes.stream().allMatch(VALID_SHIPMENT_EVENT_TYPES::contains);
  }
  private boolean validateDocumentTypeCode(Collection<String> documentTypeCodes) {
    return documentTypeCodes.stream().allMatch(VALID_DOCUMENT_TYPE_CODES::contains);
  }
  private boolean validateEquipmentEventTypeCode(Collection<String> equipmentTypeCodes) {
    return equipmentTypeCodes.stream().allMatch(VALID_EQUIPMENT_EVENT_TYPES::contains);
  }

}


