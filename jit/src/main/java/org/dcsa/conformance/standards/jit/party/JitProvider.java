package org.dcsa.conformance.standards.jit.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.jit.JitStandard;
import org.dcsa.conformance.standards.jit.action.JitAction;
import org.dcsa.conformance.standards.jit.action.JitCancelAction;
import org.dcsa.conformance.standards.jit.action.JitDeclineAction;
import org.dcsa.conformance.standards.jit.action.JitGetAction;
import org.dcsa.conformance.standards.jit.action.JitOOBTimestampAction;
import org.dcsa.conformance.standards.jit.action.JitOmitPortCallAction;
import org.dcsa.conformance.standards.jit.action.JitOmitTerminalCallAction;
import org.dcsa.conformance.standards.jit.action.JitPortCallAction;
import org.dcsa.conformance.standards.jit.action.JitPortCallServiceAction;
import org.dcsa.conformance.standards.jit.action.JitTerminalCallAction;
import org.dcsa.conformance.standards.jit.action.JitTimestampAction;
import org.dcsa.conformance.standards.jit.action.JitVesselStatusAction;
import org.dcsa.conformance.standards.jit.model.JitGetType;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.JitTimestampType;
import org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode;

@Slf4j
public class JitProvider extends ConformanceParty {

  public static final String IS_FYI = "isFYI";
  public static final String REASON = "reason";

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
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    // No state to export.
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    // No state to import.
  }

  @Override
  protected void doReset() {
    // No state to reset.
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(JitPortCallAction.class, this::portCallRequest),
        Map.entry(JitTerminalCallAction.class, this::terminalCallRequest),
        Map.entry(JitPortCallServiceAction.class, this::portCallServiceRequest),
        Map.entry(JitVesselStatusAction.class, this::vesselStatusRequest),
        Map.entry(JitTimestampAction.class, this::timestampRequest),
        Map.entry(JitOOBTimestampAction.class, this::outOfBandTimestampRequest),
        Map.entry(JitCancelAction.class, this::cancelCallRequest),
        Map.entry(JitDeclineAction.class, this::declineRequest),
        Map.entry(JitOmitPortCallAction.class, this::omitPortCallRequest),
        Map.entry(JitOmitTerminalCallAction.class, this::omitTerminalCallRequest),
        Map.entry(JitGetAction.class, this::sendGetActionRequest));
  }

  private void portCallRequest(JsonNode actionPrompt) {
    log.info("JitProvider.portCallRequest({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JsonNode jsonBody = JitPartyHelper.getFileWithReplacedPlaceHolders("port-call", dsp);
    syncCounterpartPut(JitStandard.PORT_CALL_URL + dsp.portCallID(), jsonBody);

    persistentMap.save(
        JitGetType.PORT_CALLS.name(), jsonBody); // Save the response for generating GET requests.
    JitPartyHelper.flushTimestamps(persistentMap); // Prevent timestamps from being reused (if any).

    addOperatorLogEntry(
        "Submitted Port Call request for portCallID: %s".formatted(dsp.portCallID()));
  }

  private void terminalCallRequest(JsonNode actionPrompt) {
    log.info("JitProvider.terminalCallRequest({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    if (dsp.terminalCallID() == null) {
      dsp = dsp.withTerminalCallID(UUID.randomUUID().toString());
    }
    JsonNode jsonBody = JitPartyHelper.getFileWithReplacedPlaceHolders("terminal-call", dsp);
    syncCounterpartPut(JitStandard.TERMINAL_CALL_URL + dsp.terminalCallID(), jsonBody);

    persistentMap.save(
        JitGetType.TERMINAL_CALLS.name(),
        jsonBody); // Save the response for generating GET requests.

    addOperatorLogEntry(
        "Submitted Terminal Call request for portCallID: %s and TerminalCallId: %s "
            .formatted(dsp.portCallID(), dsp.terminalCallID()));
  }

  private void portCallServiceRequest(JsonNode actionPrompt) {
    log.info("JitProvider.portCallServiceRequest({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    String serviceType;
    if (actionPrompt.has(JitPortCallServiceAction.SERVICE_TYPE)) {
      serviceType = actionPrompt.required(JitPortCallServiceAction.SERVICE_TYPE).asText();
    } else {
      serviceType = dsp.portCallServiceTypeCode().name();
    }
    dsp = dsp.withPortCallServiceTypeCode(PortCallServiceTypeCode.fromName(serviceType));
    JsonNode jsonBody = JitPartyHelper.getFileWithReplacedPlaceHolders("port-call-service", dsp);

    syncCounterpartPut(JitStandard.PORT_CALL_SERVICES_URL + dsp.portCallServiceID(), jsonBody);

    persistentMap.save(
        JitGetType.PORT_CALL_SERVICES.name(),
        jsonBody); // Save the response for generating GET requests.

    addOperatorLogEntry(
        "Submitted %s Port Call Service request with portCallServiceID: %s"
            .formatted(serviceType, dsp.portCallServiceID()));
  }

  private void vesselStatusRequest(JsonNode actionPrompt) {
    log.info("JitProvider.vesselStatusRequest({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JsonNode jsonBody = JitPartyHelper.getFileWithReplacedPlaceHolders("vessel-status", dsp);
    syncCounterpartPut(JitStandard.VESSEL_STATUS_URL + dsp.portCallServiceID(), jsonBody);

    persistentMap.save(
        JitGetType.VESSEL_STATUSES.name(),
        jsonBody); // Save the response for generating GET requests.

    addOperatorLogEntry(
        "Submitted Vessel Status for portCallServiceID: %s".formatted(dsp.portCallServiceID()));
  }

  private void timestampRequest(JsonNode actionPrompt) {
    log.info("JitProvider.timestampRequest({})", actionPrompt.toPrettyString());

    JitTimestampType timestampType =
        JitTimestampType.valueOf(actionPrompt.required("timestampType").asText());

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JitTimestamp previousTimestamp =
        dsp.currentTimestamp(); // currentTimestamp is still the value from the previous action.

    JitTimestamp timestamp =
        JitTimestamp.getTimestampForType(timestampType, previousTimestamp, dsp.isFYI());

    // Reuse portCallServiceID from previous calls
    if (previousTimestamp == null) {
      timestamp = timestamp.withPortCallServiceID(dsp.portCallServiceID());
    }

    sendTimestampPutRequest(timestampType, timestamp);

    JitPartyHelper.storeTimestamp(persistentMap, timestamp);
  }

  private void outOfBandTimestampRequest(JsonNode actionPrompt) {
    log.info("JitProvider.outOfBandTimestampRequest({})", actionPrompt.toPrettyString());

    JitTimestampType timestampType =
        JitTimestampType.valueOf(actionPrompt.required("timestampType").asText());

    asyncOrchestratorPostPartyInput(
        actionPrompt.required("actionId").asText(), OBJECT_MAPPER.createObjectNode());
    addOperatorLogEntry("Submitted Out-of-Band timestamp for: %s".formatted(timestampType));
  }

  private void cancelCallRequest(JsonNode actionPrompt) {
    log.info("JitProvider.cancelCallRequest({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JsonNode jsonBody =
        OBJECT_MAPPER
            .createObjectNode()
            .put(REASON, "Cancelled, because storm is coming.")
            .put(IS_FYI, dsp.isFYI());
    syncCounterpartPost(
        JitStandard.CANCEL_URL.replace(JitStandard.PORT_CALL_SERVICE_ID, dsp.portCallServiceID()),
        jsonBody);

    addOperatorLogEntry(
        "Submitted Cancel for Port Call Service with ID: %s".formatted(dsp.portCallServiceID()));
  }

  private void declineRequest(JsonNode actionPrompt) {
    log.info("JitProvider.decline({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JsonNode jsonBody =
        OBJECT_MAPPER
            .createObjectNode()
            .put(REASON, "Declined, because crane broken.")
            .put(IS_FYI, dsp.isFYI());
    syncCounterpartPost(
        JitStandard.DECLINE_URL.replace(JitStandard.PORT_CALL_SERVICE_ID, dsp.portCallServiceID()),
        jsonBody);

    addOperatorLogEntry(
        "Submitted Decline for Port Call Service with ID: %s".formatted(dsp.portCallServiceID()));
  }

  private void omitPortCallRequest(JsonNode actionPrompt) {
    log.info("JitProvider.omitPortCallRequest({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JsonNode jsonBody =
        OBJECT_MAPPER
            .createObjectNode()
            .put(REASON, "Omitted PC, because engine failure.")
            .put(IS_FYI, dsp.isFYI());
    syncCounterpartPost(
        JitStandard.OMIT_PORT_CALL_URL.replace(JitStandard.PORT_CALL_ID, dsp.portCallID()),
        jsonBody);

    addOperatorLogEntry("Submitted Omit Port Call with ID: %s".formatted(dsp.portCallID()));
  }

  private void omitTerminalCallRequest(JsonNode actionPrompt) {
    log.info("JitProvider.omitTerminalCallRequest({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JsonNode jsonBody =
        OBJECT_MAPPER
            .createObjectNode()
            .put(REASON, "Omitted TC, because engine failure.")
            .put(IS_FYI, dsp.isFYI());
    syncCounterpartPost(
        JitStandard.OMIT_TERMINAL_CALL_URL.replace(
            JitStandard.TERMINAL_CALL_ID, dsp.terminalCallID()),
        jsonBody);

    addOperatorLogEntry("Submitted Omit Terminal Call with ID: %s".formatted(dsp.terminalCallID()));
  }

  void sendGetActionRequest(JsonNode actionPrompt) {
    log.info(
        "{}.sendGetActionRequest({})", getClass().getSimpleName(), actionPrompt.toPrettyString());

    JitGetType getType = JitGetType.valueOf(actionPrompt.required(JitGetAction.GET_TYPE).asText());
    List<String> filters =
        OBJECT_MAPPER.convertValue(actionPrompt.get(JitGetAction.FILTERS), List.class);
    Map<String, List<String>> queryParams = new HashMap<>();
    JitPartyHelper.createParamsForPortCall(persistentMap, getType, filters, queryParams);
    JitPartyHelper.createParamsForTerminalCall(persistentMap, getType, filters, queryParams);
    JitPartyHelper.createParamsForPortCallService(persistentMap, getType, filters, queryParams);
    JitPartyHelper.createParamsForVesselStatusCall(persistentMap, getType, filters, queryParams);
    JitPartyHelper.createParamsForTimestampCall(persistentMap, getType, filters, queryParams);

    syncCounterpartGet(getType.getUrlPath(), queryParams);
    addOperatorLogEntry(
        "Submitted GET %s request with URL parameters: %s."
            .formatted(getType.getName(), queryParams));
  }

  private void sendTimestampPutRequest(
      @NonNull JitTimestampType timestampType, @NonNull JitTimestamp timestamp) {
    syncCounterpartPut(JitStandard.TIMESTAMP_URL + timestamp.timestampID(), timestamp.toJson());

    addOperatorLogEntry(
        "Submitted %s timestamp for: %s".formatted(timestampType, timestamp.dateTime()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("{}.handleRequest() type: {}", getClass().getSimpleName(), request.method());
    if (request.method().equals(JitStandard.GET)) {
      return JitPartyHelper.handleGetRequest(request, persistentMap, apiVersion, this);
    }
    int statusCode = 204;
    if (request.message().body().getJsonBody().isEmpty()) {
      addOperatorLogEntry("Handled an empty request, which is wrong.");
      statusCode = 400;
    } else if (request.url().endsWith("/decline")) {
      addOperatorLogEntry("Handled Decline request accepted.");
    } else if (request.url().contains(JitStandard.TIMESTAMP_URL)) {
      JitTimestamp timestamp = JitTimestamp.fromJson(request.message().body().getJsonBody());
      addOperatorLogEntry(
          "Handled %s timestamp accepted for: %s and remark: %s"
              .formatted(
                  JitTimestampType.fromClassifierCode(timestamp.classifierCode()),
                  timestamp.dateTime(),
                  timestamp.remark()));
      JitPartyHelper.storeTimestamp(persistentMap, timestamp);
    } else {
      statusCode = 400;
      addOperatorLogEntry("Handled an unknown request, which is wrong.");
    }
    return request.createResponse(
        statusCode,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
  }
}
