package org.dcsa.conformance.standards.ebl.crypto;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;

public record JWSSignerDetails(JWSAlgorithm algorithm, JWSSigner signer) {}
