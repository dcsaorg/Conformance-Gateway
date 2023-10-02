package org.dcsa.conformance.standards.eblsurrender.v10.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Getter;
import org.dcsa.conformance.core.scenario.ConformanceAction;

public class SupplyAvailableTdrAction extends ConformanceAction {
  private String transportDocumentReference = null;

  @Getter
  private final Consumer<String> tdrConsumer =
      tdr -> {
        if (transportDocumentReference != null) {
          throw new IllegalStateException();
        }
        this.transportDocumentReference = tdr;
      };

  @Getter
  private final Supplier<String> tdrSupplier =
      () -> {
        if (transportDocumentReference == null) {
          throw new IllegalStateException();
        }
        return transportDocumentReference;
      };

  public SupplyAvailableTdrAction(String carrierPartyName, ConformanceAction previousAction) {
    super(carrierPartyName, null, previousAction, "SupplyTDR");
  }

  @Override
  public void reset() {
    super.reset();
    transportDocumentReference = null;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (transportDocumentReference != null) {
      jsonState.put("transportDocumentReference", transportDocumentReference);
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode tdrNode = jsonState.get("transportDocumentReference");
    if (tdrNode != null) {
      transportDocumentReference = tdrNode.asText();
    }
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Provide the transport document reference of an eBL "
        + "for which your party can receive and handle a surrender request.";
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    super.handlePartyInput(partyInput);
    getTdrConsumer().accept(partyInput.get("input").asText());
  }
}
