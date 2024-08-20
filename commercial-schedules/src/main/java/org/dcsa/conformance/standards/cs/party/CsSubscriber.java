package org.dcsa.conformance.standards.cs.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.cs.action.CsGetPortSchedulesAction;
import org.dcsa.conformance.standards.cs.action.CsGetRoutingsAction;
import org.dcsa.conformance.standards.cs.action.CsGetVesselSchedulesAction;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class CsSubscriber extends ConformanceParty {
  public CsSubscriber(String apiVersion, PartyConfiguration partyConfiguration, CounterpartConfiguration counterpartConfiguration, JsonNodeMap persistentMap, PartyWebClient webClient, Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(apiVersion, partyConfiguration, counterpartConfiguration, persistentMap, webClient, orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {

  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {

  }

  @Override
  protected void doReset() {

  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(Map.entry(CsGetVesselSchedulesAction.class, this::getVesselSchedules),
      Map.entry(CsGetPortSchedulesAction.class, this::getPortSchedules),
      Map.entry(CsGetRoutingsAction.class, this::getPointToPointRoutings));
  }

  private void getVesselSchedules(JsonNode actionPrompt) {
    log.info("CsSubscriber.getVesselSchedules(%s)".formatted(actionPrompt.toPrettyString()));
    SuppliedScenarioParameters ssp =
      SuppliedScenarioParameters.fromJson(actionPrompt.get("suppliedScenarioParameters"));

    syncCounterpartGet(
      "/v1/vessel-schedules",
      ssp.getMap().entrySet().stream()
        .collect(
          Collectors.toMap(
            entry -> entry.getKey().getQueryParamName(),
            entry -> Set.of(entry.getValue()))));

    addOperatorLogEntry(
      "Sent GET vessel schedules request with parameters %s".formatted(ssp.toJson().toPrettyString()));
  }

  private void getPortSchedules(JsonNode actionPrompt) {
    log.info("CsSubscriber.getPortSchedules(%s)".formatted(actionPrompt.toPrettyString()));
    SuppliedScenarioParameters ssp =
      SuppliedScenarioParameters.fromJson(actionPrompt.get("suppliedScenarioParameters"));

    syncCounterpartGet(
      "/v1/port-schedules",
      ssp.getMap().entrySet().stream()
        .collect(
          Collectors.toMap(
            entry -> entry.getKey().getQueryParamName(),
            entry -> Set.of(entry.getValue()))));

    addOperatorLogEntry(
      "Sent GET port schedules request with parameters %s".formatted(ssp.toJson().toPrettyString()));
  }

  private void getPointToPointRoutings(JsonNode actionPrompt) {
    log.info("CsSubscriber.getPointToPointRoutings(%s)".formatted(actionPrompt.toPrettyString()));
    SuppliedScenarioParameters ssp =
      SuppliedScenarioParameters.fromJson(actionPrompt.get("suppliedScenarioParameters"));
    
    syncCounterpartGet(
      "/v1/point-to-point-routes",
      ssp.getMap().entrySet().stream()
        .collect(
          Collectors.toMap(
            entry -> entry.getKey().getQueryParamName(),
            entry -> Set.of(entry.getValue()))));

    addOperatorLogEntry(
      "Sent GET point to point routings request with parameters %s".formatted(ssp.toJson().toPrettyString()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("CsSubscriber.handleRequest(%s)".formatted(request));
    throw new UnsupportedOperationException();
  }
}
