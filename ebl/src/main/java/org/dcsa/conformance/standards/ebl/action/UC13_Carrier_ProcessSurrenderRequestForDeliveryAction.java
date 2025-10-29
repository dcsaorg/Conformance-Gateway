package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

@Getter
public class UC13_Carrier_ProcessSurrenderRequestForDeliveryAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final boolean acceptDeliveryRequest;

  public UC13_Carrier_ProcessSurrenderRequestForDeliveryAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      boolean acceptDeliveryRequest,
      boolean isWithNotifications) {
    super(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        acceptDeliveryRequest ? "UC13a" : "UC13r",
        204,
        isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.acceptDeliveryRequest = acceptDeliveryRequest;
  }

  @Override
  public String getHumanReadablePrompt() {
    if (acceptDeliveryRequest) {
      return getMarkdownHumanReadablePrompt(
          Map.of("REFERENCE", getDSP().transportDocumentReference()),
          "prompt-carrier-uc13a.md",
          "prompt-carrier-notification.md");
    }
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", getDSP().transportDocumentReference()),
        "prompt-carrier-uc13r.md",
        "prompt-carrier-notification.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
        .put("documentReference", getDspSupplier().get().transportDocumentReference())
        .put("acceptDeliveryRequest", acceptDeliveryRequest);
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    var dsp = getDspSupplier().get();
    // Clear the flag if set.
    if (dsp.newTransportDocumentContent()) {
      getDspConsumer().accept(dsp.withNewTransportDocumentContent(false));
    }
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var expectedStatus =
            acceptDeliveryRequest
                ? TransportDocumentStatus.TD_SURRENDERED_FOR_DELIVERY
                : TransportDocumentStatus.TD_ISSUED;
        return getTDNotificationChecks(
            getMatchedExchangeUuid(), expectedApiVersion, requestSchemaValidator, expectedStatus);
      }
    };
  }
}
