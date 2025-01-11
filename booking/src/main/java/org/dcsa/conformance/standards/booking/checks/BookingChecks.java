package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.dcsa.conformance.standards.booking.checks.BookingDataSets.NATIONAL_COMMODITY_TYPE_CODES;

@UtilityClass
public class BookingChecks {

  private static final Set<BookingState> CONFIRMED_BOOKING_STATES = Set.of(
    BookingState.CONFIRMED,
    BookingState.PENDING_AMENDMENT,
    BookingState.COMPLETED,
    BookingState.DECLINED
  );

  private static final Set<BookingState> NON_CONFIRMED_BOOKING_STATES = Set.of(
    BookingState.RECEIVED,
    BookingState.PENDING_UPDATE,
    BookingState.UPDATE_RECEIVED
  );

  private static final Set<BookingState> PENDING_CHANGES_STATES = Set.of(
    BookingState.PENDING_UPDATE,
    BookingState.PENDING_AMENDMENT
  );

  private static final Set<BookingState> REASON_PRESENCE_STATES = Set.of(
    BookingState.PENDING_UPDATE,
    BookingState.PENDING_AMENDMENT,
    BookingState.DECLINED,
    BookingState.REJECTED,
    BookingState.CANCELLED,
    BookingState.AMENDMENT_CANCELLED,
    BookingState.AMENDMENT_DECLINED
  );

  private static final Set<BookingCancellationState> REASON_PRESENCE_CANCELLATION_STATES = Set.of(
    BookingCancellationState.CANCELLATION_RECEIVED,
    BookingCancellationState.CANCELLATION_CONFIRMED,
    BookingCancellationState.CANCELLATION_DECLINED
  );

  private static final Set<BookingState> REASON_ABSENCE_STATES = Set.of(
    BookingState.RECEIVED,
    BookingState.CONFIRMED
  );

  private static final JsonPointer CARRIER_BOOKING_REQUEST_REFERENCE = JsonPointer.compile("/carrierBookingRequestReference");
  private static final JsonPointer CARRIER_BOOKING_REFERENCE = JsonPointer.compile("/carrierBookingReference");
  private static final String RE_EMPTY_CONTAINER_PICKUP = "emptyContainerPickup";
  private static final JsonPointer BOOKING_STATUS = JsonPointer.compile("/bookingStatus");
  private static final String ATTR_AMENDED_BOOKING_STATUS = "amendedBookingStatus";
  private static final String ATTR_BOOKING_CANCELLATION_STATUS = "bookingCancellationStatus";

