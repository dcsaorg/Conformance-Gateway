package org.dcsa.conformance.gateway.standards.eblsurrender.v10.check;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import org.dcsa.conformance.gateway.check.ActionCheck;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.dcsa.conformance.gateway.toolkit.JsonToolkit;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

public class SurrenderResponseCheck extends TdrActionCheck {
  private final boolean accept;

  public SurrenderResponseCheck(
      String title, ActionCheck parent, boolean accept, int expectedStatus) {
    super(title, parent, expectedStatus);
    this.accept = accept;
  }

  @Override
  protected boolean isRelevantExchange(ConformanceExchange exchange) {
    return exchange.getResponseStatusCode() == expectedStatus
        && JsonToolkit.stringAttributeEquals(
            exchange.getJsonRequestBody(), "action", accept ? "SURR" : "SREJ");
  }

  @Override
  public boolean isRelevantForRole(String roleName) {
    return childrenStream().anyMatch(child -> child.isRelevantForRole(roleName))
        || (EblSurrenderV10Role.isPlatform(roleName) && hasNoChildren());
  }

  @Override
  protected boolean exchangeMatchesPreviousRequest(
      ConformanceExchange exchange, ConformanceExchange previousExchange) {
    return Objects.equals(
        getSrr(exchange.getJsonRequestBody()), getSrr(previousExchange.getJsonRequestBody()));
  }

  protected static String getSrr(JsonNode jsonRequest) {
    return JsonToolkit.getTextAttributeOrNull(jsonRequest, "surrenderRequestReference");
  }
}
