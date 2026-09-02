package org.dcsa.conformance.core.check;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseLimitCheckTest {

  private static final UUID EXCHANGE_UUID = UUID.randomUUID();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void passesWhenRootArraySizeIsWithinSuppliedLimit() {
    ArrayNode body = objectMapper.createArrayNode();
    body.add(objectMapper.createObjectNode());

    var check =
      new ResponseLimitCheck(
        role -> true,
        EXCHANGE_UUID,
        HttpMessageType.RESPONSE,
        () -> "1",
        "Port Schedule");

    var result = check.performCheck(uuid -> exchangeWithResponseBody(body));
    assertTrue(result.getErrorMessages().isEmpty());
  }

  @Test
  void failsWhenRootArraySizeExceedsSuppliedLimit() {
    ArrayNode body = objectMapper.createArrayNode();
    body.add(objectMapper.createObjectNode());
    body.add(objectMapper.createObjectNode());

    var check =
      new ResponseLimitCheck(
        role -> true,
        EXCHANGE_UUID,
        HttpMessageType.RESPONSE,
        () -> "1",
        "Point-to-Point Routing");

    var result = check.performCheck(uuid -> exchangeWithResponseBody(body));
    assertFalse(result.getErrorMessages().isEmpty());
    assertTrue(
      result.getErrorMessages().stream()
        .anyMatch(message -> message.contains("exceeds the supplied limit of 1")));
  }

  @Test
  void returnsIrrelevantWhenNoLimitWasSupplied() {
    ArrayNode body = objectMapper.createArrayNode();
    body.add(objectMapper.createObjectNode());

    var check =
      new ResponseLimitCheck(
        role -> true,
        EXCHANGE_UUID,
        HttpMessageType.RESPONSE,
        () -> null,
        "Service Schedule");

    var result = check.performCheck(uuid -> exchangeWithResponseBody(body));
    Set<ConformanceError> errors = ((ConformanceCheckResult.ErrorsWithRelevance) result).errors();
    assertEquals(1, errors.size());
    assertEquals(ConformanceErrorSeverity.IRRELEVANT, errors.iterator().next().severity());
  }

  @Test
  void failsWhenLimitIsNotAnInteger() {
    ArrayNode body = objectMapper.createArrayNode();
    body.add(objectMapper.createObjectNode());

    var check =
      new ResponseLimitCheck(
        role -> true,
        EXCHANGE_UUID,
        HttpMessageType.RESPONSE,
        () -> "abc",
        "Port Schedule");

    var result = check.performCheck(uuid -> exchangeWithResponseBody(body));
    assertTrue(
      result.getErrorMessages().stream()
        .anyMatch(message -> message.contains("is not a valid integer")));
  }

  @Test
  void failsWhenBodyIsNotAnArray() {
    ObjectNode body = objectMapper.createObjectNode();

    var check =
      new ResponseLimitCheck(
        role -> true,
        EXCHANGE_UUID,
        HttpMessageType.RESPONSE,
        () -> "1",
        "Port Schedule");

    var result = check.performCheck(uuid -> exchangeWithResponseBody(body));
    assertTrue(
      result.getErrorMessages().stream()
        .anyMatch(message -> message.contains("must be a root JSON array")));
  }

  @Test
  void evaluatesLimitAtCheckTimeRatherThanConstructorTime() {
    ArrayNode body = objectMapper.createArrayNode();
    body.add(objectMapper.createObjectNode());
    body.add(objectMapper.createObjectNode());

    AtomicReference<String> limit = new AtomicReference<>(null);
    var check =
        new ResponseLimitCheck(
            role -> true,
            EXCHANGE_UUID,
            HttpMessageType.RESPONSE,
            limit::get,
            "Port Schedule");

    limit.set("1");

    var result = check.performCheck(uuid -> exchangeWithResponseBody(body));
    assertFalse(result.getErrorMessages().isEmpty());
    assertTrue(
        result.getErrorMessages().stream()
            .anyMatch(message -> message.contains("exceeds the supplied limit of 1")));
  }

  private ConformanceExchange exchangeWithResponseBody(com.fasterxml.jackson.databind.JsonNode body) {
    var requestMessage =
      new ConformanceMessage(
        "consumer",
        "consumer",
        "producer",
        "producer",
        Map.of(),
        new ConformanceMessageBody(objectMapper.createObjectNode()),
        System.currentTimeMillis());
    var responseMessage =
      new ConformanceMessage(
        "producer",
        "producer",
        "consumer",
        "consumer",
        Map.of(),
        new ConformanceMessageBody(body),
        System.currentTimeMillis());
    var request =
      new ConformanceRequest("GET", "https://example.test/v1/port-schedules", Map.of(), requestMessage);
    var response = new ConformanceResponse(200, responseMessage);
    return new ConformanceExchange(request, response);
  }
}

