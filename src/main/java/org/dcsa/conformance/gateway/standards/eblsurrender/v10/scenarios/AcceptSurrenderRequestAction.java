package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import java.util.function.Supplier;

public class AcceptSurrenderRequestAction extends AsyncReplyAction {
    public AcceptSurrenderRequestAction(Supplier<String> srrSupplier, Supplier<String> tdrSupplier, String carrierPartyName, String platformPartyName) {
        super(srrSupplier, tdrSupplier, carrierPartyName, platformPartyName);
    }
}
