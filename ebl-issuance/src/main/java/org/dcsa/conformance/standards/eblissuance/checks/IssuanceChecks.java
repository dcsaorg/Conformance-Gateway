package org.dcsa.conformance.standards.eblissuance.checks;

import static org.dcsa.conformance.core.check.JsonAttribute.*;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.genericTDContentChecks;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES;

import com.fasterxml.jackson.core.JsonPointer;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.checks.SignatureChecks;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.ebl.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;
import org.dcsa.conformance.standards.eblissuance.action.EblType;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

public class IssuanceChecks {

  private static final JsonRebaseableContentCheck ISSUE_TO_CODE_LIST_PROVIDER = JsonAttribute.allIndividualMatchesMustBeValid(
    "The 'codeListProvider' is valid",
    mav -> mav.submitAllMatching("issueTo.identifyingCodes.*.codeListProvider"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES)
  );

  private static JsonRebaseableContentCheck hasEndorseeScenarioCheck(String standardsVersion, EblType eblType) {
    return JsonAttribute.customValidator(
      "[Scenario] Validate endorsee party presence is correct",
      JsonAttribute.path("document", JsonAttribute.path("documentParties",
        (documentParties, contextPath) -> {
          if (!eblType.isToOrder()) {
            return Set.of();
          }
          var hadEndorsee = documentParties.has("endorsee");
          var endorseePath = concatContextPath(contextPath, "documentParties.endorsee");
          if (eblType.isBlankEbl() && hadEndorsee) {
            return Set.of("The EBL should have been blank endorsed, but it has an '%s' attribute".formatted(endorseePath));
          }
          if (!eblType.isBlankEbl() && !hadEndorsee) {
            return Set.of("The EBL should have had a named endorsee, but it is missing the '%s' attribute".formatted(endorseePath));
          }
          return Set.of();
        }
      ))
    );
  }

  public static ActionCheck tdScenarioChecks(UUID matched, String standardsVersion, EblType eblType) {
    return JsonAttribute.contentChecks(
      "Complex validations of transport document",
      null,
      EblIssuanceRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      standardsVersion,
      JsonAttribute.mustEqual(
        "[Scenario] The 'document.isToOrder' attribute must match the scenario requirements",
        JsonPointer.compile("/document/isToOrder"),
        eblType.isToOrder()
      ),
      ISSUE_TO_CODE_LIST_PROVIDER,
      hasEndorseeScenarioCheck(standardsVersion, eblType)
    );
  }

  public static ActionCheck issuanceRequestSignatureChecks(UUID matched, String standardsVersion, JsonSchemaValidator issuanceManifestSchemaValidator, Supplier<SignatureVerifier> signatureVerifierSupplier) {
    return JsonAttribute.contentChecks(
        "",
        "Complex validations of issuanceManifest",
        EblIssuanceRole::isCarrier,
        matched,
        HttpMessageType.REQUEST,
        standardsVersion,
        JsonAttribute.customValidator(
          "Signature of the issuanceManifestSignedContent is valid",
          path("issuanceManifestSignedContent", SignatureChecks.signatureValidates(signatureVerifierSupplier))
         ),
        JsonAttribute.customValidator(
          "Schema validation of the payload of issuanceManifestSignedManifest",
          path("issuanceManifestSignedContent", SignatureChecks.signedContentSchemaValidation(
            issuanceManifestSchemaValidator
          ))
        ),
        JsonAttribute.customValidator(
          "Validate checksum of transportDocument vs. the checksum provided in the issuanceManifest",
          validateJsonNodeToChecksumAttribute(
            "document",
            "documentChecksum"
          )
        ),
        JsonAttribute.customValidator(
          "Validate checksum of issueTo vs. the checksum provided in the issuanceManifest",
          validateJsonNodeToChecksumAttribute(
            "issueTo",
            "issueToChecksum"
          )
        ),
        JsonAttribute.customValidator(
          "Validate checksum of eBLVisualisationByCarrier vs. the checksum provided in the issuanceManifest",
          validateJsonNodeToChecksumAttribute(
            "eBLVisualisationByCarrier",
            "eBLVisualisationByCarrierChecksum"
          )
        )
    );
  }

  public static JsonContentMatchedValidation validateJsonNodeToChecksumAttribute(
    String protectedAttribute,
    String manifestChecksumAttribute
  ) {
    return (nodeToValidate, contextPath) -> {
      var json = nodeToValidate.path(protectedAttribute);
      var checksumValidator = JsonAttribute.matchedMustBeAbsent();
      if (!json.isMissingNode()) {
        var actualChecksum = Checksums.sha256CanonicalJson(json);
        checksumValidator = JsonAttribute.combine(
          JsonAttribute.matchedMustBePresent(),
          matchedMustEqual(() -> actualChecksum)
        );
      }
      var c = path("issuanceManifestSignedContent", SignatureChecks.signedContentValidation(path(manifestChecksumAttribute, checksumValidator)));
      return c.validate(nodeToValidate, contextPath);
    };
  }

  public static ActionCheck tdContentChecks(UUID matched, String standardsVersion) {
    var checks = genericTDContentChecks(TransportDocumentStatus.TD_ISSUED, standardsVersion, null);
    return JsonAttribute.contentChecks(
      "Complex validations of transport document",
      EblIssuanceRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      standardsVersion,
      JsonContentCheckRebaser.of("document"),
      checks
    );
  }
}
