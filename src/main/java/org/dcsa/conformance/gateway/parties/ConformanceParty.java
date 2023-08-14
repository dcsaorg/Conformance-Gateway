package org.dcsa.conformance.gateway.parties;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;
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

@Slf4j
public abstract class ConformanceParty {
  @Getter protected final String name;
  @Getter private final boolean internal;
  protected final String gatewayBaseUrl;
  protected final String gatewayRootPath;

  public ConformanceParty(
      String name, boolean internal, String gatewayBaseUrl, String gatewayRootPath) {
    this.name = name;
    this.internal = internal;
    this.gatewayBaseUrl = gatewayBaseUrl;
    this.gatewayRootPath = gatewayRootPath;
  }

  public void handleNotification() {
    getAsync(
        "/party/%s/prompt/json".formatted(name),
        prompt -> {
          StreamSupport.stream(prompt.spliterator(), false).forEach(this::handleActionPrompt);
        });
  }

  public abstract ResponseEntity<JsonNode> handlePostRequest(JsonNode requestBody);

  protected abstract Map<Class<? extends ConformanceAction>, Consumer<JsonNode>>
      getActionPromptHandlers();

  public void handleActionPrompt(JsonNode actionPrompt) {
    String actionType = actionPrompt.get("actionType").asText();
    getActionPromptHandlers().entrySet().stream()
        .filter(entry -> Objects.equals(entry.getKey().getCanonicalName(), actionType))
        .findFirst()
        .orElseThrow()
        .getValue()
        .accept(actionPrompt);
  }

  private void getAsync(String uri, Consumer<JsonNode> responseBodyConsumer) {
    CompletableFuture.runAsync(
            () -> {
              responseBodyConsumer.accept(
                  WebTestClient.bindToServer()
                      .baseUrl(gatewayBaseUrl)
                      .build()
                      .get()
                      .uri(uri)
                      .exchange()
                      .expectBody(JsonNode.class)
                      .returnResult()
                      .getResponseBody());
            })
        .exceptionally(
            e -> {
              log.error(
                  "ConformanceParty.getAsync(gatewayBaseUrl='%s', uri='%s') failed: %s".formatted(gatewayBaseUrl, uri, e),
                  e);
              return null;
            });
  }

  protected void postAsync(String uri, JsonNode requestBody) {
    CompletableFuture.runAsync(
            () -> {
              WebTestClient.bindToServer()
                  .baseUrl(gatewayBaseUrl)
                  .build()
                  .post()
                  .uri(uri)
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(Mono.just(requestBody), JsonNode.class)
                  .exchange()
                  .expectStatus()
                  .is2xxSuccessful();
            })
        .exceptionally(
            e -> {
              log.error(
                      "ConformanceParty.postAsync(gatewayBaseUrl='%s', uri='%s') failed: %s".formatted(gatewayBaseUrl, uri, e),
                  e);
              return null;
            });
  }
}
