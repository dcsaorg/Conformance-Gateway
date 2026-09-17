package org.dcsa.conformance.standards.an.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.an.action.SubscriberGetANAction;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

public class ANSubscriber extends ConformanceParty {
  public ANSubscriber(String apiVersion, PartyConfiguration partyConfiguration, CounterpartConfiguration counterpartConfiguration, JsonNodeMap persistentMap, PartyWebClient webClient, Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(apiVersion, partyConfiguration, counterpartConfiguration, persistentMap, webClient, orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    // no state to export
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    // no state to import
  }

  @Override
  protected void doReset() {
    // no state to reset
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(Map.entry(SubscriberGetANAction.class, this::getArrivalNotices));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    ObjectNode responseNode = OBJECT_MAPPER.createObjectNode();
    responseNode.putArray("feedbackElements");

    ConformanceResponse response =
      request.createResponse(
        200,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(responseNode));

    addOperatorLogEntry(
      "Handled lightweight notification: %s".formatted(request.message().body().getJsonBody()));
    return response;
  }

  private void getArrivalNotices(JsonNode actionPrompt) {
    Map<String, Collection<String>> queryParameters = new LinkedHashMap<>();
    actionPrompt
      .path("suppliedQueryParameters")
      .properties()
      .forEach(entry -> queryParameters.put(entry.getKey(), List.of(entry.getValue().asText())));
    if (actionPrompt.has("cursor")) {
      queryParameters.put("cursor", List.of(actionPrompt.required("cursor").asText()));
    }
    syncCounterpartGet("/arrival-notices", queryParameters);
    addOperatorLogEntry("Sent a GET Arrival Notices request");
  }
}
