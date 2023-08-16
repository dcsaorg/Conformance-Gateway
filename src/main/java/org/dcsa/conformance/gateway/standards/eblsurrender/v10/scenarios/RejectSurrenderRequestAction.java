package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.function.Supplier;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

public class RejectSurrenderRequestAction extends EblSurrenderV10Action {
  public RejectSurrenderRequestAction(
      String srr, Supplier<String> tdrSupplier, String carrierPartyName, String platformPartyName) {
    super(srr, tdrSupplier, carrierPartyName, platformPartyName);
  }

  @Override
  public boolean trafficExchangeMatches(ConformanceExchange exchange) {
    if (!Objects.equals(getSourcePartyName(), exchange.getSourcePartyName())) {
      return false;
    }
    JsonNode requestJsonNode = getRequestBody(exchange);
    return stringAttributeEquals(requestJsonNode, "surrenderRequestReference", srr)
            && stringAttributeEquals(requestJsonNode, "action", "SREJ");
  }
}
