package org.dcsa.conformance.standards.tnt.v300.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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
import org.dcsa.conformance.standards.tnt.v300.action.ProducerPostTntAction;
import org.dcsa.conformance.standards.tnt.v300.action.TntEventType;

public class TntProducer extends ConformanceParty {

  public TntProducer(
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
    return Map.ofEntries(Map.entry(ProducerPostTntAction.class, this::sendTntEvents));
  }

  private void sendTntEvents(JsonNode actionPrompt) {
    var scenarioType = TntEventType.valueOf(actionPrompt.required("eventType").asText());
    String filePath = getTntEventPayloadFilepath(scenarioType);

    JsonNode jsonRequestBody = JsonToolkit.templateFileToJsonNode(filePath, Map.ofEntries());
    syncCounterpartPost("/events", jsonRequestBody);
    addOperatorLogEntry("Sent TnT Events");
  }

  private String getTntEventPayloadFilepath(TntEventType eventType) {
    return "/standards/tnt/messages/"
        + eventType.tntEventPayload(apiVersion.toLowerCase().replaceAll("[.-]", ""));
  }

  private String getAnRegularResponseFilepath() {
    return "/standards/tnt/messages/"
        + TntEventType.SHIPMENT.tntEventResponse(apiVersion.toLowerCase().replaceAll("[.-]", ""));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    Collection<String> tdrValues = request.queryParams().get("transportDocumentReferences");
    Optional<String> tdrParam =
        (tdrValues == null ? Collections.<String>emptyList() : tdrValues).stream().findFirst();

    Set<String> requestedTdrs =
        tdrParam.stream()
            .flatMap(s -> Arrays.stream(s.split(",")))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));

    String chosenTdr = requestedTdrs.stream().findFirst().orElse("");
    Map<String, String> templateVars = Map.of("TRANSPORT_DOCUMENT_REFERENCE", chosenTdr);

    String filePath = getAnRegularResponseFilepath();
    JsonNode responseObject = JsonToolkit.templateFileToJsonNode(filePath, templateVars);
    return request.createResponse(
        200, Map.of(API_VERSION, List.of(apiVersion)), new ConformanceMessageBody(responseObject));
  }
}
