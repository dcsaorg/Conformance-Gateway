package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceError;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

abstract class AbstractCarrierPayloadConformanceCheck extends PayloadContentConformanceCheck {

  protected static final Set<BookingState> BOOKING_STATES_WHERE_CBR_IS_OPTIONAL =
      Set.of(
          BookingState.RECEIVED,
          BookingState.REJECTED,
          BookingState.PENDING_UPDATE,
          BookingState.UPDATE_RECEIVED,
          BookingState.CANCELLED);

  protected final BookingState expectedBookingStatus;
  protected final BookingState expectedAmendedBookingStatus;
  protected final BookingCancellationState expectedBookingCancellationStatus;

  protected static final String FEEDBACKS = "feedbacks";

  protected AbstractCarrierPayloadConformanceCheck(
      UUID matchedExchangeUuid, HttpMessageType httpMessageType, BookingState bookingState) {
    this(matchedExchangeUuid, httpMessageType, bookingState, null, null);
  }

  protected AbstractCarrierPayloadConformanceCheck(
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      BookingState bookingState,
      BookingState expectedAmendedBookingStatus) {
    this(
        matchedExchangeUuid,
        httpMessageType,
        bookingState,
        expectedAmendedBookingStatus,
        null);
  }

  protected AbstractCarrierPayloadConformanceCheck(
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      BookingState bookingState,
      BookingState expectedAmendedBookingStatus,
      BookingCancellationState expectedBookingCancellationStatus) {
    super(
        "Validate the carrier payload",
        BookingRole::isCarrier,
        matchedExchangeUuid,
        httpMessageType);
    this.expectedBookingStatus = bookingState;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
    this.expectedBookingCancellationStatus = expectedBookingCancellationStatus;
  }

  protected ConformanceCheckResult ensureCarrierBookingReferenceCompliance(
      JsonNode responsePayload) {
    if (BOOKING_STATES_WHERE_CBR_IS_OPTIONAL.contains(expectedBookingStatus)) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    if (responsePayload.path("carrierBookingReference").isMissingNode()) {
      return ConformanceCheckResult.simple(
          Set.of("The 'carrierBookingReference' field was missing"));
    }
    return ConformanceCheckResult.simple(Collections.emptySet());
  }

  protected ConformanceCheckResult ensureBookingStatusIsCorrect(JsonNode responsePayload) {
    String actualState = responsePayload.path("bookingStatus").asText(null);
    String expectedState = expectedBookingStatus.name();
    if (Objects.equals(actualState, expectedState)) {
      return ConformanceCheckResult.simple(Collections.emptySet());
    }
    return ConformanceCheckResult.simple(
        Set.of(
            "Expected bookingStatus '%s' but found '%s'"
                .formatted(expectedState, Objects.requireNonNullElse(actualState, UNSET_MARKER))));
  }

  protected ConformanceCheckResult ensureAmendedBookingStatusIsCorrect(JsonNode responsePayload) {
    String actualState = responsePayload.path("amendedBookingStatus").asText(null);
    String expectedState =
        expectedAmendedBookingStatus != null ? expectedAmendedBookingStatus.name() : null;
    if (expectedState == null && actualState != null) {
      return ConformanceCheckResult.simple(
          Set.of(
              "The '%s' should not be present, but response contains value '%s'"
                  .formatted("amendedBookingStatus", actualState)));
    }
    if (Objects.equals(actualState, expectedState)) {
      return ConformanceCheckResult.simple(Collections.emptySet());
    }
    return ConformanceCheckResult.simple(
        Set.of(
            "Expected amendedBookingStatus '%s' but found '%s'"
                .formatted(expectedState, Objects.requireNonNullElse(actualState, UNSET_MARKER))));
  }

  protected ConformanceCheckResult ensureBookingCancellationStatusIsCorrect(
      JsonNode responsePayload) {
    String actualState = responsePayload.path("bookingCancellationStatus").asText(null);
    String expectedState =
        expectedBookingCancellationStatus != null ? expectedBookingCancellationStatus.name() : null;
    if (expectedState == null && actualState != null) {
      return ConformanceCheckResult.simple(
          Set.of(
              "The '%s' should not be present, but response contains value '%s'"
                  .formatted("bookingCancellationStatus", actualState)));
    }
    if (Objects.equals(actualState, expectedState)) {
      return ConformanceCheckResult.simple(Collections.emptySet());
    }
    return ConformanceCheckResult.simple(
        Set.of(
            "Expected bookingCancellationStatus '%s' but found '%s'"
                .formatted(expectedState, Objects.requireNonNullElse(actualState, UNSET_MARKER))));
  }

  protected ConformanceCheckResult ensureFeedbacksIsPresent(JsonNode responsePayload) {
    String bookingStatus = responsePayload.path("bookingStatus").asText(null);
    Set<String> errors = new HashSet<>();
    boolean isPendingUpdate = BookingState.PENDING_UPDATE.name().equals(bookingStatus);
    boolean isPendingAmendment = BookingState.PENDING_AMENDMENT.name().equals(bookingStatus);
    if (!isPendingUpdate && !isPendingAmendment) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    if (responsePayload.path(FEEDBACKS).isMissingNode()
        || responsePayload.path(FEEDBACKS).isEmpty()) {
      errors.add("feedbacks property is required in the booking state %s".formatted(bookingStatus));
    }
    return ConformanceCheckResult.simple(errors);
  }

  protected ConformanceCheckResult ensureFeedbackSeverityAndCodeCompliance(
      JsonNode responsePayload) {
    Set<String> errors = new HashSet<>();
    JsonNode feedbacks = responsePayload.path(FEEDBACKS);
    if (JsonUtil.isMissingOrEmpty(feedbacks)) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    for (JsonNode feedback : feedbacks) {
      String severity = feedback.path("severity").asText(null);
      String code = feedback.path("code").asText(null);
      if (!BookingDataSets.FEEDBACKS_SEVERITY.contains(severity)) {
        errors.add("Invalid feedback severity: " + severity);
      }
      if (!BookingDataSets.FEEDBACKS_CODE.contains(code)) {
        errors.add("Invalid feedback code: " + code);
      }
    }
    return ConformanceCheckResult.simple(errors);
  }
}
