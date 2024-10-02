package org.dcsa.conformance.standards.eblsurrender.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.eblsurrender.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.eblsurrender.action.SurrenderResponseAction;
import org.dcsa.conformance.standards.eblsurrender.action.VoidAndReissueAction;

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
        Map.entry(SupplyScenarioParametersAction.class, this::supplyScenarioParameters),
        Map.entry(SurrenderResponseAction.class, this::sendSurrenderResponse),
        Map.entry(VoidAndReissueAction.class, this::voidAndReissue));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info(
        "EblSurrenderPlatform.supplyScenarioParameters(%s)"
            .formatted(actionPrompt.toPrettyString()));

    String tdr = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    eblStatesById.put(tdr, EblSurrenderState.AVAILABLE_FOR_SURRENDER);

    SuppliedScenarioParameters suppliedScenarioParameters =
        new SuppliedScenarioParameters(
            tdr, "XMPL", "Example carrier party code", "Example party code", "Example code list");

    asyncOrchestratorPostPartyInput(
        actionPrompt.required("actionId").asText(), suppliedScenarioParameters.toJson());

    addOperatorLogEntry(
        "Submitting SuppliedScenarioParameters: %s"
            .formatted(suppliedScenarioParameters.toJson().toPrettyString()));
  }

  private void voidAndReissue(JsonNode actionPrompt) {
    log.info("EblSurrenderCarrier.voidAndReissue(%s)".formatted(actionPrompt.toPrettyString()));
    SuppliedScenarioParameters ssp = SuppliedScenarioParameters.fromJson(actionPrompt.get("suppliedScenarioParameters"));
    String tdr = ssp.transportDocumentReference();
    if (!Objects.equals(eblStatesById.get(tdr), EblSurrenderState.SURRENDERED_FOR_AMENDMENT)) {
      throw new IllegalStateException(
          "Cannot void and reissue in state: " + eblStatesById.get(tdr));
    }
    eblStatesById.put(tdr, EblSurrenderState.AVAILABLE_FOR_SURRENDER);
    asyncOrchestratorPostPartyInput(
        actionPrompt.get("actionId").asText(), OBJECT_MAPPER.createObjectNode());
    addOperatorLogEntry(
        "Voided and reissued the eBL with transportDocumentReference '%s'".formatted(tdr));
  }

  private void sendSurrenderResponse(JsonNode actionPrompt) {
    log.info(
        "EblSurrenderCarrier.sendSurrenderResponse(%s)".formatted(actionPrompt.toPrettyString()));
    SuppliedScenarioParameters ssp = SuppliedScenarioParameters.fromJson(actionPrompt.get("suppliedScenarioParameters"));
    String tdr = ssp.transportDocumentReference();
    boolean accept = actionPrompt.get("accept").asBoolean();
    switch (eblStatesById.get(tdr)) {
      case AMENDMENT_SURRENDER_REQUESTED -> eblStatesById.put(
          tdr,
          accept
              ? EblSurrenderState.SURRENDERED_FOR_AMENDMENT
              : EblSurrenderState.AVAILABLE_FOR_SURRENDER);
      case DELIVERY_SURRENDER_REQUESTED -> eblStatesById.put(
          tdr,
          accept
              ? EblSurrenderState.SURRENDERED_FOR_DELIVERY
              : EblSurrenderState.AVAILABLE_FOR_SURRENDER);
      default -> {} // ignore -- sending from wrong state for testing purposes
    }
    String srr = actionPrompt.get("srr").asText();
    if ("*".equals(srr)) {
      srr = UUID.randomUUID().toString();
    }
    syncCounterpartPost(
        "/v%s/ebl-surrender-responses".formatted(apiVersion.charAt(0)),
        OBJECT_MAPPER
            .createObjectNode()
            .put("surrenderRequestReference", srr)
            .put("action", accept ? "SURR" : "SREJ"));

    addOperatorLogEntry(
        "%s surrender request with surrenderRequestReference '%s' for eBL with transportDocumentReference '%s' (now in state '%s')"
            .formatted(accept ? "Accepted" : "Rejected", srr, tdr, eblStatesById.get(tdr)));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblSurrenderCarrier.handleRequest(%s)".formatted(request));
    JsonNode jsonRequest = request.message().body().getJsonBody();
    String src = jsonRequest.get("surrenderRequestCode").asText();
    String srr = jsonRequest.get("surrenderRequestReference").asText();
    String tdr = jsonRequest.get("transportDocumentReference").asText();

    if (Objects.equals(
        EblSurrenderState.AVAILABLE_FOR_SURRENDER,
        eblStatesById.getOrDefault(
            tdr,
            // workaround for supplyScenarioParameters() not getting called on parties in manual mode
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
      return request.createResponse(
          409,
          Map.of(API_VERSION, List.of(apiVersion)),
          new ConformanceMessageBody(
              OBJECT_MAPPER
                  .createObjectNode()
                  .put(
                      "comments",
                      "Rejecting '%s' for document '%s' because it is in state '%s'"
                          .formatted(src, tdr, eblStatesById.get(tdr)))));
    }
  }
}
