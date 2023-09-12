package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
public class VoidAndReissueAction extends TdrAction {

  private final Consumer<String> tdrConsumer =
      tdr -> {
        if (!Objects.equals(tdr, tdrSupplier.get())) {
          throw new UnsupportedOperationException(
              "Changing the transportDocumentReference during void/reissue is currently not supported");
        }
      };

  public VoidAndReissueAction(
      String carrierPartyName, String platformPartyName, ConformanceAction previousAction) {
    super(carrierPartyName, platformPartyName, -1, previousAction, "Void&Reissue");
  }

  @Override
  public Supplier<String> getSrrSupplier() {
    return null;
  }
}
