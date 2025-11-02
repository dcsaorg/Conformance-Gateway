package org.dcsa.conformance.standards.portcall.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.portcall.action.PublisherPostPortCallEventsAction;
import org.dcsa.conformance.standards.portcall.action.SupplyScenarioParametersAction;

@Slf4j
public class PortCallPublisher extends ConformanceParty {

  public PortCallPublisher(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      PartyWebClient webClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        persistentMap,
        webClient,
        orchestratorAuthHeader);
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
    return Map.ofEntries(
      Map.entry(SupplyScenarioParametersAction.class, this::supplyScenarioParameters),
      Map.entry(PublisherPostPortCallEventsAction.class, this::sendArrivalNotices));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info(
      "{}.supplyScenarioParameters({})",
      getClass().getSimpleName(),
      actionPrompt.toPrettyString());

    ObjectNode ssp = SupplyScenarioParametersAction.examplePrompt();
    asyncOrchestratorPostPartyInput(actionPrompt.required("actionId").asText(), ssp);

    addOperatorLogEntry("Supplying scenario parameters: %s".formatted(ssp.toPrettyString()));
  }

  private void sendArrivalNotices(JsonNode actionPrompt) {
    String filePath = getPortCallPayloadFilepath();
    JsonNode jsonRequestBody = JsonToolkit.templateFileToJsonNode(filePath, Map.ofEntries());
    syncCounterpartPost("/events", jsonRequestBody);
    addOperatorLogEntry("Sent Port Call Events ");
  }

  private String getPortCallPayloadFilepath() {
    return "/standards/portcall/messages/portcall-api-%s-post-request.json"
      .formatted(apiVersion.toLowerCase().replaceAll("[.-]", ""));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {

    JsonNode jsonResponseBody =
      JsonToolkit.templateFileToJsonNode(
        "/standards/portcall/messages/portcall-api-%s-get-response.json"
          .formatted(apiVersion.toLowerCase().replaceAll("[.-]", "")),
        Map.of());

    return request.createResponse(
      200,
      Map.of(API_VERSION, List.of(apiVersion)),
      new ConformanceMessageBody(jsonResponseBody));
  }
}
