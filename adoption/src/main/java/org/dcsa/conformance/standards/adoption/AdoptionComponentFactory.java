package org.dcsa.conformance.standards.adoption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
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
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.adoption.party.AdoptionAdopter;
import org.dcsa.conformance.standards.adoption.party.AdoptionDCSA;
import org.dcsa.conformance.standards.adoption.party.AdoptionRole;

class AdoptionComponentFactory extends AbstractComponentFactory {
  AdoptionComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
    super(
        standardName,
        standardVersion,
        scenarioSuite,
        AdoptionRole.ADOPTER.getConfigName(),
        AdoptionRole.DCSA.getConfigName());
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

    List<ConformanceParty> parties = new ArrayList<>();
    PartyConfiguration adopterConfiguration =
        partyConfigurationsByRoleName.get(AdoptionRole.ADOPTER.getConfigName());
    if (adopterConfiguration != null) {
      parties.add(
          new AdoptionAdopter(
              standardVersion,
              adopterConfiguration,
              counterpartConfigurationsByRoleName.get(AdoptionRole.DCSA.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration dcsaConfiguration =
        partyConfigurationsByRoleName.get(AdoptionRole.DCSA.getConfigName());
    if (dcsaConfiguration != null) {
      parties.add(
          new AdoptionDCSA(
              standardVersion,
              dcsaConfiguration,
              counterpartConfigurationsByRoleName.get(AdoptionRole.ADOPTER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return AdoptionScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, AdoptionRole::isAdopter),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, AdoptionRole::isDCSA));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(AdoptionRole.values())
        .map(AdoptionRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == AdoptionRole.values().length
            ? Arrays.stream(AdoptionRole.values()).map(AdoptionRole::getConfigName)
            : Arrays.stream(counterpartConfigurations)
                .map(CounterpartConfiguration::getRole)
                .filter(
                    counterpartRole ->
                        Arrays.stream(partyConfigurations)
                            .map(PartyConfiguration::getRole)
                            .noneMatch(partyRole -> Objects.equals(partyRole, counterpartRole))))
        .collect(Collectors.toSet());
  }

  public JsonSchemaValidator getMessageSchemaValidator() {
    String schemaFilePath =
        "/standards/adoption/schemas/adoption-%s_with_push-resolved.json"
            .formatted(standardVersion);
    return JsonSchemaValidator.getInstance(schemaFilePath, "AdoptionStats");
  }
}
