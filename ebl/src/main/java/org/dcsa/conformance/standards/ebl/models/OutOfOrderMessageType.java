package org.dcsa.conformance.standards.ebl.models;

public enum OutOfOrderMessageType {
  SUBMIT_SI_UPDATE,
  CANCEL_SI_UPDATE,
  APPROVE_TD,
  ;

  public boolean isTDRequest() {
    return this == APPROVE_TD;
  }

  public String getUC() {
    return switch (this) {
      case SUBMIT_SI_UPDATE -> "UC3";
      case CANCEL_SI_UPDATE -> "UC5";
      case APPROVE_TD -> "UC7";
    };
  }

  public String getExpectedRequestMethod() {
    return switch (this) {
      case SUBMIT_SI_UPDATE -> "PUT";
      case APPROVE_TD, CANCEL_SI_UPDATE -> "PATCH";
    };
  }

  public String getExpectedRequestUrlFormat() {
    if (isTDRequest()) {
      return "/v3/transport-documents/%s";
    }
    return "/v3/shipping-instructions/%s";
  }
}
