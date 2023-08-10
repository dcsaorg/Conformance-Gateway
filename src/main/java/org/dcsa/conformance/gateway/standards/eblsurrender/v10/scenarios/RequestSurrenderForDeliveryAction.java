package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import java.util.function.Supplier;

public class RequestSurrenderForDeliveryAction extends EblSurrenderV10AsyncRequestAction {
  public RequestSurrenderForDeliveryAction(
      Supplier<String> tdrSupplier, String platformPartyName, String carrierPartyName) {
    super(tdrSupplier, platformPartyName, carrierPartyName);
  }
}
