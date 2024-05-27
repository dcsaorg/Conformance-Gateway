package org.dcsa.conformance.standards.ovs;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.ovs.party.OvsPublisher;
import org.dcsa.conformance.standards.ovs.party.OvsRole;
import org.dcsa.conformance.standards.ovs.party.OvsSubscriber;

class OvsComponentFactory extends AbstractComponentFactory {
  OvsComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
    super(standardName, standardVersion, scenarioSuite, "Publisher", "Subscriber");
  }

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

    PartyConfiguration publisherConfiguration =
        partyConfigurationsByRoleName.get(OvsRole.PUBLISHER.getConfigName());
    if (publisherConfiguration != null) {
      parties.add(
          new OvsPublisher(
              standardVersion,
              publisherConfiguration,
              counterpartConfigurationsByRoleName.get(OvsRole.SUBSCRIBER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration subscriberConfiguration =
        partyConfigurationsByRoleName.get(OvsRole.SUBSCRIBER.getConfigName());
    if (subscriberConfiguration != null) {
      parties.add(
          new OvsSubscriber(
              standardVersion,
              subscriberConfiguration,
              counterpartConfigurationsByRoleName.get(OvsRole.PUBLISHER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return OvsScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, OvsRole::isPublisher),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, OvsRole::isSubscriber));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(OvsRole.values())
        .map(OvsRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == OvsRole.values().length
            ? Arrays.stream(OvsRole.values()).map(OvsRole::getConfigName)
            : Arrays.stream(counterpartConfigurations)
                .map(CounterpartConfiguration::getRole)
                .filter(
                    counterpartRole ->
                        Arrays.stream(partyConfigurations)
                            .map(PartyConfiguration::getRole)
                            .noneMatch(partyRole -> Objects.equals(partyRole, counterpartRole))))
        .collect(Collectors.toSet());
  }

  public JsonSchemaValidator getMessageSchemaValidator(String apiProviderRole, boolean forRequest) {
    String schemaFilePath =
        "/standards/ovs/schemas/ovs-%s-%s.json"
            .formatted(
                standardVersion.toLowerCase().replaceAll("[.-]", ""),
                apiProviderRole.toLowerCase());
    String schemaName =
        OvsRole.isPublisher(apiProviderRole) ? (forRequest ? null : "serviceSchedules") : null;
    return JsonSchemaValidator.getInstance(schemaFilePath, schemaName);
  }
}
