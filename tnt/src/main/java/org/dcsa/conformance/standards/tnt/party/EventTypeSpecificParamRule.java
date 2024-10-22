package org.dcsa.conformance.standards.tnt.party;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EventTypeSpecificParamRule implements QueryParamRule{

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
      if (!queryParams.containsKey("eventType")) {
        return true; // No eventType, no validation needed
      }

      Set<String> eventTypes = new HashSet<>(queryParams.get("eventType"));
      Set<String> allowedParams = queryParamMap.entrySet().stream()
        .filter(entry -> eventTypes.contains(entry.getKey()))
        .flatMap(entry -> entry.getValue().stream())
        .collect(Collectors.toSet());

      return queryParams.keySet().stream()
        .filter(Predicate.not(excludedQueryParams::contains))
        .allMatch(allowedParams::contains);
    }

}


