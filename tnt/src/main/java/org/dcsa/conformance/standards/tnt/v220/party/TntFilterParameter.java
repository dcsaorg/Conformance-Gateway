package org.dcsa.conformance.standards.tnt.v220.party;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import org.dcsa.conformance.standards.tnt.v220.action.TntEventType;

@Getter
public enum  TntFilterParameter {
  EVENT_TYPE("eventType"),
  SHIPMENT_EVENT_TYPE_CODE("shipmentEventTypeCode", TntEventType.SHIPMENT),
  DOCUMENT_TYPE_CODE("documentTypeCode", TntEventType.SHIPMENT),
  CARRIER_BOOKING_REFERENCE(
      "carrierBookingReference",
      Map.ofEntries(
          Map.entry(
              TntEventType.TRANSPORT,
              Set.of(
                  "/documentReferences/*/documentReferenceValue WHERE /documentReferenceType=BKG")),
          Map.entry(TntEventType.SHIPMENT, Set.of("/documentID WHERE /documentTypeCode=BKG")),
          Map.entry(
              TntEventType.EQUIPMENT,
              Set.of(
                  "/documentReferences/*/documentReferenceValue WHERE /documentReferenceType=BKG")))),
  TRANSPORT_DOCUMENT_REFERENCE(
      "transportDocumentReference",
      Map.ofEntries(
          Map.entry(
              TntEventType.TRANSPORT,
              Set.of(
                  "/documentReferences/*/documentReferenceValue WHERE /documentReferenceType=TRD")),
          Map.entry(TntEventType.SHIPMENT, Set.of("/documentID WHERE /documentTypeCode=TRD")),
          Map.entry(
              TntEventType.EQUIPMENT,
              Set.of(
                  "/documentReferences/*/documentReferenceValue WHERE /documentReferenceType=TRD")))),
  TRANSPORT_EVENT_TYPE_CODE("transportEventTypeCode", TntEventType.TRANSPORT),
  TRANSPORT_CALL_ID(
      "transportCallID",
      Map.ofEntries(
          Map.entry(TntEventType.TRANSPORT, Set.of("/transportCall/transportCallID")),
          Map.entry(TntEventType.SHIPMENT, Set.of()),
          Map.entry(TntEventType.EQUIPMENT, Set.of("/transportCall/transportCallID")))),
  VESSEL_IMO_NUMBER(
      "vesselIMONumber",
      Map.ofEntries(
          Map.entry(TntEventType.TRANSPORT, Set.of("/transportCall/vessel/vesselIMONumber")),
          Map.entry(TntEventType.SHIPMENT, Set.of()),
          Map.entry(TntEventType.EQUIPMENT, Set.of("/transportCall/vessel/vesselIMONumber")))),
  EXPORT_VOYAGE_NUMBER(
      "exportVoyageNumber",
      Map.ofEntries(
          Map.entry(TntEventType.TRANSPORT, Set.of("/transportCall/exportVoyageNumber")),
          Map.entry(TntEventType.SHIPMENT, Set.of()),
          Map.entry(TntEventType.EQUIPMENT, Set.of("/transportCall/exportVoyageNumber")))),
  CARRIER_SERVICE_CODE(
      "carrierServiceCode",
      Map.ofEntries(
          Map.entry(TntEventType.TRANSPORT, Set.of("/transportCall/carrierServiceCode")),
          Map.entry(TntEventType.SHIPMENT, Set.of()),
          Map.entry(TntEventType.EQUIPMENT, Set.of("/transportCall/carrierServiceCode")))),
  UN_LOCATION_CODE(
      "UNLocationCode",
      Map.ofEntries(
          Map.entry(TntEventType.TRANSPORT, Set.of("/transportCall/UNLocationCode")),
          Map.entry(TntEventType.SHIPMENT, Set.of()),
          Map.entry(
              TntEventType.EQUIPMENT,
              Set.of("/transportCall/UNLocationCode", "/eventLocation/UNLocationCode")))),
  EQUIPMENT_EVENT_TYPE_CODE("equipmentEventTypeCode", TntEventType.EQUIPMENT),
  EQUIPMENT_REFERENCE(
      "equipmentReference",
      Map.ofEntries(
          Map.entry(
              TntEventType.TRANSPORT,
              Set.of("/references/*/referenceValue WHERE /referenceType=EQ")),
          Map.entry(
              TntEventType.SHIPMENT,
              Set.of("/references/*/referenceValue WHERE /referenceType=EQ")),
          Map.entry(
              TntEventType.EQUIPMENT,
              Set.of(
                  "/equipmentReference", "/references/*/referenceValue WHERE /referenceType=EQ")))),
  // these have no JSON paths declared here because they use custom validations instead
  EVENT_CREATED_DATE_TIME("eventCreatedDateTime", Map.of()),
  EVENT_CREATED_DATE_TIME_GTE("eventCreatedDateTime:gte", Map.of()),
  EVENT_CREATED_DATE_TIME_GT("eventCreatedDateTime:gt", Map.of()),
  EVENT_CREATED_DATE_TIME_LTE("eventCreatedDateTime:lte", Map.of()),
  EVENT_CREATED_DATE_TIME_LT("eventCreatedDateTime:lt", Map.of()),
  EVENT_CREATED_DATE_TIME_EQ("eventCreatedDateTime:eq", Map.of()),
  LIMIT("limit", Map.of()),
  ;

  public static final Map<String, TntFilterParameter> byQueryParamName =
      Arrays.stream(values())
          .collect(
              Collectors.toUnmodifiableMap(
                  TntFilterParameter::getQueryParamName, Function.identity()));

  private final String queryParamName;
  private final Map<TntEventType, Set<String>> jsonPathsByEventType;

  TntFilterParameter(String queryParamName) {
    this(queryParamName, "/" + queryParamName);
  }

  TntFilterParameter(String queryParamName, String jsonPath) {
    this(queryParamName, Set.of(jsonPath));
  }

  TntFilterParameter(String queryParamName, TntEventType eventType) {
    this(
        queryParamName,
        Arrays.stream(TntEventType.values())
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    (et) -> et.equals(eventType) ? Set.of("/" + queryParamName) : Set.of())));
  }

  TntFilterParameter(String queryParamName, Set<String> jsonPaths) {
    this(
        queryParamName,
        Arrays.stream(TntEventType.values())
            .collect(Collectors.toMap(Function.identity(), (eventType) -> jsonPaths)));
  }

  TntFilterParameter(String queryParamName, Map<TntEventType, Set<String>> jsonPathsByEventType) {
    this.queryParamName = queryParamName;
    this.jsonPathsByEventType = jsonPathsByEventType;
  }
}
