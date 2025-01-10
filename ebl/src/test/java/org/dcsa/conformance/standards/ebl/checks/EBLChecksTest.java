package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.BUYER_AND_SELLER_CONDITIONAL_CHECK;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.ENS_MANIFEST_TYPE_REQUIRES_HBL_ISSUED;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.HBL_NOTIFY_PARTY_REQUIRED_IF_TO_ORDER;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.IDENTIFICATION_NUMBER_REQUIRED_IF_ENS_AND_SELF;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.NUMBER_OF_PACKAGES_CONDITIONAL_CHECK;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.SELF_FILER_CODE_REQUIRED_IF_ACE_ACI_AND_SELF;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.COUNTRY_CODE_CONDITIONAL_VALIDATION_POA;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.LOCATION_NAME_CONDITIONAL_VALIDATION_POA;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.LOCATION_NAME_CONDITIONAL_VALIDATION_POFD;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.SEND_TO_PLATFORM_CONDITIONAL_CHECK;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.VALID_CONSIGMENT_ITEMS_REFERENCE_TYPES;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.VALID_PARTY_FUNCTION;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.VALID_PARTY_FUNCTION_HBL;
import static org.dcsa.conformance.standards.ebl.checks.EBLChecks.VALID_REQUESTED_CARRIER_CLAUSES;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EBLChecksTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final ObjectNode rootNode = objectMapper.createObjectNode();

  @Test
  void testENSManifestTypeRequiresHBLIssued() {
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
  void testValidRequestedCarrierClauses() {
    ArrayNode requestedCarrierClauses = rootNode.putArray("requestedCarrierClauses");
    requestedCarrierClauses.add("CARGO_CARGOSPECIFICS");
    requestedCarrierClauses.add("VESSELCONVEYANCE_COUNTRYSPECIFIC");

    Set<String> errors = VALID_REQUESTED_CARRIER_CLAUSES.validate(rootNode);
    assertEquals(0, errors.size());

    requestedCarrierClauses.add("CARGO_CARGO");
    Set<String> invalidErrors = VALID_REQUESTED_CARRIER_CLAUSES.validate(rootNode);
    assertEquals(1, invalidErrors.size());
  }

  @Test
  void testValidConsignmentItemsReferenceTypes() {
    ArrayNode consignmentItems = rootNode.putArray("consignmentItems");
    ObjectNode consignmentItem = consignmentItems.addObject();
    ArrayNode references = consignmentItem.putArray("references");
    references.addObject().put("type", "CR");
    references.addObject().put("type", "SPO");

    Set<String> errors = VALID_CONSIGMENT_ITEMS_REFERENCE_TYPES.validate(rootNode);
    assertEquals(0, errors.size());

    references.addObject().put("type", "CRR");
    Set<String> invalidErrors = VALID_CONSIGMENT_ITEMS_REFERENCE_TYPES.validate(rootNode);
    assertEquals(1, invalidErrors.size());
  }

  @Test
  void testValidPartyFunction() {
    ObjectNode documentParties = rootNode.putObject("documentParties");
    ArrayNode otherParties = documentParties.putArray("other");
    ObjectNode otherParty = otherParties.addObject();
    otherParty.put("partyFunction", "SCO");

    Set<String> errors = VALID_PARTY_FUNCTION.validate(rootNode);
    assertEquals(0, errors.size());

    otherParty.put("partyFunction", "SSS");
    errors = VALID_PARTY_FUNCTION.validate(rootNode);
    assertEquals(1, errors.size());
  }

  @Test
  void testValidPartyFunctionHBL() {
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    ObjectNode documentParties = hbl.putObject("documentParties");
    ArrayNode otherParties = documentParties.putArray("other");
    ObjectNode otherParty = otherParties.addObject();
    otherParty.put("partyFunction", "CS");

    Set<String> errors = VALID_PARTY_FUNCTION_HBL.validate(rootNode);
    assertEquals(0, errors.size());

    otherParty.put("partyFunction", "SSS");
    errors = VALID_PARTY_FUNCTION_HBL.validate(rootNode);
    assertEquals(1, errors.size());
  }

  @Test
  void testSendToPlatformConditionalCheck() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");

    Set<String> errors = SEND_TO_PLATFORM_CONDITIONAL_CHECK.validate(rootNode);
    assertEquals(1, errors.size());

    rootNode.putObject("documentParties").putObject("issueTo").put("sendToPlatform", "CARX");
    errors = SEND_TO_PLATFORM_CONDITIONAL_CHECK.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("transportDocumentTypeCode", "SWB");
    errors = SEND_TO_PLATFORM_CONDITIONAL_CHECK.validate(rootNode);
    assertEquals(1, errors.size());

    rootNode.put("isElectronic", false);
    errors = SEND_TO_PLATFORM_CONDITIONAL_CHECK.validate(rootNode);
    assertEquals(1, errors.size());
  }

  @Test
  void testEBLSCannotHaveCopiesWithCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    Set<String> errors = EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithCharges", 1);
    errors = EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode);
    assertEquals(1, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithCharges", 0);
    errors = EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithCharges", 1);
    errors = EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfCopiesWithCharges", 1);
    errors = EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());
  }

  @Test
  void testEBLSCannotHaveCopiesWithoutCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    Set<String> errors = EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithoutCharges", 1);
    errors = EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(1, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithoutCharges", 0);
    errors = EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithoutCharges", 1);
    errors = EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfCopiesWithoutCharges", 1);
    errors = EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());
  }

  @Test
  void testEBLSCannotHaveOriginalsWithCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    Set<String> errors = SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithCharges", 0);
    errors = SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithCharges", 1);
    errors = SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES.validate(rootNode);
    assertEquals(1, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 1);
    errors = SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());
  }

  @Test
  void testEBLSCannotHaveOriginalsWithoutCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    Set<String> errors = SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithoutCharges", 0);
    errors = SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithoutCharges", 1);
    errors = SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(1, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 1);
    errors = SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());
  }

  @Test
  void testEBLSCannotHaveMoreThanOneOriginalsWithoutCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    Set<String> errors = EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 0);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 1);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 2);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(1, errors.size());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 0);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 1);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 2);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithoutCharges", 0);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

  }

  @Test
  void testEBLSCannotHaveMoreThanOneOriginalsWithCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    Set<String> errors = EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 0);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 1);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 2);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode);
    assertEquals(1, errors.size());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 0);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 1);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 2);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithCharges", 0);
    errors = EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode);
    assertEquals(0, errors.size());
    }
}
