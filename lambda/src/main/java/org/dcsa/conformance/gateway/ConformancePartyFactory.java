package org.dcsa.conformance.gateway;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.gateway.configuration.StandardConfiguration;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties.EblSurrenderV10Carrier;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties.EblSurrenderV10Platform;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;

public enum ConformancePartyFactory {
  ; // no instances

  public static List<ConformanceParty> createParties(
      StandardConfiguration standardConfiguration,
      PartyConfiguration[] partyConfigurations,
      CounterpartConfiguration[] counterpartConfigurations,
      BiConsumer<ConformanceRequest, Consumer<ConformanceResponse>> asyncWebClient) {
    if ("EblSurrender".equals(standardConfiguration.getName())
        && "1.0.0".equals(standardConfiguration.getVersion())) {

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
                counterpartConfigurationsByRoleName.get(
                    EblSurrenderV10Role.PLATFORM.getConfigName()),
                asyncWebClient));
      }

      PartyConfiguration platformConfiguration =
          partyConfigurationsByRoleName.get(EblSurrenderV10Role.PLATFORM.getConfigName());
      if (platformConfiguration != null) {
        parties.add(
            new EblSurrenderV10Platform(
                platformConfiguration,
                counterpartConfigurationsByRoleName.get(
                    EblSurrenderV10Role.CARRIER.getConfigName()),
                asyncWebClient));
      }

      return parties;
    }
    throw new UnsupportedOperationException(
        "Unsupported standard: %s".formatted(standardConfiguration));
  }
}
