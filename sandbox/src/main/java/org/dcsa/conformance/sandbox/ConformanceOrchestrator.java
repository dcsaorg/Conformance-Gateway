package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.ComponentFactory;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.report.ConformanceReport;
import org.dcsa.conformance.core.report.ConformanceStatus;
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
  private final LinkedHashMap<UUID, ConformanceScenario> scenariosById = new LinkedHashMap<>();
  private final Map<UUID, UUID> latestRunIdsByScenarioId = new HashMap<>();
  private UUID currentScenarioId;

  public ConformanceOrchestrator(
      SandboxConfiguration sandboxConfiguration,
      ComponentFactory componentFactory,
      TrafficRecorder trafficRecorder,
      Consumer<ConformanceWebRequest> asyncWebClient) {
    this.sandboxConfiguration = sandboxConfiguration;
    this.componentFactory = componentFactory;
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
    }

    String uri =
        counterpartConfiguration.getRootPath() + "/party/%s/notification".formatted(partyName);
    String url = counterpartConfiguration.getBaseUrl() + uri;
    asyncWebClient.accept(
        new ConformanceWebRequest(
            "GET", url, uri, Collections.emptyMap(), Collections.emptyMap(), ""));
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
    if (currentScenarioId == null)
      throw new IllegalStateException(
          "Ignoring party input %s: no scenario is currently active"
              .formatted(partyInput.toPrettyString()));

    ConformanceScenario currentScenario = scenariosById.get(currentScenarioId);
    ConformanceAction nextAction = currentScenario.peekNextAction();
    if (nextAction == null)
      throw new IllegalStateException(
          "Ignoring party input %s: the active scenario has no next action"
              .formatted(partyInput.toPrettyString()));

    String actionId = partyInput.get("actionId").asText();
    if (!Objects.equals(actionId, nextAction.getId().toString()))
      throw new IllegalStateException(
          "Ignoring party input %s: the expected next action id is %s in current scenario %s"
              .formatted(
                  partyInput.toPrettyString(), nextAction.getId(), currentScenario.toString()));

    currentScenario.popNextAction();

    nextAction.updateFromPartyInput(partyInput);
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
    ConformanceAction nextAction = currentScenario.peekNextAction();
    if (nextAction == null) {
      log.info(
          "Ignoring exchange because the currently active scenario '%s' has no next action: %s"
              .formatted(currentScenario.toString(), exchange));
      return;
    }

    if (nextAction.updateFromExchangeIfItMatches(exchange)) {
      currentScenario.popNextAction();
      notifyNextActionParty();
    } else {
      log.info(
          "Ignoring exchange because it does not match the next action in the currently active scenario '%s': %s"
              .formatted(currentScenario.toString(), exchange));
    }
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

  public ArrayNode getScenarioDigests() {
    ConformanceCheck conformanceCheck =
        componentFactory.createConformanceCheck(
            componentFactory.createScenarioListBuilder(
                sandboxConfiguration.getParties(), sandboxConfiguration.getCounterparts()));

    Map<String, List<ConformanceExchange>> trafficByScenarioRun =
        trafficRecorder.getTrafficByScenarioRun();

    log.info("Traffic by scenario run:");
    trafficByScenarioRun.forEach(
        (runId, exchanges) ->
            log.info("Traffic for run '%s': %d exchanges".formatted(runId, exchanges.size())));

    log.info("Scenario latest run exchange count:");
    latestRunIdsByScenarioId
        .keySet()
        .forEach(
            scenarioId ->
                log.info(
                    "For scenario '%s' latestRunId is '%s' with %d exchanges"
                        .formatted(
                            scenariosById.get(scenarioId).toString(),
                            latestRunIdsByScenarioId.get(scenarioId),
                            trafficByScenarioRun
                                .get(latestRunIdsByScenarioId.get(scenarioId).toString())
                                .size())));

    latestRunIdsByScenarioId.values().stream()
        .flatMap(latestRunId -> trafficByScenarioRun.get(latestRunId.toString()).stream())
        .forEach(
            exchange -> {
              log.info("Checking exchange: " + exchange.toJson());
              conformanceCheck.check(exchange);
            });

    ConformanceReport fullReport =
        new ConformanceReport(conformanceCheck, _getManualCounterpart().getRole());

    Map<String, ConformanceStatus> statusByScenarioReportTitleDescription =
        _collectStatusByScenarioReportTitleDescription(new HashMap<>(), fullReport);
    log.info("================ MAP ================");
    statusByScenarioReportTitleDescription.forEach(
        (description, status) -> log.info("'%s' -> %s".formatted(description, status.name())));
    log.info("================ === ================");

    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode arrayNode = objectMapper.createArrayNode();
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
                      .put(
                          "conformanceStatus",
                          Objects.requireNonNullElse(
                                  statusByScenarioReportTitleDescription.get(
                                      scenario.getReportTitleDescription()),
                                  ConformanceStatus.NO_TRAFFIC)
                              .name());
              arrayNode.add(scenarioNode);
            });
    return arrayNode;
  }

  private static Map<String, ConformanceStatus> _collectStatusByScenarioReportTitleDescription(
      Map<String, ConformanceStatus> statusMap, ConformanceReport report) {
    if (report.getConformanceCheck() instanceof ActionCheck actionCheck) {
      String expectedPrefix = "Scenario handling - ";
      if (actionCheck.getTitle().startsWith(expectedPrefix)) {
        statusMap.put(
            actionCheck.getTitle().substring(expectedPrefix.length()),
            report.getConformanceStatus());
        log.info(
            "Adding to the map: '%s' -> %s"
                .formatted(
                    actionCheck.getTitle().substring(expectedPrefix.length()),
                    report.getConformanceStatus()));
      } else {
        log.info(
            "Ignoring ActionCheck not starting with the expected prefix: '%s'"
                .formatted(actionCheck.getTitle()));
      }
    }
    report
        .getSubReports()
        .forEach(subReport -> _collectStatusByScenarioReportTitleDescription(statusMap, subReport));
    return statusMap;
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
    ConformanceScenario scenario = this.scenariosById.get(UUID.fromString(scenarioId));

    ObjectNode scenarioNode = new ObjectMapper().createObjectNode();
    scenarioNode.put("nextActions", scenario.getNextActionsDescription());

    ConformanceAction nextAction = scenario.peekNextAction();
    if (nextAction == null) return scenarioNode;
    if (!Objects.equals(nextAction.getSourcePartyName(), _getManualCounterpart().getName()))
      return scenarioNode;

    scenarioNode.put("promptActionId", nextAction.getId().toString());
    scenarioNode.put("promptText", nextAction.getHumanReadablePrompt());
    scenarioNode.put("confirmationRequired", nextAction.isConfirmationRequired());
    scenarioNode.put("inputRequired", nextAction.isInputRequired());

    return scenarioNode;
  }

  public void startScenario(String scenarioId) {
    if (currentScenarioId != null)
      throw new IllegalStateException("Another scenario is currently running");

    ConformanceScenario scenario = scenariosById.get(UUID.fromString(scenarioId));
    if (scenario == null)
      throw new IllegalArgumentException("There is no scenario with id '%s'".formatted(scenarioId));

    currentScenarioId = scenario.getId();
    latestRunIdsByScenarioId.put(scenario.getId(), UUID.randomUUID());

    notifyNextActionParty();
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
