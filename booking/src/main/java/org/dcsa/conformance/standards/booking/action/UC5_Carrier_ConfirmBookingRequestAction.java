package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.stream.Stream;

@Getter
public class UC5_Carrier_ConfirmBookingRequestAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;

  public UC5_Carrier_ConfirmBookingRequestAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    JsonSchemaValidator requestSchemaValidator,
    boolean isWithNotifications) {
    super(carrierPartyName, shipperPartyName, previousAction, "UC5", 204, isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
      "prompt-carrier-uc5.md", "prompt-carrier-notification.md");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    return jsonNode
      .put("cbrr", getDspSupplier().get().carrierBookingRequestReference())
      .put("cbr", getDspSupplier().get().carrierBookingReference());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return getSimpleNotificationChecks(
          expectedApiVersion, requestSchemaValidator, BookingState.CONFIRMED);
      }
    };
  }
}
