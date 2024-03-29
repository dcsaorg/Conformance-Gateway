package org.dcsa.conformance.standards.jit;

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
import org.dcsa.conformance.standards.jit.party.JitPublisher;
import org.dcsa.conformance.standards.jit.party.JitRole;
import org.dcsa.conformance.standards.jit.party.JitSubscriber;

public class JitComponentFactory extends AbstractComponentFactory {
  public static final String STANDARD_NAME = "JIT";
  public static final List<String> STANDARD_VERSIONS = List.of("1.2.0-Beta-1");

  private static final String PUBLISHER_AUTH_HEADER_VALUE = UUID.randomUUID().toString();
  private static final String SUBSCRIBER_AUTH_HEADER_VALUE = UUID.randomUUID().toString();

  @Getter private final String standardVersion;

  public JitComponentFactory(String standardVersion) {
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
        partyConfigurationsByRoleName.get(JitRole.PUBLISHER.getConfigName());
    if (publisherConfiguration != null) {
      parties.add(
          new JitPublisher(
              standardVersion,
              publisherConfiguration,
              counterpartConfigurationsByRoleName.get(JitRole.SUBSCRIBER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration subscriberConfiguration =
        partyConfigurationsByRoleName.get(JitRole.SUBSCRIBER.getConfigName());
    if (subscriberConfiguration != null) {
      parties.add(
          new JitSubscriber(
              standardVersion,
              subscriberConfiguration,
              counterpartConfigurationsByRoleName.get(JitRole.PUBLISHER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return JitScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, JitRole::isPublisher),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, JitRole::isSubscriber));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(JitRole.values())
        .map(JitRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == JitRole.values().length
            ? Arrays.stream(JitRole.values()).map(JitRole::getConfigName)
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
        "/standards/jit/schemas/jit-%s-%s.json"
            .formatted(
                standardVersion.toLowerCase().replaceAll("[.-]", ""),
                apiProviderRole.toLowerCase());
    String schemaName =
        JitRole.isPublisher(apiProviderRole) ? (forRequest ? null : "operationsEvents") : null;
    return JsonSchemaValidator.getInstance(schemaFilePath, schemaName);
  }

  @SneakyThrows
  public JsonNode getJsonSandboxConfigurationTemplate(
      String testedPartyRole, boolean isManual, boolean isTestingCounterpartsConfig) {
    return JsonToolkit.templateFileToJsonNode(
        "/standards/jit/sandboxes/%s.json"
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
