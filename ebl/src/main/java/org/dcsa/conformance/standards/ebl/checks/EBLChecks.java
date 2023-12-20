package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.ebl.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

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

  private static final JsonPointer[] TD_UN_LOCATION_CODES = {
    JsonPointer.compile("/invoicePayableAt/UNLocationCode"),
    JsonPointer.compile("/transports/placeOfReceipt/UNLocationCode"),
    JsonPointer.compile("/transports/portOfLoading/UNLocationCode"),
    JsonPointer.compile("/transports/portOfDischarge/UNLocationCode"),
    JsonPointer.compile("/transports/placeOfDelivery/UNLocationCode"),
    JsonPointer.compile("/transports/onwardInlandRouting/UNLocationCode"),
  };

  private static final JsonContentCheck ONLY_EBLS_CAN_BE_NEGOTIABLE = JsonAttribute.ifThen(
    "Validate transportDocumentTypeCode vs. isToOrder",
    JsonAttribute.isTrue(JsonPointer.compile("/isToOrder")),
    JsonAttribute.mustEqual(JsonPointer.compile("/transportDocumentTypeCode"), "BOL")
  );

  private static final Consumer<MultiAttributeValidator> ALL_REFERENCE_TYPES = (mav) -> {
    mav.path("references").all().path("type").submitPath();
    mav.path("utilizedTransportEquipments").all().path("references").all().path("type").submitPath();
    mav.path("consignmentItems").all().path("references").all().path("type").submitPath();
  };

  private static final JsonContentCheck VALID_REFERENCE_TYPES = JsonAttribute.allIndividualMatchesMustBeValid(
    "All reference 'type' fields must be valid",
        ALL_REFERENCE_TYPES,
        JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.REFERENCE_TYPE)
  );


  private static final Consumer<MultiAttributeValidator> ALL_UTE = (mav) -> mav.path("utilizedTransportEquipments").all().submitPath();

  private static final Predicate<JsonNode> HAS_ISO_EQUIPMENT_CODE = (uteNode) -> {
    var isoEquipmentNode = uteNode.path("equipment").path("ISOEquipmentCode");
    return isoEquipmentNode.isTextual();
  };

  private static final Predicate<JsonNode> IS_ISO_EQUIPMENT_CONTAINER_REEFER = (uteNode) -> {
    var isoEquipmentNode = uteNode.path("equipment").path("ISOEquipmentCode");
    return isReeferContainerSizeTypeCode(isoEquipmentNode.asText(""));
  };

  private static final Predicate<JsonNode> IS_ACTIVE_REEFER_SETTINGS_REQUIRED = (uteNode) -> {
    var norNode = uteNode.path("isNonOperatingReefer");
    if (norNode.isMissingNode() || !norNode.isBoolean()) {
      // Only require the reefer if there is no equipment code or the equipment code is clearly a reefer.
      // Otherwise, we give conflicting results in some scenarios.
      return !HAS_ISO_EQUIPMENT_CODE.test(uteNode) || IS_ISO_EQUIPMENT_CONTAINER_REEFER.test(uteNode);
    }
    return norNode.asBoolean(false);
  };

  private static final JsonContentCheck ISO_EQUIPMENT_CODE_IMPLIES_REEFER = JsonAttribute.allIndividualMatchesMustBeValid(
    "All utilizedTransportEquipments with a reefer ISO Equipment Code must have at least isNonOperatingReefer",
    ALL_UTE,
    JsonAttribute.ifMatchedThen(
      HAS_ISO_EQUIPMENT_CODE,
      JsonAttribute.ifMatchedThenElse(
        IS_ISO_EQUIPMENT_CONTAINER_REEFER,
        JsonAttribute.path("isNonOperatingReefer", JsonAttribute.matchedMustBePresent()),
        JsonAttribute.combine(
          JsonAttribute.path("isNonOperatingReefer", JsonAttribute.matchedMustBeAbsent()),
          JsonAttribute.path("activeReeferSettings", JsonAttribute.matchedMustBeAbsent())
        )
      )
    )
  );

  private static final JsonContentCheck NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER = JsonAttribute.allIndividualMatchesMustBeValid(
    "All utilizedTransportEquipments where 'isNonOperatingReefer' is 'false' must have 'activeReeferSettings'",
    ALL_UTE,
    JsonAttribute.ifMatchedThen(
      IS_ACTIVE_REEFER_SETTINGS_REQUIRED,
      JsonAttribute.path("activeReeferSettings", JsonAttribute.matchedMustBePresent())
    )
  );

  private static final JsonContentCheck NOR_IS_TRUE_IMPLIES_NO_ACTIVE_REEFER = JsonAttribute.allIndividualMatchesMustBeValid(
    "All utilizedTransportEquipments where 'isNonOperatingReefer' is 'true' cannot have 'activeReeferSettings'",
    ALL_UTE,
    JsonAttribute.ifMatchedThen(
      JsonAttribute.isTrue("isNonOperatingReefer"),
      JsonAttribute.path("activeReeferSettings", JsonAttribute.matchedMustBeAbsent())
    )
  );

  private static final Consumer<MultiAttributeValidator> ALL_AMF = (mav) -> mav.path("advanceManifestFilings").all().submitPath();
  private static final JsonContentCheck AMF_SELF_FILER_CODE_CONDITIONALLY_MANDATORY = JsonAttribute.allIndividualMatchesMustBeValid(
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

  private static final JsonContentCheck AMF_CC_MTC_COMBINATION_VALIDATIONS = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate combination of 'countryCode' and 'manifestTypeCode' in 'advanceManifestFilings'",
    ALL_AMF,
    (nodeToValidate, contextPath) -> {
      var country = nodeToValidate.path("countryCode").asText("");
      var manifestTypeCode = nodeToValidate.path("manifestTypeCode").asText("");
      var combined = country + "/" + manifestTypeCode;
      // This check doubles as a check of country being a valid country (DT-612) as the combination only
      // includes a very small subset of valid countries.  If you find yourself adding a "this manifestTypeCode
      // applies to *all* countries", then you need to add a separate validation that the country code is in
      // valid (in https://www.iso.org/obp/ui/#iso:pub:PUB500001:en)
      if (!EblDatasets.AMF_CC_MTC_COMBINATIONS.contains(combined)) {
        return Set.of(
          "The combination of '%s' ('countryCounty') and '%s' ('manifestTypeCode') used in '%s' is not known to be a valid combination.".
            formatted(country, manifestTypeCode, contextPath)
        );
      }
      return Set.of();
    });


  private static final JsonContentCheck VALIDATE_DOCUMENT_PARTIES = JsonAttribute.customValidator(
    "Validate documentParties",
    body -> {
      var issues = new LinkedHashSet<String>();
      var documentParties = body.path("documentParties");
      var isToOrder = body.path("isToOrder").asBoolean(false);
      var partyFunctions = StreamSupport.stream(documentParties.spliterator(), false)
        .map(p -> p.path("partyFunction"))
        .filter(JsonNode::isTextual)
        .map(n -> n.asText(""))
        .collect(Collectors.toSet());

      if (!partyFunctions.contains("OS")) {
        issues.add("The 'OS' party is mandatory in the eBL phase (SI/TD)");
      }

      if (isToOrder) {
        if (partyFunctions.contains("CN")) {
          issues.add("The 'CN' party cannot be used when 'isToOrder' is true (use 'END' instead)");
        }
      } else {
        if (!partyFunctions.contains("CN")) {
          issues.add("The 'CN' party is mandatory when 'isToOrder' is false");
        }
        if (partyFunctions.contains("END")) {
          issues.add("The 'END' party cannot be used when 'isToOrder' is false");
        }
      }

      if (!partyFunctions.contains("SCO") && !body.path("serviceContractReference").isTextual()) {
        issues.add("The 'SCO' party is mandatory when 'serviceContractReference' is absent");
      }
      return issues;
    }
  );

  private static Consumer<MultiAttributeValidator> allDg(Consumer<MultiAttributeValidator.AttributePathBuilder> consumer) {
    return (mav) -> consumer.accept(mav.path("consignmentItems").all().path("cargoItems").all().path("outerPackaging").path("dangerousGoods").all());
  }

  private static final JsonContentCheck CARGO_ITEM_REFERENCES_KNOWN_EQUIPMENT = JsonAttribute.customValidator(
    "Equipment References in 'cargoItems' must be present in 'utilizedTransportEquipments'",
    (body) -> {
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
      return missing.stream()
        .map("The equipment reference '%s' was used in a cargoItem but was not present in 'utilizedTransportEquipments'"::formatted)
        .collect(Collectors.toSet());
    }
  );

  private static final JsonContentCheck UTE_EQUIPMENT_REFERENCE_UNIQUE = JsonAttribute.customValidator(
    "Equipment References in 'utilizedTransportEquipments' must be unique",
    (body) -> {
      var duplicates = new LinkedHashSet<String>();
      allEquipmentReferences(body, duplicates);
      return duplicates.stream()
        .map("The equipment reference '%s' was used more than once in 'utilizedTransportEquipments'"::formatted)
        .collect(Collectors.toSet());
    }
  );

  private static final JsonContentCheck ADVANCED_MANIFEST_FILING_CODES_UNIQUE = JsonAttribute.customValidator(
    "The combination of 'countryCode' and 'manifestTypeCode' in 'advanceManifestFilings' must be unique",
    (body) -> {
      var seen = new HashSet<String>();
      var duplicates = new LinkedHashSet<String>();
      for (var amf : body.path("advanceManifestFilings")) {
        var cc = amf.path("countryCode").asText(null);
        var mtc = amf.path("manifestTypeCode").asText(null);
        if (cc == null || mtc == null) {
          continue;
        }
        var combined = cc + "/" + mtc;
        if (!seen.add(combined)) {
          duplicates.add(combined);
        }
      }
      return duplicates.stream()
        .map("The countryCode/manifestTypeCode combination '%s' was used more than once in 'advanceManifestFilings'"::formatted)
        .collect(Collectors.toSet());
    }
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
    JsonAttribute.ifThen(
      "'isElectronic' implies 'sendToPlatform'",
      JsonAttribute.isTrue(JsonPointer.compile("/isElectronic")),
      JsonAttribute.mustBePresent(JsonPointer.compile("/sendToPlatform"))
    ),
    VALID_REFERENCE_TYPES,
    ISO_EQUIPMENT_CODE_IMPLIES_REEFER,
    UTE_EQUIPMENT_REFERENCE_UNIQUE,
    CARGO_ITEM_REFERENCES_KNOWN_EQUIPMENT,
    ADVANCED_MANIFEST_FILING_CODES_UNIQUE,
    AMF_CC_MTC_COMBINATION_VALIDATIONS,
    AMF_SELF_FILER_CODE_CONDITIONALLY_MANDATORY,
    VALIDATE_DOCUMENT_PARTIES
  );

  private static final List<JsonContentCheck> STATIC_TD_CHECKS = Arrays.asList(
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
      "DangerousGoods implies packagingCode or imoPackagingCode",
      mav -> mav.path("consignmentItems").all().path("cargoItems").all().path("outerPackaging").submitPath(),
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
    CARGO_ITEM_REFERENCES_KNOWN_EQUIPMENT,
    ADVANCED_MANIFEST_FILING_CODES_UNIQUE,
    AMF_CC_MTC_COMBINATION_VALIDATIONS,
    AMF_SELF_FILER_CODE_CONDITIONALLY_MANDATORY,
    VALIDATE_DOCUMENT_PARTIES
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


  public static ActionCheck siRequestContentChecks(UUID matched) {
    return JsonAttribute.contentChecks(
      EblRole::isShipper,
      matched,
      HttpMessageType.REQUEST,
      STATIC_SI_CHECKS
    );
  }

  public static ActionCheck siResponseContentChecks(UUID matched, Supplier<DynamicScenarioParameters> dspSupplier, ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus updatedShippingInstructionsStatus) {
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
    return JsonAttribute.contentChecks(
      EblRole::isCarrier,
      matched,
      HttpMessageType.RESPONSE,
      checks
    );
  }

  public static Stream<ActionCheck> siRefStatusContentChecks(UUID matched, ShippingInstructionsStatus shippingInstructionsStatus, JsonContentCheck ... extraChecks) {
    return siRefStatusContentChecks(matched, shippingInstructionsStatus, null, extraChecks);
  }

  public static Stream<ActionCheck> siRefStatusContentChecks(UUID matched, ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus updatedShippingInstructionsStatus, JsonContentCheck ... extraChecks) {
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
        checks
      )
    );
  }

  public static ActionCheck tdRefStatusChecks(UUID matched, Supplier<DynamicScenarioParameters> dspSupplier, TransportDocumentStatus transportDocumentStatus) {
    return JsonAttribute.contentChecks(
      EblRole::isCarrier,
      matched,
      HttpMessageType.RESPONSE,
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

  public static ActionCheck siNotificationContentChecks(UUID matched, ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus updatedShippingInstructionsStatus, JsonContentCheck ... extraChecks) {
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
      jsonContentChecks
    );
  }

  public static ActionCheck tdNotificationContentChecks(UUID matched, TransportDocumentStatus transportDocumentStatus, JsonContentCheck ... extraChecks) {
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
      jsonContentChecks
    );
  }

  public static ActionCheck tdContentChecks(UUID matched, TransportDocumentStatus transportDocumentStatus, Supplier<DynamicScenarioParameters> dspSupplier) {
    List<JsonContentCheck> jsonContentChecks = new ArrayList<>();
    jsonContentChecks.add(JsonAttribute.mustEqual(
      TD_TDR,
      () -> dspSupplier.get().transportDocumentReference()
    ));
    jsonContentChecks.add(JsonAttribute.mustEqual(
      TD_TRANSPORT_DOCUMENT_STATUS,
      transportDocumentStatus.wireName()
    ));
    jsonContentChecks.addAll(STATIC_TD_CHECKS);
    for (var ptr : TD_UN_LOCATION_CODES) {
      jsonContentChecks.add(JsonAttribute.mustBeDatasetKeywordIfPresent(ptr, EblDatasets.UN_LOCODE_DATASET));
    }
    jsonContentChecks.add(JsonAttribute.allIndividualMatchesMustBeValid(
      "[Scenario] Whether the containers should have active reefer",
      mav-> mav.path("utilizedTransportEquipments").all().path("activeReeferSettings").submitPath(),
      (nodeToValidate, contextPath) -> {
        var scenario = dspSupplier.get().scenarioType();
        if (scenario == ScenarioType.REEFER) {
          if (!nodeToValidate.isObject()) {
            return Set.of("The scenario requires '%s' to have an active reefer".formatted(contextPath));
          }
        } else {
          if (!nodeToValidate.isMissingNode()) {
            return Set.of("The scenario requires '%s' to NOT have an active reefer".formatted(contextPath));
          }
        }
        return Set.of();
      }
    ));
    jsonContentChecks.add(JsonAttribute.allIndividualMatchesMustBeValid(
      "[Scenario] Whether the cargo should be DG",
      mav-> mav.path("consignmentItems").all().path("cargoItems").all().path("outerPackaging").path("dangerousGoods").submitPath(),
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
    return JsonAttribute.contentChecks(
      EblRole::isCarrier,
      matched,
      HttpMessageType.RESPONSE,
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
