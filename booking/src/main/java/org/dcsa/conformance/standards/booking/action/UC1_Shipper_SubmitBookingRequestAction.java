package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;

@Getter
@Slf4j
public class UC1_Shipper_SubmitBookingRequestAction extends BookingAction {
  private final JsonSchemaValidator requestSchemaValidator;

  private final AtomicReference<String> carrierBookingRequestReference;

  public UC1_Shipper_SubmitBookingRequestAction(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator) {
    super(shipperPartyName, carrierPartyName, previousAction, "UC1", 201);
    this.requestSchemaValidator = requestSchemaValidator;
    this.carrierBookingRequestReference = previousAction != null ? null : new AtomicReference<>();
  }

  @Override
  protected Supplier<String> getCbrrSupplier() {
    return this.previousAction != null
        ? ((BookingAction) this.previousAction).getCbrrSupplier()
        : this.carrierBookingRequestReference::get;
  }

  @Override
  public ObjectNode exportJsonState() {
    ObjectNode jsonState = super.exportJsonState();
    if (carrierBookingRequestReference != null) {
      String cbrr = carrierBookingRequestReference.get();
      if (cbrr != null) {
        jsonState.put("cbrr", cbrr);
      }
    }
    return jsonState;
  }

  @Override
  public void importJsonState(JsonNode jsonState) {
    super.importJsonState(jsonState);
    if (carrierBookingRequestReference != null) {
      JsonNode cbrrNode = jsonState.get("cbrr");
      if (cbrrNode != null) {
        carrierBookingRequestReference.set(cbrrNode.asText());
      }
    }
  }

  @Override
  public String getHumanReadablePrompt() {
    return ("UC1: Submit a booking request");
  }

  @Override
  public ObjectNode asJsonNode() {
    ObjectNode jsonNode = super.asJsonNode();
    String cbrr = getCbrrSupplier().get();
    if (cbrr != null) {
      jsonNode.put("cbrr", cbrr);
    }
    return jsonNode;
  }

  @Override
  public void doHandleExchange(ConformanceExchange exchange) {
    JsonNode responseJsonNode = exchange.getResponse().message().body().getJsonBody();
    String exchangeCbrr = responseJsonNode.get("carrierBookingRequestReference").asText();
    if (carrierBookingRequestReference != null && carrierBookingRequestReference.get() == null) {
      carrierBookingRequestReference.set(exchangeCbrr);
    } else {
      String expectedCbrr = getCbrrSupplier().get();
      if (!Objects.equals(exchangeCbrr, expectedCbrr)) {
        throw new IllegalStateException(
            "Exchange CBRR '%s' does not match expected CBRR '%s'"
                .formatted(exchangeCbrr, expectedCbrr));
      }
    }
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
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
                    requestSchemaValidator))
            // .filter(Objects::nonNull)
            ;
      }
    };
  }
}
