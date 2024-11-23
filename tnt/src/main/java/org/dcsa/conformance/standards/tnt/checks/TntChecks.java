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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonAttribute;
import org.dcsa.conformance.core.check.JsonContentCheck;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.party.SuppliedScenarioParameters;
import org.dcsa.conformance.standards.tnt.party.TntFilterParameter;
import org.dcsa.conformance.standards.tnt.party.TntRole;

@UtilityClass
public class TntChecks {
  private static final String SHIPMENT_EVENT_TYPE = "SHIPMENT";
  private static final String EQUIPMENT_EVENT_TYPE = "EQUIPMENT";
  private static final String TRANSPORT_EVENT_TYPE = "TRANSPORT";

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
            "If present, event attributes must match the corresponding query parameters",
            body -> {
              Map<TntFilterParameter, String> filterParametersMap = sspSupplier.get().getMap();
              Set<String> validationErrors = new LinkedHashSet<>();
              AtomicInteger eventIndex = new AtomicInteger(-1);
              TntSchemaConformanceCheck.findEventNodes(body)
                  .forEach(
                      eventNode -> {
                        eventIndex.incrementAndGet();
                        Stream.of(
                                TntFilterParameter.CARRIER_SERVICE_CODE,
                                TntFilterParameter.DOCUMENT_TYPE_CODE,
                                TntFilterParameter.EQUIPMENT_REFERENCE,
                                TntFilterParameter.EVENT_TYPE,
                                TntFilterParameter.EQUIPMENT_EVENT_TYPE_CODE,
                                TntFilterParameter.EXPORT_VOYAGE_NUMBER,
                                TntFilterParameter.SHIPMENT_EVENT_TYPE_CODE,
                                TntFilterParameter.TRANSPORT_CALL_ID,
                                TntFilterParameter.TRANSPORT_EVENT_TYPE_CODE,
                                TntFilterParameter.UN_LOCATION_CODE,
                                TntFilterParameter.VESSEL_IMO_NUMBER)
                            .filter(filterParametersMap::containsKey)
                            .forEach(
                                filterParameter -> {
                                  Set<String> parameterValues =
                                      Arrays.stream(
                                              filterParametersMap.get(filterParameter).split(","))
                                          .collect(Collectors.toSet());
                                  Set<String> attributeValues =
                                      filterParameter.getEventPaths().stream()
                                          .map(eventPath -> eventNode.at("/" + eventPath))
                                          .filter(Predicate.not(JsonNode::isMissingNode))
                                          .map(JsonNode::asText)
                                          .collect(Collectors.toSet());
                                  if (!attributeValues.isEmpty()
                                      && parameterValues.stream()
                                          .noneMatch(attributeValues::contains)) {
                                    validationErrors.add(
                                        "Event #%d: Value '%s' at path '%s' does not match value '%s' of query parameter '%s'"
                                            .formatted(
                                                eventIndex.get(),
                                                String.join(",", attributeValues),
                                                String.join(",", filterParameter.getEventPaths()),
                                                filterParametersMap.get(filterParameter),
                                                filterParameter.getQueryParamName()));
                                  }
                                });
                      });
              return validationErrors;
            }));

    // FIXME
    checks.add(
        JsonAttribute.customValidator(
            "Validate limit parameter is met",
            body -> {
              Set<String> issues = new LinkedHashSet<>();
              ArrayList<JsonNode> eventNodes = TntSchemaConformanceCheck.findEventNodes(body);

              Optional<Map.Entry<TntFilterParameter, String>> limitParam =
                  sspSupplier.get().getMap().entrySet().stream()
                      .filter(e -> e.getKey().equals(TntFilterParameter.LIMIT))
                      .findFirst();

              if (limitParam.isPresent()) {
                int expectedLimit = Integer.parseInt(limitParam.get().getValue().trim());
                if (eventNodes.size() > expectedLimit) {
                  issues.add("The number of events exceeds the limit parameter: " + expectedLimit);
                }
              }
              return issues;
            }));

    // FIXME
    checks.add(
        JsonAttribute.customValidator(
            "Validate carrierBookingReference exists and matches in JSON response "
                + "if request parameter has carrierBookingReference",
            body ->
                validateBookingDocumentReference(
                    body, sspSupplier, "CBR", TntFilterParameter.CARRIER_BOOKING_REFERENCE)));

    // FIXME
    checks.add(
        JsonAttribute.customValidator(
            "Validate transportDocumentReference exists and matches in JSON response "
                + "if request parameter has transportDocumentReference",
            body ->
                validateBookingDocumentReference(
                    body, sspSupplier, "TRD", TntFilterParameter.TRANSPORT_DOCUMENT_REFERENCE)));
    return JsonAttribute.contentChecks(
        TntRole::isPublisher, matched, HttpMessageType.RESPONSE, standardVersion, checks);
  }

  private Set<String> validateBookingDocumentReference(
      JsonNode body,
      Supplier<SuppliedScenarioParameters> sspSupplier,
      @NonNull String filterType,
      TntFilterParameter parameter) {
    Optional<Map.Entry<TntFilterParameter, String>> referenceParam =
        sspSupplier.get().getMap().entrySet().stream()
            .filter(e -> e.getKey().equals(parameter))
            .findFirst();

    Set<String> issues = new LinkedHashSet<>();
    ArrayList<JsonNode> eventNodes = TntSchemaConformanceCheck.findEventNodes(body);

    if (referenceParam.isPresent()) {
      Set<String> expectedCarrierBookingReferences =
          Arrays.stream(referenceParam.get().getValue().split(","))
              .map(String::trim)
              .collect(Collectors.toSet());

      Set<String> errors =
          Stream.concat(
                  filterNodesByEventType(eventNodes, TRANSPORT_EVENT_TYPE),
                  filterNodesByEventType(eventNodes, EQUIPMENT_EVENT_TYPE))
              .filter(
                  node -> {
                    JsonNode documentReferencesNode = node.path("documentReferences");
                    return StreamSupport.stream(documentReferencesNode.spliterator(), false)
                        .anyMatch(
                            refNode ->
                                !(isEmptyNode(refNode)
                                        && isEmptyNode(refNode.path("documentReferenceType"))
                                        && isEmptyNode(refNode.path("documentReferenceValue")))
                                    && refNode
                                        .path("documentReferenceType")
                                        .asText()
                                        .equals(filterType)
                                    && !(expectedCarrierBookingReferences.contains(
                                        refNode.path("documentReferenceValue").asText())));
                  })
              .map(
                  node ->
                      "Missing or mismatched "
                          + parameter.name()
                          + " for eventType "
                          + node.path("eventType").asText())
              .collect(Collectors.toSet());

      errors.addAll(
          filterNodesByEventType(eventNodes, SHIPMENT_EVENT_TYPE)
              .filter(
                  node -> {
                    JsonNode documentTypeCodeNode = node.path("documentTypeCode");
                    JsonNode documentIDNode = node.path("documentID");
                    return documentTypeCodeNode.asText().equals(filterType)
                        && !expectedCarrierBookingReferences.contains(documentIDNode.asText());
                  })
              .map(
                  node ->
                      "Missing or mismatched " + parameter.name() + "for eventType SHIPMENT at ")
              .collect(Collectors.toSet()));

      issues.addAll(errors);
      return issues;
    }

    return Set.of();
  }

  private static OffsetDateTime _stringToOffsetDateTime(String dateTimeString) {
    try {
      return OffsetDateTime.parse(dateTimeString);
    } catch (DateTimeParseException e) {
      return null;
    }
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

  private Stream<JsonNode> filterNodesByEventType(ArrayList<JsonNode> body, String eventType) {
    return body.stream().filter(node -> node.path("eventType").asText().equals(eventType));
  }

  private boolean isEmptyNode(JsonNode node) {
    return node.isMissingNode() || node.isNull();
  }
}
