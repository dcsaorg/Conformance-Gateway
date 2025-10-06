package org.dcsa.conformance.core.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.logs.TimestampedLogEntry;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.state.StatefulEntity;
import org.dcsa.conformance.core.toolkit.IOToolkit;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessage;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;

@Slf4j
public abstract class ConformanceParty implements StatefulEntity {
  public static final String API_VERSION = "Api-Version";

  public final String operatorLogName = "operatorLog" + getClass().getSimpleName();

  protected final String apiVersion;
  protected final PartyConfiguration partyConfiguration;
  protected final CounterpartConfiguration counterpartConfiguration;

  @Setter private BiConsumer<String, String> waitingForBiConsumer = (who, forWhat) -> {};

  /**
   * Used to store full documents between steps. Unlike the state saved and loaded via
   * exportJsonState and importJsonState, which is entirely (the whole map) stored within a single
   * DynamoDB item, the items in this map are separately stored each in its own DynamoDB item. This
   * is to avoid growing the state map past the size limit of DynamoDB items.
   */
  protected final JsonNodeMap persistentMap;

  private final PartyWebClient webClient;
  private final Map<String, ? extends Collection<String>> orchestratorAuthHeader;
  private final ActionPromptsQueue actionPromptsQueue = new ActionPromptsQueue();

  private static final int MAX_OPERATOR_LOG_RECORDS = 12;
  private final List<TimestampedLogEntry> operatorLog = new LinkedList<>();

  protected ConformanceParty(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      PartyWebClient webClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    this.apiVersion = apiVersion;
    this.partyConfiguration = partyConfiguration;
    this.counterpartConfiguration = counterpartConfiguration;
    this.persistentMap = persistentMap;
    this.webClient = webClient;
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
    ObjectNode jsonPartyState = OBJECT_MAPPER.createObjectNode();
    jsonPartyState.set("actionPromptsQueue", actionPromptsQueue.exportJsonState());

    ArrayNode operatorLogNode = jsonPartyState.putArray(operatorLogName);
    operatorLog.forEach(
        entry -> {
          ObjectNode entryNode = OBJECT_MAPPER.createObjectNode();
          entryNode.put("message", entry.message());
          entryNode.put("timestamp", entry.timestamp().toString());
          operatorLogNode.add(entryNode);
        });

    exportPartyJsonState(jsonPartyState);
    return jsonPartyState;
  }

  protected abstract void exportPartyJsonState(ObjectNode targetObjectNode);

  @Override
  public void importJsonState(JsonNode jsonState) {
    actionPromptsQueue.importJsonState(jsonState.get("actionPromptsQueue"));

    JsonNode operatorLogNode = jsonState.get(operatorLogName);
    StreamSupport.stream(operatorLogNode.spliterator(), false)
        .forEach(
            entryNode -> {
              String message = entryNode.get("message").asText();
              Instant timestamp = Instant.parse(entryNode.get("timestamp").asText());
              operatorLog.add(new TimestampedLogEntry(message, timestamp));
            });

    importPartyJsonState((ObjectNode) jsonState);
  }

  protected abstract void importPartyJsonState(ObjectNode sourceObjectNode);

  public void addOperatorLogEntry(String logEntry) {
    operatorLog.addFirst(new TimestampedLogEntry(logEntry));
    if (operatorLog.size() > MAX_OPERATOR_LOG_RECORDS - 1) {
      operatorLog.removeLast();
      operatorLog.removeLast();
      operatorLog.addLast(new TimestampedLogEntry("..."));
    }
  }

