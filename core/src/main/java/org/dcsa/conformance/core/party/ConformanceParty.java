package org.dcsa.conformance.core.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.state.StatefulEntity;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;

@Slf4j
public abstract class ConformanceParty implements StatefulEntity {
  protected final String apiVersion;
  protected final PartyConfiguration partyConfiguration;
  protected final CounterpartConfiguration counterpartConfiguration;

  /**
   * Used to store full documents between steps. Unlike the state saved and loaded via
   * exportJsonState and importJsonState, which is entirely (the whole map) stored within a single
   * DynamoDB item, the items in this map are separately stored each in its own DynamoDB item. This
   * is to avoid growing the state map past the size limit of DynamoDB items.
   */
  protected final JsonNodeMap persistentMap;

  private final BiConsumer<ConformanceRequest, Consumer<ConformanceResponse>> asyncWebClient;
  private final Map<String, ? extends Collection<String>> orchestratorAuthHeader;
  private final ActionPromptsQueue actionPromptsQueue = new ActionPromptsQueue();

  private static final int MAX_OPERATOR_LOG_RECORDS = 100;
  private final LinkedList<String> operatorLog = new LinkedList<>();

  public ConformanceParty(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      BiConsumer<ConformanceRequest, Consumer<ConformanceResponse>> asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    this.apiVersion = apiVersion;
    this.partyConfiguration = partyConfiguration;
    this.counterpartConfiguration = counterpartConfiguration;
    this.persistentMap = persistentMap;
    this.asyncWebClient = asyncWebClient;
    this.orchestratorAuthHeader =
        orchestratorAuthHeader.isEmpty()
            ? counterpartConfiguration.getAuthHeaderName().isBlank()
                ? Collections.emptyMap()
                : Map.of(
                    counterpartConfiguration.getAuthHeaderName(),
                    List.of(counterpartConfiguration.getAuthHeaderValue()))
            : orchestratorAuthHeader;
  }

  @Override
  public JsonNode exportJsonState() {
    ObjectNode jsonPartyState = new ObjectMapper().createObjectNode();
    jsonPartyState.set("actionPromptsQueue", actionPromptsQueue.exportJsonState());

    ArrayNode operatorLogNode = new ObjectMapper().createArrayNode();
    operatorLog.forEach(operatorLogNode::add);
    jsonPartyState.set("operatorLog", operatorLogNode);

    exportPartyJsonState(jsonPartyState);
    return jsonPartyState;
  }

  protected abstract void exportPartyJsonState(ObjectNode targetObjectNode);

  @Override
  public void importJsonState(JsonNode jsonState) {
    actionPromptsQueue.importJsonState(jsonState.get("actionPromptsQueue"));
    StreamSupport.stream(jsonState.get("operatorLog").spliterator(), false)
        .forEach(entryNode -> operatorLog.add(entryNode.asText()));
    importPartyJsonState((ObjectNode) jsonState);
  }

  protected abstract void importPartyJsonState(ObjectNode sourceObjectNode);

  protected void addOperatorLogEntry(String logEntry) {
    operatorLog.addFirst(logEntry);
    if (operatorLog.size() > MAX_OPERATOR_LOG_RECORDS - 1) {
      operatorLog.removeLast();
      operatorLog.removeLast();
      operatorLog.addLast("...");
    }
  }

  public List<String> getOperatorLog() {
    return List.copyOf(operatorLog);
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
    if (partyConfiguration.isInManualMode()) {
      log.info(
          "Party %s NOT posting its input automatically (it is in manual mode): %s"
              .formatted(partyConfiguration.getName(), jsonPartyInput.toPrettyString()));
      return;
    }
    asyncWebClient.accept(
        new ConformanceRequest(
            "POST",
            partyConfiguration.getOrchestratorUrl()
                + "/party/%s/input".formatted(partyConfiguration.getName()),
            Collections.emptyMap(),
            new ConformanceMessage(
                partyConfiguration.getName(),
                partyConfiguration.getRole(),
                "orchestrator",
                "orchestrator",
                orchestratorAuthHeader,
                new ConformanceMessageBody(jsonPartyInput),
                System.currentTimeMillis())),
        conformanceResponse -> {});
  }

  protected void asyncCounterpartGet(String path) {
    asyncCounterpartGet(path, conformanceResponse -> {});
  }

  protected void asyncCounterpartGet(String path, Map<String, ? extends Collection<String>> queryParams) {
    asyncCounterpartGet(path, queryParams, conformanceResponse -> {});
  }

  protected void asyncCounterpartGet(String path, Consumer<ConformanceResponse> responseCallback) {
    asyncCounterpartGet(path, Collections.emptyMap(), responseCallback);
  }

  protected void asyncCounterpartGet(String path, Map<String, ? extends Collection<String>> queryParams, Consumer<ConformanceResponse> responseCallback) {
    asyncWebClient.accept(
        new ConformanceRequest(
            "GET",
            counterpartConfiguration.getUrl() + path,
            queryParams,
            new ConformanceMessage(
                partyConfiguration.getName(),
                partyConfiguration.getRole(),
                counterpartConfiguration.getName(),
                counterpartConfiguration.getRole(),
                counterpartConfiguration.getAuthHeaderName().isBlank()
                    ? Map.of("Api-Version", List.of(apiVersion))
                    : Map.of(
                        "Api-Version",
                        List.of(apiVersion),
                        counterpartConfiguration.getAuthHeaderName(),
                        List.of(counterpartConfiguration.getAuthHeaderValue())),
                new ConformanceMessageBody(""),
                System.currentTimeMillis())),
        responseCallback);
  }

