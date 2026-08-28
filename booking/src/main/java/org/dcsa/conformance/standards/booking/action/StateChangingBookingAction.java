package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.JsonSchemaCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standardscommons.action.BookingAndEblAction;

import java.util.stream.Stream;

public abstract class StateChangingBookingAction extends BookingAction {

  protected StateChangingBookingAction(
    String sourcePartyName,
    String targetPartyName,
    BookingAndEblAction previousAction,
    String actionTitle,
    int expectedStatus,
    boolean isWithNotifications) {
    super(
      sourcePartyName,
      targetPartyName,
      previousAction,
      actionTitle,
      expectedStatus,
      isWithNotifications);
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    updateDSPFromResponsePayload(exchange);
  }

  protected Stream<ActionCheck> createPrimarySubChecks(
    String httpMethod, String expectedApiVersion, String uri, JsonSchemaValidator requestSchemaValidator, String... uriReference) {
    return Stream.of(
      new HttpMethodCheck(BookingRole::isShipper, getMatchedExchangeUuid(), httpMethod),
      new UrlPathCheck(BookingRole::isShipper, getMatchedExchangeUuid(), buildFullUris(uri, uriReference)),
      ResponseStatusCheck.forSuccessfulResponse(BookingRole::isCarrier, getMatchedExchangeUuid()),
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

  protected Stream<ActionCheck> createPatchPrimarySubChecks(
    String expectedApiVersion, String uri, JsonSchemaValidator requestSchemaValidator, String... uriReference) {
    return Stream.of(
      new HttpMethodCheck(BookingRole::isShipper, getMatchedExchangeUuid(), "PATCH"),
      new UrlPathCheck(BookingRole::isShipper, getMatchedExchangeUuid(), buildFullUris(uri, uriReference)),
      ResponseStatusCheck.forSuccessfulResponse(BookingRole::isCarrier, getMatchedExchangeUuid()),
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

}
