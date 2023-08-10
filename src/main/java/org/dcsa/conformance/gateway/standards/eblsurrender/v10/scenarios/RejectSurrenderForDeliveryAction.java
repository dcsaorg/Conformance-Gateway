package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import java.util.function.Supplier;

public class RejectSurrenderForDeliveryAction extends EblSurrenderV10AsyncReplyAction {
    public RejectSurrenderForDeliveryAction(Supplier<String> srrSupplier, Supplier<String> tdrSupplier, String carrierPartyName, String platformPartyName) {
        super(srrSupplier, tdrSupplier, carrierPartyName, platformPartyName);
    }
}
