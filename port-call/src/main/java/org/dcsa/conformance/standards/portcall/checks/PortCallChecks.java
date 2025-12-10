package org.dcsa.conformance.standards.portcall.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.core.util.JsonUtil;
import org.dcsa.conformance.standards.portcall.party.DynamicScenarioParameters;
import org.dcsa.conformance.standards.portcall.party.PortCallRole;

public class PortCallChecks {

  public static ActionCheck getPortCallPostPayloadChecks(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dsp) {

    List<JsonContentCheck> checks = new ArrayList<>();
    String scenarioType = dsp.get().scenarioType();

    checks.add(nonEmptyEvents());

    if ("TIMESTAMP".equals(scenarioType)) {
      checks.addAll(timestampScenarioChecks());
    }
    if ("MOVE_FORECAST".equals(scenarioType)) {
      checks.addAll(movesForecastsScenarioChecks());
    }

    return JsonAttribute.contentChecks(
        "",
        "The Publisher has correctly demonstrated the use of functionally required attributes in the Port Call payload",
        (role) -> true,
        matchedExchangeUuid,
        HttpMessageType.REQUEST,
        expectedApiVersion,
        checks);
  }

  public static ActionCheck getGetResponsePayloadChecks(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dsp) {
    List<JsonContentCheck> checks = new ArrayList<>();
    String scenarioType = dsp.get().scenarioType();

    checks.add(nonEmptyEvents());

    if ("TIMESTAMP".equals(scenarioType)) {
      checks.addAll(timestampScenarioChecks());
    }
    if ("MOVE_FORECAST".equals(scenarioType)) {
      checks.addAll(movesForecastsScenarioChecks());
    }
    return JsonAttribute.contentChecks(
        "",
        "The Publisher has correctly demonstrated the use of functionally required attributes in the payload",
        PortCallRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE,
        expectedApiVersion,
        checks);
  }

