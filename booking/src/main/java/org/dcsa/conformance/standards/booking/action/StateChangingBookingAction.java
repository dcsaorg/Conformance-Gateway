package org.dcsa.conformance.standards.booking.action;

import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;

import java.util.stream.Stream;

public abstract class StateChangingBookingAction extends BookingAction {
  protected StateChangingBookingAction(String sourcePartyName, String targetPartyName, BookingAction previousAction,
                                    String actionTitle, int expectedStatus) {
    super(sourcePartyName, targetPartyName, previousAction, actionTitle, expectedStatus);
  }

  @Override
  protected void doHandleExchange(ConformanceExchange exchange) {
    super.doHandleExchange(exchange);
    updateDSPFromResponsePayload(exchange);
  }

  protected Stream<ActionCheck> createPrimarySubChecks(String httpMethod,
                                                       String expectedApiVersion,
                                                       String uri) {
    return Stream.of(
      new HttpMethodCheck(BookingRole::isShipper, getMatchedExchangeUuid(), httpMethod),
      new UrlPathCheck(BookingRole::isShipper, getMatchedExchangeUuid(), uri),
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
        expectedApiVersion));
  }
}
