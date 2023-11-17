package org.dcsa.conformance.core;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceRequest;

public abstract class AbstractComponentFactory {
  public abstract List<ConformanceParty> createParties(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      JsonNodeMap persistentMap,
      Consumer<ConformanceRequest> asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader);

  public abstract ScenarioListBuilder<?> createScenarioListBuilder(
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
