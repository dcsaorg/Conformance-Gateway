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
import org.dcsa.conformance.standards.jit.model.PortCallPhaseTypeCode;
import org.dcsa.conformance.standards.jit.model.PortCallServiceEventTypeCode;

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

    DynamicScenarioParameters dsp = DynamicScenarioParameters.fromJson(actionPrompt.path("dsp"));

    String serviceType = actionPrompt.required(JitPortCallServiceAction.SERVICE_TYPE).asText();
    JsonNode jsonBody = replacePlaceHolders(dsp.portCallID(), dsp.portCallServiceID(), serviceType);

    syncCounterpartPut(JitStandard.PORT_CALL_SERVICES_URL + dsp.portCallServiceID(), jsonBody);

    addOperatorLogEntry("Submitted %s Port Call Service".formatted(serviceType));
  }

  private void timestampRequest(JsonNode actionPrompt) {
    log.info("JitProvider.timestampRequest({})", actionPrompt.toPrettyString());

    JitTimestampType timestampType =
        JitTimestampType.valueOf(actionPrompt.required("timestampType").asText());

    DynamicScenarioParameters dsp = DynamicScenarioParameters.fromJson(actionPrompt.path("dsp"));
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
              .withDateTime(
                  LocalDateTime.now().format(JsonToolkit.DEFAULT_DATE_FORMAT) + "T16:16:16+08:30");
    };
  }

  private void sendTimestampPutRequest(
      @NonNull JitTimestampType timestampType, @NonNull JitTimestamp timestamp) {
    syncCounterpartPut(
        JitStandard.PORT_CALL_SERVICES_URL + timestamp.portCallServiceID() + "/timestamp",
        timestamp.toJson());

    addOperatorLogEntry(
        "Submitted %s timestamp for: %s".formatted(timestampType, timestamp.dateTime()));
  }

  private JsonNode replacePlaceHolders(
      String portCallId, String portCallServiceId, String serviceType) {
    String portCallPhaseTypeCode = calculatePortCallPhaseTypeCode(serviceType);
    JsonNode jsonNode =
        JsonToolkit.templateFileToJsonNode(
            "/standards/jit/messages/jit-200-port-call-service-request.json",
            Map.of(
                "PORT_CALL_ID_PLACEHOLDER",
                portCallId,
                "SERVICE_TYPE_PLACEHOLDER",
                serviceType,
                "PORT_CALL_SERVICE_ID_PLACEHOLDER",
                portCallServiceId,
                "PORT_CALL_SERVICE_EVENT_TYPE_CODE_PLACEHOLDER",
                PortCallServiceEventTypeCode.getCodesForPortCallServiceType(serviceType)
                    .getFirst()
                    .name(),
                "PORT_CALL_PHASE_TYPE_CODE_PLACEHOLDER",
                portCallPhaseTypeCode));
    // Some serviceType do not have a portCallPhaseTypeCode; remove it, since it is an enum.
    if (portCallPhaseTypeCode.isEmpty())
      ((ObjectNode) jsonNode.path("specification")).remove("portCallPhaseTypeCode");
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
        "Handled %s timestamp accepted for date/time: %s and remark: %s"
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
