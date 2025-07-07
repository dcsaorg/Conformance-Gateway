package org.dcsa.conformance.standards.an.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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

public class ANSubscriber extends ConformanceParty {
  public ANSubscriber(String apiVersion, PartyConfiguration partyConfiguration, CounterpartConfiguration counterpartConfiguration, JsonNodeMap persistentMap, PartyWebClient webClient, Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(apiVersion, partyConfiguration, counterpartConfiguration, persistentMap, webClient, orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {

  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {

  }

  @Override
  protected void doReset() {

  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(Map.entry(SubscriberGetANAction.class, this::getArrivalNotices));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    ConformanceResponse response =
        request.createResponse(
            200,
            Map.of(API_VERSION, List.of(apiVersion)),
            new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));

    addOperatorLogEntry(
        "Handled lightweight notification: %s".formatted(request.message().body().getJsonBody()));
    return response;
  }

  private void getArrivalNotices(JsonNode actionPrompt) {
    JsonNode dsp = actionPrompt.required("dsp");
    syncCounterpartGet("/arrival-notices", Map.of());
  }
}
