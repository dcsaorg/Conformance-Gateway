package org.dcsa.conformance.core.party;

import com.fasterxml.jackson.databind.JsonNode;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;

@Slf4j
public abstract class ConformanceParty {
  protected final PartyConfiguration partyConfiguration;
  protected final CounterpartConfiguration counterpartConfiguration;
  protected final BiConsumer<ConformanceRequest, Consumer<ConformanceResponse>> asyncWebClient;
  private final ActionPromptsQueue actionPromptsQueue = new ActionPromptsQueue();

  public ConformanceParty(
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      BiConsumer<ConformanceRequest, Consumer<ConformanceResponse>> asyncWebClient) {
    this.partyConfiguration = partyConfiguration;
    this.counterpartConfiguration = counterpartConfiguration;
    this.asyncWebClient = asyncWebClient;
  }

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
                System.currentTimeMillis())),
        conformanceResponse -> {});
  }

  protected void asyncCounterpartPost(
      String path,
      String apiVersion,
      JsonNode jsonBody,
      Consumer<ConformanceResponse> responseConsumer) {
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
                System.currentTimeMillis())),
        responseConsumer);
  }

  public abstract ConformanceResponse handleRequest(ConformanceRequest request);

  public JsonNode handleNotification() {
    CompletableFuture.runAsync(
            () -> {
              log.info(
                  "%s[%s].handleNotification()"
                      .formatted(getClass().getSimpleName(), partyConfiguration.getName()));
              JsonNode jsonResponseBody = syncGetPartyPrompt();
              StreamSupport.stream(jsonResponseBody.spliterator(), false)
                  .forEach(actionPromptsQueue::addLast);
              asyncHandleNextActionPrompt();
            })
        .exceptionally(
            e -> {
              log.error(
                  "Failed to get prompt for party '%s': %s"
                      .formatted(partyConfiguration.getName(), e),
                  e);
              return null;
            });
    return new ObjectMapper().createObjectNode();
  }

  @SneakyThrows
  private JsonNode syncGetPartyPrompt() {
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
    return new ConformanceMessageBody(stringResponseBody).getJsonBody();
  }

  protected abstract Map<Class<? extends ConformanceAction>, Consumer<JsonNode>>
      getActionPromptHandlers();

  private void asyncHandleNextActionPrompt() {
    CompletableFuture.runAsync(
            () -> {
              JsonNode actionPrompt = actionPromptsQueue.removeFirst();
              if (actionPrompt == null) return;
              log.info(
                  "%s[%s].asyncHandleNextActionPrompt() handling %s"
                      .formatted(
                          getClass().getSimpleName(),
                          partyConfiguration.getName(),
                          actionPrompt.toPrettyString()));
              getActionPromptHandlers().entrySet().stream()
                  .filter(
                      entry ->
                          Objects.equals(
                              entry.getKey().getCanonicalName(),
                              actionPrompt.get("actionType").asText()))
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
              asyncHandleNextActionPrompt();
            })
        .exceptionally(
            e -> {
              log.error(
                  "%s[%s].asyncHandleNextActionPrompt() failed: %s"
                      .formatted(getClass().getSimpleName(), partyConfiguration.getName(), e),
                  e);
              return null;
            });
  }
}
