package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Setter;
import org.dcsa.conformance.gateway.parties.ConformanceParty;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;

import java.util.function.Supplier;

public class EblSurrenderV10Action extends ConformanceAction {

  protected final Supplier<String> tdrSupplier;

  public EblSurrenderV10Action(
      Supplier<String> tdrSupplier, String sourcePartyName, String targetPartyName) {
    super(sourcePartyName, targetPartyName);
    this.tdrSupplier = tdrSupplier;
  }

  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("tdr", tdrSupplier.get());
  }
}
