package org.dcsa.conformance.standards.eblsurrender.v10.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.Getter;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

@Getter
public class SurrenderResponseAction extends TdrAction {
  private final boolean accept;
  private Supplier<String> srrSupplier;

  public SurrenderResponseAction(
      boolean accept,
      String carrierPartyName,
      String platformPartyName,
      int expectedStatus,
      ConformanceAction previousAction) {
    super(
        carrierPartyName,
        platformPartyName,
        expectedStatus,
        previousAction,
        "%s %d".formatted(accept ? "SURR" : "SREJ", expectedStatus));
    this.accept = accept;
  }

  @Override
  public synchronized Supplier<String> getSrrSupplier() {
    if (srrSupplier != null) return srrSupplier;
    for (ConformanceAction action = this.previousAction;
        action != null;
        action = action.getPreviousAction()) {
      if (action instanceof TdrAction tdrAction) {
        if ((srrSupplier = tdrAction.getSrrSupplier()) != null) {
          return srrSupplier;
        }
      }
    }
    return () -> "*";
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("srr", getSrrSupplier().get()).put("accept", accept);
  }

  @Override
  public boolean updateFromExchangeIfItMatches(ConformanceExchange exchange) {
    if (!Objects.equals(getSourcePartyName(), exchange.getRequest().message().sourcePartyName())) {
      return false;
    }
    JsonNode requestJsonNode = exchange.getRequest().message().body().getJsonBody();
    String srr = getSrrSupplier().get();
    return ("*".equals(srr)
            || JsonToolkit.stringAttributeEquals(requestJsonNode, "surrenderRequestReference", srr))
        && JsonToolkit.stringAttributeEquals(requestJsonNode, "action", accept ? "SURR" : "SREJ");
  }
}
