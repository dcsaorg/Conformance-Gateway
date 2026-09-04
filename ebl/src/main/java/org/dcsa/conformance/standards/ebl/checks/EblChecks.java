package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.core.check.JsonAttribute.concatContextPath;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.EXEMPT_PACKAGE_CODES;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.FEEDBACKS_CODE;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.FEEDBACKS_SEVERITY;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.MODE_OF_TRANSPORT;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.NATIONAL_COMMODITY_CODES_SET;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.PARTY_FUNCTION_CODE;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.PARTY_FUNCTION_CODE_HBL;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.REQUESTED_CARRIER_CLAUSES_SET;
import static org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus.SI_PENDING_UPDATE;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.ebl.models.TriConsumer;
import org.dcsa.conformance.standards.ebl.party.*;
import org.dcsa.conformance.standardscommons.party.EblDynamicScenarioParameters;

@UtilityClass
public class EblChecks {

  private static final String IF_S_IS_NOT_PROVIDED_IN_S_THEN_S_IS_REQUIRED =
      "If '%s' is not provided in '%s', then '%s' is required.";

  private static final String S = "/%s";
  private static final String SS = "/%s/%s";
  private static final String SSS = "/%s/%s/%s";
  private static final String SDS = "/%s/%d/%s";
  private static final String SSDSS = "/%s/%s/%d/%s/%s";

  private static final String S_S = "%s.%s";
  private static final String S_S_S = "%s.%s.%s";

  private static final String S_x = "%s.*";
  private static final String S_x_S = "%s.*.%s";
  private static final String S_x_S_x_S = "%s.*.%s.*.%s";
  private static final String S_x_S_S = "%s.*.%s.%s";
  private static final String S_S_x_S = "%s.%s.*.%s";
  private static final String S_S_x_S_S = "%s.%s.*.%s.%s";
  private static final String S_x_S_x_S_S = "%s.*.%s.*.%s.%s";
  private static final String S_x_S_x_S_x_S = "%s.*.%s.*.%s.*.%s";
  private static final String S_x_S_S_x_S = "%s.*.%s.%s.*.%s";
  private static final String S_S_x_S_x_S = "%s.%s.*.%s.*.%s";
  private static final String S_S_x_S_S_x_S = "%s.%s.*.%s.%s.*.%s";
  private static final String S_S_S_x_S = "%s.%s.%s.*.%s";
  private static final String S_S_S_S_x_S = "%s.%s.%s.%s.*.%s";
  private static final String S_x_S_S_S_x_S = "%s.*.%s.%s.%s.*.%s";
  private static final String S_x_S_S_S_S_x_S = "%s.*.%s.%s.%s.%s.*.%s";

  private static final String ZERO = "0";

  private static final String CONSIGNMENT_ITEMS = "consignmentItems";
  private static final String UTILIZED_TRANSPORT_EQUIPMENTS = "utilizedTransportEquipments";
  private static final String DOCUMENT_PARTIES = "documentParties";
  private static final String CUSTOMS_REFERENCES = "customsReferences";
  private static final String ROUTING_OF_CONSIGNMENT_COUNTRIES = "routingOfConsignmentCountries";
  private static final String MANIFEST_TYPE_CODE = "manifestTypeCode";
  private static final String COUNTRY_CODE = "countryCode";
  private static final String LOCATION_NAME = "locationName";
  private static final String IS_TO_ORDER = "isToOrder";
  private static final String AMF_HBL_PERFORMED_BY = "advanceManifestFilingsHouseBLPerformedBy";
  private static final String ADVANCE_MANIFEST_FILINGS = "advanceManifestFilings";
  private static final String HOUSE_BILL_OF_LADINGS = "houseBillOfLadings";
  private static final String NUMBER_OF_COPIES_WITH_CHARGES = "numberOfCopiesWithCharges";
  private static final String PARTY_CONTACT_DETAILS = "partyContactDetails";
  private static final String SELLER = "seller";
  private static final String CONSIGNEE = "consignee";
  private static final String SHIPPER = "shipper";
  private static final String NOTIFY_PARTY = "notifyParty";
  private static final String BUYER = "buyer";
  private static final String ENDORSEE = "endorsee";
  private static final String ISSUE_TO = "issueTo";
  private static final String NOTIFY_PARTIES = "notifyParties";
  private static final String OTHER = "other";
  private static final String DISPLAYED_ADDRESS = "displayedAddress";
  private static final String IDENTIFYING_CODES = "identifyingCodes";
  private static final String TAX_LEGAL_REFERENCES = "taxLegalReferences";
  private static final String REQUESTED_CARRIER_CLAUSES = "requestedCarrierClauses";
  private static final String DESCRIPTION_OF_GOODS = "descriptionOfGoods";
  private static final String HS_CODES = "HSCodes";
  private static final String SHIPPING_MARKS = "shippingMarks";
  private static final String CARGO_ITEMS = "cargoItems";
  private static final String VALUES = "values";
  private static final String SEALS = "seals";
  private static final String REFERENCES = "references";
  private static final String TRANSPORT_DOCUMENT_TYPE_CODE = "transportDocumentTypeCode";
  private static final String REQUESTED_CARRIER_CERTIFICATES = "requestedCarrierCertificates";
  private static final String IS_ELECTRONIC = "isElectronic";
  private static final String NUMBER_OF_COPIES_WITHOUT_CHARGES = "numberOfCopiesWithoutCharges";
  private static final String NUMBER_OF_ORIGINALS_WITH_CHARGES = "numberOfOriginalsWithCharges";
  private static final String NUMBER_OF_ORIGINALS_WITHOUT_CHARGES =
      "numberOfOriginalsWithoutCharges";
  private static final String PARTY = "party";
  private static final String ADDRESS = "address";
  private static final String ADDRESS_LINES = "addressLines";
  private static final String CODE_LIST_PROVIDER = "codeListProvider";
  private static final String TYPE = "type";
  private static final String WOOD_DECLARATION = "woodDeclaration";
  private static final String OUTER_PACKAGING = "outerPackaging";
  private static final String EQUIPMENT = "equipment";
  private static final String ISO_EQUIPMENT_CODE = "ISOEquipmentCode";
  private static final String IS_NON_OPERATING_REEFER = "isNonOperatingReefer";
  private static final String ACTIVE_REEFER_SETTINGS = "activeReeferSettings";
  private static final String NATIONAL_COMMODITY_CODES = "nationalCommodityCodes";
  private static final String PARTY_FUNCTION = "partyFunction";
  private static final String ISSUING_PARTY = "issuingParty";
  private static final String EQUIPMENT_REFERENCE = "equipmentReference";
  private static final String DANGEROUS_GOODS = "dangerousGoods";
  private static final String IS_HOUSE_BILL_OF_LADINGS_ISSUED = "isHouseBillOfLadingsIssued";
  private static final String CARRIER_CODE = "carrierCode";
  private static final String CARRIER_CODE_LIST_PROVIDER = "carrierCodeListProvider";
  private static final String TYPE_OF_PERSON = "typeOfPerson";
  private static final String EBL_PLATFORM = "eblPlatform";
  private static final String ON_BEHALF_OF_SHIPPER = "onBehalfOfShipper";
  private static final String ON_BEHALF_OF_CONSIGNEE = "onBehalfOfConsignee";
  private static final String PACKAGE_CODE = "packageCode";
  private static final String NUMBER_OF_PACKAGES = "numberOfPackages";
  private static final String IDENTIFICATION_NUMBER = "identificationNumber";
  private static final String SELF_FILER_CODE = "selfFilerCode";
  private static final String UN_LOCATION_CODE = "UNLocationCode";
  private static final String PLACE_OF_ACCEPTANCE = "placeOfAcceptance";
  private static final String PLACE_OF_FINAL_DELIVERY = "placeOfFinalDelivery";
  private static final String IS_CARGO_DELIVERED_IN_ICS_2_ZONE = "isCargoDeliveredInICS2Zone";
  private static final String ADVANCE_MANIFEST_FILING_PERFORMED_BY =
      "advanceManifestFilingPerformedBy";
  private static final String SEND_TO_PLATFORM = "sendToPlatform";
  private static final String FEEDBACKS = "feedbacks";
  private static final String SEVERITY = "severity";
  private static final String CODE = "code";
  private static final String SHIPPED_ON_BOARD_DATE = "shippedOnBoardDate";
  private static final String RECEIVED_FOR_SHIPMENT_DATE = "receivedForShipmentDate";
  private static final String CARGO_MOVEMENT_TYPE_AT_ORIGIN = "cargoMovementTypeAtOrigin";
  private static final String CARGO_MOVEMENT_TYPE_AT_DESTINATION = "cargoMovementTypeAtDestination";
  private static final String DECLARED_VALUE = "declaredValue";
  private static final String DECLARED_VALUE_CURRENCY = "declaredValueCurrency";
  private static final String PRE_CARRIAGE_BY = "preCarriageBy";
  private static final String PLACE_OF_RECEIPT = "placeOfReceipt";
  private static final String TRANSPORTS = "transports";
  private static final String ON_CARRIAGE_BY = "onCarriageBy";
  private static final String PLACE_OF_DELIVERY = "placeOfDelivery";
  private static final String IMO_PACKAGING_CODE = "imoPackagingCode";
  private static final String INHALATION_ZONE = "inhalationZone";
  private static final String SEGREGATION_GROUPS = "segregationGroups";
  private static final String TEMPERATURE_SETPOINT = "temperatureSetpoint";
  private static final String TEMPERATURE_UNIT = "temperatureUnit";
  private static final String AIR_EXCHANGE_SETPOINT = "airExchangeSetpoint";
  private static final String AIR_EXCHANGE_UNIT = "airExchangeUnit";
  private static final String REFERENCE = "reference";
  private static final String PURCHASE_ORDER_REFERENCE = "purchaseOrderReference";
  private static final String SHIPPING_INSTRUCTIONS_STATUS = "shippingInstructionsStatus";
  private static final String UPDATED_SHIPPING_INSTRUCTIONS_STATUS =
      "updatedShippingInstructionsStatus";
  private static final String SHIPPING_INSTRUCTIONS_REFERENCE = "shippingInstructionsReference";
  private static final String TRANSPORT_DOCUMENT_REFERENCE = "transportDocumentReference";
  private static final String TRANSPORT_DOCUMENT_STATUS = "transportDocumentStatus";
  private static final String AMENDED_TRANSPORT_DOCUMENT_STATUS = "amendedTransportDocumentStatus";
  private static final String TRANSPORT_DOCUMENT = "transportDocument";
  private static final String ISSUE_DATE = "issueDate";
  private static final String NUMBER_OF_RIDER_PAGES = "numberOfRiderPages";
  private static final String VESSEL_VOYAGES = "vesselVoyages";
  private static final String ROLE = "role";
  private static final String EXTENDED_NATIONAL_COMMODITY_CODES =
      "extendedNationalCommodityCodes";
  private static final String INNER_PACKAGINGS = "innerPackagings";
  private static final String QUANTITY = "quantity";
  private static final String CARGO_GROSS_WEIGHT = "cargoGrossWeight";
  private static final String CARGO_NET_WEIGHT = "cargoNetWeight";
  private static final String CARGO_GROSS_VOLUME = "cargoGrossVolume";
  private static final String VALUE = "value";
  private static final String SCENARIO = "Scenario";
  private static final String CARRIERS_AGENT_AT_DESTINATION = "carriersAgentAtDestination";
  private static final String SHIPPING_INSTRUCTIONS = "shippingInstructions";
  private static final String IS_CARRIERS_AGENT_AT_DESTINATION_REQUIRED =
      "isCarriersAgentAtDestinationRequired";
  private static final String METHOD_OF_PAYMENT = "methodOfPayment";

  private static final String SWB = "SWB";
  private static final String BOL = "BOL";
  private static final String CARRIER = "CARRIER";
  private static final String ACI = "ACI";
  private static final String ACE = "ACE";
  private static final String SELF = "SELF";
  private static final String ENS = "ENS";

  private static final JsonPointer SI_REF_SIR_PTR =
      JsonPointer.compile(S.formatted(SHIPPING_INSTRUCTIONS_REFERENCE));

  private static final JsonPointer SI_REF_SI_STATUS_PTR =
      JsonPointer.compile(S.formatted(SHIPPING_INSTRUCTIONS_STATUS));

  private static final JsonPointer SI_REF_UPDATED_SI_STATUS_PTR =
      JsonPointer.compile(S.formatted(UPDATED_SHIPPING_INSTRUCTIONS_STATUS));

  private static final JsonPointer SI_REQUEST_SEND_TO_PLATFORM =
      JsonPointer.compile(SSS.formatted(DOCUMENT_PARTIES, ISSUE_TO, SEND_TO_PLATFORM));

  private static final JsonPointer TD_TDR =
      JsonPointer.compile(S.formatted(TRANSPORT_DOCUMENT_REFERENCE));

  private static final JsonPointer TD_TRANSPORT_DOCUMENT_STATUS =
      JsonPointer.compile(S.formatted(TRANSPORT_DOCUMENT_STATUS));

  private static final BiConsumer<JsonNode, TriConsumer<JsonNode, String, ArrayOrderHandler>>
      DOC_PARTY_ARRAY_ORDER_DEFINITIONS =
          (documentPartyNode, arrayNodeHandler) -> {
            arrayNodeHandler.accept(
                documentPartyNode, DISPLAYED_ADDRESS, ArrayOrderHandler.inputPreservedArrayOrder());
            arrayNodeHandler.accept(
                documentPartyNode, IDENTIFYING_CODES, ArrayOrderHandler.toStringSortableArray());
            arrayNodeHandler.accept(
                documentPartyNode, TAX_LEGAL_REFERENCES, ArrayOrderHandler.toStringSortableArray());
            arrayNodeHandler.accept(
                documentPartyNode,
                PARTY_CONTACT_DETAILS,
                ArrayOrderHandler.toStringSortableArray());
          };

  private static final BiConsumer<JsonNode, TriConsumer<JsonNode, String, ArrayOrderHandler>>
      DOC_PARTIES_ARRAY_ORDER_DEFINITIONS =
          (documentPartyNode, arrayNodeHandler) -> {
            for (var partyName :
                List.of(SHIPPER, CONSIGNEE, NOTIFY_PARTY, SELLER, BUYER, ENDORSEE, ISSUE_TO)) {
              DOC_PARTY_ARRAY_ORDER_DEFINITIONS.accept(
                  documentPartyNode.path(partyName), arrayNodeHandler);
            }

            arrayNodeHandler.accept(
                documentPartyNode, NOTIFY_PARTIES, ArrayOrderHandler.inputPreservedArrayOrder());
            for (var party : documentPartyNode.path(NOTIFY_PARTIES)) {
              DOC_PARTY_ARRAY_ORDER_DEFINITIONS.accept(party, arrayNodeHandler);
            }
            arrayNodeHandler.accept(
                documentPartyNode, OTHER, ArrayOrderHandler.toStringSortableArray());
            for (var party : documentPartyNode.path(OTHER)) {
              DOC_PARTY_ARRAY_ORDER_DEFINITIONS.accept(party, arrayNodeHandler);
            }
          };

  static final JsonRebasableContentCheck VALID_REQUESTED_CARRIER_CLAUSES =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate that '%s' is valid.".formatted(REQUESTED_CARRIER_CLAUSES),
          mav -> mav.submitAllMatching(S_x.formatted(REQUESTED_CARRIER_CLAUSES)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(REQUESTED_CARRIER_CLAUSES_SET));

