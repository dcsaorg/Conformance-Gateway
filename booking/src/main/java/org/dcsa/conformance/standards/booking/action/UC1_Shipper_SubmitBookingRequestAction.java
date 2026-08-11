package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.booking.checks.BookingChecks;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@Getter
@Slf4j
public class UC1_Shipper_SubmitBookingRequestAction extends ShipperNotificationBookingAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;

  public UC1_Shipper_SubmitBookingRequestAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator notificationSchemaValidator,
    boolean isWithNotifications) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC1", 202, isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    ScenarioType scenarioType = ScenarioType.valueOf(getDspSupplier().get().scenarioType());
    return getMarkdownHumanReadablePrompt(
      "prompt-shipper-uc1.md", "prompt-shipper-refresh-complete.md")
      .replace(
        "BOOKING_TYPE_PLACEHOLDER",
        scenarioType == ScenarioType.ANY
          ? "a booking request using any supported cargo type"
          : "a %s booking request".formatted(scenarioType.getDisplayName()));
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.set("bookingPayload", getBookingPayloadSupplier().get());
    jsonNode.put("scenarioType", getDspSupplier().get().scenarioType());
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
        return Stream.of(
          createPrimarySubChecks("POST", expectedApiVersion, "/v2/bookings", requestSchemaValidator),
          Stream.of(
            BookingChecks.requestContentChecks(getMatchedExchangeUuid(), expectedApiVersion, getDspSupplier())
          ),
          getNotificationChecks(expectedApiVersion, notificationSchemaValidator, BookingState.RECEIVED, null)
        ).flatMap(Function.identity());
      }
    };
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    getBookingPayloadConsumer().accept(OBJECT_MAPPER.createObjectNode());
  }
}
