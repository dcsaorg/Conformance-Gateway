package org.dcsa.conformance.standards.booking.party;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BookingStateTest {

  @Test
  void testFromString_validInput() {
    assertEquals(BookingState.START, BookingState.fromString("START"));
    assertEquals(BookingState.RECEIVED, BookingState.fromString("RECEIVED"));
  }

  @Test
  void testFromString_invalidInput() {
    assertThrowsExactly(
        IllegalArgumentException.class, () -> BookingState.fromString("INVALID_STATE"));
    assertThrowsExactly(IllegalArgumentException.class, () -> BookingState.fromString(null));
    assertThrowsExactly(IllegalArgumentException.class, () -> BookingState.fromString(" "));
    assertThrowsExactly(IllegalArgumentException.class, () -> BookingState.fromString(""));
  }

  @Test
  void testFromString_caseInsensitiveInput() {
    assertThrowsExactly(IllegalArgumentException.class, () -> BookingState.fromString("start"));
    assertThrowsExactly(IllegalArgumentException.class, () -> BookingState.fromString("received"));
  }
}
