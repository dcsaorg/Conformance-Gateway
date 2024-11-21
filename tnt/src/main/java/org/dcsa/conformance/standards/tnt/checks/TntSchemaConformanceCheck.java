package org.dcsa.conformance.standards.tnt.checks;

import com.fasterxml.jackson.databind.JsonNode;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.action.TntEventType;
import org.dcsa.conformance.standards.tnt.party.TntHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public class TntSchemaConformanceCheck extends ActionCheck {

  private final Map<TntEventType, JsonSchemaValidator> eventSchemaValidators;

  public TntSchemaConformanceCheck(
      Predicate<String> isRelevantForRoleName,
      UUID matchedExchangeUuid,
      HttpMessageType httpMessageType,
      Map<TntEventType, JsonSchemaValidator> eventSchemaValidators) {
    super(
        "",
        "The HTTP %s matches the standard JSON schema"
            .formatted(HttpMessageType.RESPONSE.name().toLowerCase()),
        isRelevantForRoleName,
        matchedExchangeUuid,
        httpMessageType);
    this.eventSchemaValidators = eventSchemaValidators;
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) {
      return Set.of();
    }
    JsonNode jsonResponse = exchange.getMessage(httpMessageType).body().getJsonBody();
    Set<String> issues = new LinkedHashSet<>();

    if (!jsonResponse.isArray()) {
      issues.add("The root JSON response must be an array of events");
    }

    ArrayList<JsonNode> eventNodes = TntHelper.findEventNodes(jsonResponse, issues);

    eventNodes.stream()
        .forEachOrdered(
            eventNode -> {
              JsonNode eventTypeNode = eventNode.path("eventType");
              String eventTypeText = eventTypeNode.asText().toUpperCase();
              TntEventType eventType;
              try {
                eventType = TntEventType.valueOf(eventTypeText);
              } catch (RuntimeException e) {
                issues.add(
                    "Event #%d: incorrect eventType attribute: %s"
                        .formatted(eventNodes.indexOf(eventNode), eventTypeNode));
                return;
              }

              JsonSchemaValidator eventSchemaValidator = eventSchemaValidators.get(eventType);
              eventSchemaValidator
                  .validate(eventNode)
                  .forEach(
                      validationError ->
                          issues.add(
                              "Event #%d: %s"
                                  .formatted(eventNodes.indexOf(eventNode), validationError)));
            });
    return issues;
  }
}
