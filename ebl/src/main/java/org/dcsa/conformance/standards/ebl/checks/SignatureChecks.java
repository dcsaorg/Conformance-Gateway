package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSObject;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonContentCheckRebaser;
import org.dcsa.conformance.core.check.JsonContentMatchedValidation;
import org.dcsa.conformance.core.check.JsonRebaseableContentCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.ebl.crypto.SignatureVerifier;

import java.text.ParseException;
import java.util.Set;
import java.util.function.Supplier;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@UtilityClass
public class SignatureChecks {

  private static final JsonContentCheckRebaser SIGNED_CONTENT_REBASER = delegate -> (nodeToValidate, contextPath) -> {
    var content = nodeToValidate.asText();
    if (content == null || !content.contains(".")) {
      return ConformanceCheckResult.simple(Set.of(
        "The path '%s' should have been a signed payload, but was not".formatted(contextPath)));
    }
    JWSObject jwsObject;
    try {
      jwsObject = JWSObject.parse(content);
    } catch (ParseException e) {
      return ConformanceCheckResult.simple(Set.of(
        "The path '%s' should have been a signed payload, but could not be parsed as a JWS."
          .formatted(contextPath)));
    }
    JsonNode jsonBody;
    try {
      jsonBody = OBJECT_MAPPER.readTree(jwsObject.getPayload().toString());
    } catch (Exception e) {
      return ConformanceCheckResult.simple(Set.of(
        "The path '%s' should have been a signed payload containing Json as content, but could not be parsed: %s".formatted(
          contextPath, e.toString()
        )));
    }
    return delegate.validate(jsonBody, contextPath + "!");
  };

  public static JsonRebaseableContentCheck signedContentValidation(
    JsonRebaseableContentCheck delegate
  ) {
    return SIGNED_CONTENT_REBASER.offset(delegate);
  }

  public static JsonContentMatchedValidation signedContentValidation(
    JsonContentMatchedValidation delegate) {
    return SIGNED_CONTENT_REBASER.offset(delegate);
  }

  public static JsonContentMatchedValidation signedContentSchemaValidation(
    JsonSchemaValidator schemaValidator) {
    return (nodeToValidate, contextPath) -> {
      var content = nodeToValidate.asText();
      if (content == null || !content.contains(".")) {
        return ConformanceCheckResult.simple(Set.of("The path '%s' should have been a signed payload, but was not".formatted(contextPath)));
      }
      JWSObject jwsObject;
      try {
        jwsObject = JWSObject.parse(content);
      } catch (ParseException e) {
        return ConformanceCheckResult.simple(Set.of(
            "The path '%s' should have been a signed payload, but could not be parsed as a JWS.".formatted(contextPath)));
      }
      JsonNode jsonBody;
      try {
        jsonBody = OBJECT_MAPPER.readTree(jwsObject.getPayload().toString());
      } catch (Exception e) {
        return ConformanceCheckResult.simple(Set.of(
            "The path '%s' should have been a signed payload containing Json as content, but could not be parsed: %s".formatted(
              contextPath,
              e.toString()
            )));
      }
      return ConformanceCheckResult.simple(schemaValidator.validate(jsonBody));
    };
  }

  public static JsonContentMatchedValidation signatureValidates(
    Supplier<SignatureVerifier> signatureVerifierSupplier) {
    return (nodeToValidate, contextPath) -> {
      var content = nodeToValidate.asText();
      if (content == null || !content.contains(".")) {
        return ConformanceCheckResult.simple(Set.of("The path '%s' should have been a signed payload, but was not".formatted(contextPath)));
      }
      JWSObject jwsObject;
      try {
        jwsObject = JWSObject.parse(content);
      } catch (ParseException e) {
        return ConformanceCheckResult.simple(Set.of(
            "The path '%s' should have been a signed payload, but could not be parsed as a JWS (%s).".formatted(contextPath, e.toString())));
      }
      if (Algorithm.NONE.equals(jwsObject.getHeader().getAlgorithm())) {
        return ConformanceCheckResult.simple(Set.of(
          "The JWS payload at '%s' uses the 'none' algorithm and is therefore unsigned.".formatted(contextPath)));
      }
      var signatureVerifier = signatureVerifierSupplier.get();
      if (signatureVerifier == null) {
        throw new AssertionError("Missing signatureVerifier");
      }
      if (!signatureVerifier.verifySignature(jwsObject)) {
        return ConformanceCheckResult.simple(Set.of("The path '%s' was a valid JWS, but it was not signed by the expected key".formatted(contextPath)));
      }
      return ConformanceCheckResult.simple(Set.of());
    };
  }

}
