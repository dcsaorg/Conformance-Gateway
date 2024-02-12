package org.dcsa.conformance.standards.eblinterop.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSObject;
import java.text.ParseException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.eblinterop.action.PintResponseCode;
import org.dcsa.conformance.standards.eblinterop.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.eblinterop.models.DynamicScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.ReceiverScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.SenderScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

public class PintChecks {

  private static final JsonPointer TDR_PTR = JsonPointer.compile("/transportDocument/transportDocumentReference");

  public static JsonContentMatchedValidation anyArrayElementMatching(Predicate<JsonNode> matcher, JsonContentMatchedValidation delegate, boolean invalidIfNoMatch) {
    return (nodeToValidate,contextPath) -> {
      boolean hadMatch = false;
      Set<String> issues = new LinkedHashSet<>();
      if (nodeToValidate.isArray()) {
        int idx = -1;
        for (var node : nodeToValidate) {
          idx++;
          if (matcher.test(node)) {
            var r = delegate.validate(node, contextPath + "[" + idx + "]");
            issues.addAll(r);
            hadMatch = true;
          }
        }

        if (invalidIfNoMatch && !hadMatch) {
          issues.add("None of the elements in '" + contextPath + "' were the right type");
        }
      } else if (invalidIfNoMatch){
        issues.add("'" + contextPath + "' as not an array");
      }
      return issues;
    };
  }

  public static JsonContentMatchedValidation path(int idx, JsonContentMatchedValidation delegate) {
    return (nodeToValidate,contextPath) -> {
      var pathSegment = "[" + idx + "]";
      var realIdx = idx;
      if (realIdx == -1) {
        pathSegment = "[(last)]";
        realIdx = nodeToValidate.size() - 1;
      }
      var node = nodeToValidate.path(realIdx);
      var fullContext = contextPath.isEmpty() ? pathSegment : contextPath + pathSegment;
      return delegate.validate(node, fullContext);
    };
  }

  public static JsonContentMatchedValidation pathChain(JsonContentMatchedValidation delegate, Object ... paths) {
    JsonContentMatchedValidation combined = delegate;
    if (paths.length < 1) {
      throw new IllegalArgumentException();
    }
    for (int i = paths.length - 1 ; i >= 0; i--) {
      var path = paths[i];
      if (path instanceof Integer pathIdx) {
        combined = path(pathIdx, combined);
      } else if (path instanceof String pathStr) {
        combined = JsonAttribute.path(pathStr, combined);
      } else {
        throw new IllegalArgumentException("Only String and Integer paths are supported");
      }
    }
    return combined;
  }

  public static JsonContentMatchedValidation signedContentValidation(
    JsonContentCheck delegate) {
    return (nodeToValidate,contextPath) -> delegate.validate(nodeToValidate);
  }

  public static JsonContentMatchedValidation signedContentValidation(
      JsonContentMatchedValidation delegate) {
    return (nodeToValidate, contextPath) -> {
      var content = nodeToValidate.asText();
      if (content == null || !content.contains(".")) {
        return Set.of(
            "The path '%s' should have been a signed payload, but was not".formatted(contextPath));
      }
      JWSObject jwsObject;
      try {
        jwsObject = JWSObject.parse(content);
      } catch (ParseException e) {
        return Set.of(
            "The path '%s' should have been a signed payload, but could not be parsed as a JWS."
                .formatted(contextPath));
      }
      JsonNode jsonBody;
      try {
        jsonBody = OBJECT_MAPPER.readTree(jwsObject.getPayload().toString());
      } catch (Exception e) {
        return Set.of(
            "The path '%s' should have been a signed payload containing Json as content, but could not be parsed: "
                + e.toString());
      }
      return delegate.validate(jsonBody, contextPath + "!");
    };
  }

