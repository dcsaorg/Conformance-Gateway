package org.dcsa.conformance.end;

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
import org.dcsa.conformance.end.party.EndorsementChainCarrier;
import org.dcsa.conformance.end.party.EndorsementChainProvider;
import org.dcsa.conformance.end.party.EndorsementChainRole;

public class EndorsementChainComponentFactory extends AbstractComponentFactory {
  public EndorsementChainComponentFactory(
      String standardName, String standardVersion, String scenarioSuite) {
    super(
        standardName,
        standardVersion,
        scenarioSuite,
        EndorsementChainRole.PROVIDER.getConfigName(),
        EndorsementChainRole.CARRIER.getConfigName());
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
        partyConfigurationsByRoleName.get(EndorsementChainRole.PROVIDER.getConfigName());
    if (publisherConfiguration != null) {
      parties.add(
          new EndorsementChainProvider(
              standardVersion,
              publisherConfiguration,
              counterpartConfigurationsByRoleName.get(EndorsementChainRole.CARRIER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration consumerConfiguration =
        partyConfigurationsByRoleName.get(EndorsementChainRole.CARRIER.getConfigName());
    if (consumerConfiguration != null) {
      parties.add(
          new EndorsementChainCarrier(
              standardVersion,
              consumerConfiguration,
              counterpartConfigurationsByRoleName.get(
                  EndorsementChainRole.PROVIDER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }
    return parties;
  }

  @Override
  protected Map<String, EndorsementChainScenarioListBuilder> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      boolean isWithNotifications) {
    return EndorsementChainScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, EndorsementChainRole::isProvider),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, EndorsementChainRole::isCarrier));
  }


  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(EndorsementChainRole.values())
        .map(EndorsementChainRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  @Override
  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == EndorsementChainRole.values().length
            ? Arrays.stream(EndorsementChainRole.values()).map(EndorsementChainRole::getConfigName)
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
    String schemaFilePath =
        "/standards/end/schemas/end-v%s-openapi.yaml".formatted(standardVersion);
    return JsonSchemaValidator.getInstance(schemaFilePath, jsonSchema);
  }
}
