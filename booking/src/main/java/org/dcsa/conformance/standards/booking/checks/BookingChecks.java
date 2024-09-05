package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
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

  private static final Set<BookingState> REASON_ABSENCE_STATES = Set.of(
    BookingState.RECEIVED,
    BookingState.CONFIRMED
  );

  private static final JsonPointer CARRIER_BOOKING_REQUEST_REFERENCE = JsonPointer.compile("/carrierBookingRequestReference");
  private static final JsonPointer CARRIER_BOOKING_REFERENCE = JsonPointer.compile("/carrierBookingReference");
  private static final JsonPointer BOOKING_STATUS = JsonPointer.compile("/bookingStatus");
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

  private static final JsonContentCheck VALIDATE_ALL_BOOKING_UN_LOCATION_CODES = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate all booking UNLocationCodes",
    mav -> {
      mav.submitAllMatching("shipmentLocations.*.location.UNLocationCode");
      mav.submitAllMatching("invoicePayableAt.UNLocationCode");
      mav.submitAllMatching("placeOfBLIssue.UNLocationCode");
      mav.submitAllMatching("transportPlan.loadLocation.UNLocationCode");
      mav.submitAllMatching("transportPlan.dischargeLocation.UNLocationCode");

      // Beta-2 only
      mav.submitAllMatching("documentParties.shipper.address.UNLocationCode");
      mav.submitAllMatching("documentParties.consignee.address.UNLocationCode");
      mav.submitAllMatching("documentParties.bookingAgent.address.UNLocationCode");
      mav.submitAllMatching("documentParties.serviceContractOwner.address.UNLocationCode");
      mav.submitAllMatching("documentParties.carrierBookingOffice.address.UNLocationCode");
      mav.submitAllMatching("documentParties.other.*.party.address.UNLocationCode");

    },
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.UN_LOCODE_DATASET)
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

  private static final JsonContentCheck TLR_TYPE_CODE_VALIDATIONS = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate 'type' in 'taxAndLegalReferences' static data",
    mav -> mav.submitAllMatching("documentParties.*.party.taxLegalReferences.*"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.LTR_TYPE_CODES)
  );

  private static final JsonContentCheck ISO_EQUIPMENT_CODE_VALIDATION = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate ISO Equipment code",
    mav -> {
      mav.submitAllMatching("requestedEquipments.*.ISOEquipmentCode");
      mav.submitAllMatching("confirmedEquipments.*.ISOEquipmentCode");
    },
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.ISO_6346_CONTAINER_CODES)
  );

  private static final JsonContentCheck OUTER_PACKAGING_CODE_IS_VALID = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate that 'packagingCode' is a known code",
    mav -> mav.submitAllMatching("requestedEquipments.*.commodities.*.outerPackaging.packageCode"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.OUTER_PACKAGING_CODE)
  );

  private static Consumer<MultiAttributeValidator> allDg(Consumer<MultiAttributeValidator.AttributePathBuilder> consumer) {
    return mav -> consumer.accept(mav.path("requestedEquipments").all().path("commodities").all().path("outerPackaging").path("dangerousGoods").all());
  }

  private static final JsonContentCheck IS_EXPORT_DECLARATION_REFERENCE_ABSENCE = JsonAttribute.ifThen(
    "Check Export declaration reference absence",
    JsonAttribute.isFalse("/isExportDeclarationRequired"),
    JsonAttribute.mustBeAbsent(JsonPointer.compile("/exportDeclarationReference"))
  );

  private static final JsonContentCheck IS_IMPORT_DECLARATION_REFERENCE_ABSENCE = JsonAttribute.ifThen(
    "Check Import declaration reference absence",
    JsonAttribute.isFalse("/isImportLicenseRequired"),
    JsonAttribute.mustBeAbsent(JsonPointer.compile("/importLicenseReference"))
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
    var address = documentPartyNode.path("address");
    var identifyingCodes = documentPartyNode.path("identifyingCodes");
    if (address.isMissingNode() && identifyingCodes.isMissingNode()) {
      issues.add("address or identifyingCodes must have provided.");
    }
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
    JsonAttribute.unique("commoditySubreference")
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

  private static final Consumer<MultiAttributeValidator> ALL_CUSTOMS_REFERENCES_TYPE = mav -> {
    mav.submitAllMatching("customsReferences.*.type");
    mav.submitAllMatching("requestedEquipments.*.customsReferences.*.type");
    mav.submitAllMatching("requestedEquipments.*.commodities.*.customsReferences.*.type");
  };

  private static final JsonRebaseableContentCheck CR_TYPE_CODES_VALIDATIONS = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate 'type' in 'customsReferences' must be valid",
    ALL_CUSTOMS_REFERENCES_TYPE,
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.CUSTOMS_REFERENCE_RE_REC_TYPE_CODES)
  );

  private static final JsonContentCheck AMF_MTC_VALIDATIONS = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate 'manifestTypeCode' in 'advanceManifestFilings' static data",
    mav -> mav.submitAllMatching("advanceManifestFilings.*.type"),
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.AMF_CC_MTC_TYPE_CODES)
  );

  private static final JsonRebaseableContentCheck COUNTRY_CODE_VALIDATIONS = JsonAttribute.allIndividualMatchesMustBeValid(
    "Validate field is a known ISO 3166 alpha 2 code",
    mav -> {
      mav.submitAllMatching("advancedManifestFilings.*.countryCode");
      mav.submitAllMatching("documentParties.*.party.taxLegalReferences.*.countryCode");

      // Beta-2 only
      mav.submitAllMatching("documentParties.shippers.address.countryCode");
      mav.submitAllMatching("documentParties.consignee.address.countryCode");
      mav.submitAllMatching("documentParties.endorsee.address.countryCode");
      mav.submitAllMatching("documentParties.serviceContractOwner.address.countryCode");
      mav.submitAllMatching("documentParties.carrierBookingOffice.address.countryCode");
      mav.submitAllMatching("documentParties.other.*.party.address.countryCode");
      mav.submitAllMatching("placeOfBLIssue.countryCode");
      mav.submitAllMatching("requestedEquipments.*.commodities.*.nationalCommodityCodes.*.countryCode");
    },
    JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.ISO_3166_ALPHA2_COUNTRY_CODES)
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
              if ((element.path("emptyContainerPositioningLocation").isContainerNode()
                || (preNode != null && !preNode.isEmpty())) && element.path("emptyContainerPositioningDateTime").asText("").isEmpty()  ) {
                issues.add("Empty container positioning DateTime at requestedEquipments position %s must be provided.".formatted(currentCount));
              }
            });
        }
      }
      var requestedEquipments = body.path("requestedEquipments");
      if (requestedEquipments.isArray()) {
        AtomicInteger counter = new AtomicInteger(0);
        StreamSupport.stream(requestedEquipments.spliterator(), false)
          .forEach(element -> {
            int currentCount = counter.getAndIncrement();
            if (element.path("emptyContainerDepotReleaseLocation").isContainerNode()
              && element.path("emptyContainerPickupDateTime").asText("").isEmpty()  ) {
              issues.add("Empty container Pickup DateTime at requestedEquipments position %s must be provided.".formatted(currentCount));
            }
          });
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
      if (PENDING_CHANGES_STATES.contains(BookingState.fromWireName(bookingStatus))) {
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
      var amendedBookingStatus = body.path("amendedBookingStatus");
      var issues = new LinkedHashSet<String>();
      var status = amendedBookingStatus.isMissingNode() || amendedBookingStatus.isNull() ? bookingStatus : amendedBookingStatus;
      if (REASON_PRESENCE_STATES.contains(BookingState.fromWireName(status.asText()))) {
        var reason = body.get("reason");
        if (reason == null) {
          issues.add("reason is missing in the Booking States %s".formatted(REASON_PRESENCE_STATES));
        }
      }
      return issues;
    }
  );
  private static final JsonContentCheck REASON_FIELD_ABSENCE = JsonAttribute.customValidator(
    "Reason field must be present for the selected Booking Status",
    body -> {
      var bookingStatus = body.path("bookingStatus");
      var amendedBookingStatus = body.path("amendedBookingStatus");
      var issues = new LinkedHashSet<String>();
      var status = amendedBookingStatus.isMissingNode() || amendedBookingStatus.isNull() ? bookingStatus : amendedBookingStatus;
      if (REASON_ABSENCE_STATES.contains(BookingState.fromWireName(status.asText()))) {
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
      if (NON_CONFIRMED_BOOKING_STATES.contains(BookingState.fromWireName(bookingStatus))) {
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
      if (CONFIRMED_BOOKING_STATES.contains(BookingState.fromWireName(bookingStatus))) {
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

  private static final List<JsonContentCheck> STATIC_BOOKING_CHECKS = Arrays.asList(
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/cargoMovementTypeAtOrigin"), BookingDataSets.CARGO_MOVEMENT_TYPE),
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/cargoMovementTypeAtDestination"), BookingDataSets.CARGO_MOVEMENT_TYPE),
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/communicationChannelCode"), BookingDataSets.COMMUNICATION_CHANNEL_CODES),
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/declaredValueCurrency"), BookingDataSets.ISO_4217_CURRENCY_CODES),
    JsonAttribute.mustBeDatasetKeywordIfPresent(JsonPointer.compile("/incoTerms"), BookingDataSets.INCO_TERMS_VALUES),
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'cargoGrossVolume' implies 'cargoGrossVolumeUnit'",
      mav -> mav.submitAllMatching("requestedEquipments.*.commodities.*"),
      JsonAttribute.presenceImpliesOtherField(
        "cargoGrossVolume",
        "cargoGrossVolumeUnit"
      )
    ),
    VALIDATE_ALL_BOOKING_UN_LOCATION_CODES,
    CHECK_EXPECTED_DEPARTURE_DATE,
    CHECK_EXPECTED_ARRIVAL_POD,
    NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER,
    ISO_EQUIPMENT_CODE_AND_NOR_CHECK,
    REFERENCE_TYPE_VALIDATION,
    ISO_EQUIPMENT_CODE_VALIDATION,
    IS_EXPORT_DECLARATION_REFERENCE_ABSENCE,
    IS_IMPORT_DECLARATION_REFERENCE_ABSENCE,
    OUTER_PACKAGING_CODE_IS_VALID,
    TLR_TYPE_CODE_VALIDATIONS,
    DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE,
    UNIVERSAL_SERVICE_REFERENCE,
    VALIDATE_SHIPMENT_CUTOFF_TIME_CODE,
    VALIDATE_ALLOWED_SHIPMENT_CUTOFF_CODE,
    COUNTRY_CODE_VALIDATIONS,
    VALIDATE_SHIPPER_MINIMUM_REQUEST_FIELDS,
    VALIDATE_DOCUMENT_PARTY,
    CR_TYPE_CODES_VALIDATIONS,
    NATIONAL_COMMODITY_TYPE_CODE_VALIDATION,
    JsonAttribute.atLeastOneOf(
      JsonPointer.compile("/expectedDepartureDate"),
      JsonPointer.compile("/expectedArrivalAtPlaceOfDeliveryStartDate"),
      JsonPointer.compile("/expectedArrivalAtPlaceOfDeliveryEndDate"),
      JsonPointer.compile("/carrierExportVoyageNumber")
    ),
    JsonAttribute.atLeastOneOf(
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
      "The 'imoClass' values must be from dataset",
      allDg(dg -> dg.path("imoClass").submitPath()),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.DG_IMO_CLASSES)
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
      "The charges 'currencyCode' values must be from dataset",
      mav -> mav.submitAllMatching("charges.*.currencyCode"),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.ISO_4217_CURRENCY_CODES)
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
    AMF_MTC_VALIDATIONS,
    SHIPMENT_CUTOFF_TIMES_UNIQUE,
    CHECK_CONFIRMED_BOOKING_FIELDS,
    VALIDATE_SHIPMENT_LOCATIONS,
    REQUESTED_CHANGES_PRESENCE,
    REASON_FIELD_PRESENCE,
    REASON_FIELD_ABSENCE
  );

  public static ActionCheck responseContentChecks(UUID matched, String standardVersion, Supplier<CarrierScenarioParameters> cspSupplier, Supplier<DynamicScenarioParameters> dspSupplier, BookingState bookingStatus, BookingState amendedBookingState, Boolean requestAmendedContent) {
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
          "The commoditySubreference is not present for confirmed booking",
          mav -> mav.submitAllMatching("requestedEquipments.*.commodities.*"),
          (nodeToValidate, contextPath) -> {
            var commoditySubreference = nodeToValidate.path("commoditySubreference");
            if (commoditySubreference.isMissingNode() || commoditySubreference.isNull()) {
              return Set.of("The commoditySubreference at %s is not present for confirmed booking".formatted(contextPath));
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


