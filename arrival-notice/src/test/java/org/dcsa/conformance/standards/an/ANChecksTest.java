package org.dcsa.conformance.standards.an;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.standards.an.checks.ANChecks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ANChecksTest {

  private ObjectMapper mapper;
  private ObjectNode body;
  private ArrayNode arrivalNotices;
  private ObjectNode an;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
    body = mapper.createObjectNode();
    arrivalNotices = body.putArray("arrivalNotices");
    an = arrivalNotices.addObject();
  }

  @Test
  void testValidateNonEmptyResponse() {
    an.put("carrierCode", "MAEU");
    assertTrue(ANChecks.VALIDATE_NON_EMPTY_RESPONSE.validate(body).isEmpty());

    ObjectNode empty = mapper.createObjectNode();
    assertFalse(ANChecks.VALIDATE_NON_EMPTY_RESPONSE.validate(empty).isEmpty());
  }

  @Test
  void testValidateBasicFields() {
    List<JsonContentCheck> checks = ANChecks.validateBasicFields();

    assertFalse(checks.stream().allMatch(c -> c.validate(body).isEmpty()));

    an.put("carrierCode", "MAEU");
    an.put("carrierCodeListProvider", "SMDG");
    an.put("deliveryTypeAtDestination", "CY");
    assertTrue(checks.stream().allMatch(c -> c.validate(body).isEmpty()));
  }

  @Test
  void testValidateCarrierContactInformation() {

    ArrayNode carrierContactInfos = an.putArray("carrierContactInformation");
    ObjectNode contactInfo = carrierContactInfos.addObject();
    contactInfo.put("name", "Ops Desk");
    assertFalse(ANChecks.validateCarrierContactInformation().validate(body).isEmpty());

    contactInfo.put("email", "ops@example.com");
    assertTrue(ANChecks.validateCarrierContactInformation().validate(body).isEmpty());
  }

  @Test
  void testValidateDocumentParties() {

    ArrayNode parties = an.putArray("documentParties");
    ObjectNode p = parties.addObject();
    p.put("partyFunction", "CN");
    p.put("partyName", "Consignee LLC");
    p.put("partyContactDetails", "consignee@example.com");
    ObjectNode addr = p.putObject("address");
    addr.put("street", "Harbor Rd 1");

    assertTrue(ANChecks.validateDocumentParties().validate(body).isEmpty());

    p.remove("partyFunction");
    assertFalse(ANChecks.validateDocumentParties().validate(body).isEmpty());
  }

  @Test
  void testValidateTransport() {
    assertFalse(ANChecks.validateTransport().validate(body).isEmpty());

    ObjectNode transport = an.putObject("transport");
    transport.putObject("portOfDischargeArrivalDate").put("value", "2025-10-01T10:00:00Z");

    ObjectNode pod = transport.putObject("portOfDischarge");
    pod.put("UNLocationCode", "NLRTM");

    ArrayNode legs = transport.putArray("legs");
    ObjectNode leg = legs.addObject();
    ObjectNode voyage = leg.putObject("vesselVoyage");
    voyage.put("vesselName", "MSC Example");
    voyage.put("carrierImportVoyageNumber", "0123W");

    assertTrue(ANChecks.validateTransport().validate(body).isEmpty());

    voyage.remove("vesselName");
    assertFalse(ANChecks.validateTransport().validate(body).isEmpty());
  }

  @Test
  void testValidateUtilizedTransportEquipments() {

    ArrayNode utes = an.putArray("utilizedTransportEquipments");
    ObjectNode ute = utes.addObject();

    ObjectNode eq = ute.putObject("equipment");
    eq.put("equipmentReference", "MSCU1234567");
    eq.put("ISOEquipmentCode", "22G1");

    ArrayNode seals = ute.putArray("seals");
    ObjectNode seal = seals.addObject();
    seal.put("number", "ABC123");

    assertTrue(ANChecks.validateUtilizedTransportEquipments().validate(body).isEmpty());

    ute.set("seals", mapper.createArrayNode());
    assertFalse(ANChecks.validateUtilizedTransportEquipments().validate(body).isEmpty());

    ute.remove("equipment");
    assertFalse(ANChecks.validateUtilizedTransportEquipments().validate(body).isEmpty());
  }

  @Test
  void testValidateConsignmentItems() {

    ArrayNode consignmentItems = an.putArray("consignmentItems");
    ObjectNode ci = consignmentItems.addObject();
    assertFalse(ANChecks.validateConsignmentItems().validate(body).isEmpty());

    ArrayNode dog = ci.putArray("descriptionOfGoods");
    dog.add("Widgets");

    ArrayNode cargoItems = ci.putArray("cargoItems");
    ObjectNode item = cargoItems.addObject();
    item.put("equipmentReference", "MSCU1234567");
    ObjectNode cgw = item.putObject("cargoGrossWeight");
    cgw.put("value", 1234.5);
    cgw.put("unit", "KGM");

    ObjectNode op = item.putObject("outerPackaging");
    op.put("packageCode", "CT");
    op.put("numberOfPackages", 10);

    assertTrue(ANChecks.validateConsignmentItems().validate(body).isEmpty());

    op.remove("packageCode");
    op.remove("IMOPackagingCode");
    op.remove("description");
    assertFalse(ANChecks.validateConsignmentItems().validate(body).isEmpty());
  }

  @Test
  void testValidateFreeTimeObjectStructure() {

    ArrayNode freeTimes = an.putArray("freeTimes");
    ObjectNode ft = freeTimes.addObject();
    ft.putArray("typeCodes").add("DEM");
    ft.putArray("ISOEquipmentCodes").add("22G1");
    ft.putArray("equipmentReferences").add("MSCU1234567");
    ft.put("duration", 5);
    ft.put("timeUnit", "DAY");

    assertTrue(ANChecks.validateFreeTimeObjectStructure("FREE_TIME").validate(body).isEmpty());


    an.set("freeTimes", mapper.createArrayNode());
    assertFalse(ANChecks.validateFreeTimeObjectStructure("FREE_TIME").validate(body).isEmpty());
  }

  @Test
  void testValidateChargesStructure() {
    ArrayNode charges = an.putArray("charges");
    ObjectNode ch = charges.addObject();
    ch.put("chargeName", "Arrival Notice Fee");
    ch.put("currencyAmount", 50.0);
    ch.put("currencyCode", "EUR");
    ch.put("paymentTermCode", "PRE");
    ch.put("unitPrice", 50.0);
    ch.put("quantity", 1);

    assertTrue(ANChecks.validateChargesStructure().validate(body).isEmpty());


    ch.remove("currencyCode");
    assertFalse(ANChecks.validateChargesStructure().validate(body).isEmpty());
  }

  @Test
  void testGetFreeTimeScenarioRelatedChecks_FREE_TIME() {

    var checks = ANChecks.getScenarioRelatedChecks("FREE_TIME");
    assertFalse(checks.isEmpty());

  }

  @Test
  void testGetFreightedScenarioRelatedChecks() {

    var checks = ANChecks.getScenarioRelatedChecks("FREIGHTED");
    assertFalse(checks.isEmpty());
  }

  @Test
  void testValidatePartyContactDetailsName() {

    ArrayNode documentParties = an.putArray("documentParties");
    ObjectNode documentParty = documentParties.addObject();
    ArrayNode partyContactDetails = documentParty.putArray("partyContactDetails");
    ObjectNode partyContactDetail = partyContactDetails.addObject();
    assertFalse(ANChecks.validatePartyContactName().validate(body).isEmpty());
    partyContactDetail.put("name", "Ops Desk");
    assertTrue(ANChecks.validatePartyContactName().validate(body).isEmpty());

  }

  @Test
  void testValidatePartyContactDetailsEmailOrPhone() {

    ArrayNode documentParties = an.putArray("documentParties");
    ObjectNode documentParty = documentParties.addObject();
    ArrayNode partyContactDetails = documentParty.putArray("partyContactDetails");
    ObjectNode partyContactDetail = partyContactDetails.addObject();

    assertFalse(ANChecks.validatePartyContactEmailOrPhone().validate(body).isEmpty());

    partyContactDetail.put("email", "ops@example.com");
    assertTrue(ANChecks.validatePartyContactEmailOrPhone().validate(body).isEmpty());
  }

  @Test
  void testValidatePortOfDischargeFields() {

    ObjectNode transport = an.putObject("transport");
    ObjectNode pod = transport.putObject("portOfDischarge");
    ObjectNode facility = pod.putObject("facility");

    assertFalse(
        ANChecks.validatePortOfDischargeFacilityFields(
                "facilityCode", "arrivalNotices.*.transport.portOfDischarge")
            .validate(body)
            .isEmpty());

    facility.put("facilityCode", "NLRTM");
    assertTrue(
        ANChecks.validatePortOfDischargeFacilityFields(
                "facilityCode", "arrivalNotices.*.transport.portOfDischarge")
            .validate(body)
            .isEmpty());

    facility.put("facilityCodeListProvider", "SMDG");
    assertTrue(
        ANChecks.validatePortOfDischargeFacilityFields(
                "facilityCodeListProvider", "arrivalNotices.*.transport.portOfDischarge")
            .validate(body)
            .isEmpty());
  }
}

