package org.dcsa.conformance.standards.ovs.action;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.junit.jupiter.api.Test;

class OvsActionTest {

  private static final String SUBSCRIBER = "Schedule Consumer1";
  private static final String PUBLISHER = "Schedule Producer1";
  private static final String CURSOR = "next-page-cursor";

  @Test
  void nextGetReadsCursorFromPreviousResponseAfterScenarioConstruction() {
    OvsGetSchedulesAction firstGet = getSchedulesAfter(null);
    OvsGetSchedulesAction secondGet = getSchedulesAfter(firstGet);

    assertFalse(secondGet.asJsonNode().has("cursor"));

    firstGet.handleExchange(exchangeWithCursor());

    assertEquals(CURSOR, secondGet.asJsonNode().path("cursor").asText());

    OvsGetSchedulesAction restoredFirstGet = getSchedulesAfter(null);
    OvsGetSchedulesAction restoredSecondGet = getSchedulesAfter(restoredFirstGet);
    restoredFirstGet.importJsonState(firstGet.exportJsonState());

    assertEquals(CURSOR, restoredSecondGet.asJsonNode().path("cursor").asText());

    restoredFirstGet.reset();

    assertFalse(restoredSecondGet.asJsonNode().has("cursor"));
  }

  private static OvsGetSchedulesAction getSchedulesAfter(OvsGetSchedulesAction previousAction) {
    return new OvsGetSchedulesAction(SUBSCRIBER, PUBLISHER, previousAction, null, false);
  }

  private static ConformanceExchange exchangeWithCursor() {
    ConformanceRequest request =
        new ConformanceRequest(
            "GET",
            "http://localhost/v3/service-schedules",
            Map.of(),
            new ConformanceMessage(
                SUBSCRIBER,
                "Schedule Consumer",
                PUBLISHER,
                "Schedule Producer",
                Map.of(),
                new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()),
                0));
    return new ConformanceExchange(
        request,
        request.createResponse(
            200,
            Map.of("Next-Page-Cursor", List.of(CURSOR)),
            new ConformanceMessageBody(OBJECT_MAPPER.createArrayNode())));
  }
}


