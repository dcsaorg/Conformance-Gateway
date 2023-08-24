package org.dcsa.conformance.gateway.standards.eblsurrender.v10.check;

import com.fasterxml.jackson.databind.JsonNode;

import org.dcsa.conformance.gateway.check.ActionCheck;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.dcsa.conformance.gateway.toolkit.JsonToolkit;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

import java.util.Objects;

public class SurrenderRequestCheck extends TdrActionCheck {
  private final boolean forAmendment;

  public SurrenderRequestCheck(
      String title, ActionCheck parent, boolean forAmendment, int expectedStatus) {
    super(title, parent, expectedStatus, EblSurrenderV10Role::isCarrier);
    this.forAmendment = forAmendment;
  }

  @Override
  protected boolean isRelevantRequestType(JsonNode jsonRequest) {
    return JsonToolkit.stringAttributeEquals(
        jsonRequest, "surrenderRequestCode", forAmendment ? "AREQ" : "SREQ");
  }

  @Override
  protected boolean exchangeMatchesPreviousExchange(ConformanceExchange exchange, ConformanceExchange previousExchange) {
    return Objects.equals(
            getTdr(getJsonRequest(exchange)),
            getTdr(getJsonRequest(Objects.requireNonNull(previousExchange))));
  }

  @Override
  public boolean isRelevantForRole(String roleName) {
    return hasChildren() || EblSurrenderV10Role.isCarrier(roleName);
  }
}
