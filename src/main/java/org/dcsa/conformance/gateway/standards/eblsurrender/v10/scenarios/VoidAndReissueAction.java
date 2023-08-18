package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import java.util.function.Supplier;

public class VoidAndReissueAction extends TdrAction {
  public VoidAndReissueAction(
      Supplier<String> tdrSupplier, String carrierPartyName, String platformPartyName) {
    super(tdrSupplier, carrierPartyName, platformPartyName, -1);
  }
}
