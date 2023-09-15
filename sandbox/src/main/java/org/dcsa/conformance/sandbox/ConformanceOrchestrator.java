package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.ComponentFactory;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.report.ConformanceReport;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.state.StatefulEntity;
import org.dcsa.conformance.core.traffic.*;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;

@Slf4j
public class ConformanceOrchestrator implements StatefulEntity {
  private final boolean inactive;
  private final SandboxConfiguration sandboxConfiguration;
  private final ComponentFactory componentFactory;
  private final Consumer<Consumer<ConformanceOrchestrator>> asyncOrchestratorActionConsumer;
  private final ConformanceTrafficRecorder trafficRecorder;
  private final LinkedList<LinkedList<ConformanceScenario>> allScenarioBatches = new LinkedList<>();
  private final LinkedList<LinkedList<ConformanceScenario>> nextScenarioBatches =
      new LinkedList<>();

  public ConformanceOrchestrator(
      SandboxConfiguration sandboxConfiguration,
      ComponentFactory componentFactory,
      Consumer<Consumer<ConformanceOrchestrator>> asyncOrchestratorActionConsumer) {
    this.inactive = sandboxConfiguration.getOrchestrator() == null;
    this.sandboxConfiguration = sandboxConfiguration;
    this.componentFactory = componentFactory;
    this.asyncOrchestratorActionConsumer = asyncOrchestratorActionConsumer;
    trafficRecorder = inactive ? null : new ConformanceTrafficRecorder();
    if (!inactive) {
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
      if (allScenarioBatches.size() > 1) {
        log.info(
            "Running the scenarios in %d batches of up to %d scenarios each"
                .formatted(allScenarioBatches.size(), batchSize));
      }
    }
  }

  @Override
  public JsonNode exportJsonState() {
    ObjectNode jsonState = new ObjectMapper().createObjectNode();
    if (inactive) return jsonState;
    jsonState.set("trafficRecorder", trafficRecorder.exportJsonState());

    jsonState.put("nextScenarioBatchesSize", nextScenarioBatches.size());

    ArrayNode scenariosArrayNode = new ObjectMapper().createArrayNode();
    allScenarioBatches.forEach(
        batch -> batch.forEach(scenario -> scenariosArrayNode.add(scenario.exportJsonState())));
    jsonState.set("scenarios", scenariosArrayNode);

    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    if (inactive) return;
    trafficRecorder.importJsonState(jsonState.get("trafficRecorder"));

    int nextScenarioBatchesSize = jsonState.get("nextScenarioBatchesSize").asInt();
    while (nextScenarioBatches.size() > nextScenarioBatchesSize) {
      nextScenarioBatches.removeFirst();
    }

    ArrayNode scenariosNode = (ArrayNode) jsonState.get("scenarios");
    AtomicInteger index = new AtomicInteger(0);
    allScenarioBatches.forEach(
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

  public void scheduleNotifyAllParties() {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
    log.info("ConformanceOrchestrator.scheduleNotifyAllParties()");
    asyncOrchestratorActionConsumer.accept(ConformanceOrchestrator::_notifyAllParties);
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

  private void _notifyAllParties() {
    _nextScenarioBatch().stream()
        .map(ConformanceScenario::peekNextAction)
        .filter(Objects::nonNull)
        .map(ConformanceAction::getSourcePartyName)
        .collect(Collectors.toSet())
        .forEach(this::asyncNotifyParty);
  }

  private void asyncNotifyParty(String partyName) {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
    log.info("ConformanceOrchestrator.asyncNotifyParty(%s)".formatted(partyName));
    CompletableFuture.runAsync(() -> _syncNotifyParty(partyName))
        .exceptionally(
            e -> {
              log.error("ConformanceSandbox.asyncNotifyParty() failed: %s".formatted(e), e);
              return null;
            });
  }

  @SneakyThrows
  private void _syncNotifyParty(String partyName) {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
    log.info("ConformanceOrchestrator._syncNotifyParty(%s)".formatted(partyName));
    CounterpartConfiguration counterpartConfiguration =
        Arrays.stream(sandboxConfiguration.getCounterparts())
            .collect(Collectors.toMap(CounterpartConfiguration::getName, Function.identity()))
            .get(partyName);
    HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder()
                .uri(
                    URI.create(
                        counterpartConfiguration.getBaseUrl()
                            + counterpartConfiguration.getRootPath()
                            + "/party/%s/notification".formatted(partyName)))
                .timeout(Duration.ofHours(1))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  public JsonNode handleGetPartyPrompt(String partyName) {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
    log.info("ConformanceOrchestrator.handleGetPartyPrompt(%s)".formatted(partyName));
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

  public JsonNode handlePartyInput(JsonNode partyInput) {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");
    log.info("ConformanceOrchestrator.handlePartyInput(%s)".formatted(partyInput.toPrettyString()));
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
                        "Input for already handled(?) actionId %s: %s"
                            .formatted(actionId, partyInput.toPrettyString())));
    action.updateFromPartyInput(partyInput);
    scheduleNotifyAllParties();
    return new ObjectMapper().createObjectNode();
  }

  public void handlePartyTrafficExchange(ConformanceExchange exchange) {
    if (inactive) return;
    log.info(
        "ConformanceOrchestrator.handlePartyTrafficExchange(%s)".formatted(exchange.getUuid()));
    trafficRecorder.recordExchange(exchange);
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
    scheduleNotifyAllParties();
  }

  public String generateReport(Set<String> roleNames) {
    if (inactive) throw new UnsupportedOperationException("This orchestrator is inactive");

    ConformanceCheck conformanceCheck =
        componentFactory.createConformanceCheck(
            componentFactory.createScenarioListBuilder(
                sandboxConfiguration.getParties(), sandboxConfiguration.getCounterparts()));
    trafficRecorder.getTrafficStream().forEach(conformanceCheck::check);
    Map<String, ConformanceReport> reportsByRoleName =
        ConformanceReport.createForRoles(conformanceCheck, roleNames);

    return ConformanceReport.toHtmlReport(reportsByRoleName);
  }
}
