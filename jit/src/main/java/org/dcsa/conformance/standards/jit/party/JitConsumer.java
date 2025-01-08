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
import org.dcsa.conformance.standards.jit.action.JitOOBTimestampInputAction;
import org.dcsa.conformance.standards.jit.action.JitTimestampAction;
import org.dcsa.conformance.standards.jit.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.jit.model.JitServiceTypeSelector;
import org.dcsa.conformance.standards.jit.model.JitTimestamp;
import org.dcsa.conformance.standards.jit.model.JitTimestampType;
import org.dcsa.conformance.standards.jit.model.PortCallServiceType;

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
        Map.entry(JitOOBTimestampInputAction.class, this::outOfBandTimestampRequest));
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
  }

  public static SuppliedScenarioParameters createSuppliedScenarioParameters(
      JitServiceTypeSelector selector) {
    PortCallServiceType type =
        switch (selector) {
          case FULL_ERP -> PortCallServiceType.getServicesWithERPAndA().iterator().next();
          case S_A_PATTERN -> PortCallServiceType.getServicesHavingOnlyA().iterator().next();
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
          "Only REQUESTED timestamps are supported for a Consumer party.");
    }

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JitTimestamp timestamp =
        JitTimestamp.getTimestampForType(timestampType, dsp.currentTimestamp());

    syncCounterpartPut(
        JitStandard.PORT_CALL_SERVICES_URL + timestamp.portCallServiceID() + "/timestamp",
        timestamp.toJson());

    addOperatorLogEntry(
        "Submitted %s timestamp for: %s".formatted(timestampType, timestamp.dateTime()));
  }

  private void declineRequest(JsonNode actionPrompt) {
    log.info("JitConsumer.decline({})", actionPrompt.toPrettyString());

    DynamicScenarioParameters dsp =
        DynamicScenarioParameters.fromJson(actionPrompt.path(JitAction.DSP_TAG));
    JsonNode jsonBody =
        OBJECT_MAPPER
            .createObjectNode()
            .put("reason", "Declined, because crane broken.")
            .put("isFYI", false);
    syncCounterpartPost(
        JitStandard.DECLINE_URL.replace("{portCallServiceID}", dsp.portCallServiceID()), jsonBody);

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
        JitTimestamp.getTimestampForType(timestampType, dsp.currentTimestamp());

    asyncOrchestratorPostPartyInput(
        actionPrompt.required("actionId").asText(),
        OBJECT_MAPPER.createObjectNode().put("timestamp", timestamp.dateTime()));
    addOperatorLogEntry("Submitted Out-of-Band timestamp '%s' for type: %s".formatted(timestamp.dateTime(), timestampType));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("JitConsumer.handleRequest()");
    int statusCode = 204;
    if (request.message().body().getJsonBody().has("timestampID")) {
      JitTimestamp timestamp = JitTimestamp.fromJson(request.message().body().getJsonBody());
      addOperatorLogEntry(
          "Handled %s timestamp accepted for: %s"
              .formatted(
                  JitTimestampType.fromClassifierCode(timestamp.classifierCode()),
                  timestamp.dateTime()));
    } else if (request.url().endsWith("/cancel")) {
      addOperatorLogEntry("Handled Cancel request accepted.");
    } else if (request.url().endsWith("/omit")) {
      addOperatorLogEntry("Handled Omit request accepted.");
    } else {
      addOperatorLogEntry("Handled request accepted.");
    }

    return request.createResponse(
        statusCode,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
  }
}
