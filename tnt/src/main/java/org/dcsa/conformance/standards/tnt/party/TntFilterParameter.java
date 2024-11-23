package org.dcsa.conformance.standards.tnt.party;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum TntFilterParameter {
  EVENT_TYPE("eventType"),
  SHIPMENT_EVENT_TYPE_CODE("shipmentEventTypeCode"),
  DOCUMENT_TYPE_CODE("documentTypeCode"),
  CARRIER_BOOKING_REFERENCE("carrierBookingReference"),
  TRANSPORT_DOCUMENT_REFERENCE("transportDocumentReference"),
  TRANSPORT_EVENT_TYPE_CODE("transportEventTypeCode"),
  TRANSPORT_CALL_ID("transportCallID", "transportCall/transportCallID"),
  VESSEL_IMO_NUMBER("vesselIMONumber", "transportCall/vessel/vesselIMONumber"),
  EXPORT_VOYAGE_NUMBER("exportVoyageNumber", "transportCall/exportVoyageNumber"),
  CARRIER_SERVICE_CODE("carrierServiceCode", "transportCall/carrierServiceCode"),
  UN_LOCATION_CODE("UNLocationCode", Set.of("transportCall/UNLocationCode", "eventLocation/UNLocationCode")),
  EQUIPMENT_EVENT_TYPE_CODE("equipmentEventTypeCode"),
  EQUIPMENT_REFERENCE("equipmentReference"),
  EVENT_CREATED_DATE_TIME("eventCreatedDateTime"),
  EVENT_CREATED_DATE_TIME_GTE("eventCreatedDateTime:gte"),
  EVENT_CREATED_DATE_TIME_GT("eventCreatedDateTime:gt"),
  EVENT_CREATED_DATE_TIME_LTE("eventCreatedDateTime:lte"),
  EVENT_CREATED_DATE_TIME_LT("eventCreatedDateTime:lt"),
  EVENT_CREATED_DATE_TIME_EQ("eventCreatedDateTime:eq"),
  LIMIT("limit"),
  ;

  public static final Map<String, TntFilterParameter> byQueryParamName =
      Arrays.stream(values())
          .collect(
              Collectors.toUnmodifiableMap(
                  TntFilterParameter::getQueryParamName, Function.identity()));

  private final String queryParamName;
  private final Set<String> eventPaths;

  TntFilterParameter(String queryParamName) {
    this.queryParamName = queryParamName;
    this.eventPaths = Set.of(queryParamName);
  }

  TntFilterParameter(String queryParamName, String eventPath) {
    this.queryParamName = queryParamName;
    this.eventPaths = Set.of(eventPath);
  }

  TntFilterParameter(String queryParamName, Set<String> eventPaths) {
    this.queryParamName = queryParamName;
    this.eventPaths = eventPaths;
  }
}
