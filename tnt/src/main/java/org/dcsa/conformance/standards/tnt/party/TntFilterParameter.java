package org.dcsa.conformance.standards.tnt.party;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

public enum TntFilterParameter {
  EVENT_TYPE("eventType"),
  SHIPMENT_EVENT_TYPE_CODE("shipmentEventTypeCode"),
  DOCUMENT_TYPE_CODE("documentTypeCode"),
  CARRIER_BOOKING_REFERENCE("carrierBookingReference"),
  TRANSPORT_DOCUMENT_REFERENCE("transportDocumentReference"),
  TRANSPORT_EVENT_TYPE_CODE("transportEventTypeCode"),
  TRANSPORT_CALL_ID("transportCallID"),
  VESSEL_IMO_NUMBER("vesselIMONumber"),
  EXPORT_VOYAGE_NUMBER("exportVoyageNumber"),
  CARRIER_SERVICE_CODE("carrierServiceCode"),
  UN_LOCATION_CODE("UNLocationCode"),
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

  @Getter private final String queryParamName;

  TntFilterParameter(String queryParamName) {
    this.queryParamName = queryParamName;
  }
}
