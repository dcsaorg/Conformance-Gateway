package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.core.check.JsonAttribute.concatContextPath;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.DOCUMENTATION_PARTY_CODE_LIST_PROVIDER_CODES;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.MODE_OF_TRANSPORT;
import static org.dcsa.conformance.standards.ebl.checks.EblDatasets.NATIONAL_COMMODITY_CODES;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.models.TriConsumer;
import org.dcsa.conformance.standards.ebl.party.*;

@UtilityClass
public class EBLChecks {

  private static final JsonPointer SI_REF_SIR_PTR = JsonPointer.compile("/shippingInstructionsReference");
  private static final JsonPointer SI_REF_SI_STATUS_PTR = JsonPointer.compile("/shippingInstructionsStatus");
  private static final JsonPointer SI_REF_UPDATED_SI_STATUS_PTR = JsonPointer.compile("/updatedShippingInstructionsStatus");
  private static final JsonPointer SI_NOTIFICATION_SIR_PTR = JsonPointer.compile("/data/shippingInstructionsReference");
  private static final JsonPointer SI_NOTIFICATION_SI_STATUS_PTR = JsonPointer.compile("/data/shippingInstructionsStatus");
  private static final JsonPointer SI_NOTIFICATION_UPDATED_SI_STATUS_PTR = JsonPointer.compile("/data/updatedShippingInstructionsStatus");

  private static final JsonPointer TD_REF_TDR_PTR = JsonPointer.compile("/transportDocumentReference");
  private static final JsonPointer TD_REF_TD_STATUS_PTR = JsonPointer.compile("/transportDocumentStatus");
  private static final JsonPointer TD_NOTIFICATION_TDR_PTR = JsonPointer.compile("/data/transportDocumentReference");
  private static final JsonPointer TD_NOTIFICATION_TD_STATUS_PTR = JsonPointer.compile("/data/transportDocumentStatus");

  private static final JsonPointer SI_REQUEST_INVOICE_PAYABLE_AT_UN_LOCATION_CODE = JsonPointer.compile("/invoicePayableAt/UNLocationCode");
  private static final JsonPointer SI_REQUEST_SEND_TO_PLATFORM = JsonPointer.compile("/documentParties/issueTo/sendToPlatform");
  private static final JsonPointer ISSUE_TO_PARTY = JsonPointer.compile("/documentParties/issueTo");

  private static final JsonPointer TD_TDR = JsonPointer.compile("/transportDocumentReference");
  private static final JsonPointer TD_TRANSPORT_DOCUMENT_STATUS = JsonPointer.compile("/transportDocumentStatus");

  private static final String CONSIGNMENT_ITEMS = "consignmentItems";
  private static final String UTILIZED_TRANSPORT_EQUIPMENTS = "utilizedTransportEquipments";
  private static final String DOCUMENT_PARTIES = "documentParties";
  private static final String CUSTOMS_REFERENCES = "customsReferences";


  private static final BiConsumer<JsonNode, TriConsumer<JsonNode, String, ArrayOrderHandler>> DOC_PARTY_ARRAY_ORDER_DEFINITIONS =
    (documentPartyNode, arrayNodeHandler) -> {
      arrayNodeHandler.accept(
        documentPartyNode, "displayedAddress", ArrayOrderHandler.inputPreservedArrayOrder());
      arrayNodeHandler.accept(
        documentPartyNode, "identifyingCodes", ArrayOrderHandler.toStringSortableArray());
      arrayNodeHandler.accept(
        documentPartyNode, "taxLegalReferences", ArrayOrderHandler.toStringSortableArray());
      arrayNodeHandler.accept(
        documentPartyNode, "partyContactDetails", ArrayOrderHandler.toStringSortableArray());
    };

