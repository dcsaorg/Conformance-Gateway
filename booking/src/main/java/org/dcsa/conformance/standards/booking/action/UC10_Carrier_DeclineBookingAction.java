package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.CarrierBookingNotificationDataPayloadRequestConformanceCheck;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

@Getter
public class UC10_Carrier_DeclineBookingAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final BookingState expectedAmendedBookingStatus;

  public UC10_Carrier_DeclineBookingAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      BookingState expectedAmendedBookingStatus,
      JsonSchemaValidator requestSchemaValidator) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC10", 204);
    this.requestSchemaValidator = requestSchemaValidator;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC10: Decline the booking request with CBR '%s' and CBRR '%s'"
        .formatted(
            getDspSupplier().get().carrierBookingReference(),
            getDspSupplier().get().carrierBookingRequestReference()));
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
        return Stream.of(
            new UrlPathCheck(
                BookingRole::isCarrier, getMatchedExchangeUuid(), "/v2/booking-notifications"),
            new ResponseStatusCheck(
                BookingRole::isShipper, getMatchedExchangeUuid(), expectedStatus),
            new CarrierBookingNotificationDataPayloadRequestConformanceCheck(
                getMatchedExchangeUuid(),
                BookingState.DECLINED,
                expectedAmendedBookingStatus),
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
                requestSchemaValidator));
      }
    };
  }
}
