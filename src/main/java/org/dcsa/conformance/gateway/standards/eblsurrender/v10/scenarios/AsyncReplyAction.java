package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import java.util.function.Supplier;

public class AsyncReplyAction extends EblSurrenderV10Action {
    protected final Supplier<String> srrSupplier;
    public AsyncReplyAction(Supplier<String> srrSupplier, Supplier<String> tdrSupplier, String sourcePartyName, String targetPartyName) {
        super(tdrSupplier, sourcePartyName, targetPartyName);
        this.srrSupplier = srrSupplier;
    }
}
