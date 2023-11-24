package org.dcsa.conformance.standards.booking.party;

public enum BookingState {
    START,
    RECEIVED,
    PENDING_UPDATE,
    PENDING_UPDATE_CONFIRMATION,
    REJECTED,
    CANCELLED,
    CONFIRMED,
    PENDING_AMENDMENT,
    AMENDMENT_RECEIVED,
    DECLINED,
    COMPLETED,
  ;

  public String wireName() {
    if (!hasWireName()) {
      throw new IllegalArgumentException("State does not have a name visible in any transmission");
    }
    return this.name().replace("_", " ");
  }

  private boolean hasWireName() {
    return this != START;
  }

  public static BookingState fromWireName(String wireName) {
    var v = BookingState.valueOf(wireName.replace(" ", "_"));
    if (!v.hasWireName()) {
      throw new IllegalArgumentException("The State does not have a wire name");
    }
    return v;
  }
}
