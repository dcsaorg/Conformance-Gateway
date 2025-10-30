package org.dcsa.conformance.standards.ebl.crypto;

public interface SignatureVerifierWithKey extends SignatureVerifier {

  String getPublicKeyInPemFormat();
}
