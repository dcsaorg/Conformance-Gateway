package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.standards.ebl.checks.EBLChecks;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

@Getter
public class UC9_Carrier_AwaitSurrenderRequestForAmendmentAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;

  public UC9_Carrier_AwaitSurrenderRequestForAmendmentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC9", 204);
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC9: Shipper requests surrender for amendment (via the surrender API if applicable) for transport document with reference %s and carrier sends notification that surrender has been requested. Note when the conformance toolkit is acting as carrier, no action is required from the shipper (the action will auto-resolve). When the conformance toolkit is acting like the Shipper, you will have to ensure that the carrier system sees a surrender request (the surrender uses a different API not in scope for this test)."
        .formatted(getDspSupplier().get().transportDocumentReference()));
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
      .put("documentReference", getDspSupplier().get().transportDocumentReference());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.concat(
          Stream.concat(
            EBLChecks.tdNotificationTDR(getMatchedExchangeUuid(), getDspSupplier().get().transportDocumentReference()),
            EBLChecks.tdNotificationStatusChecks(getMatchedExchangeUuid(), TransportDocumentStatus.TD_PENDING_SURRENDER_FOR_AMENDMENT)
          ),
          getTDNotificationChecks(
            getMatchedExchangeUuid(),
            expectedApiVersion,
            requestSchemaValidator,
            TransportDocumentStatus.TD_PENDING_SURRENDER_FOR_AMENDMENT
          ));
      }
    };
  }
}
