package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import java.util.List;
import java.util.Set;
import org.dcsa.conformance.core.check.*;
import org.junit.jupiter.api.Test;

class SignatureChecksTest {
  private static JsonNode signed(String payload) throws Exception {
    JWSObject jws = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(payload));
    jws.sign(new MACSigner(new byte[32]));
    return OBJECT_MAPPER.getNodeFactory().textNode(jws.serialize());
  }

  @Test
  void signedContentReportsMalformedTokensAndDelegatesDecodedJson() throws Exception {
    JsonContentMatchedValidation delegate =
        (node, path) -> {
          assertEquals("root!", path);
          assertEquals("ok", node.required("value").asText());
          return ConformanceCheckResult.simple(Set.of());
        };
    var validation = SignatureChecks.signedContentValidation(delegate);
    JsonSchemaValidator schema = mock(JsonSchemaValidator.class);
    when(schema.validate(any(JsonNode.class))).thenReturn(Set.of());
    var schemaCheck = SignatureChecks.signedContentSchemaValidation(schema);
    JsonNode nullText = mock(JsonNode.class);
    when(nullText.asText()).thenReturn(null);
    for (JsonNode bad :
        List.of(
            nullText,
            OBJECT_MAPPER.getNodeFactory().textNode("not signed"),
            OBJECT_MAPPER.getNodeFactory().textNode("bad.jws.token"),
            signed("{invalid"))) {
      assertFalse(validation.validate(bad, "root").isConformant());
      assertFalse(schemaCheck.validate(bad, "root").isConformant());
    }
    JsonNode valid = signed("{\"value\":\"ok\"}");
    assertTrue(validation.validate(valid, "root").isConformant());
    assertTrue(schemaCheck.validate(valid, "root").isConformant());
    verify(schema).validate(OBJECT_MAPPER.readTree("{\"value\":\"ok\"}"));
    JsonRebasableContentCheck rebasable = JsonAttribute.customValidator("decoded", delegate);
    assertTrue(
        SignatureChecks.signedContentValidation(rebasable).validate(valid, "root").isConformant());
  }

  @Test
  void signatureVerificationRejectsMalformedUnsignedAndWrongKeyTokens() throws Exception {
    JsonNode nullText = mock(JsonNode.class);
    when(nullText.asText()).thenReturn(null);
    for (JsonNode bad :
        List.of(
            nullText,
            OBJECT_MAPPER.getNodeFactory().textNode("plain"),
            OBJECT_MAPPER.getNodeFactory().textNode("bad.jws.token"))) {
      assertFalse(
          SignatureChecks.signatureValidates(() -> token -> true)
              .validate(bad, "signature")
              .isConformant());
    }
    JsonNode valid = signed("{}");
    assertTrue(
        SignatureChecks.signatureValidates(() -> token -> true)
            .validate(valid, "signature")
            .isConformant());
    assertFalse(
        SignatureChecks.signatureValidates(() -> token -> false)
            .validate(valid, "signature")
            .isConformant());
    assertThrows(
        AssertionError.class,
        () -> SignatureChecks.signatureValidates(() -> null).validate(valid, "signature"));
    // Nimbus rejects alg=none during parsing. Exercise the defensive guard independently.
    JWSObject unsigned = mock(JWSObject.class);
    JWSHeader header = mock(JWSHeader.class);
    when(unsigned.getHeader()).thenReturn(header);
    when(header.getAlgorithm()).thenReturn(new JWSAlgorithm("none"));
    try (var parser = mockStatic(JWSObject.class)) {
      parser.when(() -> JWSObject.parse("unsigned.token.value")).thenReturn(unsigned);
      assertFalse(
          SignatureChecks.signatureValidates(() -> token -> true)
              .validate(
                  OBJECT_MAPPER.getNodeFactory().textNode("unsigned.token.value"), "signature")
              .isConformant());
    }
  }
}