  public static JsonContentMatchedValidation signedContentSchemaValidation(
      JsonSchemaValidator schemaValidator) {
    return (nodeToValidate, contextPath) -> {
      var content = nodeToValidate.asText();
      if (content == null || !content.contains(".")) {
        return Set.of("The path '%s' should have been a signed payload, but was not");
      }
      JWSObject jwsObject = null;
      try {
        jwsObject = JWSObject.parse(content);
      } catch (ParseException e) {
        return Set.of(
            "The path '%s' should have been a signed payload, but could not be parsed as a JWS.");
      }
      JsonNode jsonBody;
      try {
        jsonBody = OBJECT_MAPPER.readTree(jwsObject.getPayload().toString());
      } catch (Exception e) {
        return Set.of(
            "The path '%s' should have been a signed payload containing Json as content, but could not be parsed: "
                + e.toString());
      }
      return schemaValidator.validate(jsonBody);
    };
  }
  ;

  public static JsonContentMatchedValidation signatureValidates(
      Supplier<SignatureVerifier> signatureVerifierSupplier) {
    return (nodeToValidate, contextPath) -> {
      var content = nodeToValidate.asText();
      if (content == null || !content.contains(".")) {
        return Set.of("The path '%s' should have been a signed payload, but was not");
      }
      JWSObject jwsObject;
      try {
        jwsObject = JWSObject.parse(content);
      } catch (ParseException e) {
        return Set.of(
            "The path '%s' should have been a signed payload, but could not be parsed as a JWS.");
      }
      var signatureVerifier = signatureVerifierSupplier.get();
      if (signatureVerifier == null) {
        throw new AssertionError("Missing signatureVerifier");
      }
      if (!signatureVerifier.verifySignature(jwsObject)) {
        return Set.of("The path '%s' was a valid JWS, but it was not signed by the expected key");
      }
      return Set.of();
    };
  }

  private static void generateScenarioRelatedChecksForTransferRequest(
      List<JsonContentCheck> checks,
      Supplier<SenderScenarioParameters> sspSupplier,
      Supplier<ReceiverScenarioParameters> rspSupplier,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    checks.add(
        JsonAttribute.mustEqual(
            "[Scenario] Verify that the correct 'transportDocumentReference' is used",
            TDR_PTR,
            delayedValue(sspSupplier, SenderScenarioParameters::transportDocumentReference)));
    checks.add(
      JsonAttribute.customValidator(
        "[Scenario] Verify receiver EPUI is correct",
        pathChain(
          signedContentValidation(
            pathChain(
              anyArrayElementMatching(
                (n) -> n.path("codeListProvider").asText("").equals("EPUI"),
                JsonAttribute.path("partyCode", JsonAttribute.matchedMustEqual(delayedValue(rspSupplier, ReceiverScenarioParameters::receiverEPUI))),
                true
              ),
              "transactions", -1, "recipient", "partyCodes")
             //
          ),
          "envelopeTransferChain", -1
        )
      )
    );
  }

  public static ActionCheck validateSignedFinishResponse(
    UUID matched,
    PintResponseCode expectedResponseCode
  ) {
    var jsonContentChecks = new ArrayList<JsonContentCheck>();

    jsonContentChecks.add(
      JsonAttribute.customValidator(
        "Validate that the response code was as expected",
        signedContentValidation(
          JsonAttribute.path("responseCode", JsonAttribute.matchedMustEqual(expectedResponseCode::name))
        )
      )
    );
    return JsonAttribute.contentChecks(
      PintRole::isReceivingPlatform,
      matched,
      HttpMessageType.RESPONSE,
      jsonContentChecks
    );

  }

  public static ActionCheck validateInitiateTransferRequest(
    UUID matched,
    Supplier<SenderScenarioParameters> sspSupplier,
    Supplier<ReceiverScenarioParameters> rspSupplier,
    Supplier<DynamicScenarioParameters> dspSupplier
  ) {
    var jsonContentChecks = new ArrayList<JsonContentCheck>();

    generateScenarioRelatedChecksForTransferRequest(jsonContentChecks, sspSupplier, rspSupplier, dspSupplier);
    return JsonAttribute.contentChecks(
      PintRole::isSendingPlatform,
      matched,
      HttpMessageType.REQUEST,
      jsonContentChecks
    );
  }

  private static <T, O> Supplier<T> delayedValue(Supplier<O> cspSupplier, Function<O, T> field) {
    return () -> {
      var csp = cspSupplier.get();
      if (csp == null) {
        return null;
      }
      return field.apply(csp);
    };
  }

}
