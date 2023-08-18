package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Supplier;

import lombok.Getter;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;

@Getter
public class TdrAction extends ConformanceAction {
  protected final Supplier<String> tdrSupplier;
  private final int expectedStatus;

  public TdrAction(
      Supplier<String> tdrSupplier,
      String sourcePartyName,
      String targetPartyName,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName);
    this.tdrSupplier = tdrSupplier;
    this.expectedStatus = expectedStatus;
  }

  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("tdr", tdrSupplier.get());
  }
}
