package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

abstract class AbstractCarrierPayloadConformanceCheck extends PayloadContentConformanceCheck {


  protected static final Set<BookingState> PENDING_CHANGES_STATES = Set.of(
    BookingState.PENDING_UPDATE,
    BookingState.PENDING_AMENDMENT
  );

  protected static final Set<BookingState> REASON_STATES = Set.of(
    BookingState.PENDING_UPDATE,
    BookingState.PENDING_AMENDMENT,
    BookingState.DECLINED,
    BookingState.REJECTED,
    BookingState.CANCELLED,
    BookingState.AMENDMENT_DECLINED,
    BookingState.AMENDMENT_CANCELLED
  );

  protected static final Set<BookingCancellationState> CANCELLATION_REASON_STATES = Set.of(
    BookingCancellationState.CANCELLATION_RECEIVED,
    BookingCancellationState.CANCELLATION_CONFIRMED,
    BookingCancellationState.CANCELLATION_DECLINED
  );

  protected static final Set<BookingState> BOOKING_STATES_WHERE_CBR_IS_OPTIONAL = Set.of(
    BookingState.RECEIVED,
    BookingState.REJECTED,
    BookingState.PENDING_UPDATE,
    BookingState.UPDATE_RECEIVED,
    /* CANCELLED depends on whether cancel happens before CONFIRMED, but the logic does not track prior
     * states. Therefore, we just assume it is optional in CANCELLED here.
     */
    BookingState.CANCELLED
  );

  protected final BookingState expectedBookingStatus;
  protected final BookingState expectedAmendedBookingStatus;
  protected final BookingCancellationState expectedBookingCancellationStatus;
  protected final boolean amendedContent;

  protected AbstractCarrierPayloadConformanceCheck(UUID matchedExchangeUuid, HttpMessageType httpMessageType, BookingState bookingState) {
    this(matchedExchangeUuid, httpMessageType, bookingState, null, null,false);
  }

  protected AbstractCarrierPayloadConformanceCheck(UUID matchedExchangeUuid, HttpMessageType httpMessageType, BookingState bookingState, BookingState expectedAmendedBookingStatus) {
    this(matchedExchangeUuid, httpMessageType, bookingState, expectedAmendedBookingStatus, null,false);
  }

  protected AbstractCarrierPayloadConformanceCheck(
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    BookingState bookingState,
    BookingState expectedAmendedBookingStatus,
    BookingCancellationState expectedBookingCancellationStatus,
    boolean amendedContent
  ) {
    super(
      "Validate the carrier payload",
      BookingRole::isCarrier,
      matchedExchangeUuid,
      httpMessageType
    );
    this.expectedBookingStatus = bookingState;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
    this.expectedBookingCancellationStatus = expectedBookingCancellationStatus;
    this.amendedContent = amendedContent;
  }

  protected Set<String> ensureCarrierBookingReferenceCompliance(JsonNode responsePayload) {
    if (BOOKING_STATES_WHERE_CBR_IS_OPTIONAL.contains(expectedBookingStatus)) {
      return Collections.emptySet();
    }
    if (responsePayload.path("carrierBookingReference").isMissingNode()) {
      return Set.of("The 'carrierBookingReference' field was missing");
    }
    return Collections.emptySet();
  }

  protected Set<String> ensureBookingStatusIsCorrect(JsonNode responsePayload) {
    String actualState = responsePayload.path("bookingStatus").asText(null);
    String expectedState = expectedBookingStatus.name();
    if (Objects.equals(actualState, expectedState)) {
      return Collections.emptySet();
    }
    return Set.of("Expected bookingStatus '%s' but found '%s'"
      .formatted(expectedState, Objects.requireNonNullElse(actualState, UNSET_MARKER)));
  }

  protected Set<String> ensureAmendedBookingStatusIsCorrect(JsonNode responsePayload) {
    String actualState = responsePayload.path("amendedBookingStatus").asText(null);
    String expectedState = expectedAmendedBookingStatus != null ? expectedAmendedBookingStatus.name() : null;
    if (Objects.equals(actualState, expectedState)) {
      return Collections.emptySet();
    }
    return Set.of("Expected amendedBookingStatus '%s' but found '%s'"
      .formatted(
        Objects.requireNonNullElse(expectedState, UNSET_MARKER),
        Objects.requireNonNullElse(actualState, UNSET_MARKER)));
  }

  protected Set<String> ensureBookingCancellationStatusIsCorrect(JsonNode responsePayload) {
    String actualState = responsePayload.path("bookingCancellationStatus").asText(null);
    String expectedState = expectedBookingCancellationStatus != null ? expectedBookingCancellationStatus.name() : null;
    if (Objects.equals(actualState, expectedState)) {
      return Collections.emptySet();
    }
    return Set.of("Expected bookingCancellationStatus '%s' but found '%s'"
      .formatted(
        Objects.requireNonNullElse(expectedState, UNSET_MARKER),
        Objects.requireNonNullElse(actualState, UNSET_MARKER)));
  }


  protected boolean expectedStateMatch(Set<BookingState> states) {
    return expectedStateMatch(states::contains);
  }

  protected boolean expectedStateMatch(Predicate<BookingState> check) {
    var state = Objects.requireNonNullElse(expectedAmendedBookingStatus, expectedBookingStatus);
    return check.test(state);
  }

  protected boolean expectedCancellationStateMatch(Predicate<BookingCancellationState> check) {
    if (expectedBookingCancellationStatus != null) {
      return check.test(expectedBookingCancellationStatus);
    }
    return false;
  }

  protected Function<JsonNode, Set<String>> requiredOrExcludedByState(Set<BookingState> conditionalInTheseStates, Set<BookingCancellationState> cancellationConditionalStates, String fieldName) {
    if (expectedCancellationStateMatch(cancellationConditionalStates::contains)) {
      return jsonNode -> Collections.emptySet();
    }
    if (expectedStateMatch(conditionalInTheseStates)) {
      return payload -> nonEmptyField(payload, fieldName);
    }
    return payload -> fieldIsOmitted(payload, fieldName);
  }


  protected Function<JsonNode, Set<String>> reasonFieldRequiredForCancellation(Set<BookingState> conditionalInTheseStates, Set<BookingCancellationState> cancellationConditionalStates, String fieldName) {
    if (expectedCancellationStateMatch(cancellationConditionalStates::contains)) {
      return payload -> nonEmptyField(payload, fieldName);
    }
    if (expectedStateMatch(conditionalInTheseStates)) {
      return payload -> nonEmptyField(payload, fieldName);
    }
    return payload -> fieldIsOmitted(payload, fieldName);
  }

  protected Set<String> fieldIsOmitted(JsonNode responsePayload, String key) {
    if (responsePayload.has(key) || responsePayload.get(key) != null) {
      return Set.of("The field '%s' must *NOT* be a present for a booking in status '%s'".formatted(
        key, this.expectedBookingStatus.name()
      ));
    }
    return Collections.emptySet();
  }

  protected Set<String> nonEmptyField(JsonNode responsePayload, String key) {
    var field = responsePayload.get(key);
    if (isNonEmptyNode(field)) {
      return Collections.emptySet();
    }
    return Set.of("The field '%s' must be a present and non-empty for a booking in status '%s'".formatted(
      key, this.expectedBookingStatus.name()
    ));
  }
}
