package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standards.booking.party.CarrierScenarioParameters;
import org.dcsa.conformance.standards.booking.party.DynamicScenarioParameters;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@UtilityClass
public class BookingChecks {

  private static final JsonPointer CARRIER_BOOKING_REQUEST_REFERENCE = JsonPointer.compile("/carrierBookingRequestReference");
  private static final JsonPointer BOOKING_STATUS = JsonPointer.compile("/bookingStatus");
  private static final JsonPointer[] TD_UN_LOCATION_CODES = {
    JsonPointer.compile("/invoicePayableAt/UNLocationCode"),
    JsonPointer.compile("/transports/placeOfReceipt/UNLocationCode"),
    JsonPointer.compile("/transports/portOfLoading/UNLocationCode"),
    JsonPointer.compile("/transports/portOfDischarge/UNLocationCode"),
    JsonPointer.compile("/transports/placeOfDelivery/UNLocationCode"),
    JsonPointer.compile("/transports/onwardInlandRouting/UNLocationCode"),
  };

  public static ActionCheck bookingContentChecks(UUID matched, BookingState bookingState, Supplier<CarrierScenarioParameters> cspSupplier, Supplier<DynamicScenarioParameters> dspSupplier) {
    List<JsonContentCheck> jsonContentChecks = new ArrayList<>();

    return null;
  }
  private static final JsonContentCheck CHECK_EXPECTED_DEPARTURE_DATE = JsonAttribute.customValidator(
    "Check expected departure date can not be past date",
    (body) -> {
      String providedLocalDate = body.path("expectedDepartureDate").asText("");
      var invalidDates = new LinkedHashSet<String>();
      LocalDate providedExpectedDepartureDate = LocalDate.parse(providedLocalDate);
      if (providedExpectedDepartureDate.isBefore(LocalDate.now())) {
        invalidDates.add(providedLocalDate);
      }
      return invalidDates.stream()
        .map("The expected departure date  '%s' can not be paste date"::formatted)
        .collect(Collectors.toSet());
    }
  );

  private static final JsonContentCheck CHECK_EXPECTED_ARRIVAL_POD = JsonAttribute.customValidator(
    "Check expected arrival dates can valid",
    (body) -> {
      String providedArrivalStartDate = body.path("expectedArrivalAtPlaceOfDeliveryStartDate").asText("");
      String providedArrivalEndDate = body.path("expectedArrivalAtPlaceOfDeliveryEndDate").asText("");
      var invalidDates = new LinkedHashSet<String>();
      LocalDate arrivalStartDate = LocalDate.parse(providedArrivalStartDate);
      LocalDate arrivalEndDate = LocalDate.parse(providedArrivalEndDate);
      if (arrivalStartDate.isAfter(arrivalEndDate)) {
        invalidDates.add(arrivalStartDate.toString());
      }
      if (arrivalEndDate.isBefore(arrivalStartDate)) {
        invalidDates.add(arrivalEndDate.toString());
      }
      return invalidDates.stream()
        .map("The expected departure date  '%s' can not be past date"::formatted)
        .collect(Collectors.toSet());
    }
  );

  private static final Predicate<JsonNode> IS_ISO_EQUIPMENT_CONTAINER_REEFER = (uteNode) -> {
    var isoEquipmentNode = uteNode.path("ISOEquipmentCode");
    return isReeferContainerSizeTypeCode(isoEquipmentNode.asText(""));
  };
  private static final Predicate<JsonNode> HAS_ISO_EQUIPMENT_CODE = (reqEquipNode) -> {
    var isoEquipmentNode = reqEquipNode.path("ISOEquipmentCode");
    return isoEquipmentNode.isTextual();
  };
  private static final Predicate<JsonNode> IS_ACTIVE_REEFER_SETTINGS_REQUIRED = (reqEquipNode) -> {
    var norNode = reqEquipNode.path("isNonOperatingReefer");
    if (norNode.isMissingNode() || !norNode.isBoolean()) {
      // Only require the reefer if there is no equipment code or the equipment code is clearly a reefer.
      // Otherwise, we give conflicting results in some scenarios.
      return !HAS_ISO_EQUIPMENT_CODE.test(reqEquipNode) || IS_ISO_EQUIPMENT_CONTAINER_REEFER.test(reqEquipNode);
    }
    return !norNode.asBoolean(false);
  };

  private static final Consumer<MultiAttributeValidator> ALL_REQ_EQUIP = (mav) -> mav.submitAllMatching("requestedEquipments.*");
  private static final JsonContentCheck NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER = JsonAttribute.allIndividualMatchesMustBeValid(
    "All requested Equipments where 'isNonOperatingReefer' is 'false' must have 'activeReeferSettings'",
    ALL_REQ_EQUIP,
    JsonAttribute.ifMatchedThen(
      IS_ACTIVE_REEFER_SETTINGS_REQUIRED,
      JsonAttribute.path("activeReeferSettings", JsonAttribute.matchedMustBePresent())
    )
  );

