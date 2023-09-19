package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final SandboxConfiguration sandboxConfiguration;
  private final ComponentFactory componentFactory;
  private final TrafficRecorder trafficRecorder;
  private final Consumer<ConformanceWebRequest> asyncWebClient;
  private final LinkedList<LinkedList<ConformanceScenario>> allScenarioBatches = new LinkedList<>();
  private final LinkedList<LinkedList<ConformanceScenario>> nextScenarioBatches =
      new LinkedList<>();

  public ConformanceOrchestrator(
      SandboxConfiguration sandboxConfiguration,
      ComponentFactory componentFactory,
      TrafficRecorder trafficRecorder,
      Consumer<ConformanceWebRequest> asyncWebClient) {
    this.sandboxConfiguration = sandboxConfiguration;
    this.componentFactory = componentFactory;
    this.trafficRecorder = trafficRecorder;
    this.asyncWebClient = asyncWebClient;

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

  @Override
  public JsonNode exportJsonState() {
    ObjectNode jsonState = new ObjectMapper().createObjectNode();

    jsonState.put("nextScenarioBatchesSize", nextScenarioBatches.size());

    ArrayNode scenariosArrayNode = new ObjectMapper().createArrayNode();
    allScenarioBatches.forEach(
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

  public void notifyRelevantParties() {
    log.info("ConformanceOrchestrator.notifyRelevantParties()");
    _nextScenarioBatch().stream()
        .map(ConformanceScenario::peekNextAction)
        .filter(Objects::nonNull)
        .map(ConformanceAction::getSourcePartyName)
        .collect(Collectors.toSet())
        .forEach(this::_asyncNotifyParty);
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

  private void _asyncNotifyParty(String partyName) {
    log.info("ConformanceOrchestrator.asyncNotifyParty(%s)".formatted(partyName));
    CounterpartConfiguration counterpartConfiguration =
        Arrays.stream(sandboxConfiguration.getCounterparts())
            .collect(Collectors.toMap(CounterpartConfiguration::getName, Function.identity()))
            .get(partyName);
    String uri =
        counterpartConfiguration.getRootPath() + "/party/%s/notification".formatted(partyName);
    String url = counterpartConfiguration.getBaseUrl() + uri;
    asyncWebClient.accept(
        new ConformanceWebRequest(
            "GET", url, uri, Collections.emptyMap(), Collections.emptyMap(), ""));
  }

  public JsonNode handleGetPartyPrompt(String partyName) {
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

  public void handlePartyInput(JsonNode partyInput) {
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
    notifyRelevantParties();
  }

  public String generateReport(Set<String> roleNames) {
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
