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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.dcsa.conformance.standards.booking.checks.BookingDataSets.CUTOFF_DATE_TIME_CODES;

@UtilityClass
public class BookingChecks {

  private static final Set<BookingState> CONFIRMED_BOOKING_STATES = Set.of(
    BookingState.CONFIRMED,
    BookingState.PENDING_AMENDMENT,
    BookingState.COMPLETED,
    BookingState.DECLINED
  );

  private static final Set<BookingState> PENDING_CHANGES_STATES = Set.of(
    BookingState.PENDING_UPDATE,
    BookingState.PENDING_AMENDMENT
  );

  private static final Set<BookingState> REASON_STATES = Set.of(
    BookingState.DECLINED,
    BookingState.REJECTED,
    BookingState.CANCELLED,
    BookingState.AMENDMENT_CANCELLED,
    BookingState.AMENDMENT_DECLINED
  );

  private static final Set<BookingState> BOOKING_STATES_WHERE_CBR_IS_OPTIONAL = Set.of(
    BookingState.RECEIVED,
    BookingState.REJECTED,
    BookingState.PENDING_UPDATE,
    BookingState.UPDATE_RECEIVED,
    /* CANCELLED depends on whether cancel happens before CONFIRMED, but the logic does not track prior
     * states. Therefore, we just assume it is optional in CANCELLED here.
     */
    BookingState.CANCELLED
  );
  private static final JsonPointer CARRIER_BOOKING_REQUEST_REFERENCE = JsonPointer.compile("/carrierBookingRequestReference");
  private static final JsonPointer CARRIER_BOOKING_REFERENCE = JsonPointer.compile("/carrierBookingReference");
  private static final JsonPointer BOOKING_STATUS = JsonPointer.compile("/bookingStatus");
  private static final JsonPointer[] TD_UN_LOCATION_CODES = {
    JsonPointer.compile("/invoicePayableAt/UNLocationCode"),
    JsonPointer.compile("/shipmentLocations/location/UNLocationCode"),
    JsonPointer.compile("/shipmentLocations/location/facilityLocation/UNLocationCode")
  };

  public static ActionCheck requestContentChecks(UUID matched, Supplier<CarrierScenarioParameters> cspSupplier, Supplier<DynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<>(STATIC_BOOKING_CHECKS);
    generateScenarioRelatedChecks(checks, cspSupplier, dspSupplier);
    return JsonAttribute.contentChecks(
      BookingRole::isShipper,
      matched,
      HttpMessageType.REQUEST,
      checks
    );
  }
  private static final JsonContentCheck CHECK_EXPECTED_DEPARTURE_DATE = JsonAttribute.customValidator(
    "Check expected departure date can not be past date",
    (body) -> {
      String expectedDepartureDate = body.path("expectedDepartureDate").asText("");
      var invalidDates = new LinkedHashSet<String>();
      if(!expectedDepartureDate.isEmpty()) {
        LocalDate expectedDepartureLocalDate = LocalDate.parse(expectedDepartureDate);
        if (expectedDepartureLocalDate.isBefore(LocalDate.now())) {
          invalidDates.add(expectedDepartureLocalDate.toString());
        }
      }
      return invalidDates.stream()
        .map("The expected departure date '%s' can not be past date"::formatted)
        .collect(Collectors.toSet());
    }
  );