  private static final BiConsumer<JsonNode, TriConsumer<JsonNode, String, ArrayOrderHandler>> DOC_PARTIES_ARRAY_ORDER_DEFINITIONS =
    (documentPartyNode, arrayNodeHandler) -> {
      for (var partyName : List.of("shipper", "consignee", "notifyParty", "seller", "buyer", "endorsee", "issueTo")) {
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

  public static final BiConsumer<JsonNode, TriConsumer<JsonNode, String, ArrayOrderHandler>>
      SI_ARRAY_ORDER_DEFINITIONS =
          (rootNode, arrayNodeHandler) -> {
            arrayNodeHandler.accept(
              rootNode, "partyContactDetails", ArrayOrderHandler.inputPreservedArrayOrder());
            arrayNodeHandler.accept(
              rootNode, "routingOfConsignmentCountries", ArrayOrderHandler.inputPreservedArrayOrder());

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
                rootNode, "advanceManifestFilings", ArrayOrderHandler.toStringSortableArray());
            arrayNodeHandler.accept(
                rootNode, "references", ArrayOrderHandler.toStringSortableArray());
            for (var cr : rootNode.path(CUSTOMS_REFERENCES)) {
              arrayNodeHandler.accept(cr, "values", ArrayOrderHandler.toStringSortableArray());
            }
            arrayNodeHandler.accept(
              rootNode, CUSTOMS_REFERENCES, ArrayOrderHandler.toStringSortableArray());

            DOC_PARTIES_ARRAY_ORDER_DEFINITIONS.accept(rootNode.path(DOCUMENT_PARTIES), arrayNodeHandler);

            for (var hbl : rootNode.path("houseBillOfLadings")) {
              DOC_PARTIES_ARRAY_ORDER_DEFINITIONS.accept(hbl.path(DOCUMENT_PARTIES), arrayNodeHandler);
            }
            arrayNodeHandler.accept(
              rootNode, "houseBillOfLadings", ArrayOrderHandler.toStringSortableArray());
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

  public static final Predicate<JsonNode> IS_ELECTRONIC = td -> td.path("isElectronic").asBoolean(false);

  public static final Predicate<JsonNode> IS_AN_EBL = IS_ELECTRONIC.and(td -> td.path("transportDocumentTypeCode").asText("").equals("BOL"));
  public static final Predicate<JsonNode> IS_AN_ESWB = IS_ELECTRONIC.and(td -> td.path("transportDocumentTypeCode").asText("").equals("SWB"));

  private static final JsonRebaseableContentCheck EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES = JsonAttribute.ifThen(
    "EBLs cannot have copies with charges",
    IS_AN_EBL,
    JsonAttribute.path("numberOfCopiesWithCharges", JsonAttribute.matchedMustBeAbsent())
  );

  private static final JsonRebaseableContentCheck EBLS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES = JsonAttribute.ifThen(
    "EBLs cannot have copies without charges",
    IS_AN_EBL,
    JsonAttribute.path("numberOfOriginalsWithoutCharges", JsonAttribute.matchedMustBeAbsent())
  );

  private static final JsonRebaseableContentCheck E_SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES = JsonAttribute.ifThen(
    "Electronic SWBs cannot have originals with charges",
    IS_AN_ESWB,
    JsonAttribute.path("numberOfOriginalsWithCharges", JsonAttribute.matchedMustBeAbsent())
  );

  private static final JsonRebaseableContentCheck E_SWBS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES = JsonAttribute.ifThen(
    "Electronic SWBs cannot have originals without charges",
    IS_AN_ESWB,
    JsonAttribute.path("numberOfCopiesWithoutCharges", JsonAttribute.matchedMustBeAbsent())
  );

  private static final JsonRebaseableContentCheck EBL_AT_MOST_ONE_COPY_WITHOUT_CHARGES = JsonAttribute.ifThen(
    "Cannot have more than one copy without charges when isElectronic",
    IS_ELECTRONIC,
    JsonAttribute.path("numberOfCopiesWithoutCharges", JsonAttribute.matchedMaximum(1))
  );

  private static final JsonRebaseableContentCheck EBL_AT_MOST_ONE_COPY_WITH_CHARGES = JsonAttribute.ifThen(
    "Cannot have more than one copy with charges when isElectronic",
    IS_ELECTRONIC,
    JsonAttribute.path("numberOfCopiesWithCharges", JsonAttribute.matchedMaximum(1))
  );

  private static final JsonRebaseableContentCheck EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES = JsonAttribute.ifThen(
    "Cannot have more than one original without charges when isElectronic",
    IS_ELECTRONIC,
    JsonAttribute.path("numberOfOriginalsWithoutCharges", JsonAttribute.matchedMaximum(1))
  );

  private static final JsonRebaseableContentCheck EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES = JsonAttribute.ifThen(
    "Cannot have more than one original without charges when isElectronic",
    IS_ELECTRONIC,
    JsonAttribute.path("numberOfOriginalsWithCharges", JsonAttribute.matchedMaximum(1))
  );

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

  private static final JsonRebaseableContentCheck NOTIFY_PARTIES_REQUIRED_IN_NEGOTIABLE_BLS = JsonAttribute.ifThen(
    "The 'documentParties.notifyParties' attribute is mandatory for negotiable B/Ls",
    JsonAttribute.isTrue("isToOrder"),
    JsonAttribute.at(JsonPointer.compile("/documentParties/notifyParties"), JsonAttribute.matchedMustBeNonEmpty())
  );

  private static final Consumer<MultiAttributeValidator> ALL_REFERENCE_TYPES = mav -> {
    mav.submitAllMatching("references.*.type");
    mav.submitAllMatching("utilizedTransportEquipments.*.references.*.type");
    mav.submitAllMatching("consignmentItems.*.references.*.type");
  };

  public static final JsonRebaseableContentCheck VALID_WOOD_DECLARATIONS = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate the 'woodDeclaration' against known dataset",
    mav -> mav.submitAllMatching("consignmentItems.*.cargoItems.*.outerPackaging.woodDeclaration"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.WOOD_DECLARATION_VALUES)
  );

  private static final JsonRebaseableContentCheck VALID_REFERENCE_TYPES = JsonAttribute.allIndividualMatchesMustBeValid(
    "All reference 'type' fields must be valid",
        ALL_REFERENCE_TYPES,
        JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.REFERENCE_TYPE)
  );


  private static final JsonRebaseableContentCheck TLR_CC_T_COMBINATION_UNIQUE = JsonAttribute.allIndividualMatchesMustBeValid(
    "Each document party can be used at most once",
    mav -> {
      mav.submitAllMatching("issuingParty.taxLegalReferences");
      mav.submitAllMatching("documentParties.*.party.taxLegalReferences");
    },
    JsonAttribute.unique("countryCode", "type")
  );



  private static final JsonRebaseableContentCheck EBL_DISPLAYED_ADDRESS_LIMIT = JsonAttribute.ifThen(
    "Validate displayed address length for EBLs",
      td -> td.path("isElectronic").asBoolean(false),
      JsonAttribute.allIndividualMatchesMustBeValid(
      "(not used)",
      mav -> {
        mav.submitAllMatching("documentParties.shipper.displayedAddress");
        mav.submitAllMatching("documentParties.consignee.displayedAddress");
        mav.submitAllMatching("documentParties.endorsee.displayedAddress");
        mav.submitAllMatching("documentParties.notifyParties.*.displayedAddress");
      },
      JsonAttribute.matchedMaxLength(2)
    )
  );

  private static final Consumer<MultiAttributeValidator> ALL_UTE = mav -> mav.submitAllMatching("utilizedTransportEquipments.*");

