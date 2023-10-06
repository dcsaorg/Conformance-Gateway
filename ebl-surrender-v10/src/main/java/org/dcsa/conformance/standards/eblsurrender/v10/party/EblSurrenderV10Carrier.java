package org.dcsa.conformance.standards.eblsurrender.v10.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.eblsurrender.v10.action.SupplyAvailableTdrAction;
import org.dcsa.conformance.standards.eblsurrender.v10.action.SurrenderResponseAction;
import org.dcsa.conformance.standards.eblsurrender.v10.action.VoidAndReissueAction;

@Slf4j
public class EblSurrenderV10Carrier extends ConformanceParty {
  private final Map<String, EblSurrenderV10State> eblStatesById = new HashMap<>();

  public EblSurrenderV10Carrier(
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      Consumer<ConformanceRequest> asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(partyConfiguration, counterpartConfiguration, asyncWebClient, orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode arrayNode = objectMapper.createArrayNode();
    eblStatesById.forEach(
        (key, value) -> {
          ObjectNode entryNode = objectMapper.createObjectNode();
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
                    EblSurrenderV10State.valueOf(entryNode.get("value").asText())));
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(SupplyAvailableTdrAction.class, this::supplyAvailableTdr),
        Map.entry(SurrenderResponseAction.class, this::sendSurrenderResponse),
        Map.entry(VoidAndReissueAction.class, this::voidAndReissue));
  }

  private void supplyAvailableTdr(JsonNode actionPrompt) {
    log.info(
        "EblSurrenderV10Carrier.supplyAvailableTdr(%s)".formatted(actionPrompt.toPrettyString()));
    String tdr = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20);
    eblStatesById.put(tdr, EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
    asyncOrchestratorPostPartyInput(
        new ObjectMapper()
            .createObjectNode()
            .put("actionId", actionPrompt.get("actionId").asText())
            .put("input", tdr));
  }

  private void voidAndReissue(JsonNode actionPrompt) {
    log.info("EblSurrenderV10Carrier.voidAndReissue(%s)".formatted(actionPrompt.toPrettyString()));
    String tdr = actionPrompt.get("tdr").asText();
    if (!Objects.equals(eblStatesById.get(tdr), EblSurrenderV10State.SURRENDERED_FOR_AMENDMENT)) {
      throw new IllegalStateException(
          "Cannot void and reissue in state: " + eblStatesById.get(tdr));
    }
    eblStatesById.put(tdr, EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
    asyncOrchestratorPostPartyInput(
        new ObjectMapper()
            .createObjectNode()
            .put("actionId", actionPrompt.get("actionId").asText()));
  }

  private void sendSurrenderResponse(JsonNode actionPrompt) {
    log.info(
        "EblSurrenderV10Carrier.sendSurrenderResponse(%s)"
            .formatted(actionPrompt.toPrettyString()));
    String tdr = actionPrompt.get("tdr").asText();
    boolean accept = actionPrompt.get("accept").asBoolean();
    switch (eblStatesById.get(tdr)) {
      case AMENDMENT_SURRENDER_REQUESTED -> eblStatesById.put(
          tdr,
          accept
              ? EblSurrenderV10State.SURRENDERED_FOR_AMENDMENT
              : EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
      case DELIVERY_SURRENDER_REQUESTED -> eblStatesById.put(
          tdr,
          accept
              ? EblSurrenderV10State.SURRENDERED_FOR_DELIVERY
              : EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
      default -> {} // ignore -- sending from wrong state for testing purposes
    }
    String srr = actionPrompt.get("srr").asText();
    if ("*".equals(srr)) {
      srr = UUID.randomUUID().toString();
    }
    asyncCounterpartPost(
        "/v1/surrender-request-responses",
        "1.0.0",
        new ObjectMapper()
            .createObjectNode()
            .put("surrenderRequestReference", srr)
            .put("action", accept ? "SURR" : "SREJ"));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblSurrenderV10Carrier.handleRequest(%s)".formatted(request));
    JsonNode jsonRequest = request.message().body().getJsonBody();
    String srr = jsonRequest.get("surrenderRequestReference").asText();
    String tdr = jsonRequest.get("transportDocumentReference").asText();
    String src = jsonRequest.get("surrenderRequestCode").asText();
    if (Objects.equals(EblSurrenderV10State.AVAILABLE_FOR_SURRENDER, eblStatesById.get(tdr))) {
      eblStatesById.put(
          tdr,
          Objects.equals("AREQ", src)
              ? EblSurrenderV10State.AMENDMENT_SURRENDER_REQUESTED
              : EblSurrenderV10State.DELIVERY_SURRENDER_REQUESTED);
      return request.createResponse(
          202,
          Map.of("Api-Version", List.of("1.0.0")),
          new ConformanceMessageBody(
              new ObjectMapper()
                  .createObjectNode()
                  .put("surrenderRequestReference", srr)
                  .put("transportDocumentReference", tdr)));
    } else {
      return request.createResponse(
          409,
          Map.of("Api-Version", List.of("1.0.0")),
          new ConformanceMessageBody(
              new ObjectMapper()
                  .createObjectNode()
                  .put(
                      "comments",
                      "Rejecting '%s' for document '%s' because it is in state '%s'"
                          .formatted(src, tdr, eblStatesById.get(tdr)))));
    }
  }
}
