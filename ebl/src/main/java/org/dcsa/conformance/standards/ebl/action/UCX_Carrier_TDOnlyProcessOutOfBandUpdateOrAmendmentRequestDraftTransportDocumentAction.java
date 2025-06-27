package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import lombok.Getter;

@Getter
public class UCX_Carrier_TDOnlyProcessOutOfBandUpdateOrAmendmentRequestDraftTransportDocumentAction extends StateChangingSIAction {

  public UCX_Carrier_TDOnlyProcessOutOfBandUpdateOrAmendmentRequestDraftTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction) {
    super(carrierPartyName, shipperPartyName, previousAction, "TD Change (Out of Band)", 204);
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", getDSP().transportDocumentReference()), "prompt-carrier-oob.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    var node = super.asJsonNode()
      .put("documentReference", dsp.transportDocumentReference())
      .put("scenarioType", dsp.scenarioType().name());
    node.set("eblPayload", getCspSupplier().get());
    return node;
  }

  @Override
  public boolean isConfirmationRequired() {
    return true;
  }

  @Override
  public boolean isMissingMatchedExchange() {
    return false;
  }
}
