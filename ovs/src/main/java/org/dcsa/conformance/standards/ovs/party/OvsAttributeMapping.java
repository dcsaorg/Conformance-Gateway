package org.dcsa.conformance.standards.ovs.party;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

public record OvsAttributeMapping(
    String jsonPath, BiPredicate<JsonNode, String> condition, Set<String> values) {

  public static Map<String, List<OvsAttributeMapping>> initializeAttributeMappings() {
    Map<String, List<OvsAttributeMapping>> attributeMappings = new HashMap<>();

    addCarrierServiceNameMappings(attributeMappings);
    addCarrierServiceCodeMappings(attributeMappings);
    addUniversalServiceReferenceMappings(attributeMappings);
    addVesselIMONumberMappings(attributeMappings);
    addVesselNameMappings(attributeMappings);
    addCarrierVoyageNumberMappings(attributeMappings);
    addUniversalVoyageReferenceMappings(attributeMappings);
    addUNLocationCodeMappings(attributeMappings);
    addFacilitySMDGCodeMappings(attributeMappings);
    addStartDateMappings(attributeMappings);
    addEndDateMappings(attributeMappings);
    return attributeMappings;
  }

  private static void addCarrierServiceNameMappings(
      Map<String, List<OvsAttributeMapping>> attributeMappings) {
    attributeMappings.put(
        "carrierServiceName",
        List.of(
            new OvsAttributeMapping(
                "carrierServiceName", OvsAttributeMapping::isNodeMatchingValue, Set.of())));
  }

  private static void addCarrierServiceCodeMappings(
      Map<String, List<OvsAttributeMapping>> attributeMappings) {
    attributeMappings.put(
        "carrierServiceCode",
        List.of(
            new OvsAttributeMapping(
                "carrierServiceCode", OvsAttributeMapping::isNodeMatchingValue, Set.of())));
  }

  private static void addUniversalServiceReferenceMappings(
      Map<String, List<OvsAttributeMapping>> attributeMappings) {
    attributeMappings.put(
        "universalServiceReference",
        List.of(
            new OvsAttributeMapping(
                "universalServiceReference", OvsAttributeMapping::isNodeMatchingValue, Set.of())));
  }

  private static void addVesselNameMappings(
      Map<String, List<OvsAttributeMapping>> attributeMappings) {
    attributeMappings.put(
        "vesselName",
        List.of(
            new OvsAttributeMapping(
                "vesselSchedules/*/vesselName",
                OvsAttributeMapping::isNodeMatchingValue,
                Set.of())));
  }

  private static void addVesselIMONumberMappings(
      Map<String, List<OvsAttributeMapping>> attributeMappings) {
    attributeMappings.put(
        "vesselIMONumber",
        List.of(
            new OvsAttributeMapping(
                "vesselSchedules/*/vesselIMONumber",
                OvsAttributeMapping::isNodeMatchingValue,
                Set.of())));
  }

  private static void addUniversalVoyageReferenceMappings(
      Map<String, List<OvsAttributeMapping>> attributeMappings) {
    attributeMappings.put(
        "carrierVoyageNumber",
        List.of(
            new OvsAttributeMapping(
                "vesselSchedules/*/transportCalls/*/carrierExportVoyageNumber",
                OvsAttributeMapping::isNodeMatchingValue,
                Set.of()),
            new OvsAttributeMapping(
                "vesselSchedules/*/transportCalls/*/carrierImportVoyageNumber",
                OvsAttributeMapping::isNodeMatchingValue,
                Set.of())));
  }

  private static void addCarrierVoyageNumberMappings(
      Map<String, List<OvsAttributeMapping>> attributeMappings) {
    attributeMappings.put(
        "universalVoyageReference",
        List.of(
            new OvsAttributeMapping(
                "vesselSchedules/*/transportCalls/*/universalImportVoyageReference",
                OvsAttributeMapping::isNodeMatchingValue,
                Set.of()),
            new OvsAttributeMapping(
                "vesselSchedules/*/transportCalls/*/universalExportVoyageReference",
                OvsAttributeMapping::isNodeMatchingValue,
                Set.of())));
  }

  private static void addUNLocationCodeMappings(
      Map<String, List<OvsAttributeMapping>> attributeMappings) {
    attributeMappings.put(
        "UNLocationCode",
        List.of(
            new OvsAttributeMapping(
                "vesselSchedules/*/transportCalls/*/location/UNLocationCode",
                OvsAttributeMapping::isNodeMatchingValue,
                Set.of())));
  }

  private static void addFacilitySMDGCodeMappings(
      Map<String, List<OvsAttributeMapping>> attributeMappings) {
    attributeMappings.put(
        "facilitySMDGCode",
        List.of(
            new OvsAttributeMapping(
                "vesselSchedules/*/transportCalls/*/location/facilitySMDGCode",
                OvsAttributeMapping::isNodeMatchingValue,
                Set.of())));
  }

  private static void addStartDateMappings(
      Map<String, List<OvsAttributeMapping>> attributeMappings) {
    attributeMappings.put(
        "startDate",
        List.of(
            new OvsAttributeMapping(
                "vesselSchedules/*/transportCalls/*/timestamps/*/eventDateTime",
                OvsAttributeMapping::checkStartDateCondition,
                Set.of())));
  }

  private static void addEndDateMappings(Map<String, List<OvsAttributeMapping>> attributeMappings) {
    attributeMappings.put(
        "endDate",
        List.of(
            new OvsAttributeMapping(
                "vesselSchedules/*/transportCalls/*/timestamps/*/eventDateTime",
                OvsAttributeMapping::checkEndDateCondition,
                Set.of())));
  }

  private static boolean checkStartDateCondition(JsonNode node, String value) {
    if (!node.isMissingNode() && StringUtils.isNotBlank(value)) {
      try {
        OffsetDateTime eventDateTime =
            OffsetDateTime.parse(node.asText(), DateTimeFormatter.ISO_DATE_TIME);
        LocalDate startDate = LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
        return eventDateTime.toLocalDate().isAfter(startDate);
      } catch (DateTimeParseException e) {
        return false;
      }
    }
    return true;
  }

  private static boolean checkEndDateCondition(JsonNode node, String value) {
    if (!node.isMissingNode() && StringUtils.isNotBlank(value)) {
      try {
        OffsetDateTime eventDateTime =
            OffsetDateTime.parse(node.asText(), DateTimeFormatter.ISO_DATE_TIME);
        LocalDate endDate = LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
        return eventDateTime.toLocalDate().isBefore(endDate);
      } catch (DateTimeParseException e) {
        return false;
      }
    }
    return true;
  }

  private static boolean isNodeMatchingValue(JsonNode node, String value) {
    return !node.isMissingNode() && node.asText().equals(value);
  }
}
