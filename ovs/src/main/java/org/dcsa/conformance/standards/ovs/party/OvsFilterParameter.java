package org.dcsa.conformance.standards.ovs.party;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum OvsFilterParameter {
  CARRIER_SERVICE_NAME("carrierServiceName"),
  CARRIER_SERVICE_CODE("carrierServiceCode"),
  UNIVERSAL_SERVICE_REFERENCE("universalServiceReference"),
  VESSEL_IMO_NUMBER("vesselIMONumber"),
  VESSEL_NAME("vesselName"),
  CARRIER_VOYAGE_NUMBER("carrierVoyageNumber"),
  UNIVERSAL_VOYAGE_REFERENCE("universalVoyageReference"),
  UN_LOCATION_CODE("UNLocationCode"),
  FACILITY_SMDG_CODE("facilitySMDGCode"),
  START_DATE("startDate"),
  END_DATE("endDate"),
  LIMIT("limit"),
  ;

  public static final Map<String, OvsFilterParameter> byQueryParamName =
      Arrays.stream(values())
          .collect(
              Collectors.toUnmodifiableMap(
                  OvsFilterParameter::getQueryParamName, Function.identity()));

  @Getter private final String queryParamName;

  OvsFilterParameter(String queryParamName) {
    this.queryParamName = queryParamName;
  }
}
