package org.dcsa.conformance.standards.ebl.crypto;

public interface PayloadSigner {
  String sign(String payload);
}
