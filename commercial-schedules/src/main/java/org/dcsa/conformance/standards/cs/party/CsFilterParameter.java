package org.dcsa.conformance.standards.cs.party;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum CsFilterParameter {
  PLACE_OF_RECEIPT("placeOfReceipt"),
  PLACE_OF_DELIVERY("placeOfDelivery"),
  DEPARTURE_START_DATE("departureStartDate"),
  DEPARTURE_END_DATE("departureEndDate"),
  ARRIVAL_START_DATE("arrivalStartDate"),
  ARRIVAL_END_DATE("arrivalEndDate"),
  MAX_TRANSHIPMENT("maxTranshipment"),
  RECEIPT_TYPE_AT_ORIGIN("receiptTypeAtOrigin"),
  DELIVERY_TYPE_AT_DESTINATION("deliveryTypeAtDestination"),
  DATE("date"),
  CARRIER_SERVICE_NAME("carrierServiceName"),
  CARRIER_SERVICE_CODE("carrierServiceCode"),
  UNIVERSAL_SERVICE_REFERENCE("universalServiceReference"),
  VESSEL_IMO_NUMBER("vesselIMONumber"),
  VESSEL_NAME("vesselName"),
  CARRIER_VOYAGE_NUMBER("carrierVoyageNumber"),
  UNIVERSAL_VOYAGE_REFERENCE("universalVoyageReference"),
  VESSEL_OPERATOR_CARRIER_CODE("vesselOperatorCarrierCode"),
  UN_LOCATION_CODE("UNLocationCode"),
  FACILITY_SMDG_CODE("facilitySMDGCode");

  public static final Map<String, CsFilterParameter> byQueryParamName =
      Arrays.stream(values())
          .collect(
              Collectors.toUnmodifiableMap(
                  CsFilterParameter::getQueryParamName, Function.identity()));
  @Getter private final String queryParamName;

  CsFilterParameter(String queryParamName) {
    this.queryParamName = queryParamName;
  }
}