  private static final JsonContentCheck IS_SHIPPER_OWNED_CONTAINER = JsonAttribute.customValidator(
    "valid Requested Equipments",
    (body) -> {
      var issues = new LinkedHashSet<String>();
      var requestedEquipments = body.path("requestedEquipments");
      for (var rq: requestedEquipments) {
        var isShipperOwned = rq.get("isShipperOwned").asBoolean(false);
        var commodities = requestedEquipments.get("commodities");
        if (isShipperOwned) {
          for (var commodity : commodities) {
            var cargoGrossWeight = commodity.get("cargoGrossWeight");
            var cargoGrossWeightUnit = commodity.get("cargoGrossWeightUnit");
            var cargoGrossVolume = commodity.get("cargoGrossVolume");
              var cargoGrossVolumeUnit = commodity.get("cargoGrossVolumeUnit");
            if (cargoGrossWeight == null || cargoGrossWeight.isEmpty()) {
              issues.add("the container weight be provided");
            }
            if(cargoGrossWeightUnit == null || cargoGrossWeightUnit.isEmpty()) {
              issues.add("the container weight unit must be provided");
            }
            if(cargoGrossVolume != null && (cargoGrossVolumeUnit  == null || cargoGrossVolumeUnit.isEmpty()) ) {
              issues.add("the cargo gross volume unit must be provided");
            }
          }
        }
      }
      return issues;
    }
  );

  private static final JsonContentCheck COND_CARRIER_VOYAGE_NUMBER = JsonAttribute.customValidator(
    "Conditional Carrier Export Voyage number",
    (body) -> {
      var expectedDepartureDate = body.path("expectedDepartureDate");
      var expectedArrivalStartDate = body.path("expectedArrivalAtPlaceOfDeliveryStartDate");
      var expectedArrivalEndDate = body.path("expectedArrivalAtPlaceOfDeliveryStartDate");
      var carrierExportVoyageNumber = body.path("carrierExportVoyageNumber");
      var issues = new LinkedHashSet<String>();

      if (carrierExportVoyageNumber == null || (expectedDepartureDate == null || ( expectedArrivalEndDate == null && expectedArrivalStartDate == null)))  {
        issues.add("The carrierExportVoyageNumber must be present as either expectedDepartureDate or expectedArrivalDates are not present");
      }

      return issues;
    }
  );

  private static final JsonContentCheck UNIVERSAL_SERVICE_REFERENCE = JsonAttribute.customValidator(
    "Conditional Universal Service Reference",
    (body) -> {
      var expectedDepartureDate = body.path("universalExportVoyageReference");
      var expectedArrivalDate = body.path("expectedArrivalDate");
      var carrierExportVoyageNumber = body.path("carrierExportVoyageNumber");
      var issues = new LinkedHashSet<String>();

      if (carrierExportVoyageNumber == null && (expectedDepartureDate == null || expectedArrivalDate == null))  {
        issues.add("The carrierExportVoyageNumber must be present as either expectedDepartureDate or expectedArrivalDate are not present");
      }

      return issues;
    }
  );

