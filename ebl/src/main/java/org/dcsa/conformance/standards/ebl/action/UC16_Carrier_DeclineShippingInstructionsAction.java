package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.standards.ebl.checks.EblChecks;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;

@Getter
public class UC16_Carrier_DeclineShippingInstructionsAction extends CarrierNotificationEblAction {
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC16_Carrier_DeclineShippingInstructionsAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator notificationSchemaValidator,
      boolean isWithNotifications) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC16", 204, isWithNotifications);
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of("REFERENCE", getDSP().shippingInstructionsReference()),
        "prompt-carrier-uc16.md",
        "prompt-carrier-notification.md");
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
        return getSINotificationChecks(
            getMatchedExchangeUuid(),
            expectedApiVersion,
            notificationSchemaValidator,
            ShippingInstructionsStatus.SI_DECLINED,
            EblChecks.sirInNotificationMustMatchDSP(getDspSupplier()));
      }
    };
  }
}

