package org.dcsa.conformance.standards.tnt;

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
import org.dcsa.conformance.standards.tnt.action.TntEventType;
import org.dcsa.conformance.standards.tnt.party.TntPublisher;
import org.dcsa.conformance.standards.tnt.party.TntRole;
import org.dcsa.conformance.standards.tnt.party.TntSubscriber;

class TntComponentFactory extends AbstractComponentFactory {
  TntComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
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
        partyConfigurationsByRoleName.get(TntRole.PUBLISHER.getConfigName());
    if (publisherConfiguration != null) {
      parties.add(
          new TntPublisher(
              standardVersion,
              publisherConfiguration,
              counterpartConfigurationsByRoleName.get(TntRole.SUBSCRIBER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration subscriberConfiguration =
        partyConfigurationsByRoleName.get(TntRole.SUBSCRIBER.getConfigName());
    if (subscriberConfiguration != null) {
      parties.add(
          new TntSubscriber(
              standardVersion,
              subscriberConfiguration,
              counterpartConfigurationsByRoleName.get(TntRole.PUBLISHER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return TntScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, TntRole::isPublisher),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, TntRole::isSubscriber));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(TntRole.values())
        .map(TntRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == TntRole.values().length
            ? Arrays.stream(TntRole.values()).map(TntRole::getConfigName)
            : Arrays.stream(counterpartConfigurations)
                .map(CounterpartConfiguration::getRole)
                .filter(
                    counterpartRole ->
                        Arrays.stream(partyConfigurations)
                            .map(PartyConfiguration::getRole)
                            .noneMatch(partyRole -> Objects.equals(partyRole, counterpartRole))))
        .collect(Collectors.toSet());
  }

  public Map<TntEventType, JsonSchemaValidator> getEventSchemaValidators() {
    String schemaFilePath = "/standards/tnt/schemas/tnt-220-publisher.json";
    return Map.ofEntries(
        Map.entry(
            TntEventType.EQUIPMENT,
            JsonSchemaValidator.getInstance(schemaFilePath, "equipmentEvent")),
        Map.entry(
            TntEventType.SHIPMENT,
            JsonSchemaValidator.getInstance(schemaFilePath, "shipmentEvent")),
        Map.entry(
            TntEventType.TRANSPORT,
            JsonSchemaValidator.getInstance(schemaFilePath, "transportEvent")));
  }
}
