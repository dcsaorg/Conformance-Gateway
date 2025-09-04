package org.dcsa.conformance.standards.booking.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

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

@Getter
@Slf4j
public class UC1_Shipper_SubmitBookingRequestAction extends StateChangingBookingAction {
  
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC1_Shipper_SubmitBookingRequestAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator,
      JsonSchemaValidator notificationSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC1", 202);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    String prompt =
        getMarkdownHumanReadablePrompt(
                "prompt-shipper-uc1.md", "prompt-shipper-refresh-complete.md")
            .replace(
                "BOOKING_TYPE_PLACEHOLDER",
                switch (getDspSupplier().get().scenarioType()) {
                  case DG -> "DG";
                  case REEFER -> "Reefer";
                  case NON_OPERATING_REEFER -> "Non-Operating Reefer";
                  default -> "Dry Cargo";
                });
    return prompt.replace(
        "CARRIER_SCENARIO_PARAMETERS", getBookingPayloadSupplier().get().toString());
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.set("bookingPayload", getBookingPayloadSupplier().get());
    jsonNode.put("scenarioType", getDspSupplier().get().scenarioType().name());
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
        return Stream.concat(
            Stream.of(
                BookingChecks.requestContentChecks(
                    getMatchedExchangeUuid(), expectedApiVersion, getDspSupplier()),
                new JsonSchemaCheck(
                    BookingRole::isShipper,
                    getMatchedExchangeUuid(),
                    HttpMessageType.REQUEST,
                    requestSchemaValidator)),
            Stream.concat(
                createPrimarySubChecks("POST", expectedApiVersion, "/v2/bookings"),
                getNotificationChecks(
                    expectedApiVersion, notificationSchemaValidator, BookingState.RECEIVED, null)));
      }
    };
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    getBookingPayloadConsumer().accept(OBJECT_MAPPER.createObjectNode());
  }
}
