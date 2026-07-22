package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.stream.Stream;

@Getter
public class UC10_Carrier_DeclineBookingAction extends CarrierNotificationBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final BookingState expectedAmendedBookingStatus;

  public UC10_Carrier_DeclineBookingAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    BookingState expectedAmendedBookingStatus,
    JsonSchemaValidator requestSchemaValidator,
    boolean isWithNotifications) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC10", 204, isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
      "prompt-carrier-uc10.md", "prompt-carrier-notification.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    var dsp = getDspSupplier().get();
    return jsonNode
      .put("cbr", dsp.carrierBookingReference())
      .put("cbrr", dsp.carrierBookingRequestReference());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return getSimpleNotificationChecks(
          expectedApiVersion,
          requestSchemaValidator,
          BookingState.DECLINED,
          expectedAmendedBookingStatus);
      }
    };
  }
}
