package org.dcsa.conformance.standards.eblinterop;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.eblinterop.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.eblinterop.party.PintReceivingPlatform;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;
import org.dcsa.conformance.standards.eblinterop.party.PintSendingPlatform;

public class PintComponentFactory extends AbstractComponentFactory {
  public static final String STANDARD_NAME = "PINT";
  public static final List<String> STANDARD_VERSIONS = List.of("3.0.0");

  private static final String SENDING_PLATFORM_AUTH_HEADER_VALUE = UUID.randomUUID().toString();
  private static final String RECEIVING_PLATFORM_AUTH_HEADER_VALUE = UUID.randomUUID().toString();

  private final String standardVersion;

  public PintComponentFactory(String standardVersion) {
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
      PartyWebClient asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    Map<String, PartyConfiguration> partyConfigurationsByRoleName =
        Arrays.stream(partyConfigurations)
            .collect(Collectors.toMap(PartyConfiguration::getRole, Function.identity()));
    Map<String, CounterpartConfiguration> counterpartConfigurationsByRoleName =
        Arrays.stream(counterpartConfigurations)
            .collect(Collectors.toMap(CounterpartConfiguration::getRole, Function.identity()));

    LinkedList<ConformanceParty> parties = new LinkedList<>();

    PartyConfiguration sendingPlatformConfiguration =
        partyConfigurationsByRoleName.get(PintRole.SENDING_PLATFORM.getConfigName());
    if (sendingPlatformConfiguration != null) {
      parties.add(
          new PintSendingPlatform(
              standardVersion,
              sendingPlatformConfiguration,
              counterpartConfigurationsByRoleName.get(PintRole.RECEIVING_PLATFORM.getConfigName()),
              persistentMap,
              asyncWebClient,
              orchestratorAuthHeader,
              PayloadSignerFactory.senderPayloadSigner()
          ));
    }

    PartyConfiguration receivingPlatformConfiguration =
        partyConfigurationsByRoleName.get(PintRole.RECEIVING_PLATFORM.getConfigName());
    if (receivingPlatformConfiguration != null) {
      parties.add(
          new PintReceivingPlatform(
              standardVersion,
              receivingPlatformConfiguration,
              counterpartConfigurationsByRoleName.get(PintRole.SENDING_PLATFORM.getConfigName()),
              persistentMap,
              asyncWebClient,
              orchestratorAuthHeader,
              PayloadSignerFactory.receiverPayloadSigner()
          ));
    }

    return parties;
  }

  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return PintScenarioListBuilder.createModuleScenarioListBuilders(
        this.standardVersion,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, PintRole::isSendingPlatform),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, PintRole::isReceivingPlatform));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(PintRole.values())
        .map(PintRole::getConfigName)
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public Set<String> getReportRoleNames(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == PintRole.values().length
            ? Arrays.stream(PintRole.values()).map(PintRole::getConfigName)
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
        "/standards/pint/sandboxes/%s.json"
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
            Map.entry("SENDING_PLATFORM_AUTH_HEADER_VALUE_PLACEHOLDER", SENDING_PLATFORM_AUTH_HEADER_VALUE),
            Map.entry("RECEIVING_PLATFORM_AUTH_HEADER_VALUE_PLACEHOLDER", RECEIVING_PLATFORM_AUTH_HEADER_VALUE),
            Map.entry(
                "SANDBOX_ID_PREFIX",
                AbstractComponentFactory._sandboxIdPrefix(STANDARD_NAME, standardVersion))));
  }
}
