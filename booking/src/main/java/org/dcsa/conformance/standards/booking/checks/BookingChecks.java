package org.dcsa.conformance.standards.booking.checks;

import static org.dcsa.conformance.standards.booking.checks.AbstractCarrierPayloadConformanceCheck.FEEDBACKS;
import static org.dcsa.conformance.standards.booking.checks.BookingDataSets.FEEDBACKS_CODE;
import static org.dcsa.conformance.standards.booking.checks.BookingDataSets.FEEDBACKS_SEVERITY;
import static org.dcsa.conformance.standards.booking.checks.BookingDataSets.NATIONAL_COMMODITY_TYPE_CODES;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.*;

@UtilityClass
public class BookingChecks {

  private static final Set<BookingState> CONFIRMED_BOOKING_STATES = Set.of(
    BookingState.CONFIRMED,
    BookingState.PENDING_AMENDMENT,
    BookingState.COMPLETED,
    BookingState.DECLINED
  );

  private static final JsonPointer BOOKING_STATUS = JsonPointer.compile("/bookingStatus");

  private static final String CARRIER_BOOKING_REQUEST_REFERENCE = "carrierBookingRequestReference";
  private static final String CARRIER_BOOKING_REFERENCE = "carrierBookingReference";
  private static final String ATTR_AMENDED_BOOKING_STATUS = "amendedBookingStatus";
  private static final String ATTR_BOOKING_CANCELLATION_STATUS = "bookingCancellationStatus";

  public static ActionCheck requestContentChecks(
      UUID matched,
      String standardVersion,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<>(STATIC_BOOKING_CHECKS);
    checks.addAll(generateScenarioRelatedChecks(dspSupplier));
    return JsonAttribute.contentChecks(
      BookingRole::isShipper,
      matched,
      HttpMessageType.REQUEST,
      standardVersion,
      checks
    );
  }

  private static final JsonRebaseableContentCheck NATIONAL_COMMODITY_TYPE_CODE_VALIDATION = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate that 'type' of 'nationalCommodityCodes' is a known code",
    mav -> mav.submitAllMatching("requestedEquipments.*.commodities.*.nationalCommodityCodes.*.type"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(NATIONAL_COMMODITY_TYPE_CODES)
  );

  private static final JsonContentCheck CHECK_EXPECTED_ARRIVAL_POD = JsonAttribute.customValidator(
    "Check expected arrival dates are valid",
    body -> {
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
        .map("The expected arrival dates  '%s' are not valid"::formatted)
        .collect(Collectors.toSet());
    }
  );

  private static final Predicate<JsonNode> IS_ISO_EQUIPMENT_CONTAINER_REEFER = uteNode -> {
    var isoEquipmentNode = uteNode.path("ISOEquipmentCode");
    return isReeferContainerSizeTypeCode(isoEquipmentNode.asText(""));
  };
  private static final Predicate<JsonNode> HAS_ISO_EQUIPMENT_CODE = reqEquipNode -> {
    var isoEquipmentNode = reqEquipNode.path("ISOEquipmentCode");
    return isoEquipmentNode.isTextual();
  };
  private static final Predicate<JsonNode> IS_ACTIVE_REEFER_SETTINGS_REQUIRED = reqEquipNode -> {
    var norNode = reqEquipNode.path("isNonOperatingReefer");
    if (HAS_ISO_EQUIPMENT_CODE.test(reqEquipNode) && IS_ISO_EQUIPMENT_CONTAINER_REEFER.test(reqEquipNode)) {
      return !norNode.isMissingNode() && !norNode.asBoolean(false);
    }
    return false;
  };