  public static JsonContentCheck nonEmptyEvents() {
    return JsonAttribute.customValidator(
        "At least one event must be included in the payload",
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
    checks.add(atLeastOneTimestampClassifierCodeCorrect());
    checks.add(atLeastOneTimestampServiceDateTimeCorrect());
    return checks;
  }

  public static JsonContentCheck atLeastOneTimestampClassifierCodeCorrect() {
    return JsonAttribute.customValidator(
        "At least one event must demonstrate the correct use of the 'timestamp/classifierCode' attribute",
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
              errors.add("arrivalNotices[" + i + "]." + err);
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
        "At least one event must demonstrate the correct use of the 'timestamp/serviceDateTime' attribute",
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
              errors.add("arrivalNotices[" + i + "]." + err);
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
    checks.add(movesForecastsPresenceCheck());
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

  public static JsonContentCheck movesForecastsPresenceCheck() {
    return JsonAttribute.customValidator(
        "At least one event must demonstrate the correct use within the 'movesForecasts' object",
        (body, ctx) -> {
          var events = body.path("events");

          if (!events.isArray() || events.isEmpty()) {
            return ConformanceCheckResult.simple(Set.of("events must be a non-empty array"));
          }

          boolean seenMF = false;
          boolean seenUnitObject = false;

          for (JsonNode event : events) {
            var mfArr = event.path("movesForecasts");

            if (mfArr.isArray() && !mfArr.isEmpty()) {
              seenMF = true;

              for (JsonNode mf : mfArr) {
                if (!JsonUtil.isMissingOrEmpty(mf.path("restowUnits"))
                    || !JsonUtil.isMissingOrEmpty(mf.path("loadUnits"))
                    || !JsonUtil.isMissingOrEmpty(mf.path("dischargeUnits"))) {
                  seenUnitObject = true;
                  break;
                }
              }
            }

            if (seenMF && seenUnitObject) {
              return ConformanceCheckResult.simple(Set.of());
            }
          }

          Set<String> issues = new LinkedHashSet<>();
          if (!seenMF) {
            issues.add("At least one event must include a non-empty movesForecasts array");
          } else {
            issues.add(
                "At least one movesForecasts entry must contain restowUnits/loadUnits/dischargeUnits");
          }
          return ConformanceCheckResult.simple(issues);
        });
  }

  public static JsonContentCheck loadUnitsCategoryCheck() {
    return buildUnitsCategoryCheck("movesForecasts/loadUnits", mf -> mf.path("loadUnits"));
  }

  public static JsonContentCheck dischargeUnitsCategoryCheck() {
    return buildUnitsCategoryCheck(
        "movesForecasts/dischargeUnits", mf -> mf.path("dischargeUnits"));
  }

  private static JsonContentCheck buildUnitsCategoryCheck(
      String label, Function<JsonNode, JsonNode> extractor) {

    String description =
        "At least one event including '"
            + label
            + "' must have totalUnits or laden/empty/plugged/oog units";

    return JsonAttribute.customValidator(
        description,
        (body, ctx) -> {
          var events = body.path("events");
          if (!events.isArray() || events.isEmpty()) {
            return ConformanceCheckResult.simple(Set.of("events must be a non-empty array"));
          }

          boolean validFound = false;
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

              boolean ok =
                  !JsonUtil.isMissingOrEmpty(base.path("totalUnits"))
                      || !JsonUtil.isMissingOrEmpty(base.path("ladenUnits"))
                      || !JsonUtil.isMissingOrEmpty(base.path("emptyUnits"))
                      || !JsonUtil.isMissingOrEmpty(base.path("pluggedReeferUnits"))
                      || !JsonUtil.isMissingOrEmpty(base.path("outOfGaugeUnits"));

              if (ok) {
                validFound = true;
              } else {
                errors.add(
                    "events["
                        + e
                        + "].movesForecasts["
                        + m
                        + "]."
                        + label.substring("movesForecasts/".length())
                        + " must contain 'totalUnits' or at least one of "
                        + "'ladenUnits', 'emptyUnits', 'pluggedReeferUnits', 'outOfGaugeUnits'");
              }
            }
          }


          // Case 2: base appears but no valid examples
          if (!validFound) return ConformanceCheckResult.simple(errors);

          // Case 3: at least one valid example
          return ConformanceCheckResult.simple(Set.of());
        });
  }

  public static JsonContentCheck restowUnitsSizeCheck() {
    return buildUnitsSizeCheck("movesForecasts/restowUnits", mf -> mf.path("restowUnits"));
  }

  public static JsonContentCheck loadUnitsTotalUnitsSizeCheck() {
    return buildUnitsSizeCheck(
        "movesForecasts/loadUnits/totalUnits", mf -> mf.path("loadUnits").path("totalUnits"));
  }

  public static JsonContentCheck loadUnitsLadenUnitsSizeCheck() {
    return buildUnitsSizeCheck(
        "movesForecasts/loadUnits/ladenUnits", mf -> mf.path("loadUnits").path("ladenUnits"));
  }

  public static JsonContentCheck loadUnitsEmptyUnitsSizeCheck() {
    return buildUnitsSizeCheck(
        "movesForecasts/loadUnits/emptyUnits", mf -> mf.path("loadUnits").path("emptyUnits"));
  }

  public static JsonContentCheck loadUnitsPluggedReeferUnitsSizeCheck() {
    return buildUnitsSizeCheck(
        "movesForecasts/loadUnits/pluggedReeferUnits",
        mf -> mf.path("loadUnits").path("pluggedReeferUnits"));
  }

  public static JsonContentCheck loadUnitsOutOfGaugeUnitsSizeCheck() {
    return buildUnitsSizeCheck(
        "movesForecasts/loadUnits/outOfGaugeUnits",
        mf -> mf.path("loadUnits").path("outOfGaugeUnits"));
  }

  public static JsonContentCheck dischargeUnitsTotalUnitsSizeCheck() {
    return buildUnitsSizeCheck(
        "movesForecasts/dischargeUnits/totalUnits",
        mf -> mf.path("dischargeUnits").path("totalUnits"));
  }

  public static JsonContentCheck dischargeUnitsLadenUnitsSizeCheck() {
    return buildUnitsSizeCheck(
        "movesForecasts/dischargeUnits/ladenUnits",
        mf -> mf.path("dischargeUnits").path("ladenUnits"));
  }

  public static JsonContentCheck dischargeUnitsEmptyUnitsSizeCheck() {
    return buildUnitsSizeCheck(
        "movesForecasts/dischargeUnits/emptyUnits",
        mf -> mf.path("dischargeUnits").path("emptyUnits"));
  }

  public static JsonContentCheck dischargeUnitsPluggedReeferUnitsSizeCheck() {
    return buildUnitsSizeCheck(
        "movesForecasts/dischargeUnits/pluggedReeferUnits",
        mf -> mf.path("dischargeUnits").path("pluggedReeferUnits"));
  }

  public static JsonContentCheck dischargeUnitsOutOfGaugeUnitsSizeCheck() {
    return buildUnitsSizeCheck(
        "movesForecasts/dischargeUnits/outOfGaugeUnits",
        mf -> mf.path("dischargeUnits").path("outOfGaugeUnits"));
  }

  private static JsonContentCheck buildUnitsSizeCheck(
      String label, Function<JsonNode, JsonNode> extractor) {

    return JsonAttribute.customValidator(
        "At least one event including '" + label + "' must demonstrate correct size attributes",
        (body, ctx) -> {
          var events = body.path("events");

          if (!events.isArray() || events.isEmpty()) {
            return ConformanceCheckResult.simple(Set.of("events must be a non-empty array"));
          }

          boolean seenBase = false;
          boolean valid = false;
          Set<String> errors = new LinkedHashSet<>();

          for (int e = 0; e < events.size(); e++) {
            var mfArr = events.get(e).path("movesForecasts");
            if (!mfArr.isArray()) continue;

            for (int m = 0; m < mfArr.size(); m++) {
              JsonNode base = extractor.apply(mfArr.get(m));
              if (base.isMissingNode() || base.isNull()) {
                continue;
              }
              seenBase = true;

              String suffix = label.substring("movesForecasts/".length());
              String basePath = "events[" + e + "].movesForecasts[" + m + "]." + suffix;

              List<String> local = validateUnitsSizeBlock(base, basePath);
              if (local.isEmpty()) {
                valid = true;
                break;
              } else {
                errors.addAll(local);
              }
            }

            if (valid) return ConformanceCheckResult.simple(Set.of());
          }

          return ConformanceCheckResult.simple(errors);
        });
  }

  private static List<String> validateUnitsSizeBlock(JsonNode units, String basePath) {
    List<String> issues = new ArrayList<>();

    boolean hasTotal = units.path("totalUnits").isNumber();
    boolean has20 = units.path("size20Units").isNumber();
    boolean has40 = units.path("size40Units").isNumber();
    boolean has45 = units.path("size45Units").isNumber();

    if (!hasTotal && !has20 && !has40 && !has45) {
      issues.add(
          basePath
              + " must contain numeric totalUnits or size20Units or size40Units or size45Units");
    }

    return issues;
  }
}
