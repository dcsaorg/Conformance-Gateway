package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.checks.CarrierStatusScenario;

import java.util.stream.Stream;

@Getter
public class UC8_Carrier_ProcessAmendmentAction extends CarrierNotificationBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;

  public UC8_Carrier_ProcessAmendmentAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    JsonSchemaValidator requestSchemaValidator,
    boolean isWithNotifications) {
    super(
      carrierPartyName,
      shipperPartyName,
      previousAction,
      "UC8",
      204,
      isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
      "prompt-carrier-uc8.md", "prompt-carrier-notification.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    var dsp = getDspSupplier().get();
    return jsonNode
      .put("cbrr", dsp.carrierBookingRequestReference())
      .put("cbr", dsp.carrierBookingReference());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return getSimpleNotificationChecks(
          expectedApiVersion, requestSchemaValidator, CarrierStatusScenario.uc8());
      }
    };
  }
}
