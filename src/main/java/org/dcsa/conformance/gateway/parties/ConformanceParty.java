package org.dcsa.conformance.gateway.parties;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import lombok.Getter;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;

public abstract class ConformanceParty {
  @Getter protected final String name;
  @Getter private final boolean internal;
  protected final String gatewayBaseUrl;
  protected final String gatewayRootPath;
  protected final Function<String, JsonNode> partyPromptGetter;
  protected final Consumer<JsonNode> partyInputConsumer;

  public ConformanceParty(
      String name,
      boolean internal,
      String gatewayBaseUrl,
      String gatewayRootPath,
      Function<String, JsonNode> partyPromptGetter,
      Consumer<JsonNode> partyInputConsumer) {
    this.name = name;
    this.internal = internal;
    this.gatewayBaseUrl = gatewayBaseUrl;
    this.gatewayRootPath = gatewayRootPath;
    this.partyPromptGetter = partyPromptGetter;
    this.partyInputConsumer = partyInputConsumer;
  }

  public void handleNotification() {
    JsonNode prompt = partyPromptGetter.apply(name);
    StreamSupport.stream(prompt.spliterator(), false).forEach(this::handleActionPrompt);
  }

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
}
