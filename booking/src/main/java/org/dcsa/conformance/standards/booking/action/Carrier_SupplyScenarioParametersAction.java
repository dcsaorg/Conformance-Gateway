package org.dcsa.conformance.standards.booking.action;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.standards.booking.checks.BookingInputPayloadValidations;
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
    JsonNode inputNode = partyInput.get("input");

    Set<String> schemaChecksErrors =
        BookingInputPayloadValidations.validateBookingSchema(inputNode, requestSchemaValidator);

    Set<String> contentChecksErrors =
        BookingInputPayloadValidations.validateBookingContent(inputNode, getDspSupplier());

    Set<String> scenarioTypeChecksErrors =
        BookingInputPayloadValidations.validateBookingScenarioType(inputNode, scenarioType);

    Set<String> allErrors =
        Stream.of(schemaChecksErrors, contentChecksErrors, scenarioTypeChecksErrors)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

    if (!allErrors.isEmpty()) {
      throw new UserFacingException(
          "The booking input has the following errors:\n\n"
              + allErrors.stream()
                  .map(error -> " \uD83D\uDEAB " + error.replace(": ", ""))
                  .collect(Collectors.joining("\n")));
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
