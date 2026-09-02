package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.standards.booking.checks.BookingChecks;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@Getter
@Slf4j
public class UC1_Shipper_SubmitBookingRequestAction extends ShipperNotificationBookingAction {

  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;
  private final JsonSchemaValidator notificationSchemaValidator;
  private final ScenarioType configuredScenarioType;
  private final String standardVersion;
  private JsonNode bookingPayload;

  private void seedStandaloneState() {
    if (previousAction != null) {
      return;
    }
    ScenarioType seedScenarioType = configuredScenarioType == null ? ScenarioType.ANY : configuredScenarioType;
    this.bookingPayload = standardVersion == null
      ? OBJECT_MAPPER.createObjectNode()
      : JsonToolkit.templateFileToJsonNode(
      "/standards/booking/messages/" + seedScenarioType.bookingPayload(standardVersion),
      Map.of());
    getDspConsumer().accept(getDspSupplier().get().withScenarioType(seedScenarioType.name()));
  }

  public UC1_Shipper_SubmitBookingRequestAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator responseSchemaValidator,
    JsonSchemaValidator notificationSchemaValidator,
    boolean isWithNotifications) {
    this(
      carrierPartyName,
      shipperPartyName,
      previousAction,
      requestSchemaValidator,
      responseSchemaValidator,
      notificationSchemaValidator,
      isWithNotifications,
      null,
      null,
      "UC1");
  }

  public UC1_Shipper_SubmitBookingRequestAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    JsonSchemaValidator requestSchemaValidator,
    JsonSchemaValidator responseSchemaValidator,
    JsonSchemaValidator notificationSchemaValidator,
    boolean isWithNotifications,
    ScenarioType configuredScenarioType,
    String standardVersion,
    String actionTitle) {
    super(shipperPartyName, carrierPartyName, previousAction, actionTitle, 202, isWithNotifications);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
    this.notificationSchemaValidator = notificationSchemaValidator;
    this.configuredScenarioType = configuredScenarioType;
    this.standardVersion = standardVersion;
    seedStandaloneState();
  }

  @Override
  public void reset() {
    super.reset();
    seedStandaloneState();
  }

  private ScenarioType resolvedScenarioType() {
    String scenarioType = getDspSupplier().get().scenarioType();
    if (scenarioType != null) {
      return ScenarioType.valueOf(scenarioType);
    }
    return configuredScenarioType == null ? ScenarioType.ANY : configuredScenarioType;
  }

  @Override
  protected Consumer<JsonNode> getBookingPayloadConsumer() {
    if (previousAction == null) {
      return payload -> this.bookingPayload = payload;
    }
    return super.getBookingPayloadConsumer();
  }

  @Override
  protected Supplier<JsonNode> getBookingPayloadSupplier() {
    if (previousAction == null) {
      return () -> bookingPayload;
    }
    return super.getBookingPayloadSupplier();
  }

  @Override
  public String getHumanReadablePrompt() {
    ScenarioType scenarioType = resolvedScenarioType();
    return getMarkdownHumanReadablePrompt(
      "prompt-shipper-uc1.md", "prompt-shipper-refresh-complete.md")
      .replace(
        "BOOKING_TYPE_PLACEHOLDER",
        scenarioType == ScenarioType.ANY
          ? "a booking request using any supported cargo type"
          : "a %s booking request".formatted(scenarioType.getDisplayName()));
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    if (previousAction != null) {
      return super.getJsonForHumanReadablePrompt();
    }
    return getBookingPayloadSupplier().get();
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.set("bookingPayload", getBookingPayloadSupplier().get());
    jsonNode.put("scenarioType", resolvedScenarioType().name());
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
