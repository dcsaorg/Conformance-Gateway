package org.dcsa.conformance.standards.tnt.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.dcsa.conformance.standards.tnt.action.SupplyScenarioParametersAction;

@Slf4j
public class TntPublisher extends ConformanceParty {

  public TntPublisher(
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
    log.info("TntPublisher.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));

    SuppliedScenarioParameters responseSsp =
        SuppliedScenarioParameters.fromMap(
            StreamSupport.stream(
                    actionPrompt.required("tntFilterParametersQueryParamNames").spliterator(),
                    false)
                .map(
                    jsonOvsFilterParameter ->
                        TntFilterParameter.byQueryParamName.get(jsonOvsFilterParameter.asText()))
                .collect(
                    Collectors.toMap(
                        Function.identity(),
                        tntFilterParameter ->
                            switch (tntFilterParameter) {
                              case EVENT_TYPE -> "SHIPMENT,EQUIPMENT";
                              case SHIPMENT_EVENT_TYPE_CODE ->
                                  "RECE,DRFT,PENA,PENU,REJE,APPR,ISSU,SURR,SUBM,VOID,CONF,REQS,CMPL,HOLD,RELS";
                              case DOCUMENT_TYPE_CODE ->
                                  "CBR,BKG,SHI,SRM,TRD,ARN,VGM,CAS,CUS,DGD,OOG";
                              case CARRIER_BOOKING_REFERENCE -> "ABC709951";
                              case TRANSPORT_DOCUMENT_REFERENCE ->
                                  UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20);
                              case TRANSPORT_EVENT_TYPE_CODE -> "ARRI,DEPA";
                              case TRANSPORT_CALL_ID -> UUID.randomUUID().toString();
                              case VESSEL_IMO_NUMBER -> "9321483";
                              case EXPORT_VOYAGE_NUMBER -> "2103S";
                              case CARRIER_SERVICE_CODE -> "FE1";
                              case UN_LOCATION_CODE -> "FRPAR";
                              case EQUIPMENT_EVENT_TYPE_CODE ->
                                  "LOAD,DISC,GTIN,GTOT,STUF,STRP,PICK,DROP,INSP,RSEA,RMVD";
                              case EQUIPMENT_REFERENCE -> "APZU4812090";
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
    log.info("TntPublisher.handleRequest(%s)".formatted(request));

    JsonNode jsonResponseBody =
        JsonToolkit.templateFileToJsonNode(
            "/standards/tnt/messages/tnt-220-response.json", Map.ofEntries());

    return request.createResponse(
        200,
        Map.of("Api-Version", List.of(apiVersion)),
        new ConformanceMessageBody(jsonResponseBody));
  }
}
