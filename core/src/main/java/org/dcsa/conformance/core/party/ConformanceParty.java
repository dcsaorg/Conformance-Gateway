package org.dcsa.conformance.core.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.StatefulEntity;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;

@Slf4j
public abstract class ConformanceParty implements StatefulEntity {
  protected final PartyConfiguration partyConfiguration;
  protected final CounterpartConfiguration counterpartConfiguration;

  protected final Consumer<ConformanceRequest> asyncWebClient;
  private final ActionPromptsQueue actionPromptsQueue = new ActionPromptsQueue();

  public ConformanceParty(
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      Consumer<ConformanceRequest> asyncWebClient) {
    this.partyConfiguration = partyConfiguration;
    this.counterpartConfiguration = counterpartConfiguration;
    this.asyncWebClient = asyncWebClient;
  }

  @Override
  public JsonNode exportJsonState() {
    ObjectNode jsonPartyState = new ObjectMapper().createObjectNode();
    jsonPartyState.set("actionPromptsQueue", actionPromptsQueue.exportJsonState());
    exportPartyJsonState(jsonPartyState);
    return jsonPartyState;
  }

  protected abstract void exportPartyJsonState(ObjectNode targetObjectNode);

  @Override
  public void importJsonState(JsonNode jsonState) {
    actionPromptsQueue.importJsonState(jsonState.get("actionPromptsQueue"));
    importPartyJsonState((ObjectNode) jsonState);
  }

  protected abstract void importPartyJsonState(ObjectNode sourceObjectNode);

  public String getName() {
    return partyConfiguration.getName();
  }

  public String getRole() {
    return partyConfiguration.getRole();
  }

  public String getCounterpartName() {
    return counterpartConfiguration.getName();
  }

  public String getCounterpartRole() {
    return counterpartConfiguration.getRole();
  }

  protected void asyncOrchestratorPostPartyInput(JsonNode jsonPartyInput) {
    if (partyConfiguration.isManualMode()) {
      log.info(
          "Party %s is in manual mode and will NOT post its input automatically: %s"
              .formatted(partyConfiguration.getName(), jsonPartyInput.toPrettyString()));
      return;
    }
    asyncWebClient.accept(
        new ConformanceRequest(
            "POST",
            partyConfiguration.getOrchestratorBaseUrl(),
            partyConfiguration.getOrchestratorRootPath()
                + "/party/%s/input".formatted(partyConfiguration.getName()),
            Collections.emptyMap(),
            new ConformanceMessage(
                partyConfiguration.getName(),
                partyConfiguration.getRole(),
                "orchestrator",
                "orchestrator",
                Collections.emptyMap(),
                new ConformanceMessageBody(jsonPartyInput),
                System.currentTimeMillis())));
  }

  @SuppressWarnings("unused")
  private void useDifferentApiVersion() {
    asyncCounterpartPost("", "", new ObjectMapper().createObjectNode());
  }

  protected void asyncCounterpartPost(String path, String apiVersion, JsonNode jsonBody) {
    asyncWebClient.accept(
        new ConformanceRequest(
            "POST",
            counterpartConfiguration.getBaseUrl(),
            counterpartConfiguration.getRootPath()
                + "/party/%s/api".formatted(counterpartConfiguration.getName())
                + path,
            Collections.emptyMap(),
            new ConformanceMessage(
                partyConfiguration.getName(),
                partyConfiguration.getRole(),
                counterpartConfiguration.getName(),
                counterpartConfiguration.getRole(),
                Map.of("Api-Version", List.of(apiVersion)),
                new ConformanceMessageBody(jsonBody),
                System.currentTimeMillis())));
  }

  public abstract ConformanceResponse handleRequest(ConformanceRequest request);

  public void handleNotification() {
    log.info(
        "%s[%s].handleNotification()"
            .formatted(getClass().getSimpleName(), partyConfiguration.getName()));
    JsonNode partyPrompt = _syncGetPartyPrompt();
    if (!partyPrompt.isEmpty()) {
      StreamSupport.stream(partyPrompt.spliterator(), false).forEach(actionPromptsQueue::addLast);
      _handleNextActionPrompt();
    }
  }

  @SneakyThrows
  private JsonNode _syncGetPartyPrompt() {
    log.info(
        "%s[%s].getPartyPrompt()"
            .formatted(getClass().getSimpleName(), partyConfiguration.getName()));
    URI uri =
        URI.create(
            partyConfiguration.getOrchestratorBaseUrl()
                + partyConfiguration.getOrchestratorRootPath()
                + "/party/%s/prompt/json".formatted(partyConfiguration.getName()));
    log.info("ConformanceParty.getPartyPrompt() calling: %s".formatted(uri));
    String stringResponseBody =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder().uri(uri).timeout(Duration.ofHours(1)).GET().build(),
                HttpResponse.BodyHandlers.ofString())
            .body();
    return new ConformanceMessageBody(stringResponseBody).getJsonBody();
  }

  private void _handleNextActionPrompt() {
    if (actionPromptsQueue.isEmpty()) return;
    JsonNode actionPrompt = actionPromptsQueue.removeFirst();
    if (actionPrompt == null) return;
    log.info(
        "%s[%s]._handleNextActionPrompt() handling %s"
            .formatted(
                getClass().getSimpleName(),
                partyConfiguration.getName(),
                actionPrompt.toPrettyString()));
    getActionPromptHandlers().entrySet().stream()
        .filter(
            entry ->
                Objects.equals(
                    entry.getKey().getCanonicalName(), actionPrompt.get("actionType").asText()))
        .findFirst()
        .orElseThrow(
            () ->
                new RuntimeException(
                    "Handler not found by %s for action prompt %s\nAvailable action prompts are %s"
                        .formatted(
                            ConformanceParty.this.getClass().getCanonicalName(),
                            actionPrompt.toPrettyString(),
                            getActionPromptHandlers().keySet())))
        .getValue()
        .accept(actionPrompt);
    _handleNextActionPrompt();
  }

  protected abstract Map<Class<? extends ConformanceAction>, Consumer<JsonNode>>
      getActionPromptHandlers();
}
