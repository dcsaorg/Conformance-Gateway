package org.dcsa.conformance.standards.ebl.crypto;

import com.nimbusds.jose.JWSObject;

/**
 * Method for verifying signatures
 *
 * <p>
 * Abstracts away logic of trust store management behind this interface as those are generally
 * implementation details.
 * </p>
 */
public interface SignatureVerifier {

  /**
   * @param jwsObject The JWS object that has a signature to be validated
   * @return true if and only the implementation deemed the JWSObject provided to have a valid
   *   signature from the provided platform.
   */
  boolean verifySignature(JWSObject jwsObject);

}
