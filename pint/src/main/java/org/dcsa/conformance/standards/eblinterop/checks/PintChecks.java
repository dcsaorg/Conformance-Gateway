package org.dcsa.conformance.standards.eblinterop.checks;

import static org.dcsa.conformance.core.check.JsonAttribute.matchedMustEqual;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.genericTDContentChecks;
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

  private static final String TRANSACTIONS = "transactions";
  private static final String ACTOR = "actor";
  private static final String IDENTIFYING_CODE = "identifyingCodes";
  private static final String CODE_LIST_PROVIDER = "codeListProvider";
  private static final String RECIPIENT = "recipient";
  private static final String TRANSPORT_DOCUMENT = "transportDocument";
  private static final String TRANSPORT_DOCUMENT_REFERENCE = "transportDocumentReference";
  private static final String ENVELOPE_TRANSFER_CHAIN = "envelopeTransferChain";
  private static final String ENVELOPE_MANIFEST_SIGNED_CONTENT = "envelopeManifestSignedContent";
  private static final String SUPPORTING_DOCUMENTS = "supportingDocuments";
  private static final String MISSING_ADDITIONAL_DOCUMENT_CHECKSUMS =
      "missingAdditionalDocumentChecksums";
  private static final String TRANSPORT_DOCUMENT_CHECKSUM = "transportDocumentChecksum";
  private static final String RESPONSE_CODE = "responseCode";
  private static final String DUPLICATE_OF_ACCEPTED_ENVELOPE_TRANSFER_CHAIN_ENTRY_SIGNED_CONTENT =
      "duplicateOfAcceptedEnvelopeTransferChainEntrySignedContent";
  private static final String RECEIVED_ADDITIONAL_DOCUMENT_CHECKSUMS =
      "receivedAdditionalDocumentChecksums";
  private static final String ISSUANCE_MANIFEST_SIGNED_CONTENT = "issuanceManifestSignedContent";
  private static final String PREVIOUS_ENVELOPE_TRANSFER_CHAIN_ENTRY_SIGNED_CONTENT_CHECKSUM =
      "previousEnvelopeTransferChainEntrySignedContentChecksum";
  private static final String ISSUANCE_MANIFEST = "issuanceManifest";
  private static final String ACTION = "action";
  private static final String ISSU = "ISSU";
  private static final String DOCUMENT_CHECKSUM = "documentChecksum";

  private static final String SS = "/%s/%s";
  private static final String S_x_S_S_x_S = "%s.*.%s.%s.*.%s";
  private static final String S_x = "%s.*";

  private static final String S_SIGNATURE_COULD_BE_VALIDATED = "'%s' signature could be validated";
  private static final String S_MATCHES_SCHEMA = "'%s' matches schema";
  private static final String VALIDATE_THE_S_IN_THE_S = "Validate the '%s' in the '%s'";
  private static final String VALIDATE_S = "Validate '%s'";

  private PintChecks() {}

  private static final JsonPointer TDR_PTR =
      JsonPointer.compile(SS.formatted(TRANSPORT_DOCUMENT, TRANSPORT_DOCUMENT_REFERENCE));

  private static final JsonRebasableContentCheck TRANSACTION_PARTY_CODE_LIST_PROVIDER =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate '%s' is a known value".formatted(CODE_LIST_PROVIDER),
          mav -> {
            mav.submitAllMatching(
                S_x_S_S_x_S.formatted(TRANSACTIONS, ACTOR, IDENTIFYING_CODE, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_x_S_S_x_S.formatted(
                    TRANSACTIONS, RECIPIENT, IDENTIFYING_CODE, CODE_LIST_PROVIDER));
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES));

  public static JsonContentMatchedValidation arraySizeMustEqual(IntSupplier expectedSizeSupplier) {
    return (nodeToValidate, contextPath) -> {
      if (!nodeToValidate.isArray()) {
        var expectedSize = expectedSizeSupplier.getAsInt();
        if (expectedSize > 0) {
          return ConformanceCheckResult.simple(
              Set.of(
                  "Expected '%s' to be an array with size %d, but it was not an array"
                      .formatted(contextPath, expectedSize)));
        }
        // Schema validation error if anything
        return ConformanceCheckResult.simple(Set.of());
      }
      var size = nodeToValidate.size();
      var expectedSize = expectedSizeSupplier.getAsInt();
      if (expectedSize == size) {
        return ConformanceCheckResult.simple(Set.of());
      }
      if (expectedSize < 0) {
        return ConformanceCheckResult.simple(
            Set.of(
                "Error: Could not determine the expected size of the array at '%s'. This is a bug in the test"
                    .formatted(contextPath)));
      }
      return ConformanceCheckResult.simple(
          Set.of(
              "The size of the array at '%s' was %d, but it should have been %d"
                  .formatted(contextPath, size, expectedSize)));
    };
  }

  public static JsonContentMatchedValidation missingDocumentChecksumsSubsetCheck(
      Supplier<DynamicScenarioParameters> dspSupplier) {
    return (nodeToValidate, contextPath) -> {
      if (!nodeToValidate.isArray()) {
        // Schema validation will take care of this one.
        return ConformanceCheckResult.simple(Set.of());
      }
      var checksums = dspSupplier.get().documentChecksums();
      int idx = 0;
      var issues = new LinkedHashSet<String>();
      for (var checksumNode : nodeToValidate) {
        var path = contextPath + "[" + idx + "]";
        idx++;
        var value = checksumNode.asText();
        if (!checksums.contains(value)) {
          issues.add(
              "The checksum '%s' at '%s' was not listed in the initial transfer request"
                  .formatted(value, path));
        }
      }
      return ConformanceCheckResult.simple(issues);
    };
  }

  public static JsonContentMatchedValidation path(int idx, JsonContentMatchedValidation delegate) {
    return (nodeToValidate, contextPath) -> {
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

  public static JsonContentMatchedValidation pathChain(
      JsonContentMatchedValidation delegate, Object... paths) {
    JsonContentMatchedValidation combined = delegate;
    if (paths.length < 1) {
      throw new IllegalArgumentException();
    }
    for (int i = paths.length - 1; i >= 0; i--) {
      var path = paths[i];
      switch (path) {
        case Integer pathIdx -> combined = path(pathIdx, combined);
        case String pathStr -> combined = JsonAttribute.path(pathStr, combined);
        default ->
            throw new IllegalArgumentException("Only String and Integer paths are supported");
      }
    }
    return combined;
  }

  public static ActionCheck tdContentChecks(
      UUID matched,
      String standardsVersion,
      Supplier<SenderScenarioParameters> senderScenarioParametersSupplier) {
    var checks =
        genericTDContentChecks(
            TransportDocumentStatus.TD_ISSUED,
            delayedValue(
                senderScenarioParametersSupplier,
                SenderScenarioParameters::transportDocumentReference));
    return JsonAttribute.contentChecks(
        "Complex validations of '%s' content".formatted(TRANSPORT_DOCUMENT),
        PintRole::isSendingPlatform,
        matched,
        HttpMessageType.REQUEST,
        standardsVersion,
        JsonContentCheckRebaser.of(TRANSPORT_DOCUMENT),
        checks);
  }

  public static JsonContentMatchedValidation expectedTDChecksum(
      Supplier<DynamicScenarioParameters> dynamicScenarioParametersSupplier) {
    return matchedMustEqual(
        delayedValue(
            dynamicScenarioParametersSupplier,
            DynamicScenarioParameters::transportDocumentChecksum));
  }

  private static void generateScenarioRelatedChecksForTransferRequest(
      List<JsonContentCheck> checks,
      SenderTransmissionClass senderTransmissionClass,
      Supplier<SenderScenarioParameters> sspSupplier,
      Supplier<ReceiverScenarioParameters> rspSupplier,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    checks.add(
        JsonAttribute.mustEqual(
            "[Scenario] Verify that the correct '%s' is used"
                .formatted(TRANSPORT_DOCUMENT_REFERENCE),
            TDR_PTR,
            delayedValue(sspSupplier, SenderScenarioParameters::transportDocumentReference)));
    checks.add(
        JsonAttribute.customValidator(
            "[Scenario] Verify receiver party is present (and exactly as-is)",
            pathChain(
                signedContentValidation(
                    pathChain(
                        (n, p) -> {
                          if (senderTransmissionClass
                              == SenderTransmissionClass.WRONG_RECIPIENT_PLATFORM) {
                            return ConformanceCheckResult.simple(Set.of());
                          }
                          if (Objects.equals(rspSupplier.get().receiverParty(), n)) {
                            return ConformanceCheckResult.simple(Set.of());
                          }
                          return ConformanceCheckResult.simple(
                              Set.of(
                                  "[Scenario] Last transaction did not use the receiving party provided by the receiver (exactly as-is)"));
                        },
                        TRANSACTIONS,
                        -1,
                        RECIPIENT)),
                ENVELOPE_TRANSFER_CHAIN,
                -1)));
    checks.add(
        JsonAttribute.customValidator(
            "[Scenario] Verify that the number of additional documents match the scenario",
            JsonAttribute.path(
                ENVELOPE_MANIFEST_SIGNED_CONTENT,
                signedContentValidation(
                    JsonAttribute.path(
                        SUPPORTING_DOCUMENTS,
                        arraySizeMustEqual(
                            delayedValue(
                                dspSupplier, DynamicScenarioParameters::documentCount, -1)))))));
  }

  public static ActionCheck validateUnsignedStartResponse(
      UUID matched,
      String standardsVersion,
      int missingDocumentCount,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    var jsonContentChecks = new ArrayList<JsonContentCheck>();
    jsonContentChecks.add(
        JsonAttribute.customValidator(
            "The number of missing documents is correct",
            JsonAttribute.path(
                MISSING_ADDITIONAL_DOCUMENT_CHECKSUMS,
                arraySizeMustEqual(() -> missingDocumentCount))));
    jsonContentChecks.add(
        JsonAttribute.customValidator(
            "The checksums of additional documents are known",
            JsonAttribute.path(
                MISSING_ADDITIONAL_DOCUMENT_CHECKSUMS,
                missingDocumentChecksumsSubsetCheck(dspSupplier))));
    jsonContentChecks.add(
        JsonAttribute.customValidator(
            "Validate the '%s'".formatted(TRANSPORT_DOCUMENT_CHECKSUM),
            JsonAttribute.path(TRANSPORT_DOCUMENT_CHECKSUM, expectedTDChecksum(dspSupplier))));
    return JsonAttribute.contentChecks(
        PintRole::isReceivingPlatform,
        matched,
        HttpMessageType.RESPONSE,
        standardsVersion,
        jsonContentChecks);
  }

  public static ActionCheck validateSignedFinishResponse(
      UUID matched, String standardsVersion, PintResponseCode expectedResponseCode) {
    var jsonContentChecks = new ArrayList<JsonContentCheck>();

    jsonContentChecks.add(
        JsonAttribute.customValidator(
            "Validate that the '%s' was as expected".formatted(RESPONSE_CODE),
            signedContentValidation(
                JsonAttribute.path(RESPONSE_CODE, matchedMustEqual(expectedResponseCode::name)))));
    jsonContentChecks.add(
        signedContentValidation(
            JsonAttribute.ifThenElse(
                "Validate that '%s' is conditionally present (%s)"
                    .formatted(
                        DUPLICATE_OF_ACCEPTED_ENVELOPE_TRANSFER_CHAIN_ENTRY_SIGNED_CONTENT,
                        PintResponseCode.DUPE.name()),
                JsonAttribute.isEqualTo(RESPONSE_CODE, PintResponseCode.DUPE.name()),
                JsonAttribute.path(
                        DUPLICATE_OF_ACCEPTED_ENVELOPE_TRANSFER_CHAIN_ENTRY_SIGNED_CONTENT,
                        JsonAttribute.matchedMustBeNotNull())
                    ::validate,
                JsonAttribute.path(
                        DUPLICATE_OF_ACCEPTED_ENVELOPE_TRANSFER_CHAIN_ENTRY_SIGNED_CONTENT,
                        JsonAttribute.matchedMustBeAbsent())
                    ::validate)));
    jsonContentChecks.add(
        signedContentValidation(
            JsonAttribute.ifThenElse(
                "Validate that '%s' is conditionally present (%s or %s)"
                    .formatted(
                        RECEIVED_ADDITIONAL_DOCUMENT_CHECKSUMS,
                        PintResponseCode.RECE.name(),
                        PintResponseCode.DUPE.name()),
                JsonAttribute.isOneOf(
                    RESPONSE_CODE,
                    Set.of(PintResponseCode.RECE.name(), PintResponseCode.DUPE.name())),
                JsonAttribute.path(
                        RECEIVED_ADDITIONAL_DOCUMENT_CHECKSUMS,
                        JsonAttribute.matchedMustBePresent())
                    ::validate,
                JsonAttribute.path(
                        RECEIVED_ADDITIONAL_DOCUMENT_CHECKSUMS, JsonAttribute.matchedMustBeAbsent())
                    ::validate)));

    jsonContentChecks.add(
        signedContentValidation(
            JsonAttribute.ifThenElse(
                "Validate that '%s' is conditionally present (%s)"
                    .formatted(MISSING_ADDITIONAL_DOCUMENT_CHECKSUMS, PintResponseCode.MDOC.name()),
                JsonAttribute.isEqualTo(RESPONSE_CODE, PintResponseCode.MDOC.name()),
                JsonAttribute.path(
                        MISSING_ADDITIONAL_DOCUMENT_CHECKSUMS,
                        JsonAttribute.matchedMustBeNonEmpty())
                    ::validate,
                JsonAttribute.path(
                        MISSING_ADDITIONAL_DOCUMENT_CHECKSUMS, JsonAttribute.matchedMustBeAbsent())
                    ::validate)));
    return JsonAttribute.contentChecks(
        PintRole::isReceivingPlatform,
        matched,
        HttpMessageType.RESPONSE,
        standardsVersion,
        jsonContentChecks);
  }

  public static ActionCheck validateRequestSignatures(
      UUID uuid,
      String expectedApiVersion,
      Supplier<SignatureVerifier> senderVerifierSupplier,
      Supplier<SignatureVerifier> carrierVerifierSupplier) {
    return JsonAttribute.contentChecks(
        PintRole::isSendingPlatform,
        uuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        JsonAttribute.customValidator(
            S_SIGNATURE_COULD_BE_VALIDATED.formatted(ENVELOPE_MANIFEST_SIGNED_CONTENT),
            JsonAttribute.path(
                ENVELOPE_MANIFEST_SIGNED_CONTENT,
                SignatureChecks.signatureValidates(senderVerifierSupplier))),
        JsonAttribute.allIndividualMatchesMustBeValid(
            S_SIGNATURE_COULD_BE_VALIDATED.formatted(ENVELOPE_MANIFEST_SIGNED_CONTENT),
            mav -> mav.submitAllMatching(S_x.formatted(ENVELOPE_TRANSFER_CHAIN)),
            SignatureChecks.signatureValidates(senderVerifierSupplier)),
        JsonAttribute.customValidator(
            S_SIGNATURE_COULD_BE_VALIDATED.formatted(ISSUANCE_MANIFEST_SIGNED_CONTENT),
            JsonAttribute.path(
                ISSUANCE_MANIFEST_SIGNED_CONTENT, signatureValidates(carrierVerifierSupplier))));
  }

  public static ActionCheck validateInnerRequestSchemas(
      UUID uuid,
      String expectedApiVersion,
      JsonSchemaValidator envelopeEnvelopeSchemaValidator,
      JsonSchemaValidator envelopeTransferChainEntrySchemaValidator,
      JsonSchemaValidator issuanceManifestSchemaValidator) {
    return JsonAttribute.contentChecks(
        PintRole::isSendingPlatform,
        uuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        JsonAttribute.customValidator(
            S_MATCHES_SCHEMA.formatted(ENVELOPE_MANIFEST_SIGNED_CONTENT),
            JsonAttribute.path(
                ENVELOPE_MANIFEST_SIGNED_CONTENT,
                SignatureChecks.signedContentSchemaValidation(envelopeEnvelopeSchemaValidator))),
        JsonAttribute.allIndividualMatchesMustBeValid(
            S_MATCHES_SCHEMA.formatted(ENVELOPE_TRANSFER_CHAIN),
            mav -> mav.submitAllMatching(S_x.formatted(ENVELOPE_TRANSFER_CHAIN)),
            SignatureChecks.signedContentSchemaValidation(
                envelopeTransferChainEntrySchemaValidator)),
        JsonAttribute.customValidator(
            "Validate '%s".formatted(ISSUANCE_MANIFEST_SIGNED_CONTENT),
            JsonAttribute.path(
                ISSUANCE_MANIFEST_SIGNED_CONTENT,
                signedContentSchemaValidation(issuanceManifestSchemaValidator))));
  }

  public static ActionCheck validateInitiateTransferRequest(
      UUID matched,
      String standardsVersion,
      SenderTransmissionClass senderTransmissionClass,
      Supplier<SenderScenarioParameters> sspSupplier,
      Supplier<ReceiverScenarioParameters> rspSupplier,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    var jsonContentChecks = new ArrayList<JsonContentCheck>();

    generateScenarioRelatedChecksForTransferRequest(
        jsonContentChecks, senderTransmissionClass, sspSupplier, rspSupplier, dspSupplier);
    jsonContentChecks.add(
        JsonAttribute.customValidator(
            VALIDATE_THE_S_IN_THE_S.formatted(
                TRANSPORT_DOCUMENT_CHECKSUM, ENVELOPE_MANIFEST_SIGNED_CONTENT),
            JsonAttribute.path(
                ENVELOPE_MANIFEST_SIGNED_CONTENT,
                signedContentValidation(
                    JsonAttribute.path(
                        TRANSPORT_DOCUMENT_CHECKSUM, expectedTDChecksum(dspSupplier))))));
    jsonContentChecks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            VALIDATE_THE_S_IN_THE_S.formatted(TRANSPORT_DOCUMENT_CHECKSUM, ENVELOPE_TRANSFER_CHAIN),
            mav -> mav.submitAllMatching(S_x.formatted(ENVELOPE_TRANSFER_CHAIN)),
            signedContentValidation(
                JsonAttribute.path(TRANSPORT_DOCUMENT_CHECKSUM, expectedTDChecksum(dspSupplier)))));
    jsonContentChecks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            VALIDATE_S.formatted(CODE_LIST_PROVIDER),
            mav -> mav.submitAllMatching(S_x.formatted(ENVELOPE_TRANSFER_CHAIN)),
            signedContentValidation(TRANSACTION_PARTY_CODE_LIST_PROVIDER::validate)));
    jsonContentChecks.add(
        JsonAttribute.customValidator(
            "Validate transfer chain checksums",
            JsonAttribute.path(
                ENVELOPE_TRANSFER_CHAIN,
                (etc, contextPath) -> {
                  String expectedChecksum = null;
                  if (!etc.isArray()) {
                    // Leave that to schema validation
                    return ConformanceCheckResult.simple(Set.of());
                  }
                  var issues = new LinkedHashSet<String>();
                  for (int i = 0; i < etc.size(); i++) {
                    JsonNode entry = etc.path(i);
                    JsonNode parsed;
                    try {
                      parsed = parseSignedNode(entry);
                    } catch (ParseException | JsonProcessingException e) {
                      // Signed content + schema validation already takes care of that issue.
                      continue;
                    }
                    var actualChecksum =
                        parsed
                            .path(PREVIOUS_ENVELOPE_TRANSFER_CHAIN_ENTRY_SIGNED_CONTENT_CHECKSUM)
                            .asText(null);
                    if (!Objects.equals(expectedChecksum, actualChecksum)) {
                      var path =
                          contextPath
                              + "["
                              + i
                              + "].%s"
                                  .formatted(
                                      PREVIOUS_ENVELOPE_TRANSFER_CHAIN_ENTRY_SIGNED_CONTENT_CHECKSUM);
                      issues.add(
                          "The checksum in '%s' was '%s' but it should have been '%s' (which is the checksum of the preceding item)"
                              .formatted(path, actualChecksum, expectedChecksum));
                    }
                    expectedChecksum = Checksums.sha256(entry.asText());
                  }
                  return ConformanceCheckResult.simple(issues);
                })));
    jsonContentChecks.add(
        JsonAttribute.customValidator(
            "Validate conditional presence of '%s'".formatted(ISSUANCE_MANIFEST),
            (rootNode, contextPath) -> {
              var chain = rootNode.path(ENVELOPE_TRANSFER_CHAIN);
              var lastIdx = chain.size() - 1;
              var lastEntry = chain.path(lastIdx);
              var payload = SignedNodeSupport.parseSignedNodeNoErrors(lastEntry);
              var issues = new LinkedHashSet<String>();
              var hadIssuance = false;
              for (var transaction : payload.path(TRANSACTIONS)) {
                if (transaction.path(ACTION).asText("").equals(ISSU)) {
                  hadIssuance = true;
                  break;
                }
              }
              if (hadIssuance && rootNode.path(ISSUANCE_MANIFEST_SIGNED_CONTENT).isMissingNode()) {
                issues.add(
                    "Issuance transaction implies '%s' being present"
                        .formatted(ISSUANCE_MANIFEST_SIGNED_CONTENT));
              }
              return ConformanceCheckResult.simple(issues);
            }));
    jsonContentChecks.add(
        JsonAttribute.customValidator(
            "Validate issuance manifest checksums",
            JsonAttribute.path(
                ISSUANCE_MANIFEST_SIGNED_CONTENT,
                signedContentValidation(
                    JsonAttribute.path(DOCUMENT_CHECKSUM, expectedTDChecksum(dspSupplier))))));
    return JsonAttribute.contentChecks(
        PintRole::isSendingPlatform,
        matched,
        HttpMessageType.REQUEST,
        standardsVersion,
        jsonContentChecks);
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

  private static <O> IntSupplier delayedValue(
      Supplier<O> cspSupplier, ToIntFunction<O> field, int placeholderValue) {
    return () -> {
      var csp = cspSupplier.get();
      if (csp == null) {
        return placeholderValue;
      }
      return field.applyAsInt(csp);
    };
  }
}