  private static final JsonContentCheck CHECK_EXPECTED_ARRIVAL_POD = JsonAttribute.customValidator(
    "Check expected arrival dates are valid",
    (body) -> {
      String providedArrivalStartDate = body.path("expectedArrivalAtPlaceOfDeliveryStartDate").asText("");
      String providedArrivalEndDate = body.path("expectedArrivalAtPlaceOfDeliveryEndDate").asText("");
      var invalidDates = new LinkedHashSet<String>();
      if (!providedArrivalStartDate.isEmpty() && !providedArrivalEndDate.isEmpty()) {
        LocalDate arrivalStartDate = LocalDate.parse(providedArrivalStartDate);
        LocalDate arrivalEndDate = LocalDate.parse(providedArrivalEndDate);
        if (arrivalStartDate.isAfter(arrivalEndDate)) {
          invalidDates.add(arrivalStartDate.toString());
        }
        if (arrivalEndDate.isBefore(arrivalStartDate)) {
          invalidDates.add(arrivalEndDate.toString());
        }
      }
      return invalidDates.stream()
        .map("The expected arrival dates  '%s' are valid"::formatted)
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
    JsonAttribute.combineAndValidateAgainstDataset(BookingDataSets.LTR_CC_T_COMBINATIONS, "countryCode", "type")
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
    (mav) -> mav.submitAllMatching("requestedEquipments.*.commodities.*.outerPackaging.packageCode"),
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

  private static final JsonContentCheck DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE = JsonAttribute.customValidator(
    "Each document party can be used at most once",
    JsonAttribute.path(
      "documentParties",
      JsonAttribute.unique("partyFunction")
    ));

  private static final JsonContentCheck COMMODITIES_SUBREFERENCE_UNIQUE = JsonAttribute.allIndividualMatchesMustBeValid(
    "Each Subreference in commodities must be unique",
    (mav) -> {
      mav.submitAllMatching("requestedEquipments.*.commodities");
    },
    JsonAttribute.unique("subReference")
  );

  private static final JsonContentCheck VALIDATE_ALLOWED_SHIPMENT_CUTOFF_CODE = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate allowed shipment cutoff codes",
    (mav) -> mav.submitAllMatching("shipmentCutOffTimes.*.cutOffDateTimeCode"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.CUTOFF_DATE_TIME_CODES)
  );

  private static final JsonContentCheck VALIDATE_SHIPMENT_CUTOFF_TIME_CODE = JsonAttribute.customValidator(
    "validate shipment cutOff Date time code",
    (body) -> {
      var shipmentCutOffTimes = body.path("shipmentCutOffTimes");
      var receiptTypeAtOrigin = body.path("receiptTypeAtOrigin").asText("");
      var issues = new LinkedHashSet<String>();
      var cutOffDateTimeCodes = StreamSupport.stream(shipmentCutOffTimes.spliterator(), false)
        .map(p -> p.path("cutOffDateTimeCode"))
        .filter(JsonNode::isTextual)
        .map(n -> n.asText(""))
        .collect(Collectors.toSet());
      if (receiptTypeAtOrigin.equals("CFS")) {
        if (cutOffDateTimeCodes.contains("LCL")) {
          issues.add("For ReceiptTypeAtOrigin as CFS, the shipment cut off date time must have only LCL");
        }
      }
      return issues;
    }
  );

  private static final Consumer<MultiAttributeValidator> ALL_AMF = (mav) -> mav.submitAllMatching("advanceManifestFilings.*");

  private static final JsonContentCheck ADVANCED_MANIFEST_FILING_CODES_UNIQUE = JsonAttribute.allIndividualMatchesMustBeValid(
    "The combination of 'countryCode' and 'manifestTypeCode' in 'advanceManifestFilings' must be unique",
    ALL_AMF,
    JsonAttribute.unique("countryCode", "manifestTypeCode")
  );
  private static final Consumer<MultiAttributeValidator> ALL_SHIPMENT_CUTOFF_TIMES = (mav) -> mav.submitAllMatching("shipmentCutOffTimes.*");

  private static final JsonContentCheck SHIPMENT_CUTOFF_TIMES_UNIQUE = JsonAttribute.allIndividualMatchesMustBeValid(
    "in 'shipmentCutOfftimes' cutOff date time code must be unique",
    ALL_SHIPMENT_CUTOFF_TIMES,
    JsonAttribute.unique("cutOffDateTimeCode")
  );
  private static final JsonContentCheck AMF_CC_MTC_COMBINATION_VALIDATIONS = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate combination of 'countryCode' and 'manifestTypeCode' in 'advanceManifestFilings'",
    ALL_AMF,
    JsonAttribute.combineAndValidateAgainstDataset(BookingDataSets.AMF_CC_MTC_COMBINATIONS, "countryCode", "manifestTypeCode")
  );

