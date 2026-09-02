package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.checks.BookingChecks;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standardscommons.action.BookingAndEblAction;

import java.util.function.Function;
import java.util.stream.Stream;

@Getter
@Slf4j
public class UC7_Shipper_SubmitBookingAmendment extends ShipperNotificationBookingAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final BookingState expectedBookingStatus;
  private final BookingState expectedAmendedBookingStatus;

  public UC7_Shipper_SubmitBookingAmendment(
    String carrierPartyName,
    String shipperPartyName,
    BookingAndEblAction previousAction,
    BookingState expectedBookingStatus,
    BookingState expectedAmendedBookingStatus,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator notificationSchemaValidator,
    boolean isWithNotifications) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC7", 202, isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.expectedBookingStatus = expectedBookingStatus;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
      "prompt-shipper-uc7.md", "prompt-shipper-refresh-complete.md");
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
          getNotificationChecks(
            expectedApiVersion,
            notificationSchemaValidator,
            expectedBookingStatus,
            expectedAmendedBookingStatus)
        ).flatMap(Function.identity());
      }
    };
  }
}
