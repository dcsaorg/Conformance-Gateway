package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Supplier;

import lombok.Getter;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;

@Getter
public class EblSurrenderV10Action extends ConformanceAction {
  protected final String srr;

  protected final Supplier<String> tdrSupplier;

  public EblSurrenderV10Action(
      String srr, Supplier<String> tdrSupplier, String sourcePartyName, String targetPartyName) {
    super(sourcePartyName, targetPartyName);
    this.srr = srr;
    this.tdrSupplier = tdrSupplier;
  }

  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("srr", srr).put("tdr", tdrSupplier.get());
  }
}
