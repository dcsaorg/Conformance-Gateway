package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.checks.CarrierStatusScenario;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.stream.Stream;

@Getter
public class UC14CarrierProcessBookingCancellationAction extends CarrierNotificationBookingAction {

  private final JsonSchemaValidator requestSchemaValidator;

  public UC14CarrierProcessBookingCancellationAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    JsonSchemaValidator requestSchemaValidator,
    boolean isWithNotifications) {
    super(
      carrierPartyName,
      shipperPartyName,
      previousAction,
      "UC14(Confirm)",
      204,
      isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
      "prompt-carrier-uc14.md", "prompt-carrier-notification.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    var dsp = getDspSupplier().get();
    return jsonNode.put("cbr", dsp.carrierBookingReference());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return getSimpleNotificationChecks(
          expectedApiVersion,
          requestSchemaValidator,
          CarrierStatusScenario.from(BookingState.CANCELLED, null, BookingCancellationState.CANCELLATION_CONFIRMED));
      }
    };
  }
}
