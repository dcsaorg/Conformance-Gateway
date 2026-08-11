package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.function.Function;
import java.util.stream.Stream;

@Getter
@Slf4j
public class UC9_Shipper_CancelBookingAmendment extends ShipperNotificationBookingAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final BookingState expectedBookingStatus;
  private final BookingState expectedAmendedBookingStatus;

  public UC9_Shipper_CancelBookingAmendment(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    BookingState expectedBookingStatus,
    BookingState expectedAmendedBookingStatus,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator notificationSchemaValidator,
    boolean isWithNotifications) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC9", 202, isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.expectedBookingStatus = expectedBookingStatus;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
      "prompt-shipper-uc9.md", "prompt-shipper-refresh-complete.md");
  }

  @Override
  protected boolean expectsNotificationExchange() {
    return true;
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("cbrr", getDspSupplier().get().carrierBookingRequestReference());
    jsonNode.put("cbr", getDspSupplier().get().carrierBookingReference());
    return jsonNode;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        String cbrr = dsp.carrierBookingRequestReference();
        String cbr = dsp.carrierBookingReference();
        return Stream.of(
          createPatchPrimarySubChecks(expectedApiVersion, "/v2/bookings/", requestSchemaValidator, cbrr, cbr),
          Stream.of(
            createShipperPatchPreconditionCheck(
              "[Scenario] The Booking cancellation request must demonstrate that this precondition is respected: It is a precondition that %s is AMENDMENT_RECEIVED in order to cancel the amendment to a Confirmed Booking.".formatted(jsonPath(AMENDED_BOOKING_STATUS)),
              AMENDED_BOOKING_STATUS,
              BookingState.AMENDMENT_RECEIVED.name()::equals,
              BookingState.AMENDMENT_RECEIVED.name()),
            createCarrierPatchPreconditionResponseStatusCheck(
              "[Scenario] The HTTP response status is correct for the applicable PATCH business precondition",
              AMENDED_BOOKING_STATUS,
              BookingState.AMENDMENT_RECEIVED.name()::equals,
              BookingState.AMENDMENT_RECEIVED.name(),
              404)),
          getNotificationChecks(expectedApiVersion, notificationSchemaValidator, expectedBookingStatus, expectedAmendedBookingStatus)
        ).flatMap(Function.identity());
      }
    };
  }
}
