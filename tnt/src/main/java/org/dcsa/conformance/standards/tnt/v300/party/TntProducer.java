package org.dcsa.conformance.standards.tnt.v300.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.dcsa.conformance.standards.tnt.TntStandard;
import org.dcsa.conformance.standards.tnt.v300.action.ProducerPostEventsAction;
import org.dcsa.conformance.standards.tnt.v300.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.tnt.v300.action.TntEventType;

@Slf4j
public class TntProducer extends ConformanceParty {

  public TntProducer(
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
    // no state to export
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    // no state to import
  }

  @Override
  protected void doReset() {
    // no state to reset
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(SupplyScenarioParametersAction.class, this::supplyScenarioParameters),
        Map.entry(ProducerPostEventsAction.class, this::sendTntEvents));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info(
        "{}.supplyScenarioParameters({})",
        getClass().getSimpleName(),
        actionPrompt.toPrettyString());

    JsonNode queryParametersNode = actionPrompt.required(TntConstants.TNT_QUERY_PARAMETERS);
    Set<TntQueryParameters> queryParameters =
        StreamSupport.stream(queryParametersNode.spliterator(), false)
            .map(JsonNode::asText)
            .map(TntQueryParameters::fromParameterName)
            .collect(Collectors.toSet());

    ObjectNode ssp = SupplyScenarioParametersAction.examplePrompt(queryParameters);
    asyncOrchestratorPostPartyInput(actionPrompt.required(TntConstants.ACTION_ID).asText(), ssp);

    addOperatorLogEntry("Supplying scenario parameters: %s".formatted(ssp.toPrettyString()));
  }

  private void sendTntEvents(JsonNode actionPrompt) {
    log.info("{}.sendTntEvents({})", getClass().getSimpleName(), actionPrompt.toPrettyString());

    var eventType = TntEventType.valueOf(actionPrompt.required(TntConstants.EVENT_TYPE).asText());
    String filePath = getTntEventPayloadFilepath(eventType);

    JsonNode jsonRequestBody = JsonToolkit.templateFileToJsonNode(filePath, Map.ofEntries());
    syncCounterpartPost(TntStandard.API_PATH, jsonRequestBody);

    addOperatorLogEntry("Sent TnT Events %s".formatted(jsonRequestBody.toPrettyString()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("{}.handleRequest({})", getClass().getSimpleName(), request.toString());

    Map<String, List<String>> initialIMap = Map.of(API_VERSION, List.of(apiVersion));
    Map<String, Collection<String>> headers = new HashMap<>(initialIMap);
    if (request.queryParams().containsKey(TntQueryParameters.LIMIT.getParameterName())
        && !request.queryParams().containsKey(TntQueryParameters.CURSOR.getParameterName())) {
      String cursor = "fE9mZnNldHw9MTAmbGltaXQ9MTA"; // example value for a cursor
      headers.put(TntConstants.HEADER_CURSOR_NAME, List.of(cursor));
    }

    boolean hasCursor =
        request.queryParams().containsKey(TntQueryParameters.CURSOR.getParameterName());

    Collection<String> eventTypesValues =
        request.queryParams().get(TntQueryParameters.ET.getParameterName());
    List<TntEventType> eventTypes =
        eventTypesValues == null
            ? Collections.emptyList()
            : eventTypesValues.stream().findFirst().map(str -> str.split(",")).stream()
                .flatMap(Arrays::stream)
                .map(String::trim)
                .map(TntEventType::valueOf)
                .toList();

    String filePath = getTntEventResponseFilepath(hasCursor);
    JsonNode responseObject = JsonToolkit.templateFileToJsonNode(filePath, Map.ofEntries());

    if (!eventTypes.isEmpty() && responseObject.has(TntConstants.EVENTS)) {
      JsonNode filteredResponse = filterEventsByType(responseObject, eventTypes);
      return request.createResponse(200, headers, new ConformanceMessageBody(filteredResponse));
    }

    return request.createResponse(200, headers, new ConformanceMessageBody(responseObject));
  }

  private String getTntEventPayloadFilepath(TntEventType eventType) {
    return "/standards/tnt/messages/" + eventType.tntEventPayload(getFormattedVersion());
  }

  private JsonNode filterEventsByType(JsonNode responseObject, List<TntEventType> eventTypes) {
    ObjectNode filteredResponse = responseObject.deepCopy();
    JsonNode eventsArray = filteredResponse.get(TntConstants.EVENTS);

    if (eventsArray != null && eventsArray.isArray()) {
      var filteredEvents =
          StreamSupport.stream(eventsArray.spliterator(), false)
              .filter(
                  event -> {
                    JsonNode eventClassification = event.path(TntConstants.EVENT_CLASSIFICATION);
                    if (eventClassification.isMissingNode()) {
                      return false;
                    }
                    String eventTypeCode = eventClassification.path(TntConstants.EVENT_TYPE_CODE).asText();
                    return eventTypes.stream().anyMatch(type -> type.name().equals(eventTypeCode));
                  })
              .toList();

      filteredResponse.putArray(TntConstants.EVENTS).addAll(filteredEvents);
    }

    return filteredResponse;
  }

  private String getTntEventResponseFilepath(boolean hasCursor) {
    if (hasCursor) {
      return "/standards/tnt/messages/tnt-%s-response-nextpage.json"
          .formatted(getFormattedVersion());
    }
    return "/standards/tnt/messages/tnt-%s-response.json".formatted(getFormattedVersion());
  }

  private String getFormattedVersion() {
    return apiVersion.toLowerCase().replaceAll("[.-]", "");
  }
}
