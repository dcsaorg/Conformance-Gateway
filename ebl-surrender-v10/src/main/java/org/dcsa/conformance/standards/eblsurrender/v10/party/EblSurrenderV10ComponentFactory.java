package org.dcsa.conformance.standards.eblsurrender.v10.party;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dcsa.conformance.core.ComponentFactory;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.standards.eblsurrender.v10.EblSurrenderV10ScenarioListBuilder;
import org.dcsa.conformance.standards.eblsurrender.v10.check.EblSurrenderV10ConformanceCheck;

public class EblSurrenderV10ComponentFactory implements ComponentFactory {

  public ConformanceCheck createConformanceCheck(ScenarioListBuilder<?> scenarioListBuilder) {
    return new EblSurrenderV10ConformanceCheck(scenarioListBuilder);
  }

  public List<ConformanceParty> createParties(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      Consumer<ConformanceRequest> asyncWebClient,
      BiConsumer<String, Consumer<ConformanceParty>> asyncPartyActionConsumer) {
    Map<String, PartyConfiguration> partyConfigurationsByRoleName =
        Arrays.stream(partyConfigurations)
            .collect(Collectors.toMap(PartyConfiguration::getRole, Function.identity()));
    Map<String, CounterpartConfiguration> counterpartConfigurationsByRoleName =
        Arrays.stream(counterpartConfigurations)
            .collect(Collectors.toMap(CounterpartConfiguration::getRole, Function.identity()));

    LinkedList<ConformanceParty> parties = new LinkedList<>();

    PartyConfiguration carrierConfiguration =
        partyConfigurationsByRoleName.get(EblSurrenderV10Role.CARRIER.getConfigName());
    if (carrierConfiguration != null) {
      parties.add(
          new EblSurrenderV10Carrier(
              carrierConfiguration,
              counterpartConfigurationsByRoleName.get(EblSurrenderV10Role.PLATFORM.getConfigName()),
              asyncWebClient,
              asyncPartyActionConsumer));
    }

    PartyConfiguration platformConfiguration =
        partyConfigurationsByRoleName.get(EblSurrenderV10Role.PLATFORM.getConfigName());
    if (platformConfiguration != null) {
      parties.add(
          new EblSurrenderV10Platform(
              platformConfiguration,
              counterpartConfigurationsByRoleName.get(EblSurrenderV10Role.CARRIER.getConfigName()),
              asyncWebClient,
              asyncPartyActionConsumer));
    }

    return parties;
  }

  public ScenarioListBuilder<?> createScenarioListBuilder(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    return EblSurrenderV10ScenarioListBuilder.buildTree(
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, EblSurrenderV10Role::isCarrier),
        _findPartyOrCounterpartName(
            partyConfigurations, counterpartConfigurations, EblSurrenderV10Role::isPlatform));
  }

  private static String _findPartyOrCounterpartName(
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      Predicate<String> rolePredicate) {
    return Stream.concat(
            Arrays.stream(partyConfigurations)
                .filter(partyConfiguration -> rolePredicate.test(partyConfiguration.getRole()))
                .map(PartyConfiguration::getName),
            Arrays.stream(counterpartConfigurations)
                .filter(
                    counterpartConfigurationConfiguration ->
                        rolePredicate.test(counterpartConfigurationConfiguration.getRole()))
                .map(CounterpartConfiguration::getName))
        .findFirst()
        .orElseThrow();
  }
}
