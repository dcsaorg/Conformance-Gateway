package org.dcsa.conformance.gateway.parties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.dcsa.conformance.gateway.scenarios.ConformanceScenario;
import org.dcsa.conformance.gateway.scenarios.ScenarioListBuilder;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SupplyAvailableTdrAction;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;
import org.springframework.test.web.reactive.server.WebTestClient;

@Slf4j
public class ConformanceOrchestrator {
  protected final ScenarioListBuilder scenarioListBuilder;
  protected final List<ConformanceScenario> scenarios = new ArrayList<>();

  public ConformanceOrchestrator(ScenarioListBuilder scenarioListBuilder) {
    this.scenarioListBuilder = scenarioListBuilder;
  }

  public void reset() {
    initializeScenarios();
    notifyAllPartiesOfNextActions();
  }

  private void notifyAllPartiesOfNextActions() {
    scenarios.stream()
        .map(ConformanceScenario::peekNextAction)
        .filter(Objects::nonNull)
        .map(ConformanceAction::getSourcePartyName)
        .collect(Collectors.toSet())
        .forEach(this::asyncNotifyParty);
  }

  protected void initializeScenarios() {
    scenarios.clear();
    scenarios.addAll(scenarioListBuilder.buildList());
  }

  private void asyncNotifyParty(String partyName) {
    CompletableFuture.runAsync(
            () -> {
              log.info("ConformanceOrchestrator.asyncNotifyParty(%s)".formatted(partyName));
              WebTestClient.bindToServer()
                  .baseUrl("http://localhost:8080") // FIXME use config / deployment variable URL
                  .build()
                  .get()
                  .uri("/party/%s/notify".formatted(partyName))
                  .exchange()
                  .expectStatus()
                  .is2xxSuccessful();
            })
        .exceptionally(
            e -> {
              log.error("Failed to notify party '%s': %s".formatted(partyName, e), e);
              return null;
            });
  }

  public JsonNode handleGetPartyPrompt(String partyName) {
    ArrayNode jsonNodes =
        new ObjectMapper()
            .createArrayNode()
            .addAll(
                scenarios.stream()
                    .map(ConformanceScenario::peekNextAction)
                    .filter(Objects::nonNull)
                    .filter(action -> Objects.equals(action.getSourcePartyName(), partyName))
                    .map(ConformanceAction::asJsonNode)
                    .collect(Collectors.toList()));
    log.info(
        "ConformanceOrchestrator.getPartyPrompt(%s) fetched: %s"
            .formatted(partyName, jsonNodes.toPrettyString()));
    return jsonNodes;
  }

  public JsonNode handlePartyInput(JsonNode partyInput) {
    log.info("ConformanceOrchestrator.handlePartyInput(%s)".formatted(partyInput.toPrettyString()));
    String actionId = partyInput.get("actionId").asText();
    ConformanceAction action =
        scenarios.stream()
            .filter(
                scenario ->
                    scenario.hasNextAction()
                        && Objects.equals(actionId, scenario.peekNextAction().getId().toString()))
            .map(ConformanceScenario::popNextAction)
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Input for already handled(?) actionId %s: %s"
                            .formatted(actionId, partyInput.toPrettyString())));
    if (action instanceof SupplyAvailableTdrAction supplyAvailableTdrAction) {
      supplyAvailableTdrAction.getTdrConsumer().accept(partyInput.get("tdr").asText());
    } else {
      throw new UnsupportedOperationException(partyInput.toString());
    }
    notifyAllPartiesOfNextActions();
    return new ObjectMapper().createObjectNode();
  }

  public void handlePartyTrafficExchange(ConformanceExchange conformanceExchange) {
    ConformanceAction action =
        scenarios.stream()
            .filter(
                scenario ->
                    scenario.hasNextAction()
                        && scenario.peekNextAction().updateFromExchangeIfItMatches(conformanceExchange))
            .map(ConformanceScenario::popNextAction)
            .findFirst()
            .orElse(null);
    if (action == null) {
      log.info(
          "Ignoring conformance exchange not matched by any pending actions: %s"
              .formatted(conformanceExchange));
      return;
    }
    notifyAllPartiesOfNextActions();
  }
}
