package org.dcsa.conformance.standards.eblsurrender.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
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
import org.dcsa.conformance.standards.eblsurrender.action.SupplyScenarioParametersAction;

@Slf4j
public class EblSurrenderCarrier extends ConformanceParty {
  private final Map<String, EblSurrenderState> eblStatesById = new HashMap<>();

  public EblSurrenderCarrier(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      PartyWebClient webClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        persistentMap,
        webClient,
        orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
    eblStatesById.forEach(
        (key, value) -> {
          ObjectNode entryNode = OBJECT_MAPPER.createObjectNode();
          entryNode.put("key", key);
          entryNode.put("value", value.name());
          arrayNode.add(entryNode);
        });
    targetObjectNode.set("eblStatesById", arrayNode);
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StreamSupport.stream(sourceObjectNode.get("eblStatesById").spliterator(), false)
        .forEach(
            entryNode ->
                eblStatesById.put(
                    entryNode.get("key").asText(),
                    EblSurrenderState.valueOf(entryNode.get("value").asText())));
  }

  @Override
  protected void doReset() {
    eblStatesById.clear();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(SupplyScenarioParametersAction.class, this::supplyScenarioParameters));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info(
        "EblSurrenderPlatform.supplyScenarioParameters(%s)"
            .formatted(actionPrompt.toPrettyString()));

    String tdr = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    eblStatesById.put(tdr, EblSurrenderState.AVAILABLE_FOR_SURRENDER);
    persistentMap.save("response", actionPrompt.get("response"));

    var identifyingCodes =
        OBJECT_MAPPER
            .createArrayNode()
            .add(
                OBJECT_MAPPER
                    .createObjectNode()
                    .put("codeListProvider", "WAVE")
                    .put("partyCode", "MSK")
                    .put("codeListName", "DID"));

    var issueToParty = OBJECT_MAPPER.createObjectNode();
    issueToParty.set("identifyingCodes", identifyingCodes);
    issueToParty.put("partyName", "Issue To name").put("eblPlatform", "WAVE");
    var carrierParty = OBJECT_MAPPER.createObjectNode();
    carrierParty.set("identifyingCodes", identifyingCodes);
    carrierParty.put("partyName", "Carrier name").put("eblPlatform", "WAVE");

    var surrendereeParty = OBJECT_MAPPER.createObjectNode();
    surrendereeParty.set("identifyingCodes", identifyingCodes);
    surrendereeParty.put("partyName", "Surrenderee name").put("eblPlatform", "BOLE");

    SuppliedScenarioParameters suppliedScenarioParameters =
        new SuppliedScenarioParameters(tdr, issueToParty, carrierParty, surrendereeParty);

    asyncOrchestratorPostPartyInput(
        actionPrompt.required("actionId").asText(), suppliedScenarioParameters.toJson());

    addOperatorLogEntry(
        "Submitting SuppliedScenarioParameters: %s"
            .formatted(suppliedScenarioParameters.toJson().toPrettyString()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblSurrenderCarrier.handleRequest(%s)".formatted(request));
    JsonNode jsonRequest = request.message().body().getJsonBody();
    String src = jsonRequest.get("surrenderRequestCode").asText();
    String srr = jsonRequest.get("surrenderRequestReference").asText();
    String tdr = jsonRequest.get("transportDocumentReference").asText();
    String action = tdr.contains("WAVER") ? "SREJ" : "SURR";

    if ("*".equals(srr)) {
      srr = UUID.randomUUID().toString();
    }
    if (persistentMap.load("response") != null) {
      action = persistentMap.load("response").asText();
    }

    var carrierResponse =
        OBJECT_MAPPER
            .createObjectNode()
            .put("surrenderRequestReference", srr)
            .put("action", action);
    asyncCounterpartNotification(null, "/v3/ebl-surrender-responses", carrierResponse);

    if (Objects.equals(
        EblSurrenderState.AVAILABLE_FOR_SURRENDER,
        eblStatesById.getOrDefault(
            tdr,
            // workaround for supplyScenarioParameters() not getting called on parties in manual
            // mode
            this.partyConfiguration.isInManualMode()
                ? EblSurrenderState.AVAILABLE_FOR_SURRENDER
                : null))) {
      eblStatesById.put(
          tdr,
          Objects.equals("AREQ", src)
              ? EblSurrenderState.AMENDMENT_SURRENDER_REQUESTED
              : EblSurrenderState.DELIVERY_SURRENDER_REQUESTED);

      addOperatorLogEntry(
          "Handling surrender request with surrenderRequestCode '%s' and surrenderRequestReference '%s' for eBL with transportDocumentReference '%s' (now in state '%s')"
              .formatted(src, srr, tdr, eblStatesById.get(tdr)));

      return request.createResponse(
          204,
          Map.of(API_VERSION, List.of(apiVersion)),
          new ConformanceMessageBody(
              OBJECT_MAPPER
                  .createObjectNode()
                  .put("surrenderRequestReference", srr)
                  .put("transportDocumentReference", tdr)));
    } else {
      return return409(
          request,
          "Rejecting '%s' for document '%s' because it is in state '%s'"
              .formatted(src, tdr, eblStatesById.get(tdr)));
    }
  }

  private ConformanceResponse return409(ConformanceRequest request, String message) {
    ObjectNode response =
        (ObjectNode)
            JsonToolkit.templateFileToJsonNode(
                "/standards/eblsurrender/messages/eblsurrender-api-v3.0.0-error-message.json",
                Map.of(
                    "HTTP_METHOD_PLACEHOLDER",
                    request.method(),
                    "REQUEST_URI_PLACEHOLDER",
                    request.url(),
                    "REFERENCE_PLACEHOLDER",
                    UUID.randomUUID().toString(),
                    "ERROR_DATE_TIME_PLACEHOLDER",
                    LocalDateTime.now().format(JsonToolkit.ISO_8601_DATE_TIME_FORMAT),
                    "ERROR_MESSAGE_PLACEHOLDER",
                    message));

    return request.createResponse(
        409, Map.of(API_VERSION, List.of(apiVersion)), new ConformanceMessageBody(response));
  }
}
