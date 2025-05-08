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
      JsonSchemaValidator responseSchemaValidator
) {
    super(
        shipperPartyName,
        carrierPartyName,
        previousAction,
         "GET (Non existing booking)",
        404);
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
    return getMarkdownHumanReadablePrompt(
            "prompt-shipper-get.md", "prompt-shipper-refresh-complete.md")
        .replace("ORIGINAL_OR_AMENDED_PLACEHOLDER","ORIGINAL");
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        String reference = dsp.carrierBookingReference() !=  null ? dsp.carrierBookingReference() : dsp.carrierBookingRequestReference();
        return Stream.of(
            new UrlPathCheck(
                BookingRole::isShipper,
                getMatchedExchangeUuid(),
                "/v2/bookings/" + reference),
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
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE,
                responseSchemaValidator))
        ;
      }
    };
  }
}