  private static final Predicate<JsonNode> HAS_ISO_EQUIPMENT_CODE = uteNode -> {
    var isoEquipmentNode = uteNode.path("equipment").path("ISOEquipmentCode");
    return isoEquipmentNode.isTextual();
  };

  private static final Predicate<JsonNode> IS_ISO_EQUIPMENT_CONTAINER_REEFER = uteNode -> {
    var isoEquipmentNode = uteNode.path("equipment").path("ISOEquipmentCode");
    return isReeferContainerSizeTypeCode(isoEquipmentNode.asText(""));
  };

  public static final String IS_NON_OPERATING_REEFER = "isNonOperatingReefer";
  public static final String ACTIVE_REEFER_SETTINGS = "activeReeferSettings";
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

  private static final JsonRebaseableContentCheck NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER = JsonAttribute.allIndividualMatchesMustBeValid(
    "All utilizedTransportEquipments where 'isNonOperatingReefer' is 'false' must have 'activeReeferSettings'",
    ALL_UTE,
    JsonAttribute.ifMatchedThen(
      JsonAttribute.isFalse(IS_NON_OPERATING_REEFER),
      JsonAttribute.path(ACTIVE_REEFER_SETTINGS, JsonAttribute.matchedMustBePresent())
    )
  );

  private static final JsonRebaseableContentCheck NOR_IS_TRUE_IMPLIES_NO_ACTIVE_REEFER = JsonAttribute.allIndividualMatchesMustBeValid(
    "All utilizedTransportEquipments where 'isNonOperatingReefer' is 'true' cannot have 'activeReeferSettings'",
    ALL_UTE,
    JsonAttribute.ifMatchedThen(
      JsonAttribute.isTrue(IS_NON_OPERATING_REEFER),
      JsonAttribute.path(ACTIVE_REEFER_SETTINGS, JsonAttribute.matchedMustBeAbsent())
    )
  );


  private static final JsonRebaseableContentCheck CR_CC_T_CODES_UNIQUE = JsonAttribute.allIndividualMatchesMustBeValid(
    "The combination of 'countryCode' and 'type' in '*.customsReferences' must be unique",
    mav -> {
      mav.submitAllMatching(CUSTOMS_REFERENCES);
      mav.submitAllMatching("consignmentItems.*.customsReferences");
      mav.submitAllMatching("consignmentItems.*.cargoItems.*.customsReferences");
      mav.submitAllMatching("utilizedTransportEquipments.*.customsReferences");
    },
    JsonAttribute.unique("countryCode", "type")
  );


  private static final JsonRebaseableContentCheck NATIONAL_COMMODITY_CODE_IS_VALID = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate that 'type' of 'nationalCommodityCodes' is a known code",
    mav -> mav.submitAllMatching("consignmentItems.*.nationalCommodityCodes.*.type"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(NATIONAL_COMMODITY_CODES)
  );


  private static final JsonRebaseableContentCheck DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE = JsonAttribute.customValidator(
    "Each document party can be used at most once",
    JsonAttribute.path(
      DOCUMENT_PARTIES,
      JsonAttribute.path("other", JsonAttribute.unique("partyFunction"))
    ));


