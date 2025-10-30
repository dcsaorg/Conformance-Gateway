package org.dcsa.conformance.standards.booking.party;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BookingCancellationStateTest {

  @Test
  void testFromString_validInput() {
    assertEquals(
        BookingCancellationState.CANCELLATION_RECEIVED,
        BookingCancellationState.fromString("CANCELLATION_RECEIVED"));
    assertEquals(
        BookingCancellationState.CANCELLATION_CONFIRMED,
        BookingCancellationState.fromString("CANCELLATION_CONFIRMED"));
    assertEquals(
        BookingCancellationState.CANCELLATION_DECLINED,
        BookingCancellationState.fromString("CANCELLATION_DECLINED"));
  }

  @Test
  void testFromString_invalidInput() {
    assertThrows(
        IllegalArgumentException.class, () -> BookingCancellationState.fromString("INVALID_STATE"));
    assertThrows(IllegalArgumentException.class, () -> BookingCancellationState.fromString(null));
    assertThrows(IllegalArgumentException.class, () -> BookingCancellationState.fromString(" "));
    assertThrows(IllegalArgumentException.class, () -> BookingCancellationState.fromString(""));
  }

  @Test
  void testFromString_caseInsensitiveInput() {
    assertThrows(
        IllegalArgumentException.class,
        () -> BookingCancellationState.fromString("cancellation_confirmed"));
    assertThrows(
        IllegalArgumentException.class,
        () -> BookingCancellationState.fromString("Cancellation_received"));
  }
}
