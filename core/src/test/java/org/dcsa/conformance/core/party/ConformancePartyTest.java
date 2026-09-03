package org.dcsa.conformance.core.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.Url;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConformancePartyTest {

  @Test
  void suppressedNotificationCompletesIdentifiedActionWithNotificationDataAndWithoutCounterpartTraffic() {
    PartyWebClient webClient = mock(PartyWebClient.class);
    TestParty party = testParty(webClient);
    party.setSuppressNotifications(true);

    ObjectNode notificationData =
        OBJECT_MAPPER.createObjectNode().put("transportDocumentReference", "TDR-1");
    party.sendNotification(
        "action-id",
        "/notifications",
        OBJECT_MAPPER.createObjectNode(),
        notificationData);

    ArgumentCaptor<ConformanceRequest> requestCaptor =
        ArgumentCaptor.forClass(ConformanceRequest.class);
    verify(webClient).asyncRequest(requestCaptor.capture());
    ConformanceRequest completionRequest = requestCaptor.getValue();
    assertEquals("http://orchestrator/party/Carrier1/input", completionRequest.url());
    assertEquals(
        "action-id",
        completionRequest.message().body().getJsonBody().required("actionId").asText());
    assertEquals(
        notificationData,
        completionRequest.message().body().getJsonBody().required("input"));
    assertEquals(
        "Carrier",
        completionRequest
            .message()
            .body()
            .getJsonBody()
            .required("completeCurrentActionWithoutTraffic")
            .asText());
  }

  @Test
  void suppressedNotificationWithoutActionIdSignalsOptionalFollowUpCompletion() {
    PartyWebClient webClient = mock(PartyWebClient.class);
    TestParty party = testParty(webClient);
    party.setSuppressNotifications(true);
    party.setCurrentSessionId("session-1");

    party.sendNotification(
        null,
        "/notifications",
        OBJECT_MAPPER.createObjectNode(),
        OBJECT_MAPPER.createObjectNode());

    ArgumentCaptor<ConformanceRequest> requestCaptor =
        ArgumentCaptor.forClass(ConformanceRequest.class);
    verify(webClient).asyncRequest(requestCaptor.capture());
    ConformanceRequest completionRequest = requestCaptor.getValue();
    assertEquals("http://orchestrator/party/Carrier1/input", completionRequest.url());
    assertEquals(
        "Carrier",
        completionRequest
            .message()
            .body()
            .getJsonBody()
            .required("completeCurrentActionWithoutNotification")
            .asText());
    assertEquals(
        "session-1",
        completionRequest.message().body().getJsonBody().required("sessionId").asText());
  }

  private static TestParty testParty(PartyWebClient webClient) {
    PartyConfiguration partyConfiguration = new PartyConfiguration();
    partyConfiguration.setName("Carrier1");
    partyConfiguration.setRole("Carrier");
    partyConfiguration.setOrchestratorUrl("http://orchestrator");
    CounterpartConfiguration counterpartConfiguration = new CounterpartConfiguration();
    counterpartConfiguration.setName("Shipper1");
    counterpartConfiguration.setRole("Shipper");
    counterpartConfiguration.setUrl(Url.ofTrusted("http://counterpart"));
    return new TestParty(
        partyConfiguration,
        counterpartConfiguration,
        mock(JsonNodeMap.class),
        webClient);
  }

  private static final class TestParty extends ConformanceParty {
    private TestParty(
        PartyConfiguration partyConfiguration,
        CounterpartConfiguration counterpartConfiguration,
        JsonNodeMap persistentMap,
        PartyWebClient webClient) {
      super(
          "3.0.0",
          partyConfiguration,
          counterpartConfiguration,
          persistentMap,
          webClient,
          Collections.emptyMap());
    }

    private void sendNotification(
        String actionId, String path, JsonNode body, ObjectNode completionInput) {
      asyncCounterpartNotification(actionId, path, body, completionInput);
    }

    @Override
    protected void exportPartyJsonState(ObjectNode targetObjectNode) {}

    @Override
    protected void importPartyJsonState(ObjectNode sourceObjectNode) {}

    @Override
    public ConformanceResponse handleRequest(ConformanceRequest request) {
      return null;
    }

    @Override
    protected void doReset() {}

    @Override
    public Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
      return Collections.emptyMap();
    }
  }
}


