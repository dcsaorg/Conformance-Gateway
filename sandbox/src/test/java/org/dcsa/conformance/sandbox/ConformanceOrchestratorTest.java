package org.dcsa.conformance.sandbox;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.TrafficRecorder;
import org.dcsa.conformance.sandbox.configuration.SandboxConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConformanceOrchestratorTest {

  @Test
  void skippingAnActionDoesNotAlsoSkipTheFollowingGet() {
    var skippedAction =
      new TestAction("Shipper", "Carrier", null, "GET", Set.of("Shipper"), Set.of());
    var nextAction =
      new TestAction("Carrier", "Shipper", skippedAction, "UC5", Set.of(), Set.of());
    var scenario = new ConformanceScenario(0, 0, List.of(skippedAction, nextAction));

    var orchestrator = orchestrator(scenario, "Shipper");
    orchestrator.completeCurrentAction(true);

    assertEquals(ConformanceAction.CompletionOutcome.SKIPPED, skippedAction.getCompletionOutcome());
    assertSame(nextAction, scenario.peekNextAction());
  }

  @Test
  void completingOptionalCarrierNotificationWithoutTrafficUnlocksFollowingGet() {
    var carrierAction =
      new TestAction("Carrier", "Shipper", null, "UC5", Set.of(), Set.of("Carrier"));
    var getAction =
      new TestAction("Shipper", "Carrier", carrierAction, "GET", Set.of(), Set.of());
    var scenario = new ConformanceScenario(0, 0, List.of(carrierAction, getAction));

    var orchestrator = orchestrator(scenario, "Carrier");
    orchestrator.completeCurrentAction(false);

    assertEquals(
      ConformanceAction.CompletionOutcome.COMPLETED_WITHOUT_TRAFFIC,
      carrierAction.getCompletionOutcome());
    assertSame(getAction, scenario.peekNextAction());
  }

  @Test
  void roleQualifiedPartyInputMarksStandaloneNotificationActionCompletedWithoutTraffic() {
    var carrierAction =
      new TestAction("Carrier", "Shipper", null, "UC6", Set.of(), Set.of("Carrier"));
    var getAction =
      new TestAction("Shipper", "Carrier", carrierAction, "GET", Set.of(), Set.of());
    var scenario = new ConformanceScenario(0, 0, List.of(carrierAction, getAction));
    var orchestrator = orchestrator(scenario, "Carrier");

    var partyInput =
      OBJECT_MAPPER
        .createObjectNode()
        .put("actionId", carrierAction.getId().toString())
        .put("completeCurrentActionWithoutTraffic", "Carrier");
    partyInput.set("input", OBJECT_MAPPER.createObjectNode());
    orchestrator.handlePartyInput(partyInput);

    assertEquals(
      ConformanceAction.CompletionOutcome.COMPLETED_WITHOUT_TRAFFIC,
      carrierAction.getCompletionOutcome());
    assertSame(getAction, scenario.peekNextAction());
  }

  @Test
  void suppressedFollowUpCompletesActionThatHasPrimaryTrafficAndAllowsCarrierNotificationOmission() {
    var shipperAction =
      new TestAction(
        "Shipper", "Carrier", null, "UC7", Set.of(), Set.of("Carrier"), true, true);
    var nextAction =
      new TestAction("Carrier", "Shipper", shipperAction, "UC8", Set.of(), Set.of());
    var scenario = new ConformanceScenario(0, 0, List.of(shipperAction, nextAction));
    var orchestrator = orchestrator(scenario, "Shipper");

    orchestrator.handlePartyInput(
      OBJECT_MAPPER
        .createObjectNode()
        .put("completeCurrentActionWithoutNotification", "Carrier")
        .put("sessionId", "session-1"));

    assertEquals(
      ConformanceAction.CompletionOutcome.COMPLETED_WITHOUT_TRAFFIC,
      shipperAction.getCompletionOutcome());
    assertSame(nextAction, scenario.peekNextAction());
  }

  @Test
  void suppressedFollowUpFromDisallowedRoleDoesNotCompleteCurrentAction() {
    var shipperAction =
      new TestAction(
        "Shipper", "Carrier", null, "UC7", Set.of(), Set.of("Carrier"), true, true);
    var scenario = new ConformanceScenario(0, 0, List.of(shipperAction));
    var orchestrator = orchestrator(scenario, "Shipper");

    orchestrator.handlePartyInput(
      OBJECT_MAPPER
        .createObjectNode()
        .put("completeCurrentActionWithoutNotification", "Shipper")
        .put("sessionId", "session-1"));

    assertEquals(ConformanceAction.CompletionOutcome.NONE, shipperAction.getCompletionOutcome());
    assertSame(shipperAction, scenario.peekNextAction());
  }

  @Test
  void staleSuppressedFollowUpCannotCompleteAnActionInANewerSession() {
    var shipperAction =
      new TestAction(
        "Shipper", "Carrier", null, "UC7", Set.of(), Set.of("Carrier"), true, true);
    var scenario = new ConformanceScenario(0, 0, List.of(shipperAction));
    var orchestrator = orchestrator(scenario, "Shipper");
    orchestrator.startSession("session-2");

    orchestrator.handlePartyInput(
      OBJECT_MAPPER
        .createObjectNode()
        .put("completeCurrentActionWithoutNotification", "Carrier")
        .put("sessionId", "session-1"));

    assertEquals(ConformanceAction.CompletionOutcome.NONE, shipperAction.getCompletionOutcome());
    assertSame(shipperAction, scenario.peekNextAction());
  }

  @Test
  void optionalCarrierNotificationCannotBeExplicitlySkipped() {
    var carrierAction =
      new TestAction("Carrier", "Shipper", null, "UC5", Set.of(), Set.of("Carrier"));
    var orchestrator =
      orchestrator(new ConformanceScenario(0, 0, List.of(carrierAction)), "Carrier");

    assertThrows(UserFacingException.class, () -> orchestrator.completeCurrentAction(true));
  }

  @Test
  void requiredActionCannotBeCompletedWithoutTraffic() {
    var requiredAction =
      new TestAction("Carrier", "Shipper", null, "Required", Set.of(), Set.of());
    var orchestrator =
      orchestrator(new ConformanceScenario(0, 0, List.of(requiredAction)), "Carrier");

    assertThrows(UserFacingException.class, () -> orchestrator.completeCurrentAction(false));
  }

  @Test
  void completionOutcomeIsPersisted() {
    var original =
      new TestAction("Carrier", "Shipper", null, "UC5", Set.of(), Set.of("Carrier"));
    original.markCompletedWithoutTraffic();
    var restored =
      new TestAction("Carrier", "Shipper", null, "UC5", Set.of(), Set.of("Carrier"));

    restored.importJsonState(original.exportJsonState());

    assertEquals(
      ConformanceAction.CompletionOutcome.COMPLETED_WITHOUT_TRAFFIC,
      restored.getCompletionOutcome());
  }

  private static ConformanceOrchestrator orchestrator(
      ConformanceScenario scenario, String externalRole) {
    var orchestrator = new ConformanceOrchestrator(
      sandboxConfiguration(externalRole),
      new TestComponentFactory(scenario),
      new TrafficRecorder(null, ""),
      new EmptyJsonNodeMap(),
      ignored -> {});
    orchestrator.startOrStopScenario(scenario.getId().toString());
    orchestrator.startSession("session-1");
    return orchestrator;
  }

  private static SandboxConfiguration sandboxConfiguration(String externalRole) {
    String internalRole = externalRole.equals("Carrier") ? "Shipper" : "Carrier";
    var internalParty = new PartyConfiguration();
    internalParty.setName(internalRole);
    internalParty.setRole(internalRole);

    var shipperCounterpart = counterpart("Shipper", "Shipper");
    var carrierCounterpart = counterpart("Carrier", "Carrier");

    var configuration = new SandboxConfiguration();
    configuration.setParties(new PartyConfiguration[]{internalParty});
    configuration.setCounterparts(new CounterpartConfiguration[]{shipperCounterpart, carrierCounterpart});
    return configuration;
  }

  private static CounterpartConfiguration counterpart(String name, String role) {
    var counterpart = new CounterpartConfiguration();
    counterpart.setName(name);
    counterpart.setRole(role);
    counterpart.setInManualMode(true);
    return counterpart;
  }

  private static final class TestComponentFactory extends AbstractComponentFactory {
    private final ConformanceScenario scenario;

    private TestComponentFactory(ConformanceScenario scenario) {
      super("Test", "1.0.0", "Test", "Carrier", "Shipper");
      this.scenario = scenario;
    }

    @Override
    public List<ConformanceParty> createParties(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      JsonNodeMap persistentMap,
      PartyWebClient webClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
      return List.of();
    }

    @Override
    protected <T extends ScenarioListBuilder<T>> Map<String, T> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      boolean isWithNotifications) {
      return Map.of();
    }

    @Override
    public <T extends ScenarioListBuilder<T>> void generateConformanceScenarios(
      Map<String, List<ConformanceScenario>> scenariosByModuleName,
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      boolean isWithNotifications) {
      scenariosByModuleName.put("", List.of(scenario));
    }

    @Override
    public SortedSet<String> getRoleNames() {
      return new TreeSet<>(Set.of("Carrier", "Shipper"));
    }

    @Override
    public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
      return getRoleNames();
    }
  }

  private static final class EmptyJsonNodeMap extends JsonNodeMap {
    private EmptyJsonNodeMap() {
      super(null, "", "");
    }

    @Override
    public void save(String key, JsonNode value) {}

    @Override
    public JsonNode load(String key) {
      return null;
    }
  }

  private static final class TestAction extends ConformanceAction {
    private final Set<String> skippableForRoles;
    private final Set<String> completableWithoutTrafficForRoles;
    private final boolean primaryExchangeMatched;
    private final boolean notificationExchangeExpected;

    private TestAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      String actionTitle,
      Set<String> skippableForRoles,
      Set<String> completableWithoutTrafficForRoles) {
      this(
        sourcePartyName,
        targetPartyName,
        previousAction,
        actionTitle,
        skippableForRoles,
        completableWithoutTrafficForRoles,
        false,
        false);
    }

    private TestAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      String actionTitle,
      Set<String> skippableForRoles,
      Set<String> completableWithoutTrafficForRoles,
      boolean primaryExchangeMatched,
      boolean notificationExchangeExpected) {
      super(sourcePartyName, targetPartyName, previousAction, actionTitle);
      this.skippableForRoles = skippableForRoles;
      this.completableWithoutTrafficForRoles = completableWithoutTrafficForRoles;
      this.primaryExchangeMatched = primaryExchangeMatched;
      this.notificationExchangeExpected = notificationExchangeExpected;
    }

    @Override
    public String getHumanReadablePrompt() {
      return "";
    }

    @Override
    public Set<String> skippableForRoles() {
      return skippableForRoles;
    }

    @Override
    public Set<String> completableWithoutTrafficForRoles() {
      return completableWithoutTrafficForRoles;
    }

    @Override
    public boolean isMissingMatchedExchange() {
      return !primaryExchangeMatched;
    }

    @Override
    public boolean isMissingMatchedNotificationExchange() {
      return notificationExchangeExpected;
    }
  }
}


