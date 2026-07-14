package org.dcsa.conformance.standards.booking.checks;

import static org.dcsa.conformance.standards.booking.checks.AbstractCarrierPayloadConformanceCheck.FEEDBACKS;
import static org.dcsa.conformance.standards.booking.checks.BookingDataSets.FEEDBACKS_CODE;
import static org.dcsa.conformance.standards.booking.checks.BookingDataSets.FEEDBACKS_SEVERITY;
import static org.dcsa.conformance.standards.booking.checks.BookingDataSets.NATIONAL_COMMODITY_TYPE_CODES;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.math.BigDecimal;
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
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.booking.party.*;
import org.dcsa.conformance.standardscommons.party.BookingDynamicScenarioParameters;

@UtilityClass
public class BookingChecks {

  private static final Set<BookingState> CONFIRMED_BOOKING_STATES =
      Set.of(BookingState.CONFIRMED, BookingState.PENDING_AMENDMENT);

  private static final String CARRIER_BOOKING_REQUEST_REFERENCE = "carrierBookingRequestReference";
  private static final String CARRIER_BOOKING_REFERENCE = "carrierBookingReference";
  private static final String ATTR_AMENDED_BOOKING_STATUS = "amendedBookingStatus";
  private static final String ATTR_BOOKING_CANCELLATION_STATUS = "bookingCancellationStatus";
  private static final String DANGEROUS_GOODS = "dangerousGoods";
  private static final String COMMODITIES = "commodities";
  private static final String REQUESTED_EQUIPMENTS = "requestedEquipments";
  private static final String NATIONAL_COMMODITY_CODES = "nationalCommodityCodes";
  private static final String EXTENDED_NATIONAL_COMMODITY_CODES = "extendedNationalCommodityCodes";
  private static final String TYPE = "type";
  private static final String ISO_EQUIPMENT_CODE = "ISOEquipmentCode";
  private static final String ACTIVE_REEFER_SETTINGS = "activeReeferSettings";
  private static final String IS_NON_OPERATING_REEFER = "isNonOperatingReefer";
  private static final String ROUTING_REFERENCE = "routingReference";
  private static final String EXPECTED_ARRIVAL_AT_PLACE_OF_DELIVERY_START_DATE =
      "expectedArrivalAtPlaceOfDeliveryStartDate";
  private static final String EXPECTED_ARRIVAL_AT_PLACE_OF_DELIVERY_END_DATE =
      "expectedArrivalAtPlaceOfDeliveryEndDate";
  private static final String SHIPMENT_CUT_OFF_TIMES = "shipmentCutOffTimes";
  private static final String RECEIPT_TYPE_AT_ORIGIN = "receiptTypeAtOrigin";
  private static final String VESSEL = "vessel";
  private static final String REFERENCES = "references";
  private static final String OUTER_PACKAGING = "outerPackaging";
  private static final String DOCUMENT_PARTIES = "documentParties";
  private static final String OTHER = "other";
  private static final String PARTY_FUNCTION = "partyFunction";
  private static final String UNIVERSAL_EXPORT_VOYAGE_REFERENCE = "universalExportVoyageReference";
  private static final String UNIVERSAL_IMPORT_VOYAGE_REFERENCE = "universalImportVoyageReference";
  private static final String UNIVERSAL_SERVICE_REFERENCE1 = "universalServiceReference";
  private static final String COMMODITY_SUB_REFERENCE = "commoditySubReference";
  private static final String CUT_OFF_DATE_TIME_CODE = "cutOffDateTimeCode";
  private static final String COUNTRY_CODE = "countryCode";
  private static final String MANIFEST_TYPE_CODE = "manifestTypeCode";
  private static final String ADVANCE_MANIFEST_FILINGS = "advanceManifestFilings";
  private static final String SHIPMENT_LOCATIONS = "shipmentLocations";
  private static final String DELIVERY_TYPE_AT_DESTINATION = "deliveryTypeAtDestination";
  private static final String CONTAINER_POSITIONINGS = "containerPositionings";
  private static final String DATE_TIME = "dateTime";
  private static final String NAME = "name";
  private static final String CARRIER_EXPORT_VOYAGE_NUMBER = "carrierExportVoyageNumber";
  private static final String CARRIER_SERVICE_CODE = "carrierServiceCode";
  private static final String CARRIER_SERVICE_NAME = "carrierServiceName";
  private static final String EXPECTED_DEPARTURE_DATE = "expectedDepartureDate";
  private static final String EXPECTED_DEPARTURE_FROM_PLACE_OF_RECEIPT_DATE =
      "expectedDepartureFromPlaceOfReceiptDate";
  private static final String VESSEL_IMO_NUMBER = "vesselIMONumber";
  private static final String LOCATION_TYPE_CODE = "locationTypeCode";
  private static final String BOOKING_STATUS = "bookingStatus";
  private static final String CONFIRMED_EQUIPMENTS = "confirmedEquipments";
  private static final String TRANSPORT_PLAN = "transportPlan";
  private static final String CARGO_GROSS_WEIGHT = "cargoGrossWeight";
  private static final String CONTRACT_QUOTATION_REFERENCE = "contractQuotationReference";
  private static final String SERVICE_CONTRACT_REFERENCE = "serviceContractReference";
  private static final String CARGO_MOVEMENT_TYPE_AT_ORIGIN = "cargoMovementTypeAtOrigin";
  private static final String CARGO_MOVEMENT_TYPE_AT_DESTINATION = "cargoMovementTypeAtDestination";
  private static final String SEVERITY = "severity";
  private static final String CODE = "code";
  private static final String PACKAGE_CODE = "packageCode";
  private static final String IMO_PACKAGING_CODE = "imoPackagingCode";
  private static final String NUMBER_OF_PACKAGES = "numberOfPackages";
  private static final String SEGREGATION_GROUPS = "segregationGroups";
  private static final String INHALATION_ZONE = "inhalationZone";
  private static final String DECLARED_VALUE = "declaredValue";
  private static final String DECLARED_VALUE_CURRENCY = "declaredValueCurrency";
  private static final String CHARGES = "charges";
  private static final String CURRENCY_AMOUNT = "currencyAmount";
  private static final String IDENTIFYING_CODES = "identifyingCodes";
  private static final String CODE_LIST_PROVIDER = "codeListProvider";
  private static final String PARTY = "party";
  private static final String BOOKING_AGENT = "bookingAgent";
  private static final String SHIPPER = "shipper";
  private static final String CONSIGNEE = "consignee";
  private static final String SERVICE_CONTRACT_OWNER = "serviceContractOwner";
  private static final String ISSUE_TO = "issueTo";
  private static final String MODE_OF_TRANSPORT = "modeOfTransport";
  private static final String REQUESTED_PRE_CARRIAGE_MODE_OF_TRANSPORT =
      "requestedPreCarriageModeOfTransport";
  private static final String REQUESTED_ON_CARRIAGE_MODE_OF_TRANSPORT =
      "requestedOnCarriageModeOfTransport";
  private static final String TRANSPORT_PLAN_STAGE_SEQUENCE_NUMBER =
      "transportPlanStageSequenceNumber";
  private static final String INNER_PACKAGINGS = "innerPackagings";
  private static final String QUANTITY = "quantity";
  private static final String EMPTY_CONTAINER_PICKUP = "emptyContainerPickup";
  private static final String TARE_WEIGHT = "tareWeight";
  private static final String IS_SHIPPER_OWNED = "isShipperOwned";
  private static final String PLACE_OF_BL_ISSUE = "placeOfBLIssue";
  private static final String INVOICE_PAYABLE_AT = "invoicePayableAt";
  private static final String PARTY_CONTACT_DETAILS = "partyContactDetails";
  private static final String UN_LOCATION_CODE = "UNLocationCode";
  private static final String SEND_TO_PLATFORM = "sendToPlatform";
  private static final String TRANSPORT_DOCUMENT_TYPE_CODE = "transportDocumentTypeCode";
  private static final String IS_ELECTRONIC = "isElectronic";
  private static final String PHONE = "phone";
  private static final String EMAIL = "email";
  private static final String EXPORT_LICENSE = "exportLicense";
  private static final String IS_REQUIRED = "isRequired";
  private static final String REFERENCE = "reference";
  private static final String TEMPERATURE_SETPOINT = "temperatureSetpoint";
  private static final String TEMPERATURE_UNIT = "temperatureUnit";
  private static final String AIR_EXCHANGE = "airExchange";
  private static final String AIR_EXCHANGE_UNIT = "airExchangeUnit";
  private static final String ESTIMATED_DATE_TIME = "estimatedDateTime";

  private static final String S_MUST_NOT_BE_PROVIDED_WHEN_S_IS_PROVIDED =
      "'%s' must not be provided when '%s' is provided.";
  private static final String S_FOR_CONFIRMED_BOOKING_IS_NOT_PRESENT =
      "'%s' for confirmed booking is not present";

  private static final String S_S_S = "%s.*.%s.*.%s";
  private static final String S_S = "%s.*.%s";
  private static final String THE_SCENARIO_REQUIRES_S_S_TO_BE_ABSENT =
      "The scenario requires '%s.%s' to be absent";

  public static ActionCheck requestContentChecks(
      UUID matched,
      String standardVersion,
      Supplier<BookingDynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<>(STATIC_BOOKING_CHECKS);
    checks.add(SHIPPER_REFERENCE_TYPE_VALIDATION);
    checks.addAll(generateScenarioRelatedChecks(dspSupplier));

    return JsonAttribute.contentChecks(
        BookingRole::isShipper, matched, HttpMessageType.REQUEST, standardVersion, checks);
  }

