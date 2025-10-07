package org.dcsa.conformance.sandbox;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ScenarioCheck;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.report.ConformanceReport;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.state.StatefulEntity;
import org.dcsa.conformance.core.traffic.*;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;

@Slf4j
public class ConformanceOrchestrator implements StatefulEntity {
  private final SandboxConfiguration sandboxConfiguration;
  private final TrafficRecorder trafficRecorder;
  private final JsonNodeMap persistentMap;
  private final Consumer<ConformanceWebRequest> asyncWebClient;
  private final LinkedHashMap<String, List<ConformanceScenario>> scenariosByModuleName =
      new LinkedHashMap<>();
  private final LinkedHashMap<UUID, ConformanceScenario> _scenariosById = new LinkedHashMap<>();
  private final Map<UUID, UUID> latestRunIdsByScenarioId = new HashMap<>();
  private UUID currentScenarioId;

  @Setter private BiConsumer<String, String> waitingForBiConsumer = (forWhom, toDoWhat) -> {};

  public ConformanceOrchestrator(
      SandboxConfiguration sandboxConfiguration,
      AbstractComponentFactory componentFactory,
      TrafficRecorder trafficRecorder,
      JsonNodeMap persistentMap,
      Consumer<ConformanceWebRequest> asyncWebClient) {
    this.sandboxConfiguration = sandboxConfiguration;
    this.trafficRecorder = trafficRecorder;
    this.persistentMap = persistentMap;
    this.asyncWebClient = asyncWebClient;

    componentFactory.generateConformanceScenarios(
      this.scenariosByModuleName,
      sandboxConfiguration.getParties(),
      sandboxConfiguration.getCounterparts()
    );
    for (var scenarios : this.scenariosByModuleName.values()) {
      for (var scenario : scenarios) {
        _scenariosById.put(scenario.getId(), scenario);
      }
    }
  }

