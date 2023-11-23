package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;

@Getter
@Slf4j
public class UC1_Shipper_SubmitBookingRequestAction extends BookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final JsonSchemaValidator responseSchemaValidator;

  public UC1_Shipper_SubmitBookingRequestAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      JsonSchemaValidator responseSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC1", 201);
    this.requestSchemaValidator = requestSchemaValidator;
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC1: Submit a booking request using the following parameters:");
  }

  @Override
  public JsonNode getJsonForHumanReadablePrompt() {
    return getCspSupplier().get().toJson();
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.set("csp", getCspSupplier().get().toJson());
    return jsonNode;
  }

  @Override
  public void doHandleExchange(ConformanceExchange exchange) {
    JsonNode responseJsonNode = exchange.getResponse().message().body().getJsonBody();
    // FIXME: Guard against non-conformant parties
    getDspConsumer()
        .accept(
            new DynamicScenarioParameters(
                responseJsonNode.get("carrierBookingRequestReference").asText(),
                getDspSupplier().get().carrierBookingReference()));
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new HttpMethodCheck(BookingRole::isShipper, getMatchedExchangeUuid(), "POST"),
            new UrlPathCheck(BookingRole::isShipper, getMatchedExchangeUuid(), "/v2/bookings"),
            new ResponseStatusCheck(
                BookingRole::isCarrier, getMatchedExchangeUuid(), expectedStatus),
            new ApiHeaderCheck(
                BookingRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new ApiHeaderCheck(
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                BookingRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                requestSchemaValidator),
            new JsonSchemaCheck(
              BookingRole::isCarrier,
              getMatchedExchangeUuid(),
              HttpMessageType.RESPONSE,
              responseSchemaValidator))
        // .filter(Objects::nonNull)
        ;
      }
    };
  }
}
