package org.dcsa.conformance.standards.an.party;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum ANFilterParameter {
  TRANSPORT_DOCUMENT_REFERENCES("transportDocumentReferences"),
  EQUIPMENT_REFERENCES("equipmentReferences"),
  PORT_OF_DISCHARGE("portOfDischarge"),
  VESSEL_IMO_NUMBER("vesselIMONumber"),
  VESSEL_NAME("vesselName"),
  CARRIER_IMPORT_VOYAGE_NUMBER("carrierImportVoyageNumber"),
  UNIVERSAL_IMPORT_VOYAGE_REFERENCE("universalImportVoyageReference"),
  CARRIER_SERVICE_CODE("carrierServiceCode"),
  UNIVERSAL_SERVICE_REFERENCE("universalServiceReference"),
  PORT_OF_DISCHARGE_ARRIVAL_DATE_MIN("portOfDischargeArrivalDateMin"),
  PORT_OF_DISCHARGE_ARRIVAL_DATE_MAX("portOfDischargeArrivalDateMax"),
  LIMIT("limit");

  public static final Map<String, ANFilterParameter> BY_QUERY_PARAM_NAME =
    Arrays.stream(values())
      .collect(Collectors.toUnmodifiableMap(ANFilterParameter::getQueryParamName, Function.identity()));

  private final String queryParamName;

  ANFilterParameter(String queryParamName) {
    this.queryParamName = queryParamName;
  }
}

