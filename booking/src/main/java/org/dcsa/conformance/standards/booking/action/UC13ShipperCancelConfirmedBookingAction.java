package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

@Getter
@Slf4j
public class UC13ShipperCancelConfirmedBookingAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final BookingState expectedBookingStatus;
  private final BookingState expectedAmendedBookingStatus;
  private final BookingCancellationState expectedBookingCancellationStatus;

  public UC13ShipperCancelConfirmedBookingAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      BookingState expectedBookingStatus,
      BookingState expectedAmendedBookingStatus,
      BookingCancellationState expectedBookingCancellationStatus,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator,
      boolean isWithNotifications) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC13", 202, isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.expectedBookingStatus = expectedBookingStatus;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
    this.expectedBookingCancellationStatus = expectedBookingCancellationStatus;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        "prompt-shipper-uc13.md", "prompt-shipper-refresh-complete.md");
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
        return Stream.concat(
            Stream.concat(
                createPatchPrimarySubChecks(
                    expectedApiVersion, "/v2/bookings/", cbrr, cbr),
                Stream.concat(
                    Stream.of(new JsonSchemaCheck(
                        BookingRole::isShipper,
                        getMatchedExchangeUuid(),
                        HttpMessageType.REQUEST,
                        requestSchemaValidator)),
                    patchPreconditionChecks(
                        "[Scenario] The Booking cancellation request must demonstrate that this precondition is respected: It is a precondition that %s is CONFIRMED or PENDING_AMENDMENT in order to cancel it. If this is not the case a 409 (Conflict) error response should be returned"
                            .formatted(jsonPath(BOOKING_STATUS)),
                        BOOKING_STATUS,
                        status ->
                            BookingState.CONFIRMED.name().equals(status)
                                || BookingState.PENDING_AMENDMENT.name().equals(status),
                        "CONFIRMED or PENDING_AMENDMENT",
                        409))),
            expectedBookingStatus != null
                ? getNotificationChecks(
                    expectedApiVersion,
                    notificationSchemaValidator,
                    expectedBookingStatus,
                    expectedAmendedBookingStatus,
                    expectedBookingCancellationStatus)
                : Stream.empty());
      }
    };
  }
}
