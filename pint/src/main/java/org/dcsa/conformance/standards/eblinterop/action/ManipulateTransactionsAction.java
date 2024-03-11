package org.dcsa.conformance.standards.eblinterop.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class ManipulateTransactionsAction extends PintAction {

  public ManipulateTransactionsAction(
      String platformPartyName,
      String carrierPartyName,
      PintAction previousAction) {
    super(
        carrierPartyName,
        platformPartyName,
        previousAction,
        "ManipulateTransaction",
        -1);
  }

  @Override
  public ObjectNode asJsonNode() {
    var node = super.asJsonNode();
    node.set("rsp", getRsp().toJson());
    node.set("ssp", getSsp().toJson());
    node.set("dsp", getDsp().toJson());
    return node;
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }


  @Override
  public String getHumanReadablePrompt() {
    return ("Manipulate the latest transaction");
  }

}
