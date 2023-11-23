package org.dcsa.conformance.standards.eblsurrender.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.state.StateManagementUtil;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.eblsurrender.action.SurrenderRequestAction;

@Slf4j
public class EblSurrenderPlatform extends ConformanceParty {
  private final Map<String, EblSurrenderState> eblStatesById = new HashMap<>();
  private final Map<String, String> tdrsBySrr = new HashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public EblSurrenderPlatform(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      Consumer<ConformanceRequest> asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        persistentMap,
        asyncWebClient,
        orchestratorAuthHeader);
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    targetObjectNode.set("eblStatesById", StateManagementUtil.storeMap(objectMapper, eblStatesById, EblSurrenderState::name));
    targetObjectNode.set("tdrsBySrr", StateManagementUtil.storeMap(objectMapper, tdrsBySrr));
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StateManagementUtil.restoreIntoMap(eblStatesById, sourceObjectNode.get("eblStatesById"), EblSurrenderState::valueOf);
    StateManagementUtil.restoreIntoMap(tdrsBySrr, sourceObjectNode.get("tdrsBySrr"));
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
              new ConformanceMessageBody(objectMapper.createObjectNode()));
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
              new ConformanceMessageBody(objectMapper.createObjectNode()));
    } else {
      response =
          request.createResponse(
              409,
              Map.of("Api-Version", List.of(apiVersion)),
              new ConformanceMessageBody(
                  objectMapper
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
