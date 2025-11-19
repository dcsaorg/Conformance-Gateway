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
        "At least one Arrival Notice must demonstrate the correct use of the 'carrierCode' attribute",
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
    var v = an.path("carrierCode");
    if (JsonUtil.isMissingOrEmpty(v)) {
      issues.add("carrierCode must functionally be present and non-empty");
    }
    return issues;
  }

  public static JsonContentCheck atLeastOneTransportDocumentReferenceCorrect() {
    return JsonAttribute.customValidator(
        "At least one Arrival Notice must demonstrate the correct use of the 'transportDocumentReference' attribute",
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
    var v = an.path("transportDocumentReference");
    if (JsonUtil.isMissingOrEmpty(v)) {
      issues.add("transportDocumentReference must be non-empty");
    }
    return issues;
  }

  public static JsonContentCheck atLeastOneCarrierCodeListProviderCorrect() {
    return JsonAttribute.customValidator(
        "At least one Arrival Notice must demonstrate the correct use of the 'carrierCodeListProvider' attribute",
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
    var v = an.path("carrierCodeListProvider");

    if (JsonUtil.isMissingOrEmpty(v)) {
      issues.add("carrierCodeListProvider must functionally be non-empty");
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
        "At least one Arrival Notice must demonstrate the correct use of the 'carrierContactInformation' object",
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
    var ccis = an.path("carrierContactInformation");
    List<String> errors = new ArrayList<>();

    if (!ccis.isArray() || ccis.isEmpty()) {
      errors.add("carrierContactInformation must be a non-empty array");
      return errors;
    }

    boolean anyValid = false;
    int idx = 0;

    for (JsonNode cci : ccis) {
      List<String> local = new ArrayList<>();

      if (cci.path("name").asText().isBlank()) {
        local.add("carrierContactInformation[" + idx + "].name must functionally be non-empty");
      }

      boolean hasEmailOrPhone =
          !cci.path("email").asText().isBlank() || !cci.path("phone").asText().isBlank();

      if (!hasEmailOrPhone) {
        local.add(
            "carrierContactInformation["
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
        "At least one Arrival Notice must demonstrate the correct use of the 'deliveryTypeAtDestination' object",
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
    var v = an.path("deliveryTypeAtDestination");

    if (JsonUtil.isMissingOrEmpty(v)) {
      issues.add("deliveryTypeAtDestination must functionally be present and non-empty");
      return issues;
    }

    String val = v.asText();
    if (!ANDatasets.DELIVERY_TYPE_AT_DESTINATION.contains(val)) {
      issues.add("Invalid deliveryTypeAtDestination, must be one of CY', 'SD', or 'CFS'");
    }

    return issues;
  }

  public static JsonContentCheck atLeastOneDocumentPartiesCorrect() {
    return JsonAttribute.customValidator(
        "At least one Arrival Notice must demonstrate the correct use of the 'documentParties' object",
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

    var documentParties = an.path("documentParties");
    if (!documentParties.isArray() || documentParties.isEmpty()) {
      issues.add("documentParties must be a non-empty array");
      return issues;
    }

    int i = 0;
    for (JsonNode documentParty : documentParties) {
      String base = "documentParties[" + i + "]";

      // -----------------------------
      // partyFunction
      // -----------------------------
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
        "At least one Arrival Notice must demonstrate the correct use of the 'transport' object",
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
    var t = an.path("transport");

    if (JsonUtil.isMissingOrEmpty(t)) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted("transport"));
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
            "utilizedTransportEquipments"),
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

    var arr = an.path("utilizedTransportEquipments");
    if (!arr.isArray() || arr.isEmpty()) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted("utilizedTransportEquipments"));
      return issues;
    }

    int i = 0;
    for (var ute : arr) {
      String base = "utilizedTransportEquipments[" + i + "]";

      var eq = ute.path("equipment");
      if (JsonUtil.isMissingOrEmpty(eq)) {
        issues.add(base + ".equipment must be present and non-empty");
      } else {
        if (eq.path("equipmentReference").asText().isBlank()) {
          issues.add(base + ".equipment.equipmentReference must be non-empty");
        }
        if (eq.path("ISOEquipmentCode").asText().isBlank()) {
          issues.add(base + ".equipment.ISOEquipmentCode must be non-empty");
        }
      }

      var seals = ute.path("seals");

      if (!seals.isArray() || seals.isEmpty()) {
        issues.add(base + ".seals must be a non-empty array");
      } else {
        boolean hasValidSeal = false;

        int s = 0;
        for (var seal : seals) {
          String sealPath = base + ".seals[" + s + "]";
          var numNode = seal.path("number");

          if (numNode != null && !numNode.asText().isBlank()) {
            hasValidSeal = true;
          } else {
            issues.add(sealPath + ".number must be non-empty");
          }

          s++;
        }

        if (!hasValidSeal) {
          issues.add(base + ".seals must contain at least one seal with a non-empty number");
        }
      }

      i++;
    }

    return issues;
  }

  public static JsonContentCheck atLeastOneConsignmentItemsCorrect() {
    return JsonAttribute.customValidator(
        ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_OBJECT.formatted("consignmentItems"),
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

    var arr = an.path("consignmentItems");
    if (!arr.isArray() || arr.isEmpty()) {
      issues.add(S_MUST_BE_PRESENT_AND_NON_EMPTY.formatted("consignmentItems"));
      return issues;
    }

    int i = 0;
    for (var item : arr) {
      String base = "consignmentItems[" + i + "]";

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

        int c = 0;
        for (var cItem : cargo) {
          String cargoBase = base + ".cargoItems[" + c + "]";

          var eqRef = cItem.path("equipmentReference").asText("");
          if (eqRef.isBlank()) {
            issues.add(cargoBase + ".equipmentReference must be non-empty");
          } else {
            foundValidEquipmentRef = true;
          }

          var gw = cItem.path("cargoGrossWeight");
          if (JsonUtil.isMissingOrEmpty(gw)) {
            issues.add(cargoBase + ".cargoGrossWeight must be present");
          } else {
            var valueNode = gw.path("value");
            if (!valueNode.isNumber()) {
              issues.add(cargoBase + ".cargoGrossWeight.value must be a number");
            } else if (valueNode.asDouble() <= 0) {
              issues.add(cargoBase + ".cargoGrossWeight.value must be positive");
            } else {
              foundValidWeightValue = true;
            }

            var unit = gw.path("unit").asText("");
            if (unit.isBlank()) {
              issues.add(cargoBase + ".cargoGrossWeight.unit must be non-empty");
            } else if (!ANDatasets.CARGO_GROSS_WEIGHT_UNIT.contains(unit)) {
              issues.add(cargoBase + ".cargoGrossWeight.unit must be one of KGM, LBR, GRM, ONZ");
            } else {
              foundValidWeightUnit = true;
            }
          }

          var op = cItem.path("outerPackaging");
          if (JsonUtil.isMissingOrEmpty(op)) {
            issues.add(cargoBase + ".outerPackaging must be present and non-empty");
          } else {

            boolean hasField =
                (op.hasNonNull("packageCode") && !op.path("packageCode").asText().isBlank())
                    || (op.hasNonNull("IMOPackagingCode")
                        && !op.path("IMOPackagingCode").asText().isBlank())
                    || (op.hasNonNull("description") && !op.path("description").asText().isBlank());

            if (!hasField) {
              issues.add(
                  cargoBase
                      + ".outerPackaging must contain at least one of: "
                      + "packageCode, IMOPackagingCode, or description");
            } else {
              foundValidOuterPkg = true;
            }

            // numberOfPackages
            var numPkgsNode = op.path("numberOfPackages");
            if (!numPkgsNode.isNumber()) {
              issues.add(cargoBase + ".outerPackaging.numberOfPackages must be a number");
            } else if (numPkgsNode.asInt() <= 0) {
              issues.add(cargoBase + ".outerPackaging.numberOfPackages must be positive");
            } else {
              foundValidNumPackages = true;
            }
          }

          c++;
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
        ATLEAST_ONE_AN_MUST_DEMONSTRATE_THE_CORRECT_USE_OF_S_OBJECT.formatted("freeTimes"),
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

    var freeTimes = an.path("freeTimes");
    if (!freeTimes.isArray() || freeTimes.isEmpty()) {
      issues.add("freeTimes must be a non-empty array");
      return issues;
    }

    boolean foundValidTypeCodes = false;
    boolean foundValidISOEquipment = false;
    boolean foundValidEquipRefs = false;
    boolean foundValidDuration = false;
    boolean foundValidTimeUnit = false;

    int i = 0;
    for (var ft : freeTimes) {
      String base = "freeTimes[" + i + "]";

      var typeCodes = ft.path("typeCodes");
      if (!typeCodes.isArray() || typeCodes.isEmpty()) {
        issues.add(base + ".typeCodes must be a non-empty array");
      } else {
        boolean valid = false;
        int t = 0;
        for (var tc : typeCodes) {
          String val = tc.asText("");
          if (ANDatasets.FREE_TIME_TYPE_CODES.contains(val)) valid = true;
          if (val.isBlank()) {
            issues.add(base + ".typeCodes[" + t + "] must be non-empty");
          }
          t++;
        }
        if (valid) foundValidTypeCodes = true;
      }

      var isoCodes = ft.path("ISOEquipmentCodes");
      if (!isoCodes.isArray() || isoCodes.isEmpty()) {
        issues.add(base + ".ISOEquipmentCodes must be a non-empty array");
      } else {
        boolean valid = false;
        int j = 0;
        for (var iso : isoCodes) {
          String val = iso.asText("");
          if (!val.isBlank()) valid = true;
          else issues.add(base + ".ISOEquipmentCodes[" + j + "] must be non-empty");
          j++;
        }
        if (valid) foundValidISOEquipment = true;
      }

      var eqRefs = ft.path("equipmentReferences");
      if (!eqRefs.isArray() || eqRefs.isEmpty()) {
        issues.add(base + ".equipmentReferences must be a non-empty array");
      } else {
        boolean valid = false;
        int k = 0;
        for (var ref : eqRefs) {
          String val = ref.asText("");
          if (!val.isBlank()) valid = true;
          else issues.add(base + ".equipmentReferences[" + k + "] must be non-empty");
          k++;
        }
        if (valid) foundValidEquipRefs = true;
      }

      var dur = ft.path("duration");
      if (!dur.isNumber()) {
        issues.add(base + ".duration must be a positive number");
      } else if (dur.asDouble() > 0) {
        foundValidDuration = true;
      } else {
        issues.add(base + ".duration must be positive");
      }

      var timeUnit = ft.path("timeUnit").asText("");
      if (timeUnit.isBlank()) {
        issues.add(base + ".timeUnit must be non-empty");
      } else if (!ANDatasets.FREE_TIME_TIME_UNIT.contains(timeUnit)) {
        issues.add(base + ".timeUnit must be one of CD, WD, or HR");
      } else {
        foundValidTimeUnit = true;
      }

      i++;
    }

    if (!foundValidTypeCodes) {
      issues.add(
          "freeTimes must contain at least one item with a valid typeCodes entry (DEM/DET/STO)");
    }
    if (!foundValidISOEquipment) {
      issues.add("freeTimes must contain at least one item with a valid ISOEquipmentCodes value");
    }
    if (!foundValidEquipRefs) {
      issues.add("freeTimes must contain at least one item with a valid equipmentReference");
    }
    if (!foundValidDuration) {
      issues.add("freeTimes must contain at least one item with a positive duration");
    }
    if (!foundValidTimeUnit) {
      issues.add("freeTimes must contain at least one item with a valid timeUnit (CD/WD/HR)");
    }

    return issues;
  }

  public static JsonContentCheck atLeastOneANChargesCorrect() {
    return JsonAttribute.customValidator(
        "At least one Arrival Notice must demonstrate the correct use of the 'charges' object",
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

    var arr = an.path("charges");
    if (!arr.isArray() || arr.isEmpty()) {
      issues.add("charges must be a non-empty array");
      return issues;
    }

    boolean foundValidChargeName = false;
    boolean foundValidCurrencyAmount = false;
    boolean foundValidCurrencyCode = false;
    boolean foundValidPaymentTerm = false;
    boolean foundValidUnitPrice = false;
    boolean foundValidQuantity = false;

    int i = 0;
    for (var charge : arr) {
      String base = "charges[" + i + "]";

      String name = charge.path("chargeName").asText("");
      if (name.isBlank()) {
        issues.add(base + ".chargeName must be non-empty");
      } else {
        foundValidChargeName = true;
      }

      var ca = charge.path("currencyAmount");
      if (!ca.isNumber()) {
        issues.add(base + ".currencyAmount must be a positive number");
      } else if (ca.asDouble() <= 0) {
        issues.add(base + ".currencyAmount must be positive");
      } else {
        foundValidCurrencyAmount = true;
      }

      String currencyCode = charge.path("currencyCode").asText("");
      if (currencyCode.isBlank()) {
        issues.add(base + ".currencyCode must be non-empty");
      } else {
        foundValidCurrencyCode = true;
      }

      var unitPrice = charge.path("unitPrice");
      if (!unitPrice.isNumber()) {
        issues.add(base + ".unitPrice must be a positive number");
      } else if (unitPrice.asDouble() <= 0) {
        issues.add(base + ".unitPrice must be positive");
      } else {
        foundValidUnitPrice = true;
      }

      var qty = charge.path("quantity");
      if (!qty.isNumber()) {
        issues.add(base + ".quantity must be a positive number");
      } else if (qty.asDouble() <= 0) {
        issues.add(base + ".quantity must be positive");
      } else {
        foundValidQuantity = true;
      }

      String pt = charge.path("paymentTermCode").asText("");
      if (pt.isBlank()) {
        issues.add(base + ".paymentTermCode must be non-empty");
      } else if (!ANDatasets.PAYMENT_TERM_CODE.contains(pt)) {
        issues.add(base + ".paymentTermCode must be one of PRE or COL");
      } else {
        foundValidPaymentTerm = true;
      }

      i++;
    }

    if (!foundValidChargeName) {
      issues.add("At least one charges item must contain a valid non-empty 'chargeName'");
    }
    if (!foundValidCurrencyAmount) {
      issues.add("At least one charges item must contain a positive 'currencyAmount'");
    }
    if (!foundValidCurrencyCode) {
      issues.add("At least one charges item must contain a valid non-empty 'currencyCode'");
    }
    if (!foundValidPaymentTerm) {
      issues.add("At least one charges item must contain a valid 'paymentTermCode' (PRE or COL)");
    }
    if (!foundValidUnitPrice) {
      issues.add("At least one charges item must contain a positive 'unitPrice'");
    }
    if (!foundValidQuantity) {
      issues.add("At least one charges item must contain a positive 'quantity'");
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
