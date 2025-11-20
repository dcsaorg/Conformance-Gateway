package org.dcsa.conformance.standards.an;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.standards.an.checks.ANChecks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ANChecksTest {

  private ObjectMapper mapper;
  private ObjectNode body;
  private ObjectNode an;

  @BeforeEach
  void setUp() {
    mapper = new ObjectMapper();
    body = mapper.createObjectNode();
    ArrayNode arrivalNotices = body.putArray("arrivalNotices");
    an = arrivalNotices.addObject();
  }

  private static boolean isOk(JsonContentCheck check, ObjectNode body) {
    ConformanceCheckResult r = check.validate(body);
    return r.getErrorMessages().isEmpty();
  }

  private static boolean isFail(JsonContentCheck check, ObjectNode body) {
    return !isOk(check, body);
  }

  @Test
  void nonEmptyArrivalNotices_fails_when_missing_array() {
    ObjectNode empty = mapper.createObjectNode();
    assertTrue(
        ANChecks.nonEmptyArrivalNotices()
            .validate(empty)
            .getErrorMessages()
            .contains("arrivalNotices must be a non-empty array"));
  }

  @Test
  void nonEmptyArrivalNotices_passes_when_array_has_one_AN() {
    assertTrue(isOk(ANChecks.nonEmptyArrivalNotices(), body));
  }

  @Test
  void atLeastOneCarrierCodeCorrect_fails_when_all_missing() {
    assertTrue(isFail(ANChecks.atLeastOneCarrierCodeCorrect(), body));
  }

  @Test
  void atLeastOneCarrierCodeCorrect_passes_when_one_AN_has_carrierCode() {
    an.put("carrierCode", "MAEU");
    assertTrue(isOk(ANChecks.atLeastOneCarrierCodeCorrect(), body));
  }


  @Test
  void atLeastOneTransportDocumentReferenceCorrect_fails_when_all_missing() {
    assertTrue(isFail(ANChecks.atLeastOneTransportDocumentReferenceCorrect(), body));
  }

  @Test
  void atLeastOneTransportDocumentReferenceCorrect_passes_when_one_AN_has_TDR() {
    an.put("transportDocumentReference", "TDR12345");
    assertTrue(isOk(ANChecks.atLeastOneTransportDocumentReferenceCorrect(), body));
  }

  @Test
  void atLeastOneCarrierCodeListProviderCorrect_fails_when_missing() {
    assertTrue(isFail(ANChecks.atLeastOneCarrierCodeListProviderCorrect(), body));
  }

  @Test
  void atLeastOneCarrierCodeListProviderCorrect_fails_when_invalid_value() {
    an.put("carrierCodeListProvider", "FOO");
    assertTrue(isFail(ANChecks.atLeastOneCarrierCodeListProviderCorrect(), body));
  }

  @Test
  void atLeastOneCarrierCodeListProviderCorrect_passes_when_NMFTA() {
    an.put("carrierCodeListProvider", "NMFTA");
    assertTrue(isOk(ANChecks.atLeastOneCarrierCodeListProviderCorrect(), body));
  }

  @Test
  void atLeastOneCarrierCodeListProviderCorrect_passes_when_SMDG() {
    an.put("carrierCodeListProvider", "SMDG");
    assertTrue(isOk(ANChecks.atLeastOneCarrierCodeListProviderCorrect(), body));
  }



  @Test
  void atLeastOneCarrierContactInformationCorrect_fails_when_array_missing() {
    assertTrue(isFail(ANChecks.atLeastOneCarrierContactInformationCorrect(), body));
  }

  @Test
  void atLeastOneCarrierContactInformationCorrect_fails_when_all_invalid() {
    ArrayNode ccis = an.putArray("carrierContactInformation");
    ccis.addObject();
    assertTrue(isFail(ANChecks.atLeastOneCarrierContactInformationCorrect(), body));
  }

  @Test
  void atLeastOneCarrierContactInformationCorrect_passes_when_one_valid_entry() {
    ArrayNode ccis = an.putArray("carrierContactInformation");
    ccis.addObject().put("name", "Ops Desk").put("email", "ops@example.com");
    assertTrue(isOk(ANChecks.atLeastOneCarrierContactInformationCorrect(), body));
  }


  @Test
  void atLeastOneDeliveryTypeAtDestination_fails_when_missing() {
    assertTrue(isFail(ANChecks.atLeastOneDeliveryTypeAtDestination(), body));
  }

  @Test
  void atLeastOneDeliveryTypeAtDestination_fails_when_invalid() {
    an.put("deliveryTypeAtDestination", "FOO");
    assertTrue(isFail(ANChecks.atLeastOneDeliveryTypeAtDestination(), body));
  }

  @Test
  void atLeastOneDeliveryTypeAtDestination_passes_when_valid() {

    an.put("deliveryTypeAtDestination", "CY");
    assertTrue(isOk(ANChecks.atLeastOneDeliveryTypeAtDestination(), body));
  }


  @Test
  void atLeastOneDocumentPartiesCorrect_fails_when_missing_array() {
    assertTrue(isFail(ANChecks.atLeastOneDocumentPartiesCorrect(), body));
  }

  @Test
  void atLeastOneDocumentPartiesCorrect_fails_when_party_incomplete() {
    var parties = an.putArray("documentParties");
    var p = parties.addObject();
    ConformanceCheckResult r = ANChecks.atLeastOneDocumentPartiesCorrect().validate(body);
    assertFalse(r.getErrorMessages().isEmpty());
  }

  @Test
  void atLeastOneDocumentPartiesCorrect_passes_when_one_party_is_valid() {
    var parties = an.putArray("documentParties");
    var p = parties.addObject();
    p.put("partyFunction", "CN");
    p.put("partyName", "Consignee LLC");

    var cds = p.putArray("partyContactDetails");
    cds.addObject().put("name", "Ops Desk").put("email", "ops@example.com");

    var addr = p.putObject("address");
    addr.put("street", "Harbor Rd 1");
    addr.put("city", "Rotterdam");

    ConformanceCheckResult r = ANChecks.atLeastOneDocumentPartiesCorrect().validate(body);
    assertTrue(r.getErrorMessages().isEmpty());
  }


  @Test
  void atLeastOneTransportCorrect_fails_when_transport_missing() {
    assertTrue(isFail(ANChecks.atLeastOneTransportCorrect(), body));
  }

  @Test
  void atLeastOneTransportCorrect_passes_with_minimal_valid_transport() {
    var transport = an.putObject("transport");
    transport.putObject("portOfDischargeArrivalDate").put("value", "2025-10-01T10:00:00Z");

    var pod = transport.putObject("portOfDischarge");
    pod.put("UNLocationCode", "NLRTM"); // valid UNLoc

    var legs = transport.putArray("legs");
    var leg = legs.addObject();
    var vv = leg.putObject("vesselVoyage");
    vv.put("vesselName", "MSC Example");
    vv.put("carrierImportVoyageNumber", "0123W");

    ConformanceCheckResult r = ANChecks.atLeastOneTransportCorrect().validate(body);
    assertTrue(r.getErrorMessages().isEmpty());
  }

  @Test
  void atLeastOneTransportCorrect_fails_when_facility_present_but_incomplete() {
    var transport = an.putObject("transport");
    transport.putObject("portOfDischargeArrivalDate").put("value", "2025-10-01T10:00:00Z");

    var pod = transport.putObject("portOfDischarge");
    var facility = pod.putObject("facility");
    facility.put("facilityCode", "ADT");

    var legs = transport.putArray("legs");
    var leg = legs.addObject();
    var vv = leg.putObject("vesselVoyage");
    vv.put("vesselName", "MSC Example");
    vv.put("carrierImportVoyageNumber", "0123W");

    ConformanceCheckResult r = ANChecks.atLeastOneTransportCorrect().validate(body);
    assertFalse(r.getErrorMessages().isEmpty());
  }


  @Test
  void atLeastOneUtilizedTransportEquipmentsCorrect_fails_when_missing_array() {
    assertTrue(isFail(ANChecks.atLeastOneUtilizedTransportEquipmentsCorrect(), body));
  }

  @Test
  void atLeastOneUtilizedTransportEquipmentsCorrect_passes_with_valid_UTE() {
    var utes = an.putArray("utilizedTransportEquipments");
    var ute = utes.addObject();

    var eq = ute.putObject("equipment");
    eq.put("equipmentReference", "MSCU1234567");
    eq.put("ISOEquipmentCode", "22G1");

    var seals = ute.putArray("seals");
    seals.addObject().put("number", "ABC123");

    ConformanceCheckResult r =
        ANChecks.atLeastOneUtilizedTransportEquipmentsCorrect().validate(body);
    assertTrue(r.getErrorMessages().isEmpty());
  }

  @Test
  void atLeastOneUtilizedTransportEquipmentsCorrect_fails_when_all_seals_empty() {
    var utes = an.putArray("utilizedTransportEquipments");
    var ute = utes.addObject();

    var eq = ute.putObject("equipment");
    eq.put("equipmentReference", "MSCU1234567");
    eq.put("ISOEquipmentCode", "22G1");

    var seals = ute.putArray("seals");
    seals.addObject().put("number", "");

    ConformanceCheckResult r =
        ANChecks.atLeastOneUtilizedTransportEquipmentsCorrect().validate(body);
    assertFalse(r.getErrorMessages().isEmpty());
  }


  @Test
  void atLeastOneConsignmentItemsCorrect_fails_when_missing_array() {
    assertTrue(isFail(ANChecks.atLeastOneConsignmentItemsCorrect(), body));
  }

  @Test
  void atLeastOneConsignmentItemsCorrect_passes_with_valid_structure() {
    var consignmentItems = an.putArray("consignmentItems");
    var ci = consignmentItems.addObject();

    var dog = ci.putArray("descriptionOfGoods");
    dog.add("Widgets");

    var cargoItems = ci.putArray("cargoItems");
    var item = cargoItems.addObject();
    item.put("equipmentReference", "MSCU1234567");

    var cgw = item.putObject("cargoGrossWeight");
    cgw.put("value", 1234.5);
    cgw.put("unit", "KGM");

    var op = item.putObject("outerPackaging");
    op.put("packageCode", "CT");
    op.put("numberOfPackages", 10);

    ConformanceCheckResult r = ANChecks.atLeastOneConsignmentItemsCorrect().validate(body);
    assertTrue(r.getErrorMessages().isEmpty());
  }

  @Test
  void atLeastOneConsignmentItemsCorrect_fails_when_no_valid_cargo_item() {
    var consignmentItems = an.putArray("consignmentItems");
    var ci = consignmentItems.addObject();

    var dog = ci.putArray("descriptionOfGoods");
    dog.add("Widgets");

    var cargoItems = ci.putArray("cargoItems");
    var item = cargoItems.addObject();
    // All invalid / empty
    item.put("equipmentReference", "");

    ConformanceCheckResult r = ANChecks.atLeastOneConsignmentItemsCorrect().validate(body);
    assertFalse(r.getErrorMessages().isEmpty());
  }


  @Test
  void atLeastOneANFreeTimeCorrect_fails_when_no_freeTimes() {
    assertTrue(isFail(ANChecks.atLeastOneANFreeTimeCorrect(), body));
  }

  @Test
  void atLeastOneANFreeTimeCorrect_passes_with_valid_freeTime_item() {
    var freeTimes = an.putArray("freeTimes");
    var ft = freeTimes.addObject();

    ft.putArray("typeCodes").add("DEM");
    ft.putArray("ISOEquipmentCodes").add("22G1");
    ft.putArray("equipmentReferences").add("MSCU1234567");
    ft.put("duration", 5);
    ft.put("timeUnit", "HR");

    ConformanceCheckResult r = ANChecks.atLeastOneANFreeTimeCorrect().validate(body);
    assertTrue(r.getErrorMessages().isEmpty());
  }

  @Test
  void atLeastOneANFreeTimeCorrect_fails_with_invalid_timeUnit() {
    var freeTimes = an.putArray("freeTimes");
    var ft = freeTimes.addObject();

    ft.putArray("typeCodes").add("DEM");
    ft.putArray("ISOEquipmentCodes").add("22G1");
    ft.putArray("equipmentReferences").add("MSCU1234567");
    ft.put("duration", 5);
    ft.put("timeUnit", "DAY");

    ConformanceCheckResult r = ANChecks.atLeastOneANFreeTimeCorrect().validate(body);
    assertFalse(r.getErrorMessages().isEmpty());
  }


  @Test
  void atLeastOneANChargesCorrect_fails_when_no_charges() {
    assertTrue(isFail(ANChecks.atLeastOneANChargesCorrect(), body));
  }

  @Test
  void atLeastOneANChargesCorrect_passes_with_valid_charge_item() {
    var charges = an.putArray("charges");
    var ch = charges.addObject();
    ch.put("chargeName", "Arrival Notice Fee");
    ch.put("currencyAmount", 50.0);
    ch.put("currencyCode", "EUR");
    ch.put("paymentTermCode", "PRE");
    ch.put("unitPrice", 50.0);
    ch.put("quantity", 1);

    ConformanceCheckResult r = ANChecks.atLeastOneANChargesCorrect().validate(body);
    assertTrue(r.getErrorMessages().isEmpty());
  }

  @Test
  void atLeastOneANChargesCorrect_fails_when_all_charges_invalid() {
    var charges = an.putArray("charges");
    var ch = charges.addObject();
    ch.put("chargeName", "");
    ch.put("currencyAmount", -5.0);
    ch.put("currencyCode", "");
    ch.put("unitPrice", 0.0);
    ch.put("quantity", 0.0);
    ch.put("paymentTermCode", "XXX");

    ConformanceCheckResult r = ANChecks.atLeastOneANChargesCorrect().validate(body);
    assertFalse(r.getErrorMessages().isEmpty());
  }


}
