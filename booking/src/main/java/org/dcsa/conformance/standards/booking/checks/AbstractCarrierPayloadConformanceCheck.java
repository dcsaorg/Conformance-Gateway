package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceError;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

abstract class AbstractCarrierPayloadConformanceCheck extends PayloadContentConformanceCheck {

  protected static final Set<BookingState> BOOKING_STATES_WHERE_CBR_IS_OPTIONAL =
    Set.of(
      BookingState.RECEIVED,
      BookingState.REJECTED,
      BookingState.PENDING_UPDATE,
      BookingState.UPDATE_RECEIVED,
      BookingState.CANCELLED);

  protected final BookingState expectedBookingStatus;
  protected final CarrierStatusScenario carrierStatusScenario;

  protected static final String FEEDBACKS = "feedbacks";

  protected AbstractCarrierPayloadConformanceCheck(
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    BookingState bookingState,
    BookingState expectedAmendedBookingStatus,
    BookingCancellationState expectedBookingCancellationStatus) {
    this(
      matchedExchangeUuid,
      httpMessageType,
      bookingState,
      CarrierStatusScenario.from(
        bookingState, expectedAmendedBookingStatus, expectedBookingCancellationStatus));
  }

  protected AbstractCarrierPayloadConformanceCheck(
    UUID matchedExchangeUuid,
    HttpMessageType httpMessageType,
    BookingState bookingState,
    CarrierStatusScenario carrierStatusScenario) {
    super(
      "Validate the carrier payload",
      BookingRole::isCarrier,
      matchedExchangeUuid,
      httpMessageType);
    this.expectedBookingStatus = bookingState;
    this.carrierStatusScenario = Objects.requireNonNull(carrierStatusScenario);
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

  protected ConformanceCheckResult ensureAtLeastOneCarrierReferenceIsPresent(JsonNode payload) {
    if (!JsonUtil.isMissingOrEmpty(payload.path("carrierBookingReference"))
      || !JsonUtil.isMissingOrEmpty(payload.path("carrierBookingRequestReference"))) {
      return ConformanceCheckResult.simple(Collections.emptySet());
    }
    return ConformanceCheckResult.simple(
      Set.of(
        "At least one of 'carrierBookingReference' or 'carrierBookingRequestReference' must be present"));
  }

  protected ConformanceCheckResult ensureBookingStatusIsCorrect(JsonNode responsePayload) {
    return carrierStatusScenario.validateBookingStatus(responsePayload);
  }

  protected ConformanceCheckResult ensureAmendedBookingStatusIsCorrect(JsonNode responsePayload) {
    return carrierStatusScenario.validateAmendedBookingStatus(responsePayload);
  }

  protected ConformanceCheckResult ensureAmendedBookingStatusUsageIsCorrect(JsonNode payload) {
    return carrierStatusScenario.validateAmendedBookingStatus(payload);
  }

  protected ConformanceCheckResult ensureBookingCancellationStatusIsCorrect(
    JsonNode responsePayload) {
    return carrierStatusScenario.validateBookingCancellationStatus(responsePayload);
  }

  protected ConformanceCheckResult ensureBookingCancellationStatusUsageIsCorrect(JsonNode payload) {
    return carrierStatusScenario.validateBookingCancellationStatus(payload);
  }

  protected ConformanceCheckResult ensureBookingStatusCodeCompliance(JsonNode payload) {
    return ensureStatusCodeCompliance(payload, "bookingStatus", BookingDataSets.BOOKING_STATUS);
  }

  protected ConformanceCheckResult ensureAmendedBookingStatusCodeCompliance(JsonNode payload) {
    if (!carrierStatusScenario.shouldValidateSecondaryStatuses()) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    return ensureStatusCodeCompliance(
      payload, "amendedBookingStatus", BookingDataSets.AMENDED_BOOKING_STATUS);
  }

  protected ConformanceCheckResult ensureBookingCancellationStatusCodeCompliance(JsonNode payload) {
    if (!carrierStatusScenario.shouldValidateSecondaryStatuses()) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    return ensureStatusCodeCompliance(
      payload, "bookingCancellationStatus", BookingDataSets.BOOKING_CANCELLATION_STATUS);
  }

  private static ConformanceCheckResult ensureStatusCodeCompliance(
    JsonNode payload, String field, org.dcsa.conformance.core.check.KeywordDataset dataset) {
    JsonNode value = payload.path(field);
    if (value.isMissingNode()) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    if (!value.isTextual() || !dataset.contains(value.asText())) {
      return ConformanceCheckResult.simple(
        Set.of("Invalid '%s' value: '%s'".formatted(field, value.asText())));
    }
    return ConformanceCheckResult.simple(Collections.emptySet());
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

  protected ConformanceCheckResult ensureFeedbackSeverityCompliance(JsonNode responsePayload) {
    JsonNode feedbacks = responsePayload.path(FEEDBACKS);
    if (JsonUtil.isMissingOrEmpty(feedbacks)) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    Set<String> errors = new HashSet<>();
    for (JsonNode feedback : feedbacks) {
      String severity = feedback.path("severity").asText(null);
      if (!BookingDataSets.FEEDBACKS_SEVERITY.contains(severity)) {
        errors.add("Invalid feedback severity: " + severity);
      }
    }
    return ConformanceCheckResult.simple(errors);
  }

  protected ConformanceCheckResult ensureFeedbackCodeCompliance(JsonNode responsePayload) {
    JsonNode feedbacks = responsePayload.path(FEEDBACKS);
    if (JsonUtil.isMissingOrEmpty(feedbacks)) {
      return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
    }
    Set<String> errors = new HashSet<>();
    for (JsonNode feedback : feedbacks) {
      String code = feedback.path("code").asText(null);
      if (!BookingDataSets.FEEDBACKS_CODE.contains(code)) {
        errors.add("Invalid feedback code: " + code);
      }
    }
    return ConformanceCheckResult.simple(errors);
  }
}
