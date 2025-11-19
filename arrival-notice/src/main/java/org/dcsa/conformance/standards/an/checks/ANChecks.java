package org.dcsa.conformance.standards.an.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.check.JsonRebasableContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.an.party.ANRole;
import org.dcsa.conformance.standards.an.party.DynamicScenarioParameters;

public class ANChecks {

  private static final String S_MUST_BE_PRESENT_AND_NON_EMPTY = "%s must be present and non-empty";
  private static final String ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_OBJECT =
      "At least one Arrival Notice must demonstrate the correct use of a '%s' object";
  private static final String ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_ATTRIBUTE =
      "At least one Arrival Notice must demonstrate the correct use of a '%s' attribute";

  private static final String CARRIER_CODE_LIST_PROVIDER = "carrierCodeListProvider";
  private static final String CARRIER_CODE = "carrierCode";
  private static final String TRANSPORT_DOCUMENT_REFERENCE = "transportDocumentReference";
  private static final String CARRIER_CONTACT_INFORMATION = "carrierContactInformation";
  private static final String DELIVERY_TYPE_AT_DESTINATION = "deliveryTypeAtDestination";
  private static final String DOCUMENT_PARTIES = "documentParties";
  private static final String TRANSPORT = "transport";
  private static final String UTILIZED_TRANSPORT_EQUIPMENTS = "utilizedTransportEquipments";
  private static final String CONSIGNMENT_ITEMS = "consignmentItems";
  private static final String FREE_TIMES = "freeTimes";
  private static final String CHARGES = "charges";

  public static ActionCheck getANPostPayloadChecks(
      UUID matchedExchangeUuid, String expectedApiVersion, String scenarioType) {

    var checks = new ArrayList<JsonContentCheck>();

    checks.add(nonEmptyArrivalNotices());
    checks.addAll(guardEachWithBodyPresent(getPayloadChecks(scenarioType), "arrivalNotices"));

    return JsonAttribute.contentChecks(
        "",
        "The Publisher has correctly demonstrated the use of functionally required attributes in the payload",
        ANRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        checks);
  }

  private static ArrayList<JsonContentCheck> getPayloadChecks(String scenarioType) {
    var checks = new ArrayList<JsonContentCheck>();

    checks.add(atLeastOneTransportDocumentReferenceCorrect());
    checks.add(atLeastOneCarrierCodeCorrect());
    checks.add(atLeastOneCarrierCodeListProviderCorrect());
    checks.add(atLeastOneCarrierContactInformationCorrect());
    checks.add(atLeastOneDeliveryTypeAtDestination());
    checks.add(atLeastOneDocumentPartiesCorrect());
    checks.add(atLeastOneTransportCorrect());
    checks.add(atLeastOneUtilizedTransportEquipmentsCorrect());
    checks.add(atLeastOneConsignmentItemsCorrect());

    if ("FREE_TIME".equals(scenarioType)) {
      checks.add(atLeastOneANFreeTimeCorrect());
    }
    if ("FREIGHTED".equals(scenarioType)) {
      checks.add(atLeastOneANChargesCorrect());
    }
    return checks;
  }

