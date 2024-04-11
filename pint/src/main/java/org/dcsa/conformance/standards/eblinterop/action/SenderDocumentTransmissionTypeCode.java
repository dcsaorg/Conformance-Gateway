package org.dcsa.conformance.standards.eblinterop.action;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SenderDocumentTransmissionTypeCode {
  VALID_DOCUMENT(204),
  CORRUPTED_DOCUMENT(409),
  UNRELATED_DOCUMENT(409),
  ;
  private final int httpResponseCode;
}
