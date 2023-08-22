package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.Getter;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.dcsa.conformance.gateway.traffic.ConformanceExchange;

@Getter
public class SurrenderRequestAction extends TdrAction {
  private final boolean forAmendment;

  private final AtomicReference<String> surrenderRequestReference = new AtomicReference<>();

  private final Supplier<String> srrSupplier =
      () -> {
        String srr = surrenderRequestReference.get();
        if (srr == null) {
          throw new IllegalStateException();
        }
        return srr;
      };

  public SurrenderRequestAction(
      boolean forAmendment,
      String platformPartyName,
      String carrierPartyName,
      int expectedStatus,
      ConformanceAction previousAction) {
    super(platformPartyName, carrierPartyName, expectedStatus, previousAction);
    this.forAmendment = forAmendment;
  }

  @Override
  public ObjectNode asJsonNode() {
    // don't include srr because it's not known when this is sent out
    return super.asJsonNode().put("forAmendment", forAmendment);
  }

  @Override
  public boolean updateFromExchangeIfItMatches(ConformanceExchange exchange) {
    if (!Objects.equals(getSourcePartyName(), exchange.getSourcePartyName())) {
      return false;
    }
    JsonNode requestJsonNode = getRequestBody(exchange);
    boolean matches =
        stringAttributeEquals(requestJsonNode, "transportDocumentReference", tdrSupplier.get())
            && stringAttributeEquals(
                requestJsonNode, "surrenderRequestCode", forAmendment ? "AREQ" : "SREQ");
    if (matches) {
      this.surrenderRequestReference.set(requestJsonNode.get("surrenderRequestReference").asText());
    }
    return matches;
  }
}
