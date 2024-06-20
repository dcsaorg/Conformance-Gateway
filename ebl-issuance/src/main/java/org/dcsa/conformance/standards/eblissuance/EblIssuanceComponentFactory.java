package org.dcsa.conformance.standards.eblissuance;

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
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceCarrier;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuancePlatform;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

class EblIssuanceComponentFactory extends AbstractComponentFactory {
  EblIssuanceComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
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
        partyConfigurationsByRoleName.get(EblIssuanceRole.CARRIER.getConfigName());
    if (carrierConfiguration != null) {
      parties.add(
          new EblIssuanceCarrier(
              standardVersion,
              carrierConfiguration,
              counterpartConfigurationsByRoleName.get(EblIssuanceRole.PLATFORM.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader,
              PayloadSignerFactory.senderPayloadSigner()
            ));
    }

    PartyConfiguration platformConfiguration =
        partyConfigurationsByRoleName.get(EblIssuanceRole.PLATFORM.getConfigName());
    if (platformConfiguration != null) {
      parties.add(
          new EblIssuancePlatform(
              standardVersion,
              platformConfiguration,
              counterpartConfigurationsByRoleName.get(EblIssuanceRole.CARRIER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return EblIssuanceScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, EblIssuanceRole::isCarrier),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, EblIssuanceRole::isPlatform));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(EblIssuanceRole.values())
        .map(EblIssuanceRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == EblIssuanceRole.values().length
            ? Arrays.stream(EblIssuanceRole.values()).map(EblIssuanceRole::getConfigName)
            : Arrays.stream(counterpartConfigurations)
                .map(CounterpartConfiguration::getRole)
                .filter(
                    counterpartRole ->
                        Arrays.stream(partyConfigurations)
                            .map(PartyConfiguration::getRole)
                            .noneMatch(partyRole -> Objects.equals(partyRole, counterpartRole))))
        .collect(Collectors.toSet());
  }

  public JsonSchemaValidator getMessageSchemaValidator(String apiProviderRole, boolean forRequest, boolean issuanceManifest) {
    String schemaFilePath =
        "/standards/eblissuance/schemas/eblissuance-v%s-%s.json"
            .formatted(
                standardVersion, apiProviderRole.toLowerCase());
    String schemaName;
    if (standardVersion.startsWith("2.")) {
      schemaName = EblIssuanceRole.isCarrier(apiProviderRole)
        ? (forRequest ? "issuanceRequest" : null)
        : (forRequest ? "issuanceResponse" : null);
    } else {
      schemaName = EblIssuanceRole.isCarrier(apiProviderRole)
        ? (forRequest ? "IssuanceRequest" : null)
        : (forRequest ? "IssuanceResponse" : null);
      if (issuanceManifest) {
        schemaName = "IssuanceManifest";
      }
    }


    return JsonSchemaValidator.getInstance(schemaFilePath, schemaName);
  }
}
