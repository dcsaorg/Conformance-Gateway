package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.NonNull;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.party.CarrierScenarioParameters;

public class Carrier_SupplyScenarioParametersAction extends BookingAction {
  private CarrierScenarioParameters carrierScenarioParameters = null;

  private ScenarioType scenarioType;
  public Carrier_SupplyScenarioParametersAction(String carrierPartyName, @NonNull ScenarioType scenarioType) {
    super(carrierPartyName, null, null,
      switch (scenarioType) {
      case REGULAR -> "SupplyCSP";
      case REEFER -> "SupplyCSP-AR";
      case DG -> "SupplyCSP-DG";
    }, -1);
    this.scenarioType = scenarioType;
    this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType));
  }

  @Override
  public void reset() {
    super.reset();
    carrierScenarioParameters = null;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
      .put("scenarioType", scenarioType.name());
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (carrierScenarioParameters != null) {
      jsonState.set("carrierScenarioParameters", carrierScenarioParameters.toJson());
    }
    return jsonState.put("scenarioType", scenarioType.name());
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode cspNode = jsonState.get("carrierScenarioParameters");
    if (cspNode != null) {
      carrierScenarioParameters = CarrierScenarioParameters.fromJson(cspNode);
    }
    this.scenarioType = ScenarioType.valueOf(jsonState.required("scenarioType").asText());
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Supply the parameters required by the scenario using the following format:";
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    var csp = switch (scenarioType) {
      case REGULAR -> new CarrierScenarioParameters("Example Carrier Service",
        "1234567",
        "service Name",
        "411510",
        "commodity Type",
        "DKCPH",
        "FRPAR");
      case REEFER -> new CarrierScenarioParameters("Example Carrier Service",
        "1234567",
        "service Name",
        "411510",
        "commodity Type",
        "DKCPH",
        "FRPAR");
      case DG -> new CarrierScenarioParameters("Example Carrier Service",
        "1234567",
        "service Name",
        "411510",
        "commodity Type",
        "DKCPH",
        "FRPAR");
    };

    return csp.toJson();
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
}
