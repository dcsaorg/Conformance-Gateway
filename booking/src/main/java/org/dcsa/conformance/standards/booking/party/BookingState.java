package org.dcsa.conformance.standards.booking.party;

public enum BookingState {
    START,
    RECEIVED,
    PENDING_UPDATE,
    UPDATE_RECEIVED,
    REJECTED,
    CANCELLED,
    CONFIRMED,
    PENDING_AMENDMENT,
    DECLINED,
    AMENDMENT_RECEIVED,
    AMENDMENT_CONFIRMED,
    AMENDMENT_DECLINED,
    AMENDMENT_CANCELLED,
    COMPLETED;

  public static BookingState fromString(String bookingStateName) {
    try {
      return valueOf(bookingStateName); // Case-sensitive
    } catch (IllegalArgumentException | NullPointerException e) {
      return null;
    }
  }

}
