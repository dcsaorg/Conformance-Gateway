package org.dcsa.conformance.standards.ovs.party;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum OvsFilterParameter {
  CARRIER_SERVICE_NAME("carrierServiceName"),
  CARRIER_SERVICE_CODE("carrierServiceCode"),
  UNIVERSAL_SERVICE_REFERENCE("universalServiceReference"),
  VESSEL_IMO_NUMBER("vesselIMONumber", false, "*/vesselSchedules/*/vesselIMONumber"),
  VESSEL_NAME("vesselName", false, "*/vesselSchedules/*/vesselName"),
  CARRIER_VOYAGE_NUMBER(
      "carrierVoyageNumber",
      false,
      "*/vesselSchedules/*/transportCalls/*/carrierExportVoyageNumber",
      "*/vesselSchedules/*/transportCalls/*/carrierImportVoyageNumber"),
  UNIVERSAL_VOYAGE_REFERENCE(
      "universalVoyageReference",
      false,
      "*/vesselSchedules/*/transportCalls/*/universalImportVoyageReference",
      "*/vesselSchedules/*/transportCalls/*/universalExportVoyageReference"),
  UN_LOCATION_CODE(
      "UNLocationCode", false, "*/vesselSchedules/*/transportCalls/*/location/UNLocationCode"),
  FACILITY_SMDG_CODE(
      "facilitySMDGCode", false, "*/vesselSchedules/*/transportCalls/*/location/facilitySMDGCode"),
  START_DATE("startDate", true, "*/vesselSchedules/*/transportCalls/*/timestamps/*/eventDateTime"),
  END_DATE("endDate", true, "*/vesselSchedules/*/transportCalls/*/timestamps/*/eventDateTime"),
  LIMIT("limit", true),
  ;

  public static final Map<String, OvsFilterParameter> byQueryParamName =
      Arrays.stream(values())
          .collect(
              Collectors.toUnmodifiableMap(
                  OvsFilterParameter::getQueryParamName, Function.identity()));

  private final String queryParamName;

  private final Set<String> jsonPaths;

  private final boolean isSeparateCheckRequired;

  OvsFilterParameter(String queryParamName, boolean isSeparateCheckRequired, String... jsonPaths) {
    this.queryParamName = queryParamName;
    this.jsonPaths = Set.of(jsonPaths);
    this.isSeparateCheckRequired = isSeparateCheckRequired;
  }

  OvsFilterParameter(String queryParamName) {
    this(queryParamName, false, "*/" + queryParamName);
  }

  OvsFilterParameter(String queryParamName, boolean isSeparateCheckRequired) {
    this(queryParamName, isSeparateCheckRequired, "*/" + queryParamName);
  }
}
