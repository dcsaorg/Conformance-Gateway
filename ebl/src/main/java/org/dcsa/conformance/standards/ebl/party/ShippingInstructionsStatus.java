package org.dcsa.conformance.standards.ebl.party;

public enum ShippingInstructionsStatus {
  SI_ANY,
  SI_START,
  SI_RECEIVED,
  SI_PENDING_UPDATE,
  SI_UPDATE_RECEIVED,
  SI_CANCELLED,
  SI_DECLINED,
  SI_COMPLETED,
  ;

  public String wireName() {
    if (!hasWireName()) {
      throw new IllegalArgumentException("State does not have a name visible in any transmission");
    }
    return this.name().replace("_", " ");
  }

  private boolean hasWireName() {
    return this != SI_START;
  }

  public static ShippingInstructionsStatus fromWireName(String wireName) {
    var v = ShippingInstructionsStatus.valueOf(wireName.replace(" ", "_"));
    if (!v.hasWireName()) {
      throw new IllegalArgumentException("The State does not have a wire name");
    }
    return v;
  }
}
