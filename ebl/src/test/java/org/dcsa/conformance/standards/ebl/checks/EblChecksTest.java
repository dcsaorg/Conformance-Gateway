package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.standards.ebl.checks.EblChecks.BUYER_AND_SELLER_CONDITIONAL_CHECK;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.COUNTRY_CODE_CONDITIONAL_VALIDATION_POA;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.ENS_MANIFEST_TYPE_REQUIRES_HBL_ISSUED;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.FEEDBACKS_PRESENCE;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.HBL_NOTIFY_PARTY_REQUIRED_IF_TO_ORDER;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.IDENTIFICATION_NUMBER_REQUIRED_IF_ENS_AND_SELF;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.LOCATION_NAME_CONDITIONAL_VALIDATION_POA;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.LOCATION_NAME_CONDITIONAL_VALIDATION_POFD;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.NUMBER_OF_PACKAGES_CONDITIONAL_CHECK;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.SELF_FILER_CODE_REQUIRED_IF_ACE_ACI_AND_SELF;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.SEND_TO_PLATFORM_CONDITIONAL_CHECK;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.VALIDATE_DOCUMENT_PARTY;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.VALID_CONSIGMENT_ITEMS_REFERENCE_TYPES;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.VALID_PARTY_FUNCTION;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.VALID_PARTY_FUNCTION_HBL;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.VALID_REQUESTED_CARRIER_CLAUSES;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.function.Supplier;
import org.dcsa.conformance.standards.ebl.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.junit.jupiter.api.Test;

class EblChecksTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final ObjectNode rootNode = objectMapper.createObjectNode();

  @Test
  void testENSManifestTypeRequiresHBLIssued() {
    ArrayNode advanceManifestFilings = rootNode.putArray("advanceManifestFilings");
    advanceManifestFilings.addObject().put("manifestTypeCode", "ENS");
    assertFalse(ENS_MANIFEST_TYPE_REQUIRES_HBL_ISSUED.validate(rootNode).isEmpty());

    rootNode.put("isHouseBillOfLadingsIssued", true);
    assertTrue(ENS_MANIFEST_TYPE_REQUIRES_HBL_ISSUED.validate(rootNode).isEmpty());
  }

  @Test
  void testHBLNotifyPartyRequiredIfToOrder() {
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    hbl.put("isToOrder", true);
    hbl.set("documentParties", objectMapper.createObjectNode());
    assertFalse(HBL_NOTIFY_PARTY_REQUIRED_IF_TO_ORDER.validate(rootNode).isEmpty());

    ((ObjectNode) hbl.get("documentParties")).set("notifyParty", objectMapper.createObjectNode());
    assertTrue(HBL_NOTIFY_PARTY_REQUIRED_IF_TO_ORDER.validate(rootNode).isEmpty());
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
    assertFalse(NUMBER_OF_PACKAGES_CONDITIONAL_CHECK.validate(rootNode).isEmpty());

    outerPackaging.put("numberOfPackages", 10);
    assertTrue(NUMBER_OF_PACKAGES_CONDITIONAL_CHECK.validate(rootNode).isEmpty());
  }

  @Test
  void testIdentificationNumberRequiredIfENSAndSelf() {
    ArrayNode advanceManifestFilings = rootNode.putArray("advanceManifestFilings");
    ObjectNode filing = advanceManifestFilings.addObject();
    filing.put("manifestTypeCode", "ENS");
    filing.put("advanceManifestFilingsHouseBLPerformedBy", "SELF");
    assertFalse(IDENTIFICATION_NUMBER_REQUIRED_IF_ENS_AND_SELF.validate(rootNode).isEmpty());

    filing.put("identificationNumber", "ID12345");
    assertTrue(IDENTIFICATION_NUMBER_REQUIRED_IF_ENS_AND_SELF.validate(rootNode).isEmpty());
  }

  @Test
  void testSelfFilerCodeRequiredIfAceOrAciAndSelf() {
    ArrayNode advanceManifestFilings = rootNode.putArray("advanceManifestFilings");
    ObjectNode filing = advanceManifestFilings.addObject();
    filing.put("manifestTypeCode", "ACE");
    filing.put("advanceManifestFilingsHouseBLPerformedBy", "SELF");
    assertFalse(SELF_FILER_CODE_REQUIRED_IF_ACE_ACI_AND_SELF.validate(rootNode).isEmpty());

    filing.put("selfFilerCode", "FLXP");
    assertTrue(SELF_FILER_CODE_REQUIRED_IF_ACE_ACI_AND_SELF.validate(rootNode).isEmpty());
  }

  @Test
  void testLocationNameRequiredIfNoUNLocationCodeInPlaceOfAcceptance() {
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    ObjectNode placeOfAcceptance = hbl.putObject("placeOfAcceptance");
    assertFalse(LOCATION_NAME_CONDITIONAL_VALIDATION_POA.validate(rootNode).isEmpty());

    placeOfAcceptance.put("locationName", "Amsterdam");
    assertTrue(LOCATION_NAME_CONDITIONAL_VALIDATION_POA.validate(rootNode).isEmpty());

    placeOfAcceptance.put("UNLocationCode", "NLAMS");
    placeOfAcceptance.remove("locationName");
    assertTrue(LOCATION_NAME_CONDITIONAL_VALIDATION_POA.validate(rootNode).isEmpty());
  }

  @Test
  void testLocationNameRequiredIfNoUNLocationCodeInPlaceOfFinalDelivery() {
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    ObjectNode placeOfFinalDelivery = hbl.putObject("placeOfFinalDelivery");
    assertFalse(LOCATION_NAME_CONDITIONAL_VALIDATION_POFD.validate(rootNode).isEmpty());

    placeOfFinalDelivery.put("locationName", "Berlin");
    assertTrue(LOCATION_NAME_CONDITIONAL_VALIDATION_POFD.validate(rootNode).isEmpty());

    placeOfFinalDelivery.put("UNLocationCode", "DEBER");
    placeOfFinalDelivery.remove("locationName");
    assertTrue(LOCATION_NAME_CONDITIONAL_VALIDATION_POFD.validate(rootNode).isEmpty());
  }

  @Test
  void testCountryCodeRequiredIfNoUNLocationCodeInPlaceOfAcceptance() {
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    ObjectNode placeOfAcceptance = hbl.putObject("placeOfAcceptance");
    assertFalse(COUNTRY_CODE_CONDITIONAL_VALIDATION_POA.validate(rootNode).isEmpty());

    placeOfAcceptance.put("countryCode", "NL");
    assertTrue(COUNTRY_CODE_CONDITIONAL_VALIDATION_POA.validate(rootNode).isEmpty());

    placeOfAcceptance.put("UNLocationCode", "NLAMS");
    placeOfAcceptance.remove("countryCode");
    assertTrue(COUNTRY_CODE_CONDITIONAL_VALIDATION_POA.validate(rootNode).isEmpty());
  }

  @Test
  void testCountryCodeRequiredIfNoUNLocationCodeInPlaceOfFinalDelivery() {
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    ObjectNode placeOfFinalDelivery = hbl.putObject("placeOfFinalDelivery");
    assertFalse(COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD.validate(rootNode).isEmpty());

    placeOfFinalDelivery.put("countryCode", "DE");
    assertTrue(COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD.validate(rootNode).isEmpty());

    placeOfFinalDelivery.put("UNLocationCode", "DEBER");
    placeOfFinalDelivery.remove("countryCode");
    assertTrue(COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD.validate(rootNode).isEmpty());
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
    assertFalse(BUYER_AND_SELLER_CONDITIONAL_CHECK.validate(rootNode).isEmpty());

    documentParties.putObject("buyer");
    documentParties.putObject("seller");
    assertTrue(BUYER_AND_SELLER_CONDITIONAL_CHECK.validate(rootNode).isEmpty());
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
    assertTrue(ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK.validate(rootNode).isEmpty());
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
    assertFalse(ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK.validate(rootNode).isEmpty());
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
    assertFalse(ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK.validate(rootNode).isEmpty());
  }

  @Test
  void testValidRequestedCarrierClauses() {
    ArrayNode requestedCarrierClauses = rootNode.putArray("requestedCarrierClauses");
    requestedCarrierClauses.add("CARGO_CARGOSPECIFICS");
    requestedCarrierClauses.add("VESSELCONVEYANCE_COUNTRYSPECIFIC");

    assertTrue(VALID_REQUESTED_CARRIER_CLAUSES.validate(rootNode).isEmpty());

    requestedCarrierClauses.add("CARGO_CARGO");
    assertFalse(VALID_REQUESTED_CARRIER_CLAUSES.validate(rootNode).isEmpty());
  }

  @Test
  void testValidConsignmentItemsReferenceTypes() {
    ArrayNode consignmentItems = rootNode.putArray("consignmentItems");
    ObjectNode consignmentItem = consignmentItems.addObject();
    ArrayNode references = consignmentItem.putArray("references");
    references.addObject().put("type", "CR");
    references.addObject().put("type", "SPO");
    assertTrue(VALID_CONSIGMENT_ITEMS_REFERENCE_TYPES.validate(rootNode).isEmpty());

    references.addObject().put("type", "CRR");
    assertFalse(VALID_CONSIGMENT_ITEMS_REFERENCE_TYPES.validate(rootNode).isEmpty());
  }

  @Test
  void testValidPartyFunction() {
    ObjectNode documentParties = rootNode.putObject("documentParties");
    ArrayNode otherParties = documentParties.putArray("other");
    ObjectNode otherParty = otherParties.addObject();
    otherParty.put("partyFunction", "SCO");
    assertTrue(VALID_PARTY_FUNCTION.validate(rootNode).isEmpty());

    otherParty.put("partyFunction", "SSS");
    assertFalse(VALID_PARTY_FUNCTION.validate(rootNode).isEmpty());
  }

  @Test
  void testValidPartyFunctionHBL() {
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    ObjectNode documentParties = hbl.putObject("documentParties");
    ArrayNode otherParties = documentParties.putArray("other");
    ObjectNode otherParty = otherParties.addObject();
    otherParty.put("partyFunction", "CS");
    assertTrue(VALID_PARTY_FUNCTION_HBL.validate(rootNode).isEmpty());

    otherParty.put("partyFunction", "SSS");
    assertFalse(VALID_PARTY_FUNCTION_HBL.validate(rootNode).isEmpty());
  }

  @Test
  void testSendToPlatformConditionalCheck() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    assertFalse(SEND_TO_PLATFORM_CONDITIONAL_CHECK.validate(rootNode).isEmpty());

    rootNode.putObject("documentParties").putObject("issueTo").put("sendToPlatform", "CARX");
    assertTrue(SEND_TO_PLATFORM_CONDITIONAL_CHECK.validate(rootNode).isEmpty());

    rootNode.put("transportDocumentTypeCode", "SWB");
    assertFalse(SEND_TO_PLATFORM_CONDITIONAL_CHECK.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", false);
    assertFalse(SEND_TO_PLATFORM_CONDITIONAL_CHECK.validate(rootNode).isEmpty());
  }

  @Test
  void testEBLSCannotHaveCopiesWithCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    assertTrue(EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithCharges", 1);
    assertFalse(EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithCharges", 0);
    assertTrue(EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithCharges", 1);
    assertTrue(EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfCopiesWithCharges", 1);
    assertTrue(EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode).isEmpty());
  }

  @Test
  void testEBLSCannotHaveCopiesWithoutCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    assertTrue(EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithoutCharges", 1);
    assertFalse(EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithoutCharges", 0);
    assertTrue(EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithoutCharges", 1);
    assertTrue(EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfCopiesWithoutCharges", 1);
    assertTrue(EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode).isEmpty());
  }

  @Test
  void testEBLSCannotHaveOriginalsWithCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    assertTrue(SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithCharges", 0);
    assertTrue(SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithCharges", 1);
    assertFalse(SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 1);
    assertTrue(SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES.validate(rootNode).isEmpty());
  }

  @Test
  void testEBLSCannotHaveOriginalsWithoutCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    assertTrue(SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithoutCharges", 0);
    assertTrue(SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithoutCharges", 1);
    assertFalse(SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 1);
    assertTrue(SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES.validate(rootNode).isEmpty());
  }

  @Test
  void testEBLSCannotHaveMoreThanOneOriginalsWithoutCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 0);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 1);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 2);
    assertFalse(EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 0);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 1);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 2);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithoutCharges", 0);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES.validate(rootNode).isEmpty());
  }

  @Test
  void testEBLSCannotHaveMoreThanOneOriginalsWithCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 0);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 1);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 2);
    assertFalse(EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 0);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 1);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 2);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode).isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithCharges", 0);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES.validate(rootNode).isEmpty());
  }

  @Test
  void testValidateDocumentPartyOther() {
    ObjectNode documentParties = rootNode.putObject("documentParties");

    ArrayNode otherParties = documentParties.putArray("other");
    ObjectNode party = otherParties.addObject().putObject("party");
    party.putObject("address").put("street", "Ruijggoordweg");
    party.putArray("identifyingCodes").addObject().put("codeListProvider", "WAVE");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).isEmpty());

    party.remove("address");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).isEmpty());

    party.remove("address");
    party.remove("identifyingCodes");
    assertFalse(VALIDATE_DOCUMENT_PARTY.validate(rootNode).isEmpty());
  }

  @Test
  void testValidateDocumentPartyNotifyParties() {
    ObjectNode documentParties = rootNode.putObject("documentParties");
    ArrayNode notifyParties = documentParties.putArray("notifyParties");
    ObjectNode notifyParty = notifyParties.addObject();
    notifyParty.putObject("address").put("street", "Ruijggoordweg");
    notifyParty.putArray("identifyingCodes").addObject().put("codeListProvider", "WAVE");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).isEmpty());

    notifyParty.remove("address");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).isEmpty());

    notifyParty.remove("identifyingCodes");
    assertFalse(VALIDATE_DOCUMENT_PARTY.validate(rootNode).isEmpty());
  }

  @Test
  void testValidateDocumentPartyBuyerAndSeller() {
    ObjectNode documentParties = rootNode.putObject("documentParties");
    ObjectNode buyer = documentParties.putObject("buyer");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).isEmpty());

    buyer.putObject("address").put("street", "Ruijggoordweg");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).isEmpty());

    ObjectNode seller = documentParties.putObject("seller");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).isEmpty());

    seller.putObject("address").put("street", "Ruijggoordweg");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).isEmpty());
  }

  @Test
  void testValidateDocumentParty() {
    ObjectNode documentParties = rootNode.putObject("documentParties");
    ObjectNode shipper = documentParties.putObject("shipper");
    assertFalse(VALIDATE_DOCUMENT_PARTY.validate(rootNode).isEmpty());

    shipper.putObject("address").put("street", "Ruijggoordweg");
    shipper.putArray("identifyingCodes").addObject().put("codeListProvider", "WAVE");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).isEmpty());

    shipper.remove("address");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).isEmpty());
  }

  @Test
  void testFeedbacksPresence() {
    rootNode.put(
        "shippingInstructionsStatus", ShippingInstructionsStatus.SI_PENDING_UPDATE.wireName());
    assertFalse(FEEDBACKS_PRESENCE.validate(rootNode).isEmpty());

    rootNode.putArray("feedbacks").addObject().put("code", "INFORMATIONAL_MESSAGE");
    assertTrue(FEEDBACKS_PRESENCE.validate(rootNode).isEmpty());

    rootNode.put(
        "updatedShippingInstructionsStatus",
        ShippingInstructionsStatus.SI_UPDATE_RECEIVED.wireName());
    rootNode.remove("feedbacks");
    assertTrue(FEEDBACKS_PRESENCE.validate(rootNode).isEmpty());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckOneEquipmentValid() {
    Supplier<DynamicScenarioParameters> dspSupplier =
        getDynamicScenarioParametersSupplier(ScenarioType.REGULAR_2C_1U);

    ArrayNode utilizedTransportEquipments = rootNode.putArray("utilizedTransportEquipments");
    utilizedTransportEquipments.addObject();

    assertTrue(
        EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(dspSupplier)
            .validate(rootNode, "")
            .isEmpty());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckOneEquipmentsTooFew() {
    Supplier<DynamicScenarioParameters> dspSupplier =
        getDynamicScenarioParametersSupplier(ScenarioType.REGULAR_2C_1U);

    assertFalse(
        EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(dspSupplier)
            .validate(rootNode, "")
            .isEmpty());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckOneEquipmentTooMany() {
    Supplier<DynamicScenarioParameters> dspSupplier =
        getDynamicScenarioParametersSupplier(ScenarioType.REGULAR_2C_1U);

    ArrayNode utilizedTransportEquipments = rootNode.putArray("utilizedTransportEquipments");
    utilizedTransportEquipments.addObject();
    utilizedTransportEquipments.addObject();

    assertFalse(
        EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(dspSupplier)
            .validate(rootNode, "")
            .isEmpty());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckTwoEquipmentsValid() {
    Supplier<DynamicScenarioParameters> dspSupplier =
        getDynamicScenarioParametersSupplier(ScenarioType.REGULAR_2C_2U);

    ArrayNode utilizedTransportEquipments = rootNode.putArray("utilizedTransportEquipments");
    utilizedTransportEquipments.addObject();
    utilizedTransportEquipments.addObject();

    assertTrue(
        EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(dspSupplier)
            .validate(rootNode, "")
            .isEmpty());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckTwoEquipmentsTooFew() {
    Supplier<DynamicScenarioParameters> dspSupplier =
        getDynamicScenarioParametersSupplier(ScenarioType.REGULAR_2C_2U);

    ArrayNode utilizedTransportEquipments = rootNode.putArray("utilizedTransportEquipments");
    utilizedTransportEquipments.addObject();

    assertFalse(
        EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(dspSupplier)
            .validate(rootNode, "")
            .isEmpty());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckTwoEquipmentsTooMany() {
    Supplier<DynamicScenarioParameters> dspSupplier =
        getDynamicScenarioParametersSupplier(ScenarioType.REGULAR_2C_2U);

    ArrayNode utilizedTransportEquipments = rootNode.putArray("utilizedTransportEquipments");
    utilizedTransportEquipments.addObject();
    utilizedTransportEquipments.addObject();
    utilizedTransportEquipments.addObject();

    assertFalse(
        EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(dspSupplier)
            .validate(rootNode, "")
            .isEmpty());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckNoConstraint() {
    Supplier<DynamicScenarioParameters> dspSupplierOther =
        getDynamicScenarioParametersSupplier(ScenarioType.ACTIVE_REEFER);

    assertTrue(
        EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(dspSupplierOther)
            .validate(rootNode, "")
            .isEmpty());
  }

  private static Supplier<DynamicScenarioParameters> getDynamicScenarioParametersSupplier(
      ScenarioType regular2c1u) {
    return () -> {
      DynamicScenarioParameters dsp = mock(DynamicScenarioParameters.class);
      when(ScenarioType.valueOf(dsp.eblScenarioType())).thenReturn(regular2c1u);
      return dsp;
    };
  }
}
