package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.dcsa.conformance.standards.booking.party.CarrierScenarioParameters;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;

public class Carrier_SupplyScenarioParametersAction extends BookingAction {
  private CarrierScenarioParameters carrierScenarioParameters = null;
  private DynamicScenarioParameters dynamicScenarioParameters = new DynamicScenarioParameters(null, null);

  public Carrier_SupplyScenarioParametersAction(String carrierPartyName) {
    super(carrierPartyName, null, null, "SupplyCSP", -1);
  }

  @Override
  public void reset() {
    super.reset();
    carrierScenarioParameters = null;
    dynamicScenarioParameters = new DynamicScenarioParameters(null, null);
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (carrierScenarioParameters != null) {
      jsonState.set("carrierScenarioParameters", carrierScenarioParameters.toJson());
    }
    if (dynamicScenarioParameters != null) {
      jsonState.set("dynamicScenarioParameters", dynamicScenarioParameters.toJson());
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
    JsonNode dspNode = jsonState.get("dynamicScenarioParameters");
    if (dspNode != null) {
      dynamicScenarioParameters = DynamicScenarioParameters.fromJson(dspNode);
    }
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Supply the parameters required by the scenario using the following format:";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return new CarrierScenarioParameters("Example Carrier Service", "1234567890").toJson();
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
  protected Consumer<DynamicScenarioParameters> getDspConsumer() {
    return dsp -> this.dynamicScenarioParameters = dsp;
  }

  @Override
  protected Supplier<CarrierScenarioParameters> getCspSupplier() {
    return () -> carrierScenarioParameters;
  }

  @Override
  protected Supplier<DynamicScenarioParameters> getDspSupplier() {
    return () -> dynamicScenarioParameters;
  }
}