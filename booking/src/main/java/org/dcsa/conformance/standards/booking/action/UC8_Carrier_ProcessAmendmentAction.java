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
public class UC8_Carrier_ProcessAmendmentAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;

  private final boolean acceptAmendment;

  public UC8_Carrier_ProcessAmendmentAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      boolean acceptAmendment) {
    super(
        carrierPartyName,
        shipperPartyName,
        previousAction,
        "UC8%s [%s]".formatted(acceptAmendment ? "a" : "b", acceptAmendment ? "A" : "D") ,
        204);
    this.requestSchemaValidator = requestSchemaValidator;
    this.acceptAmendment = acceptAmendment;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC8: Process the booking amendment with CBR %s".formatted(getDspSupplier().get().carrierBookingReference()));
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    var dsp = getDspSupplier().get();
    return jsonNode.put("cbrr", dsp.carrierBookingRequestReference())
        .put("cbr", dsp.carrierBookingReference())
        .put("acceptAmendment", acceptAmendment);
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        var bookingStatus = dsp.bookingStatus();
        return Stream.of(
            new UrlPathCheck(
                BookingRole::isCarrier, getMatchedExchangeUuid(), "/v2/booking-notifications"),
            new ResponseStatusCheck(
                BookingRole::isShipper, getMatchedExchangeUuid(), expectedStatus),
            new CarrierBookingNotificationDataPayloadRequestConformanceCheck(
              getMatchedExchangeUuid(),
              acceptAmendment ? BookingState.CONFIRMED : bookingStatus,
              acceptAmendment ? BookingState.AMENDMENT_CONFIRMED : BookingState.AMENDMENT_DECLINED
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
