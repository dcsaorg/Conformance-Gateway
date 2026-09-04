package org.dcsa.conformance.standards.an.checks;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.KeywordDataset;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.an.party.ANRole;
import org.dcsa.conformance.standards.an.party.DynamicScenarioParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class ANChecks {

  private static final String NON_EMPTY_ARRIVAL_NOTICES_DESCRIPTION =
    "At least one Arrival Notice must be included in the message's 'arrivalNotices' list.";

  private static final String ARRIVAL_NOTICES = "arrivalNotices";
  private static final String TRANSPORT_DOCUMENT_REFERENCE = "transportDocumentReference";
  private static final String CARRIER_CODE = "carrierCode";
  private static final String CARRIER_CODE_LIST_PROVIDER = "carrierCodeListProvider";
  private static final String CARRIER_CONTACT_INFORMATION = "carrierContactInformation";
  private static final String NAME = "name";
  private static final String PHONE = "phone";
  private static final String EMAIL = "email";
  private static final String DELIVERY_TYPE_AT_DESTINATION = "deliveryTypeAtDestination";
  private static final String DOCUMENT_PARTIES = "documentParties";
  private static final String PARTY_FUNCTION = "partyFunction";
  private static final String PARTY_NAME = "partyName";
  private static final String PARTY_CONTACT_DETAILS = "partyContactDetails";
  private static final String ADDRESS = "address";
  private static final String TRANSPORT = "transport";
  private static final String PORT_OF_DISCHARGE_ARRIVAL_DATE = "portOfDischargeArrivalDate";
  private static final String PLACE_OF_DELIVERY_ARRIVAL_DATE = "placeOfDeliveryArrivalDate";
  private static final String VALUE = "value";
  private static final String PORT_OF_DISCHARGE = "portOfDischarge";
  private static final String UN_LOCATION_CODE = "UNLocationCode";
  private static final String FACILITY = "facility";
  private static final String FACILITY_NAME = "facilityName";
  private static final String FACILITY_CODE = "facilityCode";
  private static final String FACILITY_CODE_LIST_PROVIDER = "facilityCodeListProvider";
  private static final String LEGS = "legs";
  private static final String VESSEL_VOYAGE = "vesselVoyage";
  private static final String VESSEL_NAME = "vesselName";
  private static final String CARRIER_IMPORT_VOYAGE_NUMBER = "carrierImportVoyageNumber";
  private static final String UTILIZED_TRANSPORT_EQUIPMENTS = "utilizedTransportEquipments";
  private static final String EQUIPMENT = "equipment";
  private static final String EQUIPMENT_REFERENCE = "equipmentReference";
  private static final String ISO_EQUIPMENT_CODE = "ISOEquipmentCode";
  private static final String SEALS = "seals";
  private static final String NUMBER = "number";
  private static final String CONSIGNMENT_ITEMS = "consignmentItems";
  private static final String DESCRIPTION_OF_GOODS = "descriptionOfGoods";
  private static final String CARGO_ITEMS = "cargoItems";
  private static final String CARGO_GROSS_WEIGHT = "cargoGrossWeight";
  private static final String UNIT = "unit";
  private static final String OUTER_PACKAGING = "outerPackaging";
  private static final String PACKAGE_CODE = "packageCode";
  private static final String IMO_PACKAGING_CODE = "IMOPackagingCode";
  private static final String DESCRIPTION = "description";
  private static final String NUMBER_OF_PACKAGES = "numberOfPackages";
  private static final String FREE_TIMES = "freeTimes";
  private static final String TYPE_CODES = "typeCodes";
  private static final String ISO_EQUIPMENT_CODES = "ISOEquipmentCodes";
  private static final String EQUIPMENT_REFERENCES = "equipmentReferences";
  private static final String DURATION = "duration";
  private static final String TIME_UNIT = "timeUnit";
  private static final String CHARGES = "charges";
  private static final String CHARGE_NAME = "chargeName";
  private static final String CURRENCY_AMOUNT = "currencyAmount";
  private static final String CURRENCY_CODE = "currencyCode";
  private static final String PAYMENT_TERM_CODE = "paymentTermCode";
  private static final String UNIT_PRICE = "unitPrice";
  private static final String QUANTITY = "quantity";
  private static final String STREET = "street";
  private static final String STREET_NUMBER = "streetNumber";
  private static final String FLOOR = "floor";
  private static final String POST_CODE = "postCode";
  private static final String PO_BOX = "POBox";
  private static final String CITY = "city";
  private static final String STATE_REGION = "stateRegion";
  private static final String COUNTRY_CODE = "countryCode";
  private static final String ADDRESS_LINES = "addressLines";

  private static final List<String> ADDRESS_FIELDS =
    List.of(
      STREET,
      STREET_NUMBER,
      FLOOR,
      POST_CODE,
      PO_BOX,
      CITY,
      STATE_REGION,
      COUNTRY_CODE,
      ADDRESS_LINES);

  private ANChecks() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static ActionCheck getANPostPayloadChecks(
    UUID matchedExchangeUuid, String expectedApiVersion, String scenarioType) {
    return JsonAttribute.contentChecks(
      "",
      "The AN Producer has correctly demonstrated the use of functionally required attributes in the payload",
      ANRole::isProducer,
      matchedExchangeUuid,
      HttpMessageType.REQUEST,
      expectedApiVersion,
      payloadChecksForScenario(scenarioType));
  }

  public static ActionCheck getANGetResponseChecks(
    UUID matchedExchangeUuid,
    String expectedApiVersion,
    Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.contentChecks(
      ANRole::isProducer,
      matchedExchangeUuid,
      HttpMessageType.RESPONSE,
      expectedApiVersion,
      payloadChecksForScenario(dspSupplier.get().scenarioType()));
  }

  public static JsonContentCheck nonEmptyArrivalNotices() {
    return rule(NON_EMPTY_ARRIVAL_NOTICES_DESCRIPTION, path(), node -> nonEmptyArray(node, ARRIVAL_NOTICES));
  }

  static List<JsonContentCheck> payloadChecksForScenario(String scenarioType) {
    List<JsonContentCheck> checks = new ArrayList<>(commonChecks());
    if (ScenarioType.FREE_TIME.name().equals(scenarioType)) {
      checks.addAll(freeTimeChecks());
    }
    if (ScenarioType.FREIGHTED.name().equals(scenarioType)) {
      checks.addAll(freightedChecks());
    }
    return checks;
  }

  private static List<JsonContentCheck> commonChecks() {
    return List.of(
      nonEmptyArrivalNotices(),
      rule("At least one Arrival Notice must demonstrate the correct use of 'transportDocumentReference' (not empty or blank).", path(ARRIVAL_NOTICES), node -> nonBlank(node, TRANSPORT_DOCUMENT_REFERENCE)),
      rule("At least one Arrival Notice must demonstrate the correct use of 'carrierCode' (not empty or blank).", path(ARRIVAL_NOTICES), node -> nonBlank(node, CARRIER_CODE)),
      rule("At least one Arrival Notice must demonstrate the correct use of 'carrierCodeListProvider' ('NMFTA' or 'SMDG').", path(ARRIVAL_NOTICES), node -> allowed(node, CARRIER_CODE_LIST_PROVIDER, ANDatasets.CARRIER_CODE_LIST_PROVIDER)),
      rule("At least one Arrival Notice must demonstrate the correct use of a 'carrierContactInformation' list with at least one item.", path(ARRIVAL_NOTICES), node -> nonEmptyArray(node, CARRIER_CONTACT_INFORMATION)),
      rule("At least one 'carrierContactInformation[]' item within at least one Arrival Notice must demonstrate the correct use of 'phone' or 'email' (not empty or blank).", path(ARRIVAL_NOTICES, CARRIER_CONTACT_INFORMATION), node -> nonBlank(node, PHONE) || nonBlank(node, EMAIL)),
      rule("At least one 'carrierContactInformation[]' item within at least one Arrival Notice must demonstrate the correct use of 'name' (not empty or blank).", path(ARRIVAL_NOTICES, CARRIER_CONTACT_INFORMATION), node -> nonBlank(node, NAME)),
      rule("At least one Arrival Notice must demonstrate the correct use of 'deliveryTypeAtDestination' ('CY', 'SD', or 'CFS').", path(ARRIVAL_NOTICES), node -> allowed(node, DELIVERY_TYPE_AT_DESTINATION, ANDatasets.DELIVERY_TYPE_AT_DESTINATION)),
      rule("At least one Arrival Notice must demonstrate the correct use of a 'documentParties' list with at least one item.", path(ARRIVAL_NOTICES), node -> nonEmptyArray(node, DOCUMENT_PARTIES)),
      rule("At least one 'documentParties[]' item within at least one Arrival Notice must demonstrate the correct use of 'partyFunction' ('OS', 'CN', 'END', 'RW', 'CG', 'N1', 'N2', 'NI', 'SCO', 'DDR', 'DDS', 'COW', 'COX', 'CS', 'MF', or 'WH').", path(ARRIVAL_NOTICES, DOCUMENT_PARTIES), node -> allowed(node, PARTY_FUNCTION, ANDatasets.PARTY_FUNCTION)),
      rule("At least one 'documentParties[]' item within at least one Arrival Notice must demonstrate the correct use of 'partyName' (not empty or blank).", path(ARRIVAL_NOTICES, DOCUMENT_PARTIES), node -> nonBlank(node, PARTY_NAME)),
      rule("At least one 'documentParties[].partyContactDetails[]' item within at least one Arrival Notice must demonstrate the correct use of 'phone' or 'email' (not empty or blank).", path(ARRIVAL_NOTICES, DOCUMENT_PARTIES, PARTY_CONTACT_DETAILS), node -> nonBlank(node, PHONE) || nonBlank(node, EMAIL)),
      rule("At least one 'documentParties[].partyContactDetails[]' item within at least one Arrival Notice must demonstrate the correct use of 'name' (not empty or blank).", path(ARRIVAL_NOTICES, DOCUMENT_PARTIES, PARTY_CONTACT_DETAILS), node -> nonBlank(node, NAME)),
      rule("At least one 'documentParties[]' item within at least one Arrival Notice must demonstrate the correct use of the 'address' object.", path(ARRIVAL_NOTICES, DOCUMENT_PARTIES), node -> object(node, ADDRESS)),
      rule("At least one 'documentParties[].address' object within at least one Arrival Notice must contain at least one non-empty address attribute.", path(ARRIVAL_NOTICES, DOCUMENT_PARTIES, ADDRESS), ANChecks::hasNonEmptyAddressAttribute),
      rule("At least one Arrival Notice must demonstrate the correct use of the 'transport' object.", path(ARRIVAL_NOTICES), node -> object(node, TRANSPORT)),
      rule("The 'transport' object within at least one Arrival Notice must contain 'portOfDischargeArrivalDate.value' or 'placeOfDeliveryArrivalDate.value'.", path(ARRIVAL_NOTICES, TRANSPORT), ANChecks::hasArrivalDate),
      rule("The 'transport' object within at least one Arrival Notice must demonstrate the correct use of the 'portOfDischarge' object.", path(ARRIVAL_NOTICES, TRANSPORT), node -> object(node, PORT_OF_DISCHARGE)),
      rule("The 'transport.portOfDischarge' object within at least one Arrival Notice must contain a non-empty 'UNLocationCode', a 'facility' object, or an 'address' object.", path(ARRIVAL_NOTICES, TRANSPORT, PORT_OF_DISCHARGE), ANChecks::hasPortOfDischargeIdentifier),
      allRule("If 'transport.portOfDischarge.facility' is present, it must contain either a non-empty 'facilityName' or both a non-empty 'facilityCode' and a 'facilityCodeListProvider' of 'SMDG' or 'BIC'.", path(ARRIVAL_NOTICES, TRANSPORT, PORT_OF_DISCHARGE, FACILITY), ANChecks::validFacility),
      allRule("If 'transport.portOfDischarge.address' is present, it must contain at least one non-empty address attribute.", path(ARRIVAL_NOTICES, TRANSPORT, PORT_OF_DISCHARGE, ADDRESS), ANChecks::hasNonEmptyAddressAttribute),
      rule("At least one Arrival Notice must demonstrate the correct use of a 'transport.legs' list with at least one item.", path(ARRIVAL_NOTICES, TRANSPORT), node -> nonEmptyArray(node, LEGS)),
      rule("At least one 'transport.legs[]' item within at least one Arrival Notice must demonstrate the correct use of the 'vesselVoyage' object.", path(ARRIVAL_NOTICES, TRANSPORT, LEGS), node -> object(node, VESSEL_VOYAGE)),
      rule("At least one 'transport.legs[].vesselVoyage' object within at least one Arrival Notice must demonstrate the correct use of 'vesselName' (not empty or blank).", path(ARRIVAL_NOTICES, TRANSPORT, LEGS, VESSEL_VOYAGE), node -> nonBlank(node, VESSEL_NAME)),
      rule("At least one 'transport.legs[].vesselVoyage' object within at least one Arrival Notice must demonstrate the correct use of 'carrierImportVoyageNumber' (not empty or blank).", path(ARRIVAL_NOTICES, TRANSPORT, LEGS, VESSEL_VOYAGE), node -> nonBlank(node, CARRIER_IMPORT_VOYAGE_NUMBER)),
      rule("At least one Arrival Notice must demonstrate the correct use of a 'utilizedTransportEquipments' list with at least one item.", path(ARRIVAL_NOTICES), node -> nonEmptyArray(node, UTILIZED_TRANSPORT_EQUIPMENTS)),
      rule("At least one 'utilizedTransportEquipments[]' item within at least one Arrival Notice must demonstrate the correct use of the 'equipment' object.", path(ARRIVAL_NOTICES, UTILIZED_TRANSPORT_EQUIPMENTS), node -> object(node, EQUIPMENT)),
      rule("At least one 'utilizedTransportEquipments[].equipment' object within at least one Arrival Notice must demonstrate the correct use of 'equipmentReference' (not empty or blank).", path(ARRIVAL_NOTICES, UTILIZED_TRANSPORT_EQUIPMENTS, EQUIPMENT), node -> nonBlank(node, EQUIPMENT_REFERENCE)),
      rule("At least one 'utilizedTransportEquipments[].equipment' object within at least one Arrival Notice must demonstrate the correct use of 'ISOEquipmentCode' (not empty or blank).", path(ARRIVAL_NOTICES, UTILIZED_TRANSPORT_EQUIPMENTS, EQUIPMENT), node -> nonBlank(node, ISO_EQUIPMENT_CODE)),
      rule("At least one 'utilizedTransportEquipments[]' item within at least one Arrival Notice must demonstrate the correct use of a 'seals' list with at least one item.", path(ARRIVAL_NOTICES, UTILIZED_TRANSPORT_EQUIPMENTS), node -> nonEmptyArray(node, SEALS)),
      rule("At least one 'utilizedTransportEquipments[].seals[]' item within at least one Arrival Notice must demonstrate the correct use of 'number' (not empty or blank).", path(ARRIVAL_NOTICES, UTILIZED_TRANSPORT_EQUIPMENTS, SEALS), node -> nonBlank(node, NUMBER)),
      rule("At least one Arrival Notice must demonstrate the correct use of a 'consignmentItems' list with at least one item.", path(ARRIVAL_NOTICES), node -> nonEmptyArray(node, CONSIGNMENT_ITEMS)),
      rule("At least one 'consignmentItems[]' item within at least one Arrival Notice must demonstrate the correct use of a 'descriptionOfGoods' list with at least one non-empty value.", path(ARRIVAL_NOTICES, CONSIGNMENT_ITEMS, DESCRIPTION_OF_GOODS), ANChecks::nonBlankValue),
      rule("At least one 'consignmentItems[]' item within at least one Arrival Notice must demonstrate the correct use of a 'cargoItems' list with at least one item.", path(ARRIVAL_NOTICES, CONSIGNMENT_ITEMS), node -> nonEmptyArray(node, CARGO_ITEMS)),
      rule("At least one 'consignmentItems[].cargoItems[]' item within at least one Arrival Notice must demonstrate the correct use of 'equipmentReference' (not empty or blank).", path(ARRIVAL_NOTICES, CONSIGNMENT_ITEMS, CARGO_ITEMS), node -> nonBlank(node, EQUIPMENT_REFERENCE)),
      rule("At least one 'consignmentItems[].cargoItems[]' item within at least one Arrival Notice must demonstrate the correct use of the 'cargoGrossWeight' object.", path(ARRIVAL_NOTICES, CONSIGNMENT_ITEMS, CARGO_ITEMS), node -> object(node, CARGO_GROSS_WEIGHT)),
      rule("At least one 'consignmentItems[].cargoItems[].cargoGrossWeight' object within at least one Arrival Notice must demonstrate the correct use of 'value' (positive number).", path(ARRIVAL_NOTICES, CONSIGNMENT_ITEMS, CARGO_ITEMS, CARGO_GROSS_WEIGHT), node -> positive(node, VALUE)),
      rule("At least one 'consignmentItems[].cargoItems[].cargoGrossWeight' object within at least one Arrival Notice must demonstrate the correct use of 'unit' ('KGM', 'LBR', 'GRM', or 'ONZ').", path(ARRIVAL_NOTICES, CONSIGNMENT_ITEMS, CARGO_ITEMS, CARGO_GROSS_WEIGHT), node -> allowed(node, UNIT, ANDatasets.CARGO_GROSS_WEIGHT_UNIT)),
      rule("At least one 'consignmentItems[].cargoItems[]' item within at least one Arrival Notice must demonstrate the correct use of the 'outerPackaging' object.", path(ARRIVAL_NOTICES, CONSIGNMENT_ITEMS, CARGO_ITEMS), node -> object(node, OUTER_PACKAGING)),
      rule("At least one 'consignmentItems[].cargoItems[].outerPackaging' object within at least one Arrival Notice must contain a non-empty 'packageCode', 'IMOPackagingCode', or 'description'.", path(ARRIVAL_NOTICES, CONSIGNMENT_ITEMS, CARGO_ITEMS, OUTER_PACKAGING), node -> nonBlank(node, PACKAGE_CODE) || nonBlank(node, IMO_PACKAGING_CODE) || nonBlank(node, DESCRIPTION)),
      rule("At least one 'consignmentItems[].cargoItems[].outerPackaging' object within at least one Arrival Notice must demonstrate the correct use of 'numberOfPackages' (positive number).", path(ARRIVAL_NOTICES, CONSIGNMENT_ITEMS, CARGO_ITEMS, OUTER_PACKAGING), node -> positive(node, NUMBER_OF_PACKAGES)));
  }

  private static List<JsonContentCheck> freeTimeChecks() {
    return List.of(
      rule("At least one Arrival Notice must demonstrate the correct use of a 'freeTimes' list with at least one item.", path(ARRIVAL_NOTICES), node -> nonEmptyArray(node, FREE_TIMES)),
      rule("At least one 'freeTimes[]' item within at least one Arrival Notice must demonstrate the correct use of a 'typeCodes' list with at least one value ('DEM', 'DET', or 'STO').", path(ARRIVAL_NOTICES, FREE_TIMES, TYPE_CODES), node -> ANDatasets.FREE_TIME_TYPE_CODES.contains(node.asText())),
      rule("At least one 'freeTimes[]' item within at least one Arrival Notice must demonstrate the correct use of an 'ISOEquipmentCodes' list with at least one non-empty value.", path(ARRIVAL_NOTICES, FREE_TIMES, ISO_EQUIPMENT_CODES), ANChecks::nonBlankValue),
      rule("At least one 'freeTimes[]' item within at least one Arrival Notice must demonstrate the correct use of an 'equipmentReferences' list with at least one non-empty value.", path(ARRIVAL_NOTICES, FREE_TIMES, EQUIPMENT_REFERENCES), ANChecks::nonBlankValue),
      rule("At least one 'freeTimes[]' item within at least one Arrival Notice must demonstrate the correct use of 'duration' (positive number).", path(ARRIVAL_NOTICES, FREE_TIMES), node -> positive(node, DURATION)),
      rule("At least one 'freeTimes[]' item within at least one Arrival Notice must demonstrate the correct use of 'timeUnit' ('CD', 'WD', or 'HR').", path(ARRIVAL_NOTICES, FREE_TIMES), node -> allowed(node, TIME_UNIT, ANDatasets.FREE_TIME_TIME_UNIT)));
  }

  private static List<JsonContentCheck> freightedChecks() {
    return List.of(
      rule("At least one Arrival Notice must demonstrate the correct use of a 'charges' list with at least one item.", path(ARRIVAL_NOTICES), node -> nonEmptyArray(node, CHARGES)),
      rule("At least one 'charges[]' item within at least one Arrival Notice must demonstrate the correct use of 'chargeName' (not empty or blank).", path(ARRIVAL_NOTICES, CHARGES), node -> nonBlank(node, CHARGE_NAME)),
      rule("At least one 'charges[]' item within at least one Arrival Notice must demonstrate the correct use of 'currencyAmount' (positive number).", path(ARRIVAL_NOTICES, CHARGES), node -> positive(node, CURRENCY_AMOUNT)),
      rule("At least one 'charges[]' item within at least one Arrival Notice must demonstrate the correct use of 'currencyCode' (not empty or blank).", path(ARRIVAL_NOTICES, CHARGES), node -> nonBlank(node, CURRENCY_CODE)),
      rule("At least one 'charges[]' item within at least one Arrival Notice must demonstrate the correct use of 'paymentTermCode' ('PRE' or 'COL').", path(ARRIVAL_NOTICES, CHARGES), node -> allowed(node, PAYMENT_TERM_CODE, ANDatasets.PAYMENT_TERM_CODE)),
      rule("At least one 'charges[]' item within at least one Arrival Notice must demonstrate the correct use of 'unitPrice' (positive number).", path(ARRIVAL_NOTICES, CHARGES), node -> positive(node, UNIT_PRICE)),
      rule("At least one 'charges[]' item within at least one Arrival Notice must demonstrate the correct use of 'quantity' (positive number).", path(ARRIVAL_NOTICES, CHARGES), node -> positive(node, QUANTITY)));
  }

  private static JsonContentCheck rule(
    String description, String[] path, Predicate<JsonNode> predicate) {
    return validation(description, body -> nodes(body, path).anyMatch(predicate));
  }

  private static JsonContentCheck allRule(
    String description, String[] path, Predicate<JsonNode> predicate) {
    return validation(description, body -> nodes(body, path).allMatch(predicate));
  }

  private static JsonContentCheck validation(String description, Predicate<JsonNode> predicate) {
    return JsonAttribute.customValidator(
      description,
      body ->
        ConformanceCheckResult.simple(
          predicate.test(body) ? Set.of() : Set.of("No occurrence satisfied: " + description)));
  }

  private static String[] path(String... segments) {
    return segments;
  }

  private static Stream<JsonNode> nodes(JsonNode body, String... path) {
    Stream<JsonNode> nodes = Stream.of(body);
    for (String segment : path) {
      nodes = nodes.flatMap(node -> children(node.path(segment)));
    }
    return nodes;
  }

  private static Stream<JsonNode> children(JsonNode node) {
    if (node.isMissingNode() || node.isNull()) {
      return Stream.empty();
    }
    return node.isArray()
      ? StreamSupport.stream(node.spliterator(), false)
      : Stream.of(node);
  }

  private static boolean nonEmptyArray(JsonNode parent, String field) {
    JsonNode value = parent.path(field);
    return value.isArray() && !value.isEmpty();
  }

  private static boolean object(JsonNode parent, String field) {
    return parent.path(field).isObject();
  }

  private static boolean nonBlank(JsonNode parent, String field) {
    return nonBlankValue(parent.path(field));
  }

  private static boolean nonBlankValue(JsonNode value) {
    return value.isValueNode() && !value.asText().isBlank();
  }

  private static boolean positive(JsonNode parent, String field) {
    JsonNode value = parent.path(field);
    return value.isNumber() && value.asDouble() > 0;
  }

  private static boolean allowed(JsonNode parent, String field, KeywordDataset dataset) {
    JsonNode value = parent.path(field);
    return nonBlankValue(value) && dataset.contains(value.asText());
  }

  private static boolean hasNonEmptyAddressAttribute(JsonNode address) {
    return ADDRESS_FIELDS.stream().map(address::path).anyMatch(ANChecks::hasValue);
  }

  private static boolean hasValue(JsonNode value) {
    return value.isTextual() ? !value.asText().isBlank() : !JsonUtil.isMissingOrEmpty(value);
  }

  private static boolean hasArrivalDate(JsonNode transport) {
    return nonBlank(transport.path(PORT_OF_DISCHARGE_ARRIVAL_DATE), VALUE)
      || nonBlank(transport.path(PLACE_OF_DELIVERY_ARRIVAL_DATE), VALUE);
  }

  private static boolean hasPortOfDischargeIdentifier(JsonNode portOfDischarge) {
    return nonBlank(portOfDischarge, UN_LOCATION_CODE)
      || object(portOfDischarge, FACILITY)
      || object(portOfDischarge, ADDRESS);
  }

  private static boolean validFacility(JsonNode facility) {
    return nonBlank(facility, FACILITY_NAME)
      || (nonBlank(facility, FACILITY_CODE)
      && allowed(facility, FACILITY_CODE_LIST_PROVIDER, ANDatasets.FACILITY_CODE_LIST_PROVIDER));
  }
}
