package org.dcsa.conformance.standards.vgm.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.vgm.action.ConsumerGetVgmDeclarationAction;

@Slf4j
public class VgmConsumer extends ConformanceParty {

  public VgmConsumer(
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
    return Map.ofEntries(Map.entry(ConsumerGetVgmDeclarationAction.class, this::getVgmDeclaration));
  }

  private void getVgmDeclaration(JsonNode actionPrompt) {
    log.info("{}.getVgmDeclaration({})", getClass().getSimpleName(), actionPrompt.toPrettyString());

    SuppliedScenarioParameters ssp =
        SuppliedScenarioParameters.fromJson(actionPrompt.get("suppliedScenarioParameters"));

    Map<String, Collection<String>> queryParams =
        ssp.getMap().entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> entry.getKey().getParameterName(), entry -> Set.of(entry.getValue())));

    syncCounterpartGet("/vgm-declarations", queryParams);

    addOperatorLogEntry(
        "Sent GET VGM Declaration request with parameters %s"
            .formatted(ssp.toJson().toPrettyString()));
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
}
