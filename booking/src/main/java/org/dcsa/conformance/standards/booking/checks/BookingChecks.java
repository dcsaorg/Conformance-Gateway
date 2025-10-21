package org.dcsa.conformance.standards.booking.checks;

import static org.dcsa.conformance.standards.booking.checks.AbstractCarrierPayloadConformanceCheck.FEEDBACKS;
import static org.dcsa.conformance.standards.booking.checks.BookingDataSets.FEEDBACKS_CODE;
import static org.dcsa.conformance.standards.booking.checks.BookingDataSets.FEEDBACKS_SEVERITY;
import static org.dcsa.conformance.standards.booking.checks.BookingDataSets.NATIONAL_COMMODITY_TYPE_CODES;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
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
  private static final String IS_EXPORT_DECLARATION_REQUIRED = "isExportDeclarationRequired";
  private static final String EXPORT_DECLARATION_REFERENCE = "exportDeclarationReference";
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
    checks.addAll(generateScenarioRelatedChecks(dspSupplier));

    return JsonAttribute.contentChecks(
        BookingRole::isShipper, matched, HttpMessageType.REQUEST, standardVersion, checks);
  }

  private static final JsonRebaseableContentCheck NATIONAL_COMMODITY_TYPE_CODE_VALIDATION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate that '%s' of '%s' is a known code".formatted(TYPE, NATIONAL_COMMODITY_CODES),
          mav ->
              mav.submitAllMatching(
                  "%s.*.%s.*.%s.*.%s"
                      .formatted(
                          REQUESTED_EQUIPMENTS, COMMODITIES, NATIONAL_COMMODITY_CODES, TYPE)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(NATIONAL_COMMODITY_TYPE_CODES));

  private static final JsonContentCheck CHECK_EXPECTED_ARRIVAL_POD =
      JsonAttribute.customValidator(
          "Check expected arrival dates are valid",
          body -> {
            String providedArrivalStartDate =
                body.path(EXPECTED_ARRIVAL_AT_PLACE_OF_DELIVERY_START_DATE).asText("");
            String providedArrivalEndDate =
                body.path(EXPECTED_ARRIVAL_AT_PLACE_OF_DELIVERY_END_DATE).asText("");
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
            return ConformanceCheckResult.simple(
                invalidDates.stream()
                    .map("The expected arrival dates '%s' are not valid"::formatted)
                    .collect(Collectors.toSet()));
          });

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
          "All requested Equipments where '%s' is 'false' must have '%s'"
              .formatted(IS_NON_OPERATING_REEFER, ACTIVE_REEFER_SETTINGS),
          body -> {
            var requestedEquipments = body.path(REQUESTED_EQUIPMENTS);
            var errors = new LinkedHashSet<ConformanceError>();
            var index = new AtomicInteger(0);

            StreamSupport.stream(requestedEquipments.spliterator(), false)
                .forEach(
                    reqEquipNode -> {
                      if (IS_ACTIVE_REEFER_SETTINGS_REQUIRED.test(reqEquipNode)) {
                        if (JsonUtil.isMissingOrEmpty(reqEquipNode.path(ACTIVE_REEFER_SETTINGS))) {
                          errors.add(
                              ConformanceError.error(
                                  "The attribute '%s[%d].%s' should have been present but was absent"
                                      .formatted(
                                          REQUESTED_EQUIPMENTS,
                                          index.getAndIncrement(),
                                          ACTIVE_REEFER_SETTINGS)));
                        }
                      } else {
                        errors.add(ConformanceError.irrelevant(index.getAndIncrement()));
                      }
                    });

            return ConformanceCheckResult.withRelevance(errors);
          });

  private static final JsonContentCheck ISO_EQUIPMENT_CODE_AND_NOR_CHECK =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "All requested Equipments where ISOEquipmentCode is reefer code must have '%s' flag"
              .formatted(IS_NON_OPERATING_REEFER),
          ALL_REQ_EQUIP,
          JsonAttribute.ifMatchedThen(
              IS_ISO_EQUIPMENT_CONTAINER_REEFER,
              JsonAttribute.path(IS_NON_OPERATING_REEFER, JsonAttribute.matchedMustBePresent())));

  private static final JsonContentCheck UNIVERSAL_SERVICE_REFERENCE =
      JsonAttribute.customValidator(
          "Conditional Universal Service Reference",
          body -> {
            var routingReference = body.path(ROUTING_REFERENCE).asText("");
            var universalExportVoyageReference = body.path(UNIVERSAL_EXPORT_VOYAGE_REFERENCE);
            var universalImportVoyageReference = body.path(UNIVERSAL_IMPORT_VOYAGE_REFERENCE);
            var universalServiceReference = body.path(UNIVERSAL_SERVICE_REFERENCE1);
            if (!routingReference.isBlank()) {
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
            if ((JsonAttribute.isJsonNodePresent(universalExportVoyageReference)
                    || JsonAttribute.isJsonNodePresent(universalImportVoyageReference))
                && JsonAttribute.isJsonNodeAbsent(universalServiceReference)) {
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

  private static final JsonContentCheck REFERENCE_TYPE_VALIDATION =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate reference type field",
          mav -> {
            mav.submitAllMatching(S_S_S.formatted(REQUESTED_EQUIPMENTS, REFERENCES, TYPE));
            mav.submitAllMatching(
                "%s.*.%s.*.%s.*.%s".formatted(REQUESTED_EQUIPMENTS, COMMODITIES, REFERENCES, TYPE));
            mav.submitAllMatching(S_S.formatted(REFERENCES, TYPE));
          },
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(BookingDataSets.REFERENCE_TYPES));

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

  static final JsonContentCheck IS_EXPORT_DECLARATION_REFERENCE_PRESENCE =
      JsonAttribute.ifThenElse(
          "Check Export declaration reference presence",
          JsonAttribute.isTrue(
              JsonPointer.compile("/%s".formatted(IS_EXPORT_DECLARATION_REQUIRED))),
          JsonAttribute.mustBePresent(
              JsonPointer.compile("/%s".formatted(EXPORT_DECLARATION_REFERENCE))),
          JsonAttribute.mustBeAbsent(
              JsonPointer.compile("/%s".formatted(EXPORT_DECLARATION_REFERENCE))));

  static final JsonContentCheck DOCUMENT_PARTY_FUNCTIONS_MUST_BE_UNIQUE =
      JsonAttribute.customValidator(
          "Each document party can be used at most once, except 'NI' which can be repeated",
          body -> {
            var partyFunctionCounts = new HashMap<String, Integer>();

            StreamSupport.stream(body.path(DOCUMENT_PARTIES).path(OTHER).spliterator(), false)
                .map(party -> party.path(PARTY_FUNCTION).asText(""))
                .filter(partyFunction -> !partyFunction.isBlank())
                .forEach(
                    partyFunction -> partyFunctionCounts.merge(partyFunction, 1, Integer::sum));

            return ConformanceCheckResult.simple(
                partyFunctionCounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > 1 && !"NI".equals(entry.getKey()))
                    .map(
                        entry ->
                            "Party function '%s' cannot be repeated. Found %d occurrences."
                                .formatted(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
          });

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
          "Validate allowed shipment cutoff codes",
          mav ->
              mav.submitAllMatching(S_S.formatted(SHIPMENT_CUT_OFF_TIMES, CUT_OFF_DATE_TIME_CODE)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
              BookingDataSets.CUTOFF_DATE_TIME_CODES));

  private static final JsonContentCheck VALIDATE_SHIPMENT_CUTOFF_TIME_CODE =
      JsonAttribute.customValidator(
          "Validate shipment cutOff Date time code",
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
            if (receiptTypeAtOrigin.equals("CFS") && !cutOffDateTimeCodes.contains("LCL")) {
              issues.add(
                  "'%s' 'LCL' must be present when '%s' is 'CFS'"
                      .formatted(CUT_OFF_DATE_TIME_CODE, RECEIPT_TYPE_AT_ORIGIN));
            }
            return ConformanceCheckResult.simple(issues);
          });

  private static final Consumer<MultiAttributeValidator> ALL_AMF =
      mav -> mav.submitAllMatching(ADVANCE_MANIFEST_FILINGS);

  private static final JsonContentCheck ADVANCED_MANIFEST_FILING_CODES_UNIQUE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "The combination of '%s' and '%s' in '%s' must be unique"
              .formatted(COUNTRY_CODE, MANIFEST_TYPE_CODE, ADVANCE_MANIFEST_FILINGS),
          ALL_AMF,
          JsonAttribute.unique(COUNTRY_CODE, MANIFEST_TYPE_CODE));
  private static final Consumer<MultiAttributeValidator> ALL_SHIPMENT_CUTOFF_TIMES =
      mav -> mav.submitAllMatching(SHIPMENT_CUT_OFF_TIMES);

  private static final JsonContentCheck SHIPMENT_CUTOFF_TIMES_UNIQUE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "in '%s' cutOff date time code must be unique".formatted(SHIPMENT_CUT_OFF_TIMES),
          ALL_SHIPMENT_CUTOFF_TIMES,
          JsonAttribute.unique(CUT_OFF_DATE_TIME_CODE));

  private static final JsonContentCheck VALIDATE_SHIPMENT_LOCATIONS =
      JsonAttribute.customValidator(
          "Validate %s".formatted(SHIPMENT_LOCATIONS),
          body -> {
            var issues = new LinkedHashSet<String>();
            var routingReference = body.path(ROUTING_REFERENCE).asText("");
            var receiptTypeAtOrigin = body.path(RECEIPT_TYPE_AT_ORIGIN).asText("");
            var deliveryTypeAtDestination = body.path(DELIVERY_TYPE_AT_DESTINATION).asText("");
            var polNode = getShipmentLocationTypeCode(body, "POL");
            var preNode = getShipmentLocationTypeCode(body, "PRE");
            var pdeNode = getShipmentLocationTypeCode(body, "PDE");
            var podNode = getShipmentLocationTypeCode(body, "POD");

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
              if (requestedEquipments.isArray()) {
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
          "Feedbacks must be present for the selected Booking Status",
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
          "check confirmed booking fields availability",
          body -> {
            var issues = new LinkedHashSet<String>();
            var bookingStatus = body.path(BOOKING_STATUS).asText("");
            if (CONFIRMED_BOOKING_STATES.contains(BookingState.fromString(bookingStatus))) {
              if (body.path(CONFIRMED_EQUIPMENTS).isEmpty()) {
                issues.add(S_FOR_CONFIRMED_BOOKING_IS_NOT_PRESENT.formatted(CONFIRMED_EQUIPMENTS));
              }
              if (body.path(TRANSPORT_PLAN).isEmpty()) {
                issues.add(S_FOR_CONFIRMED_BOOKING_IS_NOT_PRESENT.formatted(TRANSPORT_PLAN));
              }
              if (body.path(SHIPMENT_CUT_OFF_TIMES).isEmpty()) {
                issues.add(
                    S_FOR_CONFIRMED_BOOKING_IS_NOT_PRESENT.formatted(SHIPMENT_CUT_OFF_TIMES));
              }
            }
            return ConformanceCheckResult.simple(issues);
          });

  static final JsonContentCheck CHECK_CARGO_GROSS_WEIGHT_CONDITIONS =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Check Cargo Gross Weight conditions",
          mav -> mav.submitAllMatching("%s.*".formatted(REQUESTED_EQUIPMENTS)),
          (nodeToValidate, contextPath) -> {
            var issues = new LinkedHashSet<String>();
            var cargoGrossWeight = nodeToValidate.path(CARGO_GROSS_WEIGHT);
            if (cargoGrossWeight.isMissingNode() || cargoGrossWeight.isNull()) {
              var commodities = nodeToValidate.path(COMMODITIES);
              if (!(commodities.isMissingNode() || commodities.isNull()) && commodities.isArray()) {
                AtomicInteger commodityCounter = new AtomicInteger(0);
                StreamSupport.stream(commodities.spliterator(), false)
                    .forEach(
                        commodity -> {
                          var commodityGrossWeight = commodity.path(CARGO_GROSS_WEIGHT);
                          int currentCommodityCount = commodityCounter.getAndIncrement();
                          if (commodityGrossWeight.isMissingNode()
                              || commodityGrossWeight.isNull()) {
                            issues.add(
                                "The '%s' must have '%s' at '%s' position %s"
                                    .formatted(
                                        contextPath,
                                        CARGO_GROSS_WEIGHT,
                                        COMMODITIES,
                                        currentCommodityCount));
                          }
                        });
              }
            }
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

  static final JsonRebaseableContentCheck VALID_FEEDBACK_SEVERITY =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate that '%s.*.%s' is valid".formatted(FEEDBACKS, SEVERITY),
          mav -> mav.submitAllMatching(S_S.formatted(FEEDBACKS, SEVERITY)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(FEEDBACKS_SEVERITY));

  static final JsonRebaseableContentCheck VALID_FEEDBACK_CODE =
      JsonAttribute.allIndividualMatchesMustBeValid(
          "Validate that '%s.*.%s' is valid".formatted(FEEDBACKS, CODE),
          mav -> mav.submitAllMatching(S_S.formatted(FEEDBACKS, CODE)),
          JsonAttribute.matchedMustBeDatasetKeywordIfPresent(FEEDBACKS_CODE));

  static final List<JsonContentCheck> STATIC_BOOKING_CHECKS =
      Arrays.asList(
          JsonAttribute.mustBeDatasetKeywordIfPresent(
              JsonPointer.compile("/%s".formatted(CARGO_MOVEMENT_TYPE_AT_ORIGIN)),
              BookingDataSets.CARGO_MOVEMENT_TYPE),
          JsonAttribute.mustBeDatasetKeywordIfPresent(
              JsonPointer.compile("/%s".formatted(CARGO_MOVEMENT_TYPE_AT_DESTINATION)),
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
                if (!dg.isArray() || dg.isEmpty()) {
                  return ConformanceCheckResult.simple(Set.of());
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
              "DangerousGoods implies '%s'".formatted(NUMBER_OF_PACKAGES),
              mav ->
                  mav.submitAllMatching(
                      S_S_S.formatted(REQUESTED_EQUIPMENTS, COMMODITIES, OUTER_PACKAGING)),
              (nodeToValidate, contextPath) -> {
                var dg = nodeToValidate.path(DANGEROUS_GOODS);
                if (!dg.isArray() || dg.isEmpty()) {
                  return ConformanceCheckResult.simple(Set.of());
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
              "The '%s' values must be from dataset".formatted(SEGREGATION_GROUPS),
              allDg(dg -> dg.path(SEGREGATION_GROUPS).all().submitPath()),
              JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                  BookingDataSets.DG_SEGREGATION_GROUPS)),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The '%s' values must be from dataset".formatted(INHALATION_ZONE),
              allDg(dg -> dg.path(INHALATION_ZONE).all().submitPath()),
              JsonAttribute.matchedMustBeDatasetKeywordIfPresent(
                  BookingDataSets.INHALATION_ZONE_CODE)),
          JsonAttribute.allOrNoneArePresent(
              JsonPointer.compile("/%s".formatted(DECLARED_VALUE)),
              JsonPointer.compile("/%s".formatted(DECLARED_VALUE_CURRENCY))),
          JsonAttribute.allIndividualMatchesMustBeValid(
              "The '%s.*.%s' must not exceed more than 2 decimal points"
                  .formatted(CHARGES, CURRENCY_AMOUNT),
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
          SHIPMENT_CUTOFF_TIMES_UNIQUE,
          CHECK_CONFIRMED_BOOKING_FIELDS,
          VALIDATE_SHIPMENT_LOCATIONS,
          FEEDBACKS_PRESENCE,
          VALID_FEEDBACK_SEVERITY,
          VALID_FEEDBACK_CODE);

  public static ActionCheck responseContentChecks(
      UUID matched,
      String standardVersion,
      Supplier<BookingDynamicScenarioParameters> dspSupplier,
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
      Supplier<BookingDynamicScenarioParameters> dspSupplier,
      BookingState bookingStatus,
      BookingState expectedAmendedBookingStatus,
      BookingCancellationState expectedCancelledBookingStatus,
      Boolean requestAmendedContent) {

    var checks = new ArrayList<JsonContentCheck>();

    checks.add(cbrrOrCbr(dspSupplier));

    checks.add(
        JsonAttribute.mustEqual(
            JsonPointer.compile("/%s".formatted(BOOKING_STATUS)), bookingStatus.name()));

    checks.add(
        JsonAttribute.customValidator(
            "Validate '%s'".formatted(ATTR_AMENDED_BOOKING_STATUS),
            body -> {
              String amendedBookingStatus = body.path(ATTR_AMENDED_BOOKING_STATUS).asText("");
              if (!amendedBookingStatus.isEmpty()
                  && expectedAmendedBookingStatus != null
                  && !expectedAmendedBookingStatus.name().equals(amendedBookingStatus)) {
                return ConformanceCheckResult.simple(
                    Set.of(
                        "The expected '%s' %s is not equal to response '%s' %s"
                            .formatted(
                                ATTR_AMENDED_BOOKING_STATUS,
                                expectedAmendedBookingStatus.name(),
                                ATTR_AMENDED_BOOKING_STATUS,
                                amendedBookingStatus)));
              }
              return ConformanceCheckResult.simple(Set.of());
            }));

    checks.add(
        JsonAttribute.customValidator(
            "Validate '%s'".formatted(ATTR_BOOKING_CANCELLATION_STATUS),
            body -> {
              String bookingCancellationStatus =
                  body.path(ATTR_BOOKING_CANCELLATION_STATUS).asText("");
              if (!bookingCancellationStatus.isEmpty()
                  && expectedCancelledBookingStatus != null
                  && !expectedCancelledBookingStatus.name().equals(bookingCancellationStatus)) {
                return ConformanceCheckResult.simple(
                    Set.of(
                        "The expected '%s' %s is not equal to response '%s' %s"
                            .formatted(
                                ATTR_BOOKING_CANCELLATION_STATUS,
                                expectedCancelledBookingStatus.name(),
                                ATTR_BOOKING_CANCELLATION_STATUS,
                                bookingCancellationStatus)));
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
        "Validate Carrier Booking Request Reference and Carrier Booking Reference",
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

  private boolean isReeferContainerSizeTypeCode(String isoEquipmentCode) {
    var codeChar = isoEquipmentCode.length() > 2 ? isoEquipmentCode.charAt(2) : '?';
    return codeChar == 'R' || codeChar == 'H';
  }
}