  private static final JsonContentCheck VALIDATE_SHIPMENT_LOCATIONS = JsonAttribute.customValidator(
    "Validate shipmentLocations",
    body -> {
      var issues = new LinkedHashSet<String>();
      var shipmentLocations = body.path("shipmentLocations");
      var receiptTypeAtOrigin = body.path("receiptTypeAtOrigin").asText("");
      var polNode =
        StreamSupport.stream(shipmentLocations.spliterator(), false)
          .filter(o ->  o.path("locationTypeCode").asText("").equals("POL")
            || o.path("locationTypeCode").asText("").equals("PDE") )
          .findFirst()
          .orElse(null);
      var podNode =
        StreamSupport.stream(shipmentLocations.spliterator(), false)
          .filter(o -> o.path("locationTypeCode").asText("").equals("POD")
          || o.path("locationTypeCode").asText("").equals("PRE") )
          .findFirst()
          .orElse(null);

      var ielNode =
        StreamSupport.stream(shipmentLocations.spliterator(), false)
          .filter(o ->  o.path("locationTypeCode").asText("").equals("IEL"))
          .findFirst()
          .orElse(null);

      if (polNode == null || polNode.isEmpty() ) {
        issues.add("Port of Discharge/Place of Delivery value must be provided");
      }
      if (podNode == null || podNode.isEmpty()) {
        issues.add("Port of Load/Place of Receipt values must be provided");
      }
      if(!"SD".equals(receiptTypeAtOrigin) && ielNode != null) {
        issues.add("Container intermediate export stop-off location should not be provided");
      }

      return issues;
    });


  private static final JsonContentCheck REQUESTED_CHANGES_PRESENCE = JsonAttribute.customValidator(
    "Requested changes must be present for the selected Booking Status ",
    (body) -> {
      var bookingStatus = body.path("bookingStatus").asText("");
      var issues = new LinkedHashSet<String>();
      if (PENDING_CHANGES_STATES.contains(BookingState.fromWireName(bookingStatus))) {
        var requestedChanges = body.get("requestedChanges");
        if (requestedChanges == null) {
          issues.add("requestedChanges is missing in allowed booking states %s".formatted(PENDING_CHANGES_STATES));
        }
      }
      return issues;
    }
  );

  private static final JsonContentCheck REASON_PRESENCE = JsonAttribute.customValidator(
    "Reason field must be present for the selected Booking Status",
    (body) -> {
      var bookingStatus = body.path("bookingStatus").asText("");
      var issues = new LinkedHashSet<String>();
      if (REASON_STATES.contains(BookingState.fromWireName(bookingStatus))) {
        var reason = body.get("reason");
        if (reason == null) {
          issues.add("reason is missing in the Booking States %s".formatted(REASON_STATES));
        }
      }
      return issues;
    }
  );

  private static final JsonContentCheck CHECK_CONFIRMED_BOOKING_FIELDS = JsonAttribute.customValidator(
    "check confirmed booking fields availability",
    body -> {
      var issues = new LinkedHashSet<String>();
      var bookingStatus = body.path("bookingStatus").asText("");
      if (CONFIRMED_BOOKING_STATES.contains(BookingState.fromWireName(bookingStatus))) {
        if (body.get("confirmedEquipments") == null) {
          issues.add("confirmedEquipments for confirmed booking is not present");
        }
        if (body.get("termsAndConditions") == null) {
          issues.add("termsAndConditions for confirmed booking is not present");
        }
        if (body.get("transportPlan") == null) {
          issues.add("transportPlan for confirmed booking is not present");
        }
        if (body.get("carrierClauses") == null) {
          issues.add("carrierClauses for confirmed booking is not present");
        }
        if (body.get("charges") == null) {
          issues.add("charges for confirmed booking is not present");
        }
        if (body.get("shipmentCutOffTimes") == null) {
          issues.add("shipmentCutOffTimes for confirmed booking is not present");
        }
        if (body.get("advanceManifestFilings") == null) {
          issues.add("advanceManifestFilings for confirmed booking is not present");
        }
      }
      return issues;
    });

