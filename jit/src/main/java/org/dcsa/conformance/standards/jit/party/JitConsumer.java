package org.dcsa.conformance.standards.jit.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
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
import org.dcsa.conformance.standards.jit.action.JitTimestampAction;
import org.dcsa.conformance.standards.jit.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.JitTimestampType;

@Slf4j
public class JitConsumer extends ConformanceParty {

  public JitConsumer(
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
        Map.entry(SupplyScenarioParametersAction.class, this::supplyScenarioParameters),
        Map.entry(JitTimestampAction.class, this::timestampRequest));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info("JitConsumer.supplyScenarioParameters({})", actionPrompt.toPrettyString());

    SuppliedScenarioParameters parameters = createSuppliedScenarioParameters();
    asyncOrchestratorPostPartyInput(
        actionPrompt.required("actionId").asText(), parameters.toJson());

    addOperatorLogEntry(
        "Submitted SuppliedScenarioParameters: %s".formatted(parameters.toJson().toPrettyString()));
  }

  public static SuppliedScenarioParameters createSuppliedScenarioParameters() {
    return new SuppliedScenarioParameters(
        UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Great Lion Service", "FE1");
  }

  private void timestampRequest(JsonNode actionPrompt) {
    log.info("JitConsumer.timestampRequest({})", actionPrompt.toPrettyString());

    JitTimestamp timestamp =
        new JitTimestamp(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            actionPrompt.required("portCallServiceID").asText(),
            LocalDateTime.now().format(JsonToolkit.DEFAULT_DATE_FORMAT) + "T07:41:00+08:30",
            "STR",
            false,
            "Port closed due to strike");

    // TODO: FROM DSP?
    JitTimestampType timestampType =
        JitTimestampType.valueOf(actionPrompt.required("timestampType").asText());

    syncCounterpartPut(
        JitStandard.PORT_CALL_SERVICES_URL + timestamp.portCallServiceID() + "/requested-timestamp",
        timestamp.toJson());

    addOperatorLogEntry("Submitted %s timestamp for: %s".formatted(timestampType, timestamp.portCallServiceDateTime()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("JitConsumer.handleRequest()");
    int statusCode = 204;
    if (request.message().body().getJsonBody().has("specification")) {
      String portCallServiceType =
          request
              .message()
              .body()
              .getJsonBody()
              .get("specification")
              .get("portCallServiceType")
              .asText();
      addOperatorLogEntry("Handled Port Call Service accepted: %s".formatted(portCallServiceType));
    } else {
      JitTimestamp jitTimestamp = null;
      try {
        jitTimestamp =
            OBJECT_MAPPER.readValue(request.message().body().getStringBody(), JitTimestamp.class);
        addOperatorLogEntry(
            "Handled Timestamp accepted for: " + jitTimestamp.portCallServiceDateTime());
      } catch (JsonProcessingException e) {
        addOperatorLogEntry("Wrong Timestamp format");
        statusCode = 400;
      }
      // TODO: verify if recieved timestamp is correct
      // {
      //  "timestampID" : "ab1a5283-ca44-4cdd-8c95-fd62726c7f1b",
      //  "replyToTimestampID" : "0b7cb1fc-be6c-4b36-bc33-b3c13e97ebd2",
      //  "portCallServiceID" : "11675cf1-39d2-430b-9b81-a5a11c9a7bbc",
      //  "portCallServiceDateTime" : "2024-11-22T07:41:00+08:30",
    }

    return request.createResponse(
        statusCode,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
  }
}
