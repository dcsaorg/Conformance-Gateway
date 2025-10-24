package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.standards.booking.party.BookingState;

@Getter
public class UC8_Carrier_ProcessAmendmentAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final BookingState expectedBookingStatus;
  private final BookingState expectedAmendedBookingStatus;

  private final boolean acceptAmendment;

  public UC8_Carrier_ProcessAmendmentAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      BookingState expectedBookingStatus,
      BookingState expectedAmendedBookingStatus,
      JsonSchemaValidator requestSchemaValidator,
      boolean acceptAmendment,
      boolean isWithNotifications) {
    super(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        "UC8%s [%s]".formatted(acceptAmendment ? "a" : "b", acceptAmendment ? "A" : "D"),
        204,
        isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.acceptAmendment = acceptAmendment;
    this.expectedBookingStatus = expectedBookingStatus;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        "prompt-carrier-uc8%s.md".formatted(acceptAmendment ? "c" : "d"),
        "prompt-carrier-notification.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    var dsp = getDspSupplier().get();
    return jsonNode
        .put("cbrr", dsp.carrierBookingRequestReference())
        .put("cbr", dsp.carrierBookingReference())
        .put("acceptAmendment", acceptAmendment);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return getSimpleNotificationChecks(
            expectedApiVersion,
            requestSchemaValidator,
            expectedBookingStatus,
            expectedAmendedBookingStatus);
      }
    };
  }
}
