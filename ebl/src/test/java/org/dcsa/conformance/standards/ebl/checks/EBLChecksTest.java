package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.BUYER_AND_SELLER_CONDITIONAL_CHECK;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.ENS_MANIFEST_TYPE_REQUIRES_HBL_ISSUED;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.HBL_NOTIFY_PARTY_REQUIRED_IF_TO_ORDER;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.IDENTIFICATION_NUMBER_REQUIRED_IF_ENS_AND_SELF;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.NUMBER_OF_PACKAGES_CONDITIONAL_CHECK;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.SELF_FILER_CODE_REQUIRED_IF_ACE_ACI_AND_SELF;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.COUNTRY_CODE_CONDITIONAL_VALIDATION_POA;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.LOCATION_NAME_CONDITIONAL_VALIDATION_POA;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.LOCATION_NAME_CONDITIONAL_VALIDATION_POFD;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.VALID_REQUESTED_CARRIER_CLAUSES;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EBLChecksTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testENSManifestTypeRequiresHBLIssued() {
    ObjectNode rootNode = objectMapper.createObjectNode();
    ArrayNode advanceManifestFilings = rootNode.putArray("advanceManifestFilings");
    advanceManifestFilings.addObject().put("manifestTypeCode", "ENS");

    Set<String> errors = ENS_MANIFEST_TYPE_REQUIRES_HBL_ISSUED.validate(rootNode);
    assertEquals(1, errors.size());

    rootNode.put("isHouseBillOfLadingsIssued", true);

    errors = ENS_MANIFEST_TYPE_REQUIRES_HBL_ISSUED.validate(rootNode);
    assertEquals(0, errors.size());
  }

  @Test
  void testHBLNotifyPartyRequiredIfToOrder() {
    ObjectNode rootNode = objectMapper.createObjectNode();
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    hbl.put("isToOrder", true);
    hbl.set("documentParties", objectMapper.createObjectNode());

    Set<String> errors = HBL_NOTIFY_PARTY_REQUIRED_IF_TO_ORDER.validate(rootNode);
    assertEquals(1, errors.size());

    ((ObjectNode) hbl.get("documentParties")).set("notifyParty", objectMapper.createObjectNode());

    errors = HBL_NOTIFY_PARTY_REQUIRED_IF_TO_ORDER.validate(rootNode);
    assertEquals(0, errors.size());
  }

  @Test
  void testNumberOfPackagesConditionalCheck() {
    ObjectNode rootNode = objectMapper.createObjectNode();
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    ArrayNode consignmentItems = hbl.putArray("consignmentItems");
    ObjectNode consignmentItem = consignmentItems.addObject();
    ArrayNode cargoItems = consignmentItem.putArray("cargoItems");
    ObjectNode cargoItem = cargoItems.addObject();
    ObjectNode outerPackaging = cargoItem.putObject("outerPackaging");
    outerPackaging.put("packageCode", "SH");

    Set<String> errors = NUMBER_OF_PACKAGES_CONDITIONAL_CHECK.validate(rootNode);
    assertEquals(1, errors.size());

    outerPackaging.put("numberOfPackages", 10);

    errors = NUMBER_OF_PACKAGES_CONDITIONAL_CHECK.validate(rootNode);
    assertEquals(0, errors.size());
  }

  @Test
  void testIdentificationNumberRequiredIfENSAndSelf() {
    ObjectNode rootNode = objectMapper.createObjectNode();
    ArrayNode advanceManifestFilings = rootNode.putArray("advanceManifestFilings");
    ObjectNode filing = advanceManifestFilings.addObject();
    filing.put("manifestTypeCode", "ENS");
    filing.put("advanceManifestFilingsHouseBLPerformedBy", "SELF");

    Set<String> errors = IDENTIFICATION_NUMBER_REQUIRED_IF_ENS_AND_SELF.validate(rootNode);
    assertEquals(1, errors.size());

    filing.put("identificationNumber", "ID12345");

    errors = IDENTIFICATION_NUMBER_REQUIRED_IF_ENS_AND_SELF.validate(rootNode);
    assertEquals(0, errors.size());
  }

  @Test
  void testSelfFilerCodeRequiredIfAceOrAciAndSelf() {
    ObjectNode rootNode = objectMapper.createObjectNode();
    ArrayNode advanceManifestFilings = rootNode.putArray("advanceManifestFilings");
    ObjectNode filing = advanceManifestFilings.addObject();
    filing.put("manifestTypeCode", "ACE");
    filing.put("advanceManifestFilingsHouseBLPerformedBy", "SELF");

    Set<String> errors = SELF_FILER_CODE_REQUIRED_IF_ACE_ACI_AND_SELF.validate(rootNode);
    assertEquals(1, errors.size());

    filing.put("selfFilerCode", "FLXP");

    errors = SELF_FILER_CODE_REQUIRED_IF_ACE_ACI_AND_SELF.validate(rootNode);
    assertEquals(0, errors.size());
  }

    @Test
    void testLocationNameRequiredIfNoUNLocationCodeInPlaceOfAcceptance() {
      ObjectNode rootNode = objectMapper.createObjectNode();
      ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
      ObjectNode hbl = houseBillOfLadings.addObject();
      ObjectNode placeOfAcceptance = hbl.putObject("placeOfAcceptance");

      Set<String> errors = LOCATION_NAME_CONDITIONAL_VALIDATION_POA.validate(rootNode);
      assertEquals(1, errors.size());

      placeOfAcceptance.put("locationName", "Amsterdam");
      errors = LOCATION_NAME_CONDITIONAL_VALIDATION_POA.validate(rootNode);
      assertEquals(0, errors.size());

      placeOfAcceptance.put("UNLocationCode", "NLAMS");
      placeOfAcceptance.remove("locationName");
      errors = LOCATION_NAME_CONDITIONAL_VALIDATION_POA.validate(rootNode);
      assertEquals(0, errors.size());
    }

    @Test
    void testLocationNameRequiredIfNoUNLocationCodeInPlaceOfFinalDelivery() {
      ObjectNode rootNode = objectMapper.createObjectNode();
      ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
      ObjectNode hbl = houseBillOfLadings.addObject();
      ObjectNode placeOfFinalDelivery = hbl.putObject("placeOfFinalDelivery");

      Set<String> errors = LOCATION_NAME_CONDITIONAL_VALIDATION_POFD.validate(rootNode);
      assertEquals(1, errors.size());

      placeOfFinalDelivery.put("locationName", "Berlin");
      errors = LOCATION_NAME_CONDITIONAL_VALIDATION_POFD.validate(rootNode);
      assertEquals(0, errors.size());

      placeOfFinalDelivery.put("UNLocationCode", "DEBER");
      placeOfFinalDelivery.remove("locationName");
      errors = LOCATION_NAME_CONDITIONAL_VALIDATION_POFD.validate(rootNode);
      assertEquals(0, errors.size());
    }

    @Test
    void testCountryCodeRequiredIfNoUNLocationCodeInPlaceOfAcceptance() {
      ObjectNode rootNode = objectMapper.createObjectNode();
      ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
      ObjectNode hbl = houseBillOfLadings.addObject();
      ObjectNode placeOfAcceptance = hbl.putObject("placeOfAcceptance");

      Set<String> errors = COUNTRY_CODE_CONDITIONAL_VALIDATION_POA.validate(rootNode);
      assertEquals(1, errors.size());

      placeOfAcceptance.put("countryCode", "NL");
      errors = COUNTRY_CODE_CONDITIONAL_VALIDATION_POA.validate(rootNode);
      assertEquals(0, errors.size());

      placeOfAcceptance.put("UNLocationCode", "NLAMS");
      placeOfAcceptance.remove("countryCode");
      errors = COUNTRY_CODE_CONDITIONAL_VALIDATION_POA.validate(rootNode);
      assertEquals(0, errors.size());
    }

    @Test
    void testCountryCodeRequiredIfNoUNLocationCodeInPlaceOfFinalDelivery() {
      ObjectNode rootNode = objectMapper.createObjectNode();
      ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
      ObjectNode hbl = houseBillOfLadings.addObject();
      ObjectNode placeOfFinalDelivery = hbl.putObject("placeOfFinalDelivery");

      Set<String> errors = COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD.validate(rootNode);
      assertEquals(1, errors.size());

      placeOfFinalDelivery.put("countryCode", "DE");
      errors = COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD.validate(rootNode);
      assertEquals(0, errors.size());

      placeOfFinalDelivery.put("UNLocationCode", "DEBER");
      placeOfFinalDelivery.remove("countryCode");
      errors = COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD.validate(rootNode);
      assertEquals(0, errors.size());
    }

  @Test
  void testBuyerAndSellerConditionalCheck() {
    ObjectNode rootNode = objectMapper.createObjectNode();
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    hbl.put("isCargoDeliveredInICS2Zone", true);
    ObjectNode documentParties = hbl.putObject("documentParties");

    ArrayNode advanceManifestFilings = rootNode.putArray("advanceManifestFilings");
    ObjectNode filing = advanceManifestFilings.addObject();
    filing.put("manifestTypeCode", "ENS");
    filing.put("advanceManifestFilingsHouseBLPerformedBy", "CARRIER");


    Set<String> errors = BUYER_AND_SELLER_CONDITIONAL_CHECK.validate(rootNode);
    assertEquals(1, errors.size());


    documentParties.putObject("buyer");
    documentParties.putObject("seller");

    errors = BUYER_AND_SELLER_CONDITIONAL_CHECK.validate(rootNode);
    assertEquals(0, errors.size());
  }

  @Test
  void testValidRoutingOfConsignmentCountries() {
    ObjectNode rootNode = objectMapper.createObjectNode();
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();

    ObjectNode placeOfAcceptance = hbl.putObject("placeOfAcceptance");
    placeOfAcceptance.put("countryCode", "NL");

    ObjectNode placeOfFinalDelivery = hbl.putObject("placeOfFinalDelivery");
    placeOfFinalDelivery.put("countryCode", "DE");

    ArrayNode routingOfConsignmentCountries = hbl.putArray("routingOfConsignmentCountries");
    routingOfConsignmentCountries.add("NL");
    routingOfConsignmentCountries.add("BE");
    routingOfConsignmentCountries.add("DE");

    Set<String> errors = ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK.validate(rootNode);
    assertEquals(0, errors.size());
  }

  @Test
  void testInvalidFirstCountryInRouting() {
    ObjectNode rootNode = objectMapper.createObjectNode();
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();

    ObjectNode placeOfAcceptance = hbl.putObject("placeOfAcceptance");
    placeOfAcceptance.put("countryCode", "NL");

    ObjectNode placeOfFinalDelivery = hbl.putObject("placeOfFinalDelivery");
    placeOfFinalDelivery.put("countryCode", "DE");

    ArrayNode routingOfConsignmentCountries = hbl.putArray("routingOfConsignmentCountries");
    routingOfConsignmentCountries.add("BE");
    routingOfConsignmentCountries.add("DE");

    Set<String> errors = ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK.validate(rootNode);
    assertEquals(1, errors.size());
  }

  @Test
  void testInvalidLastCountryInRouting() {
    ObjectNode rootNode = objectMapper.createObjectNode();
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();

    ObjectNode placeOfAcceptance = hbl.putObject("placeOfAcceptance");
    placeOfAcceptance.put("countryCode", "NL");

    ObjectNode placeOfFinalDelivery = hbl.putObject("placeOfFinalDelivery");
    placeOfFinalDelivery.put("countryCode", "DE");

    ArrayNode routingOfConsignmentCountries = hbl.putArray("routingOfConsignmentCountries");
    routingOfConsignmentCountries.add("NL");
    routingOfConsignmentCountries.add("BE");
    routingOfConsignmentCountries.add("FR");


    Set<String> errors = ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK.validate(rootNode);
    assertEquals(1, errors.size());
  }

  @Test
  void testValidRequestedCarrierClauses(){
    ObjectNode rootNode = objectMapper.createObjectNode();
    ArrayNode requestedCarrierClauses = rootNode.putArray("requestedCarrierClauses");
    requestedCarrierClauses.add("CARGO_CARGOSPECIFICS");
    requestedCarrierClauses.add("VESSELCONVEYANCE_COUNTRYSPECIFIC");

    Set<String> errors = VALID_REQUESTED_CARRIER_CLAUSES.validate(rootNode);
    assertEquals(0, errors.size());

    requestedCarrierClauses.add("CARGO_CARGO");
    Set<String> invalidErrors = VALID_REQUESTED_CARRIER_CLAUSES.validate(rootNode);
    assertEquals(1, invalidErrors.size());

  }

}
