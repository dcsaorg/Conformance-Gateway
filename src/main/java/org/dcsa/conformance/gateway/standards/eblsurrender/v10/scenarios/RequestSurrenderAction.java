package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

import java.util.function.Supplier;

public class RequestSurrenderAction extends AsyncRequestAction {
  @Getter private boolean forAmendment;

  public RequestSurrenderAction(
      boolean forAmendment,
      Supplier<String> tdrSupplier,
      String platformPartyName,
      String carrierPartyName) {
    super(tdrSupplier, platformPartyName, carrierPartyName);
    this.forAmendment = forAmendment;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("forAmendment", forAmendment);
  }
}
