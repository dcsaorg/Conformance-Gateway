package org.dcsa.conformance.standards.tnt.v220.checks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheckResult;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.tnt.v220.action.TntEventType;
import org.dcsa.conformance.standards.tnt.v220.party.TntRole;

public class TntSchemaConformanceCheck extends ActionCheck {

  private final Map<TntEventType, JsonSchemaValidator> eventSchemaValidators;

  public TntSchemaConformanceCheck(
      UUID matchedExchangeUuid, Map<TntEventType, JsonSchemaValidator> eventSchemaValidators) {
    super(
        "",
        "The HTTP %s matches the standard JSON schema"
            .formatted(HttpMessageType.RESPONSE.name().toLowerCase()),
        TntRole::isPublisher,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE);
    this.eventSchemaValidators = eventSchemaValidators;
  }

  public static ArrayList<JsonNode> findEventNodes(JsonNode jsonNode) {
    return _findEventNodes(0, new ArrayList<>(), jsonNode, (error) -> {});
  }

  private static boolean _isEventNode(JsonNode jsonNode) {
    return jsonNode.isObject() && !jsonNode.path("eventType").isMissingNode();
  }

  private static ArrayList<JsonNode> _findEventNodes(
      int level,
      ArrayList<JsonNode> foundEventNodes,
      JsonNode searchInJsonNode,
      Consumer<String> validationErrorConsumer) {
    if (_isEventNode(searchInJsonNode)) {
      foundEventNodes.add(searchInJsonNode);
      if (level > 1) {
        validationErrorConsumer.accept(
            "At least one event is not an element in the root response array");
      }
    } else {
      searchInJsonNode.forEach(
          elementNode ->
              _findEventNodes(level + 1, foundEventNodes, elementNode, validationErrorConsumer));
    }
    return foundEventNodes;
  }

  @Override
  protected ConformanceCheckResult performCheck(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
    if (exchange == null) return ConformanceCheckResult.simple(Set.of());
    JsonNode jsonResponse = exchange.getMessage(httpMessageType).body().getJsonBody();
    LinkedHashSet<String> validationErrors = new LinkedHashSet<>();
    if (!jsonResponse.isArray()) {
      validationErrors.add("The root JSON response must be an array of events");
    }
    ArrayList<JsonNode> eventNodes =
        _findEventNodes(0, new ArrayList<>(), jsonResponse, validationErrors::add);
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
            "Event #%d: incorrect eventType attribute: %s".formatted(eventIndex, eventTypeNode));
        continue;
      }
      JsonSchemaValidator eventSchemaValidator = eventSchemaValidators.get(eventType);
      for (String validationError : eventSchemaValidator.validate(eventNode)) {
        validationErrors.add("Event #%d: %s".formatted(eventIndex, validationError));
      }
    }
    return ConformanceCheckResult.simple(validationErrors);
  }
}
