package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.party.CarrierScenarioParameters;

public class Carrier_SupplyScenarioParametersAction extends EblAction {
  public static final String DKAAR = "DKAAR";
  public static final String BOOKING_REFERENCE = "Booking Reference";
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
    var csp =
        switch (scenarioType) {
          case REGULAR_SWB,
              REGULAR_STRAIGHT_BL,
              REGULAR_SWB_AMF,
              REGULAR_CLAD,
              REGULAR_NEGOTIABLE_BL ->
              new CarrierScenarioParameters(
                  BOOKING_REFERENCE,
                  "Commodity subreference for regular (non-DG, non-reefer) cargo",
                  null,
                  // Any valid regular equipment reference will do as an example.
                  "NARU3472484",
                  null,
                  DKAAR,
                  "640510",
                  null,
                  "Shoes - black, 400 boxes",
                  null,
                  "Fibreboard boxes");
          case REGULAR_NO_COMMODITY_SUBREFERENCE ->
              new CarrierScenarioParameters(
                  BOOKING_REFERENCE,
                  null,
                  null,
                  // Any valid regular equipment reference will do as an example.
                  "NARU3472484",
                  null,
                  DKAAR,
                  "640510",
                  null,
                  "Shoes - black, 400 boxes",
                  null,
                  "Fibreboard boxes");
          case ACTIVE_REEFER ->
              new CarrierScenarioParameters(
                  BOOKING_REFERENCE,
                  "Commodity subreference for cargo requiring an *active* reefer",
                  null,
                  // Any valid reefer equipment reference will do as an example.
                  "KKFU6671914",
                  null,
                  DKAAR,
                  "04052090",
                  null,
                  "Dairy products",
                  null,
                  "Bottles");
          case NON_OPERATING_REEFER ->
              new CarrierScenarioParameters(
                  BOOKING_REFERENCE,
                  "Commodity subreference for cargo requiring an non-operating reefer",
                  null,
                  // Any valid reefer equipment reference will do as an example.
                  "KKFU6671914",
                  null,
                  DKAAR,
                  "220299",
                  null,
                  "Non alcoholic beverages",
                  null,
                  "Bottles");
          case DG ->
              new CarrierScenarioParameters(
                  BOOKING_REFERENCE,
                  "Commodity subreference for dangerous goods cargo",
                  null,
                  // Any valid regular equipment reference will do as an example.
                  "NARU3472484",
                  null,
                  DKAAR,
                  "293499",
                  null,
                  "Environmentally hazardous substance",
                  null,
                  null);
          case REGULAR_2C_2U_1E, REGULAR_2C_2U_2E ->
              new CarrierScenarioParameters(
                  BOOKING_REFERENCE,
                  "Commodity Subreference for regular cargo 1",
                  "Commodity Subreference for regular cargo 2",
                  // Any valid reefer equipment reference will do as an example.
                  "MSKU3963442",
                  "MSKU7895860",
                  DKAAR,
                  "691110",
                  "732391",
                  "Tableware and kitchenware",
                  "Kitchen pots and pans",
                  "Fibreboard boxes");
          case REGULAR_SWB_SOC_AND_REFERENCES ->
              new CarrierScenarioParameters(
                  BOOKING_REFERENCE,
                  "Commodity Subreference for regular cargo",
                  null,
                  "ABCU7341935",
                  null,
                  DKAAR,
                  "691110",
                  null,
                  "Tableware and kitchenware",
                  null,
                  "Fibreboard boxes");
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
    validateCSR(carrierScenarioParameters, scenarioType);
  }

  private static void validateCSR(
      CarrierScenarioParameters carrierScenarioParameters, ScenarioType scenarioType) {
    validateRequiredField(
        carrierScenarioParameters.carrierBookingReference(), "Carrier Booking Reference");
    validateRequiredField(carrierScenarioParameters.equipmentReference(), "Equipment Reference");
    validateRequiredField(
        carrierScenarioParameters.invoicePayableAtUNLocationCode(),
        "Invoice Payable At UN Location Code");
    validateScenarioSpecificField(
        carrierScenarioParameters.equipmentReference2(),
        "Equipment Reference 2",
        scenarioType,
        ScenarioType.REGULAR_2C_2U_2E);
    validateRequiredField(carrierScenarioParameters.descriptionOfGoods(), "Description Of Goods");

    boolean isRequiredScenario = isRequiredScenarioType(scenarioType);
    validateConditionalField(
        carrierScenarioParameters.descriptionOfGoods2(),
        "Description Of Goods 2",
        isRequiredScenario);
    validateConditionalField(
        carrierScenarioParameters.outerPackagingDescription(),
        "Outer Packaging Description",
        !scenarioType.equals(ScenarioType.DG));
    validateRequiredField(
        carrierScenarioParameters.consignmentItemHSCode(), "Consignment Item HSCode");
    validateConditionalField(
        carrierScenarioParameters.consignmentItem2HSCode(),
        "Consignment Item HSCode 2",
        isRequiredScenario);
  }

  private static void validateRequiredField(String field, String fieldName) {
    if (field == null || field.isEmpty()) {
      throw new UserFacingException(fieldName + " is required and cannot be empty");
    }
  }

  private static void validateScenarioSpecificField(
      String field, String fieldName, ScenarioType currentScenario, ScenarioType requiredScenario) {
    if (currentScenario.equals(requiredScenario)) {
      validateRequiredField(field, fieldName);
    }
  }

  private static void validateConditionalField(String field, String fieldName, boolean condition) {
    if (condition) {
      validateRequiredField(field, fieldName);
    }
  }

  private static boolean isRequiredScenarioType(ScenarioType scenarioType) {
    return scenarioType == ScenarioType.REGULAR_2C_2U_2E
        || scenarioType == ScenarioType.REGULAR_2C_2U_1E;
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
