package org.dcsa.conformance.standards.jit;

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
import org.dcsa.conformance.standards.jit.model.JitSchema;
import org.dcsa.conformance.standards.jit.party.JitConsumer;
import org.dcsa.conformance.standards.jit.party.JitProvider;
import org.dcsa.conformance.standards.jit.party.JitRole;

public class JitComponentFactory extends AbstractComponentFactory {
  JitComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
    super(
        standardName,
        standardVersion,
        scenarioSuite,
        JitRole.PROVIDER.getConfigName(),
        JitRole.CONSUMER.getConfigName());
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

    PartyConfiguration providerConfiguration =
        partyConfigurationsByRoleName.get(JitRole.PROVIDER.getConfigName());
    if (providerConfiguration != null) {
      parties.add(
          new JitProvider(
              standardVersion,
              providerConfiguration,
              counterpartConfigurationsByRoleName.get(JitRole.CONSUMER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }

    PartyConfiguration consumerConfiguration =
        partyConfigurationsByRoleName.get(JitRole.CONSUMER.getConfigName());
    if (consumerConfiguration != null) {
      parties.add(
          new JitConsumer(
              standardVersion,
              consumerConfiguration,
              counterpartConfigurationsByRoleName.get(JitRole.PROVIDER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }
    return parties;
  }

  public LinkedHashMap<String, ? extends ScenarioListBuilder<?>> createModuleScenarioListBuilders(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      boolean isWithNotifications) {
    return JitScenarioListBuilder.createModuleScenarioListBuilders(
        this,
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, JitRole::isProvider),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, JitRole::isConsumer));
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

  public JsonSchemaValidator getMessageSchemaValidator(JitSchema schemaName) {
    String schemaFilePath = "/standards/jit/schemas/JIT_v%s.yaml".formatted(standardVersion);
    return JsonSchemaValidator.getInstance(schemaFilePath, schemaName.getSchemaName());
  }
}
