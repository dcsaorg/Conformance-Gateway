package org.dcsa.conformance.core;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;

public abstract class AbstractComponentFactory {
  public abstract List<ConformanceParty> createParties(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      JsonNodeMap persistentMap,
      PartyWebClient webClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader);

  /**
   * Creates the ScenarioListBuilders of each standard module.
   * <p>
   * For a standard without modules, return a single-entry map with "" as key:
   * <p>
   * <code>
   * return Stream.of(Map.entry("", yourRootScenarioListBuilder)).collect(Collectors.toMap(
   *    Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new))
   * </code>
   */
  public abstract LinkedHashMap<String, ? extends ScenarioListBuilder<?>>
      createModuleScenarioListBuilders(
          PartyConfiguration[] partyConfigurations,
          CounterpartConfiguration[] counterpartConfigurations);

  public abstract SortedSet<String> getRoleNames();

  public abstract Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations);

  public abstract JsonNode getJsonSandboxConfigurationTemplate(
      String testedPartyRole, boolean isManual, boolean isTestingCounterpartsConfig);

  protected static String _findPartyOrCounterpartName(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      Predicate<String> rolePredicate) {
    return Stream.concat(
            Arrays.stream(partyConfigurations)
                .filter(partyConfiguration -> rolePredicate.test(partyConfiguration.getRole()))
                .map(PartyConfiguration::getName),
            Arrays.stream(counterpartConfigurations)
                .filter(
                    counterpartConfigurationConfiguration ->
                        rolePredicate.test(counterpartConfigurationConfiguration.getRole()))
                .map(CounterpartConfiguration::getName))
        .findFirst()
        .orElseThrow();
  }

  protected static String _sandboxIdPrefix(String standardName, String standardVersion) {
    return "%s-%s"
        .formatted(
            standardName.replaceAll(" ", ""),
            standardVersion.replaceAll("\\.", "").replaceAll("-", ""))
        .toLowerCase();
  }
}
