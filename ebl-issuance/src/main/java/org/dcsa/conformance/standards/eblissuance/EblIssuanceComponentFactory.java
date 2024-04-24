package org.dcsa.conformance.standards.eblissuance;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.SneakyThrows;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceCarrier;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuancePlatform;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

public class EblIssuanceComponentFactory extends AbstractComponentFactory {
  public static final String STANDARD_NAME = "eBL Issuance";
  public static final List<String> STANDARD_VERSIONS = List.of(
    "2.0.0-Beta-1",
    "3.0.0-Beta-1",
    "3.0.0-Beta-2"
  );

  private static final String CARRIER_AUTH_HEADER_VALUE = UUID.randomUUID().toString();
  private static final String PLATFORM_AUTH_HEADER_VALUE = UUID.randomUUID().toString();

  @Getter
  private final String standardVersion;

  public EblIssuanceComponentFactory(String standardVersion) {
    this.standardVersion = standardVersion;
    if (STANDARD_VERSIONS.stream().noneMatch(version -> version.equals(standardVersion))) {
      throw new IllegalArgumentException(
          "Unsupported standard version '%s'".formatted(standardVersion));
    }
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
              orchestratorAuthHeader));
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

  public JsonSchemaValidator getMessageSchemaValidator(String apiProviderRole, boolean forRequest) {
    String schemaFilePath =
        "/standards/eblissuance/schemas/eblissuance-v%s-%s.json"
            .formatted(
                standardVersion, apiProviderRole.toLowerCase());
    String schemaName;
    if (standardVersion.startsWith("2.") || standardVersion.equals("3.0.0-Beta-1")) {
      schemaName = EblIssuanceRole.isCarrier(apiProviderRole)
        ? (forRequest ? "issuanceRequest" : null)
        : (forRequest ? "issuanceResponse" : null);
    } else {
      schemaName = EblIssuanceRole.isCarrier(apiProviderRole)
        ? (forRequest ? "IssuanceRequest" : null)
        : (forRequest ? "IssuanceResponse" : null);
    }


    return JsonSchemaValidator.getInstance(schemaFilePath, schemaName);
  }

  @SneakyThrows
  public JsonNode getJsonSandboxConfigurationTemplate(
      String testedPartyRole, boolean isManual, boolean isTestingCounterpartsConfig) {
    return JsonToolkit.templateFileToJsonNode(
        "/standards/eblissuance/sandboxes/%s.json"
            .formatted(
                testedPartyRole == null
                    ? "auto-all-in-one"
                    : "%s-%s-%s"
                        .formatted(
                            isManual ? "manual" : "auto",
                            testedPartyRole.toLowerCase(),
                            isTestingCounterpartsConfig ? "testing-counterparts" : "tested-party")),
        Map.ofEntries(
            Map.entry("STANDARD_NAME_PLACEHOLDER", STANDARD_NAME),
            Map.entry("STANDARD_VERSION_PLACEHOLDER", standardVersion),
            Map.entry("CARRIER_AUTH_HEADER_VALUE_PLACEHOLDER", CARRIER_AUTH_HEADER_VALUE),
            Map.entry("PLATFORM_AUTH_HEADER_VALUE_PLACEHOLDER", PLATFORM_AUTH_HEADER_VALUE),
            Map.entry(
                "SANDBOX_ID_PREFIX",
                AbstractComponentFactory._sandboxIdPrefix(STANDARD_NAME, standardVersion))));
  }
}
