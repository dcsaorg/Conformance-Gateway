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
public class UC12_Carrier_ConfirmBookingCompletedAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;

  public UC12_Carrier_ConfirmBookingCompletedAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC12", 204);
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC12: Complete the booking request with CBR %s"
        .formatted(getDspSupplier().get().carrierBookingReference()));
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    var dsp = getDspSupplier().get();
    return jsonNode.put("cbrr", dsp.carrierBookingRequestReference())
      .put("cbr", dsp.carrierBookingReference());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        return Stream.of(
            new UrlPathCheck(
                BookingRole::isCarrier, getMatchedExchangeUuid(), "/v2/booking-notifications"),
            new ResponseStatusCheck(
                BookingRole::isShipper, getMatchedExchangeUuid(), expectedStatus),
            new CarrierBookingNotificationDataPayloadRequestConformanceCheck(
              getMatchedExchangeUuid(),
              BookingState.COMPLETED,
              dsp.amendedBookingStatus() != null ? BookingState.AMENDMENT_CONFIRMED : null
            ),
            new ApiHeaderCheck(
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
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
