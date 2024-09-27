package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.UserFacingException;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@Getter
public class UC6_Carrier_PublishDraftTransportDocumentAction extends StateChangingSIAction {
  private final JsonSchemaValidator notificationSchemaValidator;
  private final boolean skipSI;

  public UC6_Carrier_PublishDraftTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator notificationSchemaValidator,
      boolean skipSI) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC6", 204);
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.skipSI = skipSI;
  }

  @Override
  public String getHumanReadablePrompt() {
    if (skipSI) {
      return "UC6: Publish draft transport document matching the scenario parameters provided in the previous step and provide its transport document reference here";
    }
    return ("UC6: Publish draft transport document for shipping instructions with reference %s"
        .formatted(getDspSupplier().get().shippingInstructionsReference()));
  }


  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);

    var dsp = getDspSupplier().get();
    if (skipSI) {
      var tdr = exchange.getRequest().message().body().getJsonBody().path("data").path("transportDocumentReference");
      if (!tdr.isMissingNode()) {
        dsp = dsp.withTransportDocumentReference(tdr.asText());
      }
    }
    getDspConsumer().accept(dsp.withNewTransportDocumentContent(true));
  }

  @Override
  public boolean isInputRequired() {
    return this.skipSI;
  }

  @Override
  public void handlePartyInput(JsonNode partyInput) throws UserFacingException {
    super.handlePartyInput(partyInput);
    getDspConsumer().accept(getDspSupplier().get().withTransportDocumentReference(partyInput.path("transportDocumentReference").asText()));
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return OBJECT_MAPPER.createObjectNode()
      .put("transportDocumentReference", "Insert TDR here");
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    var dr = dsp.transportDocumentReference() != null ? dsp.transportDocumentReference() : dsp.shippingInstructionsReference();
    var node = super.asJsonNode()
      .put("documentReference", dr)
      .put("scenarioType", dsp.scenarioType().name())
      .put("skipSI", skipSI);
    node.set("csp", getCspSupplier().get().toJson());
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
