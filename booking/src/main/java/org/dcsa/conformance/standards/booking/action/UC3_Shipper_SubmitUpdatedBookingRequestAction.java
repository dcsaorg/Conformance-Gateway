package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.BookingChecks;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

@Getter
@Slf4j
public class UC3_Shipper_SubmitUpdatedBookingRequestAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final BookingState expectedBookingState;

  public UC3_Shipper_SubmitUpdatedBookingRequestAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      BookingState expectedBookingState,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC3", 202);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.expectedBookingState = expectedBookingState;
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
            BookingChecks.requestContentChecks(getMatchedExchangeUuid(),expectedApiVersion, getCspSupplier(), getDspSupplier())),
          Stream.concat(
            createPrimarySubChecks("PUT", expectedApiVersion, "/v2/bookings/%s".formatted(cbrr)),
            getNotificationChecks(
                expectedApiVersion,
                notificationSchemaValidator,
                expectedBookingState,
                null)));
      }
    };
  }
}
