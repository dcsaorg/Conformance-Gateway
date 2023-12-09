package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;

@Getter
public class UC4_Carrier_ProcessUpdateToShippingInstructionsAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final boolean acceptChanges;

  public UC4_Carrier_ProcessUpdateToShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      boolean acceptChanges) {
    super(carrierPartyName, shipperPartyName, previousAction, acceptChanges ? "UC4a" : "UC4d", 204);
    this.requestSchemaValidator = requestSchemaValidator;
    this.acceptChanges = acceptChanges;
  }

  @Override
  public String getHumanReadablePrompt() {
    if (acceptChanges) {
      return ("UC4a: Accept updated shipping instructions with document reference %s"
        .formatted(getDspSupplier().get().shippingInstructionsReference()));
    }
    return ("UC4d: Decline updated shipping instructions with document reference %s"
        .formatted(getDspSupplier().get().shippingInstructionsReference()));
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
      .put("documentReference", getDspSupplier().get().shippingInstructionsReference())
      .put("acceptChanges",  acceptChanges);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return getSINotificationChecks(
          getMatchedExchangeUuid(),
          expectedApiVersion,
          requestSchemaValidator
        );
      }
    };
  }
}