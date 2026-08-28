package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.booking.party.BookingCancellationState.CANCELLATION_CONFIRMED;
import static org.dcsa.conformance.standards.booking.party.BookingCancellationState.CANCELLATION_DECLINED;
import static org.dcsa.conformance.standards.booking.party.BookingCancellationState.CANCELLATION_RECEIVED;
import static org.dcsa.conformance.standards.booking.party.BookingState.AMENDMENT_CANCELLED;
import static org.dcsa.conformance.standards.booking.party.BookingState.AMENDMENT_CONFIRMED;
import static org.dcsa.conformance.standards.booking.party.BookingState.AMENDMENT_DECLINED;
import static org.dcsa.conformance.standards.booking.party.BookingState.AMENDMENT_RECEIVED;
import static org.dcsa.conformance.standards.booking.party.BookingState.CANCELLED;
import static org.dcsa.conformance.standards.booking.party.BookingState.COMPLETED;
import static org.dcsa.conformance.standards.booking.party.BookingState.CONFIRMED;
import static org.dcsa.conformance.standards.booking.party.BookingState.DECLINED;
import static org.dcsa.conformance.standards.booking.party.BookingState.PENDING_AMENDMENT;
import static org.dcsa.conformance.standards.booking.party.BookingState.PENDING_UPDATE;
import static org.dcsa.conformance.standards.booking.party.BookingState.RECEIVED;
import static org.dcsa.conformance.standards.booking.party.BookingState.REJECTED;
import static org.dcsa.conformance.standards.booking.party.BookingState.UPDATE_RECEIVED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarrierStatusScenarioTest {

  @Test
  void acceptsEveryCarrierStatusTableUseCase() {
    List<StatusCase> cases =
      List.of(
        statusCase(RECEIVED, null, null),
        statusCase(PENDING_UPDATE, null, null),
        statusCase(UPDATE_RECEIVED, null, null),
        statusCase(REJECTED, null, null),
        statusCase(CONFIRMED, null, null),
        statusCase(PENDING_AMENDMENT, null, null),
        statusCase(CONFIRMED, AMENDMENT_RECEIVED, null),
        statusCase(PENDING_AMENDMENT, AMENDMENT_RECEIVED, null),
        statusCase(CONFIRMED, AMENDMENT_CONFIRMED, null),
        statusCase(CONFIRMED, AMENDMENT_DECLINED, null),
        statusCase(PENDING_AMENDMENT, AMENDMENT_DECLINED, null),
        statusCase(CONFIRMED, AMENDMENT_CANCELLED, null),
        statusCase(PENDING_AMENDMENT, AMENDMENT_CANCELLED, null),
        statusCase(DECLINED, null, null),
        statusCase(DECLINED, AMENDMENT_DECLINED, null),
        statusCase(CANCELLED, null, null),
        statusCase(COMPLETED, null, null),
        statusCase(CONFIRMED, null, CANCELLATION_RECEIVED),
        statusCase(CONFIRMED, AMENDMENT_RECEIVED, CANCELLATION_RECEIVED),
        statusCase(PENDING_AMENDMENT, AMENDMENT_CONFIRMED, CANCELLATION_RECEIVED),
        statusCase(CONFIRMED, AMENDMENT_DECLINED, CANCELLATION_RECEIVED),
        statusCase(PENDING_AMENDMENT, AMENDMENT_CANCELLED, CANCELLATION_RECEIVED),
        statusCase(CANCELLED, null, CANCELLATION_CONFIRMED),
        statusCase(CANCELLED, AMENDMENT_CANCELLED, CANCELLATION_CONFIRMED),
        statusCase(CONFIRMED, null, CANCELLATION_DECLINED),
        statusCase(PENDING_AMENDMENT, AMENDMENT_RECEIVED, CANCELLATION_DECLINED),
        statusCase(CONFIRMED, AMENDMENT_CONFIRMED, CANCELLATION_DECLINED),
        statusCase(PENDING_AMENDMENT, AMENDMENT_DECLINED, CANCELLATION_DECLINED),
        statusCase(CONFIRMED, AMENDMENT_CANCELLED, CANCELLATION_DECLINED));

    cases.forEach(this::assertValid);
  }

  @Test
  void rejectsStatusesOutsideTheActiveUseCaseCombination() {
    assertInvalid(
      CarrierStatusScenario.from(RECEIVED, null, null),
      statusCase(RECEIVED, AMENDMENT_RECEIVED, null),
      "amendedBookingStatus");
    assertInvalid(
      CarrierStatusScenario.from(CONFIRMED, AMENDMENT_RECEIVED, null),
      statusCase(CONFIRMED, null, null),
      "amendedBookingStatus");
    assertInvalid(
      CarrierStatusScenario.from(CONFIRMED, null, CANCELLATION_RECEIVED),
      statusCase(RECEIVED, null, CANCELLATION_RECEIVED),
      "bookingStatus");
    assertInvalid(
      CarrierStatusScenario.from(CANCELLED, null, CANCELLATION_CONFIRMED),
      statusCase(CANCELLED, AMENDMENT_RECEIVED, CANCELLATION_CONFIRMED),
      "amendedBookingStatus");
    assertInvalid(
      CarrierStatusScenario.from(CONFIRMED, null, CANCELLATION_DECLINED),
      statusCase(CONFIRMED, null, null),
      "bookingCancellationStatus");
  }

  @Test
  void bookingStatusOnlyScenarioIgnoresSecondaryStatusValues() {
    CarrierStatusScenario scenario = CarrierStatusScenario.bookingStatusOnly(CONFIRMED);
    ObjectNode payload = OBJECT_MAPPER.createObjectNode()
      .put("bookingStatus", CONFIRMED.name())
      .put("amendedBookingStatus", "SYSTEM_LIMITATION_VALUE")
      .put("bookingCancellationStatus", "SYSTEM_LIMITATION_VALUE");

    assertTrue(scenario.validateBookingStatus(payload).isConformant());
    assertTrue(scenario.validateAmendedBookingStatus(payload).isConformant());
    assertTrue(scenario.validateBookingCancellationStatus(payload).isConformant());
    assertFalse(scenario.shouldValidateSecondaryStatuses());
  }

  @Test
  void uc8OnlyAcceptsApprovedOutcome() {
    CarrierStatusScenario scenario =
      CarrierStatusScenario.from(CONFIRMED, AMENDMENT_CONFIRMED, null);
    assertValid(scenario, statusCase(CONFIRMED, AMENDMENT_CONFIRMED, null));
  }

  @Test
  void uc8RejectsNonApprovedCombinations() {
    CarrierStatusScenario scenario =
      CarrierStatusScenario.from(CONFIRMED, AMENDMENT_CONFIRMED, null);
    assertInvalid(scenario, statusCase(CONFIRMED, AMENDMENT_DECLINED, null), "amendedBookingStatus");
    assertInvalid(scenario, statusCase(PENDING_AMENDMENT, AMENDMENT_CONFIRMED, null), "bookingStatus");
    assertInvalid(scenario, statusCase(PENDING_AMENDMENT, AMENDMENT_DECLINED, null), "bookingStatus");
  }

  @Test
  void uc14OnlyAcceptsApprovedOutcome() {
    CarrierStatusScenario scenario =
      CarrierStatusScenario.from(CANCELLED, null, CANCELLATION_CONFIRMED);
    List.of(
        statusCase(CANCELLED, null, CANCELLATION_CONFIRMED),
        statusCase(CANCELLED, AMENDMENT_CANCELLED, CANCELLATION_CONFIRMED))
      .forEach(statusCase -> assertValid(scenario, statusCase));
  }

  @Test
  void uc14RejectsNonApprovedCombinations() {
    CarrierStatusScenario scenario =
      CarrierStatusScenario.from(CANCELLED, null, CANCELLATION_CONFIRMED);
    assertInvalid(scenario, statusCase(CONFIRMED, null, CANCELLATION_DECLINED), "bookingStatus");
    assertInvalid(scenario, statusCase(PENDING_AMENDMENT, AMENDMENT_RECEIVED, CANCELLATION_DECLINED), "bookingStatus");
    assertInvalid(scenario, statusCase(CONFIRMED, AMENDMENT_CONFIRMED, CANCELLATION_DECLINED), "bookingStatus");
    assertInvalid(scenario, statusCase(PENDING_AMENDMENT, AMENDMENT_DECLINED, CANCELLATION_DECLINED), "bookingStatus");
    assertInvalid(scenario, statusCase(CONFIRMED, AMENDMENT_CANCELLED, CANCELLATION_DECLINED), "bookingStatus");
    assertInvalid(scenario, statusCase(CONFIRMED, null, CANCELLATION_CONFIRMED), "bookingStatus");
    assertInvalid(scenario, statusCase(PENDING_AMENDMENT, AMENDMENT_CANCELLED, CANCELLATION_CONFIRMED), "bookingStatus");
    assertInvalid(scenario, statusCase(CANCELLED, AMENDMENT_RECEIVED, CANCELLATION_CONFIRMED), "amendedBookingStatus");
  }

  private void assertValid(StatusCase statusCase) {
    assertValid(scenarioFor(statusCase), statusCase);
  }

  private void assertValid(CarrierStatusScenario scenario, StatusCase statusCase) {
    ObjectNode payload = payload(statusCase);
    assertTrue(
      scenario.validateBookingStatus(payload).getErrorMessages().isEmpty(),
      statusCase::toString);
    assertTrue(
      scenario.validateAmendedBookingStatus(payload).getErrorMessages().isEmpty(),
      statusCase::toString);
    assertTrue(
      scenario.validateBookingCancellationStatus(payload).getErrorMessages().isEmpty(),
      statusCase::toString);
  }

  private static void assertInvalid(
    CarrierStatusScenario scenario, StatusCase statusCase, String invalidProperty) {
    ObjectNode payload = payload(statusCase);
    var errors =
      switch (invalidProperty) {
        case "bookingStatus" -> scenario.validateBookingStatus(payload).getErrorMessages();
        case "amendedBookingStatus" -> scenario.validateAmendedBookingStatus(payload).getErrorMessages();
        case "bookingCancellationStatus" -> scenario.validateBookingCancellationStatus(payload).getErrorMessages();
        default -> throw new IllegalArgumentException(invalidProperty);
      };
    assertFalse(errors.isEmpty(), statusCase::toString);
  }

  private static CarrierStatusScenario scenarioFor(StatusCase statusCase) {
    BookingState expectedBookingStatus = statusCase.bookingStatus();
    BookingState expectedAmendedStatus = statusCase.amendedBookingStatus();
    BookingCancellationState expectedCancellationStatus = statusCase.cancellationStatus();

    if (expectedBookingStatus == PENDING_AMENDMENT && expectedAmendedStatus != null) {
      expectedBookingStatus = CONFIRMED;
    }
    if (expectedBookingStatus == DECLINED) {
      expectedAmendedStatus = null;
    }
    if (expectedCancellationStatus != null) {
      expectedBookingStatus =
        expectedCancellationStatus == CANCELLATION_CONFIRMED ? CANCELLED : CONFIRMED;
      expectedAmendedStatus = null;
    }
    return CarrierStatusScenario.from(
      expectedBookingStatus, expectedAmendedStatus, expectedCancellationStatus);
  }

  private static ObjectNode payload(StatusCase statusCase) {
    ObjectNode payload = OBJECT_MAPPER.createObjectNode();
    payload.put("bookingStatus", statusCase.bookingStatus().name());
    if (statusCase.amendedBookingStatus() != null) {
      payload.put("amendedBookingStatus", statusCase.amendedBookingStatus().name());
    }
    if (statusCase.cancellationStatus() != null) {
      payload.put("bookingCancellationStatus", statusCase.cancellationStatus().name());
    }
    return payload;
  }

  private static StatusCase statusCase(
    BookingState bookingStatus,
    BookingState amendedBookingStatus,
    BookingCancellationState cancellationStatus) {
    return new StatusCase(bookingStatus, amendedBookingStatus, cancellationStatus);
  }

  private record StatusCase(
    BookingState bookingStatus,
    BookingState amendedBookingStatus,
    BookingCancellationState cancellationStatus) {
  }
}