  public static final BiConsumer<JsonNode, TriConsumer<JsonNode, String, ArrayOrderHandler>>
      SI_ARRAY_ORDER_DEFINITIONS =
          (rootNode, arrayNodeHandler) -> {
            arrayNodeHandler.accept(
                rootNode, PARTY_CONTACT_DETAILS, ArrayOrderHandler.inputPreservedArrayOrder());
            for (var ci : rootNode.path(CONSIGNMENT_ITEMS)) {
              arrayNodeHandler.accept(
                  ci, DESCRIPTION_OF_GOODS, ArrayOrderHandler.toStringSortableArray());
              arrayNodeHandler.accept(ci, HS_CODES, ArrayOrderHandler.toStringSortableArray());
              arrayNodeHandler.accept(
                  ci, NATIONAL_COMMODITY_CODES, ArrayOrderHandler.toStringSortableArray());
              arrayNodeHandler.accept(
                  ci, SHIPPING_MARKS, ArrayOrderHandler.toStringSortableArray());
              for (var cargoItem : ci.path(CARGO_ITEMS)) {
                arrayNodeHandler.accept(
                    cargoItem, NATIONAL_COMMODITY_CODES, ArrayOrderHandler.toStringSortableArray());
                for (var cr : cargoItem.path(CUSTOMS_REFERENCES)) {
                  arrayNodeHandler.accept(cr, VALUES, ArrayOrderHandler.toStringSortableArray());
                }
                arrayNodeHandler.accept(
                    ci, CUSTOMS_REFERENCES, ArrayOrderHandler.toStringSortableArray());
              }
              arrayNodeHandler.accept(ci, CARGO_ITEMS, ArrayOrderHandler.toStringSortableArray());
              for (var cr : ci.path(CUSTOMS_REFERENCES)) {
                arrayNodeHandler.accept(cr, VALUES, ArrayOrderHandler.toStringSortableArray());
              }
              arrayNodeHandler.accept(
                  ci, CUSTOMS_REFERENCES, ArrayOrderHandler.toStringSortableArray());
            }

            arrayNodeHandler.accept(
                rootNode, CONSIGNMENT_ITEMS, ArrayOrderHandler.toStringSortableArray());
            for (var ute : rootNode.path(UTILIZED_TRANSPORT_EQUIPMENTS)) {
              arrayNodeHandler.accept(
                  ute, SHIPPING_MARKS, ArrayOrderHandler.toStringSortableArray());
              arrayNodeHandler.accept(ute, SEALS, ArrayOrderHandler.toStringSortableArray());
              arrayNodeHandler.accept(ute, REFERENCES, ArrayOrderHandler.toStringSortableArray());
              for (var cr : ute.path(CUSTOMS_REFERENCES)) {
                arrayNodeHandler.accept(cr, VALUES, ArrayOrderHandler.toStringSortableArray());
              }
              arrayNodeHandler.accept(
                  ute, CUSTOMS_REFERENCES, ArrayOrderHandler.toStringSortableArray());
            }
            arrayNodeHandler.accept(
                rootNode, UTILIZED_TRANSPORT_EQUIPMENTS, ArrayOrderHandler.toStringSortableArray());
            arrayNodeHandler.accept(
                rootNode, ADVANCE_MANIFEST_FILINGS, ArrayOrderHandler.toStringSortableArray());
            arrayNodeHandler.accept(
                rootNode, REFERENCES, ArrayOrderHandler.toStringSortableArray());
            for (var cr : rootNode.path(CUSTOMS_REFERENCES)) {
              arrayNodeHandler.accept(cr, VALUES, ArrayOrderHandler.toStringSortableArray());
            }
            arrayNodeHandler.accept(
                rootNode, CUSTOMS_REFERENCES, ArrayOrderHandler.toStringSortableArray());

            DOC_PARTIES_ARRAY_ORDER_DEFINITIONS.accept(
                rootNode.path(DOCUMENT_PARTIES), arrayNodeHandler);

            for (var hbl : rootNode.path(HOUSE_BILL_OF_LADINGS)) {
              DOC_PARTIES_ARRAY_ORDER_DEFINITIONS.accept(
                  hbl.path(DOCUMENT_PARTIES), arrayNodeHandler);
              arrayNodeHandler.accept(
                  hbl,
                  ROUTING_OF_CONSIGNMENT_COUNTRIES,
                  ArrayOrderHandler.inputPreservedArrayOrder());
            }
            arrayNodeHandler.accept(
                rootNode, HOUSE_BILL_OF_LADINGS, ArrayOrderHandler.toStringSortableArray());
            arrayNodeHandler.accept(
                rootNode,
                REQUESTED_CARRIER_CERTIFICATES,
                ArrayOrderHandler.toStringSortableArray());
            arrayNodeHandler.accept(
                rootNode, REQUESTED_CARRIER_CLAUSES, ArrayOrderHandler.toStringSortableArray());
          };

  private static final JsonRebasableContentCheck ONLY_EBLS_CAN_BE_NEGOTIABLE =
      JsonAttribute.ifThen(
          "Validate '%s' vs '%s'.".formatted(TRANSPORT_DOCUMENT_TYPE_CODE, IS_TO_ORDER),
          JsonAttribute.isTrue(JsonPointer.compile(S.formatted(IS_TO_ORDER))),
          JsonAttribute.mustEqual(
              JsonPointer.compile(S.formatted(TRANSPORT_DOCUMENT_TYPE_CODE)), BOL));

  static final JsonRebasableContentCheck SWBS_CANNOT_BE_NEGOTIABLE =
      JsonAttribute.ifThen(
          "Validate '%s' vs '%s' for SWBs.".formatted(TRANSPORT_DOCUMENT_TYPE_CODE, IS_TO_ORDER),
          node -> SWB.equals(node.path(TRANSPORT_DOCUMENT_TYPE_CODE).asText("")),
          JsonAttribute.customValidator(
              "SWBs cannot be negotiable - '%s' must be false or absent".formatted(IS_TO_ORDER),
              (node, contextPath) -> {
                boolean isToOrder = node.path(IS_TO_ORDER).asBoolean(false);
                if (isToOrder) {
                  return ConformanceCheckResult.simple(
                      Set.of(
                          "The '%s' must be false (or absent) when '%s' is '%s', but was true at '%s'."
                              .formatted(
                                  IS_TO_ORDER,
                                  TRANSPORT_DOCUMENT_TYPE_CODE,
                                  SWB,
                                  concatContextPath(contextPath, IS_TO_ORDER))));
                }
                return ConformanceCheckResult.simple(Set.of());
              }));

  private static final Predicate<JsonNode> IS_ELECTRONIC_PREDICATE =
      td -> td.path(IS_ELECTRONIC).asBoolean(false);

  private static final Predicate<JsonNode> IS_AN_EBL =
      IS_ELECTRONIC_PREDICATE.and(
          td -> td.path(TRANSPORT_DOCUMENT_TYPE_CODE).asText("").equals(BOL));

  static final JsonRebasableContentCheck EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES =
      eblsCannotHaveCopiesCheck(
          NUMBER_OF_COPIES_WITH_CHARGES,
          "Electronic original Bills of Lading('%s'=true and '%s'=%s) cannot have any copies with charges."
              .formatted(IS_ELECTRONIC, TRANSPORT_DOCUMENT_TYPE_CODE, BOL));

  static final JsonRebasableContentCheck EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES =
      eblsCannotHaveCopiesCheck(
          NUMBER_OF_COPIES_WITHOUT_CHARGES,
          "Electronic original Bills of Lading('%s'=true and '%s'=%s) cannot have any copies without charges."
              .formatted(IS_ELECTRONIC, TRANSPORT_DOCUMENT_TYPE_CODE, BOL));

  private static JsonRebasableContentCheck eblsCannotHaveCopiesCheck(
      String fieldName, String errorMessage) {
    return JsonAttribute.customValidator(
        errorMessage,
        (node, contextPath) -> {
          JsonNode numberOfCopiesNode = node.path(fieldName);
          if (!IS_AN_EBL.test(node)) {
            return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
          }
          if (numberOfCopiesNode.isMissingNode() || numberOfCopiesNode.asText().equals(ZERO)) {
            return ConformanceCheckResult.simple(Set.of());
          }
          String path = concatContextPath(contextPath, fieldName);
          return ConformanceCheckResult.simple(Set.of("%s at %s.".formatted(errorMessage, path)));
        });
  }

  static final JsonRebasableContentCheck SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES =
      eblsCannotHaveOriginalsCheck(
          NUMBER_OF_ORIGINALS_WITH_CHARGES,
          "'%s' must be absent for SWBs('%s'=%s)."
              .formatted(NUMBER_OF_ORIGINALS_WITH_CHARGES, TRANSPORT_DOCUMENT_TYPE_CODE, SWB));

  static final JsonRebasableContentCheck SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES =
      eblsCannotHaveOriginalsCheck(
          NUMBER_OF_ORIGINALS_WITHOUT_CHARGES,
          "'%s' must be absent for SWBs('%s'=%s)."
              .formatted(NUMBER_OF_ORIGINALS_WITHOUT_CHARGES, TRANSPORT_DOCUMENT_TYPE_CODE, SWB));

  private static JsonRebasableContentCheck eblsCannotHaveOriginalsCheck(
      String fieldName, String errorMessage) {
    return JsonAttribute.customValidator(
        errorMessage,
        (node, contextPath) -> {
          JsonNode numberOfOriginalsNode = node.path(fieldName);
          if (!node.path(TRANSPORT_DOCUMENT_TYPE_CODE).asText("").equals(SWB)) {
            return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
          }
          if (numberOfOriginalsNode.isMissingNode()) {
            return ConformanceCheckResult.simple(Set.of());
          }
          String path = concatContextPath(contextPath, fieldName);
          return ConformanceCheckResult.simple(Set.of("%s at %s.".formatted(errorMessage, path)));
        });
  }

  static final JsonRebasableContentCheck EBL_AT_MOST_ONE_ORIGINAL_TOTAL =
      JsonAttribute.ifThen(
          "Cannot have more than one original in total when '%s'.".formatted(IS_ELECTRONIC),
          IS_AN_EBL,
          JsonAttribute.customValidator(
              "Sum of '%s' and '%s' must be at most 1 for Electronic original Bills of Ladings."
                  .formatted(NUMBER_OF_ORIGINALS_WITHOUT_CHARGES, NUMBER_OF_ORIGINALS_WITH_CHARGES),
              (node, contextPath) -> {
                int withoutCharges = node.path(NUMBER_OF_ORIGINALS_WITHOUT_CHARGES).asInt(0);
                int withCharges = node.path(NUMBER_OF_ORIGINALS_WITH_CHARGES).asInt(0);
                int total = withoutCharges + withCharges;

                if (total > 1) {
                  return ConformanceCheckResult.simple(
                      Set.of(
                          "The sum of '%s' (%d) and '%s' (%d) cannot exceed 1 for Electronic original Bills of Ladings, but was %d at '%s'."
                              .formatted(
                                  NUMBER_OF_ORIGINALS_WITHOUT_CHARGES,
                                  withoutCharges,
                                  NUMBER_OF_ORIGINALS_WITH_CHARGES,
                                  withCharges,
                                  total,
                                  contextPath)));
                }
                return ConformanceCheckResult.simple(Set.of());
              }));

  static final JsonRebasableContentCheck VALIDATE_DOCUMENT_PARTY =
      JsonAttribute.customValidator(
          "Validate that '%s', '%s', or '%s' is present in every supported '%s'."
              .formatted(
                  ADDRESS,
                  ADDRESS_LINES,
                  IDENTIFYING_CODES,
                  DOCUMENT_PARTIES),
          (body, ignoredContextPath) -> {
            var documentParties = body.path(DOCUMENT_PARTIES);
            var issues = new LinkedHashSet<ConformanceError>();
            boolean hasApplicableParty = false;

            for (Map.Entry<String, JsonNode> field : documentParties.properties()) {
              JsonNode childNode = field.getValue();

              switch (field.getKey()) {
                case OTHER -> {
                  var otherDocumentParties = field.getValue();
                  for (JsonNode node : otherDocumentParties) {
                    hasApplicableParty = true;
                    issues.addAll(validateDocumentPartyFields(node.path(PARTY), field.getKey()));
                  }
                }
                case NOTIFY_PARTIES -> {
                  var notifyParties = field.getValue();
                  for (JsonNode node : notifyParties) {
                    hasApplicableParty = true;
                    issues.addAll(validateDocumentPartyFields(node, field.getKey()));
                  }
                }
                case SHIPPER, CONSIGNEE, ENDORSEE, ON_BEHALF_OF_SHIPPER -> {
                  hasApplicableParty = true;
                  issues.addAll(validateDocumentPartyFields(childNode, field.getKey()));
                }
                default -> {
                  // This workbook rule intentionally applies to only the six party types above.
                }
              }
            }
            if (!hasApplicableParty) {
              issues.add(ConformanceError.irrelevant());
            }
            return ConformanceCheckResult.withRelevance(issues);
          });

  private static Set<ConformanceError> validateDocumentPartyFields(
      JsonNode documentPartyNode, String partyName) {
    var issues = new LinkedHashSet<ConformanceError>();
    var address = documentPartyNode.path(ADDRESS);
    var addressLines = documentPartyNode.path(ADDRESS_LINES);
    var identifyingCodes = documentPartyNode.path(IDENTIFYING_CODES);
    if (JsonUtil.isMissingOrEmpty(address)
        && JsonUtil.isMissingOrEmpty(addressLines)
        && JsonUtil.isMissingOrEmpty(identifyingCodes)) {
      issues.add(
          ConformanceError.error(
              "At least one of '%s', '%s', or '%s' must be provided in party '%s'."
                  .formatted(ADDRESS, ADDRESS_LINES, IDENTIFYING_CODES, partyName)));
    }
    return issues;
  }

  static final JsonRebasableContentCheck DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The code in '%s' is known.".formatted(CODE_LIST_PROVIDER),
          mav -> {
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, SHIPPER, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, CONSIGNEE, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, ENDORSEE, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, ISSUE_TO, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, ISSUING_PARTY, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES,
                    ON_BEHALF_OF_SHIPPER,
                    IDENTIFYING_CODES,
                    CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, SELLER, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, BUYER, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_x_S_x_S.formatted(
                    DOCUMENT_PARTIES, NOTIFY_PARTIES, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, OTHER, PARTY, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_x_S_S_S_x_S.formatted(
                    HOUSE_BILL_OF_LADINGS,
                    DOCUMENT_PARTIES,
                    SHIPPER,
                    IDENTIFYING_CODES,
                    CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_x_S_S_S_x_S.formatted(
                    HOUSE_BILL_OF_LADINGS,
                    DOCUMENT_PARTIES,
                    CONSIGNEE,
                    IDENTIFYING_CODES,
                    CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_x_S_S_S_x_S.formatted(
                    HOUSE_BILL_OF_LADINGS,
                    DOCUMENT_PARTIES,
                    NOTIFY_PARTY,
                    IDENTIFYING_CODES,
                    CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_x_S_S_S_x_S.formatted(
                    HOUSE_BILL_OF_LADINGS,
                    DOCUMENT_PARTIES,
                    SELLER,
                    IDENTIFYING_CODES,
                    CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_x_S_S_S_x_S.formatted(
                    HOUSE_BILL_OF_LADINGS,
                    DOCUMENT_PARTIES,
                    BUYER,
                    IDENTIFYING_CODES,
                    CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_x_S_S_S_S_x_S.formatted(
                    HOUSE_BILL_OF_LADINGS,
                    DOCUMENT_PARTIES,
                    OTHER,
                    PARTY,
                    IDENTIFYING_CODES,
                    CODE_LIST_PROVIDER));
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES));

  static final JsonRebasableContentCheck TD_DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The code in '%s' is known for every Transport Document party."
              .formatted(CODE_LIST_PROVIDER),
          mav -> {
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, SHIPPER, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, CONSIGNEE, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, ENDORSEE, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES,
                    ON_BEHALF_OF_SHIPPER,
                    IDENTIFYING_CODES,
                    CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES,
                    ON_BEHALF_OF_CONSIGNEE,
                    IDENTIFYING_CODES,
                    CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, ISSUING_PARTY, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_x_S_x_S.formatted(
                    DOCUMENT_PARTIES, NOTIFY_PARTIES, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_x_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, OTHER, PARTY, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES));

  private static final JsonRebasableContentCheck NOTIFY_PARTIES_REQUIRED_IN_NEGOTIABLE_BLS =
      JsonAttribute.ifThen(
          "The '%s.%s' attribute is mandatory when '%s' is true."
              .formatted(DOCUMENT_PARTIES, NOTIFY_PARTIES, IS_TO_ORDER),
          JsonAttribute.isTrue(IS_TO_ORDER),
          JsonAttribute.at(
              JsonPointer.compile(SS.formatted(DOCUMENT_PARTIES, NOTIFY_PARTIES)),
              JsonAttribute.matchedMustBeNonEmpty()));

  private static final Consumer<MultiAttributeValidator> ALL_REFERENCE_TYPES =
      mav -> {
        mav.submitAllMatching(S_x_S.formatted(REFERENCES, TYPE));
        mav.submitAllMatching(S_x_S_x_S.formatted(UTILIZED_TRANSPORT_EQUIPMENTS, REFERENCES, TYPE));
      };

  static final JsonRebasableContentCheck VALID_WOOD_DECLARATIONS =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate the '%s' against known dataset.".formatted(WOOD_DECLARATION),
          mav ->
              mav.submitAllMatching(
                  S_x_S_x_S_S.formatted(
                      CONSIGNMENT_ITEMS, CARGO_ITEMS, OUTER_PACKAGING, WOOD_DECLARATION)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.WOOD_DECLARATION_VALUES));

  private static final JsonRebasableContentCheck VALID_REFERENCE_TYPES =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All reference '%s' fields must be valid.".formatted(TYPE),
          ALL_REFERENCE_TYPES,
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.REFERENCE_TYPE));

  private static final JsonRebasableContentCheck VALID_TD_REFERENCE_TYPES =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All general reference '%s' fields must be valid.".formatted(TYPE),
          mav -> mav.submitAllMatching(S_x_S.formatted(REFERENCES, TYPE)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.REFERENCE_TYPE));

