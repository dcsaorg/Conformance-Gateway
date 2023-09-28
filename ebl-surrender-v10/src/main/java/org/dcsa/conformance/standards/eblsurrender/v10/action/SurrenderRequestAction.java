package org.dcsa.conformance.standards.eblsurrender.v10.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

@Getter
@Slf4j
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
    super(
        platformPartyName,
        carrierPartyName,
        expectedStatus,
        previousAction,
        "%s %d".formatted(forAmendment ? "AREQ" : "SREQ", expectedStatus));
    this.forAmendment = forAmendment;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    String srr = surrenderRequestReference.get();
    if (srr != null) {
      jsonState.put("surrenderRequestReference", srr);
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode srrNode = jsonState.get("surrenderRequestReference");
    if (srrNode != null) {
      surrenderRequestReference.set(srrNode.asText());
    }
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("Send a surrender request for %s "
            + "for the eBL with the transport document reference '%s'")
        .formatted(forAmendment ? "amendment" : "delivery", tdrSupplier.get());
  }

  @Override
  public synchronized Supplier<String> getSrrSupplier() {
    return srrSupplier;
  }

  @Override
  public ObjectNode asJsonNode() {
    // don't include srr because it's not known when this is sent out
    return super.asJsonNode().put("forAmendment", forAmendment);
  }

  @Override
  public boolean updateFromExchangeIfItMatches(ConformanceExchange exchange) {
    if (!Objects.equals(getSourcePartyName(), exchange.getRequest().message().sourcePartyName())) {
      return false;
    }
    JsonNode requestJsonNode = exchange.getRequest().message().body().getJsonBody();
    boolean matches =
        JsonToolkit.stringAttributeEquals(
                requestJsonNode, "transportDocumentReference", tdrSupplier.get())
            && JsonToolkit.stringAttributeEquals(
                requestJsonNode, "surrenderRequestCode", forAmendment ? "AREQ" : "SREQ");
    if (matches) {
      String srr = requestJsonNode.get("surrenderRequestReference").asText();
      log.info(
          "Updating SurrenderRequestAction '%s' with SRR '%s'".formatted(getActionTitle(), srr));
      this.surrenderRequestReference.set(srr);
    }
    return matches;
  }
}
