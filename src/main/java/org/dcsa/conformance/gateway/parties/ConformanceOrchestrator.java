package org.dcsa.conformance.gateway.parties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.dcsa.conformance.gateway.scenarios.ConformanceScenario;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SupplyAvailableTdrAction;
import org.springframework.test.web.reactive.server.WebTestClient;

@Slf4j
public abstract class ConformanceOrchestrator {
  private final Map<String, Set<ConformanceAction>> notifiedActionsByPartyName = new HashMap<>();
  protected final List<ConformanceScenario> scenarios = new ArrayList<>();

  public void reset() {
    initializeScenarios();
    notifyAllPartiesOfNextActions();
  }

  protected abstract void initializeScenarios();

  private void notifyAllPartiesOfNextActions() {
    scenarios.stream()
        .map(ConformanceScenario::nextAction)
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(ConformanceAction::getSourcePartyName, Collectors.toSet()))
        .forEach(
            (partyName, nextActions) -> {
              Set<ConformanceAction> notifiedActions =
                  notifiedActionsByPartyName.computeIfAbsent(partyName, ignored -> new HashSet<>());
              HashSet<ConformanceAction> newActions = new HashSet<>(nextActions);
              newActions.removeAll(notifiedActions);
              notifiedActions.addAll(newActions);
              notifyPartyAsync(partyName);
            });
  }

  private void notifyPartyAsync(String partyName) {
    CompletableFuture.runAsync(
            () -> {
              log.info("DcsaConformanceGatewayApplication.notifyParty(%s)".formatted(partyName));
              WebTestClient.bindToServer()
                  .baseUrl("http://localhost:8080") // FIXME use config / deployment variable URL
                  .build()
                  .get()
                  .uri("/party/%s/notify".formatted(partyName))
                  .exchange()
                  .expectStatus()
                  .is2xxSuccessful();
              log.info(
                  "DcsaConformanceGatewayApplication.notifyParty(%s) done".formatted(partyName));
            })
        .exceptionally(
            e -> {
              log.error("Failed to notify party '%s': %s".formatted(partyName, e), e);
              return null;
            });
  }

  @SneakyThrows
  public JsonNode getPartyPrompt(String partyName) {
    ArrayNode jsonNodes =
        new ObjectMapper()
            .createArrayNode()
            .addAll(
                scenarios.stream()
                    .map(ConformanceScenario::nextAction)
                    .filter(Objects::nonNull)
                    .filter(action -> Objects.equals(action.getSourcePartyName(), partyName))
                    .map(ConformanceAction::asJsonNode)
                    .collect(Collectors.toList()));
    log.info(
        "ConformanceOrchestrator.getPartyPrompt(%s): %s"
            .formatted(partyName, jsonNodes.toPrettyString()));
    return jsonNodes;
  }

  public JsonNode postPartyInput(JsonNode partyInput) {
    log.info("ConformanceOrchestrator.postPartyInput(%s)".formatted(partyInput.toPrettyString()));
    String actionId = partyInput.get("actionId").asText();
    ConformanceAction action =
        scenarios.stream()
            .map(ConformanceScenario::nextAction)
            .filter(Objects::nonNull)
            .filter(nextAction -> Objects.equals(nextAction.getId().toString(), actionId))
            .findFirst()
            .orElseThrow();
    if (action instanceof SupplyAvailableTdrAction supplyAvailableTdrAction) {
      supplyAvailableTdrAction.getTdrConsumer().accept(partyInput.get("tdr").asText());
    } else {
      throw new UnsupportedOperationException(partyInput.toString());
    }
    return new ObjectMapper().createObjectNode();
  }
}
