package org.dcsa.conformance.standards.portcall.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.dcsa.conformance.core.util.ReferenceGenerator;
import org.dcsa.conformance.standards.portcall.action.PostPortCallEventsAction;
import org.dcsa.conformance.standards.portcall.action.SupplyScenarioParametersAction;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Slf4j
public class PortCallProducer extends ConformanceParty {

  public PortCallProducer(
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
      Map.entry(PostPortCallEventsAction.class, this::sendPortCallEvents));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info("{}.supplyScenarioParameters({})", getClass().getSimpleName(), actionPrompt.toPrettyString());

    JsonNode filterParametersNode = actionPrompt.path("filterParameters");
    ObjectNode ssp =
      filterParametersNode.isArray() && !filterParametersNode.isEmpty()
        ? SupplyScenarioParametersAction.examplePrompt(
        StreamSupport.stream(filterParametersNode.spliterator(), false)
          .map(JsonNode::asText)
          .map(PortCallFilterParameter::fromQueryParamName)
          .collect(Collectors.toCollection(LinkedHashSet::new)))
        : SupplyScenarioParametersAction.examplePrompt();
    asyncOrchestratorPostPartyInput(actionPrompt.required("actionId").asText(), ssp);

    addOperatorLogEntry("Supplying scenario parameters: %s".formatted(ssp.toPrettyString()));
  }

  private void sendPortCallEvents(JsonNode actionPrompt) {
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
    boolean hasCursor = request.queryParams().containsKey(PortCallFilterParameter.CURSOR.getQueryParamName());
    JsonNode jsonResponseBody = JsonToolkit.templateFileToJsonNode(
      "/standards/portcall/messages/portcall-api-%s-get-response%s.json"
        .formatted(apiVersion.toLowerCase().replaceAll("[.-]", ""), hasCursor ? "-nextpage" : ""), Map.of());

    Map<String, Collection<String>> responseHeaders = new LinkedHashMap<>();
    responseHeaders.put(API_VERSION, List.of(apiVersion));
    if (request.queryParams().containsKey(PortCallFilterParameter.LIMIT.getQueryParamName())
      && !hasCursor) {
      responseHeaders.put("Next-Page-Cursor", List.of(ReferenceGenerator.newReference()));
    }

    addOperatorLogEntry("Handled request: %s".formatted(request.toString()));
    return request.createResponse(
      200,
      responseHeaders,
      new ConformanceMessageBody(jsonResponseBody));

  }
}
