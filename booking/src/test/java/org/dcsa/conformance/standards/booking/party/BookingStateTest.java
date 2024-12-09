package org.dcsa.conformance.standards.booking.party;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;

import static org.junit.jupiter.api.Assertions.*;

class BookingStateTest {

  @Test
  void testFromString_validInput() {
    assertEquals(BookingState.START, BookingState.fromString("START"));
    assertEquals(BookingState.RECEIVED, BookingState.fromString("RECEIVED"));
  }

  @ParameterizedTest
  @NullSource
  @EmptySource
  @CsvSource({
    "INVALID_STATE",
    "start",
    "received",
  })
  void testFromString_invalidInput(String input) {
    assertThrows(IllegalArgumentException.class, () -> BookingState.fromString(input));
  }

}
