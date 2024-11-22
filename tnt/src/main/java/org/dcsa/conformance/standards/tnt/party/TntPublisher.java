package org.dcsa.conformance.standards.tnt.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
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

import static org.dcsa.conformance.core.party.CustomJsonPointer.traverse;
import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

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
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    //no state to export
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    //no state to import
  }

  @Override
  protected void doReset() {
    //no state to reset
  }

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
            actionPrompt.required("tntFilterParametersQueryParam").spliterator(),
            false)
          .collect(
            Collectors.toMap(
              jsonTntFilterParameter -> TntFilterParameter.byQueryParamName.get(jsonTntFilterParameter.get("parameter").asText()),
              jsonTntFilterParameter -> jsonTntFilterParameter.get("value").asText(),
              (oldValue, newValue) -> oldValue, // merge function to handle duplicate keys
              LinkedHashMap::new // supplier to create a LinkedHashMap
            )));


    asyncOrchestratorPostPartyInput(
      actionPrompt.required("actionId").asText(), responseSsp.toJson());

    addOperatorLogEntry(
      "Submitting SuppliedScenarioParameters: %s"
        .formatted(responseSsp.toJson().toPrettyString()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("TntPublisher.handleRequest(%s)".formatted(request));
    Map<String, Collection<String>> headers = new HashMap<>(Map.of(API_VERSION, List.of(apiVersion)));

    Map<String, List<AttributeMapping>> attributeMappings = AttributeMapping.initializeAttributeMappings();

    JsonNode jsonResponseBody = JsonToolkit.templateFileToJsonNode(
      "/standards/tnt/messages/tnt-220-response.json", Map.ofEntries());

    Set<String> issues = new LinkedHashSet<>();
    ArrayList<JsonNode> eventNodes = TntHelper.findEventNodes(jsonResponseBody, issues);

    if (!new QueryParameterSpecificRule().validate(request.queryParams())) {
      return return400(request, "Error: Invalid query parameters provided.");
    }
    ArrayNode filteredArray = OBJECT_MAPPER.createArrayNode();
    eventNodes.forEach(filteredArray::add);

    // Chained Filtering Logic
    for (Map.Entry<String, ? extends Collection<String>> queryParam : request.queryParams().entrySet()) {
      String paramName = queryParam.getKey();
      Collection<String> paramValues = queryParam.getValue().stream()
        .flatMap(value -> Arrays.stream(value.split(",")))
        .collect(Collectors.toList());
      List<AttributeMapping> mappings = attributeMappings.get(paramName);
      Set<String> seenEventIds = new HashSet<>();
      if (mappings != null) {
        filteredArray = applyFilter(filteredArray, mappings, paramValues,seenEventIds);
      }
    }

    if (request.queryParams().containsKey("sort")) {
      Collection<String> sortingFields = request.queryParams().get("sort");
      List<SortCriteria> sortCriteria = sortingFields.stream()
        .map(sortField -> {
          String[] parts = sortField.split(":");
          return new SortCriteria(parts[0], attributeMappings.get(parts[0]),
            parts.length > 1 ? SortDirection.valueOf(parts[1].toUpperCase()) : SortDirection.ASC);
        })
        .collect(Collectors.toList());
      filteredArray = sortJsonArray(filteredArray, sortCriteria);

    }
    int limit = Integer.parseInt(request.queryParams().containsKey("limit") ?
      request.queryParams().get("limit").iterator().next() : "100");
    String cursor = request.queryParams()
      .containsKey("cursor") ? request.queryParams().get("cursor").iterator().next() : null;
    String cursorKey = "cursorKey";

    ArrayNode limitedArray = applyCursorLogic(filteredArray, cursor, cursorKey, limit, headers);

    return request.createResponse(
      200,
      headers,
      new ConformanceMessageBody(limitedArray));
  }

  private ArrayNode applyFilter(ArrayNode inputArray, List<AttributeMapping> mappings,
                                Collection<String> paramValues, Set<String> seenEventIds) {
    ArrayNode resultArray = OBJECT_MAPPER.createArrayNode();// Track seen eventIds within this filter

    mappings.forEach(mapping ->
      paramValues.forEach(paramValue ->
        StreamSupport.stream(inputArray.spliterator(), false)
          .forEach(node -> {
            String jsonPath = mapping.getJsonPath();
            BiPredicate<JsonNode, String> condition = mapping.getCondition();

            List<JsonNode> results = new ArrayList<>();
            traverse(node, jsonPath.split("/"), 0, results, condition, paramValue);

            if (!results.isEmpty()) {
              String eventId = node.at("/eventID").asText();
              if (!seenEventIds.contains(eventId) &&
                (mapping.getValues().isEmpty() ||
                  results.stream().anyMatch(
                    result -> mapping.getValues().contains(result.asText())))) {
                seenEventIds.add(eventId);
                resultArray.add(node);
              }
            }
          })
      )
    );
    return resultArray;
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
        comparisonResult = compareNodesByField(node1, node2, criterion.field(), criterion.direction());
        if (comparisonResult != 0) {
          return comparisonResult; // Stop comparing if a difference is found
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

  private ObjectNode createErrorResponse(String httpMethod, String requestUri, String reason, String message) {
    // Create the root object node
    ObjectNode rootNode = OBJECT_MAPPER.createObjectNode();
    rootNode.put("httpMethod", httpMethod);
    rootNode.put("requestUri", requestUri);

    // Create the errors array node
    ArrayNode errorsArray = OBJECT_MAPPER.createArrayNode();
    ObjectNode errorDetails = OBJECT_MAPPER.createObjectNode();
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
      return direction == SortDirection.ASC ?
        value1.asText().compareTo(value2.asText()) :
        value2.asText().compareTo(value1.asText());
    } else if (value1.isNumber() && value2.isNumber()) {
      return direction == SortDirection.ASC ?
        Double.compare(value1.asDouble(), value2.asDouble()) :
        Double.compare(value2.asDouble(), value1.asDouble());
    } else {
      // Add more type handling as needed (e.g., boolean, dates)
      return 0; // Consider how to handle incomparable types
    }
  }

  protected String generateCursor(ArrayNode events, int limit, String cursorKey) {
    if (events.size() <= limit) {
      persistentMap.save(cursorKey, OBJECT_MAPPER.createObjectNode());
      return null;
    }
    JsonNode lastEvent = events.get(limit - 1);
    String lastEventId = lastEvent.at("/eventID").asText();
    persistentMap.save(cursorKey, OBJECT_MAPPER.createObjectNode().put("cursor", lastEventId));
    return lastEventId;
  }

  private ArrayNode applyCursorLogic(ArrayNode filteredArray, String cursor, String cursorKey, int limit, Map<String, Collection<String>> headers) {
    // Retrieve cursor from persistentMap if available
    if (cursor != null) {
      JsonNode storedCursor = persistentMap.load(cursorKey);
      if (storedCursor != null) {
        cursor = storedCursor.path("cursor").asText();
      }
    }

    ArrayNode limitedArray = OBJECT_MAPPER.createArrayNode();
    int startIndex = 0; // Start from the beginning if no cursor

    // Apply cursor logic (skip events based on cursor)
    if (cursor != null) {
      for (int i = 0; i < filteredArray.size(); i++) {
        if (filteredArray.get(i).at("/eventID").asText().equals(cursor)) {
          startIndex = i + 1; // Start from the next event
          break;
        }
      }
    }

    // Limit results
    for (int i = startIndex; i < filteredArray.size() && limitedArray.size() < limit; i++) {
      limitedArray.add(filteredArray.get(i));
    }

    // Generate and store cursor for the next page
    String nextCursor = generateCursor(filteredArray, startIndex + limit, cursorKey);

    if (nextCursor != null) {
      headers.put("Next-Page-Cursor", List.of(nextCursor));
    }

    return limitedArray;
  }

}
