package org.dcsa.conformance.standards.ovs.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
import org.dcsa.conformance.standards.ovs.action.SupplyScenarioParametersAction;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.ovs.party.CustomJsonPointer.traverse;

@Slf4j
public class OvsPublisher extends ConformanceParty {

  public OvsPublisher(
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
    log.info("OvsPublisher.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));

    SuppliedScenarioParameters responseSsp =
      SuppliedScenarioParameters.fromMap(
        StreamSupport.stream(
            actionPrompt.required("ovsFilterParametersQueryParam").spliterator(),
            false)
          .collect(
            Collectors.toMap(
              jsonOvsFilterParameter ->
                OvsFilterParameter.byQueryParamName.get(jsonOvsFilterParameter.get("parameter").asText()),
              jsonOvsFilterParameter ->
                jsonOvsFilterParameter.get("value").asText(),
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
    log.info("OvsPublisher.handleRequest(%s)".formatted(request));
    Map<String, Collection<String>> headers = new HashMap<>(Map.of(API_VERSION, List.of(apiVersion)));

    Map<String, List<OvsAttributeMapping>> ovsAttributeMappings = OvsAttributeMapping.initializeAttributeMappings();

    JsonNode jsonResponseBody =
      JsonToolkit.templateFileToJsonNode(
        "/standards/ovs/messages/ovs-%s-response.json"
          .formatted(apiVersion.toLowerCase().replaceAll("[.-]", "")),
        Map.ofEntries());

    ArrayNode filteredArray = OBJECT_MAPPER.createArrayNode();
    jsonResponseBody.forEach(filteredArray::add);

    // Chained Filtering Logic
    for (Map.Entry<String, ? extends Collection<String>> queryParam : request.queryParams().entrySet()) {
      String paramName = queryParam.getKey();
      Collection<String> paramValues = queryParam.getValue().stream()
        .flatMap(value -> Arrays.stream(value.split(",")))
        .collect(Collectors.toList());

      List<OvsAttributeMapping> mappings = ovsAttributeMappings.get(paramName);
      if (mappings != null) {
        filteredArray = applyFilter(filteredArray, mappings, paramValues);
      }
    }

    int limit = Integer.parseInt(request.queryParams().containsKey("limit") ?
      request.queryParams().get("limit").iterator().next() : "100");
    if (filteredArray.size() > limit) {
      ArrayNode limitedArray = OBJECT_MAPPER.createArrayNode();
      for (int i = 0; i < limit; i++) {
        limitedArray.add(filteredArray.get(i));
      }
      filteredArray = limitedArray;
    }


    return request.createResponse(
        200,
        headers,
        new ConformanceMessageBody(filteredArray));
  }

  private ArrayNode applyFilter(ArrayNode inputArray, List<OvsAttributeMapping> mappings,
                                Collection<String> paramValues) {
    ArrayNode resultArray = OBJECT_MAPPER.createArrayNode();

    mappings.forEach(mapping ->
      paramValues.forEach(paramValue ->
        StreamSupport.stream(inputArray.spliterator(), false)
          .forEach(node -> {
            String jsonPath = mapping.jsonPath();
            BiPredicate<JsonNode, String> condition = mapping.condition();

            List<JsonNode> results = new ArrayList<>();
            traverse(node, jsonPath.split("/"), 0, results, condition, paramValue);

            if (!results.isEmpty() && (mapping.values().isEmpty() ||
                  results.stream().anyMatch(
                    result -> mapping.values().contains(result.asText())))) {
                resultArray.add(node);
              }

          })
      )
    );
    return resultArray;
  }
}