  public static ActionCheck requestContentChecks(UUID matched, String standardVersion, Supplier<CarrierScenarioParameters> cspSupplier, Supplier<DynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<>(STATIC_BOOKING_CHECKS);
    generateScenarioRelatedChecks(checks, cspSupplier, dspSupplier);
    return JsonAttribute.contentChecks(
      BookingRole::isShipper,
      matched,
      HttpMessageType.REQUEST,
      standardVersion,
      checks
    );
  }
  private static final JsonContentCheck CHECK_EXPECTED_DEPARTURE_DATE = JsonAttribute.customValidator(
    "Check expected departure date can not be past date",
    body -> {
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

  private static final JsonRebaseableContentCheck DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE = JsonAttribute.customValidator(
    "Each document party can be used at most once",
    JsonAttribute.path(
      "documentParties",
      JsonAttribute.path("other", JsonAttribute.unique("partyFunction"))
    ));

  private static final JsonContentCheck VALIDATE_DOCUMENT_PARTY = JsonAttribute.customValidator(
    "Validate document party for address, identifyingCodes and partyContactDetails",
    body -> {
      var documentParties = body.path("documentParties");
      var issues = new LinkedHashSet<String>();
      Iterator<Map.Entry<String, JsonNode>> fields = documentParties.fields();
      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> field = fields.next();
        JsonNode childNode = field.getValue();
        if(field.getKey().equals("other")) {
          var otherDocumentParties = childNode.path("other");
          for(JsonNode node:otherDocumentParties) {
            issues.addAll(validateDocumentPartyFields(node.path("party")));
          }
        }
        else {
          issues.addAll(validateDocumentPartyFields(childNode));
        }
      }
      return issues;
    });

  private static Set<String> validateDocumentPartyFields(JsonNode documentPartyNode) {
    var issues = new LinkedHashSet<String>();
    // FIXME delete when confirmed as the correct and complete fix for SD-1938
//    var address = documentPartyNode.path("address");
//    var identifyingCodes = documentPartyNode.path("identifyingCodes");
//    if (address.isMissingNode() && identifyingCodes.isMissingNode()) {
//      issues.add("address or identifyingCodes must have provided.");
//    }
    var partyContactDetails = documentPartyNode.path("partyContactDetails");
    if (partyContactDetails.isArray()) {
      StreamSupport.stream(partyContactDetails.spliterator(), false)
        .forEach(element -> {
          if (element.path("phone").isMissingNode() && element.path("email").isMissingNode()) {
            issues.add("PartyContactDetails must have phone or an email id.");
          }
        });
    }
    return issues;
  }

  private static final JsonContentCheck COMMODITIES_SUBREFERENCE_UNIQUE = JsonAttribute.allIndividualMatchesMustBeValid(
    "Each Subreference in commodities must be unique",
    mav -> mav.submitAllMatching("requestedEquipments.*.commodities"),
    JsonAttribute.unique("commoditySubReference")
  );

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
      var ielNode = getShipmentLocationTypeCode(body,"IEL");


      if ((pdeNode == null || pdeNode.isEmpty()) && "SD".equals(deliveryTypeAtDestination) && (podNode == null || podNode.isEmpty())) {
        issues.add("Place of Delivery value must be provided");
      }
      if ((pdeNode == null || pdeNode.isEmpty()) && (podNode == null || podNode.isEmpty()) ) {
        issues.add("Port of Discharge value must be provided");
      }
      if ((preNode == null || preNode.isEmpty()) && (polNode == null || polNode.isEmpty())) {
        issues.add("Port of Load values must be provided");
      }
      if ((preNode == null || preNode.isEmpty()) && "SD".equals(receiptTypeAtOrigin) && (polNode == null || polNode.isEmpty())) {
        issues.add("Place of Receipt values must be provided");
      }
      if(!"SD".equals(receiptTypeAtOrigin) && ielNode != null) {
        issues.add("Container intermediate export stop-off location should not be provided");
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
              if ((preNode == null || preNode.isEmpty()) || containerPositionsDateTime == null){
                issues.add("Empty container positioning DateTime at requestedEquipments position %s must be provided.".formatted(currentCount));
              }
            });
        }
      }
      if("CY".equals(receiptTypeAtOrigin)) {
        var requestedEquipments = body.path("requestedEquipments");
        if (requestedEquipments.isArray()) {
          AtomicInteger counter = new AtomicInteger(0);
          StreamSupport.stream(requestedEquipments.spliterator(), false)
            .forEach(element -> {
              int currentCount = counter.getAndIncrement();
              if (element.path(RE_EMPTY_CONTAINER_PICKUP).isContainerNode()
                && element.path(RE_EMPTY_CONTAINER_PICKUP).path("dateTime").asText("").isEmpty()
                && element.path(RE_EMPTY_CONTAINER_PICKUP).path("depotReleaseLocation").asText("").isEmpty()) {
                issues.add("Empty container Pickup DateTime/depotReleaseLocation  at requestedEquipments position %s must be provided.".formatted(currentCount));
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
      var vesselName = body.path("vessel").path("name").asText("");
      var carrierExportVoyageNumber = body.path("carrierExportVoyageNumber").asText("");
      var carrierServiceCode = body.path("carrierServiceCode").asText("");
      var carrierServiceName = body.path("carrierServiceName").asText("");
      var expectedDepartureDate = body.path("expectedDepartureDate").asText("");

      var polNode = getShipmentLocationTypeCode(body,"POL");
      var preNode = getShipmentLocationTypeCode(body,"PRE");
      var pdeNode = getShipmentLocationTypeCode(body,"PDE");
      var podNode = getShipmentLocationTypeCode(body,"POD");

      String providedArrivalStartDate = body.path("expectedArrivalAtPlaceOfDeliveryStartDate").asText("");
      String providedArrivalEndDate = body.path("expectedArrivalAtPlaceOfDeliveryEndDate").asText("");

      var isPodAbsent = (pdeNode == null || pdeNode.isEmpty()) && (podNode == null || podNode.isEmpty());
      var isPolAbsent = (preNode == null || preNode.isEmpty()) && (polNode == null || polNode.isEmpty());

      var poldServiceCodeAbsent = isPolAbsent && isPodAbsent && vesselName.isEmpty() && carrierExportVoyageNumber.isEmpty()
        || isPolAbsent && isPodAbsent && carrierServiceCode.isEmpty() && carrierExportVoyageNumber.isEmpty()
        || isPolAbsent && isPodAbsent && carrierServiceName.isEmpty() && carrierExportVoyageNumber.isEmpty();

      var poldExpectedDepartureDateAbsent = isPolAbsent && isPodAbsent && expectedDepartureDate.isEmpty() ;

      var poldArrivalStartEndDateAbsent = isPolAbsent && isPodAbsent && providedArrivalStartDate.isEmpty() && providedArrivalEndDate.isEmpty();

      if ( poldServiceCodeAbsent || poldExpectedDepartureDateAbsent || poldArrivalStartEndDateAbsent ) {
        issues.add("The minimum options to provide shipper's requested fields are missing.");
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

  private static final JsonContentCheck REQUESTED_CHANGES_PRESENCE = JsonAttribute.customValidator(
    "Requested changes must be present for the selected Booking Status ",
    body -> {
      var bookingStatus = body.path("bookingStatus").asText("");
      var issues = new LinkedHashSet<String>();
      if (PENDING_CHANGES_STATES.contains(BookingState.fromString(bookingStatus))) {
        var feedbacks = body.get("feedbacks");
        if (feedbacks == null) {
          issues.add("feedbacks is missing in allowed booking states %s".formatted(PENDING_CHANGES_STATES));
        }
      }
      return issues;
    }
  );

  private static final JsonContentCheck REASON_FIELD_PRESENCE = JsonAttribute.customValidator(
    "Reason field must be present for the selected Booking Status",
    body -> {
      var bookingStatus = body.path("bookingStatus");
      var amendedBookingStatus = body.path(ATTR_AMENDED_BOOKING_STATUS);
      var bookingCancellationStatus = body.path(ATTR_BOOKING_CANCELLATION_STATUS);
      var issues = new LinkedHashSet<String>();
      var status = amendedBookingStatus.isMissingNode() || amendedBookingStatus.isNull() ? bookingStatus : amendedBookingStatus;
      var reason = body.get("reason");
      if (REASON_PRESENCE_STATES.contains(BookingState.fromString(status.asText())) && reason == null) {
          issues.add("reason is missing in the Booking States %s".formatted(REASON_PRESENCE_STATES));
        }
      if(!bookingCancellationStatus.isMissingNode() && REASON_PRESENCE_CANCELLATION_STATES
        .contains(BookingCancellationState.fromString(bookingCancellationStatus.asText()))
        && reason == null) {
          issues.add("reason is missing in the Booking States %s".formatted(REASON_PRESENCE_CANCELLATION_STATES));
        }

      return issues;
    }
  );
  private static final JsonContentCheck REASON_FIELD_ABSENCE = JsonAttribute.customValidator(
    "Reason field must be present for the selected Booking Status",
    body -> {
      var bookingStatus = body.path("bookingStatus");
      var amendedBookingStatus = body.path(ATTR_AMENDED_BOOKING_STATUS);
      var bookingCancellationStatus = body.path(ATTR_BOOKING_CANCELLATION_STATUS);
      var issues = new LinkedHashSet<String>();
      var status = amendedBookingStatus.isMissingNode() || amendedBookingStatus.isNull() ? bookingStatus : amendedBookingStatus;
      if (REASON_ABSENCE_STATES.contains(BookingState.fromString(status.asText())) && bookingCancellationStatus == null) {
        if (body.hasNonNull("reason")) {
          issues.add("reason must not be in the Booking States %s".formatted(REASON_ABSENCE_STATES));
        }
      }
      return issues;
    }
  );


  private static final JsonContentCheck CHECK_ABSENCE_OF_CONFIRMED_FIELDS = JsonAttribute.customValidator(
    "check absence of confirmed fields in non confirmed booking states",
    body -> {
      var issues = new LinkedHashSet<String>();
      var bookingStatus = body.path("bookingStatus").asText("");
      if (NON_CONFIRMED_BOOKING_STATES.contains(BookingState.fromString(bookingStatus))) {
        if (body.hasNonNull("termsAndConditions")) {
          issues.add("termsAndConditions must not be present in %s".formatted(bookingStatus));
        }
        if (body.hasNonNull("carrierClauses")) {
          issues.add("carrierClauses must not be present in %s".formatted(bookingStatus));
        }
        if (body.hasNonNull("charges")) {
          issues.add("charges must not be present in %s".formatted(bookingStatus));
        }
        if (body.hasNonNull("advanceManifestFilings")) {
          issues.add("advanceManifestFilings must not be present in %s".formatted(bookingStatus));
        }
        if (body.hasNonNull("confirmedEquipments")) {
          issues.add("confirmedEquipments must not be present in %s".formatted(bookingStatus));
        }
        if (body.hasNonNull("transportPlan")) {
          issues.add("transportPlan must not be present in %s".formatted(bookingStatus));
        }
        if (body.hasNonNull("shipmentCutOffTimes")) {
          issues.add("shipmentCutOffTimes must not be present in %s".formatted(bookingStatus));
        }
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
        if (body.path("shipmentCutOffTimes").isEmpty()) {
          issues.add("shipmentCutOffTimes for confirmed booking is not present");
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

    checks.add(JsonAttribute.customValidator(
      "[Scenario] Verify that the correct 'contractQuotationReference'/'serviceContractReference' is used",
      body -> {
        var contractQuotationReference = body.path("contractQuotationReference").asText("");
        var serviceContractReference = body.path("serviceContractReference").asText("");
        if (!contractQuotationReference.isEmpty() && !serviceContractReference.isEmpty()) {
          return Set.of("The scenario requires either of 'contractQuotationReference'/'serviceContractReference'" +
            " to be present, but not both");
        }
        return Set.of();
      }
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

  private static final List<JsonContentCheck> STATIC_BOOKING_CHECKS = Arrays.asList(
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/cargoMovementTypeAtOrigin"), BookingDataSets.CARGO_MOVEMENT_TYPE),
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/cargoMovementTypeAtDestination"), BookingDataSets.CARGO_MOVEMENT_TYPE),
    CHECK_EXPECTED_DEPARTURE_DATE,
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
    VALIDATE_DOCUMENT_PARTY,
    NATIONAL_COMMODITY_TYPE_CODE_VALIDATION,
    CHECK_CARGO_GROSS_WEIGHT_CONDITIONS,
    JsonAttribute.atLeastOneOf(
      JsonPointer.compile("/expectedDepartureDate"),
      JsonPointer.compile("/expectedArrivalAtPlaceOfDeliveryStartDate"),
      JsonPointer.compile("/expectedArrivalAtPlaceOfDeliveryEndDate"),
      JsonPointer.compile("/carrierExportVoyageNumber")
    ),
    JsonAttribute.xOrFields(
      JsonPointer.compile("/contractQuotationReference"),
      JsonPointer.compile("/serviceContractReference")
    ),
    JsonAttribute.allOrNoneArePresent(
      JsonPointer.compile("/expectedArrivalAtPlaceOfDeliveryStartDate"),
      JsonPointer.compile("/expectedArrivalAtPlaceOfDeliveryEndDate")
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
      "The 'segregationGroups' values must be from dataset",
      allDg(dg -> dg.path("segregationGroups").all().submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.DG_SEGREGATION_GROUPS)
    ),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'inhalationZone' values must be from dataset",
      allDg(dg -> dg.path("inhalationZone").all().submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.INHALATION_ZONE_CODE)
    ),
    JsonAttribute.allOrNoneArePresent(
      JsonPointer.compile("/declaredValue"),
      JsonPointer.compile("/declaredValueCurrency")
    ),

    JsonAttribute.allIndividualMatchesMustBeValid(
      "The charges currency amount must not exceed more than 2 decimal points",
      mav -> mav.submitAllMatching("charges.*.currencyAmount"),
      (nodeToValidate, contextPath) -> {
        var currencyAmount = nodeToValidate.asDouble();
        if (BigDecimal.valueOf(currencyAmount).scale() > 2) {
          return Set.of("%s must have at most 2 decimal point of precision".formatted(contextPath));
        }
        return Set.of();
      }
    ));

  private static final List<JsonContentCheck> RESPONSE_ONLY_CHECKS = Arrays.asList(
    CHECK_ABSENCE_OF_CONFIRMED_FIELDS,
    ADVANCED_MANIFEST_FILING_CODES_UNIQUE,
    SHIPMENT_CUTOFF_TIMES_UNIQUE,
    CHECK_CONFIRMED_BOOKING_FIELDS,
    VALIDATE_SHIPMENT_LOCATIONS,
    REQUESTED_CHANGES_PRESENCE,
    REASON_FIELD_PRESENCE,
    REASON_FIELD_ABSENCE
  );

  public static ActionCheck responseContentChecks(UUID matched, String standardVersion, Supplier<CarrierScenarioParameters> cspSupplier,
                                                  Supplier<DynamicScenarioParameters> dspSupplier, BookingState bookingStatus,
                                                  BookingState expectedAmendedBookingStatus, BookingCancellationState expectedCancelledBookingStatus,
                                                  Boolean requestAmendedContent) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(JsonAttribute.mustEqual(
      CARRIER_BOOKING_REQUEST_REFERENCE,
      () -> dspSupplier.get().carrierBookingRequestReference()
    ));
    checks.add(JsonAttribute.mustEqual(
      BOOKING_STATUS,
      bookingStatus.name()
    ));
    checks.add(JsonAttribute.customValidator(
      "Validate Amended Booking Status",
      body -> {
        String amendedBookingStatus = body.path(ATTR_AMENDED_BOOKING_STATUS).asText("");
        if(!amendedBookingStatus.isEmpty()
          && expectedAmendedBookingStatus!= null
          && !expectedAmendedBookingStatus.name().equals(amendedBookingStatus)) {
          return Set.of("The expected amendedBookingStatus %s is not equal to response AmendedBookingStatus %s", expectedAmendedBookingStatus.name(), amendedBookingStatus);
        }
        return Set.of();
      }
    ));
    checks.add(JsonAttribute.customValidator(
      "Validate Booking Cancellation Status",
      body -> {
        String bookingCancellationStatus = body.path(ATTR_BOOKING_CANCELLATION_STATUS).asText("");
        if(!bookingCancellationStatus.isEmpty()
          && expectedCancelledBookingStatus!= null
          && !expectedCancelledBookingStatus.name().equals(bookingCancellationStatus)) {
          return Set.of("The expected bookingCancellationStatus %s is not equal to response bookingCancellationStatus %s", expectedCancelledBookingStatus.name(), bookingCancellationStatus);
        }
        return Set.of();
      }
    ));




    checks.addAll(STATIC_BOOKING_CHECKS);
    checks.addAll(RESPONSE_ONLY_CHECKS);
    checks.add(JsonAttribute.lostAttributeCheck(
      "Validate that shipper provided data was not altered",
      delayedValue(dspSupplier, dsp -> requestAmendedContent ? dsp.updatedBooking() : dsp.booking())
    ));
    if (CONFIRMED_BOOKING_STATES.contains(bookingStatus)) {
      checks.add(COMMODITIES_SUBREFERENCE_UNIQUE);
      checks.add(JsonAttribute.mustBePresent(
        CARRIER_BOOKING_REFERENCE
      ));
      checks.add(JsonAttribute.allIndividualMatchesMustBeValid(
          "The commoditySubReference is not present for confirmed booking",
          mav -> mav.submitAllMatching("requestedEquipments.*.commodities.*"),
          (nodeToValidate, contextPath) -> {
            var commoditySubReference = nodeToValidate.path("commoditySubReference");
            if (commoditySubReference.isMissingNode() || commoditySubReference.isNull()) {
              return Set.of("The commoditySubReference at %s is not present for confirmed booking".formatted(contextPath));
            }
            return Set.of();
          }
        )
      );
    }
    generateScenarioRelatedChecks(checks, cspSupplier, dspSupplier);
    return JsonAttribute.contentChecks(
      BookingRole::isCarrier,
      matched,
      HttpMessageType.RESPONSE,
      standardVersion,
      checks
    );
  }

  private boolean isReeferContainerSizeTypeCode(String isoEquipmentCode) {
    var codeChar = isoEquipmentCode.length() > 2 ? isoEquipmentCode.charAt(2) : '?';
    return codeChar == 'R' || codeChar == 'H';
  }
}


