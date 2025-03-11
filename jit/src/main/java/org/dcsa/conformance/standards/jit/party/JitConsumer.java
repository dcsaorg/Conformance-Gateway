package org.dcsa.conformance.standards.jit.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.UserFacingException;
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
import org.dcsa.conformance.standards.jit.action.JitDeclineAction;
import org.dcsa.conformance.standards.jit.action.JitGetAction;
import org.dcsa.conformance.standards.jit.action.JitOOBTimestampInputAction;
import org.dcsa.conformance.standards.jit.action.JitTimestampAction;
import org.dcsa.conformance.standards.jit.action.JitWrongTimestampAction;
import org.dcsa.conformance.standards.jit.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.jit.checks.JitChecks;
import org.dcsa.conformance.standards.jit.model.JitGetType;
import org.dcsa.conformance.standards.jit.model.JitServiceTypeSelector;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.JitTimestampType;
import org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode;

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
        Map.entry(SupplyScenarioParametersAction.class, this::supplyScenarioParameters),
        Map.entry(JitTimestampAction.class, this::timestampRequest),
        Map.entry(JitDeclineAction.class, this::declineRequest),
        Map.entry(JitOOBTimestampInputAction.class, this::outOfBandTimestampRequest),
        Map.entry(JitGetAction.class, this::sendGetActionRequest),
        Map.entry(JitWrongTimestampAction.class, this::sendWrongTimestampRequest));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info("JitConsumer.supplyScenarioParameters({})", actionPrompt.toPrettyString());

    JitServiceTypeSelector selector =
        JitServiceTypeSelector.valueOf(actionPrompt.required("selector").asText(null));
    SuppliedScenarioParameters parameters = createSuppliedScenarioParameters(selector);
    asyncOrchestratorPostPartyInput(
        actionPrompt.required("actionId").asText(), parameters.toJson());

    addOperatorLogEntry(
        "Submitted SuppliedScenarioParameters: %s".formatted(parameters.toJson().toPrettyString()));

    JitPartyHelper.flushTimestamps(persistentMap); // Prevent timestamps from being reused
  }

  public static SuppliedScenarioParameters createSuppliedScenarioParameters(
      JitServiceTypeSelector selector) {
    PortCallServiceTypeCode type =
        switch (selector) {
          case FULL_ERP -> PortCallServiceTypeCode.getServicesWithERPAndA().iterator().next();
          case S_A_PATTERN, ANY ->
              PortCallServiceTypeCode.getServicesHavingOnlyA().iterator().next();
          case GIVEN -> null;
        };
    return new SuppliedScenarioParameters(
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        type);
  }

  private void timestampRequest(JsonNode actionPrompt) {
    log.info("JitConsumer.timestampRequest({})", actionPrompt.toPrettyString());

    JitTimestampType timestampType =
        JitTimestampType.valueOf(actionPrompt.required("timestampType").asText());
    if (timestampType != JitTimestampType.REQUESTED) {
      throw new UserFacingException(
          "Only REQUESTED timestamps are supported for a Service Consumer party.");
    }

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JitTimestamp timestamp =
        JitTimestamp.getTimestampForType(timestampType, dsp.currentTimestamp(), dsp.isFYI());

    syncCounterpartPut(JitStandard.TIMESTAMP_URL + timestamp.timestampID(), timestamp.toJson());
    JitPartyHelper.storeTimestamp(persistentMap, timestamp);

    addOperatorLogEntry(
        "Submitted %s timestamp for: %s".formatted(timestampType, timestamp.dateTime()));
  }

  private void sendWrongTimestampRequest(JsonNode actionPrompt) {
    log.info(
        "{}.sendWrongTimestampRequest({})",
        getClass().getSimpleName(),
        actionPrompt.toPrettyString());

    JitTimestamp timestamp =
        JitTimestamp.getTimestampForType(JitTimestampType.REQUESTED, null, false);

    // On purpose, set a random portCallServiceID to simulate a wrong timestamp.
    timestamp = timestamp.withPortCallServiceID(UUID.randomUUID().toString());

    syncCounterpartPut(JitStandard.TIMESTAMP_URL + timestamp.timestampID(), timestamp.toJson());
    addOperatorLogEntry(
        "Submitted %s timestamp with (random) portCallServiceID: %s, timestampID: %s"
            .formatted(
                timestamp.classifierCode(),
                timestamp.portCallServiceID(),
                timestamp.timestampID()));
  }

  private void declineRequest(JsonNode actionPrompt) {
    log.info("JitConsumer.decline({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JsonNode jsonBody =
        OBJECT_MAPPER
            .createObjectNode()
            .put(JitProvider.REASON, "Declined, because crane broken.")
            .put(JitProvider.IS_FYI, dsp.isFYI());
    syncCounterpartPost(
        JitStandard.DECLINE_URL.replace(JitStandard.PORT_CALL_SERVICE_ID, dsp.portCallServiceID()),
        jsonBody);

    addOperatorLogEntry(
        "Submitted Decline for Port Call Service with ID: %s".formatted(dsp.portCallServiceID()));
  }

  private void outOfBandTimestampRequest(JsonNode actionPrompt) {
    log.info("JitConsumer.outOfBandTimestampRequest({})", actionPrompt.toPrettyString());

    JitTimestampType timestampType =
        JitTimestampType.valueOf(actionPrompt.required("timestampType").asText());

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JitTimestamp timestamp =
        JitTimestamp.getTimestampForType(timestampType, dsp.currentTimestamp(), dsp.isFYI());

    asyncOrchestratorPostPartyInput(
        actionPrompt.required("actionId").asText(),
        OBJECT_MAPPER.createObjectNode().put("timestamp", timestamp.dateTime()));
    addOperatorLogEntry(
        "Submitted Out-of-Band timestamp '%s' for type: %s"
            .formatted(timestamp.dateTime(), timestampType));
  }

  private void sendGetActionRequest(JsonNode actionPrompt) {
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

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("{}.handleRequest() type: {}", getClass().getSimpleName(), request.method());
    if (request.method().equals(JitStandard.GET)) {
      return JitPartyHelper.handleGetRequest(request, persistentMap, apiVersion, this);
    }
    int statusCode = 204;
    String url = request.url();
    JsonNode jsonBody = request.message().body().getJsonBody();
    if (jsonBody.isEmpty()) {
      addOperatorLogEntry("Handled an empty request, which is wrong.");
      statusCode = 400;
    } else if (url.contains(JitStandard.TIMESTAMP_URL)) {
      JitTimestamp timestamp = JitTimestamp.fromJson(jsonBody);

      // Check if the portCallServiceID is known, if not, return 404.
      JsonNode portCallService = persistentMap.load(JitGetType.PORT_CALL_SERVICES.name());
      if (portCallService == null
          || !timestamp
              .portCallServiceID()
              .equals(portCallService.path(JitChecks.PORT_CALL_SERVICE_ID).asText(null))) {
        return JitPartyHelper.handleWrongTimestamp(
            request, apiVersion, timestamp.portCallServiceID(), this);
      } else {
        addOperatorLogEntry(
            "Handled %s timestamp accepted for: %s and remark: %s"
                .formatted(
                    JitTimestampType.fromClassifierCode(timestamp.classifierCode()),
                    timestamp.dateTime(),
                    timestamp.remark()));
        JitPartyHelper.storeTimestamp(persistentMap, timestamp);
      }
    } else if (url.endsWith("/cancel")) {
      addOperatorLogEntry("Handled Cancel request accepted.");
    } else if (url.endsWith("/omit")) {
      addOperatorLogEntry("Handled Omit request accepted.");
    } else if (url.endsWith("/decline")) {
      addOperatorLogEntry("Handled Decline request accepted.");
    } else if (url.contains(JitStandard.PORT_CALL_URL)) {
      addOperatorLogEntry("Handled Port Call request accepted.");
      persistentMap.save(JitGetType.PORT_CALLS.name(), jsonBody);
    } else if (url.contains(JitStandard.TERMINAL_CALL_URL)) {
      addOperatorLogEntry("Handled Terminal Call request accepted.");
      persistentMap.save(JitGetType.TERMINAL_CALLS.name(), jsonBody);
    } else if (url.contains(JitStandard.PORT_CALL_SERVICES_URL)) {
      addOperatorLogEntry("Handled Port Call Service request accepted.");
      persistentMap.save(JitGetType.PORT_CALL_SERVICES.name(), jsonBody);
    } else if (url.contains(JitStandard.VESSEL_STATUS_URL)) {
      addOperatorLogEntry("Handled Vessel Status request accepted.");
      persistentMap.save(JitGetType.VESSEL_STATUSES.name(), jsonBody);
    } else {
      addOperatorLogEntry("Handled an unknown request, which is wrong.");
      statusCode = 400;
    }

    return request.createResponse(
        statusCode,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
  }
}
