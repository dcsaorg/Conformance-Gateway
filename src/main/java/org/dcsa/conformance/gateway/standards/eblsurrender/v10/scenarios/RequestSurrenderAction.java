package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.Getter;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

@Getter
public class RequestSurrenderAction extends EblSurrenderV10Action {
  private final boolean forAmendment;

  public RequestSurrenderAction(
      String srr,
      boolean forAmendment,
      Supplier<String> tdrSupplier,
      String platformPartyName,
      String carrierPartyName) {
    super(srr, tdrSupplier, platformPartyName, carrierPartyName);
    this.forAmendment = forAmendment;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("forAmendment", forAmendment);
  }

  @Override
  public boolean trafficExchangeMatches(ConformanceExchange exchange) {
    if (!Objects.equals(getSourcePartyName(), exchange.getSourcePartyName())) {
      return false;
    }
    JsonNode requestJsonNode = getRequestBody(exchange);
    return stringAttributeEquals(requestJsonNode, "transportDocumentReference", tdrSupplier.get())
        && stringAttributeEquals(
            requestJsonNode, "surrenderRequestCode", forAmendment ? "AREQ" : "SREQ");
  }
}
