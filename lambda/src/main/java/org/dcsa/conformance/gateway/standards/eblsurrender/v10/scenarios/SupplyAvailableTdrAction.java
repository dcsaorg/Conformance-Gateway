package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;

public class SupplyAvailableTdrAction extends ConformanceAction {
  private String transportDocumentReference = null;

  @Getter
  private final Consumer<String> tdrConsumer =
      tdr -> {
        if (transportDocumentReference != null) {
          throw new IllegalStateException();
        }
        this.transportDocumentReference = tdr;
      };

  @Getter
  private final Supplier<String> tdrSupplier =
      () -> {
        if (transportDocumentReference == null) {
          throw new IllegalStateException();
        }
        return transportDocumentReference;
      };

  public SupplyAvailableTdrAction(String carrierPartyName, ConformanceAction previousAction) {
    super(carrierPartyName, null, previousAction, "SupplyTDR");
  }
}
