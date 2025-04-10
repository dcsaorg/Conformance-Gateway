package org.dcsa.conformance.standards.eblissuance.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.eblissuance.party.CarrierScenarioParameters;

public class CarrierScenarioParametersAction extends IssuanceAction {
  public static final String ACTION_TITLE = "Carrier scenario parameters";
  private CarrierScenarioParameters carrierScenarioParameters = null;

  public CarrierScenarioParametersAction(
      String sourcePartyName, String targetPartyName, IssuanceAction previousAction) {
    super(sourcePartyName, targetPartyName, previousAction, ACTION_TITLE, -1);
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
    return getMarkdownHumanReadablePrompt(null, "prompt-carrier-csp.md");
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
  protected void doHandlePartyInput(JsonNode partyInput) {
    CarrierScenarioParameters carrierScenarioParameters = CarrierScenarioParameters.fromJson(partyInput.get("input"));
    // validate the carrier signing key
    PayloadSignerFactory.verifierFromPemEncodedCertificate(
      carrierScenarioParameters.carriersX509SigningCertificateInPEMFormat(),
      "carriersX509SigningCertificateInPEMFormat"
    );
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
