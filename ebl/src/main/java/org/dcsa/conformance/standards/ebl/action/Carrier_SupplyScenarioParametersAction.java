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
      case REGULAR_SWB, REGULAR_STRAIGHT_BL, REGULAR_SWB_AMF, REGULAR_CLAD, REGULAR_NEGOTIABLE_BL -> new CarrierScenarioParameters(
        "Booking Reference",
        "Commodity subreference for regular (non-DG, non-reefer) cargo",
        null,
        // Any valid regular equipment reference will do as an example.
        "NARU3472484",
        null,
        "DKAAR",
        "640510",
        null,
        "Shoes - black, 400 boxes",
        null,
        "Fibreboard boxes"
      );
      case REGULAR_NO_COMMODITY_SUBREFERENCE -> new CarrierScenarioParameters(
        "Booking Reference",
        null,
        null,
        // Any valid regular equipment reference will do as an example.
        "NARU3472484",
        null,
        "DKAAR",
        "640510",
        null,
        "Shoes - black, 400 boxes",
        null,
        "Fibreboard boxes"
      );
      case ACTIVE_REEFER -> new CarrierScenarioParameters(
        "Booking Reference",
        "Commodity subreference for cargo requiring an *active* reefer",
        null,
        // Any valid reefer equipment reference will do as an example.
        "KKFU6671914",
        null,
        "DKAAR",
        "04052090",
        null,
        "Dairy products",
        null,
        "Bottles"
      );
      case NON_OPERATING_REEFER -> new CarrierScenarioParameters(
        "Booking Reference",
        "Commodity subreference for cargo requiring an non-operating reefer",
        null,
        // Any valid reefer equipment reference will do as an example.
        "KKFU6671914",
        null,
        "DKAAR",
        "220299",
        null,
        "Non alcoholic beverages",
        null,
        "Bottles"
      );
      case DG -> new CarrierScenarioParameters(
        "Booking Reference",
        "Commodity subreference for dangerous goods cargo",
        null,
        // Any valid regular equipment reference will do as an example.
        "NARU3472484",
        null,
        "DKAAR",
        "293499",
        null,
        "Environmentally hazardous substance",
        null,
        null
      );
      case REGULAR_2C_2U_1E -> new CarrierScenarioParameters(
        "Booking Reference",
        "Commodity Subreference for regular cargo 1",
        "Commodity Subreference for regular cargo 2",
        // Any valid reefer equipment reference will do as an example.
        "MSKU3963442",
        "MSKU7895860",
        "DKAAR",
        "691110",
        "732391",
        "Tableware and kitchenware",
        "Kitchen pots and pans",
        "Fibreboard boxes"
      );
      case REGULAR_2C_2U_2E -> new CarrierScenarioParameters(
        "Booking Reference",
        "Commodity Subreference for regular cargo 1",
        "Commodity Subreference for regular cargo 2",
        // Any valid reefer equipment reference will do as an example.
        "MSKU3963442",
        "MSKU7895860",
        "DKAAR",
        "691110",
        "732391",
        "Tableware and kitchenware",
        "Kitchen pots and pans",
        "Fibreboard boxes"
      );
      case REGULAR_SWB_SOC_AND_REFERENCES -> new CarrierScenarioParameters(
        "Booking Reference",
        "Commodity Subreference for regular cargo",
        null,
        null,
        null,
        "DKAAR",
        "691110",
        null,
        "Tableware and kitchenware",
        null,
        "Fibreboard boxes"
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
