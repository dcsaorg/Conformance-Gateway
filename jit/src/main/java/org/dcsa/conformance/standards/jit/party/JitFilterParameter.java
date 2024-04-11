package org.dcsa.conformance.standards.jit.party;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

public enum JitFilterParameter {
  TRANSPORT_CALL_ID("transportCallID"),
  VESSEL_IMO_NUMBER("vesselIMONumber"),
  CARRIER_SERVICE_CODE("carrierServiceCode"),
  UN_LOCATION_CODE("UNLocationCode"),
  OPERATIONS_EVENT_TYPE_CODE("operationsEventTypeCode"),
  EVENT_CREATED_DATE_TIME("eventCreatedDateTime"),
  EVENT_CREATED_DATE_TIME_GTE("eventCreatedDateTime:gte"),
  EVENT_CREATED_DATE_TIME_GT("eventCreatedDateTime:gt"),
  EVENT_CREATED_DATE_TIME_LTE("eventCreatedDateTime:lte"),
  EVENT_CREATED_DATE_TIME_LT("eventCreatedDateTime:lt"),
  EVENT_CREATED_DATE_TIME_EQ("eventCreatedDateTime:eq"),
  LIMIT("limit"),
  ;

  public static final Map<String, JitFilterParameter> byQueryParamName =
      Arrays.stream(values())
          .collect(
              Collectors.toUnmodifiableMap(
                  JitFilterParameter::getQueryParamName, Function.identity()));

  @Getter private final String queryParamName;

  JitFilterParameter(String queryParamName) {
    this.queryParamName = queryParamName;
  }
}
