package org.dcsa.conformance.standards.tnt.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.tnt.action.TntGetEventsAction;

@Slf4j
public class TntSubscriber extends ConformanceParty {

  public TntSubscriber(
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
    return Map.ofEntries(Map.entry(TntGetEventsAction.class, this::getEvents));
  }

  private void getEvents(JsonNode actionPrompt) {
    log.info("TntSubscriber.getEvents(%s)".formatted(actionPrompt.toPrettyString()));
    SuppliedScenarioParameters ssp =
        SuppliedScenarioParameters.fromJson(actionPrompt.get("suppliedScenarioParameters"));

    syncCounterpartGet(
        "/v2/events",
        ssp.getMap().entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> entry.getKey().getQueryParamName(),
                    entry -> Set.of(entry.getValue()))));

    addOperatorLogEntry(
        "Sent GET events request with parameters %s".formatted(ssp.toJson().toPrettyString()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("TntSubscriber.handleRequest(%s)".formatted(request));
    throw new UnsupportedOperationException();
  }
}
