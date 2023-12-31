package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

@Getter
public class UC6_Carrier_PublishDraftTransportDocumentAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;

  public UC6_Carrier_PublishDraftTransportDocumentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC6", 204);
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC6: Publish draft transport document for shipping instructions with reference %s"
        .formatted(getDspSupplier().get().shippingInstructionsReference()));
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    return super.asJsonNode()
      .put("documentReference", dsp.shippingInstructionsReference())
      .put("scenarioType", dsp.scenarioType().name());
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
                TransportDocumentStatus.TD_DRAFT,
                false);
      }
    };
  }
}
