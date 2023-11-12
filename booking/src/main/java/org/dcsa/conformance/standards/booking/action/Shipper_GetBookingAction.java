package org.dcsa.conformance.standards.booking.action;

import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;

public class Shipper_GetBookingAction extends BookingAction {
  private final JsonSchemaValidator responseSchemaValidator;

  public Shipper_GetBookingAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "GET", 200);
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    jsonNode.put("cbrr", getDspSupplier().get().carrierBookingRequestReference());
    return jsonNode;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "GET the booking with CBR '%s'"
        .formatted(getDspSupplier().get().carrierBookingReference());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(
                BookingRole::isShipper,
                getMatchedExchangeUuid(),
                "/v2/booking/" + getDspSupplier().get().carrierBookingRequestReference()),
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
                responseSchemaValidator),
            new ActionCheck(
                "GET returns the expected Booking data",
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE) {
              @Override
              protected Set<String> checkConformance(ConformanceExchange exchange) {
                // TODO
                // Pass UUID -> message function to doCheck() instead of exchange
                // Check against previousAction.getMatchedExchangeUuid()
                return null;
              }
            })
        // .filter(Objects::nonNull)
        ;
      }
    };
  }
}
