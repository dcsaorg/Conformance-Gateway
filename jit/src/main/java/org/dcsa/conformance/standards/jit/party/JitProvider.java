package org.dcsa.conformance.standards.jit.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import lombok.NonNull;
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
import org.dcsa.conformance.standards.jit.JitStandard;
import org.dcsa.conformance.standards.jit.action.JitPortCallServiceAction;
import org.dcsa.conformance.standards.jit.action.JitTimestampAction;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.JitTimestampType;

@Slf4j
public class JitProvider extends ConformanceParty {

  public JitProvider(
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
        Map.entry(JitPortCallServiceAction.class, this::portCallServiceRequest),
        Map.entry(JitTimestampAction.class, this::timestampRequest));
  }

  private void portCallServiceRequest(JsonNode actionPrompt) {
    log.info("JitProvider.portCallServiceRequest({})", actionPrompt.toPrettyString());

    String serviceType = actionPrompt.required(JitPortCallServiceAction.SERVICE_TYPE).asText();
    String portCallServiceID = UUID.randomUUID().toString();

    JsonNode jsonBody = replacePlaceHolders(portCallServiceID, serviceType);

    syncCounterpartPut(JitStandard.PORT_CALL_SERVICES_URL + portCallServiceID, jsonBody);

    addOperatorLogEntry(
        "Submitted %s Port Call Service"
            .formatted(serviceType));
  }

  private void timestampRequest(JsonNode actionPrompt) {
    log.info("JitProvider.timestampRequest({})", actionPrompt.toPrettyString());

    JitTimestampType timestampType =
        JitTimestampType.valueOf(actionPrompt.required("timestampType").asText());

    // TODO: fetch timestamp from SSP
    JitTimestamp timestamp =
        new JitTimestamp(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            actionPrompt.required("portCallServiceID").asText(),
            LocalDateTime.now().format(JsonToolkit.DEFAULT_DATE_FORMAT) + "T07:41:00+08:30",
            "STR",
            false,
            "Port closed due to strike");

    sendTimestampPutRequest(timestampType, timestamp);
  }

  private void sendTimestampPutRequest(
      @NonNull JitTimestampType timestampType, @NonNull JitTimestamp timestamp) {
    String urlPart =
        switch (timestampType) {
          case ESTIMATED -> "estimated-timestamp";
          case PLANNED -> "planned-timestamp";
          case ACTUAL -> "actual-timestamp";
          case REQUESTED -> throw new IllegalArgumentException("Requested timestamp not supported to send to a consumer");
        };

    syncCounterpartPut(
        JitStandard.PORT_CALL_SERVICES_URL + timestamp.portCallServiceID() + "/" + urlPart,
        timestamp.toJson());

    addOperatorLogEntry("Submitted %s timestamp for: %s".formatted(timestampType, timestamp.portCallServiceDateTime()));
  }

  private JsonNode replacePlaceHolders(String portCallServiceId, String serviceType) {
    return JsonToolkit.templateFileToJsonNode(
        "/standards/jit/messages/jit-200-port-call-service-request.json",
        Map.of(
            "SERVICE_TYPE_PLACEHOLDER",
            serviceType,
            "PORT_CALL_SERVICE_ID_PLACEHOLDER",
            portCallServiceId));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("JitProvider.handleRequest({})", request);

//    JsonNode jsonResponseBody =
//        JsonToolkit.templateFileToJsonNode(
//            "/standards/jit/messages/jit-%s-response.json"
//                .formatted(apiVersion.toLowerCase().replaceAll("[.-]", "")),
//            Map.ofEntries());
    addOperatorLogEntry("Provider Handled Port Call Service accepted");
    return request.createResponse(
        204,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
  }
}
