package org.dcsa.conformance.standards.tnt.v300;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.tnt.v300.party.TntConsumer;
import org.dcsa.conformance.standards.tnt.v300.party.TntProducer;
import org.dcsa.conformance.standards.tnt.v300.party.TntRole;

public class TntComponentFactory extends AbstractComponentFactory {

  public TntComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
    super(
        standardName,
        standardVersion,
        scenarioSuite,
        TntRole.PRODUCER.getConfigName(),
        TntRole.CONSUMER.getConfigName());
  }

  @Override
  public List<ConformanceParty> createParties(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      JsonNodeMap persistentMap,
      PartyWebClient webClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    Map<String, PartyConfiguration> partyConfigurationsByRoleName =
        Arrays.stream(partyConfigurations)
            .collect(Collectors.toMap(PartyConfiguration::getRole, Function.identity()));
    Map<String, CounterpartConfiguration> counterpartConfigurationsByRoleName =
        Arrays.stream(counterpartConfigurations)
            .collect(Collectors.toMap(CounterpartConfiguration::getRole, Function.identity()));

    LinkedList<ConformanceParty> parties = new LinkedList<>();

    PartyConfiguration providerConfiguration =
        partyConfigurationsByRoleName.get(TntRole.PRODUCER.getConfigName());
    if (providerConfiguration != null) {
      parties.add(
          new TntProducer(
              standardVersion,
              providerConfiguration,
              counterpartConfigurationsByRoleName.get(TntRole.CONSUMER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration consumerConfiguration =
        partyConfigurationsByRoleName.get(TntRole.CONSUMER.getConfigName());
    if (consumerConfiguration != null) {
      parties.add(
          new TntConsumer(
              standardVersion,
              consumerConfiguration,
              counterpartConfigurationsByRoleName.get(TntRole.PRODUCER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  @Override
  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      boolean isWithNotifications) {
    return TntScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, TntRole::isProducer),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, TntRole::isConsumer));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(TntRole.values())
        .map(TntRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  @Override
  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length
                == TntRole.values().length
            ? Arrays.stream(TntRole.values())
                .map(TntRole::getConfigName)
            : Arrays.stream(counterpartConfigurations)
                .map(CounterpartConfiguration::getRole)
                .filter(
                    counterpartRole ->
                        Arrays.stream(partyConfigurations)
                            .map(PartyConfiguration::getRole)
                            .noneMatch(partyRole -> Objects.equals(partyRole, counterpartRole))))
        .collect(Collectors.toSet());
  }
}
