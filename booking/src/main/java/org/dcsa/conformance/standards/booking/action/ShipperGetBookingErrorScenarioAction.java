package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;

public class ShipperGetBookingErrorScenarioAction extends BookingAction {

  private final JsonSchemaValidator responseSchemaValidator;

  public ShipperGetBookingErrorScenarioAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator responseSchemaValidator) {
    super(
        shipperPartyName,
        carrierPartyName,
        previousAction,
        "GET (Non existing booking)",
        404,
        true);
    this.responseSchemaValidator = responseSchemaValidator;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
        .put("cbrr", getDspSupplier().get().carrierBookingRequestReference())
        .put("cbr", getDspSupplier().get().carrierBookingReference())
        .put("invalidBookingReference", true);
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt("prompt-shipper-get-booking-error-scenario.md");
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new ResponseStatusCheck(
                BookingRole::isCarrier, getMatchedExchangeUuid(), expectedStatus),
            new ApiHeaderCheck(
                BookingRole::isShipper,
                getMatchedExchangeUuid(),
                HttpMessageType.REQUEST,
                expectedApiVersion),
            new NothingToCheck(getMatchedExchangeUuid(), HttpMessageType.REQUEST),
            new ApiHeaderCheck(
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                expectedApiVersion),
            new JsonSchemaCheck(
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator));
      }
    };
  }
}
