package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.model.InvalidBookingMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;

@Getter
@Slf4j
public class AUCShipperSendInvalidBookingAction extends StateChangingBookingAction {
  private final JsonSchemaValidator requestSchemaValidator;
  private final InvalidBookingMessageType invalidBookingMessageType;

  public AUCShipperSendInvalidBookingAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      InvalidBookingMessageType invalidBookingMessageType,
      JsonSchemaValidator requestSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "AUC-InvalidBookingAction [%s]".formatted(invalidBookingMessageType.getUC()), 409);
    this.invalidBookingMessageType = invalidBookingMessageType;
    this.requestSchemaValidator = requestSchemaValidator;
  }

  @Override
  public String getHumanReadablePrompt() {
    // no markdown instructions needed: never expected to be performed by a human operator
    var dsp = getDspSupplier().get();
    return ("AUC: Send an invalid booking action but otherwise valid message of type %s (%s) to the booking reference %s".formatted(
      invalidBookingMessageType.name(),
      invalidBookingMessageType.getUC(),
      dsp.carrierBookingRequestReference()
    ));
  }

  @Override
  public ObjectNode asJsonNode() {
    var dsp = getDspSupplier().get();
    return super.asJsonNode()
      .put("invalidBookingMessageType", this.invalidBookingMessageType.name())
      .put("cbrr", dsp.carrierBookingRequestReference())
      .put("cbr", dsp.carrierBookingReference());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        var urlFormat = invalidBookingMessageType.getExpectedRequestUrlFormat();
        String reference = dsp.carrierBookingReference() !=  null ? dsp.carrierBookingReference() : dsp.carrierBookingRequestReference();
        if (InvalidBookingMessageType.CANCEL_BOOKING.equals(invalidBookingMessageType)){
          reference = dsp.carrierBookingRequestReference();
        }
        return Stream.of(
            new HttpMethodCheck(BookingRole::isShipper, getMatchedExchangeUuid(), invalidBookingMessageType.getExpectedRequestMethod()),
            new UrlPathCheck(BookingRole::isShipper, getMatchedExchangeUuid(), urlFormat.formatted(reference)),
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
                requestSchemaValidator));
      }
    };
  }
}