  public static JsonContentCheck nonEmptyArrivalNotices() {
    return JsonAttribute.customValidator(
        "At least one Arrival Notice must be included in a message sent during conformance testing",
        (body, ctx) -> {
          var ans = body.path("arrivalNotices");
          if (!ans.isArray() || ans.isEmpty()) {
            return ConformanceCheckResult.simple(
                Set.of("arrivalNotices must be a non-empty array"));
          }
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck atLeastOneCarrierCodeCorrect() {
    return JsonAttribute.customValidator(
        ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_ATTRIBUTE.formatted(CARRIER_CODE),
        (body, ctx) -> {
          var ans = body.path("arrivalNotices");
          Set<String> errors = new LinkedHashSet<>();
          int i = 0;

          for (var an : ans) {
            List<String> local = validateCarrierCode(an);
            final int idx = i;
            if (local.isEmpty()) return ConformanceCheckResult.simple(Set.of());
            local.forEach(e -> errors.add("arrivalNotices[" + idx + "]." + e));
            i++;
          }

          return ConformanceCheckResult.simple(errors);
        });
  }

  private static List<String> validateCarrierCode(JsonNode an) {
    List<String> issues = new ArrayList<>();
    var v = an.path(CARRIER_CODE);
    if (JsonUtil.isMissingOrEmpty(v)) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(CARRIER_CODE));
    }
    return issues;
  }

  public static JsonContentCheck atLeastOneTransportDocumentReferenceCorrect() {
    return JsonAttribute.customValidator(
        ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_ATTRIBUTE.formatted(
            TRANSPORT_DOCUMENT_REFERENCE),
        (body, ctx) -> {
          var ans = body.path("arrivalNotices");
          Set<String> errors = new LinkedHashSet<>();
          int i = 0;

          for (var an : ans) {
            List<String> local = validateTDR(an);
            final int idx = i;
            if (local.isEmpty()) return ConformanceCheckResult.simple(Set.of());
            local.forEach(e -> errors.add("arrivalNotices[" + idx + "]." + e));
            i++;
          }
          return ConformanceCheckResult.simple(errors);
        });
  }

  private static List<String> validateTDR(JsonNode an) {
    List<String> issues = new ArrayList<>();
    var v = an.path(TRANSPORT_DOCUMENT_REFERENCE);
    if (JsonUtil.isMissingOrEmpty(v)) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(TRANSPORT_DOCUMENT_REFERENCE));
    }
    return issues;
  }

  public static JsonContentCheck atLeastOneCarrierCodeListProviderCorrect() {
    return JsonAttribute.customValidator(
        ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_ATTRIBUTE.formatted(
            CARRIER_CODE_LIST_PROVIDER),
        (body, ctx) -> {
          var ans = body.path("arrivalNotices");
          Set<String> errors = new LinkedHashSet<>();
          int i = 0;

          for (var an : ans) {
            List<String> local = validateCarrierCodeListProvider(an);
            final int idx = i;
            if (local.isEmpty()) return ConformanceCheckResult.simple(Set.of());
            local.forEach(e -> errors.add("arrivalNotices[" + idx + "]." + e));
            i++;
          }
          return ConformanceCheckResult.simple(errors);
        });
  }

  private static List<String> validateCarrierCodeListProvider(JsonNode an) {
    List<String> issues = new ArrayList<>();
    var v = an.path(CARRIER_CODE_LIST_PROVIDER);

    if (JsonUtil.isMissingOrEmpty(v)) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(CARRIER_CODE_LIST_PROVIDER));
      return issues;
    }

    String val = v.asText();
    if (!ANDatasets.CARRIER_CODE_LIST_PROVIDER.contains(val)) {
      issues.add("Invalid carrierCodeListProvider, must functionally be either 'NMFTA' or 'SMDG'");
    }

    return issues;
  }

  public static JsonContentCheck atLeastOneCarrierContactInformationCorrect() {
    return JsonAttribute.customValidator(
        ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_OBJECT.formatted(
            CARRIER_CONTACT_INFORMATION),
        (body, ctx) -> {
          var ans = body.path("arrivalNotices");
          Set<String> errors = new LinkedHashSet<>();
          int i = 0;

          for (var an : ans) {
            List<String> local = validateCarrierContactInformation(an);
            final int idx = i;
            if (local.isEmpty()) return ConformanceCheckResult.simple(Set.of());
            local.forEach(e -> errors.add("arrivalNotices[" + idx + "]." + e));
            i++;
          }
          return ConformanceCheckResult.simple(errors);
        });
  }

  private static List<String> validateCarrierContactInformation(JsonNode an) {
    var ccis = an.path(CARRIER_CONTACT_INFORMATION);
    List<String> errors = new ArrayList<>();

    if (!ccis.isArray() || ccis.isEmpty()) {
      errors.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(CARRIER_CONTACT_INFORMATION));
      return errors;
    }

    boolean anyValid = false;
    int idx = 0;

    for (JsonNode cci : ccis) {
      List<String> local = new ArrayList<>();

      if (cci.path("name").asText().isBlank()) {
        local.add(
            CARRIER_CONTACT_INFORMATION + "[" + idx + "].name must functionally be non-empty");
      }

      boolean hasEmailOrPhone =
          !cci.path("email").asText().isBlank() || !cci.path("phone").asText().isBlank();

      if (!hasEmailOrPhone) {
        local.add(
            CARRIER_CONTACT_INFORMATION
                + "["
                + idx
                + "] must functionally contain either email or phone");
      }

      if (local.isEmpty()) {
        anyValid = true;
        break;
      }

      errors.addAll(local);
      idx++;
    }

    if (anyValid) {
      return List.of();
    }

    return errors;
  }

  public static JsonContentCheck atLeastOneDeliveryTypeAtDestination() {
    return JsonAttribute.customValidator(
        ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_OBJECT.formatted(
            DELIVERY_TYPE_AT_DESTINATION),
        (body, ctx) -> {
          var ans = body.path("arrivalNotices");
          Set<String> errors = new LinkedHashSet<>();
          int i = 0;

          for (var an : ans) {
            List<String> local = validateDeliveryTypeAtDestination(an);
            final int idx = i;
            if (local.isEmpty()) return ConformanceCheckResult.simple(Set.of());
            local.forEach(e -> errors.add("arrivalNotices[" + idx + "]." + e));
            i++;
          }
          return ConformanceCheckResult.simple(errors);
        });
  }

  private static List<String> validateDeliveryTypeAtDestination(JsonNode an) {
    List<String> issues = new ArrayList<>();
    var v = an.path(DELIVERY_TYPE_AT_DESTINATION);

    if (JsonUtil.isMissingOrEmpty(v)) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(DELIVERY_TYPE_AT_DESTINATION));
      return issues;
    }

    String val = v.asText();
    if (!ANDatasets.DELIVERY_TYPE_AT_DESTINATION.contains(val)) {
      issues.add(
          "Invalid " + DELIVERY_TYPE_AT_DESTINATION + ", must be one of CY', 'SD', or 'CFS'");
    }

    return issues;
  }

  public static JsonContentCheck atLeastOneDocumentPartiesCorrect() {
    return JsonAttribute.customValidator(
        ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_OBJECT.formatted(DOCUMENT_PARTIES),
        (body, ctx) -> {
          var ans = body.path("arrivalNotices");
          Set<String> errors = new LinkedHashSet<>();
          int i = 0;

          for (var an : ans) {
            List<String> local = validateDocumentParties(an);
            final int idx = i;
            if (local.isEmpty()) return ConformanceCheckResult.simple(Set.of());
            local.forEach(e -> errors.add("arrivalNotices[" + idx + "]." + e));
            i++;
          }
          return ConformanceCheckResult.simple(errors);
        });
  }

  private static List<String> validateDocumentParties(JsonNode an) {
    List<String> issues = new ArrayList<>();

    var documentParties = an.path(DOCUMENT_PARTIES);
    if (!documentParties.isArray() || documentParties.isEmpty()) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(DOCUMENT_PARTIES));
      return issues;
    }

    int i = 0;
    for (JsonNode documentParty : documentParties) {
      String base = DOCUMENT_PARTIES + "[" + i + "]";

      var partyFunction = documentParty.path("partyFunction");

      if (partyFunction.isMissingNode() || partyFunction.asText().isBlank()) {
        issues.add(base + ".partyFunction must functionally be present and non-empty");
      } else {
        String pf = partyFunction.asText();
        if (!ANDatasets.PARTY_FUNCTION.contains(pf)) {
          issues.add(
              base
                  + ".partyFunction must be one of: OS, CN, END, RW, CG, N1, N2, NI, SCO, DDR, DDS, COW, COX, CS, MF, WH");
        }
      }

      var partyName = documentParty.path("partyName");
      if (partyName.isMissingNode() || partyName.asText().isBlank()) {
        issues.add(base + ".partyName must functionally be present and non-empty");
      }

      var contactDetails = documentParty.path("partyContactDetails");
      issues.addAll(validatePartyContactDetails(contactDetails, base));

      var address = documentParty.path("address");
      if (JsonUtil.isMissingOrEmpty(address)) {
        issues.add(base + ".address must functionally be present and non-empty");
      } else {
        boolean hasNonEmptyField =
            ADDRESS_FIELDS.stream()
                .anyMatch(f -> address.hasNonNull(f) && !address.get(f).asText().isBlank());

        if (!hasNonEmptyField) {
          issues.add(
              base
                  + ".address must contain at least one non-empty value among: "
                  + String.join(", ", ADDRESS_FIELDS));
        }
      }

      i++;
    }

    return issues;
  }

  private static List<String> validatePartyContactDetails(JsonNode contactArr, String base) {
    List<String> errors = new ArrayList<>();

    if (!contactArr.isArray() || contactArr.isEmpty()) {
      errors.add(base + ".partyContactDetails must functionally be present and a non-empty array");
      return errors;
    }

    boolean anyValid = false;
    int idx = 0;

    for (JsonNode contact : contactArr) {
      boolean nameOk = contact.hasNonNull("name") && !contact.get("name").asText().isBlank();
      boolean emailOk = contact.hasNonNull("email") && !contact.get("email").asText().isBlank();
      boolean phoneOk = contact.hasNonNull("phone") && !contact.get("phone").asText().isBlank();

      if (nameOk && (emailOk || phoneOk)) {
        anyValid = true;
        break;
      }

      if (!nameOk) {
        errors.add(base + ".partyContactDetails[" + idx + "].name must be non-empty");
      }
      if (!emailOk && !phoneOk) {
        errors.add(base + ".partyContactDetails[" + idx + "] must contain email or phone");
      }

      idx++;
    }

    if (anyValid) {
      return List.of();
    }

    return errors;
  }

  public static JsonContentCheck atLeastOneTransportCorrect() {
    return JsonAttribute.customValidator(
        ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_OBJECT.formatted(TRANSPORT),
        (body, ctx) -> {
          var ans = body.path("arrivalNotices");
          Set<String> errors = new LinkedHashSet<>();
          int i = 0;

          for (var an : ans) {
            List<String> local = validateTransport(an);
            final int idx = i;
            if (local.isEmpty()) return ConformanceCheckResult.simple(Set.of());
            local.forEach(e -> errors.add("arrivalNotices[" + idx + "]." + e));
            i++;
          }

          return ConformanceCheckResult.simple(errors);
        });
  }

  private static List<String> validateTransport(JsonNode an) {
    List<String> issues = new ArrayList<>();
    var t = an.path(TRANSPORT);

    if (JsonUtil.isMissingOrEmpty(t)) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(TRANSPORT));
      return issues;
    }

    boolean hasPODvalue =
        t.hasNonNull("portOfDischargeArrivalDate")
            && t.path("portOfDischargeArrivalDate").hasNonNull("value");

    boolean hasPODelValue =
        t.hasNonNull("placeOfDeliveryArrivalDate")
            && t.path("placeOfDeliveryArrivalDate").hasNonNull("value");

    if (!hasPODvalue && !hasPODelValue) {
      issues.add(
          "transport must functionally contain either portOfDischargeArrivalDate.value or placeOfDeliveryArrivalDate.value");
    }

    var pod = t.path("portOfDischarge");
    if (JsonUtil.isMissingOrEmpty(pod)) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted("transport.portOfDischarge"));
      return issues;
    }

    boolean hasUNLoc =
        pod.hasNonNull("UNLocationCode") && !pod.path("UNLocationCode").asText().isBlank();

    boolean hasAddress = false;
    if (pod.hasNonNull("address") && pod.path("address").isObject()) {
      JsonNode addr = pod.path("address");
      hasAddress =
          ADDRESS_FIELDS.stream()
              .anyMatch(f -> addr.hasNonNull(f) && !addr.get(f).asText().isBlank());

      if (addr.isObject() && !hasAddress) {
        issues.add(
            "transport.portOfDischarge.address must contain at least one non-empty field: "
                + String.join(", ", ADDRESS_FIELDS));
      }
    }

    boolean hasFacility = false;
    if (pod.hasNonNull("facility") && pod.path("facility").isObject()) {
      var facility = pod.path("facility");

      boolean hasCode =
          facility.hasNonNull("facilityCode") && !facility.get("facilityCode").asText().isBlank();

      boolean hasProvider =
          facility.hasNonNull("facilityCodeListProvider")
              && !facility.get("facilityCodeListProvider").asText().isBlank();

      if (hasProvider) {
        String provider = facility.get("facilityCodeListProvider").asText();
        if (!ANDatasets.FACILITY_CODE_LIST_PROVIDER.contains(provider)) {
          issues.add(
              "transport.portOfDischarge.facility.facilityCodeListProvider must be one of: SMDG or BIC");
        }
      }

      hasFacility = hasCode && hasProvider;

      if (facility.isObject() && !hasFacility) {
        if (!hasCode) {
          issues.add(
              S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(
                  "transport.portOfDischarge.facility.facilityCode"));
        }
        if (!hasProvider) {
          issues.add(
              S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(
                  "transport.portOfDischarge.facility.facilityCodeListProvider"));
        }
      }
    }

    if (!hasUNLoc && !hasAddress && !hasFacility) {
      issues.add(
          "transport.portOfDischarge must contain at least one of: UNLocationCode (non empty), a valid address, or a facility with both facilityCode and facilityCodeListProvider");
    }

    var legs = t.path("legs");
    if (!legs.isArray() || legs.isEmpty()) {
      issues.add("transport.legs must be present and a non-empty array");
    } else {
      int i = 0;
      for (var leg : legs) {
        var vv = leg.path("vesselVoyage");
        if (JsonUtil.isMissingOrEmpty(vv)) {
          issues.add(
              S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted("transport.legs[" + i + "].vesselVoyage"));
        } else {
          if (vv.path("vesselName").asText().isBlank()) {
            issues.add(
                S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(
                    "transport.legs[" + i + "].vesselVoyage.vesselName"));
          }
          if (vv.path("carrierImportVoyageNumber").asText().isBlank()) {
            issues.add(
                S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(
                    "transport.legs[" + i + "].vesselVoyage.carrierImportVoyageNumber"));
          }
        }
        i++;
      }
    }

    return issues;
  }

  public static JsonContentCheck atLeastOneUtilizedTransportEquipmentsCorrect() {
    return JsonAttribute.customValidator(
        ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_OBJECT.formatted(
            UTILIZED_TRANSPORT_EQUIPMENTS),
        (body, ctx) -> {
          var ans = body.path("arrivalNotices");
          Set<String> errors = new LinkedHashSet<>();
          int i = 0;

          for (var an : ans) {
            List<String> local = validateUTE(an);
            final int idx = i;
            if (local.isEmpty()) return ConformanceCheckResult.simple(Set.of());
            local.forEach(e -> errors.add("arrivalNotices[" + idx + "]." + e));
            i++;
          }

          return ConformanceCheckResult.simple(errors);
        });
  }

  private static List<String> validateUTE(JsonNode an) {
    List<String> issues = new ArrayList<>();

    var arr = an.path(UTILIZED_TRANSPORT_EQUIPMENTS);
    if (!arr.isArray() || arr.isEmpty()) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(UTILIZED_TRANSPORT_EQUIPMENTS));
      return issues;
    }

    boolean foundValidEquipmentRef = false;
    boolean foundValidISOCode = false;
    boolean foundValidSeal = false;

    int i = 0;
    for (var ute : arr) {
      String base = UTILIZED_TRANSPORT_EQUIPMENTS + "[" + i + "]";

      var eq = ute.path("equipment");
      if (eq.isObject()) {

        String eqRef = eq.path("equipmentReference").asText("");
        if (!eqRef.isBlank()) {
          foundValidEquipmentRef = true;
        }

        String iso = eq.path("ISOEquipmentCode").asText("");
        if (!iso.isBlank()) {
          foundValidISOCode = true;
        }

      } else {
        issues.add(base + ".equipment must be present and non-empty");
      }

      var seals = ute.path("seals");
      if (seals.isArray() && !seals.isEmpty()) {

        for (var seal : seals) {
          String val = seal.path("number").asText("");
          if (!val.isBlank()) {
            foundValidSeal = true;
            break;
          }
        }

      } else {
        issues.add(base + ".seals must be a non-empty array");
      }

      i++;
    }

    if (!foundValidEquipmentRef) {
      issues.add(
          UTILIZED_TRANSPORT_EQUIPMENTS
              + " must contain at least one item with a valid equipmentReference");
    }
    if (!foundValidISOCode) {
      issues.add(
          UTILIZED_TRANSPORT_EQUIPMENTS
              + " must contain at least one item with a valid ISOEquipmentCode");
    }
    if (!foundValidSeal) {
      issues.add(
          UTILIZED_TRANSPORT_EQUIPMENTS
              + " must contain at least one seal entry with a non-empty number");
    }

    return issues;
  }


  public static JsonContentCheck atLeastOneConsignmentItemsCorrect() {
    return JsonAttribute.customValidator(
        ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_OBJECT.formatted(CONSIGNMENT_ITEMS),
        (body, ctx) -> {
          var ans = body.path("arrivalNotices");
          Set<String> errors = new LinkedHashSet<>();
          int i = 0;

          for (var an : ans) {
            List<String> local = validateConsignmentItems(an);
            final int idx = i;
            if (local.isEmpty()) return ConformanceCheckResult.simple(Set.of());
            local.forEach(e -> errors.add("arrivalNotices[" + idx + "]." + e));
            i++;
          }

          return ConformanceCheckResult.simple(errors);
        });
  }

  private static List<String> validateConsignmentItems(JsonNode an) {
    List<String> issues = new ArrayList<>();

    var arr = an.path(CONSIGNMENT_ITEMS);
    if (!arr.isArray() || arr.isEmpty()) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(CONSIGNMENT_ITEMS));
      return issues;
    }

    int i = 0;
    for (var item : arr) {
      String base = CONSIGNMENT_ITEMS + "[" + i + "]";

      var descriptionOfGoods = item.path("descriptionOfGoods");
      if (!descriptionOfGoods.isArray() || descriptionOfGoods.isEmpty()) {
        issues.add(base + ".descriptionOfGoods must be a non-empty array");
      }

      var cargo = item.path("cargoItems");
      if (!cargo.isArray() || cargo.isEmpty()) {
        issues.add(base + ".cargoItems must be a non-empty array");
      } else {

        boolean foundValidEquipmentRef = false;
        boolean foundValidWeightValue = false;
        boolean foundValidWeightUnit = false;
        boolean foundValidOuterPkg = false;
        boolean foundValidNumPackages = false;

        for (var cItem : cargo) {

          String eqRef = cItem.path("equipmentReference").asText("");
          if (!eqRef.isBlank()) {
            foundValidEquipmentRef = true;
          }

          var gw = cItem.path("cargoGrossWeight");
          if (gw.isObject()) {
            var val = gw.path("value");
            if (val.isNumber() && val.asDouble() > 0) {
              foundValidWeightValue = true;
            }

            var unit = gw.path("unit").asText("");
            if (!unit.isBlank() && ANDatasets.CARGO_GROSS_WEIGHT_UNIT.contains(unit)) {
              foundValidWeightUnit = true;
            }
          }
          var op = cItem.path("outerPackaging");
          if (op.isObject()) {

            boolean hasField =
                (op.hasNonNull("packageCode") && !op.path("packageCode").asText().isBlank())
                    || (op.hasNonNull("IMOPackagingCode")
                        && !op.path("IMOPackagingCode").asText().isBlank())
                    || (op.hasNonNull("description") && !op.path("description").asText().isBlank());

            if (hasField) foundValidOuterPkg = true;

            var numPkgsNode = op.path("numberOfPackages");
            if (numPkgsNode.isNumber() && numPkgsNode.asInt() > 0) {
              foundValidNumPackages = true;
            }
          }
        }

        if (!foundValidEquipmentRef) {
          issues.add(
              base + ".cargoItems must contain at least one item with a valid equipmentReference");
        }
        if (!foundValidWeightValue) {
          issues.add(
              base
                  + ".cargoItems must contain at least one item with a positive cargoGrossWeight.value");
        }
        if (!foundValidWeightUnit) {
          issues.add(
              base
                  + ".cargoItems must contain at least one item with a valid cargoGrossWeight.unit (KGM/LBR/GRM/ONZ)");
        }
        if (!foundValidOuterPkg) {
          issues.add(
              base
                  + ".cargoItems must contain at least one item with a valid outerPackaging field");
        }
        if (!foundValidNumPackages) {
          issues.add(
              base
                  + ".cargoItems must contain at least one item with a positive outerPackaging.numberOfPackages");
        }
      }

      i++;
    }

    return issues;
  }

  public static JsonContentCheck atLeastOneANFreeTimeCorrect() {
    return JsonAttribute.customValidator(
        ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_OBJECT.formatted(FREE_TIMES),
        (body, contextPath) -> {
          Set<String> allIssues = new LinkedHashSet<>();
          var ans = body.path("arrivalNotices");

          if (!ans.isArray() || ans.isEmpty()) {
            allIssues.add("No Arrival Notices found in the payload");
            return ConformanceCheckResult.simple(allIssues);
          }

          int anIndex = 0;

          for (var an : ans) {
            List<String> errors = validateFreeTimes(an);

            if (errors.isEmpty()) {
              return ConformanceCheckResult.simple(Set.of());
            }

            for (String e : errors) {
              allIssues.add("arrivalNotices[" + anIndex + "]." + e);
            }

            anIndex++;
          }

          return ConformanceCheckResult.simple(allIssues);
        });
  }

  private static List<String> validateFreeTimes(JsonNode an) {
    List<String> issues = new ArrayList<>();

    var freeTimes = an.path(FREE_TIMES);
    if (!freeTimes.isArray() || freeTimes.isEmpty()) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(FREE_TIMES));
      return issues;
    }

    boolean foundValidTypeCodes = false;
    boolean foundValidISOEquipment = false;
    boolean foundValidEquipRefs = false;
    boolean foundValidDuration = false;
    boolean foundValidTimeUnit = false;

    int i = 0;
    for (var ft : freeTimes) {

      var typeCodes = ft.path("typeCodes");
      if (typeCodes.isArray() && !typeCodes.isEmpty()) {
        boolean valid = false;
        for (var tc : typeCodes) {
          String val = tc.asText("");
          if (ANDatasets.FREE_TIME_TYPE_CODES.contains(val)) {
            valid = true;
          }
        }
        if (valid) foundValidTypeCodes = true;
      }

      var isoCodes = ft.path("ISOEquipmentCodes");
      if (isoCodes.isArray() && !isoCodes.isEmpty()) {
        boolean valid = false;
        for (var iso : isoCodes) {
          String val = iso.asText("");
          if (!val.isBlank()) valid = true;
        }
        if (valid) foundValidISOEquipment = true;
      }

      var eqRefs = ft.path("equipmentReferences");
      if (eqRefs.isArray() && !eqRefs.isEmpty()) {
        boolean valid = false;
        for (var ref : eqRefs) {
          String val = ref.asText("");
          if (!val.isBlank()) valid = true;
        }
        if (valid) foundValidEquipRefs = true;
      }

      var dur = ft.path("duration");
      if (dur.isNumber() && dur.asDouble() > 0) {
        foundValidDuration = true;
      }

      String timeUnit = ft.path("timeUnit").asText("");
      if (!timeUnit.isBlank() && ANDatasets.FREE_TIME_TIME_UNIT.contains(timeUnit)) {
        foundValidTimeUnit = true;
      }

      i++;
    }


    if (!foundValidTypeCodes) {
      issues.add(
          FREE_TIMES
              + " must contain at least one item with a valid typeCodes entry (DEM/DET/STO)");
    }
    if (!foundValidISOEquipment) {
      issues.add(
          FREE_TIMES + " must contain at least one item with a valid ISOEquipmentCodes value");
    }
    if (!foundValidEquipRefs) {
      issues.add(FREE_TIMES + " must contain at least one item with a valid equipmentReference");
    }
    if (!foundValidDuration) {
      issues.add(FREE_TIMES + " must contain at least one item with a positive duration");
    }
    if (!foundValidTimeUnit) {
      issues.add(FREE_TIMES + " must contain at least one item with a valid timeUnit (CD/WD/HR)");
    }

    return issues;
  }


  public static JsonContentCheck atLeastOneANChargesCorrect() {
    return JsonAttribute.customValidator(
        ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_OBJECT.formatted(CHARGES),
        (body, contextPath) -> {
          Set<String> allIssues = new LinkedHashSet<>();
          var ans = body.path("arrivalNotices");

          if (!ans.isArray() || ans.isEmpty()) {
            allIssues.add("No Arrival Notices found in the payload");
            return ConformanceCheckResult.simple(allIssues);
          }

          int anIndex = 0;

          for (var an : ans) {
            List<String> errors = validateCharges(an);

            if (errors.isEmpty()) {
              return ConformanceCheckResult.simple(Set.of());
            }

            for (String e : errors) {
              allIssues.add("arrivalNotices[" + anIndex + "]." + e);
            }

            anIndex++;
          }

          return ConformanceCheckResult.simple(allIssues);
        });
  }

  private static List<String> validateCharges(JsonNode an) {
    List<String> issues = new ArrayList<>();

    var arr = an.path(CHARGES);
    if (!arr.isArray() || arr.isEmpty()) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted(CHARGES));
      return issues;
    }

    boolean foundValidChargeName = false;
    boolean foundValidCurrencyAmount = false;
    boolean foundValidCurrencyCode = false;
    boolean foundValidPaymentTerm = false;
    boolean foundValidUnitPrice = false;
    boolean foundValidQuantity = false;

    for (JsonNode charge : arr) {

      String name = charge.path("chargeName").asText("");
      if (!name.isBlank()) {
        foundValidChargeName = true;
      }

      var ca = charge.path("currencyAmount");
      if (ca.isNumber() && ca.asDouble() > 0) {
        foundValidCurrencyAmount = true;
      }

      String currencyCode = charge.path("currencyCode").asText("");
      if (!currencyCode.isBlank()) {
        foundValidCurrencyCode = true;
      }

      var unitPrice = charge.path("unitPrice");
      if (unitPrice.isNumber() && unitPrice.asDouble() > 0) {
        foundValidUnitPrice = true;
      }

      var qty = charge.path("quantity");
      if (qty.isNumber() && qty.asDouble() > 0) {
        foundValidQuantity = true;
      }

      String pt = charge.path("paymentTermCode").asText("");
      if (!pt.isBlank() && ANDatasets.PAYMENT_TERM_CODE.contains(pt)) {
        foundValidPaymentTerm = true;
      }
    }

    if (!foundValidChargeName) {
      issues.add("At least one" + CHARGES + " item must contain a valid non-empty 'chargeName'");
    }
    if (!foundValidCurrencyAmount) {
      issues.add("At least one" + CHARGES + " item must contain a positive 'currencyAmount'");
    }
    if (!foundValidCurrencyCode) {
      issues.add("At least one" + CHARGES + " item must contain a valid non-empty 'currencyCode'");
    }
    if (!foundValidPaymentTerm) {
      issues.add(
          "At least one" + CHARGES + " item must contain a valid 'paymentTermCode' (PRE or COL)");
    }
    if (!foundValidUnitPrice) {
      issues.add("At least one" + CHARGES + " item must contain a positive 'unitPrice'");
    }
    if (!foundValidQuantity) {
      issues.add("At least one" + CHARGES + " item must contain a positive 'quantity'");
    }

    return issues;
  }


  private static final List<String> ADDRESS_FIELDS =
      List.of(
          "street",
          "streetNumber",
          "floor",
          "postCode",
          "POBox",
          "city",
          "stateRegion",
          "countryCode");

  public static ActionCheck getANGetResponseChecks(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dspSupplier) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(nonEmptyArrivalNotices());
    checks.addAll(
        guardEachWithBodyPresent(
            getPayloadChecks(dspSupplier.get().scenarioType()), "arrivalNotices"));
    return JsonAttribute.contentChecks(
        ANRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }


  public static ActionCheck getANNPostPayloadChecks(
      UUID matchedExchangeUuid, String expectedApiVersion) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE_NOTIFICATION);
    return JsonAttribute.contentChecks(
        ANRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        checks);
  }

  public static JsonContentCheck atLeastOneTransportDocumentReferenceCorrectANN() {
    return JsonAttribute.customValidator(
        "At least one Arrival Notice Notification must demonstrate the correct use of the 'transportDocumentReference' attribute",
        (body, ctx) -> {
          var ans = body.path("arrivalNoticeNotifications");
          Set<String> errors = new LinkedHashSet<>();
          int i = 0;

          for (var an : ans) {
            List<String> local = validateTDR(an);
            final int idx = i;
            if (local.isEmpty()) return ConformanceCheckResult.simple(Set.of());
            local.forEach(e -> errors.add("arrivalNoticeNotifications[" + idx + "]." + e));
            i++;
          }
          return ConformanceCheckResult.simple(errors);
        });
  }

  public static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE_NOTIFICATION =
      JsonAttribute.customValidator(
          "At least one Arrival Notice Notification must be included in a message sent to the sandbox during conformance testing",
          body ->
              ConformanceCheckResult.simple(
                  (body.path("arrivalNoticeNotifications").isEmpty())
                      ? Set.of("The response body must not be empty")
                      : Set.of()));

  public static List<JsonRebasableContentCheck> guardEachWithBodyPresent(
      List<JsonContentCheck> checks, String payload) {

    Predicate<JsonNode> bodyPresent = body -> !JsonUtil.isMissingOrEmpty(body.path(payload));

    return checks.stream()
        .map(
            ch -> {
              JsonRebasableContentCheck rebasable =
                  JsonAttribute.customValidator(ch.description(), (node, ctx) -> ch.validate(node));

              return JsonAttribute.ifThen(ch.description(), bodyPresent, rebasable);
            })
        .toList();
  }
}
