package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.core.check.JsonAttribute.concatContextPath;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.EXEMPT_PACKAGE_CODES;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.FEEDBACKS_CODE;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.FEEDBACKS_SEVERITY;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.MODE_OF_TRANSPORT;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.NATIONAL_COMMODITY_CODES;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.PARTY_FUNCTION_CODE;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.PARTY_FUNCTION_CODE_HBL;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.REQUESTED_CARRIER_CLAUSES;
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

  private static final JsonPointer SI_REF_SIR_PTR = JsonPointer.compile("/shippingInstructionsReference");
  private static final JsonPointer SI_REF_SI_STATUS_PTR = JsonPointer.compile("/shippingInstructionsStatus");
  private static final JsonPointer SI_REF_UPDATED_SI_STATUS_PTR = JsonPointer.compile("/updatedShippingInstructionsStatus");

  private static final JsonPointer TD_REF_TDR_PTR = JsonPointer.compile("/transportDocumentReference");
  private static final JsonPointer TD_REF_TD_STATUS_PTR = JsonPointer.compile("/transportDocumentStatus");

  private static final JsonPointer SI_REQUEST_SEND_TO_PLATFORM = JsonPointer.compile("/documentParties/issueTo/sendToPlatform");

  private static final JsonPointer TD_TDR = JsonPointer.compile("/transportDocumentReference");
  private static final JsonPointer TD_TRANSPORT_DOCUMENT_STATUS = JsonPointer.compile("/transportDocumentStatus");

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


  private static final BiConsumer<JsonNode, TriConsumer<JsonNode, String, ArrayOrderHandler>> DOC_PARTY_ARRAY_ORDER_DEFINITIONS =
    (documentPartyNode, arrayNodeHandler) -> {
      arrayNodeHandler.accept(
        documentPartyNode, "displayedAddress", ArrayOrderHandler.inputPreservedArrayOrder());
      arrayNodeHandler.accept(
        documentPartyNode, "identifyingCodes", ArrayOrderHandler.toStringSortableArray());
      arrayNodeHandler.accept(
        documentPartyNode, "taxLegalReferences", ArrayOrderHandler.toStringSortableArray());
      arrayNodeHandler.accept(
        documentPartyNode, PARTY_CONTACT_DETAILS, ArrayOrderHandler.toStringSortableArray());
    };

  private static final BiConsumer<JsonNode, TriConsumer<JsonNode, String, ArrayOrderHandler>> DOC_PARTIES_ARRAY_ORDER_DEFINITIONS =
    (documentPartyNode, arrayNodeHandler) -> {
      for (var partyName : List.of("shipper", "consignee", "notifyParty", SELLER, "buyer", "endorsee", "issueTo")) {
        DOC_PARTY_ARRAY_ORDER_DEFINITIONS.accept(documentPartyNode.path(partyName), arrayNodeHandler);
      }

      arrayNodeHandler.accept(
        documentPartyNode,
        "notifyParties",
        ArrayOrderHandler.inputPreservedArrayOrder());
      for (var party : documentPartyNode.path("notifyParties")) {
        DOC_PARTY_ARRAY_ORDER_DEFINITIONS.accept(party, arrayNodeHandler);
      }
      arrayNodeHandler.accept(
        documentPartyNode,
        "other",
        ArrayOrderHandler.toStringSortableArray());
      for (var party : documentPartyNode.path("other")) {
        DOC_PARTY_ARRAY_ORDER_DEFINITIONS.accept(party, arrayNodeHandler);
      }
    };

  static final JsonRebaseableContentCheck VALID_REQUESTED_CARRIER_CLAUSES =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate that 'requestedCarrierClauses' is valid",
          mav -> mav.submitAllMatching("requestedCarrierClauses.*"),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(REQUESTED_CARRIER_CLAUSES));

  public static final BiConsumer<JsonNode, TriConsumer<JsonNode, String, ArrayOrderHandler>>
      SI_ARRAY_ORDER_DEFINITIONS =
          (rootNode, arrayNodeHandler) -> {
            arrayNodeHandler.accept(
              rootNode, PARTY_CONTACT_DETAILS, ArrayOrderHandler.inputPreservedArrayOrder());
            for (var ci : rootNode.path(CONSIGNMENT_ITEMS)) {
              arrayNodeHandler.accept(
                ci, "descriptionOfGoods", ArrayOrderHandler.toStringSortableArray());
              arrayNodeHandler.accept(
                ci, "HSCodes", ArrayOrderHandler.toStringSortableArray());
              arrayNodeHandler.accept(
                ci, "nationalCommodityCodes", ArrayOrderHandler.toStringSortableArray());
              arrayNodeHandler.accept(
                ci, "shippingMarks", ArrayOrderHandler.toStringSortableArray());
              for (var cargoItem : ci.path("cargoItems")) {
                arrayNodeHandler.accept(cargoItem, "nationalCommodityCodes", ArrayOrderHandler.toStringSortableArray());
                for (var cr : cargoItem.path(CUSTOMS_REFERENCES)) {
                  arrayNodeHandler.accept(cr, "values", ArrayOrderHandler.toStringSortableArray());
                }
                arrayNodeHandler.accept(
                  ci, CUSTOMS_REFERENCES, ArrayOrderHandler.toStringSortableArray());
              }
              arrayNodeHandler.accept(ci, "cargoItems", ArrayOrderHandler.toStringSortableArray());
              for (var cr : ci.path(CUSTOMS_REFERENCES)) {
                arrayNodeHandler.accept(cr, "values", ArrayOrderHandler.toStringSortableArray());
              }
              arrayNodeHandler.accept(
                ci, CUSTOMS_REFERENCES, ArrayOrderHandler.toStringSortableArray());
            }

            arrayNodeHandler.accept(
              rootNode, CONSIGNMENT_ITEMS, ArrayOrderHandler.toStringSortableArray());
            for (var ute : rootNode.path(UTILIZED_TRANSPORT_EQUIPMENTS)) {
              arrayNodeHandler.accept(
                ute, "shippingMarks", ArrayOrderHandler.toStringSortableArray());
              arrayNodeHandler.accept(
                ute, "seals", ArrayOrderHandler.toStringSortableArray());
              arrayNodeHandler.accept(
                ute, "references", ArrayOrderHandler.toStringSortableArray());
              for (var cr : ute.path(CUSTOMS_REFERENCES)) {
                arrayNodeHandler.accept(cr, "values", ArrayOrderHandler.toStringSortableArray());
              }
              arrayNodeHandler.accept(
                ute, CUSTOMS_REFERENCES, ArrayOrderHandler.toStringSortableArray());
            }
            arrayNodeHandler.accept(
              rootNode, UTILIZED_TRANSPORT_EQUIPMENTS, ArrayOrderHandler.toStringSortableArray());
            arrayNodeHandler.accept(
                rootNode, ADVANCE_MANIFEST_FILINGS, ArrayOrderHandler.toStringSortableArray());
            arrayNodeHandler.accept(
                rootNode, "references", ArrayOrderHandler.toStringSortableArray());
            for (var cr : rootNode.path(CUSTOMS_REFERENCES)) {
              arrayNodeHandler.accept(cr, "values", ArrayOrderHandler.toStringSortableArray());
            }
            arrayNodeHandler.accept(
              rootNode, CUSTOMS_REFERENCES, ArrayOrderHandler.toStringSortableArray());

            DOC_PARTIES_ARRAY_ORDER_DEFINITIONS.accept(rootNode.path(DOCUMENT_PARTIES), arrayNodeHandler);

            for (var hbl : rootNode.path(HOUSE_BILL_OF_LADINGS)) {
              DOC_PARTIES_ARRAY_ORDER_DEFINITIONS.accept(hbl.path(DOCUMENT_PARTIES), arrayNodeHandler);
              arrayNodeHandler.accept(
                hbl, ROUTING_OF_CONSIGNMENT_COUNTRIES, ArrayOrderHandler.inputPreservedArrayOrder());
            }
            arrayNodeHandler.accept(
              rootNode, HOUSE_BILL_OF_LADINGS, ArrayOrderHandler.toStringSortableArray());
            arrayNodeHandler.accept(
                rootNode,
                "requestedCarrierCertificates",
                ArrayOrderHandler.toStringSortableArray());
            arrayNodeHandler.accept(
                rootNode, "requestedCarrierClauses", ArrayOrderHandler.toStringSortableArray());
          };

    private static final BiConsumer<JsonNode, JsonNode> SI_NORMALIZER = (leftNode, rhsNode) -> {
        for (var node : List.of(leftNode, rhsNode)) {
            SI_ARRAY_ORDER_DEFINITIONS.accept(node, ArrayOrderHelper::restoreArrayOrder);
        }
    };

    private static final JsonRebaseableContentCheck ONLY_EBLS_CAN_BE_NEGOTIABLE = JsonAttribute.ifThen(
    "Validate transportDocumentTypeCode vs. isToOrder",
    JsonAttribute.isTrue(JsonPointer.compile("/isToOrder")),
    JsonAttribute.mustEqual(JsonPointer.compile("/transportDocumentTypeCode"), "BOL")
  );

  private static final Predicate<JsonNode> IS_ELECTRONIC =
      td -> td.path("isElectronic").asBoolean(false);

  private static final Predicate<JsonNode> IS_AN_EBL =
      IS_ELECTRONIC.and(td -> td.path("transportDocumentTypeCode").asText("").equals("BOL"));

  static final JsonRebaseableContentCheck EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES =
      eblsCannotHaveCopiesCheck(
          NUMBER_OF_COPIES_WITH_CHARGES,
          "Electronic original Bills of Lading cannot have any copies with charges.");

  static final JsonRebaseableContentCheck EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES =
      eblsCannotHaveCopiesCheck(
          "numberOfCopiesWithoutCharges",
          "Electronic original Bills of Lading cannot have any copies without charges.");

  private static JsonRebaseableContentCheck eblsCannotHaveCopiesCheck(
      String fieldName, String errorMessage) {
    return JsonAttribute.customValidator(
        errorMessage,
        (node, contextPath) -> {
          JsonNode numberOfCopiesNode = node.path(fieldName);
          if (IS_AN_EBL.test(node)) {
            if (numberOfCopiesNode.isMissingNode() || numberOfCopiesNode.asText().equals("0")) {
              return Set.of();
            }
            String path = concatContextPath(contextPath, fieldName);
            return Set.of("%s at %s".formatted(errorMessage, path));
          }
          return Set.of();
        });
  }

  static final JsonRebaseableContentCheck SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES =
      eblsCannotHaveOriginalsCheck(
          "numberOfOriginalsWithCharges",
          "Number of originals with charges must be absent for SWBs");
  static final JsonRebaseableContentCheck SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES =
      eblsCannotHaveOriginalsCheck(
          "numberOfOriginalsWithoutCharges",
          "Number of originals without charges must be absent for SWBs");

  private static JsonRebaseableContentCheck eblsCannotHaveOriginalsCheck(
      String fieldName, String errorMessage) {
    return JsonAttribute.customValidator(
        errorMessage,
        (node, contextPath) -> {
          JsonNode numberOfOriginalsNode = node.path(fieldName);
          if (node.path("transportDocumentTypeCode").asText("").equals("SWB")) {
            if (numberOfOriginalsNode.isMissingNode()) {
              return Set.of();
            }
            String path = concatContextPath(contextPath, fieldName);
            return Set.of("%s at %s".formatted(errorMessage, path));
          }
          return Set.of();
        });
  }

  static final JsonRebaseableContentCheck EBL_AT_MOST_ONE_ORIGINAL_TOTAL =
      JsonAttribute.ifThen(
          "Cannot have more than one original in total when isElectronic",
          IS_AN_EBL,
          JsonAttribute.customValidator(
              "Sum of 'numberOfOriginalsWithoutCharges' and 'numberOfOriginalsWithCharges' must be at most 1 for Electronic original Bills of Ladings.",
              (node, contextPath) -> {
                int withoutCharges = node.path("numberOfOriginalsWithoutCharges").asInt(0);
                int withCharges = node.path("numberOfOriginalsWithCharges").asInt(0);
                int total = withoutCharges + withCharges;

                if (total > 1) {
                  return Set.of(
                      "The sum of 'numberOfOriginalsWithoutCharges' (%d) and 'numberOfOriginalsWithCharges' (%d) cannot exceed 1 for Electronic original Bills of Ladings, but was %d at '%s'"
                          .formatted(withoutCharges, withCharges, total, contextPath));
                }
                return Set.of();
              }));

  static final JsonRebaseableContentCheck VALIDATE_DOCUMENT_PARTY =
      JsonAttribute.customValidator(
          "Validate if address or identifyingCodes present in document parties - shipper, consignee,endorsee, notify parties and 'other' ",
          (body, contextPath) -> {
            var documentParties = body.path(DOCUMENT_PARTIES);
            var issues = new LinkedHashSet<String>();
            Iterator<Map.Entry<String, JsonNode>> fields = documentParties.fields();

            while (fields.hasNext()) {
              Map.Entry<String, JsonNode> field = fields.next();
              JsonNode childNode = field.getValue();

              switch (field.getKey()) {
                case "other" -> {
                  var otherDocumentParties = field.getValue();
                  for (JsonNode node : otherDocumentParties) {
                    issues.addAll(validateDocumentPartyFields(node.path("party"), field.getKey()));
                  }
                }
                case "notifyParties" -> {
                  var notifyParties = field.getValue();
                  for (JsonNode node : notifyParties) {
                    issues.addAll(validateDocumentPartyFields(node, field.getKey()));
                  }
                }
                case "buyer", SELLER -> {
                  // No validation needed for buyer and seller
                }
                default -> issues.addAll(validateDocumentPartyFields(childNode, field.getKey()));
              }
            }
            return issues;
          });

  private static Set<String> validateDocumentPartyFields(JsonNode documentPartyNode, String partyName) {
    var issues = new LinkedHashSet<String>();
    var address = documentPartyNode.path("address");
    var identifyingCodes = documentPartyNode.path("identifyingCodes");
    if (address.isMissingNode() && identifyingCodes.isMissingNode()) {
      issues.add("address or identifyingCodes must be provided in party '%s' ".formatted(partyName));
    }
    return issues;
  }

  private static final JsonRebaseableContentCheck DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS = JsonAttribute.allIndividualMatchesMustBeValid(
    "The code in 'codeListProvider' is known",
    mav -> {
      mav.submitAllMatching("documentParties.shipper.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("documentParties.consignee.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("documentParties.endorsee.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("documentParties.issueTo.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("documentParties.seller.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("documentParties.buyer.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("documentParties.notifyParties.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("documentParties.other.party.identifyingCodes.*.codeListProvider");


      mav.submitAllMatching("houseBillOfLadings.*.documentParties.shipper.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("houseBillOfLadings.*.documentParties.consignee.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("houseBillOfLadings.*.documentParties.notifyParty.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("houseBillOfLadings.*.documentParties.seller.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("houseBillOfLadings.*.documentParties.buyer.identifyingCodes.*.codeListProvider");
      mav.submitAllMatching("houseBillOfLadings.*.documentParties.other.party.identifyingCodes.*.codeListProvider");
    },
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES)
  );

  private static final JsonRebaseableContentCheck NOTIFY_PARTIES_REQUIRED_IN_NEGOTIABLE_BLS =
      JsonAttribute.ifThen(
          "The 'documentParties.notifyParties' attribute is mandatory when 'isToOrder' is true",
          JsonAttribute.isTrue(IS_TO_ORDER),
          JsonAttribute.at(
              JsonPointer.compile("/documentParties/notifyParties"),
              JsonAttribute.matchedMustBeNonEmpty()));

  private static final Consumer<MultiAttributeValidator> ALL_REFERENCE_TYPES = mav -> {
    mav.submitAllMatching("references.*.type");
    mav.submitAllMatching("utilizedTransportEquipments.*.references.*.type");
  };

  private static final JsonRebaseableContentCheck VALID_WOOD_DECLARATIONS =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate the 'woodDeclaration' against known dataset",
          mav ->
              mav.submitAllMatching(
                  "consignmentItems.*.cargoItems.*.outerPackaging.woodDeclaration"),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.WOOD_DECLARATION_VALUES));

  private static final JsonRebaseableContentCheck VALID_REFERENCE_TYPES =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All reference 'type' fields must be valid",
          ALL_REFERENCE_TYPES,
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.REFERENCE_TYPE));

  static final JsonRebaseableContentCheck VALID_CONSIGMENT_ITEMS_REFERENCE_TYPES =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All consignmentItems reference 'type' fields must be valid",
          mav -> mav.submitAllMatching("consignmentItems.*.references.*.type"),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              EblDatasets.CONSIGNMENT_ITEMS_REFERENCE_TYPE));

  private static final JsonRebaseableContentCheck TLR_CC_T_COMBINATION_UNIQUE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Each combination of 'countryCode'and 'type'can be used at most once.",
          mav -> {
            mav.submitAllMatching("issuingParty.taxLegalReferences");
            mav.submitAllMatching("documentParties.*.party.taxLegalReferences");
          },
          JsonAttribute.unique(COUNTRY_CODE, "type"));

  private static final Consumer<MultiAttributeValidator> DISPLAYED_ADDRESS_MAV_CONSUMER =
      mav -> {
        mav.submitAllMatching("documentParties.shipper.displayedAddress");
        mav.submitAllMatching("documentParties.consignee.displayedAddress");
        mav.submitAllMatching("documentParties.endorsee.displayedAddress");
        mav.submitAllMatching("documentParties.notifyParties.*.displayedAddress");
      };

  private static final JsonRebaseableContentCheck EBL_DISPLAYED_ADDRESS_LIMIT =
      JsonAttribute.ifThen(
          "Validate displayed address length for EBLs. A maximum of 6 lines can be provided for electronic Bills of Lading.",
          td -> td.path("isElectronic").asBoolean(true),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "(not used)", DISPLAYED_ADDRESS_MAV_CONSUMER, JsonAttribute.matchedMaxLength(6)));

  private static final Consumer<MultiAttributeValidator> ALL_UTE = mav -> mav.submitAllMatching("utilizedTransportEquipments.*");

  private static final Predicate<JsonNode> HAS_ISO_EQUIPMENT_CODE = uteNode -> {
    var isoEquipmentNode = uteNode.path("equipment").path("ISOEquipmentCode");
    return isoEquipmentNode.isTextual();
  };

  private static final Predicate<JsonNode> IS_ISO_EQUIPMENT_CONTAINER_REEFER = uteNode -> {
    var isoEquipmentNode = uteNode.path("equipment").path("ISOEquipmentCode");
    return isReeferContainerSizeTypeCode(isoEquipmentNode.asText(""));
  };

  private static final String IS_NON_OPERATING_REEFER = "isNonOperatingReefer";
  private static final String ACTIVE_REEFER_SETTINGS = "activeReeferSettings";
  private static final JsonRebaseableContentCheck ISO_EQUIPMENT_CODE_IMPLIES_REEFER = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate utilizedTransportEquipment and reefer attributes",
    ALL_UTE,
    JsonAttribute.ifMatchedThenElse(
      HAS_ISO_EQUIPMENT_CODE,
      JsonAttribute.ifMatchedThenElse(
        IS_ISO_EQUIPMENT_CONTAINER_REEFER,
        JsonAttribute.path(IS_NON_OPERATING_REEFER, JsonAttribute.matchedMustBePresent()),
        JsonAttribute.combine(
          JsonAttribute.path(IS_NON_OPERATING_REEFER, JsonAttribute.matchedMustBeAbsent()),
          JsonAttribute.path(ACTIVE_REEFER_SETTINGS, JsonAttribute.matchedMustBeAbsent())
        )
      ),
      // If there is no ISOEquipmentCode, then we can only say that `activeReeferSettings` implies
      // `isNonOperatingReefer=False` (the `=False` part is checked elsewhere).
      JsonAttribute.presenceImpliesOtherField(
        ACTIVE_REEFER_SETTINGS,
        IS_NON_OPERATING_REEFER
      )
    )
  );

  private static final JsonRebaseableContentCheck NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All utilizedTransportEquipments where 'isNonOperatingReefers' is 'false' must have 'activeReeferSettings'",
          ALL_UTE,
          JsonAttribute.ifMatchedThen(
              JsonAttribute.isFalse(IS_NON_OPERATING_REEFER),
              JsonAttribute.path(ACTIVE_REEFER_SETTINGS, JsonAttribute.matchedMustBePresent())));

  private static final JsonRebaseableContentCheck NOR_IS_TRUE_IMPLIES_NO_ACTIVE_REEFER =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All utilizedTransportEquipments where 'isNonOperatingReefER' is 'true' cannot have 'activeReeferSettings'",
          ALL_UTE,
          JsonAttribute.ifMatchedThen(
              JsonAttribute.isTrue(IS_NON_OPERATING_REEFER),
              JsonAttribute.path(ACTIVE_REEFER_SETTINGS, JsonAttribute.matchedMustBeAbsent())));

  private static final JsonRebaseableContentCheck CR_CC_T_CODES_UNIQUE = JsonAttribute.allIndividualMatchesMustBeValid(
    "The combination of 'countryCode' and 'type' in '*.customsReferences' must be unique",
    mav -> {
      mav.submitAllMatching(CUSTOMS_REFERENCES);
      mav.submitAllMatching("consignmentItems.*.customsReferences");
      mav.submitAllMatching("consignmentItems.*.cargoItems.*.customsReferences");
      mav.submitAllMatching("utilizedTransportEquipments.*.customsReferences");
    },
    JsonAttribute.unique(COUNTRY_CODE, "type")
  );


  private static final JsonRebaseableContentCheck NATIONAL_COMMODITY_CODE_IS_VALID = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate that 'type' of 'nationalCommodityCodes' is a known code",
    mav -> mav.submitAllMatching("consignmentItems.*.nationalCommodityCodes.*.type"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(NATIONAL_COMMODITY_CODES)
  );

  public static final JsonRebaseableContentCheck  DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE = JsonAttribute.customValidator(
    "Each document party can be used at most once",
    JsonAttribute.path(
      DOCUMENT_PARTIES,
      JsonAttribute.path("other", JsonAttribute.unique("partyFunction"))
    ));


  public static final JsonRebaseableContentCheck VALIDATE_DOCUMENT_PARTIES_MATCH_EBL = JsonAttribute.customValidator(
    "Validate documentParties match the EBL type",
    (body, contextPath) -> {
      var issues = new LinkedHashSet<String>();
      var documentParties = body.path(DOCUMENT_PARTIES);
      var isToOrder = body.path(IS_TO_ORDER).asBoolean(false);

      var isToOrderPath = concatContextPath(contextPath, IS_TO_ORDER);

      if (isToOrder) {
        if (documentParties.has("consignee")) {
          var documentPartiesPath = concatContextPath(contextPath, "documentParties.consignee");
          var endorseePartiesPath = concatContextPath(contextPath, "documentParties.endorsee");
          issues.add("The '%s' party cannot be used when '%s' is true (use '%s' instead)".formatted(documentPartiesPath, isToOrderPath, endorseePartiesPath));
        }
      } else {
        if (!documentParties.has("consignee")) {
          var documentPartiesPath = concatContextPath(contextPath, "documentParties.consignee");
          issues.add("The '%s' party is mandatory when '%s' is false".formatted(documentPartiesPath, isToOrderPath));
        }
        if (documentParties.has("endorsee")) {
          var documentPartiesPath = concatContextPath(contextPath, "documentParties.endorsee");
          issues.add("The '%s' party cannot be used when '%s' is false".formatted(documentPartiesPath, isToOrderPath));
        }
      }
      return issues;
    });

  private static Consumer<MultiAttributeValidator> allDg(Consumer<MultiAttributeValidator.AttributePathBuilder> consumer) {
    return mav -> consumer.accept(mav.path(CONSIGNMENT_ITEMS).all().path("cargoItems").all().path("outerPackaging").path("dangerousGoods").all());
  }

  private static final String EQUIPMENT_REFERENCE = "equipmentReference";
  private static final JsonRebaseableContentCheck CARGO_ITEM_REFERENCES_KNOWN_EQUIPMENT = JsonAttribute.customValidator(
    "Equipment References in 'cargoItems' must be present in 'utilizedTransportEquipments'",
    (body, contextPath) -> {
      var knownEquipmentReferences = allEquipmentReferences(body);
      var missing = new LinkedHashSet<String>();
      for (var consignmentItem : body.path(CONSIGNMENT_ITEMS)) {
        for (var cargoItem : consignmentItem.path("cargoItems")) {
          var ref = cargoItem.path(
            EQUIPMENT_REFERENCE).asText(null);
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
      return missing.stream()
        .map(ref -> "The equipment reference '%s' was used in a cargoItem but was not present in '%s'".formatted(ref, path))
        .collect(Collectors.toSet());
    }
  );

  private static final JsonRebaseableContentCheck UTE_EQUIPMENT_REFERENCE_UNIQUE = JsonAttribute.customValidator(
    "Equipment References in 'utilizedTransportEquipments' must be unique",
    (body, contextPath) -> {
      var duplicates = new LinkedHashSet<String>();
      allEquipmentReferences(body, duplicates);
      var path = concatContextPath(contextPath, UTILIZED_TRANSPORT_EQUIPMENTS);
      return duplicates.stream()
        .map(ref -> "The equipment reference '%s' was used more than once in '%s'".formatted(ref, path))
        .collect(Collectors.toSet());
    }
  );

  private static final JsonRebaseableContentCheck ADVANCED_MANIFEST_FILING_CODES_UNIQUE = JsonAttribute.customValidator(
    "The combination of 'countryCode' and 'manifestTypeCode' in 'advanceManifestFilings' must be unique",
    JsonAttribute.unique(COUNTRY_CODE, MANIFEST_TYPE_CODE)
  );

  static JsonRebaseableContentCheck ENS_MANIFEST_TYPE_REQUIRES_HBL_ISSUED =
      JsonAttribute.ifThen(
          "If any manifestTypeCode in advanceManifestFilings is ENS, isHouseBillOfLadingsIssued is required",
          node -> {
            JsonNode advanceManifestFilings = node.path(ADVANCE_MANIFEST_FILINGS);
            if (advanceManifestFilings.isMissingNode() || !advanceManifestFilings.isArray()) {
              return false;
            }
            for (JsonNode filing : advanceManifestFilings) {
              if ("ENS".equals(filing.path(MANIFEST_TYPE_CODE).asText())) {
                return true;
              }
            }
            return false;
          },
          JsonAttribute.mustBePresent(JsonPointer.compile("/isHouseBillOfLadingsIssued")));

  static final JsonRebaseableContentCheck HBL_NOTIFY_PARTY_REQUIRED_IF_TO_ORDER =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If isToOrder is true in any houseBillOfLading, notifyParty is required in documentParties of that houseBillOfLading",
          mav -> mav.submitAllMatching("houseBillOfLadings.*"),
          (node, contextPath) -> {
            boolean isToOrder = node.path(IS_TO_ORDER).asBoolean(false);
            if (isToOrder && node.path(DOCUMENT_PARTIES).path("notifyParty").isMissingNode()) {
              return Set.of(
                  "If isToOrder is true in any houseBillOfLading, notifyParty is required in documentParties of that houseBillOfLading at %s"
                      .formatted(contextPath));
            }
            return Set.of();
          });

  static final JsonRebaseableContentCheck VALID_HBL_METHOD_OF_PAYMENT =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All houseBillOfLadings methodOfPayment must be valid",
          mav -> mav.submitAllMatching("houseBillOfLadings.*.methodOfPayment"),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.METHOD_OF_PAYMENT));

  private static final JsonRebaseableContentCheck VALIDATE_CARRIER_CODE_AND_LIST_PROVIDER =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If 'carrierCode' is present, 'carrierCodeListProvider' is required and vice versa",
          mav -> {
            mav.submitAllMatching("carrierCode");
            mav.submitAllMatching("carrierCodeListProvider");
          },
          (node, contextPath) -> {
            boolean hasCarrierCode = !node.path("carrierCode").isMissingNode();
            boolean hasProvider = !node.path("carrierCodeListProvider").isMissingNode();

            if (hasCarrierCode && !hasProvider) {
              return Set.of(
                  "'carrierCodeListProvider' is required when 'carrierCode' is present at %s"
                      .formatted(contextPath));
            }
            if (!hasCarrierCode && hasProvider) {
              return Set.of(
                  "'carrierCode' is required when 'carrierCodeListProvider' is present at %s"
                      .formatted(contextPath));
            }
            return Set.of();
          });

  private static final JsonRebaseableContentCheck VALID_TYPE_OF_PERSON =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate 'typeOfPerson' values in documentParties",
          mav -> {
            // single objects
            mav.submitAllMatching("documentParties.shipper.typeOfPerson");
            mav.submitAllMatching("documentParties.consignee.typeOfPerson");
            mav.submitAllMatching("documentParties.seller.typeOfPerson");
            mav.submitAllMatching("documentParties.buyer.typeOfPerson");
            // array of notifyParty objects
            mav.submitAllMatching("documentParties.notifyParties.*.typeOfPerson");
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.TYPE_OF_PERSON));
  private static final Predicate<JsonNode> NUMBER_OF_PACKAGES_REQUIRED =
      packaging -> {
        String packageCode = packaging.path("packageCode").asText(null);
        return packageCode != null && !EXEMPT_PACKAGE_CODES.contains(packageCode);
      };
  static final JsonRebaseableContentCheck NUMBER_OF_PACKAGES_CONDITIONAL_CHECK =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If packageCode in outerPackaging is not exempt, numberOfPackages is required",
          mav ->
              mav.submitAllMatching(
                  "houseBillOfLadings.*.consignmentItems.*.cargoItems.*.outerPackaging"),
          JsonAttribute.ifMatchedThen(
              NUMBER_OF_PACKAGES_REQUIRED,
              JsonAttribute.path("numberOfPackages", JsonAttribute.matchedMustBePresent())));

  private static final Predicate<JsonNode> IDENTIFICATION_NUMBER_REQUIRED =
      filingsNode ->
          "ENS".equals(filingsNode.path(MANIFEST_TYPE_CODE).asText())
              && "SELF"
                  .equals(filingsNode.path(AMF_HBL_PERFORMED_BY).asText());

  static final JsonRebaseableContentCheck IDENTIFICATION_NUMBER_REQUIRED_IF_ENS_AND_SELF =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If manifestTypeCode is ENS and advanceManifestFilingsHouseBLPerformedBy is SELF, identificationNumber is required",
          mav -> mav.submitAllMatching("advanceManifestFilings.*"),
          JsonAttribute.ifMatchedThen(
              IDENTIFICATION_NUMBER_REQUIRED,
              JsonAttribute.path("identificationNumber", JsonAttribute.matchedMustBePresent())));

  private static final Predicate<JsonNode> SELF_FILER_CODE_REQUIRED =
      filingsNode ->
          ("ACI".equals(filingsNode.path(MANIFEST_TYPE_CODE).asText())
                  || "ACE".equals(filingsNode.path(MANIFEST_TYPE_CODE).asText()))
              && "SELF"
                  .equals(filingsNode.path(AMF_HBL_PERFORMED_BY).asText());

  static final JsonRebaseableContentCheck SELF_FILER_CODE_REQUIRED_IF_ACE_ACI_AND_SELF =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If manifestTypeCode is ACE/ACI and advanceManifestFilingsHouseBLPerformedBy is SELF, selfFilerCode is required",
          mav -> mav.submitAllMatching("advanceManifestFilings.*"),
          JsonAttribute.ifMatchedThen(
              SELF_FILER_CODE_REQUIRED,
              JsonAttribute.path("selfFilerCode", JsonAttribute.matchedMustBePresent())));

  private static final Predicate<JsonNode> LOCATION_NAME_REQUIRED =
      place ->
          (place.path("UNLocationCode").isMissingNode()
              && (place.path(LOCATION_NAME).isMissingNode()));

  private static final Predicate<JsonNode> COUNTRY_CODE_REQUIRED =
      place ->
          (place.path("UNLocationCode").isMissingNode()
              && (place.path(COUNTRY_CODE).isMissingNode()));

  static final JsonRebaseableContentCheck LOCATION_NAME_CONDITIONAL_VALIDATION_POA =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If UNLocationCode is not provided in PlaceOfAcceptance, locationName is required",
          mav -> mav.submitAllMatching("houseBillOfLadings.*.placeOfAcceptance"),
          JsonAttribute.ifMatchedThen(
              LOCATION_NAME_REQUIRED,
              JsonAttribute.path(LOCATION_NAME, JsonAttribute.matchedMustBePresent())));

  static final JsonRebaseableContentCheck LOCATION_NAME_CONDITIONAL_VALIDATION_POFD =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If UNLocationCode is not provided in PlaceOfFinalDelivery, locationName is required",
          mav -> mav.submitAllMatching("houseBillOfLadings.*.placeOfFinalDelivery"),
          JsonAttribute.ifMatchedThen(
              LOCATION_NAME_REQUIRED,
              JsonAttribute.path(LOCATION_NAME, JsonAttribute.matchedMustBePresent())));

  static final JsonRebaseableContentCheck COUNTRY_CODE_CONDITIONAL_VALIDATION_POA =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If UNLocationCode is not provided in PlaceOfAcceptance, countryCode is required",
          mav -> mav.submitAllMatching("houseBillOfLadings.*.placeOfAcceptance"),
          JsonAttribute.ifMatchedThen(
              COUNTRY_CODE_REQUIRED,
              JsonAttribute.path(COUNTRY_CODE, JsonAttribute.matchedMustBePresent())));

  static final JsonRebaseableContentCheck COUNTRY_CODE_CONDITIONAL_VALIDATION_POFD =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If UNLocationCode is not provided in PlaceOfFinalDelivery, countryCode is required",
          mav -> mav.submitAllMatching("houseBillOfLadings.*.placeOfFinalDelivery"),
          JsonAttribute.ifMatchedThen(
              COUNTRY_CODE_REQUIRED,
              JsonAttribute.path(COUNTRY_CODE, JsonAttribute.matchedMustBePresent())));

  static final JsonRebaseableContentCheck ROUTING_OF_CONSIGNMENT_COUNTRIES_CHECK =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "If first country in routingOfConsignmentCountries in houseBillOfLadings should be placeOfAcceptance and the last country (if more than one) should be placeOfFinalDelivery",
          mav -> mav.submitAllMatching("houseBillOfLadings.*"),
          (node, contextPath) -> {
            JsonNode routingOfConsignmentCountries = node.path(ROUTING_OF_CONSIGNMENT_COUNTRIES);
            if (routingOfConsignmentCountries.isMissingNode()
                || !routingOfConsignmentCountries.isArray()) {
              return Set.of();
            }
            String placeOfAcceptanceCountry =
                node.path("placeOfAcceptance").path(COUNTRY_CODE).asText(null);
            String placeOfFinalDeliveryCountry =
                node.path("placeOfFinalDelivery").path(COUNTRY_CODE).asText(null);
            if ((placeOfAcceptanceCountry != null
                    && !placeOfAcceptanceCountry.equals(
                        routingOfConsignmentCountries.path(0).asText()))
                || (placeOfFinalDeliveryCountry != null
                    && routingOfConsignmentCountries.size() > 1
                    && !placeOfFinalDeliveryCountry.equals(
                        routingOfConsignmentCountries
                            .path(routingOfConsignmentCountries.size() - 1)
                            .asText()))) {
              return Set.of(
                  "The first country in routingOfConsignmentCountries should be placeOfAcceptance and the last country (if more than one) should be placeOfFinalDelivery at %s"
                      .formatted(contextPath));
            }
            return Set.of();
          });

  static final JsonRebaseableContentCheck BUYER_AND_SELLER_CONDITIONAL_CHECK =
      JsonAttribute.customValidator(
          "If isCargoDeliveredInICS2Zone is true, advanceManifestFilingPerformedBy is 'CARRIER', manifestTypeCode is 'ENS' and isHouseBillOfLadingsIssued is false, then Buyer and Seller is required",
          (node, contextPath) -> {
            JsonNode houseBillOfLadings = node.path(HOUSE_BILL_OF_LADINGS);
            if (houseBillOfLadings.isMissingNode() || !houseBillOfLadings.isArray()) {
              return Set.of();
            }
            JsonNode advanceManifestFilings = node.path(ADVANCE_MANIFEST_FILINGS);
            if (advanceManifestFilings.isMissingNode() || !advanceManifestFilings.isArray()) {
              return Set.of();
            }
            boolean isHouseBlsIssued = node.path("isHouseBillOfLadingsIssued").asBoolean(true);
            int index = 0;
            for (JsonNode hbl : houseBillOfLadings) {
              if (hbl.path("isCargoDeliveredInICS2Zone").asBoolean(false)) {
                for (JsonNode filing : advanceManifestFilings) {
                  if ("CARRIER".equals(filing.path(AMF_HBL_PERFORMED_BY).asText())
                      && "ENS".equals(filing.path(MANIFEST_TYPE_CODE).asText())
                      && !isHouseBlsIssued
                      && (hbl.path(DOCUMENT_PARTIES).path("buyer").isMissingNode()
                          || hbl.path(DOCUMENT_PARTIES).path(SELLER).isMissingNode())) {
                    String specificContextPath =
                        concatContextPath(
                            contextPath, "houseBillOfLadings[" + index + "].documentParties");
                    return Set.of(
                        "Buyer and Seller is required in documentParties in houseBillOfLadings when isCargoDeliveredInICS2Zone is true, advanceManifestFilingPerformedBy is 'CARRIER', manifestTypeCode is 'ENS' and and isHouseBillOfLadingsIssued is false at %s"
                            .formatted(specificContextPath));
                  }
                }
              }
              index++;
            }
            return Set.of();
          });

  static final JsonRebaseableContentCheck SEND_TO_PLATFORM_CONDITIONAL_CHECK =
      JsonAttribute.ifThenElse(
          "'sendToPlatform' is mandatory when 'isElectronic' is true and 'transportDocumentTypeCode' is 'BOL'",
          JsonAttribute.isTrue(JsonPointer.compile("/isElectronic")),
          JsonAttribute.ifThenElse(
              "'transportDocumentTypeCode' is BOL",
              JsonAttribute.isEqualTo("transportDocumentTypeCode", "BOL"),
              JsonAttribute.mustBePresent(SI_REQUEST_SEND_TO_PLATFORM),
              JsonAttribute.mustBeAbsent(SI_REQUEST_SEND_TO_PLATFORM)),
          JsonAttribute.mustBeAbsent(SI_REQUEST_SEND_TO_PLATFORM));

  static final JsonRebaseableContentCheck VALID_PARTY_FUNCTION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The partyFunction in OtherDocumentParty is valid",
          mav -> mav.submitAllMatching("documentParties.other.*.partyFunction"),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(PARTY_FUNCTION_CODE));

  static final JsonRebaseableContentCheck VALID_PARTY_FUNCTION_HBL =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The partyFunction in OtherDocumentParty of houseBillOfLadings is valid",
          mav ->
              mav.submitAllMatching("houseBillOfLadings.*.documentParties.other.*.partyFunction"),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(PARTY_FUNCTION_CODE_HBL));

  static final JsonRebaseableContentCheck VALID_FEEDBACKS_SEVERITY =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate that 'feedback severity' is valid",
          mav -> mav.submitAllMatching("feedbacks.*.severity"),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(FEEDBACKS_SEVERITY));

  static final JsonRebaseableContentCheck VALID_FEEDBACKS_CODE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate that 'feedback code' is valid",
          mav -> mav.submitAllMatching("feedbacks.*.code"),
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
          VALID_CONSIGMENT_ITEMS_REFERENCE_TYPES,
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

  private static final List<JsonRebaseableContentCheck> STATIC_TD_CHECKS =
      Arrays.asList(
          ONLY_EBLS_CAN_BE_NEGOTIABLE,
          EBL_AT_MOST_ONE_ORIGINAL_TOTAL,
          EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES,
          EBLS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES,
          SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES,
          SWBS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES,
          VALIDATE_DOCUMENT_PARTY,
          JsonAttribute.ifThenElse(
              "'isShippedOnBoardType' vs. 'shippedOnBoardDate' or 'receivedForShipmentDate'",
              JsonAttribute.isTrue(JsonPointer.compile("/isShippedOnBoardType")),
              JsonAttribute.mustBePresent(JsonPointer.compile("/shippedOnBoardDate")),
              JsonAttribute.mustBePresent(JsonPointer.compile("/receivedForShipmentDate"))),
          JsonAttribute.atMostOneOf(
              JsonPointer.compile("/shippedOnBoardDate"),
              JsonPointer.compile("/receivedForShipmentDate")),
          JsonAttribute.mustBeDatasetKeywordIfPresent(
              JsonPointer.compile("/cargoMovementTypeAtOrigin"), EblDatasets.CARGO_MOVEMENT_TYPE),
          JsonAttribute.mustBeDatasetKeywordIfPresent(
              JsonPointer.compile("/cargoMovementTypeAtDestination"),
              EblDatasets.CARGO_MOVEMENT_TYPE),
          // receiptTypeAtOrigin + deliveryTypeAtDestination are schema validated
          JsonAttribute.allOrNoneArePresent(
              JsonPointer.compile("/declaredValue"), JsonPointer.compile("/declaredValueCurrency")),
          JsonAttribute.ifThen(
              "Pre-Carriage By implies Place of Receipt",
              JsonAttribute.isNotNull(JsonPointer.compile("/transports/preCarriageBy")),
              JsonAttribute.mustBeNotNull(
                  JsonPointer.compile("/transports/placeOfReceipt"), "'preCarriageBy' is present")),
          JsonAttribute.ifThen(
              "On Carriage By implies Place of Delivery",
              JsonAttribute.isNotNull(JsonPointer.compile("/transports/onCarriageBy")),
              JsonAttribute.mustBeNotNull(
                  JsonPointer.compile("/transports/placeOfDelivery"), "'onCarriageBy' is present")),
          DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS,
          VALID_WOOD_DECLARATIONS,
          NATIONAL_COMMODITY_CODE_IS_VALID,
          VALID_REFERENCE_TYPES,
          VALID_CONSIGMENT_ITEMS_REFERENCE_TYPES,
          ISO_EQUIPMENT_CODE_IMPLIES_REEFER,
          NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER,
          NOR_IS_TRUE_IMPLIES_NO_ACTIVE_REEFER,
          JsonAttribute.allIndividualMatchesMustBeValid(
              "DangerousGoods implies packagingCode or imoPackagingCode",
              mav -> mav.submitAllMatching("consignmentItems.*.cargoItems.*.outerPackaging"),
              (nodeToValidate, contextPath) -> {
                var dg = nodeToValidate.path("dangerousGoods");
                if (!dg.isArray() || dg.isEmpty()) {
                  return Set.of();
                }
                if (nodeToValidate.path("packageCode").isMissingNode()
                    && nodeToValidate.path("imoPackagingCode").isMissingNode()) {
                  return Set.of(
                      "The '%s' object did not have a 'packageCode' nor an 'imoPackagingCode', which is required due to dangerousGoods"
                          .formatted(contextPath));
                }
                return Set.of();
              }),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The 'inhalationZone' values must be from dataset",
              allDg(dg -> dg.path("inhalationZone").submitPath()),
              JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.DG_INHALATION_ZONES)),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The 'segregationGroups' values must be from dataset",
              allDg(dg -> dg.path("segregationGroups").all().submitPath()),
              JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                  EblDatasets.DG_SEGREGATION_GROUPS)),
          UTE_EQUIPMENT_REFERENCE_UNIQUE,
          JsonAttribute.allIndividualMatchesMustBeValid(
              "If 'temperatureSetpoint' is present, 'temperatureUnit' must be present (and vice versa)",
              mav -> mav.submitAllMatching("utilizedTransportEquipments.*.activeReeferSettings"),
              JsonAttribute.presenceImpliesOtherField("temperatureSetpoint", "temperatureUnit")),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "If 'temperatureUnit' is present, 'temperatureSetpoint' must be present",
              mav -> mav.submitAllMatching("utilizedTransportEquipments.*.activeReeferSettings"),
              JsonAttribute.presenceImpliesOtherField("temperatureUnit", "temperatureSetpoint")),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The 'airExchangeSetpoint' implies 'airExchangeUnit'",
              mav -> mav.submitAllMatching("utilizedTransportEquipments.*.activeReeferSettings"),
              JsonAttribute.presenceImpliesOtherField("airExchangeSetpoint", "airExchangeUnit")),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "If 'airExchangeUnit' is present, 'airExchangeSetpoint' must be present",
              mav -> mav.submitAllMatching("utilizedTransportEquipments.*.activeReeferSettings"),
              JsonAttribute.presenceImpliesOtherField("airExchangeUnit", "airExchangeSetpoint")),
          EBL_DISPLAYED_ADDRESS_LIMIT,
          CARGO_ITEM_REFERENCES_KNOWN_EQUIPMENT,
          ADVANCED_MANIFEST_FILING_CODES_UNIQUE,
          CR_CC_T_CODES_UNIQUE,
          JsonAttribute.allIndividualMatchesMustBeValid(
              "Validate mode of transport type",
              mav -> {
                mav.submitAllMatching("transports.preCarriageBy");
                mav.submitAllMatching("transports.onCarriageBy");
              },
              JsonAttribute.matchedMustBeDatasetKeywordIfPresent(MODE_OF_TRANSPORT)),
          NOTIFY_PARTIES_REQUIRED_IN_NEGOTIABLE_BLS,
          TLR_CC_T_COMBINATION_UNIQUE,
          VALID_PARTY_FUNCTION,
          VALID_TYPE_OF_PERSON);

  public static final JsonContentCheck SIR_OR_TDR_REQUIRED_IN_NOTIFICATION =
      JsonAttribute.atLeastOneOf(SI_REF_SIR_PTR, TD_REF_TDR_PTR);

  public static JsonContentCheck sirInNotificationMustMatchDSP(Supplier<EblDynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.mustEqual(
        SI_REF_SIR_PTR, () -> dspSupplier.get().shippingInstructionsReference());
  }

  public static JsonContentCheck tdrInNotificationMustMatchDSP(Supplier<EblDynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.mustEqual(
        TD_REF_TDR_PTR, () -> dspSupplier.get().transportDocumentReference());
  }

  public static List<JsonContentCheck> generateScenarioRelatedChecks(
      ScenarioType scenarioType, boolean isTD, EblDynamicScenarioParameters dsp) {
    List<JsonContentCheck> checks = new ArrayList<>();
    checks.add(
        JsonAttribute.mustEqual(
            "[Scenario] Verify that the correct 'transportDocumentTypeCode' is used",
            "transportDocumentTypeCode",
            scenarioType::transportDocumentTypeCode));
    if (isTD) {
      checks.add(
          JsonAttribute.ifThen(
              "[Scenario] Verify that the transportDocument included 'carriersAgentAtDestination'",
              ignored -> {
                return dsp.isCladInSI() || scenarioType.isCarriersAgentAtDestinationRequired();
              },
              JsonAttribute.path(
                  DOCUMENT_PARTIES,
                  JsonAttribute.path(
                      "carriersAgentAtDestination", JsonAttribute.matchedMustBePresent()))));

    } else {
      checks.add(
        JsonAttribute.ifThen(
          "[Scenario] Verify that the shippingInstructions had 'isCarriersAgentAtDestinationRequired' as true if scenario requires it",
          ignored -> scenarioType.isCarriersAgentAtDestinationRequired(),
          JsonAttribute.path("isCarriersAgentAtDestinationRequired", JsonAttribute.matchedMustBeTrue())
        ));

      checks.add(
          JsonAttribute.allIndividualMatchesMustBeValid(
              "[Scenario] Non-DG: outerPackaging must be present in the SI",
              mav -> mav.submitAllMatching("consignmentItems.*.cargoItems.*"),
              JsonAttribute.ifMatchedThen(
                  ignored -> !scenarioType.hasDG(),
                  JsonAttribute.path("outerPackaging", JsonAttribute.matchedMustBePresent()))));
    }

    checks.add(JsonAttribute.customValidator(
      "[Scenario] Verify that the scenario contained references when the scenario requires it",
      scenarioReferencesCheck(scenarioType)
    ));

    checks.add(JsonAttribute.customValidator(
      "[Scenario] Verify that 'customsReferences' is used when the scenario requires it",
      scenarioCustomsReferencesCheck(scenarioType)
    ));

    checks.add(
        JsonAttribute.customValidator(
            "[Scenario] Verify that the scenario contains the required amount of 'utilizedTransportEquipments'",
            utilizedTransportEquipmentsScenarioSizeCheck(scenarioType)));

    checks.add(
        JsonAttribute.customValidator(
            "[Scenario] Verify that the scenario contains the required amount of 'consignmentItems'",
            consignmentItemsScenarioSizeCheck(scenarioType)));

    return checks;
  }

  private static JsonContentMatchedValidation scenarioCustomsReferencesCheck(ScenarioType scenarioType) {
    return (nodeToValidate,contextPath) -> {
      if (!scenarioType.isCustomsReferencesRequired()) {
        return Set.of();
      }
      var allReferencesParents = nodeToValidate.findParents(CUSTOMS_REFERENCES);
      for (var referencesParent : allReferencesParents) {
        if (isNonEmptyNode(referencesParent.path(CUSTOMS_REFERENCES))) {
          return Set.of();
        }
      }
      return Set.of("Expected 'customsReferences' to be used somewhere.");
    };
  }

  private static final JsonPointer[] REFERENCE_PATHS = {
    JsonPointer.compile("/references"),
    JsonPointer.compile("/documentParties/shipper/reference"),
    JsonPointer.compile("/documentParties/shipper/reference"),
    JsonPointer.compile("/documentParties/consignee/purchaseOrderReference"),
    JsonPointer.compile("/documentParties/consignee/reference")
  };

  private static JsonContentMatchedValidation scenarioReferencesCheck(ScenarioType scenarioType) {
    return JsonAttribute.ifMatchedThen(
      ignored -> scenarioType.isReferencesRequired(),
      JsonAttribute.atLeastOneOfMatched((body, ptrs) -> {
        ptrs.addAll(Arrays.asList(REFERENCE_PATHS));
        var uteCount = body.path(UTILIZED_TRANSPORT_EQUIPMENTS).size();
        for (int i = 0 ; i < uteCount ; i++) {
          ptrs.add(JsonPointer.compile("/utilizedTransportEquipments/%d/references".formatted(i)));
        }
        var ciCount = body.path(CONSIGNMENT_ITEMS).size();
        for (int i = 0 ; i < ciCount ; i++) {
          ptrs.add(JsonPointer.compile("/consignmentItems/%d/references".formatted(i)));
        }
        var notifyPartyCount = body.path(DOCUMENT_PARTIES).path("notifyParties").size();
        for (int i = 0 ; i < notifyPartyCount ; i++) {
          ptrs.add(JsonPointer.compile("/documentParties/notifyParties/%d/party/reference".formatted(i)));
        }
        var otherPartyCount = body.path(DOCUMENT_PARTIES).path("other").size();
        for (int i = 0 ; i < otherPartyCount ; i++) {
          ptrs.add(JsonPointer.compile("/documentParties/other/%d/party/reference".formatted(i)));
        }
      })
    );
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
    checks.addAll(generateScenarioRelatedChecks(scenarioType, false, null));
    return JsonAttribute.contentChecks(
      EblRole::isShipper,
      matched,
      HttpMessageType.REQUEST,
      standardVersion,
      checks
    );
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
            ScenarioType.valueOf(dspSupplier.get().scenarioType()), false, null));
    return checks;
  }

  private static JsonRebaseableContentCheck getUpdatedShippingInstructionsStatusCheck(
      ShippingInstructionsStatus updatedShippingInstructionsStatus) {
    return updatedShippingInstructionsStatus != null
        ? JsonAttribute.mustEqual(
            SI_REF_UPDATED_SI_STATUS_PTR, updatedShippingInstructionsStatus.wireName())
        : JsonAttribute.mustBeAbsent(SI_REF_UPDATED_SI_STATUS_PTR);
  }

  static final JsonContentCheck FEEDBACKS_PRESENCE =
      JsonAttribute.customValidator(
          "Feedbacks must be present for the selected shipping instructions status ",
          body -> {
            var siStatus = body.path("shippingInstructionsStatus").asText("");
            var updatedSiStatus = body.path("updatedShippingInstructionsStatus").asText("");
            var issues = new LinkedHashSet<String>();
            if (SI_PENDING_UPDATE.wireName().equals(siStatus) && updatedSiStatus.isEmpty()) {
              var feedbacks = body.get("feedbacks");
              if (feedbacks == null || feedbacks.isEmpty()) {
                issues.add(
                    "feedbacks is missing for the si in status %s"
                        .formatted(SI_PENDING_UPDATE.wireName()));
              }
            }
            return issues;
          });

  public static ActionCheck tdRefStatusChecks(UUID matched, String standardVersion, Supplier<EblDynamicScenarioParameters> dspSupplier, TransportDocumentStatus transportDocumentStatus) {
    return JsonAttribute.contentChecks(
      EblRole::isCarrier,
      matched,
      HttpMessageType.RESPONSE,
      standardVersion,
      JsonAttribute.mustEqual(
        TD_REF_TDR_PTR,
        () -> dspSupplier.get().transportDocumentReference()
      ),
      JsonAttribute.mustEqual(
        TD_REF_TD_STATUS_PTR,
        transportDocumentStatus.wireName()
      )
    );
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
      List<? super JsonRebaseableContentCheck> jsonContentChecks,
      Supplier<String> tdrSupplier,
      TransportDocumentStatus transportDocumentStatus) {
    if (tdrSupplier != null) {
      jsonContentChecks.add(JsonAttribute.mustEqual(TD_TDR, tdrSupplier));
    }
    jsonContentChecks.add(JsonAttribute.mustEqual(
      TD_TRANSPORT_DOCUMENT_STATUS,
      transportDocumentStatus.wireName()
    ));
    jsonContentChecks.addAll(STATIC_TD_CHECKS);
    jsonContentChecks.add(DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE);
    jsonContentChecks.add(VALIDATE_DOCUMENT_PARTIES_MATCH_EBL);
  }

  public static List<JsonRebaseableContentCheck> genericTDContentChecks(
      TransportDocumentStatus transportDocumentStatus, Supplier<String> tdrReferenceSupplier) {
    List<JsonRebaseableContentCheck> jsonContentChecks = new ArrayList<>();
    genericTdContentChecks(
      jsonContentChecks,
      tdrReferenceSupplier,
      transportDocumentStatus
    );
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
            "[Scenario] Validate the containers reefer settings",
            mav -> mav.submitAllMatching("utilizedTransportEquipments.*"),
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
                        "The scenario requires '%s.isNonOperatingReefer' to be true"
                            .formatted(contextPath));
                  }
                }
              }
              return issues;
            }));
    jsonContentChecks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "[Scenario] Whether the cargo should be DG",
            mav ->
                mav.submitAllMatching(
                    "consignmentItems.*.cargoItems.*.outerPackaging.*.dangerousGoods"),
            (nodeToValidate, contextPath) -> {
              if (ScenarioType.valueOf(dspSupplier.get().scenarioType()) == ScenarioType.DG) {
                if (!nodeToValidate.isArray() || nodeToValidate.isEmpty()) {
                  return Set.of(
                      "The scenario requires '%s' to contain dangerous goods"
                          .formatted(contextPath));
                }
              }
              return Set.of();
            }));
    jsonContentChecks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "[Scenario] The 'isShipperOwned' should be 'true for SOC scenarios",
            mav -> mav.submitAllMatching("utilizedTransportEquipments.*"),
            (nodeToValidate, contextPath) -> {
              var scenario = ScenarioType.valueOf(dspSupplier.get().scenarioType());
              if (scenario == ScenarioType.REGULAR_SWB_SOC_AND_REFERENCES) {
                if (!nodeToValidate.path("isShipperOwned").asBoolean(false)) {
                  return Set.of(
                      "The scenario requires '%s.isShipperOwned' to be true"
                          .formatted(contextPath));
                }
              }
              return Set.of();
            }));
    jsonContentChecks.addAll(
        generateScenarioRelatedChecks(
            ScenarioType.valueOf(dspSupplier.get().scenarioType()), true, dspSupplier.get()));
    return jsonContentChecks;
  }

  public static JsonContentMatchedValidation utilizedTransportEquipmentsScenarioSizeCheck(
      ScenarioType scenarioType) {
    return (body, contextPath) -> {
      var utilizedTransportEquipments = body.path(UTILIZED_TRANSPORT_EQUIPMENTS);
      int actualSize = utilizedTransportEquipments.size();

      Integer expectedSize =
          switch (scenarioType) {
            case ScenarioType.REGULAR_2C_1U -> 1;
            case ScenarioType.REGULAR_2C_2U -> 2;
            default -> null;
          };

      if (expectedSize != null && actualSize != expectedSize) {
        String path = concatContextPath(contextPath, UTILIZED_TRANSPORT_EQUIPMENTS);
        return Set.of(
            "The scenario requires exactly %d 'utilizedTransportEquipments' but found %d at %s"
                .formatted(expectedSize, actualSize, path));
      }

      return Set.of();
    };
  }

  public static JsonContentMatchedValidation consignmentItemsScenarioSizeCheck(
      ScenarioType scenarioType) {
    return (body, contextPath) -> {
      var scenario = scenarioType;
      var consignmentItems = body.path(CONSIGNMENT_ITEMS);
      int actualSize = consignmentItems.size();

      Integer expectedSize =
          switch (scenario) {
            case ScenarioType.REGULAR_2C_1U, ScenarioType.REGULAR_2C_2U -> 2;
            default -> null;
          };

      if (expectedSize != null && actualSize != expectedSize) {
        String path = concatContextPath(contextPath, CONSIGNMENT_ITEMS);
        return Set.of(
            "The scenario requires exactly %d 'consignemntItems' but found %d at %s"
                .formatted(expectedSize, actualSize, path));
      }

      return Set.of();
    };
  }

  private static Set<String> allEquipmentReferences(JsonNode body) {
    return allEquipmentReferences(body, null);
  }


  private static Set<String> allEquipmentReferences(JsonNode body, Set<String> duplicates) {
    var seen = new HashSet<String>();
    for (var ute : body.path(UTILIZED_TRANSPORT_EQUIPMENTS)) {
      // TD or SI with SOC
      var ref = ute.path("equipment").path(EQUIPMENT_REFERENCE).asText(null);
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
