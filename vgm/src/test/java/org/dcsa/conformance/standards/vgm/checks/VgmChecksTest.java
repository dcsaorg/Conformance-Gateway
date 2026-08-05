package org.dcsa.conformance.standards.vgm.checks;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VgmChecksTest {

  @Test
  void shouldCreateGetPayloadChecks() {
    assertNotNull(VgmChecks.getVGMGetPayloadChecks(null, "1.0.0"));
  }

  @Test
  void shouldCreatePostPayloadChecks() {
    assertNotNull(VgmChecks.getVGMPostPayloadChecks(null, "1.0.0"));
  }

  @Test
  void shouldUseDocumentedDescriptionForDeclarationInMessageValidation() {
    assertEquals(
      "At least one VGM Declaration must be included in a message sent to the sandbox during conformance testing.",
      VgmChecks.atLeastOneVgmDeclarationInMessageCheck().description());
  }

  @Test
  void shouldUseDocumentedDescriptionForVgmObjectValidation() {
    assertEquals(
      "At least one VGM Declaration must demonstrate the correct use of the VGM object.",
      VgmChecks.atLeastOneVgmDeclarationWithVgmObjectCheck().description());
  }

  @Test
  void shouldUseDocumentedDescriptionForWeightObjectValidation() {
    assertEquals(
      "The VGM object within at least one VGM Declaration must demonstrate the correct use of the weight object.",
      VgmChecks.atLeastOneVgmDeclarationWithWeightObjectCheck().description());
  }

  @Test
  void shouldUseDocumentedDescriptionForWeightValueValidation() {
    assertEquals(
      "The VGM.weight object within at least one VGM Declaration must demonstrate the correct use of the value attribute (positive number).",
      VgmChecks.atLeastOneVgmDeclarationWithPositiveWeightValueCheck().description());
  }

  @Test
  void shouldUseDocumentedDescriptionForWeightUnitValidation() {
    assertEquals(
      "The VGM.weight object within at least one VGM Declaration must demonstrate the correct use of the unit attribute ('KGM' or 'LBR').",
      VgmChecks.atLeastOneVgmDeclarationWithValidWeightUnitCheck().description());
  }

  @Test
  void shouldUseDocumentedDescriptionForMethodValidation() {
    assertEquals(
      "The VGM object within at least one VGM Declaration must demonstrate the correct use of the method attribute ('SM1' or 'SM2').",
      VgmChecks.atLeastOneVgmDeclarationWithValidMethodCheck().description());
  }

  @Test
  void shouldUseDocumentedDescriptionForEquipmentDetailsValidation() {
    assertEquals(
      "At least one VGM Declaration must demonstrate the correct use of the equipmentDetails object.",
      VgmChecks.atLeastOneVgmDeclarationWithEquipmentDetailsCheck().description());
  }

  @Test
  void shouldUseDocumentedDescriptionForEquipmentReferenceValidation() {
    assertEquals(
      "The equipmentDetails object within at least one VGM Declaration must demonstrate the correct use of the equipmentReference attribute (not empty or blank).",
      VgmChecks.atLeastOneVgmDeclarationWithEquipmentReferenceCheck().description());
  }

  @Test
  void shouldUseDocumentedDescriptionForShipmentDetailsValidation() {
    assertEquals(
      "At least one VGM Declaration must demonstrate the correct use of the shipmentDetails object.",
      VgmChecks.atLeastOneVgmDeclarationWithShipmentDetailsCheck().description());
  }

  @Test
  void shouldUseDocumentedDescriptionForShipmentReferenceValidation() {
    assertEquals(
      "The shipmentDetails object within at least one VGM Declaration must demonstrate the correct use of the carrierBookingReference or transportDocumentReference attribute (not empty or blank).",
      VgmChecks.atLeastOneVgmDeclarationWithShipmentReferenceCheck().description());
  }

  @Test
  void shouldUseDocumentedDescriptionForResponsiblePartyValidation() {
    assertEquals(
      "At least one VGM Declaration must demonstrate the correct use of the responsibleParty object.",
      VgmChecks.atLeastOneVgmDeclarationWithResponsiblePartyCheck().description());
  }

  @Test
  void shouldUseDocumentedDescriptionForResponsiblePartyNameValidation() {
    assertEquals(
      "The responsibleParty object within at least one VGM Declaration must demonstrate the correct use of the partyName or contactDetails.name attribute (not empty or blank).",
      VgmChecks.atLeastOneVgmDeclarationWithResponsiblePartyNameCheck().description());
  }

  @Test
  void shouldUseDocumentedDescriptionForAuthorizedPersonSignatoryValidation() {
    assertEquals(
      "At least one VGM Declaration must demonstrate the correct use of the authorizedPersonSignatory attribute (not empty or blank).",
      VgmChecks.atLeastOneVgmDeclarationWithAuthorizedPersonSignatoryCheck().description());
  }

  @Test
  void shouldPassDeclarationInMessageValidationWhenOneDeclarationExists() {
    assertTrue(
      VgmChecks.atLeastOneVgmDeclarationInMessageCheck()
        .validate(bodyWithDeclaration())
        .isConformant());
  }

  @Test
  void shouldFailDeclarationInMessageValidationWhenArrayIsEmpty() {
    assertFalse(
      VgmChecks.atLeastOneVgmDeclarationInMessageCheck()
        .validate(emptyBody())
        .isConformant());
  }

  @Test
  void shouldPassVgmObjectValidationWhenNonEmptyObjectExists() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithVgmObjectCheck(),
      declaration -> declaration.putObject("VGM").put("method", "SM1"));
  }

  @Test
  void shouldFailVgmObjectValidationWhenObjectIsMissing() {
    assertNonConformant(VgmChecks.atLeastOneVgmDeclarationWithVgmObjectCheck(), declaration -> {
    });
  }

  @Test
  void shouldFailVgmObjectValidationWhenObjectIsEmpty() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithVgmObjectCheck(),
      declaration -> declaration.putObject("VGM"));
  }

  @Test
  void shouldFailVgmObjectValidationWhenDeclarationsIsNotArray() {
    ObjectNode body = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    body.putObject("VGMDeclarations");

    assertFalse(
      VgmChecks.atLeastOneVgmDeclarationWithVgmObjectCheck().validate(body).isConformant());
  }

  @Test
  void shouldPassVgmObjectValidationWhenSecondDeclarationMatches() {
    ObjectNode body = bodyWithDeclaration();
    body.withArray("VGMDeclarations").addObject().putObject("VGM").put("method", "SM1");

    assertTrue(
      VgmChecks.atLeastOneVgmDeclarationWithVgmObjectCheck().validate(body).isConformant());
  }

  @Test
  void shouldPassWeightObjectValidationWhenNonEmptyObjectExists() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithWeightObjectCheck(),
      declaration -> declaration.putObject("VGM").putObject("weight").put("value", 1));
  }

  @Test
  void shouldFailWeightObjectValidationWhenObjectIsMissing() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithWeightObjectCheck(),
      declaration -> declaration.putObject("VGM"));
  }

  @Test
  void shouldFailWeightObjectValidationWhenObjectIsEmpty() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithWeightObjectCheck(),
      declaration -> declaration.putObject("VGM").putObject("weight"));
  }

  @Test
  void shouldPassWeightValueValidationWhenValueIsPositiveNumber() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithPositiveWeightValueCheck(),
      declaration -> declaration.putObject("VGM").putObject("weight").put("value", 1));
  }

  @Test
  void shouldFailWeightValueValidationWhenValueIsZero() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithPositiveWeightValueCheck(),
      declaration -> declaration.putObject("VGM").putObject("weight").put("value", 0));
  }

  @Test
  void shouldFailWeightValueValidationWhenValueIsNotNumber() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithPositiveWeightValueCheck(),
      declaration -> declaration.putObject("VGM").putObject("weight").put("value", "1"));
  }

  @Test
  void shouldPassWeightUnitValidationWhenUnitIsKgm() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithValidWeightUnitCheck(),
      declaration -> declaration.putObject("VGM").putObject("weight").put("unit", "KGM"));
  }

  @Test
  void shouldPassWeightUnitValidationWhenUnitIsLbr() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithValidWeightUnitCheck(),
      declaration -> declaration.putObject("VGM").putObject("weight").put("unit", "LBR"));
  }

  @Test
  void shouldFailWeightUnitValidationWhenUnitIsUnsupported() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithValidWeightUnitCheck(),
      declaration -> declaration.putObject("VGM").putObject("weight").put("unit", "TNE"));
  }

  @Test
  void shouldPassMethodValidationWhenMethodIsSm1() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithValidMethodCheck(),
      declaration -> declaration.putObject("VGM").put("method", "SM1"));
  }

  @Test
  void shouldPassMethodValidationWhenMethodIsSm2() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithValidMethodCheck(),
      declaration -> declaration.putObject("VGM").put("method", "SM2"));
  }

  @Test
  void shouldFailMethodValidationWhenMethodIsUnsupported() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithValidMethodCheck(),
      declaration -> declaration.putObject("VGM").put("method", "SM3"));
  }

  @Test
  void shouldPassEquipmentDetailsValidationWhenNonEmptyObjectExists() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithEquipmentDetailsCheck(),
      declaration -> declaration.putObject("equipmentDetails").put("equipmentReference", "MSCU1"));
  }

  @Test
  void shouldFailEquipmentDetailsValidationWhenObjectIsMissing() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithEquipmentDetailsCheck(), declaration -> {
      });
  }

  @Test
  void shouldFailEquipmentDetailsValidationWhenObjectIsEmpty() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithEquipmentDetailsCheck(),
      declaration -> declaration.putObject("equipmentDetails"));
  }

  @Test
  void shouldPassEquipmentReferenceValidationWhenReferenceIsNotBlank() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithEquipmentReferenceCheck(),
      declaration -> declaration.putObject("equipmentDetails").put("equipmentReference", "MSCU1"));
  }

  @Test
  void shouldFailEquipmentReferenceValidationWhenReferenceIsBlank() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithEquipmentReferenceCheck(),
      declaration -> declaration.putObject("equipmentDetails").put("equipmentReference", " "));
  }

  @Test
  void shouldFailEquipmentReferenceValidationWhenReferenceIsNotText() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithEquipmentReferenceCheck(),
      declaration -> declaration.putObject("equipmentDetails").put("equipmentReference", 1));
  }

  @Test
  void shouldPassShipmentDetailsValidationWhenNonEmptyObjectExists() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithShipmentDetailsCheck(),
      declaration -> declaration.putObject("shipmentDetails").put("carrierBookingReference", "CBR1"));
  }

  @Test
  void shouldFailShipmentDetailsValidationWhenObjectIsMissing() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithShipmentDetailsCheck(), declaration -> {
      });
  }

  @Test
  void shouldFailShipmentDetailsValidationWhenObjectIsEmpty() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithShipmentDetailsCheck(),
      declaration -> declaration.putObject("shipmentDetails"));
  }

  @Test
  void shouldPassShipmentReferenceValidationWhenCarrierBookingReferenceExists() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithShipmentReferenceCheck(),
      declaration -> declaration.putObject("shipmentDetails").put("carrierBookingReference", "CBR1"));
  }

  @Test
  void shouldPassShipmentReferenceValidationWhenTransportDocumentReferenceExists() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithShipmentReferenceCheck(),
      declaration -> declaration.putObject("shipmentDetails").put("transportDocumentReference", "TDR1"));
  }

  @Test
  void shouldFailShipmentReferenceValidationWhenBothReferencesAreBlank() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithShipmentReferenceCheck(),
      declaration -> {
        ObjectNode details = declaration.putObject("shipmentDetails");
        details.put("carrierBookingReference", " ");
        details.put("transportDocumentReference", " ");
      });
  }

  @Test
  void shouldPassResponsiblePartyValidationWhenNonEmptyObjectExists() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithResponsiblePartyCheck(),
      declaration -> declaration.putObject("responsibleParty").put("partyName", "Party"));
  }

  @Test
  void shouldFailResponsiblePartyValidationWhenObjectIsMissing() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithResponsiblePartyCheck(), declaration -> {
      });
  }

  @Test
  void shouldFailResponsiblePartyValidationWhenObjectIsEmpty() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithResponsiblePartyCheck(),
      declaration -> declaration.putObject("responsibleParty"));
  }

  @Test
  void shouldPassResponsiblePartyNameValidationWhenPartyNameExists() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithResponsiblePartyNameCheck(),
      declaration -> declaration.putObject("responsibleParty").put("partyName", "Party"));
  }

  @Test
  void shouldPassResponsiblePartyNameValidationWhenContactNameExists() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithResponsiblePartyNameCheck(),
      declaration -> declaration.putObject("responsibleParty").putObject("contactDetails").put("name", "Name"));
  }

  @Test
  void shouldFailResponsiblePartyNameValidationWhenBothNamesAreBlank() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithResponsiblePartyNameCheck(),
      declaration -> {
        ObjectNode party = declaration.putObject("responsibleParty");
        party.put("partyName", " ");
        party.putObject("contactDetails").put("name", " ");
      });
  }

  @Test
  void shouldPassAuthorizedPersonSignatoryValidationWhenValueIsNotBlank() {
    assertConformant(
      VgmChecks.atLeastOneVgmDeclarationWithAuthorizedPersonSignatoryCheck(),
      declaration -> declaration.put("authorizedPersonSignatory", "Jane Doe"));
  }

  @Test
  void shouldFailAuthorizedPersonSignatoryValidationWhenValueIsBlank() {
    assertNonConformant(
      VgmChecks.atLeastOneVgmDeclarationWithAuthorizedPersonSignatoryCheck(),
      declaration -> declaration.put("authorizedPersonSignatory", " "));
  }

  private static ObjectNode emptyBody() {
    ObjectNode body = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    body.putArray("VGMDeclarations");
    return body;
  }

  private static ObjectNode bodyWithDeclaration() {
    ObjectNode body = emptyBody();
    body.withArray("VGMDeclarations").addObject();
    return body;
  }

  private static void assertConformant(
    JsonContentCheck check, Consumer<ObjectNode> declarationCustomizer) {
    ObjectNode body = bodyWithDeclaration();
    declarationCustomizer.accept((ObjectNode) body.withArray("VGMDeclarations").get(0));
    assertTrue(check.validate(body).isConformant());
  }

  private static void assertNonConformant(
    JsonContentCheck check, Consumer<ObjectNode> declarationCustomizer) {
    ObjectNode body = bodyWithDeclaration();
    declarationCustomizer.accept((ObjectNode) body.withArray("VGMDeclarations").get(0));
    assertFalse(check.validate(body).isConformant());
  }
}
