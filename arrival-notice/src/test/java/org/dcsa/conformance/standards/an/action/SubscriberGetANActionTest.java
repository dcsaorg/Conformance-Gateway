package org.dcsa.conformance.standards.an.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.standards.an.ANComponentFactory;
import org.dcsa.conformance.standards.an.checks.ScenarioType;
import org.dcsa.conformance.standards.an.party.DynamicScenarioParameters;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriberGetANActionTest {

  private static final ANComponentFactory FACTORY =
    new ANComponentFactory("AN", "1.0.0", "Conformance");

  @Test
  void propagatesNextPageCursorToFollowUpRequestChecks() {
    SubscriberGetANAction firstPageAction =
      new SubscriberGetANAction(
        "Subscriber1",
        "Publisher1",
        null,
        FACTORY.getMessageSchemaValidator("GetArrivalNoticesResponse"),
        "GET Arrival Notices page 1",
        true);

    firstPageAction.getDspConsumer().accept(
      new DynamicScenarioParameters(
        Map.of("transportDocumentReferences", "HHL71800000", "limit", "1"),
        ScenarioType.BASIC.name(),
        null,
        null,
        null));
    firstPageAction.handleExchange(exchangeWithCursor("next-page-token"));

    SubscriberGetANAction secondPageAction =
      new SubscriberGetANAction(
        "Subscriber1",
        "Publisher1",
        firstPageAction,
        FACTORY.getMessageSchemaValidator("GetArrivalNoticesResponse"),
        "GET Arrival Notices page 2",
        false);

    ObjectNode prompt = secondPageAction.asJsonNode();
    assertTrue(prompt.has("cursor") && "next-page-token".equals(prompt.path("cursor").asText()));

  }

  @Test
  void requiredConsumerGetScenarioStillChecksForNonEmptyArrivalNotices() {
    SubscriberGetANAction action =
      new SubscriberGetANAction(
        "Subscriber1",
        "Publisher1",
        null,
        FACTORY.getMessageSchemaValidator("GetArrivalNoticesResponse"),
        "GET Arrival Notices",
        false);

    List<String> allTitles = allTitles(action.createCheck("1.0.0")).toList();

    assertTrue(
      allTitles.stream()
        .anyMatch(
          title ->
            title.contains("At least one Arrival Notice must be included in the message's 'arrivalNotices' list.")));
  }

  private static Stream<String> allTitles(ConformanceCheck check) {
    return Stream.concat(
      Stream.of(check.getTitle()),
      check.subChecksStream().flatMap(SubscriberGetANActionTest::allTitles));
  }

  private static ConformanceExchange exchangeWithCursor(String cursor) {
    ObjectNode requestBody = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    ConformanceRequest request =
      new ConformanceRequest(
        "GET",
        "http://localhost/arrival-notices",
        Map.of("transportDocumentReferences", List.of("HHL71800000"), "limit", List.of("1")),
        new ConformanceMessage(
          "Subscriber1",
          "Consumer",
          "Publisher1",
          "Producer",
          Map.of(),
          new ConformanceMessageBody(requestBody),
          System.currentTimeMillis()));

    ObjectNode responseBody = JsonToolkit.OBJECT_MAPPER.createObjectNode();
    responseBody.putArray("arrivalNotices").addObject().put("transportDocumentReference", "HHL71800000");

    return new ConformanceExchange(
      request,
      request.createResponse(
        200,
        Map.of("API-Version", List.of("1.0.0"), "Next-Page-Cursor", List.of(cursor)),
        new ConformanceMessageBody(responseBody)));
  }
}