  private static final JsonRebaseableContentCheck VALIDATE_DOCUMENT_PARTIES_MATCH_EBL = JsonAttribute.customValidator(
    "Validate documentParties match the EBL type",
    (body, contextPath) -> {
      var issues = new LinkedHashSet<String>();
      var documentParties = body.path(DOCUMENT_PARTIES);
      var isToOrder = body.path("isToOrder").asBoolean(false);

      if (!documentParties.has("shipper")) {
        var documentPartiesPath = concatContextPath(contextPath, "documentParties.shipper");
        issues.add("The '%s' party is mandatory in the eBL phase (SI/TD)".formatted(documentPartiesPath));
      }
      var isToOrderPath = concatContextPath(contextPath, "isToOrder");

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

  public static final String EQUIPMENT_REFERENCE = "equipmentReference";
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
    JsonAttribute.unique("countryCode", "manifestTypeCode")
  );

  private static final List<JsonContentCheck> STATIC_SI_CHECKS = Arrays.asList(
    JsonAttribute.mustBeDatasetKeywordIfPresent(
      SI_REQUEST_SEND_TO_PLATFORM,
      EblDatasets.EBL_PLATFORMS_DATASET
    ),
    ONLY_EBLS_CAN_BE_NEGOTIABLE,
    EBL_AT_MOST_ONE_COPY_WITHOUT_CHARGES,
    EBL_AT_MOST_ONE_COPY_WITH_CHARGES,
    EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES,
    EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES,
    EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES,
    EBLS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES,
    E_SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES,
    E_SWBS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES,
    JsonAttribute.ifThenElse(
      "'isElectronic' implies 'issueTo' party",
      JsonAttribute.isTrue(JsonPointer.compile("/isElectronic")),
      JsonAttribute.mustBePresent(ISSUE_TO_PARTY),
      JsonAttribute.mustBeAbsent(ISSUE_TO_PARTY)
    ),
    DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS,
    VALID_WOOD_DECLARATIONS,
    NATIONAL_COMMODITY_CODE_IS_VALID,
    VALID_REFERENCE_TYPES,
    ISO_EQUIPMENT_CODE_IMPLIES_REEFER,
    UTE_EQUIPMENT_REFERENCE_UNIQUE,
    EBL_DISPLAYED_ADDRESS_LIMIT,
    CARGO_ITEM_REFERENCES_KNOWN_EQUIPMENT,
    ADVANCED_MANIFEST_FILING_CODES_UNIQUE,
    CR_CC_T_CODES_UNIQUE,
    NOTIFY_PARTIES_REQUIRED_IN_NEGOTIABLE_BLS,
    TLR_CC_T_COMBINATION_UNIQUE
  );

  private static final List<JsonRebaseableContentCheck> STATIC_TD_CHECKS = Arrays.asList(
    ONLY_EBLS_CAN_BE_NEGOTIABLE,
    EBL_AT_MOST_ONE_COPY_WITHOUT_CHARGES,
    EBL_AT_MOST_ONE_COPY_WITH_CHARGES,
    EBL_AT_MOST_ONE_ORIGINAL_WITHOUT_CHARGES,
    EBL_AT_MOST_ONE_ORIGINAL_WITH_CHARGES,
    EBLS_CANNOT_HAVE_COPIES_WITH_CHARGES,
    EBLS_CANNOT_HAVE_ORIGINALS_WITHOUT_CHARGES,
    E_SWBS_CANNOT_HAVE_ORIGINALS_WITH_CHARGES,
    E_SWBS_CANNOT_HAVE_COPIES_WITHOUT_CHARGES,
    JsonAttribute.ifThenElse(
      "'isShippedOnBoardType' vs. 'shippedOnBoardDate' or 'receivedForShipmentDate'",
      JsonAttribute.isTrue(JsonPointer.compile("/isShippedOnBoardType")),
      JsonAttribute.mustBePresent(JsonPointer.compile("/shippedOnBoardDate")),
      JsonAttribute.mustBePresent(JsonPointer.compile("/receivedForShipmentDate"))
    ),
    JsonAttribute.atMostOneOf(
      JsonPointer.compile("/shippedOnBoardDate"),
      JsonPointer.compile("/receivedForShipmentDate")
    ),
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/cargoMovementTypeAtOrigin"), EblDatasets.CARGO_MOVEMENT_TYPE),
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/cargoMovementTypeAtDestination"), EblDatasets.CARGO_MOVEMENT_TYPE),
    // receiptTypeAtOrigin + deliveryTypeAtDestination are schema validated
    JsonAttribute.allOrNoneArePresent(
      JsonPointer.compile("/declaredValue"),
      JsonPointer.compile("/declaredValueCurrency")
    ),
    JsonAttribute.ifThen(
      "Pre-Carriage By implies Place of Receipt",
      JsonAttribute.isNotNull(JsonPointer.compile("/transports/preCarriageBy")),
      JsonAttribute.mustBeNotNull(JsonPointer.compile("/transports/placeOfReceipt"), "'preCarriageBy' is present")
    ),
    JsonAttribute.ifThen(
      "On Carriage By implies Place of Delivery",
      JsonAttribute.isNotNull(JsonPointer.compile("/transports/onCarriageBy")),
      JsonAttribute.mustBeNotNull(JsonPointer.compile("/transports/placeOfDelivery"), "'onCarriageBy' is present")
    ),
    DOCUMENTATION_PARTIES_CODE_LIST_PROVIDERS,
    VALID_WOOD_DECLARATIONS,
    NATIONAL_COMMODITY_CODE_IS_VALID,
    VALID_REFERENCE_TYPES,
    ISO_EQUIPMENT_CODE_IMPLIES_REEFER,
    NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER,
    NOR_IS_TRUE_IMPLIES_NO_ACTIVE_REEFER,
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'commoditySubreference' must not be present in the transport document",
      mav -> mav.submitAllMatching("consignmentItems.*.commoditySubreference"),
      JsonAttribute.matchedMustBeAbsent()
    ),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "DangerousGoods implies packagingCode or imoPackagingCode",
      mav -> mav.submitAllMatching("consignmentItems.*.cargoItems.*.outerPackaging"),
      (nodeToValidate, contextPath) -> {
        var dg = nodeToValidate.path("dangerousGoods");
        if (!dg.isArray() || dg.isEmpty()) {
          return Set.of();
        }
        if (nodeToValidate.path("packageCode").isMissingNode() && nodeToValidate.path("imoPackagingCode").isMissingNode()) {
          return Set.of("The '%s' object did not have a 'packageCode' nor an 'imoPackagingCode', which is required due to dangerousGoods"
            .formatted(contextPath));
        }
        return Set.of();
      }
    ),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'inhalationZone' values must be from dataset",
      allDg(dg -> dg.path("inhalationZone").submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.DG_INHALATION_ZONES)
    ),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'segregationGroups' values must be from dataset",
      allDg(dg -> dg.path("segregationGroups").all().submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.DG_SEGREGATION_GROUPS)
    ),
    UTE_EQUIPMENT_REFERENCE_UNIQUE,
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'temperatureSetpoint' implies 'temperatureUnit'",
      mav -> mav.submitAllMatching("utilizedTransportEquipments.*.activeReeferSettings"),
      JsonAttribute.presenceImpliesOtherField(
        "temperatureSetpoint",
        "temperatureUnit"
    )),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'airExchangeSetpoint' implies 'airExchangeUnit'",
      mav -> mav.submitAllMatching("utilizedTransportEquipments.*.activeReeferSettings"),
      JsonAttribute.presenceImpliesOtherField(
        "airExchangeSetpoint",
        "airExchangeUnit"
      )),
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
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(MODE_OF_TRANSPORT)
    ),
    NOTIFY_PARTIES_REQUIRED_IN_NEGOTIABLE_BLS,
    TLR_CC_T_COMBINATION_UNIQUE
  );

  public static final JsonContentCheck SIR_REQUIRED_IN_NOTIFICATION = JsonAttribute.mustBePresent(SI_NOTIFICATION_SIR_PTR);
  public static final JsonContentCheck TDR_REQUIRED_IN_NOTIFICATION = JsonAttribute.mustBePresent(TD_NOTIFICATION_TDR_PTR);

  public static JsonContentCheck sirInNotificationMustMatchDSP(Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.mustEqual(SI_NOTIFICATION_SIR_PTR, () -> dspSupplier.get().shippingInstructionsReference());
  }

  public static JsonContentCheck tdrInNotificationMustMatchDSP(Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.mustEqual(TD_NOTIFICATION_TDR_PTR, () -> dspSupplier.get().transportDocumentReference());
  }

  private static <T, O> Supplier<T> delayedValue(Supplier<O> cspSupplier, Function<O, T> field) {
    return () -> {
      var csp = cspSupplier.get();
      if (csp == null) {
        return null;
      }
      return field.apply(csp);
    };
  }

  private static void generateScenarioRelatedChecks(List<JsonContentCheck> checks, String standardVersion, Supplier<CarrierScenarioParameters> cspSupplier, Supplier<DynamicScenarioParameters> dspSupplier, boolean isTD) {
    checks.add(JsonAttribute.mustEqual(
      "[Scenario] Verify that the correct 'transportDocumentTypeCode' is used",
      "transportDocumentTypeCode",
      delayedValue(dspSupplier, dsp -> dsp.scenarioType().transportDocumentTypeCode())
    ));
    checks.add(JsonAttribute.allIndividualMatchesMustBeValid(
      "[Scenario] Verify that the correct 'carrierBookingReference' is used",
      mav -> mav.submitAllMatching("consignmentItems.*.carrierBookingReference"),
      JsonAttribute.matchedMustEqual(delayedValue(cspSupplier, CarrierScenarioParameters::carrierBookingReference))
    ));
    if (isTD) {
      checks.add(
        JsonAttribute.ifThen(
          "[Scenario] Verify that the transportDocument included 'carriersAgentAtDestination'",
          ignored -> {
            var dsp = dspSupplier.get();
            return dsp.shippingInstructions().path("isCarriersAgentAtDestinationRequired").asBoolean(false) || dsp.scenarioType().isCarriersAgentAtDestinationRequired();
          },
          JsonAttribute.path(DOCUMENT_PARTIES, JsonAttribute.path("carriersAgentAtDestination", JsonAttribute.matchedMustBePresent()))
      ));
    } else {
      checks.add(
        JsonAttribute.ifThen(
          "[Scenario] Verify that the shippingInstructions had 'isCarriersAgentAtDestinationRequired' as true if scenario requires it",
          ignored -> dspSupplier.get().scenarioType().isCarriersAgentAtDestinationRequired(),
          JsonAttribute.path("isCarriersAgentAtDestinationRequired", JsonAttribute.matchedMustBeTrue())
        ));
      checks.add(
        JsonAttribute.customValidator(
          "[Scenario] Verify that the correct 'commoditySubreference' is used",
          JsonAttribute.path(CONSIGNMENT_ITEMS, checkCommoditySubreference(cspSupplier))));

      checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
          "[Scenario] CargoItem and DG vs. outerPackaging in the SI",
          mav -> mav.submitAllMatching("consignmentItems.*.cargoItems.*"),
          JsonAttribute.ifMatchedThenElse(
            ignored -> dspSupplier.get().scenarioType().hasDG(),
            JsonAttribute.path("outerPackaging", JsonAttribute.matchedMustBeAbsent()),
            JsonAttribute.path("outerPackaging", JsonAttribute.matchedMustBePresent())
          )
        )
      );
    }
    checks.add(JsonAttribute.customValidator(
      "[Scenario] Verify that the correct 'equipmentReference' values are used",
      JsonAttribute.path(UTILIZED_TRANSPORT_EQUIPMENTS, checkEquipmentReference(cspSupplier))
    ));
    checks.add(JsonAttribute.customValidator(
      "[Scenario] Verify that the correct 'HSCodes' are used",
      JsonAttribute.path(CONSIGNMENT_ITEMS, checkHSCodes(cspSupplier))
    ));
    checks.add(JsonAttribute.customValidator(
      "[Scenario] Verify that the correct 'descriptionOfGoods' is used",
      JsonAttribute.path(CONSIGNMENT_ITEMS, checkDescriptionOfGoods(cspSupplier))
    ));

    checks.add(JsonAttribute.mustEqual(
      "[Scenario] Verify that the correct 'invoicePayableAt' location is used",
      SI_REQUEST_INVOICE_PAYABLE_AT_UN_LOCATION_CODE,
      delayedValue(cspSupplier, CarrierScenarioParameters::invoicePayableAtUNLocationCode)
    ));

    checks.add(JsonAttribute.customValidator(
      "[Scenario] Verify that the scenario contained references when the scenario requires it",
      scenarioReferencesCheck(dspSupplier)
    ));
    checks.add(JsonAttribute.customValidator(
      "[Scenario] Verify that 'customsReferences' is used when the scenario requires it",
      scenarioCustomsReferencesCheck(dspSupplier)
    ));
  }

  private static JsonContentMatchedValidation scenarioCustomsReferencesCheck(Supplier<DynamicScenarioParameters> dspSupplier) {
    return (nodeToValidate,contextPath) -> {
      var dsp = dspSupplier.get();
      var scenarioType = dsp.scenarioType();
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

  private static JsonContentMatchedValidation scenarioReferencesCheck(Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.ifMatchedThen(
      ignored -> dspSupplier.get().scenarioType().isReferencesRequired(),
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

  private static <T> Set<T> setOf(T v1, T v2) {
    var s = new LinkedHashSet<T>();
    if (v1 != null) {
      s.add(v1);
    }
    if (v2 != null) {
      s.add(v2);
    }
    return s;
  }

  private static JsonContentMatchedValidation checkCSPAllUsedAtLeastOnce(
    String attributeName,
    Supplier<Set<String>> expectedValuesSupplier
  ) {
    return checkCSPAllUsedAtLeastOnce(
      attributeName,
      expectedValuesSupplier,
      (node, pathBuilder) -> {
        pathBuilder.append(".").append(attributeName);
        return node.path(attributeName);
      }
    );
  }

  private static JsonContentMatchedValidation checkCSPValueBasedOnOtherValue(
    String attributeName,
    String referenceAttributeName,
    Supplier<Map<String, String>> expectedValuesSupplier,
    BiFunction<JsonNode, StringBuilder, JsonNode> resolveValue
  ) {
    return (nodes, contextPath) -> {
      var issues = new LinkedHashSet<String>();
      var seen = new HashSet<String>();
      var expectedValues = expectedValuesSupplier.get();
      int index = 0;
      var pathBuilder = new StringBuilder(contextPath);
      for (var node : nodes) {
        pathBuilder.setLength(contextPath.length());
        pathBuilder.append('[').append(index).append(']');
        var refPath = pathBuilder + "." + referenceAttributeName;
        var valueNode = resolveValue != null ? resolveValue.apply(node, pathBuilder) : node;
        index++;
        if (valueNode == null || !valueNode.isTextual()) {
          continue;
        }
        var value = valueNode.asText(null);
        if (value == null) {
          continue;
        }
        seen.add(value);
        var expectedReferenceValue = expectedValues.get(value);
        var refNode = node.path(referenceAttributeName);
        if (expectedReferenceValue == null) {
          if (!refNode.isMissingNode()) {
            issues.add("The '%s' attribute must be absent".formatted(refPath));
          }
          continue;
        }
        if (refNode.isMissingNode() || !refNode.isTextual()) {
          continue;
        }
        var refValue = refNode.asText(null);
        if (refValue == null) {
          continue;
        }
        if (!refValue.equals(expectedReferenceValue)) {
          issues.add("The value %s at %s is not aligned with %s. The %s was '%s' but should have been '%s'".formatted(
            value, pathBuilder.toString(), refPath, refPath, refValue, expectedReferenceValue
          ));
        }
      }
      for (var ref : expectedValues.keySet()) {
        if (!seen.contains(ref)) {
          issues.add("Expected %s %s to be used by one of the elements in %s, but it was not present".formatted(attributeName, ref, contextPath));
        }
      }
      return issues;
    };
  }

  private static JsonContentMatchedValidation checkCSPAllUsedAtLeastOnce(
    String attributeName,
    Supplier<Set<String>> expectedValuesSupplier,
    BiFunction<JsonNode, StringBuilder, JsonNode> resolveValue
  ) {
    return (nodes, contextPath) -> {
      var issues = new LinkedHashSet<String>();
      var seen = new HashSet<String>();
      var expectedValues = expectedValuesSupplier.get();
      int index = 0;
      var pathBuilder = new StringBuilder(contextPath);
      for (var node : nodes) {
        pathBuilder.setLength(contextPath.length());
        pathBuilder.append('[').append(index).append(']');
        var valueNode = resolveValue != null ? resolveValue.apply(node, pathBuilder) : node;
        index++;
        if (valueNode == null || !valueNode.isTextual()) {
          continue;
        }
        var value = valueNode.asText(null);
        if (value == null) {
          continue;
        }
        seen.add(value);
        if (!expectedValues.contains(value)) {
          issues.add("Unexpected %s %s at %s".formatted(attributeName, value, pathBuilder.toString()));
        }
      }
      for (var ref : expectedValues) {
        if (!seen.contains(ref)) {
          issues.add("Expected %s %s to be used by one of the elements in %s, but it was not present".formatted(attributeName, ref, contextPath));
        }
      }
      return issues;
    };
  }

  private static JsonContentMatchedValidation checkEquipmentReference(Supplier<CarrierScenarioParameters> cspSupplier) {
    BiFunction<JsonNode, StringBuilder, JsonNode> resolver = (ute, pathBuilder) -> {
      var equipmentReferenceNode = ute.get(EQUIPMENT_REFERENCE);
      if (equipmentReferenceNode != null) {
        pathBuilder.append(".equipmentReference");
      } else {
        pathBuilder.append(".equipment.equipmentReference");
        equipmentReferenceNode = ute.path("equipment").path(EQUIPMENT_REFERENCE);
      }
      return equipmentReferenceNode;
    };
    return (nodes, contextPath) -> {
      var csp = cspSupplier.get();
      var expectedReferences = setOf(csp.equipmentReference(), csp.equipmentReference2());
      if (expectedReferences.isEmpty()) {
        // SOC-case
        var issues = new LinkedHashSet<String>();
        int index = 0;
        var pathBuilder = new StringBuilder(contextPath);
        for (var node : nodes) {
          pathBuilder.setLength(contextPath.length());
          pathBuilder.append('[').append(index).append("].isShipperOwned");
          var isSoc = node.path("isShipperOwned").asBoolean(false);
          if (isSoc) {
            continue;
          }
          issues.add(
            "Expected %s to be true".formatted(pathBuilder.toString())
          );
        }

        return issues;
      }
      return checkCSPAllUsedAtLeastOnce(EQUIPMENT_REFERENCE, () -> expectedReferences, resolver)
          .validate(nodes, contextPath);
    };
   }

  private static JsonContentMatchedValidation checkCommoditySubreference(Supplier<CarrierScenarioParameters> cspSupplier) {
    Supplier<Set<String>> expectedValueSupplier = () -> {
      var csp = cspSupplier.get();
      return setOf(csp.commoditySubreference(), csp.commoditySubreference2());
    };
    return checkCSPAllUsedAtLeastOnce(
      "commoditySubreference",
      expectedValueSupplier
    );
  }

  private static JsonContentMatchedValidation checkDescriptionOfGoods(Supplier<CarrierScenarioParameters> cspSupplier) {
    Supplier<Map<String, String>> expectedValueSupplier = () -> {
      var csp = cspSupplier.get();
      var m = new LinkedHashMap<String, String>();
      if (csp.descriptionOfGoods() != null) {
        m.put(csp.descriptionOfGoods(), csp.commoditySubreference());
      }
      if (csp.descriptionOfGoods2() != null) {
        m.put(csp.descriptionOfGoods2(), csp.commoditySubreference2());
      }
      return m;
    };
    BiFunction<JsonNode, StringBuilder, JsonNode> resolver = (consignmentItem, pathBuilder) -> {
      var hsCodes = consignmentItem.path("descriptionOfGoods");
      if (!hsCodes.isArray()) {
        return null;
      }
      // TODO: We should support more descriptionOfGoods some other day.
      pathBuilder.append(".descriptionOfGoods[0]");
      return hsCodes.path(0);
    };
    return checkCSPValueBasedOnOtherValue(
      "descriptionOfGoods",
      "commoditySubreference",
      expectedValueSupplier,
      resolver
    );
  }

  private static JsonContentMatchedValidation checkHSCodes(Supplier<CarrierScenarioParameters> cspSupplier) {
    Supplier<Map<String, String>> expectedValueSupplier = () -> {
      var csp = cspSupplier.get();
      var m = new LinkedHashMap<String, String>();
      if (csp.descriptionOfGoods() != null) {
        m.put(csp.consignmentItemHSCode(), csp.commoditySubreference());
      }
      if (csp.descriptionOfGoods2() != null) {
        m.put(csp.consignmentItem2HSCode(), csp.commoditySubreference2());
      }
      return m;
    };
    BiFunction<JsonNode, StringBuilder, JsonNode> resolver = (consignmentItem, pathBuilder) -> {
      var hsCodes = consignmentItem.path("HSCodes");
      if (!hsCodes.isArray()) {
        return null;
      }
      // TODO: We should support more HSCodes some other day.
      pathBuilder.append(".HSCodes[0]");
      return hsCodes.path(0);
    };
    return checkCSPValueBasedOnOtherValue(
      "HSCodes",
      "commoditySubreference",
      expectedValueSupplier,
      resolver
    );
  }

  public static ActionCheck siRequestContentChecks(UUID matched, String standardVersion, Supplier<CarrierScenarioParameters> cspSupplier, Supplier<DynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<>(STATIC_SI_CHECKS);
    checks.add(DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE);
    checks.add(VALIDATE_DOCUMENT_PARTIES_MATCH_EBL);
    generateScenarioRelatedChecks(checks, standardVersion, cspSupplier, dspSupplier, false);
    return JsonAttribute.contentChecks(
      EblRole::isShipper,
      matched,
      HttpMessageType.REQUEST,
      standardVersion,
      checks
    );
  }

  public static ActionCheck siResponseContentChecks(UUID matched, String standardVersion, Supplier<CarrierScenarioParameters> cspSupplier, Supplier<DynamicScenarioParameters> dspSupplier, ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus updatedShippingInstructionsStatus, boolean requestedAmendment) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(JsonAttribute.mustEqual(
      SI_REF_SIR_PTR,
      () -> dspSupplier.get().shippingInstructionsReference()
    ));
    checks.add(JsonAttribute.mustEqual(
      SI_REF_SI_STATUS_PTR,
      shippingInstructionsStatus.wireName()
    ));
    if (updatedShippingInstructionsStatus != ShippingInstructionsStatus.SI_ANY) {
      var updatedStatusCheck = updatedShippingInstructionsStatus != null
        ? JsonAttribute.mustEqual(
        SI_REF_UPDATED_SI_STATUS_PTR,
        updatedShippingInstructionsStatus.wireName())
        : JsonAttribute.mustBeAbsent(SI_REF_UPDATED_SI_STATUS_PTR);
      checks.add(updatedStatusCheck);
    }
    checks.addAll(STATIC_SI_CHECKS);
    checks.add(JsonAttribute.lostAttributeCheck(
      "Validate that shipper provided data was not altered",
      delayedValue(dspSupplier, dsp -> requestedAmendment ? dsp.updatedShippingInstructions() : dsp.shippingInstructions()),
      SI_NORMALIZER
    ));
    generateScenarioRelatedChecks(checks, standardVersion, cspSupplier, dspSupplier, false);
    return JsonAttribute.contentChecks(
      EblRole::isCarrier,
      matched,
      HttpMessageType.RESPONSE,
      standardVersion,
      checks
    );
  }

  public static Stream<ActionCheck> siRefStatusContentChecks(UUID matched, String standardsVersion, ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus updatedShippingInstructionsStatus, JsonContentCheck ... extraChecks) {
    var updatedStatusCheck = updatedShippingInstructionsStatus != null
      ? JsonAttribute.mustEqual(
      SI_REF_UPDATED_SI_STATUS_PTR,
      updatedShippingInstructionsStatus.wireName())
      : JsonAttribute.mustBeAbsent(SI_REF_UPDATED_SI_STATUS_PTR);
    var checks = new ArrayList<>(Arrays.asList(extraChecks));
    checks.add(JsonAttribute.mustEqual(
      SI_REF_SI_STATUS_PTR,
      shippingInstructionsStatus.wireName()
    ));
    checks.add(updatedStatusCheck);
    return Stream.of(
      JsonAttribute.contentChecks(
        EblRole::isCarrier,
        matched,
        HttpMessageType.RESPONSE,
        standardsVersion,
        checks
      )
    );
  }

  public static ActionCheck tdRefStatusChecks(UUID matched, String standardVersion, Supplier<DynamicScenarioParameters> dspSupplier, TransportDocumentStatus transportDocumentStatus) {
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

  public static ActionCheck siNotificationContentChecks(UUID matched, String standardsVersion, ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus updatedShippingInstructionsStatus, JsonContentCheck ... extraChecks) {
    String titlePrefix = "[Notification]";
    var updatedStatusCheck = updatedShippingInstructionsStatus != null
      ? JsonAttribute.mustEqual(
      SI_NOTIFICATION_UPDATED_SI_STATUS_PTR,
      updatedShippingInstructionsStatus.wireName())
      : JsonAttribute.mustBeAbsent(SI_NOTIFICATION_UPDATED_SI_STATUS_PTR);
    List<JsonContentCheck> jsonContentChecks = new ArrayList<>(Arrays.asList(extraChecks));
    jsonContentChecks.add(JsonAttribute.mustEqual(
      SI_NOTIFICATION_SI_STATUS_PTR,
      shippingInstructionsStatus.wireName()
    ));
    jsonContentChecks.add(updatedStatusCheck);
    return JsonAttribute.contentChecks(
      titlePrefix,
      null,
      EblRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      standardsVersion,
      jsonContentChecks
    );
  }

  public static ActionCheck tdNotificationContentChecks(UUID matched, String standardsVersion, TransportDocumentStatus transportDocumentStatus, JsonContentCheck ... extraChecks) {
    String titlePrefix = "[Notification]";
    List<JsonContentCheck> jsonContentChecks = new ArrayList<>(Arrays.asList(extraChecks));
    jsonContentChecks.add(JsonAttribute.mustEqual(
      TD_NOTIFICATION_TD_STATUS_PTR,
      transportDocumentStatus.wireName()
    ));
    return JsonAttribute.contentChecks(
      titlePrefix,
      null,
      EblRole::isCarrier,
      matched,
      HttpMessageType.REQUEST,
      standardsVersion,
      jsonContentChecks
    );
  }

  public static void genericTdContentChecks(List<? super JsonRebaseableContentCheck> jsonContentChecks, String standardVersion, Supplier<String> tdrSupplier, TransportDocumentStatus transportDocumentStatus) {
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

  public static List<JsonRebaseableContentCheck> genericTDContentChecks(TransportDocumentStatus transportDocumentStatus, String eblStandardVersion, Supplier<String> tdrReferenceSupplier) {
    List<JsonRebaseableContentCheck> jsonContentChecks = new ArrayList<>();
    genericTdContentChecks(
      jsonContentChecks,
      eblStandardVersion,
      tdrReferenceSupplier,
      transportDocumentStatus
    );
    return jsonContentChecks;
  }

  public static ActionCheck tdPlusScenarioContentChecks(UUID matched, String standardVersion, TransportDocumentStatus transportDocumentStatus, Supplier<CarrierScenarioParameters> cspSupplier, Supplier<DynamicScenarioParameters> dspSupplier) {
    List<JsonContentCheck> jsonContentChecks = new ArrayList<>();
    genericTdContentChecks(
      jsonContentChecks,
      standardVersion,
      () -> dspSupplier.get().transportDocumentReference(),
      transportDocumentStatus
    );
    jsonContentChecks.add(JsonAttribute.allIndividualMatchesMustBeValid(
      "[Scenario] Validate the containers reefer settings",
      mav-> mav.submitAllMatching("utilizedTransportEquipments.*"),
      (nodeToValidate, contextPath) -> {
        var scenario = dspSupplier.get().scenarioType();
        var activeReeferNode = nodeToValidate.path(ACTIVE_REEFER_SETTINGS);
        var nonOperatingReeferNode = nodeToValidate.path(IS_NON_OPERATING_REEFER);
        var issues = new LinkedHashSet<String>();
        switch (scenario) {
          case ACTIVE_REEFER -> {
            if (!activeReeferNode.isObject()) {
              issues.add("The scenario requires '%s' to have an active reefer".formatted(contextPath));
            }
          }
          case NON_OPERATING_REEFER -> {
            if (!nonOperatingReeferNode.asBoolean(false)) {
              issues.add("The scenario requires '%s.isNonOperatingReefer' to be true".formatted(contextPath));
            }
          }
          default -> {
            if (!activeReeferNode.isMissingNode()) {
              issues.add("The scenario requires '%s' to NOT have an active reefer".formatted(contextPath));
            }
            if (nonOperatingReeferNode.asBoolean(false)) {
              issues.add("The scenario requires '%s.isNonOperatingReefer' to be omitted or false (depending on the container ISO code)".formatted(contextPath));
            }
          }
        }
        return issues;
      }
    ));
    jsonContentChecks.add(JsonAttribute.allIndividualMatchesMustBeValid(
      "[Scenario] Whether the cargo should be DG",
      mav-> mav.submitAllMatching("consignmentItems.*.cargoItems.*.outerPackaging.*.dangerousGoods"),
      (nodeToValidate, contextPath) -> {
        var scenario = dspSupplier.get().scenarioType();
        if (scenario == ScenarioType.DG) {
          if (!nodeToValidate.isArray() || nodeToValidate.isEmpty()) {
            return Set.of("The scenario requires '%s' to contain dangerous goods".formatted(contextPath));
          }
        } else {
          if (!nodeToValidate.isMissingNode() || !nodeToValidate.isEmpty()) {
            return Set.of("The scenario requires '%s' to NOT contain any dangerous goods".formatted(contextPath));
          }
        }
        return Set.of();
      }
    ));
    generateScenarioRelatedChecks(jsonContentChecks, standardVersion, cspSupplier, dspSupplier, true);
    return JsonAttribute.contentChecks(
      EblRole::isCarrier,
      matched,
      HttpMessageType.RESPONSE,
      standardVersion,
      jsonContentChecks
    );
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
