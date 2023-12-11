package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.standards.ebl.checks.EBLChecks;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

@Getter
public class UC8_Carrier_IssueTransportDocumentAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;

  public UC8_Carrier_IssueTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC8", 204);
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC8: Issue transport document with transport document reference %s"
        .formatted(getDspSupplier().get().transportDocumentReference()));
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
      .put("documentReference", getDspSupplier().get().shippingInstructionsReference());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.concat(
          Stream.concat(
            EBLChecks.tdNotificationTDR(getMatchedExchangeUuid(), getDspSupplier().get().transportDocumentReference()),
            EBLChecks.tdNotificationStatusChecks(getMatchedExchangeUuid(), TransportDocumentStatus.TD_ISSUED)
          ),
          getTDNotificationChecks(
            getMatchedExchangeUuid(),
            expectedApiVersion,
            requestSchemaValidator,
            TransportDocumentStatus.TD_ISSUED
          ));
      }
    };
  }
}
