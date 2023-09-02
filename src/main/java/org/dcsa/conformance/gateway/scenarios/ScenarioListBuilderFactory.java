package org.dcsa.conformance.gateway.scenarios;

import java.util.Arrays;
import org.dcsa.conformance.gateway.configuration.CounterpartConfiguration;
import org.dcsa.conformance.gateway.configuration.StandardConfiguration;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties.EblSurrenderV10ScenarioListBuilder;

public enum ScenarioListBuilderFactory {
  ; // no instances

  public static ScenarioListBuilder<?> create(
      StandardConfiguration standardConfiguration,
      CounterpartConfiguration[] counterpartConfigurations) {
    if ("EblSurrender".equals(standardConfiguration.getName())
        && "1.0.0".equals(standardConfiguration.getVersion())) {

      return EblSurrenderV10ScenarioListBuilder.buildTree(
          Arrays.stream(counterpartConfigurations)
              .filter(
                  counterpartConfiguration ->
                      EblSurrenderV10Role.isCarrier(counterpartConfiguration.getRole()))
              .findFirst()
              .orElseThrow()
              .getName(),
          Arrays.stream(counterpartConfigurations)
              .filter(
                  counterpartConfiguration ->
                      EblSurrenderV10Role.isPlatform(counterpartConfiguration.getRole()))
              .findFirst()
              .orElseThrow()
              .getName());
    }
    throw new UnsupportedOperationException(
        "Unsupported standard '%s' version '%s'"
            .formatted(standardConfiguration.getName(), standardConfiguration.getVersion()));
  }
}
