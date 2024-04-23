package org.dcsa.conformance.standards.booking.model;

public enum InvalidBookingMessageType {
  CANCEL_BOOKING_AMENDMENT,
  SUBMIT_BOOKING_AMENDMENT,
  UPDATE_BOOKING,
  CANCEL_BOOKING;

  public String getUC() {
    return switch (this) {
      case CANCEL_BOOKING_AMENDMENT -> "UC9";
      case SUBMIT_BOOKING_AMENDMENT -> "UC7";
      case CANCEL_BOOKING -> "UC11";
      case UPDATE_BOOKING -> "UC3";
    };
  }

  public String getExpectedRequestMethod() {
    return switch (this) {
      case CANCEL_BOOKING_AMENDMENT,CANCEL_BOOKING -> "PATCH";
      case SUBMIT_BOOKING_AMENDMENT,UPDATE_BOOKING -> "PUT";
    };
  }

  public String getExpectedRequestUrlFormat() {
    return "/v2/bookings/%s";
  }
}
