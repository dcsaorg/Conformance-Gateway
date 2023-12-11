package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.EBLChecks;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

@Getter
public class UC2_Carrier_RequestUpdateToShippingInstructionsAction extends StateChangingSIAction {
  private final JsonSchemaValidator requestSchemaValidator;

  public UC2_Carrier_RequestUpdateToShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC2", 204);
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC2: Request update to the shipping instructions with shipping instructions reference %s"
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
        return Stream.concat(
          EBLChecks.siNotificationSIR(
            getMatchedExchangeUuid(),
            getDspSupplier().get().shippingInstructionsReference()
          ),
          getSINotificationChecks(
            getMatchedExchangeUuid(),
            expectedApiVersion,
            requestSchemaValidator,
            ShippingInstructionsStatus.SI_PENDING_UPDATE
        ));
      }
    };
  }
}
