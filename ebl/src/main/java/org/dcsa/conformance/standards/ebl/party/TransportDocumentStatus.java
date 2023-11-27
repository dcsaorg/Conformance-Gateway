package org.dcsa.conformance.standards.ebl.party;

public enum TransportDocumentStatus {
  TD_ANY,
  TD_START,
  TD_DRAFT,
  TD_APPROVED,
  TD_ISSUED,
  TD_PENDING_SURRENDER_FOR_AMENDMENT,
  TD_SURRENDERED_FOR_AMENDMENT,
  TD_PENDING_SURRENDER_FOR_DELIVERY,
  TD_SURRENDERED_FOR_DELIVERY,
  TD_VOIDED,
  ;

  public String wireName() {
    if (!hasWireName()) {
      throw new IllegalArgumentException("State does not have a name visible in any transmission");
    }
    return this.name().replace("_", " ");
  }

  private boolean hasWireName() {
    return this != TD_START;
  }

  public static TransportDocumentStatus fromWireName(String wireName) {
    var v = TransportDocumentStatus.valueOf(wireName.replace(" ", "_"));
    if (!v.hasWireName()) {
      throw new IllegalArgumentException("The State does not have a wire name");
    }
    return v;
  }
}
