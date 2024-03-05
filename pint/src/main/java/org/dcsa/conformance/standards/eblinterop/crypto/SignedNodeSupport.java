package org.dcsa.conformance.standards.eblinterop.crypto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSObject;

import java.text.ParseException;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public class SignedNodeSupport {
  public static JsonNode parseSignedNode(JsonNode node) throws ParseException, JsonProcessingException {
    var jwsObject = JWSObject.parse(node.asText());
    return OBJECT_MAPPER.readTree(jwsObject.getPayload().toString());
  }

  public static JsonNode parseSignedNode(JsonNode node, SignatureVerifier signatureVerifier)
      throws ParseException, JsonProcessingException, CouldNotValidateSignatureException {
    var jwsObject = JWSObject.parse(node.asText());
    if (!signatureVerifier.verifySignature(jwsObject)) {
      throw new CouldNotValidateSignatureException();
    }
    return OBJECT_MAPPER.readTree(jwsObject.getPayload().toString());
  }


  public static JsonNode parseSignedNodeNoErrors(JsonNode node) {
      try {
          return parseSignedNode(node);
      } catch (ParseException|JsonProcessingException e) {
          return OBJECT_MAPPER.missingNode();
      }

  }
}