  public List<TimestampedLogEntry> getOperatorLog() {
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

  /**
   * To be invoked like this:
   * <pre>
   * {@code
   * asyncOrchestratorPostPartyInput(
   *   actionPrompt.required("actionId").asText(),
   *   OBJECT_MAPPER.createObjectNode()
   *     .put("keyOne", valueOne)
   *     .put("keyTwo", valueTwo));
   * }
   * </pre>
   */
  protected void asyncOrchestratorPostPartyInput(String actionId, ObjectNode inputObjectNode) {
    if (actionId == null) throw new IllegalArgumentException("The actionId may not be null");
    if (inputObjectNode == null)
      throw new IllegalArgumentException("The inputObjectNode may not be null");
    ObjectNode jsonPartyInput =
        OBJECT_MAPPER.createObjectNode().put("actionId", actionId).set("input", inputObjectNode);
    if (partyConfiguration.isInManualMode()) {
      log.info(
          "Party {} NOT posting its input automatically (it is in manual mode): {}",
          partyConfiguration.getName(),
          jsonPartyInput.toPrettyString());
      return;
    }
    webClient.asyncRequest(
        new ConformanceRequest(
            "POST",
            partyConfiguration.getOrchestratorUrl()
                + "/party/%s/input"
                    .formatted(
                        URLEncoder.encode(partyConfiguration.getName(), StandardCharsets.UTF_8)),
            Collections.emptyMap(),
            new ConformanceMessage(
                partyConfiguration.getName(),
                partyConfiguration.getRole(),
                "orchestrator",
                "orchestrator",
                orchestratorAuthHeader,
                new ConformanceMessageBody(jsonPartyInput),
                System.currentTimeMillis())));
  }

  protected void syncCounterpartGet(
      String path, Map<String, ? extends Collection<String>> queryParams) {
    _syncWebClientRequest(_createConformanceRequest(false, "GET", path, queryParams, null));
  }

  protected void syncCounterpartPatch(
      String path, Map<String, ? extends Collection<String>> queryParams, JsonNode jsonBody) {
    _syncWebClientRequest(
        _createConformanceRequest(false, "PATCH", path, queryParams, jsonBody));
  }

  protected ConformanceResponse syncCounterpartPost(String path, JsonNode jsonBody) {
    return _syncWebClientRequest(
        _createConformanceRequest(false, "POST", path, Collections.emptyMap(), jsonBody));
  }

  protected ConformanceResponse syncCounterpartPut(String path, JsonNode jsonBody) {
    return _syncWebClientRequest(
        _createConformanceRequest(false, "PUT", path, Collections.emptyMap(), jsonBody));
  }

  private ConformanceResponse _syncWebClientRequest(ConformanceRequest conformanceRequest) {
    waitingForBiConsumer.accept(
        counterpartConfiguration.getName(),
        "respond to %s request".formatted(conformanceRequest.method()));
    ConformanceResponse conformanceResponse;
    try {
      conformanceResponse = webClient.syncRequest(conformanceRequest);
    } finally {
      waitingForBiConsumer.accept(null, null);
    }
    return conformanceResponse;
  }

  protected void asyncCounterpartNotification(String actionId, String path, JsonNode jsonBody) {
    String counterpartBaseUrl = counterpartConfiguration.getUrl();
    if (counterpartBaseUrl != null && !counterpartBaseUrl.isEmpty()) {
      webClient.asyncRequest(
          _createConformanceRequest(true, "POST", path, Collections.emptyMap(), jsonBody));
    } else {
      if (actionId != null) {
        log.info(
            "Party {} notifying orchestrator that action {} is completed instead of sending notification: no counterpart URL is configured",
            actionId,
            partyConfiguration.getName());
        webClient.asyncRequest(
            _createConformanceRequest(
                true,
                "POST",
                partyConfiguration.getOrchestratorUrl() + "/dev/null",
                Collections.emptyMap(),
                jsonBody));
      } else {
        log.info(
            "Party {} NOT sending a notification and NOT notifying orchestrator either: no counterpart URL is configured",
            partyConfiguration.getName());
      }
    }
  }

  private ConformanceRequest _createConformanceRequest(
      boolean withFullApiVersionHeader,
      String method,
      String path,
      Map<String, ? extends Collection<String>> queryParams,
      JsonNode jsonBody) {
    String apiVersionHeaderValue =
        withFullApiVersionHeader ? apiVersion : apiVersion.split("\\.")[0];
    return new ConformanceRequest(
        method,
        _getCounterpartUrl(counterpartConfiguration, method, path),
        queryParams,
        new ConformanceMessage(
            partyConfiguration.getName(),
            partyConfiguration.getRole(),
            counterpartConfiguration.getName(),
            counterpartConfiguration.getRole(),
            Stream.concat(
                    Stream.concat(
                        Stream.of(
                            Map.entry(API_VERSION, List.of(apiVersionHeaderValue)),
                            Map.entry("Content-Type", List.of(JsonToolkit.JSON_UTF_8))),
                        counterpartConfiguration.getAuthHeaderName().isBlank()
                            ? Stream.of()
                            : Stream.of(
                                Map.entry(
                                    counterpartConfiguration.getAuthHeaderName(),
                                    List.of(counterpartConfiguration.getAuthHeaderValue())))),
                    counterpartConfiguration.getExternalPartyAdditionalHeaders() == null
                        ? Stream.of()
                        : Stream.of(counterpartConfiguration.getExternalPartyAdditionalHeaders())
                            .map(
                                httpHeaderConfiguration ->
                                    Map.entry(
                                        httpHeaderConfiguration.getHeaderName(),
                                        List.of(httpHeaderConfiguration.getHeaderValue()))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
            jsonBody == null
                ? new ConformanceMessageBody("")
                : new ConformanceMessageBody(jsonBody),
            System.currentTimeMillis()));
  }

  private String _getCounterpartUrl(
      CounterpartConfiguration counterpartConfiguration, String method, String path) {
    var url = counterpartConfiguration.getUrl() + path;
    EndpointUriOverrideConfiguration[] endpointUriOverrideConfigurations =
        counterpartConfiguration.getEndpointUriOverrideConfigurations();
    if (endpointUriOverrideConfigurations != null) {
      for (EndpointUriOverrideConfiguration endpointUriOverrideConfiguration :
          endpointUriOverrideConfigurations) {
        if (method.equals(endpointUriOverrideConfiguration.getMethod())) {
          int baseUriIndex = url.indexOf(endpointUriOverrideConfiguration.getEndpointBaseUri());
          if (baseUriIndex > -1) {
            return url.replace(
                endpointUriOverrideConfiguration.getEndpointBaseUri(),
                endpointUriOverrideConfiguration.getBaseUriOverride());
          }
        }
      }
    }
    return url;
  }

  public abstract ConformanceResponse handleRequest(ConformanceRequest request);

  public void handleNotification() {
    log.info(
        "{}[{}].handleNotification()", getClass().getSimpleName(), partyConfiguration.getName());
    JsonNode partyPrompt = _syncGetPartyPrompt();
    if (!partyPrompt.isEmpty()) {
      StreamSupport.stream(partyPrompt.spliterator(), false).forEach(actionPromptsQueue::addLast);
      _handleNextActionPrompt();
    }
  }

  public void reset() {
    log.info("{}[{}].reset()", getClass().getSimpleName(), partyConfiguration.getName());
    this.actionPromptsQueue.clear();
    this.operatorLog.clear();
    doReset();
  }

  protected abstract void doReset();

  @SneakyThrows
  private JsonNode _syncGetPartyPrompt() {
    log.info("{}[{}].getPartyPrompt()", getClass().getSimpleName(), partyConfiguration.getName());
    URI uri =
        URI.create(
            partyConfiguration.getOrchestratorUrl()
                + "/party/%s/prompt/json"
                    .formatted(
                        URLEncoder.encode(partyConfiguration.getName(), StandardCharsets.UTF_8)));
    log.info("ConformanceParty.getPartyPrompt() calling: %s".formatted(uri));
    // Allow long debugging sessions or slow business logic at customer's side
    HttpRequest.Builder httpRequestBuilder =
        HttpRequest.newBuilder().uri(uri).timeout(Duration.ofHours(1)).GET();
    orchestratorAuthHeader.forEach(
        (name, values) -> values.forEach(value -> httpRequestBuilder.header(name, value)));
    String stringResponseBody =
      IOToolkit.HTTP_CLIENT
            .send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString())
            .body();
    return new ConformanceMessageBody(stringResponseBody).getJsonBody();
  }

  private void _handleNextActionPrompt() {
    if (actionPromptsQueue.isEmpty()) return;
    JsonNode actionPrompt = actionPromptsQueue.removeFirst();
    if (actionPrompt == null) return;
    log.info(
        "{}[{}]._handleNextActionPrompt() handling {}",
        getClass().getSimpleName(),
        partyConfiguration.getName(),
        actionPrompt.toPrettyString());
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

  protected ConformanceResponse invalidRequest(
      ConformanceRequest request, int statusCode, String message) {
    log.warn("Invalid request: %s", message);
    return request.createResponse(
        statusCode,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode().put("message", message)));
  }
}
