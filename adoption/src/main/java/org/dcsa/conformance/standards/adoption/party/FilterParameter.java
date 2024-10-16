package org.dcsa.conformance.standards.adoption.party;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum FilterParameter {
  INTERVAL("interval"),
  DATE("date");

  private final String queryParamName;

  FilterParameter(String queryParamName) {
    this.queryParamName = queryParamName;
  }

  public static final Map<String, FilterParameter> byQueryParamName =
      Arrays.stream(values())
          .collect(
              Collectors.toUnmodifiableMap(
                  FilterParameter::getQueryParamName, Function.identity()));
}
