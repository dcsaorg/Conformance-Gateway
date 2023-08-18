package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.Getter;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

@Getter
public class SurrenderResponseAction extends TdrAction {
  private final boolean accept;
  private final Supplier<String> srrSupplier;

  public SurrenderResponseAction(
      boolean accept,
      Supplier<String> srrSupplier,
      Supplier<String> tdrSupplier,
      String carrierPartyName,
      String platformPartyName,
      int expectedStatus) {
    super(tdrSupplier, carrierPartyName, platformPartyName, expectedStatus);
    this.accept = accept;
    this.srrSupplier = srrSupplier;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("srr", srrSupplier.get()).put("accept", accept);
  }

  @Override
  public boolean updateFromExchangeIfItMatches(ConformanceExchange exchange) {
    if (!Objects.equals(getSourcePartyName(), exchange.getSourcePartyName())) {
      return false;
    }
    JsonNode requestJsonNode = getRequestBody(exchange);
    return stringAttributeEquals(requestJsonNode, "surrenderRequestReference", srrSupplier.get())
        && stringAttributeEquals(requestJsonNode, "action", accept ? "SURR" : "SREJ");
  }
}
