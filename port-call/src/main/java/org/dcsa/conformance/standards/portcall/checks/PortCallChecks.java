package org.dcsa.conformance.standards.portcall.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;
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
    checks.add(PortCallChecks.nonEmptyEvents());

    if ("TIMESTAMP".equals(scenarioType)) {
      checks.addAll(PortCallChecks.timestampScenarioChecks());
    }
    if ("MOVES_FORECASTS".equals(scenarioType)) {
      checks.addAll(PortCallChecks.movesForecastsScenarioChecks());
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

  public static JsonContentCheck atLeastOneTimestampClassifierCodeCorrect() {
    return JsonAttribute.customValidator(
        "At least one event must demonstrate the correct use of the 'timestamp/classifierCode' attribute",
        (body, ctx) -> {
          var events = body.path("events");
          Set<String> allIssues = new LinkedHashSet<>();

          if (!events.isArray() || events.isEmpty()) {
            allIssues.add("events must be a non-empty array");
            return ConformanceCheckResult.simple(allIssues);
          }

          for (int i = 0; i < events.size(); i++) {
            var event = events.get(i);
            List<String> errorsAtIndex = validateTimestampClassifierCode(event);

            if (errorsAtIndex.isEmpty()) {
              return ConformanceCheckResult.simple(Set.of());
            }
            for (String err : errorsAtIndex) {
              allIssues.add("events[" + i + "]." + err);
            }
          }
          return ConformanceCheckResult.simple(allIssues);
        });
  }

  private static List<String> validateTimestampClassifierCode(JsonNode event) {
    List<String> issues = new ArrayList<>();

    var ts = event.path("timestamp");
    if (JsonUtil.isMissingOrEmpty(ts)) {
      issues.add("timestamp.classifierCode must be functionally present and non-empty");
      return issues;
    }

    String classifier = ts.path("classifierCode").asText("");
    if (classifier.isBlank()) {
      issues.add("timestamp.classifierCode must be functionally present and non-empty");
    }

    return issues;
  }

  public static JsonContentCheck atLeastOneTimestampServiceDateTimeCorrect() {
    return JsonAttribute.customValidator(
        "At least one event must demonstrate the correct use of the 'timestamp/serviceDateTime' attribute",
        (body, ctx) -> {
          var events = body.path("events");
          Set<String> allIssues = new LinkedHashSet<>();

          if (!events.isArray() || events.isEmpty()) {
            allIssues.add("events must be a non-empty array");
            return ConformanceCheckResult.simple(allIssues);
          }

          for (int i = 0; i < events.size(); i++) {
            var event = events.get(i);
            List<String> errorsAtIndex = validateTimestampServiceDateTime(event);

            if (errorsAtIndex.isEmpty()) {
              return ConformanceCheckResult.simple(Set.of());
            }
            for (String err : errorsAtIndex) {
              allIssues.add("events[" + i + "]." + err);
            }
          }
          return ConformanceCheckResult.simple(allIssues);
        });
  }

  private static List<String> validateTimestampServiceDateTime(JsonNode event) {
    List<String> issues = new ArrayList<>();

    var ts = event.path("timestamp");
    if (JsonUtil.isMissingOrEmpty(ts)) {
      issues.add("timestamp.serviceDateTime must be functionally present and non-empty");
      return issues;
    }

    String svc = ts.path("serviceDateTime").asText("");
    if (svc.isBlank()) {
      issues.add("timestamp.serviceDateTime must be functionally present and non-empty");
    }

    return issues;
  }

  public static JsonContentCheck atLeastOneMovesForecastsUnitsBlockPresent() {
    return JsonAttribute.customValidator(
        "At least one event must demonstrate the correct use within the 'movesForecasts' object of at least one of the 'restowUnits', 'loadUnits' or 'dischargeUnits' sub-objects",
        (body, ctx) -> {
          var events = body.path("events");
          Set<String> allIssues = new LinkedHashSet<>();

          if (!events.isArray() || events.isEmpty()) {
            allIssues.add("events must be a non-empty array");
            return ConformanceCheckResult.simple(allIssues);
          }

          for (int i = 0; i < events.size(); i++) {
            var event = events.get(i);
            List<String> errorsAtIndex = validateMovesForecastsTopLevel(event);

            if (errorsAtIndex.isEmpty()) {
              return ConformanceCheckResult.simple(Set.of());
            }
            for (String err : errorsAtIndex) {
              allIssues.add("events[" + i + "]." + err);
            }
          }

          return ConformanceCheckResult.simple(allIssues);
        });
  }

  private static List<String> validateMovesForecastsTopLevel(JsonNode event) {
    List<String> issues = new ArrayList<>();

    var mfarray = event.path("movesForecasts");
    if (!mfarray.isArray() || mfarray.isEmpty()) {
      issues.add("movesForecasts must be a non-empty array");
      return issues;
    }

    for (int i = 0; i < mfarray.size(); i++) {
      var movesForecast = mfarray.get(i);
      boolean hasRestow = !JsonUtil.isMissingOrEmpty(movesForecast.path("restowUnits"));
      boolean hasLoad = !JsonUtil.isMissingOrEmpty(movesForecast.path("loadUnits"));
      boolean hasDischarge = !JsonUtil.isMissingOrEmpty(movesForecast.path("dischargeUnits"));

      if (hasRestow || hasLoad || hasDischarge) {
        return List.of();
      }

    }
      issues.add(
          "movesForecasts must contain at least one of 'restowUnits', 'loadUnits', or 'dischargeUnits'");

    return issues;
  }

  public static JsonContentCheck atLeastOneLoadUnitsCategoryBlockCorrect() {
    return buildUnitsCategoryCheck("movesForecasts/loadUnits");
  }

  public static JsonContentCheck atLeastOneDischargeUnitsCategoryBlockCorrect() {
    return buildUnitsCategoryCheck("movesForecasts/dischargeUnits");
  }

  private static JsonContentCheck buildUnitsCategoryCheck(String baseObjectPath) {
    String description =
        "At least one event including the '"
            + baseObjectPath
            + "' object must demonstrate the correct use within it of the 'totalUnits' sub-object or of at least one of the 'ladenUnits', 'emptyUnits', 'pluggedReeferUnits' or 'outOfGaugeUnits' sub-object";

    return JsonAttribute.customValidator(
        description,
        (body, ctx) -> {
          var events = body.path("events");
          Set<String> allIssues = new LinkedHashSet<>();

          if (!events.isArray() || events.isEmpty()) {
            allIssues.add("events must be a non-empty array");
            return ConformanceCheckResult.simple(allIssues);
          }

          boolean foundAnyBaseObject = false;

          int ei = 0;
          for (var event : events) {
            var mfArr = event.path("movesForecasts");
            if (!mfArr.isArray() || mfArr.isEmpty()) {
              ei++;
              continue;
            }

            int mi = 0;
            for (var mf : mfArr) {
              JsonNode base = resolveMovesForecastsBase(mf, baseObjectPath);
              if (JsonUtil.isMissingOrEmpty(base)) {
                mi++;
                continue;
              }

              foundAnyBaseObject = true;
              String basePathWithIndex =
                  "events[" + ei + "].movesForecasts[" + mi + "]." + tailOf(baseObjectPath);

              List<String> local = validateUnitsCategoryBlock(base, basePathWithIndex);

              if (local.isEmpty()) {
                return ConformanceCheckResult.simple(Set.of());
              }

              local.forEach(allIssues::add);
              mi++;
            }

            ei++;
          }

          if (!foundAnyBaseObject) {
            return ConformanceCheckResult.simple(Set.of());
          }

          return ConformanceCheckResult.simple(allIssues);
        });
  }

  private static List<String> validateUnitsCategoryBlock(JsonNode base, String basePath) {
    List<String> issues = new ArrayList<>();

    boolean hasTotal = !JsonUtil.isMissingOrEmpty(base.path("totalUnits"));
    boolean hasLaden = !JsonUtil.isMissingOrEmpty(base.path("ladenUnits"));
    boolean hasEmpty = !JsonUtil.isMissingOrEmpty(base.path("emptyUnits"));
    boolean hasPlugged = !JsonUtil.isMissingOrEmpty(base.path("pluggedReeferUnits"));
    boolean hasOog = !JsonUtil.isMissingOrEmpty(base.path("outOfGaugeUnits"));

    if (!hasTotal && !hasLaden && !hasEmpty && !hasPlugged && !hasOog) {
      issues.add(
          basePath
              + " must contain 'totalUnits' or at least one of 'ladenUnits', 'emptyUnits', 'pluggedReeferUnits' or 'outOfGaugeUnits'");
    }

    return issues;
  }

  public static List<JsonContentCheck> moveForecastsUnitSizeChecks() {
    List<JsonContentCheck> checks = new ArrayList<>();

    checks.add(buildUnitsSizeCheck("movesForecasts/restowUnits"));
    checks.add(buildUnitsSizeCheck("movesForecasts/loadUnits/totalUnits"));
    checks.add(buildUnitsSizeCheck("movesForecasts/loadUnits/ladenUnits"));
    checks.add(buildUnitsSizeCheck("movesForecasts/loadUnits/emptyUnits"));
    checks.add(buildUnitsSizeCheck("movesForecasts/loadUnits/pluggedReeferUnits"));
    checks.add(buildUnitsSizeCheck("movesForecasts/loadUnits/outOfGaugeUnits"));
    checks.add(buildUnitsSizeCheck("movesForecasts/dischargeUnits/totalUnits"));
    checks.add(buildUnitsSizeCheck("movesForecasts/dischargeUnits/ladenUnits"));
    checks.add(buildUnitsSizeCheck("movesForecasts/dischargeUnits/emptyUnits"));
    checks.add(buildUnitsSizeCheck("movesForecasts/dischargeUnits/pluggedReeferUnits"));
    checks.add(buildUnitsSizeCheck("movesForecasts/dischargeUnits/outOfGaugeUnits"));

    return checks;
  }

  private static JsonContentCheck buildUnitsSizeCheck(String baseObjectPath) {
    String description =
        "At least one event including the '"
            + baseObjectPath
            + "' object must demonstrate the correct use within it of the 'totalUnits' attribute or of at least one of the 'size20Units', 'size40Units' or 'size45Units' attribute";

    return JsonAttribute.customValidator(
        description,
        (body, ctx) -> {
          var events = body.path("events");
          Set<String> allIssues = new LinkedHashSet<>();

          if (!events.isArray() || events.isEmpty()) {
            allIssues.add("events must be a non-empty array");
            return ConformanceCheckResult.simple(allIssues);
          }

          boolean foundAnyBaseObject = false;

          int ei = 0;
          for (var event : events) {
            var mfArr = event.path("movesForecasts");
            if (!mfArr.isArray() || mfArr.isEmpty()) {
              ei++;
              continue;
            }

            int mi = 0;
            for (var mf : mfArr) {
              JsonNode base = resolveMovesForecastsBase(mf, baseObjectPath);
              if (JsonUtil.isMissingOrEmpty(base)) {
                mi++;
                continue;
              }

              foundAnyBaseObject = true;
              String basePathWithIndex =
                  "events[" + ei + "].movesForecasts[" + mi + "]." + tailOf(baseObjectPath);

              List<String> local = validateUnitsSizeBlock(base, basePathWithIndex);

              if (local.isEmpty()) {
                return ConformanceCheckResult.simple(Set.of());
              }

              local.forEach(allIssues::add);
              mi++;
            }

            ei++;
          }

          if (!foundAnyBaseObject) {
            return ConformanceCheckResult.simple(Set.of());
          }

          return ConformanceCheckResult.simple(allIssues);
        });
  }

  private static List<String> validateUnitsSizeBlock(JsonNode units, String basePath) {
    List<String> issues = new ArrayList<>();

    boolean hasValidTotal = false;
    boolean hasValid20 = false;
    boolean hasValid40 = false;
    boolean hasValid45 = false;

    var total = units.path("totalUnits");
    if (total.isNumber()) {
      hasValidTotal = true;
    } else if (!total.isMissingNode() && !total.isNull()) {
      issues.add(basePath + ".totalUnits must be a number if present");
    }

    var s20 = units.path("size20Units");
    if (s20.isNumber()) {
      hasValid20 = true;
    } else if (!s20.isMissingNode() && !s20.isNull()) {
      issues.add(basePath + ".size20Units must be a number if present");
    }

    var s40 = units.path("size40Units");
    if (s40.isNumber()) {
      hasValid40 = true;
    } else if (!s40.isMissingNode() && !s40.isNull()) {
      issues.add(basePath + ".size40Units must be a number if present");
    }

    var s45 = units.path("size45Units");
    if (s45.isNumber()) {
      hasValid45 = true;
    } else if (!s45.isMissingNode() && !s45.isNull()) {
      issues.add(basePath + ".size45Units must be a number if present");
    }

    if (!hasValidTotal && !hasValid20 && !hasValid40 && !hasValid45) {
      issues.add(
          basePath
              + " must contain a numeric 'totalUnits' or at least one numeric 'size20Units', 'size40Units' or 'size45Units'");
    }

    return issues;
  }

  private static JsonNode resolveMovesForecastsBase(JsonNode mf, String baseObjectPath) {
    String[] parts = baseObjectPath.split("/");
    int start = 0;
    if ("movesForecasts".equals(parts[0])) {
      start = 1;
    }

    JsonNode current = mf;
    for (int i = start; i < parts.length; i++) {
      current = current.path(parts[i]);
    }
    return current;
  }

  private static String tailOf(String baseObjectPath) {
    if (!baseObjectPath.startsWith("movesForecasts/")) return baseObjectPath;
    return baseObjectPath.substring("movesForecasts/".length());
  }

  public static List<JsonContentCheck> timestampScenarioChecks() {
    List<JsonContentCheck> checks = new ArrayList<>();
    checks.add(nonEmptyEvents());
    checks.add(atLeastOneTimestampClassifierCodeCorrect());
    checks.add(atLeastOneTimestampServiceDateTimeCorrect());
    return checks;
  }

  public static List<JsonContentCheck> movesForecastsScenarioChecks() {
    List<JsonContentCheck> checks = new ArrayList<>();
    checks.add(nonEmptyEvents());
    checks.add(atLeastOneMovesForecastsUnitsBlockPresent());
    checks.add(atLeastOneLoadUnitsCategoryBlockCorrect());
    checks.add(atLeastOneDischargeUnitsCategoryBlockCorrect());
    checks.addAll(moveForecastsUnitSizeChecks());
    return checks;
  }

  private static final JsonContentCheck VALIDATE_NON_EMPTY_RESPONSE =
      JsonAttribute.customValidator(
          "At least one event must be included in a message sent to the sandbox during conformance testing",
          body -> {
            var eventsNode = body.get("events");

            if (eventsNode == null || !eventsNode.isArray() || eventsNode.isEmpty()) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "'events' must functionally contain at least one event when sending data to the sandbox"));
            }

            boolean hasNonEmptyEvent =
                StreamSupport.stream(eventsNode.spliterator(), false)
                    .filter(JsonNode::isObject)
                    .anyMatch(
                        eventObj ->
                            eventObj.fieldNames().hasNext()
                                && StreamSupport.stream(
                                        Spliterators.spliteratorUnknownSize(
                                            eventObj.fieldNames(), 0),
                                        false)
                                    .map(eventObj::get)
                                    .anyMatch(
                                        field ->
                                            (field.isValueNode() && !field.asText().isBlank())
                                                || (field.isContainerNode() && !field.isEmpty())));

            if (!hasNonEmptyEvent) {
              return ConformanceCheckResult.simple(
                  Set.of(
                      "At least one non empty event must be included in a message sent to the sandbox during conformance testing"));
            }
            return ConformanceCheckResult.simple(Set.of());
          });

  public static ActionCheck getGetResponsePayloadChecks(
      UUID matchedExchangeUuid,
      String expectedApiVersion,
      Supplier<DynamicScenarioParameters> dsp) {
    var checks = new ArrayList<JsonContentCheck>();
    checks.add(VALIDATE_NON_EMPTY_RESPONSE);
    return JsonAttribute.contentChecks(
      "",
      "The Publisher has correctly demonstrated the use of functionally required attributes in the payload",
      PortCallRole::isPublisher,
      matchedExchangeUuid,
      HttpMessageType.RESPONSE,
      expectedApiVersion,
      checks);
  }

}
