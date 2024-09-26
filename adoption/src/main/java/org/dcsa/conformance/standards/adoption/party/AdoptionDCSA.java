package org.dcsa.conformance.standards.adoption.party;

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
import org.dcsa.conformance.standards.adoption.action.GetAdoptionStatsAction;

@Slf4j
public class AdoptionDCSA extends ConformanceParty {

  public AdoptionDCSA(
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
    return Map.ofEntries(Map.entry(GetAdoptionStatsAction.class, this::getAdoptionStats));
  }

  private void getAdoptionStats(JsonNode actionPrompt) {
    syncCounterpartGet(GetAdoptionStatsAction.GET_ADOPTION_STATS_URL, Collections.emptyMap());
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("AdoptionDCSA.handleRequest()");

    JsonNode jsonResponseBody =
        JsonToolkit.templateFileToJsonNode(
            AdoptionStandard.ADOPTION_STATS_EXAMPLE.formatted(apiVersion), null);

    return request.createResponse(
        204,
        Map.of("Api-Version", List.of(apiVersion)),
        new ConformanceMessageBody(jsonResponseBody));
  }
}
