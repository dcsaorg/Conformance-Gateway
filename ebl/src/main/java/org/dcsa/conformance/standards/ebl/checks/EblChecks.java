package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.core.check.JsonAttribute.concatContextPath;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.EXEMPT_PACKAGE_CODES;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.FEEDBACKS_CODE;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.FEEDBACKS_SEVERITY;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.METHOD_OF_PAYMENT;
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
  private static final String S_x_S_x_S_S = "%s.*.%s.*.%s.%s";
  private static final String S_x_S_x_S_x_S = "%s.*.%s.*.%s.*.%s";
  private static final String S_x_S_S_x_S = "%s.*.%s.%s.*.%s";
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
  private static final String IS_SHIPPED_ON_BOARD_TYPE = "isShippedOnBoardType";
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
  private static final String TRANSPORT_DOCUMENT = "transportDocument";
  private static final String SCENARIO = "Scenario";
  private static final String CARRIERS_AGENT_AT_DESTINATION = "carriersAgentAtDestination";
  private static final String SHIPPING_INSTRUCTIONS = "shippingInstructions";
  private static final String IS_CARRIERS_AGENT_AT_DESTINATION_REQUIRED =
      "isCarriersAgentAtDestinationRequired";
  private static final String IS_SHIPPER_OWNED = "isShipperOwned";

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

  private static final JsonPointer TD_REF_TDR_PTR =
      JsonPointer.compile(S.formatted(TRANSPORT_DOCUMENT_REFERENCE));

  private static final JsonPointer TD_REF_TD_STATUS_PTR =
      JsonPointer.compile(S.formatted(TRANSPORT_DOCUMENT_STATUS));

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
          if (IS_AN_EBL.test(node)) {
            if (numberOfCopiesNode.isMissingNode() || numberOfCopiesNode.asText().equals(ZERO)) {
              return ConformanceCheckResult.simple(Set.of());
            }
            String path = concatContextPath(contextPath, fieldName);
            return ConformanceCheckResult.simple(Set.of("%s at %s.".formatted(errorMessage, path)));
          }
          return ConformanceCheckResult.simple(Set.of());
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
          if (node.path(TRANSPORT_DOCUMENT_TYPE_CODE).asText("").equals(SWB)) {
            if (numberOfOriginalsNode.isMissingNode()) {
              return ConformanceCheckResult.simple(Set.of());
            }
            String path = concatContextPath(contextPath, fieldName);
            return ConformanceCheckResult.simple(Set.of("%s at %s.".formatted(errorMessage, path)));
          }
          return ConformanceCheckResult.simple(Set.of());
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
          "Validate if '%s' or '%s' present in document parties - '%s', '%s', '%s', '%s' and '%s'."
              .formatted(
                  ADDRESS, IDENTIFYING_CODES, SHIPPER, CONSIGNEE, ENDORSEE, NOTIFY_PARTIES, OTHER),
          (body, contextPath) -> {
            var documentParties = body.path(DOCUMENT_PARTIES);
            var issues = new LinkedHashSet<String>();
            Iterator<Map.Entry<String, JsonNode>> fields = documentParties.fields();

            while (fields.hasNext()) {
              Map.Entry<String, JsonNode> field = fields.next();
              JsonNode childNode = field.getValue();

              switch (field.getKey()) {
                case OTHER -> {
                  var otherDocumentParties = field.getValue();
                  for (JsonNode node : otherDocumentParties) {
                    issues.addAll(validateDocumentPartyFields(node.path(PARTY), field.getKey()));
                  }
                }
                case NOTIFY_PARTIES -> {
                  var notifyParties = field.getValue();
                  for (JsonNode node : notifyParties) {
                    issues.addAll(validateDocumentPartyFields(node, field.getKey()));
                  }
                }
                case BUYER, SELLER -> {
                  // No validation needed for buyer and seller
                }
                default -> issues.addAll(validateDocumentPartyFields(childNode, field.getKey()));
              }
            }
            return ConformanceCheckResult.simple(issues);
          });

  private static Set<String> validateDocumentPartyFields(
      JsonNode documentPartyNode, String partyName) {
    var issues = new LinkedHashSet<String>();
    var address = documentPartyNode.path(ADDRESS);
    var identifyingCodes = documentPartyNode.path(IDENTIFYING_CODES);
    if (address.isMissingNode() && identifyingCodes.isMissingNode()) {
      issues.add(
          "'%s' or '%s' must be provided in party '%s'."
              .formatted(ADDRESS, IDENTIFYING_CODES, partyName));
    }
    return issues;
  }

  private static final JsonRebasableContentCheck DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS =
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
                    DOCUMENT_PARTIES, SELLER, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
                    DOCUMENT_PARTIES, BUYER, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            mav.submitAllMatching(
                S_S_S_x_S.formatted(
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

  private static final JsonRebasableContentCheck VALID_WOOD_DECLARATIONS =
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

  static final JsonRebasableContentCheck VALID_CONSIGNMENT_ITEMS_REFERENCE_TYPES =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All '%s' reference '%s' fields must be valid.".formatted(CONSIGNMENT_ITEMS, TYPE),
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
            S_S_x_S.formatted(DOCUMENT_PARTIES, NOTIFY_PARTIES, DISPLAYED_ADDRESS));
      };

  private static final JsonRebasableContentCheck EBL_DISPLAYED_ADDRESS_LIMIT =
      JsonAttribute.ifThen(
          "Validate displayed address length for eBLs. A maximum of 6 lines can be provided for electronic Bills of Lading.",
          td -> td.path(IS_ELECTRONIC).asBoolean(true),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "(not used)", DISPLAYED_ADDRESS_MAV_CONSUMER, JsonAttribute.matchedMaxLength(6)));

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

  private static final JsonRebasableContentCheck NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All '%s' where '%s' is false must have '%s'."
              .formatted(
                  UTILIZED_TRANSPORT_EQUIPMENTS, IS_NON_OPERATING_REEFER, ACTIVE_REEFER_SETTINGS),
          ALL_UTE,
          JsonAttribute.ifMatchedThen(
              JsonAttribute.isFalse(IS_NON_OPERATING_REEFER),
              JsonAttribute.path(ACTIVE_REEFER_SETTINGS, JsonAttribute.matchedMustBePresent())));

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

  private static final JsonRebasableContentCheck NATIONAL_COMMODITY_CODE_IS_VALID =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate that '%s' of '%s' is a known code.".formatted(TYPE, NATIONAL_COMMODITY_CODES),
          mav ->
              mav.submitAllMatching(
                  S_x_S_x_S.formatted(CONSIGNMENT_ITEMS, NATIONAL_COMMODITY_CODES, TYPE)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(NATIONAL_COMMODITY_CODES_SET));

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
            if (isToOrder && node.path(DOCUMENT_PARTIES).path(NOTIFY_PARTY).isMissingNode()) {
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
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.METHOD_OF_PAYMENT));

  private static final JsonRebasableContentCheck VALIDATE_CARRIER_CODE_AND_LIST_PROVIDER =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If '%s' is present, then '%s' is required and vice versa."
              .formatted(CARRIER_CODE, CARRIER_CODE_LIST_PROVIDER),
          mav -> {
            mav.submitAllMatching(CARRIER_CODE);
            mav.submitAllMatching(CARRIER_CODE_LIST_PROVIDER);
          },
          (node, contextPath) -> {
            boolean hasCarrierCode = !node.path(CARRIER_CODE).isMissingNode();
            boolean hasProvider = !node.path(CARRIER_CODE_LIST_PROVIDER).isMissingNode();

            if (hasCarrierCode && !hasProvider) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "'%s' is required when '%s' is present at %s."
                          .formatted(CARRIER_CODE_LIST_PROVIDER, CARRIER_CODE, contextPath)));
            }
            if (!hasCarrierCode && hasProvider) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "'%s' is required when '%s' is present at %s."
                          .formatted(CARRIER_CODE, CARRIER_CODE_LIST_PROVIDER, contextPath)));
            }
            return ConformanceCheckResult.simple(Set.of());
          });

  private static final JsonRebasableContentCheck VALID_TYPE_OF_PERSON =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate '%s' values in '%s'.".formatted(TYPE_OF_PERSON, DOCUMENT_PARTIES),
          mav -> {
            // single objects
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, SHIPPER, TYPE_OF_PERSON));
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, CONSIGNEE, TYPE_OF_PERSON));
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, SELLER, TYPE_OF_PERSON));
            mav.submitAllMatching(S_S_S.formatted(DOCUMENT_PARTIES, BUYER, TYPE_OF_PERSON));
            // array of notifyParty objects
            mav.submitAllMatching(
                S_S_x_S.formatted(DOCUMENT_PARTIES, NOTIFY_PARTIES, TYPE_OF_PERSON));
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
            if (routingOfConsignmentCountries.isMissingNode()
                || !routingOfConsignmentCountries.isArray()) {
              return ConformanceCheckResult.simple(Set.of());
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
              return ConformanceCheckResult.simple(Set.of());
            }
            JsonNode advanceManifestFilings = node.path(ADVANCE_MANIFEST_FILINGS);
            if (advanceManifestFilings.isMissingNode() || !advanceManifestFilings.isArray()) {
              return ConformanceCheckResult.simple(Set.of());
            }
            boolean isHouseBlsIssued = node.path(IS_HOUSE_BILL_OF_LADINGS_ISSUED).asBoolean(true);
            int index = 0;
            for (JsonNode hbl : houseBillOfLadings) {
              if (hbl.path(IS_CARGO_DELIVERED_IN_ICS_2_ZONE).asBoolean(false)) {
                for (JsonNode filing : advanceManifestFilings) {
                  if (CARRIER.equals(filing.path(AMF_HBL_PERFORMED_BY).asText())
                      && ENS.equals(filing.path(MANIFEST_TYPE_CODE).asText())
                      && !isHouseBlsIssued
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
              }
              index++;
            }
            return ConformanceCheckResult.simple(Set.of());
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
          ONLY_EBLS_CAN_BE_NEGOTIABLE,
          EBL_AT_MOST_ONE_ORIGINAL_TOTAL,
          EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES,
          EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES,
          SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES,
          SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES,
          VALIDATE_DOCUMENT_PARTY,
          JsonAttribute.ifThenElse(
              "If '%s' is present, then '%s' or '%s' must be present."
                  .formatted(
                      IS_SHIPPED_ON_BOARD_TYPE, SHIPPED_ON_BOARD_DATE, RECEIVED_FOR_SHIPMENT_DATE),
              JsonAttribute.isTrue(JsonPointer.compile(S.formatted(IS_SHIPPED_ON_BOARD_TYPE))),
              JsonAttribute.mustBePresent(JsonPointer.compile(S.formatted(SHIPPED_ON_BOARD_DATE))),
              JsonAttribute.mustBePresent(
                  JsonPointer.compile(S.formatted(RECEIVED_FOR_SHIPMENT_DATE)))),
          JsonAttribute.atMostOneOf(
              JsonPointer.compile(S.formatted(SHIPPED_ON_BOARD_DATE)),
              JsonPointer.compile(S.formatted(RECEIVED_FOR_SHIPMENT_DATE))),
          JsonAttribute.mustBeDatasetKeywordIfPresent(
              JsonPointer.compile(S.formatted(CARGO_MOVEMENT_TYPE_AT_ORIGIN)),
              EblDatasets.CARGO_MOVEMENT_TYPE),
          JsonAttribute.mustBeDatasetKeywordIfPresent(
              JsonPointer.compile(S.formatted(CARGO_MOVEMENT_TYPE_AT_DESTINATION)),
              EblDatasets.CARGO_MOVEMENT_TYPE),
          // receiptTypeAtOrigin + deliveryTypeAtDestination are schema validated
          JsonAttribute.allOrNoneArePresent(
              JsonPointer.compile(S.formatted(DECLARED_VALUE)),
              JsonPointer.compile(S.formatted(DECLARED_VALUE_CURRENCY))),
          JsonAttribute.ifThen(
              "'%s' implies '%s.".formatted(PRE_CARRIAGE_BY, PLACE_OF_RECEIPT),
              JsonAttribute.isNotNull(
                  JsonPointer.compile(SS.formatted(TRANSPORTS, PRE_CARRIAGE_BY))),
              JsonAttribute.mustBeNotNull(
                  JsonPointer.compile(SS.formatted(TRANSPORTS, PLACE_OF_RECEIPT)),
                  "'%s' is present.".formatted(PRE_CARRIAGE_BY))),
          JsonAttribute.ifThen(
              "'%s' implies '%s'.".formatted(ON_CARRIAGE_BY, PLACE_OF_DELIVERY),
              JsonAttribute.isNotNull(
                  JsonPointer.compile(SS.formatted(TRANSPORTS, ON_CARRIAGE_BY))),
              JsonAttribute.mustBeNotNull(
                  JsonPointer.compile(SS.formatted(TRANSPORTS, PLACE_OF_DELIVERY)),
                  "'%s' is present.".formatted(ON_CARRIAGE_BY))),
          DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS,
          VALID_WOOD_DECLARATIONS,
          NATIONAL_COMMODITY_CODE_IS_VALID,
          VALID_REFERENCE_TYPES,
          VALID_CONSIGNMENT_ITEMS_REFERENCE_TYPES,
          ISO_EQUIPMENT_CODE_IMPLIES_REEFER,
          NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER,
          NOR_IS_TRUE_IMPLIES_NO_ACTIVE_REEFER,
          JsonAttribute.allIndividualMatchesMustBeValid(
              "'%s' implies '%s' or '%s'."
                  .formatted(DANGEROUS_GOODS, PACKAGE_CODE, IMO_PACKAGING_CODE),
              mav ->
                  mav.submitAllMatching(
                      S_x_S_x_S.formatted(CONSIGNMENT_ITEMS, CARGO_ITEMS, OUTER_PACKAGING)),
              (nodeToValidate, contextPath) -> {
                var dg = nodeToValidate.path(DANGEROUS_GOODS);
                if (!dg.isArray() || dg.isEmpty()) {
                  return ConformanceCheckResult.simple(Set.of());
                }
                if (nodeToValidate.path(PACKAGE_CODE).isMissingNode()
                    && nodeToValidate.path(IMO_PACKAGING_CODE).isMissingNode()) {
                  return ConformanceCheckResult.simple(
                      Set.of(
                          "The '%s' object did not have a '%s' nor an '%s', which is required due to '%s'."
                              .formatted(
                                  contextPath, PACKAGE_CODE, IMO_PACKAGING_CODE, DANGEROUS_GOODS)));
                }
                return ConformanceCheckResult.simple(Set.of());
              }),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The '%s' values must be from dataset.".formatted(INHALATION_ZONE),
              allDg(dg -> dg.path(INHALATION_ZONE).submitPath()),
              JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.DG_INHALATION_ZONES)),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The '%s' values must be from dataset.".formatted(SEGREGATION_GROUPS),
              allDg(dg -> dg.path(SEGREGATION_GROUPS).all().submitPath()),
              JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                  EblDatasets.DG_SEGREGATION_GROUPS)),
          UTE_EQUIPMENT_REFERENCE_UNIQUE,
          JsonAttribute.allIndividualMatchesMustBeValid(
              "If '%s' is present, '%s' must be present (and vice versa)."
                  .formatted(TEMPERATURE_SETPOINT, TEMPERATURE_UNIT),
              mav ->
                  mav.submitAllMatching(
                      S_x_S.formatted(UTILIZED_TRANSPORT_EQUIPMENTS, ACTIVE_REEFER_SETTINGS)),
              JsonAttribute.presenceImpliesOtherField(TEMPERATURE_SETPOINT, TEMPERATURE_UNIT)),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "If '%s' is present, '%s' must be present."
                  .formatted(TEMPERATURE_UNIT, TEMPERATURE_SETPOINT),
              mav ->
                  mav.submitAllMatching(
                      S_x_S.formatted(UTILIZED_TRANSPORT_EQUIPMENTS, ACTIVE_REEFER_SETTINGS)),
              JsonAttribute.presenceImpliesOtherField(TEMPERATURE_UNIT, TEMPERATURE_SETPOINT)),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The '%s' implies '%s'.".formatted(AIR_EXCHANGE_SETPOINT, AIR_EXCHANGE_UNIT),
              mav ->
                  mav.submitAllMatching(
                      S_x_S.formatted(UTILIZED_TRANSPORT_EQUIPMENTS, ACTIVE_REEFER_SETTINGS)),
              JsonAttribute.presenceImpliesOtherField(AIR_EXCHANGE_SETPOINT, AIR_EXCHANGE_UNIT)),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "If '%s' is present, '%s' must be present."
                  .formatted(AIR_EXCHANGE_UNIT, AIR_EXCHANGE_SETPOINT),
              mav ->
                  mav.submitAllMatching(
                      S_x_S.formatted(UTILIZED_TRANSPORT_EQUIPMENTS, ACTIVE_REEFER_SETTINGS)),
              JsonAttribute.presenceImpliesOtherField(AIR_EXCHANGE_UNIT, AIR_EXCHANGE_SETPOINT)),
          EBL_DISPLAYED_ADDRESS_LIMIT,
          CARGO_ITEM_REFERENCES_KNOWN_EQUIPMENT,
          ADVANCED_MANIFEST_FILING_CODES_UNIQUE,
          CR_CC_T_CODES_UNIQUE,
          JsonAttribute.allIndividualMatchesMustBeValid(
              "Validate mode of transport type.",
              mav -> {
                mav.submitAllMatching(S_S.formatted(TRANSPORTS, PRE_CARRIAGE_BY));
                mav.submitAllMatching(S_S.formatted(TRANSPORTS, ON_CARRIAGE_BY));
              },
              JsonAttribute.matchedMustBeDatasetKeywordIfPresent(MODE_OF_TRANSPORT)),
          NOTIFY_PARTIES_REQUIRED_IN_NEGOTIABLE_BLS,
          TLR_CC_T_COMBINATION_UNIQUE,
          VALID_PARTY_FUNCTION,
          VALID_TYPE_OF_PERSON);

  public static final JsonContentCheck SIR_OR_TDR_REQUIRED_IN_NOTIFICATION =
      JsonAttribute.atLeastOneOf(SI_REF_SIR_PTR, TD_REF_TDR_PTR);

  public static JsonContentCheck sirInNotificationMustMatchDSP(
      Supplier<EblDynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.mustEqual(
        SI_REF_SIR_PTR, () -> dspSupplier.get().shippingInstructionsReference());
  }

  public static JsonContentCheck tdrInNotificationMustMatchDSP(
      Supplier<EblDynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.mustEqual(
        TD_REF_TDR_PTR, () -> dspSupplier.get().transportDocumentReference());
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
      boolean requestAmendedStatus,
      Supplier<EblDynamicScenarioParameters> dspSupplier) {
    var checks =
        getSiPayloadChecks(
            standardVersion,
            shippingInstructionsStatus,
            updatedShippingInstructionsStatus,
            dspSupplier,
            requestAmendedStatus);
    return JsonAttribute.contentChecks(
        EblRole::isCarrier, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  public static List<JsonContentCheck> getSiPayloadChecks(
      String standardVersion,
      ShippingInstructionsStatus shippingInstructionsStatus,
      ShippingInstructionsStatus updatedShippingInstructionsStatus,
      Supplier<EblDynamicScenarioParameters> dspSupplier,
      boolean requestedAmendment) {
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
            if (SI_PENDING_UPDATE.wireName().equals(siStatus) && updatedSiStatus.isEmpty()) {
              var feedbacks = body.get(FEEDBACKS);
              if (feedbacks == null || feedbacks.isEmpty()) {
                issues.add(
                    "'%s' is missing for the si in status %s."
                        .formatted(FEEDBACKS, SI_PENDING_UPDATE.wireName()));
              }
            }
            return ConformanceCheckResult.simple(issues);
          });

  public static ActionCheck tdRefStatusChecks(
      UUID matched,
      String standardVersion,
      Supplier<EblDynamicScenarioParameters> dspSupplier,
      TransportDocumentStatus transportDocumentStatus) {
    return JsonAttribute.contentChecks(
        EblRole::isCarrier,
        matched,
        HttpMessageType.RESPONSE,
        standardVersion,
        JsonAttribute.mustEqual(
            TD_REF_TDR_PTR, () -> dspSupplier.get().transportDocumentReference()),
        JsonAttribute.mustEqual(TD_REF_TD_STATUS_PTR, transportDocumentStatus.wireName()));
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
      TransportDocumentStatus transportDocumentStatus, JsonContentCheck... extraChecks) {
    List<JsonContentCheck> jsonContentChecks = new ArrayList<>(Arrays.asList(extraChecks));

    jsonContentChecks.add(
        JsonAttribute.mustEqual(TD_REF_TD_STATUS_PTR, transportDocumentStatus.wireName()));

    jsonContentChecks.add(FEEDBACKS_PRESENCE);
    jsonContentChecks.add(VALID_FEEDBACKS_SEVERITY);
    jsonContentChecks.add(VALID_FEEDBACKS_CODE);
    return jsonContentChecks;
  }

  private static void genericTdContentChecks(
      List<? super JsonRebasableContentCheck> jsonContentChecks,
      Supplier<String> tdrSupplier,
      TransportDocumentStatus transportDocumentStatus) {
    if (tdrSupplier != null) {
      jsonContentChecks.add(JsonAttribute.mustEqual(TD_TDR, tdrSupplier));
    }
    jsonContentChecks.add(
        JsonAttribute.mustEqual(TD_TRANSPORT_DOCUMENT_STATUS, transportDocumentStatus.wireName()));
    jsonContentChecks.addAll(STATIC_TD_CHECKS);
    jsonContentChecks.add(DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE);
    jsonContentChecks.add(VALIDATE_DOCUMENT_PARTIES_MATCH_EBL);
  }

  public static List<JsonRebasableContentCheck> genericTDContentChecks(
      TransportDocumentStatus transportDocumentStatus, Supplier<String> tdrReferenceSupplier) {
    List<JsonRebasableContentCheck> jsonContentChecks = new ArrayList<>();
    genericTdContentChecks(jsonContentChecks, tdrReferenceSupplier, transportDocumentStatus);
    return jsonContentChecks;
  }

  public static ActionCheck tdPlusScenarioContentChecks(
      UUID matched,
      String standardVersion,
      TransportDocumentStatus transportDocumentStatus,
      Supplier<EblDynamicScenarioParameters> dspSupplier) {
    List<JsonContentCheck> jsonContentChecks =
        getTdPayloadChecks(transportDocumentStatus, dspSupplier);
    return JsonAttribute.contentChecks(
        EblRole::isCarrier, matched, HttpMessageType.RESPONSE, standardVersion, jsonContentChecks);
  }

  public static List<JsonContentCheck> getTdPayloadChecks(
      TransportDocumentStatus transportDocumentStatus,
      Supplier<EblDynamicScenarioParameters> dspSupplier) {

    List<JsonContentCheck> jsonContentChecks = new ArrayList<>();

    genericTdContentChecks(
        jsonContentChecks,
        () -> dspSupplier.get().transportDocumentReference(),
        transportDocumentStatus);

    jsonContentChecks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "[%s] Validate the containers reefer settings.".formatted(SCENARIO),
            mav -> mav.submitAllMatching(S_x.formatted(UTILIZED_TRANSPORT_EQUIPMENTS)),
            (nodeToValidate, contextPath) -> {
              var activeReeferNode = nodeToValidate.path(ACTIVE_REEFER_SETTINGS);
              var nonOperatingReeferNode = nodeToValidate.path(IS_NON_OPERATING_REEFER);
              var issues = new LinkedHashSet<String>();
              switch (ScenarioType.valueOf(dspSupplier.get().scenarioType())) {
                case ACTIVE_REEFER -> {
                  if (!activeReeferNode.isObject()) {
                    issues.add(
                        "The scenario requires '%s' to have an active reefer"
                            .formatted(contextPath));
                  }
                }
                case NON_OPERATING_REEFER -> {
                  if (!nonOperatingReeferNode.asBoolean(false)) {
                    issues.add(
                        "The scenario requires '%s.%s' to be true"
                            .formatted(contextPath, IS_NON_OPERATING_REEFER));
                  }
                }
              }
              return ConformanceCheckResult.simple(issues);
            }));
    jsonContentChecks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "[%s] Whether the cargo should be DG.".formatted(SCENARIO),
            mav ->
                mav.submitAllMatching(
                    S_x_S_x_S_x_S.formatted(
                        CONSIGNMENT_ITEMS, CARGO_ITEMS, OUTER_PACKAGING, DANGEROUS_GOODS)),
            (nodeToValidate, contextPath) -> {
              if (ScenarioType.valueOf(dspSupplier.get().scenarioType()) == ScenarioType.DG) {
                if (!nodeToValidate.isArray() || nodeToValidate.isEmpty()) {
                  return ConformanceCheckResult.simple(
                      Set.of(
                          "The scenario requires '%s' to contain '%s'."
                              .formatted(contextPath, DANGEROUS_GOODS)));
                }
              }
              return ConformanceCheckResult.simple(Set.of());
            }));
    jsonContentChecks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "[%s] The '%s' should be true for SOC scenarios.".formatted(SCENARIO, IS_SHIPPER_OWNED),
            mav -> mav.submitAllMatching(S_x.formatted(UTILIZED_TRANSPORT_EQUIPMENTS)),
            (nodeToValidate, contextPath) -> {
              var scenario = ScenarioType.valueOf(dspSupplier.get().scenarioType());
              if (scenario == ScenarioType.REGULAR_SWB_SOC_AND_REFERENCES) {
                if (!nodeToValidate.path(IS_SHIPPER_OWNED).asBoolean(false)) {
                  return ConformanceCheckResult.simple(
                      Set.of(
                          "The scenario requires '%s.%s' to be true."
                              .formatted(contextPath, IS_SHIPPER_OWNED)));
                }
              }
              return ConformanceCheckResult.simple(Set.of());
            }));
    jsonContentChecks.addAll(
        generateScenarioRelatedChecks(
            ScenarioType.valueOf(dspSupplier.get().scenarioType()),
            true,
            dspSupplier.get().isCladInSI()));
    return jsonContentChecks;
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
