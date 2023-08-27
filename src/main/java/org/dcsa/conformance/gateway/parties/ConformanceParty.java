package org.dcsa.conformance.gateway.parties;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@Getter
@Slf4j
public abstract class ConformanceParty {
  protected final String name;
  private final boolean internal;
  protected final String gatewayBaseUrl;
  protected final String gatewayRootPath;
  private final ActionPromptsQueue actionPromptsQueue = new ActionPromptsQueue();

  public ConformanceParty(
      String name, boolean internal, String gatewayBaseUrl, String gatewayRootPath) {
    this.name = name;
    this.internal = internal;
    this.gatewayBaseUrl = gatewayBaseUrl;
    this.gatewayRootPath = gatewayRootPath;
  }

  public void handleNotification() {
    log.info("%s[%s].handleNotification()".formatted(getClass().getSimpleName(), name));
    asyncGet(
        "/party/%s/prompt/json".formatted(name),
        prompt -> {
          StreamSupport.stream(prompt.spliterator(), false).forEach(actionPromptsQueue::addLast);
          asyncHandleNextActionPrompt();
        });
  }

  public abstract ResponseEntity<JsonNode> handleRegularTraffic(JsonNode requestBody);

  protected abstract Map<Class<? extends ConformanceAction>, Consumer<JsonNode>>
      getActionPromptHandlers();

  private void asyncHandleNextActionPrompt() {
    CompletableFuture.runAsync(
            () -> {
              JsonNode actionPrompt = actionPromptsQueue.removeFirst();
              if (actionPrompt == null) return;
              log.info(
                  "%s[%s].asyncHandleNextActionPrompt() handling %s"
                      .formatted(getClass().getSimpleName(), name, actionPrompt.toPrettyString()));
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
                      .formatted(getClass().getSimpleName(), name, e),
                  e);
              return null;
            });
  }

  private void asyncGet(String uri, Consumer<JsonNode> responseBodyConsumer) {
    CompletableFuture.runAsync(
            () -> {
              JsonNode responseBody =
                  WebTestClient.bindToServer()
                      .responseTimeout(Duration.ofHours(1))
                      .baseUrl(gatewayBaseUrl)
                      .build()
                      .get()
                      .uri(uri)
                      .exchange()
                      .expectBody(JsonNode.class)
                      .returnResult()
                      .getResponseBody();
              log.info(
                  "%s[%s].asyncGet(%s): %s"
                      .formatted(
                          getClass().getSimpleName(),
                          name,
                          uri,
                          Objects.requireNonNull(responseBody).toPrettyString()));
              responseBodyConsumer.accept(responseBody);
            })
        .exceptionally(
            e -> {
              log.error(
                  "%s[%s].asyncGet(gatewayBaseUrl='%s', uri='%s') failed: %s"
                      .formatted(getClass().getSimpleName(), name, gatewayBaseUrl, uri, e),
                  e);
              return null;
            });
  }

  protected void asyncPost(String uri, String apiVersion, JsonNode requestBody) {
    CompletableFuture.runAsync(
            () -> {
              log.info(
                  "%s[%s].asyncPost(%s, %s)"
                      .formatted(
                          getClass().getSimpleName(), name, uri, requestBody.toPrettyString()));
              WebTestClient.bindToServer()
                  .responseTimeout(Duration.ofHours(1))
                  .baseUrl(gatewayBaseUrl)
                  .build()
                  .post()
                  .uri(uri)
                  .header("Api-Version", apiVersion)
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(Mono.just(requestBody), JsonNode.class)
                  .exchange();
            })
        .exceptionally(
            e -> {
              log.error(
                  "%s[%s].asyncPost(gatewayBaseUrl='%s', uri='%s') failed: %s"
                      .formatted(getClass().getSimpleName(), name, gatewayBaseUrl, uri, e),
                  e);
              return null;
            });
  }
}
