package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.party.CarrierScenarioParameters;

public class Carrier_SupplyScenarioParametersAction extends EblAction {
  private CarrierScenarioParameters carrierScenarioParameters = null;

  private ScenarioType scenarioType;

  public Carrier_SupplyScenarioParametersAction(String carrierPartyName, @NonNull ScenarioType scenarioType) {
    super(
      carrierPartyName,
      null,
      null,
      switch (scenarioType) {
        case REGULAR -> "SupplyCSP";
        case REEFER -> "SupplyCSP-AR";
        case DG -> "SupplyCSP-DG";
      },
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
      case REGULAR -> new CarrierScenarioParameters(
        "Booking Reference",
        "Commodity subreference for regular (non-DG, non-reefer) cargo",
        // Any valid regular equipment reference will do as an example.
        "NARU3472484",
        "DKAAR",
        "640510",
        "Shoes - black, 400 boxes",
        "ServiceContractReference-1234"
      );
      case REEFER -> new CarrierScenarioParameters(
        "Booking Reference",
        "Commodity subreference for cargo requiring an *active* reefer",
        // Any valid reefer equipment reference will do as an example.
        "KKFU6671914",
        "DKAAR",
        "04052090",
        "Dairy products",
        "ServiceContractReference-1234"
      );
      case DG -> new CarrierScenarioParameters(
        "Booking Reference",
        "Commodity subreference for dangerous goods cargo",
        // Any valid regular equipment reference will do as an example.
        "NARU3472484",
        "DKAAR",
        "293499",
        "Environmentally hazardous substance, liquid, N.O.S (Propiconazole)",
        "ServiceContractReference-1234"
      );
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
