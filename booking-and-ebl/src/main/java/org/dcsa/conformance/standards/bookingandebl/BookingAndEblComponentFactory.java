package org.dcsa.conformance.standards.bookingandebl;

import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

public class BookingAndEblComponentFactory extends AbstractComponentFactory {

  BookingAndEblComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
    super(standardName, standardVersion, scenarioSuite, "Carrier", "Shipper");
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
      CounterpartConfiguration[] counterpartConfigurations) {
    return Map.of();
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return null;
  }

  @Override
  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return Set.of();
  }
}
