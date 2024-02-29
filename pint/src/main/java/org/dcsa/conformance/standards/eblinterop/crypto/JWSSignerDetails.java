package org.dcsa.conformance.standards.eblinterop.crypto;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;

public record JWSSignerDetails(JWSAlgorithm algorithm, JWSSigner signer) {}
