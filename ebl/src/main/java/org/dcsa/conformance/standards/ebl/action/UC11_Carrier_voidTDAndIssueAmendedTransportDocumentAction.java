package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.standardscommons.action.BookingAndEblAction;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

@Getter
public class UC11_Carrier_voidTDAndIssueAmendedTransportDocumentAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;

  public UC11_Carrier_voidTDAndIssueAmendedTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAndEblAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC11(void and reissue)", 204);
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", getDSP().transportDocumentReference()),
        "prompt-carrier-uc11.md",
        "prompt-carrier-notification.md");
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    var dsp = getDspSupplier().get();
    // This is a re-issuance; those will de facto change the TD.
    if (!dsp.newTransportDocumentContent()) {
      getDspConsumer().accept(dsp.withNewTransportDocumentContent(true));
    }
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    return super.asJsonNode()
      .put("documentReference", dsp.transportDocumentReference())
      .put("scenarioType", getDspSupplier().get().scenarioType());
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
          TransportDocumentStatus.TD_ISSUED
        );
      }
    };
  }
}
