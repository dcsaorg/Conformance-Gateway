package org.dcsa.conformance.standards.eblissuance.party;

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
import org.dcsa.conformance.standards.eblissuance.action.IssuanceResponseAction;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceResponseCode;

@Slf4j
public class EblIssuancePlatform extends ConformanceParty {
  private final Map<String, EblIssuanceState> eblStatesByTdr = new HashMap<>();

  public EblIssuancePlatform(
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
    eblStatesByTdr.forEach(
        (key, value) -> {
          ObjectNode entryNode = objectMapper.createObjectNode();
          entryNode.put("key", key);
          entryNode.put("value", value.name());
          arrayNodeEblStatesById.add(entryNode);
        });
    targetObjectNode.set("eblStatesByTdr", arrayNodeEblStatesById);
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StreamSupport.stream(sourceObjectNode.get("eblStatesByTdr").spliterator(), false)
        .forEach(
            entryNode ->
                eblStatesByTdr.put(
                    entryNode.get("key").asText(),
                    EblIssuanceState.valueOf(entryNode.get("value").asText())));
  }

  @Override
  protected void doReset() {
    eblStatesByTdr.clear();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(Map.entry(IssuanceResponseAction.class, this::sendIssuanceResponse));
  }

  private void sendIssuanceResponse(JsonNode actionPrompt) {
    log.info(
        "EblIssuancePlatform.sendIssuanceResponse(%s)".formatted(actionPrompt.toPrettyString()));
    String tdr = actionPrompt.get("tdr").asText();
    String irc = actionPrompt.get("irc").asText();

    if (eblStatesByTdr.containsKey(tdr)) {
      if (Objects.equals(IssuanceResponseCode.ACCEPTED.standardCode, irc)) {
        eblStatesByTdr.put(tdr, EblIssuanceState.ISSUED);
      } else {
        eblStatesByTdr.remove(tdr);
      }
    }

    asyncCounterpartPost(
        "/%s/ebl-issuance-responses".formatted(apiVersion.startsWith("3") ? "v3" : "v2"),
        new ObjectMapper()
            .createObjectNode()
            .put("transportDocumentReference", tdr)
            .put("issuanceResponseCode", irc));

    addOperatorLogEntry(
        "Sending issuance response with issuanceResponseCode '%s' for eBL with transportDocumentReference '%s'"
            .formatted(irc, tdr));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblIssuancePlatform.handleRequest(%s)".formatted(request));
    JsonNode jsonRequest = request.message().body().getJsonBody();

    String tdr = jsonRequest.get("document").get("transportDocumentReference").asText();

    ConformanceResponse response;
    if (!jsonRequest.get("document").has("issuingParty")) {
      response =
          request.createResponse(
              400,
              Map.of("Api-Version", List.of(apiVersion)),
              new ConformanceMessageBody(
                  new ObjectMapper()
                      .createObjectNode()
                      .put(
                          "message",
                          "Rejecting issuance request for document '%s' because the issuing party is missing"
                              .formatted(tdr))));
    } else if (!eblStatesByTdr.containsKey(tdr)) {
      eblStatesByTdr.put(tdr, EblIssuanceState.ISSUANCE_REQUESTED);
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
                          "message",
                          "Rejecting issuance request for document '%s' because it is in state '%s'"
                              .formatted(tdr, eblStatesByTdr.get(tdr)))));
    }
    addOperatorLogEntry(
        "Handling issuance request for eBL with transportDocumentReference '%s' (now in state '%s')"
            .formatted(tdr, eblStatesByTdr.get(tdr)));
    return response;
  }
}
