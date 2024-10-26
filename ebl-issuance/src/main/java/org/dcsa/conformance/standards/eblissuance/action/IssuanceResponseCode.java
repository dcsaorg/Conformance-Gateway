package org.dcsa.conformance.standards.eblissuance.action;

import java.util.Objects;
import java.util.stream.Stream;

public enum IssuanceResponseCode {
  ACCEPTED("ISSU", "DCSA"),
  BLOCKED("BREQ", "DCSB"),
  REFUSED("REFU", "DCSR");
  public final String standardCode;
  public final String sendToPlatform;

  IssuanceResponseCode(String standardCode, String sendToPlatform) {
    this.standardCode = standardCode;
    this.sendToPlatform = sendToPlatform;
  }

  public static IssuanceResponseCode forStandardCode(String standardCode) {
    return Stream.of(values())
        .filter(
            issuanceResponseCode -> Objects.equals(issuanceResponseCode.standardCode, standardCode))
        .findFirst()
        .orElseThrow();
  }
}
