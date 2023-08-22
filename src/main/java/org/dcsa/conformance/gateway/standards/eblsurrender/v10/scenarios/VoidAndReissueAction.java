package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import org.dcsa.conformance.gateway.scenarios.ConformanceAction;

import java.util.function.Supplier;

public class VoidAndReissueAction extends TdrAction {
  public VoidAndReissueAction(
      String carrierPartyName,
      String platformPartyName,
      ConformanceAction previousAction) {
    super(carrierPartyName, platformPartyName, -1, previousAction);
  }
}
