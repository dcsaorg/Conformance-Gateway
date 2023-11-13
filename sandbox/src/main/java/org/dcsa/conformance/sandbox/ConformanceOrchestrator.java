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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ScenarioCheck;
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
  private final TrafficRecorder trafficRecorder;
  private final Consumer<ConformanceWebRequest> asyncWebClient;
  private final LinkedHashMap<UUID, ConformanceScenario> scenariosById = new LinkedHashMap<>();
  private final Map<UUID, UUID> latestRunIdsByScenarioId = new HashMap<>();
  private UUID currentScenarioId;

  public ConformanceOrchestrator(
      SandboxConfiguration sandboxConfiguration,
      AbstractComponentFactory componentFactory,
      TrafficRecorder trafficRecorder,
      Consumer<ConformanceWebRequest> asyncWebClient) {
    this.sandboxConfiguration = sandboxConfiguration;
    this.trafficRecorder = trafficRecorder;
    this.asyncWebClient = asyncWebClient;

    componentFactory
        .createScenarioListBuilder(
            sandboxConfiguration.getParties(), sandboxConfiguration.getCounterparts())
        .buildScenarioList()
        .forEach(scenario -> this.scenariosById.put(scenario.getId(), scenario));
  }

  @Override
  public JsonNode exportJsonState() {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode jsonState = objectMapper.createObjectNode();

    { // scoped
      ArrayNode arrayNode = objectMapper.createArrayNode();
      scenariosById.values().forEach(scenario -> arrayNode.add(scenario.exportJsonState()));
      jsonState.set("scenarios", arrayNode);
    }

    { // scoped
      ArrayNode arrayNode = objectMapper.createArrayNode();
      latestRunIdsByScenarioId.forEach(
          (key, value) ->
              arrayNode.add(
                  objectMapper
                      .createObjectNode()
                      .put("scenarioId", key.toString())
                      .put("latestRunId", value.toString())));
      jsonState.set("latestRunIdsByScenarioId", arrayNode);
    }

    if (currentScenarioId != null) {
      jsonState.put("currentScenarioId", currentScenarioId.toString());
    }

    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    ArrayNode scenariosNode = (ArrayNode) jsonState.get("scenarios");
    AtomicInteger index = new AtomicInteger(0);
    ArrayList<ConformanceScenario> scenariosList = new ArrayList<>(scenariosById.values());
    scenariosById.clear();
    scenariosList.forEach(
        scenario -> {
          scenario.importJsonState(scenariosNode.get(index.getAndIncrement()));
          scenariosById.put(scenario.getId(), scenario);
        });

    StreamSupport.stream(jsonState.get("latestRunIdsByScenarioId").spliterator(), false)
        .forEach(
            pairNode ->
                latestRunIdsByScenarioId.put(
                    UUID.fromString(pairNode.get("scenarioId").asText()),
                    UUID.fromString(pairNode.get("latestRunId").asText())));

    if (jsonState.has("currentScenarioId")) {
      currentScenarioId = UUID.fromString(jsonState.get("currentScenarioId").asText());
    }
  }

  public JsonNode getStatus() {
    ObjectNode statusNode = new ObjectMapper().createObjectNode();
    statusNode.put(
        "scenariosLeft",
        scenariosById.values().stream().filter(ConformanceScenario::hasNextAction).count());
    return statusNode;
  }

  public void notifyNextActionParty() {
    log.info("ConformanceOrchestrator.notifyNextActionParty()");
    boolean autoAdvance =
        Arrays.stream(sandboxConfiguration.getCounterparts())
            .noneMatch(CounterpartConfiguration::isInManualMode);
    if (currentScenarioId == null) {
      if (autoAdvance) {
        currentScenarioId = scenariosById.keySet().stream().findFirst().orElseThrow();
        latestRunIdsByScenarioId.put(currentScenarioId, UUID.randomUUID());
      } else {
        log.info("Skipping party notification: no current scenario");
        return;
      }
    }

    ConformanceAction nextAction = scenariosById.get(currentScenarioId).peekNextAction();
    if (nextAction == null) {
      if (autoAdvance) {
        currentScenarioId =
            scenariosById.keySet().stream()
                .dropWhile(scenarioId -> !scenarioId.equals(currentScenarioId))
                .skip(1)
                .findFirst()
                .orElse(null);
        if (currentScenarioId != null) {
          latestRunIdsByScenarioId.put(currentScenarioId, UUID.randomUUID());
          nextAction = scenariosById.get(currentScenarioId).peekNextAction();
        } else {
          log.info("Skipping party notification: no more scenarios to run");
          return;
        }
      } else {
        currentScenarioId = null;
        log.info("Skipping party notification: no next action in the current scenario");
        return;
      }
    }

    String partyName = nextAction.getSourcePartyName();
    CounterpartConfiguration counterpartConfiguration =
        Arrays.stream(sandboxConfiguration.getCounterparts())
            .collect(Collectors.toMap(CounterpartConfiguration::getName, Function.identity()))
            .get(partyName);
    if (counterpartConfiguration.isInManualMode()) {
      log.info(
          "Skipping party notification: counterpart %s is in manual mode".formatted(partyName));
      return;
    }

    String url = counterpartConfiguration.getUrl() + "/conformance/notification";
    asyncWebClient.accept(
        new ConformanceWebRequest(
            "GET",
            url,
            Collections.emptyMap(),
            counterpartConfiguration.getAuthHeaderName().isBlank()
                ? Collections.emptyMap()
                : Map.of(
                    counterpartConfiguration.getAuthHeaderName(),
                    List.of(counterpartConfiguration.getAuthHeaderValue())),
            ""));
  }

  public JsonNode handleGetPartyPrompt(String partyName) {
    log.info("ConformanceOrchestrator.handleGetPartyPrompt(%s)".formatted(partyName));
    ObjectMapper objectMapper = new ObjectMapper();

    ArrayNode partyPrompt = objectMapper.createArrayNode();
    if (currentScenarioId == null) return partyPrompt;

    ConformanceAction nextAction = scenariosById.get(currentScenarioId).peekNextAction();
    if (nextAction == null) return partyPrompt;

    if (partyName.equals(nextAction.getSourcePartyName())) {
      partyPrompt.add(nextAction.asJsonNode());
    }
    return partyPrompt;
  }

  public void handlePartyInput(JsonNode partyInput) {
    log.info("ConformanceOrchestrator.handlePartyInput(%s)".formatted(partyInput.toPrettyString()));
    if (currentScenarioId == null) {
      log.info(
          "Ignoring party input %s: no scenario is currently active"
              .formatted(partyInput.toPrettyString()));
      return;
    }

    ConformanceScenario currentScenario = scenariosById.get(currentScenarioId);
    ConformanceAction nextAction = currentScenario.peekNextAction();
    if (nextAction == null) {
      log.info(
          "Ignoring party input %s: the active scenario has no next action"
              .formatted(partyInput.toPrettyString()));
      return;
    }

    String actionId = partyInput.get("actionId").asText();
    if (!Objects.equals(actionId, nextAction.getId().toString())) {
      if (partyInput.has("input")) {
        throw new IllegalStateException(
            "Unexpected party input %s: the expected next action id is %s in current scenario %s"
                .formatted(
                    partyInput.toPrettyString(), nextAction.getId(), currentScenario.toString()));
      } else {
        log.info("Ignoring redundant party input %s".formatted(partyInput.toPrettyString()));
        return;
      }
    }

    currentScenario.popNextAction();

    nextAction.handlePartyInput(partyInput);
    notifyNextActionParty();
  }

  public void handlePartyTrafficExchange(ConformanceExchange exchange) {
    log.info(
        "ConformanceOrchestrator.handlePartyTrafficExchange(%s)".formatted(exchange.getUuid()));

    trafficRecorder.recordExchange(
        exchange,
        currentScenarioId == null
            ? "unknown"
            : latestRunIdsByScenarioId.get(currentScenarioId).toString());

    if (currentScenarioId == null) {
      log.info(
          "Ignoring exchange because there is no currently active scenario: %s"
              .formatted(exchange));
      return;
    }

    ConformanceScenario currentScenario = scenariosById.get(currentScenarioId);
    if (currentScenario.peekNextAction() == null) {
      log.info(
          "Ignoring exchange because the currently active scenario '%s' has no next action: %s"
              .formatted(currentScenario.toString(), exchange));
      return;
    }

    currentScenario.popNextAction().handleExchange(exchange);
    notifyNextActionParty();
  }

  public String generateReport(Set<String> roleNames, boolean printable) {
    if (!sandboxConfiguration.getOrchestrator().isActive()) throw new IllegalStateException();

    ConformanceCheck conformanceCheck = _createScenarioConformanceCheck();

    Map<String, List<ConformanceExchange>> trafficByScenarioRun =
        trafficRecorder.getTrafficByScenarioRun();
    Map<UUID, ConformanceExchange> exchangesByUuid =
        latestRunIdsByScenarioId.values().stream()
            .filter(latestRunId -> trafficByScenarioRun.containsKey(latestRunId.toString()))
            .flatMap(latestRunId -> trafficByScenarioRun.get(latestRunId.toString()).stream())
            .collect(Collectors.toMap(ConformanceExchange::getUuid, Function.identity()));
    conformanceCheck.check(exchangesByUuid::get);

    return ConformanceReport.toHtmlReport(
        ConformanceReport.createForRoles(conformanceCheck, roleNames), printable);
  }

  public ArrayNode getScenarioDigests() {
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode arrayNode = objectMapper.createArrayNode();
    if (!sandboxConfiguration.getOrchestrator().isActive()) return arrayNode;

    ConformanceCheck conformanceCheck = _createScenarioConformanceCheck();

    Map<String, List<ConformanceExchange>> trafficByScenarioRun =
        trafficRecorder.getTrafficByScenarioRun();
    Map<UUID, ConformanceExchange> exchangesByUuid =
        latestRunIdsByScenarioId.values().stream()
            .filter(latestRunId -> trafficByScenarioRun.containsKey(latestRunId.toString()))
            .flatMap(latestRunId -> trafficByScenarioRun.get(latestRunId.toString()).stream())
            .collect(Collectors.toMap(ConformanceExchange::getUuid, Function.identity()));
    conformanceCheck.check(exchangesByUuid::get);

    new ConformanceReport(conformanceCheck, _getManualCounterpart().getRole());

    this.scenariosById
        .values()
        .forEach(
            scenario -> {
              log.info(
                  "Scenario description: '%s'".formatted(scenario.getReportTitleDescription()));
              ObjectNode scenarioNode =
                  objectMapper
                      .createObjectNode()
                      .put("id", scenario.getId().toString())
                      .put("name", scenario.getTitle())
                      .put("isRunning", scenario.getId().equals(currentScenarioId))
                      .put("conformanceStatus", scenario.getLatestComputedStatus().name());
              arrayNode.add(scenarioNode);
            });
    return arrayNode;
  }

  private ConformanceCheck _createScenarioConformanceCheck() {
    return new ConformanceCheck("Scenario conformance") {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return scenariosById.values().stream()
            .map(
                scenario ->
                    new ScenarioCheck(scenario, sandboxConfiguration.getStandard().getVersion()));
      }
    };
  }

  public ObjectNode getScenarioDigest(String scenarioId) {
    ConformanceScenario scenario = this.scenariosById.get(UUID.fromString(scenarioId));
    return new ObjectMapper()
        .createObjectNode()
        .put("id", scenario.getId().toString())
        .put("name", scenario.getTitle())
        .put("conformanceStatus", "not fetched");
  }

  public ObjectNode getScenarioStatus(String scenarioId) {
    UUID scenarioUuid = UUID.fromString(scenarioId);

    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode scenarioNode = objectMapper.createObjectNode();
    UUID runUuid = latestRunIdsByScenarioId.get(scenarioUuid);
    if (runUuid == null) return scenarioNode;

    ConformanceScenario scenario = this.scenariosById.get(scenarioUuid);
    scenarioNode.put("isRunning", scenario.getId().equals(currentScenarioId));
    scenarioNode.put("nextActions", scenario.getNextActionsDescription());

    ConformanceAction nextAction = scenario.peekNextAction();
    if (nextAction != null
        && Objects.equals(nextAction.getSourcePartyName(), _getManualCounterpart().getName())) {
      scenarioNode.put("promptActionId", nextAction.getId().toString());
      scenarioNode.put("promptText", nextAction.getHumanReadablePrompt());
      JsonNode jsonForHumanReadablePrompt = nextAction.getJsonForHumanReadablePrompt();
      if (jsonForHumanReadablePrompt != null) {
        scenarioNode.set("jsonForPromptText", jsonForHumanReadablePrompt);
      }
      scenarioNode.put("confirmationRequired", nextAction.isConfirmationRequired());
      scenarioNode.put("inputRequired", nextAction.isInputRequired());
    }

    ConformanceCheck conformanceCheck = _createScenarioConformanceCheck();
    Map<String, List<ConformanceExchange>> trafficByScenarioRun =
        trafficRecorder.getTrafficByScenarioRun();
    List<ConformanceExchange> scenarioRunExchanges =
        Objects.requireNonNullElse(
            trafficByScenarioRun.get(runUuid.toString()), Collections.emptyList());
    Map<UUID, ConformanceExchange> exchangesByUuid =
        scenarioRunExchanges.stream()
            .collect(Collectors.toMap(ConformanceExchange::getUuid, Function.identity()));
    conformanceCheck.check(exchangesByUuid::get);
    ConformanceReport fullReport =
        new ConformanceReport(conformanceCheck, _getManualCounterpart().getRole());
    ConformanceReport scenarioSubReport =
        fullReport.getSubReports().stream()
            .filter(subReport -> subReport.getTitle().equals(scenario.getTitle()))
            .findFirst()
            .orElseThrow();
    scenarioNode.set("conformanceSubReport", scenarioSubReport.toJsonReport());

    return scenarioNode;
  }

  public void startOrStopScenario(String scenarioId) {
    UUID scenarioUuid = UUID.fromString(scenarioId);
    if (currentScenarioId != null) {
      if (currentScenarioId.equals(scenarioUuid)) {
        // stop
        scenariosById.get(currentScenarioId).reset();
        latestRunIdsByScenarioId.remove(currentScenarioId);
        currentScenarioId = null;
      } else {
        throw new IllegalStateException("Another scenario is currently running");
      }
    } else {
      // start or restart
      ConformanceScenario scenario = scenariosById.get(scenarioUuid);
      if (scenario == null)
        throw new IllegalArgumentException(
            "There is no scenario with id '%s'".formatted(scenarioId));
      if (latestRunIdsByScenarioId.containsKey(scenario.getId())) {
        scenario.reset();
      }
      currentScenarioId = scenario.getId();
      latestRunIdsByScenarioId.put(scenario.getId(), UUID.randomUUID());
      notifyNextActionParty();
    }
  }

  private CounterpartConfiguration _getManualCounterpart() {
    return Arrays.stream(sandboxConfiguration.getCounterparts())
        .filter(
            counterpart ->
                Arrays.stream(sandboxConfiguration.getParties())
                    .noneMatch(party -> party.getName().equals(counterpart.getName())))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Manual counterpart not found"));
  }
}
