package org.dcsa.conformance.standards.tnt.party;

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
    boolean isBadRequest = actionPrompt.get("isBadRequest").asBoolean(false);

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
                              case SHIPMENT_EVENT_TYPE_CODE -> {
                                if (isBadRequest) {
                                  yield "INVALID_CODE";
                                } else {
                                  yield "DRFT";
                                }
                              }
                              case DOCUMENT_TYPE_CODE -> {
                                if (isBadRequest) {
                                  yield "INVALID_CODE";
                                } else {
                                  yield "CBR";
                                }
                              }
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
                                  "2021-01-09T14:12:56+01:00";
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

    Map<String, List<AttributeMapping>> attributeMappings = AttributeMapping.initializeAttributeMappings();

    ObjectMapper mapper = new ObjectMapper();
    ArrayNode filteredArray = mapper.createArrayNode();

    ArrayNode jsonResponseBody = (ArrayNode) JsonToolkit.templateFileToJsonNode(
        "/standards/tnt/messages/tnt-220-response.json", Map.ofEntries());

    if (!new QueryParameterSpecificRule().validate(request.queryParams())) {
      return return400(request,"Error: Invalid query parameters provided.");
    }

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
              if ( !eventIds.contains(eventId)
                && (mapping.getValues().isEmpty()
                  || results.stream().anyMatch(result -> mapping.getValues().contains(result.asText())))) {
                  eventIds.add(eventId);
                  filteredArray.add(node);
                }
              }
            }
          }
        }
      }
    }
    //TODO:: To handle the complex sorting logic like CarrierBookingReference where references are different base on the jsonPath.
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
        new ConformanceMessageBody(limitedArray.isEmpty() ? filteredArray : limitedArray ));
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

  private ConformanceResponse return400(ConformanceRequest request, String message) {
    return request.createResponse(
      400,
      Map.of(API_VERSION, List.of(apiVersion)),
      new ConformanceMessageBody(createErrorResponse(request.method(), request.url(), "Bad Request", message)));
  }

  private ConformanceResponse return404(ConformanceRequest request) {
    return return404(request, "Returning 404 since the request did not match any known URL");
  }

  private ConformanceResponse return404(ConformanceRequest request, String message) {
    return request.createResponse(
      404,
      Map.of(API_VERSION, List.of(apiVersion)),
      new ConformanceMessageBody(createErrorResponse(request.method(), request.url(), "Not Found", message)));
  }

  private ObjectNode createErrorResponse(String httpMethod, String requestUri, String reason, String message) {
    ObjectMapper mapper = new ObjectMapper();

    // Create the root object node
    ObjectNode rootNode = mapper.createObjectNode();
    rootNode.put("httpMethod", httpMethod);
    rootNode.put("requestUri", requestUri);

    // Create the errors array node
    ArrayNode errorsArray = mapper.createArrayNode();
    ObjectNode errorDetails = mapper.createObjectNode();
    errorDetails.put("reason", reason);
    errorDetails.put("message", message);
    errorsArray.add(errorDetails);

    // Add the errors array to the root node
    rootNode.set("errors", errorsArray);

    // Add other fields to the root node
    rootNode.put("statusCode", 400);
    rootNode.put("statusCodeText", "Bad Request");
    rootNode.put("errorDateTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

    return rootNode;
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
