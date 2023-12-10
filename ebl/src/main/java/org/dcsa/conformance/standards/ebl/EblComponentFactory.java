package org.dcsa.conformance.standards.ebl;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.ebl.party.EblCarrier;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.EblShipper;

public class EblComponentFactory extends AbstractComponentFactory {
  public static final String STANDARD_NAME = "Ebl";
  public static final List<String> STANDARD_VERSIONS = List.of("3.0.0-Beta-1");

  private static final String CARRIER_AUTH_HEADER_VALUE = UUID.randomUUID().toString();
  private static final String SHIPPER_AUTH_HEADER_VALUE = UUID.randomUUID().toString();

  private final String standardVersion;

  public EblComponentFactory(String standardVersion) {
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
      BiConsumer<ConformanceRequest, Consumer<ConformanceResponse>> asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    Map<String, PartyConfiguration> partyConfigurationsByRoleName =
        Arrays.stream(partyConfigurations)
            .collect(Collectors.toMap(PartyConfiguration::getRole, Function.identity()));
    Map<String, CounterpartConfiguration> counterpartConfigurationsByRoleName =
        Arrays.stream(counterpartConfigurations)
            .collect(Collectors.toMap(CounterpartConfiguration::getRole, Function.identity()));

    LinkedList<ConformanceParty> parties = new LinkedList<>();

    PartyConfiguration carrierConfiguration =
        partyConfigurationsByRoleName.get(EblRole.CARRIER.getConfigName());
    if (carrierConfiguration != null) {
      parties.add(
          new EblCarrier(
              standardVersion,
              carrierConfiguration,
              counterpartConfigurationsByRoleName.get(EblRole.SHIPPER.getConfigName()),
              persistentMap,
              asyncWebClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration shipperConfiguration =
        partyConfigurationsByRoleName.get(EblRole.SHIPPER.getConfigName());
    if (shipperConfiguration != null) {
      parties.add(
          new EblShipper(
              standardVersion,
              shipperConfiguration,
              counterpartConfigurationsByRoleName.get(EblRole.CARRIER.getConfigName()),
              persistentMap,
              asyncWebClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  public ScenarioListBuilder<?> createScenarioListBuilder(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return EblScenarioListBuilder.buildTree(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, EblRole::isCarrier),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, EblRole::isShipper));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(EblRole.values())
        .map(EblRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == EblRole.values().length
            ? Arrays.stream(EblRole.values()).map(EblRole::getConfigName)
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
    String schemaFilePath = "/standards/ebl/schemas/ebl-%s-v%s0.json"
      .formatted(apiName, standardVersion.charAt(0));

    return JsonSchemaValidator.getInstance(schemaFilePath, jsonSchema);
  }

  @SneakyThrows
  public JsonNode getJsonSandboxConfigurationTemplate(
      String testedPartyRole, boolean isManual, boolean isTestingCounterpartsConfig) {
    return JsonToolkit.templateFileToJsonNode(
        "/standards/ebl/sandboxes/%s.json"
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
            Map.entry("SHIPPER_AUTH_HEADER_VALUE_PLACEHOLDER", SHIPPER_AUTH_HEADER_VALUE),
            Map.entry(
                "SANDBOX_ID_PREFIX",
                AbstractComponentFactory._sandboxIdPrefix(STANDARD_NAME, standardVersion))));
  }
}