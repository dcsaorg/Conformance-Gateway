package org.dcsa.conformance.standards.ebl.crypto.impl;

import org.bouncycastle.cert.X509CertificateHolder;
import org.dcsa.conformance.standards.ebl.crypto.JWSSignerDetails;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerWithKey;

import static org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory.pemEncodeCertificate;

public class X509BackedPayloadSigner extends DefaultPayloadSigner implements PayloadSignerWithKey {

  private final X509CertificateHolder x509Cert;

  public X509BackedPayloadSigner(JWSSignerDetails jwsSignerDetails, X509CertificateHolder x509Cert) {
    super(jwsSignerDetails);
    this.x509Cert = x509Cert;
  }

  @Override
  public String getPublicKeyInPemFormat() {
    return pemEncodeCertificate(this.x509Cert);
  }
}
