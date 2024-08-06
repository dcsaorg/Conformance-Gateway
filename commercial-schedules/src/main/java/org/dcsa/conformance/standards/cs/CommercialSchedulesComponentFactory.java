package org.dcsa.conformance.standards.cs;

import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;

import java.util.*;

public class CommercialSchedulesComponentFactory extends AbstractComponentFactory {
  protected CommercialSchedulesComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
    super(standardName, standardVersion, scenarioSuite, "Publisher", "Consumer");
  }

  @Override
  public List<ConformanceParty> createParties(PartyConfiguration[] partyConfigurations, CounterpartConfiguration[] counterpartConfigurations, JsonNodeMap persistentMap, PartyWebClient webClient, Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    return null;
  }

  @Override
  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(PartyConfiguration[] partyConfigurations, CounterpartConfiguration[] counterpartConfigurations) {
    return null;
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return null;
  }

  @Override
  public Set<String> getReportRoleNames(PartyConfiguration[] partyConfigurations, CounterpartConfiguration[] counterpartConfigurations) {
    return null;
  }
}
