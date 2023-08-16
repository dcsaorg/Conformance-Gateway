package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import java.util.function.Supplier;

public class AmendDocumentOfflineAction extends EblSurrenderV10Action {
  public AmendDocumentOfflineAction(
      String srr, Supplier<String> tdrSupplier, String carrierPartyName, String platformPartyName) {
    super(srr, tdrSupplier, carrierPartyName, platformPartyName);
  }
}
