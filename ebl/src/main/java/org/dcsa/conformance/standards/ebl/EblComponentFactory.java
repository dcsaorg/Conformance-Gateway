package org.dcsa.conformance.standards.ebl;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceScenario;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.ebl.party.EblCarrier;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.EblShipper;

public class EblComponentFactory extends AbstractComponentFactory {
  public static final String STANDARD_NAME = "Ebl";
  public static final List<String> STANDARD_VERSIONS = List.of("3.0.0-Beta-1");

  private static final String CARRIER_AUTH_HEADER_VALUE = UUID.randomUUID().toString();
  private static final String SHIPPER_AUTH_HEADER_VALUE = UUID.randomUUID().toString();

  private static final ConcurrentHashMap<String, List<ConformanceScenario>> SCENARIO_CACHE = new ConcurrentHashMap<>();

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
        partyConfigurationsByRoleName.get(EblRole.CARRIER.getConfigName());
    if (carrierConfiguration != null) {
      parties.add(
          new EblCarrier(
              standardVersion,
              carrierConfiguration,
              counterpartConfigurationsByRoleName.get(EblRole.SHIPPER.getConfigName()),
              persistentMap,
              webClient,
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
              webClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return EblScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, EblRole::isCarrier),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, EblRole::isShipper)));
  }
  public ScenarioListBuilder<?> createScenarioListBuilder(
    PartyConfiguration[] partyConfigurations,
    CounterpartConfiguration[] counterpartConfigurations) {
    return new CachedEblScenarioListBuilder<>(standardVersion, () -> EblScenarioListBuilder.buildTree(
      this.standardVersion,
      _findPartyOrCounterpartName(
        partyConfigurations, counterpartConfigurations, EblRole::isCarrier),
      _findPartyOrCounterpartName(
        partyConfigurations, counterpartConfigurations, EblRole::isShipper)));
  }


  private static class CachedEblScenarioListBuilder<T extends ScenarioListBuilder<T>> extends ScenarioListBuilder<T> {

    private final Supplier<EblScenarioListBuilder> realScenarioBuilder;
    private final String standardVersion;

    protected CachedEblScenarioListBuilder(String standardVersion, Supplier<EblScenarioListBuilder> realScenarioBuilder) {
      super(Function.identity());
      this.standardVersion = standardVersion;
      this.realScenarioBuilder = realScenarioBuilder;
    }

    @Override
    protected List<ConformanceScenario> _buildScenarioList() {
      var scenarioList = SCENARIO_CACHE.get(standardVersion);
      if (scenarioList != null) {
        return scenarioList;
      }
      scenarioList = Collections.unmodifiableList(realScenarioBuilder.get().buildScenarioList());
      SCENARIO_CACHE.put(standardVersion, scenarioList);
      return scenarioList;
    }
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
