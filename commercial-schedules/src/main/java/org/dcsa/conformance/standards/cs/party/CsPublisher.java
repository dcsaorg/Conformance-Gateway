package org.dcsa.conformance.standards.cs.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.dcsa.conformance.standards.cs.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.cs.model.CsDateUtils;

@Slf4j
public class CsPublisher extends ConformanceParty {

  public CsPublisher(
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
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("CsPublisher.handleRequest(%s)".formatted(request));
    String filePath;
    Map<String, List<String>> initialIMap = Map.of("Api-Version", List.of(apiVersion));
    Map<String, Collection<String>> headers = new HashMap<>(initialIMap);
    if (request.queryParams().containsKey("limit")
        && !request.queryParams().containsKey("cursor")) {
      String cursor = "fE9mZnNldHw9MTAmbGltaXQ9MTA";
      headers.put("Next-Page-Cursor", List.of(cursor));
    }
    boolean hasCursor = request.queryParams().containsKey("cursor");

    String baseFilePath = "/standards/commercialschedules/messages/commercialschedules-api-1.0.0-";
    String suffix = hasCursor ? "nextpage.json" : ".json";
    String routeType = "vs";

    if (request.url().endsWith("v1/point-to-point-routes")) {
      routeType = "ptp";
    } else if (request.url().endsWith("v1/port-schedules")) {
      routeType = "ps";
    }

    filePath = baseFilePath + routeType + suffix;

    JsonNode jsonResponseBody = replacePlaceHolders(filePath, request);
    return request.createResponse(200, headers, new ConformanceMessageBody(jsonResponseBody));
  }

  private JsonNode replacePlaceHolders(String filePath, ConformanceRequest request) {
    Map<String, String> entryMap = new HashMap<>();

    String url = request.url();

    if (url.endsWith("v1/point-to-point-routes")) {
      CsDateUtils.handleArrivalAndDepartureDates(entryMap, request.queryParams());
    } else if (url.endsWith("v1/port-schedules")) {
      CsDateUtils.handleSingleDate(entryMap, request.queryParams());
    }
    return JsonToolkit.templateFileToJsonNode(filePath, entryMap);
  }

  @Override
  protected void doReset() {
    // no state to reset
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
                    actionPrompt.required("csFilterParametersQueryParamNames").spliterator(), false)
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
                              case UN_LOCATION_CODE, PLACE_OF_RECEIPT, PLACE_OF_DELIVERY -> "NLAMS";
                              case FACILITY_SMDG_CODE -> "APM";
                              case VESSEL_OPERATOR_CARRIER_CODE -> "MAEU";
                              case DATE, DEPARTURE_START_DATE, ARRIVAL_START_DATE ->
                                  CsDateUtils.getCurrentDate();
                              case DEPARTURE_END_DATE, ARRIVAL_END_DATE ->
                                  CsDateUtils.getEndDateAfter3Months();
                              case MAX_TRANSHIPMENT -> "1";
                              case RECEIPT_TYPE_AT_ORIGIN, DELIVERY_TYPE_AT_DESTINATION -> "CY";
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
}
