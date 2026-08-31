package org.dcsa.conformance.standards.eblissuance.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.UUID;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerWithKey;
import org.junit.jupiter.api.Test;

class IssuanceChecksTest {

  private static final String SCHEMA_PATH =
    "/standards/eblissuance/schemas/EBL_ISS_v3.0.0.yaml";
  private static final PayloadSignerWithKey SIGNER = PayloadSignerFactory.carrierPayloadSigner();
  private static final JsonSchemaValidator MANIFEST_SCHEMA =
      JsonSchemaValidator.getInstance(SCHEMA_PATH, "IssuanceManifest");

  @Test
  void validIssuanceRequestPassesEveryIssuanceCheck() {
    ObjectNode request = validRequest(true);

    issuanceChecks().forEach(check -> assertValid(check, request));
  }

  @Test
  void usesDocumentedIssuanceManifestValidationDescriptions() {
    assertEquals(
        List.of(
            "Signature of the 'issuanceManifestSignedContent' is valid",
            "Schema validation of the payload of 'issuanceManifestSignedManifest'",
            "Checksum of 'transportDocument' vs. the checksum provided in the 'issuanceManifest'",
            "Checksum of 'issueTo' vs. the checksum provided in the 'issuanceManifest'",
            "Checksum of 'eBLVisualisationByCarrier' vs. the checksum provided in the 'issuanceManifest' (if provided)"),
        issuanceChecks().stream().map(JsonContentCheck::description).toList());
  }

  @Test
  void invalidSignatureIsRejected() {
    ObjectNode request = validRequest(false);
    String signature = request.path("issuanceManifestSignedContent").asText();
    int signatureStart = signature.lastIndexOf('.') + 1;
    int tamperIndex = signatureStart + 5;
    char replacement = signature.charAt(tamperIndex) == 'A' ? 'B' : 'A';
    request.put(
        "issuanceManifestSignedContent",
        signature.substring(0, tamperIndex)
            + replacement
            + signature.substring(tamperIndex + 1));

    assertInvalid(issuanceChecks().get(0), request);
  }

  @Test
  void invalidSignedManifestSchemaIsRejected() {
    ObjectNode request = validRequest(false);
    request.put("issuanceManifestSignedContent", SIGNER.sign("{}"));

    assertValid(issuanceChecks().get(0), request);
    assertInvalid(issuanceChecks().get(1), request);
  }

  @Test
  void transportDocumentChecksumMismatchIsRejected() {
    ObjectNode request = validRequest(false);
    ((ObjectNode) request.path("document")).put("transportDocumentReference", "changed");

    assertInvalid(issuanceChecks().get(2), request);
  }

  @Test
  void issueToChecksumMismatchIsRejected() {
    ObjectNode request = validRequest(false);
    ((ObjectNode) request.path("issueTo")).put("partyName", "changed");

    assertInvalid(issuanceChecks().get(3), request);
  }

  @Test
  void optionalVisualizationAndItsChecksumMustHaveMatchingPresence() {
    ObjectNode requestWithoutVisualization = validRequest(false);
    assertValid(issuanceChecks().get(4), requestWithoutVisualization);

    ObjectNode request = validRequest(true);
    ((ObjectNode) request.path("eBLVisualisationByCarrier")).put("content", "changed");
    assertInvalid(issuanceChecks().get(4), request);

    ObjectNode manifest = manifestFor(requestWithoutVisualization, false);
    manifest.put("eBLVisualisationByCarrierChecksum", "0".repeat(64));
    requestWithoutVisualization.put("issuanceManifestSignedContent", SIGNER.sign(manifest.toString()));
    assertInvalid(issuanceChecks().get(4), requestWithoutVisualization);
  }

  @Test
  void malformedVisualizationContentIsRejectedWithoutThrowing() {
    ObjectNode request = validRequest(true);
    ((ObjectNode) request.path("eBLVisualisationByCarrier")).put("content", "%%%not-base64%%%");

    assertInvalid(issuanceChecks().get(4), request);
  }

  @Test
  void createsRuntimeActionChecks() {
    UUID exchangeId = UUID.randomUUID();
    var issuanceManifestChecks =
        IssuanceChecks.issuanceRequestSignatureChecks(
            exchangeId,
            "3.0.3",
            MANIFEST_SCHEMA,
            () ->
                PayloadSignerFactory.verifierFromPemEncodedCertificate(
                    SIGNER.getPublicKeyInPemFormat(), "certificate"));

    assertEquals(
        "Complex validations of 'issuanceManifest'", issuanceManifestChecks.getTitle().trim());
    assertTrue(issuanceManifestChecks.isRelevant());
    assertTrue(IssuanceChecks.tdContentChecks(exchangeId, "3.0.3").isRelevant());
  }

  private static List<JsonContentCheck> issuanceChecks() {
    return IssuanceChecks.issuanceRequestContentChecks(
        MANIFEST_SCHEMA,
        () ->
            PayloadSignerFactory.verifierFromPemEncodedCertificate(
                SIGNER.getPublicKeyInPemFormat(), "certificate"));
  }

  private static ObjectNode validRequest(boolean includeVisualization) {
    ObjectNode request = OBJECT_MAPPER.createObjectNode();
    request
        .putObject("document")
        .put("transportDocumentReference", "TDR-1")
        .put("transportDocumentStatus", "ISSUED");
    request.putObject("issueTo").put("partyName", "Issue-to party");
    if (includeVisualization) {
      request
          .putObject("eBLVisualisationByCarrier")
          .put("name", "eBL.pdf")
          .put("content", new byte[] {1, 2, 3});
    }
    request.put(
        "issuanceManifestSignedContent",
        SIGNER.sign(manifestFor(request, includeVisualization).toString()));
    return request;
  }

  private static ObjectNode manifestFor(ObjectNode request, boolean includeVisualization) {
    ObjectNode manifest =
        OBJECT_MAPPER
            .createObjectNode()
            .put("documentChecksum", Checksums.sha256CanonicalJson(request.path("document")))
            .put("issueToChecksum", Checksums.sha256CanonicalJson(request.path("issueTo")));
    if (includeVisualization) {
      try {
        manifest.put(
            "eBLVisualisationByCarrierChecksum",
            Checksums.sha256(
                request.path("eBLVisualisationByCarrier").path("content").binaryValue()));
      } catch (java.io.IOException e) {
        throw new IllegalArgumentException(e);
      }
    }
    return manifest;
  }

  private static void assertValid(JsonContentCheck check, ObjectNode request) {
    assertTrue(check.validate(request).getErrorMessages().isEmpty());
  }

  private static void assertInvalid(JsonContentCheck check, ObjectNode request) {
    assertFalse(check.validate(request).getErrorMessages().isEmpty());
  }
}




