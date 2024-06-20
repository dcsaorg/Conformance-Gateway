package org.dcsa.conformance.standards.ebl.crypto.impl;

import org.dcsa.conformance.standards.ebl.crypto.JWSSignerDetails;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerWithKey;
import org.dcsa.conformance.standards.ebl.crypto.SignatureVerifierWithKey;

import java.security.interfaces.RSAPublicKey;

import static org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory.pemEncodeKey;

public class RSAPayloadSigner extends DefaultPayloadSigner implements PayloadSignerWithKey {

  private final RSAPublicKey rsaPublicKey;

  public RSAPayloadSigner(JWSSignerDetails jwsSignerDetails, RSAPublicKey rsaPublicKey) {
    super(jwsSignerDetails);
    this.rsaPublicKey = rsaPublicKey;
  }

  @Override
  public String getPublicKeyInPemFormat() {
    return pemEncodeKey(this.rsaPublicKey);
  }
}
