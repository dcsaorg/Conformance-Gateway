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
import org.dcsa.conformance.standards.an.party.OvsPublisher;
import org.dcsa.conformance.standards.an.party.OvsRole;
import org.dcsa.conformance.standards.an.party.OvsSubscriber;

public class OvsComponentFactory extends AbstractComponentFactory {
  public static final String STANDARD_NAME = "OVS";
  public static final List<String> STANDARD_VERSIONS = List.of("3.0.0");

  private static final String PUBLISHER_AUTH_HEADER_VALUE = UUID.randomUUID().toString();
  private static final String SUBSCRIBER_AUTH_HEADER_VALUE = UUID.randomUUID().toString();

  @Getter
  private final String standardVersion;

  public OvsComponentFactory(String standardVersion) {
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
        partyConfigurationsByRoleName.get(OvsRole.PUBLISHER.getConfigName());
    if (publisherConfiguration != null) {
      parties.add(
          new OvsPublisher(
              standardVersion,
              publisherConfiguration,
              counterpartConfigurationsByRoleName.get(OvsRole.SUBSCRIBER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration subscriberConfiguration =
        partyConfigurationsByRoleName.get(OvsRole.SUBSCRIBER.getConfigName());
    if (subscriberConfiguration != null) {
      parties.add(
          new OvsSubscriber(
              standardVersion,
              subscriberConfiguration,
              counterpartConfigurationsByRoleName.get(OvsRole.PUBLISHER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return OvsScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, OvsRole::isPublisher),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, OvsRole::isSubscriber));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(OvsRole.values())
        .map(OvsRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == OvsRole.values().length
            ? Arrays.stream(OvsRole.values()).map(OvsRole::getConfigName)
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
        "/standards/ovs/schemas/ovs-%s-%s.json"
            .formatted(
                standardVersion.toLowerCase().replaceAll("[.-]", ""),
                apiProviderRole.toLowerCase());
    String schemaName =
        OvsRole.isPublisher(apiProviderRole) ? (forRequest ? null : "serviceSchedules") : null;
    return JsonSchemaValidator.getInstance(schemaFilePath, schemaName);
  }

  @SneakyThrows
  public JsonNode getJsonSandboxConfigurationTemplate(
      String testedPartyRole, boolean isManual, boolean isTestingCounterpartsConfig) {
    return JsonToolkit.templateFileToJsonNode(
        "/standards/ovs/sandboxes/%s.json"
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
            Map.entry("PUBLISHER_AUTH_HEADER_VALUE_PLACEHOLDER", PUBLISHER_AUTH_HEADER_VALUE),
            Map.entry("SUBSCRIBER_AUTH_HEADER_VALUE_PLACEHOLDER", SUBSCRIBER_AUTH_HEADER_VALUE),
            Map.entry(
                "SANDBOX_ID_PREFIX",
                AbstractComponentFactory._sandboxIdPrefix(STANDARD_NAME, standardVersion))));
  }
}