  protected void asyncCounterpartPatch(String path, JsonNode jsonBody) {
    asyncCounterpartPatch(path, jsonBody, conformanceResponse -> {});
  }

  protected void asyncCounterpartPatch(String path, Map<String, ? extends Collection<String>> queryParams, JsonNode jsonBody) {
    asyncCounterpartPatch(path, queryParams, jsonBody, conformanceResponse -> {});
  }

  protected void asyncCounterpartGet(
      String path,
      Map<String, ? extends Collection<String>> queryParams) {
    asyncWebClient.accept(
        new ConformanceRequest(
            "GET",
            counterpartConfiguration.getUrl() + path,
            queryParams,
            new ConformanceMessage(
                partyConfiguration.getName(),
                partyConfiguration.getRole(),
                counterpartConfiguration.getName(),
                counterpartConfiguration.getRole(),
                counterpartConfiguration.getAuthHeaderName().isBlank()
                    ? Map.of("Api-Version", List.of(apiVersion))
                    : Map.of(
                        "Api-Version",
                        List.of(apiVersion),
                        counterpartConfiguration.getAuthHeaderName(),
                        List.of(counterpartConfiguration.getAuthHeaderValue())),
                new ConformanceMessageBody(""),
                System.currentTimeMillis())));
  }

  protected void asyncCounterpartPost(String path, JsonNode jsonBody) {
    asyncCounterpartPost(path, jsonBody, conformanceResponse -> {});
  }

  protected void asyncCounterpartPut(String path, JsonNode jsonBody) {
    asyncCounterpartPut(path, jsonBody, conformanceResponse -> {});
  }

  protected void asyncCounterpartPatch(String path, JsonNode jsonBody, Consumer<ConformanceResponse> responseCallback) {
    _asyncCounterpartPatchPostOrPut("PATCH", path, Collections.emptyMap(), jsonBody, responseCallback);
  }

  protected void asyncCounterpartPatch(String path,Map<String, ? extends Collection<String>> queryParams,
                                       JsonNode jsonBody, Consumer<ConformanceResponse> responseCallback) {
    _asyncCounterpartPatchPostOrPut("PATCH", path, queryParams, jsonBody, responseCallback);
  }

  protected void asyncCounterpartPost(String path, JsonNode jsonBody, Consumer<ConformanceResponse> responseCallback) {
    _asyncCounterpartPatchPostOrPut("POST", path, Collections.emptyMap(), jsonBody, responseCallback);
  }

  protected void asyncCounterpartPut(String path, JsonNode jsonBody, Consumer<ConformanceResponse> responseCallback) {
    _asyncCounterpartPatchPostOrPut("PUT", path, Collections.emptyMap(), jsonBody, responseCallback);
  }

  private void _asyncCounterpartPatchPostOrPut(
      String method,
      String path,
      Map<String, ? extends Collection<String>> queryParams,
      JsonNode jsonBody,
      Consumer<ConformanceResponse> responseCallback) {
    asyncWebClient.accept(
        new ConformanceRequest(
            method,
            counterpartConfiguration.getUrl() + path,
            queryParams,
            new ConformanceMessage(
                partyConfiguration.getName(),
                partyConfiguration.getRole(),
                counterpartConfiguration.getName(),
                counterpartConfiguration.getRole(),
                counterpartConfiguration.getAuthHeaderName().isBlank()
                    ? Map.ofEntries(
                        Map.entry("Api-Version", List.of(apiVersion)),
                        Map.entry("Content-Type", List.of(JsonToolkit.JSON_UTF_8)))
                    : Map.ofEntries(
                        Map.entry("Api-Version", List.of(apiVersion)),
                        Map.entry("Content-Type", List.of(JsonToolkit.JSON_UTF_8)),
                        Map.entry(
                            counterpartConfiguration.getAuthHeaderName(),
                            List.of(counterpartConfiguration.getAuthHeaderValue()))),
                new ConformanceMessageBody(jsonBody),
                System.currentTimeMillis())),
        responseCallback);
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

  public void reset() {
    log.info("%s[%s].reset()".formatted(getClass().getSimpleName(), partyConfiguration.getName()));
    this.actionPromptsQueue.clear();
    this.operatorLog.clear();
    doReset();
  }

  protected abstract void doReset();

  @SneakyThrows
  private JsonNode _syncGetPartyPrompt() {
    log.info(
        "%s[%s].getPartyPrompt()"
            .formatted(getClass().getSimpleName(), partyConfiguration.getName()));
    URI uri =
        URI.create(
            partyConfiguration.getOrchestratorUrl()
                + "/party/%s/prompt/json".formatted(partyConfiguration.getName()));
    log.info("ConformanceParty.getPartyPrompt() calling: %s".formatted(uri));
    HttpRequest.Builder httpRequestBuilder =
        HttpRequest.newBuilder().uri(uri).timeout(Duration.ofHours(1)).GET();
    orchestratorAuthHeader.forEach(
        (name, values) -> values.forEach(value -> httpRequestBuilder.header(name, value)));
    String stringResponseBody =
        HttpClient.newHttpClient()
            .send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString())
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
