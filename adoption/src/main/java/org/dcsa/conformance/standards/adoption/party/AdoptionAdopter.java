package org.dcsa.conformance.standards.adoption.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.Consumer;
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
import org.dcsa.conformance.standards.adoption.AdoptionStandard;
import org.dcsa.conformance.standards.adoption.action.PutAdoptionStatsAction;
import org.dcsa.conformance.standards.adoption.action.SupplyScenarioParametersAction;

@Slf4j
public class AdoptionAdopter extends ConformanceParty {

  public static final String ACTION_ID = "actionId";

  public AdoptionAdopter(
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
    // no state
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    // no state
  }

  @Override
  protected void doReset() {
    // no state
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(SupplyScenarioParametersAction.class, this::getAdoptionStats),
        Map.entry(PutAdoptionStatsAction.class, this::putAdoptionStats));
  }

  private void getAdoptionStats(JsonNode actionPrompt) {
    log.info("AdoptionAdopter.getAdoptionStats({})", actionPrompt);

    asyncOrchestratorPostPartyInput(
        actionPrompt.required(ACTION_ID).asText(),
        OBJECT_MAPPER.createObjectNode().put("interval", "week").put("date", "2024-12-31"));
    addOperatorLogEntry(
        "Provided AdoptionScenarioParameters: %s".formatted(actionPrompt.get(ACTION_ID)));
  }

  private void putAdoptionStats(JsonNode actionPrompt) {
    log.info("AdoptionAdopter.putAdoptionStats()");

    String fileName = AdoptionStandard.ADOPTION_STATS_EXAMPLE.formatted(apiVersion);
    JsonNode jsonResponseBody = JsonToolkit.templateFileToJsonNode(fileName, null);
    syncCounterpartPut(PutAdoptionStatsAction.PUT_ADOPTION_STATS_URL, jsonResponseBody);

    addOperatorLogEntry("Provided response body, contents of file: %s".formatted(fileName));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    JsonNode jsonResponseBody =
        JsonToolkit.templateFileToJsonNode(
            AdoptionStandard.ADOPTION_STATS_EXAMPLE.formatted(apiVersion), Map.ofEntries());

    return request.createResponse(
        200,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(jsonResponseBody));
  }
}
