package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.standards.ebl.checks.EBLChecks;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

@Getter
public class UC4_Carrier_ProcessUpdateToShippingInstructionsAction extends StateChangingSIAction {
  private final ShippingInstructionsStatus expectedSIStatus;
  private final JsonSchemaValidator requestSchemaValidator;
  private final boolean acceptChanges;

  public UC4_Carrier_ProcessUpdateToShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      ShippingInstructionsStatus expectedSIStatus,
      JsonSchemaValidator requestSchemaValidator,
      boolean acceptChanges) {
    super(carrierPartyName, shipperPartyName, previousAction, acceptChanges ? "UC4a" : "UC4d", 204);
    this.expectedSIStatus = expectedSIStatus;
    this.requestSchemaValidator = requestSchemaValidator;
    this.acceptChanges = acceptChanges;
    assert !acceptChanges || expectedSIStatus == ShippingInstructionsStatus.SI_RECEIVED;
  }

  @Override
  public String getHumanReadablePrompt() {
    if (acceptChanges) {
      return getMarkdownHumanReadablePrompt(
          Map.of("REFERENCE", getDSP().shippingInstructionsReference()),
          "prompt-carrier-uc4a.md",
          "prompt-carrier-notification.md");
    }
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", getDSP().shippingInstructionsReference()),
        "prompt-carrier-uc4d.md",
        "prompt-carrier-notification.md");
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
          requestSchemaValidator,
          expectedSIStatus,
          acceptChanges ? ShippingInstructionsStatus.SI_UPDATE_CONFIRMED : ShippingInstructionsStatus.SI_UPDATE_DECLINED,
          EBLChecks.sirInNotificationMustMatchDSP(getDspSupplier())
        );
      }
    };
  }
}
