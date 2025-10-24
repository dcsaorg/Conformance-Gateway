package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Optional;
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
import org.dcsa.conformance.standards.ebl.checks.EblInputPayloadValidations;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standardscommons.action.BookingAndEblAction;

public class CarrierSupplyPayloadAction extends EblAction {

  public static final String CARRIER_PAYLOAD = "carrierPayload";
  private static final String SCENARIO_TYPE = "scenarioType";
  private static final String INPUT = "input";
  private static final String CBR_PLACEHOLDER = "{CBR}";
  private static final String DEFAULT_CBR = "BOOKING202507041234567890123456";

  private ScenarioType scenarioType;
  private JsonNode carrierPayload;
  private final String standardVersion;
  private final JsonSchemaValidator requestSchemaValidator;
  private final boolean isTd;

  public CarrierSupplyPayloadAction(
      String carrierPartyName, @NonNull ScenarioType scenarioType, String standardVersion, JsonSchemaValidator requestSchemaValidator, boolean isTd) {
    super(
        carrierPartyName,
        null,
        null,
        "SupplyCSP [%s]"
            .formatted(isTd ? scenarioType.getTDScenarioTypeName() : scenarioType.name()),
        -1,
        true);
    this.scenarioType = scenarioType;
    this.standardVersion = standardVersion;
    this.requestSchemaValidator = requestSchemaValidator;
    this.isTd = isTd;
    this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
  }

  public CarrierSupplyPayloadAction(
      String carrierPartyName,
      BookingAndEblAction previousAction,
      @NonNull ScenarioType scenarioType,
      String standardVersion,
      JsonSchemaValidator requestSchemaValidator,
      boolean isTd) {
    super(
        carrierPartyName,
        null,
        previousAction,
        "SupplyCSP [%s]".formatted(scenarioType.name()),
        -1,
        true);
    this.scenarioType = scenarioType;
    this.standardVersion = standardVersion;
    this.requestSchemaValidator = requestSchemaValidator;
    this.isTd = isTd;
    this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
  }

  @Override
  public void reset() {
    super.reset();
    carrierPayload = null;
    if (scenarioType != null) {
      this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode().put(SCENARIO_TYPE, scenarioType.name());
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (carrierPayload!= null) {
      jsonState.set(CARRIER_PAYLOAD, carrierPayload);
    }
    return jsonState.put(SCENARIO_TYPE, scenarioType.name());
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    JsonNode eblPayloadNode = jsonState.get(CARRIER_PAYLOAD);
    if (eblPayloadNode != null) {
      carrierPayload = eblPayloadNode;
    }
    this.scenarioType = ScenarioType.valueOf(jsonState.required(SCENARIO_TYPE).asText());
  }

  @Override
  public String getHumanReadablePrompt() {
    if (shouldIncludeCbr()) {
      return getMarkdownHumanReadablePrompt(
          Map.of("SCENARIO_TYPE", scenarioType.name(), CBR_PLACEHOLDER, getCbrValue()),
          "prompt-carrier-supply-csp-with-cbr.md");
    }
    return getMarkdownHumanReadablePrompt(
        Map.of("SCENARIO_TYPE", scenarioType.name()), "prompt-carrier-supply-csp.md");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return JsonToolkit.templateFileToJsonNode(
        "/standards/ebl/messages/" + scenarioType.eblPayload(standardVersion),
        Map.of(CBR_PLACEHOLDER, getCbrValue()));
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
        EblInputPayloadValidations.validateEblSchema(inputNode, requestSchemaValidator);

    Set<String> contentChecksErrors =
        EblInputPayloadValidations.validateEblContent(
            inputNode, scenarioType, isTd, getDspSupplier().get());

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
    getCarrierPayloadConsumer().accept(partyInput.get(INPUT));
  }

  @Override
  protected Consumer<JsonNode> getCarrierPayloadConsumer() {
    return carrierPayloadNode -> this.carrierPayload = carrierPayloadNode;
  }

  @Override
  protected Supplier<JsonNode> getCarrierPayloadSupplier() {
    return () -> carrierPayload;
  }

  private boolean shouldIncludeCbr() {
    return !(previousAction instanceof EblAction);
  }

  private String getCbrValue() {
    return shouldIncludeCbr()
        ? Optional.of(getBookingDspReference().get().carrierBookingReference()).orElse(DEFAULT_CBR)
        : DEFAULT_CBR;
  }
}
