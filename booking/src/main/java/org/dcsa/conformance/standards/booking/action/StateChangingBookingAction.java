package org.dcsa.conformance.standards.booking.action;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ApiHeaderCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceError;
import org.dcsa.conformance.core.check.HttpMethodCheck;
import org.dcsa.conformance.core.check.ResponseStatusCheck;
import org.dcsa.conformance.core.check.UrlPathCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standardscommons.action.BookingAndEblAction;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class StateChangingBookingAction extends BookingAction {

  protected static final String BOOKING_STATUS = "bookingStatus";
  protected static final String AMENDED_BOOKING_STATUS = "amendedBookingStatus";

  protected static String jsonPath(String... segments) {
    return "'%s'".formatted(String.join(".", segments));
  }

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
    String httpMethod, String expectedApiVersion, String uri, String... uriReference) {
    return Stream.of(
      new HttpMethodCheck(BookingRole::isShipper, getMatchedExchangeUuid(), httpMethod),
      new UrlPathCheck(
        BookingRole::isShipper, getMatchedExchangeUuid(), buildFullUris(uri, uriReference)),
      new ResponseStatusCheck(BookingRole::isCarrier, getMatchedExchangeUuid(), expectedStatus),
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

  protected Stream<ActionCheck> createPatchPrimarySubChecks(
    String expectedApiVersion, String uri, String... uriReference) {
    return Stream.of(
      new HttpMethodCheck(BookingRole::isShipper, getMatchedExchangeUuid(), "PATCH"),
      new UrlPathCheck(
        BookingRole::isShipper, getMatchedExchangeUuid(), buildFullUris(uri, uriReference)),
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

  protected Stream<ActionCheck> patchPreconditionChecks(
    String description,
    String priorStatusField,
    Predicate<String> precondition,
    String expectedPriorStateDescription,
    int preconditionFailureStatus) {

    ConformanceAction precedingAction = previousAction;
    UUID patchExchangeUuid = getMatchedExchangeUuid();

    ActionCheck shipperPreconditionCheck = new ActionCheck(
      description, BookingRole::isShipper, patchExchangeUuid, HttpMessageType.REQUEST) {
      @Override
      protected ConformanceCheckResult performCheck(
        Function<UUID, ConformanceExchange> getExchangeByUuid) {
        if (wasPreviousShipperGetActionSkipped()) {
          return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
        }
        JsonNode priorStatus = findLatestPriorStatus(getExchangeByUuid, precedingAction, priorStatusField);
        if (priorStatus == null) {
          return ConformanceCheckResult.simple(
            Set.of(
              "Could not determine the prior %s from an earlier Booking response or notification"
                .formatted(jsonPath(priorStatusField))));
        }

        String actualPriorState = priorStatus.asText("");
        String renderedPriorState =
          priorStatus.isMissingNode() ? "absent" : "'%s'".formatted(actualPriorState);
        if (!precondition.test(actualPriorState)) {
          return ConformanceCheckResult.simple(
            Set.of(
              "Prior %s was %s but the active scenario requires %s"
                .formatted(
                  jsonPath(priorStatusField), renderedPriorState, expectedPriorStateDescription)));
        }
        return ConformanceCheckResult.simple(Collections.emptySet());
      }
    };

    ActionCheck carrierResponseStatusCheck = new ActionCheck(
      "[Scenario] The HTTP response status is correct for the applicable PATCH business precondition",
      BookingRole::isCarrier,
      patchExchangeUuid,
      HttpMessageType.RESPONSE) {
      @Override
      protected ConformanceCheckResult performCheck(
        Function<UUID, ConformanceExchange> getExchangeByUuid) {
        JsonNode priorStatus = findLatestPriorStatus(getExchangeByUuid, precedingAction, priorStatusField);
        if (priorStatus == null) {
          return ConformanceCheckResult.simple(
            Set.of(
              "Could not determine the prior %s from an earlier Booking response or notification"
                .formatted(jsonPath(priorStatusField))));
        }
        String actualPriorState = priorStatus.asText("");
        int expectedResponseStatus =
          precondition.test(actualPriorState) ? expectedStatus : preconditionFailureStatus;
        ConformanceExchange patchExchange = getExchangeByUuid.apply(patchExchangeUuid);
        if (patchExchange == null) {
          return ConformanceCheckResult.simple(Collections.emptySet());
        }
        int actualResponseStatus = patchExchange.getResponse().statusCode();
        if (actualResponseStatus != expectedResponseStatus) {
          return ConformanceCheckResult.simple(
            Set.of(
              "Prior %s was '%s' (required: %s), so PATCH response status must be %d but was %d"
                .formatted(
                  jsonPath(priorStatusField),
                  actualPriorState,
                  expectedPriorStateDescription,
                  expectedResponseStatus,
                  actualResponseStatus)));
        }
        return ConformanceCheckResult.simple(Collections.emptySet());
      }
    };
    return Stream.of(shipperPreconditionCheck, carrierResponseStatusCheck);
  }

  private boolean wasPreviousShipperGetActionSkipped() {
    return previousAction instanceof ShipperGetBookingSkippableAction && previousAction.isMissingMatchedExchange();
  }

  private static JsonNode findLatestPriorStatus(
    Function<UUID, ConformanceExchange> getExchangeByUuid,
    ConformanceAction action,
    String statusField) {
    return Stream.iterate(action, Objects::nonNull, ConformanceAction::getPreviousAction)
      .map(ConformanceAction::getMatchedExchangeUuid)
      .filter(Objects::nonNull)
      .map(getExchangeByUuid)
      .filter(Objects::nonNull)
      .map(exchange -> exchange.getResponse().message().body().getJsonBody())
      .filter(response -> !response.path(BOOKING_STATUS).isMissingNode())
      .findFirst()
      .map(response -> response.path(statusField))
      .orElse(null);
  }
}
