package org.dcsa.conformance.standards.an;

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
import org.dcsa.conformance.standards.an.party.ArrivalNoticeCarrier;
import org.dcsa.conformance.standards.an.party.ArrivalNoticeRole;
import org.dcsa.conformance.standards.an.party.ArrivalNoticeNotifyParty;

public class ArrivalNoticeComponentFactory extends AbstractComponentFactory {
  public static final String STANDARD_NAME = "Arrival Notice";
  public static final List<String> STANDARD_VERSIONS = List.of("1.0.0");

  private static final String CARRIER_AUTH_HEADER_VALUE = UUID.randomUUID().toString();
  private static final String NOTIFYPARTY_AUTH_HEADER_VALUE = UUID.randomUUID().toString();

  @Getter
  private final String standardVersion;

  public ArrivalNoticeComponentFactory(String standardVersion) {
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

    PartyConfiguration publisherConfiguration =
        partyConfigurationsByRoleName.get(ArrivalNoticeRole.CARRIER.getConfigName());
    if (publisherConfiguration != null) {
      parties.add(
          new ArrivalNoticeCarrier(
              standardVersion,
              publisherConfiguration,
              counterpartConfigurationsByRoleName.get(ArrivalNoticeRole.NOTIFYPARTY.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration subscriberConfiguration =
        partyConfigurationsByRoleName.get(ArrivalNoticeRole.NOTIFYPARTY.getConfigName());
    if (subscriberConfiguration != null) {
      parties.add(
          new ArrivalNoticeNotifyParty(
              standardVersion,
              subscriberConfiguration,
              counterpartConfigurationsByRoleName.get(ArrivalNoticeRole.CARRIER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return ArrivalNoticeScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, ArrivalNoticeRole::isCarrier),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, ArrivalNoticeRole::isNotifyParty));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(ArrivalNoticeRole.values())
        .map(ArrivalNoticeRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == ArrivalNoticeRole.values().length
            ? Arrays.stream(ArrivalNoticeRole.values()).map(ArrivalNoticeRole::getConfigName)
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
        "/standards/an/schemas/an-%s.json"
            .formatted(
                standardVersion.toLowerCase().replaceAll("[.-]", ""));
    return JsonSchemaValidator.getInstance(schemaFilePath, "ArrivalNotice");
  }

  @SneakyThrows
  public JsonNode getJsonSandboxConfigurationTemplate(
      String testedPartyRole, boolean isManual, boolean isTestingCounterpartsConfig) {
    return JsonToolkit.templateFileToJsonNode(
        "/standards/an/sandboxes/%s.json"
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
            Map.entry("NOTIFYPARTY_AUTH_HEADER_VALUE_PLACEHOLDER", NOTIFYPARTY_AUTH_HEADER_VALUE),
            Map.entry(
                "SANDBOX_ID_PREFIX",
                AbstractComponentFactory._sandboxIdPrefix(STANDARD_NAME, standardVersion))));
  }
}
