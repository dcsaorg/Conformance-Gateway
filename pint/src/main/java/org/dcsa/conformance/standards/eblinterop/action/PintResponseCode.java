package org.dcsa.conformance.standards.eblinterop.action;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PintResponseCode {
  RECE(200),
  DUPE(200),
  BENV(422),
  BSIG(422),
  BETR(422),
  INCD(409),
  MDOC(409),
  DISE(409),
  ;

  private final int httpResponseCode;
}
