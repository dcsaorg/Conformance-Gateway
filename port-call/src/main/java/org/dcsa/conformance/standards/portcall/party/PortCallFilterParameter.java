package org.dcsa.conformance.standards.portcall.party;

import lombok.Getter;

@Getter
public enum PortCallFilterParameter {
  VESSEL_IMO("vesselImo"),
  PORT_CALL_ID("portCallId"),
  SERVICE_ID("serviceId"),
  PORT_VISIT_REFERENCE("portVisitReference"),
  UN_LOCATION_CODE("UNLocationCode"),
  CARRIER_SERVICE_NAME("carrierServiceName"),
  CARRIER_SERVICE_CODE("carrierServiceCode"),
  UNIVERSAL_SERVICE_REFERENCE("universalServiceReference"),
  TERMINAL_CALL_REFERENCE("terminalCallReference"),
  CARRIER_IMPORT_VOYAGE_NUMBER("carrierImportVoyageNumber"),
  UNIVERSAL_IMPORT_VOYAGE_REFERENCE("universalImportVoyageReference"),
  CARRIER_EXPORT_VOYAGE_NUMBER("carrierExportVoyageNumber"),
  UNIVERSAL_EXPORT_VOYAGE_REFERENCE("universalExportVoyageReference"),
  PORT_CALL_SERVICE_TYPE_CODE("portCallServiceTypeCode"),
  VESSEL_IMO_NUMBER("vesselIMONumber"),
  VESSEL_NAME("vesselName"),
  VESSEL_MMSI_NUMBER("vesselMMSINumber"),
  PORT_CALL_ID_ALT("portCallID"),
  TERMINAL_CALL_ID("terminalCallID"),
  PORT_CALL_SERVICE_ID("portCallServiceID"),
  TIMESTAMP_ID("timestampID"),
  CLASSIFIER_CODE("classifierCode"),
  EVENT_TIMESTAMP_MIN("eventTimestampMin"),
  EVENT_TIMESTAMP_MAX("eventTimestampMax"),
  LIMIT("limit");

  private final String queryParamName;

  PortCallFilterParameter(String queryParamName) {
    this.queryParamName = queryParamName;
  }

  public String getQueryParamName() {
    return queryParamName;
  }
}

