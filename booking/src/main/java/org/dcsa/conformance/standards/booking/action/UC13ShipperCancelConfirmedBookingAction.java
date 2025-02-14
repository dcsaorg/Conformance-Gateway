package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

@Getter
@Slf4j
public class UC13ShipperCancelConfirmedBookingAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final BookingState expectedBookingStatus;
  private final BookingState expectedAmendedBookingStatus;
  private final BookingCancellationState expectedBookingCancellationStatus;

  public UC13ShipperCancelConfirmedBookingAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      BookingState expectedBookingStatus,
      BookingState expectedAmendedBookingStatus,
      BookingCancellationState expectedBookingCancellationStatus,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC13", 202);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.expectedBookingStatus = expectedBookingStatus;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
    this.expectedBookingCancellationStatus = expectedBookingCancellationStatus;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        "prompt-shipper-uc13.md", "prompt-shipper-refresh-complete.md");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return getCspSupplier().get().toJson();
  }

  @Override
  protected boolean expectsNotificationExchange() {
    return true;
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("cbr", getDspSupplier().get().carrierBookingReference());
    return jsonNode;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        String cbr = dsp.carrierBookingReference();
        return Stream.concat(
          Stream.concat(createPrimarySubChecks("PATCH",expectedApiVersion,"/v2/bookings/%s".formatted(cbr)),
            Stream.of(
              new JsonSchemaCheck(
                BookingRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator))),
          expectedBookingStatus != null ?  getNotificationChecks(
            expectedApiVersion,
            notificationSchemaValidator,
            expectedBookingStatus,
            expectedAmendedBookingStatus,
            expectedBookingCancellationStatus): Stream.empty());
      }
    };
  }
}
