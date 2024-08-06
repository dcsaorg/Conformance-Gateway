package org.dcsa.conformance.standards.cs;

import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.cs.party.CommercialSchedulesPublisher;
import org.dcsa.conformance.standards.cs.party.CommercialSchedulesRole;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommercialSchedulesComponentFactory extends AbstractComponentFactory {
  protected CommercialSchedulesComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
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
      partyConfigurationsByRoleName.get(CommercialSchedulesRole.PUBLISHER.getConfigName());
    if (publisherConfiguration != null) {
      parties.add(
        new CommercialSchedulesPublisher(
          standardVersion,
          publisherConfiguration,
          counterpartConfigurationsByRoleName.get(CommercialSchedulesRole.SUBSCRIBER.getConfigName()),
          persistentMap,
          webClient,
          orchestratorAuthHeader));
    }

    PartyConfiguration consumerConfiguration =
      partyConfigurationsByRoleName.get(CommercialSchedulesRole.PUBLISHER.getConfigName());
    if (consumerConfiguration != null) {
      parties.add(
        new CommercialSchedulesPublisher(
          standardVersion,
          consumerConfiguration,
          counterpartConfigurationsByRoleName.get(CommercialSchedulesRole.SUBSCRIBER.getConfigName()),
          persistentMap,
          webClient,
          orchestratorAuthHeader));
    }

    return parties;
  }

  @Override
  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(PartyConfiguration[] partyConfigurations, CounterpartConfiguration[] counterpartConfigurations) {
    return null;
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(CommercialSchedulesRole.values())
      .map(CommercialSchedulesRole::getConfigName)
      .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> getReportRoleNames(PartyConfiguration[] partyConfigurations, CounterpartConfiguration[] counterpartConfigurations)
    {
      return (partyConfigurations.length == CommercialSchedulesRole.values().length
        ? Arrays.stream(CommercialSchedulesRole.values()).map(CommercialSchedulesRole::getConfigName)
        : Arrays.stream(counterpartConfigurations)
        .map(CounterpartConfiguration::getRole)
        .filter(
          counterpartRole ->
            Arrays.stream(partyConfigurations)
              .map(PartyConfiguration::getRole)
              .noneMatch(partyRole -> Objects.equals(partyRole, counterpartRole))))
        .collect(Collectors.toSet());
    }

  public JsonSchemaValidator getMessageSchemaValidator(String apiName, String jsonSchema) {
    String schemaFilePath = "/standards/commercialschedules/schemas/commercialschedules-%s-v%s0.json"
      .formatted(apiName, standardVersion.charAt(0));

    return JsonSchemaValidator.getInstance(schemaFilePath, jsonSchema);
  }

}