  static final JsonRebasableContentCheck VALID_CONSIGNMENT_ITEMS_REFERENCE_TYPES =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All '%s.*.%s.*.%s' fields must be valid.".formatted(CONSIGNMENT_ITEMS, REFERENCES, TYPE),
          mav -> mav.submitAllMatching(S_x_S_x_S.formatted(CONSIGNMENT_ITEMS, REFERENCES, TYPE)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              EblDatasets.CONSIGNMENT_ITEMS_REFERENCE_TYPE));

  private static final JsonRebasableContentCheck TLR_CC_T_COMBINATION_UNIQUE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Each combination of '%s' and '%s' can be used at most once."
              .formatted(COUNTRY_CODE, TYPE),
          mav -> {
            mav.submitAllMatching(S_S.formatted(ISSUING_PARTY, TAX_LEGAL_REFERENCES));
            mav.submitAllMatching(S_x_S_S.formatted(DOCUMENT_PARTIES, PARTY, TAX_LEGAL_REFERENCES));
          },
          JsonAttribute.unique(COUNTRY_CODE, TYPE));

  private static final Consumer<MultiAttributeValidator> DISPLAYED_ADDRESS_MAV_CONSUMER =
      mav -> {
        mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, SHIPPER, DISPLAYED_ADDRESS));
        mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, CONSIGNEE, DISPLAYED_ADDRESS));
        mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, ENDORSEE, DISPLAYED_ADDRESS));
        mav.submitAllMatching(
            S_S_S.formatted(DOCUMENT_PARTIES, ISSUING_PARTY, DISPLAYED_ADDRESS));
        mav.submitAllMatching(
            S_S_x_S.formatted(DOCUMENT_PARTIES, NOTIFY_PARTIES, DISPLAYED_ADDRESS));
        mav.submitAllMatching(
            S_S_S.formatted(DOCUMENT_PARTIES, ON_BEHALF_OF_SHIPPER, DISPLAYED_ADDRESS));
        mav.submitAllMatching(
            S_S_x_S_S.formatted(DOCUMENT_PARTIES, OTHER, PARTY, DISPLAYED_ADDRESS));
      };

  static final JsonRebasableContentCheck EBL_DISPLAYED_ADDRESS_LIMIT =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Every displayed address has the allowed number and length of lines.",
          DISPLAYED_ADDRESS_MAV_CONSUMER,
          (node, contextPath) -> {
            if (node.isMissingNode()) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            var issues = new LinkedHashSet<String>();
            for (int index = 0; index < node.size(); index++) {
              if (node.path(index).asText().length() > 35) {
                issues.add(
                    "The displayed-address line at '%s[%d]' exceeds 35 characters."
                        .formatted(contextPath, index));
              }
            }
            return ConformanceCheckResult.simple(issues);
          });

  static final JsonRebasableContentCheck DISPLAYED_ADDRESS_LINE_COUNT =
      JsonAttribute.customValidator(
          "A physical B/L has at most 2 displayed-address lines and an electronic B/L at most 6.",
          (body, contextPath) -> {
            int limit = body.path(IS_ELECTRONIC).asBoolean(false) ? 6 : 2;
            var issues = new LinkedHashSet<String>();
            validateDisplayedAddressLineCounts(body.path(DOCUMENT_PARTIES), contextPath, limit, issues);
            return ConformanceCheckResult.simple(issues);
          });

  private static void validateDisplayedAddressLineCounts(
      JsonNode documentParties, String contextPath, int limit, Set<String> issues) {
    for (String partyName :
        List.of(SHIPPER, CONSIGNEE, ENDORSEE, ISSUING_PARTY, ON_BEHALF_OF_SHIPPER)) {
      validateDisplayedAddressLineCount(
          documentParties.path(partyName),
          concatContextPath(contextPath, S_S.formatted(DOCUMENT_PARTIES, partyName)),
          limit,
          issues);
    }
    validateDisplayedAddressArrayLineCounts(
        documentParties.path(NOTIFY_PARTIES),
        concatContextPath(contextPath, S_S.formatted(DOCUMENT_PARTIES, NOTIFY_PARTIES)),
        false,
        limit,
        issues);
    validateDisplayedAddressArrayLineCounts(
        documentParties.path(OTHER),
        concatContextPath(contextPath, S_S.formatted(DOCUMENT_PARTIES, OTHER)),
        true,
        limit,
        issues);
  }

  private static void validateDisplayedAddressArrayLineCounts(
      JsonNode parties,
      String contextPath,
      boolean wrappedInParty,
      int limit,
      Set<String> issues) {
    for (int index = 0; index < parties.size(); index++) {
      JsonNode party = wrappedInParty ? parties.path(index).path(PARTY) : parties.path(index);
      validateDisplayedAddressLineCount(
          party, "%s[%d]".formatted(contextPath, index), limit, issues);
    }
  }

  private static void validateDisplayedAddressLineCount(
      JsonNode party, String contextPath, int limit, Set<String> issues) {
    JsonNode displayedAddress = party.path(DISPLAYED_ADDRESS);
    if (displayedAddress.isArray() && displayedAddress.size() > limit) {
      issues.add(
          "The displayed address at '%s.%s' has %d lines; the limit is %d."
              .formatted(contextPath, DISPLAYED_ADDRESS, displayedAddress.size(), limit));
    }
  }

  private static final Consumer<MultiAttributeValidator> ALL_UTE =
      mav -> mav.submitAllMatching(S_x.formatted(UTILIZED_TRANSPORT_EQUIPMENTS));

  private static final Predicate<JsonNode> HAS_ISO_EQUIPMENT_CODE =
      uteNode -> {
        var isoEquipmentNode = uteNode.path(EQUIPMENT).path(ISO_EQUIPMENT_CODE);
        return isoEquipmentNode.isTextual();
      };

  private static final Predicate<JsonNode> IS_ISO_EQUIPMENT_CONTAINER_REEFER =
      uteNode -> {
        var isoEquipmentNode = uteNode.path(EQUIPMENT).path(ISO_EQUIPMENT_CODE);
        return isReeferContainerSizeTypeCode(isoEquipmentNode.asText(""));
      };

  private static final JsonRebasableContentCheck ISO_EQUIPMENT_CODE_IMPLIES_REEFER =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate '%s' and reefer attributes.".formatted(UTILIZED_TRANSPORT_EQUIPMENTS),
          ALL_UTE,
          JsonAttribute.ifMatchedThenElse(
              HAS_ISO_EQUIPMENT_CODE,
              JsonAttribute.ifMatchedThenElse(
                  IS_ISO_EQUIPMENT_CONTAINER_REEFER,
                  JsonAttribute.path(IS_NON_OPERATING_REEFER, JsonAttribute.matchedMustBePresent()),
                  JsonAttribute.combine(
                      JsonAttribute.path(
                          IS_NON_OPERATING_REEFER, JsonAttribute.matchedMustBeAbsent()),
                      JsonAttribute.path(
                          ACTIVE_REEFER_SETTINGS, JsonAttribute.matchedMustBeAbsent()))),
              // If there is no ISOEquipmentCode, then we can only say that `activeReeferSettings`
              // implies
              // `isNonOperatingReefer=False` (the `=False` part is checked elsewhere).
              JsonAttribute.presenceImpliesOtherField(
                  ACTIVE_REEFER_SETTINGS, IS_NON_OPERATING_REEFER)));

  private static final JsonRebasableContentCheck NOR_IS_TRUE_IMPLIES_NO_ACTIVE_REEFER =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All '%s' where '%s' is true cannot have '%s'."
              .formatted(
                  UTILIZED_TRANSPORT_EQUIPMENTS, IS_NON_OPERATING_REEFER, ACTIVE_REEFER_SETTINGS),
          ALL_UTE,
          JsonAttribute.ifMatchedThen(
              JsonAttribute.isTrue(IS_NON_OPERATING_REEFER),
              JsonAttribute.path(ACTIVE_REEFER_SETTINGS, JsonAttribute.matchedMustBeAbsent())));

  private static final JsonRebasableContentCheck CR_CC_T_CODES_UNIQUE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The combination of '%s' and '%s' in '*.%s' must be unique."
              .formatted(COUNTRY_CODE, TYPE, CUSTOMS_REFERENCES),
          mav -> {
            mav.submitAllMatching(CUSTOMS_REFERENCES);
            mav.submitAllMatching(S_x_S.formatted(CONSIGNMENT_ITEMS, CUSTOMS_REFERENCES));
            mav.submitAllMatching(
                S_x_S_x_S.formatted(CONSIGNMENT_ITEMS, CARGO_ITEMS, CUSTOMS_REFERENCES));
            mav.submitAllMatching(
                S_x_S.formatted(UTILIZED_TRANSPORT_EQUIPMENTS, CUSTOMS_REFERENCES));
          },
          JsonAttribute.unique(COUNTRY_CODE, TYPE));

  static final JsonRebasableContentCheck NATIONAL_COMMODITY_CODE_IS_VALID =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate that every national commodity-code '%s' is a known code.".formatted(TYPE),
          mav -> {
            mav.submitAllMatching(
                S_x_S_x_S.formatted(CONSIGNMENT_ITEMS, NATIONAL_COMMODITY_CODES, TYPE));
            mav.submitAllMatching(
                S_x_S_x_S.formatted(
                    CONSIGNMENT_ITEMS, EXTENDED_NATIONAL_COMMODITY_CODES, TYPE));
            mav.submitAllMatching(
                S_x_S_x_S_x_S.formatted(
                    CONSIGNMENT_ITEMS, CARGO_ITEMS, NATIONAL_COMMODITY_CODES, TYPE));
              mav.submitAllMatching(
                S_x_S_x_S_x_S.formatted(
                    CONSIGNMENT_ITEMS, CARGO_ITEMS, EXTENDED_NATIONAL_COMMODITY_CODES, TYPE));
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(NATIONAL_COMMODITY_CODES_SET));

  static final JsonRebasableContentCheck VALID_EBL_PLATFORMS =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Every '%s' in '%s' is valid.".formatted(EBL_PLATFORM, DOCUMENT_PARTIES),
          mav -> {
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, SHIPPER, EBL_PLATFORM));
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, CONSIGNEE, EBL_PLATFORM));
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, ENDORSEE, EBL_PLATFORM));
            mav.submitAllMatching(
                S_S_S.formatted(DOCUMENT_PARTIES, ISSUING_PARTY, EBL_PLATFORM));
            mav.submitAllMatching(
                S_S_S.formatted(DOCUMENT_PARTIES, ON_BEHALF_OF_SHIPPER, EBL_PLATFORM));
            mav.submitAllMatching(
                S_S_x_S.formatted(DOCUMENT_PARTIES, NOTIFY_PARTIES, EBL_PLATFORM));
            mav.submitAllMatching(
                S_S_x_S_S.formatted(DOCUMENT_PARTIES, OTHER, PARTY, EBL_PLATFORM));
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.EBL_PLATFORMS_DATASET));

  static final JsonRebasableContentCheck ISSUE_DATE_REQUIRED_WHEN_ISSUED =
      JsonAttribute.ifThen(
          "'%s' is required when '%s' is 'ISSUED'."
              .formatted(ISSUE_DATE, TRANSPORT_DOCUMENT_STATUS),
          body -> "ISSUED".equals(body.path(TRANSPORT_DOCUMENT_STATUS).asText()),
          JsonAttribute.mustBePresent(JsonPointer.compile(S.formatted(ISSUE_DATE))));

  static final JsonRebasableContentCheck RIDER_PAGES_NOT_ALLOWED_FOR_ELECTRONIC_TD =
      JsonAttribute.ifThen(
          "'%s' must be absent when '%s' is true."
              .formatted(NUMBER_OF_RIDER_PAGES, IS_ELECTRONIC),
          IS_ELECTRONIC_PREDICATE,
          JsonAttribute.mustBeAbsent(JsonPointer.compile(S.formatted(NUMBER_OF_RIDER_PAGES))));

  static final JsonRebasableContentCheck VALID_VESSEL_VOYAGE_ROLES =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Every vessel-voyage '%s' is valid.".formatted(ROLE),
          mav -> mav.submitAllMatching(S_S_x_S.formatted(TRANSPORTS, VESSEL_VOYAGES, ROLE)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.VESSEL_VOYAGE_ROLES));

  static final JsonRebasableContentCheck VALID_INNER_PACKAGING_QUANTITIES =
      JsonAttribute.customValidator(
          "Every inner-packaging quantity is a positive integer.",
          (body, contextPath) -> {
            var issues = new LinkedHashSet<String>();
            for (int ci = 0; ci < body.path(CONSIGNMENT_ITEMS).size(); ci++) {
              JsonNode cargoItems = body.path(CONSIGNMENT_ITEMS).path(ci).path(CARGO_ITEMS);
              for (int cargo = 0; cargo < cargoItems.size(); cargo++) {
                JsonNode dangerousGoods =
                    cargoItems.path(cargo).path(OUTER_PACKAGING).path(DANGEROUS_GOODS);
                for (int dg = 0; dg < dangerousGoods.size(); dg++) {
                  String path =
                      concatContextPath(
                          contextPath,
                          "%s[%d].%s[%d].%s.%s[%d].%s"
                              .formatted(
                                  CONSIGNMENT_ITEMS,
                                  ci,
                                  CARGO_ITEMS,
                                  cargo,
                                  OUTER_PACKAGING,
                                  DANGEROUS_GOODS,
                                  dg,
                                  INNER_PACKAGINGS));
                  validateInnerPackagingQuantities(
                      dangerousGoods.path(dg).path(INNER_PACKAGINGS), path, issues);
                }
              }
            }
            return ConformanceCheckResult.simple(issues);
          });

  private static void validateInnerPackagingQuantities(
      JsonNode innerPackagings, String contextPath, Set<String> issues) {
    for (int index = 0; index < innerPackagings.size(); index++) {
      JsonNode innerPackaging = innerPackagings.path(index);
      JsonNode quantity = innerPackaging.path(QUANTITY);
      String itemPath = "%s[%d]".formatted(contextPath, index);
      if (!quantity.isIntegralNumber() || quantity.asLong() <= 0) {
        issues.add(
            "The value at '%s.%s' must be a positive integer."
                .formatted(itemPath, QUANTITY));
      }
      validateInnerPackagingQuantities(
          innerPackaging.path(INNER_PACKAGINGS),
          concatContextPath(itemPath, INNER_PACKAGINGS),
          issues);
    }
  }

  static final JsonRebasableContentCheck VALID_CARGO_MEASUREMENT_PRECISION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Cargo weights have at most 3 decimal places and cargo volumes at most 4.",
          mav -> {
            mav.submitAllMatching(
                S_x_S_x_S_S.formatted(
                    CONSIGNMENT_ITEMS, CARGO_ITEMS, CARGO_GROSS_WEIGHT, VALUE));
            mav.submitAllMatching(
                S_x_S_x_S_S.formatted(
                    CONSIGNMENT_ITEMS, CARGO_ITEMS, CARGO_NET_WEIGHT, VALUE));
          },
          decimalScaleAtMost(3));

  static final JsonRebasableContentCheck VALID_CARGO_VOLUME_PRECISION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Cargo volumes have at most 4 decimal places.",
          mav ->
              mav.submitAllMatching(
                  S_x_S_x_S_S.formatted(
                      CONSIGNMENT_ITEMS, CARGO_ITEMS, CARGO_GROSS_VOLUME, VALUE)),
          decimalScaleAtMost(4));

  private static JsonContentMatchedValidation decimalScaleAtMost(int maximumScale) {
    return (node, contextPath) -> {
      if (node.isMissingNode()) {
        return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
      }
      if (!node.isNumber() || Math.max(0, node.decimalValue().scale()) > maximumScale) {
        return ConformanceCheckResult.simple(
            Set.of(
                "The numeric value at '%s' must have at most %d decimal places."
                    .formatted(contextPath, maximumScale)));
      }
      return ConformanceCheckResult.simple(Set.of());
    };
  }

  public static final JsonRebasableContentCheck DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE =
      JsonAttribute.customValidator(
          "Each document party can be used at most once.",
          JsonAttribute.path(
              DOCUMENT_PARTIES, JsonAttribute.path(OTHER, JsonAttribute.unique(PARTY_FUNCTION))));

  public static final JsonRebasableContentCheck VALIDATE_DOCUMENT_PARTIES_MATCH_EBL =
      JsonAttribute.customValidator(
          "Validate '%s' match the eBL type.".formatted(DOCUMENT_PARTIES),
          (body, contextPath) -> {
            var issues = new LinkedHashSet<String>();
            var documentParties = body.path(DOCUMENT_PARTIES);
            var isToOrder = body.path(IS_TO_ORDER).asBoolean(false);

            var isToOrderPath = concatContextPath(contextPath, IS_TO_ORDER);

            if (isToOrder) {
              if (documentParties.has(CONSIGNEE)) {
                var documentPartiesPath =
                    concatContextPath(contextPath, S_S.formatted(DOCUMENT_PARTIES, CONSIGNEE));
                var endorseePartiesPath =
                    concatContextPath(contextPath, S_S.formatted(DOCUMENT_PARTIES, ENDORSEE));
                issues.add(
                    "The '%s' party cannot be used when '%s' is true (use '%s' instead)."
                        .formatted(documentPartiesPath, isToOrderPath, endorseePartiesPath));
              }
            } else {
              if (!documentParties.has(CONSIGNEE)) {
                var documentPartiesPath =
                    concatContextPath(contextPath, S_S.formatted(DOCUMENT_PARTIES, CONSIGNEE));
                issues.add(
                    "The '%s' party is mandatory when '%s' is false."
                        .formatted(documentPartiesPath, isToOrderPath));
              }
              if (documentParties.has(ENDORSEE)) {
                var documentPartiesPath =
                    concatContextPath(contextPath, S_S.formatted(DOCUMENT_PARTIES, ENDORSEE));
                issues.add(
                    "The '%s' party cannot be used when '%s' is false."
                        .formatted(documentPartiesPath, isToOrderPath));
              }
            }
            return ConformanceCheckResult.simple(issues);
          });

  private static Consumer<MultiAttributeValidator> allDg(
      Consumer<MultiAttributeValidator.AttributePathBuilder> consumer) {
    return mav ->
        consumer.accept(
            mav.path(CONSIGNMENT_ITEMS)
                .all()
                .path(CARGO_ITEMS)
                .all()
                .path(OUTER_PACKAGING)
                .path(DANGEROUS_GOODS)
                .all());
  }

  private static final JsonRebasableContentCheck CARGO_ITEM_REFERENCES_KNOWN_EQUIPMENT =
      JsonAttribute.customValidator(
          "Equipment References in '%s' must be present in '%s'."
              .formatted(CARGO_ITEMS, UTILIZED_TRANSPORT_EQUIPMENTS),
          (body, contextPath) -> {
            var knownEquipmentReferences = allEquipmentReferences(body);
            var missing = new LinkedHashSet<String>();
            for (var consignmentItem : body.path(CONSIGNMENT_ITEMS)) {
              for (var cargoItem : consignmentItem.path(CARGO_ITEMS)) {
                var ref = cargoItem.path(EQUIPMENT_REFERENCE).asText(null);
                if (ref == null) {
                  // Schema validated
                  continue;
                }
                if (!knownEquipmentReferences.contains(ref)) {
                  missing.add(ref);
                }
              }
            }
            var path = concatContextPath(contextPath, UTILIZED_TRANSPORT_EQUIPMENTS);
            return ConformanceCheckResult.simple(
                missing.stream()
                    .map(
                        ref ->
                            "The '%s' '%s' was used in a '%s' element but was not present in '%s'."
                                .formatted(EQUIPMENT_REFERENCE, ref, CARGO_ITEMS, path))
                    .collect(Collectors.toSet()));
          });

  private static final JsonRebasableContentCheck UTE_EQUIPMENT_REFERENCE_UNIQUE =
      JsonAttribute.customValidator(
          "Equipment References in '%s' must be unique.".formatted(UTILIZED_TRANSPORT_EQUIPMENTS),
          (body, contextPath) -> {
            var duplicates = new LinkedHashSet<String>();
            allEquipmentReferences(body, duplicates);
            var path = concatContextPath(contextPath, UTILIZED_TRANSPORT_EQUIPMENTS);
            return ConformanceCheckResult.simple(
                duplicates.stream()
                    .map(
                        ref ->
                            "The '%s' '%s' was used more than once in '%s'."
                                .formatted(EQUIPMENT_REFERENCE, ref, path))
                    .collect(Collectors.toSet()));
          });

  private static final JsonRebasableContentCheck ADVANCED_MANIFEST_FILING_CODES_UNIQUE =
      JsonAttribute.customValidator(
          "The combination of '%s' and '%s' in '%s' must be unique."
              .formatted(COUNTRY_CODE, MANIFEST_TYPE_CODE, ADVANCE_MANIFEST_FILINGS),
          JsonAttribute.unique(COUNTRY_CODE, MANIFEST_TYPE_CODE));

  static JsonRebasableContentCheck ENS_MANIFEST_TYPE_REQUIRES_HBL_ISSUED =
      JsonAttribute.ifThen(
          "If any '%s' in '%s' is '%s', then '%s' is required."
              .formatted(
                  MANIFEST_TYPE_CODE,
                  ADVANCE_MANIFEST_FILINGS,
                  ENS,
                  IS_HOUSE_BILL_OF_LADINGS_ISSUED),
          node -> {
            JsonNode advanceManifestFilings = node.path(ADVANCE_MANIFEST_FILINGS);
            if (advanceManifestFilings.isMissingNode() || !advanceManifestFilings.isArray()) {
              return false;
            }
            for (JsonNode filing : advanceManifestFilings) {
              if (ENS.equals(filing.path(MANIFEST_TYPE_CODE).asText())) {
                return true;
              }
            }
            return false;
          },
          JsonAttribute.mustBePresent(
              JsonPointer.compile(S.formatted(IS_HOUSE_BILL_OF_LADINGS_ISSUED))));

  static final JsonRebasableContentCheck HBL_NOTIFY_PARTY_REQUIRED_IF_TO_ORDER =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If '%s' is true in any '%s', then '%s' is required in '%s' of that '%s'."
              .formatted(
                  IS_TO_ORDER,
                  HOUSE_BILL_OF_LADINGS,
                  NOTIFY_PARTY,
                  DOCUMENT_PARTIES,
                  HOUSE_BILL_OF_LADINGS),
          mav -> mav.submitAllMatching(S_x.formatted(HOUSE_BILL_OF_LADINGS)),
          (node, contextPath) -> {
            boolean isToOrder = node.path(IS_TO_ORDER).asBoolean(false);
            if (!isToOrder) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            if (node.path(DOCUMENT_PARTIES).path(NOTIFY_PARTY).isMissingNode()) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "If '%s' is true in any '%s', then '%s' is required in '%s' of that '%s' at %s."
                          .formatted(
                              IS_TO_ORDER,
                              HOUSE_BILL_OF_LADINGS,
                              NOTIFY_PARTY,
                              DOCUMENT_PARTIES,
                              HOUSE_BILL_OF_LADINGS,
                              contextPath)));
            }
            return ConformanceCheckResult.simple(Set.of());
          });

  static final JsonRebasableContentCheck VALID_HBL_METHOD_OF_PAYMENT =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All '%s.*.%s' must be valid.".formatted(HOUSE_BILL_OF_LADINGS, METHOD_OF_PAYMENT),
          mav -> mav.submitAllMatching(S_x_S.formatted(HOUSE_BILL_OF_LADINGS, METHOD_OF_PAYMENT)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.METHOD_OF_PAYMENT_SET));

  private static final JsonRebasableContentCheck VALIDATE_CARRIER_CODE_AND_LIST_PROVIDER =
      JsonAttribute.customValidator(
          "If '%s' is present, then '%s' is required and vice versa."
              .formatted(CARRIER_CODE, CARRIER_CODE_LIST_PROVIDER),
          (node, contextPath) -> {
            boolean hasCarrierCode = !node.path(CARRIER_CODE).isMissingNode();
            boolean hasProvider = !node.path(CARRIER_CODE_LIST_PROVIDER).isMissingNode();

            if (!hasCarrierCode && !hasProvider) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }

            if (hasCarrierCode && !hasProvider) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "'%s' is required when '%s' is present at %s."
                          .formatted(CARRIER_CODE_LIST_PROVIDER, CARRIER_CODE, contextPath)));
            }
            if (!hasCarrierCode) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "'%s' is required when '%s' is present at %s."
                          .formatted(CARRIER_CODE, CARRIER_CODE_LIST_PROVIDER, contextPath)));
            }
            return ConformanceCheckResult.simple(Set.of());
          });

  static final JsonRebasableContentCheck VALID_TYPE_OF_PERSON =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate '%s' values in '%s'.".formatted(TYPE_OF_PERSON, DOCUMENT_PARTIES),
          mav -> {
            // single objects
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, SHIPPER, TYPE_OF_PERSON));
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, CONSIGNEE, TYPE_OF_PERSON));
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, ENDORSEE, TYPE_OF_PERSON));
            mav.submitAllMatching(
                S_S_S.formatted(DOCUMENT_PARTIES, ISSUING_PARTY, TYPE_OF_PERSON));
            mav.submitAllMatching(
                S_S_S.formatted(DOCUMENT_PARTIES, ON_BEHALF_OF_SHIPPER, TYPE_OF_PERSON));
            mav.submitAllMatching(
                S_S_x_S.formatted(DOCUMENT_PARTIES, NOTIFY_PARTIES, TYPE_OF_PERSON));
            mav.submitAllMatching(
                S_S_x_S_S.formatted(DOCUMENT_PARTIES, OTHER, PARTY, TYPE_OF_PERSON));
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.TYPE_OF_PERSON_SET));

  private static final Predicate<JsonNode> NUMBER_OF_PACKAGES_REQUIRED =
      packaging -> {
        String packageCode = packaging.path(PACKAGE_CODE).asText(null);
        return packageCode != null && !EXEMPT_PACKAGE_CODES.contains(packageCode);
      };

  static final JsonRebasableContentCheck NUMBER_OF_PACKAGES_CONDITIONAL_CHECK =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If '%s' in '%s' is not exempt, then '%s' is required."
              .formatted(PACKAGE_CODE, OUTER_PACKAGING, NUMBER_OF_PACKAGES),
          mav ->
              mav.submitAllMatching(
                  S_x_S_x_S_x_S.formatted(
                      HOUSE_BILL_OF_LADINGS, CONSIGNMENT_ITEMS, CARGO_ITEMS, OUTER_PACKAGING)),
          JsonAttribute.ifMatchedThen(
              NUMBER_OF_PACKAGES_REQUIRED,
              JsonAttribute.path(NUMBER_OF_PACKAGES, JsonAttribute.matchedMustBePresent())));

  private static final Predicate<JsonNode> IDENTIFICATION_NUMBER_REQUIRED =
      filingsNode ->
          ENS.equals(filingsNode.path(MANIFEST_TYPE_CODE).asText())
              && SELF.equals(filingsNode.path(AMF_HBL_PERFORMED_BY).asText());

  static final JsonRebasableContentCheck IDENTIFICATION_NUMBER_REQUIRED_IF_ENS_AND_SELF =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If '%s' is '%s' and '%s' is '%s', then '%s' is required."
              .formatted(
                  MANIFEST_TYPE_CODE, ENS, AMF_HBL_PERFORMED_BY, SELF, IDENTIFICATION_NUMBER),
          mav -> mav.submitAllMatching(S_x.formatted(ADVANCE_MANIFEST_FILINGS)),
          JsonAttribute.ifMatchedThen(
              IDENTIFICATION_NUMBER_REQUIRED,
              JsonAttribute.path(IDENTIFICATION_NUMBER, JsonAttribute.matchedMustBePresent())));

  private static final Predicate<JsonNode> SELF_FILER_CODE_REQUIRED =
      filingsNode ->
          (ACI.equals(filingsNode.path(MANIFEST_TYPE_CODE).asText())
                  || ACE.equals(filingsNode.path(MANIFEST_TYPE_CODE).asText()))
              && SELF.equals(filingsNode.path(AMF_HBL_PERFORMED_BY).asText());

  static final JsonRebasableContentCheck SELF_FILER_CODE_REQUIRED_IF_ACE_ACI_AND_SELF =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If '%s' is '%s'/'%s' and '%s' is '%s', then '%s' is required."
              .formatted(MANIFEST_TYPE_CODE, ACE, ACI, AMF_HBL_PERFORMED_BY, SELF, SELF_FILER_CODE),
          mav -> mav.submitAllMatching(S_x.formatted(ADVANCE_MANIFEST_FILINGS)),
          JsonAttribute.ifMatchedThen(
              SELF_FILER_CODE_REQUIRED,
              JsonAttribute.path(SELF_FILER_CODE, JsonAttribute.matchedMustBePresent())));

  private static final Predicate<JsonNode> LOCATION_NAME_REQUIRED =
      place -> place.path(UN_LOCATION_CODE).isMissingNode();

  private static final Predicate<JsonNode> COUNTRY_CODE_REQUIRED =
      place -> place.path(UN_LOCATION_CODE).isMissingNode();

  static final JsonRebasableContentCheck LOCATION_NAME_CONDITIONAL_VALIDATION_POA =
      JsonAttribute.allIndividualMatchesMustBeValid(
          IF_S_IS_NOT_PROVIDED_IN_S_THEN_S_IS_REQUIRED.formatted(
              UN_LOCATION_CODE, PLACE_OF_ACCEPTANCE, LOCATION_NAME),
          mav -> mav.submitAllMatching(S_x_S.formatted(HOUSE_BILL_OF_LADINGS, PLACE_OF_ACCEPTANCE)),
          JsonAttribute.ifMatchedThen(
              LOCATION_NAME_REQUIRED,
              JsonAttribute.path(LOCATION_NAME, JsonAttribute.matchedMustBePresent())));

  static final JsonRebasableContentCheck LOCATION_NAME_CONDITIONAL_VALIDATION_POFD =
      JsonAttribute.allIndividualMatchesMustBeValid(
          IF_S_IS_NOT_PROVIDED_IN_S_THEN_S_IS_REQUIRED.formatted(
              UN_LOCATION_CODE, PLACE_OF_FINAL_DELIVERY, LOCATION_NAME),
          mav ->
              mav.submitAllMatching(
                  S_x_S.formatted(HOUSE_BILL_OF_LADINGS, PLACE_OF_FINAL_DELIVERY)),
          JsonAttribute.ifMatchedThen(
              LOCATION_NAME_REQUIRED,
              JsonAttribute.path(LOCATION_NAME, JsonAttribute.matchedMustBePresent())));

  static final JsonRebasableContentCheck COUNTRY_CODE_CONDITIONAL_VALIDATION_POA =
      JsonAttribute.allIndividualMatchesMustBeValid(
          IF_S_IS_NOT_PROVIDED_IN_S_THEN_S_IS_REQUIRED.formatted(
              UN_LOCATION_CODE, PLACE_OF_ACCEPTANCE, COUNTRY_CODE),
          mav -> mav.submitAllMatching(S_x_S.formatted(HOUSE_BILL_OF_LADINGS, PLACE_OF_ACCEPTANCE)),
          JsonAttribute.ifMatchedThen(
              COUNTRY_CODE_REQUIRED,
              JsonAttribute.path(COUNTRY_CODE, JsonAttribute.matchedMustBePresent())));

  static final JsonRebasableContentCheck COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD =
      JsonAttribute.allIndividualMatchesMustBeValid(
          IF_S_IS_NOT_PROVIDED_IN_S_THEN_S_IS_REQUIRED.formatted(
              UN_LOCATION_CODE, PLACE_OF_FINAL_DELIVERY, COUNTRY_CODE),
          mav ->
              mav.submitAllMatching(
                  S_x_S.formatted(HOUSE_BILL_OF_LADINGS, PLACE_OF_FINAL_DELIVERY)),
          JsonAttribute.ifMatchedThen(
              COUNTRY_CODE_REQUIRED,
              JsonAttribute.path(COUNTRY_CODE, JsonAttribute.matchedMustBePresent())));

  static final JsonRebasableContentCheck ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If first country in '%s' in '%s' should be '%s' and the last country (if more than one) should be '%s'."
              .formatted(
                  ROUTING_OF_CONSIGNMENT_COUNTRIES,
                  HOUSE_BILL_OF_LADINGS,
                  PLACE_OF_ACCEPTANCE,
                  PLACE_OF_FINAL_DELIVERY),
          mav -> mav.submitAllMatching(S_x.formatted(HOUSE_BILL_OF_LADINGS)),
          (node, contextPath) -> {
            JsonNode routingOfConsignmentCountries = node.path(ROUTING_OF_CONSIGNMENT_COUNTRIES);
            if (JsonUtil.isMissingOrEmpty(routingOfConsignmentCountries)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            String placeOfAcceptanceCountry =
                node.path(PLACE_OF_ACCEPTANCE).path(COUNTRY_CODE).asText(null);
            String placeOfFinalDeliveryCountry =
                node.path(PLACE_OF_FINAL_DELIVERY).path(COUNTRY_CODE).asText(null);
            if ((placeOfAcceptanceCountry != null
                    && !placeOfAcceptanceCountry.equals(
                        routingOfConsignmentCountries.path(0).asText()))
                || (placeOfFinalDeliveryCountry != null
                    && routingOfConsignmentCountries.size() > 1
                    && !placeOfFinalDeliveryCountry.equals(
                        routingOfConsignmentCountries
                            .path(routingOfConsignmentCountries.size() - 1)
                            .asText()))) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "The first country in '%s' should be '%s' and the last country (if more than one) should be '%s' at %s."
                          .formatted(
                              ROUTING_OF_CONSIGNMENT_COUNTRIES,
                              PLACE_OF_ACCEPTANCE,
                              PLACE_OF_FINAL_DELIVERY,
                              contextPath)));
            }
            return ConformanceCheckResult.simple(Set.of());
          });

  static final JsonRebasableContentCheck BUYER_AND_SELLER_CONDITIONAL_CHECK =
      JsonAttribute.customValidator(
          "If '%s' is true, '%s' is '%s', '%s' is '%s' and '%s' is false, then '%s' and '%s' is required."
              .formatted(
                  IS_CARGO_DELIVERED_IN_ICS_2_ZONE,
                  ADVANCE_MANIFEST_FILING_PERFORMED_BY,
                  CARRIER,
                  MANIFEST_TYPE_CODE,
                  ENS,
                  IS_HOUSE_BILL_OF_LADINGS_ISSUED,
                  BUYER,
                  SELLER),
          (node, contextPath) -> {
            JsonNode houseBillOfLadings = node.path(HOUSE_BILL_OF_LADINGS);
            if (houseBillOfLadings.isMissingNode() || !houseBillOfLadings.isArray()) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            JsonNode advanceManifestFilings = node.path(ADVANCE_MANIFEST_FILINGS);
            if (advanceManifestFilings.isMissingNode() || !advanceManifestFilings.isArray()) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            boolean isHouseBlsIssued = node.path(IS_HOUSE_BILL_OF_LADINGS_ISSUED).asBoolean(true);
            if (isHouseBlsIssued) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            int index = 0;
            Set<ConformanceError> issues = new LinkedHashSet<>();
            for (JsonNode hbl : houseBillOfLadings) {
              if (hbl.path(IS_CARGO_DELIVERED_IN_ICS_2_ZONE).asBoolean(false)) {
                for (JsonNode filing : advanceManifestFilings) {
                  if (CARRIER.equals(filing.path(AMF_HBL_PERFORMED_BY).asText())
                      && ENS.equals(filing.path(MANIFEST_TYPE_CODE).asText())
                      && (hbl.path(DOCUMENT_PARTIES).path(BUYER).isMissingNode()
                          || hbl.path(DOCUMENT_PARTIES).path(SELLER).isMissingNode())) {
                    String specificContextPath =
                        concatContextPath(
                            contextPath,
                            "%s[%d].%s".formatted(HOUSE_BILL_OF_LADINGS, index, DOCUMENT_PARTIES));
                    return ConformanceCheckResult.simple(
                        Set.of(
                            "'%s' and '%s' are required in '%s' in '%s' when '%s' is true, '%s' is '%s', '%s' is '%s' and '%s' is false at %s."
                                .formatted(
                                    BUYER,
                                    SELLER,
                                    DOCUMENT_PARTIES,
                                    HOUSE_BILL_OF_LADINGS,
                                    IS_CARGO_DELIVERED_IN_ICS_2_ZONE,
                                    ADVANCE_MANIFEST_FILING_PERFORMED_BY,
                                    CARRIER,
                                    MANIFEST_TYPE_CODE,
                                    ENS,
                                    IS_HOUSE_BILL_OF_LADINGS_ISSUED,
                                    specificContextPath)));
                  }
                }
              } else {
                issues.add(ConformanceError.irrelevant());
              }
              index++;
            }
            return ConformanceCheckResult.withRelevance(issues);
          });

  static final JsonRebasableContentCheck SEND_TO_PLATFORM_CONDITIONAL_CHECK =
      JsonAttribute.ifThenElse(
          "'%s' is mandatory when '%s' is true and '%s' is '%s'."
              .formatted(SEND_TO_PLATFORM, IS_ELECTRONIC, TRANSPORT_DOCUMENT_TYPE_CODE, BOL),
          JsonAttribute.isTrue(JsonPointer.compile(S.formatted(IS_ELECTRONIC))),
          JsonAttribute.ifThenElse(
              "'%s' is '%s'.".formatted(TRANSPORT_DOCUMENT_TYPE_CODE, BOL),
              JsonAttribute.isEqualTo(TRANSPORT_DOCUMENT_TYPE_CODE, BOL),
              JsonAttribute.mustBePresent(SI_REQUEST_SEND_TO_PLATFORM),
              JsonAttribute.mustBeAbsent(SI_REQUEST_SEND_TO_PLATFORM)),
          JsonAttribute.mustBeAbsent(SI_REQUEST_SEND_TO_PLATFORM));

  static final JsonRebasableContentCheck VALID_PARTY_FUNCTION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The '%s' in '%s.%s' is valid.".formatted(PARTY_FUNCTION, DOCUMENT_PARTIES, OTHER),
          mav -> mav.submitAllMatching(S_S_x_S.formatted(DOCUMENT_PARTIES, OTHER, PARTY_FUNCTION)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(PARTY_FUNCTION_CODE));

  static final JsonRebasableContentCheck VALID_TD_PARTY_FUNCTION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The '%s' in '%s.%s' is valid.".formatted(PARTY_FUNCTION, DOCUMENT_PARTIES, OTHER),
          mav -> mav.submitAllMatching(S_S_x_S.formatted(DOCUMENT_PARTIES, OTHER, PARTY_FUNCTION)),
          JsonAttribute.matchedMustBeOneOf(Set.of("SCO", "DDR", "DDS", "COW", "COX")));

  static final JsonRebasableContentCheck VALID_PARTY_FUNCTION_HBL =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The '%s' in '%s.%s' of '%s' is valid."
              .formatted(PARTY_FUNCTION, DOCUMENT_PARTIES, OTHER, HOUSE_BILL_OF_LADINGS),
          mav ->
              mav.submitAllMatching(
                  S_x_S_S_x_S.formatted(
                      HOUSE_BILL_OF_LADINGS, DOCUMENT_PARTIES, OTHER, PARTY_FUNCTION)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(PARTY_FUNCTION_CODE_HBL));

  static final JsonRebasableContentCheck VALID_FEEDBACKS_SEVERITY =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate that '%s.*.%s' is valid.".formatted(FEEDBACKS, SEVERITY),
          mav -> mav.submitAllMatching(S_x_S.formatted(FEEDBACKS, SEVERITY)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(FEEDBACKS_SEVERITY));

  static final JsonRebasableContentCheck VALID_FEEDBACKS_CODE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate that '%s.*.%s' is valid.".formatted(FEEDBACKS, CODE),
          mav -> mav.submitAllMatching(S_x_S.formatted(FEEDBACKS, CODE)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(FEEDBACKS_CODE));

  private static JsonRebasableContentCheck describedTdCheck(
      String description, JsonRebasableContentCheck validation) {
    return JsonAttribute.customValidator(
        tdDisplayDescription(description), (JsonContentMatchedValidation) validation::validate);
  }

  private static JsonRebasableContentCheck describedTdCheck(
      String description, JsonContentMatchedValidation validation) {
    return JsonAttribute.customValidator(tdDisplayDescription(description), validation);
  }

  private static JsonRebasableContentCheck describedTdCheck(
      String description, JsonRebasableContentCheck... validations) {
    return JsonAttribute.customValidator(
        tdDisplayDescription(description),
        (body, contextPath) -> {
          Set<ConformanceCheckResult> results =
              Arrays.stream(validations)
                  .map(validation -> validation.validate(body, contextPath))
                  .collect(Collectors.toSet());
          Set<ConformanceCheckResult> relevantResults =
              results.stream()
                  .filter(ConformanceCheckResult::isRelevant)
                  .collect(Collectors.toSet());
          return ConformanceCheckResult.from(
              relevantResults.isEmpty() ? results : relevantResults);
        });
  }

  private static String tdDisplayDescription(String workbookDescription) {
    return workbookDescription.replace('`', '\'');
  }

  private static final Set<String> TRANSPORT_DOCUMENT_STATUSES =
      Set.of(
          "DRAFT",
          "APPROVED",
          "ISSUED",
          "PENDING_SURRENDER_FOR_AMENDMENT",
          "SURRENDERED_FOR_AMENDMENT",
          "PENDING_SURRENDER_FOR_DELIVERY",
          "SURRENDERED_FOR_DELIVERY",
          "VOIDED");

  private static final Set<String> TRANSPORT_DOCUMENT_NOTIFICATION_STATUSES =
      Set.of(
          "DRAFT",
          "APPROVED",
          "ISSUED",
          "PENDING_SURRENDER_FOR_AMENDMENT",
          "SURRENDER_FOR_AMENDMENT",
          "VOID",
          "PENDING_SURRENDER_FOR_DELIVERY",
          "SURRENDER_FOR_DELIVERY");

  private static final Set<String> AMENDED_TRANSPORT_DOCUMENT_STATUSES =
      Set.of(
          "AMENDMENT_RECEIVED",
          "AMENDMENT_CONFIRMED",
          "AMENDMENT_CANCELLED",
          "AMENDMENT_DECLINED");

  private static final String SEGREGATION_GROUPS_DESCRIPTION =
      "If present, `segregationGroups[]` must be an integer from 1 through 18.";
  private static final String AMENDMENT_SEGREGATION_GROUPS_DESCRIPTION =
      "If present, `segregationGroups[]` must be an integer from 1 through 18.";

  enum TdPayloadContext {
    STANDARD,
    TRANSPORT_DOCUMENT_NOTIFICATION,
    AMENDED_TRANSPORT_DOCUMENT
  }

  private static final JsonRebasableContentCheck TD_CONSIGNEE_AND_ENDORSEE_CONDITIONS =
      JsonAttribute.customValidator(
          "If isToOrder=false, consignee must be present and endorsee must be absent. If endorsee is present, isToOrder must be true.",
          (body, contextPath) -> {
            JsonNode documentParties = body.path(DOCUMENT_PARTIES);
            boolean isToOrder = body.path(IS_TO_ORDER).asBoolean(false);
            var issues = new LinkedHashSet<String>();
            if (!isToOrder && !documentParties.has(CONSIGNEE)) {
              issues.add(
                  "'%s.%s' must be present when '%s' is false at '%s'."
                      .formatted(DOCUMENT_PARTIES, CONSIGNEE, IS_TO_ORDER, contextPath));
            }
            if (!isToOrder && documentParties.has(ENDORSEE)) {
              issues.add(
                  "'%s.%s' must be absent when '%s' is false at '%s'."
                      .formatted(DOCUMENT_PARTIES, ENDORSEE, IS_TO_ORDER, contextPath));
            }
            return ConformanceCheckResult.simple(issues);
          });

  private static final JsonRebasableContentCheck TD_CONSIGNEE_AND_ENDORSEE_MUTUALLY_EXCLUSIVE =
      describedTdCheck(
          "Consignee and endorsee must never both be present (mutually exclusive).",
          JsonAttribute.atMostOneOf(
              JsonPointer.compile(SS.formatted(DOCUMENT_PARTIES, CONSIGNEE)),
              JsonPointer.compile(SS.formatted(DOCUMENT_PARTIES, ENDORSEE))));

  private static final JsonRebasableContentCheck VALID_TD_TYPE_OF_PERSON =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "For every document party where `typeOfPerson` is present, it must equal `NATURAL_PERSON`, `LEGAL_PERSON`, or `ASSOCIATION_OF_PERSONS`.",
          mav -> {
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, SHIPPER, TYPE_OF_PERSON));
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, CONSIGNEE, TYPE_OF_PERSON));
            mav.submitAllMatching(
                S_S_x_S_S.formatted(DOCUMENT_PARTIES, OTHER, PARTY, TYPE_OF_PERSON));
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.TYPE_OF_PERSON_SET));

  private static final JsonRebasableContentCheck VALID_TD_EBL_PLATFORMS =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "For every document party where `eblPlatform` is present, the value must equal `WAVE`, `CARX`, `ESSD`, `IDT`, `BOLE`, `EDOX`, `IQAX`, `SECR`, `TRGO`, `ETEU`, `TRAC`, `BRIT`, `COVA`, `ETIT`, `KTNE`, `CRED`, `BLOC`, `DOCU`, `AEOT`, or `SGTD`.",
          mav -> {
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, SHIPPER, EBL_PLATFORM));
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, CONSIGNEE, EBL_PLATFORM));
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, ENDORSEE, EBL_PLATFORM));
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.EBL_PLATFORMS_DATASET));

  private static final JsonRebasableContentCheck VALID_NATIONAL_COMMODITY_CODE_TYPES =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "For every `NationalCommodityCode` object, `type` must equal `NCM`, `HTS`, `SCHEDULE_B`, `TARIC`, `CN`, or `CUS`.",
          mav -> {
            mav.submitAllMatching(
                S_x_S_x_S.formatted(CONSIGNMENT_ITEMS, NATIONAL_COMMODITY_CODES, TYPE));
            mav.submitAllMatching(
                S_x_S_x_S_x_S.formatted(
                    CONSIGNMENT_ITEMS, CARGO_ITEMS, NATIONAL_COMMODITY_CODES, TYPE));
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(NATIONAL_COMMODITY_CODES_SET));

  private static final JsonRebasableContentCheck VALID_EXTENDED_NATIONAL_COMMODITY_CODE_TYPES =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "For every `ExtendedNationalCommodityCode` object, `type` must equal `NCM`, `HTS`, `SCHEDULE_B`, `TARIC`, `CN`, or `CUS`.",
          mav -> {
            mav.submitAllMatching(
                S_x_S_x_S.formatted(
                    CONSIGNMENT_ITEMS, EXTENDED_NATIONAL_COMMODITY_CODES, TYPE));
            mav.submitAllMatching(
                S_x_S_x_S_x_S.formatted(
                    CONSIGNMENT_ITEMS, CARGO_ITEMS, EXTENDED_NATIONAL_COMMODITY_CODES, TYPE));
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(NATIONAL_COMMODITY_CODES_SET));

  public static final List<JsonContentCheck> STATIC_SI_CHECKS =
      Arrays.asList(
          JsonAttribute.mustBeDatasetKeywordIfPresent(
              SI_REQUEST_SEND_TO_PLATFORM, EblDatasets.EBL_PLATFORMS_DATASET),
          SEND_TO_PLATFORM_CONDITIONAL_CHECK,
          ENS_MANIFEST_TYPE_REQUIRES_HBL_ISSUED,
          HBL_NOTIFY_PARTY_REQUIRED_IF_TO_ORDER,
          NUMBER_OF_PACKAGES_CONDITIONAL_CHECK,
          IDENTIFICATION_NUMBER_REQUIRED_IF_ENS_AND_SELF,
          SELF_FILER_CODE_REQUIRED_IF_ACE_ACI_AND_SELF,
          LOCATION_NAME_CONDITIONAL_VALIDATION_POA,
          LOCATION_NAME_CONDITIONAL_VALIDATION_POFD,
          COUNTRY_CODE_CONDITIONAL_VALIDATION_POA,
          COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD,
          ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK,
          VALID_REQUESTED_CARRIER_CLAUSES,
          BUYER_AND_SELLER_CONDITIONAL_CHECK,
          VALID_PARTY_FUNCTION,
          VALID_PARTY_FUNCTION_HBL,
          ONLY_EBLS_CAN_BE_NEGOTIABLE,
          SWBS_CANNOT_BE_NEGOTIABLE,
          EBL_AT_MOST_ONE_ORIGINAL_TOTAL,
          EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES,
          EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES,
          SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES,
          SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES,
          VALIDATE_DOCUMENT_PARTY,
          DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS,
          VALID_WOOD_DECLARATIONS,
          NATIONAL_COMMODITY_CODE_IS_VALID,
          VALID_REFERENCE_TYPES,
          VALID_CONSIGNMENT_ITEMS_REFERENCE_TYPES,
          ISO_EQUIPMENT_CODE_IMPLIES_REEFER,
          UTE_EQUIPMENT_REFERENCE_UNIQUE,
          EBL_DISPLAYED_ADDRESS_LIMIT,
          CARGO_ITEM_REFERENCES_KNOWN_EQUIPMENT,
          ADVANCED_MANIFEST_FILING_CODES_UNIQUE,
          CR_CC_T_CODES_UNIQUE,
          NOTIFY_PARTIES_REQUIRED_IN_NEGOTIABLE_BLS,
          TLR_CC_T_COMBINATION_UNIQUE,
          VALID_FEEDBACKS_SEVERITY,
          VALID_FEEDBACKS_CODE,
          VALID_HBL_METHOD_OF_PAYMENT,
          VALIDATE_CARRIER_CODE_AND_LIST_PROVIDER,
          VALID_TYPE_OF_PERSON);

  private static final List<JsonRebasableContentCheck> STATIC_TD_CHECKS =
      Arrays.asList(
          describedTdCheck(
              "When `isElectronic` is `true`, no more than one original may be requested. Therefore, the sum of `numberOfOriginalsWithoutCharges` and `numberOfOriginalsWithCharges` cannot be greater than `1`.",
              EBL_AT_MOST_ONE_ORIGINAL_TOTAL),
          describedTdCheck(
              "When isElectronic is true and transportDocumentTypeCode is BOL, neither copies with charges nor copies without charges are allowed. Therefore, both numberOfCopiesWithCharges and numberOfCopiesWithoutCharges must be 0 or absent.",
              EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES,
              EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES),
          describedTdCheck(
              "If transportDocumentTypeCode='SWB', then numberOfOriginalsWithCharges and numberOfOriginalsWithoutCharges must be absent",
              SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES,
              SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES),
          describedTdCheck(
              "`transportDocumentStatus` must equal `DRAFT`, `APPROVED`, `ISSUED`, `PENDING_SURRENDER_FOR_AMENDMENT`, `SURRENDERED_FOR_AMENDMENT`, `PENDING_SURRENDER_FOR_DELIVERY`, `SURRENDERED_FOR_DELIVERY`, or `VOIDED`.",
              JsonAttribute.mustBeOneOf(
                  TD_TRANSPORT_DOCUMENT_STATUS, TRANSPORT_DOCUMENT_STATUSES)),
          describedTdCheck(
              "If `transportDocumentStatus='ISSUED'`, then `issueDate` must be present",
              ISSUE_DATE_REQUIRED_WHEN_ISSUED),
          describedTdCheck(
              "Exactly one of `shippedOnBoardDate` and `receivedForShipmentDate` must be present.",
              JsonAttribute.atLeastOneOf(
                  JsonPointer.compile(S.formatted(SHIPPED_ON_BOARD_DATE)),
                  JsonPointer.compile(S.formatted(RECEIVED_FOR_SHIPMENT_DATE))),
              JsonAttribute.atMostOneOf(
                  JsonPointer.compile(S.formatted(SHIPPED_ON_BOARD_DATE)),
                  JsonPointer.compile(S.formatted(RECEIVED_FOR_SHIPMENT_DATE)))),
          describedTdCheck(
              "`declaredValue` and `declaredValueCurrency` must either both be present or both be absent.",
              JsonAttribute.allOrNoneArePresent(
                  JsonPointer.compile(S.formatted(DECLARED_VALUE)),
                  JsonPointer.compile(S.formatted(DECLARED_VALUE_CURRENCY)))),
          describedTdCheck(
              "If `isElectronic=true`, then `numberOfRiderPages` must not be present.",
              RIDER_PAGES_NOT_ALLOWED_FOR_ELECTRONIC_TD),
          describedTdCheck(
              "`cargoMovementTypeAtOrigin` must equal `FCL` or `LCL`.",
              JsonAttribute.mustBeDatasetKeywordIfPresent(
                  JsonPointer.compile(S.formatted(CARGO_MOVEMENT_TYPE_AT_ORIGIN)),
                  EblDatasets.CARGO_MOVEMENT_TYPE)),
          describedTdCheck(
              "`cargoMovementTypeAtDestination` must equal `FCL` or `LCL`.",
              JsonAttribute.mustBeDatasetKeywordIfPresent(
                  JsonPointer.compile(S.formatted(CARGO_MOVEMENT_TYPE_AT_DESTINATION)),
                  EblDatasets.CARGO_MOVEMENT_TYPE)),
          describedTdCheck(
              "If `preCarriageBy` is present, then `placeOfReceipt` must be present.",
              JsonAttribute.ifThen(
                  "'%s' implies '%s'.".formatted(PRE_CARRIAGE_BY, PLACE_OF_RECEIPT),
                  JsonAttribute.isNotNull(
                      JsonPointer.compile(SS.formatted(TRANSPORTS, PRE_CARRIAGE_BY))),
                  JsonAttribute.mustBeNotNull(
                      JsonPointer.compile(SS.formatted(TRANSPORTS, PLACE_OF_RECEIPT)),
                      "'%s' is present.".formatted(PRE_CARRIAGE_BY)))),
          describedTdCheck(
              "If `preCarriageBy` is present, then it must equal `VESSEL`, `RAIL`, `TRUCK`, `BARGE`, or `MULTIMODAL`.",
              JsonAttribute.mustBeDatasetKeywordIfPresent(
                  JsonPointer.compile(SS.formatted(TRANSPORTS, PRE_CARRIAGE_BY)),
                  MODE_OF_TRANSPORT)),
          describedTdCheck(
              "If `onCarriageBy` is present, then it must equal `VESSEL`, `RAIL`, `TRUCK`, `BARGE`, or `MULTIMODAL`.",
              JsonAttribute.mustBeDatasetKeywordIfPresent(
                  JsonPointer.compile(SS.formatted(TRANSPORTS, ON_CARRIAGE_BY)),
                  MODE_OF_TRANSPORT)),
          describedTdCheck(
              "For every item in `transports.vesselVoyages[]`, `role` must equal `FIRST_SEA_GOING` or `MOTHER`.",
              VALID_VESSEL_VOYAGE_ROLES),
          describedTdCheck(
              "If `onCarriageBy` is present, then `placeOfDelivery` must be present.",
              JsonAttribute.ifThen(
                  "'%s' implies '%s'.".formatted(ON_CARRIAGE_BY, PLACE_OF_DELIVERY),
                  JsonAttribute.isNotNull(
                      JsonPointer.compile(SS.formatted(TRANSPORTS, ON_CARRIAGE_BY))),
                  JsonAttribute.mustBeNotNull(
                      JsonPointer.compile(SS.formatted(TRANSPORTS, PLACE_OF_DELIVERY)),
                      "'%s' is present.".formatted(ON_CARRIAGE_BY)))),
          describedTdCheck(
              "For each `documentParty`—`shipper`, `consignee`, `endorsee`, `notifyParties`, `other`, `onBehalfOfShipper`—at least one of `address`, `addressLines`, or `identifyingCodes` must be provided.",
              VALIDATE_DOCUMENT_PARTY),
          TD_CONSIGNEE_AND_ENDORSEE_CONDITIONS,
          TD_CONSIGNEE_AND_ENDORSEE_MUTUALLY_EXCLUSIVE,
          describedTdCheck(
              "If `isToOrder=true`, then at least one notify party must exist: `documentParties.notifyParties` must be present.",
              NOTIFY_PARTIES_REQUIRED_IN_NEGOTIABLE_BLS),
          describedTdCheck(
              "For every item in `documentParties.other[]`, `partyFunction` must equal `SCO`, `DDR`, `DDS`, `COW`, or `COX`.",
              VALID_TD_PARTY_FUNCTION),
          describedTdCheck(VALID_TD_TYPE_OF_PERSON.description(), VALID_TD_TYPE_OF_PERSON),
          describedTdCheck(VALID_TD_EBL_PLATFORMS.description(), VALID_TD_EBL_PLATFORMS),
          describedTdCheck(
              "If present, `codeListProvider` must equal  `WAVE`, `CARX`, `ESSD`, `IDT`, `BOLE`, `EDOX`, `IQAX`, `SECR`, `TRGO`, `ETEU`, `TRAC`, `BRIT`, `COVA`, `ETIT`, `KTNE`, `CRED`, `BLOC`, `DOCU`, `AEOT`, `SGTD`, `GSBN`, `WISE`, `GLEIF`, `W3C`, `DNB`, `FMC`, `DCSA`, or `ZZZ`.",
              TD_DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS),
          describedTdCheck(
              "For every item in a general `references[]` collection, `type` must equal `CR` or `AKG`.",
              VALID_TD_REFERENCE_TYPES),
          describedTdCheck(
              "For every item in a consignment-item `references[]` collection, `type` must equal `CR`, `AKG`, `SPO`, or `CPO`.",
              VALID_CONSIGNMENT_ITEMS_REFERENCE_TYPES),
          describedTdCheck(
              VALID_NATIONAL_COMMODITY_CODE_TYPES.description(),
              VALID_NATIONAL_COMMODITY_CODE_TYPES),
          describedTdCheck(
              VALID_EXTENDED_NATIONAL_COMMODITY_CODE_TYPES.description(),
              VALID_EXTENDED_NATIONAL_COMMODITY_CODE_TYPES),
          describedTdCheck(
              "For every cargo item containing `dangerousGoods`, `imoPackagingCode` or `packageCode` must be present.",
              JsonAttribute.allIndividualMatchesMustBeValid(
                  "Dangerous-goods packaging code is present.",
                  mav ->
                      mav.submitAllMatching(
                          S_x_S_x_S.formatted(CONSIGNMENT_ITEMS, CARGO_ITEMS, OUTER_PACKAGING)),
                  (nodeToValidate, contextPath) -> {
                    var dg = nodeToValidate.path(DANGEROUS_GOODS);
                    if (!dg.isArray() || dg.isEmpty()) {
                      return ConformanceCheckResult.withRelevance(
                          Set.of(ConformanceError.irrelevant()));
                    }
                    if (nodeToValidate.path(PACKAGE_CODE).isMissingNode()
                        && nodeToValidate.path(IMO_PACKAGING_CODE).isMissingNode()) {
                      return ConformanceCheckResult.simple(
                          Set.of(
                              "The '%s' object did not have a '%s' nor an '%s', which is required due to '%s'."
                                  .formatted(
                                      contextPath,
                                      PACKAGE_CODE,
                                      IMO_PACKAGING_CODE,
                                      DANGEROUS_GOODS)));
                    }
                    return ConformanceCheckResult.simple(Set.of());
                  })),
          describedTdCheck(
              "If `woodDeclaration` is present, then it must equal `NOT_APPLICABLE`, `NOT_TREATED_AND_NOT_CERTIFIED`, `PROCESSED`, or `TREATED_AND_CERTIFIED`.",
              VALID_WOOD_DECLARATIONS),
          describedTdCheck(
              SEGREGATION_GROUPS_DESCRIPTION,
              JsonAttribute.allIndividualMatchesMustBeValid(
                  "Segregation groups are valid.",
                  allDg(dg -> dg.path(SEGREGATION_GROUPS).all().submitPath()),
                  JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                      EblDatasets.DG_SEGREGATION_GROUPS))),
          describedTdCheck(
              "If `inhalationZone` is present, then it must equal `A`, `B`, `C`, or `D`",
              JsonAttribute.allIndividualMatchesMustBeValid(
                  "Inhalation zones are valid.",
                  allDg(dg -> dg.path(INHALATION_ZONE).submitPath()),
                  JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                      EblDatasets.DG_INHALATION_ZONES))),
          describedTdCheck(
              "For every `innerPackaging` object, `quantity` must be a positive integer greater than 0.",
              VALID_INNER_PACKAGING_QUANTITIES),
          describedTdCheck(
              "For every item in `utilizedTransportEquipments[]`, if `isNonOperatingReefer=true`, then `activeReeferSettings` must not be present.",
              NOR_IS_TRUE_IMPLIES_NO_ACTIVE_REEFER),
          describedTdCheck(
              "`temperatureSetpoint` and `temperatureUnit` must either both be present or both be absent.",
              JsonAttribute.allIndividualMatchesMustBeValid(
                  "Temperature setpoint and unit have matching presence.",
                  mav ->
                      mav.submitAllMatching(
                          S_x_S.formatted(
                              UTILIZED_TRANSPORT_EQUIPMENTS, ACTIVE_REEFER_SETTINGS)),
                  JsonAttribute.combine(
                      JsonAttribute.presenceImpliesOtherField(
                          TEMPERATURE_SETPOINT, TEMPERATURE_UNIT),
                      JsonAttribute.presenceImpliesOtherField(
                          TEMPERATURE_UNIT, TEMPERATURE_SETPOINT)))),
          describedTdCheck(
              "`airExchangeSetpoint` and `airExchangeUnit` must either both be present or both be absent.",
              JsonAttribute.allIndividualMatchesMustBeValid(
                  "Air-exchange setpoint and unit have matching presence.",
                  mav ->
                      mav.submitAllMatching(
                          S_x_S.formatted(
                              UTILIZED_TRANSPORT_EQUIPMENTS, ACTIVE_REEFER_SETTINGS)),
                  JsonAttribute.combine(
                      JsonAttribute.presenceImpliesOtherField(
                          AIR_EXCHANGE_SETPOINT, AIR_EXCHANGE_UNIT),
                      JsonAttribute.presenceImpliesOtherField(
                          AIR_EXCHANGE_UNIT, AIR_EXCHANGE_SETPOINT)))),
          describedTdCheck(
              "Every cargo gross-weight and cargo net-weight value must contain no more than 3 decimal places, and every cargo gross-volume value must contain no more than 4 decimal places.",
              VALID_CARGO_MEASUREMENT_PRECISION,
              VALID_CARGO_VOLUME_PRECISION),
          describedTdCheck(
              "For every item in `feedbacks[]`, `severity` must equal `INFO`, `WARN`, or `ERROR`.",
              VALID_FEEDBACKS_SEVERITY),
          describedTdCheck(
              "For every item in `feedbacks[]`, `code` must equal `INFORMATIONAL_MESSAGE`, `PROPERTY_WILL_BE_IGNORED`, `PROPERTY_VALUE_MUST_CHANGE`, `PROPERTY_VALUE_HAS_BEEN_CHANGED`, `PROPERTY_VALUE_MAY_CHANGE`, or `PROPERTY_HAS_BEEN_DELETED`.",
              VALID_FEEDBACKS_CODE));

  public static final JsonContentCheck SIR_OR_TDR_REQUIRED_IN_NOTIFICATION =
      JsonAttribute.atLeastOneOf(SI_REF_SIR_PTR, TD_TDR);

  public static JsonContentCheck sirInNotificationMustMatchDSP(
      Supplier<EblDynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.mustEqual(
        SI_REF_SIR_PTR, () -> dspSupplier.get().shippingInstructionsReference());
  }

  public static JsonContentCheck tdrInNotificationMustMatchDSP(
      Supplier<EblDynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.mustEqual(
            TD_TDR, () -> dspSupplier.get().transportDocumentReference());
  }

  public static List<JsonContentCheck> generateScenarioRelatedChecks(
      ScenarioType scenarioType, boolean isTD, boolean isCladInSI) {
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(
        JsonAttribute.mustEqual(
            "[%s] Verify that the correct '%s' is used"
                .formatted(SCENARIO, TRANSPORT_DOCUMENT_TYPE_CODE),
            TRANSPORT_DOCUMENT_TYPE_CODE,
            scenarioType::transportDocumentTypeCode));

    checks.add(
        JsonAttribute.ifThen(
            "[%s] Verify that the '%s' contains '%s'."
                .formatted(SCENARIO, TRANSPORT_DOCUMENT, CARRIERS_AGENT_AT_DESTINATION),
            ignored -> isTD && (isCladInSI || scenarioType.isCarriersAgentAtDestinationRequired()),
            JsonAttribute.path(
                DOCUMENT_PARTIES,
                JsonAttribute.path(
                    CARRIERS_AGENT_AT_DESTINATION, JsonAttribute.matchedMustBePresent()))));

    checks.add(
        JsonAttribute.ifThen(
            "[%s] Verify that the '%s' had '%s' as true if scenario requires it."
                .formatted(
                    SCENARIO, SHIPPING_INSTRUCTIONS, IS_CARRIERS_AGENT_AT_DESTINATION_REQUIRED),
            ignored -> !isTD && scenarioType.isCarriersAgentAtDestinationRequired(),
            JsonAttribute.path(
                IS_CARRIERS_AGENT_AT_DESTINATION_REQUIRED, JsonAttribute.matchedMustBeTrue())));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "[%s] Non-DG: '%s' must be present in the SI.".formatted(SCENARIO, OUTER_PACKAGING),
            mav -> mav.submitAllMatching("%s.*.%s.*".formatted(CONSIGNMENT_ITEMS, CARGO_ITEMS)),
            JsonAttribute.ifMatchedThen(
                ignored -> !isTD && !scenarioType.hasDG(),
                JsonAttribute.path(OUTER_PACKAGING, JsonAttribute.matchedMustBePresent()))));

    checks.add(
        JsonAttribute.customValidator(
            "[%s] Verify that the scenario contained references when the scenario requires it."
                .formatted(SCENARIO),
            scenarioReferencesCheck(scenarioType)));

    checks.add(
        JsonAttribute.customValidator(
            "[%s] Verify that '%s' is used when the scenario requires it."
                .formatted(SCENARIO, CUSTOMS_REFERENCES),
            scenarioCustomsReferencesCheck(scenarioType)));

    checks.add(
        JsonAttribute.customValidator(
            "[%s] Verify that the scenario contains the required amount of '%s'."
                .formatted(SCENARIO, UTILIZED_TRANSPORT_EQUIPMENTS),
            utilizedTransportEquipmentsScenarioSizeCheck(scenarioType)));

    checks.add(
        JsonAttribute.customValidator(
            "[%s] Verify that the scenario contains the required amount of '%s'."
                .formatted(SCENARIO, CONSIGNMENT_ITEMS),
            consignmentItemsScenarioSizeCheck(scenarioType)));

    return checks;
  }

  private static JsonContentMatchedValidation scenarioCustomsReferencesCheck(
      ScenarioType scenarioType) {
    return (nodeToValidate, contextPath) -> {
      if (!scenarioType.isCustomsReferencesRequired()) {
        return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
      }
      var allReferencesParents = nodeToValidate.findParents(CUSTOMS_REFERENCES);
      for (var referencesParent : allReferencesParents) {
        if (isNonEmptyNode(referencesParent.path(CUSTOMS_REFERENCES))) {
          return ConformanceCheckResult.simple(Set.of());
        }
      }
      return ConformanceCheckResult.simple(
          Set.of("Expected '%s' to be used somewhere.".formatted(CUSTOMS_REFERENCES)));
    };
  }

  private static final JsonPointer[] REFERENCE_PATHS = {
    JsonPointer.compile(S.formatted(REFERENCES)),
    JsonPointer.compile(SSS.formatted(DOCUMENT_PARTIES, SHIPPER, REFERENCES)),
    JsonPointer.compile(SSS.formatted(DOCUMENT_PARTIES, SHIPPER, REFERENCE)),
    JsonPointer.compile(SSS.formatted(DOCUMENT_PARTIES, CONSIGNEE, PURCHASE_ORDER_REFERENCE)),
    JsonPointer.compile(SSS.formatted(DOCUMENT_PARTIES, CONSIGNEE, REFERENCE))
  };

  private static JsonContentMatchedValidation scenarioReferencesCheck(ScenarioType scenarioType) {
    return JsonAttribute.ifMatchedThen(
        ignored -> scenarioType.isReferencesRequired(),
        JsonAttribute.atLeastOneOfMatched(
            (body, ptrs) -> {
              ptrs.addAll(Arrays.asList(REFERENCE_PATHS));
              var uteCount = body.path(UTILIZED_TRANSPORT_EQUIPMENTS).size();
              for (int i = 0; i < uteCount; i++) {
                ptrs.add(
                    JsonPointer.compile(
                        SDS.formatted(UTILIZED_TRANSPORT_EQUIPMENTS, i, REFERENCES)));
              }
              var ciCount = body.path(CONSIGNMENT_ITEMS).size();
              for (int i = 0; i < ciCount; i++) {
                ptrs.add(JsonPointer.compile(SDS.formatted(CONSIGNMENT_ITEMS, i, REFERENCES)));
              }
              var notifyPartyCount = body.path(DOCUMENT_PARTIES).path(NOTIFY_PARTIES).size();
              for (int i = 0; i < notifyPartyCount; i++) {
                ptrs.add(
                    JsonPointer.compile(
                        SSDSS.formatted(DOCUMENT_PARTIES, NOTIFY_PARTIES, i, PARTY, REFERENCE)));
              }
              var otherPartyCount = body.path(DOCUMENT_PARTIES).path(OTHER).size();
              for (int i = 0; i < otherPartyCount; i++) {
                ptrs.add(
                    JsonPointer.compile(
                        SSDSS.formatted(DOCUMENT_PARTIES, OTHER, i, PARTY, REFERENCE)));
              }
            }));
  }

  private static boolean isNonEmptyNode(JsonNode field) {
    if (field == null || field.isMissingNode()) {
      return false;
    }
    if (field.isTextual()) {
      return !field.asText().isBlank();
    }
    return !field.isEmpty() || field.isValueNode();
  }

  public static ActionCheck siRequestContentChecks(
      UUID matched, String standardVersion, ScenarioType scenarioType) {
    var checks = new ArrayList<>(STATIC_SI_CHECKS);
    checks.add(DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE);
    checks.add(VALIDATE_DOCUMENT_PARTIES_MATCH_EBL);
    checks.addAll(generateScenarioRelatedChecks(scenarioType, false, false));
    return JsonAttribute.contentChecks(
        EblRole::isShipper, matched, HttpMessageType.REQUEST, standardVersion, checks);
  }

  public static ActionCheck siResponseContentChecks(
      UUID matched,
      String standardVersion,
      ShippingInstructionsStatus shippingInstructionsStatus,
      ShippingInstructionsStatus updatedShippingInstructionsStatus,
      Supplier<EblDynamicScenarioParameters> dspSupplier) {
    var checks =
        getSiPayloadChecks(
            shippingInstructionsStatus, updatedShippingInstructionsStatus, dspSupplier);
    return JsonAttribute.contentChecks(
        EblRole::isCarrier, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  public static List<JsonContentCheck> getSiPayloadChecks(
      ShippingInstructionsStatus shippingInstructionsStatus,
      ShippingInstructionsStatus updatedShippingInstructionsStatus,
      Supplier<EblDynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<JsonContentCheck>();

    checks.add(
        JsonAttribute.mustEqual(
            SI_REF_SIR_PTR, () -> dspSupplier.get().shippingInstructionsReference()));

    checks.add(
        JsonAttribute.mustEqual(SI_REF_SI_STATUS_PTR, shippingInstructionsStatus.wireName()));

    if (updatedShippingInstructionsStatus != ShippingInstructionsStatus.SI_ANY) {
      var updatedStatusCheck =
          getUpdatedShippingInstructionsStatusCheck(updatedShippingInstructionsStatus);
      checks.add(updatedStatusCheck);
    }

    checks.addAll(STATIC_SI_CHECKS);

    checks.add(FEEDBACKS_PRESENCE);

    checks.addAll(
        generateScenarioRelatedChecks(
            ScenarioType.valueOf(dspSupplier.get().scenarioType()), false, false));
    return checks;
  }

  private static JsonRebasableContentCheck getUpdatedShippingInstructionsStatusCheck(
      ShippingInstructionsStatus updatedShippingInstructionsStatus) {
    return updatedShippingInstructionsStatus != null
        ? JsonAttribute.mustEqual(
            SI_REF_UPDATED_SI_STATUS_PTR, updatedShippingInstructionsStatus.wireName())
        : JsonAttribute.mustBeAbsent(SI_REF_UPDATED_SI_STATUS_PTR);
  }

  static final JsonContentCheck FEEDBACKS_PRESENCE =
      JsonAttribute.customValidator(
          "'%s' must be present for the selected shipping instructions status."
              .formatted(FEEDBACKS),
          body -> {
            var siStatus = body.path(SHIPPING_INSTRUCTIONS_STATUS).asText("");
            var updatedSiStatus = body.path(UPDATED_SHIPPING_INSTRUCTIONS_STATUS).asText("");
            var issues = new LinkedHashSet<String>();
            if (!Objects.equals(SI_PENDING_UPDATE.wireName(), siStatus)
                || !updatedSiStatus.isEmpty()) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            var feedbacks = body.get(FEEDBACKS);
            if (feedbacks == null || feedbacks.isEmpty()) {
              issues.add(
                  "'%s' is missing for the si in status %s."
                      .formatted(FEEDBACKS, SI_PENDING_UPDATE.wireName()));
            }
            return ConformanceCheckResult.simple(issues);
          });

  public static ActionCheck tdRefStatusChecks(
      UUID matched,
      String standardVersion,
      Supplier<EblDynamicScenarioParameters> dspSupplier,
      TransportDocumentStatusScenario statusScenario) {
    List<JsonContentCheck> checks = new ArrayList<>();
    checks.add(
        JsonAttribute.mustEqual(
            TD_TDR, () -> dspSupplier.get().transportDocumentReference()));
    checks.addAll(statusScenario.checks(false));
    return JsonAttribute.contentChecks(
        EblRole::isCarrier,
        matched,
        HttpMessageType.RESPONSE,
        standardVersion,
        checks);
  }

  public static List<JsonContentCheck> getSiNotificationChecks(
      ShippingInstructionsStatus shippingInstructionsStatus,
      ShippingInstructionsStatus updatedShippingInstructionsStatus,
      List<JsonContentCheck> extraChecks) {
    List<JsonContentCheck> jsonContentChecks = new ArrayList<>(extraChecks);

    jsonContentChecks.add(
        JsonAttribute.mustEqual(SI_REF_SI_STATUS_PTR, shippingInstructionsStatus.wireName()));

    jsonContentChecks.add(
        getUpdatedShippingInstructionsStatusCheck(updatedShippingInstructionsStatus));

    jsonContentChecks.add(FEEDBACKS_PRESENCE);
    jsonContentChecks.add(VALID_FEEDBACKS_SEVERITY);
    jsonContentChecks.add(VALID_FEEDBACKS_CODE);
    return jsonContentChecks;
  }

  public static List<JsonContentCheck> getTdNotificationChecks(
      List<TransportDocumentStatus> transportDocumentStatus, JsonContentCheck... extraChecks) {
    return getTdNotificationChecks(
        TransportDocumentStatusScenario.primaryStatusesOnly(
            new LinkedHashSet<>(transportDocumentStatus)),
        extraChecks);
  }

  public static List<JsonContentCheck> getTdNotificationChecks(
      TransportDocumentStatusScenario statusScenario, JsonContentCheck... extraChecks) {
    return getTdNotificationChecks(statusScenario, null, extraChecks);
  }

  public static List<JsonContentCheck> getTdNotificationChecks(
      TransportDocumentStatusScenario statusScenario,
      Supplier<EblDynamicScenarioParameters> dspSupplier,
      JsonContentCheck... extraChecks) {
    List<JsonContentCheck> jsonContentChecks = new ArrayList<>(Arrays.asList(extraChecks));

    jsonContentChecks.add(
        describedTdCheck(
            "`data.transportDocumentStatus` must equal `DRAFT`, `APPROVED`, `ISSUED`, `PENDING_SURRENDER_FOR_AMENDMENT`, `SURRENDER_FOR_AMENDMENT`, `VOID`, `PENDING_SURRENDER_FOR_DELIVERY`, or `SURRENDER_FOR_DELIVERY`.",
            JsonAttribute.mustBeOneOf(
                TD_TRANSPORT_DOCUMENT_STATUS, TRANSPORT_DOCUMENT_NOTIFICATION_STATUSES)));
    jsonContentChecks.add(
        describedTdCheck(
            "If `amendedTransportDocumentStatus` is present, then it must equal `AMENDMENT_RECEIVED`, `AMENDMENT_CONFIRMED`, `AMENDMENT_CANCELLED`, or `AMENDMENT_DECLINED`.",
            JsonAttribute.path(
                AMENDED_TRANSPORT_DOCUMENT_STATUS,
                JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                    KeywordDataset.staticDataset(
                        AMENDED_TRANSPORT_DOCUMENT_STATUSES.toArray(String[]::new))))));
    jsonContentChecks.addAll(statusScenario.checks(true));
    if (statusScenario.requiresUnchangedPrimaryStatus() && dspSupplier != null) {
      jsonContentChecks.add(
          describedTdCheck(
              "After `%s`, `transportDocumentStatus` must equal its value from before the action."
                  .formatted(statusScenario.useCase()),
              JsonAttribute.mustEqual(
                  TD_TRANSPORT_DOCUMENT_STATUS,
                  () -> dspSupplier.get().transportDocumentStatus())));
    }

    jsonContentChecks.add(
        describedTdCheck(
            "For every item in `feedbacks[]`, `severity` must equal `INFO`, `WARN`, or `ERROR`.",
            VALID_FEEDBACKS_SEVERITY));
    jsonContentChecks.add(
        describedTdCheck(
            "For every item in `feedbacks[]`, `code` must equal `INFORMATIONAL_MESSAGE`, `PROPERTY_WILL_BE_IGNORED`, `PROPERTY_VALUE_MUST_CHANGE`, `PROPERTY_VALUE_HAS_BEEN_CHANGED`, `PROPERTY_VALUE_MAY_CHANGE`, or `PROPERTY_HAS_BEEN_DELETED`.",
            VALID_FEEDBACKS_CODE));
    return jsonContentChecks;
  }

  private static void genericTdContentChecks(
      List<? super JsonRebasableContentCheck> jsonContentChecks,
      TransportDocumentStatus transportDocumentStatus) {
    genericTdContentChecks(jsonContentChecks, List.of(transportDocumentStatus));
  }

  private static void genericTdContentChecks(
      List<? super JsonRebasableContentCheck> jsonContentChecks,
      List<TransportDocumentStatus> transportDocumentStatus) {
    genericTdContentChecks(jsonContentChecks, transportDocumentStatus, TdPayloadContext.STANDARD);
  }

  private static void genericTdContentChecks(
      List<? super JsonRebasableContentCheck> jsonContentChecks,
      List<TransportDocumentStatus> transportDocumentStatus,
      TdPayloadContext payloadContext) {
    jsonContentChecks.add(
        JsonAttribute.mustBeOneOf(
            TD_TRANSPORT_DOCUMENT_STATUS,
            transportDocumentStatus.stream().allMatch(TransportDocumentStatus::hasWireName),
            transportDocumentStatus.stream()
                .map(TransportDocumentStatus::wireName)
                .collect(Collectors.toSet())));
    addTransportDocumentCarrierChecks(jsonContentChecks, payloadContext);
  }

  private static void addTransportDocumentCarrierChecks(
      List<? super JsonRebasableContentCheck> jsonContentChecks, TdPayloadContext payloadContext) {
    STATIC_TD_CHECKS.stream()
        .map(
            check ->
                payloadContext != TdPayloadContext.STANDARD
                        && tdDisplayDescription(SEGREGATION_GROUPS_DESCRIPTION)
                            .equals(check.description())
                    ? describedTdCheck(AMENDMENT_SEGREGATION_GROUPS_DESCRIPTION, check)
                    : check)
        .forEach(jsonContentChecks::add);
  }

  public static List<JsonRebasableContentCheck> genericTDContentChecks(
      TransportDocumentStatus transportDocumentStatus, Supplier<String> tdrReferenceSupplier) {
    List<JsonRebasableContentCheck> jsonContentChecks = new ArrayList<>();
    if (tdrReferenceSupplier != null) {
      jsonContentChecks.add(JsonAttribute.mustEqual(TD_TDR, tdrReferenceSupplier));
    }
    genericTdContentChecks(jsonContentChecks, transportDocumentStatus);
    return jsonContentChecks;
  }

  public static List<JsonRebasableContentCheck> transportDocumentCarrierContentChecks() {
    return List.copyOf(STATIC_TD_CHECKS);
  }

  public static List<JsonContentCheck> transportDocumentCarrierContentChecks(
      ScenarioType scenarioType) {
    List<JsonContentCheck> checks = new ArrayList<>(STATIC_TD_CHECKS);
    checks.removeIf(check -> !tdCheckAppliesToScenario(check, scenarioType));
    checks.addAll(tdScopeChecks(scenarioType));
    return List.copyOf(checks);
  }

  public static ActionCheck tdPlusScenarioContentChecks(
      UUID matched,
      String standardVersion,
      List<TransportDocumentStatus> transportDocumentStatus,
      Supplier<EblDynamicScenarioParameters> dspSupplier) {
      List<JsonContentCheck> jsonContentChecks = new ArrayList<>();
    if (dspSupplier.get().transportDocumentReference() != null) {
      jsonContentChecks.add(
          JsonAttribute.mustEqual(TD_TDR, dspSupplier.get().transportDocumentReference()));
    }
    jsonContentChecks.addAll(getTdPayloadChecks(transportDocumentStatus, dspSupplier));
    return JsonAttribute.contentChecks(
        EblRole::isCarrier, matched, HttpMessageType.RESPONSE, standardVersion, jsonContentChecks);
  }

  public static List<JsonContentCheck> getTdPayloadChecks(
          List<TransportDocumentStatus> transportDocumentStatus,
      Supplier<EblDynamicScenarioParameters> dspSupplier) {
    return getTdPayloadChecks(
        transportDocumentStatus, dspSupplier, TdPayloadContext.STANDARD);
  }

  static List<JsonContentCheck> getTdPayloadChecks(
      List<TransportDocumentStatus> transportDocumentStatus,
      Supplier<EblDynamicScenarioParameters> dspSupplier,
      TdPayloadContext payloadContext) {

    String scenarioTypeName = dspSupplier.get().scenarioType();
    ScenarioType scenarioType =
        scenarioTypeName == null ? null : ScenarioType.valueOf(scenarioTypeName);
    List<JsonContentCheck> jsonContentChecks = new ArrayList<>();

    if (payloadContext == TdPayloadContext.STANDARD) {
      genericTdContentChecks(jsonContentChecks, transportDocumentStatus, payloadContext);
    } else {
      addTransportDocumentCarrierChecks(jsonContentChecks, payloadContext);
    }
    if (scenarioType != null) {
      jsonContentChecks.removeIf(check -> !tdCheckAppliesToScenario(check, scenarioType));
      jsonContentChecks.addAll(tdScopeChecks(scenarioType));
    }
    return jsonContentChecks;
  }

  private static boolean tdCheckAppliesToScenario(JsonContentCheck check, ScenarioType scenarioType) {
    String description = check.description();
    if (description.startsWith("When 'isElectronic' is 'true', no more than one original")
        || description.startsWith("When isElectronic is true and transportDocumentTypeCode is BOL")) {
      return !scenarioType.isSWB();
    }
    if (description.equals("Consignee and endorsee must never both be present (mutually exclusive).")
        || description.startsWith("If 'isToOrder=true', then at least one notify party")) {
      return scenarioType.isToOrder();
    }
    return true;
  }

  private static List<JsonContentCheck> tdScopeChecks(ScenarioType scenarioType) {
    List<JsonContentCheck> checks = new ArrayList<>();
    checks.add(
        describedTdCheck(
            "[Scope] `transportDocumentTypeCode` must equal `%s`."
                .formatted(scenarioType.transportDocumentTypeCode()),
            JsonAttribute.path(
                TRANSPORT_DOCUMENT_TYPE_CODE,
                JsonAttribute.matchedMustEqual(scenarioType::transportDocumentTypeCode))));
    checks.add(
        describedTdCheck(
            "[Scope] `isToOrder` must equal `%s`.".formatted(scenarioType.isToOrder()),
            JsonAttribute.path(
                IS_TO_ORDER,
                scenarioType.isToOrder()
                    ? JsonAttribute.matchedMustBeTrue()
                    : (node, contextPath) ->
                        node.isBoolean() && !node.booleanValue()
                            ? ConformanceCheckResult.simple(Set.of())
                            : ConformanceCheckResult.simple(
                                Set.of(
                                    "The value of '%s' was '%s' instead of 'false'"
                                        .formatted(contextPath, node))))));
    return checks;
  }

  public static ActionCheck shipperApprovalContentChecks(
      UUID matched, String standardVersion) {
    return JsonAttribute.contentChecks(
        EblRole::isShipper,
        matched,
        HttpMessageType.REQUEST,
        standardVersion,
        List.of(
            describedTdCheck(
                "`transportDocumentStatus` must equal `APPROVED`.",
                JsonAttribute.mustEqual(TD_TRANSPORT_DOCUMENT_STATUS, "APPROVED"))));
  }

  public static ActionCheck shipperAmendmentContentChecks(
      UUID matched,
      String standardVersion,
      Supplier<EblDynamicScenarioParameters> dspSupplier) {
    String scenarioTypeName = dspSupplier.get().scenarioType();
    ScenarioType scenarioType =
        scenarioTypeName == null ? null : ScenarioType.valueOf(scenarioTypeName);
    List<JsonContentCheck> checks = new ArrayList<>(STATIC_TD_CHECKS);
    checks.removeIf(
        check ->
            check.description().startsWith("'transportDocumentStatus' must equal")
                || (scenarioType != null && !tdCheckAppliesToScenario(check, scenarioType)));
    checks.add(
        describedTdCheck(
            "`transportDocumentStatus` must equal `DRAFT`, `ISSUED`, or `PENDING_SURRENDER_FOR_AMENDMENT`.",
            JsonAttribute.mustBeOneOf(
                TD_TRANSPORT_DOCUMENT_STATUS,
                Set.of("DRAFT", "ISSUED", "PENDING_SURRENDER_FOR_AMENDMENT"))));
    checks.add(
        describedTdCheck(
            "`transportDocumentStatus` must equal its value from before the action.",
            JsonAttribute.mustEqual(
                TD_TRANSPORT_DOCUMENT_STATUS,
                () -> dspSupplier.get().transportDocumentStatus())));
    if (scenarioType != null) {
      checks.addAll(tdScopeChecks(scenarioType));
    }
    return JsonAttribute.contentChecks(
        EblRole::isShipper, matched, HttpMessageType.REQUEST, standardVersion, checks);
  }

  public static List<JsonRebasableContentCheck> amendedTransportDocumentCarrierContentChecks(
      Supplier<EblDynamicScenarioParameters> dspSupplier) {
    String scenarioTypeName = dspSupplier.get().scenarioType();
    ScenarioType scenarioType =
        scenarioTypeName == null ? null : ScenarioType.valueOf(scenarioTypeName);
    List<JsonRebasableContentCheck> checks = new ArrayList<>();
    addTransportDocumentCarrierChecks(checks, TdPayloadContext.AMENDED_TRANSPORT_DOCUMENT);
    if (scenarioType != null) {
      checks.removeIf(check -> !tdCheckAppliesToScenario(check, scenarioType));
      tdScopeChecks(scenarioType).stream()
          .map(JsonRebasableContentCheck.class::cast)
          .forEach(checks::add);
    }
    return checks;
  }

  public static ActionCheck amendedTransportDocumentStatusChecks(
      UUID matched,
      String standardVersion,
      AmendedTransportDocumentStatus expectedStatus) {
    List<JsonContentCheck> checks = new ArrayList<>();
    checks.add(
        describedTdCheck(
            "If `amendedTransportDocumentStatus` is present, then it must equal `AMENDMENT_RECEIVED`, `AMENDMENT_CONFIRMED`, `AMENDMENT_CANCELLED`, or `AMENDMENT_DECLINED`.",
            JsonAttribute.path(
                AMENDED_TRANSPORT_DOCUMENT_STATUS,
                JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                    KeywordDataset.staticDataset(
                        AMENDED_TRANSPORT_DOCUMENT_STATUSES.toArray(String[]::new))))));
    checks.add(
        describedTdCheck(
            "The `amendedTransportDocumentStatus` must equal `%s`.".formatted(expectedStatus),
            JsonAttribute.path(
                AMENDED_TRANSPORT_DOCUMENT_STATUS,
                JsonAttribute.matchedMustEqual(expectedStatus::name))));
    return JsonAttribute.contentChecks(
        EblRole::isCarrier, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  public static JsonContentMatchedValidation utilizedTransportEquipmentsScenarioSizeCheck(
      ScenarioType scenarioType) {
    return (body, contextPath) -> {
      var utilizedTransportEquipments = body.path(UTILIZED_TRANSPORT_EQUIPMENTS);
      int actualSize = utilizedTransportEquipments.size();

      if (!ScenarioType.REGULAR_2C_1U.equals(scenarioType)
          && !ScenarioType.REGULAR_2C_2U.equals(scenarioType)) {
        return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
      }

      Integer expectedSize =
          switch (scenarioType) {
            case ScenarioType.REGULAR_2C_1U -> 1;
            case ScenarioType.REGULAR_2C_2U -> 2;
            default -> null;
          };

      if (actualSize != expectedSize) {
        String path = concatContextPath(contextPath, UTILIZED_TRANSPORT_EQUIPMENTS);
        return ConformanceCheckResult.simple(
            Set.of(
                "The scenario requires exactly %d '%s' but found %d at %s."
                    .formatted(expectedSize, UTILIZED_TRANSPORT_EQUIPMENTS, actualSize, path)));
      }

      return ConformanceCheckResult.simple(Set.of());
    };
  }

  public static JsonContentMatchedValidation consignmentItemsScenarioSizeCheck(
      ScenarioType scenarioType) {
    return (body, contextPath) -> {
      var consignmentItems = body.path(CONSIGNMENT_ITEMS);
      int actualSize = consignmentItems.size();

      if (!ScenarioType.REGULAR_2C_1U.equals(scenarioType)
          && !ScenarioType.REGULAR_2C_2U.equals(scenarioType)) {
        return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
      }

      int expectedSize = 2;
      if (actualSize != expectedSize) {
        String path = concatContextPath(contextPath, CONSIGNMENT_ITEMS);
        return ConformanceCheckResult.simple(
            Set.of(
                "The scenario requires exactly %d '%s' but found %d at %s."
                    .formatted(expectedSize, CONSIGNMENT_ITEMS, actualSize, path)));
      }

      return ConformanceCheckResult.simple(Set.of());
    };
  }

  private static Set<String> allEquipmentReferences(JsonNode body) {
    return allEquipmentReferences(body, null);
  }

  private static Set<String> allEquipmentReferences(JsonNode body, Set<String> duplicates) {
    var seen = new HashSet<String>();
    for (var ute : body.path(UTILIZED_TRANSPORT_EQUIPMENTS)) {
      // TD or SI with SOC
      var ref = ute.path(EQUIPMENT).path(EQUIPMENT_REFERENCE).asText(null);
      if (ref == null) {
        // SI with COC
        ref = ute.path(EQUIPMENT_REFERENCE).asText(null);
      }
      if (ref == null) {
        continue;
      }
      if (!seen.add(ref) && duplicates != null) {
        duplicates.add(ref);
      }
    }
    return seen;
  }

  private boolean isReeferContainerSizeTypeCode(String isoEquipmentCode) {
    // DT-437
    var codeChar = isoEquipmentCode.length() > 2 ? isoEquipmentCode.charAt(2) : '?';
    return codeChar == 'R' || codeChar == 'H';
  }
}
