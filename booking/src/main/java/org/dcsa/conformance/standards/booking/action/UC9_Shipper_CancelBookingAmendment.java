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
public class UC9_Shipper_CancelBookingAmendment extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final BookingState expectedBookingStatus;
  private final BookingState expectedAmendedBookingStatus;

  public UC9_Shipper_CancelBookingAmendment(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    BookingState expectedBookingStatus,
    BookingState expectedAmendedBookingStatus,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator responseSchemaValidator,
    JsonSchemaValidator notificationSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC9", 202);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.expectedBookingStatus = expectedBookingStatus;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
        "prompt-shipper-uc9.md", "prompt-shipper-refresh-complete.md");
  }

  @Override
  protected boolean expectsNotificationExchange() {
    return true;
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("cbrr", getDspSupplier().get().carrierBookingRequestReference());
    jsonNode.put("cbr", getDspSupplier().get().carrierBookingReference());
    return jsonNode;
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        String reference = dsp.carrierBookingReference() !=  null ? dsp.carrierBookingReference() : dsp.carrierBookingRequestReference();
        return Stream.concat(
          Stream.concat(createPrimarySubChecks("PATCH", expectedApiVersion, "/v2/bookings/%s".formatted(reference)),
            Stream.of(
              new JsonSchemaCheck(
                BookingRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator))),
          getNotificationChecks(
            expectedApiVersion,
            notificationSchemaValidator,
            expectedBookingStatus,
            expectedAmendedBookingStatus));
      }
    };
  }
}
