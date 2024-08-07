package org.dcsa.conformance.standards.cs;

import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.cs.party.CsPublisher;
import org.dcsa.conformance.standards.cs.party.CsRole;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CsComponentFactory extends AbstractComponentFactory {
  protected CsComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
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
      partyConfigurationsByRoleName.get(CsRole.PUBLISHER.getConfigName());
    if (publisherConfiguration != null) {
      parties.add(
        new CsPublisher(
          standardVersion,
          publisherConfiguration,
          counterpartConfigurationsByRoleName.get(CsRole.SUBSCRIBER.getConfigName()),
          persistentMap,
          webClient,
          orchestratorAuthHeader));
    }

    PartyConfiguration consumerConfiguration =
      partyConfigurationsByRoleName.get(CsRole.PUBLISHER.getConfigName());
    if (consumerConfiguration != null) {
      parties.add(
        new CsPublisher(
          standardVersion,
          consumerConfiguration,
          counterpartConfigurationsByRoleName.get(CsRole.SUBSCRIBER.getConfigName()),
          persistentMap,
          webClient,
          orchestratorAuthHeader));
    }
    return parties;
  }


  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
    PartyConfiguration[] partyConfigurations,
    CounterpartConfiguration[] counterpartConfigurations) {
    return CsScenarioListBuilder.createModuleScenarioListBuilders(
      this,
      _findPartyOrCounterpartName(
        partyConfigurations, counterpartConfigurations, CsRole::isPublisher),
      _findPartyOrCounterpartName(
        partyConfigurations, counterpartConfigurations, CsRole::isSubscriber));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(CsRole.values())
      .map(CsRole::getConfigName)
      .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> getReportRoleNames(PartyConfiguration[] partyConfigurations, CounterpartConfiguration[] counterpartConfigurations)
    {
      return (partyConfigurations.length == CsRole.values().length
        ? Arrays.stream(CsRole.values()).map(CsRole::getConfigName)
        : Arrays.stream(counterpartConfigurations)
        .map(CounterpartConfiguration::getRole)
        .filter(
          counterpartRole ->
            Arrays.stream(partyConfigurations)
              .map(PartyConfiguration::getRole)
              .noneMatch(partyRole -> Objects.equals(partyRole, counterpartRole))))
        .collect(Collectors.toSet());
    }

  public JsonSchemaValidator getMessageSchemaValidator(String api, String jsonSchema) {
    String schemaFilePath = "/standards/commercialschedules/schemas/commercialschedules-%s-v%s0.json"
      .formatted(api, standardVersion.charAt(0));

    return JsonSchemaValidator.getInstance(schemaFilePath, jsonSchema);
  }

}
