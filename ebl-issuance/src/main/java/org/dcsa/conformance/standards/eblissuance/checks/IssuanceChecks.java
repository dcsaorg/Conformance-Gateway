package org.dcsa.conformance.standards.eblissuance.checks;

import static org.dcsa.conformance.core.check.JsonAttribute.*;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.genericTDContentChecks;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.SignatureChecks;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.ebl.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;
import org.dcsa.conformance.standards.eblissuance.action.EblType;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

@UtilityClass
public class IssuanceChecks {

  private static final String CODE_LIST_PROVIDER = "codeListProvider";
  private static final String ISSUE_TO = "issueTo";
  private static final String IDENTIFYING_CODES = "identifyingCodes";
  private static final String ENDORSEE = "endorsee";
  private static final String DOCUMENT = "document";
  private static final String DOCUMENT_PARTIES = "documentParties";
  private static final String IS_TO_ORDER = "isToOrder";
  private static final String ISSUANCE_MANIFEST_SIGNED_CONTENT = "issuanceManifestSignedContent";
  private static final String ISSUANCE_MANIFEST_SIGNED_MANIFEST = "issuanceManifestSignedManifest";
  private static final String DOCUMENT_CHECKSUM = "documentChecksum";
  private static final String ISSUE_TO_CHECKSUM = "issueToChecksum";
  private static final String EBL_VISUALISATION_BY_CARRIER = "eBLVisualisationByCarrier";
  private static final String EBL_VISUALISATION_BY_CARRIER_CHECKSUM =
      "eBLVisualisationByCarrierChecksum";

  private static final JsonRebasableContentCheck ISSUE_TO_CODE_LIST_PROVIDER =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The '%s' is valid".formatted(CODE_LIST_PROVIDER),
          mav ->
              mav.submitAllMatching(
                  "%s.%s.*.%s".formatted(ISSUE_TO, IDENTIFYING_CODES, CODE_LIST_PROVIDER)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES));

  private static JsonRebasableContentCheck hasEndorseeScenarioCheck(EblType eblType) {
    return JsonAttribute.customValidator(
        "[Scenario] Validate '%s' party presence is correct".formatted(ENDORSEE),
        JsonAttribute.path(
            DOCUMENT,
            JsonAttribute.path(
                DOCUMENT_PARTIES,
                (documentParties, contextPath) -> {
                  if (!eblType.isToOrder()) {
                    return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
                  }
                  var hadEndorsee = documentParties.has(ENDORSEE);
                  var endorseePath =
                      concatContextPath(contextPath, "%s.%s".formatted(DOCUMENT_PARTIES, ENDORSEE));
                  if (eblType.isBlankEbl() && hadEndorsee) {
                    return ConformanceCheckResult.simple(
                        Set.of(
                            "The EBL should have been blank endorsed, but it has an '%s' attribute"
                                .formatted(endorseePath)));
                  }
                  if (!eblType.isBlankEbl() && !hadEndorsee) {
                    return ConformanceCheckResult.simple(
                        Set.of(
                            "The EBL should have had a named endorsee, but it is missing the '%s' attribute"
                                .formatted(endorseePath)));
                  }
                  return ConformanceCheckResult.simple(Set.of());
                })));
  }

  public static ActionCheck tdScenarioChecks(
      UUID matched, String standardsVersion, EblType eblType) {
    return JsonAttribute.contentChecks(
        "Complex validations of transport document",
        null,
        EblIssuanceRole::isCarrier,
        matched,
        HttpMessageType.REQUEST,
        standardsVersion,
        JsonAttribute.mustEqual(
            "[Scenario] The '%s.%s' attribute must match the scenario requirements"
                .formatted(DOCUMENT, IS_TO_ORDER),
            JsonPointer.compile("/%s/%s".formatted(DOCUMENT, IS_TO_ORDER)),
            eblType.isToOrder()),
        ISSUE_TO_CODE_LIST_PROVIDER,
        hasEndorseeScenarioCheck(eblType));
  }

  public static ActionCheck issuanceRequestSignatureChecks(
      UUID matched,
      String standardsVersion,
      JsonSchemaValidator issuanceManifestSchemaValidator,
      Supplier<SignatureVerifier> signatureVerifierSupplier) {
    return JsonAttribute.contentChecks(
        "",
        "Complex validations of issuanceManifest",
        EblIssuanceRole::isCarrier,
        matched,
        HttpMessageType.REQUEST,
        standardsVersion,
        JsonAttribute.customValidator(
            "Signature of the '%s' is valid".formatted(ISSUANCE_MANIFEST_SIGNED_CONTENT),
            path(
                ISSUANCE_MANIFEST_SIGNED_CONTENT,
                SignatureChecks.signatureValidates(signatureVerifierSupplier))),
        JsonAttribute.customValidator(
            "Schema validation of the payload of '%s'".formatted(ISSUANCE_MANIFEST_SIGNED_MANIFEST),
            path(
                ISSUANCE_MANIFEST_SIGNED_CONTENT,
                SignatureChecks.signedContentSchemaValidation(issuanceManifestSchemaValidator))),
        JsonAttribute.customValidator(
            "Validate checksum of transportDocument vs. the checksum provided in the issuanceManifest",
            validateJsonNodeToChecksumAttribute(
                DOCUMENT, DOCUMENT_CHECKSUM, Checksums::sha256CanonicalJson)),
        JsonAttribute.customValidator(
            "Validate checksum of issueTo vs. the checksum provided in the issuanceManifest",
            validateJsonNodeToChecksumAttribute(
                ISSUE_TO, ISSUE_TO_CHECKSUM, Checksums::sha256CanonicalJson)),
        JsonAttribute.customValidator(
            "Validate checksum of '%s' vs. the checksum provided in the issuanceManifest"
                .formatted(EBL_VISUALISATION_BY_CARRIER),
            validateJsonNodeToChecksumAttribute(
                EBL_VISUALISATION_BY_CARRIER,
                EBL_VISUALISATION_BY_CARRIER_CHECKSUM,
                IssuanceChecks::checksumSupportingDocument)));
  }

  private static String checksumSupportingDocument(JsonNode node) {
    try {
      return Checksums.sha256(node.path("content").binaryValue());
    } catch (IOException e) {
      return "<Document was incorrect format>";
    }
  }

  public static JsonContentMatchedValidation validateJsonNodeToChecksumAttribute(
      String protectedAttribute,
      String manifestChecksumAttribute,
      Function<JsonNode, String> checksummer) {
    return (nodeToValidate, contextPath) -> {
      var json = nodeToValidate.path(protectedAttribute);
      var checksumValidator = JsonAttribute.matchedMustBeAbsent();
      if (!json.isMissingNode()) {
        var actualChecksum = checksummer.apply(json);
        checksumValidator =
            JsonAttribute.combine(
                JsonAttribute.matchedMustBePresent(), matchedMustEqual(() -> actualChecksum));
      }
      var c =
          path(
              ISSUANCE_MANIFEST_SIGNED_CONTENT,
              SignatureChecks.signedContentValidation(
                  path(manifestChecksumAttribute, checksumValidator)));
      return c.validate(nodeToValidate, contextPath);
    };
  }

  public static ActionCheck tdContentChecks(UUID matched, String standardsVersion) {
    var checks = genericTDContentChecks(TransportDocumentStatus.TD_ISSUED, null);
    return JsonAttribute.contentChecks(
        "Complex validations of transport document",
        EblIssuanceRole::isCarrier,
        matched,
        HttpMessageType.REQUEST,
        standardsVersion,
        JsonContentCheckRebaser.of(DOCUMENT),
        checks);
  }
}
