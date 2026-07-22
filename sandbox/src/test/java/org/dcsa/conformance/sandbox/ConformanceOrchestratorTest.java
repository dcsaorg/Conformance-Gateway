package org.dcsa.conformance.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.AbstractComponentFactory;
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

import static org.junit.jupiter.api.Assertions.assertSame;

class ConformanceOrchestratorTest {

  @Test
  void skippingAnActionDoesNotAlsoSkipTheFollowingGet() {
    var skippedAction = new TestAction("Carrier", "Shipper", null, "UC5", Set.of("Carrier"));
    var getAction = new TestAction("Shipper", "Carrier", skippedAction, "GET", Set.of());
    var scenario = new ConformanceScenario(0, 0, List.of(skippedAction, getAction));

    var orchestrator = new ConformanceOrchestrator(
      sandboxConfiguration(),
      new TestComponentFactory(scenario),
      new TrafficRecorder(null, ""),
      new EmptyJsonNodeMap(),
      ignored -> {});
    orchestrator.startOrStopScenario(scenario.getId().toString());

    orchestrator.completeCurrentAction(true);

    assertSame(getAction, scenario.peekNextAction());
  }

  private static SandboxConfiguration sandboxConfiguration() {
    var shipperParty = new PartyConfiguration();
    shipperParty.setName("Shipper");
    shipperParty.setRole("Shipper");

    var shipperCounterpart = counterpart("Shipper", "Shipper");
    var carrierCounterpart = counterpart("Carrier", "Carrier");

    var configuration = new SandboxConfiguration();
    configuration.setParties(new PartyConfiguration[]{shipperParty});
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

    private TestAction(
      String sourcePartyName,
      String targetPartyName,
      ConformanceAction previousAction,
      String actionTitle,
      Set<String> skippableForRoles) {
      super(sourcePartyName, targetPartyName, previousAction, actionTitle);
      this.skippableForRoles = skippableForRoles;
    }

    @Override
    public String getHumanReadablePrompt() {
      return "";
    }

    @Override
    public Set<String> skippableForRoles() {
      return skippableForRoles;
    }
  }
}


