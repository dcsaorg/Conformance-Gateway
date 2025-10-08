package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.CarrierBookingNotificationDataPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

@Getter
public class UC14CarrierProcessBookingCancellationAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final boolean isCancellationConfirmed;
  private final BookingState expectedBookingStatus;
  private final BookingState expectedAmendedBookingStatus;

  public UC14CarrierProcessBookingCancellationAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      BookingState expectedBookingStatus,
      BookingState expectedAmendedBookingStatus,
      JsonSchemaValidator requestSchemaValidator,
      boolean isCancellationConfirmed,
      boolean isWithNotifications) {
    super(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        getFormattedActionTitle(isCancellationConfirmed),
        204,
        isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.isCancellationConfirmed = isCancellationConfirmed;
    this.expectedBookingStatus = expectedBookingStatus;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        "prompt-carrier-uc14%s.md".formatted(isCancellationConfirmed ? "c" : "d"),
        "prompt-carrier-notification.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    var dsp = getDspSupplier().get();
    return jsonNode
        .put("cbr", dsp.carrierBookingReference())
        .put("isCancellationConfirmed", isCancellationConfirmed);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var cancelledStatus =
            isCancellationConfirmed
                ? BookingCancellationState.CANCELLATION_CONFIRMED
                : BookingCancellationState.CANCELLATION_DECLINED;
        return getSimpleNotificationChecks(
            expectedApiVersion,
            requestSchemaValidator,
            expectedBookingStatus,
            expectedAmendedBookingStatus,
            cancelledStatus);
      }
    };
  }

  private static String getFormattedActionTitle(boolean isCancellationConfirmed) {
    return "UC14%s [%s]"
        .formatted(isCancellationConfirmed ? "a" : "b", isCancellationConfirmed ? "A" : "D");
  }
}
