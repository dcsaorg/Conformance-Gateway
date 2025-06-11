package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;

public class Carrier_SupplyScenarioParametersAction extends BookingAction {

  private JsonNode bookingPayload = null;

  private ScenarioType scenarioType;

  public Carrier_SupplyScenarioParametersAction(
      String carrierPartyName, @NonNull ScenarioType scenarioType) {
    super(carrierPartyName, null, null, "SupplyCSP [%s]".formatted(scenarioType.name()), -1);
    this.scenarioType = scenarioType;
    this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType));
  }

  @Override
  public void reset() {
    super.reset();
    bookingPayload = null;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("scenarioType", scenarioType.name());
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (bookingPayload != null) {
      jsonState.set("bookingPayload", bookingPayload);
    }
    return jsonState.put("scenarioType", scenarioType.name());
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode bookingPayloadNode = jsonState.get("bookingPayload");
    if (bookingPayloadNode != null) {
      bookingPayload = bookingPayloadNode;
    }
    this.scenarioType = ScenarioType.valueOf(jsonState.required("scenarioType").asText());
  }

  @Override
  public String getHumanReadablePrompt() {
    // TODO: change to use the bookingPayload
    return getMarkdownHumanReadablePrompt("prompt-carrier-supply-csp.md");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return JsonToolkit.templateFileToJsonNode(
        "/standards/booking/payloads/" + scenarioType.bookingPayload(), Map.of());
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) throws UserFacingException {
    // Validate that the input is a valid CarrierScenarioParameters JSON object
    doHandlePartyInput(partyInput);
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    getCspConsumer().accept(partyInput.get("input"));
  }

  @Override
  protected Consumer<JsonNode> getCspConsumer() {
    return bkgPayload -> this.bookingPayload = bkgPayload;
  }

  @Override
  protected Supplier<JsonNode> getCspSupplier() {
    return () -> bookingPayload;
  }
}
