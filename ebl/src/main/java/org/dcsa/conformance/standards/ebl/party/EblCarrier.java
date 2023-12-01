package org.dcsa.conformance.standards.ebl.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.ebl.action.Carrier_SupplyScenarioParametersAction;

@Slf4j
public class EblCarrier extends ConformanceParty {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public EblCarrier(
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
    return Map.ofEntries(
      Map.entry(Carrier_SupplyScenarioParametersAction.class, this::supplyScenarioParameters)
    );
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info("Carrier.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));
    CarrierScenarioParameters carrierScenarioParameters =
      new CarrierScenarioParameters(
        "CARRIER_BOOKING_REFX",
        "COMMODITY_SUBREFERENCE_FOR_REFX");
    asyncOrchestratorPostPartyInput(
      OBJECT_MAPPER
        .createObjectNode()
        .put("actionId", actionPrompt.get("actionId").asText())
        .set("input", carrierScenarioParameters.toJson()));
    addOperatorLogEntry(
      "Provided CarrierScenarioParameters: %s".formatted(carrierScenarioParameters));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    return request.createResponse(
        404,
        Map.of("Api-Version", List.of(apiVersion)),
        new ConformanceMessageBody(
            OBJECT_MAPPER
                .createObjectNode()
                .put("message", "Returning 404 since the request did not match any known URL")));
  }
}
