package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

@Getter
public class UC10_Carrier_ProcessSurrenderRequestForAmendmentAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final boolean acceptAmendmentRequest;

  public UC10_Carrier_ProcessSurrenderRequestForAmendmentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      boolean acceptAmendmentRequest) {
    super(carrierPartyName, shipperPartyName, previousAction, acceptAmendmentRequest ? "UC10a" : "UC10r", 204);
    this.requestSchemaValidator = requestSchemaValidator;
    this.acceptAmendmentRequest = acceptAmendmentRequest;
  }

  @Override
  public String getHumanReadablePrompt() {
    if (acceptAmendmentRequest) {
      return ("UC10a: Accept surrender request for amendment for transport document with reference %s"
        .formatted(getDspSupplier().get().transportDocumentReference()));
    }
    return ("UC10r: Reject surrender request for amendment for transport document with reference %s"
        .formatted(getDspSupplier().get().transportDocumentReference()));
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
      .put("documentReference", getDspSupplier().get().transportDocumentReference())
      .put("acceptAmendmentRequest", acceptAmendmentRequest);
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
        var expectedStatus = acceptAmendmentRequest
          ? TransportDocumentStatus.TD_SURRENDERED_FOR_AMENDMENT
          : TransportDocumentStatus.TD_ISSUED;
        return getTDNotificationChecks(
          getMatchedExchangeUuid(),
          expectedApiVersion,
          requestSchemaValidator,
          expectedStatus
        );
      }
    };
  }
}
