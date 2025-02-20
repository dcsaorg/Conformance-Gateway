package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
    // no markdown instructions needed: never expected to be performed by a human operator
    return ("Process and accept an out of band request for a change to the TD with reference %s"
        .formatted(getDspSupplier().get().transportDocumentReference()));
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    var node = super.asJsonNode()
      .put("documentReference", dsp.transportDocumentReference())
      .put("scenarioType", dsp.scenarioType().name());
    node.set("csp", getCspSupplier().get().toJson());
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
