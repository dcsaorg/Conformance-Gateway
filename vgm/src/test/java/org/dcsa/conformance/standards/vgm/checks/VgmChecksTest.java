package org.dcsa.conformance.standards.vgm.checks;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class VgmChecksTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Nested
  class AtLeastOneVgmDeclarationInMessageCheckTest {

    private final JsonContentCheck check = VgmChecks.atLeastOneVgmDeclarationInMessageCheck();

    @Test
    void testValidMessage_WithOneVgmDeclaration() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject();

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(result.isConformant(), "Message with one VGM declaration should pass");
    }

    @Test
    void testValidMessage_WithMultipleVgmDeclarations() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject();
      declarations.addObject();
      declarations.addObject();

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(result.isConformant(), "Message with multiple VGM declarations should pass");
    }

    @Test
    void testInvalidMessage_WithEmptyVgmDeclarationsArray() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();
      body.putArray("VGMDeclarations");

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Message with empty VGMDeclarations array should fail");
    }

    @Test
    void testInvalidMessage_WithMissingVgmDeclarationsArray() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Message without VGMDeclarations array should fail");
    }

    @Test
    void testInvalidMessage_WithNullVgmDeclarationsArray() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();
      body.putNull("VGMDeclarations");

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Message with null VGMDeclarations should fail");
    }

    @Test
    void testInvalidMessage_WithVgmDeclarationsAsObject() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();
      body.putObject("VGMDeclarations");

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(
          result.isConformant(),
          "Message with VGMDeclarations as object instead of array should fail");
    }

    @Test
    void testInvalidMessage_WithVgmDeclarationsAsString() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();
      body.put("VGMDeclarations", "not-an-array");

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Message with VGMDeclarations as string should fail");
    }
  }

  @Nested
  class AtLeastOneVgmDeclarationWithVgmObjectCheckTest {

    private final JsonContentCheck check = VgmChecks.atLeastOneVgmDeclarationWithVgmObjectCheck();

    @Test
    void testValidVgmDeclaration_WithAllRequiredFields() {
      // Arrange
      JsonNode body = createVgmDeclaration(createValidVgm());

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(result.isConformant(), "Valid VGM declaration should pass");
    }

    @Test
    void testValidVgmDeclaration_WithKGMUnit() {
      // Arrange
      JsonNode body = createVgmDeclaration(createVgmWithUnit("KGM"));

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(result.isConformant(), "VGM with KGM unit should pass");
    }

    @Test
    void testValidVgmDeclaration_WithLBRUnit() {
      // Arrange
      JsonNode body = createVgmDeclaration(createVgmWithUnit("LBR"));

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(result.isConformant(), "VGM with LBR unit should pass");
    }

    @Test
    void testValidVgmDeclaration_WithSM1Method() {
      // Arrange
      JsonNode body = createVgmDeclaration(createVgmWithMethod("SM1"));

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(result.isConformant(), "VGM with SM1 method should pass");
    }

    @Test
    void testValidVgmDeclaration_WithSM2Method() {
      // Arrange
      JsonNode body = createVgmDeclaration(createVgmWithMethod("SM2"));

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(result.isConformant(), "VGM with SM2 method should pass");
    }

    @Test
    void testInvalidVgmDeclaration_MissingVgmObject() {
      // Arrange
      JsonNode body = createVgmDeclaration(null);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Missing VGM object should fail");
    }

    @Test
    void testInvalidVgmDeclaration_MissingWeightObject() {
      // Arrange
      ObjectNode vgm = MAPPER.createObjectNode();
      vgm.put("method", "SM1");
      JsonNode body = createVgmDeclaration(vgm);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Missing weight object should fail");
    }

    @Test
    void testInvalidVgmDeclaration_MissingWeightValue() {
      // Arrange
      ObjectNode weight = MAPPER.createObjectNode();
      weight.put("unit", "KGM");
      ObjectNode vgm = MAPPER.createObjectNode();
      vgm.set("weight", weight);
      vgm.put("method", "SM1");
      JsonNode body = createVgmDeclaration(vgm);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Missing weight.value should fail");
    }

    @Test
    void testInvalidVgmDeclaration_WeightValueNotANumber() {
      // Arrange
      ObjectNode weight = MAPPER.createObjectNode();
      weight.put("value", "not-a-number");
      weight.put("unit", "KGM");
      ObjectNode vgm = MAPPER.createObjectNode();
      vgm.set("weight", weight);
      vgm.put("method", "SM1");
      JsonNode body = createVgmDeclaration(vgm);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Non-numeric weight.value should fail");
    }

    @Test
    void testInvalidVgmDeclaration_WeightValueZero() {
      // Arrange
      ObjectNode weight = MAPPER.createObjectNode();
      weight.put("value", 0);
      weight.put("unit", "KGM");
      ObjectNode vgm = MAPPER.createObjectNode();
      vgm.set("weight", weight);
      vgm.put("method", "SM1");
      JsonNode body = createVgmDeclaration(vgm);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Zero weight.value should fail");
    }

    @Test
    void testInvalidVgmDeclaration_WeightValueNegative() {
      // Arrange
      ObjectNode weight = MAPPER.createObjectNode();
      weight.put("value", -10.5);
      weight.put("unit", "KGM");
      ObjectNode vgm = MAPPER.createObjectNode();
      vgm.set("weight", weight);
      vgm.put("method", "SM1");
      JsonNode body = createVgmDeclaration(vgm);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Negative weight.value should fail");
    }

    @Test
    void testInvalidVgmDeclaration_MissingWeightUnit() {
      // Arrange
      ObjectNode weight = MAPPER.createObjectNode();
      weight.put("value", 100.5);
      ObjectNode vgm = MAPPER.createObjectNode();
      vgm.set("weight", weight);
      vgm.put("method", "SM1");
      JsonNode body = createVgmDeclaration(vgm);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Missing weight.unit should fail");
    }

    @Test
    void testInvalidVgmDeclaration_InvalidWeightUnit() {
      // Arrange
      ObjectNode weight = MAPPER.createObjectNode();
      weight.put("value", 100.5);
      weight.put("unit", "TON");
      ObjectNode vgm = MAPPER.createObjectNode();
      vgm.set("weight", weight);
      vgm.put("method", "SM1");
      JsonNode body = createVgmDeclaration(vgm);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Invalid weight.unit (TON) should fail");
    }

    @Test
    void testInvalidVgmDeclaration_MissingMethod() {
      // Arrange
      ObjectNode weight = MAPPER.createObjectNode();
      weight.put("value", 100.5);
      weight.put("unit", "KGM");
      ObjectNode vgm = MAPPER.createObjectNode();
      vgm.set("weight", weight);
      JsonNode body = createVgmDeclaration(vgm);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Missing method should fail");
    }

    @Test
    void testInvalidVgmDeclaration_InvalidMethod() {
      // Arrange
      ObjectNode weight = MAPPER.createObjectNode();
      weight.put("value", 100.5);
      weight.put("unit", "KGM");
      ObjectNode vgm = MAPPER.createObjectNode();
      vgm.set("weight", weight);
      vgm.put("method", "SM3");
      JsonNode body = createVgmDeclaration(vgm);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Invalid method (SM3) should fail");
    }

    @Test
    void testValidVgmDeclaration_MultipleDeclarationsWithOneValid() {
      // Arrange - First declaration invalid, second valid
      ObjectNode invalidVgm = MAPPER.createObjectNode();
      invalidVgm.put("method", "INVALID");

      ObjectNode validVgm = createValidVgm();

      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().set("VGM", invalidVgm);
      declarations.addObject().set("VGM", validVgm);

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(
          result.isConformant(), "At least one valid VGM declaration should make the check pass");
    }

    @Test
    void testInvalidVgmDeclaration_MultipleDeclarationsAllInvalid() {
      // Arrange - Both declarations invalid
      ObjectNode invalidVgm1 = MAPPER.createObjectNode();
      invalidVgm1.put("method", "INVALID");

      ObjectNode invalidVgm2 = MAPPER.createObjectNode();
      ObjectNode weight = MAPPER.createObjectNode();
      weight.put("value", -1);
      weight.put("unit", "KGM");
      invalidVgm2.set("weight", weight);
      invalidVgm2.put("method", "SM1");

      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().set("VGM", invalidVgm1);
      declarations.addObject().set("VGM", invalidVgm2);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "All invalid VGM declarations should fail");
    }

    // Helper methods
    private ObjectNode createValidVgm() {
      ObjectNode weight = MAPPER.createObjectNode();
      weight.put("value", 100.5);
      weight.put("unit", "KGM");

      ObjectNode vgm = MAPPER.createObjectNode();
      vgm.set("weight", weight);
      vgm.put("method", "SM1");
      return vgm;
    }

    private ObjectNode createVgmWithUnit(String unit) {
      ObjectNode weight = MAPPER.createObjectNode();
      weight.put("value", 100.5);
      weight.put("unit", unit);

      ObjectNode vgm = MAPPER.createObjectNode();
      vgm.set("weight", weight);
      vgm.put("method", "SM1");
      return vgm;
    }

    private ObjectNode createVgmWithMethod(String method) {
      ObjectNode weight = MAPPER.createObjectNode();
      weight.put("value", 100.5);
      weight.put("unit", "KGM");

      ObjectNode vgm = MAPPER.createObjectNode();
      vgm.set("weight", weight);
      vgm.put("method", method);
      return vgm;
    }

    private ObjectNode createVgmDeclaration(ObjectNode vgm) {
      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      ObjectNode declaration = declarations.addObject();
      if (vgm != null) {
        declaration.set("VGM", vgm);
      }
      return body;
    }
  }

  @Nested
  class AtLeastOneVgmDeclarationWithEquipmentDetailsCheckTest {

    private final JsonContentCheck check =
        VgmChecks.atLeastOneVgmDeclarationWithEquipmentDetailsCheck();

    @Test
    void testValidEquipmentDetails_WithEquipmentReference() {
      // Arrange
      ObjectNode equipmentDetails = MAPPER.createObjectNode();
      equipmentDetails.put("equipmentReference", "APZU4812090");
      JsonNode body = createVgmDeclarationWithEquipmentDetails(equipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(result.isConformant(), "Valid equipment details with reference should pass");
    }

    @Test
    void testInvalidEquipmentDetails_MissingEquipmentDetailsObject() {
      // Arrange
      JsonNode body = createVgmDeclarationWithEquipmentDetails(null);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Missing equipmentDetails object should fail");
    }

    @Test
    void testInvalidEquipmentDetails_MissingEquipmentReference() {
      // Arrange
      ObjectNode equipmentDetails = MAPPER.createObjectNode();
      equipmentDetails.put("ISOEquipmentCode", "22GP");
      JsonNode body = createVgmDeclarationWithEquipmentDetails(equipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Missing equipmentReference should fail");
    }

    @Test
    void testInvalidEquipmentDetails_EmptyEquipmentReference() {
      // Arrange
      ObjectNode equipmentDetails = MAPPER.createObjectNode();
      equipmentDetails.put("equipmentReference", "");
      JsonNode body = createVgmDeclarationWithEquipmentDetails(equipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Empty equipmentReference should fail");
    }

    @Test
    void testInvalidEquipmentDetails_BlankEquipmentReference() {
      // Arrange
      ObjectNode equipmentDetails = MAPPER.createObjectNode();
      equipmentDetails.put("equipmentReference", "   ");
      JsonNode body = createVgmDeclarationWithEquipmentDetails(equipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Blank equipmentReference should fail");
    }

    @Test
    void testInvalidEquipmentDetails_NullEquipmentReference() {
      // Arrange
      ObjectNode equipmentDetails = MAPPER.createObjectNode();
      equipmentDetails.putNull("equipmentReference");
      JsonNode body = createVgmDeclarationWithEquipmentDetails(equipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Null equipmentReference should fail");
    }

    @Test
    void testValidEquipmentDetails_MultipleDeclarationsWithOneValid() {
      // Arrange - First declaration invalid, second valid
      ObjectNode invalidEquipmentDetails = MAPPER.createObjectNode();
      invalidEquipmentDetails.put("equipmentReference", "");

      ObjectNode validEquipmentDetails = MAPPER.createObjectNode();
      validEquipmentDetails.put("equipmentReference", "APZU4812090");

      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().set("equipmentDetails", invalidEquipmentDetails);
      declarations.addObject().set("equipmentDetails", validEquipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(
          result.isConformant(), "At least one valid equipmentDetails should make the check pass");
    }

    @Test
    void testInvalidEquipmentDetails_MultipleDeclarationsAllInvalid() {
      // Arrange - All declarations invalid
      ObjectNode invalidEquipmentDetails1 = MAPPER.createObjectNode();
      invalidEquipmentDetails1.put("equipmentReference", "");

      ObjectNode invalidEquipmentDetails2 = MAPPER.createObjectNode();
      invalidEquipmentDetails2.put("equipmentReference", "   ");

      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().set("equipmentDetails", invalidEquipmentDetails1);
      declarations.addObject().set("equipmentDetails", invalidEquipmentDetails2);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "All invalid equipmentDetails should fail");
    }

    // Helper methods
    private ObjectNode createVgmDeclarationWithEquipmentDetails(ObjectNode equipmentDetails) {
      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      ObjectNode declaration = declarations.addObject();
      if (equipmentDetails != null) {
        declaration.set("equipmentDetails", equipmentDetails);
      }
      return body;
    }
  }

  @Nested
  class AtLeastOneVgmDeclarationWithShipmentDetailsCheckTest {

    private final JsonContentCheck check =
        VgmChecks.atLeastOneVgmDeclarationWithShipmentDetailsCheck();

    @Test
    void testValidShipmentDetails_WithCarrierBookingReference() {
      // Arrange
      ObjectNode shipmentDetails = MAPPER.createObjectNode();
      shipmentDetails.put("carrierBookingReference", "ABC709951");
      JsonNode body = createVgmDeclarationWithShipmentDetails(shipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(
          result.isConformant(), "Valid shipmentDetails with carrierBookingReference should pass");
    }

    @Test
    void testValidShipmentDetails_WithTransportDocumentReference() {
      // Arrange
      ObjectNode shipmentDetails = MAPPER.createObjectNode();
      shipmentDetails.put("transportDocumentReference", "HHL71800000");
      JsonNode body = createVgmDeclarationWithShipmentDetails(shipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(
          result.isConformant(),
          "Valid shipmentDetails with transportDocumentReference should pass");
    }

    @Test
    void testValidShipmentDetails_WithBothReferences() {
      // Arrange
      ObjectNode shipmentDetails = MAPPER.createObjectNode();
      shipmentDetails.put("carrierBookingReference", "ABC709951");
      shipmentDetails.put("transportDocumentReference", "HHL71800000");
      JsonNode body = createVgmDeclarationWithShipmentDetails(shipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(result.isConformant(), "Valid shipmentDetails with both references should pass");
    }

    @Test
    void testInvalidShipmentDetails_MissingShipmentDetailsObject() {
      // Arrange
      JsonNode body = createVgmDeclarationWithShipmentDetails(null);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Missing shipmentDetails object should fail");
    }

    @Test
    void testInvalidShipmentDetails_MissingBothReferences() {
      // Arrange
      ObjectNode shipmentDetails = MAPPER.createObjectNode();
      JsonNode body = createVgmDeclarationWithShipmentDetails(shipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Missing both references should fail");
    }

    @Test
    void testInvalidShipmentDetails_EmptyCarrierBookingReference() {
      // Arrange
      ObjectNode shipmentDetails = MAPPER.createObjectNode();
      shipmentDetails.put("carrierBookingReference", "");
      JsonNode body = createVgmDeclarationWithShipmentDetails(shipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Empty carrierBookingReference should fail");
    }

    @Test
    void testInvalidShipmentDetails_BlankCarrierBookingReference() {
      // Arrange
      ObjectNode shipmentDetails = MAPPER.createObjectNode();
      shipmentDetails.put("carrierBookingReference", "   ");
      JsonNode body = createVgmDeclarationWithShipmentDetails(shipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Blank carrierBookingReference should fail");
    }

    @Test
    void testInvalidShipmentDetails_EmptyTransportDocumentReference() {
      // Arrange
      ObjectNode shipmentDetails = MAPPER.createObjectNode();
      shipmentDetails.put("transportDocumentReference", "");
      JsonNode body = createVgmDeclarationWithShipmentDetails(shipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Empty transportDocumentReference should fail");
    }

    @Test
    void testInvalidShipmentDetails_BlankTransportDocumentReference() {
      // Arrange
      ObjectNode shipmentDetails = MAPPER.createObjectNode();
      shipmentDetails.put("transportDocumentReference", "   ");
      JsonNode body = createVgmDeclarationWithShipmentDetails(shipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Blank transportDocumentReference should fail");
    }

    @Test
    void testValidShipmentDetails_MultipleDeclarationsWithOneValid() {
      // Arrange - First declaration invalid, second valid
      ObjectNode invalidShipmentDetails = MAPPER.createObjectNode();
      invalidShipmentDetails.put("carrierBookingReference", "");
      invalidShipmentDetails.put("transportDocumentReference", "");

      ObjectNode validShipmentDetails = MAPPER.createObjectNode();
      validShipmentDetails.put("carrierBookingReference", "ABC709951");

      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().set("shipmentDetails", invalidShipmentDetails);
      declarations.addObject().set("shipmentDetails", validShipmentDetails);

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(
          result.isConformant(), "At least one valid shipmentDetails should make the check pass");
    }

    @Test
    void testInvalidShipmentDetails_MultipleDeclarationsAllInvalid() {
      // Arrange - All declarations invalid
      ObjectNode invalidShipmentDetails1 = MAPPER.createObjectNode();
      invalidShipmentDetails1.put("carrierBookingReference", "");

      ObjectNode invalidShipmentDetails2 = MAPPER.createObjectNode();
      invalidShipmentDetails2.put("transportDocumentReference", "   ");

      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().set("shipmentDetails", invalidShipmentDetails1);
      declarations.addObject().set("shipmentDetails", invalidShipmentDetails2);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "All invalid shipmentDetails should fail");
    }

    // Helper methods
    private ObjectNode createVgmDeclarationWithShipmentDetails(ObjectNode shipmentDetails) {
      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      ObjectNode declaration = declarations.addObject();
      if (shipmentDetails != null) {
        declaration.set("shipmentDetails", shipmentDetails);
      }
      return body;
    }
  }

  @Nested
  class AtLeastOneVgmDeclarationWithResponsiblePartyCheckTest {

    private final JsonContentCheck check =
        VgmChecks.atLeastOneVgmDeclarationWithResponsiblePartyCheck();

    @Test
    void testValidResponsibleParty_WithPartyName() {
      // Arrange
      ObjectNode responsibleParty = MAPPER.createObjectNode();
      responsibleParty.put("partyName", "Acme Inc.");
      JsonNode body = createVgmDeclarationWithResponsibleParty(responsibleParty);

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(result.isConformant(), "Valid responsibleParty with partyName should pass");
    }

    @Test
    void testValidResponsibleParty_WithContactDetailsName() {
      // Arrange
      ObjectNode contactDetails = MAPPER.createObjectNode();
      contactDetails.put("name", "Jane Doe");
      ObjectNode responsibleParty = MAPPER.createObjectNode();
      responsibleParty.set("contactDetails", contactDetails);
      JsonNode body = createVgmDeclarationWithResponsibleParty(responsibleParty);

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(
          result.isConformant(), "Valid responsibleParty with contactDetails.name should pass");
    }

    @Test
    void testValidResponsibleParty_WithBothPartyNameAndContactDetailsName() {
      // Arrange
      ObjectNode contactDetails = MAPPER.createObjectNode();
      contactDetails.put("name", "Jane Doe");
      ObjectNode responsibleParty = MAPPER.createObjectNode();
      responsibleParty.put("partyName", "Acme Inc.");
      responsibleParty.set("contactDetails", contactDetails);
      JsonNode body = createVgmDeclarationWithResponsibleParty(responsibleParty);

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(
          result.isConformant(),
          "Valid responsibleParty with both partyName and contactDetails.name should pass");
    }

    @Test
    void testInvalidResponsibleParty_MissingResponsiblePartyObject() {
      // Arrange
      JsonNode body = createVgmDeclarationWithResponsibleParty(null);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Missing responsibleParty object should fail");
    }

    @Test
    void testInvalidResponsibleParty_MissingBothPartyNameAndContactDetailsName() {
      // Arrange
      ObjectNode responsibleParty = MAPPER.createObjectNode();
      JsonNode body = createVgmDeclarationWithResponsibleParty(responsibleParty);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(
          result.isConformant(), "Missing both partyName and contactDetails.name should fail");
    }

    @Test
    void testInvalidResponsibleParty_EmptyPartyName() {
      // Arrange
      ObjectNode responsibleParty = MAPPER.createObjectNode();
      responsibleParty.put("partyName", "");
      JsonNode body = createVgmDeclarationWithResponsibleParty(responsibleParty);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Empty partyName should fail");
    }

    @Test
    void testInvalidResponsibleParty_BlankPartyName() {
      // Arrange
      ObjectNode responsibleParty = MAPPER.createObjectNode();
      responsibleParty.put("partyName", "   ");
      JsonNode body = createVgmDeclarationWithResponsibleParty(responsibleParty);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Blank partyName should fail");
    }

    @Test
    void testInvalidResponsibleParty_EmptyContactDetailsName() {
      // Arrange
      ObjectNode contactDetails = MAPPER.createObjectNode();
      contactDetails.put("name", "");
      ObjectNode responsibleParty = MAPPER.createObjectNode();
      responsibleParty.set("contactDetails", contactDetails);
      JsonNode body = createVgmDeclarationWithResponsibleParty(responsibleParty);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Empty contactDetails.name should fail");
    }

    @Test
    void testInvalidResponsibleParty_BlankContactDetailsName() {
      // Arrange
      ObjectNode contactDetails = MAPPER.createObjectNode();
      contactDetails.put("name", "   ");
      ObjectNode responsibleParty = MAPPER.createObjectNode();
      responsibleParty.set("contactDetails", contactDetails);
      JsonNode body = createVgmDeclarationWithResponsibleParty(responsibleParty);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Blank contactDetails.name should fail");
    }

    @Test
    void testInvalidResponsibleParty_MissingContactDetailsObject() {
      // Arrange
      ObjectNode responsibleParty = MAPPER.createObjectNode();
      responsibleParty.put("partyName", "");
      JsonNode body = createVgmDeclarationWithResponsibleParty(responsibleParty);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Empty partyName with missing contactDetails should fail");
    }

    @Test
    void testValidResponsibleParty_MultipleDeclarationsWithOneValid() {
      // Arrange - First declaration invalid, second valid
      ObjectNode invalidResponsibleParty = MAPPER.createObjectNode();
      invalidResponsibleParty.put("partyName", "");

      ObjectNode validResponsibleParty = MAPPER.createObjectNode();
      validResponsibleParty.put("partyName", "Acme Inc.");

      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().set("responsibleParty", invalidResponsibleParty);
      declarations.addObject().set("responsibleParty", validResponsibleParty);

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(
          result.isConformant(), "At least one valid responsibleParty should make the check pass");
    }

    @Test
    void testInvalidResponsibleParty_MultipleDeclarationsAllInvalid() {
      // Arrange - All declarations invalid
      ObjectNode invalidResponsibleParty1 = MAPPER.createObjectNode();
      invalidResponsibleParty1.put("partyName", "");

      ObjectNode invalidResponsibleParty2 = MAPPER.createObjectNode();
      ObjectNode contactDetails = MAPPER.createObjectNode();
      contactDetails.put("name", "   ");
      invalidResponsibleParty2.set("contactDetails", contactDetails);

      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().set("responsibleParty", invalidResponsibleParty1);
      declarations.addObject().set("responsibleParty", invalidResponsibleParty2);

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "All invalid responsibleParty should fail");
    }

    // Helper methods
    private ObjectNode createVgmDeclarationWithResponsibleParty(ObjectNode responsibleParty) {
      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      ObjectNode declaration = declarations.addObject();
      if (responsibleParty != null) {
        declaration.set("responsibleParty", responsibleParty);
      }
      return body;
    }
  }

  @Nested
  class AtLeastOneVgmDeclarationWithAuthorizedPersonSignatoryCheckTest {

    private final JsonContentCheck check =
        VgmChecks.atLeastOneVgmDeclarationWithAuthorizedPersonSignatoryCheck();

    @Test
    void testValidAuthorizedPersonSignatory_WithValue() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().put("authorizedPersonSignatory", "Jane Doe");

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(result.isConformant(), "Valid authorizedPersonSignatory with value should pass");
    }

    @Test
    void testInvalidAuthorizedPersonSignatory_Missing() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject();

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Missing authorizedPersonSignatory should fail");
    }

    @Test
    void testInvalidAuthorizedPersonSignatory_Empty() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().put("authorizedPersonSignatory", "");

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Empty authorizedPersonSignatory should fail");
    }

    @Test
    void testInvalidAuthorizedPersonSignatory_Blank() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().put("authorizedPersonSignatory", "   ");

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Blank authorizedPersonSignatory should fail");
    }

    @Test
    void testInvalidAuthorizedPersonSignatory_Null() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().putNull("authorizedPersonSignatory");

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "Null authorizedPersonSignatory should fail");
    }

    @Test
    void testValidAuthorizedPersonSignatory_MultipleDeclarationsWithOneValid() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().put("authorizedPersonSignatory", "");
      declarations.addObject().put("authorizedPersonSignatory", "Jane Doe");
      declarations.addObject().put("authorizedPersonSignatory", "   ");

      // Act
      var result = check.validate(body);

      // Assert
      assertTrue(
          result.isConformant(),
          "At least one valid authorizedPersonSignatory should make the check pass");
    }

    @Test
    void testInvalidAuthorizedPersonSignatory_MultipleDeclarationsAllInvalid() {
      // Arrange
      ObjectNode body = MAPPER.createObjectNode();
      var declarations = body.putArray("VGMDeclarations");
      declarations.addObject().put("authorizedPersonSignatory", "");
      declarations.addObject().put("authorizedPersonSignatory", "   ");
      declarations.addObject();

      // Act
      var result = check.validate(body);

      // Assert
      assertFalse(result.isConformant(), "All invalid authorizedPersonSignatory should fail");
    }
  }
}
