package org.dcsa.conformance.standards.tnt.v300.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.util.JsonUtil;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

/**
 * Filters the events of a simulated {@code GET /events} response according to the query parameters
 * of the request.
 *
 * <p>An event matches a reference filter only when it actually carries that reference: {@code
 * carrierBookingReference} and {@code transportDocumentReference} are matched against the document
 * references of {@code shipmentDetails}, and {@code equipmentReference} against {@code
 * equipmentDetails}. Consequently a filter that an event type cannot carry never matches, which is
 * why each per-event-type scenario only offers the filters applicable to it (see {@code
 * TntEventType#applicableBaseFilters()}).
 */
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
    return matchesDocumentReference(event, BOOKING_DOCUMENT_TYPE, carrierBookingReference);
  }

  private static boolean matchesTransportDocumentReference(
    JsonNode event, String transportDocumentReference) {
    return matchesDocumentReference(event, TRANSPORT_DOCUMENT_TYPE, transportDocumentReference);
  }
  
  private static boolean matchesDocumentReference(
    JsonNode event, String documentReferenceType, String requestedReference) {
    if (requestedReference == null || requestedReference.isBlank()) {
      return true;
    }

    return collectDocumentReferences(event.path(SHIPMENT_DETAILS), documentReferenceType)
      .contains(requestedReference);
  }

  private static List<String> collectDocumentReferences(
    JsonNode shipmentDetails, String documentReferenceType) {
    List<String> references = new ArrayList<>();
    addDocumentReferenceIfOfType(
      references, shipmentDetails.path(DOCUMENT_REFERENCE), documentReferenceType);
    shipmentDetails
      .path(ADDITIONAL_DOCUMENT_REFERENCES)
      .forEach(docRef -> addDocumentReferenceIfOfType(references, docRef, documentReferenceType));
    return references;
  }

  private static void addDocumentReferenceIfOfType(
    List<String> references, JsonNode documentReference, String documentReferenceType) {
    if (documentReferenceType.equals(documentReference.path(DOCUMENT_REFERENCE_TYPE).asText(""))) {
      String value = documentReference.path(DOCUMENT_REFERENCE_VALUE).asText("");
      if (!value.isBlank()) {
        references.add(value);
      }
    }
  }

  private static boolean matchesEquipmentReference(JsonNode event, String equipmentReference) {
    if (equipmentReference == null || equipmentReference.isBlank()) {
      return true;
    }
    return equipmentReference.equals(
      event.path(EQUIPMENT_DETAILS).path(EQUIPMENT_REFERENCE).asText(""));
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
    return maxDateTime == null || !eventUpdatedDateTime.isAfter(maxDateTime);
  }
}

