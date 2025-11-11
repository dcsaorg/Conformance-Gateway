package org.dcsa.conformance.standards.vgm;

import java.util.Arrays;
import java.util.Collection;
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
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.vgm.party.VgmProducer;
import org.dcsa.conformance.standards.vgm.party.VgmRole;

public class VgmComponentFactory extends AbstractComponentFactory {

  public VgmComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
    super(
        standardName,
        standardVersion,
        scenarioSuite,
        VgmRole.PRODUCER.getConfigName(),
        VgmRole.CONSUMER.getConfigName());
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

    List<ConformanceParty> parties = new LinkedList<>();

    PartyConfiguration publisherConfiguration =
        partyConfigurationsByRoleName.get(VgmRole.PRODUCER.getConfigName());
    if (publisherConfiguration != null) {
      parties.add(
          new VgmProducer(
              standardVersion,
              publisherConfiguration,
              counterpartConfigurationsByRoleName.get(VgmRole.CONSUMER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration consumerConfiguration =
        partyConfigurationsByRoleName.get(VgmRole.CONSUMER.getConfigName());
    if (consumerConfiguration != null) {
      parties.add(
          new VgmProducer(
              standardVersion,
              consumerConfiguration,
              counterpartConfigurationsByRoleName.get(VgmRole.PRODUCER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }
    return parties;
  }

  @Override
  protected Map<String, VgmScenarioListBuilder> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      boolean isWithNotifications) {
    return VgmScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, VgmRole::isProducer),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, VgmRole::isConsumer));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(VgmRole.values())
        .map(VgmRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  @Override
  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == VgmRole.values().length
            ? Arrays.stream(VgmRole.values()).map(VgmRole::getConfigName)
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
