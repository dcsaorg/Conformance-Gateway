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
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

@Getter
public class UC6_Carrier_PublishDraftTransportDocumentAction extends StateChangingSIAction {
  public static final String ACTION_TITLE = "UC6";
  private final JsonSchemaValidator notificationSchemaValidator;
  private final boolean skipSI;

  public UC6_Carrier_PublishDraftTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator notificationSchemaValidator,
      boolean skipSI) {
    super(carrierPartyName, shipperPartyName, previousAction, ACTION_TITLE, 204);
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.skipSI = skipSI;
  }

  @Override
  public String getHumanReadablePrompt() {
    if (skipSI) {
      return getMarkdownHumanReadablePrompt(
          null, "prompt-carrier-uc6.md", "prompt-carrier-notification.md");
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
      var tdr = exchange.getRequest().message().body().getJsonBody().path("data").path("transportDocumentReference");
      if (!tdr.isMissingNode()) {
        dsp = dsp.withTransportDocumentReference(tdr.asText());
      }
    getDspConsumer().accept(dsp.withNewTransportDocumentContent(true));
    getCarrierPayloadConsumer().accept(OBJECT_MAPPER.createObjectNode());
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
    return OBJECT_MAPPER.createObjectNode()
      .put("transportDocumentReference", "Insert TDR here");
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    var dr = dsp.transportDocumentReference() != null ? dsp.transportDocumentReference() : dsp.shippingInstructionsReference();
    var node = super.asJsonNode()
      .put("documentReference", dr)
      .put("eblScenarioType", getDspSupplier().get().scenarioType())
      .put("skipSI", skipSI);
    node.set(CarrierSupplyPayloadAction.CARRIER_PAYLOAD, getCarrierPayloadSupplier().get());
    return node;
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
                TransportDocumentStatus.TD_DRAFT,
                false);
      }
    };
  }
}
