package org.dcsa.conformance.standards.tnt.v300.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.tnt.TntStandard;
import org.dcsa.conformance.standards.tnt.v300.action.ConsumerGetEventsWithQueryParametersAction;
import org.dcsa.conformance.standards.tnt.v300.action.ConsumerGetEventsWithTypeAction;
import org.dcsa.conformance.standards.tnt.v300.action.TntEventType;

@Slf4j
public class TntConsumer extends ConformanceParty {

  public TntConsumer(
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
        Map.entry(ConsumerGetEventsWithTypeAction.class, this::getTntEvents),
        Map.entry(
            ConsumerGetEventsWithQueryParametersAction.class,
            this::getTntEventsWithQueryParameters));
  }

  private void getTntEvents(JsonNode actionPrompt) {
    log.info("{}.getTntEvents({})", getClass().getSimpleName(), actionPrompt.toPrettyString());

    var eventType = TntEventType.valueOf(actionPrompt.required(TntConstants.EVENT_TYPE).asText());

    syncCounterpartGet(TntStandard.API_PATH, Map.of(TntQueryParameters.ET.getParameterName(), List.of(eventType.toString())));

    addOperatorLogEntry("Sent GET TNT Events request with event type %s".formatted(eventType));
  }

  private void getTntEventsWithQueryParameters(JsonNode actionPrompt) {
    log.info(
        "{}.getTntEventsWithQueryParameters({})",
        getClass().getSimpleName(),
        actionPrompt.toPrettyString());

    SuppliedScenarioParameters ssp =
        SuppliedScenarioParameters.fromJson(actionPrompt.get(TntConstants.SUPPLIED_SCENARIO_PARAMETERS));

    Map<String, Collection<String>> queryParams =
        ssp.getMap().entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> entry.getKey().getParameterName(), entry -> Set.of(entry.getValue())));

    if (actionPrompt.hasNonNull(TntQueryParameters.CURSOR.getParameterName())) {
      queryParams.put(
          TntQueryParameters.CURSOR.getParameterName(),
          List.of(actionPrompt.path(TntQueryParameters.CURSOR.getParameterName()).asText()));
    }

    syncCounterpartGet(TntStandard.API_PATH, queryParams);

    addOperatorLogEntry("Sent GET events request with parameters %s".formatted(queryParams));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    ObjectNode responseNode = OBJECT_MAPPER.createObjectNode();
    responseNode.putArray(TntConstants.FEEDBACK_ELEMENTS);

    ConformanceResponse response =
        request.createResponse(
            200,
            Map.of(API_VERSION, List.of(apiVersion)),
            new ConformanceMessageBody(responseNode));

    addOperatorLogEntry(
        "Handled lightweight notification: %s".formatted(request.message().body().getJsonBody()));
    return response;
  }
}
