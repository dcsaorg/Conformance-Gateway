package org.dcsa.conformance.standards.tnt.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.action.TntEventType;
import org.dcsa.conformance.standards.tnt.party.SuppliedScenarioParameters;
import org.dcsa.conformance.standards.tnt.party.TntFilterParameter;
import org.dcsa.conformance.standards.tnt.party.TntRole;

@UtilityClass
public class TntChecks {

  public static ActionCheck responseContentChecks(
      UUID matched, String standardVersion, Supplier<SuppliedScenarioParameters> sspSupplier) {
    var checks = new ArrayList<JsonContentCheck>();

    checks.add(
        JsonAttribute.customValidator(
            "Every response received during a conformance test must contain events",
            body ->
                TntSchemaConformanceCheck.findEventNodes(body).isEmpty()
                    ? Set.of("No events found in response")
                    : Set.of()));

    checks.add(
        JsonAttribute.customValidator(
            "Each event must have a unique eventID",
            body -> {
              LinkedHashSet<String> validationErrors = new LinkedHashSet<>();
              HashSet<Object> uniqueEventIds = new HashSet<>();
              TntSchemaConformanceCheck.findEventNodes(body).stream()
                  .filter(node -> node.has("eventID"))
                  .map(node -> node.path("eventID").asText())
                  .forEach(
                      eventId -> {
                        if (!uniqueEventIds.add(eventId)) {
                          validationErrors.add("Duplicate eventId: '%s'".formatted(eventId));
                        }
                      });
              return validationErrors;
            }));

    checks.add(
        JsonAttribute.customValidator(
            "If present, at least event attribute must match the corresponding query parameters",
            body -> {
              Map<TntFilterParameter, String> filterParametersMap = sspSupplier.get().getMap();
              Set<String> validationErrors = new LinkedHashSet<>();
              AtomicInteger eventIndex = new AtomicInteger(-1);
              TntSchemaConformanceCheck.findEventNodes(body)
                  .forEach(
                      eventNode -> {
                        eventIndex.incrementAndGet();
                        _checkThatEventValuesMatchParamValues(eventNode, filterParametersMap)
                            .forEach(
                                validationError ->
                                    validationErrors.add(
                                        "Event #%d: %s"
                                            .formatted(eventIndex.get(), validationError)));
                      });
              return validationErrors;
            }));

    checks.add(
        JsonAttribute.customValidator(
            "The eventCreatedDateTime of each event matches the date-time filter parameters",
            body -> {
              List<Map.Entry<TntFilterParameter, String>> eventCreatedDateTimeParams =
                  sspSupplier.get().getMap().entrySet().stream()
                      .filter(
                          entry ->
                              entry
                                  .getKey()
                                  .getQueryParamName()
                                  .startsWith(
                                      TntFilterParameter.EVENT_CREATED_DATE_TIME
                                          .getQueryParamName()))
                      .toList();
              if (eventCreatedDateTimeParams.isEmpty()) {
                return Set.of();
              }
              Set<String> validationErrors = new LinkedHashSet<>();
              AtomicInteger eventIndex = new AtomicInteger(-1);
              TntSchemaConformanceCheck.findEventNodes(body).stream()
                  .map(
                      eventNode ->
                          _stringToOffsetDateTime(eventNode.path("eventCreatedDateTime").asText()))
                  .filter(Objects::nonNull)
                  .forEach(
                      eventCreatedDateTime -> {
                        eventIndex.incrementAndGet();
                        eventCreatedDateTimeParams.forEach(
                            entry -> {
                              OffsetDateTime filterParamDateTime =
                                  _stringToOffsetDateTime(entry.getValue());
                              if (filterParamDateTime != null) {
                                String validationError =
                                    _validateEventCreatedDateTime(
                                      eventCreatedDateTime, entry.getKey(), filterParamDateTime);
                                if (validationError != null) {
                                  validationErrors.add(
                                      "Event #%d: %s".formatted(eventIndex.get(), validationError));
                                }
                              }
                            });
                      });
              return validationErrors;
            }));

    checks.add(
        JsonAttribute.customValidator(
            "The number of events must not exceed the 'limit' query parameter",
            body -> {
              String limitQueryParameter = sspSupplier.get().getMap().get(TntFilterParameter.LIMIT);
              if (limitQueryParameter == null) return Set.of();

              int maxEventCount = Integer.parseInt(limitQueryParameter);
              int eventCount = TntSchemaConformanceCheck.findEventNodes(body).size();
              if (eventCount > maxEventCount) {
                return Set.of(
                    "The number of events (%d) exceeds the value of the 'limit' query parameter (%d)"
                        .formatted(eventCount, maxEventCount));
              }
              return Set.of();
            }));

    return JsonAttribute.contentChecks(
        TntRole::isPublisher, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  private static OffsetDateTime _stringToOffsetDateTime(String dateTimeString) {
    try {
      return OffsetDateTime.parse(dateTimeString);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  static TntEventType _getEventType(JsonNode eventNode) {
    return Arrays.stream(TntEventType.values())
        .filter(
            eventType -> eventType.name().equalsIgnoreCase(eventNode.path("eventType").asText()))
        .findFirst()
        .orElse(null);
  }

  static Set<String> _checkThatEventValuesMatchParamValues(
      JsonNode eventNode, Map<TntFilterParameter, String> filterParametersMap) {
    TntEventType eventType = _getEventType(eventNode);
    if (eventType == null) return Set.of();
    Set<String> validationErrors = new LinkedHashSet<>();
    Arrays.stream(TntFilterParameter.values())
        .filter(param -> !param.getJsonPathsByEventType().isEmpty())
        .filter(filterParametersMap::containsKey)
        .forEach(
            filterParameter -> {
              Set<String> parameterValues =
                  Arrays.stream(filterParametersMap.get(filterParameter).split(","))
                      .collect(Collectors.toSet());
              Set<String> attributeValues = new HashSet<>();
              Set<String> eventTypeJsonPaths =
                  filterParameter.getJsonPathsByEventType().get(eventType);
              eventTypeJsonPaths.forEach(
                  jsonPathExpression -> {
                    if (jsonPathExpression.contains(" WHERE ")) {
                      // "/some/path WHERE /where/clause"
                      String[] jsonPathExpressionTokens = jsonPathExpression.split(" WHERE ");
                      String jsonPath = jsonPathExpressionTokens[0];
                      String whereClause = jsonPathExpressionTokens[1];
                      // "/documentReferenceType=BKG"
                      String[] whereClauseTokens = whereClause.split("=");
                      String whereClauseJsonPath = whereClauseTokens[0];
                      String whereClauseValue = whereClauseTokens[1];
                      if (jsonPath.contains("/*/")) {
                        // "/documentReferences/*/documentReferenceValue"
                        String[] jsonPathTokens = jsonPath.split("/\\*");
                        String jsonPathToArray = jsonPathTokens[0];
                        String jsonPathInElement = jsonPathTokens[1];
                        JsonNode arrayNode = eventNode.at(jsonPathToArray);
                        if (arrayNode.isArray()) {
                          StreamSupport.stream(arrayNode.spliterator(), false)
                              .filter(
                                  elementNode ->
                                      whereClauseValue.equals(
                                          elementNode.at(whereClauseJsonPath).textValue()))
                              .forEach(
                                  elementNode -> {
                                    JsonNode attributeValueNode = elementNode.at(jsonPathInElement);
                                    if (attributeValueNode.isTextual()) {
                                      attributeValues.add(attributeValueNode.textValue());
                                    }
                                  });
                        }
                      } else {
                        // "/documentID"
                        if (whereClauseValue.equals(
                            eventNode.at(whereClauseJsonPath).textValue())) {
                          JsonNode attributeValueNode = eventNode.at(jsonPath);
                          if (attributeValueNode.isTextual()) {
                            attributeValues.add(attributeValueNode.textValue());
                          }
                        }
                      }
                    } else {
                      // "/transportCall/transportCallID"
                      JsonNode attributeNode = eventNode.at(jsonPathExpression);
                      if (attributeNode.isTextual()) {
                        attributeValues.add(attributeNode.textValue());
                      }
                    }
                  });
              if (!attributeValues.isEmpty()
                  && parameterValues.stream().noneMatch(attributeValues::contains)) {
                validationErrors.add(
                    "Value%s '%s' at path%s '%s' do%s not match value%s '%s' of query parameter '%s'"
                        .formatted(
                            attributeValues.size() > 1 ? "s" : "",
                            String.join(", ", attributeValues),
                            eventTypeJsonPaths.size() > 1 ? "s" : "",
                            String.join(", ", eventTypeJsonPaths),
                            attributeValues.size() > 1 ? "" : "es",
                            filterParametersMap.get(filterParameter).contains(",") ? "s" : "",
                            String.join(", ", filterParametersMap.get(filterParameter).split(",")),
                            filterParameter.getQueryParamName()));
              }
            });
    return validationErrors;
  }

  static String _validateEventCreatedDateTime(OffsetDateTime eventCreatedDateTime, TntFilterParameter parameterKey, OffsetDateTime parameterValue) {
    switch (parameterKey) {
      case EVENT_CREATED_DATE_TIME:
      case EVENT_CREATED_DATE_TIME_EQ:
        if (!eventCreatedDateTime.isEqual(parameterValue))
          return "eventCreatedDateTime '%s' does not equal filter parameter date time '%s'"
              .formatted(eventCreatedDateTime, parameterValue);
        break;

      case EVENT_CREATED_DATE_TIME_LT:
        if (!eventCreatedDateTime.isBefore(parameterValue))
          return "eventCreatedDateTime '%s' is not before filter parameter date time '%s'"
              .formatted(eventCreatedDateTime, parameterValue);
        break;

      case EVENT_CREATED_DATE_TIME_LTE:
        if (!(eventCreatedDateTime.isBefore(parameterValue)
            || eventCreatedDateTime.isEqual(parameterValue)))
          return "eventCreatedDateTime '%s' is not before or equal to filter parameter date time '%s'"
              .formatted(eventCreatedDateTime, parameterValue);
        break;

      case EVENT_CREATED_DATE_TIME_GT:
        if (!eventCreatedDateTime.isAfter(parameterValue))
          return "eventCreatedDateTime '%s' is not after filter parameter date time '%s'"
              .formatted(eventCreatedDateTime, parameterValue);
        break;

      case EVENT_CREATED_DATE_TIME_GTE:
        if (!(eventCreatedDateTime.isAfter(parameterValue)
            || eventCreatedDateTime.isEqual(parameterValue)))
          return "eventCreatedDateTime '%s' is not after or equal to filter parameter date time '%s'"
              .formatted(eventCreatedDateTime, parameterValue);
        break;
    }
    return null;
  }
}
