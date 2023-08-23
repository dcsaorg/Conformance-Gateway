package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.Getter;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.dcsa.conformance.gateway.toolkit.JsonToolkit;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

@Getter
public class SurrenderResponseAction extends TdrAction {
  private final boolean accept;
  private final Supplier<String> srrSupplier;

  public SurrenderResponseAction(
      boolean accept,
      String carrierPartyName,
      String platformPartyName,
      int expectedStatus,
      ConformanceAction previousAction) {
    super(carrierPartyName, platformPartyName, expectedStatus, previousAction);
    this.accept = accept;
    this.srrSupplier = _getSrrSupplier(previousAction);
  }

  private Supplier<String> _getSrrSupplier(ConformanceAction previousAction) {
    return previousAction instanceof SurrenderRequestAction surrenderRequestAction
            ? surrenderRequestAction.getSrrSupplier()
            : _getSrrSupplier(previousAction.getPreviousAction());
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
    return JsonToolkit.stringAttributeEquals(requestJsonNode, "surrenderRequestReference", srrSupplier.get())
        && JsonToolkit.stringAttributeEquals(requestJsonNode, "action", accept ? "SURR" : "SREJ");
  }
}
