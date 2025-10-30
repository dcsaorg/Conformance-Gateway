package org.dcsa.conformance.standards.ebl.crypto;

public interface PayloadSignerWithKey extends PayloadSigner {
  String getPublicKeyInPemFormat();
}
