package org.dcsa.conformance.standards.eblinterop.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.OverwritingReference;
import org.dcsa.conformance.standards.eblinterop.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.eblinterop.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.eblinterop.models.DynamicScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.ReceiverScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.SenderScenarioParameters;

import java.util.Set;

public abstract class PintAction extends ConformanceAction {
  protected final int expectedStatus;
  private final OverwritingReference<DynamicScenarioParameters> dspReference;
  private final OverwritingReference<ReceiverScenarioParameters> rspReference;
  private final OverwritingReference<SenderScenarioParameters> sspReference;

  public PintAction(
      String sourcePartyName,
      String targetPartyName,
      PintAction previousAction,
      String actionTitle,
      int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle);
    this.expectedStatus = expectedStatus;
    if (previousAction == null) {
      this.dspReference =
          new OverwritingReference<>(null, new DynamicScenarioParameters(null, -1, Set.of(), null));
      this.rspReference = new OverwritingReference<>(null, new ReceiverScenarioParameters("", "", "", ""));
      this.sspReference = new OverwritingReference<>(null, new SenderScenarioParameters(null));
    } else {
      this.dspReference = new OverwritingReference<>(previousAction.dspReference, null);
      this.rspReference = new OverwritingReference<>(previousAction.rspReference, null);
      this.sspReference = new OverwritingReference<>(previousAction.sspReference, null);
    }
  }

  @Override
  public void reset() {
    super.reset();
    if (previousAction != null) {
      this.dspReference.set(null);
    }
  }


  public SignatureVerifier resolveSignatureVerifierSenderSignatures() {
    return PayloadSignerFactory.testKeySignatureVerifier();
  }

  public SignatureVerifier resolveSignatureVerifierForReceiverSignatures() {
    return PayloadSignerFactory.testKeySignatureVerifier();
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (dspReference.hasCurrentValue()) {
      jsonState.set("dspReference", dspReference.get().toJson());
    }
    if (rspReference.hasCurrentValue()) {
      jsonState.set("rspReference", rspReference.get().toJson());
    }
    if (sspReference.hasCurrentValue()) {
      jsonState.set("sspReference", sspReference.get().toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode dspNode = jsonState.get("dspReference");
    if (dspNode != null) {
      dspReference.set(DynamicScenarioParameters.fromJson(dspNode));
    }
    JsonNode rspNode = jsonState.get("rspReference");
    if (rspNode != null) {
      rspReference.set(ReceiverScenarioParameters.fromJson(rspNode));
    }
    JsonNode sspNode = jsonState.get("sspReference");
    if (sspNode != null) {
      sspReference.set(SenderScenarioParameters.fromJson(sspNode));
    }
  }


  public DynamicScenarioParameters getDsp() {
    return this.dspReference.get();
  }

  public void setDsp(DynamicScenarioParameters dsp) {
    this.dspReference.set(dsp);
  }

  public SenderScenarioParameters getSsp() {
    var previousAction = (PintAction)this.previousAction;
    if (previousAction != null) {
      return previousAction.getSsp();
    }
    return this.sspReference.get();
  }

  public void setSsp(SenderScenarioParameters sp) {
    var previousAction = (PintAction)this.previousAction;
    if (previousAction != null) {
      previousAction.setSsp(sp);
    } else {
      this.sspReference.set(sp);
    }
  }


  public ReceiverScenarioParameters getRsp() {
    var previousAction = (PintAction)this.previousAction;
    if (previousAction != null) {
      return previousAction.getRsp();
    }
    return this.rspReference.get();
  }

  public void setRsp(ReceiverScenarioParameters sp) {
    var previousAction = (PintAction)this.previousAction;
    if (previousAction != null) {
      previousAction.setRsp(sp);
    } else {
      this.rspReference.set(sp);
    }
  }
}
