package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.checks.CarrierStatusScenario;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.stream.Stream;

@Getter
public class UC8_Carrier_ProcessAmendmentAction extends CarrierNotificationBookingAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private final boolean confirmAmendment;

  public UC8_Carrier_ProcessAmendmentAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    JsonSchemaValidator requestSchemaValidator,
    boolean isWithNotifications) {
    this(
      carrierPartyName,
      shipperPartyName,
      previousAction,
      requestSchemaValidator,
      isWithNotifications,
      true);
  }

  public UC8_Carrier_ProcessAmendmentAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    JsonSchemaValidator requestSchemaValidator,
    boolean isWithNotifications,
    boolean confirmAmendment) {
    super(
      carrierPartyName,
      shipperPartyName,
      previousAction,
      confirmAmendment ? "UC8 (Confirm)" : "UC8 (Decline)",
      204,
      isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.confirmAmendment = confirmAmendment;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
      confirmAmendment ? "prompt-carrier-uc8.md" : "prompt-carrier-uc8-decline.md",
      "prompt-carrier-notification.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    var dsp = getDspSupplier().get();
    return jsonNode
      .put("cbrr", dsp.carrierBookingRequestReference())
      .put("cbr", dsp.carrierBookingReference())
      .put("confirmAmendment", confirmAmendment);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return getSimpleNotificationChecks(
          expectedApiVersion,
          requestSchemaValidator,
          CarrierStatusScenario.from(
            BookingState.CONFIRMED,
            confirmAmendment ? BookingState.AMENDMENT_CONFIRMED : BookingState.AMENDMENT_DECLINED,
            null
          )
        );
      }
    };
  }
}
