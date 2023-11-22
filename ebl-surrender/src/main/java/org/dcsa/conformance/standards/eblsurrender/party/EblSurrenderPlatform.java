package org.dcsa.conformance.standards.eblsurrender.party;

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
import org.dcsa.conformance.standards.eblsurrender.action.SurrenderRequestAction;

@Slf4j
public class EblSurrenderPlatform extends ConformanceParty {
  private final Map<String, EblSurrenderState> eblStatesById = new HashMap<>();
  private final Map<String, String> tdrsBySrr = new HashMap<>();

  public EblSurrenderPlatform(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      Consumer<ConformanceRequest> asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        asyncWebClient,
        orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    ObjectMapper objectMapper = new ObjectMapper();

    ArrayNode arrayNodeEblStatesById = objectMapper.createArrayNode();
    eblStatesById.forEach(
        (key, value) -> {
          ObjectNode entryNode = objectMapper.createObjectNode();
          entryNode.put("key", key);
          entryNode.put("value", value.name());
          arrayNodeEblStatesById.add(entryNode);
        });
    targetObjectNode.set("eblStatesById", arrayNodeEblStatesById);

    ArrayNode arrayNodeTdrsBySrr = objectMapper.createArrayNode();
    tdrsBySrr.forEach(
        (key, value) -> {
          ObjectNode entryNode = objectMapper.createObjectNode();
          entryNode.put("key", key);
          entryNode.put("value", value);
          arrayNodeTdrsBySrr.add(entryNode);
        });
    targetObjectNode.set("tdrsBySrr", arrayNodeTdrsBySrr);
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StreamSupport.stream(sourceObjectNode.get("eblStatesById").spliterator(), false)
        .forEach(
            entryNode ->
                eblStatesById.put(
                    entryNode.get("key").asText(),
                    EblSurrenderState.valueOf(entryNode.get("value").asText())));

    StreamSupport.stream(sourceObjectNode.get("tdrsBySrr").spliterator(), false)
        .forEach(
            entryNode ->
                tdrsBySrr.put(entryNode.get("key").asText(), entryNode.get("value").asText()));
  }

  @Override
  protected void doReset() {
    eblStatesById.clear();
    tdrsBySrr.clear();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(Map.entry(SurrenderRequestAction.class, this::requestSurrender));
  }

  private void requestSurrender(JsonNode actionPrompt) {
    log.info("EblSurrenderPlatform.requestSurrender(%s)".formatted(actionPrompt.toPrettyString()));
    String srr = UUID.randomUUID().toString();
    String tdr = actionPrompt.get("tdr").asText();
    boolean forAmendment = actionPrompt.get("forAmendment").booleanValue();
    String src = forAmendment ? "AREQ" : "SREQ";
    tdrsBySrr.put(srr, tdr);
    eblStatesById.put(
        tdr,
        forAmendment
            ? EblSurrenderState.AMENDMENT_SURRENDER_REQUESTED
            : EblSurrenderState.DELIVERY_SURRENDER_REQUESTED);
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode jsonRequestBody =
        objectMapper
            .createObjectNode()
            .put("surrenderRequestReference", srr)
            .put("transportDocumentReference", tdr)
            .put("surrenderRequestCode", src);

    jsonRequestBody.set(
        "surrenderRequestedBy",
        objectMapper
            .createObjectNode()
            .put("eblPlatformIdentifier", "one@example.com")
            .put("legalName", "Legal Name One"));

    ObjectNode endorsementChainLink =
        objectMapper
            .createObjectNode()
            .put("action", "ETOF")
            .put("actionDateTime", "2023-08-27T19:38:24.342Z");
    endorsementChainLink.set(
        "actor",
        objectMapper
            .createObjectNode()
            .put("eblPlatformIdentifier", "two@example.com")
            .put("legalName", "Legal Name Two"));
    endorsementChainLink.set(
        "recipient",
        objectMapper
            .createObjectNode()
            .put("eblPlatformIdentifier", "three@example.com")
            .put("legalName", "Legal Name Three"));

    jsonRequestBody.set(
        "endorsementChain", objectMapper.createArrayNode().add(endorsementChainLink));
    asyncCounterpartPost(
        "/%s/ebl-surrender-requests".formatted(apiVersion.startsWith("3") ? "v3" : "v2"),
        jsonRequestBody);

    addOperatorLogEntry(
        "Sending surrender request with surrenderRequestCode '%s' and surrenderRequestReference '%s' for eBL with transportDocumentReference '%s'"
            .formatted(src, srr, tdr));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblSurrenderPlatform.handleRequest(%s)".formatted(request));
    JsonNode jsonRequest = request.message().body().getJsonBody();
    String action = jsonRequest.get("action").asText();
    boolean isSurrenderAccepted = Objects.equals("SURR", action);
    String srr = jsonRequest.get("surrenderRequestReference").asText();
    String tdr = tdrsBySrr.remove(srr);

    ConformanceResponse response;
    if (Objects.equals(EblSurrenderState.AMENDMENT_SURRENDER_REQUESTED, eblStatesById.get(tdr))) {
      eblStatesById.put(
          tdr,
          isSurrenderAccepted
              ? EblSurrenderState.SURRENDERED_FOR_AMENDMENT
              : EblSurrenderState.AVAILABLE_FOR_SURRENDER);
      response =
          request.createResponse(
              204,
              Map.of("Api-Version", List.of(apiVersion)),
              new ConformanceMessageBody(new ObjectMapper().createObjectNode()));
    } else if (Objects.equals(
        EblSurrenderState.DELIVERY_SURRENDER_REQUESTED, eblStatesById.get(tdr))) {
      eblStatesById.put(
          tdr,
          isSurrenderAccepted
              ? EblSurrenderState.SURRENDERED_FOR_DELIVERY
              : EblSurrenderState.AVAILABLE_FOR_SURRENDER);
      response =
          request.createResponse(
              204,
              Map.of("Api-Version", List.of(apiVersion)),
              new ConformanceMessageBody(new ObjectMapper().createObjectNode()));
    } else {
      response =
          request.createResponse(
              409,
              Map.of("Api-Version", List.of(apiVersion)),
              new ConformanceMessageBody(
                  new ObjectMapper()
                      .createObjectNode()
                      .put(
                          "comments",
                          "Rejecting '%s' for document '%s' because it is in state '%s'"
                              .formatted(action, tdr, eblStatesById.get(tdr)))));
    }
    addOperatorLogEntry(
        "Handling surrender response with action '%s' and surrenderRequestReference '%s' for eBL with transportDocumentReference '%s' (now in state '%s')"
            .formatted(action, srr, tdr, eblStatesById.get(tdr)));
    return response;
  }
}
