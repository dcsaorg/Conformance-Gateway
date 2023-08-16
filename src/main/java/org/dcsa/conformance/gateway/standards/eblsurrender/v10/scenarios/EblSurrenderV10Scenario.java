package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.gateway.scenarios.ConformanceScenario;

@Getter
public class EblSurrenderV10Scenario extends ConformanceScenario {
  private final Supplier<String> tdrSupplier;

  public EblSurrenderV10Scenario(
      SupplyAvailableTdrAction supplyAvailableEblAction,
      EblSurrenderV10Action... remainingActions) {
    super(
        Stream.concat(Stream.of(supplyAvailableEblAction), Stream.of(remainingActions))
            .collect(Collectors.toList()));
    this.tdrSupplier = supplyAvailableEblAction.getTdrSupplier();
  }
}
