package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.booking.checks.BookingChecks;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;

public class Carrier_SupplyScenarioParametersAction extends BookingAction {

  private JsonNode bookingPayload = null;

  private ScenarioType scenarioType;
  private final String standardVersion;
  private final JsonSchemaValidator requestSchemaValidator;

  public Carrier_SupplyScenarioParametersAction(
      String carrierPartyName,
      @NonNull ScenarioType scenarioType,
      String standardVersion,
      JsonSchemaValidator requestSchemaValidator) {
    super(carrierPartyName, null, null, "SupplyCSP [%s]".formatted(scenarioType.name()), -1);
    this.scenarioType = scenarioType;
    this.standardVersion = standardVersion;
    this.requestSchemaValidator = requestSchemaValidator;
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
        "/standards/booking/messages/" + scenarioType.bookingPayload(standardVersion), Map.of());
  }

  @Override
  public boolean isInputRequired() {
    return true;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) throws UserFacingException {
    List<String> requestContentChecksErrors =
        BookingChecks.requestContentChecks(
                getMatchedExchangeUuid(), standardVersion, getDspSupplier())
            .getResults()
            .stream()
            .flatMap(result -> result.getErrors().stream())
            .toList();

    if (!requestContentChecksErrors.isEmpty()) {
      throw new UserFacingException(
          "The booking request has the following errors: %s"
              .formatted(String.join(", ", requestContentChecksErrors)));
    }

    Set<String> schemaChecksErrors = requestSchemaValidator.validate(partyInput);

    if (!schemaChecksErrors.isEmpty()) {
      throw new UserFacingException(
          "The booking schema has the following errors: %s"
              .formatted(String.join(", ", schemaChecksErrors)));
    }

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