  @Override
  public JsonNode exportJsonState() {
    ObjectNode jsonState = OBJECT_MAPPER.createObjectNode();

    ArrayNode arrayNode = jsonState.putArray("latestRunIdsByScenarioId");
    latestRunIdsByScenarioId.forEach(
        (key, value) ->
            arrayNode
                .addObject()
                .put("scenarioId", key.toString())
                .put("latestRunId", value.toString()));

    if (currentScenarioId != null) {
      jsonState.put("currentScenarioId", currentScenarioId.toString());
      jsonState.set("currentScenario", _getCurrentScenario().exportJsonState());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    StreamSupport.stream(jsonState.get("latestRunIdsByScenarioId").spliterator(), false)
        .forEach(
            pairNode ->
                latestRunIdsByScenarioId.put(
                    UUID.fromString(pairNode.get("scenarioId").asText()),
                    UUID.fromString(pairNode.get("latestRunId").asText())));

    if (jsonState.has("currentScenarioId")) {
      currentScenarioId = UUID.fromString(jsonState.get("currentScenarioId").asText());
      _getCurrentScenario().importJsonState(jsonState.get("currentScenario"));
    }
  }

  private void _saveInactiveScenario(ConformanceScenario scenario) {
    UUID scenarioId = scenario.getId();
    JsonNode scenarioState = scenario.exportJsonState();
    persistentMap.save("scenario#%s".formatted(scenarioId), scenarioState);
  }

  private void _loadInactiveScenario(ConformanceScenario scenario) {
    UUID scenarioId = scenario.getId();
    JsonNode scenarioState = persistentMap.load("scenario#%s".formatted(scenarioId));
    if (scenarioState != null) {
      scenario.importJsonState(scenarioState);
    }
  }

  private void _loadAllInactiveScenarios() {
    _scenariosById.values().stream()
        .filter(scenario -> !scenario.getId().equals(currentScenarioId))
        .forEach(this::_loadInactiveScenario);
  }

  private Stream<ConformanceScenario> _allScenariosStream() {
    _loadAllInactiveScenarios();
    return _scenariosById.values().stream();
  }

  private Stream<UUID> _allScenarioIdsStream() {
    return _scenariosById.keySet().stream();
  }

  private ConformanceScenario _getCurrentScenario() {
    return _scenariosById.get(currentScenarioId);
  }

  private ConformanceScenario _getScenario(UUID scenarioId) {
    if (Objects.equals(scenarioId, currentScenarioId)) {
      return _getCurrentScenario();
    }
    ConformanceScenario scenario = _scenariosById.get(scenarioId);
    _loadInactiveScenario(scenario);
    return scenario;
  }

  public JsonNode getStatus() {
    ObjectNode statusNode = OBJECT_MAPPER.createObjectNode();
    statusNode.put(
        "scenariosLeft", _allScenariosStream().filter(ConformanceScenario::hasNextAction).count());
    return statusNode;
  }

  public void notifyNextActionParty() {
    log.info("ConformanceOrchestrator.notifyNextActionParty()");
    boolean autoAdvance =
        Arrays.stream(sandboxConfiguration.getCounterparts())
            .noneMatch(CounterpartConfiguration::isInManualMode);
    if (currentScenarioId == null) {
      if (autoAdvance) {
        currentScenarioId = _allScenarioIdsStream().findFirst().orElseThrow();
        latestRunIdsByScenarioId.put(currentScenarioId, UUID.randomUUID());
      } else {
        log.info("Skipping party notification: no current scenario");
        return;
      }
    }

    ConformanceAction nextAction = _getCurrentScenario().peekNextAction();
    if (nextAction == null) {
      _saveInactiveScenario(_getCurrentScenario());
      if (autoAdvance) {
        currentScenarioId =
            _allScenarioIdsStream()
                .dropWhile(scenarioId -> !scenarioId.equals(currentScenarioId))
                .skip(1)
                .findFirst()
                .orElse(null);
        if (currentScenarioId != null) {
          latestRunIdsByScenarioId.put(currentScenarioId, UUID.randomUUID());
          nextAction = _getCurrentScenario().peekNextAction();
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

    waitingForBiConsumer.accept(
        partyName, "perform action '%s'".formatted(nextAction.getActionTitle()));

    asyncWebClient.accept(
        new ConformanceWebRequest(
            "GET",
            counterpartConfiguration.getUrl() + "/conformance/notification",
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

    ArrayNode partyPrompt = OBJECT_MAPPER.createArrayNode();
    if (currentScenarioId == null) return partyPrompt;

    ConformanceAction nextAction = _getCurrentScenario().peekNextAction();
    if (nextAction == null) return partyPrompt;

    if (partyName.equals(nextAction.getSourcePartyName())) {
      partyPrompt.add(nextAction.asJsonNode());
    }
    return partyPrompt;
  }

  public void handlePartyInput(JsonNode partyInput) {
    log.info("ConformanceOrchestrator.handlePartyInput(%s)".formatted(partyInput.toPrettyString()));
    if (currentScenarioId == null) {
      log.info("Ignoring party input: no scenario is currently active");
      return;
    }

    ConformanceScenario currentScenario = _getCurrentScenario();
    ConformanceAction nextAction = currentScenario.peekNextAction();
    if (nextAction == null) {
      log.info("Ignoring party input: the active scenario has no next action");
      return;
    }

    String actionId = partyInput.get("actionId").asText();
    if (!Objects.equals(actionId, nextAction.getId().toString())) {
      log.info(
          "Ignoring party input: the expected next action id is %s in current scenario %s"
              .formatted(nextAction.getId(), currentScenario.toString()));
      return;
    }

    waitingForBiConsumer.accept(nextAction.getSourcePartyName(), null);

    currentScenario.popNextAction();
    try {
      nextAction.handlePartyInput(partyInput);
    } catch (UserFacingException e) {
      throw new UserFacingException(e.getMessage(), e);
    } catch (Exception e) {
      throw new UserFacingException(e);
    }
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

    ConformanceScenario currentScenario = _getCurrentScenario();
    ConformanceAction nextAction = currentScenario.peekNextAction();
    if (nextAction == null) {
      log.info(
          "Ignoring exchange because the currently active scenario '%s' has no next action: %s"
              .formatted(currentScenario.toString(), exchange));
      return;
    }

    waitingForBiConsumer.accept(nextAction.getSourcePartyName(), null);

    if (nextAction.handleExchange(exchange)) {
      boolean autoAdvance =
          Arrays.stream(sandboxConfiguration.getCounterparts())
              .noneMatch(CounterpartConfiguration::isInManualMode);
      if (autoAdvance) {
        currentScenario.popNextAction();
        notifyNextActionParty();
      }
    }
  }

  public void completeCurrentAction() {
    log.info("ConformanceOrchestrator.completeCurrentAction()");

    if (currentScenarioId == null) {
      log.info("Ignoring request to complete the current action: there is no active scenario");
      return;
    }

    ConformanceScenario currentScenario = _getCurrentScenario();
    ConformanceAction nextAction = currentScenario.peekNextAction();
    if (nextAction == null) {
      log.info(
          "Ignoring request to complete the current action: the currently active scenario '%s' has no current action"
              .formatted(currentScenario.toString()));
      return;
    }
    if (nextAction.isMissingMatchedExchange()
        && !nextAction.skippableForRoles().contains(sandboxConfiguration.getExternalPartyCounterpartConfiguration().getRole())) {
      throw new UserFacingException(
          "A required API exchange was not yet detected for action '%s'"
              .formatted(nextAction.getActionTitle()));
    }

    currentScenario.popNextAction();
    notifyNextActionParty();
  }

  public String generateReport(Set<String> roleNames, boolean printable) {
    if (!sandboxConfiguration.getOrchestrator().isActive()) throw new IllegalStateException();

    ConformanceCheck conformanceCheck = _createScenarioConformanceCheck();

    return ConformanceReport.toHtmlReport(
        ConformanceReport.createForRoles(conformanceCheck, roleNames), printable);
  }

  public ArrayNode getScenarioDigests() {
    ArrayNode allModulesNode = OBJECT_MAPPER.createArrayNode();
    if (!sandboxConfiguration.getOrchestrator().isActive()) return allModulesNode;

    _loadAllInactiveScenarios();

    new ConformanceReport(_createScenarioConformanceCheck(), _getManualCounterpart().getRole());

    scenariosByModuleName.forEach(
      (moduleName, scenarios) -> {
        ObjectNode moduleNode = allModulesNode.addObject();
        moduleNode.put("moduleName", moduleName);
        ArrayNode allScenariosNode = moduleNode.putArray("scenarios");
        scenarios.forEach(
          scenario -> {
            ObjectNode scenarioNode = allScenariosNode.addObject();
            scenarioNode.put("id", scenario.getId().toString());
            scenarioNode.put("name", scenario.getTitle());
            scenarioNode.put("isRunning", scenario.getId().equals(currentScenarioId));
            scenarioNode.put("conformanceStatus", scenario.getLatestComputedStatus().name());
          });
      });
    return allModulesNode;
  }

  private ConformanceCheck _createScenarioConformanceCheck() {
    _loadAllInactiveScenarios();
    ConformanceCheck conformanceCheck =
      new ConformanceCheck("Scenario conformance") { // standard check
        @Override
        protected Stream<? extends ConformanceCheck> createSubChecks() {
          return scenariosByModuleName.entrySet().stream()
            .map(
              moduleNameAndScenarios -> {
                String moduleName = moduleNameAndScenarios.getKey();
                var scenarios = moduleNameAndScenarios.getValue();
                return new ConformanceCheck(
                  moduleName.isBlank() ? "All scenarios" : moduleName) { // module check
                  @Override
                  protected Stream<? extends ConformanceCheck> createSubChecks() {
                    return scenarios.stream()
                      .map(
                        scenario ->
                          new ScenarioCheck( // scenario check
                            scenario,
                            sandboxConfiguration.getStandard().getVersion()));
                  }
                };
              });
        }
      };

    Map<String, List<ConformanceExchange>> trafficByScenarioRun =
      trafficRecorder.getTrafficByScenarioRun();
    Map<UUID, ConformanceExchange> exchangesByUuid =
      latestRunIdsByScenarioId.values().stream()
        .filter(latestRunId -> trafficByScenarioRun.containsKey(latestRunId.toString()))
        .flatMap(latestRunId -> trafficByScenarioRun.get(latestRunId.toString()).stream())
        .collect(Collectors.toMap(ConformanceExchange::getUuid, Function.identity()));
    conformanceCheck.check(exchangesByUuid::get);

    return conformanceCheck;
  }

  public JsonNode createFullReport() {
    return new ConformanceReport(
            _createScenarioConformanceCheck(), _getManualCounterpart().getRole())
        .toJsonReport();
  }

  public ObjectNode getScenarioDigest(String scenarioId) {
    ConformanceScenario scenario = _getScenario(UUID.fromString(scenarioId));
    return OBJECT_MAPPER
        .createObjectNode()
        .put("id", scenario.getId().toString())
        .put("name", scenario.getTitle())
        .put("conformanceStatus", "not fetched");
  }

  public ObjectNode getScenarioStatus(String scenarioId) {
    UUID scenarioUuid = UUID.fromString(scenarioId);

    ObjectNode scenarioNode = OBJECT_MAPPER.createObjectNode();
    UUID runUuid = latestRunIdsByScenarioId.get(scenarioUuid);
    if (runUuid == null) return scenarioNode;

    ConformanceScenario scenario = _getScenario(scenarioUuid);
    boolean isActive = scenario.getId().equals(currentScenarioId);
    if (!isActive) {
      _loadInactiveScenario(scenario);
    }
    scenarioNode.put("isRunning", isActive);
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

    ConformanceReport fullReport =
        new ConformanceReport(conformanceCheck, _getManualCounterpart().getRole());
    ConformanceReport scenarioSubReport =
        fullReport.getSubReports().stream()
            .flatMap(subReport -> subReport.getSubReports().stream())
            .filter(subSubReport -> subSubReport.getTitle().equals(scenario.getTitle()))
            .findFirst()
            .orElseThrow();
    scenarioNode.set("conformanceSubReport", scenarioSubReport.toJsonReport());

    scenarioNode.put("isSkippable", false);
    if (nextAction != null
        && nextAction.skippableForRoles().contains(sandboxConfiguration.getExternalPartyCounterpartConfiguration().getRole())) {
      scenarioNode.put("isSkippable", true);
    }

    return scenarioNode;
  }

  public void startOrStopScenario(String scenarioId) {
    UUID scenarioUuid = UUID.fromString(scenarioId);
    if (currentScenarioId != null) {
      if (currentScenarioId.equals(scenarioUuid)) {
        // stop
        ConformanceScenario currentScenario = _getCurrentScenario();
        _saveInactiveScenario(currentScenario);
        currentScenarioId = null;
      } else {
        throw new IllegalStateException("Another scenario is currently running");
      }
    } else {
      // start or restart
      ConformanceScenario inactiveScenario = _getScenario(scenarioUuid);
      if (inactiveScenario == null)
        throw new IllegalArgumentException(
            "There is no scenario with id '%s'".formatted(scenarioId));
      if (latestRunIdsByScenarioId.containsKey(inactiveScenario.getId())) {
        inactiveScenario.reset();
        _saveInactiveScenario(inactiveScenario);
      }
      currentScenarioId = inactiveScenario.getId();
      latestRunIdsByScenarioId.put(inactiveScenario.getId(), UUID.randomUUID());
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

  public ObjectNode getCurrentActionExchanges(String scenarioId) {
    ConformanceScenario scenario = _getScenario(UUID.fromString(scenarioId));
    boolean isActive = scenario.getId().equals(currentScenarioId);
    if (!isActive) {
      _loadInactiveScenario(scenario);
    }
    ObjectNode exchangesNode = OBJECT_MAPPER.createObjectNode();
    ConformanceAction nextAction = scenario.peekNextAction();
    if (nextAction == null) {
      return exchangesNode;
    }
    UUID matchedExchangeUuid = nextAction.getMatchedExchangeUuid();
    UUID matchedNotificationExchangeUuid = nextAction.getMatchedNotificationExchangeUuid();
    if (matchedExchangeUuid != null || matchedNotificationExchangeUuid != null) {
      List<ConformanceExchange> conformanceExchanges =
          this.trafficRecorder
              .getTrafficByScenarioRun()
              .get(latestRunIdsByScenarioId.get(scenario.getId()).toString());
      if (matchedExchangeUuid != null) {
        exchangesNode.set(
            "primaryExchange",
            conformanceExchanges.stream()
                .filter(exchange -> exchange.getUuid().equals(matchedExchangeUuid))
                .findFirst()
                .orElseThrow()
                .toJson());
      }
      if (matchedNotificationExchangeUuid != null) {
        exchangesNode.set(
            "secondaryExchange",
            conformanceExchanges.stream()
                .filter(exchange -> exchange.getUuid().equals(matchedNotificationExchangeUuid))
                .findFirst()
                .orElseThrow()
                .toJson());
      }
    }
    return exchangesNode;
  }
}
