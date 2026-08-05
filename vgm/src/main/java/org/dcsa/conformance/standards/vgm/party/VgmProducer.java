package org.dcsa.conformance.standards.vgm.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
import org.dcsa.conformance.standards.vgm.action.ProducerPostVgmDeclarationAction;
import org.dcsa.conformance.standards.vgm.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.vgm.checks.VgmQueryParameters;

@Slf4j
public class VgmProducer extends ConformanceParty {

  public VgmProducer(
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
        Map.entry(ProducerPostVgmDeclarationAction.class, this::sendVgmDeclarations));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info(
        "{}.supplyScenarioParameters({})",
        getClass().getSimpleName(),
        actionPrompt.toPrettyString());

    JsonNode queryParametersNode = actionPrompt.required("vgmQueryParameters");
    Set<VgmQueryParameters> queryParameters =
        StreamSupport.stream(queryParametersNode.spliterator(), false)
            .map(JsonNode::asText)
            .map(VgmQueryParameters::fromParameterName)
            .collect(Collectors.toSet());

    ObjectNode ssp = SupplyScenarioParametersAction.examplePrompt(queryParameters);
    asyncOrchestratorPostPartyInput(actionPrompt.required("actionId").asText(), ssp);

    addOperatorLogEntry("Supplying scenario parameters: %s".formatted(ssp.toPrettyString()));
  }

  private void sendVgmDeclarations(JsonNode actionPrompt) {
    log.info(
        "{}.sendVgmDeclarations({})", getClass().getSimpleName(), actionPrompt.toPrettyString());

    JsonNode jsonRequestBody =
        JsonToolkit.templateFileToJsonNode(
            "/standards/vgm/messages/vgm-api-%s-post-request.json"
                .formatted(apiVersion.toLowerCase().replaceAll("[.-]", "")),
            Map.of(
                "CARRIER_BOOKING_REFERENCE_PLACEHOLDER", ReferenceGenerator.newReference(),
                "TRANSPORT_DOCUMENT_REFERENCE_PLACEHOLDER", ReferenceGenerator.newReference()));
    syncCounterpartPost("/vgm-declarations", jsonRequestBody);

    addOperatorLogEntry("Sent VGM Declarations");
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    JsonNode jsonResponseBody =
        JsonToolkit.templateFileToJsonNode(
            "/standards/vgm/messages/vgm-api-%s-get-response.json"
                .formatted(apiVersion.toLowerCase().replaceAll("[.-]", "")),
            Map.of(
                "CARRIER_BOOKING_REFERENCE_PLACEHOLDER", ReferenceGenerator.newReference(),
                "TRANSPORT_DOCUMENT_REFERENCE_PLACEHOLDER", ReferenceGenerator.newReference()));

    Map<String, Collection<String>> responseHeaders = new LinkedHashMap<>();
    responseHeaders.put(API_VERSION, List.of(apiVersion));
    if (request.queryParams().containsKey(VgmQueryParameters.LIMIT.getParameterName())
        && !request.queryParams().containsKey(VgmQueryParameters.CURSOR.getParameterName())) {
      responseHeaders.put("Next-Page-Cursor", List.of(ReferenceGenerator.newReference()));
    }

    return request.createResponse(200, responseHeaders, new ConformanceMessageBody(jsonResponseBody));
  }
}
