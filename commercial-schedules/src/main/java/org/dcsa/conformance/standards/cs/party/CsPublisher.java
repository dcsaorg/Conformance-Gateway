package org.dcsa.conformance.standards.cs.party;

import static org.dcsa.conformance.standards.cs.party.CsFilterParameter.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.dcsa.conformance.standards.cs.action.SupplyScenarioParametersAction;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class CsPublisher extends ConformanceParty {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  public CsPublisher(String apiVersion, PartyConfiguration partyConfiguration, CounterpartConfiguration counterpartConfiguration, JsonNodeMap persistentMap, PartyWebClient webClient, Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
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
    log.info("CsPublisher.handleRequest(%s)".formatted(request));

    String filePath;
    if(request.url().endsWith("v1/point-to-point-routes")){
      filePath = "/standards/commercialschedules/messages/commercialschedules-api-1.0.0-ptp.json";
    }else if(request.url().endsWith("v1/port-schedules")){
      filePath = "/standards/commercialschedules/messages/commercialschedules-api-1.0.0-ps.json";
    }else{
      filePath = "/standards/commercialschedules/messages/commercialschedules-api-1.0.0-vs.json";
    }
    JsonNode jsonResponseBody = JsonToolkit.templateFileToJsonNode(filePath,
      Map.ofEntries());

    return request.createResponse(
      200,
      Map.of("Api-Version", List.of(apiVersion)),
      new ConformanceMessageBody(jsonResponseBody));
  }

  @Override
  protected void doReset() {

  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
      Map.entry(SupplyScenarioParametersAction.class, this::supplyScenarioParameters));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info("CsPublisher.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));

    SuppliedScenarioParameters responseSsp =
      SuppliedScenarioParameters.fromMap(
        StreamSupport.stream(
            actionPrompt.required("csFilterParametersQueryParamNames").spliterator(),
            false)
          .map(
            jsonCsFilterParameter ->
              CsFilterParameter.byQueryParamName.get(jsonCsFilterParameter.asText()))
          .collect(
            Collectors.toMap(
              Function.identity(),
              csFilterParameter ->
                switch (csFilterParameter) {
                  case CARRIER_SERVICE_NAME -> "Great Lion Service";
                  case CARRIER_SERVICE_CODE -> "FE1";
                  case UNIVERSAL_SERVICE_REFERENCE -> "SR12345A";
                  case VESSEL_IMO_NUMBER -> "9321483";
                  case VESSEL_NAME -> "King of the Seas";
                  case CARRIER_VOYAGE_NUMBER -> "2103S";
                  case UNIVERSAL_VOYAGE_REFERENCE -> "2201N";
                  case UN_LOCATION_CODE -> "NLAMS";
                  case FACILITY_SMDG_CODE -> "APM";
                  case VESSEL_OPERATOR_CARRIER_CODE -> "MAEU";
                  case START_DATE, END_DATE, DATE -> DATE_FORMAT.format(new Date());
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
}