  private static final Consumer<MultiAttributeValidator> ALL_REQ_EQUIP = mav -> mav.submitAllMatching("requestedEquipments.*");
  private static final JsonContentCheck NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER = JsonAttribute.allIndividualMatchesMustBeValid(
    "All requested Equipments where 'isNonOperatingReefer' is 'false' must have 'activeReeferSettings'",
    ALL_REQ_EQUIP,
    JsonAttribute.ifMatchedThen(
      IS_ACTIVE_REEFER_SETTINGS_REQUIRED,
      JsonAttribute.path("activeReeferSettings", JsonAttribute.matchedMustBePresent())
    )
  );

  private static final JsonContentCheck ISO_EQUIPMENT_CODE_AND_NOR_CHECK = JsonAttribute.allIndividualMatchesMustBeValid(
    "All requested Equipments where ISOEquipmentCode is reefer code must have 'isNonOperatingReefer' flag",
    ALL_REQ_EQUIP,
    JsonAttribute.ifMatchedThen(
      IS_ISO_EQUIPMENT_CONTAINER_REEFER,
      JsonAttribute.path("isNonOperatingReefer", JsonAttribute.matchedMustBePresent())
    )
  );

  private static final JsonContentCheck UNIVERSAL_SERVICE_REFERENCE = JsonAttribute.customValidator(
    "Conditional Universal Service Reference",
    body -> {
      var universalExportVoyageReference = body.path("universalExportVoyageReference");
      var universalImportVoyageReference = body.path("universalImportVoyageReference");
      var universalServiceReference = body.path("universalServiceReference");
      if (JsonAttribute.isJsonNodePresent(universalExportVoyageReference) || JsonAttribute.isJsonNodePresent(universalImportVoyageReference) ) {
        if (JsonAttribute.isJsonNodeAbsent(universalServiceReference) ){
          return Set.of("The universalServiceReference must be present as either universalExportVoyageReference or universalExportVoyageReference are present");
        }
      }
      return Set.of();
    }
  );

