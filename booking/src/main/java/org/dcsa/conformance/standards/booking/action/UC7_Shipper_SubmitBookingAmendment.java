package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.BookingChecks;
import org.dcsa.conformance.standards.booking.checks.CarrierBookingRefStatusPayloadResponseConformanceCheck;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;

import java.util.Set;
import java.util.stream.Stream;

@Getter
@Slf4j
public class UC7_Shipper_SubmitBookingAmendment extends StateChangingBookingAction {
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
    super(shipperPartyName, carrierPartyName, previousAction, "UC7", Set.of(200, 202));
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
    jsonNode.put("cbr", getDspSupplier().get().carrierBookingReference());
    return jsonNode;
  }

  @Override
  protected DynamicScenarioParameters updateDSPFromBookingAction(ConformanceExchange exchange, DynamicScenarioParameters dynamicScenarioParameters) {
    var body = exchange.getRequest().message().body().getJsonBody();
    return dynamicScenarioParameters.withUpdatedBooking(body);
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
        var dsp = getDspSupplier().get();
        String reference = dsp.carrierBookingReference() !=  null ? dsp.carrierBookingReference() : dsp.carrierBookingRequestReference();
        var expectedBookingStatus = dsp.bookingStatus();
        Stream<ActionCheck> primaryExchangeChecks =
            Stream.of(
                new HttpMethodCheck(BookingRole::isShipper, getMatchedExchangeUuid(), "PUT"),
                new UrlPathCheck(
                    BookingRole::isShipper,
                    getMatchedExchangeUuid(),
                    "/v2/bookings/%s".formatted(reference)),
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
        return Stream.concat(
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
