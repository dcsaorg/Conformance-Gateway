package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.function.Function;
import java.util.stream.Stream;

@Getter
@Slf4j
public class UC11_Shipper_CancelBookingRequestAction extends ShipperNotificationBookingAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final BookingState expectedBookingStatus;

  public UC11_Shipper_CancelBookingRequestAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    BookingState expectedBookingStatus,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator notificationSchemaValidator,
    boolean isWithNotifications) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC11", 202, isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.expectedBookingStatus = expectedBookingStatus;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
      "prompt-shipper-uc11.md", "prompt-shipper-refresh-complete.md");
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
        String cbrr = dsp.carrierBookingRequestReference();
        String cbr = dsp.carrierBookingReference();
        return Stream.of(
          createPatchPrimarySubChecks(expectedApiVersion, "/v2/bookings/", requestSchemaValidator, cbrr, cbr),
          getNotificationChecks(expectedApiVersion, notificationSchemaValidator, expectedBookingStatus, null)
        ).flatMap(Function.identity());
      }
    };
  }
}
