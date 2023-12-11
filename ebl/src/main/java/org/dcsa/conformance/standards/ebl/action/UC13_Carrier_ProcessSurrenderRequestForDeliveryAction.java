package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.standards.ebl.checks.EBLChecks;
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
      boolean acceptDeliveryRequest) {
    super(carrierPartyName, shipperPartyName, previousAction, acceptDeliveryRequest ? "UC13a" : "UC13r", 204);
    this.requestSchemaValidator = requestSchemaValidator;
    this.acceptDeliveryRequest = acceptDeliveryRequest;
  }

  @Override
  public String getHumanReadablePrompt() {
    if (acceptDeliveryRequest) {
      return ("UC13a: Accept surrender request for delivery for transport document with reference %s"
        .formatted(getDspSupplier().get().transportDocumentReference()));
    }
    return ("UC13r: Reject surrender request for delivery for transport document with reference %s"
        .formatted(getDspSupplier().get().transportDocumentReference()));
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
      .put("documentReference", getDspSupplier().get().transportDocumentReference())
      .put("acceptDeliveryRequest", acceptDeliveryRequest);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var expectedStatus = acceptDeliveryRequest
          ? TransportDocumentStatus.TD_SURRENDERED_FOR_DELIVERY
          : TransportDocumentStatus.TD_ISSUED;
        return Stream.concat(
          Stream.concat(
            EBLChecks.tdNotificationTDR(getMatchedExchangeUuid(), getDspSupplier().get().transportDocumentReference()),
            EBLChecks.tdNotificationStatusChecks(getMatchedExchangeUuid(), expectedStatus)
          ),
          getTDNotificationChecks(
            getMatchedExchangeUuid(),
            expectedApiVersion,
            requestSchemaValidator,
            expectedStatus
          ));
      }
    };
  }
}
