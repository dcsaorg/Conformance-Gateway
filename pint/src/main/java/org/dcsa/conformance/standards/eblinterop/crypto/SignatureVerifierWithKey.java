package org.dcsa.conformance.standards.eblinterop.crypto;


public interface SignatureVerifierWithKey extends SignatureVerifier {

  String getPublicKeyInPemFormat();

}
