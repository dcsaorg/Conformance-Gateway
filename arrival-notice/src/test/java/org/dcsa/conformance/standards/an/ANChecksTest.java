package org.dcsa.conformance.standards.an;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Set;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceError;
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
    assertTrue(ANChecks.VALIDATE_NON_EMPTY_RESPONSE.validate(body).getErrorMessages().isEmpty());

    ObjectNode empty = mapper.createObjectNode();
    assertFalse(ANChecks.VALIDATE_NON_EMPTY_RESPONSE.validate(empty).getErrorMessages().isEmpty());
  }

  @Test
  void testValidateBasicFields() {
    List<JsonContentCheck> checks = ANChecks.validateBasicFields();

    assertFalse(checks.stream().allMatch(c -> c.validate(body).getErrorMessages().isEmpty()));

    an.put("carrierCode", "MAEU");
    an.put("carrierCodeListProvider", "SMDG");
    an.put("deliveryTypeAtDestination", "CY");
    an.put("transportDocumentReference", "HHL123");
    assertTrue(checks.stream().allMatch(c -> c.validate(body).getErrorMessages().isEmpty()));
  }

  @Test
  void testValidateCarrierContactInformation() {

    ArrayNode carrierContactInfos = an.putArray("carrierContactInformation");
    ObjectNode contactInfo = carrierContactInfos.addObject();
    contactInfo.put("name", "Ops Desk");
    assertFalse(
        ANChecks.validateCarrierContactInformation().validate(body).getErrorMessages().isEmpty());

    contactInfo.put("email", "ops@example.com");
    assertTrue(
        ANChecks.validateCarrierContactInformation().validate(body).getErrorMessages().isEmpty());
  }

  @Test
  void testInvalidDocumentParties() {
    ArrayNode parties = an.putArray("documentParties");
    ObjectNode p = parties.addObject();
    p.put("partyName", "Consignee LLC");
    p.put("partyContactDetails", "consignee@example.com");
    ObjectNode addr = p.putObject("address");
    addr.put("street", "Harbor Rd 1");

    Set<ConformanceError> errors =((ConformanceCheckResult.ErrorsWithRelevance) ANChecks.validateDocumentParties().validate(body)).errors();
    assertEquals(1, errors.size());

    p.put("partyFunction", "CN");
    errors =
        ((ConformanceCheckResult.ErrorsWithRelevance)
                ANChecks.validateDocumentParties().validate(body))
            .errors();
    assertEquals(0, errors.size());

    p.remove("partyName");
    assertFalse(ANChecks.validateDocumentParties().validate(body).getErrorMessages().isEmpty());
  }

  @Test
  void testValidDocumentParties() {
    ArrayNode parties = an.putArray("documentParties");
    ObjectNode p = parties.addObject();
    p.put("partyName", "Consignee LLC");
    p.put("partyContactDetails", "consignee@example.com");
    ObjectNode addr = p.putObject("address");
    addr.put("street", "Harbor Rd 1");
    p.put("partyFunction", "CN");

    Set<ConformanceError> errors =
        ((ConformanceCheckResult.ErrorsWithRelevance)
                ANChecks.validateDocumentParties().validate(body))
            .errors();
    assertEquals(0, errors.size());
  }

  @Test
  void testValidateTransport() {
    assertFalse(ANChecks.validateTransport().validate(body).getErrorMessages().isEmpty());

    ObjectNode transport = an.putObject("transport");
    transport.putObject("portOfDischargeArrivalDate").put("value", "2025-10-01T10:00:00Z");

    ObjectNode pod = transport.putObject("portOfDischarge");
    pod.put("UNLocationCode", "NLRTM");

    ArrayNode legs = transport.putArray("legs");
    ObjectNode leg = legs.addObject();
    ObjectNode voyage = leg.putObject("vesselVoyage");
    voyage.put("vesselName", "MSC Example");
    voyage.put("carrierImportVoyageNumber", "0123W");

    assertTrue(ANChecks.validateTransport().validate(body).getErrorMessages().isEmpty());

    voyage.remove("vesselName");
    assertFalse(ANChecks.validateTransport().validate(body).getErrorMessages().isEmpty());
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

    assertTrue(
        ANChecks.validateUtilizedTransportEquipments().validate(body).getErrorMessages().isEmpty());

    ute.set("seals", mapper.createArrayNode());
    assertFalse(
        ANChecks.validateUtilizedTransportEquipments().validate(body).getErrorMessages().isEmpty());

    ute.remove("equipment");
    assertFalse(
        ANChecks.validateUtilizedTransportEquipments().validate(body).getErrorMessages().isEmpty());
  }

  @Test
  void testValidateConsignmentItems() {

    ArrayNode consignmentItems = an.putArray("consignmentItems");
    ObjectNode ci = consignmentItems.addObject();
    assertFalse(ANChecks.validateConsignmentItems().validate(body).getErrorMessages().isEmpty());

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

    assertTrue(ANChecks.validateConsignmentItems().validate(body).getErrorMessages().isEmpty());

    op.remove("packageCode");
    op.remove("IMOPackagingCode");
    op.remove("description");
    assertFalse(ANChecks.validateConsignmentItems().validate(body).getErrorMessages().isEmpty());
  }

  @Test
  void testValidateFreeTimeObjectStructure() {

    ArrayNode freeTimes = an.putArray("freeTimes");
    ObjectNode ft = freeTimes.addObject();
    ft.putArray("typeCodes").add("DEM");
    ft.putArray("ISOEquipmentCodes").add("22G1");
    ft.putArray("equipmentReferences").add("MSCU1234567");
    ft.put("duration", 5);
    ft.put("timeUnit", "HR");

    assertTrue(
        ANChecks.validateFreeTimeObjectStructure().validate(body).getErrorMessages().isEmpty());

    an.set("freeTimes", mapper.createArrayNode());
    assertFalse(
        ANChecks.validateFreeTimeObjectStructure().validate(body).getErrorMessages().isEmpty());
  }

  @Test
  void testValidateFreeTimeInvalidTimeUnit() {

    ArrayNode freeTimes = an.putArray("freeTimes");
    ObjectNode ft = freeTimes.addObject();
    ft.putArray("typeCodes").add("DEM");
    ft.putArray("ISOEquipmentCodes").add("22G1");
    ft.putArray("equipmentReferences").add("MSCU1234567");
    ft.put("duration", 5);
    ft.put("timeUnit", "DAY");

    assertFalse(
        ANChecks.validateFreeTimeObjectStructure().validate(body).getErrorMessages().isEmpty());
  }

  @Test
  void testValidateFreeTimeMissingReqFields() {

    ArrayNode freeTimes = an.putArray("freeTimes");
    ObjectNode ft = freeTimes.addObject();
    ft.putArray("equipmentReferences").add("MSCU1234567");
    ft.put("duration", 5);
    ft.put("timeUnit", "DAY");

    assertFalse(
        ANChecks.validateFreeTimeObjectStructure().validate(body).getErrorMessages().isEmpty());
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

    assertTrue(ANChecks.validateChargesStructure().validate(body).getErrorMessages().isEmpty());

    ch.remove("currencyCode");
    assertFalse(ANChecks.validateChargesStructure().validate(body).getErrorMessages().isEmpty());
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
    assertFalse(ANChecks.validatePartyContactName().validate(body).getErrorMessages().isEmpty());
    partyContactDetail.put("name", "Ops Desk");
    assertTrue(ANChecks.validatePartyContactName().validate(body).getErrorMessages().isEmpty());
  }

  @Test
  void testValidatePartyContactDetailsEmailOrPhone() {

    ArrayNode documentParties = an.putArray("documentParties");
    ObjectNode documentParty = documentParties.addObject();
    ArrayNode partyContactDetails = documentParty.putArray("partyContactDetails");
    ObjectNode partyContactDetail = partyContactDetails.addObject();

    assertFalse(
        ANChecks.validatePartyContactEmailOrPhone().validate(body).getErrorMessages().isEmpty());

    partyContactDetail.put("email", "ops@example.com");
    assertTrue(
        ANChecks.validatePartyContactEmailOrPhone().validate(body).getErrorMessages().isEmpty());
  }

  @Test
  void testInvalidValidatePortOfDischargeLocationFields() {

    ObjectNode transport = an.putObject("transport");
    transport.putObject("portOfDischarge");

    assertFalse(
        ANChecks.validatePortOfDischargeLocation("arrivalNotices.*.transport")
            .validate(body)
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testIValidValidatePortOfDischargeFacilityFields() {

    ObjectNode transport = an.putObject("transport");
    ObjectNode pod = transport.putObject("portOfDischarge");
    pod.putObject("facility");

    assertFalse(
        ANChecks.validatePortOfDischargeLocation("arrivalNotices.*.transport")
            .validate(body)
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testInvalidPortOfDischargeFacilityFields() {

    ObjectNode transport = an.putObject("transport");
    ObjectNode pod = transport.putObject("portOfDischarge");
    pod.putObject("facility");

    assertFalse(
        ANChecks.validatePortOfDischargeLocation("arrivalNotices.*.transport")
            .validate(body)
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testvPortOfDischargeFacilityWithoutFacilityListProvider() {

    ObjectNode transport = an.putObject("transport");
    ObjectNode pod = transport.putObject("portOfDischarge");
    ObjectNode facility = pod.putObject("facility");
    facility.put("facilityCode", "ADT");

    assertFalse(
        ANChecks.validatePortOfDischargeLocation("arrivalNotices.*.transport")
            .validate(body)
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testvPortOfDischargeFacilityWithInvalidFacilityListProvider() {

    ObjectNode transport = an.putObject("transport");
    ObjectNode pod = transport.putObject("portOfDischarge");
    ObjectNode facility = pod.putObject("facility");
    facility.put("facilityCode", "ADT");
    facility.put("facilityCodeListProvider", "SMDG_INVALID");

    assertFalse(
        ANChecks.validatePortOfDischargeLocation("arrivalNotices.*.transport")
            .validate(body)
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testPortOfDischargeFacilityWithvalidFacilityListProvider() {

    ObjectNode transport = an.putObject("transport");
    ObjectNode pod = transport.putObject("portOfDischarge");
    ObjectNode facility = pod.putObject("facility");
    facility.put("facilityCode", "ADT");
    facility.put("facilityCodeListProvider", "SMDG");

    assertTrue(
        ANChecks.validatePortOfDischargeLocation("arrivalNotices.*.transport")
            .validate(body)
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testANNPortOfDischargeFacilityWithInvalidFacilityListProvider() {
    ArrayNode arrivalNoticeNotifications = body.putArray("arrivalNoticeNotifications");
    ObjectNode arrivalNotice = arrivalNoticeNotifications.addObject();
    ObjectNode pod = arrivalNotice.putObject("portOfDischarge");
    ObjectNode facility = pod.putObject("facility");
    facility.put("facilityCode", "ADT");
    facility.put("facilityCodeListProvider", "SMDG_INVALID");

    assertFalse(
        ANChecks.validatePortOfDischargeLocation("arrivalNoticeNotifications.*")
            .validate(body)
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testANNPortOfDischargeFacilityWithValidFacilityListProvider() {
    ArrayNode arrivalNoticeNotifications = body.putArray("arrivalNoticeNotifications");
    ObjectNode arrivalNotice = arrivalNoticeNotifications.addObject();
    ObjectNode pod = arrivalNotice.putObject("portOfDischarge");
    ObjectNode facility = pod.putObject("facility");
    facility.put("facilityCode", "ADT");
    facility.put("facilityCodeListProvider", "SMDG");

    assertTrue(
        ANChecks.validatePortOfDischargeLocation("arrivalNoticeNotifications.*")
            .validate(body)
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testValidatePortOfDischargeAddress() {

    ObjectNode transport = an.putObject("transport");
    ObjectNode pod = transport.putObject("portOfDischarge");
    ObjectNode address = pod.putObject("address");
    address.put("street", "");

    assertFalse(ANChecks.validatePODAdrressAN().validate(body).getErrorMessages().isEmpty());

    address.put("street", "Harbor Rd 1");
    address.put("city", "Rotterdam");
    assertTrue(ANChecks.validatePODAdrressAN().validate(body).getErrorMessages().isEmpty());

    pod.remove("address");
    pod.put("UNLocationCode", "NLRTM");
    assertTrue(ANChecks.validatePODAdrressAN().validate(body).getErrorMessages().isEmpty());
  }

  @Test
  void testValidateDocumentPartyAddress() {

    ArrayNode documentParties = an.putArray("documentParties");
    ObjectNode documentParty = documentParties.addObject();
    assertFalse(ANChecks.validateDocumentPartyAddress().validate(body).getErrorMessages().isEmpty());

    ObjectNode address = documentParty.putObject("address");
    assertFalse(ANChecks.validateDocumentPartyAddress().validate(body).getErrorMessages().isEmpty());

    address.put("street", "");

    assertFalse(ANChecks.validateDocumentPartyAddress().validate(body).getErrorMessages().isEmpty());

    address.put("street", "Harbor Rd 1");
    address.put("city", "Rotterdam");
    assertTrue(ANChecks.validatePODAdrressAN().validate(body).getErrorMessages().isEmpty());
  }
}
