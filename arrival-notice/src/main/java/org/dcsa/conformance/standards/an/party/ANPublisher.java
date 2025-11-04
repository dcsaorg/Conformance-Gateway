package org.dcsa.conformance.standards.an.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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
import org.dcsa.conformance.standards.an.action.PublisherPostANAction;
import org.dcsa.conformance.standards.an.action.PublisherPostANNotificationAction;
import org.dcsa.conformance.standards.an.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.an.checks.ScenarioType;

@Slf4j
public class ANPublisher extends ConformanceParty {
  public ANPublisher(String apiVersion, PartyConfiguration partyConfiguration, CounterpartConfiguration counterpartConfiguration, JsonNodeMap persistentMap, PartyWebClient webClient, Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(apiVersion, partyConfiguration, counterpartConfiguration, persistentMap, webClient, orchestratorAuthHeader);
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
  protected void doReset() {
    // no state to reset
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(SupplyScenarioParametersAction.class, this::supplyScenarioParameters),
        Map.entry(PublisherPostANAction.class, this::sendArrivalNotices),
        Map.entry(PublisherPostANNotificationAction.class, this::sendArrivalNoticeNotification));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info(
        "{}.supplyScenarioParameters({})",
        getClass().getSimpleName(),
        actionPrompt.toPrettyString());
    var scenarioType = ScenarioType.valueOf(actionPrompt.required("scenarioType").asText());
    persistentMap.save("scenarioType", JsonNodeFactory.instance.textNode(scenarioType.name()));
    ObjectNode ssp = SupplyScenarioParametersAction.examplePrompt();
    asyncOrchestratorPostPartyInput(actionPrompt.required("actionId").asText(), ssp);

    addOperatorLogEntry("Supplying scenario parameters: %s".formatted(ssp.toPrettyString()));
  }

  private void sendArrivalNotices(JsonNode actionPrompt) {
    var scenarioType = ScenarioType.valueOf(actionPrompt.required("scenarioType").asText());
    String filePath = getAnPayloadFilepath(scenarioType);
    JsonNode jsonRequestBody = JsonToolkit.templateFileToJsonNode(filePath, Map.ofEntries());
    persistentMap.save("lastArrivalNoticePayload", jsonRequestBody);
    persistentMap.save("scenarioType", JsonNodeFactory.instance.textNode(scenarioType.name()));
    syncCounterpartPost("/arrival-notices", jsonRequestBody);
    addOperatorLogEntry("Sent Arrival Notices ");
  }

  private String getAnPayloadFilepath(ScenarioType scenarioType) {

    return "/standards/an/messages/"
        + scenarioType.arrivalNoticePayload(apiVersion.toLowerCase().replaceAll("[.-]", ""));
  }

  private String getAnResponseFilepath(ScenarioType scenarioType) {

    return "/standards/an/messages/"
        + scenarioType.arrivalNoticeResponse(apiVersion.toLowerCase().replaceAll("[.-]", ""));
  }

  private void sendArrivalNoticeNotification(JsonNode actionPrompt) {
    JsonNode jsonRequestBody =
        JsonToolkit.templateFileToJsonNode(
            "/standards/an/messages/arrivalnotice-api-%s-post-notification-request.json"
                .formatted(apiVersion.toLowerCase().replaceAll("[.-]", "")),
            Map.ofEntries());
    persistentMap.save("scenarioType", JsonNodeFactory.instance.textNode("Notification"));
    persistentMap.save("lastArrivalNoticePayload", JsonNodeFactory.instance.objectNode());
    syncCounterpartPost("/arrival-notice-notifications", jsonRequestBody);
    addOperatorLogEntry("Sent Arrival Notice Notifications");
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {

    Collection<String> tdrValues = request.queryParams().get("transportDocumentReferences");
    Optional<String> tdrParamOpt =
        (tdrValues == null ? Collections.<String>emptyList() : tdrValues).stream().findFirst();

    JsonNode payload = persistentMap.load("lastArrivalNoticePayload");
    JsonNode scenarioTypeNode = persistentMap.load("scenarioType");

    Set<String> requestedTdrs =
        tdrParamOpt.map(s -> Arrays.stream(s.split(","))).stream()
            .flatMap(x -> x)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));

    String chosenTdr = requestedTdrs.stream().findFirst().orElse("");
    Map<String, String> templateVars = Map.of("TRANSPORT_DOCUMENT_REFERENCE", chosenTdr);

    if (scenarioTypeNode != null) {
      ScenarioType scenarioType = ScenarioType.valueOf(scenarioTypeNode.asText());

      if (scenarioType.name().equals("Notification")) {
        JsonNode response =
            JsonToolkit.templateFileToJsonNode(
                getAnResponseFilepath(ScenarioType.REGULAR), templateVars);
        return request.createResponse(
            200, Map.of(API_VERSION, List.of(apiVersion)), new ConformanceMessageBody(response));
      }

      if (payload == null || payload.isEmpty()) {
        String filePath = getAnResponseFilepath(scenarioType);
        payload = JsonToolkit.templateFileToJsonNode(filePath, templateVars);
      }
    }


    if (tdrParamOpt.isEmpty() || tdrParamOpt.get().isBlank()) {
      return request.createResponse(
          200, Map.of(API_VERSION, List.of(apiVersion)), new ConformanceMessageBody(payload));
    }

    ArrayNode arrivalNoticesNode = JsonNodeFactory.instance.arrayNode();
    if (payload != null
        && payload.has("arrivalNotices")
        && payload.get("arrivalNotices").isArray()) {
      for (JsonNode notice : payload.get("arrivalNotices")) {
        String tdr = notice.path("transportDocumentReference").asText(null);
        if (tdr != null && requestedTdrs.contains(tdr)) {
          arrivalNoticesNode.add(notice);
        }
      }
    }

    ObjectNode responseObject = JsonNodeFactory.instance.objectNode();
    responseObject.set("arrivalNotices", arrivalNoticesNode);

    return request.createResponse(
        200, Map.of(API_VERSION, List.of(apiVersion)), new ConformanceMessageBody(responseObject));
  }
}
