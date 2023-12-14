package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.standards.ebl.checks.EBLChecks;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

@Getter
public class UC11i_Carrier_IssueAmendedTransportDocumentAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;

  public UC11i_Carrier_IssueAmendedTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC11i", 204);
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC11i: Issue amended transport document to replace the transport document with reference %s."
        .formatted(getDspSupplier().get().transportDocumentReference()));
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    return super.asJsonNode()
      .put("documentReference", dsp.transportDocumentReference())
      .put("scenarioType", dsp.scenarioType().name());
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
