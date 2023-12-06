package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;

@Getter
public class UC14_Carrier_ConfirmShippingInstructionsCompleteAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;

  public UC14_Carrier_ConfirmShippingInstructionsCompleteAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC14", 204);
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC14: Confirm shipping instructions with reference %s is complete."
        .formatted(getDspSupplier().get().shippingInstructionsReference()));
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
        return getTDNotificationChecks(
          getMatchedExchangeUuid(),
          expectedApiVersion,
          requestSchemaValidator
        );
      }
    };
  }
}
