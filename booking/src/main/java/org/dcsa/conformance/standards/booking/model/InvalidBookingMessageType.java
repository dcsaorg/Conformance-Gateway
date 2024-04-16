package org.dcsa.conformance.standards.booking.model;

public enum InvalidBookingMessageType {
  CANCEL_BOOKING_AMENDMENT,
  CANCEL_BOOKING,
  CREATE_BOOKING,
  COMPLETE_BOOKING,
  REJECT_BOOKING,
  DECLINE_BOOKING
  ;

  public String getUC() {
    return switch (this) {
      case CANCEL_BOOKING_AMENDMENT -> "UC9";
      case CANCEL_BOOKING -> "UC11";
      case CREATE_BOOKING -> "UC1";
      case COMPLETE_BOOKING -> "UC12";
      case REJECT_BOOKING -> "UC4";
      case DECLINE_BOOKING -> "UC10";
    };
  }

  public String getExpectedRequestMethod() {
    return switch (this) {
      case CANCEL_BOOKING_AMENDMENT -> "UC9";
      case CANCEL_BOOKING -> "UC11";
      case CREATE_BOOKING -> "UC1";
      case COMPLETE_BOOKING -> "UC12";
      case REJECT_BOOKING -> "UC4";
      case DECLINE_BOOKING -> "UC10";
    };
  }

  public String getExpectedRequestUrlFormat() {
    return "/v3/shipping-instructions/%s";
  }
}
