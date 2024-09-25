package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.BookingChecks;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;

@Getter
@Slf4j
public class UC3_Shipper_SubmitUpdatedBookingRequestAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC3_Shipper_SubmitUpdatedBookingRequestAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC3", 200);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC3: Submit an updated booking request");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("cbrr", getDspSupplier().get().carrierBookingRequestReference());
    return jsonNode;
  }

  @Override
  protected DynamicScenarioParameters updateDSPFromBookingAction(ConformanceExchange exchange, DynamicScenarioParameters dynamicScenarioParameters) {
    var body = exchange.getRequest().message().body().getJsonBody();
    return dynamicScenarioParameters.withBooking(body);
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
        return Stream.concat(
          Stream.of(
          new JsonSchemaCheck(
            BookingRole::isShipper,
            getMatchedExchangeUuid(),
            HttpMessageType.REQUEST,
            requestSchemaValidator),
          new JsonSchemaCheck(
            BookingRole::isCarrier,
            getMatchedExchangeUuid(),
            HttpMessageType.RESPONSE,
            responseSchemaValidator),
            BookingChecks.requestContentChecks(getMatchedExchangeUuid(),expectedApiVersion, getCspSupplier(), getDspSupplier())),
          Stream.concat(
            createPrimarySubChecks("PUT", expectedApiVersion, "/v2/bookings/%s".formatted(cbrr)),
            getNotificationChecks(
                expectedApiVersion,
                notificationSchemaValidator,
                BookingState.UPDATE_RECEIVED,
                null)));
      }
    };
  }
}
