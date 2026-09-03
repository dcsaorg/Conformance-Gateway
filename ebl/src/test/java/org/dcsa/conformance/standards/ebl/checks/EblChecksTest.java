package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.standards.ebl.checks.EblChecks.BUYER_AND_SELLER_CONDITIONAL_CHECK;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.COUNTRY_CODE_CONDITIONAL_VALIDATION_POA;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.EBL_AT_MOST_ONE_ORIGINAL_TOTAL;
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
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.SWBS_CANNOT_BE_NEGOTIABLE;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.VALIDATE_DOCUMENT_PARTY;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.VALID_CONSIGNMENT_ITEMS_REFERENCE_TYPES;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.VALID_PARTY_FUNCTION;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.VALID_PARTY_FUNCTION_HBL;
import static org.dcsa.conformance.standards.ebl.checks.EblChecks.VALID_REQUESTED_CARRIER_CLAUSES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceErrorSeverity;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;
import org.dcsa.conformance.standardscommons.party.EblDynamicScenarioParameters;
import org.junit.jupiter.api.Test;

class EblChecksTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final ObjectNode rootNode = objectMapper.createObjectNode();

  @Test
  void testENSManifestTypeRequiresHBLIssued() {
    ArrayNode advanceManifestFilings = rootNode.putArray("advanceManifestFilings");
    advanceManifestFilings.addObject().put("manifestTypeCode", "ENS");
    assertFalse(
        ENS_MANIFEST_TYPE_REQUIRES_HBL_ISSUED.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isHouseBillOfLadingsIssued", true);
    assertTrue(
        ENS_MANIFEST_TYPE_REQUIRES_HBL_ISSUED.validate(rootNode).getErrorMessages().isEmpty());
  }

  @Test
  void testHBLNotifyPartyRequiredIfToOrder() {
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    hbl.put("isToOrder", true);
    hbl.set("documentParties", objectMapper.createObjectNode());
    assertFalse(
        HBL_NOTIFY_PARTY_REQUIRED_IF_TO_ORDER.validate(rootNode).getErrorMessages().isEmpty());

    ((ObjectNode) hbl.get("documentParties")).set("notifyParty", objectMapper.createObjectNode());
    assertTrue(
        HBL_NOTIFY_PARTY_REQUIRED_IF_TO_ORDER.validate(rootNode).getErrorMessages().isEmpty());
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
    assertFalse(
        NUMBER_OF_PACKAGES_CONDITIONAL_CHECK.validate(rootNode).getErrorMessages().isEmpty());

    outerPackaging.put("numberOfPackages", 10);
    assertTrue(
        NUMBER_OF_PACKAGES_CONDITIONAL_CHECK.validate(rootNode).getErrorMessages().isEmpty());
  }

  @Test
  void testIdentificationNumberRequiredIfENSAndSelf() {
    ArrayNode advanceManifestFilings = rootNode.putArray("advanceManifestFilings");
    ObjectNode filing = advanceManifestFilings.addObject();
    filing.put("manifestTypeCode", "ENS");
    filing.put("advanceManifestFilingsHouseBLPerformedBy", "SELF");
    assertFalse(
        IDENTIFICATION_NUMBER_REQUIRED_IF_ENS_AND_SELF
            .validate(rootNode)
            .getErrorMessages()
            .isEmpty());

    filing.put("identificationNumber", "ID12345");
    assertTrue(
        IDENTIFICATION_NUMBER_REQUIRED_IF_ENS_AND_SELF
            .validate(rootNode)
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testSelfFilerCodeRequiredIfAceOrAciAndSelf() {
    ArrayNode advanceManifestFilings = rootNode.putArray("advanceManifestFilings");
    ObjectNode filing = advanceManifestFilings.addObject();
    filing.put("manifestTypeCode", "ACE");
    filing.put("advanceManifestFilingsHouseBLPerformedBy", "SELF");
    assertFalse(
        SELF_FILER_CODE_REQUIRED_IF_ACE_ACI_AND_SELF
            .validate(rootNode)
            .getErrorMessages()
            .isEmpty());

    filing.put("selfFilerCode", "FLXP");
    assertTrue(
        SELF_FILER_CODE_REQUIRED_IF_ACE_ACI_AND_SELF
            .validate(rootNode)
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testLocationNameRequiredIfNoUNLocationCodeInPlaceOfAcceptance() {
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    ObjectNode placeOfAcceptance = hbl.putObject("placeOfAcceptance");
    assertFalse(
        LOCATION_NAME_CONDITIONAL_VALIDATION_POA.validate(rootNode).getErrorMessages().isEmpty());

    placeOfAcceptance.put("locationName", "Amsterdam");
    assertTrue(
        LOCATION_NAME_CONDITIONAL_VALIDATION_POA.validate(rootNode).getErrorMessages().isEmpty());

    placeOfAcceptance.put("UNLocationCode", "NLAMS");
    placeOfAcceptance.remove("locationName");
    var resultWithRelevance =
        (ConformanceCheckResult.ErrorsWithRelevance)
            LOCATION_NAME_CONDITIONAL_VALIDATION_POA.validate(rootNode);
    assertEquals(1, resultWithRelevance.errors().size());
    assertEquals(
        ConformanceErrorSeverity.IRRELEVANT,
        resultWithRelevance.errors().iterator().next().severity());
  }

  @Test
  void testLocationNameRequiredIfNoUNLocationCodeInPlaceOfFinalDelivery() {
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    ObjectNode placeOfFinalDelivery = hbl.putObject("placeOfFinalDelivery");
    assertFalse(
        LOCATION_NAME_CONDITIONAL_VALIDATION_POFD.validate(rootNode).getErrorMessages().isEmpty());

    placeOfFinalDelivery.put("locationName", "Berlin");
    assertTrue(
        LOCATION_NAME_CONDITIONAL_VALIDATION_POFD.validate(rootNode).getErrorMessages().isEmpty());

    placeOfFinalDelivery.put("UNLocationCode", "DEBER");
    placeOfFinalDelivery.remove("locationName");
    var resultWithRelevance =
        (ConformanceCheckResult.ErrorsWithRelevance)
            LOCATION_NAME_CONDITIONAL_VALIDATION_POFD.validate(rootNode);
    assertEquals(1, resultWithRelevance.errors().size());
    assertEquals(
        ConformanceErrorSeverity.IRRELEVANT,
        resultWithRelevance.errors().iterator().next().severity());
  }

  @Test
  void testCountryCodeRequiredIfNoUNLocationCodeInPlaceOfAcceptance() {
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    ObjectNode placeOfAcceptance = hbl.putObject("placeOfAcceptance");
    assertFalse(
        COUNTRY_CODE_CONDITIONAL_VALIDATION_POA.validate(rootNode).getErrorMessages().isEmpty());

    placeOfAcceptance.put("countryCode", "NL");
    assertTrue(
        COUNTRY_CODE_CONDITIONAL_VALIDATION_POA.validate(rootNode).getErrorMessages().isEmpty());

    placeOfAcceptance.put("UNLocationCode", "NLAMS");
    placeOfAcceptance.remove("countryCode");
    var resultWithRelevance =
        (ConformanceCheckResult.ErrorsWithRelevance)
            COUNTRY_CODE_CONDITIONAL_VALIDATION_POA.validate(rootNode);
    assertEquals(1, resultWithRelevance.errors().size());
    assertEquals(
        ConformanceErrorSeverity.IRRELEVANT,
        resultWithRelevance.errors().iterator().next().severity());
  }

  @Test
  void testCountryCodeRequiredIfNoUNLocationCodeInPlaceOfFinalDelivery() {
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    ObjectNode placeOfFinalDelivery = hbl.putObject("placeOfFinalDelivery");
    assertFalse(
        COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD.validate(rootNode).getErrorMessages().isEmpty());

    placeOfFinalDelivery.put("countryCode", "DE");
    assertTrue(
        COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD.validate(rootNode).getErrorMessages().isEmpty());

    placeOfFinalDelivery.put("UNLocationCode", "DEBER");
    placeOfFinalDelivery.remove("countryCode");
    var resultWithRelevance =
        (ConformanceCheckResult.ErrorsWithRelevance)
            COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD.validate(rootNode);
    assertEquals(1, resultWithRelevance.errors().size());
    assertEquals(
        ConformanceErrorSeverity.IRRELEVANT,
        resultWithRelevance.errors().iterator().next().severity());

    placeOfFinalDelivery.remove("UNLocationCode");
    resultWithRelevance =
        (ConformanceCheckResult.ErrorsWithRelevance)
            COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD.validate(rootNode);
    assertEquals(1, resultWithRelevance.errors().size());
    assertEquals(
        ConformanceErrorSeverity.ERROR, resultWithRelevance.errors().iterator().next().severity());
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
    rootNode.put("isHouseBillOfLadingsIssued", false);
    assertFalse(BUYER_AND_SELLER_CONDITIONAL_CHECK.validate(rootNode).getErrorMessages().isEmpty());

    documentParties.putObject("buyer");
    documentParties.putObject("seller");
    assertTrue(BUYER_AND_SELLER_CONDITIONAL_CHECK.validate(rootNode).getErrorMessages().isEmpty());
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
    assertTrue(
        ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK.validate(rootNode).getErrorMessages().isEmpty());
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
    assertFalse(
        ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK.validate(rootNode).getErrorMessages().isEmpty());
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
    assertFalse(
        ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK.validate(rootNode).getErrorMessages().isEmpty());
  }

  @Test
  void testValidRequestedCarrierClauses() {
    ArrayNode requestedCarrierClauses = rootNode.putArray("requestedCarrierClauses");
    requestedCarrierClauses.add("CARGO_CARGOSPECIFICS");
    requestedCarrierClauses.add("VESSELCONVEYANCE_COUNTRYSPECIFIC");

    assertTrue(VALID_REQUESTED_CARRIER_CLAUSES.validate(rootNode).getErrorMessages().isEmpty());

    requestedCarrierClauses.add("CARGO_CARGO");
    assertFalse(VALID_REQUESTED_CARRIER_CLAUSES.validate(rootNode).getErrorMessages().isEmpty());
  }

  @Test
  void testValidConsignmentItemsReferenceTypes() {
    ArrayNode consignmentItems = rootNode.putArray("consignmentItems");
    ObjectNode consignmentItem = consignmentItems.addObject();
    ArrayNode references = consignmentItem.putArray("references");
    references.addObject().put("type", "CR");
    references.addObject().put("type", "SPO");
    assertTrue(
        VALID_CONSIGNMENT_ITEMS_REFERENCE_TYPES.validate(rootNode).getErrorMessages().isEmpty());

    references.addObject().put("type", "CRR");
    assertFalse(
        VALID_CONSIGNMENT_ITEMS_REFERENCE_TYPES.validate(rootNode).getErrorMessages().isEmpty());
  }

  @Test
  void testValidPartyFunction() {
    ObjectNode documentParties = rootNode.putObject("documentParties");
    ArrayNode otherParties = documentParties.putArray("other");
    ObjectNode otherParty = otherParties.addObject();
    otherParty.put("partyFunction", "SCO");
    assertTrue(VALID_PARTY_FUNCTION.validate(rootNode).getErrorMessages().isEmpty());

    otherParty.put("partyFunction", "SSS");
    assertFalse(VALID_PARTY_FUNCTION.validate(rootNode).getErrorMessages().isEmpty());
  }

  @Test
  void testValidPartyFunctionHBL() {
    ArrayNode houseBillOfLadings = rootNode.putArray("houseBillOfLadings");
    ObjectNode hbl = houseBillOfLadings.addObject();
    ObjectNode documentParties = hbl.putObject("documentParties");
    ArrayNode otherParties = documentParties.putArray("other");
    ObjectNode otherParty = otherParties.addObject();
    otherParty.put("partyFunction", "CS");
    assertTrue(VALID_PARTY_FUNCTION_HBL.validate(rootNode).getErrorMessages().isEmpty());

    otherParty.put("partyFunction", "SSS");
    assertFalse(VALID_PARTY_FUNCTION_HBL.validate(rootNode).getErrorMessages().isEmpty());
  }

  @Test
  void testSendToPlatformConditionalCheck() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    assertFalse(SEND_TO_PLATFORM_CONDITIONAL_CHECK.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.putObject("documentParties").putObject("issueTo").put("sendToPlatform", "CARX");
    assertTrue(SEND_TO_PLATFORM_CONDITIONAL_CHECK.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("transportDocumentTypeCode", "SWB");
    assertFalse(SEND_TO_PLATFORM_CONDITIONAL_CHECK.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", false);
    assertFalse(SEND_TO_PLATFORM_CONDITIONAL_CHECK.validate(rootNode).getErrorMessages().isEmpty());
  }

  @Test
  void testEBLSCannotHaveCopiesWithCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    assertTrue(
        EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithCharges", 1);
    assertFalse(
        EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithCharges", 0);
    assertTrue(
        EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithCharges", 1);
    ConformanceCheckResult.ErrorsWithRelevance irrelevantResult =
        (ConformanceCheckResult.ErrorsWithRelevance)
            EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode);
    assertEquals(1, irrelevantResult.errors().size());
    assertFalse(irrelevantResult.isRelevant());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfCopiesWithCharges", 1);
    irrelevantResult =
        (ConformanceCheckResult.ErrorsWithRelevance)
            EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES.validate(rootNode);
    assertEquals(1, irrelevantResult.errors().size());
    assertFalse(irrelevantResult.isRelevant());
  }

  @Test
  void testEBLSCannotHaveCopiesWithoutCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    assertTrue(
        EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithoutCharges", 1);
    assertFalse(
        EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithoutCharges", 0);
    assertTrue(
        EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfCopiesWithoutCharges", 1);
    ConformanceCheckResult.ErrorsWithRelevance irrelevantResult =
        (ConformanceCheckResult.ErrorsWithRelevance)
            EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(1, irrelevantResult.errors().size());
    assertFalse(irrelevantResult.isRelevant());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfCopiesWithoutCharges", 1);
    irrelevantResult =
        (ConformanceCheckResult.ErrorsWithRelevance)
            EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(1, irrelevantResult.errors().size());
    assertFalse(irrelevantResult.isRelevant());
  }

  @Test
  void testSWBsCannotHaveOriginalsWithCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    assertTrue(
        SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "SWB");
    assertTrue(
        SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithCharges", 1);
    assertFalse(
        SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 1);
    ConformanceCheckResult.ErrorsWithRelevance irrelevantResult =
        (ConformanceCheckResult.ErrorsWithRelevance)
            SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES.validate(rootNode);
    assertEquals(1, irrelevantResult.errors().size());
    assertFalse(irrelevantResult.isRelevant());
  }

  @Test
  void testSWBsCannotHaveOriginalsWithoutCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    assertTrue(
        SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "SWB");
    assertTrue(
        SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("numberOfOriginalsWithoutCharges", 1);
    assertFalse(
        SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithoutCharges", 1);
    ConformanceCheckResult.ErrorsWithRelevance irrelevantResult =
        (ConformanceCheckResult.ErrorsWithRelevance)
            SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES.validate(rootNode);
    assertEquals(1, irrelevantResult.errors().size());
    assertFalse(irrelevantResult.isRelevant());
  }

  @Test
  void testSWBsCannotBeNegotiable() {
    // SWB with isToOrder = false should pass
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("isToOrder", false);
    assertTrue(SWBS_CANNOT_BE_NEGOTIABLE.validate(rootNode).getErrorMessages().isEmpty());

    // SWB with isToOrder = true should fail
    rootNode.put("transportDocumentTypeCode", "SWB");
    rootNode.put("isToOrder", true);
    assertFalse(SWBS_CANNOT_BE_NEGOTIABLE.validate(rootNode).getErrorMessages().isEmpty());

    // SWB without isToOrder (defaults to false) should pass
    rootNode.removeAll();
    rootNode.put("transportDocumentTypeCode", "SWB");
    assertTrue(SWBS_CANNOT_BE_NEGOTIABLE.validate(rootNode).getErrorMessages().isEmpty());

    // BOL with isToOrder = true should be irrelevant (not an SWB)
    rootNode.removeAll();
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("isToOrder", true);
    ConformanceCheckResult.ErrorsWithRelevance irrelevantResult =
        (ConformanceCheckResult.ErrorsWithRelevance)
            SWBS_CANNOT_BE_NEGOTIABLE.validate(rootNode);
    assertEquals(1, irrelevantResult.errors().size());
    assertFalse(irrelevantResult.isRelevant());

    // BOL with isToOrder = false should also be irrelevant
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("isToOrder", false);
    irrelevantResult =
        (ConformanceCheckResult.ErrorsWithRelevance)
            SWBS_CANNOT_BE_NEGOTIABLE.validate(rootNode);
    assertEquals(1, irrelevantResult.errors().size());
    assertFalse(irrelevantResult.isRelevant());
  }

  @Test
  void testEBLSCannotHaveMoreThanOneOriginalsWithAndWithoutCharges() {
    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_TOTAL.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 0);
    rootNode.put("numberOfOriginalsWithoutCharges", 0);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_TOTAL.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 1);
    rootNode.put("numberOfOriginalsWithoutCharges", 0);
    assertTrue(EBL_AT_MOST_ONE_ORIGINAL_TOTAL.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", true);
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("numberOfOriginalsWithCharges", 1);
    rootNode.put("numberOfOriginalsWithoutCharges", 1);
    assertFalse(EBL_AT_MOST_ONE_ORIGINAL_TOTAL.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("transportDocumentTypeCode", "SWB");
    assertFalse(EBL_AT_MOST_ONE_ORIGINAL_TOTAL.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put("isElectronic", false);
    rootNode.put("transportDocumentTypeCode", "BOL");
    var resultWithRelevance =
        (ConformanceCheckResult.ErrorsWithRelevance)
            EBL_AT_MOST_ONE_ORIGINAL_TOTAL.validate(rootNode);
    assertEquals(1, resultWithRelevance.errors().size());
    assertEquals(
        ConformanceErrorSeverity.IRRELEVANT,
        resultWithRelevance.errors().iterator().next().severity());
  }

  @Test
  void testValidateDocumentPartyOther() {
    ObjectNode documentParties = rootNode.putObject("documentParties");

    ArrayNode otherParties = documentParties.putArray("other");
    ObjectNode party = otherParties.addObject().putObject("party");
    party.putObject("address").put("street", "Ruijggoordweg");
    party.putArray("identifyingCodes").addObject().put("codeListProvider", "WAVE");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).getErrorMessages().isEmpty());

    party.remove("address");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).getErrorMessages().isEmpty());

    party.remove("address");
    party.remove("identifyingCodes");
    assertFalse(VALIDATE_DOCUMENT_PARTY.validate(rootNode).getErrorMessages().isEmpty());
  }

  @Test
  void testValidateDocumentPartyNotifyParties() {
    ObjectNode documentParties = rootNode.putObject("documentParties");
    ArrayNode notifyParties = documentParties.putArray("notifyParties");
    ObjectNode notifyParty = notifyParties.addObject();
    notifyParty.putObject("address").put("street", "Ruijggoordweg");
    notifyParty.putArray("identifyingCodes").addObject().put("codeListProvider", "WAVE");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).getErrorMessages().isEmpty());

    notifyParty.remove("address");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).getErrorMessages().isEmpty());

    notifyParty.remove("identifyingCodes");
    assertFalse(VALIDATE_DOCUMENT_PARTY.validate(rootNode).getErrorMessages().isEmpty());
  }

  @Test
  void testValidateDocumentPartyBuyerAndSeller() {
    ObjectNode documentParties = rootNode.putObject("documentParties");
    ObjectNode buyer = documentParties.putObject("buyer");
    ConformanceCheckResult.ErrorsWithRelevance irrelevantResult =
        (ConformanceCheckResult.ErrorsWithRelevance) VALIDATE_DOCUMENT_PARTY.validate(rootNode);
    assertEquals(1, irrelevantResult.errors().size());
    assertFalse(irrelevantResult.isRelevant());

    buyer.putObject("address").put("street", "Ruijggoordweg");
    irrelevantResult =
        (ConformanceCheckResult.ErrorsWithRelevance) VALIDATE_DOCUMENT_PARTY.validate(rootNode);
    assertEquals(1, irrelevantResult.errors().size());
    assertFalse(irrelevantResult.isRelevant());

    ObjectNode seller = documentParties.putObject("seller");
    irrelevantResult =
        (ConformanceCheckResult.ErrorsWithRelevance) VALIDATE_DOCUMENT_PARTY.validate(rootNode);
    assertEquals(1, irrelevantResult.errors().size());
    assertFalse(irrelevantResult.isRelevant());

    seller.putObject("address").put("street", "Ruijggoordweg");
    irrelevantResult =
        (ConformanceCheckResult.ErrorsWithRelevance) VALIDATE_DOCUMENT_PARTY.validate(rootNode);
    assertEquals(1, irrelevantResult.errors().size());
    assertFalse(irrelevantResult.isRelevant());
  }

  @Test
  void unscopedDocumentPartiesDoNotMakeTheAddressRuleIrrelevant() {
    ObjectNode documentParties = rootNode.putObject("documentParties");
    documentParties.putObject("issuingParty").putObject("address");
    documentParties.putObject("shipper");

    var result = VALIDATE_DOCUMENT_PARTY.validate(rootNode);

    assertTrue(result.isRelevant());
    assertFalse(result.isConformant());
  }

  @Test
  void testValidateDocumentParty() {
    ObjectNode documentParties = rootNode.putObject("documentParties");
    ObjectNode shipper = documentParties.putObject("shipper");
    assertFalse(VALIDATE_DOCUMENT_PARTY.validate(rootNode).getErrorMessages().isEmpty());

    shipper.putObject("address").put("street", "Ruijggoordweg");
    shipper.putArray("identifyingCodes").addObject().put("codeListProvider", "WAVE");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).getErrorMessages().isEmpty());

    shipper.remove("address");
    assertTrue(VALIDATE_DOCUMENT_PARTY.validate(rootNode).getErrorMessages().isEmpty());
  }

  @Test
  void testFeedbacksPresence() {
    rootNode.put(
        "shippingInstructionsStatus", ShippingInstructionsStatus.SI_PENDING_UPDATE.wireName());
    assertFalse(FEEDBACKS_PRESENCE.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.putArray("feedbacks").addObject().put("code", "INFORMATIONAL_MESSAGE");
    assertTrue(FEEDBACKS_PRESENCE.validate(rootNode).getErrorMessages().isEmpty());

    rootNode.put(
        "updatedShippingInstructionsStatus",
        ShippingInstructionsStatus.SI_UPDATE_RECEIVED.wireName());
    rootNode.remove("feedbacks");

    ConformanceCheckResult.ErrorsWithRelevance result =
        (ConformanceCheckResult.ErrorsWithRelevance) FEEDBACKS_PRESENCE.validate(rootNode);

    assertEquals(1, result.errors().size());
    assertFalse(result.isRelevant());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckOneEquipmentValid() {
    ArrayNode utilizedTransportEquipments = rootNode.putArray("utilizedTransportEquipments");
    utilizedTransportEquipments.addObject();

    assertTrue(
        EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(ScenarioType.REGULAR_2C_1U)
            .validate(rootNode, "")
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckOneEquipmentsTooFew() {
    assertFalse(
        EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(ScenarioType.REGULAR_2C_1U)
            .validate(rootNode, "")
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckOneEquipmentTooMany() {
    ArrayNode utilizedTransportEquipments = rootNode.putArray("utilizedTransportEquipments");
    utilizedTransportEquipments.addObject();
    utilizedTransportEquipments.addObject();

    assertFalse(
        EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(ScenarioType.REGULAR_2C_1U)
            .validate(rootNode, "")
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckTwoEquipmentsValid() {
    ArrayNode utilizedTransportEquipments = rootNode.putArray("utilizedTransportEquipments");
    utilizedTransportEquipments.addObject();
    utilizedTransportEquipments.addObject();

    assertTrue(
        EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(ScenarioType.REGULAR_2C_2U)
            .validate(rootNode, "")
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckTwoEquipmentsTooFew() {
    ArrayNode utilizedTransportEquipments = rootNode.putArray("utilizedTransportEquipments");
    utilizedTransportEquipments.addObject();

    assertFalse(
        EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(ScenarioType.REGULAR_2C_2U)
            .validate(rootNode, "")
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckTwoEquipmentsTooMany() {
    ArrayNode utilizedTransportEquipments = rootNode.putArray("utilizedTransportEquipments");
    utilizedTransportEquipments.addObject();
    utilizedTransportEquipments.addObject();
    utilizedTransportEquipments.addObject();

    assertFalse(
        EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(ScenarioType.REGULAR_2C_2U)
            .validate(rootNode, "")
            .getErrorMessages()
            .isEmpty());
  }

  @Test
  void testUtilizedTransportEquipmentsScenarioSizeCheckNoConstraint() {
    ConformanceCheckResult.ErrorsWithRelevance result =
        (ConformanceCheckResult.ErrorsWithRelevance)
            EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(ScenarioType.ACTIVE_REEFER)
                .validate(rootNode, "");

    assertEquals(1, result.errors().size());
    assertFalse(result.isRelevant());
  }

  @Test
  void testDocumentPartyMayUseAddressLines() {
    ObjectNode documentParties = rootNode.putObject("documentParties");
    ObjectNode onBehalfOfShipper = documentParties.putObject("onBehalfOfShipper");
    onBehalfOfShipper.putArray("addressLines").add("Main Street 1");

    assertValid(EblChecks.VALIDATE_DOCUMENT_PARTY);

    onBehalfOfShipper.remove("addressLines");
    assertInvalid(EblChecks.VALIDATE_DOCUMENT_PARTY);
    onBehalfOfShipper.putArray("identifyingCodes");
    assertInvalid(EblChecks.VALIDATE_DOCUMENT_PARTY);
  }

  @Test
  void testDisplayedAddressLineAndCharacterLimits() {
    rootNode.put("isElectronic", false);
    ArrayNode displayedAddress =
        rootNode.putObject("documentParties").putObject("shipper").putArray("displayedAddress");
    displayedAddress.add("line 1").add("line 2");
    assertValid(EblChecks.DISPLAYED_ADDRESS_LINE_COUNT);
    assertValid(EblChecks.EBL_DISPLAYED_ADDRESS_LIMIT);

    displayedAddress.add("line 3");
    assertInvalid(EblChecks.DISPLAYED_ADDRESS_LINE_COUNT);
    rootNode.put("isElectronic", true);
    displayedAddress.add("line 4").add("line 5").add("line 6");
    assertValid(EblChecks.DISPLAYED_ADDRESS_LINE_COUNT);
    displayedAddress.add("line 7");
    assertInvalid(EblChecks.DISPLAYED_ADDRESS_LINE_COUNT);

    displayedAddress.removeAll().add("x".repeat(36));
    assertInvalid(EblChecks.EBL_DISPLAYED_ADDRESS_LIMIT);
  }

  @Test
  void testDocumentationPartyCodeListProviderIncludes303ValuesAndNotifyPartyPath() {
    ObjectNode identifyingCode =
        rootNode
            .putObject("documentParties")
            .putArray("notifyParties")
            .addObject()
            .putArray("identifyingCodes")
            .addObject();
    identifyingCode.put("codeListProvider", "COVA");
    assertValid(EblChecks.DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS);
    identifyingCode.put("codeListProvider", "UNKNOWN");
    assertInvalid(EblChecks.DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS);
  }

  @Test
  void tdCodeListProviderValidationCoversEveryApplicableTdPartyPath() {
    ObjectNode documentParties = rootNode.putObject("documentParties");
    Map<String, ObjectNode> parties = new LinkedHashMap<>();
    parties.put("shipper", documentParties.putObject("shipper"));
    parties.put("onBehalfOfShipper", documentParties.putObject("onBehalfOfShipper"));
    parties.put("consignee", documentParties.putObject("consignee"));
    parties.put("onBehalfOfConsignee", documentParties.putObject("onBehalfOfConsignee"));
    parties.put("endorsee", documentParties.putObject("endorsee"));
    parties.put("issuingParty", documentParties.putObject("issuingParty"));
    parties.put("notifyParties", documentParties.putArray("notifyParties").addObject());
    parties.put("other", documentParties.putArray("other").addObject().putObject("party"));

    parties.values().forEach(
        party ->
            party
                .putArray("identifyingCodes")
                .addObject()
                .put("codeListProvider", "DCSA"));
    assertValid(EblChecks.TD_DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS);

    for (Map.Entry<String, ObjectNode> entry : parties.entrySet()) {
      ObjectNode party = entry.getValue();
      ((ObjectNode) party.path("identifyingCodes").path(0))
          .put("codeListProvider", "UNKNOWN");
      var result = EblChecks.TD_DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS.validate(rootNode);
      assertFalse(result.getErrorMessages().isEmpty(), entry.getKey());
      ((ObjectNode) party.path("identifyingCodes").path(0)).put("codeListProvider", "DCSA");
    }
  }

  @Test
  void testUpdatedWoodAndNationalCommodityCodeDatasetsAndPaths() {
    ObjectNode cargoItem =
        rootNode
            .putArray("consignmentItems")
            .addObject()
            .putArray("cargoItems")
            .addObject();
    ObjectNode packaging = cargoItem.putObject("outerPackaging");
    packaging.put("woodDeclaration", "TREATED_AND_CERTIFIED");
    cargoItem
        .putArray("extendedNationalCommodityCodes")
        .addObject()
        .put("type", "SCHEDULE_B");
    assertValid(EblChecks.VALID_WOOD_DECLARATIONS);
    assertValid(EblChecks.NATIONAL_COMMODITY_CODE_IS_VALID);

    packaging.put("woodDeclaration", "Treated and certified");
    assertInvalid(EblChecks.VALID_WOOD_DECLARATIONS);
    ((ObjectNode) cargoItem.path("extendedNationalCommodityCodes").path(0))
        .put("type", "OTHER");
    assertInvalid(EblChecks.NATIONAL_COMMODITY_CODE_IS_VALID);
  }

  @Test
  void testPartyEnumsUseLatestDatasetsAndAllPartyPaths() {
    ObjectNode party =
        rootNode
            .putObject("documentParties")
            .putArray("other")
            .addObject()
            .putObject("party");
    party.put("eblPlatform", "SGTD");
    party.put("typeOfPerson", "LEGAL_PERSON");
    assertValid(EblChecks.VALID_EBL_PLATFORMS);
    assertValid(EblChecks.VALID_TYPE_OF_PERSON);

    party.put("eblPlatform", "UNKNOWN");
    party.put("typeOfPerson", "UNKNOWN");
    assertInvalid(EblChecks.VALID_EBL_PLATFORMS);
    assertInvalid(EblChecks.VALID_TYPE_OF_PERSON);

    ObjectNode other = (ObjectNode) rootNode.path("documentParties").path("other").path(0);
    other.put("partyFunction", "SCO");
    assertValid(EblChecks.VALID_TD_PARTY_FUNCTION);
    other.put("partyFunction", "CS");
    assertInvalid(EblChecks.VALID_TD_PARTY_FUNCTION);
  }

  @Test
  void testIssuedDateAndRiderPagesRules() {
    rootNode.put("transportDocumentStatus", "ISSUED");
    assertInvalid(EblChecks.ISSUE_DATE_REQUIRED_WHEN_ISSUED);
    rootNode.put("issueDate", "2026-08-28");
    assertValid(EblChecks.ISSUE_DATE_REQUIRED_WHEN_ISSUED);

    rootNode.put("isElectronic", true).put("numberOfRiderPages", 1);
    assertInvalid(EblChecks.RIDER_PAGES_NOT_ALLOWED_FOR_ELECTRONIC_TD);
    rootNode.remove("numberOfRiderPages");
    assertValid(EblChecks.RIDER_PAGES_NOT_ALLOWED_FOR_ELECTRONIC_TD);
  }

  @Test
  void testVesselVoyageRole() {
    ObjectNode voyage =
        rootNode.putObject("transports").putArray("vesselVoyages").addObject();
    voyage.put("role", "MOTHER");
    assertValid(EblChecks.VALID_VESSEL_VOYAGE_ROLES);
    voyage.put("role", "FEEDER");
    assertInvalid(EblChecks.VALID_VESSEL_VOYAGE_ROLES);
  }

  @Test
  void testNestedInnerPackagingQuantities() {
    ObjectNode innerPackaging =
        rootNode
            .putArray("consignmentItems")
            .addObject()
            .putArray("cargoItems")
            .addObject()
            .putObject("outerPackaging")
            .putArray("dangerousGoods")
            .addObject()
            .putArray("innerPackagings")
            .addObject();
    innerPackaging.put("quantity", 0);
    assertInvalid(EblChecks.VALID_INNER_PACKAGING_QUANTITIES);
    innerPackaging.put("quantity", 1);
    innerPackaging.putArray("innerPackagings").addObject().put("quantity", 2);
    assertValid(EblChecks.VALID_INNER_PACKAGING_QUANTITIES);
  }

  @Test
  void testCargoMeasurementPrecision() throws Exception {
    ObjectNode cargoItem =
        rootNode
            .putArray("consignmentItems")
            .addObject()
            .putArray("cargoItems")
            .addObject();
    cargoItem.set("cargoGrossWeight", objectMapper.readTree("{\"value\":1.234}"));
    cargoItem.set("cargoNetWeight", objectMapper.readTree("{\"value\":1.2345}"));
    cargoItem.set("cargoGrossVolume", objectMapper.readTree("{\"value\":1.2345}"));
    assertInvalid(EblChecks.VALID_CARGO_MEASUREMENT_PRECISION);
    assertValid(EblChecks.VALID_CARGO_VOLUME_PRECISION);

    cargoItem.set("cargoNetWeight", objectMapper.readTree("{\"value\":1.234}"));
    cargoItem.set("cargoGrossVolume", objectMapper.readTree("{\"value\":1.23456}"));
    assertValid(EblChecks.VALID_CARGO_MEASUREMENT_PRECISION);
    assertInvalid(EblChecks.VALID_CARGO_VOLUME_PRECISION);
  }

  @Test
  void workbookReeferRuleOnlyProhibitsSettingsForNonOperatingReefers() {
    JsonContentCheck reeferCheck =
        EblChecks.transportDocumentCarrierContentChecks().stream()
            .filter(
                check ->
                    check.description()
                        .startsWith("For every item in `utilizedTransportEquipments[]`"))
            .findFirst()
            .orElseThrow();
    ObjectNode equipment = rootNode.putArray("utilizedTransportEquipments").addObject();

    equipment.put("isNonOperatingReefer", false);
    var result = reeferCheck.validate(rootNode);
    assertFalse(result.isRelevant());

    equipment.putObject("activeReeferSettings");
    result = reeferCheck.validate(rootNode);
    assertFalse(result.isRelevant());

    equipment.put("isNonOperatingReefer", true);
    result = reeferCheck.validate(rootNode);
    assertTrue(result.isRelevant());
    assertFalse(result.isConformant());

    equipment.remove("activeReeferSettings");
    result = reeferCheck.validate(rootNode);
    assertTrue(result.isRelevant());
    assertTrue(result.isConformant());
  }

  @Test
  void testFeedbackCodeAndSeverity() {
    ObjectNode feedback = rootNode.putArray("feedbacks").addObject();
    feedback.put("severity", "WARN").put("code", "PROPERTY_VALUE_MAY_CHANGE");
    assertValid(EblChecks.VALID_FEEDBACKS_SEVERITY);
    assertValid(EblChecks.VALID_FEEDBACKS_CODE);
    feedback.put("severity", "FATAL").put("code", "UNKNOWN");
    assertInvalid(EblChecks.VALID_FEEDBACKS_SEVERITY);
    assertInvalid(EblChecks.VALID_FEEDBACKS_CODE);
  }

  @Test
  void tdChecksUseExactlyTheWorkbookDescriptions() {
    Set<String> expectedDescriptions =
        Set.of(
            "When `isElectronic` is `true`, no more than one original may be requested. Therefore, the sum of `numberOfOriginalsWithoutCharges` and `numberOfOriginalsWithCharges` cannot be greater than `1`.",
            "When isElectronic is true and transportDocumentTypeCode is BOL, neither copies with charges nor copies without charges are allowed. Therefore, both numberOfCopiesWithCharges and numberOfCopiesWithoutCharges must be 0 or absent.",
            "If transportDocumentTypeCode='SWB', then numberOfOriginalsWithCharges and numberOfOriginalsWithoutCharges must be absent",
            "`transportDocumentStatus` must equal `DRAFT`, `APPROVED`, `ISSUED`, `PENDING_SURRENDER_FOR_AMENDMENT`, `SURRENDERED_FOR_AMENDMENT`, `PENDING_SURRENDER_FOR_DELIVERY`, `SURRENDERED_FOR_DELIVERY`, or `VOIDED`.",
            "If `transportDocumentStatus='ISSUED'`, then `issueDate` must be present",
            "shippedOnBoardDate and receivedForShipmentDate must not both be present.",
            "`declaredValue` and `declaredValueCurrency` must either both be present or both be absent.",
            "If `isElectronic=true`, then `numberOfRiderPages` must not be present.",
            "`cargoMovementTypeAtOrigin` must equal `FCL` or `LCL`.",
            "`cargoMovementTypeAtDestination` must equal `FCL` or `LCL`.",
            "If `preCarriageBy` is present, then `placeOfReceipt` must be present.",
            "If `preCarriageBy` is present, then it must equal `VESSEL`, `RAIL`, `TRUCK`, `BARGE`, or `MULTIMODAL`.",
            "If `onCarriageBy` is present, then it must equal `VESSEL`, `RAIL`, `TRUCK`, `BARGE`, or `MULTIMODAL`.",
            "For every item in `transports.vesselVoyages[]`, `role` must equal `FIRST_SEA_GOING` or `MOTHER`.",
            "If `onCarriageBy` is present, then `placeOfDelivery` must be present.",
            "For each `documentParty`—`shipper`, `consignee`, `endorsee`, `notifyParties`, `other`, `onBehalfOfShipper`—at least one of `address`, `addressLines`, or `identifyingCodes` must be provided.",
            "If isToOrder=false, consignee must be present and endorsee must be absent. If endorsee is present, isToOrder must be true.",
            "Consignee and endorsee must never both be present (mutually exclusive).",
            "If `isToOrder=true`, then at least one notify party must exist: `documentParties.notifyParties` must be present.",
            "For every item in `documentParties.other[]`, `partyFunction` must equal `SCO`, `DDR`, `DDS`, `COW`, or `COX`.",
            "For every document party where `typeOfPerson` is present, it must equal `NATURAL_PERSON`, `LEGAL_PERSON`, or `ASSOCIATION_OF_PERSONS`.",
            "For every document party where `eblPlatform` is present, the value must equal `WAVE`, `CARX`, `ESSD`, `IDT`, `BOLE`, `EDOX`, `IQAX`, `SECR`, `TRGO`, `ETEU`, `TRAC`, `BRIT`, `COVA`, `ETIT`, `KTNE`, `CRED`, `BLOC`, `DOCU`, `AEOT`, or `SGTD`.",
            "If present, `codeListProvider` must equal  `WAVE`, `CARX`, `ESSD`, `IDT`, `BOLE`, `EDOX`, `IQAX`, `SECR`, `TRGO`, `ETEU`, `TRAC`, `BRIT`, `COVA`, `ETIT`, `KTNE`, `CRED`, `BLOC`, `DOCU`, `AEOT`, `SGTD`, `GSBN`, `WISE`, `GLEIF`, `W3C`, `DNB`, `FMC`, `DCSA`, or `ZZZ`.",
            "For every item in a general `references[]` collection, `type` must equal `CR` or `AKG`.",
            "For every item in a consignment-item `references[]` collection, `type` must equal `CR`, `AKG`, `SPO`, or `CPO`.",
            "For every `NationalCommodityCode` object, `type` must equal `NCM`, `HTS`, `SCHEDULE_B`, `TARIC`, `CN`, or `CUS`.",
            "For every `ExtendedNationalCommodityCode` object, `type` must equal `NCM`, `HTS`, `SCHEDULE_B`, `TARIC`, `CN`, or `CUS`.",
            "For every cargo item containing `dangerousGoods`, `imoPackagingCode` or `packageCode` must be present.",
            "If `woodDeclaration` is present, then it must equal `NOT_APPLICABLE`, `NOT_TREATED_AND_NOT_CERTIFIED`, `PROCESSED`, or `TREATED_AND_CERTIFIED`.",
            "If present, `segregationGroups[]` must be an integer from 1 through 18.",
            "If `inhalationZone` is present, then it must equal `A`, `B`, `C`, or `D`",
            "For every `innerPackaging` object, `quantity` must be a positive integer greater than 0.",
            "For every item in `utilizedTransportEquipments[]`, if `isNonOperatingReefer=true`, then `activeReeferSettings` must not be present.",
            "`temperatureSetpoint` and `temperatureUnit` must either both be present or both be absent.",
            "`airExchangeSetpoint` and `airExchangeUnit` must either both be present or both be absent.",
            "Every cargo gross-weight and cargo net-weight value must contain no more than 3 decimal places, and every cargo gross-volume value must contain no more than 4 decimal places.",
            "For every item in `feedbacks[]`, `severity` must equal `INFO`, `WARN`, or `ERROR`.",
            "For every item in `feedbacks[]`, `code` must equal `INFORMATIONAL_MESSAGE`, `PROPERTY_WILL_BE_IGNORED`, `PROPERTY_VALUE_MUST_CHANGE`, `PROPERTY_VALUE_HAS_BEEN_CHANGED`, `PROPERTY_VALUE_MAY_CHANGE`, or `PROPERTY_HAS_BEEN_DELETED`.");

    List<String> actualDescriptions =
        EblChecks.transportDocumentCarrierContentChecks().stream()
            .map(JsonContentCheck::description)
            .toList();

    assertEquals(38, actualDescriptions.size());
    assertEquals(expectedDescriptions, Set.copyOf(actualDescriptions));
  }

  @Test
  void tdNotificationChecksUseWorkbookStatusDescriptionsAndValidateAmendedStatus() {
    List<JsonContentCheck> checks =
        EblChecks.getTdNotificationChecks(List.of(TransportDocumentStatus.TD_DRAFT));
    JsonContentCheck amendedStatusCheck =
        checks.stream()
            .filter(check -> check.description().startsWith("If `amendedTransportDocumentStatus`"))
            .findFirst()
            .orElseThrow();

    assertTrue(
        checks.stream()
            .anyMatch(
                check ->
                    check.description()
                        .equals(
                            "`data.transportDocumentStatus` must equal `DRAFT`, `APPROVED`, `ISSUED`, `PENDING_SURRENDER_FOR_AMENDMENT`, `SURRENDER_FOR_AMENDMENT`, `VOID`, `PENDING_SURRENDER_FOR_DELIVERY`, or `SURRENDER_FOR_DELIVERY`.")));
    rootNode.put("amendedTransportDocumentStatus", "AMENDMENT_RECEIVED");
    assertValid(amendedStatusCheck);
    rootNode.put("amendedTransportDocumentStatus", "UNKNOWN");
    assertInvalid(amendedStatusCheck);
  }

  @Test
  void negotiableTdMayHaveConsigneeWhenEndorseeIsAbsent() {
    rootNode.put("transportDocumentStatus", "DRAFT");
    rootNode.put("isToOrder", true);
    rootNode.putObject("documentParties").putObject("consignee");

    JsonContentCheck consigneeCondition =
        EblChecks.genericTDContentChecks(TransportDocumentStatus.TD_DRAFT, null).stream()
            .filter(check -> check.description().startsWith("If isToOrder=false"))
            .findFirst()
            .orElseThrow();
    JsonContentCheck mutualExclusion =
        EblChecks.genericTDContentChecks(TransportDocumentStatus.TD_DRAFT, null).stream()
            .filter(check -> check.description().startsWith("Consignee and endorsee"))
            .findFirst()
            .orElseThrow();

    assertValid(consigneeCondition);
    assertValid(mutualExclusion);
    rootNode.path("documentParties").withObject("/endorsee");
    assertInvalid(mutualExclusion);
  }

  @Test
  void validatesEveryUseCaseStatusOutcomeFromWorkbook() {
    assertStatusScenario(
        TransportDocumentStatusScenario.uc6(), "DRAFT", null, true);
    assertStatusScenario(
        TransportDocumentStatusScenario.uc7(), "APPROVED", null, true);
    assertStatusScenario(
        TransportDocumentStatusScenario.uc8(), "ISSUED", null, true);

    for (String primaryStatus :
        List.of("DRAFT", "ISSUED", "PENDING_SURRENDER_FOR_AMENDMENT")) {
      assertStatusScenario(
          TransportDocumentStatusScenario.uc17(),
          primaryStatus,
          "AMENDMENT_RECEIVED",
          true);
      assertStatusScenario(
          TransportDocumentStatusScenario.uc18(),
          primaryStatus,
          "AMENDMENT_CANCELLED",
          true);
      assertStatusScenario(
          TransportDocumentStatusScenario.uc19(true),
          primaryStatus,
          "AMENDMENT_CONFIRMED",
          true);
      assertStatusScenario(
          TransportDocumentStatusScenario.uc19(false),
          primaryStatus,
          "AMENDMENT_DECLINED",
          true);
    }

    assertStatusScenario(
        TransportDocumentStatusScenario.uc17(),
        "APPROVED",
        "AMENDMENT_RECEIVED",
        false);
    assertStatusScenario(
        TransportDocumentStatusScenario.uc18(),
        "ISSUED",
        "AMENDMENT_RECEIVED",
        false);
  }

  @Test
  void notificationChecksIncludeAmendedTransportDocumentPayload() {
    var dsp =
        new EblDynamicScenarioParameters(
            ScenarioType.REGULAR_STRAIGHT_BL.name(), null, "TDR-1", null, null, false, false);
    var notificationCheck =
        new CarrierTdNotificationPayloadRequestConformanceCheck(
            UUID.randomUUID(), TransportDocumentStatusScenario.uc17(), true, () -> dsp);

    List<String> titles = notificationCheck.createSubChecks().map(check -> check.getTitle()).toList();

    assertTrue(
        titles.stream()
            .anyMatch(
                title ->
                    title.contains("[Amended Transport Document]")
                        && title.contains("When `isElectronic` is `true`")));
    assertTrue(
        titles.stream()
            .anyMatch(
                title ->
                    title.contains("[Amended Transport Document]")
                        && title.contains(
                            "If present,`segregationGroups[]` must be an integer from 1 through 18.")));
    assertTrue(
        titles.stream()
            .anyMatch(
                title ->
                    title.contains("[Transport Document]")
                        && title.contains(
                            "If `transportDocumentStatus='ISSUED'`, then `issueDate` must be present")));
    assertTrue(
        titles.stream()
            .anyMatch(
                title ->
                    title.contains("[Amended Transport Document]")
                        && title.contains(
                            "If `transportDocumentStatus='ISSUED'`, then `issueDate` must be present")));
    assertTrue(
        titles.stream()
            .anyMatch(title -> title.contains("After UC17, the 'transportDocumentStatus'")));
    assertFalse(
        titles.stream()
            .anyMatch(
                title ->
                    (title.contains("[Transport Document]")
                            || title.contains("[Amended Transport Document]"))
                        && title.contains("After UC17, the 'transportDocumentStatus'")));
  }

  @Test
  void uc6NotificationChecksExcludeAmendedTransportDocumentPayload() {
    var dsp =
        new EblDynamicScenarioParameters(
            ScenarioType.REGULAR_STRAIGHT_BL.name(), null, "TDR-1", null, null, false, false);
    var notificationCheck =
        new CarrierTdNotificationPayloadRequestConformanceCheck(
            UUID.randomUUID(), TransportDocumentStatusScenario.uc6(), true, () -> dsp);

    List<String> titles = notificationCheck.createSubChecks().map(check -> check.getTitle()).toList();

    assertTrue(titles.stream().anyMatch(title -> title.contains("[Transport Document]")));
    assertFalse(titles.stream().anyMatch(title -> title.contains("[Amended Transport Document]")));
  }

  @Test
  void amendedNotificationValidatesEveryWorkbookNationalCommodityCodePath() {
    var dsp =
        new EblDynamicScenarioParameters(
            ScenarioType.REGULAR_STRAIGHT_BL.name(), null, "TDR-1", null, null, false, false);
    JsonContentCheck nationalCommodityCodeCheck =
        EblChecks.getTdPayloadChecks(
                List.of(TransportDocumentStatus.TD_DRAFT),
                () -> dsp,
                EblChecks.TdPayloadContext.AMENDED_TRANSPORT_DOCUMENT)
            .stream()
            .filter(
                check ->
                    check.description()
                        .startsWith("For every `NationalCommodityCode` object"))
            .findFirst()
            .orElseThrow();
    ObjectNode consignmentItem = rootNode.putArray("consignmentItems").addObject();
    consignmentItem
        .putArray("nationalCommodityCodes")
        .addObject()
        .put("type", "UNKNOWN");

    assertInvalid(nationalCommodityCodeCheck);

    ((ObjectNode) consignmentItem.path("nationalCommodityCodes").path(0))
        .put("type", "NCM");
    assertValid(nationalCommodityCodeCheck);

    consignmentItem
        .putArray("cargoItems")
        .addObject()
        .putArray("nationalCommodityCodes")
        .addObject()
        .put("type", "UNKNOWN");
    assertInvalid(nationalCommodityCodeCheck);
  }

  private void assertStatusScenario(
      TransportDocumentStatusScenario scenario,
      String transportDocumentStatus,
      String amendedTransportDocumentStatus,
      boolean expectedValid) {
    rootNode.removeAll();
    rootNode.put("transportDocumentStatus", transportDocumentStatus);
    if (amendedTransportDocumentStatus != null) {
      rootNode.put("amendedTransportDocumentStatus", amendedTransportDocumentStatus);
    }
    boolean valid =
        scenario.checks(false).stream()
            .map(check -> check.validate(rootNode))
            .allMatch(ConformanceCheckResult::isConformant);
    assertEquals(expectedValid, valid);
  }

  private void assertValid(org.dcsa.conformance.core.check.JsonContentCheck check) {
    assertTrue(check.validate(rootNode).getErrorMessages().isEmpty());
  }

  private void assertInvalid(org.dcsa.conformance.core.check.JsonContentCheck check) {
    assertFalse(check.validate(rootNode).getErrorMessages().isEmpty());
  }
}
