package org.dcsa.conformance.end.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
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
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.end.action.CarrierGetEndorsementChainAction;

@Slf4j
public class EndorsementChainCarrier extends ConformanceParty {
  public EndorsementChainCarrier(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      PartyWebClient webClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(apiVersion, partyConfiguration, counterpartConfiguration, persistentMap, webClient, orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {

  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {

  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    return null;
  }

  @Override
  protected void doReset() {

  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.of(CarrierGetEndorsementChainAction.class, this::getEndorsementChain);
  }

  private void getEndorsementChain(JsonNode actionPrompt) {
    log.info("Carrier.getEndorsementChain %s".formatted(actionPrompt.toPrettyString()));
    SuppliedScenarioParameters ssp =
        SuppliedScenarioParameters.fromJson(actionPrompt.get("suppliedScenarioParameters"));

    Map<String, Collection<String>> queryParams = getQueryParams(ssp);
    String pathParam = getPathParam(ssp);
    syncCounterpartGet("/endorsement-chains/" + pathParam, queryParams);

    addOperatorLogEntry(
        "Sent GET Endorsement Chain request with parameters: %s".formatted(ssp.toString()));
  }

  private static Map<String, Collection<String>> getQueryParams(SuppliedScenarioParameters ssp) {
    return ssp.getMap().entrySet().stream()
        .filter(
            entry ->
                !entry
                    .getKey()
                    .equals(EndorsementChainFilterParameter.TRANSPORT_DOCUMENT_REFERENCE))
        .collect(
            Collectors.toMap(
                entry -> entry.getKey().getQueryParamName(), entry -> Set.of(entry.getValue())));
  }

  private static String getPathParam(SuppliedScenarioParameters ssp) {
    Optional<Map.Entry<EndorsementChainFilterParameter, String>> pathParam =
        ssp.getMap().entrySet().stream()
            .filter(
                entry ->
                    entry
                        .getKey()
                        .equals(EndorsementChainFilterParameter.TRANSPORT_DOCUMENT_REFERENCE))
            .findFirst();

    return pathParam.isPresent() ? pathParam.get().getValue() : "";
  }
}
