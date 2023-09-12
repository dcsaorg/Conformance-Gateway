package org.dcsa.conformance.standards.eblsurrender.v10.check;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

public class SurrenderRequestCheck extends TdrActionCheck {
  private final boolean forAmendment;

  public SurrenderRequestCheck(
      String title, ActionCheck parent, boolean forAmendment, int expectedStatus) {
    super(title, parent, expectedStatus);
    this.forAmendment = forAmendment;
  }

  @Override
  protected boolean isRelevantExchange(ConformanceExchange exchange) {
    return exchange.getResponse().statusCode() == expectedStatus
        && JsonToolkit.stringAttributeEquals(
            exchange.getRequest().message().body().getJsonBody(),
            "surrenderRequestCode",
            forAmendment ? "AREQ" : "SREQ");
  }

  @Override
  public boolean isRelevantForRole(String roleName) {
    return childrenStream().anyMatch(child -> child.isRelevantForRole(roleName))
        || (EblSurrenderV10Role.isCarrier(roleName) && hasNoChildren());
  }

  @Override
  protected boolean exchangeMatchesPreviousRequest(
      ConformanceExchange exchange, ConformanceExchange previousExchange) {
    return Objects.equals(
        getTdr(exchange.getRequest().message().body().getJsonBody()),
        getTdr(previousExchange.getRequest().message().body().getJsonBody()));
  }

  protected static String getTdr(JsonNode jsonRequest) {
    return JsonToolkit.getTextAttributeOrNull(jsonRequest, "transportDocumentReference");
  }
}