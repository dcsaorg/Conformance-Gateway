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
import org.dcsa.conformance.standards.jit.action.JitAction;
import org.dcsa.conformance.standards.jit.action.JitCancelAction;
import org.dcsa.conformance.standards.jit.action.JitPortCallAction;
import org.dcsa.conformance.standards.jit.action.JitPortCallServiceAction;
import org.dcsa.conformance.standards.jit.action.JitTerminalCallAction;
import org.dcsa.conformance.standards.jit.action.JitTimestampAction;
import org.dcsa.conformance.standards.jit.action.JitVesselStatusAction;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.JitTimestampType;
import org.dcsa.conformance.standards.jit.model.PortCallPhaseTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceEventTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceType;

@Slf4j
public class JitProvider extends ConformanceParty {

  @SuppressWarnings("java:S2245") // Random is used for generating random timestamps. Secure enough.
  private static final Random RANDOM = new Random();

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
        Map.entry(JitCancelAction.class, this::cancelCallRequest));
  }

  private void portCallRequest(JsonNode actionPrompt) {
    log.info("JitProvider.portCallRequest({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp = DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JsonNode jsonBody = replacePlaceHolders("port-call", dsp);
    syncCounterpartPut(JitStandard.PORT_CALL_URL + dsp.portCallID(), jsonBody);

    addOperatorLogEntry(
        "Submitted Port Call request for portCallID: %s".formatted(dsp.portCallID()));
  }

  private void terminalCallRequest(JsonNode actionPrompt) {
    log.info("JitProvider.terminalCallRequest({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp = DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    if (dsp.terminalCallID() == null) {
      dsp = dsp.withTerminalCallID(UUID.randomUUID().toString());
    }
    JsonNode jsonBody = replacePlaceHolders("terminal-call", dsp);
    syncCounterpartPut(JitStandard.TERMINAL_CALL_URL + dsp.terminalCallID(), jsonBody);

    addOperatorLogEntry(
        "Submitted Terminal Call request for portCallID: %s".formatted(dsp.portCallID()));
  }

  private void portCallServiceRequest(JsonNode actionPrompt) {
    log.info("JitProvider.portCallServiceRequest({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp = DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    String serviceType;
    if (actionPrompt.has(JitPortCallServiceAction.SERVICE_TYPE)) {
      serviceType = actionPrompt.required(JitPortCallServiceAction.SERVICE_TYPE).asText();
    } else {
      serviceType = dsp.portCallServiceType().name();
    }
    dsp = dsp.withPortCallServiceType(PortCallServiceType.fromName(serviceType));
    JsonNode jsonBody = replacePlaceHolders("port-call-service", dsp);

    // Only MOVES service type requires the Moves part of the request. Removing it from the others.
    if (dsp.portCallServiceType() != PortCallServiceType.MOVES) {
      ((ObjectNode) jsonBody).remove("moves");
    }

    syncCounterpartPut(JitStandard.PORT_CALL_SERVICES_URL + dsp.portCallServiceID(), jsonBody);

    addOperatorLogEntry("Submitted %s Port Call Service request".formatted(serviceType));
  }

  private void vesselStatusRequest(JsonNode actionPrompt) {
    log.info("JitProvider.vesselStatusRequest({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp = DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JsonNode jsonBody = replacePlaceHolders("vessel-status", dsp);
    syncCounterpartPut(JitStandard.VESSEL_STATUS_URL + dsp.portCallServiceID(), jsonBody);

    addOperatorLogEntry(
        "Submitted Vessel Status for portCallServiceID: %s".formatted(dsp.portCallServiceID()));
  }

  private void timestampRequest(JsonNode actionPrompt) {
    log.info("JitProvider.timestampRequest({})", actionPrompt.toPrettyString());

    JitTimestampType timestampType =
        JitTimestampType.valueOf(actionPrompt.required("timestampType").asText());

    DynamicScenarioParameters dsp = DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JitTimestamp previousTimestamp =
        dsp.currentTimestamp(); // currentTimestamp is still the value from the previous action.

    // Create values for the first timestamp in the sequence.
    if (previousTimestamp == null) {
      previousTimestamp = getTimestampForType(JitTimestampType.ESTIMATED, null);
      if (dsp.portCallServiceID() != null)
        previousTimestamp = previousTimestamp.withPortCallServiceID(dsp.portCallServiceID());
    }
    JitTimestamp timestamp = getTimestampForType(timestampType, previousTimestamp);
    sendTimestampPutRequest(timestampType, timestamp);
  }

  private void cancelCallRequest(JsonNode actionPrompt) {
    log.info("JitProvider.cancelCallRequest({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp = DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JsonNode jsonBody =
        OBJECT_MAPPER
            .createObjectNode()
            .put("reason", "Cancelled, because storm is coming.")
            .put("isFYI", false);
    syncCounterpartPost(
        JitStandard.CANCEL_URL.replace("{portCallServiceID}", dsp.portCallServiceID()), jsonBody);

    addOperatorLogEntry(
        "Submitted Cancel for Port Call Service with ID: %s".formatted(dsp.portCallServiceID()));
  }

  static JitTimestamp getTimestampForType(
      JitTimestampType timestampType, JitTimestamp previousTimestamp) {
    return switch (timestampType) {
      case ESTIMATED ->
          new JitTimestamp(
              UUID.randomUUID().toString(),
              previousTimestamp != null ? previousTimestamp.timestampID() : null,
              previousTimestamp != null
                  ? previousTimestamp.portCallServiceID()
                  : UUID.randomUUID().toString(),
              timestampType.getClassifierCode(),
              LocalDateTime.now().format(JsonToolkit.DEFAULT_DATE_FORMAT) + "T07:41:00+08:30",
              "STR",
              false,
              "Port closed due to strike");
      case PLANNED, ACTUAL ->
          previousTimestamp.withClassifierCode(timestampType.getClassifierCode());
      case REQUESTED ->
          previousTimestamp
              .withClassifierCode(timestampType.getClassifierCode())
              .withTimestampID(
                  UUID.randomUUID().toString()) // Create new ID, because it's a new timestamp
              .withReplyToTimestampID(
                  previousTimestamp.timestampID()) // Respond to the previous timestamp
              .withDateTime(generateRandomDateTime());
    };
  }

  // Random date/time in the future (3 - 7 hours from now), with a random offset of up to 4 hours.
  private static String generateRandomDateTime() {
    return LocalDateTime.now()
        .plusHours(3)
        .plusSeconds(RANDOM.nextInt(60 * 60 * 4))
        .format(JsonToolkit.ISO_8601_DATE_TIME_FORMAT);
  }

  private void sendTimestampPutRequest(
      @NonNull JitTimestampType timestampType, @NonNull JitTimestamp timestamp) {
    syncCounterpartPut(JitStandard.TIMESTAMP_URL + timestamp.timestampID(), timestamp.toJson());

    addOperatorLogEntry(
        "Submitted %s timestamp for: %s".formatted(timestampType, timestamp.dateTime()));
  }

  private JsonNode replacePlaceHolders(String fileType, DynamicScenarioParameters dsp) {
    PortCallServiceType serviceType = dsp.portCallServiceType();
    String portCallPhaseTypeCode = "";
    String portCallServiceEventTypeCode = "";
    if (serviceType != null) {
      portCallPhaseTypeCode = calculatePortCallPhaseTypeCode(serviceType.name());
      portCallServiceEventTypeCode =
          PortCallServiceEventTypeCode.getCodesForPortCallServiceType(serviceType.name())
              .getFirst()
              .name();
    }

    JsonNode jsonNode =
        JsonToolkit.templateFileToJsonNode(
            "/standards/jit/messages/jit-200-%s-request.json".formatted(fileType),
            Map.of(
                "PORT_CALL_ID_PLACEHOLDER",
                Objects.requireNonNullElse(dsp.portCallID(), ""),
                "TERMINAL_CALL_ID_PLACEHOLDER",
                Objects.requireNonNullElse(dsp.terminalCallID(), ""),
                "PORT_CALL_SERVICE_TYPE_PLACEHOLDER",
                serviceType != null ? serviceType.name() : "",
                "PORT_CALL_SERVICE_ID_PLACEHOLDER",
                Objects.requireNonNullElse(dsp.portCallServiceID(), ""),
                "PORT_CALL_SERVICE_EVENT_TYPE_CODE_PLACEHOLDER",
                portCallServiceEventTypeCode,
                "PORT_CALL_PHASE_TYPE_CODE_PLACEHOLDER",
                portCallPhaseTypeCode));
    // Some serviceType do not have a portCallPhaseTypeCode; remove it, since it is an enum.
    if (serviceType != null && portCallPhaseTypeCode.isEmpty())
      ((ObjectNode) jsonNode).remove("portCallPhaseTypeCode");
    return jsonNode;
  }

  private static String calculatePortCallPhaseTypeCode(String serviceType) {
    List<PortCallPhaseTypeCode> typeCodes =
        PortCallPhaseTypeCode.getCodesForPortCallServiceType(serviceType);
    if (typeCodes.isEmpty()) {
      return "";
    }
    return typeCodes.getFirst().name();
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("JitProvider.handleRequest({})", request);

    JitTimestamp timestamp = JitTimestamp.fromJson(request.message().body().getJsonBody());

    addOperatorLogEntry(
        "Handled %s timestamp accepted for: %s and remark: %s"
            .formatted(
                JitTimestampType.fromClassifierCode(timestamp.classifierCode()),
                timestamp.dateTime(),
                timestamp.remark()));

    return request.createResponse(
        204,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
  }
}
