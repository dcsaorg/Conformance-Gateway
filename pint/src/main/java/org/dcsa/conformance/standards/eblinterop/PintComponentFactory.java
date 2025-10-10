package org.dcsa.conformance.standards.eblinterop;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.eblinterop.party.PintReceivingPlatform;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;
import org.dcsa.conformance.standards.eblinterop.party.PintSendingPlatform;

class PintComponentFactory extends AbstractComponentFactory {

  PintComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
    super(standardName, standardVersion, scenarioSuite, "SendingPlatform", "ReceivingPlatform");
  }

  @Override
  public List<ConformanceParty> createParties(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      JsonNodeMap persistentMap,
      PartyWebClient asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    Map<String, PartyConfiguration> partyConfigurationsByRoleName =
        Arrays.stream(partyConfigurations)
            .collect(Collectors.toMap(PartyConfiguration::getRole, Function.identity()));
    Map<String, CounterpartConfiguration> counterpartConfigurationsByRoleName =
        Arrays.stream(counterpartConfigurations)
            .collect(Collectors.toMap(CounterpartConfiguration::getRole, Function.identity()));

    LinkedList<ConformanceParty> parties = new LinkedList<>();

    PartyConfiguration sendingPlatformConfiguration =
        partyConfigurationsByRoleName.get(PintRole.SENDING_PLATFORM.getConfigName());
    if (sendingPlatformConfiguration != null) {
      parties.add(
          new PintSendingPlatform(
              standardVersion,
              sendingPlatformConfiguration,
              counterpartConfigurationsByRoleName.get(PintRole.RECEIVING_PLATFORM.getConfigName()),
              persistentMap,
              asyncWebClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration receivingPlatformConfiguration =
        partyConfigurationsByRoleName.get(PintRole.RECEIVING_PLATFORM.getConfigName());
    if (receivingPlatformConfiguration != null) {
      parties.add(
          new PintReceivingPlatform(
              standardVersion,
              receivingPlatformConfiguration,
              counterpartConfigurationsByRoleName.get(PintRole.SENDING_PLATFORM.getConfigName()),
              persistentMap,
              asyncWebClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  @Override
  public Map<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      boolean isWithNotifications) {
    return PintScenarioListBuilder.createModuleScenarioListBuilders(
        this.standardVersion,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, PintRole::isSendingPlatform),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, PintRole::isReceivingPlatform));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(PintRole.values())
        .map(PintRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  @Override
  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == PintRole.values().length
            ? Arrays.stream(PintRole.values()).map(PintRole::getConfigName)
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
