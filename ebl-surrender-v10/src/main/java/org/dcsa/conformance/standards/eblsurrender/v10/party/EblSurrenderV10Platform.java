package org.dcsa.conformance.standards.eblsurrender.v10.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.BiConsumer;
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
import org.dcsa.conformance.standards.eblsurrender.v10.action.SurrenderRequestAction;

@Slf4j
public class EblSurrenderV10Platform extends ConformanceParty {
  private final Map<String, EblSurrenderV10State> eblStatesById = new HashMap<>();
  private final Map<String, String> tdrsBySrr = new HashMap<>();

  public EblSurrenderV10Platform(
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      Consumer<ConformanceRequest> asyncWebClient,
      BiConsumer<String, Consumer<ConformanceParty>> asyncPartyActionConsumer) {
    super(partyConfiguration, counterpartConfiguration, asyncWebClient, asyncPartyActionConsumer);
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
                    EblSurrenderV10State.valueOf(entryNode.get("value").asText())));

    StreamSupport.stream(sourceObjectNode.get("tdrsBySrr").spliterator(), false)
        .forEach(
            entryNode ->
                tdrsBySrr.put(entryNode.get("key").asText(), entryNode.get("value").asText()));
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(Map.entry(SurrenderRequestAction.class, this::requestSurrender));
  }

  private void requestSurrender(JsonNode actionPrompt) {
    log.info(
        "EblSurrenderV10Platform.requestSurrender(%s)".formatted(actionPrompt.toPrettyString()));
    String srr = UUID.randomUUID().toString();
    String tdr = actionPrompt.get("tdr").asText();
    boolean forAmendment = actionPrompt.get("forAmendment").booleanValue();
    tdrsBySrr.put(srr, tdr);
    eblStatesById.put(
        tdr,
        forAmendment
            ? EblSurrenderV10State.AMENDMENT_SURRENDER_REQUESTED
            : EblSurrenderV10State.DELIVERY_SURRENDER_REQUESTED);
    ObjectNode jsonRequestBody =
        new ObjectMapper()
            .createObjectNode()
            .put("surrenderRequestReference", srr)
            .put("transportDocumentReference", tdr)
            .put("surrenderRequestCode", forAmendment ? "AREQ" : "SREQ");
    jsonRequestBody.set(
        "surrenderRequestedBy",
        new ObjectMapper()
            .createObjectNode()
            .put("eblPlatformIdentifier", "one@example.com")
            .put("legalName", "Legal Name One"));
    jsonRequestBody.set(
        "endorsementChain",
        new ObjectMapper()
            .createArrayNode()
            .add(
                new ObjectMapper()
                    .createObjectNode()
                    .put("action", "ETOF")
                    .put("actionDateTime", "2023-08-27T19:38:24.342Z")
                    .set(
                        "actor",
                        new ObjectMapper()
                            .createObjectNode()
                            .put("eblPlatformIdentifier", "two@example.com")
                            .put("legalName", "Legal Name Two"))));
    asyncCounterpartPost(
        "/v1/surrender-requests", "1.0.0", jsonRequestBody);
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblSurrenderV10Platform.handleRequest(%s)".formatted(request));
    JsonNode jsonRequest = request.message().body().getJsonBody();
    String action = jsonRequest.get("action").asText();
    String srr = jsonRequest.get("surrenderRequestReference").asText();
    String tdr = tdrsBySrr.remove(srr);
    if (Objects.equals(
        EblSurrenderV10State.AMENDMENT_SURRENDER_REQUESTED, eblStatesById.get(tdr))) {
      eblStatesById.put(
          tdr,
          Objects.equals("SURR", action)
              ? EblSurrenderV10State.SURRENDERED_FOR_AMENDMENT
              : EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
      return request.createResponse(
          204,
          Map.of("Api-Version", List.of("1.0.0")),
          new ConformanceMessageBody(new ObjectMapper().createObjectNode()));
    } else if (Objects.equals(
        EblSurrenderV10State.DELIVERY_SURRENDER_REQUESTED, eblStatesById.get(tdr))) {
      eblStatesById.put(
          tdr,
          Objects.equals("SURR", action)
              ? EblSurrenderV10State.SURRENDERED_FOR_DELIVERY
              : EblSurrenderV10State.AVAILABLE_FOR_SURRENDER);
      return request.createResponse(
          204,
          Map.of("Api-Version", List.of("1.0.0")),
          new ConformanceMessageBody(new ObjectMapper().createObjectNode()));
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
                          .formatted(action, tdr, eblStatesById.get(tdr)))));
    }
  }
}
