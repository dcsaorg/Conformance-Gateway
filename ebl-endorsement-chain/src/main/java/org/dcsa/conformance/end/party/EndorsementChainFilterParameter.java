package org.dcsa.conformance.end.party;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum EndorsementChainFilterParameter {
  TRANSPORT_DOCUMENT_REFERENCE("transportDocumentReference"),
  TRANSPORT_DOCUMENT_SUB_REFERENCE("transportDocumentSubReference"),
  CARRIER_SCAC_CODE("carrierSCACCode");

  public static final Map<String, EndorsementChainFilterParameter> byQueryParamName =
    Arrays.stream(values())
      .collect(
        Collectors.toUnmodifiableMap(
          EndorsementChainFilterParameter::getQueryParamName, Function.identity()));
  private final String queryParamName;

  EndorsementChainFilterParameter(String queryParamName) {
    this.queryParamName = queryParamName;
  }
}
