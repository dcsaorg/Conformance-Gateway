package org.dcsa.conformance.standards.tnt.action;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.party.TntRole;

@Getter
@Slf4j
public class TntGetEventsAction extends TntAction {
  private final Map<TntEventType, JsonSchemaValidator> eventSchemaValidators;

  public TntGetEventsAction(
      String subscriberPartyName,
      String publisherPartyName,
      TntAction previousAction,
      Map<TntEventType, JsonSchemaValidator> eventSchemaValidators) {
    super(subscriberPartyName, publisherPartyName, previousAction, "GetEvents", 200);
    this.eventSchemaValidators = eventSchemaValidators;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a GET events request";
  }

  @Override
  public ConformanceCheck createCheck(String expectedApiVersion) {
    return new ConformanceCheck(getActionTitle()) {
      @Override
      protected Stream<? extends ConformanceCheck> createSubChecks() {
        return Stream.of(
            new UrlPathCheck(TntRole::isSubscriber, getMatchedExchangeUuid(), "/events"),
            new ResponseStatusCheck(TntRole::isPublisher, getMatchedExchangeUuid(), expectedStatus),
            new ActionCheck(
                "The HTTP %s matches the standard JSON schema"
                    .formatted(HttpMessageType.RESPONSE.name().toLowerCase()),
                TntRole::isPublisher,
                getMatchedExchangeUuid(),
                HttpMessageType.RESPONSE) {
              @Override
              protected Set<String> checkConformance(ConformanceExchange exchange) {
                JsonNode jsonResponse = exchange.getMessage(httpMessageType).body().getJsonBody();
                LinkedHashSet<String> validationErrors = new LinkedHashSet<>();
                if (!jsonResponse.isArray()) {
                  validationErrors.add("The root JSON response must be an array of events");
                  return validationErrors;
                }
                ArrayNode arrayResponse = (ArrayNode) jsonResponse;
                int eventCount = arrayResponse.size();
                for (int eventIndex = 0; eventIndex < eventCount; ++eventIndex) {
                  JsonNode eventNode = arrayResponse.get(eventIndex);
                  JsonNode eventTypeNode = eventNode.path("eventType");
                  if (eventTypeNode.isMissingNode()) {
                    validationErrors.add(
                        "Event #%d: missing eventType attribute".formatted(eventIndex));
                    continue;
                  }
                  TntEventType eventType;
                  try {
                    eventType = TntEventType.valueOf(eventTypeNode.asText());
                  } catch (RuntimeException e) {
                    validationErrors.add(
                        "Event #%d: incorrect eventType attribute: %s"
                            .formatted(eventIndex, eventTypeNode));
                    continue;
                  }
                  JsonSchemaValidator eventSchemaValidator = eventSchemaValidators.get(eventType);
                  for (String validationError : eventSchemaValidator.validate(eventNode)) {
                    validationErrors.add("Event #%d: %s".formatted(eventIndex, validationError));
                  }
                }
                return validationErrors;
              }
            });
      }
    };
  }
}
