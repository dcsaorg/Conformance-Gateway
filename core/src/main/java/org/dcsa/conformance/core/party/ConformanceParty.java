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
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
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
  protected final BiConsumer<String, Consumer<ConformanceParty>> asyncPartyActionConsumer;
  private final ActionPromptsQueue actionPromptsQueue = new ActionPromptsQueue();

  public ConformanceParty(
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      Consumer<ConformanceRequest> asyncWebClient,
      BiConsumer<String, Consumer<ConformanceParty>> asyncPartyActionConsumer) {
    this.partyConfiguration = partyConfiguration;
    this.counterpartConfiguration = counterpartConfiguration;
    this.asyncWebClient = asyncWebClient;
    this.asyncPartyActionConsumer = asyncPartyActionConsumer;
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
    asyncWebClient.accept(
        new ConformanceRequest(
            "POST",
            partyConfiguration.getOrchestratorBaseUrl(),
            partyConfiguration.getOrchestratorRootPath()
                + "/orchestrator/party/%s/input".formatted(partyConfiguration.getName()),
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

  protected void asyncCounterpartPost(String path, String apiVersion, JsonNode jsonBody) {
    asyncWebClient.accept(
        new ConformanceRequest(
            "POST",
            counterpartConfiguration.getBaseUrl(),
            counterpartConfiguration.getRootPath()
                + "/party/%s/from/%s"
                    .formatted(counterpartConfiguration.getName(), partyConfiguration.getName())
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

  public JsonNode handleNotification() {
    log.info(
        "%s[%s].handleNotification()"
            .formatted(getClass().getSimpleName(), partyConfiguration.getName()));
    CompletableFuture.runAsync(this::getPartyPrompt)
        .exceptionally(
            e -> {
              log.error("ConformanceSandbox.asyncNotifyParty() failed: %s".formatted(e), e);
              return null;
            });
    return new ObjectMapper().createObjectNode();
  }

  @SneakyThrows
  private void getPartyPrompt() {
    log.info(
        "%s[%s].getPartyPrompt()"
            .formatted(getClass().getSimpleName(), partyConfiguration.getName()));
    String stringResponseBody =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder()
                    .uri(
                        URI.create(
                            partyConfiguration.getOrchestratorBaseUrl()
                                + partyConfiguration.getOrchestratorRootPath()
                                + "/orchestrator/party/%s/prompt/json"
                                    .formatted(partyConfiguration.getName())))
                    .timeout(Duration.ofHours(1))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString())
            .body();
    JsonNode jsonResponseBody = new ConformanceMessageBody(stringResponseBody).getJsonBody();
    if (!jsonResponseBody.isEmpty()) {
      asyncPartyActionConsumer.accept(
          getName(), newParty -> newParty._handlePartyPrompt(jsonResponseBody));
    }
  }

  private void _handlePartyPrompt(JsonNode partyPrompt) {
    StreamSupport.stream(partyPrompt.spliterator(), false).forEach(actionPromptsQueue::addLast);
    handleNextActionPrompt();
  }

  private void handleNextActionPrompt() {
    if (actionPromptsQueue.isEmpty()) return;
    JsonNode actionPrompt = actionPromptsQueue.removeFirst();
    if (actionPrompt == null) return;
    log.info(
        "%s[%s].handleNextActionPrompt() handling %s"
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
    handleNextActionPrompt();
  }

  protected abstract Map<Class<? extends ConformanceAction>, Consumer<JsonNode>>
  getActionPromptHandlers();
}
