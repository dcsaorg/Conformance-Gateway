package org.dcsa.conformance.standards.booking.party;

public enum BookingCancellationState {
  CANCELLATION_RECEIVED,
  CANCELLATION_CONFIRMED,
  CANCELLATION_DECLINED;

  public static BookingCancellationState fromString(String bookingCancellationStateName) {
    try {
      return valueOf(bookingCancellationStateName);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException("Unknown booking cancellation state: " + bookingCancellationStateName);
    }
  }
}
