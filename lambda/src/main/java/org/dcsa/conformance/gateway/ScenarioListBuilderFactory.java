package org.dcsa.conformance.gateway;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.gateway.configuration.StandardConfiguration;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.dcsa.conformance.standards.eblsurrender.v10.party.EblSurrenderV10ScenarioListBuilder;

public enum ScenarioListBuilderFactory {
  ; // no instances

  public static ScenarioListBuilder<?> create(
      StandardConfiguration standardConfiguration,
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations) {
    if ("EblSurrender".equals(standardConfiguration.getName())
        && "1.0.0".equals(standardConfiguration.getVersion())) {
      return EblSurrenderV10ScenarioListBuilder.buildTree(
          findPartyOrCounterpartName(
              partyConfigurations, counterpartConfigurations, EblSurrenderV10Role::isCarrier),
          findPartyOrCounterpartName(
              partyConfigurations, counterpartConfigurations, EblSurrenderV10Role::isPlatform));
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'"
            .formatted(standardConfiguration.getName(), standardConfiguration.getVersion()));
  }

  private static String findPartyOrCounterpartName(
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
