package org.dcsa.conformance.standards.eblissuance.checks;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonContentCheckRebaser;
import org.dcsa.conformance.core.check.JsonContentMatchedValidation;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.SignatureChecks;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.ebl.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.dcsa.conformance.core.check.JsonAttribute.matchedMustEqual;
import static org.dcsa.conformance.core.check.JsonAttribute.path;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.transportDocumentCarrierContentChecks;

@UtilityClass
public class IssuanceChecks {

  private static final String ISSUE_TO = "issueTo";
  private static final String DOCUMENT = "document";
  private static final String TRANSPORT_DOCUMENT = "transportDocument";
  private static final String ISSUANCE_MANIFEST = "issuanceManifest";
  private static final String ISSUANCE_MANIFEST_SIGNED_CONTENT = "issuanceManifestSignedContent";
  private static final String ISSUANCE_MANIFEST_SIGNED_MANIFEST = "issuanceManifestSignedManifest";
  private static final String DOCUMENT_CHECKSUM = "documentChecksum";
  private static final String ISSUE_TO_CHECKSUM = "issueToChecksum";
  private static final String EBL_VISUALISATION_BY_CARRIER = "eBLVisualisationByCarrier";
  private static final String EBL_VISUALISATION_BY_CARRIER_CHECKSUM = "eBLVisualisationByCarrierChecksum";
  private static final String CONTENT = "content";
  private static final String TRANSPORT_DOCUMENT_REFERENCE = "transportDocumentReference";
  private static final String ISSUANCE_RESPONSE_CODE = "issuanceResponseCode";
  private static final String SUCCESSFUL_ISSUANCE_RESPONSE_CODE = "ISSU";

  public static ActionCheck issuanceRequestSignatureChecks(
    UUID matched,
    String standardsVersion,
    JsonSchemaValidator issuanceManifestSchemaValidator,
    Supplier<SignatureVerifier> signatureVerifierSupplier) {
    return JsonAttribute.contentChecks(
      "",
      "Complex validations of '%s'".formatted(ISSUANCE_MANIFEST),
      EblIssuanceRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      standardsVersion,
      issuanceRequestContentChecks(issuanceManifestSchemaValidator, signatureVerifierSupplier));
  }

  static List<JsonContentCheck> issuanceRequestContentChecks(
    JsonSchemaValidator issuanceManifestSchemaValidator,
    Supplier<SignatureVerifier> signatureVerifierSupplier) {
    return List.of(
      JsonAttribute.customValidator(
        "Signature of the '%s' is valid".formatted(ISSUANCE_MANIFEST_SIGNED_CONTENT),
        path(
          ISSUANCE_MANIFEST_SIGNED_CONTENT,
          SignatureChecks.signatureValidates(signatureVerifierSupplier))),
      JsonAttribute.customValidator(
        "Schema validation of the payload of '%s'"
          .formatted(ISSUANCE_MANIFEST_SIGNED_MANIFEST),
        path(
          ISSUANCE_MANIFEST_SIGNED_CONTENT,
          SignatureChecks.signedContentSchemaValidation(issuanceManifestSchemaValidator))),
      JsonAttribute.customValidator(
        "Checksum of '%s' vs. the checksum provided in the '%s'"
          .formatted(TRANSPORT_DOCUMENT, ISSUANCE_MANIFEST),
        validateJsonNodeToChecksumAttribute(
          DOCUMENT, DOCUMENT_CHECKSUM, Checksums::sha256CanonicalJson)),
      JsonAttribute.customValidator(
        "Checksum of '%s' vs. the checksum provided in the '%s'"
          .formatted(ISSUE_TO, ISSUANCE_MANIFEST),
        validateJsonNodeToChecksumAttribute(
          ISSUE_TO, ISSUE_TO_CHECKSUM, Checksums::sha256CanonicalJson)),
      JsonAttribute.customValidator(
        "Checksum of '%s' vs. the checksum provided in the '%s' (if provided)"
          .formatted(EBL_VISUALISATION_BY_CARRIER, ISSUANCE_MANIFEST),
        validateJsonNodeToChecksumAttribute(
          EBL_VISUALISATION_BY_CARRIER,
          EBL_VISUALISATION_BY_CARRIER_CHECKSUM,
          IssuanceChecks::checksumSupportingDocument)));
  }

  public static ActionCheck issuanceResponseChecks(UUID matched, String standardsVersion, Supplier<String> transportDocumentReferenceSupplier) {
    return JsonAttribute.contentChecks(
      "[Response]",
      null,
      EblIssuanceRole::isPlatform,
      matched,
      HttpMessageType.REQUEST,
      standardsVersion,
      issuanceResponseContentChecks(transportDocumentReferenceSupplier));
  }

  static List<JsonContentCheck> issuanceResponseContentChecks(Supplier<String> transportDocumentReferenceSupplier) {
    return List.of(
      JsonAttribute.mustEqual(
        JsonPointer.compile("/" + TRANSPORT_DOCUMENT_REFERENCE),
        transportDocumentReferenceSupplier),
      JsonAttribute.mustEqual(
        JsonPointer.compile("/" + ISSUANCE_RESPONSE_CODE),
        () -> SUCCESSFUL_ISSUANCE_RESPONSE_CODE)
    );
  }

  private static String checksumSupportingDocument(JsonNode node) {
    try {
      return Checksums.sha256(node.path(CONTENT).binaryValue());
    } catch (IOException e) {
      return "<Document was incorrect format>";
    }
  }

  public static JsonContentMatchedValidation validateJsonNodeToChecksumAttribute(String protectedAttribute, String manifestChecksumAttribute, Function<JsonNode, String> checksummer) {
    return (nodeToValidate, contextPath) -> {
      var json = nodeToValidate.path(protectedAttribute);
      var checksumValidator = JsonAttribute.matchedMustBeAbsent();
      if (!json.isMissingNode()) {
        var actualChecksum = checksummer.apply(json);
        checksumValidator = JsonAttribute.combine(JsonAttribute.matchedMustBePresent(), matchedMustEqual(() -> actualChecksum));
      }
      var c = path(
        ISSUANCE_MANIFEST_SIGNED_CONTENT,
        SignatureChecks.signedContentValidation(path(manifestChecksumAttribute, checksumValidator)));
      return c.validate(nodeToValidate, contextPath);
    };
  }

  public static ActionCheck tdContentChecks(UUID matched, String standardsVersion) {
    var checks = transportDocumentCarrierContentChecks();
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
