package org.dcsa.conformance.standards.adoption.party;

import lombok.Getter;

@Getter
public enum FilterParameter {
  INTERVAL("interval"),
  DATE("date");

  private final String queryParamName;

  FilterParameter(String queryParamName) {
    this.queryParamName = queryParamName;
  }

}
