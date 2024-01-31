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
      "SupplyCSP [%s]".formatted(scenarioType.name()),
      -1);
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
      case REGULAR, REGULAR_SHIPPER_OWNED -> new CarrierScenarioParameters("Example Carrier Service",
        "402E",
        "service Name",
        "640510",
        "Shoes - black, 400 boxes",
        null,
        null,
        "DKAAR",
        "DEBRV");
      case REGULAR_2RE1C, REGULAR_2RE2C -> new CarrierScenarioParameters("Example Carrier Service",
        "402E",
        "service Name",
        "630260",
        "Tableware and kitchenware",
        "691010",
        "Kitchen pots and pans",
        "DKAAR",
        "DEBRV");
      case REGULAR_CHO_DEST -> new CarrierScenarioParameters("Example Carrier Service",
        "402E",
        "service Name",
        "640510",
        "Shoes - black, 400 boxes",
        null,
        null,
        "DKAAR",
        "USGBO");
      case REGULAR_CHO_ORIG -> new CarrierScenarioParameters("Example Carrier Service",
        "402E",
        "service Name",
        "640510",
        "Shoes - black, 400 boxes",
        null,
        null,
        "DKAAR",
        "DKAAR");
      case REGULAR_NON_OPERATING_REEFER -> new CarrierScenarioParameters("Example Carrier Service",
        "402E",
        "service Name",
        "220291",
        "Non alcoholic beverages",
        null,
        null,
        "DKAAR",
        "DEBRV");
      case REEFER, REEFER_TEMP_CHANGE -> new CarrierScenarioParameters("Example Carrier Service",
        "402E",
        "service Name",
        "04052090",
        "Dairy products",
        null,
        null,
        "DKAAR",
        "DEBRV");
      case DG -> new CarrierScenarioParameters("Example Carrier Service",
        "403W",
        "TA1",
        "293499",
        "Environmentally hazardous substance, liquid, N.O.S (Propiconazole)",
        null,
        null,
        "DKAAR",
        "DEBRV");
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
