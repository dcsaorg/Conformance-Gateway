package org.dcsa.conformance.standards.booking.action;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

public class Shipper_GetBookingAction extends BookingAction {
  private final BookingState expectedState;
  private final JsonSchemaValidator responseSchemaValidator;

  public Shipper_GetBookingAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      BookingState expectedState,
      JsonSchemaValidator responseSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "GET", 200);
    this.expectedState = expectedState;
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
              protected Set<String> checkConformance(
                  Function<UUID, ConformanceExchange> getExchangeByUuid) {
                ConformanceExchange getExchange = getExchangeByUuid.apply(getMatchedExchangeUuid());
                if (getExchange == null) return Set.of();
                String exchangeState =
                    getExchange
                        .getResponse()
                        .message()
                        .body()
                        .getJsonBody()
                        .get("bookingStatus")
                        .asText();
                Set<String> conformanceErrors = new HashSet<>();
                if (!Objects.equals(exchangeState, expectedState.wireName())) {
                  conformanceErrors.add(
                      "Expected bookingStatus '%s' but found '%s'"
                          .formatted(expectedState.wireName(), exchangeState));
                }
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
