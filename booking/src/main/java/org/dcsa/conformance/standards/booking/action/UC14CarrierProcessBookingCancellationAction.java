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
      boolean isCancellationConfirmed) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC14", 204);
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
        var cancelledStatus = isCancellationConfirmed ?
          BookingCancellationState.CANCELLATION_CONFIRMED : BookingCancellationState.CANCELLATION_DECLINED;
        return Stream.of(
            new UrlPathCheck(
                BookingRole::isCarrier, getMatchedExchangeUuid(), "/v2/booking-notifications"),
            new ResponseStatusCheck(
                BookingRole::isShipper, getMatchedExchangeUuid(), expectedStatus),
            new CarrierBookingNotificationDataPayloadRequestConformanceCheck(
              getMatchedExchangeUuid(),
              expectedBookingStatus,
              expectedAmendedBookingStatus,
              cancelledStatus),
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
          );
      }
    };
  }
}
