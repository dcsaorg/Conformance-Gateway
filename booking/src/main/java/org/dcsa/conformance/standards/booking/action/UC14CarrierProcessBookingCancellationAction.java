package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.CarrierBookingNotificationDataPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingRole;

import java.util.Objects;
import java.util.stream.Stream;

@Getter
public class UC14CarrierProcessBookingCancellationAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final boolean isCancellationConfirmed;

  public UC14CarrierProcessBookingCancellationAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      boolean isCancellationConfirmed) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC14", 204);
    this.requestSchemaValidator = requestSchemaValidator;
    this.isCancellationConfirmed = isCancellationConfirmed;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC14: Process the confirmed booking cancellation  CBR '%s'"
        .formatted(
            getDspSupplier().get().carrierBookingReference()));
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
        var dsp = getDspSupplier().get();
        var bookingStatus = dsp.bookingStatus();
        var amendedBookingStatus = dsp.amendedBookingStatus();
        var isCancelled = isCancellationConfirmed ?
          BookingCancellationState.CANCELLATION_CONFIRMED : BookingCancellationState.CANCELLATION_DECLINED;
        return Stream.of(
            new UrlPathCheck(
                BookingRole::isCarrier, getMatchedExchangeUuid(), "/v2/booking-notifications"),
            new ResponseStatusCheck(
                BookingRole::isShipper, getMatchedExchangeUuid(), expectedStatus),
            bookingStatus == null ? null: new CarrierBookingNotificationDataPayloadRequestConformanceCheck(
              getMatchedExchangeUuid(),
              bookingStatus,
              amendedBookingStatus,
              isCancelled),
            ApiHeaderCheck.createNotificationCheck(
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            ApiHeaderCheck.createNotificationCheck(
                BookingRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator)
          ).filter(Objects::nonNull);
      }
    };
  }
}
