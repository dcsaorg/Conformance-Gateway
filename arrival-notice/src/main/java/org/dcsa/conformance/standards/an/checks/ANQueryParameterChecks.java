package org.dcsa.conformance.standards.an.checks;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.an.party.ANFilterParameter;
import org.dcsa.conformance.standards.an.party.ANRole;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

public final class ANQueryParameterChecks {

  private static final String ARRIVAL_NOTICES = "arrivalNotices";
  private static final String TRANSPORT_DOCUMENT_REFERENCE = "transportDocumentReference";
  private static final String TRANSPORT = "transport";
  private static final String PORT_OF_DISCHARGE = "portOfDischarge";
  private static final String UN_LOCATION_CODE = "UNLocationCode";
  private static final String PORT_OF_DISCHARGE_ARRIVAL_DATE = "portOfDischargeArrivalDate";
  private static final String VALUE = "value";
  private static final String LEGS = "legs";
  private static final String VESSEL_VOYAGE = "vesselVoyage";
  private static final String UTILIZED_TRANSPORT_EQUIPMENTS = "utilizedTransportEquipments";
  private static final String EQUIPMENT = "equipment";
  private static final String EQUIPMENT_REFERENCE = "equipmentReference";

  private ANQueryParameterChecks() {
  }

  public static ActionCheck matchingResponse(
    UUID matchedExchangeUuid,
    String expectedApiVersion,
    Supplier<Map<String, String>> suppliedParameters) {
    return JsonAttribute.contentChecks(
      ANRole::isProducer,
      matchedExchangeUuid,
      HttpMessageType.RESPONSE,
      expectedApiVersion,
      JsonAttribute.customValidator(
        "At least one Arrival Notice matches all supplied filtering query parameters",
        body -> {
          Map<String, String> parameters = suppliedParameters.get();
          boolean matches =
            parameters == null
              || parameters.entrySet().stream()
              .filter(entry -> !ANFilterParameter.LIMIT.getQueryParamName().equals(entry.getKey()))
              .findAny()
              .isEmpty()
              || StreamSupport.stream(body.path(ARRIVAL_NOTICES).spliterator(), false)
              .anyMatch(notice -> matchesAll(notice, parameters));
          return ConformanceCheckResult.simple(
            matches ? Set.of() : Set.of("No Arrival Notice matched all supplied filtering query parameters"));
        }));
  }

  static boolean matchesAll(JsonNode notice, Map<String, String> parameters) {
    return parameters.entrySet().stream()
      .filter(entry -> !ANFilterParameter.LIMIT.getQueryParamName().equals(entry.getKey()))
      .allMatch(entry -> matches(notice, entry));
  }

  private static boolean matches(JsonNode notice, Map.Entry<String, String> entry) {
    ANFilterParameter parameter = ANFilterParameter.BY_QUERY_PARAM_NAME.get(entry.getKey());
    return switch (parameter) {
      case TRANSPORT_DOCUMENT_REFERENCES ->
        containsCsvValue(entry.getValue(), notice.path(TRANSPORT_DOCUMENT_REFERENCE).asText());
      case EQUIPMENT_REFERENCES -> StreamSupport.stream(notice.path(UTILIZED_TRANSPORT_EQUIPMENTS).spliterator(), false)
        .map(item -> item.path(EQUIPMENT).path(EQUIPMENT_REFERENCE).asText())
        .anyMatch(value -> containsCsvValue(entry.getValue(), value));
      case PORT_OF_DISCHARGE -> entry
        .getValue()
        .equals(
          notice
            .path(TRANSPORT)
            .path(PORT_OF_DISCHARGE)
            .path(UN_LOCATION_CODE)
            .asText());
      case VESSEL_IMO_NUMBER,
           VESSEL_NAME,
           CARRIER_IMPORT_VOYAGE_NUMBER,
           UNIVERSAL_IMPORT_VOYAGE_REFERENCE,
           CARRIER_SERVICE_CODE,
           UNIVERSAL_SERVICE_REFERENCE -> matchesVesselVoyage(notice, parameter, entry.getValue());
      case PORT_OF_DISCHARGE_ARRIVAL_DATE_MIN -> matchesArrivalDate(notice, entry.getValue(), true);
      case PORT_OF_DISCHARGE_ARRIVAL_DATE_MAX -> matchesArrivalDate(notice, entry.getValue(), false);
      case LIMIT -> true;
    };
  }

  private static boolean matchesVesselVoyage(
    JsonNode notice, ANFilterParameter parameter, String expectedValue) {
    return StreamSupport.stream(
        notice.path(TRANSPORT).path(LEGS).spliterator(), false)
      .map(leg -> leg.path(VESSEL_VOYAGE).path(parameter.getQueryParamName()).asText())
      .anyMatch(expectedValue::equals);
  }

  private static boolean matchesArrivalDate(JsonNode notice, String boundary, boolean minimum) {
    try {
      LocalDate arrivalDate =
        LocalDate.parse(
          notice
            .path(TRANSPORT)
            .path(PORT_OF_DISCHARGE_ARRIVAL_DATE)
            .path(VALUE)
            .asText());
      LocalDate suppliedBoundary = LocalDate.parse(boundary);
      return minimum
        ? !arrivalDate.isBefore(suppliedBoundary)
        : !arrivalDate.isAfter(suppliedBoundary);
    } catch (DateTimeParseException e) {
      return false;
    }
  }

  private static boolean containsCsvValue(String csv, String expected) {
    return Arrays.stream(csv.split(",")).map(String::trim).anyMatch(expected::equals);
  }
}



