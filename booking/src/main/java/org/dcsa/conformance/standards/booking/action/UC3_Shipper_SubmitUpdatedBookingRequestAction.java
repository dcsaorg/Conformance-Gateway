package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.checks.BookingChecks;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.function.Function;
import java.util.stream.Stream;

@Getter
@Slf4j
public class UC3_Shipper_SubmitUpdatedBookingRequestAction extends ShipperNotificationBookingAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final BookingState expectedBookingState;

  public UC3_Shipper_SubmitUpdatedBookingRequestAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    BookingState expectedBookingState,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator notificationSchemaValidator,
    boolean isWithNotifications) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC3", 202, isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.expectedBookingState = expectedBookingState;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
      "prompt-shipper-uc3.md", "prompt-shipper-refresh-complete.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("cbrr", getDspSupplier().get().carrierBookingRequestReference());
    jsonNode.put("cbr", getDspSupplier().get().carrierBookingReference());
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
        var dsp = getDspSupplier().get();
        String cbrr = dsp.carrierBookingRequestReference();
        String cbr = dsp.carrierBookingReference();
        return Stream.of(
          createPrimarySubChecks("PUT", expectedApiVersion, "/v2/bookings/", requestSchemaValidator, cbrr, cbr),
          Stream.of(
            BookingChecks.updateRequestContentChecks(getMatchedExchangeUuid(), expectedApiVersion, getDspSupplier())
          ),
          getNotificationChecks(expectedApiVersion, notificationSchemaValidator, expectedBookingState, null)
        ).flatMap(Function.identity());
      }
    };
  }
}
