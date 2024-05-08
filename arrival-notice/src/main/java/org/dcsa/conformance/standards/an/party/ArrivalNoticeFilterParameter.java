package org.dcsa.conformance.standards.an.party;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ArrivalNoticeFilterParameter {
  TRANSPORT_DOCUMENT_REFERENCE("transportDocumentReference");

  public static final Map<String, ArrivalNoticeFilterParameter> byQueryParamName =
      Arrays.stream(values())
          .collect(
              Collectors.toUnmodifiableMap(
                ArrivalNoticeFilterParameter::getQueryParamName, Function.identity()));

  @Getter private final String queryParamName;

  ArrivalNoticeFilterParameter(String queryParamName) {
    this.queryParamName = queryParamName;
  }
}
