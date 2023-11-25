package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.stream.Stream;

@Getter
@Slf4j
public class UC7_Shipper_SubmitBookingAmendment extends BookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC7_Shipper_SubmitBookingAmendment(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC7", 200);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
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
  protected void doHandleExchange(ConformanceExchange exchange) {
    storeCbrAndCbrrIfPresent(exchange);
  }

  @Override
  protected boolean expectsNotificationExchange() {
    return true;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var cbrr = getDspSupplier().get().carrierBookingRequestReference();
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
                    requestSchemaValidator),
                new JsonSchemaCheck(
                    BookingRole::isCarrier,
                    getMatchedExchangeUuid(),
                    HttpMessageType.RESPONSE,
                    responseSchemaValidator));
        BookingState notificationBookingStatus = null;
        for (BookingAction bookingAction = getPreviousBookingAction();
            bookingAction != null;
            bookingAction = bookingAction.getPreviousBookingAction()) {
          if (bookingAction instanceof UC5_Carrier_ConfirmBookingRequestAction) { // TODO or UC8 when created
            notificationBookingStatus = BookingState.CONFIRMED;
            break;
          }
          if (bookingAction instanceof UC6_Carrier_RequestUpdateToConfirmedBookingAction) {
            notificationBookingStatus = BookingState.PENDING_AMENDMENT;
            break;
          }
        }
        return Stream.concat(
            primaryExchangeChecks,
            getNotificationChecks(
                expectedApiVersion,
                notificationSchemaValidator,
                notificationBookingStatus,
                BookingState.AMENDMENT_RECEIVED));
      }
    };
  }
}
