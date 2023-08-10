package org.dcsa.conformance.gateway.parties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.dcsa.conformance.gateway.scenarios.ConformanceScenario;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SupplyAvailableTdrAction;

public abstract class ConformanceOrchestrator {
  private final Map<String, Set<ConformanceAction>> notifiedActionsByPartyName = new HashMap<>();
  protected final List<ConformanceScenario> scenarios = new ArrayList<>();
  private final BiConsumer<String, JsonNode> partyNotifier;

  protected ConformanceOrchestrator(BiConsumer<String, JsonNode> partyNotifier) {
    this.partyNotifier = partyNotifier;
  }

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
                  notifiedActionsByPartyName.putIfAbsent(partyName, new HashSet<>());
              HashSet<ConformanceAction> newActions = new HashSet<>(nextActions);
              newActions.removeAll(notifiedActions);

              // TODO: async?
              partyNotifier.accept(
                  partyName,
                  new ObjectMapper()
                      .createArrayNode()
                      .addAll(
                          newActions.stream()
                              .map(ConformanceAction::asJsonNode)
                              .collect(Collectors.toSet())));

              notifiedActions.addAll(newActions);
            });
  }

  @SneakyThrows
  public JsonNode getPartyPrompt(String partyName) {
    return new ObjectMapper()
        .createArrayNode()
        .addAll(
            scenarios.stream()
                .map(ConformanceScenario::nextAction)
                .filter(Objects::nonNull)
                .filter(action -> Objects.equals(action.getSourcePartyName(), partyName))
                .map(ConformanceAction::asJsonNode)
                .collect(Collectors.toList()));
  }

  public JsonNode postPartyInput(JsonNode partyInput) {
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
