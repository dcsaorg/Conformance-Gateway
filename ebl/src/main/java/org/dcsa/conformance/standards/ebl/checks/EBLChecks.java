package org.dcsa.conformance.standards.ebl.checks;

import static org.dcsa.conformance.core.check.JsonAttribute.concatContextPath;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
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
  private static final JsonPointer SI_REQUEST_SEND_TO_PLATFORM = JsonPointer.compile("/sendToPlatform");

  private static final JsonPointer TD_TDR = JsonPointer.compile("/transportDocumentReference");
  private static final JsonPointer TD_TRANSPORT_DOCUMENT_STATUS = JsonPointer.compile("/transportDocumentStatus");

  private static final JsonRebaseableContentCheck ONLY_EBLS_CAN_BE_NEGOTIABLE = JsonAttribute.ifThen(
    "Validate transportDocumentTypeCode vs. isToOrder",
    JsonAttribute.isTrue(JsonPointer.compile("/isToOrder")),
    JsonAttribute.mustEqual(JsonPointer.compile("/transportDocumentTypeCode"), "BOL")
  );

  private static final Consumer<MultiAttributeValidator> ALL_REFERENCE_TYPES = (mav) -> {
    mav.submitAllMatching("references.*.type");
    mav.submitAllMatching("utilizedTransportEquipments.*.references.*.type");
    mav.submitAllMatching("consignmentItems.*.references.*.type");
  };

  private static final JsonRebaseableContentCheck VALID_REFERENCE_TYPES = JsonAttribute.allIndividualMatchesMustBeValid(
    "All reference 'type' fields must be valid",
        ALL_REFERENCE_TYPES,
        JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.REFERENCE_TYPE)
  );

  private static final JsonRebaseableContentCheck TLR_CC_T_COMBINATION_VALIDATIONS = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate combination of 'countryCode' and 'type' in 'taxAndLegalReferences'",
    (mav) -> {
      mav.submitAllMatching("issuingParty.taxLegalReferences.*");
      mav.submitAllMatching("documentParties.*.party.taxLegalReferences.*");
    },
    JsonAttribute.combineAndValidateAgainstDataset(EblDatasets.LTR_CC_T_COMBINATIONS, "countryCode", "type")
   );

  private static final JsonRebaseableContentCheck TLR_CC_T_COMBINATION_UNIQUE = JsonAttribute.allIndividualMatchesMustBeValid(
    "Each document party can be used at most once",
    (mav) -> {
      mav.submitAllMatching("issuingParty.taxLegalReferences");
      mav.submitAllMatching("documentParties.*.party.taxLegalReferences");
    },
    JsonAttribute.unique("countryCode", "type")
  );

  private static final Consumer<MultiAttributeValidator> ALL_UN_LOCATION_CODES = (mav) -> {
    mav.submitAllMatching("invoicePayableAt.UNLocationCode");
    mav.submitAllMatching("transports.placeOfReceipt.UNLocationCode");
    mav.submitAllMatching("transports.portOfLoading.UNLocationCode");
    mav.submitAllMatching("transports.portOfDischarge.UNLocationCode");
    mav.submitAllMatching("transports.placeOfDelivery.UNLocationCode");
    mav.submitAllMatching("transports.onwardInlandRouting.UNLocationCode");

    // Beta-2 only
    mav.submitAllMatching("issuingParty.address.UNLocationCode");
    mav.submitAllMatching("documentParties.shippers.address.UNLocationCode");
    mav.submitAllMatching("documentParties.consignee.address.UNLocationCode");
    mav.submitAllMatching("documentParties.endorsee.address.UNLocationCode");
    mav.submitAllMatching("documentParties.other.*.party.address.UNLocationCode");
  };

  private static final JsonRebaseableContentCheck TD_UN_LOCATION_CODES_VALID = JsonAttribute.allIndividualMatchesMustBeValid(
    "UN Location are valid",
      ALL_UN_LOCATION_CODES,
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.UN_LOCODE_DATASET)
  );

  private static final Consumer<MultiAttributeValidator> ALL_UTE = (mav) -> mav.submitAllMatching("utilizedTransportEquipments.*");

  private static final Predicate<JsonNode> HAS_ISO_EQUIPMENT_CODE = (uteNode) -> {
    var isoEquipmentNode = uteNode.path("equipment").path("ISOEquipmentCode");
    return isoEquipmentNode.isTextual();
  };

  private static final Predicate<JsonNode> IS_ISO_EQUIPMENT_CONTAINER_REEFER = (uteNode) -> {
    var isoEquipmentNode = uteNode.path("equipment").path("ISOEquipmentCode");
    return isReeferContainerSizeTypeCode(isoEquipmentNode.asText(""));
  };

  private static final JsonRebaseableContentCheck ISO_EQUIPMENT_CODE_IMPLIES_REEFER = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate utilizedTransportEquipment and reefer attributes",
    ALL_UTE,
    JsonAttribute.ifMatchedThenElse(
      HAS_ISO_EQUIPMENT_CODE,
      JsonAttribute.ifMatchedThenElse(
        IS_ISO_EQUIPMENT_CONTAINER_REEFER,
        JsonAttribute.path("isNonOperatingReefer", JsonAttribute.matchedMustBePresent()),
        JsonAttribute.combine(
          JsonAttribute.path("isNonOperatingReefer", JsonAttribute.matchedMustBeAbsent()),
          JsonAttribute.path("activeReeferSettings", JsonAttribute.matchedMustBeAbsent())
        )
      ),
      // If there is no ISOEquipmentCode, then we can only say that `activeReeferSettings` implies
      // `isNonOperatingReefer=False` (the `=False` part is checked elsewhere).
      JsonAttribute.presenceImpliesOtherField(
        "activeReeferSettings",
        "isNonOperatingReefer"
      )
    )
  );

  private static final JsonRebaseableContentCheck NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER = JsonAttribute.allIndividualMatchesMustBeValid(
    "All utilizedTransportEquipments where 'isNonOperatingReefer' is 'false' must have 'activeReeferSettings'",
    ALL_UTE,
    JsonAttribute.ifMatchedThen(
      JsonAttribute.isFalse("isNonOperatingReefer"),
      JsonAttribute.path("activeReeferSettings", JsonAttribute.matchedMustBePresent())
    )
  );

  private static final JsonRebaseableContentCheck NOR_IS_TRUE_IMPLIES_NO_ACTIVE_REEFER = JsonAttribute.allIndividualMatchesMustBeValid(
    "All utilizedTransportEquipments where 'isNonOperatingReefer' is 'true' cannot have 'activeReeferSettings'",
    ALL_UTE,
    JsonAttribute.ifMatchedThen(
      JsonAttribute.isTrue("isNonOperatingReefer"),
      JsonAttribute.path("activeReeferSettings", JsonAttribute.matchedMustBeAbsent())
    )
  );

  private static final Consumer<MultiAttributeValidator> ALL_AMF = (mav) -> mav.submitAllMatching("advanceManifestFilings.*");
  private static final JsonRebaseableContentCheck AMF_SELF_FILER_CODE_CONDITIONALLY_MANDATORY = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate conditionally mandatory 'selfFilerCode' in 'advanceManifestFilings'",
    ALL_AMF,
    (nodeToValidate, contextPath) -> {
      if (!nodeToValidate.path("advanceManifestFilingsHouseBLPerformedBy").asText("").equals("SHIPPER")
      || nodeToValidate.path("selfFilerCode").isTextual()) {
        return Set.of();
      }
      var country = nodeToValidate.path("countryCode").asText("");
      var manifestTypeCode = nodeToValidate.path("manifestTypeCode").asText("");
      var combined = country + "/" + manifestTypeCode;
      if (EblDatasets.AMF_CC_MTC_REQUIRES_SELF_FILER_CODE.contains(combined)) {
        return Set.of(
          "The 'selfFilerCode' must be provided in '%s' due to the combination of 'advanceManifestFilingsHouseBLPerformedBy', 'countryCode' and 'manifestTypeCode'.".formatted(contextPath)
        );
      }
      return Set.of();
  });

  private static final JsonRebaseableContentCheck AMF_CC_MTC_COMBINATION_VALIDATIONS = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate combination of 'countryCode' and 'manifestTypeCode' in 'advanceManifestFilings'",
    ALL_AMF,
    JsonAttribute.combineAndValidateAgainstDataset(EblDatasets.AMF_CC_MTC_COMBINATIONS, "countryCode", "manifestTypeCode")
  );

  private static final Consumer<MultiAttributeValidator> ALL_CUSTOMS_REFERENCES = (mav) -> {
    mav.submitAllMatching("customsReferences.*");
    mav.submitAllMatching("consignmentItems.*.customsReferences.*");
    mav.submitAllMatching("consignmentItems.*.cargoItems.*.customsReferences.*");
    mav.submitAllMatching("utilizedTransportEquipments.*.customsReferences.*");
  };

  private static final JsonRebaseableContentCheck CR_CC_T_COMBINATION_KNOWN = JsonAttribute.allIndividualMatchesMustBeValid(
    "The combination of 'countryCode' and 'type' in 'customsReferences' must be valid",
    ALL_CUSTOMS_REFERENCES,
    JsonAttribute.combineAndValidateAgainstDataset(EblDatasets.CUSTOMS_REFERENCE_CC_RTC_COMBINATIONS, "countryCode", "type")
  );

  private static final JsonRebaseableContentCheck CR_CC_T_CODES_UNIQUE = JsonAttribute.allIndividualMatchesMustBeValid(
    "The combination of 'countryCode' and 'type' in '*.customsReferences' must be unique",
    (mav) -> {
      mav.submitAllMatching("customsReferences");
      mav.submitAllMatching("consignmentItems.*.customsReferences");
      mav.submitAllMatching("consignmentItems.*.cargoItems.*.customsReferences");
      mav.submitAllMatching("utilizedTransportEquipments.*.customsReferences");
    },
    JsonAttribute.unique("countryCode", "type")
  );

  private static final JsonRebaseableContentCheck COUNTRY_CODE_VALIDATIONS = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate field is a known ISO 3166 alpha 2 code",
    (mav) -> {
      mav.submitAllMatching("placeOfIssue.countryCode");
      mav.submitAllMatching("advancedManifestFilings.*.countryCode");
      mav.submitAllMatching("customsReferences.*.countryCode");
      mav.submitAllMatching("consignmentItems.*.customsReferences.*.countryCode");
      mav.submitAllMatching("consignmentItems.*.cargoItems.*.customsReferences.*.countryCode");
      mav.submitAllMatching("utilizedTransportEquipments.*.customsReferences.*.countryCode");
      mav.submitAllMatching("documentParties.*.party.taxLegalReferences.*.countryCode");
      mav.submitAllMatching("issuingParty.taxLegalReferences.*.countryCode");

      // Beta-2 only
      mav.submitAllMatching("issuingParty.address.countryCode");
      mav.submitAllMatching("documentParties.shippers.address.countryCode");
      mav.submitAllMatching("documentParties.consignee.address.countryCode");
      mav.submitAllMatching("documentParties.endorsee.address.countryCode");
      mav.submitAllMatching("documentParties.other.*.party.address.countryCode");
    },
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.ISO_3166_ALPHA2_COUNTRY_CODES)
  );

  private static final JsonRebaseableContentCheck OUTER_PACKAGING_CODE_IS_VALID = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate that 'packagingCode' is a known code",
    (mav) -> mav.submitAllMatching("consignmentItems.*.cargoItems.*.outerPackaging.packageCode"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.OUTER_PACKAGING_CODE)
  );

  private static final JsonRebaseableContentCheck DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE = JsonAttribute.customValidator(
    "Each document party can be used at most once",
    JsonAttribute.path(
      "documentParties",
      JsonAttribute.path("other", JsonAttribute.unique("partyFunction"))
    ));


  private static final JsonRebaseableContentCheck VALIDATE_DOCUMENT_PARTIES_MATCH_EBL = JsonAttribute.customValidator(
    "Validate documentParties match the EBL type",
    (body, contextPath) -> {
      var issues = new LinkedHashSet<String>();
      var documentParties = body.path("documentParties");
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

  private static final JsonRebaseableContentCheck VALIDATE_CONTRACT_REFERENCE = JsonAttribute.atLeastOneOf(
      JsonPointer.compile("/contractQuotationReference"),
      JsonPointer.compile("/serviceContractReference")
  );

  private static Consumer<MultiAttributeValidator> allDg(Consumer<MultiAttributeValidator.AttributePathBuilder> consumer) {
    return (mav) -> consumer.accept(mav.path("consignmentItems").all().path("cargoItems").all().path("outerPackaging").path("dangerousGoods").all());
  }

  private static final JsonRebaseableContentCheck CARGO_ITEM_REFERENCES_KNOWN_EQUIPMENT = JsonAttribute.customValidator(
    "Equipment References in 'cargoItems' must be present in 'utilizedTransportEquipments'",
    (body, contextPath) -> {
      var knownEquipmentReferences = allEquipmentReferences(body);
      var missing = new LinkedHashSet<String>();
      for (var consignmentItem : body.path("consignmentItems")) {
        for (var cargoItem : consignmentItem.path("cargoItems")) {
          var ref = cargoItem.path("equipmentReference").asText(null);
          if (ref == null) {
            // Schema validated
            continue;
          }
          if (!knownEquipmentReferences.contains(ref)) {
            missing.add(ref);
          }
        }
      }
      var path = concatContextPath(contextPath, "utilizedTransportEquipments");
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
      var path = concatContextPath(contextPath, "utilizedTransportEquipments");
      return duplicates.stream()
        .map(ref -> "The equipment reference '%s' was used more than once in '%s'".formatted(ref, path))
        .collect(Collectors.toSet());
    }
  );

  private static final JsonRebaseableContentCheck VOLUME_IMPLIES_VOLUME_UNIT = JsonAttribute.allIndividualMatchesMustBeValid(
    "The use of 'volume' implies 'volumeUnit'",
    (mav) -> {
      mav.submitAllMatching("consignmentItems.*");
      mav.submitAllMatching("consignmentItems.*.cargoItems.*");
    },
    JsonAttribute.presenceImpliesOtherField(
      "volume",
      "volumeUnit"
    )
  );

  private static void utilizedTransportEquipmentCargoItemAlignment(
    JsonNode consignmentItems,
    String equipmentReference,
    String uteContextPath,
    double uteNumber,
    String uteUnit,
    String numberFieldName,
    String numberUnitFieldName,
    Map<String, Double> conversionTable,
    Set<String> issues
  ) {
    boolean converted = false;
    var sum = 0.0;
    boolean matchedEquipmentReference = false;
    for (var consignmentItem : consignmentItems) {
      for (var cargoItem : consignmentItem.path("cargoItems")) {
        var cargoItemEquipmentReference = cargoItem.path("equipmentReference").asText(null);
        if (!Objects.equals(equipmentReference, cargoItemEquipmentReference)) {
          continue;
        }
        matchedEquipmentReference = true;
        var cargoItemWeight = cargoItem.path(numberFieldName).asDouble(0.0);
        var cargoItemWeightUnit = cargoItem.path(numberUnitFieldName).asText(null);
        if (cargoItemWeightUnit != null && !cargoItemWeightUnit.equals(uteUnit)) {
          var conversionFactor = conversionTable.get(uteUnit + "->" + cargoItemWeightUnit);
          if (conversionFactor != null) {
            converted = true;
            cargoItemWeight *= conversionFactor;
          }
        }
        sum += cargoItemWeight;
      }
    }
    if (!matchedEquipmentReference) {
      // Another validation gets to complain about this
      return;
    }
    var delta = Math.abs(sum - uteNumber);
    // Give some leeway when converting.
    var e = converted ? 0.99 : 0.0001;
    if (delta < e) {
      return;
    }
    issues.add(
      "The utilizedTransportEquipment at '%s' had '%s' of %.2f %s but the cargoItems '%s' for it sums to %.2f %s%s"
        .formatted(
          uteContextPath,
          numberFieldName,
          uteNumber,
          uteUnit,
          numberFieldName,
          sum,
          uteUnit,
          converted
            ? " ('cargoItems' values were normalized to the utilizedTransportEquipment unit)"
            : ""));
  }

  private static JsonContentMatchedValidation allUtilizedTransportEquipmentCargoItemAreAligned(
    String uteNumberFieldName,
    String uteNumberUnitFieldName,
    String cargoItemNumberFieldName,
    String cargoItemNumberUnitFieldName,
    Map<String, Double> conversionTable
  ) {
    return (tdRoot, contextPath) -> {
      var consignmentItems = tdRoot.path("consignmentItems");
      var issues = new LinkedHashSet<String>();
      var index = 0;
      var key = "utilizedTransportEquipments";
      for (var ute : tdRoot.path(key)) {
        var uteContextPath = concatContextPath(contextPath, key + "[" + index + "]");
        ++index;
        var equipmentReference = ute.path("equipment").path("equipmentReference").asText(null);
        var uteNumberNode = ute.path(uteNumberFieldName);
        var uteUnit = ute.path(uteNumberUnitFieldName).asText("");
        if (equipmentReference == null || uteNumberNode.isMissingNode() || !uteNumberNode.isNumber()) {
          // Another validation will complain if applicable
          continue;
        }
        utilizedTransportEquipmentCargoItemAlignment(
          consignmentItems,
          equipmentReference,
          uteContextPath,
          uteNumberNode.asDouble(),
          uteUnit,
          cargoItemNumberFieldName,
          cargoItemNumberUnitFieldName,
          conversionTable,
          issues
        );
      }
      return issues;
    };
  }


  private static JsonContentMatchedValidation consignmentItemCargoItemAlignment(
    String numberFieldName,
    String numberUnitFieldName,
    Map<String, Double> conversionTable
  ) {
    return (consignmentItem, contextPath) -> {
      boolean converted = false;
      var sum = 0.0;
      var unit = consignmentItem.path(numberUnitFieldName).asText(null);
      if (unit == null) {
        // Some other validation will complain about this if relevant.
        return Set.of();
      }
      for (var cargoItem : consignmentItem.path("cargoItems")) {
        var cargoItemWeight = cargoItem.path(numberFieldName).asDouble(0.0);
        var cargoItemWeightUnit = cargoItem.path(numberUnitFieldName).asText(null);
        if (cargoItemWeightUnit != null && !cargoItemWeightUnit.equals(unit)) {
          var conversionFactor = conversionTable.get(unit + "->" + cargoItemWeightUnit);
          if (conversionFactor != null) {
            converted = true;
            cargoItemWeight *= conversionFactor;
          }
        }
        sum += cargoItemWeight;
      }
      var expected = consignmentItem.path(numberFieldName).asDouble(0.0);
      var delta = Math.abs(sum - expected);
      // Give some leeway when converting.
      var e = converted ? 0.99 : 0.0001;
      if (delta < e) {
        return Set.of();
      }
      return Set.of(
          "The consignmentItem at '%s' had '%s' of %.2f %s but the cargoItem '%s' sums to %.2f %s%s"
              .formatted(
                  contextPath,
                  numberFieldName,
                  expected,
                  unit,
                  numberFieldName,
                  sum,
                  unit,
                  converted
                      ? " ('cargoItems' values were normalized to the consignmentItem unit)"
                      : ""));
    };
  }

  private static final Map<String, Double> RATIO_WEIGHT = Map.of(
    "KGM->LBR", 0.45359237,
    "LBR->KGM", 2.2046226218
  );

  private static final Map<String, Double> RATIO_VOLUME = Map.of(
    "MTQ->FTQ", 0.02831685,
    "FTQ->MTQ",  35.3146667
  );

  private static final JsonRebaseableContentCheck CONSIGNMENT_ITEM_VS_CARGO_ITEM_WEIGHT_IS_ALIGNED = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate that 'consignmentItem' weight is aligned with its 'cargoItems'",
    (mav) -> mav.submitAllMatching("consignmentItems.*"),
    consignmentItemCargoItemAlignment("weight",
      "weightUnit",
      RATIO_WEIGHT
  ));

  private static final JsonRebaseableContentCheck CONSIGNMENT_ITEM_VS_CARGO_ITEM_VOLUME_IS_ALIGNED = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate that 'consignmentItem' volume is aligned with its 'cargoItems'",
    (mav) -> mav.submitAllMatching("consignmentItems.*"),
    consignmentItemCargoItemAlignment("volume",
      "volumeUnit",
      RATIO_VOLUME
    ));

  private static final JsonRebaseableContentCheck ADVANCED_MANIFEST_FILING_CODES_UNIQUE = JsonAttribute.customValidator(
    "The combination of 'countryCode' and 'manifestTypeCode' in 'advanceManifestFilings' must be unique",
    JsonAttribute.unique("countryCode", "manifestTypeCode")
  );

  private static final List<JsonContentCheck> STATIC_SI_CHECKS = Arrays.asList(
    JsonAttribute.mustBeDatasetKeywordIfPresent(
      SI_REQUEST_INVOICE_PAYABLE_AT_UN_LOCATION_CODE,
      EblDatasets.UN_LOCODE_DATASET
    ),
    JsonAttribute.mustBeDatasetKeywordIfPresent(
      SI_REQUEST_SEND_TO_PLATFORM,
      EblDatasets.EBL_PLATFORMS_DATASET
    ),
    ONLY_EBLS_CAN_BE_NEGOTIABLE,
    JsonAttribute.ifThenElse(
      "'isElectronic' implies 'sendToPlatform'",
      JsonAttribute.isTrue(JsonPointer.compile("/isElectronic")),
      JsonAttribute.mustBePresent(JsonPointer.compile("/sendToPlatform")),
      JsonAttribute.mustBeAbsent(JsonPointer.compile("/sendToPlatform"))
    ),
    VALID_REFERENCE_TYPES,
    ISO_EQUIPMENT_CODE_IMPLIES_REEFER,
    UTE_EQUIPMENT_REFERENCE_UNIQUE,
    CARGO_ITEM_REFERENCES_KNOWN_EQUIPMENT,
    ADVANCED_MANIFEST_FILING_CODES_UNIQUE,
    COUNTRY_CODE_VALIDATIONS,
    AMF_CC_MTC_COMBINATION_VALIDATIONS,
    AMF_SELF_FILER_CODE_CONDITIONALLY_MANDATORY,
    CR_CC_T_COMBINATION_KNOWN,
    CR_CC_T_CODES_UNIQUE,
    OUTER_PACKAGING_CODE_IS_VALID,
    VOLUME_IMPLIES_VOLUME_UNIT,
    CONSIGNMENT_ITEM_VS_CARGO_ITEM_WEIGHT_IS_ALIGNED,
    CONSIGNMENT_ITEM_VS_CARGO_ITEM_VOLUME_IS_ALIGNED,
    TLR_CC_T_COMBINATION_VALIDATIONS,
    TLR_CC_T_COMBINATION_UNIQUE
  );

  private static final List<JsonRebaseableContentCheck> STATIC_TD_CHECKS = Arrays.asList(
    ONLY_EBLS_CAN_BE_NEGOTIABLE,
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
    VALID_REFERENCE_TYPES,
    ISO_EQUIPMENT_CODE_IMPLIES_REEFER,
    NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER,
    NOR_IS_TRUE_IMPLIES_NO_ACTIVE_REEFER,
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'commoditySubreference' must not be present in the transport document",
      (mav) -> mav.submitAllMatching("consignmentItems.*.commoditySubreference"),
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
      "The 'imoClass' values must be from dataset",
      allDg((dg) -> dg.path("imoClass").submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.DG_IMO_CLASSES)
    ),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'inhalationZone' values must be from dataset",
      allDg((dg) -> dg.path("inhalationZone").submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.DG_INHALATION_ZONES)
    ),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'segregationGroups' values must be from dataset",
      allDg((dg) -> dg.path("segregationGroups").all().submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.DG_SEGREGATION_GROUPS)
    ),
    UTE_EQUIPMENT_REFERENCE_UNIQUE,
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'temperatureSetpoint' implies 'temperatureUnit'",
      (mav) -> mav.submitAllMatching("utilizedTransportEquipments.*.activeReeferSettings"),
      JsonAttribute.presenceImpliesOtherField(
        "temperatureSetpoint",
        "temperatureUnit"
    )),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'airExchangeSetpoint' implies 'airExchangeUnit'",
      (mav) -> mav.submitAllMatching("utilizedTransportEquipments.*.activeReeferSettings"),
      JsonAttribute.presenceImpliesOtherField(
        "airExchangeSetpoint",
        "airExchangeUnit"
      )),
    CARGO_ITEM_REFERENCES_KNOWN_EQUIPMENT,
    ADVANCED_MANIFEST_FILING_CODES_UNIQUE,
    COUNTRY_CODE_VALIDATIONS,
    CR_CC_T_COMBINATION_KNOWN,
    CR_CC_T_CODES_UNIQUE,
    AMF_CC_MTC_COMBINATION_VALIDATIONS,
    AMF_SELF_FILER_CODE_CONDITIONALLY_MANDATORY,
    VOLUME_IMPLIES_VOLUME_UNIT,
    OUTER_PACKAGING_CODE_IS_VALID,
    CONSIGNMENT_ITEM_VS_CARGO_ITEM_WEIGHT_IS_ALIGNED,
    CONSIGNMENT_ITEM_VS_CARGO_ITEM_VOLUME_IS_ALIGNED,
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'cargoGrossVolume' implies 'cargoGrossVolumeUnit'",
      (mav) -> mav.submitAllMatching("utilizedTransportEquipments.*"),
      JsonAttribute.presenceImpliesOtherField(
        "cargoGrossVolume",
        "cargoGrossVolumeUnit"
    )),
    JsonAttribute.customValidator(
      "Validate that 'utilizedTransportEquipment' weight is aligned with its 'cargoItems'",
      allUtilizedTransportEquipmentCargoItemAreAligned(
        "cargoGrossWeight",
        "cargoGrossWeightUnit",
        "weight",
        "weightUnit",
        RATIO_WEIGHT
      )
    ),
    JsonAttribute.customValidator(
      "Validate that 'utilizedTransportEquipment' volume is aligned with its 'cargoItems'",
      allUtilizedTransportEquipmentCargoItemAreAligned(
        "cargoGrossVolume",
        "cargoGrossVolumeUnit",
        "volume",
        "volumeUnit",
        RATIO_VOLUME
      )
    ),
    TLR_CC_T_COMBINATION_VALIDATIONS,
    TLR_CC_T_COMBINATION_UNIQUE,
    TD_UN_LOCATION_CODES_VALID
  );

  public static final JsonContentCheck SIR_REQUIRED_IN_REF_STATUS = JsonAttribute.mustBePresent(SI_REF_SIR_PTR);
  public static final JsonContentCheck SIR_REQUIRED_IN_NOTIFICATION = JsonAttribute.mustBePresent(SI_NOTIFICATION_SIR_PTR);
  public static final JsonContentCheck TDR_REQUIRED_IN_NOTIFICATION = JsonAttribute.mustBePresent(TD_NOTIFICATION_TDR_PTR);

  public static JsonContentCheck sirInRefStatusMustMatchDSP(Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.mustEqual(SI_REF_SIR_PTR, () -> dspSupplier.get().shippingInstructionsReference());
  }

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
    if (!isTD) {
      checks.add(
        JsonAttribute.customValidator(
          "[Scenario] Verify that the correct 'commoditySubreference' is used",
          JsonAttribute.path("consignmentItems", checkCommoditySubreference(cspSupplier))));

      checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
          "[Scenario] CargoItem and DG vs. outerPackaging in the SI",
          (mav) -> mav.submitAllMatching("consignmentItems.*.cargoItems.*"),
          JsonAttribute.ifMatchedThenElse(
            (ignored) -> dspSupplier.get().scenarioType().hasDG(),
            JsonAttribute.path("outerPackaging", JsonAttribute.matchedMustBeAbsent()),
            JsonAttribute.path("outerPackaging", JsonAttribute.matchedMustBePresent())
          )
        )
      );
    }
    checks.add(JsonAttribute.customValidator(
      "[Scenario] Verify that the correct 'equipmentReference' values are used",
      JsonAttribute.path("utilizedTransportEquipments", checkEquipmentReference(cspSupplier))
    ));
    checks.add(JsonAttribute.customValidator(
      "[Scenario] Verify that the correct 'HSCodes' are used",
      JsonAttribute.path("consignmentItems", checkHSCodes(cspSupplier))
    ));
    checks.add(JsonAttribute.customValidator(
      "[Scenario] Verify that the correct 'descriptionOfGoods' is used",
      JsonAttribute.path("consignmentItems", checkDescriptionOfGoods(cspSupplier))
    ));

    checks.add(JsonAttribute.mustEqual(
      "[Scenario] Verify that the correct 'serviceContractReference' is used",
      "serviceContractReference",
      delayedValue(cspSupplier, CarrierScenarioParameters::serviceContractReference)
    ));
    checks.add(JsonAttribute.mustEqual(
      "[Scenario] Verify that the correct 'contractQuotationReference' is used",
      "contractQuotationReference",
      delayedValue(cspSupplier, CarrierScenarioParameters::contractQuotationReference)
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
      var allReferencesParents = nodeToValidate.findParents("customsReferences");
      for (var referencesParent : allReferencesParents) {
        if (isNonEmptyNode(referencesParent.path("customsReferences"))) {
          return Set.of();
        }
      }
      return Set.of("Expected 'customsReferences' to be used somewhere.");
    };
  }

  private static final JsonPointer[] REFERENCE_PATHS = {
    JsonPointer.compile("/references"),
    JsonPointer.compile("/documentParties/shipper/shippersReference"),
    JsonPointer.compile("/documentParties/shipper/shippersPurchaseOrderReference"),
    JsonPointer.compile("/documentParties/consignee/consigneesReference"),
    JsonPointer.compile("/documentParties/consignee/consigneesPurchaseOrderReference")
  };

  private static JsonContentMatchedValidation scenarioReferencesCheck(Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.ifMatchedThen(
      (ignored) -> dspSupplier.get().scenarioType().isReferencesRequired(),
      JsonAttribute.atLeastOneOfMatched((body, ptrs) -> {
        ptrs.addAll(Arrays.asList(REFERENCE_PATHS));
        var uteCount = body.path("utilizedTransportEquipments").size();
        for (int i = 0 ; i < uteCount ; i++) {
          ptrs.add(JsonPointer.compile("/utilizedTransportEquipments/%d/references".formatted(i)));
        }
        var ciCount = body.path("consignmentItems").size();
        for (int i = 0 ; i < ciCount ; i++) {
          ptrs.add(JsonPointer.compile("/consignmentItems/%d/references".formatted(i)));
        }
        var otherPartyCount = body.path("documentParties").path("other").size();
        for (int i = 0 ; i < otherPartyCount ; i++) {
          ptrs.add(JsonPointer.compile("/documentParties/other/%d/party/references".formatted(i)));
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
    Supplier<Map<String, String>> expectedValuesSupplier
  ) {
    return checkCSPValueBasedOnOtherValue(
      attributeName,
      referenceAttributeName,
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
        var refPath = pathBuilder.toString() + "." + referenceAttributeName;
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
        if (expectedReferenceValue == null) {
          issues.add("Unexpected %s '%s' at %s".formatted(attributeName, value, pathBuilder.toString()));
          continue;
        }
        var refNode = node.path(referenceAttributeName);
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
      var equipmentReferenceNode = ute.get("equipmentReference");
      if (equipmentReferenceNode != null) {
        pathBuilder.append(".equipmentReference");
      } else {
        pathBuilder.append(".equipment.equipmentReference");
        equipmentReferenceNode = ute.path("equipment").path("equipmentReference");
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
      return checkCSPAllUsedAtLeastOnce("equipmentReference", () -> expectedReferences, resolver)
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
    return checkCSPValueBasedOnOtherValue(
      "descriptionOfGoods",
      "commoditySubreference",
      expectedValueSupplier
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
      var hsCodes = consignmentItem.get("HSCodes");
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
    checks.add(VALIDATE_CONTRACT_REFERENCE);
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
      delayedValue(dspSupplier, (dsp) -> requestedAmendment ? dsp.updatedShippingInstructions() : dsp.shippingInstructions())
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

  public static Stream<ActionCheck> siRefStatusContentChecks(UUID matched, String standardsVersion, ShippingInstructionsStatus shippingInstructionsStatus, JsonContentCheck ... extraChecks) {
    return siRefStatusContentChecks(matched, standardsVersion, shippingInstructionsStatus, null, extraChecks);
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
    jsonContentChecks.add(VALIDATE_CONTRACT_REFERENCE);
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
        var activeReeferNode = nodeToValidate.path("activeReeferSettings");
        var nonOperatingReeferNode = nodeToValidate.path("isNonOperatingReefer");
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
    for (var ute : body.path("utilizedTransportEquipments")) {
      // TD or SI with SOC
      var ref = ute.path("equipment").path("equipmentReference").asText(null);
      if (ref == null) {
        // SI with COC
        ref = ute.path("equipmentReference").asText(null);
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
