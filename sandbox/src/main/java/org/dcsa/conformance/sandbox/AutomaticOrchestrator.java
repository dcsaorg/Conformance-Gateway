package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.ComponentFactory;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.TrafficRecorder;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;

@Slf4j
public class AutomaticOrchestrator extends ConformanceOrchestrator {
  private final LinkedList<LinkedList<ConformanceScenario>> allScenarioBatches = new LinkedList<>();
  private final LinkedList<LinkedList<ConformanceScenario>> nextScenarioBatches =
      new LinkedList<>();

  public AutomaticOrchestrator(
      SandboxConfiguration sandboxConfiguration,
      ComponentFactory componentFactory,
      TrafficRecorder trafficRecorder,
      Consumer<ConformanceWebRequest> asyncWebClient) {
    super(sandboxConfiguration, componentFactory, trafficRecorder, asyncWebClient);

    int batchSize = sandboxConfiguration.getOrchestrator().getMaxParallelScenarios();
    allScenarioBatches.addLast(new LinkedList<>());
    componentFactory
        .createScenarioListBuilder(
            sandboxConfiguration.getParties(), sandboxConfiguration.getCounterparts())
        .buildScenarioList()
        .forEach(
            scenario -> {
              LinkedList<ConformanceScenario> currentBatch = allScenarioBatches.peekLast();
              if (Objects.requireNonNull(currentBatch).size() >= batchSize) {
                allScenarioBatches.addLast(currentBatch = new LinkedList<>());
              }
              currentBatch.addLast(scenario);
            });
    nextScenarioBatches.addAll(allScenarioBatches);
  }

  @Override
  public JsonNode exportJsonState() {
    ObjectNode jsonState = new ObjectMapper().createObjectNode();

    jsonState.put("nextScenarioBatchesSize", nextScenarioBatches.size());

    ArrayNode scenariosArrayNode = new ObjectMapper().createArrayNode();
    nextScenarioBatches.forEach(
        batch -> batch.forEach(scenario -> scenariosArrayNode.add(scenario.exportJsonState())));
    jsonState.set("scenarios", scenariosArrayNode);

    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    int nextScenarioBatchesSize = jsonState.get("nextScenarioBatchesSize").asInt();
    while (nextScenarioBatches.size() > nextScenarioBatchesSize) {
      nextScenarioBatches.removeFirst();
    }

    ArrayNode scenariosNode = (ArrayNode) jsonState.get("scenarios");
    AtomicInteger index = new AtomicInteger(0);
    nextScenarioBatches.forEach(
        batch ->
            batch.forEach(
                scenario -> scenario.importJsonState(scenariosNode.get(index.getAndIncrement()))));
  }

  public JsonNode getStatus() {
    ObjectNode statusNode = new ObjectMapper().createObjectNode();
    statusNode.put(
        "scenariosLeft",
        nextScenarioBatches.stream()
            .flatMap(Collection::stream)
            .filter(ConformanceScenario::hasNextAction)
            .count());
    return statusNode;
  }

  private LinkedList<ConformanceScenario> _nextScenarioBatch() {
    if (!nextScenarioBatches.isEmpty()) {
      if (nextScenarioBatches.peekFirst().stream().noneMatch(ConformanceScenario::hasNextAction)) {
        nextScenarioBatches.removeFirst();
        log.info("Scenario batch completed, %d batches left".formatted(nextScenarioBatches.size()));
      }
    }
    return Objects.requireNonNullElse(nextScenarioBatches.peekFirst(), new LinkedList<>());
  }

  public void notifyRelevantParties() {
    log.info("AutomaticOrchestrator.notifyRelevantParties()");
    _nextScenarioBatch().stream()
        .map(ConformanceScenario::peekNextAction)
        .filter(Objects::nonNull)
        .map(ConformanceAction::getSourcePartyName)
        .collect(Collectors.toSet())
        .forEach(this::_asyncNotifyParty);
  }

  public JsonNode handleGetPartyPrompt(String partyName) {
    log.info("AutomaticOrchestrator.handleGetPartyPrompt(%s)".formatted(partyName));
    return new ObjectMapper()
        .createArrayNode()
        .addAll(
            _nextScenarioBatch().stream()
                .map(ConformanceScenario::peekNextAction)
                .filter(Objects::nonNull)
                .filter(action -> Objects.equals(action.getSourcePartyName(), partyName))
                .map(ConformanceAction::asJsonNode)
                .collect(Collectors.toList()));
  }

  public void handlePartyInput(JsonNode partyInput) {
    log.info("AutomaticOrchestrator.handlePartyInput(%s)".formatted(partyInput.toPrettyString()));
    String actionId = partyInput.get("actionId").asText();
    ConformanceAction action =
        _nextScenarioBatch().stream()
            .filter(
                scenario ->
                    scenario.hasNextAction()
                        && Objects.equals(actionId, scenario.peekNextAction().getId().toString()))
            .map(ConformanceScenario::popNextAction)
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Ignoring party input %s with unrecognized action id, known next actions are: %s"
                            .formatted(
                                actionId,
                                _nextScenarioBatch().stream()
                                    .filter(ConformanceScenario::hasNextAction)
                                    .map(
                                        scenario ->
                                            scenario.peekNextAction().asJsonNode().toPrettyString())
                                    .toList())));
    action.updateFromPartyInput(partyInput);
    notifyRelevantParties();
  }

  public void handlePartyTrafficExchange(ConformanceExchange exchange) {
    log.info(
        "AutomaticOrchestrator.handlePartyTrafficExchange(%s)".formatted(exchange.getUuid()));
    trafficRecorder.recordExchange(exchange, "unset");
    ConformanceAction action =
        _nextScenarioBatch().stream()
            .filter(
                scenario ->
                    scenario.hasNextAction()
                        && scenario.peekNextAction().updateFromExchangeIfItMatches(exchange))
            .findFirst()
            .map(ConformanceScenario::popNextAction)
            .orElse(null);
    if (action == null) {
      log.info(
          "Ignoring conformance exchange not matched by any pending actions: %s"
              .formatted(exchange));
      return;
    }
    notifyRelevantParties();
  }
}
