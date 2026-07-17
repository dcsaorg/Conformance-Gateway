package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.checks.BookingChecks;
import org.dcsa.conformance.standards.booking.checks.CarrierStatusScenario;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.Set;
import java.util.stream.Stream;

public class ShipperGetBookingAction extends BookingAction {

  private final BookingState expectedBookingStatus;
  private final CarrierStatusScenario carrierStatusScenario;
  private final JsonSchemaValidator responseSchemaValidator;
  private final boolean requestAmendedContent;

  public ShipperGetBookingAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    BookingState expectedBookingStatus,
    BookingState expectedAmendedBookingStatus,
    BookingCancellationState expectedCancelledBookingStatus,
    JsonSchemaValidator responseSchemaValidator,
    boolean requestAmendedStatus) {
    super(
      shipperPartyName,
      carrierPartyName,
      previousAction,
      requestAmendedStatus ? "GET (amended content)" : "GET",
      200,
      true);
    this.expectedBookingStatus = expectedBookingStatus;
    this.carrierStatusScenario =
      CarrierStatusScenario.from(
        expectedBookingStatus,
        expectedAmendedBookingStatus,
        expectedCancelledBookingStatus);
    this.responseSchemaValidator = responseSchemaValidator;
    this.requestAmendedContent = requestAmendedStatus;
  }

  public ShipperGetBookingAction(
    String carrierPartyName,
    String shipperPartyName,
    BookingAction previousAction,
    CarrierStatusScenario carrierStatusScenario,
    JsonSchemaValidator responseSchemaValidator,
    boolean requestAmendedContent) {
    super(
      shipperPartyName,
      carrierPartyName,
      previousAction,
      requestAmendedContent ? "GET (amended content)" : "GET",
      200,
      true);
    this.expectedBookingStatus = BookingState.CONFIRMED;
    this.carrierStatusScenario = carrierStatusScenario;
    this.responseSchemaValidator = responseSchemaValidator;
    this.requestAmendedContent = requestAmendedContent;
  }

  @Override
  public ObjectNode asJsonNode() {
    return super.asJsonNode()
      .put("cbrr", getDspSupplier().get().carrierBookingRequestReference())
      .put("cbr", getDspSupplier().get().carrierBookingReference())
      .put("amendedContent", requestAmendedContent);
  }

  @Override
  public String getHumanReadablePrompt() {
    return getMarkdownHumanReadablePrompt(
      "prompt-shipper-get.md", "prompt-shipper-refresh-complete.md")
      .replace("ORIGINAL_OR_AMENDED_PLACEHOLDER", requestAmendedContent ? "AMENDED" : "ORIGINAL");
  }

  @Override
  public Set<String> skippableForRoles() {
    return Set.of(BookingRole.SHIPPER.getConfigName());
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        var dsp = getDspSupplier().get();
        String cbrr = dsp.carrierBookingRequestReference();
        String cbr = dsp.carrierBookingReference();
        Set<Integer> expectedStatuses = BookingState.PENDING_AMENDMENT.equals(expectedBookingStatus)
          ? Set.of(expectedStatus, 202)
          : Set.of(expectedStatus);
        return Stream.of(
          new UrlPathCheck(
            BookingRole::isShipper,
            getMatchedExchangeUuid(),
            buildFullUris("/v2/bookings/", cbrr, cbr)),
          new ResponseStatusCheck(
            BookingRole::isCarrier, getMatchedExchangeUuid(), expectedStatuses),
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
            responseSchemaValidator),
          BookingChecks.responseContentChecks(
            getMatchedExchangeUuid(),
            expectedApiVersion,
            getDspSupplier(),
            carrierStatusScenario));
      }
    };
  }
}
