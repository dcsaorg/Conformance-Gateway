package org.dcsa.conformance.standards.tnt.party;

public enum SortDirection {
  ASCENDING("ASC"),
  DESCENDING("DESC");
  private final String value;
    SortDirection(String value) {
    this.value = value;
  }
  public String getValue() {
    return value;
  }
}
