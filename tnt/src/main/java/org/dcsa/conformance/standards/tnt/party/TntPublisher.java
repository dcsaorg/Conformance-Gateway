package org.dcsa.conformance.standards.tnt.party;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.tnt.action.SupplyScenarioParametersAction;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.tnt.party.CustomJsonPointer.traverse;

@Slf4j
public class TntPublisher extends ConformanceParty {

  public TntPublisher(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      PartyWebClient webClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        persistentMap,
        webClient,
        orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {}

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {}

  @Override
  protected void doReset() {}






  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(SupplyScenarioParametersAction.class, this::supplyScenarioParameters));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info("TntPublisher.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));

    SuppliedScenarioParameters responseSsp =
        SuppliedScenarioParameters.fromMap(
            StreamSupport.stream(
                    actionPrompt.required("tntFilterParametersQueryParamNames").spliterator(),
                    false)
                .map(
                    jsonTntFilterParameter ->
                        TntFilterParameter.byQueryParamName.get(jsonTntFilterParameter.asText()))
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        tntFilterParameter ->
                            switch (tntFilterParameter) {
                              case EVENT_TYPE -> "SHIPMENT";
                              case SHIPMENT_EVENT_TYPE_CODE ->
                                  "DRFT";
                              case DOCUMENT_TYPE_CODE ->
                                  "CBR";
                              case CARRIER_BOOKING_REFERENCE -> "ABC123123123";
                              case TRANSPORT_DOCUMENT_REFERENCE ->
                                  "reserved-HHL123";
                              case TRANSPORT_EVENT_TYPE_CODE -> "ARRI";
                              case TRANSPORT_CALL_ID -> UUID.randomUUID().toString();
                              case VESSEL_IMO_NUMBER -> "9321483";
                              case EXPORT_VOYAGE_NUMBER -> "2103S";
                              case CARRIER_SERVICE_CODE -> "FE1";
                              case UN_LOCATION_CODE -> "FRPAR";
                              case EQUIPMENT_EVENT_TYPE_CODE ->
                                  "LOAD";
                              case EQUIPMENT_REFERENCE -> "APZU4812090";
                              case EVENT_CREATED_DATE_TIME,
                                      EVENT_CREATED_DATE_TIME_GTE,
                                      EVENT_CREATED_DATE_TIME_GT,
                                      EVENT_CREATED_DATE_TIME_LTE,
                                      EVENT_CREATED_DATE_TIME_LT,
                                      EVENT_CREATED_DATE_TIME_EQ ->
                                  ZonedDateTime.now()
                                      .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                              case LIMIT -> "100";
                            })));

    asyncOrchestratorPostPartyInput(
        actionPrompt.required("actionId").asText(), responseSsp.toJson());

    addOperatorLogEntry(
        "Submitting SuppliedScenarioParameters: %s"
            .formatted(responseSsp.toJson().toPrettyString()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("TntPublisher.handleRequest(%s)".formatted(request));

    ObjectMapper mapper = new ObjectMapper();
    ArrayNode filteredArray = mapper.createArrayNode();

    ArrayNode jsonResponseBody = (ArrayNode) JsonToolkit.templateFileToJsonNode(
        "/standards/tnt/messages/tnt-220-response.json", Map.ofEntries());

    if (!new EventTypeSpecificParamRule().validate(request.queryParams())) {
      throw new IllegalArgumentException("Error: Invalid query parameters provided.");
    }

    Map<String, List<AttributeMapping>> attributeMappings = new HashMap<>();
    /*attributeMappings.put("shipmentEventTypeCode", List.of(new AttributeMapping( JsonPointer.compile("/shipmentEventTypeCode"),
      (node, value) -> !node.isMissingNode() && node.asText().equals(value), Set.of("RECE", "DRFT", "PENA", "PENU", "REJE", "APPR", "ISSU", "SURR", "SUBM", "VOID", "CONF", "REQS", "CMPL", "HOLD", "RELS"))));
    attributeMappings.put("documentTypeCode", List.of(new AttributeMapping(JsonPointer.compile("/documentTypeCode"),
      (node, value) -> !node.isMissingNode() && node.asText().equals(value), Set.of("CBR", "BKG", "SHI", "SRM", "TRD", "ARN", "VGM", "CAS", "CUS", "DGD", "OOG"))));
    attributeMappings.put("equipmentTypeCode", List.of(new AttributeMapping(JsonPointer.compile("/equipmentEventTypeCode"),
      (node, value) -> !node.isMissingNode() && node.asText().equals(value), Set.of("LOAD", "DISC", "GTIN", "GTOT", "STUF", "STRP", "PICK", "DROP", "INSP", "RSEA", "RMVD"))));
    attributeMappings.put("eventType", List.of(new AttributeMapping(JsonPointer.compile("/eventType"), (node, value) -> !node.isMissingNode() && node.asText().equals(value), Set.of("SHIPMENT", "TRANSPORT", "EQUIPMENT"))));
    attributeMappings.put("transportEventTypeCode", List.of(new AttributeMapping(JsonPointer.compile("/transportEventTypeCode"), (node, value) -> !node.isMissingNode() && node.asText().equals(value), Set.of())));
    attributeMappings.put("transportCallID", List.of(new AttributeMapping(JsonPointer.compile("/transportCall/transportCallID"), (node, value) -> !node.isMissingNode() && node.asText().equals(value), Set.of())));
    attributeMappings.put("vesselIMONumber", List.of(new AttributeMapping(JsonPointer.compile("/transportCall/vessel/vesselIMONumber"), (node, value) -> !node.isMissingNode() && node.asText().equals(value), Set.of())));
    attributeMappings.put("exportVoyageNumber", List.of(new AttributeMapping(JsonPointer.compile("/transportCall/exportVoyageNumber"), (node, value) -> !node.isMissingNode() && node.asText().equals(value), Set.of())));
    attributeMappings.put("carrierServiceCode", List.of(new AttributeMapping(JsonPointer.compile("/transportCall/carrierServiceCode"), (node, value) -> !node.isMissingNode() && node.asText().equals(value), Set.of())));
    attributeMappings.put("UNLocationCode", List.of(new AttributeMapping(JsonPointer.compile("/transportCall/location/UNLocationCode"), (node, value) -> !node.isMissingNode() && node.asText().equals(value), Set.of())));
    attributeMappings.put("eventCreatedDateTime", List.of(new AttributeMapping(JsonPointer.compile("/eventCreatedDateTime"),
      (node, value) -> !node.isMissingNode() && OffsetDateTime.parse(node.asText()).isEqual(OffsetDateTime.parse(value)), Set.of())));
    attributeMappings.put("eventCreatedDateTime:gte", List.of(new AttributeMapping(JsonPointer.compile("/eventCreatedDateTime"), (node, value) -> !node.isMissingNode()
      && (OffsetDateTime.parse(node.asText()).isAfter(OffsetDateTime.parse(value)) || OffsetDateTime.parse(node.asText()).isEqual(OffsetDateTime.parse(value))), Set.of())));
    attributeMappings.put("eventCreatedDateTime:gt", List.of(new AttributeMapping(JsonPointer.compile("/eventCreatedDateTime"), (node, value) -> !node.isMissingNode()
      && (OffsetDateTime.parse(node.asText()).isAfter(OffsetDateTime.parse(value))), Set.of())));
    attributeMappings.put("eventCreatedDateTime:lt", List.of(new AttributeMapping(JsonPointer.compile("/eventCreatedDateTime"), (node, value) -> !node.isMissingNode()
      &&  (OffsetDateTime.parse(node.asText()).isBefore(OffsetDateTime.parse(value))), Set.of())));
    attributeMappings.put("eventCreatedDateTime:lte", List.of(new AttributeMapping(JsonPointer.compile("/eventCreatedDateTime"), (node, value) -> !node.isMissingNode()
      && (OffsetDateTime.parse(node.asText()).isBefore(OffsetDateTime.parse(value)) || OffsetDateTime.parse(node.asText()).isEqual(OffsetDateTime.parse(value))), Set.of())));
    attributeMappings.put("eventCreatedDateTime:eq", List.of(new AttributeMapping(JsonPointer.compile("/eventCreatedDateTime"), (node, value) -> !node.isMissingNode()
      && OffsetDateTime.parse(node.asText()).isEqual(OffsetDateTime.parse(value)), Set.of())));*/
    attributeMappings.put("carrierBookingReference", List.of(new AttributeMapping("documentReferences/*", (node, value) -> !node.isMissingNode()
      && node.path("documentReferenceType").asText().equals("BKG") && node.path("documentReferenceValue").asText().equals(value), Set.of())));
    attributeMappings.put("transportDocumentID", List.of(new AttributeMapping("transportDocumentID", (node, value) -> !node.isMissingNode()
      && node.path("documentTypeCode").asText().equals("TRD") && node.path("documentID").asText().equals(value) , Set.of())));

    Set<String> eventIds = new HashSet<>();

    for (Map.Entry<String, ? extends Collection<String>> queryParam : request.queryParams().entrySet()) {
      String paramName = queryParam.getKey();
      Collection<String> paramValues = queryParam.getValue();


      List<AttributeMapping> mappings = attributeMappings.get(paramName);
      if (mappings != null) {
        for (AttributeMapping mapping : mappings) {
          for (String paramValue : paramValues) {
            String jsonPath = mapping.getJsonPath();
            BiPredicate<JsonNode,String> condition = mapping.getCondition();
            for (JsonNode node : jsonResponseBody) {
              List<JsonNode> results = new ArrayList<>();
              traverse(node, jsonPath.split("/"), 0, results, condition, paramValue);
              if(!results.isEmpty()){
              var eventId = node.at("/eventID").asText();
              if ( !eventIds.contains(eventId) &&
                (mapping.getValues().isEmpty() || mapping.getValues().contains(node.asText()))) {
                  eventIds.add(eventId);
                  filteredArray.add(node);
                }
              }
            }
          }
        }
      }
    }
    //TODO:: To handle the complex sorting logic like CarrierBookingReference where referecences are different base on the jsonPath.
    if (request.queryParams().containsKey("sort")) {
      Collection<String> sortingFields= request.queryParams().get("sort");
      List<SortCriteria> sortCriteria = sortingFields.stream()
        .map(sortField -> {
          String[] parts = sortField.split(":");
          return new SortCriteria(parts[0], attributeMappings.get(parts[0]),
            parts.length > 1 ? SortDirection.valueOf(parts[1].toUpperCase()) : SortDirection.ASCENDING);
        })
        .collect(Collectors.toList());
      filteredArray = sortJsonArray(filteredArray, sortCriteria);

    }
    // Sort the filtered array by user provided value in parameters.
    ArrayNode limitedArray = OBJECT_MAPPER.createArrayNode();
    if (request.queryParams().containsKey("limit"))  {
      int limit = Integer.parseInt(request.queryParams().get("limit").iterator().next());
      if (filteredArray.size() > limit) {
        StreamSupport.stream(filteredArray.spliterator(), false)
          .limit(limit)
          .forEach(limitedArray::add);
      }
    }
    return request.createResponse(
        200,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(limitedArray));
  }

  public static ArrayNode sortJsonArray(ArrayNode jsonArray, List<SortCriteria> criteria) {
    if (jsonArray == null || criteria == null || criteria.isEmpty()) {
      return jsonArray; // No sorting needed
    }

    List<JsonNode> jsonNodeList = new ArrayList<>();
    jsonArray.forEach(jsonNodeList::add);

    jsonNodeList.sort((node1, node2) -> {
      int comparisonResult = 0;
      for (SortCriteria criterion : criteria) {
        comparisonResult = compareNodesByField(node1, node2, criterion.getField(), criterion.getDirection());
        if (comparisonResult != 0) {
          break; // Stop comparing if a difference is found
        }
      }
      return comparisonResult;
    });

    ArrayNode sortedArray = jsonArray.arrayNode();
    jsonNodeList.forEach(sortedArray::add);
    return sortedArray;
  }

  private static int compareNodesByField(JsonNode node1, JsonNode node2, String field, SortDirection direction) {
    // Handle nested fields using recursion
    String[] fieldParts = field.split("/", 2);
    JsonNode value1 = node1.path(fieldParts[0]);
    JsonNode value2 = node2.path(fieldParts[0]);

    if (fieldParts.length > 1 && value1.isObject() && value2.isObject()) {
      return compareNodesByField(value1, value2, fieldParts[1], direction);
    }

    // Handle different data types
    if (value1.isTextual() && value2.isTextual()) {
      return direction == SortDirection.ASCENDING ?
        value1.asText().compareTo(value2.asText()) :
        value2.asText().compareTo(value1.asText());
    } else if (value1.isNumber() && value2.isNumber()) {
      return direction == SortDirection.ASCENDING ?
        Double.compare(value1.asDouble(), value2.asDouble()) :
        Double.compare(value2.asDouble(), value1.asDouble());
    } else {
      // Add more type handling as needed (e.g., boolean, dates)
      return 0; // Consider how to handle incomparable types
    }
  }


}
