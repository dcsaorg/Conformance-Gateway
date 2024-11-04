package org.dcsa.conformance.standards.eblissuance.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.eblissuance.party.CarrierScenarioParameters;

public class CarrierScenarioParametersAction extends IssuanceAction {
  private CarrierScenarioParameters carrierScenarioParameters = null;

  public CarrierScenarioParametersAction(
      String sourcePartyName, String targetPartyName, IssuanceAction previousAction) {
    super(sourcePartyName, targetPartyName, previousAction, "Carrier scenario parameters", -1);
  }

  @Override
  public void reset() {
    super.reset();
    carrierScenarioParameters = null;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (carrierScenarioParameters != null) {
      jsonState.set("carrierScenarioParameters", carrierScenarioParameters.toJson());
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode cspNode = jsonState.get("carrierScenarioParameters");
    if (cspNode != null) {
      carrierScenarioParameters = CarrierScenarioParameters.fromJson(cspNode);
    }
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Supply the public key for validating the signed content using the following format:";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return new CarrierScenarioParameters("Provide the public key for validating the signed content in here.")
        .toJson();
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    super.handlePartyInput(partyInput);
    getCspConsumer().accept(CarrierScenarioParameters.fromJson(partyInput.get("input")));
  }

  @Override
  protected Consumer<CarrierScenarioParameters> getCspConsumer() {
    return csp -> this.carrierScenarioParameters = csp;
  }

  @Override
  protected Supplier<CarrierScenarioParameters> getCspSupplier() {
    return () -> carrierScenarioParameters;
  }

  @Override
  protected Supplier<String> getTdrSupplier() {
    return null;
  }
}
