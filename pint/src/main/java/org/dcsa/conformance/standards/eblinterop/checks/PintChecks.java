package org.dcsa.conformance.standards.eblinterop.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSObject;
import java.text.ParseException;
import java.util.*;
import java.util.function.*;
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

  public static JsonContentMatchedValidation arraySizeMustEqual(IntSupplier expectedSizeSupplier) {
    return (nodeToValidate,contextPath) -> {
      if (!nodeToValidate.isArray()) {
        var expectedSize = expectedSizeSupplier.getAsInt();
        if (expectedSize > 0) {
          return Set.of(
            "Expected '%s' to be an array with size %d, but it was not an array".formatted(
              contextPath, expectedSize
            )
          );
        }
        // Schema validation error if anything
        return Set.of();
      }
      var size = nodeToValidate.size();
      var expectedSize = expectedSizeSupplier.getAsInt();
      if (expectedSize == size) {
        return Set.of();
      }
      if (expectedSize < 0) {
        return Set.of("Error: Could not determine the expected size of the array at '%s'. This is a bug in the test".formatted(
          contextPath
        ));
      }
      return Set.of("The size of the array at '%s' was %d, but it should have been %d".formatted(
        contextPath,
        size,
        expectedSize
      ));
    };
  }

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

  public static JsonContentMatchedValidation missingDocumentChecksumsSubsetCheck(Supplier<DynamicScenarioParameters> dspSupplier) {
    return (nodeToValidate, contextPath) -> {
      if (!nodeToValidate.isArray()) {
        // Schema validation will take care of this one.
        return Set.of();
      }
      var checksums = dspSupplier.get().documentChecksums();
      int idx = 0;
      var issues = new LinkedHashSet<String>();
      for (var checksumNode : nodeToValidate) {
        var path = contextPath + "[" + idx + "]";
        idx++;
        var value = checksumNode.asText();
        if (!checksums.contains(value)) {
          issues.add("The checksum '%s' at '%s' was not listed in the initial transfer request".formatted(value, path));
        }
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

  public static JsonContentMatchedValidation expectedTDChecksum(Supplier<DynamicScenarioParameters> dynamicScenarioParametersSupplier) {
    return JsonAttribute.matchedMustEqual(delayedValue(dynamicScenarioParametersSupplier, DynamicScenarioParameters::transportDocumentChecksum));
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
            "The path '%s' should have been a signed payload containing Json as content, but could not be parsed: %s".formatted(
              contextPath, e.toString()
            ));
      }
      return delegate.validate(jsonBody, contextPath + "!");
    };
  }

  public static JsonContentMatchedValidation signedContentSchemaValidation(
      JsonSchemaValidator schemaValidator) {
    return (nodeToValidate, contextPath) -> {
      var content = nodeToValidate.asText();
      if (content == null || !content.contains(".")) {
        return Set.of("The path '%s' should have been a signed payload, but was not".formatted(contextPath));
      }
      JWSObject jwsObject;
      try {
        jwsObject = JWSObject.parse(content);
      } catch (ParseException e) {
        return Set.of(
            "The path '%s' should have been a signed payload, but could not be parsed as a JWS.".formatted(contextPath));
      }
      JsonNode jsonBody;
      try {
        jsonBody = OBJECT_MAPPER.readTree(jwsObject.getPayload().toString());
      } catch (Exception e) {
        return Set.of(
            "The path '%s' should have been a signed payload containing Json as content, but could not be parsed: %s".formatted(
              contextPath,
              e.toString()
            ));
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
        return Set.of("The path '%s' should have been a signed payload, but was not".formatted(contextPath));
      }
      JWSObject jwsObject;
      try {
        jwsObject = JWSObject.parse(content);
      } catch (ParseException e) {
        return Set.of(
            "The path '%s' should have been a signed payload, but could not be parsed as a JWS (%s).".formatted(contextPath, e.toString()));
      }
      if (Algorithm.NONE.equals(jwsObject.getHeader().getAlgorithm())) {
        return Set.of(
          "The JWS payload at '%s' uses the 'none' algorithm and is therefore unsigned.".formatted(contextPath));
      }
      var signatureVerifier = signatureVerifierSupplier.get();
      if (signatureVerifier == null) {
        throw new AssertionError("Missing signatureVerifier");
      }
      if (!signatureVerifier.verifySignature(jwsObject)) {
        return Set.of("The path '%s' was a valid JWS, but it was not signed by the expected key".formatted(contextPath));
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
    checks.add(
      JsonAttribute.customValidator(
        "[Scenario] Verify that the number of additional documents match the scenario",
        JsonAttribute.path("envelopeManifestSignedContent", signedContentValidation(
          JsonAttribute.path("supportingDocuments", arraySizeMustEqual(delayedValue(dspSupplier, DynamicScenarioParameters::documentCount, -1)))
        ))
      )
    );
  }

  public static ActionCheck validateUnsignedStartResponse(
    UUID matched,
    int missingDocumentCount,
    Supplier<DynamicScenarioParameters> dspSupplier
  ) {
    var jsonContentChecks = new ArrayList<JsonContentCheck>();
    jsonContentChecks.add(
      JsonAttribute.customValidator(
        "The number of missing documents is correct",
        JsonAttribute.path("missingAdditionalDocumentChecksums", arraySizeMustEqual(() -> missingDocumentCount))
      )
    );
    jsonContentChecks.add(
      JsonAttribute.customValidator(
        "The checksums of additional documents are known",
        JsonAttribute.path("missingAdditionalDocumentChecksums", missingDocumentChecksumsSubsetCheck(dspSupplier))
      )
    );
    jsonContentChecks.add(
      JsonAttribute.customValidator(
        "Validate the missing documents checksum",
        JsonAttribute.path("transportDocumentChecksum", expectedTDChecksum(dspSupplier))
      )
    );
    jsonContentChecks.add(
      JsonAttribute.customValidator(
        "Validate the transportDocument checksum",
        JsonAttribute.path("transportDocumentChecksum", expectedTDChecksum(dspSupplier))
      )
    );
    return JsonAttribute.contentChecks(
      PintRole::isReceivingPlatform,
      matched,
      HttpMessageType.RESPONSE,
      jsonContentChecks
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
    jsonContentChecks.add(
      JsonAttribute.customValidator(
        "Validate the transportDocument checksum in the envelopeManifestSignedContent",
        JsonAttribute.path(
          "envelopeManifestSignedContent",
          signedContentValidation(
            JsonAttribute.path("transportDocumentChecksum", expectedTDChecksum(dspSupplier))
          )
        )
      )
    );
    jsonContentChecks.add(
      JsonAttribute.allIndividualMatchesMustBeValid(
        "Validate the transportDocument checksum in the envelopeTransferChain",
        (mav) -> mav.submitAllMatching("envelopeTransferChain.*"),
        signedContentValidation(
          JsonAttribute.path("transportDocumentChecksum", expectedTDChecksum(dspSupplier))
        )
      )
    );
    return JsonAttribute.contentChecks(
      PintRole::isSendingPlatform,
      matched,
      HttpMessageType.REQUEST,
      jsonContentChecks
    );
  }

  public static <T, O> Supplier<T> delayedValue(Supplier<O> cspSupplier, Function<O, T> field) {
    return () -> {
      var csp = cspSupplier.get();
      if (csp == null) {
        return null;
      }
      return field.apply(csp);
    };
  }


  private static <O> IntSupplier delayedValue(Supplier<O> cspSupplier, ToIntFunction<O> field, int placeholderValue) {
    return () -> {
      var csp = cspSupplier.get();
      if (csp == null) {
        return placeholderValue;
      }
      return field.applyAsInt(csp);
    };
  }

}
