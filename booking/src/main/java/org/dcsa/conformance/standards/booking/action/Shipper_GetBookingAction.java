package org.dcsa.conformance.standards.booking.action;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.CarrierGetBookingPayloadResponseConformanceCheck;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

public class Shipper_GetBookingAction extends BookingAction {

  private final BookingState expectedBookingStatus;
  private final BookingState expectedAmendedBookingStatus;
  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean requestAmendedStatus;

  public Shipper_GetBookingAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      BookingState expectedBookingStatus,
      BookingState expectedAmendedBookingStatus,
      JsonSchemaValidator responseSchemaValidator,
      boolean requestAmendedStatus) {
    super(shipperPartyName, carrierPartyName, previousAction, "GET", 200);
    this.expectedBookingStatus = expectedBookingStatus;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
    this.responseSchemaValidator = responseSchemaValidator;
    this.requestAmendedStatus = requestAmendedStatus;
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
                "/v2/bookings/" + getDspSupplier().get().carrierBookingRequestReference()),
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
              responseSchemaValidator
            ),
            new CarrierGetBookingPayloadResponseConformanceCheck(
              getMatchedExchangeUuid(),
              expectedBookingStatus,
              expectedAmendedBookingStatus,
              requestAmendedStatus
            ),
            new ActionCheck(
                "GET returns the expected Booking data",
                BookingRole::isCarrier,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE) {
              @Override
              protected Set<String> checkConformance(
                  Function<UUID, ConformanceExchange> getExchangeByUuid) {
                ConformanceExchange getExchange = getExchangeByUuid.apply(getMatchedExchangeUuid());
                if (getExchange == null) return Set.of();
                Set<String> conformanceErrors = new HashSet<>();
                if (previousAction
                    instanceof UC1_Shipper_SubmitBookingRequestAction submitBookingRequestAction) {
                  ConformanceExchange submitBookingRequestExchange =
                      getExchangeByUuid.apply(submitBookingRequestAction.getMatchedExchangeUuid());
                  if (submitBookingRequestExchange == null) return Set.of();
                  // this is just an example
                  String uc1CarrierServiceName =
                      JsonToolkit.getTextAttributeOrNull(
                          submitBookingRequestExchange.getRequest().message().body().getJsonBody(),
                          "carrierServiceName");
                  String getCarrierServiceName =
                      JsonToolkit.getTextAttributeOrNull(
                          getExchange.getResponse().message().body().getJsonBody(),
                          "carrierServiceName");
                  if (!Objects.equals(uc1CarrierServiceName, getCarrierServiceName)) {
                    conformanceErrors.add(
                        "Expected carrierServiceName '%s' but found '%s'"
                            .formatted(uc1CarrierServiceName, getCarrierServiceName));
                  }
                }
                return conformanceErrors;
              }
            })
        // .filter(Objects::nonNull)
        ;
      }
    };
  }
}
