package org.dcsa.conformance.standards.ebl.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.checks.TransportDocumentStatusScenario;

@Getter
public class UC6_Carrier_PublishDraftTransportDocumentAction extends CarrierNotificationEblAction {
  public static final String ACTION_TITLE = "UC6";
  private final JsonSchemaValidator notificationSchemaValidator;
  private final boolean skipSI;
  private final boolean anyTdType;
  private ScenarioType scenarioType;

  public UC6_Carrier_PublishDraftTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      ScenarioType scenarioType,
      JsonSchemaValidator notificationSchemaValidator,
      boolean skipSI,
      boolean isWithNotifications) {
    this(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        scenarioType,
        notificationSchemaValidator,
        skipSI,
        isWithNotifications,
        true);
  }

  public UC6_Carrier_PublishDraftTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      ScenarioType scenarioType,
      JsonSchemaValidator notificationSchemaValidator,
      boolean skipSI,
      boolean isWithNotifications,
      boolean includeScenarioTypeInTitle) {
    this(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        scenarioType,
        notificationSchemaValidator,
        skipSI,
        isWithNotifications,
        includeScenarioTypeInTitle
            ? "UC6 [%s]".formatted(scenarioType.tdScopeName())
            : ACTION_TITLE,
        false);
  }

  public UC6_Carrier_PublishDraftTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      ScenarioType scenarioType,
      JsonSchemaValidator notificationSchemaValidator,
      boolean skipSI,
      boolean isWithNotifications,
      String actionTitle) {
    this(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        scenarioType,
        notificationSchemaValidator,
        skipSI,
        isWithNotifications,
        actionTitle,
        true);
  }

  private UC6_Carrier_PublishDraftTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      ScenarioType scenarioType,
      JsonSchemaValidator notificationSchemaValidator,
      boolean skipSI,
      boolean isWithNotifications,
      String actionTitle,
      boolean anyTdType) {
    super(
        carrierPartyName, shipperPartyName, previousAction, actionTitle, 204, isWithNotifications);
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.skipSI = skipSI;
    this.scenarioType = scenarioType;
    this.anyTdType = anyTdType;
    initializeScope();
  }

  public UC6_Carrier_PublishDraftTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator notificationSchemaValidator,
      boolean skipSI,
      boolean isWithNotifications) {
    super(
        carrierPartyName, shipperPartyName, previousAction, ACTION_TITLE, 204, isWithNotifications);
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.skipSI = skipSI;
    this.anyTdType = false;
  }

  private void initializeScope() {
    if (scenarioType != null) {
      getDspConsumer().accept(getDSP().withScenarioType(anyTdType ? null : scenarioType.name()));
    }
  }

  @Override
  public void reset() {
    super.reset();
    initializeScope();
  }

  @Override
  public void importJsonState(JsonNode state) {
    super.importJsonState(state);
    initializeScope();
  }

  @Override
  public String getHumanReadablePrompt() {
    if (skipSI) {
      if (anyTdType) {
        return getMarkdownHumanReadablePrompt(
            Map.of(), "prompt-carrier-uc6-any-td.md", "prompt-carrier-notification.md");
      }
      return getMarkdownHumanReadablePrompt(
          Map.of(
              "DOCUMENT_TYPE_NAME", scenarioType.tdScopeName(),
              "DOCUMENT_TYPE_CODE", scenarioType.transportDocumentTypeCode(),
              "TO_ORDER_VALUE", Boolean.toString(scenarioType.isToOrder())),
          "prompt-carrier-uc6.md",
          "prompt-carrier-notification.md");
    }
    String reference =
        getDSP().shippingInstructionsReference() != null
            ? getDSP().shippingInstructionsReference()
            : getDSP().transportDocumentReference();
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", reference),
        "prompt-carrier-uc6-si-td.md",
        "prompt-carrier-notification.md");
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);

    var dsp = getDspSupplier().get();
    var tdr =
        exchange
            .getRequest()
            .message()
            .body()
            .getJsonBody()
            .path("data")
            .path("transportDocumentReference");
    if (!tdr.isMissingNode()) {
      dsp = dsp.withTransportDocumentReference(tdr.asText());
    }
    getDspConsumer().accept(dsp.withNewTransportDocumentContent(true));
  }

  @Override
  public boolean isInputRequired() {
    return this.skipSI;
  }

  @Override
  protected void doHandlePartyInput(JsonNode partyInput) throws UserFacingException {
    getDspConsumer()
        .accept(
            getDspSupplier()
                .get()
                .withTransportDocumentReference(
                    partyInput.required("input").path("transportDocumentReference").asText()));
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    if (!skipSI) {
      return null;
    }
    return OBJECT_MAPPER.createObjectNode().put("transportDocumentReference", "Insert TDR here");
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    var dr =
        dsp.transportDocumentReference() != null
            ? dsp.transportDocumentReference()
            : dsp.shippingInstructionsReference();
    return super.asJsonNode()
        .put("documentReference", dr)
        .put("scenarioType", scenarioType == null ? getDspSupplier().get().scenarioType() : scenarioType.name())
        .put("skipSI", skipSI);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {

        return getTDNotificationChecks(
            getMatchedExchangeUuid(),
            expectedApiVersion,
            notificationSchemaValidator,
            TransportDocumentStatusScenario.uc6(),
            false);
      }
    };
  }
}