  private static final JsonContentCheck REFERENCE_TYPE_VALIDATION = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate reference type field",
    mav -> {
      mav.submitAllMatching("requestedEquipments.*.references.*.type");
      mav.submitAllMatching("requestedEquipments.*.commodities.*.references.*.type");
      mav.submitAllMatching("references.*.type");
    },
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.REFERENCE_TYPES)
  );

  private static Consumer<MultiAttributeValidator> allDg(Consumer<MultiAttributeValidator.AttributePathBuilder> consumer) {
    return mav -> consumer.accept(mav.path("requestedEquipments").all().path("commodities").all().path("outerPackaging").path("dangerousGoods").all());
  }

  static final JsonContentCheck IS_EXPORT_DECLARATION_REFERENCE_PRESENCE = JsonAttribute.ifThenElse(
    "Check Export declaration reference presence",
    JsonAttribute.isTrue(JsonPointer.compile("/isExportDeclarationRequired")),
    JsonAttribute.mustBePresent(JsonPointer.compile("/exportDeclarationReference")),
    JsonAttribute.mustBeAbsent(JsonPointer.compile("/exportDeclarationReference"))
  );

  static final JsonContentCheck DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE =
      JsonAttribute.customValidator(
          "Each document party can be used at most once, except 'NI' which can be repeated",
          body -> {
            var partyFunctionCounts = new HashMap<String, Integer>();

            StreamSupport.stream(body.path("documentParties").path("other").spliterator(), false)
                .map(party -> party.path("partyFunction").asText(""))
                .filter(partyFunction -> !partyFunction.isBlank())
                .forEach(partyFunction -> partyFunctionCounts.merge(partyFunction, 1, Integer::sum));

            return partyFunctionCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1 && !"NI".equals(entry.getKey()))
                .map(entry ->
                    "Party function '%s' cannot be repeated. Found %d occurrences."
                        .formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
          });

  static final JsonContentCheck COMMODITIES_SUBREFERENCE_UNIQUE =
      JsonAttribute.customValidator(
          "Each commoditySubReference must be unique across the entire booking",
          body -> {
            var subReferenceCount = new HashMap<String, Integer>();

            StreamSupport.stream(body.path("requestedEquipments").spliterator(), false)
                .flatMap(
                    equipment ->
                        StreamSupport.stream(equipment.path("commodities").spliterator(), false))
                .map(commodity -> commodity.path("commoditySubReference").asText(""))
                .filter(subRef -> !subRef.isBlank())
                .forEach(subRef -> subReferenceCount.merge(subRef, 1, Integer::sum));

            return subReferenceCount.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(
                    entry ->
                        "commoditySubReference '%s' is not unique across the booking. Found %d occurrences."
                            .formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
          });

  private static final JsonContentCheck VALIDATE_ALLOWED_SHIPMENT_CUTOFF_CODE = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate allowed shipment cutoff codes",
    mav -> mav.submitAllMatching("shipmentCutOffTimes.*.cutOffDateTimeCode"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.CUTOFF_DATE_TIME_CODES)
  );

  private static final JsonContentCheck VALIDATE_SHIPMENT_CUTOFF_TIME_CODE = JsonAttribute.customValidator(
    "Validate shipment cutOff Date time code",
    body -> {
      var shipmentCutOffTimes = body.path("shipmentCutOffTimes");
      var receiptTypeAtOrigin = body.path("receiptTypeAtOrigin").asText("");
      var issues = new LinkedHashSet<String>();
      var cutOffDateTimeCodes = StreamSupport.stream(shipmentCutOffTimes.spliterator(), false)
        .map(p -> p.path("cutOffDateTimeCode"))
        .filter(JsonNode::isTextual)
        .map(n -> n.asText(""))
        .collect(Collectors.toSet());
      if (receiptTypeAtOrigin.equals("CFS") && !cutOffDateTimeCodes.contains("LCL")) {
        issues.add("cutOffDateTimeCode 'LCL' must be present when receiptTypeAtOrigin is CFS");
      }
      return issues;
    }
  );

  private static final Consumer<MultiAttributeValidator> ALL_AMF = mav -> mav.submitAllMatching("advanceManifestFilings");

  private static final JsonContentCheck ADVANCED_MANIFEST_FILING_CODES_UNIQUE = JsonAttribute.allIndividualMatchesMustBeValid(
    "The combination of 'countryCode' and 'manifestTypeCode' in 'advanceManifestFilings' must be unique",
    ALL_AMF,
    JsonAttribute.unique("countryCode", "manifestTypeCode")
  );
  private static final Consumer<MultiAttributeValidator> ALL_SHIPMENT_CUTOFF_TIMES = mav -> mav.submitAllMatching("shipmentCutOffTimes");

  private static final JsonContentCheck SHIPMENT_CUTOFF_TIMES_UNIQUE = JsonAttribute.allIndividualMatchesMustBeValid(
    "in 'shipmentCutOfftimes' cutOff date time code must be unique",
    ALL_SHIPMENT_CUTOFF_TIMES,
    JsonAttribute.unique("cutOffDateTimeCode")
  );

  private static final JsonContentCheck VALIDATE_SHIPMENT_LOCATIONS = JsonAttribute.customValidator(
    "Validate shipmentLocations",
    body -> {
      var issues = new LinkedHashSet<String>();
      var receiptTypeAtOrigin = body.path("receiptTypeAtOrigin").asText("");
      var deliveryTypeAtDestination = body.path("deliveryTypeAtDestination").asText("");
      var polNode = getShipmentLocationTypeCode(body,"POL");
      var preNode = getShipmentLocationTypeCode(body,"PRE");
      var pdeNode = getShipmentLocationTypeCode(body,"PDE");
      var podNode = getShipmentLocationTypeCode(body,"POD");

      if ((pdeNode == null || pdeNode.isEmpty()) && (podNode == null || podNode.isEmpty()) ) {
        issues.add("Port of Discharge value must be provided");
      }
      if ((preNode == null || preNode.isEmpty()) && (polNode == null || polNode.isEmpty())) {
        issues.add("Port of Load values must be provided");
      }
      if ((pdeNode == null || pdeNode.isEmpty()) && "SD".equals(deliveryTypeAtDestination)) {
        issues.add("Place of Delivery value must be provided");
      }
      if ((preNode == null || preNode.isEmpty()) && "SD".equals(receiptTypeAtOrigin)) {
        issues.add("Place of Receipt values must be provided");
      }
      if("SD".equals(receiptTypeAtOrigin)) {
        var requestedEquipments = body.path("requestedEquipments");
        if (requestedEquipments.isArray()) {
          AtomicInteger counter = new AtomicInteger(0);
          StreamSupport.stream(requestedEquipments.spliterator(), false)
            .forEach(element -> {
              int currentCount = counter.getAndIncrement();
              var containerPositionings = element.path("containerPositionings");
              var containerPositionsDateTime = StreamSupport.stream(containerPositionings.spliterator(), false)
                .filter(o ->  !o.path("dateTime").asText("").isEmpty())
                .findFirst()
                .orElse(null);
              if (containerPositionsDateTime == null){
                issues.add("Empty container positioning DateTime at requestedEquipments position %s must be provided.".formatted(currentCount));
              }
            });
        }
      }
      return issues;
    });

  private static final JsonContentCheck VALIDATE_SHIPPER_MINIMUM_REQUEST_FIELDS = JsonAttribute.customValidator(
    "Validate shipper's minimum request fields",
    body -> {
      var issues = new LinkedHashSet<String>();

      // Check if routingReference is provided
      var routingReference = body.path("routingReference").asText("");
      if (!routingReference.isEmpty()) {
        // If routingReference is provided, validation passes
        return issues;
      }

      // If no routingReference, check location-based requirements
      var vesselName = body.path("vessel").path("name").asText("");
      var carrierExportVoyageNumber = body.path("carrierExportVoyageNumber").asText("");
      var carrierServiceCode = body.path("carrierServiceCode").asText("");
      var carrierServiceName = body.path("carrierServiceName").asText("");
      var expectedDepartureDate = body.path("expectedDepartureDate").asText("");
      var expectedDepartureFromPlaceOfReceiptDate = body.path("expectedDepartureFromPlaceOfReceiptDate").asText("");

      var polNode = getShipmentLocationTypeCode(body,"POL");
      var preNode = getShipmentLocationTypeCode(body,"PRE");
      var pdeNode = getShipmentLocationTypeCode(body,"PDE");
      var podNode = getShipmentLocationTypeCode(body,"POD");

      String providedArrivalStartDate = body.path("expectedArrivalAtPlaceOfDeliveryStartDate").asText("");
      String providedArrivalEndDate = body.path("expectedArrivalAtPlaceOfDeliveryEndDate").asText("");

      // Check (PRE or POL) AND (POD or PDE) requirement
      var hasOriginLocation = (preNode != null && !preNode.isEmpty()) || (polNode != null && !polNode.isEmpty());
      var hasDestinationLocation = (pdeNode != null && !pdeNode.isEmpty()) || (podNode != null && !podNode.isEmpty());

      if (!hasOriginLocation || !hasDestinationLocation) {
        issues.add("Either (PRE or POL) AND (POD or PDE) locations must be provided when routingReference is not available.");
        return issues;
      }

      // Check minimum mandatory property combinations
      var hasExpectedDepartureDate = !expectedDepartureDate.isEmpty();
      var hasExpectedDepartureFromPlaceOfReceiptDate = !expectedDepartureFromPlaceOfReceiptDate.isEmpty();
      var hasArrivalDates = !providedArrivalStartDate.isEmpty() && !providedArrivalEndDate.isEmpty();
      var hasVoyageAndVessel = !carrierExportVoyageNumber.isEmpty() && !vesselName.isEmpty();
      var hasVoyageAndServiceName = !carrierExportVoyageNumber.isEmpty() && !carrierServiceName.isEmpty();
      var hasVoyageAndServiceCode = !carrierExportVoyageNumber.isEmpty() && !carrierServiceCode.isEmpty();

      if (!hasExpectedDepartureDate && !hasExpectedDepartureFromPlaceOfReceiptDate && !hasArrivalDates &&
          !hasVoyageAndVessel && !hasVoyageAndServiceName && !hasVoyageAndServiceCode) {
        issues.add("At least one of the minimum mandatory property combinations must be provided: " +
                  "expectedDepartureDate, expectedDepartureFromPlaceOfReceiptDate, " +
                  "expectedArrival dates (both start and end), " +
                  "carrierExportVoyageNumber + vesselName, " +
                  "carrierExportVoyageNumber + carrierServiceName, or " +
                  "carrierExportVoyageNumber + carrierServiceCode.");
      }

      return issues;
    });

  private static JsonNode getShipmentLocationTypeCode(JsonNode body, @NonNull String locationTypeCode) {
    var shipmentLocations = body.path("shipmentLocations");
    return StreamSupport.stream(shipmentLocations.spliterator(), false)
      .filter(o ->  o.path("locationTypeCode").asText("").equals(locationTypeCode))
      .findFirst()
      .orElse(null);
  }

  static final JsonContentCheck FEEDBACKS_PRESENCE =
      JsonAttribute.customValidator(
          "Feedbacks must be present for the selected Booking Status",
          body -> {
            var bookingStatus = body.path("bookingStatus").asText("");
            var issues = new LinkedHashSet<String>();
            if ((BookingState.PENDING_UPDATE.name().equals(bookingStatus)
                    || (BookingState.PENDING_AMENDMENT.name().equals(bookingStatus)))
                && (body.path(FEEDBACKS).isMissingNode() || body.path(FEEDBACKS).isEmpty())) {
              issues.add("feedbacks is missing in the booking state %s".formatted(bookingStatus));
            }
            return issues;
          });

  private static final JsonContentCheck CHECK_CONFIRMED_BOOKING_FIELDS = JsonAttribute.customValidator(
    "check confirmed booking fields availability",
    body -> {
      var issues = new LinkedHashSet<String>();
      var bookingStatus = body.path("bookingStatus").asText("");
      if (CONFIRMED_BOOKING_STATES.contains(BookingState.fromString(bookingStatus))) {
        if (body.path("confirmedEquipments").isEmpty()) {
          issues.add("confirmedEquipments for confirmed booking is not present");
        }
        if (body.path("transportPlan").isEmpty()) {
          issues.add("transportPlan for confirmed booking is not present");
        }
      }
      return issues;
    });

  static final JsonContentCheck CHECK_CARGO_GROSS_WEIGHT_CONDITIONS =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Check Cargo Gross Weight conditions",
          mav -> mav.submitAllMatching("requestedEquipments.*"),
          (nodeToValidate, contextPath) -> {
            var issues = new LinkedHashSet<String>();
            var cargoGrossWeight = nodeToValidate.path("cargoGrossWeight");
            if (cargoGrossWeight.isMissingNode() || cargoGrossWeight.isNull()) {
              var commodities = nodeToValidate.path("commodities");
              if (!(commodities.isMissingNode() || commodities.isNull()) && commodities.isArray()) {
                AtomicInteger commodityCounter = new AtomicInteger(0);
                StreamSupport.stream(commodities.spliterator(), false)
                    .forEach(
                        commodity -> {
                          var commodityGrossWeight = commodity.path("cargoGrossWeight");
                          int currentCommodityCount = commodityCounter.getAndIncrement();
                          if (commodityGrossWeight.isMissingNode()
                              || commodityGrossWeight.isNull()) {
                            issues.add(
                                "The '%s' must have cargo gross weight at commodities position %s"
                                    .formatted(contextPath, currentCommodityCount));
                          }
                        });
              }
            }
            return issues;
          });

  public static List<JsonContentCheck> generateScenarioRelatedChecks(
      Supplier<DynamicScenarioParameters> dspSupplier) {
    List<JsonContentCheck> checks = new ArrayList<>();
    checks.add(
        JsonAttribute.customValidator(
            "[Scenario] Verify that the correct 'contractQuotationReference'/'serviceContractReference' is used",
            body -> {
              var contractQuotationReference = body.path("contractQuotationReference").asText("");
              var serviceContractReference = body.path("serviceContractReference").asText("");
              if (!contractQuotationReference.isEmpty() && !serviceContractReference.isEmpty()) {
                return Set.of(
                    "The scenario requires either of 'contractQuotationReference'/'serviceContractReference'"
                        + " to be present, but not both");
              }
              return Set.of();
            }));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "[Scenario] Validate the containers reefer settings",
            mav -> mav.submitAllMatching("requestedEquipments.*"),
            (nodeToValidate, contextPath) -> {
              var scenario = dspSupplier.get().scenarioType();
              var activeReeferNode = nodeToValidate.path("activeReeferSettings");
              var nonOperatingReeferNode = nodeToValidate.path("isNonOperatingReefer");
              var isoEquipmentNode = nodeToValidate.path("ISOEquipmentCode");
              var issues = new LinkedHashSet<String>();
              switch (scenario) {
                case REEFER -> {
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
                  if (!activeReeferNode.isMissingNode()) {
                    issues.add(
                        "The scenario requires '%s' to NOT have an active reefer"
                            .formatted(contextPath));
                  }
                  if (!isReeferContainerSizeTypeCode(isoEquipmentNode.asText(""))) {
                    issues.add(
                        "The scenario requires ISOEquipmentCode at '%s' to be a valid reefer container type"
                            .formatted(contextPath));
                  }
                }
                default -> {
                  if (!activeReeferNode.isMissingNode()) {
                    issues.add(
                        "The scenario requires '%s' to NOT have an active reefer"
                            .formatted(contextPath));
                  }
                  if (!nonOperatingReeferNode.isMissingNode()) {
                    issues.add(
                        "The scenario requires '%s.isNonOperatingReefer' to be omitted"
                            .formatted(contextPath));
                  }
                }
              }
              return issues;
            }));

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

    return checks;
  }

  static final JsonRebaseableContentCheck VALID_FEEDBACK_SEVERITY =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate that 'feedbacks severity' is valid",
          mav -> mav.submitAllMatching("feedbacks.*.severity"),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(FEEDBACKS_SEVERITY));

  static final JsonRebaseableContentCheck VALID_FEEDBACK_CODE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate that 'feedbacks code' is valid",
          mav -> mav.submitAllMatching("feedbacks.*.code"),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(FEEDBACKS_CODE));

  public static final List<JsonContentCheck> STATIC_BOOKING_CHECKS =
      Arrays.asList(
          JsonAttribute.mustBeDatasetKeywordIfPresent(
              JsonPointer.compile("/cargoMovementTypeAtOrigin"),
              BookingDataSets.CARGO_MOVEMENT_TYPE),
          JsonAttribute.mustBeDatasetKeywordIfPresent(
              JsonPointer.compile("/cargoMovementTypeAtDestination"),
              BookingDataSets.CARGO_MOVEMENT_TYPE),
          CHECK_EXPECTED_ARRIVAL_POD,
          NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER,
          ISO_EQUIPMENT_CODE_AND_NOR_CHECK,
          REFERENCE_TYPE_VALIDATION,
          IS_EXPORT_DECLARATION_REFERENCE_PRESENCE,
          DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE,
          UNIVERSAL_SERVICE_REFERENCE,
          VALIDATE_SHIPMENT_CUTOFF_TIME_CODE,
          VALIDATE_ALLOWED_SHIPMENT_CUTOFF_CODE,
          VALIDATE_SHIPPER_MINIMUM_REQUEST_FIELDS,
          NATIONAL_COMMODITY_TYPE_CODE_VALIDATION,
          CHECK_CARGO_GROSS_WEIGHT_CONDITIONS,
          JsonAttribute.xOrFields(
              JsonPointer.compile("/contractQuotationReference"),
              JsonPointer.compile("/serviceContractReference")),
          JsonAttribute.allOrNoneArePresent(
              JsonPointer.compile("/expectedArrivalAtPlaceOfDeliveryStartDate"),
              JsonPointer.compile("/expectedArrivalAtPlaceOfDeliveryEndDate")),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "DangerousGoods implies packagingCode or imoPackagingCode",
              mav -> mav.submitAllMatching("requestedEquipments.*.commodities.*.outerPackaging"),
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
              "DangerousGoods implies numberOfPackages",
              mav -> mav.submitAllMatching("requestedEquipments.*.commodities.*.outerPackaging"),
              (nodeToValidate, contextPath) -> {
                var dg = nodeToValidate.path("dangerousGoods");
                if (!dg.isArray() || dg.isEmpty()) {
                  return Set.of();
                }
                if (nodeToValidate.path("numberOfPackages").isMissingNode()) {
                  return Set.of(
                      "The '%s' object did not have a 'numberOfPackages', which is required due to dangerousGoods"
                          .formatted(contextPath));
                }
                return Set.of();
              }),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The 'segregationGroups' values must be from dataset",
              allDg(dg -> dg.path("segregationGroups").all().submitPath()),
              JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                  BookingDataSets.DG_SEGREGATION_GROUPS)),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The 'inhalationZone' values must be from dataset",
              allDg(dg -> dg.path("inhalationZone").all().submitPath()),
              JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                  BookingDataSets.INHALATION_ZONE_CODE)),
          JsonAttribute.allOrNoneArePresent(
              JsonPointer.compile("/declaredValue"), JsonPointer.compile("/declaredValueCurrency")),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The charges currency amount must not exceed more than 2 decimal points",
              mav -> mav.submitAllMatching("charges.*.currencyAmount"),
              (nodeToValidate, contextPath) -> {
                var currencyAmount = nodeToValidate.asDouble();
                if (BigDecimal.valueOf(currencyAmount).scale() > 2) {
                  return Set.of(
                      "%s must have at most 2 decimal point of precision".formatted(contextPath));
                }
                return Set.of();
              }));

  private static final List<JsonContentCheck> RESPONSE_ONLY_CHECKS = Arrays.asList(
    ADVANCED_MANIFEST_FILING_CODES_UNIQUE,
    SHIPMENT_CUTOFF_TIMES_UNIQUE,
    CHECK_CONFIRMED_BOOKING_FIELDS,
    VALIDATE_SHIPMENT_LOCATIONS,
    FEEDBACKS_PRESENCE,
    VALID_FEEDBACK_SEVERITY,
    VALID_FEEDBACK_CODE
  );

  public static ActionCheck responseContentChecks(
      UUID matched,
      String standardVersion,
      Supplier<DynamicScenarioParameters> dspSupplier,
      BookingState bookingStatus,
      BookingState expectedAmendedBookingStatus,
      BookingCancellationState expectedCancelledBookingStatus,
      Boolean requestAmendedContent) {
    var checks =
        fullPayloadChecks(
            dspSupplier,
            bookingStatus,
            expectedAmendedBookingStatus,
            expectedCancelledBookingStatus,
            requestAmendedContent);

    return JsonAttribute.contentChecks(
        BookingRole::isCarrier, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  public static List<JsonContentCheck> fullPayloadChecks(
      Supplier<DynamicScenarioParameters> dspSupplier,
      BookingState bookingStatus,
      BookingState expectedAmendedBookingStatus,
      BookingCancellationState expectedCancelledBookingStatus,
      Boolean requestAmendedContent) {

    var checks = new ArrayList<JsonContentCheck>();

    checks.add(cbrrOrCbr(dspSupplier));

    checks.add(JsonAttribute.mustEqual(BOOKING_STATUS, bookingStatus.name()));

    checks.add(
        JsonAttribute.customValidator(
            "Validate Amended Booking Status",
            body -> {
              String amendedBookingStatus = body.path(ATTR_AMENDED_BOOKING_STATUS).asText("");
              if (!amendedBookingStatus.isEmpty()
                  && expectedAmendedBookingStatus != null
                  && !expectedAmendedBookingStatus.name().equals(amendedBookingStatus)) {
                return Set.of(
                    "The expected amendedBookingStatus %s is not equal to response AmendedBookingStatus %s",
                    expectedAmendedBookingStatus.name(), amendedBookingStatus);
              }
              return Set.of();
            }));

    checks.add(
        JsonAttribute.customValidator(
            "Validate Booking Cancellation Status",
            body -> {
              String bookingCancellationStatus =
                  body.path(ATTR_BOOKING_CANCELLATION_STATUS).asText("");
              if (!bookingCancellationStatus.isEmpty()
                  && expectedCancelledBookingStatus != null
                  && !expectedCancelledBookingStatus.name().equals(bookingCancellationStatus)) {
                return Set.of(
                    "The expected bookingCancellationStatus %s is not equal to response bookingCancellationStatus %s",
                    expectedCancelledBookingStatus.name(), bookingCancellationStatus);
              }
              return Set.of();
            }));

    checks.addAll(STATIC_BOOKING_CHECKS);

    checks.addAll(RESPONSE_ONLY_CHECKS);

    /*
    FIXME SD-1997 implement this properly, fetching the exchange by the matched UUID of an earlier action
        checks.add(JsonAttribute.lostAttributeCheck(
          "Validate that shipper provided data was not altered",
          delayedValue(dspSupplier, dsp -> requestAmendedContent ? dsp.updatedBooking() : dsp.booking())
        ));
        */
    if (CONFIRMED_BOOKING_STATES.contains(bookingStatus)) {
      checks.add(COMMODITIES_SUBREFERENCE_UNIQUE);
      checks.add(
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The commoditySubReference is not present for confirmed booking",
              mav -> mav.submitAllMatching("requestedEquipments.*.commodities.*"),
              (nodeToValidate, contextPath) -> {
                var commoditySubReference = nodeToValidate.path("commoditySubReference");
                if (commoditySubReference.isMissingNode() || commoditySubReference.isNull()) {
                  return Set.of(
                      "The commoditySubReference at %s is not present for confirmed booking"
                          .formatted(contextPath));
                }
                return Set.of();
              }));
    }

    checks.addAll(generateScenarioRelatedChecks(dspSupplier));

    return checks;
  }

  public static JsonContentCheck cbrrOrCbr(Supplier<DynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.customValidator(
        "Validate Carrier Booking Request Reference and Carrier Booking Reference",
        body -> {
          String cbrr = body.path(CARRIER_BOOKING_REQUEST_REFERENCE).asText("");
          String cbr = body.path(CARRIER_BOOKING_REFERENCE).asText("");
          String expectedCbrr = dspSupplier.get().carrierBookingRequestReference();
          String expectedCbr = dspSupplier.get().carrierBookingReference();
          if (!cbrr.equals(expectedCbrr) && !cbr.equals(expectedCbr)) {
            return Set.of(
                "Either 'carrierBookingRequestReference' must equal %s or 'carrierBookingReference' must equal %s."
                    .formatted(expectedCbrr, expectedCbr));
          }
          return Set.of();
        });
  }

    private boolean isReeferContainerSizeTypeCode(String isoEquipmentCode) {
    var codeChar = isoEquipmentCode.length() > 2 ? isoEquipmentCode.charAt(2) : '?';
    return codeChar == 'R' || codeChar == 'H';
  }
}
