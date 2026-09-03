package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
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
import org.dcsa.conformance.standards.ebl.models.CarrierShippingInstructions;
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
  private final boolean includeAmendment;

  public CarrierSupplyPayloadAction(
      String carrierPartyName,
      @NonNull ScenarioType scenarioType,
      String standardVersion,
      JsonSchemaValidator requestSchemaValidator,
      boolean isTd) {
    this(carrierPartyName, scenarioType, standardVersion, requestSchemaValidator, isTd, false);
  }

  public CarrierSupplyPayloadAction(
      String carrierPartyName,
      @NonNull ScenarioType scenarioType,
      String standardVersion,
      JsonSchemaValidator requestSchemaValidator,
      boolean isTd,
      boolean includeAmendment) {
    super(
        carrierPartyName,
        null,
        null,
        includeAmendment
            ? "SupplyCSP [any TD + any TD amendment]"
            : "SupplyCSP [%s]"
                .formatted(isTd ? scenarioType.tdScopeName() : scenarioType.name()),
        -1,
        true);
    this.scenarioType = scenarioType;
    this.standardVersion = standardVersion;
    this.requestSchemaValidator = requestSchemaValidator;
    this.isTd = isTd;
    this.includeAmendment = includeAmendment;
    initializeScenarioType();
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
    this.includeAmendment = false;
    this.getDspConsumer().accept(getDspSupplier().get().withScenarioType(scenarioType.name()));
  }

  @Override
  public void reset() {
    super.reset();
    carrierPayload = null;
    if (scenarioType != null) {
      initializeScenarioType();
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
        .put(SCENARIO_TYPE, scenarioType.name())
        .put("isTd", isTd)
        .put("includeAmendment", includeAmendment);
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (carrierPayload != null) {
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
    if (includeAmendment) {
      return getMarkdownHumanReadablePrompt(
          Map.of(), "prompt-carrier-supply-csp-any-td-amendment.md");
    }
    return shouldIncludeCbr()
        ? getMarkdownHumanReadablePrompt(
            Map.of("SCENARIO_TYPE", scenarioType.name(), CBR_PLACEHOLDER, getCbrValue()),
            "prompt-carrier-supply-csp-with-cbr.md")
        : getMarkdownHumanReadablePrompt(
            Map.of("SCENARIO_TYPE", scenarioType.name()), "prompt-carrier-supply-csp.md");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    JsonNode fixture = JsonToolkit.templateFileToJsonNode(
        "/standards/ebl/messages/" + scenarioType.eblPayload(standardVersion),
        Map.of(CBR_PLACEHOLDER, getCbrValue()));
    if (!isTd) {
      return fixture;
    }
    ObjectNode transportDocument =
        CarrierShippingInstructions.createTransportDocumentFromShippingInstructions(
            (ObjectNode) fixture, standardVersion, scenarioType);
    if (!includeAmendment) {
      return transportDocument;
    }
    ObjectNode amendment = transportDocument.deepCopy();
    amendment.put(
        "serviceContractReference",
        amendment.path("serviceContractReference").asText("Ref-123") + "-AMENDED");
    ObjectNode payload = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    payload.set("transportDocument", transportDocument);
    payload.set("amendedTransportDocument", amendment);
    return payload;
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
    ScenarioType inputScenarioType = inputScenarioType(inputNode);
    List<JsonNode> payloads =
        includeAmendment
            ? List.of(
                inputNode.path("transportDocument"), inputNode.path("amendedTransportDocument"))
            : List.of(inputNode);

    Set<String> schemaChecksErrors =
        payloads.stream()
            .flatMap(
                payload ->
                    EblInputPayloadValidations.validateEblSchema(payload, requestSchemaValidator)
                        .stream())
            .collect(Collectors.toSet());

    Set<String> contentChecksErrors =
        payloads.stream()
            .flatMap(
                payload ->
                    EblInputPayloadValidations.validateEblContent(
                            payload, inputScenarioType, isTd, getDspSupplier().get())
                        .stream())
            .collect(Collectors.toSet());

    Set<String> amendmentPairErrors = validateAmendmentPair(inputNode);

    Set<String> allErrors =
        Stream.of(schemaChecksErrors, contentChecksErrors, amendmentPairErrors)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

    if (!allErrors.isEmpty()) {
      throw new UserFacingException(ErrorFormatter.formatInputErrors(allErrors));
    }
    doHandlePartyInput(partyInput);
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    JsonNode input = partyInput.get(INPUT);
    getCarrierPayloadConsumer().accept(input);
    JsonNode td = includeAmendment ? input.path("transportDocument") : input;
    if (includeAmendment) {
      getDspConsumer()
          .accept(getDspSupplier().get().withScenarioType(inputScenarioType(td).name()));
    }
    if (isTd && td.has("transportDocumentReference")) {
      getDspConsumer()
          .accept(
              getDspSupplier()
                  .get()
                  .withTransportDocumentReference(
                      td.required("transportDocumentReference").asText()));
    }
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
    return previousAction != null && !(previousAction instanceof EblAction);
  }

  private void initializeScenarioType() {
    getDspConsumer()
        .accept(
            getDspSupplier()
                .get()
                .withScenarioType(includeAmendment ? null : scenarioType.name()));
  }

  ScenarioType inputScenarioType(JsonNode input) {
    if (!includeAmendment) {
      return scenarioType;
    }
    JsonNode transportDocument =
        input.has("transportDocument") ? input.path("transportDocument") : input;
    return switch (transportDocument.path("transportDocumentTypeCode").asText()) {
      case "SWB" -> ScenarioType.REGULAR_SWB;
      case "BOL" ->
          transportDocument.path("isToOrder").asBoolean(false)
              ? ScenarioType.REGULAR_NEGOTIABLE_BL
              : ScenarioType.REGULAR_STRAIGHT_BL;
      default -> scenarioType;
    };
  }

  Set<String> validateAmendmentPair(JsonNode input) {
    if (!includeAmendment) {
      return Set.of();
    }
    JsonNode transportDocument = input.path("transportDocument");
    JsonNode amendedTransportDocument = input.path("amendedTransportDocument");
    return Stream.of("transportDocumentTypeCode", "isToOrder", "transportDocumentReference")
        .filter(
            fieldName ->
                !transportDocument.path(fieldName).equals(amendedTransportDocument.path(fieldName)))
        .map(
            fieldName ->
                "The original and amended Transport Documents must have the same `%s` value."
                    .formatted(fieldName))
        .collect(Collectors.toSet());
  }

  private String getCbrValue() {
    return shouldIncludeCbr()
        ? Optional.ofNullable(getBookingDspReference().get().carrierBookingReference())
            .orElse(DEFAULT_CBR)
        : DEFAULT_CBR;
  }
}
