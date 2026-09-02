package org.dcsa.conformance.standards.booking.model;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.templateFileToJsonNode;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PersistableCarrierBookingTest {

  @Test
  void bookingRequestLifecycleMatchesSpreadsheetStates() {
    var updatedBooking = newBooking();
    String updatedCbrr = updatedBooking.getCarrierBookingRequestReference();
    assertStatuses(updatedBooking, RECEIVED, null, null);
    updatedBooking.requestUpdateToBooking(updatedCbrr, ignored -> {});
    assertStatuses(updatedBooking, PENDING_UPDATE, null, null);
    updatedBooking.putBooking(updatedCbrr, updatedBooking.getBooking().deepCopy());
    assertStatuses(updatedBooking, UPDATE_RECEIVED, null, null);

    var rejectedBooking = newBooking();
    rejectedBooking.rejectBooking(rejectedBooking.getCarrierBookingRequestReference());
    assertStatuses(rejectedBooking, REJECTED, null, null);

    var cancelledBooking = newBooking();
    cancelledBooking.cancelBookingRequest(cancelledBooking.getCarrierBookingRequestReference());
    assertStatuses(cancelledBooking, CANCELLED, null, null);

    var completedBooking = confirmedBooking();
    completedBooking.confirmBookingCompleted(
        completedBooking.getCarrierBookingRequestReference(), true, true);
    assertStatuses(completedBooking, COMPLETED, null, null);
  }

  @Test
  void receivingAmendmentRestoresConfirmedPrimaryBookingState() {
    var booking = confirmedBooking();
    String cbrr = booking.getCarrierBookingRequestReference();
    booking.updateConfirmedBooking(cbrr, ignored -> {}, true);
    assertEquals(PENDING_AMENDMENT, booking.getOriginalBookingState());

    submitAmendment(booking);

    assertStatuses(booking, CONFIRMED, AMENDMENT_RECEIVED, null);
    assertEquals("CONFIRMED", booking.getBooking().path("bookingStatus").asText());
    assertEquals(
        "CONFIRMED",
        booking.getAmendedBooking().orElseThrow().path("bookingStatus").asText());
    assertEquals(
        "AMENDMENT_RECEIVED",
        booking.getAmendedBooking().orElseThrow().path("amendedBookingStatus").asText());
  }

  @Test
  void amendmentOutcomesMatchSpreadsheetStates() {
    var confirmedAmendment = bookingWithReceivedAmendment();
    confirmedAmendment.confirmBookingAmendment(confirmedAmendment.getCarrierBookingReference());
    assertStatuses(confirmedAmendment, CONFIRMED, AMENDMENT_CONFIRMED, null);

    var declinedAmendment = bookingWithReceivedAmendment();
    declinedAmendment.declineBookingAmendment(declinedAmendment.getCarrierBookingReference());
    assertStatuses(declinedAmendment, CONFIRMED, AMENDMENT_DECLINED, null);

    var cancelledAmendment = bookingWithReceivedAmendment();
    cancelledAmendment.cancelBookingAmendment(cancelledAmendment.getCarrierBookingReference());
    assertStatuses(cancelledAmendment, CONFIRMED, AMENDMENT_CANCELLED, null);

    var declinedBooking = bookingWithReceivedAmendment();
    declinedBooking.declineBooking(declinedBooking.getCarrierBookingReference());
    assertStatuses(declinedBooking, DECLINED, AMENDMENT_DECLINED, null);
  }

  @Test
  void confirmedBookingCancellationOutcomesMatchSpreadsheetStates() {
    var confirmedCancellation = bookingWithReceivedAmendment();
    confirmedCancellation.updateCancelConfirmedBooking(
        confirmedCancellation.getCarrierBookingReference());
    assertStatuses(
        confirmedCancellation, CONFIRMED, AMENDMENT_RECEIVED, CANCELLATION_RECEIVED);
    confirmedCancellation.cancelConfirmedBooking(
        confirmedCancellation.getCarrierBookingReference());
    assertStatuses(
        confirmedCancellation, CANCELLED, AMENDMENT_CANCELLED, CANCELLATION_CONFIRMED);

    var declinedCancellation = bookingWithReceivedAmendment();
    declinedCancellation.updateCancelConfirmedBooking(declinedCancellation.getCarrierBookingReference());
    declinedCancellation.declineConfirmedBookingCancellation(
        declinedCancellation.getCarrierBookingReference());
    assertStatuses(
        declinedCancellation, CONFIRMED, AMENDMENT_RECEIVED, CANCELLATION_DECLINED);
  }

  @Test
  void bookingRequestCancellationRejectsConfirmedBooking() {
    var booking = confirmedBooking();
    assertThrows(
        IllegalStateException.class,
        () -> booking.cancelBookingRequest(booking.getCarrierBookingReference()));
  }

  private static PersistableCarrierBooking newBooking() {
    return PersistableCarrierBooking.initializeFromBookingRequest(
        (ObjectNode)
            templateFileToJsonNode(
                "/standards/booking/messages/booking-api-2.0.0-dry-cargo.json", Map.of()));
  }

  private static PersistableCarrierBooking confirmedBooking() {
    var booking = newBooking();
    booking.confirmBooking(booking.getCarrierBookingRequestReference(), () -> "CBR-1");
    return booking;
  }

  private static PersistableCarrierBooking bookingWithReceivedAmendment() {
    var booking = confirmedBooking();
    submitAmendment(booking);
    return booking;
  }

  private static void submitAmendment(PersistableCarrierBooking booking) {
    ObjectNode amendment = booking.getBooking().deepCopy();
    amendment.withObject("/vessel").put("name", "King of the Seas");
    booking.putBooking(booking.getCarrierBookingReference(), amendment);
  }

  private static void assertStatuses(
      PersistableCarrierBooking booking,
      org.dcsa.conformance.standards.booking.party.BookingState bookingStatus,
      org.dcsa.conformance.standards.booking.party.BookingState amendedBookingStatus,
      org.dcsa.conformance.standards.booking.party.BookingCancellationState cancellationStatus) {
    assertEquals(bookingStatus, booking.getOriginalBookingState());
    if (amendedBookingStatus == null) {
      assertNull(booking.getBookingAmendedState());
    } else {
      assertEquals(amendedBookingStatus, booking.getBookingAmendedState());
    }
    assertEquals(cancellationStatus, booking.getBookingCancellationState());
  }
}

