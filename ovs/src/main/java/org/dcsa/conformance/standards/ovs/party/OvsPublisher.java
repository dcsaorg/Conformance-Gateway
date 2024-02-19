package org.dcsa.conformance.standards.ovs.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
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
import org.dcsa.conformance.standards.ovs.action.SupplyScenarioParametersAction;

@Slf4j
public class OvsPublisher extends ConformanceParty {

  public OvsPublisher(
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
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {}

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {}

  @Override
  protected void doReset() {}

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(SupplyScenarioParametersAction.class, this::supplyScenarioParameters));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info("OvsPublisher.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));

    SuppliedScenarioParameters responseSsp =
        SuppliedScenarioParameters.fromMap(
            StreamSupport.stream(
                    actionPrompt.required("ovsFilterParametersQueryParamNames").spliterator(),
                    false)
                .map(
                    jsonOvsFilterParameter ->
                        OvsFilterParameter.byQueryParamName.get(jsonOvsFilterParameter.asText()))
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        ovsFilterParameter ->
                            switch (ovsFilterParameter) {
                              case CARRIER_SERVICE_NAME -> "Great Lion Service";
                              case CARRIER_SERVICE_CODE -> "FE1";
                              case UNIVERSAL_SERVICE_REFERENCE -> "SR12345A";
                              case VESSEL_IMO_NUMBER -> "9321483";
                              case VESSEL_NAME -> "King of the Seas";
                              case CARRIER_VOYAGE_NUMBER -> "2103S";
                              case UNIVERSAL_VOYAGE_REFERENCE -> "2201N";
                              case UN_LOCATION_CODE -> "NLAMS";
                              case FACILITY_SMDG_CODE -> "APM";
                              case START_DATE, END_DATE ->
                                  new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                              case LIMIT -> "100";
                            })));

    asyncOrchestratorPostPartyInput(
        new ObjectMapper()
            .createObjectNode()
            .put("actionId", actionPrompt.required("actionId").asText())
            .set("input", responseSsp.toJson()));

    addOperatorLogEntry(
        "Submitting SuppliedScenarioParameters: %s"
            .formatted(responseSsp.toJson().toPrettyString()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("OvsPublisher.handleRequest(%s)".formatted(request));

    JsonNode jsonResponseBody =
        JsonToolkit.templateFileToJsonNode(
            "/standards/ovs/messages/ovs-%s-response.json"
                .formatted(apiVersion.toLowerCase().replaceAll("[.-]", "")),
            Map.ofEntries());

    return request.createResponse(
        200,
        Map.of("Api-Version", List.of(apiVersion)),
        new ConformanceMessageBody(jsonResponseBody));
  }
}
