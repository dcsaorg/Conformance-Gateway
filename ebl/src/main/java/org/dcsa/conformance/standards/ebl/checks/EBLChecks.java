package org.dcsa.conformance.standards.ebl.checks;

import com.fasterxml.jackson.core.JsonPointer;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
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
    if (uteNode.path("isNonOperatingReefer").asBoolean(false)) {
      return false;
    }
    // Only require the reefer if there is no equipment code or the equipment code is clearly a reefer.
    // Otherwise, we give conflicting results in some scenarios.
    return !HAS_ISO_EQUIPMENT_CODE.test(uteNode) || IS_ISO_EQUIPMENT_CONTAINER_REEFER.test(uteNode);
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

  private static Consumer<MultiAttributeValidator> allDg(Consumer<MultiAttributeValidator.AttributePathBuilder> consumer) {
    return (mav) -> consumer.accept(mav.path("consignmentItems").all().path("cargoItems").all().path("outerPackaging").path("dangerousGoods").all());
  }

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
    ISO_EQUIPMENT_CODE_IMPLIES_REEFER
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
      "The 'imoClass' values must be from dataset",
      allDg((dg) -> dg.path("imoClass").submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.DG_IMO_CLASSES)
    ),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'inhalationZone' values must be from dataset",
      allDg((dg) -> dg.path("inhalationZone").submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.DG_INHALATIONZONES)
    ),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'segregationGroups' values must be from dataset",
      allDg((dg) -> dg.path("segregationGroups").all().submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(EblDatasets.DG_SEGREGATION_GROUPS)
    )
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

    return JsonAttribute.contentChecks(
      EblRole::isCarrier,
      matched,
      HttpMessageType.RESPONSE,
      jsonContentChecks
    );
  }


  private boolean isReeferContainerSizeTypeCode(String isoEquipmentCode) {
    // DT-437
    var codeChar = isoEquipmentCode.length() > 2 ? isoEquipmentCode.charAt(2) : '?';
    return codeChar == 'R' || codeChar == 'H';
  }
}
