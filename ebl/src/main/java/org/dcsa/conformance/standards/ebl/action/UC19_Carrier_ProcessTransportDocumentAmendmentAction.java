package org.dcsa.conformance.standards.ebl.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.ebl.checks.TransportDocumentStatusScenario;

public class UC19_Carrier_ProcessTransportDocumentAmendmentAction extends CarrierNotificationEblAction {

  private final JsonSchemaValidator notificationSchemaValidator;
  private final boolean confirm;

  public UC19_Carrier_ProcessTransportDocumentAmendmentAction(
      String carrierPartyName,
      String shipperPartyName,
      EblAction previousAction,
      JsonSchemaValidator notificationSchemaValidator,
      boolean confirm,
      boolean isWithNotifications) {
    super(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        "UC19 (%s)".formatted(confirm ? "confirm" : "decline"),
        204,
        isWithNotifications);
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.confirm = confirm;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        Map.of(
            "REFERENCE", getDSP().transportDocumentReference(),
            "DECISION", confirm ? "confirm" : "decline"),
        "prompt-carrier-uc19.md",
        "prompt-carrier-notification.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
        .put("documentReference", getDspSupplier().get().transportDocumentReference())
        .put("confirm", confirm);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return getTDNotificationChecks(
            getMatchedExchangeUuid(),
            expectedApiVersion,
            notificationSchemaValidator,
            TransportDocumentStatusScenario.uc19(confirm));
      }
    };
  }
}

