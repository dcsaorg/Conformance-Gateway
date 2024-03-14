package org.dcsa.conformance.standards.eblinterop.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

@Getter
public class ResignLatestEntryAction extends PintAction {

  public ResignLatestEntryAction(
      String platformPartyName,
      String carrierPartyName,
      PintAction previousAction) {
    super(
        carrierPartyName,
        platformPartyName,
        previousAction,
        "ResignLatestEntry",
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
    return ("Resign the latest endorsement chain entry");
  }

}
