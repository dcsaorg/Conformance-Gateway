package org.dcsa.conformance.standards.portcall.party;

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
import org.dcsa.conformance.standards.portcall.JitStandard;
import org.dcsa.conformance.standards.portcall.action.PortCallAction;
import org.dcsa.conformance.standards.portcall.action.PortCallPostAction;
import org.dcsa.conformance.standards.portcall.model.JitGetType;
import org.dcsa.conformance.standards.portcall.model.JitTimestamp;
import org.dcsa.conformance.standards.portcall.model.JitTimestampType;
import org.dcsa.conformance.standards.portcall.model.PortCallServiceTypeCode;

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
    return Map.ofEntries(Map.entry(PortCallPostAction.class, this::portCallServiceRequest));
  }



  private void portCallServiceRequest(JsonNode actionPrompt) {
    log.info("JitProvider.portCallServiceRequest({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(PortCallAction.DSP_TAG));
    String serviceType;
    if (actionPrompt.has(PortCallPostAction.SERVICE_TYPE)) {
      serviceType = actionPrompt.required(PortCallPostAction.SERVICE_TYPE).asText();
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
