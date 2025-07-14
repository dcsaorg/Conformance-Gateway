package org.dcsa.conformance.standards.an;


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
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.an.party.ANPublisher;
import org.dcsa.conformance.standards.an.party.ANRole;
import org.dcsa.conformance.standards.an.party.ANSubscriber;

public class ANComponentFactory extends AbstractComponentFactory {
  protected ANComponentFactory(String standardName, String standardVersion, String scenarioSuite, String roleOne, String roleTwo) {
    super(standardName, standardVersion, scenarioSuite, roleOne, roleTwo);
  }

  public ANComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
    super(
        standardName,
        standardVersion,
        scenarioSuite,
        ANRole.PUBLISHER.getConfigName(),
        ANRole.SUBSCRIBER.getConfigName());
  }

  @Override
  public List<ConformanceParty> createParties(PartyConfiguration[] partyConfigurations, CounterpartConfiguration[] counterpartConfigurations, JsonNodeMap persistentMap, PartyWebClient webClient, Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    Map<String, PartyConfiguration> partyConfigurationsByRoleName =
      Arrays.stream(partyConfigurations)
        .collect(Collectors.toMap(PartyConfiguration::getRole, Function.identity()));
    Map<String, CounterpartConfiguration> counterpartConfigurationsByRoleName =
      Arrays.stream(counterpartConfigurations)
        .collect(Collectors.toMap(CounterpartConfiguration::getRole, Function.identity()));

    List<ConformanceParty> parties = new LinkedList<>();

    PartyConfiguration publisherConfiguration =
      partyConfigurationsByRoleName.get(ANRole.PUBLISHER.getConfigName());
    if (publisherConfiguration != null) {
      parties.add(
        new ANPublisher(
          standardVersion,
          publisherConfiguration,
          counterpartConfigurationsByRoleName.get(ANRole.SUBSCRIBER.getConfigName()),
          persistentMap,
          webClient,
          orchestratorAuthHeader));
    }

    PartyConfiguration consumerConfiguration =
      partyConfigurationsByRoleName.get(ANRole.SUBSCRIBER.getConfigName());
    if (consumerConfiguration != null) {
      parties.add(
        new ANSubscriber(
          standardVersion,
          consumerConfiguration,
          counterpartConfigurationsByRoleName.get(ANRole.PUBLISHER.getConfigName()),
          persistentMap,
          webClient,
          orchestratorAuthHeader));
    }
    return parties;
  }

  @Override
  protected Map<String, ANScenarioListBuilder> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {

    return ANScenarioListBuilder.createModuleScenarioListBuilders(
      this,
      _findPartyOrCounterpartName(
        partyConfigurations, counterpartConfigurations, ANRole::isPublisher),
      _findPartyOrCounterpartName(
        partyConfigurations, counterpartConfigurations, ANRole::isSubscriber));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(ANRole.values())
      .map(ANRole::getConfigName)
      .collect(Collectors.toCollection(TreeSet::new));
  }

  @Override
  public Set<String> getReportRoleNames(PartyConfiguration[] partyConfigurations, CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == ANRole.values().length
            ? Arrays.stream(ANRole.values()).map(ANRole::getConfigName)
            : Arrays.stream(counterpartConfigurations)
                .map(CounterpartConfiguration::getRole)
                .filter(
                    counterpartRole ->
                        Arrays.stream(partyConfigurations)
                            .map(PartyConfiguration::getRole)
                            .noneMatch(partyRole -> Objects.equals(partyRole, counterpartRole))))
        .collect(Collectors.toSet());
  }

  public JsonSchemaValidator getMessageSchemaValidator(String jsonSchema) {
    String schemaFilePath = "/standards/an/schemas/AN_v%s.yaml".formatted(standardVersion);
    return JsonSchemaValidator.getInstance(schemaFilePath, jsonSchema);
  }
}
