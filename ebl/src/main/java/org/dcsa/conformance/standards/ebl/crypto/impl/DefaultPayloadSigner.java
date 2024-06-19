package org.dcsa.conformance.standards.ebl.crypto.impl;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.dcsa.conformance.standards.ebl.crypto.JWSSignerDetails;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSigner;

@RequiredArgsConstructor
public class DefaultPayloadSigner implements PayloadSigner {

  private final JWSSignerDetails jwsSignerDetails;

  @SneakyThrows
  public String sign(String payload) {
    JWSHeader header = new JWSHeader.Builder(jwsSignerDetails.algorithm()).build();
    JWSObject jwsObject = new JWSObject(header, new Payload(payload));
    jwsObject.sign(jwsSignerDetails.signer());
    return jwsObject.serialize();
  }
}
