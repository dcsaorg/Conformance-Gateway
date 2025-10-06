package org.dcsa.conformance.standards.an.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
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
import org.dcsa.conformance.standards.an.action.PublisherPostANAction;
import org.dcsa.conformance.standards.an.action.PublisherPostANNotificationAction;
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
        Map.entry(PublisherPostANAction.class, this::sendArrivalNotices),
        Map.entry(PublisherPostANNotificationAction.class, this::sendArrivalNoticeNotification));
  }

  private void sendArrivalNotices(JsonNode actionPrompt) {
    var scenarioType = ScenarioType.valueOf(actionPrompt.required("scenarioType").asText());
    String filePath = getAnPayloadFilepath(scenarioType);
    JsonNode jsonRequestBody = JsonToolkit.templateFileToJsonNode(filePath, Map.ofEntries());
    syncCounterpartPost("/arrival-notices", jsonRequestBody);
    addOperatorLogEntry("Sent Arrival Notices ");
  }

  private String getAnPayloadFilepath(ScenarioType scenarioType) {

    return "/standards/an/messages/"
        + scenarioType.arrivalNoticePayload(apiVersion.toLowerCase().replaceAll("[.-]", ""));
  }

  private void sendArrivalNoticeNotification(JsonNode actionPrompt) {
    JsonNode jsonRequestBody =
        JsonToolkit.templateFileToJsonNode(
            "/standards/an/messages/arrivalnotice-api-%s-post-notification-request.json"
                .formatted(apiVersion.toLowerCase().replaceAll("[.-]", "")),
            Map.ofEntries());
    syncCounterpartPost("/arrival-notice-notifications", jsonRequestBody);
    addOperatorLogEntry("Sent Arrival Notice Notifications");
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    Optional<String> tdr =
        request.queryParams().get("transportDocumentReferences").stream().findFirst();

    String transportDocumentReference =
        tdr.orElseThrow(
            () ->
                new NoSuchElementException(
                    "No transportDocumentReferences present in the query param"));

    JsonNode jsonResponseBody =
        JsonToolkit.templateFileToJsonNode(
            "/standards/an/messages/arrivalnotice-api-%s-get-response.json"
                .formatted(apiVersion.toLowerCase().replaceAll("[.-]", "")),
            Map.of("TRANSPORT_DOCUMENT_REFERENCE", transportDocumentReference));

    return request.createResponse(
        200,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(jsonResponseBody));
  }
}