  private static final JsonContentCheck REFERENCE_TYPE_VALIDATION = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate reference type field",
    (mav) -> {
      mav.submitAllMatching("requestedEquipments.*.references.*.type");
        mav.submitAllMatching("requestedEquipments.*.commodities.*.references.*.type");
    },
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.REFERENCE_TYPES)
  );

  private static final JsonContentCheck TLR_CC_T_COMBINATION_VALIDATIONS = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate combination of 'countryCode' and 'type' in 'taxAndLegalReferences'",
    (mav) -> {
      mav.submitAllMatching("documentParties.*.party.taxLegalReferences.*");
    },
    combineAndValidateAgainstDataset(BookingDataSets.LTR_CC_T_COMBINATIONS, "countryCode", "type")
  );

  private static final JsonContentCheck ISO_EQUIPMENT_CODE_VALIDATION = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate ISO Equipment code",
    (mav) -> {
      mav.submitAllMatching("requestedEquipments.*.ISOEquipmentCode");
    },
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.ISO_6346_CONTAINER_CODES)
  );

  private static final JsonContentCheck OUTER_PACKAGING_CODE_IS_VALID = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate that 'packagingCode' is a known code",
    (mav) -> mav.submitAllMatching("consignmentItems.*.cargoItems.*.outerPackaging.packageCode"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.OUTER_PACKAGING_CODE)
  );

  private static Consumer<MultiAttributeValidator> allDg(Consumer<MultiAttributeValidator.AttributePathBuilder> consumer) {
    return (mav) -> consumer.accept(mav.path("requestedEquipments").all().path("commodities").all().path("outerPackaging").path("dangerousGoods").all());
  }
  private static final JsonContentCheck IS_EXPORT_DECLARATION_REFERENCE_PRESENT = JsonAttribute.ifThen(
    "Check Export declaration reference ",
    JsonAttribute.isTrue(JsonPointer.compile("/isExportDeclarationRequired")),
    JsonAttribute.mustBePresent(JsonPointer.compile("/exportDeclarationReference"))
  );

  private static final JsonContentCheck IS_IMPORT_DECLARATION_REFERENCE_PRESENT = JsonAttribute.ifThen(
    "Check Import declaration reference presence",
    JsonAttribute.isTrue(JsonPointer.compile("/isImportLicenseRequired")),
    JsonAttribute.mustBePresent(JsonPointer.compile("/importLicenseReference"))
  );
  private static final List<JsonContentCheck> STATIC_BOOKING_CHECKS = Arrays.asList(
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/cargoMovementTypeAtOrigin"), BookingDataSets.CARGO_MOVEMENT_TYPE),
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/cargoMovementTypeAtDestination"), BookingDataSets.CARGO_MOVEMENT_TYPE),
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/communicationChannelCode"), BookingDataSets.COMMUNICATION_CHANNEL_CODES),
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/declaredValueCurrency"), BookingDataSets.ISO_4217_CURRENCY_CODES),
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/incoTerms"), BookingDataSets.INCO_TERMS_VALUES),
    JsonAttribute.atMostOneOf(
      JsonPointer.compile("/shippedOnBoardDate"),
      JsonPointer.compile("/receivedForShipmentDate")
    ),
    CHECK_EXPECTED_DEPARTURE_DATE,
    CHECK_EXPECTED_ARRIVAL_POD,
    NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER,
    IS_SHIPPER_OWNED_CONTAINER,
    REFERENCE_TYPE_VALIDATION,
    ISO_EQUIPMENT_CODE_VALIDATION,
    IS_EXPORT_DECLARATION_REFERENCE_PRESENT,
    IS_IMPORT_DECLARATION_REFERENCE_PRESENT,
    OUTER_PACKAGING_CODE_IS_VALID,
    COND_CARRIER_VOYAGE_NUMBER,
    TLR_CC_T_COMBINATION_VALIDATIONS,
    JsonAttribute.allIndividualMatchesMustBeValid(
      "DangerousGoods implies packagingCode or imoPackagingCode",
      mav -> mav.submitAllMatching("requestedEquipments.*.commodities.*.outerPackaging"),
      (nodeToValidate, contextPath) -> {
        var dg = nodeToValidate.path("dangerousGoods");
        if (!dg.isArray() || dg.isEmpty()) {
          return Set.of();
        }
        if (nodeToValidate.path("packageCode").isMissingNode() && nodeToValidate.path("imoPackagingCode").isMissingNode()) {
          return Set.of("The '%s' object did not have a 'packageCode' nor an 'imoPackagingCode', which is required due to dangerousGoods"
            .formatted(contextPath));
        }
        if (nodeToValidate.path("numberOfPackages").isMissingNode() && nodeToValidate.path("description").isMissingNode()) {
          return Set.of("The '%s' object did not have a 'numberOfPackages' nor an 'description', which is required due to dangerousGoods"
            .formatted(contextPath));
        }
        return Set.of();
      }
    ),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'imoClass' values must be from dataset",
      allDg((dg) -> dg.path("imoClass").submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.DG_IMO_CLASSES)
    ),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'segregationGroups' values must be from dataset",
      allDg((dg) -> dg.path("segregationGroups").all().submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.DG_SEGREGATION_GROUPS)
    ),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'inhalationZone' values must be from dataset",
      allDg((dg) -> dg.path("inhalationZone").all().submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.INHALATION_ZONE_CODE)
    ),
    JsonAttribute.allOrNoneArePresent(
      JsonPointer.compile("/declaredValue"),
      JsonPointer.compile("/declaredValueCurrency")
    )
    );



  public static ActionCheck siResponseContentChecks(UUID matched, Supplier<CarrierScenarioParameters> cspSupplier, Supplier<DynamicScenarioParameters> dspSupplier, BookingState bookingStatus, BookingState amendedBookingState) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(JsonAttribute.mustEqual(
      CARRIER_BOOKING_REQUEST_REFERENCE,
      () -> dspSupplier.get().carrierBookingRequestReference()
    ));
    checks.add(JsonAttribute.mustEqual(
      BOOKING_STATUS,
      bookingStatus.wireName()
    ));
    checks.addAll(STATIC_BOOKING_CHECKS);
    return JsonAttribute.contentChecks(
      BookingRole::isCarrier,
      matched,
      HttpMessageType.RESPONSE,
      checks
    );
  }

  private static JsonContentMatchedValidation combineAndValidateAgainstDataset(
    KeywordDataset dataset,
    String nameA,
    String nameB
  ) {
    return (nodeToValidate, contextPath) -> {
      var codeA = nodeToValidate.path(nameA).asText("");
      var codeB = nodeToValidate.path(nameB).asText("");
      var combined = codeA + "/" + codeB;
      if (!dataset.contains(combined)) {
        return Set.of(
          "The combination of '%s' ('%s') and '%s' ('%s') used in '%s' is not known to be a valid combination.".
            formatted(codeA, nameA, codeB, nameB, contextPath)
        );
      }
      return Set.of();
    };
  }

  private boolean isReeferContainerSizeTypeCode(String isoEquipmentCode) {
    var codeChar = isoEquipmentCode.length() > 2 ? isoEquipmentCode.charAt(2) : '?';
    return codeChar == 'R' || codeChar == 'H';
  }
}


