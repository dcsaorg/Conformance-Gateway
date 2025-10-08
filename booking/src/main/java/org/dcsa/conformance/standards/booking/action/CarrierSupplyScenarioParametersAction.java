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
import org.dcsa.conformance.core.util.ErrorFormatter;
import org.dcsa.conformance.standards.booking.checks.BookingInputPayloadValidations;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;

public class CarrierSupplyScenarioParametersAction extends BookingAction {

  private static final String SCENARIO_TYPE = "scenarioType";
  private static final String BOOKING_PAYLOAD = "bookingPayload";
  private static final String INPUT = "input";

  private JsonNode bookingPayload;
  private ScenarioType scenarioType;
  private final String standardVersion;
  private final JsonSchemaValidator requestSchemaValidator;

  public CarrierSupplyScenarioParametersAction(
      String carrierPartyName,
      @NonNull ScenarioType scenarioType,
      String standardVersion,
      JsonSchemaValidator requestSchemaValidator) {
    super(carrierPartyName, null, null, "SupplyCSP [%s]".formatted(scenarioType.name()), -1, true);
    this.scenarioType = scenarioType;
    this.standardVersion = standardVersion;
    this.requestSchemaValidator = requestSchemaValidator;
    this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
  }

  @Override
  public void reset() {
    super.reset();
    bookingPayload = null;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put(SCENARIO_TYPE, scenarioType.name());
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (bookingPayload != null) {
      jsonState.set(BOOKING_PAYLOAD, bookingPayload);
    }
    return jsonState.put(SCENARIO_TYPE, scenarioType.name());
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode bookingPayloadNode = jsonState.get(BOOKING_PAYLOAD);
    if (bookingPayloadNode != null) {
      bookingPayload = bookingPayloadNode;
    }
    this.scenarioType = ScenarioType.valueOf(jsonState.required(SCENARIO_TYPE).asText());
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(scenarioType, "prompt-carrier-supply-csp.md");
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

  /**
   * This method is overridden to handle the party input for the carrier supply scenario. It
   * validates the input against the schema and content checks, and throws a UserFacingException if
   * there are any validation errors.
   *
   * @param partyInput The input from the party, expected to contain a field named "input".
   * @throws UserFacingException if there are validation errors in the input.
   */
  @Override
  public void handlePartyInput(JsonNode partyInput) throws UserFacingException {
    JsonNode inputNode = partyInput.get(INPUT);

    Set<String> schemaChecksErrors =
        BookingInputPayloadValidations.validateBookingSchema(inputNode, requestSchemaValidator);

    Set<String> contentChecksErrors =
        BookingInputPayloadValidations.validateBookingContent(inputNode, getDspSupplier());

    Set<String> allErrors =
        Stream.of(schemaChecksErrors, contentChecksErrors)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

    if (!allErrors.isEmpty()) {
      throw new UserFacingException(ErrorFormatter.formatInputErrors(allErrors));
    }

    doHandlePartyInput(partyInput);
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    getBookingPayloadConsumer().accept(partyInput.get(INPUT));
  }

  @Override
  protected Consumer<JsonNode> getBookingPayloadConsumer() {
    return bkgPayload -> this.bookingPayload = bkgPayload;
  }

  @Override
  protected Supplier<JsonNode> getBookingPayloadSupplier() {
    return () -> bookingPayload;
  }
}