  private static <T, O> Supplier<T> delayedValue(Supplier<O> cspSupplier, Function<O, T> field) {
    return () -> {
      var csp = cspSupplier.get();
      if (csp == null) {
        return null;
      }
      return field.apply(csp);
    };
  }

  private static void generateScenarioRelatedChecks(List<JsonContentCheck> checks, Supplier<CarrierScenarioParameters> cspSupplier, Supplier<DynamicScenarioParameters> dspSupplier) {
    checks.add(JsonAttribute.mustEqual(
      "[Scenario] Verify that the correct 'carrierServiceName' is used",
      "carrierServiceName",
      delayedValue(cspSupplier, CarrierScenarioParameters::carrierServiceName)
    ));
    checks.add(JsonAttribute.mustEqual(
      "[Scenario] Verify that the correct 'contractQuotationReference' is used",
      "contractQuotationReference",
      delayedValue(cspSupplier, CarrierScenarioParameters::contractQuotationReference)
    ));

    checks.add(JsonAttribute.mustEqual(
      "[Scenario] Verify that the correct 'carrierExportVoyageNumber' is used",
      "carrierExportVoyageNumber",
      delayedValue(cspSupplier, CarrierScenarioParameters::carrierExportVoyageNumber)
    ));
    checks.add(JsonAttribute.allIndividualMatchesMustBeValid(
      "[Scenario] Validate the containers reefer settings",
      mav-> mav.submitAllMatching("requestedEquipments.*"),
      (nodeToValidate, contextPath) -> {
        var scenario = dspSupplier.get().scenarioType();
        var activeReeferNode = nodeToValidate.path("activeReeferSettings");
        var nonOperatingReeferNode = nodeToValidate.path("isNonOperatingReefer");
        var issues = new LinkedHashSet<String>();
        switch (scenario) {
          case REEFER, REEFER_TEMP_CHANGE -> {
            if (!activeReeferNode.isObject()) {
              issues.add("The scenario requires '%s' to have an active reefer".formatted(contextPath));
            }
          }
          case REGULAR_NON_OPERATING_REEFER -> {
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

    checks.add(JsonAttribute.allIndividualMatchesMustBeValid(
      "[Scenario] Whether the cargo should be DG",
      mav-> mav.path("requestedEquipments").all().path("commodities").all().path("outerPackaging").path("dangerousGoods").submitPath(),
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
  }

  private static final JsonContentCheck VALIDATE_DOCUMENT_PARTIES = JsonAttribute.customValidator(
    "Validate documentParties",
    body -> {
      var issues = new LinkedHashSet<String>();
      var documentParties = body.path("documentParties");
      var partyFunctions = StreamSupport.stream(documentParties.spliterator(), false)
        .map(p -> p.path("partyFunction"))
        .filter(JsonNode::isTextual)
        .map(n -> n.asText(""))
        .collect(Collectors.toSet());

      if (!partyFunctions.contains("SCO")) {
        if (!body.path("serviceContractReference").isTextual()) {
          issues.add("The 'SCO' party is mandatory when 'serviceContractReference' is absent");
        }
        if (!body.path("contractQuotationReference").isTextual()) {
          issues.add("The 'SCO' party is mandatory when 'contractQuotationReference' is absent");
        }
      }
      Set<String> partyShippers  = new HashSet<>(Arrays.asList("OS", "DDR"));
      if (partyFunctions.stream().noneMatch(partyShippers::contains)) {
        if (!partyFunctions.contains("BA")) {
          issues.add("The 'BA' party must exist if 'OS' or 'DDR' party is absent");
        }
      }
        return issues;
    });

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
    TLR_CC_T_COMBINATION_VALIDATIONS,
    DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE,
    UNIVERSAL_SERVICE_REFERENCE,
    VALIDATE_SHIPMENT_CUTOFF_TIME_CODE,
    VALIDATE_ALLOWED_SHIPMENT_CUTOFF_CODE,
    VALIDATE_DOCUMENT_PARTIES,
    JsonAttribute.atLeastOneOf(
      JsonPointer.compile("/expectedDepartureDate"),
      JsonPointer.compile("/expectedArrivalAtPlaceOfDeliveryStartDate"),
      JsonPointer.compile("/expectedArrivalAtPlaceOfDeliveryEndDate"),
      JsonPointer.compile("/carrierExportVoyageNumber")
    ),
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
      "DangerousGoods implies numberOfPackages or description",
      mav -> mav.submitAllMatching("requestedEquipments.*.commodities.*.outerPackaging"),
      (nodeToValidate, contextPath) -> {
        var dg = nodeToValidate.path("dangerousGoods");
        if (!dg.isArray() || dg.isEmpty()) {
          return Set.of();
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
    ),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The charges 'currencyCode' values must be from dataset",
      mav -> mav.submitAllMatching("charges.*.currencyCode"),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.ISO_4217_CURRENCY_CODES)
    ),

    JsonAttribute.allIndividualMatchesMustBeValid(
      "The charges currency amount must not exceed more than 2 decimal points",
      mav -> mav.submitAllMatching("charges.*"),
      (nodeToValidate, contextPath) -> {
        var currencyAmount = nodeToValidate.path("currencyAmount").asDouble();
        if (BigDecimal.valueOf(currencyAmount).scale() > 2) {
          return Set.of("currencyAmount %s is expected to have 2 decimal precious ".formatted(contextPath));
        }
        return Set.of();
      }
    )
    );

  private static final List<JsonContentCheck> RESPONSE_ONLY_CHECKS = Arrays.asList(
    ADVANCED_MANIFEST_FILING_CODES_UNIQUE,
    AMF_CC_MTC_COMBINATION_VALIDATIONS,
    SHIPMENT_CUTOFF_TIMES_UNIQUE,
    CHECK_CONFIRMED_BOOKING_FIELDS,
    VALIDATE_SHIPMENT_LOCATIONS,
    REQUESTED_CHANGES_PRESENCE,
    REASON_PRESENCE
  );

  public static ActionCheck responseContentChecks(UUID matched, Supplier<CarrierScenarioParameters> cspSupplier, Supplier<DynamicScenarioParameters> dspSupplier, BookingState bookingStatus, BookingState amendedBookingState) {
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
    checks.addAll(RESPONSE_ONLY_CHECKS);
    if (CONFIRMED_BOOKING_STATES.contains(bookingStatus)) {
      checks.add(COMMODITIES_SUBREFERENCE_UNIQUE);
    }
    if (BOOKING_STATES_WHERE_CBR_IS_OPTIONAL.contains(bookingStatus)) {
      checks.add(JsonAttribute.mustBeAbsent(
        CARRIER_BOOKING_REFERENCE
      ));
    }
    generateScenarioRelatedChecks(checks, cspSupplier, dspSupplier);
    return JsonAttribute.contentChecks(
      BookingRole::isCarrier,
      matched,
      HttpMessageType.RESPONSE,
      checks
    );
  }

  private boolean isReeferContainerSizeTypeCode(String isoEquipmentCode) {
    var codeChar = isoEquipmentCode.length() > 2 ? isoEquipmentCode.charAt(2) : '?';
    return codeChar == 'R' || codeChar == 'H';
  }
}


