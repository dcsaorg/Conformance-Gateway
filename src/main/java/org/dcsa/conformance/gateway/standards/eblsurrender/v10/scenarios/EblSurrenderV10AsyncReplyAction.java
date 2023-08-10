package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import java.util.function.Supplier;
import org.dcsa.conformance.gateway.parties.ConformanceParty;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;

public class EblSurrenderV10AsyncReplyAction extends EblSurrenderV10Action {
    protected final Supplier<String> srrSupplier;
    public EblSurrenderV10AsyncReplyAction(Supplier<String> srrSupplier, Supplier<String> tdrSupplier, String sourcePartyName, String targetPartyName) {
        super(tdrSupplier, sourcePartyName, targetPartyName);
        this.srrSupplier = srrSupplier;
    }
}
