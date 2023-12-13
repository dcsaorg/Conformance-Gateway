package org.dcsa.conformance.standards.ovs.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.ovs.action.OvsGetSchedulesAction;

@Slf4j
public class OvsSubscriber extends ConformanceParty {

  public OvsSubscriber(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      BiConsumer<ConformanceRequest, Consumer<ConformanceResponse>> asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        persistentMap,
        asyncWebClient,
        orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {}

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {}

  @Override
  protected void doReset() {}

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(Map.entry(OvsGetSchedulesAction.class, this::getSchedules));
  }

  private void getSchedules(JsonNode actionPrompt) {
    log.info("OvsSubscriber.getSchedules(%s)".formatted(actionPrompt.toPrettyString()));

    asyncCounterpartGet(
        "/%s/service-schedules".formatted(apiVersion.startsWith("2") ? "v2" : "v3"), Map.ofEntries());

    addOperatorLogEntry("Sending GET schedules request");
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("OvsSubscriber.handleRequest(%s)".formatted(request));
    throw new UnsupportedOperationException();
  }
}
