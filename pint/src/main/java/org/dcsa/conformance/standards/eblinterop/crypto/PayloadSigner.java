package org.dcsa.conformance.standards.eblinterop.crypto;

public interface PayloadSigner {
  String sign(String payload);
}
