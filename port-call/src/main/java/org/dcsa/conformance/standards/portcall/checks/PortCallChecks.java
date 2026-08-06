package org.dcsa.conformance.standards.portcall.checks;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.portcall.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.portcall.party.PortCallRole;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class PortCallChecks {

  private PortCallChecks() {
  }

  public static ActionCheck getPortCallPostPayloadChecks(
    UUID matchedExchangeUuid,
    String expectedApiVersion,
    Supplier<DynamicScenarioParameters> dsp) {

    return JsonAttribute.contentChecks(
      "",
      "The Event Producer has correctly demonstrated the use of functionally required attributes in the Port Call payload",
      PortCallRole::isProducer,
      matchedExchangeUuid,
      HttpMessageType.REQUEST,
      expectedApiVersion,
      scenarioChecks(dsp));
  }

  public static ActionCheck getGetResponsePayloadChecks(
    UUID matchedExchangeUuid,
    String expectedApiVersion,
    Supplier<DynamicScenarioParameters> dsp) {

    return JsonAttribute.contentChecks(
      "",
      "The Event Producer has correctly demonstrated the use of functionally required attributes in the payload",
      PortCallRole::isProducer,
      matchedExchangeUuid,
      HttpMessageType.RESPONSE,
      expectedApiVersion,
      scenarioChecks(dsp));
  }

  private static List<JsonContentCheck> scenarioChecks(Supplier<DynamicScenarioParameters> dsp) {
    List<JsonContentCheck> checks = new ArrayList<>();
    String scenarioType = dsp.get().scenarioType();

    checks.add(nonEmptyEvents());

    if ("TIMESTAMP".equals(scenarioType)) {
      checks.addAll(timestampScenarioChecks());
    }
    if ("MOVE_FORECAST".equals(scenarioType)) {
      checks.addAll(movesForecastsScenarioChecks());
    }
    return checks;
  }

  public static JsonContentCheck nonEmptyEvents() {
    return JsonAttribute.customValidator(
      "At least one event must be included in the tested message.",
      (body, ctx) -> {
        var events = body.path("events");
        if (!events.isArray() || events.isEmpty()) {
          return ConformanceCheckResult.simple(Set.of("events must be a non-empty array"));
        }
        return ConformanceCheckResult.simple(Set.of());
      });
  }

  public static List<JsonContentCheck> timestampScenarioChecks() {
    List<JsonContentCheck> checks = new ArrayList<>();
    checks.add(atLeastOneEventIncludesTimestampObject());
    checks.add(atLeastOneTimestampClassifierCodeCorrect());
    checks.add(atLeastOneTimestampServiceDateTimeCorrect());
    return checks;
  }

  public static JsonContentCheck atLeastOneEventIncludesTimestampObject() {
    return JsonAttribute.customValidator(
      "At least one event must include a timestamp object.",
      (body, ctx) -> {
        var events = body.path("events");
        if (!events.isArray() || events.isEmpty()) {
          return ConformanceCheckResult.simple(Set.of("events must be a non-empty array"));
        }
        for (JsonNode event : events) {
          if (!JsonUtil.isMissingOrEmpty(event.path("timestamp"))) {
            return ConformanceCheckResult.simple(Set.of());
          }
        }
        return ConformanceCheckResult.simple(
          Set.of("At least one event must include a timestamp object"));
      });
  }

  public static JsonContentCheck atLeastOneTimestampClassifierCodeCorrect() {
    return JsonAttribute.customValidator(
      "The timestamp object used to demonstrate the scenario must demonstrate the correct use of the classifierCode attribute.",
      (body, ctx) -> {
        var events = body.path("events");
        Set<String> errors = new LinkedHashSet<>();

        if (!events.isArray() || events.isEmpty()) {
          errors.add("events must be a non-empty array");
          return ConformanceCheckResult.simple(errors);
        }

        for (int i = 0; i < events.size(); i++) {
          var event = events.get(i);
          List<String> problems = validateTimestampClassifierCode(event);
          if (problems.isEmpty()) {
            return ConformanceCheckResult.simple(Set.of());
          }
          for (String err : problems) {
            errors.add("events[" + i + "]." + err);
          }
        }

        return ConformanceCheckResult.simple(errors);
      });
  }

  private static List<String> validateTimestampClassifierCode(JsonNode event) {
    List<String> issues = new ArrayList<>();
    var ts = event.path("timestamp");

    if (JsonUtil.isMissingOrEmpty(ts)) {
      issues.add("timestamp.classifierCode must be functionally present and non-empty");
      return issues;
    }

    if (ts.path("classifierCode").asText("").isBlank()) {
      issues.add("timestamp.classifierCode must be functionally present and non-empty");
    }
    return issues;
  }

  public static JsonContentCheck atLeastOneTimestampServiceDateTimeCorrect() {
    return JsonAttribute.customValidator(
      "The timestamp object used to demonstrate the scenario must demonstrate the correct use of the serviceDateTime attribute.",
      (body, ctx) -> {
        var events = body.path("events");
        Set<String> errors = new LinkedHashSet<>();

        if (!events.isArray() || events.isEmpty()) {
          errors.add("events must be a non-empty array");
          return ConformanceCheckResult.simple(errors);
        }

        for (int i = 0; i < events.size(); i++) {
          var event = events.get(i);
          List<String> problems = validateTimestampServiceDateTime(event);
          if (problems.isEmpty()) {
            return ConformanceCheckResult.simple(Set.of());
          }
          for (String err : problems) {
            errors.add("events[" + i + "]." + err);
          }
        }

        return ConformanceCheckResult.simple(errors);
      });
  }

  private static List<String> validateTimestampServiceDateTime(JsonNode event) {
    List<String> issues = new ArrayList<>();
    var ts = event.path("timestamp");

    if (JsonUtil.isMissingOrEmpty(ts)) {
      issues.add("timestamp.serviceDateTime must be functionally present and non-empty");
      return issues;
    }

    if (ts.path("serviceDateTime").asText("").isBlank()) {
      issues.add("timestamp.serviceDateTime must be functionally present and non-empty");
    }
    return issues;
  }

  public static List<JsonContentCheck> movesForecastsScenarioChecks() {
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(movesForecastsArrayNonEmptyCheck());
    checks.add(movesForecastsItemHasUnitsObjectCheck());

    checks.add(loadUnitsCategoryCheck());
    checks.add(dischargeUnitsCategoryCheck());

    checks.add(restowUnitsSizeCheck());
    checks.add(loadUnitsTotalUnitsSizeCheck());
    checks.add(loadUnitsLadenUnitsSizeCheck());
    checks.add(loadUnitsEmptyUnitsSizeCheck());
    checks.add(loadUnitsPluggedReeferUnitsSizeCheck());
    checks.add(loadUnitsOutOfGaugeUnitsSizeCheck());

    checks.add(dischargeUnitsTotalUnitsSizeCheck());
    checks.add(dischargeUnitsLadenUnitsSizeCheck());
    checks.add(dischargeUnitsEmptyUnitsSizeCheck());
    checks.add(dischargeUnitsPluggedReeferUnitsSizeCheck());
    checks.add(dischargeUnitsOutOfGaugeUnitsSizeCheck());

    return checks;
  }

  public static JsonContentCheck movesForecastsArrayNonEmptyCheck() {
    return JsonAttribute.customValidator(
      "At least one event must include a non-empty movesForecasts array.",
      (body, ctx) -> {
        var events = body.path("events");

        if (!events.isArray() || events.isEmpty()) {
          return ConformanceCheckResult.simple(Set.of("events must be a non-empty array"));
        }

        for (JsonNode event : events) {
          var mfArr = event.path("movesForecasts");
          if (mfArr.isArray() && !mfArr.isEmpty()) {
            return ConformanceCheckResult.simple(Set.of());
          }
        }

        return ConformanceCheckResult.simple(Set.of("At least one event must include a non-empty movesForecasts array"));
      });
  }

  public static JsonContentCheck movesForecastsItemHasUnitsObjectCheck() {
    return JsonAttribute.customValidator(
      "At least one movesForecasts[] item must include at least one of the restowUnits, loadUnits, or dischargeUnits objects.",
      (body, ctx) -> {
        var events = body.path("events");

        if (!events.isArray() || events.isEmpty()) {
          return ConformanceCheckResult.simple(Set.of("events must be a non-empty array"));
        }

        for (JsonNode event : events) {
          var mfArr = event.path("movesForecasts");
          if (!mfArr.isArray() || mfArr.isEmpty()) continue;

          for (JsonNode mf : mfArr) {
            if (!JsonUtil.isMissingOrEmpty(mf.path("restowUnits"))
              || !JsonUtil.isMissingOrEmpty(mf.path("loadUnits"))
              || !JsonUtil.isMissingOrEmpty(mf.path("dischargeUnits"))) {
              return ConformanceCheckResult.simple(Set.of());
            }
          }
        }

        return ConformanceCheckResult.simple(
          Set.of("At least one movesForecasts[] item must include at least one of the restowUnits, loadUnits, or dischargeUnits objects"));
      });
  }

  public static JsonContentCheck loadUnitsCategoryCheck() {
    return buildUnitsCategoryCheck("movesForecasts[].loadUnits", mf -> mf.path("loadUnits"));
  }

  public static JsonContentCheck dischargeUnitsCategoryCheck() {
    return buildUnitsCategoryCheck(
      "movesForecasts[].dischargeUnits", mf -> mf.path("dischargeUnits"));
  }

  private static JsonContentCheck buildUnitsCategoryCheck(
    String label, Function<JsonNode, JsonNode> extractor) {

    String description =
      "For every applicable occurrence of the '"
        + label
        + "' object, the object must include either totalUnits or at least one of"
        + " ladenUnits, emptyUnits, pluggedReeferUnits, or outOfGaugeUnits.";

    return JsonAttribute.customValidator(
      description,
      (body, ctx) -> {
        var events = body.path("events");
        if (!events.isArray() || events.isEmpty()) {
          return ConformanceCheckResult.simple(Set.of("events must be a non-empty array"));
        }

        Set<String> errors = new LinkedHashSet<>();

        for (int e = 0; e < events.size(); e++) {
          var mfArr = events.get(e).path("movesForecasts");
          if (!mfArr.isArray() || mfArr.isEmpty()) continue;

          for (int m = 0; m < mfArr.size(); m++) {
            JsonNode mf = mfArr.get(m);
            JsonNode base = extractor.apply(mf);

            if (base.isMissingNode() || base.isNull()) {
              continue;
            }

            boolean valid =
              !JsonUtil.isMissingOrEmpty(base.path("totalUnits"))
                || !JsonUtil.isMissingOrEmpty(base.path("ladenUnits"))
                || !JsonUtil.isMissingOrEmpty(base.path("emptyUnits"))
                || !JsonUtil.isMissingOrEmpty(base.path("pluggedReeferUnits"))
                || !JsonUtil.isMissingOrEmpty(base.path("outOfGaugeUnits"));

            if (!valid) {
              errors.add(
                "events["
                  + e
                  + "].movesForecasts["
                  + m
                  + "]."
                  + suffixOf(label)
                  + " must be non empty and must contain 'totalUnits' or at least one of "
                  + "'ladenUnits', 'emptyUnits', 'pluggedReeferUnits', 'outOfGaugeUnits'");
            }
          }
        }

        return ConformanceCheckResult.simple(errors);
      });
  }

  public static JsonContentCheck restowUnitsSizeCheck() {
    return buildUnitsSizeCheck("movesForecasts[].restowUnits", mf -> mf.path("restowUnits"));
  }

  public static JsonContentCheck loadUnitsTotalUnitsSizeCheck() {
    return buildUnitsSizeCheck(
      "movesForecasts[].loadUnits.totalUnits", mf -> mf.path("loadUnits").path("totalUnits"));
  }

  public static JsonContentCheck loadUnitsLadenUnitsSizeCheck() {
    return buildUnitsSizeCheck(
      "movesForecasts[].loadUnits.ladenUnits", mf -> mf.path("loadUnits").path("ladenUnits"));
  }

  public static JsonContentCheck loadUnitsEmptyUnitsSizeCheck() {
    return buildUnitsSizeCheck(
      "movesForecasts[].loadUnits.emptyUnits", mf -> mf.path("loadUnits").path("emptyUnits"));
  }

  public static JsonContentCheck loadUnitsPluggedReeferUnitsSizeCheck() {
    return buildUnitsSizeCheck(
      "movesForecasts[].loadUnits.pluggedReeferUnits",
      mf -> mf.path("loadUnits").path("pluggedReeferUnits"));
  }

  public static JsonContentCheck loadUnitsOutOfGaugeUnitsSizeCheck() {
    return buildUnitsSizeCheck(
      "movesForecasts[].loadUnits.outOfGaugeUnits",
      mf -> mf.path("loadUnits").path("outOfGaugeUnits"));
  }

  public static JsonContentCheck dischargeUnitsTotalUnitsSizeCheck() {
    return buildUnitsSizeCheck(
      "movesForecasts[].dischargeUnits.totalUnits",
      mf -> mf.path("dischargeUnits").path("totalUnits"));
  }

  public static JsonContentCheck dischargeUnitsLadenUnitsSizeCheck() {
    return buildUnitsSizeCheck(
      "movesForecasts[].dischargeUnits.ladenUnits",
      mf -> mf.path("dischargeUnits").path("ladenUnits"));
  }

  public static JsonContentCheck dischargeUnitsEmptyUnitsSizeCheck() {
    return buildUnitsSizeCheck(
      "movesForecasts[].dischargeUnits.emptyUnits",
      mf -> mf.path("dischargeUnits").path("emptyUnits"));
  }

  public static JsonContentCheck dischargeUnitsPluggedReeferUnitsSizeCheck() {
    return buildUnitsSizeCheck(
      "movesForecasts[].dischargeUnits.pluggedReeferUnits",
      mf -> mf.path("dischargeUnits").path("pluggedReeferUnits"));
  }

  public static JsonContentCheck dischargeUnitsOutOfGaugeUnitsSizeCheck() {
    return buildUnitsSizeCheck(
      "movesForecasts[].dischargeUnits.outOfGaugeUnits",
      mf -> mf.path("dischargeUnits").path("outOfGaugeUnits"));
  }

  private static JsonContentCheck buildUnitsSizeCheck(
    String label, Function<JsonNode, JsonNode> extractor) {

    String description =
      "For every applicable occurrence of the '"
        + label
        + "' object, the object must include either totalUnits or at least one of"
        + " size20Units, size40Units, or size45Units.";

    return JsonAttribute.customValidator(
      description,
      (body, ctx) -> {
        var events = body.path("events");

        if (!events.isArray() || events.isEmpty()) {
          return ConformanceCheckResult.simple(Set.of("events must be a non-empty array"));
        }

        Set<String> errors = new LinkedHashSet<>();

        for (int e = 0; e < events.size(); e++) {
          var mfArr = events.get(e).path("movesForecasts");
          if (!mfArr.isArray()) continue;

          for (int m = 0; m < mfArr.size(); m++) {
            JsonNode base = extractor.apply(mfArr.get(m));
            if (base.isMissingNode() || base.isNull()) {
              continue;
            }

            String basePath = "events[" + e + "].movesForecasts[" + m + "]." + suffixOf(label);
            errors.addAll(validateUnitsSizeBlock(base, basePath));
          }
        }

        return ConformanceCheckResult.simple(errors);
      });
  }

  private static String suffixOf(String label) {
    return label.substring("movesForecasts[].".length());
  }

  private static List<String> validateUnitsSizeBlock(JsonNode units, String basePath) {
    List<String> issues = new ArrayList<>();

    boolean hasTotal = units.path("totalUnits").isNumber();
    boolean has20 = units.path("size20Units").isNumber();
    boolean has40 = units.path("size40Units").isNumber();
    boolean has45 = units.path("size45Units").isNumber();

    if (!hasTotal && !has20 && !has40 && !has45) {
      issues.add(basePath + " must contain numeric totalUnits or size20Units or size40Units or size45Units");
    }

    return issues;
  }
}
