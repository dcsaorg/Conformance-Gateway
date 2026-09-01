package org.dcsa.conformance.standards.tnt.v300.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.util.JsonUtil;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@UtilityClass
public class TntEventQueryFilter {

  private static final String SHIPMENT_DETAILS = "shipmentDetails";
  private static final String DOCUMENT_REFERENCE = "documentReference";
  private static final String DOCUMENT_REFERENCE_TYPE = "type";
  private static final String DOCUMENT_REFERENCE_VALUE = "reference";
  private static final String ADDITIONAL_DOCUMENT_REFERENCES = "additionalDocumentReferences";
  private static final String TRANSPORT_DOCUMENT_TYPE = "TRANSPORT_DOCUMENT";
  private static final String BOOKING_DOCUMENT_TYPE = "BOOKING";
  private static final String EQUIPMENT_DETAILS = "equipmentDetails";
  private static final String EQUIPMENT_REFERENCE = "equipmentReference";
  private static final String EVENT_UPDATED_DATE_TIME = "eventUpdatedDateTime";

  public static JsonNode filterEvents(
    JsonNode responseBody, Map<String, ? extends Collection<String>> queryParams) {
    if (!responseBody.isObject() || !responseBody.has(TntConstants.EVENTS)) {
      return responseBody;
    }

    JsonNode events = responseBody.path(TntConstants.EVENTS);
    if (!events.isArray()) {
      return responseBody;
    }

    String carrierBookingReference =
      JsonUtil.getFirstQueryParamValue(queryParams, TntQueryParameters.CBR.getParameterName());
    String transportDocumentReference =
      JsonUtil.getFirstQueryParamValue(queryParams, TntQueryParameters.TDR.getParameterName());
    String equipmentReference =
      JsonUtil.getFirstQueryParamValue(queryParams, TntQueryParameters.ER.getParameterName());
    String eventTypes =
      JsonUtil.getFirstQueryParamValue(queryParams, TntQueryParameters.ET.getParameterName());
    String eventUpdatedDateTimeMin =
      JsonUtil.getFirstQueryParamValue(queryParams, TntQueryParameters.E_UDT_MIN.getParameterName());
    String eventUpdatedDateTimeMax =
      JsonUtil.getFirstQueryParamValue(queryParams, TntQueryParameters.E_UDT_MAX.getParameterName());

    Set<String> requestedEventTypes = parseCsvValues(eventTypes);
    OffsetDateTime minDateTime = parseDateTime(eventUpdatedDateTimeMin);
    OffsetDateTime maxDateTime = parseDateTime(eventUpdatedDateTimeMax);

    ArrayNode filteredEvents =
      StreamSupport.stream(events.spliterator(), false)
        .filter(event -> matchesEventTypes(event, requestedEventTypes))
        .filter(event -> matchesCarrierBookingReference(event, carrierBookingReference))
        .filter(event -> matchesTransportDocumentReference(event, transportDocumentReference))
        .filter(event -> matchesEquipmentReference(event, equipmentReference))
        .filter(event -> matchesEventUpdatedDateTimeRange(event, minDateTime, maxDateTime))
        .collect(OBJECT_MAPPER::createArrayNode, ArrayNode::add, ArrayNode::addAll);

    ObjectNode filteredResponse = responseBody.deepCopy();
    filteredResponse.set(TntConstants.EVENTS, filteredEvents);
    return filteredResponse;
  }

  private static Set<String> parseCsvValues(String csvValues) {
    if (csvValues == null || csvValues.isBlank()) {
      return Collections.emptySet();
    }
    return StreamSupport.stream(java.util.Arrays.spliterator(csvValues.split(",")), false)
      .map(String::trim)
      .filter(value -> !value.isBlank())
      .collect(Collectors.toSet());
  }

  private static OffsetDateTime parseDateTime(String dateTime) {
    if (dateTime == null || dateTime.isBlank()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(dateTime);
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }

  private static boolean matchesEventTypes(JsonNode event, Set<String> requestedEventTypes) {
    if (requestedEventTypes.isEmpty()) {
      return true;
    }
    String eventType = event.path(TntConstants.EVENT_CLASSIFICATION).path(TntConstants.EVENT_TYPE).asText("");
    return requestedEventTypes.contains(eventType);
  }

  private static boolean matchesCarrierBookingReference(JsonNode event, String carrierBookingReference) {
    if (carrierBookingReference == null || carrierBookingReference.isBlank()) {
      return true;
    }

    JsonNode docRef = event.path(SHIPMENT_DETAILS).path(DOCUMENT_REFERENCE);
    String type = docRef.path(DOCUMENT_REFERENCE_TYPE).asText("");
    String value = docRef.path(DOCUMENT_REFERENCE_VALUE).asText("");
    return BOOKING_DOCUMENT_TYPE.equals(type) && carrierBookingReference.equals(value);
  }

  private static boolean matchesTransportDocumentReference(
    JsonNode event, String transportDocumentReference) {
    if (transportDocumentReference == null || transportDocumentReference.isBlank()) {
      return true;
    }

    JsonNode additionalDocumentReferences =
      event.path(SHIPMENT_DETAILS).path(ADDITIONAL_DOCUMENT_REFERENCES);
    if (!additionalDocumentReferences.isArray()) {
      return false;
    }

    return StreamSupport.stream(additionalDocumentReferences.spliterator(), false)
      .anyMatch(
        docRef ->
          TRANSPORT_DOCUMENT_TYPE.equals(docRef.path(DOCUMENT_REFERENCE_TYPE).asText(""))
            && transportDocumentReference.equals(
            docRef.path(DOCUMENT_REFERENCE_VALUE).asText("")));
  }

  private static boolean matchesEquipmentReference(JsonNode event, String equipmentReference) {
    if (equipmentReference == null || equipmentReference.isBlank()) {
      return true;
    }
    String value = event.path(EQUIPMENT_DETAILS).path(EQUIPMENT_REFERENCE).asText("");
    return equipmentReference.equals(value);
  }

  private static boolean matchesEventUpdatedDateTimeRange(
    JsonNode event, OffsetDateTime minDateTime, OffsetDateTime maxDateTime) {
    if (minDateTime == null && maxDateTime == null) {
      return true;
    }

    String eventUpdatedDateTimeValue = event.path(EVENT_UPDATED_DATE_TIME).asText("");
    OffsetDateTime eventUpdatedDateTime;
    try {
      eventUpdatedDateTime = OffsetDateTime.parse(eventUpdatedDateTimeValue);
    } catch (DateTimeParseException e) {
      return false;
    }

    if (minDateTime != null && eventUpdatedDateTime.isBefore(minDateTime)) {
      return false;
    }
    if (maxDateTime != null && eventUpdatedDateTime.isAfter(maxDateTime)) {
      return false;
    }
    return true;
  }
}

