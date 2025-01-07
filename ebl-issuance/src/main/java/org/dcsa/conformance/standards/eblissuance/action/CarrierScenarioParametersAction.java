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
    return "Supply the certificate to be used for validating the signed content using the format below. Remove all newlines from the exported X509 .pem certificate, keep the header and footer intact, and insert the four characters '\\r\\n' before and after the single-line Base64 certificate content.";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return new CarrierScenarioParameters(
            "-----BEGIN CERTIFICATE-----\r\nREPLACE_THIS_WITH_THE_BASE64_PEM_CONTENT\r\n-----END CERTIFICATE-----")
        .toJson();
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) {
    CarrierScenarioParameters carrierScenarioParameters = CarrierScenarioParameters.fromJson(partyInput.get("input"));
    // validate the carrier signing key
    PayloadSignerFactory.verifierFromPemEncodedPublicKey(carrierScenarioParameters.carrierSigningKeyPEM());
    getCspConsumer().accept(carrierScenarioParameters);
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
