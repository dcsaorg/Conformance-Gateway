package org.dcsa.conformance.standards.tnt.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.*;
import org.dcsa.conformance.core.scenario.ConformanceAction;
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
      ConformanceAction previousAction,
      Map<TntEventType, JsonSchemaValidator> eventSchemaValidators) {
    super(subscriberPartyName, publisherPartyName, previousAction, "GetEvents", 200);
    this.eventSchemaValidators = eventSchemaValidators;
  }

  @Override
  public String getHumanReadablePrompt() {
    return "Send a GET events request with the following parameters: "
        + sspSupplier.get().toJson().toPrettyString();
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

              private boolean isEventNode(JsonNode jsonNode) {
                return jsonNode.isObject() && !jsonNode.path("eventType").isMissingNode();
              }

              private ArrayList<JsonNode> _findEventNodes(
                  ArrayList<JsonNode> foundEventNodes, JsonNode searchInJsonNode) {
                if (isEventNode(searchInJsonNode)) {
                  foundEventNodes.add(searchInJsonNode);
                } else {
                  searchInJsonNode.forEach(
                      elementNode -> _findEventNodes(foundEventNodes, elementNode));
                }
                return foundEventNodes;
              }

              @Override
              protected Set<String> checkConformance(
                  Function<UUID, ConformanceExchange> getExchangeByUuid) {
                ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
                if (exchange == null) return Set.of();
                JsonNode jsonResponse = exchange.getMessage(httpMessageType).body().getJsonBody();
                LinkedHashSet<String> validationErrors = new LinkedHashSet<>();
                if (!jsonResponse.isArray()) {
                  validationErrors.add("The root JSON response must be an array of events");
                }
                ArrayList<JsonNode> eventNodes = _findEventNodes(new ArrayList<>(), jsonResponse);
                int eventCount = eventNodes.size();
                for (int eventIndex = 0; eventIndex < eventCount; ++eventIndex) {
                  JsonNode eventNode = eventNodes.get(eventIndex);
                  JsonNode eventTypeNode = eventNode.path("eventType");
                  TntEventType eventType;
                  String eventTypeText = eventTypeNode.asText().toUpperCase();
                  try {
                    eventType = TntEventType.valueOf(eventTypeText);
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

  public ObjectNode asJsonNode() {
    return super.asJsonNode().set("suppliedScenarioParameters", sspSupplier.get().toJson());
  }
}
