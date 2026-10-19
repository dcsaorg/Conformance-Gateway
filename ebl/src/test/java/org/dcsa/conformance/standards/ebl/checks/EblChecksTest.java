package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.ebl.checks.EblCheckTestFixtures.dsp;
import static org.dcsa.conformance.standards.ebl.checks.EblCheckTestFixtures.exchange;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.ConformanceErrorSeverity;
import org.dcsa.conformance.core.check.ConformanceResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonContentMatchedValidation;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.AmendedTransportDocumentStatus;
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
        (ConformanceCheckResult.ErrorsWithRelevance) SWBS_CANNOT_BE_NEGOTIABLE.validate(rootNode);
    assertEquals(1, irrelevantResult.errors().size());
    assertFalse(irrelevantResult.isRelevant());

    // BOL with isToOrder = false should also be irrelevant
    rootNode.put("transportDocumentTypeCode", "BOL");
    rootNode.put("isToOrder", false);
    irrelevantResult =
        (ConformanceCheckResult.ErrorsWithRelevance) SWBS_CANNOT_BE_NEGOTIABLE.validate(rootNode);
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

    parties
        .values()
        .forEach(
            party ->
                party.putArray("identifyingCodes").addObject().put("codeListProvider", "DCSA"));
    assertValid(EblChecks.TD_DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS);

    for (Map.Entry<String, ObjectNode> entry : parties.entrySet()) {
      ObjectNode party = entry.getValue();
      ((ObjectNode) party.path("identifyingCodes").path(0)).put("codeListProvider", "UNKNOWN");
      var result = EblChecks.TD_DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS.validate(rootNode);
      assertFalse(result.getErrorMessages().isEmpty(), entry.getKey());
      ((ObjectNode) party.path("identifyingCodes").path(0)).put("codeListProvider", "DCSA");
    }
  }

  @Test
  void testUpdatedWoodAndNationalCommodityCodeDatasetsAndPaths() {
    ObjectNode cargoItem =
        rootNode.putArray("consignmentItems").addObject().putArray("cargoItems").addObject();
    ObjectNode packaging = cargoItem.putObject("outerPackaging");
    packaging.put("woodDeclaration", "TREATED_AND_CERTIFIED");
    cargoItem.putArray("extendedNationalCommodityCodes").addObject().put("type", "SCHEDULE_B");
    assertValid(EblChecks.VALID_WOOD_DECLARATIONS);
    assertValid(EblChecks.NATIONAL_COMMODITY_CODE_IS_VALID);

    packaging.put("woodDeclaration", "Treated and certified");
    assertInvalid(EblChecks.VALID_WOOD_DECLARATIONS);
    ((ObjectNode) cargoItem.path("extendedNationalCommodityCodes").path(0)).put("type", "OTHER");
    assertInvalid(EblChecks.NATIONAL_COMMODITY_CODE_IS_VALID);
  }

  @Test
  void testPartyEnumsUseLatestDatasetsAndAllPartyPaths() {
    ObjectNode party =
        rootNode.putObject("documentParties").putArray("other").addObject().putObject("party");
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
    ObjectNode voyage = rootNode.putObject("transports").putArray("vesselVoyages").addObject();
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
        rootNode.putArray("consignmentItems").addObject().putArray("cargoItems").addObject();
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
                    check
                        .description()
                        .startsWith("For every item in 'utilizedTransportEquipments[]'"))
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
            "When `isElectronic` is `true`, no more than one original may be requested. Therefore,"
                + " the sum of `numberOfOriginalsWithoutCharges` and `numberOfOriginalsWithCharges`"
                + " cannot be greater than `1`.",
            "When isElectronic is true and transportDocumentTypeCode is BOL, neither copies with"
                + " charges nor copies without charges are allowed. Therefore, both"
                + " numberOfCopiesWithCharges and numberOfCopiesWithoutCharges must be 0 or"
                + " absent.",
            "If transportDocumentTypeCode='SWB', then numberOfOriginalsWithCharges and"
                + " numberOfOriginalsWithoutCharges must be absent",
            "`transportDocumentStatus` must equal `DRAFT`, `APPROVED`, `ISSUED`,"
                + " `PENDING_SURRENDER_FOR_AMENDMENT`, `SURRENDERED_FOR_AMENDMENT`,"
                + " `PENDING_SURRENDER_FOR_DELIVERY`, `SURRENDERED_FOR_DELIVERY`, or `VOIDED`.",
            "If `transportDocumentStatus='ISSUED'`, then `issueDate` must be present",
            "shippedOnBoardDate and receivedForShipmentDate must not both be present.",
            "If `transportDocumentStatus='ISSUED'`, exactly one of `shippedOnBoardDate` and"
                + " `receivedForShipmentDate` must be present.",
            "`declaredValue` and `declaredValueCurrency` must either both be present or both be"
                + " absent.",
            "If `isElectronic=true`, then `numberOfRiderPages` must not be present.",
            "`cargoMovementTypeAtOrigin` must equal `FCL` or `LCL`.",
            "`cargoMovementTypeAtDestination` must equal `FCL` or `LCL`.",
            "If `preCarriageBy` is present, then `placeOfReceipt` must be present.",
            "If `preCarriageBy` is present, then it must equal `VESSEL`, `RAIL`, `TRUCK`, `BARGE`,"
                + " or `MULTIMODAL`.",
            "If `onCarriageBy` is present, then it must equal `VESSEL`, `RAIL`, `TRUCK`, `BARGE`,"
                + " or `MULTIMODAL`.",
            "For every item in `transports.vesselVoyages[]`, `role` must equal `FIRST_SEA_GOING` or"
                + " `MOTHER`.",
            "If `onCarriageBy` is present, then `placeOfDelivery` must be present.",
            "For each `documentParty`—`shipper`, `consignee`, `endorsee`, `notifyParties`, `other`,"
                + " `onBehalfOfShipper`—at least one of `address`, `addressLines`, or"
                + " `identifyingCodes` must be provided.",
            "If isToOrder=false, consignee must be present and endorsee must be absent. If endorsee"
                + " is present, isToOrder must be true.",
            "Consignee and endorsee must never both be present (mutually exclusive).",
            "If `isToOrder=true`, then at least one notify party must exist:"
                + " `documentParties.notifyParties` must be present.",
            "For every item in `documentParties.other[]`, `partyFunction` must equal `SCO`, `DDR`,"
                + " `DDS`, `COW`, or `COX`.",
            "For every document party where `typeOfPerson` is present, it must equal"
                + " `NATURAL_PERSON`, `LEGAL_PERSON`, or `ASSOCIATION_OF_PERSONS`.",
            "For every document party where `eblPlatform` is present, the value must equal `WAVE`,"
                + " `CARX`, `ESSD`, `IDT`, `BOLE`, `EDOX`, `IQAX`, `SECR`, `TRGO`, `ETEU`, `TRAC`,"
                + " `BRIT`, `COVA`, `ETIT`, `KTNE`, `CRED`, `BLOC`, `DOCU`, `AEOT`, or `SGTD`.",
            "If present, `codeListProvider` must equal  `WAVE`, `CARX`, `ESSD`, `IDT`, `BOLE`,"
                + " `EDOX`, `IQAX`, `SECR`, `TRGO`, `ETEU`, `TRAC`, `BRIT`, `COVA`, `ETIT`, `KTNE`,"
                + " `CRED`, `BLOC`, `DOCU`, `AEOT`, `SGTD`, `GSBN`, `WISE`, `GLEIF`, `W3C`, `DNB`,"
                + " `FMC`, `DCSA`, or `ZZZ`.",
            "For every item in a general `references[]` collection, `type` must equal `CR` or"
                + " `AKG`.",
            "For every item in a consignment-item `references[]` collection, `type` must equal"
                + " `CR`, `AKG`, `SPO`, or `CPO`.",
            "For every `NationalCommodityCode` object, `type` must equal `NCM`, `HTS`,"
                + " `SCHEDULE_B`, `TARIC`, `CN`, or `CUS`.",
            "For every `ExtendedNationalCommodityCode` object, `type` must equal `NCM`, `HTS`,"
                + " `SCHEDULE_B`, `TARIC`, `CN`, or `CUS`.",
            "For every cargo item containing `dangerousGoods`, `imoPackagingCode` or `packageCode`"
                + " must be present.",
            "If `woodDeclaration` is present, then it must equal `NOT_APPLICABLE`,"
                + " `NOT_TREATED_AND_NOT_CERTIFIED`, `PROCESSED`, or `TREATED_AND_CERTIFIED`.",
            "If present, `segregationGroups[]` must be an integer from 1 through 18.",
            "If `inhalationZone` is present, then it must equal `A`, `B`, `C`, or `D`",
            "For every `innerPackaging` object, `quantity` must be a positive integer greater than"
                + " 0.",
            "For every item in `utilizedTransportEquipments[]`, if `isNonOperatingReefer=true`,"
                + " then `activeReeferSettings` must not be present.",
            "`temperatureSetpoint` and `temperatureUnit` must either both be present or both be"
                + " absent.",
            "`airExchangeSetpoint` and `airExchangeUnit` must either both be present or both be"
                + " absent.",
            "Every cargo gross-weight and cargo net-weight value must contain no more than 3"
                + " decimal places, and every cargo gross-volume value must contain no more than 4"
                + " decimal places.",
            "For every item in `feedbacks[]`, `severity` must equal `INFO`, `WARN`, or `ERROR`.",
            "For every item in `feedbacks[]`, `code` must equal `INFORMATIONAL_MESSAGE`,"
                + " `PROPERTY_WILL_BE_IGNORED`, `PROPERTY_VALUE_MUST_CHANGE`,"
                + " `PROPERTY_VALUE_HAS_BEEN_CHANGED`, `PROPERTY_VALUE_MAY_CHANGE`, or"
                + " `PROPERTY_HAS_BEEN_DELETED`.");

    Set<String> expectedDisplayDescriptions =
        expectedDescriptions.stream()
            .map(description -> description.replace('`', '\''))
            .collect(Collectors.toSet());
    List<String> actualDescriptions =
        EblChecks.transportDocumentCarrierContentChecks().stream()
            .map(JsonContentCheck::description)
            .toList();

    assertEquals(39, actualDescriptions.size());
    assertEquals(expectedDisplayDescriptions, Set.copyOf(actualDescriptions));
    assertTrue(actualDescriptions.stream().noneMatch(description -> description.contains("`")));
  }

  @Test
  void exactlyOneShipmentDateIsRequiredOnlyWhenIssued() {
    JsonContentCheck shipmentDateCheck =
        EblChecks.transportDocumentCarrierContentChecks().stream()
            .filter(check -> check.description().contains("exactly one of 'shippedOnBoardDate'"))
            .findFirst()
            .orElseThrow();

    JsonContentCheck mutuallyExclusive =
        EblChecks.transportDocumentCarrierContentChecks().stream()
            .filter(check -> check.description().startsWith("shippedOnBoardDate and"))
            .findFirst()
            .orElseThrow();
    for (String status :
        List.of(
            "DRAFT",
            "APPROVED",
            "ISSUED",
            "PENDING_SURRENDER_FOR_AMENDMENT",
            "SURRENDERED_FOR_AMENDMENT",
            "PENDING_SURRENDER_FOR_DELIVERY",
            "SURRENDERED_FOR_DELIVERY",
            "VOIDED")) {
      for (int mask = 0; mask < 4; mask++) {
        rootNode.removeAll();
        rootNode.put("transportDocumentStatus", status);
        if ((mask & 1) != 0) rootNode.put("shippedOnBoardDate", "2026-09-04");
        if ((mask & 2) != 0) rootNode.put("receivedForShipmentDate", "2026-09-03");
        assertEquals(
            !status.equals("ISSUED") || mask != 0,
            shipmentDateCheck.validate(rootNode).isConformant(),
            status + ":" + mask);
        assertEquals(
            mask != 3, mutuallyExclusive.validate(rootNode).isConformant(), status + ":" + mask);
      }
    }
  }

  @Test
  void uc17AndUc18NotificationStatusesMustRemainUnchanged() {
    var dsp =
        new EblDynamicScenarioParameters(
            ScenarioType.REGULAR_STRAIGHT_BL.name(),
            null,
            "TDR-1",
            "DRAFT",
            null,
            null,
            false,
            false);

    for (TransportDocumentStatusScenario scenario :
        List.of(TransportDocumentStatusScenario.uc17(), TransportDocumentStatusScenario.uc18())) {
      JsonContentCheck unchangedStatusCheck =
          EblChecks.getTdNotificationChecks(scenario, () -> dsp).stream()
              .filter(
                  check -> check.description().contains("equal its value from before the action"))
              .findFirst()
              .orElseThrow();
      rootNode.put("transportDocumentStatus", "DRAFT");
      assertValid(unchangedStatusCheck);
      rootNode.put("transportDocumentStatus", "ISSUED");
      assertInvalid(unchangedStatusCheck);
    }
  }

  @Test
  void tdNotificationChecksUseWorkbookStatusDescriptionsAndValidateAmendedStatus() {
    List<JsonContentCheck> checks =
        EblChecks.getTdNotificationChecks(List.of(TransportDocumentStatus.TD_DRAFT));
    JsonContentCheck amendedStatusCheck =
        checks.stream()
            .filter(check -> check.description().startsWith("If 'amendedTransportDocumentStatus'"))
            .findFirst()
            .orElseThrow();

    assertTrue(
        checks.stream()
            .anyMatch(
                check ->
                    check
                        .description()
                        .equals(
                            "'data.transportDocumentStatus' must equal 'DRAFT', 'APPROVED',"
                                + " 'ISSUED', 'PENDING_SURRENDER_FOR_AMENDMENT',"
                                + " 'SURRENDER_FOR_AMENDMENT', 'VOID',"
                                + " 'PENDING_SURRENDER_FOR_DELIVERY', or"
                                + " 'SURRENDER_FOR_DELIVERY'.")));
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
    assertStatusScenario(TransportDocumentStatusScenario.uc6(), "DRAFT", null, true);
    assertStatusScenario(TransportDocumentStatusScenario.uc7(), "APPROVED", null, true);
    assertStatusScenario(TransportDocumentStatusScenario.uc8(), "ISSUED", null, true);

    for (String primaryStatus : List.of("DRAFT", "ISSUED", "PENDING_SURRENDER_FOR_AMENDMENT")) {
      assertStatusScenario(
          TransportDocumentStatusScenario.uc17(), primaryStatus, "AMENDMENT_RECEIVED", true);
      assertStatusScenario(
          TransportDocumentStatusScenario.uc18(), primaryStatus, "AMENDMENT_CANCELLED", true);
      assertStatusScenario(
          TransportDocumentStatusScenario.uc19(true), primaryStatus, "AMENDMENT_CONFIRMED", true);
      assertStatusScenario(
          TransportDocumentStatusScenario.uc19(false), primaryStatus, "AMENDMENT_DECLINED", true);
    }

    assertStatusScenario(
        TransportDocumentStatusScenario.uc17(), "APPROVED", "AMENDMENT_RECEIVED", false);
    assertStatusScenario(
        TransportDocumentStatusScenario.uc18(), "ISSUED", "AMENDMENT_RECEIVED", false);
  }

  @Test
  void notificationChecksIncludeAmendedTransportDocumentPayload() {
    var dsp =
        new EblDynamicScenarioParameters(
            ScenarioType.REGULAR_STRAIGHT_BL.name(),
            null,
            "TDR-1",
            "DRAFT",
            null,
            null,
            false,
            false);
    var notificationCheck =
        new CarrierTdNotificationPayloadRequestConformanceCheck(
            UUID.randomUUID(), TransportDocumentStatusScenario.uc17(), true, () -> dsp);

    List<String> titles =
        notificationCheck.createSubChecks().map(check -> check.getTitle()).toList();

    assertTrue(
        titles.stream()
            .anyMatch(
                title ->
                    title.contains("[Amended Transport Document]")
                        && title.contains("When 'isElectronic' is 'true'")));
    assertTrue(
        titles.stream()
            .anyMatch(
                title ->
                    title.contains("[Amended Transport Document]")
                        && title.contains(
                            "If present, 'segregationGroups[]' must be an integer from 1 through"
                                + " 18.")));
    assertTrue(
        titles.stream()
            .anyMatch(
                title ->
                    title.contains("[Transport Document]")
                        && title.contains(
                            "If 'transportDocumentStatus='ISSUED'', then 'issueDate' must be"
                                + " present")));
    assertTrue(
        titles.stream()
            .anyMatch(
                title ->
                    title.contains("[Amended Transport Document]")
                        && title.contains(
                            "If 'transportDocumentStatus='ISSUED'', then 'issueDate' must be"
                                + " present")));
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
            ScenarioType.REGULAR_STRAIGHT_BL.name(),
            null,
            "TDR-1",
            "DRAFT",
            null,
            null,
            false,
            false);
    var notificationCheck =
        new CarrierTdNotificationPayloadRequestConformanceCheck(
            UUID.randomUUID(), TransportDocumentStatusScenario.uc6(), true, () -> dsp);

    List<String> titles =
        notificationCheck.createSubChecks().map(check -> check.getTitle()).toList();

    assertTrue(titles.stream().anyMatch(title -> title.contains("[Transport Document]")));
    assertFalse(titles.stream().anyMatch(title -> title.contains("[Amended Transport Document]")));
  }

  @Test
  void amendedNotificationValidatesEveryWorkbookNationalCommodityCodePath() {
    var dsp =
        new EblDynamicScenarioParameters(
            ScenarioType.REGULAR_STRAIGHT_BL.name(),
            null,
            "TDR-1",
            "DRAFT",
            null,
            null,
            false,
            false);
    JsonContentCheck nationalCommodityCodeCheck =
        EblChecks.getTdPayloadChecks(
                List.of(TransportDocumentStatus.TD_DRAFT),
                () -> dsp,
                EblChecks.TdPayloadContext.AMENDED_TRANSPORT_DOCUMENT)
            .stream()
            .filter(
                check -> check.description().startsWith("For every 'NationalCommodityCode' object"))
            .findFirst()
            .orElseThrow();
    ObjectNode consignmentItem = rootNode.putArray("consignmentItems").addObject();
    consignmentItem.putArray("nationalCommodityCodes").addObject().put("type", "UNKNOWN");

    assertInvalid(nationalCommodityCodeCheck);

    ((ObjectNode) consignmentItem.path("nationalCommodityCodes").path(0)).put("type", "NCM");
    assertValid(nationalCommodityCodeCheck);

    consignmentItem
        .putArray("cargoItems")
        .addObject()
        .putArray("nationalCommodityCodes")
        .addObject()
        .put("type", "UNKNOWN");
    assertInvalid(nationalCommodityCodeCheck);
  }

  @Test
  void tdWorkbookRulesApplyOnlyToTheirDocumentTypeScopes() {
    List<String> straightBlDescriptions = tdDescriptionsFor(ScenarioType.REGULAR_STRAIGHT_BL);
    List<String> negotiableBlDescriptions = tdDescriptionsFor(ScenarioType.REGULAR_NEGOTIABLE_BL);
    List<String> seaWaybillDescriptions = tdDescriptionsFor(ScenarioType.REGULAR_SWB);

    assertTrue(
        Stream.of(straightBlDescriptions, negotiableBlDescriptions, seaWaybillDescriptions)
            .flatMap(List::stream)
            .noneMatch(description -> description.contains("`")));
    assertTrue(
        Stream.of(straightBlDescriptions, negotiableBlDescriptions, seaWaybillDescriptions)
            .flatMap(List::stream)
            .noneMatch(description -> description.contains(".: Must equal")));
    assertTrue(
        seaWaybillDescriptions.contains("[Scope] 'transportDocumentTypeCode' must equal 'SWB'."));
    assertTrue(
        straightBlDescriptions.stream()
            .anyMatch(description -> description.startsWith("When 'isElectronic' is 'true'")));
    assertFalse(
        seaWaybillDescriptions.stream()
            .anyMatch(description -> description.startsWith("When 'isElectronic' is 'true'")));
    assertFalse(
        straightBlDescriptions.stream()
            .anyMatch(description -> description.startsWith("If 'isToOrder=true'")));
    assertTrue(
        negotiableBlDescriptions.stream()
            .anyMatch(description -> description.startsWith("If 'isToOrder=true'")));
    assertFalse(
        straightBlDescriptions.contains(
            "Consignee and endorsee must never both be present (mutually exclusive)."));
    assertTrue(
        negotiableBlDescriptions.contains(
            "Consignee and endorsee must never both be present (mutually exclusive)."));
  }

  @Test
  void siScopeValidationMessagesMatchScenarioDefinitions() {
    List<String> straightBlDescriptions = siDescriptionsFor(ScenarioType.REGULAR_STRAIGHT_BL);
    List<String> negotiableBlDescriptions = siDescriptionsFor(ScenarioType.REGULAR_NEGOTIABLE_BL);
    List<String> seaWaybillDescriptions = siDescriptionsFor(ScenarioType.REGULAR_SWB);

    assertTrue(
        seaWaybillDescriptions.contains(
            "For Sea Waybill: transportDocumentTypeCode must equal SWB and isToOrder must equal"
                + " false."));
    assertTrue(
        straightBlDescriptions.contains(
            "For Straight B/L: transportDocumentTypeCode must equal BOL and isToOrder must equal"
                + " false."));
    assertTrue(
        negotiableBlDescriptions.contains(
            "For Negotiable B/L: transportDocumentTypeCode must equal BOL and isToOrder must equal"
                + " true."));
  }

  @Test
  void siScopeChecksEnforceTransportDocumentTypeCodeAndIsToOrder() {
    assertSiScenarioScope(ScenarioType.REGULAR_SWB, "SWB", false, true);
    assertSiScenarioScope(ScenarioType.REGULAR_SWB, "SWB", true, false);

    assertSiScenarioScope(ScenarioType.REGULAR_STRAIGHT_BL, "BOL", false, true);
    assertSiScenarioScope(ScenarioType.REGULAR_STRAIGHT_BL, "BOL", true, false);

    assertSiScenarioScope(ScenarioType.REGULAR_NEGOTIABLE_BL, "BOL", true, true);
    assertSiScenarioScope(ScenarioType.REGULAR_NEGOTIABLE_BL, "BOL", false, false);
  }

  private List<String> tdDescriptionsFor(ScenarioType scenarioType) {
    var dsp =
        new EblDynamicScenarioParameters(
            scenarioType.name(), null, "TDR-1", "DRAFT", null, null, false, false);
    return EblChecks.getTdPayloadChecks(List.of(TransportDocumentStatus.TD_DRAFT), () -> dsp)
        .stream()
        .map(JsonContentCheck::description)
        .toList();
  }

  private List<String> siDescriptionsFor(ScenarioType scenarioType) {
    return EblChecks.generateScenarioRelatedChecks(scenarioType, false, false).stream()
        .map(JsonContentCheck::description)
        .toList();
  }

  private void assertSiScenarioScope(
      ScenarioType scenarioType,
      String transportDocumentTypeCode,
      boolean isToOrder,
      boolean expectedValid) {
    rootNode.removeAll();
    rootNode.put("transportDocumentTypeCode", transportDocumentTypeCode);
    rootNode.put("isToOrder", isToOrder);
    boolean valid =
        EblChecks.generateScenarioRelatedChecks(scenarioType, false, false).stream()
            .map(check -> check.validate(rootNode))
            .allMatch(ConformanceCheckResult::isConformant);
    assertEquals(expectedValid, valid);
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

  private static ObjectNode json(String value) throws Exception {
    return (ObjectNode) OBJECT_MAPPER.readTree(value);
  }

  private static List<JsonContentCheck> sharedContentChecks() throws Exception {
    List<JsonContentCheck> checks = new ArrayList<>(EblChecks.STATIC_SI_CHECKS);
    checks.addAll(EblChecks.transportDocumentCarrierContentChecks());
    checks.addAll(
        EblChecks.getSiNotificationChecks(
            ShippingInstructionsStatus.SI_RECEIVED,
            ShippingInstructionsStatus.SI_UPDATE_RECEIVED,
            List.of()));
    checks.addAll(
        List.of(
            EblChecks.NATIONAL_COMMODITY_CODE_IS_VALID,
            EblChecks.DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE,
            EblChecks.VALIDATE_DOCUMENT_PARTIES_MATCH_EBL,
            EblChecks.SIR_OR_TDR_REQUIRED_IN_NOTIFICATION,
            EblChecks.SI_STATUS_CANCELLED_ONLY_CHECK,
            EblChecks.UPDATED_SI_STATUS_UPDATE_CANCELLED_ONLY_CHECK));
    // These legacy checks have no public execution path. Do not enable extra certification
    // requirements or widen production APIs just to exercise them.
    for (String name :
        List.of(
            "TLR_CC_T_COMBINATION_UNIQUE",
            "ISO_EQUIPMENT_CODE_IMPLIES_REEFER",
            "CR_CC_T_CODES_UNIQUE",
            "UTE_EQUIPMENT_REFERENCE_UNIQUE",
            "ADVANCED_MANIFEST_FILING_CODES_UNIQUE")) {
      var field = EblChecks.class.getDeclaredField(name);
      field.setAccessible(true);
      checks.add((JsonContentCheck) field.get(null));
    }
    return checks;
  }

  @Test
  void sharedValidatorsReportMalformedContentWithoutExecutionFailures() throws Exception {
    List<ObjectNode> payloads = new ArrayList<>();
    payloads.add(json("{}"));
    payloads.add(
        json(
            """
            {"isElectronic":true,"transportDocumentTypeCode":"BOL","isToOrder":false,
             "numberOfCopiesWithCharges":0,"numberOfCopiesWithoutCharges":0,
             "numberOfOriginalsWithCharges":0,"numberOfOriginalsWithoutCharges":1,
             "carrierCode":"DCSA","carrierCodeListProvider":"SMDG",
             "advanceManifestFilings":[],"houseBillOfLadings":[],
             "documentParties":{"consignee":{"address":{"street":"Road"}}}}
            """));
    payloads.add(
        json(
            """
            {"isElectronic":false,"transportDocumentTypeCode":"SWB","isToOrder":true,
             "numberOfCopiesWithCharges":5,"numberOfCopiesWithoutCharges":5,
             "numberOfOriginalsWithCharges":2,"numberOfOriginalsWithoutCharges":2,
             "transportDocumentStatus":"ISSUED","shippingInstructionsStatus":"PENDING_UPDATE",
             "documentParties":{"endorsee":{},"ignored":{},"other":[{"party":{}}],"notifyParties":[{}]},
             "advanceManifestFilings":{},"houseBillOfLadings":{}}
            """));
    ObjectNode rich =
        json(
            """
            {"isElectronic":true,"transportDocumentTypeCode":"BOL","isToOrder":true,
             "shippingInstructionsStatus":"PENDING_UPDATE","updatedShippingInstructionsStatus":"UPDATE_RECEIVED",
             "transportDocumentStatus":"DRAFT","isHouseBillOfLadingsIssued":false,
             "feedbacks":[{"severity":"INFO","code":"INFORMATIONAL_MESSAGE"}],
             "issuingParty":{"taxLegalReferences":[{"countryCode":"NL","type":"VAT"}]},
             "references":[{"type":"CR"}],"customsReferences":[{"countryCode":"NL","type":"CR","values":["a"]}],
             "advanceManifestFilings":[{"manifestTypeCode":"ENS","advanceManifestFilingsHouseBLPerformedBy":"CARRIER"}],
             "houseBillOfLadings":[{"isToOrder":false,"isCargoDeliveredInICS2Zone":true,
               "placeOfAcceptance":{"countryCode":"NL"},"placeOfFinalDelivery":{"countryCode":"DE"},
               "routingOfConsignmentCountries":["NL","DE"],"documentParties":{"buyer":{},"seller":{}},
               "consignmentItems":[{"cargoItems":[{"outerPackaging":{}}]}]}],
             "documentParties":{"shipper":{"addressLines":["Road"]},"consignee":{"identifyingCodes":[{"codeListProvider":"DCSA"}]},
               "notifyParties":[{"identifyingCodes":[{"codeListProvider":"DCSA"}]}],
               "other":[{"partyFunction":"SCO","party":{"address":{"street":"Road"}}}]},
             "consignmentItems":[{"descriptionOfGoods":["goods"],"HSCodes":["123456"],"shippingMarks":["mark"],
               "customsReferences":[{"countryCode":"NL","type":"CR","values":["a"]}],
               "nationalCommodityCodes":[{"type":"NCM"}],"extendedNationalCommodityCodes":[{"type":"CN"}],
               "cargoItems":[{"equipmentReference":"EQ","cargoGrossWeight":{"value":1.123},"cargoNetWeight":{"value":1},
                 "cargoGrossVolume":{"value":1.1234},"customsReferences":[{"countryCode":"NL","type":"CR","values":["a"]}],
                 "nationalCommodityCodes":[{"type":"HTS"}],"extendedNationalCommodityCodes":[{"type":"TARIC"}],
                 "outerPackaging":{"packageCode":"SH","dangerousGoods":[{"inhalationZone":"A","segregationGroups":[1],
                   "innerPackagings":[{"quantity":1,"innerPackagings":[{"quantity":2}]}]}]}}]}],
             "utilizedTransportEquipments":[{"equipment":{"equipmentReference":"EQ","ISOEquipmentCode":"22R1"},
               "isNonOperatingReefer":false,"activeReeferSettings":{"temperatureSetpoint":1,"temperatureUnit":"CEL"},
               "customsReferences":[{"countryCode":"NL","type":"CR","values":["a"]}],"seals":[{}],"shippingMarks":["m"]}]}
            """);
    payloads.add(rich);
    for (boolean electronic : List.of(false, true)) {
      for (String type : List.of("SWB", "BOL")) {
        for (boolean toOrder : List.of(false, true)) {
          ObjectNode node =
              rich.deepCopy()
                  .put("isElectronic", electronic)
                  .put("transportDocumentTypeCode", type)
                  .put("isToOrder", toOrder);
          node.withObject("/documentParties").putObject("endorsee");
          node.put("numberOfCopiesWithCharges", 1)
              .put("numberOfCopiesWithoutCharges", 1)
              .put("numberOfOriginalsWithCharges", 1)
              .put("numberOfOriginalsWithoutCharges", 1);
          payloads.add(node);
        }
      }
    }
    for (String provider : List.of("carrierCode", "carrierCodeListProvider")) {
      payloads.add(json("{}").put(provider, "code"));
    }
    for (String status : List.of("PENDING_UPDATE", "RECEIVED")) {
      for (boolean feedback : List.of(false, true)) {
        ObjectNode node = json("{}").put("shippingInstructionsStatus", status);
        if (feedback) node.putArray("feedbacks").addObject().put("severity", "INFO");
        payloads.add(node);
      }
    }
    for (String iso : List.of("22R1", "22H1", "22G1", "", "2")) {
      ObjectNode node = rich.deepCopy();
      node.withObject("/utilizedTransportEquipments/0/equipment").put("ISOEquipmentCode", iso);
      payloads.add(node);
    }
    for (String filing : List.of("ENS", "ACE", "ACI", "OTHER")) {
      for (String owner : List.of("SELF", "CARRIER")) {
        ObjectNode node = rich.deepCopy();
        node.withObject("/advanceManifestFilings/0")
            .put("manifestTypeCode", filing)
            .put("advanceManifestFilingsHouseBLPerformedBy", owner);
        node.withObject("/houseBillOfLadings/0/documentParties").remove("seller");
        payloads.add(node);
      }
    }
    for (int variant = 0; variant < 10; variant++) {
      ObjectNode node = rich.deepCopy();
      ObjectNode hbl = node.withObject("/houseBillOfLadings/0");
      switch (variant) {
        case 0 -> node.remove("advanceManifestFilings");
        case 1 -> node.put("isHouseBillOfLadingsIssued", true);
        case 2 -> hbl.put("isCargoDeliveredInICS2Zone", false);
        case 3 -> hbl.putArray("routingOfConsignmentCountries");
        case 4 -> hbl.putArray("routingOfConsignmentCountries").add("BE");
        case 5 -> hbl.withObject("/placeOfAcceptance").remove("countryCode");
        case 6 -> hbl.withObject("/placeOfFinalDelivery").remove("countryCode");
        case 7 -> hbl.putArray("routingOfConsignmentCountries").add("NL").add("BE");
        case 8 -> hbl.withObject("/documentParties").remove("buyer");
        case 9 ->
            hbl.withObject("/consignmentItems/0/cargoItems/0/outerPackaging")
                .put("packageCode", "VY");
      }
      payloads.add(node);
    }
    ObjectNode duplicateEquipment = rich.deepCopy();
    duplicateEquipment
        .withArray("/utilizedTransportEquipments")
        .addObject()
        .put("equipmentReference", "EQ");
    duplicateEquipment.withArray("/utilizedTransportEquipments").addObject();
    duplicateEquipment.withArray("/consignmentItems/0/cargoItems").addObject();
    payloads.add(duplicateEquipment);
    for (JsonContentCheck check : sharedContentChecks()) {
      for (ObjectNode payload : payloads) {
        payload.putIfAbsent(
            "transportDocumentStatus", OBJECT_MAPPER.getNodeFactory().textNode("DRAFT"));
        var exchange = exchange(payload, payload);
        var wrapped =
            JsonAttribute.contentChecks(
                role -> true, exchange.getUuid(), HttpMessageType.REQUEST, "3.0.0", List.of(check));
        wrapped.check(id -> exchange);
        assertFalse(
            wrapped
                .subChecksStream()
                .flatMap(ConformanceCheck::resultsStream)
                .flatMap(result -> result.getErrors().stream())
                .anyMatch(error -> error.startsWith("Failed to perform")),
            check.description());
      }
    }
    ObjectNode restored = rich.deepCopy();
    EblChecks.SI_ARRAY_ORDER_DEFINITIONS.accept(restored, ArrayOrderHelper::restoreArrayOrder);
    assertEquals(rich, restored);
    EblChecks.SI_ARRAY_ORDER_DEFINITIONS.accept(restored, ArrayOrderHelper::shuffleArrayOrder);
    assertEquals(rich, restored);
  }

  @Test
  void innerPackagingAndMeasurementBoundariesAreValidated() throws Exception {
    for (String quantity : List.of("1", "0", "-1", "1.5", "\"invalid\"")) {
      ObjectNode node =
          json(
              "{\"consignmentItems\":[{\"cargoItems\":[{\"outerPackaging\":{\"dangerousGoods\":[{\"innerPackagings\":[{\"quantity\":"
                  + quantity
                  + "}]}]}}]}]}");
      assertEquals(
          quantity.equals("1"),
          EblChecks.VALID_INNER_PACKAGING_QUANTITIES.validate(node).isConformant());
    }
    for (String value : List.of("1", "1.123", "1.1234", "1.12345", "\"bad\"")) {
      ObjectNode node =
          json(
              "{\"consignmentItems\":[{\"cargoItems\":[{\"cargoGrossWeight\":{\"value\":"
                  + value
                  + "},\"cargoGrossVolume\":{\"value\":"
                  + value
                  + "}}]}]}");
      assertEquals(
          Set.of("1", "1.123").contains(value),
          EblChecks.VALID_CARGO_MEASUREMENT_PRECISION.validate(node).isConformant());
      assertEquals(
          Set.of("1", "1.123", "1.1234").contains(value),
          EblChecks.VALID_CARGO_VOLUME_PRECISION.validate(node).isConformant());
    }
  }

  @Test
  void publicCheckBuildersEnforceScopeSizesAndDocumentStatus() throws Exception {
    for (ScenarioType type : ScenarioType.values()) {
      var dsp = dsp(type.name());
      ObjectNode node =
          json("{}")
              .put("transportDocumentTypeCode", type.transportDocumentTypeCode())
              .put("isToOrder", type.isToOrder())
              .put("transportDocumentStatus", "DRAFT")
              .put("shippingInstructionsStatus", "RECEIVED")
              .put("transportDocumentReference", "TDR");
      node.withObject("/documentParties/consignee/address").put("street", "Road");
      if (type.isToOrder()) {
        node.withArray("/documentParties/notifyParties")
            .addObject()
            .putObject("address")
            .put("street", "Road");
      }
      for (boolean isTd : List.of(false, true)) {
        for (var check : EblChecks.generateScenarioRelatedChecks(type, isTd, false)) {
          assertTrue(check.validate(node).isConformant(), check.description());
        }
      }
      EblChecks.transportDocumentCarrierContentChecks(type)
          .forEach(c -> assertTrue(c.validate(node).isConformant(), c.description()));
      EblChecks.amendedTransportDocumentCarrierContentChecks(() -> dsp)
          .forEach(c -> assertTrue(c.validate(node).isConformant(), c.description()));
      boolean requiresTwoConsignments =
          type == ScenarioType.REGULAR_2C_1U || type == ScenarioType.REGULAR_2C_2U;
      assertEquals(
          !requiresTwoConsignments,
          EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(type)
              .validate(node, "root")
              .isConformant());
      assertEquals(
          !requiresTwoConsignments,
          EblChecks.consignmentItemsScenarioSizeCheck(type).validate(node, "root").isConformant());
      node.putArray("consignmentItems").addObject().putArray("cargoItems").addObject();
      node.withArray("/consignmentItems").addObject().putArray("cargoItems").addObject();
      node.putArray("utilizedTransportEquipments").addObject();
      if (type == ScenarioType.REGULAR_2C_2U)
        node.withArray("/utilizedTransportEquipments").addObject();
      assertTrue(
          EblChecks.utilizedTransportEquipmentsScenarioSizeCheck(type)
              .validate(node, "root")
              .isConformant());
      assertTrue(
          EblChecks.consignmentItemsScenarioSizeCheck(type).validate(node, "root").isConformant());
      node.put("amendedTransportDocumentStatus", "AMENDMENT_CANCELLED");
      var exchange = exchange(node, node);
      List<ConformanceCheck> checks =
          List.of(
              EblChecks.siRequestContentChecks(exchange.getUuid(), "3.0.0", type),
              EblChecks.siResponseContentChecks(
                  exchange.getUuid(),
                  "3.0.0",
                  ShippingInstructionsStatus.SI_RECEIVED,
                  ShippingInstructionsStatus.SI_ANY,
                  () -> dsp),
              EblChecks.tdPlusScenarioContentChecks(
                  exchange.getUuid(),
                  "3.0.0",
                  List.of(TransportDocumentStatus.TD_DRAFT),
                  () -> dsp),
              EblChecks.shipperAmendmentContentChecks(exchange.getUuid(), "3.0.0", () -> dsp),
              EblChecks.shipperApprovalContentChecks(exchange.getUuid(), "3.0.0"),
              EblChecks.amendedTransportDocumentStatusChecks(
                  exchange.getUuid(), "3.0.0", AmendedTransportDocumentStatus.AMENDMENT_RECEIVED));
      // Empty cargo-item structures are deliberately invalid for non-DG SI scenarios.
      List<Boolean> expected =
          List.of(type == ScenarioType.DG, type == ScenarioType.DG, true, true, false, false);
      for (int i = 0; i < checks.size(); i++) {
        var check = checks.get(i);
        check.check(id -> exchange);
        assertTrue(contentResults(check).findAny().isPresent(), check.getTitle());
        assertEquals(
            expected.get(i),
            contentResults(check).allMatch(ConformanceResult::isConformant),
            type + ": builder " + i + ": " + check.getTitle());
      }
    }
    var noScope = dsp(null).withTransportDocumentReference(null);
    ObjectNode node = json("{}").put("transportDocumentStatus", "DRAFT");
    node.withObject("/documentParties/consignee/address").put("street", "Road");
    var exchange = exchange(node, node);
    var amendment =
        EblChecks.shipperAmendmentContentChecks(exchange.getUuid(), "3.0.0", () -> noScope);
    amendment.check(id -> exchange);
    assertTrue(contentResults(amendment).allMatch(ConformanceResult::isConformant));
    EblChecks.amendedTransportDocumentCarrierContentChecks(() -> noScope)
        .forEach(c -> assertTrue(c.validate(node).isConformant(), c.description()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            EblChecks.tdPlusScenarioContentChecks(
                exchange.getUuid(),
                "3.0.0",
                List.of(TransportDocumentStatus.TD_ANY),
                () -> noScope));
    var genericChecks =
        EblChecks.genericTDContentChecks(TransportDocumentStatus.TD_DRAFT, () -> "TDR");
    assertFalse(genericChecks.stream().allMatch(c -> c.validate(node).isConformant()));
    node.put("transportDocumentReference", "TDR");
    genericChecks.forEach(c -> assertTrue(c.validate(node).isConformant(), c.description()));
    assertFalse(
        EblChecks.sirInNotificationMustMatchDSP(() -> dsp(null)).validate(node).isConformant());
    assertThrows(
        IllegalArgumentException.class,
        () -> TransportDocumentStatusScenario.primaryStatusesOnly(Set.of()));
  }

  private static Stream<ConformanceResult> contentResults(ConformanceCheck check) {
    return Stream.concat(
        check.resultsStream(), check.subChecksStream().flatMap(EblChecksTest::contentResults));
  }

  @Test
  void packagingScopeAndOptionalDataBoundaries() throws Exception {
    JsonContentCheck packaging =
        EblChecks.transportDocumentCarrierContentChecks().stream()
            .filter(c -> c.description().startsWith("For every cargo item containing"))
            .findFirst()
            .orElseThrow();
    for (String content :
        List.of(
            "{}",
            "{\"dangerousGoods\":[]}",
            "{\"dangerousGoods\":[{}]}",
            "{\"dangerousGoods\":[{}],\"packageCode\":\"SH\"}",
            "{\"dangerousGoods\":[{}],\"imoPackagingCode\":\"1A1\"}")) {
      ObjectNode node =
          json("{\"consignmentItems\":[{\"cargoItems\":[{\"outerPackaging\":" + content + "}]}]}");
      assertEquals(
          !content.equals("{\"dangerousGoods\":[{}]}"), packaging.validate(node).isConformant());
    }
    for (ScenarioType type : List.of(ScenarioType.REGULAR_SWB, ScenarioType.DG)) {
      ObjectNode node = json("{\"consignmentItems\":[{\"cargoItems\":[{}]}]}");
      for (boolean isTd : List.of(false, true)) {
        JsonContentCheck outer = EblChecks.generateScenarioRelatedChecks(type, isTd, false).get(1);
        assertEquals(isTd || type == ScenarioType.DG, outer.validate(node).isConformant());
      }
    }
    JsonContentCheck scope =
        EblChecks.transportDocumentCarrierContentChecks(ScenarioType.REGULAR_SWB).stream()
            .filter(c -> c.description().startsWith("[Scope] 'isToOrder'"))
            .findFirst()
            .orElseThrow();
    assertFalse(scope.validate(json("{\"isToOrder\":true}")).isConformant());
    assertFalse(scope.validate(json("{\"isToOrder\":\"false\"}")).isConformant());
    assertTrue(scope.validate(json("{\"isToOrder\":false}")).isConformant());
    assertFalse(
        EblChecks.FEEDBACKS_PRESENCE
            .validate(json("{\"shippingInstructionsStatus\":\"PENDING_UPDATE\",\"feedbacks\":[]}"))
            .isConformant());
    assertTrue(
        EblChecks.BUYER_AND_SELLER_CONDITIONAL_CHECK
            .validate(json("{\"houseBillOfLadings\":[],\"advanceManifestFilings\":{}}"))
            .isConformant());
    assertTrue(
        EblChecks.ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK
            .validate(
                json(
                    "{\"houseBillOfLadings\":[{\"placeOfAcceptance\":{\"countryCode\":\"NL\"},\"placeOfFinalDelivery\":{\"countryCode\":\"DE\"},\"routingOfConsignmentCountries\":[\"NL\"]}]}"))
            .isConformant());
    JsonContentCheck equipment =
        EblChecks.STATIC_SI_CHECKS.stream()
            .filter(c -> c.description().startsWith("Equipment References in 'cargoItems'"))
            .findFirst()
            .orElseThrow();
    assertFalse(
        equipment
            .validate(
                json(
                    "{\"consignmentItems\":[{\"cargoItems\":[{\"equipmentReference\":\"UNKNOWN\"}]}]}"))
            .isConformant());
    assertFalse(
        EblChecks.getTdNotificationChecks(TransportDocumentStatusScenario.uc17()).isEmpty());
  }

  @Test
  void legacyReferenceHelpersRecognizeOnlyNonemptyReferences() throws Exception {
    // No public callers remain for these helpers. Preserve their tests without adding
    // requirements to active SI/TD flows or changing their production visibility.
    var customs =
        EblChecks.class.getDeclaredMethod("scenarioCustomsReferencesCheck", ScenarioType.class);
    var references =
        EblChecks.class.getDeclaredMethod("scenarioReferencesCheck", ScenarioType.class);
    customs.setAccessible(true);
    references.setAccessible(true);
    for (ScenarioType type :
        List.of(ScenarioType.REGULAR_SWB, ScenarioType.REGULAR_SWB_SOC_AND_REFERENCES)) {
      JsonContentMatchedValidation customsCheck =
          (JsonContentMatchedValidation) customs.invoke(null, type);
      JsonContentMatchedValidation refsCheck =
          (JsonContentMatchedValidation) references.invoke(null, type);
      ObjectNode node =
          json(
              "{\"utilizedTransportEquipments\":[{}],\"consignmentItems\":[{}],\"documentParties\":{\"notifyParties\":[{}],\"other\":[{}]}}");
      assertEquals(
          type == ScenarioType.REGULAR_SWB, customsCheck.validate(node, "root").isConformant());
      assertEquals(
          type == ScenarioType.REGULAR_SWB, refsCheck.validate(node, "root").isConformant());
      for (String value : List.of("[]", "\"\"", "\" \"", "\"ref\"", "1", "{}", "[{}]")) {
        node.set("customsReferences", OBJECT_MAPPER.readTree(value));
        assertEquals(
            type == ScenarioType.REGULAR_SWB || Set.of("\"ref\"", "1", "[{}]").contains(value),
            customsCheck.validate(node, "root").isConformant());
      }
      node.putArray("references").addObject().put("type", "CR");
      assertTrue(refsCheck.validate(node, "root").isConformant());
    }
    var nonempty = EblChecks.class.getDeclaredMethod("isNonEmptyNode", JsonNode.class);
    nonempty.setAccessible(true);
    assertEquals(false, nonempty.invoke(null, (Object) null));
    assertEquals(false, nonempty.invoke(null, OBJECT_MAPPER.missingNode()));
  }

  private record EnumerationRule(String label, String pointer, List<String> values) {}

  private static final String CARGO = "/consignmentItems/0/cargoItems/0";
  private static final String PACKAGING = CARGO + "/outerPackaging";

  private static ObjectNode workbookPayload() throws Exception {
    return json(
        """
        {"transportDocumentStatus":"DRAFT","transportDocumentTypeCode":"BOL","isToOrder":false,
         "documentParties":{"shipper":{},"consignee":{},"endorsee":{},"onBehalfOfShipper":{},
           "notifyParties":[{}],"other":[{"partyFunction":"SCO","party":{}}]},
         "references":[{}],"feedbacks":[{}],"transports":{"vesselVoyages":[{}]},
         "consignmentItems":[{"references":[{}],"nationalCommodityCodes":[{}],"extendedNationalCommodityCodes":[{}],
           "cargoItems":[{"nationalCommodityCodes":[{}],"extendedNationalCommodityCodes":[{}],
             "outerPackaging":{"dangerousGoods":[{"segregationGroups":[1],"innerPackagings":[{}]}]}}]}],
         "utilizedTransportEquipments":[{"activeReeferSettings":{}}]}
        """);
  }

  private static JsonContentCheck workbookRule(String prefix) {
    return EblChecks.transportDocumentCarrierContentChecks().stream()
        .filter(check -> check.description().startsWith(prefix))
        .findFirst()
        .orElseThrow();
  }

  private static void setWorkbookField(ObjectNode node, String pointer, JsonNode value) {
    JsonPointer path = JsonPointer.compile(pointer);
    JsonNode parent = node.at(path.head());
    if (parent.isArray()) {
      ((ArrayNode) parent).set(Integer.parseInt(path.last().getMatchingProperty()), value);
    } else {
      ((ObjectNode) parent).set(path.last().getMatchingProperty(), value);
    }
  }

  @Test
  void workbookEnumerationsAcceptEveryDocumentedValueAndRejectUnknownValues() throws Exception {
    String platforms =
        "WAVE CARX ESSD IDT BOLE EDOX IQAX SECR TRGO ETEU TRAC BRIT COVA ETIT KTNE CRED BLOC DOCU"
            + " AEOT SGTD";
    List<EnumerationRule> cases =
        new ArrayList<>(
            List.of(
                new EnumerationRule(
                    "'transportDocumentStatus' must equal",
                    "/transportDocumentStatus",
                    List.of(
                        "DRAFT",
                        "APPROVED",
                        "ISSUED",
                        "PENDING_SURRENDER_FOR_AMENDMENT",
                        "SURRENDERED_FOR_AMENDMENT",
                        "PENDING_SURRENDER_FOR_DELIVERY",
                        "SURRENDERED_FOR_DELIVERY",
                        "VOIDED")),
                new EnumerationRule(
                    "'cargoMovementTypeAtOrigin'",
                    "/cargoMovementTypeAtOrigin",
                    List.of("FCL", "LCL")),
                new EnumerationRule(
                    "'cargoMovementTypeAtDestination'",
                    "/cargoMovementTypeAtDestination",
                    List.of("FCL", "LCL")),
                new EnumerationRule(
                    "If 'preCarriageBy' is present, then it must equal",
                    "/transports/preCarriageBy",
                    List.of("VESSEL", "RAIL", "TRUCK", "BARGE", "MULTIMODAL")),
                new EnumerationRule(
                    "If 'onCarriageBy' is present, then it must equal",
                    "/transports/onCarriageBy",
                    List.of("VESSEL", "RAIL", "TRUCK", "BARGE", "MULTIMODAL")),
                new EnumerationRule(
                    "For every item in 'transports.vesselVoyages[]'",
                    "/transports/vesselVoyages/0/role",
                    List.of("FIRST_SEA_GOING", "MOTHER")),
                new EnumerationRule(
                    "For every item in 'documentParties.other[]'",
                    "/documentParties/other/0/partyFunction",
                    List.of("SCO", "DDR", "DDS", "COW", "COX")),
                new EnumerationRule(
                    "For every item in a general", "/references/0/type", List.of("CR", "AKG")),
                new EnumerationRule(
                    "For every item in a consignment-item",
                    "/consignmentItems/0/references/0/type",
                    List.of("CR", "AKG", "SPO", "CPO")),
                new EnumerationRule(
                    "If 'woodDeclaration'",
                    PACKAGING + "/woodDeclaration",
                    List.of(
                        "NOT_APPLICABLE",
                        "NOT_TREATED_AND_NOT_CERTIFIED",
                        "PROCESSED",
                        "TREATED_AND_CERTIFIED")),
                new EnumerationRule(
                    "If 'inhalationZone'",
                    PACKAGING + "/dangerousGoods/0/inhalationZone",
                    List.of("A", "B", "C", "D")),
                new EnumerationRule(
                    "For every item in 'feedbacks[]', 'severity'",
                    "/feedbacks/0/severity",
                    List.of("INFO", "WARN", "ERROR")),
                new EnumerationRule(
                    "For every item in 'feedbacks[]', 'code'",
                    "/feedbacks/0/code",
                    List.of(
                        "INFORMATIONAL_MESSAGE",
                        "PROPERTY_WILL_BE_IGNORED",
                        "PROPERTY_VALUE_MUST_CHANGE",
                        "PROPERTY_VALUE_HAS_BEEN_CHANGED",
                        "PROPERTY_VALUE_MAY_CHANGE",
                        "PROPERTY_HAS_BEEN_DELETED"))));
    for (String party : List.of("shipper", "consignee", "other/0/party")) {
      cases.add(
          new EnumerationRule(
              "For every document party where 'typeOfPerson'",
              "/documentParties/" + party + "/typeOfPerson",
              List.of("NATURAL_PERSON", "LEGAL_PERSON", "ASSOCIATION_OF_PERSONS")));
    }
    for (String party : List.of("shipper", "consignee", "endorsee")) {
      cases.add(
          new EnumerationRule(
              "For every document party where 'eblPlatform'",
              "/documentParties/" + party + "/eblPlatform",
              List.of(platforms.split(" "))));
    }
    for (String location : List.of("/consignmentItems/0", CARGO)) {
      for (String collection :
          List.of("nationalCommodityCodes", "extendedNationalCommodityCodes")) {
        cases.add(
            new EnumerationRule(
                collection.startsWith("extended")
                    ? "For every 'ExtendedNationalCommodityCode'"
                    : "For every 'NationalCommodityCode'",
                location + "/" + collection + "/0/type",
                List.of("NCM", "HTS", "SCHEDULE_B", "TARIC", "CN", "CUS")));
      }
    }
    for (EnumerationRule test : cases) {
      JsonContentCheck check = workbookRule(test.label());
      for (String value : test.values()) {
        ObjectNode node = workbookPayload();
        setWorkbookField(node, test.pointer(), OBJECT_MAPPER.getNodeFactory().textNode(value));
        assertTrue(check.validate(node).isConformant(), test.pointer() + "=" + value);
      }
      ObjectNode node = workbookPayload();
      setWorkbookField(node, test.pointer(), OBJECT_MAPPER.getNodeFactory().textNode("UNKNOWN"));
      assertFalse(check.validate(node).isConformant(), test.pointer());
    }
  }

  @Test
  void allPartyAddressAlternativesAreAcceptedIndividually() {
    JsonContentCheck check = workbookRule("For each 'documentParty'");
    for (String party :
        List.of(
            "shipper",
            "consignee",
            "endorsee",
            "onBehalfOfShipper",
            "notifyParties/0",
            "other/0/party")) {
      for (int fields = 0; fields < 8; fields++) {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        ObjectNode target;
        if (party.equals("notifyParties/0")) {
          target = node.putObject("documentParties").putArray("notifyParties").addObject();
        } else if (party.equals("other/0/party")) {
          target =
              node.putObject("documentParties").putArray("other").addObject().putObject("party");
        } else {
          target = node.putObject("documentParties").putObject(party);
        }
        if ((fields & 1) != 0) target.putObject("address").put("street", "Road");
        if ((fields & 2) != 0) target.putArray("addressLines").add("Road");
        if ((fields & 4) != 0) target.putArray("identifyingCodes").addObject().put("code", "PARTY");
        assertEquals(fields != 0, check.validate(node).isConformant(), party + ":" + fields);
      }
    }
  }

  @Test
  void pairedFieldsAndElectronicOriginalRestrictionsMatchEveryCombination() throws Exception {
    for (String[] pair :
        List.of(
            new String[] {"declaredValue", "declaredValueCurrency"},
            new String[] {"temperatureSetpoint", "temperatureUnit"},
            new String[] {"airExchangeSetpoint", "airExchangeUnit"})) {
      JsonContentCheck check = workbookRule("'" + pair[0] + "' and");
      for (int mask = 0; mask < 4; mask++) {
        ObjectNode node = workbookPayload();
        ObjectNode parent =
            pair[0].equals("declaredValue")
                ? node
                : node.withObject("/utilizedTransportEquipments/0/activeReeferSettings");
        if ((mask & 1) != 0) parent.put(pair[0], 1);
        if ((mask & 2) != 0) parent.put(pair[1], "unit");
        assertEquals(
            mask == 0 || mask == 3, check.validate(node).isConformant(), pair[0] + ":" + mask);
      }
    }
    JsonContentCheck originals = workbookRule("When 'isElectronic' is 'true'");
    JsonContentCheck copies = workbookRule("When isElectronic is true");
    for (int with = 0; with <= 2; with++) {
      for (int without = 0; without <= 2; without++) {
        ObjectNode node =
            workbookPayload()
                .put("isElectronic", true)
                .put("numberOfOriginalsWithCharges", with)
                .put("numberOfOriginalsWithoutCharges", without)
                .put("numberOfCopiesWithCharges", with)
                .put("numberOfCopiesWithoutCharges", without);
        assertEquals(with + without <= 1, originals.validate(node).isConformant());
        assertEquals(with == 0 && without == 0, copies.validate(node).isConformant());
      }
    }
    JsonContentCheck segregation = workbookRule("If present, 'segregationGroups[]'");
    for (int group = 0; group <= 19; group++) {
      ObjectNode node = workbookPayload();
      setWorkbookField(
          node,
          PACKAGING + "/dangerousGoods/0/segregationGroups/0",
          OBJECT_MAPPER.getNodeFactory().numberNode(group));
      assertEquals(group >= 1 && group <= 18, segregation.validate(node).isConformant());
    }
  }
}
