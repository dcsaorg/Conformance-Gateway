package org.dcsa.conformance.standards.booking.party;

public enum BookingCancellationState {
  CANCELLATION_RECEIVED,
  CANCELLATION_CONFIRMED,
  CANCELLATION_DECLINED
  ;

  public static BookingCancellationState fromWireName(String wireName) {
    return BookingCancellationState.valueOf(wireName.replace(" ", "_"));
  }

  public String wireName() {
    return this.name().replace("_", " ");
  }
}
