package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.BookingChecks;
import org.dcsa.conformance.standards.booking.checks.CarrierBookingRefStatusPayloadResponseConformanceCheck;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.stream.Stream;

@Getter
@Slf4j
public class UC7_Shipper_SubmitBookingAmendment extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final boolean invalidCase;

  public UC7_Shipper_SubmitBookingAmendment(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator,
      boolean invalidCase) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC7", invalidCase? 409 : 200);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.invalidCase = invalidCase;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC7: Submit Amendment to confirmed booking");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("cbrr", getDspSupplier().get().carrierBookingRequestReference());
    return jsonNode;
  }

  @Override
  protected boolean expectsNotificationExchange() {
    return !invalidCase;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        var cbrr = dsp.carrierBookingRequestReference();
        var expectedBookingStatus = dsp.bookingStatus();
        Stream<ActionCheck> primaryExchangeChecks =
            Stream.of(
                new HttpMethodCheck(BookingRole::isShipper, getMatchedExchangeUuid(), "PUT"),
                new UrlPathCheck(
                    BookingRole::isShipper,
                    getMatchedExchangeUuid(),
                    "/v2/bookings/%s".formatted(cbrr)),
                new ResponseStatusCheck(
                    BookingRole::isCarrier, getMatchedExchangeUuid(), expectedStatus),
                new ApiHeaderCheck(
                    BookingRole::isShipper,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    expectedApiVersion),
                new ApiHeaderCheck(
                    BookingRole::isCarrier,
                    getMatchedExchangeUuid(),
                    HttpMessageType.RESPONSE,
                    expectedApiVersion),
                new JsonSchemaCheck(
                    BookingRole::isShipper,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    requestSchemaValidator));
        return invalidCase ? primaryExchangeChecks : Stream.concat(
          Stream.concat( primaryExchangeChecks,
            Stream.of(new CarrierBookingRefStatusPayloadResponseConformanceCheck(
              getMatchedExchangeUuid(),
              expectedBookingStatus,
              BookingState.AMENDMENT_RECEIVED
            ),
              BookingChecks.requestContentChecks(getMatchedExchangeUuid(),expectedApiVersion, getCspSupplier(), getDspSupplier()),
              new JsonSchemaCheck(
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator))),
            getNotificationChecks(
                expectedApiVersion,
                notificationSchemaValidator,
                expectedBookingStatus,
                BookingState.AMENDMENT_RECEIVED));
      }
    };
  }
}
