package org.dcsa.conformance.standards.eblsurrender;

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
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderCarrier;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderPlatform;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

class EblSurrenderComponentFactory extends AbstractComponentFactory {
  EblSurrenderComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
    super(standardName, standardVersion, scenarioSuite, "Carrier", "Platform");
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

    PartyConfiguration carrierConfiguration =
        partyConfigurationsByRoleName.get(EblSurrenderRole.CARRIER.getConfigName());
    if (carrierConfiguration != null) {
      parties.add(
          new EblSurrenderCarrier(
              standardVersion,
              carrierConfiguration,
              counterpartConfigurationsByRoleName.get(EblSurrenderRole.PLATFORM.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration platformConfiguration =
        partyConfigurationsByRoleName.get(EblSurrenderRole.PLATFORM.getConfigName());
    if (platformConfiguration != null) {
      parties.add(
          new EblSurrenderPlatform(
              standardVersion,
              platformConfiguration,
              counterpartConfigurationsByRoleName.get(EblSurrenderRole.CARRIER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  public Map<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return EblSurrenderScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, EblSurrenderRole::isCarrier),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, EblSurrenderRole::isPlatform));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(EblSurrenderRole.values())
        .map(EblSurrenderRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == EblSurrenderRole.values().length
            ? Arrays.stream(EblSurrenderRole.values()).map(EblSurrenderRole::getConfigName)
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
        "/standards/eblsurrender/schemas/EBL_SUR_v%s.yaml".formatted(standardVersion);
    String schemaName =
        EblSurrenderRole.isCarrier(apiProviderRole)
            ? (forRequest ? "SurrenderRequestDetails" : null)
            : (forRequest ? "SurrenderRequestAnswer" : null);

    if (schemaName == null) {
      throw new IllegalArgumentException(
          "No schema for " + apiProviderRole + " (forRequest=" + forRequest + ")");
    }
    return JsonSchemaValidator.getInstance(schemaFilePath, schemaName);
  }

  public JsonSchemaValidator getMessageSchemaValidator(String schemaName) {
    String schemaFilePath =
            "/standards/eblsurrender/schemas/EBL_SUR_v%s.yaml".formatted(standardVersion);
    return JsonSchemaValidator.getInstance(schemaFilePath, schemaName);
  }
}
