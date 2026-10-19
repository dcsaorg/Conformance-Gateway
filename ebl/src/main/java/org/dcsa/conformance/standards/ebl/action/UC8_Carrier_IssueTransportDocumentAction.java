package org.dcsa.conformance.standards.ebl.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.checks.TransportDocumentStatusScenario;
import org.dcsa.conformance.standardscommons.action.BookingAndEblAction;

@Getter
public class UC8_Carrier_IssueTransportDocumentAction extends CarrierNotificationEblAction {

  public static final String ACTION_TITLE = "UC8";

  private final JsonSchemaValidator requestSchemaValidator;
  private final ScenarioType standaloneScenarioType;

  public UC8_Carrier_IssueTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAndEblAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      boolean isWithNotifications) {
    this(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        requestSchemaValidator,
        isWithNotifications,
        null);
  }

  public UC8_Carrier_IssueTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAndEblAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      boolean isWithNotifications,
      ScenarioType standaloneScenarioType) {
    super(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        standaloneScenarioType == null
            ? ACTION_TITLE
            : "%s [%s]".formatted(ACTION_TITLE, standaloneScenarioType.tdScopeName()),
        204,
        isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.standaloneScenarioType = standaloneScenarioType;
    initializeStandaloneScope();
  }

  private void initializeStandaloneScope() {
    if (standaloneScenarioType != null) {
      getDspConsumer()
          .accept(
              getDSP()
                  .withScenarioType(standaloneScenarioType.name())
                  .withNewTransportDocumentContent(true));
    }
  }

  @Override
  public void reset() {
    super.reset();
    initializeStandaloneScope();
  }

  @Override
  public void importJsonState(JsonNode state) {
    super.importJsonState(state);
    initializeStandaloneScope();
  }

  @Override
  public boolean isInputRequired() {
    return standaloneScenarioType != null;
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) {
    getDspConsumer()
        .accept(
            getDSP()
                .withTransportDocumentReference(
                    partyInput.required("input").required("transportDocumentReference").asText()));
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return isInputRequired()
        ? OBJECT_MAPPER.createObjectNode().put("transportDocumentReference", "Insert TDR here")
        : null;
  }

  @Override
  public String getHumanReadablePrompt() {
    if (standaloneScenarioType != null) {
      return getMarkdownHumanReadablePrompt(
          Map.of(
              "DOCUMENT_TYPE_NAME",
              standaloneScenarioType.tdScopeName(),
              "DOCUMENT_TYPE_CODE",
              standaloneScenarioType.transportDocumentTypeCode(),
              "TO_ORDER_VALUE",
              Boolean.toString(standaloneScenarioType.isToOrder())),
          "prompt-carrier-uc8-standalone.md",
          "prompt-carrier-notification.md");
    }
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", getTransportDocumentReference()),
        "prompt-carrier-uc8.md",
        "prompt-carrier-notification.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    initializeStandaloneScope();
    ObjectNode prompt =
        super.asJsonNode().put("documentReference", getTransportDocumentReference());
    if (standaloneScenarioType != null) {
      prompt.put("scenarioType", standaloneScenarioType.name()).put("skipSI", true);
    }
    return prompt;
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    var dsp = getDspSupplier().get();
    String reference =
        exchange
            .getRequest()
            .message()
            .body()
            .getJsonBody()
            .path("data")
            .path("transportDocumentReference")
            .asText(null);
    if (reference != null && dsp.transportDocumentReference() == null) {
      dsp = dsp.withTransportDocumentReference(reference);
    }
    getDspConsumer().accept(dsp.withNewTransportDocumentContent(true));
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return getTDNotificationChecks(
            getMatchedExchangeUuid(),
            expectedApiVersion,
            requestSchemaValidator,
            TransportDocumentStatusScenario.uc8());
      }
    };
  }
}
