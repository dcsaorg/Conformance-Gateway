package org.dcsa.conformance.standards.portcall;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.dcsa.conformance.core.AbstractComponentFactory;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.portcall.party.PortCallConsumer;
import org.dcsa.conformance.standards.portcall.party.PortCallProducer;
import org.dcsa.conformance.standards.portcall.party.PortCallRole;

public class PortCallComponentFactory extends AbstractComponentFactory {

  public PortCallComponentFactory(String standardName, String standardVersion, String scenarioSuite) {
    super(
      standardName,
      standardVersion,
      scenarioSuite,
      PortCallRole.PRODUCER.getConfigName(),
      PortCallRole.CONSUMER.getConfigName());
  }

  @Override
  public List<ConformanceParty> createParties(PartyConfiguration[] partyConfigurations, CounterpartConfiguration[] counterpartConfigurations, JsonNodeMap persistentMap, PartyWebClient webClient, Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    Map<String, PartyConfiguration> partyConfigurationsByRoleName =
      Arrays.stream(partyConfigurations)
        .collect(Collectors.toMap(PartyConfiguration::getRole, Function.identity()));
    Map<String, CounterpartConfiguration> counterpartConfigurationsByRoleName =
      Arrays.stream(counterpartConfigurations)
        .collect(Collectors.toMap(CounterpartConfiguration::getRole, Function.identity()));

    List<ConformanceParty> parties = new LinkedList<>();

    PartyConfiguration producerConfiguration =
      partyConfigurationsByRoleName.get(PortCallRole.PRODUCER.getConfigName());
    if (producerConfiguration != null) {
      parties.add(
        new PortCallProducer(
          standardVersion,
          producerConfiguration,
          counterpartConfigurationsByRoleName.get(PortCallRole.CONSUMER.getConfigName()),
          persistentMap,
          webClient,
          orchestratorAuthHeader));
    }

    PartyConfiguration consumerConfiguration =
      partyConfigurationsByRoleName.get(PortCallRole.CONSUMER.getConfigName());
    if (consumerConfiguration != null) {
      parties.add(
          new PortCallConsumer(
              standardVersion,
              consumerConfiguration,
              counterpartConfigurationsByRoleName.get(PortCallRole.PRODUCER.getConfigName()),
              persistentMap,
              webClient,
              orchestratorAuthHeader));
    }
    return parties;
  }

  @Override
  protected Map<String, PortCallScenarioListBuilder> createModuleScenarioListBuilders(
    PartyConfiguration[] partyConfigurations,
    CounterpartConfiguration[] counterpartConfigurations,
    boolean isWithNotifications) {

    return PortCallScenarioListBuilder.createModuleScenarioListBuilders(
      this,
      getReportRoleNames(partyConfigurations, counterpartConfigurations),
      _findPartyOrCounterpartName(
        partyConfigurations, counterpartConfigurations, PortCallRole::isProducer),
      _findPartyOrCounterpartName(
        partyConfigurations, counterpartConfigurations, PortCallRole::isConsumer));
  }

  @Override
  public SortedSet<String> getRoleNames() {
    return Arrays.stream(PortCallRole.values())
      .map(PortCallRole::getConfigName)
      .collect(Collectors.toCollection(TreeSet::new));
  }

  @Override
  public Set<String> getReportRoleNames(PartyConfiguration[] partyConfigurations, CounterpartConfiguration[] counterpartConfigurations) {
    return (partyConfigurations.length == PortCallRole.values().length
      ? Arrays.stream(PortCallRole.values()).map(PortCallRole::getConfigName)
      : Arrays.stream(counterpartConfigurations)
      .map(CounterpartConfiguration::getRole)
      .filter(
        counterpartRole ->
          Arrays.stream(partyConfigurations)
            .map(PartyConfiguration::getRole)
            .noneMatch(partyRole -> Objects.equals(partyRole, counterpartRole))))
      .collect(Collectors.toSet());
  }

  public JsonSchemaValidator getMessageSchemaValidator(String jsonSchema) {
    String schemaFilePath =
        "/standards/portcall/schemas/port-call-v%s-openapi.yaml".formatted(standardVersion);
    return JsonSchemaValidator.getInstance(schemaFilePath, jsonSchema);
  }
}
