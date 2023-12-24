package org.dcsa.conformance.standards.tnt;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
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
import org.dcsa.conformance.standards.tnt.action.TntEventType;
import org.dcsa.conformance.standards.tnt.party.TntPublisher;
import org.dcsa.conformance.standards.tnt.party.TntRole;
import org.dcsa.conformance.standards.tnt.party.TntSubscriber;

public class TntComponentFactory extends AbstractComponentFactory {
  public static final String STANDARD_NAME = "TnT";
  public static final List<String> STANDARD_VERSIONS = List.of("2.2.0");

  private static final String PUBLISHER_AUTH_HEADER_VALUE = UUID.randomUUID().toString();
  private static final String SUBSCRIBER_AUTH_HEADER_VALUE = UUID.randomUUID().toString();

  private final String standardVersion;

  public TntComponentFactory(String standardVersion) {
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
      Consumer<ConformanceRequest> asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    Map<String, PartyConfiguration> partyConfigurationsByRoleName =
        Arrays.stream(partyConfigurations)
            .collect(Collectors.toMap(PartyConfiguration::getRole, Function.identity()));
    Map<String, CounterpartConfiguration> counterpartConfigurationsByRoleName =
        Arrays.stream(counterpartConfigurations)
            .collect(Collectors.toMap(CounterpartConfiguration::getRole, Function.identity()));

    LinkedList<ConformanceParty> parties = new LinkedList<>();

    PartyConfiguration publisherConfiguration =
        partyConfigurationsByRoleName.get(TntRole.PUBLISHER.getConfigName());
    if (publisherConfiguration != null) {
      parties.add(
          new TntPublisher(
              standardVersion,
              publisherConfiguration,
              counterpartConfigurationsByRoleName.get(TntRole.SUBSCRIBER.getConfigName()),
              persistentMap,
              asyncWebClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration subscriberConfiguration =
        partyConfigurationsByRoleName.get(TntRole.SUBSCRIBER.getConfigName());
    if (subscriberConfiguration != null) {
      parties.add(
          new TntSubscriber(
              standardVersion,
              subscriberConfiguration,
              counterpartConfigurationsByRoleName.get(TntRole.PUBLISHER.getConfigName()),
              persistentMap,
              asyncWebClient,
              orchestratorAuthHeader));
    }

    return parties;
  }

  public ScenarioListBuilder<?> createScenarioListBuilder(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return TntScenarioListBuilder.buildTree(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, TntRole::isPublisher),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, TntRole::isSubscriber));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(TntRole.values())
        .map(TntRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == TntRole.values().length
            ? Arrays.stream(TntRole.values()).map(TntRole::getConfigName)
            : Arrays.stream(counterpartConfigurations)
                .map(CounterpartConfiguration::getRole)
                .filter(
                    counterpartRole ->
                        Arrays.stream(partyConfigurations)
                            .map(PartyConfiguration::getRole)
                            .noneMatch(partyRole -> Objects.equals(partyRole, counterpartRole))))
        .collect(Collectors.toSet());
  }

  public Map<TntEventType, JsonSchemaValidator> getEventSchemaValidators() {
    String schemaFilePath = "/standards/tnt/schemas/tnt-v22-publisher.json";
    return Map.ofEntries(
        Map.entry(
            TntEventType.EQUIPMENT,
            JsonSchemaValidator.getInstance(schemaFilePath, "equipmentEvent")),
        Map.entry(
            TntEventType.SHIPMENT,
          JsonSchemaValidator.getInstance(schemaFilePath, "shipmentEvent")),
        Map.entry(
            TntEventType.TRANSPORT,
          JsonSchemaValidator.getInstance(schemaFilePath, "transportEvent")));
  }

  public JsonSchemaValidator getMessageSchemaValidator(String apiProviderRole, boolean forRequest) {
    String schemaFilePath =
        "/standards/tnt/schemas/tnt-%s-%s.json"
            .formatted(
                standardVersion.startsWith("2") ? "v22" : "v30", apiProviderRole.toLowerCase());
    String schemaName =
        TntRole.isPublisher(apiProviderRole) ? (forRequest ? null : "events") : null;
    return JsonSchemaValidator.getInstance(schemaFilePath, schemaName);
  }

  @SneakyThrows
  public JsonNode getJsonSandboxConfigurationTemplate(
      String testedPartyRole, boolean isManual, boolean isTestingCounterpartsConfig) {
    return JsonToolkit.templateFileToJsonNode(
        "/standards/tnt/sandboxes/%s.json"
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