  private static final JsonRebasableContentCheck NATIONAL_COMMODITY_TYPE_CODE_VALIDATION =
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'type' attribute in each 'nationalCommodityCodes' object must demonstrate the correct use of a DCSA national commodity classification code: NCM, HTS, SCHEDULE_B, TARIC, CN, or CUS",
      mav -> mav.submitAllMatching("%s.*.%s.*".formatted(REQUESTED_EQUIPMENTS, COMMODITIES)),
      JsonAttribute.validateDeprecatedUnlessReplaced(
        EXTENDED_NATIONAL_COMMODITY_CODES,
        JsonAttribute.allMatched(
          NATIONAL_COMMODITY_CODES,
          JsonAttribute.path(
            TYPE,
            JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              NATIONAL_COMMODITY_TYPE_CODES)))));

  private static final JsonRebasableContentCheck EXTENDED_NATIONAL_COMMODITY_TYPE_CODE_VALIDATION =
    JsonAttribute.allIndividualMatchesMustBeValid(
      "The 'type' attribute in each 'extendedNationalCommodityCodes' object must demonstrate the correct use of a DCSA national commodity classification code: NCM, HTS, SCHEDULE_B, TARIC, CN, or CUS",
      mav ->
        mav.submitAllMatching(
          "%s.*.%s.*.%s.*.%s"
            .formatted(
              REQUESTED_EQUIPMENTS, COMMODITIES, EXTENDED_NATIONAL_COMMODITY_CODES, TYPE)),
      JsonAttribute.matchedMustBeDatasetKeywordIfPresent(NATIONAL_COMMODITY_TYPE_CODES));

  private static final Predicate<JsonNode> IS_ISO_EQUIPMENT_CONTAINER_REEFER =
      uteNode -> {
        var isoEquipmentNode = uteNode.path(ISO_EQUIPMENT_CODE);
        return isReeferContainerSizeTypeCode(isoEquipmentNode.asText(""));
      };

  private static final Predicate<JsonNode> HAS_ISO_EQUIPMENT_CODE =
      reqEquipNode -> {
        var isoEquipmentNode = reqEquipNode.path(ISO_EQUIPMENT_CODE);
        return isoEquipmentNode.isTextual();
      };

  private static final Predicate<JsonNode> IS_ACTIVE_REEFER_SETTINGS_REQUIRED =
      reqEquipNode -> {
        var norNode = reqEquipNode.path(IS_NON_OPERATING_REEFER);
        if (HAS_ISO_EQUIPMENT_CODE.test(reqEquipNode)
            && IS_ISO_EQUIPMENT_CONTAINER_REEFER.test(reqEquipNode)) {
          return !norNode.isMissingNode() && !norNode.asBoolean(false);
        }
        return false;
      };

  private static final Consumer<MultiAttributeValidator> ALL_REQ_EQUIP =
      mav -> mav.submitAllMatching("%s.*".formatted(REQUESTED_EQUIPMENTS));

  static final JsonContentCheck NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER =
      JsonAttribute.customValidator(
          "The 'requestedEquipments.activeReeferSettings' object must only be used when the standard allows it: only applicable when 'isNonOperatingReefer' is set to false",
          body -> {
            var requestedEquipments = body.path(REQUESTED_EQUIPMENTS);
            var errors = new LinkedHashSet<ConformanceError>();
            var index = new AtomicInteger(0);

            StreamSupport.stream(requestedEquipments.spliterator(), false)
                .forEach(
                    reqEquipNode -> {
                      int currentIndex = index.getAndIncrement();
                      if (IS_ACTIVE_REEFER_SETTINGS_REQUIRED.test(reqEquipNode)) {
                        if (JsonUtil.isMissingOrEmpty(reqEquipNode.path(ACTIVE_REEFER_SETTINGS))) {
                          errors.add(
                              ConformanceError.error(
                                  "The attribute '%s[%d].%s' should have been present but was absent"
                                      .formatted(
                                          REQUESTED_EQUIPMENTS,
                                          currentIndex,
                                          ACTIVE_REEFER_SETTINGS)));
                        }
                      } else {
                        errors.add(ConformanceError.irrelevant(currentIndex));
                      }
                    });

            return ConformanceCheckResult.withRelevance(errors);
          });

  private static final JsonContentCheck ISO_EQUIPMENT_CODE_AND_NOR_CHECK =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'requestedEquipments.isNonOperatingReefer' attribute must only be used when the standard allows it: only applicable if ISOEquipmentCode shows a Reefer type",
          ALL_REQ_EQUIP,
          JsonAttribute.ifMatchedThen(
              IS_ISO_EQUIPMENT_CONTAINER_REEFER,
              JsonAttribute.path(IS_NON_OPERATING_REEFER, JsonAttribute.matchedMustBePresent())));

  private static final JsonContentCheck UNIVERSAL_SERVICE_REFERENCE =
      JsonAttribute.customValidator(
          "The 'universalServiceReference' attribute must demonstrate the correct use of this conditional requirement: if either universalExportVoyageReference or universalImportVoyageReference is present, then universalServiceReference is required",
          body -> {
            var universalExportVoyageReference = body.path(UNIVERSAL_EXPORT_VOYAGE_REFERENCE);
            var universalImportVoyageReference = body.path(UNIVERSAL_IMPORT_VOYAGE_REFERENCE);
            var universalServiceReference = body.path(UNIVERSAL_SERVICE_REFERENCE1);
            if (!JsonUtil.isMissingOrEmpty(body.path(ROUTING_REFERENCE))) {
              var issues = new LinkedHashSet<String>();
              if (JsonAttribute.isJsonNodePresent(universalExportVoyageReference)) {
                issues.add(
                    S_MUST_NOT_BE_PROVIDED_WHEN_S_IS_PROVIDED.formatted(
                        UNIVERSAL_EXPORT_VOYAGE_REFERENCE, ROUTING_REFERENCE));
              }
              if (JsonAttribute.isJsonNodePresent(universalImportVoyageReference)) {
                issues.add(
                    S_MUST_NOT_BE_PROVIDED_WHEN_S_IS_PROVIDED.formatted(
                        UNIVERSAL_IMPORT_VOYAGE_REFERENCE, ROUTING_REFERENCE));
              }
              if (JsonAttribute.isJsonNodePresent(universalServiceReference)) {
                issues.add(
                    S_MUST_NOT_BE_PROVIDED_WHEN_S_IS_PROVIDED.formatted(
                        UNIVERSAL_SERVICE_REFERENCE1, ROUTING_REFERENCE));
              }
              return ConformanceCheckResult.simple(issues);
            }
            if (JsonUtil.isMissingOrEmpty(universalImportVoyageReference)
                && JsonUtil.isMissingOrEmpty(universalExportVoyageReference)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            if (JsonAttribute.isJsonNodeAbsent(universalServiceReference)) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "The %s must be present as either %s or %s are present"
                          .formatted(
                              UNIVERSAL_SERVICE_REFERENCE1,
                              UNIVERSAL_EXPORT_VOYAGE_REFERENCE,
                              UNIVERSAL_IMPORT_VOYAGE_REFERENCE)));
            }

            return ConformanceCheckResult.simple(Set.of());
          });

  private static JsonContentCheck referenceTypeValidation(
      String description, KeywordDataset allowedReferenceTypes) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        description,
        mav -> {
          mav.submitAllMatching(S_S_S.formatted(REQUESTED_EQUIPMENTS, REFERENCES, TYPE));
          mav.submitAllMatching(
              "%s.*.%s.*.%s.*.%s".formatted(REQUESTED_EQUIPMENTS, COMMODITIES, REFERENCES, TYPE));
          mav.submitAllMatching(S_S.formatted(REFERENCES, TYPE));
        },
        JsonAttribute.matchedMustBeDatasetKeywordIfPresent(allowedReferenceTypes));
  }

  private static final JsonContentCheck SHIPPER_REFERENCE_TYPE_VALIDATION =
      referenceTypeValidation(
          "The 'type' attribute in each shipper-provided reference object must demonstrate the correct use of a DCSA reference type code: CR, AKG, or AEF",
          BookingDataSets.SHIPPER_REFERENCE_TYPES);

  private static final JsonContentCheck CARRIER_REFERENCE_TYPE_VALIDATION =
      referenceTypeValidation(
          "The 'type' attribute in each reference object in the Booking response or Booking Notification must demonstrate the correct use of a DCSA reference type code: CR, ECR, AKG, or AEF",
          BookingDataSets.CARRIER_REFERENCE_TYPES);

  private static Consumer<MultiAttributeValidator> allDg(
      Consumer<MultiAttributeValidator.AttributePathBuilder> consumer) {
    return mav ->
        consumer.accept(
            mav.path(REQUESTED_EQUIPMENTS)
                .all()
                .path(COMMODITIES)
                .all()
                .path(OUTER_PACKAGING)
                .path(DANGEROUS_GOODS)
                .all());
  }

  static final JsonContentCheck COMMODITIES_SUBREFERENCE_UNIQUE =
      JsonAttribute.customValidator(
          "Each '%s' must be unique across the entire booking".formatted(COMMODITY_SUB_REFERENCE),
          body -> {
            var subReferenceCount = new HashMap<String, Integer>();

            StreamSupport.stream(body.path(REQUESTED_EQUIPMENTS).spliterator(), false)
                .flatMap(
                    equipment ->
                        StreamSupport.stream(equipment.path(COMMODITIES).spliterator(), false))
                .map(commodity -> commodity.path(COMMODITY_SUB_REFERENCE).asText(""))
                .filter(subRef -> !subRef.isBlank())
                .forEach(subRef -> subReferenceCount.merge(subRef, 1, Integer::sum));

            return ConformanceCheckResult.simple(
                subReferenceCount.entrySet().stream()
                    .filter(entry -> entry.getValue() > 1)
                    .map(
                        entry ->
                            "%s '%s' is not unique across the booking. Found %d occurrences."
                                .formatted(
                                    COMMODITY_SUB_REFERENCE, entry.getKey(), entry.getValue()))
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
          });

  private static final JsonContentCheck VALIDATE_ALLOWED_SHIPMENT_CUTOFF_CODE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'shipmentCutOffTimes.cutOffDateTimeCode' attribute must demonstrate the correct use of a cut-off time code: DCO, VCO, FCO, LCO, or EFC",
          mav ->
              mav.submitAllMatching(S_S.formatted(SHIPMENT_CUT_OFF_TIMES, CUT_OFF_DATE_TIME_CODE)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              BookingDataSets.CUTOFF_DATE_TIME_CODES));

  private static final JsonContentCheck VALIDATE_SHIPMENT_CUTOFF_TIME_CODE =
      JsonAttribute.customValidator(
          "The 'shipmentCutOffTimes.cutOffDateTimeCode' attribute must demonstrate the correct use of this conditional requirement: only when the Receipt Type at Origin is CFS, EFC (Earliest full-container delivery date)",
          body -> {
            var shipmentCutOffTimes = body.path(SHIPMENT_CUT_OFF_TIMES);
            var receiptTypeAtOrigin = body.path(RECEIPT_TYPE_AT_ORIGIN).asText("");
            var issues = new LinkedHashSet<String>();
            var cutOffDateTimeCodes =
                StreamSupport.stream(shipmentCutOffTimes.spliterator(), false)
                    .map(p -> p.path(CUT_OFF_DATE_TIME_CODE))
                    .filter(JsonNode::isTextual)
                    .map(n -> n.asText(""))
                    .collect(Collectors.toSet());
            if (!receiptTypeAtOrigin.equals("CFS")) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            if (!cutOffDateTimeCodes.contains("EFC")) {
              issues.add(
                  "'%s' 'EFC' (Earliest full-container delivery date) must be present when '%s' is 'CFS'"
                      .formatted(CUT_OFF_DATE_TIME_CODE, RECEIPT_TYPE_AT_ORIGIN));
            }
            return ConformanceCheckResult.simple(issues);
          });

  private static final Consumer<MultiAttributeValidator> ALL_AMF =
      mav -> mav.submitAllMatching(ADVANCE_MANIFEST_FILINGS);

  private static final JsonContentCheck ADVANCED_MANIFEST_FILING_CODES_UNIQUE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'advanceManifestFilings' object must demonstrate the correct use of this conditional requirement: the combination of '%s' and '%s' MUST be unique"
              .formatted(COUNTRY_CODE, MANIFEST_TYPE_CODE),
          ALL_AMF,
          JsonAttribute.unique(COUNTRY_CODE, MANIFEST_TYPE_CODE));

  private static final JsonContentCheck VALIDATE_SHIPMENT_LOCATIONS =
      JsonAttribute.customValidator(
          "The 'shipmentLocations' object must demonstrate the correct use of these conditional requirements: a Port of Discharge (PDE/POD) and a Port of Load (PRE/POL) must be provided; when receiptTypeAtOrigin is SD, a Place of Receipt (PRE) and container positioning dateTime are required; when deliveryTypeAtDestination is SD, a Place of Delivery (PDE) is required",
          body -> {
            var issues = new LinkedHashSet<String>();
            var routingReference = body.path(ROUTING_REFERENCE).asText("");
            var receiptTypeAtOrigin = body.path(RECEIPT_TYPE_AT_ORIGIN).asText("");
            var deliveryTypeAtDestination = body.path(DELIVERY_TYPE_AT_DESTINATION).asText("");
            var polNode = getShipmentLocationTypeCode(body, "POL");
            var preNode = getShipmentLocationTypeCode(body, "PRE");
            var pdeNode = getShipmentLocationTypeCode(body, "PDE");
            var podNode = getShipmentLocationTypeCode(body, "POD");

            if (!routingReference.isBlank() && !"SD".equals(receiptTypeAtOrigin)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }

            if (routingReference.isBlank()) {
              if (pdeNode.isMissingNode() && podNode.isMissingNode()) {
                issues.add("Port of Discharge value must be provided (PDE or POD)");
              }
              if (preNode.isMissingNode() && polNode.isMissingNode()) {
                issues.add("Port of Load value must be provided (PRE or POL)");
              }
              if (pdeNode.isMissingNode() && "SD".equals(deliveryTypeAtDestination)) {
                issues.add(
                    "Place of Delivery value must be provided (PDE) when '%s' is 'SD'"
                        .formatted(DELIVERY_TYPE_AT_DESTINATION));
              }
              if (preNode.isMissingNode() && "SD".equals(receiptTypeAtOrigin)) {
                issues.add(
                    "Place of Receipt value must be provided (PRE) when '%s' is 'SD'"
                        .formatted(RECEIPT_TYPE_AT_ORIGIN));
              }
            }
            if ("SD".equals(receiptTypeAtOrigin)) {
              var requestedEquipments = body.path(REQUESTED_EQUIPMENTS);
              StreamSupport.stream(requestedEquipments.spliterator(), false)
                  .forEach(
                      element -> {
                        var containerPositionings = element.path(CONTAINER_POSITIONINGS);
                        var containerPositionsDateTime =
                            StreamSupport.stream(containerPositionings.spliterator(), false)
                                .filter(o -> !o.path(DATE_TIME).asText("").isEmpty())
                                .findFirst()
                                .orElse(null);
                        if (containerPositionsDateTime == null) {
                          issues.add(
                              "When %s is 'SD' (Store Door), '%s.%s.%s' is required"
                                  .formatted(
                                      RECEIPT_TYPE_AT_ORIGIN,
                                      REQUESTED_EQUIPMENTS,
                                      CONTAINER_POSITIONINGS,
                                      DATE_TIME));
                        }
                      });
            }
            return ConformanceCheckResult.simple(issues);
          });

  private static final JsonContentCheck VALIDATE_SHIPPER_MINIMUM_REQUEST_FIELDS =
      JsonAttribute.customValidator(
          "Validate shipper's minimum request fields",
          body -> {
            var issues = new LinkedHashSet<String>();

            var routingReference = body.path(ROUTING_REFERENCE).asText("");
            if (!routingReference.isBlank()) {
              return ConformanceCheckResult.simple(routingReferenceRequestFieldsChecks(body));
            }

            var vesselName = body.path(VESSEL).path(NAME).asText("");
            var carrierExportVoyageNumber = body.path(CARRIER_EXPORT_VOYAGE_NUMBER).asText("");
            var carrierServiceCode = body.path(CARRIER_SERVICE_CODE).asText("");
            var carrierServiceName = body.path(CARRIER_SERVICE_NAME).asText("");
            var expectedDepartureDate = body.path(EXPECTED_DEPARTURE_DATE).asText("");
            var expectedDepartureFromPlaceOfReceiptDate =
                body.path(EXPECTED_DEPARTURE_FROM_PLACE_OF_RECEIPT_DATE).asText("");

            var polNode = getShipmentLocationTypeCode(body, "POL");
            var preNode = getShipmentLocationTypeCode(body, "PRE");
            var pdeNode = getShipmentLocationTypeCode(body, "PDE");
            var podNode = getShipmentLocationTypeCode(body, "POD");

            var providedArrivalStartDate =
                body.path(EXPECTED_ARRIVAL_AT_PLACE_OF_DELIVERY_START_DATE).asText("");
            var providedArrivalEndDate =
                body.path(EXPECTED_ARRIVAL_AT_PLACE_OF_DELIVERY_END_DATE).asText("");

            if (pdeNode.isMissingNode() && podNode.isMissingNode()) {
              issues.add("Port of Discharge value must be provided (PDE or POD)");
            }
            if (preNode.isMissingNode() && polNode.isMissingNode()) {
              issues.add("Port of Load values must be provided (PRE or POL)");
            }

            // Check minimum mandatory property combinations
            var hasExpectedDepartureDate = !expectedDepartureDate.isEmpty();
            var hasExpectedDepartureFromPlaceOfReceiptDate =
                !expectedDepartureFromPlaceOfReceiptDate.isEmpty();
            var hasArrivalDates =
                !providedArrivalStartDate.isEmpty() && !providedArrivalEndDate.isEmpty();
            var hasVoyageAndVessel = !carrierExportVoyageNumber.isEmpty() && !vesselName.isEmpty();
            var hasVoyageAndServiceName =
                !carrierExportVoyageNumber.isEmpty() && !carrierServiceName.isEmpty();
            var hasVoyageAndServiceCode =
                !carrierExportVoyageNumber.isEmpty() && !carrierServiceCode.isEmpty();

            if (!hasExpectedDepartureDate
                && !hasExpectedDepartureFromPlaceOfReceiptDate
                && !hasArrivalDates
                && !hasVoyageAndVessel
                && !hasVoyageAndServiceName
                && !hasVoyageAndServiceCode) {
              issues.add(
                  "At least one of the minimum mandatory property combinations must be provided: "
                      + "%s, %s, "
                          .formatted(
                              EXPECTED_DEPARTURE_DATE,
                              EXPECTED_DEPARTURE_FROM_PLACE_OF_RECEIPT_DATE)
                      + "expectedArrival dates (both start and end), "
                      + "%s + %s, ".formatted(CARRIER_EXPORT_VOYAGE_NUMBER, NAME)
                      + "%s + %s, or ".formatted(CARRIER_EXPORT_VOYAGE_NUMBER, CARRIER_SERVICE_NAME)
                      + "%s + %s.".formatted(CARRIER_EXPORT_VOYAGE_NUMBER, CARRIER_SERVICE_CODE));
            }

            return ConformanceCheckResult.simple(issues);
          });

  private static Set<String> routingReferenceRequestFieldsChecks(JsonNode body) {
    var issues = new LinkedHashSet<String>();

    var vesselName = body.path(VESSEL).path(NAME).asText("");
    var vesselIMONumber = body.path(VESSEL).path(VESSEL_IMO_NUMBER).asText("");
    var carrierExportVoyageNumber = body.path(CARRIER_EXPORT_VOYAGE_NUMBER).asText("");
    var carrierServiceCode = body.path(CARRIER_SERVICE_CODE).asText("");
    var carrierServiceName = body.path(CARRIER_SERVICE_NAME).asText("");
    var expectedDepartureDate = body.path(EXPECTED_DEPARTURE_DATE).asText("");
    var expectedDepartureFromPlaceOfReceiptDate =
        body.path(EXPECTED_DEPARTURE_FROM_PLACE_OF_RECEIPT_DATE).asText("");

    var polNode = getShipmentLocationTypeCode(body, "POL");
    var preNode = getShipmentLocationTypeCode(body, "PRE");
    var pdeNode = getShipmentLocationTypeCode(body, "PDE");
    var podNode = getShipmentLocationTypeCode(body, "POD");

    var providedArrivalStartDate =
        body.path(EXPECTED_ARRIVAL_AT_PLACE_OF_DELIVERY_START_DATE).asText("");
    var providedArrivalEndDate =
        body.path(EXPECTED_ARRIVAL_AT_PLACE_OF_DELIVERY_END_DATE).asText("");

    if (!vesselName.isBlank()) {
      issues.add(
          "'%s.%s' must not be provided when '%s is provided."
              .formatted(VESSEL, NAME, ROUTING_REFERENCE));
    }
    if (!vesselIMONumber.isBlank()) {
      issues.add(
          "'%s.%s' must not be provided when '%s' is provided."
              .formatted(VESSEL, VESSEL_IMO_NUMBER, ROUTING_REFERENCE));
    }
    if (!carrierServiceName.isBlank()) {
      issues.add(
          S_MUST_NOT_BE_PROVIDED_WHEN_S_IS_PROVIDED.formatted(
              CARRIER_SERVICE_NAME, ROUTING_REFERENCE));
    }
    if (!carrierServiceCode.isBlank()) {
      issues.add(
          S_MUST_NOT_BE_PROVIDED_WHEN_S_IS_PROVIDED.formatted(
              CARRIER_SERVICE_CODE, ROUTING_REFERENCE));
    }
    if (!carrierExportVoyageNumber.isBlank()) {
      issues.add(
          S_MUST_NOT_BE_PROVIDED_WHEN_S_IS_PROVIDED.formatted(
              CARRIER_EXPORT_VOYAGE_NUMBER, ROUTING_REFERENCE));
    }
    if (!expectedDepartureDate.isBlank()) {
      issues.add(
          S_MUST_NOT_BE_PROVIDED_WHEN_S_IS_PROVIDED.formatted(
              EXPECTED_DEPARTURE_DATE, ROUTING_REFERENCE));
    }
    if (!expectedDepartureFromPlaceOfReceiptDate.isBlank()) {
      issues.add(
          S_MUST_NOT_BE_PROVIDED_WHEN_S_IS_PROVIDED.formatted(
              EXPECTED_DEPARTURE_FROM_PLACE_OF_RECEIPT_DATE, ROUTING_REFERENCE));
    }
    if (!providedArrivalStartDate.isBlank()) {
      issues.add(
          S_MUST_NOT_BE_PROVIDED_WHEN_S_IS_PROVIDED.formatted(
              EXPECTED_ARRIVAL_AT_PLACE_OF_DELIVERY_START_DATE, ROUTING_REFERENCE));
    }
    if (!providedArrivalEndDate.isBlank()) {
      issues.add(
          S_MUST_NOT_BE_PROVIDED_WHEN_S_IS_PROVIDED.formatted(
              EXPECTED_ARRIVAL_AT_PLACE_OF_DELIVERY_END_DATE, ROUTING_REFERENCE));
    }
    if (!preNode.isMissingNode()) {
      issues.add(
          "'%s.%s' 'PRE' must not be provided when '%s' is provided."
              .formatted(SHIPMENT_LOCATIONS, LOCATION_TYPE_CODE, ROUTING_REFERENCE));
    }
    if (!polNode.isMissingNode()) {
      issues.add(
          "'%s.%s''POL' must not be provided when '%s' is provided."
              .formatted(SHIPMENT_LOCATIONS, LOCATION_TYPE_CODE, ROUTING_REFERENCE));
    }
    if (!pdeNode.isMissingNode()) {
      issues.add(
          "'%s.%s' 'PDE' must not be provided when '%s' is provided."
              .formatted(SHIPMENT_LOCATIONS, LOCATION_TYPE_CODE, ROUTING_REFERENCE));
    }
    if (!podNode.isMissingNode()) {
      issues.add(
          "'%s.%s' 'POD' must not be provided when '%s' is provided."
              .formatted(SHIPMENT_LOCATIONS, LOCATION_TYPE_CODE, ROUTING_REFERENCE));
    }
    return issues;
  }

  private static JsonNode getShipmentLocationTypeCode(
      JsonNode body, @NonNull String locationTypeCode) {
    var shipmentLocations = body.path(SHIPMENT_LOCATIONS);
    return StreamSupport.stream(shipmentLocations.spliterator(), false)
        .filter(o -> o.path(LOCATION_TYPE_CODE).asText("").equals(locationTypeCode))
        .findFirst()
        .orElse(MissingNode.getInstance());
  }

  static final JsonContentCheck FEEDBACKS_PRESENCE =
      JsonAttribute.customValidator(
          "The 'feedbacks' attribute must be provided when bookingStatus is PENDING_UPDATE or PENDING_AMENDMENT; it is optional for all other booking statuses",
          body -> {
            var bookingStatus = body.path(BOOKING_STATUS).asText("");
            var issues = new LinkedHashSet<ConformanceError>();
            if (BookingState.PENDING_UPDATE.name().equals(bookingStatus)
                || BookingState.PENDING_AMENDMENT.name().equals(bookingStatus)) {
              if (JsonUtil.isMissingOrEmpty(body.path(FEEDBACKS))) {
                issues.add(
                    ConformanceError.error(
                        "'%s' is missing in the '%s' '%s'"
                            .formatted(FEEDBACKS, BOOKING_STATUS, bookingStatus)));
              }
            } else {
              issues.add(ConformanceError.irrelevant());
            }
            return ConformanceCheckResult.withRelevance(issues);
          });

  static final JsonContentCheck CHECK_CONFIRMED_BOOKING_FIELDS =
      JsonAttribute.customValidator(
          "The 'confirmedEquipments', 'transportPlan' and 'shipmentCutOffTimes' must be provided (mandatory and non-empty) when bookingStatus is CONFIRMED or PENDING_AMENDMENT",
          body -> {
            var issues = new LinkedHashSet<String>();
            var bookingStatusAttribute = body.path(BOOKING_STATUS).asText("");
            BookingState bookingStatus = BookingState.fromString(bookingStatusAttribute);
            if (Objects.isNull(bookingStatus)) {
              issues.add(
                  "Invalid or empty 'bookingStatus' attribute value: '%s'"
                      .formatted(bookingStatusAttribute));
              return ConformanceCheckResult.simple(issues);
            }
            if (!CONFIRMED_BOOKING_STATES.contains(bookingStatus)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            if (body.path(CONFIRMED_EQUIPMENTS).isEmpty()) {
              issues.add(S_FOR_CONFIRMED_BOOKING_IS_NOT_PRESENT.formatted(CONFIRMED_EQUIPMENTS));
            }
            if (body.path(TRANSPORT_PLAN).isEmpty()) {
              issues.add(S_FOR_CONFIRMED_BOOKING_IS_NOT_PRESENT.formatted(TRANSPORT_PLAN));
            }
            if (body.path(SHIPMENT_CUT_OFF_TIMES).isEmpty()) {
              issues.add(S_FOR_CONFIRMED_BOOKING_IS_NOT_PRESENT.formatted(SHIPMENT_CUT_OFF_TIMES));
            }
            return ConformanceCheckResult.simple(issues);
          });

  static final JsonContentCheck CHECK_CARGO_GROSS_WEIGHT_CONDITIONS =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'requestedEquipments.cargoGrossWeight' object must be provided when not provided on Commodity level",
          mav -> mav.submitAllMatching("%s.*".formatted(REQUESTED_EQUIPMENTS)),
          (nodeToValidate, contextPath) -> {
            var issues = new LinkedHashSet<String>();
            var cargoGrossWeight = nodeToValidate.path(CARGO_GROSS_WEIGHT);
            if (!JsonUtil.isMissingOrEmpty(cargoGrossWeight)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            var commodities = nodeToValidate.path(COMMODITIES);
            if (JsonUtil.isMissingOrEmpty(commodities)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            AtomicInteger commodityCounter = new AtomicInteger(0);
            StreamSupport.stream(commodities.spliterator(), false)
                .forEach(
                    commodity -> {
                      var commodityGrossWeight = commodity.path(CARGO_GROSS_WEIGHT);
                      int currentCommodityCount = commodityCounter.getAndIncrement();
                      if (commodityGrossWeight.isMissingNode() || commodityGrossWeight.isNull()) {
                        issues.add(
                            "The '%s' must have '%s' at '%s' position %s"
                                .formatted(
                                    contextPath,
                                    CARGO_GROSS_WEIGHT,
                                    COMMODITIES,
                                    currentCommodityCount));
                      }
                    });
            return ConformanceCheckResult.simple(issues);
          });

  public static List<JsonContentCheck> generateScenarioRelatedChecks(
      Supplier<BookingDynamicScenarioParameters> dspSupplier) {
    List<JsonContentCheck> checks = new ArrayList<>();

    var scenario = ScenarioType.valueOf(dspSupplier.get().scenarioType());
    boolean isScenarioRoutingReference = ScenarioType.ROUTING_REFERENCE.equals(scenario);
    boolean isScenarioStoreDoorAtOrigin = ScenarioType.STORE_DOOR_AT_ORIGIN.equals(scenario);
    boolean isScenarioStoreDoorAtDestination =
        ScenarioType.STORE_DOOR_AT_DESTINATION.equals(scenario);
    boolean isScenarioReefer = ScenarioType.REEFER.equals(scenario);
    boolean isScenarioNonOperatingReefer = ScenarioType.NON_OPERATING_REEFER.equals(scenario);
    boolean isScenarioDG = ScenarioType.DG.equals(scenario);

    checks.add(
        JsonAttribute.customValidator(
            "[Scenario] Verify that a '%s' is present".formatted(ROUTING_REFERENCE),
            isScenarioRoutingReference,
            body -> {
              var issues = new LinkedHashSet<String>();
              var routingReference = body.path(ROUTING_REFERENCE).asText("");
              if (routingReference.isBlank()) {
                issues.add(
                    "The scenario requires the booking to have a '%s'"
                        .formatted(ROUTING_REFERENCE));
              }
              return ConformanceCheckResult.simple(issues);
            }));

    checks.add(
        JsonAttribute.customValidator(
            "[Scenario] Store door at origin scenario requirements",
            isScenarioStoreDoorAtOrigin,
            body -> {
              var issues = new LinkedHashSet<>(validateStoreDoorCommonRequirements(body));
              var receiptTypeAtOrigin = body.path(RECEIPT_TYPE_AT_ORIGIN).asText("");
              if (!"SD".equals(receiptTypeAtOrigin)) {
                issues.add(
                    "The scenario requires the '%s' to be 'SD'".formatted(RECEIPT_TYPE_AT_ORIGIN));
              }
              var preNode = getShipmentLocationTypeCode(body, "PRE");
              if (preNode.isMissingNode()) {
                issues.add("The scenario requires Port of Load value to be 'PRE'");
              }
              return ConformanceCheckResult.simple(issues);
            }));

    checks.add(
        JsonAttribute.customValidator(
            "[Scenario] Store door at destination scenario requirements",
            isScenarioStoreDoorAtDestination,
            body -> {
              var issues = new LinkedHashSet<>(validateStoreDoorCommonRequirements(body));
              var deliveryTypeAtDestination = body.path(DELIVERY_TYPE_AT_DESTINATION).asText("");
              if (!"SD".equals(deliveryTypeAtDestination)) {
                issues.add(
                    "The scenario requires the '%s' to be 'SD'"
                        .formatted(DELIVERY_TYPE_AT_DESTINATION));
              }
              var pdeNode = getShipmentLocationTypeCode(body, "PDE");
              if (pdeNode.isMissingNode()) {
                issues.add("The scenario requires Port of Discharge value to be 'PDE'");
              }
              return ConformanceCheckResult.simple(issues);
            }));

    checks.add(
        JsonAttribute.customValidator(
            "[Scenario] Verify that the correct '%s'/'%s' is used"
                .formatted(CONTRACT_QUOTATION_REFERENCE, SERVICE_CONTRACT_REFERENCE),
            body -> {
              var contractQuotationReference = body.path(CONTRACT_QUOTATION_REFERENCE).asText("");
              var serviceContractReference = body.path(SERVICE_CONTRACT_REFERENCE).asText("");
              if (!contractQuotationReference.isEmpty() && !serviceContractReference.isEmpty()) {
                return ConformanceCheckResult.simple(
                    Set.of(
                        "The scenario requires either of '%s'/'%s'"
                                .formatted(CONTRACT_QUOTATION_REFERENCE, SERVICE_CONTRACT_REFERENCE)
                            + " to be present, but not both"));
              }
              return ConformanceCheckResult.simple(Set.of());
            }));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "[Scenario] Reefer scenario container validation",
            isScenarioReefer,
            mav -> mav.submitAllMatching("%s.*".formatted(REQUESTED_EQUIPMENTS)),
            (nodeToValidate, contextPath) -> {
              var issues = new LinkedHashSet<String>();
              reeferContainerChecks(contextPath, nodeToValidate, issues);
              return ConformanceCheckResult.simple(issues);
            }));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "[Scenario] Non-operating reefer scenario container validation",
            isScenarioNonOperatingReefer,
            mav -> mav.submitAllMatching("%s.*".formatted(REQUESTED_EQUIPMENTS)),
            (nodeToValidate, contextPath) -> {
              var issues = new LinkedHashSet<String>();
              nonOperatingReeferContainerChecks(contextPath, nodeToValidate, issues);
              return ConformanceCheckResult.simple(issues);
            }));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "[Scenario] Default container scenario validation",
            !isScenarioReefer && !isScenarioNonOperatingReefer,
            mav -> mav.submitAllMatching("%s.*".formatted(REQUESTED_EQUIPMENTS)),
            (nodeToValidate, contextPath) -> {
              var issues = new LinkedHashSet<String>();
              defaultContainerChecks(contextPath, nodeToValidate, issues);
              return ConformanceCheckResult.simple(issues);
            }));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "[Scenario] DG scenario requires dangerous goods to be present",
            isScenarioDG,
            mav ->
                mav.path(REQUESTED_EQUIPMENTS)
                    .all()
                    .path(COMMODITIES)
                    .all()
                    .path(OUTER_PACKAGING)
                    .path(DANGEROUS_GOODS)
                    .submitPath(),
            (nodeToValidate, contextPath) -> {
              if (!nodeToValidate.isArray() || nodeToValidate.isEmpty()) {
                return ConformanceCheckResult.simple(
                    Set.of(
                        "The scenario requires '%s' to contain dangerous goods"
                            .formatted(contextPath)));
              }
              return ConformanceCheckResult.simple(Set.of());
            }));

    checks.add(
        JsonAttribute.allIndividualMatchesMustBeValid(
            "[Scenario] Non-DG scenarios require dangerous goods to be absent",
            !isScenarioDG,
            mav ->
                mav.path(REQUESTED_EQUIPMENTS)
                    .all()
                    .path(COMMODITIES)
                    .all()
                    .path(OUTER_PACKAGING)
                    .path(DANGEROUS_GOODS)
                    .submitPath(),
            (nodeToValidate, contextPath) -> {
              if (!nodeToValidate.isMissingNode() && !nodeToValidate.isEmpty()) {
                return ConformanceCheckResult.simple(
                    Set.of(
                        "The scenario requires '%s' to NOT contain any dangerous goods"
                            .formatted(contextPath)));
              }
              return ConformanceCheckResult.simple(Set.of());
            }));

    return checks;
  }

  private static Set<String> validateStoreDoorCommonRequirements(JsonNode body) {
    var issues = new LinkedHashSet<String>();
    var cargoMovementTypeAtOrigin = body.path(CARGO_MOVEMENT_TYPE_AT_ORIGIN).asText("");
    var cargoMovementTypeAtDestination = body.path(CARGO_MOVEMENT_TYPE_AT_DESTINATION).asText("");

    if (!"FCL".equals(cargoMovementTypeAtOrigin)) {
      issues.add(
          "The scenario requires the '%s' to be 'FCL'".formatted(CARGO_MOVEMENT_TYPE_AT_ORIGIN));
    }
    if (!"FCL".equals(cargoMovementTypeAtDestination)) {
      issues.add(
          "The scenario requires the '%s' to be 'FCL'"
              .formatted(CARGO_MOVEMENT_TYPE_AT_DESTINATION));
    }

    return issues;
  }

  private static void defaultContainerChecks(
      String contextPath, JsonNode nodeToValidate, Set<String> issues) {
    var activeReeferNode = nodeToValidate.path(ACTIVE_REEFER_SETTINGS);
    var nonOperatingReeferNode = nodeToValidate.path(IS_NON_OPERATING_REEFER);
    if (!activeReeferNode.isMissingNode()) {
      issues.add(
          THE_SCENARIO_REQUIRES_S_S_TO_BE_ABSENT.formatted(contextPath, ACTIVE_REEFER_SETTINGS));
    }
    if (!nonOperatingReeferNode.isMissingNode()) {
      issues.add(
          THE_SCENARIO_REQUIRES_S_S_TO_BE_ABSENT.formatted(contextPath, IS_NON_OPERATING_REEFER));
    }
  }

  private static void nonOperatingReeferContainerChecks(
      String contextPath, JsonNode nodeToValidate, Set<String> issues) {
    var activeReeferNode = nodeToValidate.path(ACTIVE_REEFER_SETTINGS);
    var nonOperatingReeferNode = nodeToValidate.path(IS_NON_OPERATING_REEFER);
    var isoEquipmentNode = nodeToValidate.path(ISO_EQUIPMENT_CODE);

    if (!nonOperatingReeferNode.asBoolean(false)) {
      issues.add(
          "The scenario requires '%s.%s' to be true"
              .formatted(contextPath, IS_NON_OPERATING_REEFER));
    }
    if (!activeReeferNode.isMissingNode()) {
      issues.add(
          THE_SCENARIO_REQUIRES_S_S_TO_BE_ABSENT.formatted(contextPath, ACTIVE_REEFER_SETTINGS));
    }
    if (!isReeferContainerSizeTypeCode(isoEquipmentNode.asText(""))) {
      issues.add(
          "The scenario requires '%s.%s' to be a valid reefer container type"
              .formatted(ISO_EQUIPMENT_CODE, contextPath));
    }
  }

  private static void reeferContainerChecks(
      String contextPath, JsonNode nodeToValidate, Set<String> issues) {
    var activeReeferNode = nodeToValidate.path(ACTIVE_REEFER_SETTINGS);
    if (!activeReeferNode.isObject()) {
      issues.add(
          "The scenario requires '%s.%s' to be present"
              .formatted(contextPath, ACTIVE_REEFER_SETTINGS));
    }
  }

  static final JsonRebasableContentCheck VALID_FEEDBACK_SEVERITY =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'feedbacks.severity' attribute must demonstrate the correct use of a feedback severity code: INFO, WARN, or ERROR",
          mav -> mav.submitAllMatching(S_S.formatted(FEEDBACKS, SEVERITY)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(FEEDBACKS_SEVERITY));

  static final JsonRebasableContentCheck VALID_FEEDBACK_CODE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'feedbacks.code' attribute must demonstrate the correct use of a feedback code: INFORMATIONAL_MESSAGE, PROPERTY_WILL_BE_IGNORED, PROPERTY_VALUE_MUST_CHANGE, PROPERTY_VALUE_HAS_BEEN_CHANGED, PROPERTY_VALUE_MAY_CHANGE, or PROPERTY_HAS_BEEN_DELETED",
          mav -> mav.submitAllMatching(S_S.formatted(FEEDBACKS, CODE)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(FEEDBACKS_CODE));

  private static JsonContentCheck positiveIntegerCheck(
      String description, Consumer<MultiAttributeValidator> pathSelector) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        description,
        pathSelector,
        (nodeToValidate, contextPath) -> {
          if (!nodeToValidate.isIntegralNumber() || nodeToValidate.asLong() <= 0) {
            return ConformanceCheckResult.simple(
                Set.of("'%s' must be a positive integer".formatted(contextPath)));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static final JsonContentCheck OTHER_PARTY_FUNCTION_CODE_VALIDATION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The documentParties.other.partyFunction attribute must demonstrate the correct use of an other document party function code: DDR, DDS, COW, COX, N1, N2, NI, NAC, or CSR",
          mav ->
              mav.submitAllMatching(
                  "%s.%s.*.%s".formatted(DOCUMENT_PARTIES, OTHER, PARTY_FUNCTION)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              BookingDataSets.OTHER_PARTY_FUNCTION_CODES));

  private static final JsonContentCheck CODE_LIST_PROVIDER_VALIDATION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The identifyingCodes.codeListProvider attribute must demonstrate the correct use of a code list provider code: WAVE, CARX, ESSD, IDT, BOLE, EDOX, IQAX, SECR, TRGO, ETEU, TRAC, BRIT, COVA, ETIT, KTNE, CRED, BLOC, DOCU, AEOT, SGTD, GSBN, WISE, GLEIF, W3C, DNB, FMC, DCSA, or ZZZ",
          mav -> {
            for (String party :
                new String[] {
                  BOOKING_AGENT, SHIPPER, CONSIGNEE, SERVICE_CONTRACT_OWNER, ISSUE_TO
                }) {
              mav.submitAllMatching(
                  "%s.%s.%s.*.%s"
                      .formatted(DOCUMENT_PARTIES, party, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
            }
            mav.submitAllMatching(
                "%s.%s.*.%s.%s.*.%s"
                    .formatted(
                        DOCUMENT_PARTIES, OTHER, PARTY, IDENTIFYING_CODES, CODE_LIST_PROVIDER));
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              BookingDataSets.CODE_LIST_PROVIDER_CODES));

  private static final JsonContentCheck SHIPMENT_LOCATION_TYPE_CODE_VALIDATION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The shipmentLocations.locationTypeCode attribute must demonstrate the correct use of a shipment location type code: PRE, POL, POD, PDE, PCF, OIR, ORI, IEL, PTP, RTP, FCD, or ROU",
          mav -> mav.submitAllMatching(S_S.formatted(SHIPMENT_LOCATIONS, LOCATION_TYPE_CODE)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              BookingDataSets.SHIPMENT_LOCATION_TYPES));

  private static final JsonContentCheck REQUESTED_CARRIAGE_MODE_OF_TRANSPORT_VALIDATION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The requestedPreCarriageModeOfTransport / requestedOnCarriageModeOfTransport attribute must demonstrate the correct use of a mode of transport code: VESSEL, RAIL, TRUCK, BARGE, RAIL_TRUCK, BARGE_TRUCK, BARGE_RAIL, or MULTIMODAL",
          mav -> {
            mav.submitAllMatching(REQUESTED_PRE_CARRIAGE_MODE_OF_TRANSPORT);
            mav.submitAllMatching(REQUESTED_ON_CARRIAGE_MODE_OF_TRANSPORT);
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.MODE_OF_TRANSPORT));

  private static final JsonContentCheck INNER_PACKAGING_QUANTITY_POSITIVE_INTEGER =
      positiveIntegerCheck(
          "The innerPackaging.quantity attribute must be a positive integer",
          mav ->
              mav.submitAllMatching(
                  "%s.*.%s.*.%s.%s.*.%s.*.%s"
                      .formatted(
                          REQUESTED_EQUIPMENTS,
                          COMMODITIES,
                          OUTER_PACKAGING,
                          DANGEROUS_GOODS,
                          INNER_PACKAGINGS,
                          QUANTITY)));

  private static final JsonContentCheck TRANSPORT_PLAN_MODE_OF_TRANSPORT_VALIDATION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The transportPlan.modeOfTransport attribute must demonstrate the correct use of a mode of transport code: VESSEL, RAIL, TRUCK, BARGE, RAIL_TRUCK, BARGE_TRUCK, BARGE_RAIL, or MULTIMODAL",
          mav -> mav.submitAllMatching(S_S.formatted(TRANSPORT_PLAN, MODE_OF_TRANSPORT)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.MODE_OF_TRANSPORT));

  private static final JsonContentCheck TRANSPORT_PLAN_STAGE_SEQUENCE_NUMBER_POSITIVE_INTEGER =
      positiveIntegerCheck(
          "The transportPlan.transportPlanStageSequenceNumber attribute must be a positive integer",
          mav ->
              mav.submitAllMatching(
                  S_S.formatted(TRANSPORT_PLAN, TRANSPORT_PLAN_STAGE_SEQUENCE_NUMBER)));

  private static final JsonContentCheck CARGO_MOVEMENT_TYPE_AT_ORIGIN_VALIDATION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'cargoMovementTypeAtOrigin' attribute must demonstrate the correct use of a cargo movement type code: FCL or LCL",
          mav -> mav.submitAllMatching(CARGO_MOVEMENT_TYPE_AT_ORIGIN),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.CARGO_MOVEMENT_TYPE));

  private static final JsonContentCheck CARGO_MOVEMENT_TYPE_AT_DESTINATION_VALIDATION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'cargoMovementTypeAtDestination' attribute must demonstrate the correct use of a cargo movement type code: FCL or LCL",
          mav -> mav.submitAllMatching(CARGO_MOVEMENT_TYPE_AT_DESTINATION),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.CARGO_MOVEMENT_TYPE));

  private static final JsonContentCheck BOOKING_STATUS_CODE_VALIDATION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'bookingStatus' attribute must demonstrate the correct use of a booking status code: RECEIVED, PENDING_UPDATE, UPDATE_RECEIVED, CONFIRMED, PENDING_AMENDMENT, REJECTED, DECLINED, CANCELLED, or COMPLETED",
          mav -> mav.submitAllMatching(BOOKING_STATUS),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.BOOKING_STATUS));

  private static final JsonContentCheck AMENDED_BOOKING_STATUS_CODE_VALIDATION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'amendedBookingStatus' attribute must demonstrate the correct use of an amended booking status code: AMENDMENT_RECEIVED, AMENDMENT_CONFIRMED, AMENDMENT_DECLINED, or AMENDMENT_CANCELLED",
          mav -> mav.submitAllMatching(ATTR_AMENDED_BOOKING_STATUS),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.AMENDED_BOOKING_STATUS));

  private static final JsonContentCheck BOOKING_CANCELLATION_STATUS_CODE_VALIDATION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'bookingCancellationStatus' attribute must demonstrate the correct use of a booking cancellation status code: CANCELLATION_RECEIVED, CANCELLATION_DECLINED, or CANCELLATION_CONFIRMED",
          mav -> mav.submitAllMatching(ATTR_BOOKING_CANCELLATION_STATUS),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              BookingDataSets.BOOKING_CANCELLATION_STATUS));

  private static final JsonContentCheck CONTAINER_POSITIONINGS_ONLY_FOR_SD =
      JsonAttribute.customValidator(
          "(if included) The 'requestedEquipments.containerPositionings' attribute must only be used when the standard allows it: only applicable to carrier haulage service at origin (receiptTypeAtOrigin = 'SD')",
          body -> {
            var receiptTypeAtOrigin = body.path(RECEIPT_TYPE_AT_ORIGIN).asText("");
            if ("SD".equals(receiptTypeAtOrigin)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            var issues = new LinkedHashSet<String>();
            var requestedEquipments = body.path(REQUESTED_EQUIPMENTS);
            for (int i = 0; i < requestedEquipments.size(); i++) {
              if (!JsonUtil.isMissingOrEmpty(
                  requestedEquipments.path(i).path(CONTAINER_POSITIONINGS))) {
                issues.add(
                    "'%s[%d].%s' must be absent when '%s' is not 'SD'"
                        .formatted(
                            REQUESTED_EQUIPMENTS, i, CONTAINER_POSITIONINGS, RECEIPT_TYPE_AT_ORIGIN));
              }
            }
            return ConformanceCheckResult.simple(issues);
          });

  private static final JsonContentCheck EMPTY_CONTAINER_PICKUP_ONLY_FOR_CY =
      JsonAttribute.customValidator(
          "(if included) The 'requestedEquipments.emptyContainerPickup' attribute must only be used when the standard allows it: only applicable to merchant haulage service at origin (receiptTypeAtOrigin = 'CY')",
          body -> {
            var receiptTypeAtOrigin = body.path(RECEIPT_TYPE_AT_ORIGIN).asText("");
            if ("CY".equals(receiptTypeAtOrigin)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            var issues = new LinkedHashSet<String>();
            var requestedEquipments = body.path(REQUESTED_EQUIPMENTS);
            for (int i = 0; i < requestedEquipments.size(); i++) {
              if (!JsonUtil.isMissingOrEmpty(
                  requestedEquipments.path(i).path(EMPTY_CONTAINER_PICKUP))) {
                issues.add(
                    "'%s[%d].%s' must be absent when '%s' is not 'CY'"
                        .formatted(
                            REQUESTED_EQUIPMENTS, i, EMPTY_CONTAINER_PICKUP, RECEIPT_TYPE_AT_ORIGIN));
              }
            }
            return ConformanceCheckResult.simple(issues);
          });

  private static final JsonContentCheck TARE_WEIGHT_REQUIRED_FOR_SOC =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'requestedEquipments.tareWeight' object must be provided when the condition applies: in case of Shipper Owned Containers (isShipperOwned = true) this is a required property",
          mav -> mav.submitAllMatching("%s.*".formatted(REQUESTED_EQUIPMENTS)),
          (nodeToValidate, contextPath) -> {
            if (!nodeToValidate.path(IS_SHIPPER_OWNED).asBoolean(false)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            if (JsonUtil.isMissingOrEmpty(nodeToValidate.path(TARE_WEIGHT))) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "'%s.%s' is required when '%s' is 'true' (Shipper Owned Container)"
                          .formatted(contextPath, TARE_WEIGHT, IS_SHIPPER_OWNED)));
            }
            return ConformanceCheckResult.simple(Set.of());
          });

  private static final JsonContentCheck SEND_TO_PLATFORM_ONLY_FOR_ELECTRONIC_BOL =
      JsonAttribute.customValidator(
          "(if included) The 'documentParties.issueTo.sendToPlatform' attribute must only be used when the standard allows it: only applicable when isElectronic=true and transportDocumentTypeCode=BOL; the property must be absent for paper B/Ls",
          body -> {
            var sendToPlatform =
                body.path(DOCUMENT_PARTIES).path(ISSUE_TO).path(SEND_TO_PLATFORM);
            if (JsonUtil.isMissingOrEmpty(sendToPlatform)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            var issues = new LinkedHashSet<String>();
            var transportDocumentTypeCode = body.path(TRANSPORT_DOCUMENT_TYPE_CODE).asText("");
            if (!"BOL".equals(transportDocumentTypeCode)) {
              issues.add(
                  "'%s.%s.%s' is only allowed when '%s' is 'BOL'"
                      .formatted(
                          DOCUMENT_PARTIES, ISSUE_TO, SEND_TO_PLATFORM, TRANSPORT_DOCUMENT_TYPE_CODE));
            }
            var isElectronic = body.path(IS_ELECTRONIC);
            if (isElectronic.isBoolean() && !isElectronic.asBoolean()) {
              issues.add(
                  "'%s.%s.%s' must be absent for paper B/Ls ('%s' is 'false')"
                      .formatted(DOCUMENT_PARTIES, ISSUE_TO, SEND_TO_PLATFORM, IS_ELECTRONIC));
            }
            return ConformanceCheckResult.simple(issues);
          });

  private static final JsonContentCheck PLACE_OF_BL_ISSUE_UNLOCATION_XOR_COUNTRY =
      JsonAttribute.customValidator(
          "The 'placeOfBLIssue' object must demonstrate the correct use of this conditional requirement: the location can be specified as one of UNLocationCode or countryCode, but not both",
          body -> {
            var placeOfBLIssue = body.path(PLACE_OF_BL_ISSUE);
            if (JsonUtil.isMissingOrEmpty(placeOfBLIssue)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            var hasUnLocationCode =
                !JsonUtil.isMissingOrEmpty(placeOfBLIssue.path(UN_LOCATION_CODE));
            var hasCountryCode = !JsonUtil.isMissingOrEmpty(placeOfBLIssue.path(COUNTRY_CODE));
            if (hasUnLocationCode == hasCountryCode) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "'%s' must contain exactly one of '%s' or '%s', but not both"
                          .formatted(PLACE_OF_BL_ISSUE, UN_LOCATION_CODE, COUNTRY_CODE)));
            }
            return ConformanceCheckResult.simple(Set.of());
          });

  private static final JsonContentCheck INVOICE_PAYABLE_AT_MUST_USE_UN_LOCATION_CODE =
      JsonAttribute.customValidator(
          "The 'invoicePayableAt' object must demonstrate the correct use of this conditional requirement: the location must be provided as a UNLocationCode",
          body -> {
            var invoicePayableAt = body.path(INVOICE_PAYABLE_AT);
            if (JsonUtil.isMissingOrEmpty(invoicePayableAt)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            if (JsonUtil.isMissingOrEmpty(invoicePayableAt.path(UN_LOCATION_CODE))) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "'%s.%s' must be provided"
                          .formatted(INVOICE_PAYABLE_AT, UN_LOCATION_CODE)));
            }
            return ConformanceCheckResult.simple(Set.of());
          });

  private static final JsonContentCheck PARTY_CONTACT_DETAILS_NAME_AND_PHONE_OR_EMAIL =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'partyContactDetails' object must be provided when the condition applies: it is mandatory to provide either phone and/or email along with the name",
          mav -> mav.submitAllMatching("%s.*".formatted(PARTY_CONTACT_DETAILS)),
          (nodeToValidate, contextPath) -> {
            var issues = new LinkedHashSet<String>();
            if (JsonUtil.isMissingOrEmpty(nodeToValidate.path(NAME))) {
              issues.add("'%s.%s' is mandatory".formatted(contextPath, NAME));
            }
            if (JsonUtil.isMissingOrEmpty(nodeToValidate.path(PHONE))
                && JsonUtil.isMissingOrEmpty(nodeToValidate.path(EMAIL))) {
              issues.add(
                  "'%s' must provide either '%s' and/or '%s'"
                      .formatted(contextPath, PHONE, EMAIL));
            }
            return ConformanceCheckResult.simple(issues);
          });

  private static final JsonContentCheck EXPORT_LICENSE_REFERENCE_WHEN_REQUIRED =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The 'requestedEquipments.commodities.exportLicense.reference' attribute must demonstrate the correct use of this conditional requirement: the reference is required when an Export License or permit is required (isRequired = true)",
          mav ->
              mav.submitAllMatching(
                  "%s.*.%s.*.%s".formatted(REQUESTED_EQUIPMENTS, COMMODITIES, EXPORT_LICENSE)),
          (nodeToValidate, contextPath) -> {
            if (!nodeToValidate.path(IS_REQUIRED).asBoolean(false)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            if (JsonUtil.isMissingOrEmpty(nodeToValidate.path(REFERENCE))) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "'%s.%s' is required when '%s' is 'true'"
                          .formatted(contextPath, REFERENCE, IS_REQUIRED)));
            }
            return ConformanceCheckResult.simple(Set.of());
          });

  private static JsonContentCheck onlyApplicableToDangerousGoods(String field) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "(if included) The 'requestedEquipments.commodities.outerPackaging.%s' attribute must only be used when the standard allows it: only applicable to dangerous goods"
            .formatted(field),
        mav ->
            mav.submitAllMatching(
                S_S_S.formatted(REQUESTED_EQUIPMENTS, COMMODITIES, OUTER_PACKAGING)),
        (nodeToValidate, contextPath) -> {
          if (JsonUtil.isMissingOrEmpty(nodeToValidate.path(field))) {
            return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
          }
          if (JsonUtil.isMissingOrEmpty(nodeToValidate.path(DANGEROUS_GOODS))) {
            return ConformanceCheckResult.simple(
                Set.of(
                    "'%s.%s' is only applicable to dangerous goods"
                        .formatted(contextPath, field)));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static final JsonContentCheck PACKAGE_CODE_ONLY_FOR_DG =
      onlyApplicableToDangerousGoods(PACKAGE_CODE);
  private static final JsonContentCheck IMO_PACKAGING_CODE_ONLY_FOR_DG =
      onlyApplicableToDangerousGoods(IMO_PACKAGING_CODE);

  // A unit attribute is required when its corresponding value attribute is provided.
  private static JsonContentCheck reeferSettingUnitRequiredWhenValuePresent(
      String valueField, String unitField) {
    return JsonAttribute.allIndividualMatchesMustBeValid(
        "The 'requestedEquipments.activeReeferSettings.%s' attribute must be provided when %s is provided; if %s is not provided, this field must be empty"
            .formatted(unitField, valueField, valueField),
        mav -> mav.submitAllMatching("%s.*.%s".formatted(REQUESTED_EQUIPMENTS, ACTIVE_REEFER_SETTINGS)),
        (nodeToValidate, contextPath) -> {
          var hasValue = !JsonUtil.isMissingOrEmpty(nodeToValidate.path(valueField));
          var hasUnit = !JsonUtil.isMissingOrEmpty(nodeToValidate.path(unitField));
          if (hasValue != hasUnit) {
            return ConformanceCheckResult.simple(
                Set.of(
                    "'%s.%s' must be provided if and only if '%s' is provided"
                        .formatted(contextPath, unitField, valueField)));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private static final JsonContentCheck ACTIVE_REEFER_TEMPERATURE_UNIT_CONDITIONAL =
      reeferSettingUnitRequiredWhenValuePresent(TEMPERATURE_SETPOINT, TEMPERATURE_UNIT);
  private static final JsonContentCheck ACTIVE_REEFER_AIR_EXCHANGE_UNIT_CONDITIONAL =
      reeferSettingUnitRequiredWhenValuePresent(AIR_EXCHANGE, AIR_EXCHANGE_UNIT);

  // confirmedEquipments.containerPositionings is only applicable to carrier haulage at origin (SD).
  private static final JsonContentCheck CONFIRMED_CONTAINER_POSITIONINGS_ONLY_FOR_SD =
      JsonAttribute.customValidator(
          "(if included) The 'confirmedEquipments.containerPositionings' attribute must only be used when the standard allows it: only applicable to carrier haulage service at origin (receiptTypeAtOrigin = 'SD')",
          body -> {
            var receiptTypeAtOrigin = body.path(RECEIPT_TYPE_AT_ORIGIN).asText("");
            if ("SD".equals(receiptTypeAtOrigin)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            var issues = new LinkedHashSet<String>();
            var confirmedEquipments = body.path(CONFIRMED_EQUIPMENTS);
            for (int i = 0; i < confirmedEquipments.size(); i++) {
              var containerPositionings = confirmedEquipments.path(i).path(CONTAINER_POSITIONINGS);
              if (!JsonUtil.isMissingOrEmpty(containerPositionings)) {
                issues.add(
                    "'%s[%d].%s' must be absent when '%s' is not 'SD'"
                        .formatted(
                            CONFIRMED_EQUIPMENTS, i, CONTAINER_POSITIONINGS, RECEIPT_TYPE_AT_ORIGIN));
                for (int j = 0; j < containerPositionings.size(); j++) {
                  if (!JsonUtil.isMissingOrEmpty(
                      containerPositionings.path(j).path(ESTIMATED_DATE_TIME))) {
                    issues.add(
                        "'%s[%d].%s[%d].%s' must be absent when '%s' is not 'SD'"
                            .formatted(
                                CONFIRMED_EQUIPMENTS,
                                i,
                                CONTAINER_POSITIONINGS,
                                j,
                                ESTIMATED_DATE_TIME,
                                RECEIPT_TYPE_AT_ORIGIN));
                  }
                }
              }
            }
            return ConformanceCheckResult.simple(issues);
          });

  // carrierBookingReference must be present except in the states where it is still optional.
  private static final Set<String> CBR_OPTIONAL_STATES =
      Set.of("RECEIVED", "REJECTED", "PENDING_UPDATE", "UPDATE_RECEIVED", "CANCELLED");

  private static final JsonContentCheck CARRIER_BOOKING_REFERENCE_PRESENCE_BY_STATE =
      JsonAttribute.customValidator(
          "The 'carrierBookingReference' attribute in the Booking response must demonstrate the correct use of this conditional requirement: carrierBookingReference MUST be present, except for the booking states where it is still optional: RECEIVED, REJECTED, PENDING_UPDATE, UPDATE_RECEIVED, or CANCELLED",
          body -> {
            var bookingStatus = body.path(BOOKING_STATUS).asText("");
            if (CBR_OPTIONAL_STATES.contains(bookingStatus)) {
              return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
            }
            if (JsonUtil.isMissingOrEmpty(body.path(CARRIER_BOOKING_REFERENCE))) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "'%s' must be present for booking status '%s'"
                          .formatted(CARRIER_BOOKING_REFERENCE, bookingStatus)));
            }
            return ConformanceCheckResult.simple(Set.of());
          });

  static final List<JsonContentCheck> STATIC_BOOKING_CHECKS =
      Arrays.asList(
          CARGO_MOVEMENT_TYPE_AT_ORIGIN_VALIDATION,
          CARGO_MOVEMENT_TYPE_AT_DESTINATION_VALIDATION,
          CONTAINER_POSITIONINGS_ONLY_FOR_SD,
          EMPTY_CONTAINER_PICKUP_ONLY_FOR_CY,
          TARE_WEIGHT_REQUIRED_FOR_SOC,
          PACKAGE_CODE_ONLY_FOR_DG,
          IMO_PACKAGING_CODE_ONLY_FOR_DG,
          ACTIVE_REEFER_TEMPERATURE_UNIT_CONDITIONAL,
          ACTIVE_REEFER_AIR_EXCHANGE_UNIT_CONDITIONAL,
          SEND_TO_PLATFORM_ONLY_FOR_ELECTRONIC_BOL,
          PLACE_OF_BL_ISSUE_UNLOCATION_XOR_COUNTRY,
          INVOICE_PAYABLE_AT_MUST_USE_UN_LOCATION_CODE,
          PARTY_CONTACT_DETAILS_NAME_AND_PHONE_OR_EMAIL,
          EXPORT_LICENSE_REFERENCE_WHEN_REQUIRED,
          NOR_PLUS_ISO_CODE_IMPLIES_ACTIVE_REEFER,
          ISO_EQUIPMENT_CODE_AND_NOR_CHECK,
          OTHER_PARTY_FUNCTION_CODE_VALIDATION,
          CODE_LIST_PROVIDER_VALIDATION,
          SHIPMENT_LOCATION_TYPE_CODE_VALIDATION,
          REQUESTED_CARRIAGE_MODE_OF_TRANSPORT_VALIDATION,
          INNER_PACKAGING_QUANTITY_POSITIVE_INTEGER,
          UNIVERSAL_SERVICE_REFERENCE,
          VALIDATE_SHIPMENT_CUTOFF_TIME_CODE,
          VALIDATE_ALLOWED_SHIPMENT_CUTOFF_CODE,
          VALIDATE_SHIPPER_MINIMUM_REQUEST_FIELDS,
          NATIONAL_COMMODITY_TYPE_CODE_VALIDATION,
          EXTENDED_NATIONAL_COMMODITY_TYPE_CODE_VALIDATION,
          CHECK_CARGO_GROSS_WEIGHT_CONDITIONS,
          JsonAttribute.xOrFields(
              "The contractQuotationReference / serviceContractReference must demonstrate the correct use of contractQuotationReference or serviceContractReference by providing exactly one of the alternatives",
              JsonPointer.compile("/%s".formatted(CONTRACT_QUOTATION_REFERENCE)),
              JsonPointer.compile("/%s".formatted(SERVICE_CONTRACT_REFERENCE))),
          JsonAttribute.allOrNoneArePresent(
              JsonPointer.compile(
                  "/%s".formatted(EXPECTED_ARRIVAL_AT_PLACE_OF_DELIVERY_START_DATE)),
              JsonPointer.compile("/%s".formatted(EXPECTED_ARRIVAL_AT_PLACE_OF_DELIVERY_END_DATE))),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "DangerousGoods implies '%s' or '%s'".formatted(PACKAGE_CODE, IMO_PACKAGING_CODE),
              mav ->
                  mav.submitAllMatching(
                      S_S_S.formatted(REQUESTED_EQUIPMENTS, COMMODITIES, OUTER_PACKAGING)),
              (nodeToValidate, contextPath) -> {
                var dg = nodeToValidate.path(DANGEROUS_GOODS);
                if (JsonUtil.isMissingOrEmpty(dg)) {
                  return ConformanceCheckResult.withRelevance(
                      Set.of(ConformanceError.irrelevant()));
                }
                if (nodeToValidate.path(PACKAGE_CODE).isMissingNode()
                    && nodeToValidate.path(IMO_PACKAGING_CODE).isMissingNode()) {
                  return ConformanceCheckResult.simple(
                      Set.of(
                          "The '%s' object did not have a '%s' nor an '%s', which is required due to dangerousGoods"
                              .formatted(contextPath, PACKAGE_CODE, IMO_PACKAGING_CODE)));
                }
                return ConformanceCheckResult.simple(Set.of());
              }),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The 'requestedEquipments.commodities.outerPackaging.numberOfPackages' attribute must be provided when the condition applies: in case this OuterPackaging includes Dangerous Goods the numberOfPackages is mandatory to provide",
              mav ->
                  mav.submitAllMatching(
                      S_S_S.formatted(REQUESTED_EQUIPMENTS, COMMODITIES, OUTER_PACKAGING)),
              (nodeToValidate, contextPath) -> {
                var dg = nodeToValidate.path(DANGEROUS_GOODS);
                if (JsonUtil.isMissingOrEmpty(dg)) {
                  return ConformanceCheckResult.withRelevance(
                      Set.of(ConformanceError.irrelevant()));
                }
                if (nodeToValidate.path(NUMBER_OF_PACKAGES).isMissingNode()) {
                  return ConformanceCheckResult.simple(
                      Set.of(
                          "The '%s' object did not have a '%s', which is required due to dangerousGoods"
                              .formatted(contextPath, NUMBER_OF_PACKAGES)));
                }
                return ConformanceCheckResult.simple(Set.of());
              }),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The dangerousGoods.segregationGroups values must demonstrate the correct use of the IMO IMDG segregation group codes listed by the standard",
              allDg(dg -> dg.path(SEGREGATION_GROUPS).all().submitPath()),
              JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                  BookingDataSets.DG_SEGREGATION_GROUPS)),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The dangerousGoods.inhalationZone attribute must demonstrate the correct use of an inhalation hazard zone: A, B, C, or D",
              allDg(dg -> dg.path(INHALATION_ZONE).all().submitPath()),
              JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                  BookingDataSets.INHALATION_ZONE_CODE)),
          JsonAttribute.allOrNoneArePresent(
              "The declaredValueCurrency attribute must be provided when declaredValue is provided; if declaredValue is not provided, this field must be empty",
              JsonPointer.compile("/%s".formatted(DECLARED_VALUE)),
              JsonPointer.compile("/%s".formatted(DECLARED_VALUE_CURRENCY))),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The charges.currencyAmount attribute must demonstrate the correct use of a monetary amount with no more than two decimal places",
              mav -> mav.submitAllMatching(S_S.formatted(CHARGES, CURRENCY_AMOUNT)),
              (nodeToValidate, contextPath) -> {
                var currencyAmount = nodeToValidate.asDouble();
                if (BigDecimal.valueOf(currencyAmount).scale() > 2) {
                  return ConformanceCheckResult.simple(
                      Set.of(
                          "%s must have at most 2 decimal point of precision"
                              .formatted(contextPath)));
                }
                return ConformanceCheckResult.simple(Set.of());
              }));

  private static final List<JsonContentCheck> RESPONSE_ONLY_CHECKS =
      Arrays.asList(
          ADVANCED_MANIFEST_FILING_CODES_UNIQUE,
          CHECK_CONFIRMED_BOOKING_FIELDS,
          CONFIRMED_CONTAINER_POSITIONINGS_ONLY_FOR_SD,
          CARRIER_BOOKING_REFERENCE_PRESENCE_BY_STATE,
          VALIDATE_SHIPMENT_LOCATIONS,
          FEEDBACKS_PRESENCE,
          VALID_FEEDBACK_SEVERITY,
          VALID_FEEDBACK_CODE,
          CARRIER_REFERENCE_TYPE_VALIDATION,
          TRANSPORT_PLAN_MODE_OF_TRANSPORT_VALIDATION,
          TRANSPORT_PLAN_STAGE_SEQUENCE_NUMBER_POSITIVE_INTEGER,
          BOOKING_STATUS_CODE_VALIDATION,
          AMENDED_BOOKING_STATUS_CODE_VALIDATION,
          BOOKING_CANCELLATION_STATUS_CODE_VALIDATION);

  public static ActionCheck responseContentChecks(
      UUID matched,
      String standardVersion,
      Supplier<BookingDynamicScenarioParameters> dspSupplier,
      BookingState bookingStatus,
      BookingState expectedAmendedBookingStatus,
      BookingCancellationState expectedCancelledBookingStatus) {
    var checks =
        fullPayloadChecks(
            dspSupplier,
            bookingStatus,
            expectedAmendedBookingStatus,
            expectedCancelledBookingStatus);

    return JsonAttribute.contentChecks(
        BookingRole::isCarrier, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  public static List<JsonContentCheck> fullPayloadChecks(
      Supplier<BookingDynamicScenarioParameters> dspSupplier,
      BookingState bookingStatus,
      BookingState expectedAmendedBookingStatus,
      BookingCancellationState expectedCancelledBookingStatus) {

    var checks = new ArrayList<JsonContentCheck>();

    checks.add(cbrValidation(dspSupplier));
    checks.add(cbrrValidation(dspSupplier));

    checks.add(
        JsonAttribute.mustEqual(
            JsonPointer.compile("/%s".formatted(BOOKING_STATUS)), bookingStatus.name()));

    checks.add(
        JsonAttribute.customValidator(
            "(if included) The 'amendedBookingStatus' attribute in the Booking response must only be used when the standard allows it: amendedBookingStatus and bookingCancellationStatus MUST NOT be present unless required by the applicable use case",
            body -> {
              JsonNode amendedBookingStatus = body.path(ATTR_AMENDED_BOOKING_STATUS);
              if (expectedAmendedBookingStatus == null) {
                if (!JsonUtil.isMissingOrEmpty(amendedBookingStatus)) {
                  return ConformanceCheckResult.simple(
                      Set.of(
                          "The '%s' should not be present, but response contains value '%s'"
                              .formatted(
                                  ATTR_AMENDED_BOOKING_STATUS, amendedBookingStatus.asText())));
                }
                return ConformanceCheckResult.simple(Set.of());
              }
              String amendedBookingStatusValue = amendedBookingStatus.asText("");
              if (!expectedAmendedBookingStatus.name().equals(amendedBookingStatusValue)) {
                return ConformanceCheckResult.simple(
                    Set.of(
                        "The expected '%s' %s is not equal to response '%s' %s"
                            .formatted(
                                ATTR_AMENDED_BOOKING_STATUS,
                                expectedAmendedBookingStatus.name(),
                                ATTR_AMENDED_BOOKING_STATUS,
                                amendedBookingStatusValue)));
              }
              return ConformanceCheckResult.simple(Set.of());
            }));

    checks.add(
        JsonAttribute.customValidator(
            "(if included) The 'bookingCancellationStatus' attribute in the Booking response must only be used when the standard allows it: amendedBookingStatus and bookingCancellationStatus MUST NOT be present unless required by the applicable use case",
            body -> {
              JsonNode bookingCancellationStatus = body.path(ATTR_BOOKING_CANCELLATION_STATUS);
              if (expectedCancelledBookingStatus == null) {
                if (!JsonUtil.isMissingOrEmpty(bookingCancellationStatus)) {
                  return ConformanceCheckResult.simple(
                      Set.of(
                          "The '%s' should not be present, but response contains value '%s'"
                              .formatted(
                                  ATTR_BOOKING_CANCELLATION_STATUS,
                                  bookingCancellationStatus.asText())));
                }
                return ConformanceCheckResult.simple(Set.of());
              }
              String bookingCancellationStatusValue = bookingCancellationStatus.asText("");
              if (!expectedCancelledBookingStatus.name().equals(bookingCancellationStatusValue)) {
                return ConformanceCheckResult.simple(
                    Set.of(
                        "The expected '%s' %s is not equal to response '%s' %s"
                            .formatted(
                                ATTR_BOOKING_CANCELLATION_STATUS,
                                expectedCancelledBookingStatus.name(),
                                ATTR_BOOKING_CANCELLATION_STATUS,
                                bookingCancellationStatusValue)));
              }
              return ConformanceCheckResult.simple(Set.of());
            }));

    checks.addAll(STATIC_BOOKING_CHECKS);

    checks.addAll(RESPONSE_ONLY_CHECKS);

    if (CONFIRMED_BOOKING_STATES.contains(bookingStatus)) {
      checks.add(COMMODITIES_SUBREFERENCE_UNIQUE);
      checks.add(
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The '%s' is not present for confirmed booking".formatted(COMMODITY_SUB_REFERENCE),
              mav ->
                  mav.submitAllMatching("%s.*.%s.*".formatted(REQUESTED_EQUIPMENTS, COMMODITIES)),
              (nodeToValidate, contextPath) -> {
                var commoditySubReference = nodeToValidate.path(COMMODITY_SUB_REFERENCE);
                if (commoditySubReference.isMissingNode() || commoditySubReference.isNull()) {
                  return ConformanceCheckResult.simple(
                      Set.of(
                          "The '%s' at %s is not present for confirmed booking"
                              .formatted(COMMODITY_SUB_REFERENCE, contextPath)));
                }
                return ConformanceCheckResult.simple(Set.of());
              }));
    }

    checks.addAll(generateScenarioRelatedChecks(dspSupplier));

    return checks;
  }

  public static JsonContentCheck cbrrOrCbr(Supplier<BookingDynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.customValidator(
        "The carrierBookingRequestReference / carrierBookingReference must demonstrate the correct use of the carrierBookingRequestReference or carrierBookingReference attribute by providing at least one of them",
        body -> {
          String cbrr = body.path(CARRIER_BOOKING_REQUEST_REFERENCE).asText("");
          String cbr = body.path(CARRIER_BOOKING_REFERENCE).asText("");
          String expectedCbrr = dspSupplier.get().carrierBookingRequestReference();
          String expectedCbr = dspSupplier.get().carrierBookingReference();
          if (!cbrr.equals(expectedCbrr) && !cbr.equals(expectedCbr)) {
            return ConformanceCheckResult.simple(
                Set.of(
                    "Either '%s' must equal %s or '%s' must equal %s."
                        .formatted(
                            CARRIER_BOOKING_REQUEST_REFERENCE,
                            expectedCbrr,
                            CARRIER_BOOKING_REFERENCE,
                            expectedCbr)));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck cbrValidation(
      Supplier<BookingDynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.customValidator(
        "The carrierBookingReference attribute in the Booking response must demonstrate the correct use of the carrierBookingRequestReference or carrierBookingReference attribute: carrierBookingRequestReference MUST equal the reference established for the scenario, or carrierBookingReference MUST equal the reference established for the scenario",
        body -> {
          String cbr = body.path(CARRIER_BOOKING_REFERENCE).asText("");
          String expectedCbr = dspSupplier.get().carrierBookingReference();
          if (expectedCbr == null) {
            return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
          }
          if (!cbr.equals(expectedCbr)) {
            return ConformanceCheckResult.simple(
                Set.of("'%s' must equal %s.".formatted(CARRIER_BOOKING_REFERENCE, expectedCbr)));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck cbrrValidation(
      Supplier<BookingDynamicScenarioParameters> dspSupplier) {
    return JsonAttribute.customValidator(
        "The carrierBookingRequestReference attribute in the Booking response must demonstrate the correct use of the carrierBookingRequestReference or carrierBookingReference attribute: carrierBookingRequestReference MUST equal the reference established for the scenario, or carrierBookingReference MUST equal the reference established for the scenario",
        body -> {
          String cbrr = body.path(CARRIER_BOOKING_REQUEST_REFERENCE).asText("");
          String expectedCbrr = dspSupplier.get().carrierBookingRequestReference();
          if (expectedCbrr == null) {
            return ConformanceCheckResult.withRelevance(Set.of(ConformanceError.irrelevant()));
          }
          if (!cbrr.equals(expectedCbrr)) {
            return ConformanceCheckResult.simple(
                Set.of(
                    "'%s' must equal %s."
                        .formatted(CARRIER_BOOKING_REQUEST_REFERENCE, expectedCbrr)));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  private boolean isReeferContainerSizeTypeCode(String isoEquipmentCode) {
    var codeChar = isoEquipmentCode.length() > 2 ? isoEquipmentCode.charAt(2) : '?';
    return codeChar == 'R' || codeChar == 'H';
  }
}
