package org.dcsa.conformance.standards.jit.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import org.dcsa.conformance.standards.jit.action.SupplyScenarioParametersAction;

@Slf4j
public class JitPublisher extends ConformanceParty {

  public JitPublisher(
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
    log.info("JitPublisher.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));

    SuppliedScenarioParameters responseSsp =
        SuppliedScenarioParameters.fromMap(
            StreamSupport.stream(
                    actionPrompt.required("jitFilterParametersQueryParamNames").spliterator(),
                    false)
                .map(
                    jsonJitFilterParameter ->
                        JitFilterParameter.byQueryParamName.get(jsonJitFilterParameter.asText()))
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        jitFilterParameter ->
                            switch (jitFilterParameter) {
                              case TRANSPORT_CALL_ID -> UUID.randomUUID().toString();
                              case VESSEL_IMO_NUMBER -> "9321483";
                              case CARRIER_SERVICE_CODE -> "FE1";
                              case UN_LOCATION_CODE -> "FRPAR";
                              case OPERATIONS_EVENT_TYPE_CODE -> "STRT";
                              case EVENT_CREATED_DATE_TIME,
                                      EVENT_CREATED_DATE_TIME_GTE,
                                      EVENT_CREATED_DATE_TIME_GT,
                                      EVENT_CREATED_DATE_TIME_LTE,
                                      EVENT_CREATED_DATE_TIME_LT,
                                      EVENT_CREATED_DATE_TIME_EQ ->
                                  ZonedDateTime.now()
                                      .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                              case LIMIT -> "100";
                            })));

    asyncOrchestratorPostPartyInput(
        actionPrompt.required("actionId").asText(), responseSsp.toJson());

    addOperatorLogEntry(
        "Submitting SuppliedScenarioParameters: %s"
            .formatted(responseSsp.toJson().toPrettyString()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("JitPublisher.handleRequest(%s)".formatted(request));

    JsonNode jsonResponseBody =
        JsonToolkit.templateFileToJsonNode(
            "/standards/jit/messages/jit-%s-response.json"
                .formatted(apiVersion.toLowerCase().replaceAll("[.-]", "")),
            Map.ofEntries());

    return request.createResponse(
        200,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(jsonResponseBody));
  }
}
