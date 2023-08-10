package org.dcsa.conformance.gateway.parties;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import lombok.Getter;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

public abstract class ConformanceParty {
  @Getter protected final String name;
  @Getter private final boolean internal;
  protected final String gatewayBaseUrl;
  protected final String gatewayRootPath;

  public ConformanceParty(
      String name,
      boolean internal,
      String gatewayBaseUrl,
      String gatewayRootPath) {
    this.name = name;
    this.internal = internal;
    this.gatewayBaseUrl = gatewayBaseUrl;
    this.gatewayRootPath = gatewayRootPath;
  }

  public void handleNotification() {
    JsonNode prompt = get("/party/%s/prompt/json".formatted(name));
    StreamSupport.stream(prompt.spliterator(), false).forEach(this::handleActionPrompt);
  }

  abstract public ResponseEntity<JsonNode> handlePostRequest(JsonNode requestBody);

  abstract protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers();

  public void handleActionPrompt(JsonNode actionPrompt) {
    String actionId = actionPrompt.get("actionId").asText();
    String actionType = actionPrompt.get("actionType").asText();
    getActionPromptHandlers().entrySet().stream()
            .filter(entry -> Objects.equals(entry.getKey(), actionType))
            .findFirst()
            .orElseThrow()
            .getValue()
            .accept(actionPrompt);
  }

  private JsonNode get(String uri) {
    return WebTestClient.bindToServer()
            .baseUrl(gatewayBaseUrl)
            .build()
            .get()
            .uri(uri)
            .exchange()
            .expectBody(JsonNode.class)
            .returnResult()
            .getResponseBody();
  }

  protected void post(String uri, JsonNode requestBody) {
    WebTestClient.bindToServer()
            .baseUrl(gatewayBaseUrl)
            .build()
            .post()
            .uri(uri)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(requestBody), String.class)
            .exchange();
  }
}
