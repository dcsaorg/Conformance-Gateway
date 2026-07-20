package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class CarrierStatusScenario {

  private enum CombinationRule {
    NONE,
    UC8,
    UC14
  }

  private static final Set<BookingState> CONFIRMED_OR_PENDING_AMENDMENT =
    Set.of(BookingState.CONFIRMED, BookingState.PENDING_AMENDMENT);
  private static final Set<BookingState> ANY_AMENDMENT_STATUS =
    Set.of(
      BookingState.AMENDMENT_RECEIVED,
      BookingState.AMENDMENT_CONFIRMED,
      BookingState.AMENDMENT_DECLINED,
      BookingState.AMENDMENT_CANCELLED);

  private final Set<BookingState> bookingStatuses;
  private final Set<BookingState> amendedBookingStatuses;
  private final boolean amendedBookingStatusRequired;
  private final Set<BookingCancellationState> bookingCancellationStatuses;
  private final boolean bookingCancellationStatusRequired;
  private final CombinationRule combinationRule;

  private CarrierStatusScenario(
    Set<BookingState> bookingStatuses,
    Set<BookingState> amendedBookingStatuses,
    boolean amendedBookingStatusRequired,
    Set<BookingCancellationState> bookingCancellationStatuses,
    boolean bookingCancellationStatusRequired,
    CombinationRule combinationRule) {
    this.bookingStatuses = immutableEnumSet(bookingStatuses, BookingState.class);
    this.amendedBookingStatuses = immutableEnumSet(amendedBookingStatuses, BookingState.class);
    this.amendedBookingStatusRequired = amendedBookingStatusRequired;
    this.bookingCancellationStatuses =
      immutableEnumSet(bookingCancellationStatuses, BookingCancellationState.class);
    this.bookingCancellationStatusRequired = bookingCancellationStatusRequired;
    this.combinationRule = combinationRule;
  }

  public static CarrierStatusScenario from(
    BookingState bookingStatus,
    BookingState amendedBookingStatus,
    BookingCancellationState bookingCancellationStatus) {
    Objects.requireNonNull(bookingStatus, "bookingStatus");

    Set<BookingState> allowedBookingStatuses = Set.of(bookingStatus);
    Set<BookingState> allowedAmendedBookingStatuses;
    boolean amendmentRequired = false;

    if (bookingCancellationStatus == BookingCancellationState.CANCELLATION_RECEIVED
      || bookingCancellationStatus == BookingCancellationState.CANCELLATION_DECLINED) {
      allowedBookingStatuses = CONFIRMED_OR_PENDING_AMENDMENT;
      allowedAmendedBookingStatuses = ANY_AMENDMENT_STATUS;
    } else if (bookingCancellationStatus == BookingCancellationState.CANCELLATION_CONFIRMED) {
      allowedAmendedBookingStatuses = Set.of(BookingState.AMENDMENT_CANCELLED);
    } else if (bookingStatus == BookingState.DECLINED) {
      allowedAmendedBookingStatuses = Set.of(BookingState.AMENDMENT_DECLINED);
    } else if (amendedBookingStatus != null) {
      allowedAmendedBookingStatuses = Set.of(amendedBookingStatus);
      amendmentRequired = true;
      if (amendedBookingStatus == BookingState.AMENDMENT_RECEIVED
        || amendedBookingStatus == BookingState.AMENDMENT_DECLINED
        || amendedBookingStatus == BookingState.AMENDMENT_CANCELLED) {
        allowedBookingStatuses = CONFIRMED_OR_PENDING_AMENDMENT;
      }
    } else {
      allowedAmendedBookingStatuses = Set.of();
    }

    Set<BookingCancellationState> allowedCancellationStatuses =
      bookingCancellationStatus == null ? Set.of() : Set.of(bookingCancellationStatus);
    return new CarrierStatusScenario(
      allowedBookingStatuses,
      allowedAmendedBookingStatuses,
      amendmentRequired,
      allowedCancellationStatuses,
      bookingCancellationStatus != null,
      CombinationRule.NONE);
  }

  public static CarrierStatusScenario uc8() {
    return new CarrierStatusScenario(
      CONFIRMED_OR_PENDING_AMENDMENT,
      Set.of(BookingState.AMENDMENT_CONFIRMED, BookingState.AMENDMENT_DECLINED),
      true,
      Set.of(),
      false,
      CombinationRule.UC8);
  }

  public static CarrierStatusScenario uc14() {
    return new CarrierStatusScenario(
      Set.of(BookingState.CANCELLED, BookingState.CONFIRMED, BookingState.PENDING_AMENDMENT),
      ANY_AMENDMENT_STATUS,
      false,
      Set.of(
        BookingCancellationState.CANCELLATION_CONFIRMED,
        BookingCancellationState.CANCELLATION_DECLINED),
      true,
      CombinationRule.UC14);
  }

  ConformanceCheckResult validateBookingStatus(JsonNode payload) {
    return validateRequiredStatus(payload, "bookingStatus", bookingStatuses);
  }

  ConformanceCheckResult validateAmendedBookingStatus(JsonNode payload) {
    return validateConditionalStatus(
      payload,
      "amendedBookingStatus",
      amendedBookingStatuses,
      amendedBookingStatusRequired);
  }

  ConformanceCheckResult validateBookingCancellationStatus(JsonNode payload) {
    return validateConditionalStatus(
      payload,
      "bookingCancellationStatus",
      bookingCancellationStatuses,
      bookingCancellationStatusRequired);
  }

  ConformanceCheckResult validateStatusCombination(JsonNode payload) {
    if (combinationRule == CombinationRule.NONE) {
      return ConformanceCheckResult.simple(Set.of());
    }

    String bookingStatus = textValue(payload, "bookingStatus");
    String amendedBookingStatus = textValue(payload, "amendedBookingStatus");
    String cancellationStatus = textValue(payload, "bookingCancellationStatus");
    boolean valid =
      switch (combinationRule) {
        case UC8 -> BookingState.AMENDMENT_CONFIRMED.name().equals(amendedBookingStatus)
          && BookingState.CONFIRMED.name().equals(bookingStatus)
          || BookingState.AMENDMENT_DECLINED.name().equals(amendedBookingStatus)
          && containsName(CONFIRMED_OR_PENDING_AMENDMENT, bookingStatus);
        case UC14 -> BookingCancellationState.CANCELLATION_CONFIRMED.name().equals(cancellationStatus)
          && BookingState.CANCELLED.name().equals(bookingStatus)
          && (amendedBookingStatus == null
          || BookingState.AMENDMENT_CANCELLED.name().equals(amendedBookingStatus))
          || BookingCancellationState.CANCELLATION_DECLINED.name().equals(cancellationStatus)
          && containsName(CONFIRMED_OR_PENDING_AMENDMENT, bookingStatus)
          && (amendedBookingStatus == null
          || containsName(ANY_AMENDMENT_STATUS, amendedBookingStatus));
        case NONE -> true;
      };
    if (valid) {
      return ConformanceCheckResult.simple(Set.of());
    }
    return ConformanceCheckResult.simple(
      Set.of(
        "The bookingStatus, amendedBookingStatus and bookingCancellationStatus combination does not match %s"
          .formatted(statusCombinationExpectation())));
  }

  String bookingStatusExpectation() {
    return "equal " + joinedNames(bookingStatuses, " or ");
  }

  String amendedBookingStatusExpectation() {
    return conditionalExpectation(amendedBookingStatuses, amendedBookingStatusRequired);
  }

  String bookingCancellationStatusExpectation() {
    return conditionalExpectation(
      bookingCancellationStatuses, bookingCancellationStatusRequired);
  }

  String statusCombinationExpectation() {
    return switch (combinationRule) {
      case UC8 ->
        "UC8: AMENDMENT_CONFIRMED requires CONFIRMED; AMENDMENT_DECLINED permits CONFIRMED or PENDING_AMENDMENT";
      case UC14 ->
        "UC14: CANCELLATION_CONFIRMED requires CANCELLED and an absent or AMENDMENT_CANCELLED amendment status; CANCELLATION_DECLINED requires CONFIRMED or PENDING_AMENDMENT";
      case NONE -> "the active scenario";
    };
  }

  boolean requiresCrossFieldValidation() {
    return combinationRule != CombinationRule.NONE;
  }

  private static ConformanceCheckResult validateRequiredStatus(
    JsonNode payload, String property, Set<? extends Enum<?>> allowedStatuses) {
    JsonNode actualNode = payload.path(property);
    String actual = actualNode.isMissingNode() ? null : actualNode.asText(null);
    if (actual != null && containsName(allowedStatuses, actual)) {
      return ConformanceCheckResult.simple(Set.of());
    }
    return ConformanceCheckResult.simple(
      Set.of(
        "Expected %s %s but found '%s'"
          .formatted(
            property,
            expectationOf(allowedStatuses),
            Objects.requireNonNullElse(actual, PayloadContentConformanceCheck.UNSET_MARKER))));
  }

  private static ConformanceCheckResult validateConditionalStatus(
    JsonNode payload,
    String property,
    Set<? extends Enum<?>> allowedStatuses,
    boolean required) {
    JsonNode actualNode = payload.path(property);
    if (actualNode.isMissingNode()) {
      if (!required) {
        return ConformanceCheckResult.simple(Set.of());
      }
      return ConformanceCheckResult.simple(
        Set.of(
          "Expected %s %s but found '%s'"
            .formatted(
              property,
              expectationOf(allowedStatuses),
              PayloadContentConformanceCheck.UNSET_MARKER)));
    }
    if (allowedStatuses.isEmpty()) {
      return ConformanceCheckResult.simple(
        Set.of(
          "The '%s' should not be present, but response contains value '%s'"
            .formatted(property, actualNode.asText())));
    }
    String actual = actualNode.asText(null);
    if (actual != null && containsName(allowedStatuses, actual)) {
      return ConformanceCheckResult.simple(Set.of());
    }
    return ConformanceCheckResult.simple(
      Set.of(
        "Expected %s %s but found '%s'"
          .formatted(
            property,
            expectationOf(allowedStatuses),
            Objects.requireNonNullElse(actual, PayloadContentConformanceCheck.UNSET_MARKER))));
  }

  private static String conditionalExpectation(
    Set<? extends Enum<?>> allowedStatuses, boolean required) {
    if (allowedStatuses.isEmpty()) {
      return "be absent";
    }
    return (required ? "equal " : "be absent or equal ") + joinedNames(allowedStatuses, ", or ");
  }

  private static String expectationOf(Set<? extends Enum<?>> statuses) {
    return statuses.size() == 1
      ? "'%s'".formatted(statuses.iterator().next().name())
      : "one of [%s]".formatted(joinedNames(statuses, ", "));
  }

  private static boolean containsName(Set<? extends Enum<?>> statuses, String actual) {
    return statuses.stream().anyMatch(status -> status.name().equals(actual));
  }

  private static String textValue(JsonNode payload, String property) {
    JsonNode value = payload.path(property);
    return value.isMissingNode() ? null : value.asText(null);
  }

  private static String joinedNames(Set<? extends Enum<?>> statuses, String delimiter) {
    return statuses.stream().map(Enum::name).collect(Collectors.joining(delimiter));
  }

  private static <E extends Enum<E>> Set<E> immutableEnumSet(
    Set<E> values, Class<E> enumClass) {
    if (values.isEmpty()) {
      return Set.of();
    }
    EnumSet<E> copy = EnumSet.noneOf(enumClass);
    copy.addAll(values);
    return Collections.unmodifiableSet(copy);
  }
}

