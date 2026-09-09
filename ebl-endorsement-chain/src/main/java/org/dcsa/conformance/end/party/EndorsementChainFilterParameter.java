package org.dcsa.conformance.end.party;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum EndorsementChainFilterParameter {
  TRANSPORT_DOCUMENT_REFERENCE("transportDocumentReference"),
  TRANSPORT_DOCUMENT_SUB_REFERENCE("transportDocumentSubReference"),
  CARRIER_SCAC_CODE("carrierSCACCode");

  public static final Map<String, EndorsementChainFilterParameter> byParamName =
    Arrays.stream(values())
      .collect(
        Collectors.toUnmodifiableMap(
          EndorsementChainFilterParameter::getParamName, Function.identity()));
  private final String paramName;

  EndorsementChainFilterParameter(String paramName) {
    this.paramName = paramName;
  }
}
