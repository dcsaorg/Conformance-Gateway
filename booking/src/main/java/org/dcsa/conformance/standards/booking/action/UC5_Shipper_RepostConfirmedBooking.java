package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.stream.Stream;

@Getter
@Slf4j
public class UC5_Shipper_RepostConfirmedBooking extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;

  public UC5_Shipper_RepostConfirmedBooking(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC5", 200);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
  }


  @Override
  protected boolean expectsNotificationExchange() {
    return true;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC5: Repost confirmed booking");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("cbrr", getDspSupplier().get().carrierBookingRequestReference())
      .put("cbr", getDspSupplier().get().carrierBookingReference());
    return jsonNode;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var cbrr = getDspSupplier().get().carrierBookingRequestReference();
        return
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
                    requestSchemaValidator),
                new JsonSchemaCheck(
                    BookingRole::isCarrier,
                    getMatchedExchangeUuid(),
                    HttpMessageType.RESPONSE,
                    responseSchemaValidator));
      }
    };
  }
}
