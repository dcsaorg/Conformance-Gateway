package org.dcsa.conformance.standards.eblinterop.checks;

import static org.dcsa.conformance.core.check.JsonAttribute.matchedMustEqual;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.genericTDContentChecks;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES;
import static org.dcsa.conformance.standards.ebl.checks.SignatureChecks.*;
import static org.dcsa.conformance.standards.ebl.crypto.SignedNodeSupport.parseSignedNode;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.text.ParseException;
import java.util.*;
import java.util.function.*;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.SignatureChecks;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.ebl.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.ebl.crypto.SignedNodeSupport;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;
import org.dcsa.conformance.standards.eblinterop.action.PintResponseCode;
import org.dcsa.conformance.standards.eblinterop.action.SenderTransmissionClass;
import org.dcsa.conformance.standards.eblinterop.models.DynamicScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.ReceiverScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.SenderScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.party.PintRole;

public class PintChecks {

  private static final JsonPointer TDR_PTR = JsonPointer.compile("/transportDocument/transportDocumentReference");

  private static final JsonRebaseableContentCheck TRANSACTION_PARTY_CODE_LIST_PROVIDER = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate 'codeListProvider' is a known value",
    (mav) -> {
      mav.submitAllMatching("transactions.*.actor.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("transactions.*.recipient.identifyingCodes.*.codeListProvider");
    },
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES)
  );

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

  public static ActionCheck tdContentChecks(UUID matched, String standardsVersion, Supplier<SenderScenarioParameters> senderScenarioParametersSupplier) {
    var checks = genericTDContentChecks(TransportDocumentStatus.TD_ISSUED, standardsVersion, delayedValue(senderScenarioParametersSupplier, SenderScenarioParameters::transportDocumentReference));
    return JsonAttribute.contentChecks(
      "Complex validations of transport document",
      PintRole::isSendingPlatform,
      matched,
      HttpMessageType.REQUEST,
      standardsVersion,
      JsonContentCheckRebaser.of("transportDocument"),
      checks
    );
  }

  public static JsonContentMatchedValidation expectedTDChecksum(Supplier<DynamicScenarioParameters> dynamicScenarioParametersSupplier) {
    return matchedMustEqual(delayedValue(dynamicScenarioParametersSupplier, DynamicScenarioParameters::transportDocumentChecksum));
  }

  private static void generateScenarioRelatedChecksForTransferRequest(
      List<JsonContentCheck> checks,
      SenderTransmissionClass senderTransmissionClass,
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
        "[Scenario] Verify receiver party is present (and exactly as-is)",
        pathChain(
          signedContentValidation(
            pathChain(
              (n, p) -> {
                if (senderTransmissionClass == SenderTransmissionClass.WRONG_RECIPIENT_PLATFORM) {
                  return Set.of();
                }
                if (Objects.equals(rspSupplier.get().receiverParty(), n)) {
                  return Set.of();
                }
                return Set.of("[Scenario] Last transaction did not use the receiving party provided by the receiver (exactly as-is)");
              },
              "transactions", -1, "recipient")
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
    String standardsVersion,
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
        "Validate the transportDocument checksum",
        JsonAttribute.path("transportDocumentChecksum", expectedTDChecksum(dspSupplier))
      )
    );
    return JsonAttribute.contentChecks(
      PintRole::isReceivingPlatform,
      matched,
      HttpMessageType.RESPONSE,
      standardsVersion,
      jsonContentChecks
    );
  }

  public static ActionCheck validateSignedFinishResponse(
    UUID matched,
    String standardsVersion,
    PintResponseCode expectedResponseCode
  ) {
    var jsonContentChecks = new ArrayList<JsonContentCheck>();

    jsonContentChecks.add(
      JsonAttribute.customValidator(
        "Validate that the response code was as expected",
        signedContentValidation(
          JsonAttribute.path("responseCode", matchedMustEqual(expectedResponseCode::name))
        )
      )
    );
    jsonContentChecks.add(
      signedContentValidation(
        JsonAttribute.ifThenElse(
          "Validate that 'duplicateOfAcceptedEnvelopeTransferChainEntrySignedContent' is conditionally present (%s)".formatted(PintResponseCode.DUPE.name()),
          JsonAttribute.isEqualTo("responseCode", PintResponseCode.DUPE.name()),
          JsonAttribute.path("duplicateOfAcceptedEnvelopeTransferChainEntrySignedContent", JsonAttribute.matchedMustBeNotNull())::validate,
          JsonAttribute.path("duplicateOfAcceptedEnvelopeTransferChainEntrySignedContent", JsonAttribute.matchedMustBeAbsent())::validate
        )
      )
    );
    jsonContentChecks.add(
      signedContentValidation(
        JsonAttribute.ifThenElse(
          "Validate that 'receivedAdditionalDocumentChecksums' is conditionally present (%s or %s)".formatted(PintResponseCode.RECE.name(), PintResponseCode.DUPE.name()),
          JsonAttribute.isOneOf("responseCode", Set.of(PintResponseCode.RECE.name(), PintResponseCode.DUPE.name())),
          JsonAttribute.path("receivedAdditionalDocumentChecksums", JsonAttribute.matchedMustBePresent())::validate,
          JsonAttribute.path("receivedAdditionalDocumentChecksums", JsonAttribute.matchedMustBeAbsent())::validate
        )
      )
    );

    jsonContentChecks.add(
      signedContentValidation(
        JsonAttribute.ifThenElse(
          "Validate that 'missingAdditionalDocumentChecksums' is conditionally present (%s)".formatted(PintResponseCode.MDOC.name()),
          JsonAttribute.isEqualTo("responseCode", PintResponseCode.MDOC.name()),
          JsonAttribute.path("missingAdditionalDocumentChecksums", JsonAttribute.matchedMustBeNonEmpty())::validate,
          JsonAttribute.path("missingAdditionalDocumentChecksums", JsonAttribute.matchedMustBeAbsent())::validate
        )
      )
    );
    return JsonAttribute.contentChecks(
      PintRole::isReceivingPlatform,
      matched,
      HttpMessageType.RESPONSE,
      standardsVersion,
      jsonContentChecks
    );
  }

  public static ActionCheck validateRequestSignatures(
    UUID uuid,
    String expectedApiVersion,
    Supplier<SignatureVerifier> senderVerifierSupplier,
    Supplier<SignatureVerifier> carrierVerifierSupplier
  ) {
    return JsonAttribute.contentChecks(
      PintRole::isSendingPlatform,
      uuid,
      HttpMessageType.REQUEST,
      expectedApiVersion,
      JsonAttribute.customValidator("envelopeManifestSignedContent signature could be validated", JsonAttribute.path("envelopeManifestSignedContent", SignatureChecks.signatureValidates(senderVerifierSupplier))),
      JsonAttribute.allIndividualMatchesMustBeValid("envelopeManifestSignedContent signature could be validated", mav -> mav.submitAllMatching("envelopeTransferChain.*"), SignatureChecks.signatureValidates(senderVerifierSupplier)),
      JsonAttribute.customValidator(
        "issuanceManifestSignedContent signature could be validated",
        JsonAttribute.path("issuanceManifestSignedContent", signatureValidates(carrierVerifierSupplier))
      )
    );
  }

  public static ActionCheck validateInnerRequestSchemas(
    UUID uuid,
    String expectedApiVersion,
    JsonSchemaValidator envelopeEnvelopeSchemaValidator,
    JsonSchemaValidator envelopeTransferChainEntrySchemaValidator,
    JsonSchemaValidator issuanceManifestSchemaValidator
  ) {
    return JsonAttribute.contentChecks(
      PintRole::isSendingPlatform,
      uuid,
      HttpMessageType.REQUEST,
      expectedApiVersion,
      JsonAttribute.customValidator("envelopeManifestSignedContent matches schema", JsonAttribute.path("envelopeManifestSignedContent", SignatureChecks.signedContentSchemaValidation(envelopeEnvelopeSchemaValidator))),
      JsonAttribute.allIndividualMatchesMustBeValid("envelopeTransferChain matches schema", mav -> mav.submitAllMatching("envelopeTransferChain.*"), SignatureChecks.signedContentSchemaValidation(envelopeTransferChainEntrySchemaValidator)),
      JsonAttribute.customValidator(
        "Validate issuance manifest schema validation",
        JsonAttribute.path("issuanceManifestSignedContent", signedContentSchemaValidation(issuanceManifestSchemaValidator))
      )
    );
  }

  public static ActionCheck validateInitiateTransferRequest(
    UUID matched,
    String standardsVersion,
    SenderTransmissionClass senderTransmissionClass,
    Supplier<SenderScenarioParameters> sspSupplier,
    Supplier<ReceiverScenarioParameters> rspSupplier,
    Supplier<DynamicScenarioParameters> dspSupplier
  ) {
    var jsonContentChecks = new ArrayList<JsonContentCheck>();

    generateScenarioRelatedChecksForTransferRequest(jsonContentChecks, senderTransmissionClass, sspSupplier, rspSupplier, dspSupplier);
    // FIXME start of temporary workaround for SD-1942
//    jsonContentChecks.add(
//      JsonAttribute.customValidator(
//        "Validate the transportDocument checksum in the envelopeManifestSignedContent",
//        JsonAttribute.path(
//          "envelopeManifestSignedContent",
//          signedContentValidation(
//            JsonAttribute.path("transportDocumentChecksum", expectedTDChecksum(dspSupplier))
//          )
//        )
//      )
//    );
//    jsonContentChecks.add(
//      JsonAttribute.allIndividualMatchesMustBeValid(
//        "Validate the transportDocument checksum in the envelopeTransferChain",
//        (mav) -> mav.submitAllMatching("envelopeTransferChain.*"),
//        signedContentValidation(
//          JsonAttribute.path("transportDocumentChecksum", expectedTDChecksum(dspSupplier))
//        )
//      )
//    );
    // FIXME end of temporary workaround for SD-1942
    jsonContentChecks.add(
      JsonAttribute.allIndividualMatchesMustBeValid(
        "Validate codeListProvider",
        (mav) -> mav.submitAllMatching("envelopeTransferChain.*"),
        signedContentValidation(TRANSACTION_PARTY_CODE_LIST_PROVIDER::validate)
      )
    );
    jsonContentChecks.add(
      JsonAttribute.customValidator(
        "Validate transfer chain checksums",
        JsonAttribute.path("envelopeTransferChain", (etc, contextPath) -> {
          String expectedChecksum = null;
          if (!etc.isArray()) {
            // Leave that to schema validation
            return Set.of();
          }
          var issues = new LinkedHashSet<String>();
          for (int i = 0 ; i < etc.size() ; i++) {
            JsonNode entry = etc.path(i);
            JsonNode parsed;
            try {
              parsed = parseSignedNode(entry);
            } catch (ParseException | JsonProcessingException e) {
              // Signed content + schema validation already takes care of that issue.
              continue;
            }
            var actualChecksum = parsed.path("previousEnvelopeTransferChainEntrySignedContentChecksum").asText(null);
            if (!Objects.equals(expectedChecksum, actualChecksum)) {
              var path = contextPath + "[" + i + "].previousEnvelopeTransferChainEntrySignedContentChecksum";
              issues.add("The checksum in '%s' was '%s' but it should have been '%s' (which is the checksum of the preceding item)".formatted(
                path,
                actualChecksum,
                expectedChecksum
              ));
            }
            expectedChecksum = Checksums.sha256(entry.asText());
          }
          return issues;
        })
      )
    );
    jsonContentChecks.add(
      JsonAttribute.customValidator(
        "Validate conditional presence of issuanceManifest",
        (rootNode, contextPath) -> {
          var chain = rootNode.path("envelopeTransferChain");
          var lastIdx = chain.size() - 1;
          var lastEntry = chain.path(lastIdx);
          var payload = SignedNodeSupport.parseSignedNodeNoErrors(lastEntry);
          var issues = new LinkedHashSet<String>();
          var hadIssuance = false;
          for (var transaction : payload.path("transactions")) {
            if (transaction.path("action").asText("").equals("ISSU")) {
              hadIssuance = true;
              break;
            }
          }
          if (hadIssuance && rootNode.path("issuanceManifestSignedContent").isMissingNode()) {
            issues.add("Issuance transaction implies 'issuanceManifestSignedContent' being present");
          }
          return issues;
        })
    );
    // FIXME start of temporary workaround for SD-1942
//    jsonContentChecks.add(
//      JsonAttribute.customValidator(
//        "Validate issuance manifest checksums",
//        JsonAttribute.path("issuanceManifestSignedContent", signedContentValidation(JsonAttribute.path("documentChecksum", expectedTDChecksum(dspSupplier))))
//      )
//    );
    // FIXME end of temporary workaround for SD-1942
    return JsonAttribute.contentChecks(
      PintRole::isSendingPlatform,
      matched,
      HttpMessageType.REQUEST,
      standardsVersion,
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
