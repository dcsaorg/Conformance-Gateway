package org.dcsa.conformance.standards.eblsurrender.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.state.StateManagementUtil;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.eblsurrender.action.SurrenderRequestResponseAction;

@Slf4j
public class EblSurrenderPlatform extends ConformanceParty {
  private final Map<String, EblSurrenderState> eblStatesById = new HashMap<>();
  private final Map<String, String> tdrsBySrr = new HashMap<>();

  public EblSurrenderPlatform(
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
    targetObjectNode.set(
        "eblStatesById", StateManagementUtil.storeMap(eblStatesById, EblSurrenderState::name));
    targetObjectNode.set("tdrsBySrr", StateManagementUtil.storeMap(tdrsBySrr));
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StateManagementUtil.restoreIntoMap(
        eblStatesById, sourceObjectNode.get("eblStatesById"), EblSurrenderState::valueOf);
    StateManagementUtil.restoreIntoMap(tdrsBySrr, sourceObjectNode.get("tdrsBySrr"));
  }

  @Override
  protected void doReset() {
    eblStatesById.clear();
    tdrsBySrr.clear();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(Map.entry(SurrenderRequestResponseAction.class, this::requestSurrender));
  }

  private void requestSurrender(JsonNode actionPrompt) {
    log.info("EblSurrenderPlatform.requestSurrender(%s)".formatted(actionPrompt.toPrettyString()));
    SuppliedScenarioParameters ssp =
        SuppliedScenarioParameters.fromJson(actionPrompt.get("suppliedScenarioParameters"));
    String srr = UUID.randomUUID().toString();
    String tdr = ssp.transportDocumentReference();
    boolean forAmendment = actionPrompt.get("forAmendment").booleanValue();
    String src = forAmendment ? "AREQ" : "SREQ";
    String action =
        ssp.issueToParty().get("eblPlatform") != null
            ? ssp.issueToParty().get("eblPlatform").asText()
            : null;
    if (action != null && action.equals("WAVER")) {
      tdr = tdr + action;
    }
    tdrsBySrr.put(srr, tdr);
    eblStatesById.put(
        tdr,
        forAmendment
            ? EblSurrenderState.AMENDMENT_SURRENDER_REQUESTED
            : EblSurrenderState.DELIVERY_SURRENDER_REQUESTED);
    String reasonCode =
        actionPrompt.get("isSwitchToPaper") != null
                && actionPrompt.get("isSwitchToPaper").booleanValue()
            ? "SWTP"
            : "";
    JsonNode jsonRequestBody =
        JsonToolkit.templateFileToJsonNode(
            "/standards/eblsurrender/messages/eblsurrender-api-v%s-request.json"
                .formatted(apiVersion),
            Map.ofEntries(
                Map.entry("SURRENDER_REQUEST_REFERENCE_PLACEHOLDER", srr),
                Map.entry("TRANSPORT_DOCUMENT_REFERENCE_PLACEHOLDER", tdr),
                Map.entry("SURRENDER_REQUEST_CODE_PLACEHOLDER", src),
                Map.entry("REASON_CODE", reasonCode),
                Map.entry("ISSUE_TO_PARTY", ssp.issueToParty().toString()),
                Map.entry("SURRENDEREE_PARTY", ssp.surrendereeParty().toString()),
                Map.entry("CARRIER_PARTY", ssp.carrierParty().toString()),
                Map.entry("ACTION_DATE_TIME_PLACEHOLDER", Instant.now().toString()),
                Map.entry(
                    "SURRENDER_ACTION_CODE_PLACEHOLDER",
                    forAmendment ? "SURRENDER_FOR_AMENDMENT" : "SURRENDER_FOR_DELIVERY")));

    syncCounterpartPost(
        "/v%s/ebl-surrender-requests".formatted(apiVersion.charAt(0)), jsonRequestBody);

    addOperatorLogEntry(
        "Sent surrender request with surrenderRequestCode '%s' and surrenderRequestReference '%s' for eBL with transportDocumentReference '%s'"
            .formatted(src, srr, tdr));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblSurrenderPlatform.handleRequest(%s)".formatted(request));
    JsonNode jsonRequest = request.message().body().getJsonBody();
    String action = jsonRequest.get("org/dcsa/conformance/standards/standardscommons").asText();
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
              Map.of(API_VERSION, List.of(apiVersion)),
              new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
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
              Map.of(API_VERSION, List.of(apiVersion)),
              new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
    } else {
      response =
          request.createResponse(
              409,
              Map.of(API_VERSION, List.of(apiVersion)),
              new ConformanceMessageBody(
                  OBJECT_MAPPER
                      .createObjectNode()
                      .put(
                          "comments",
                          "Rejecting '%s' for document '%s' because it is in state '%s'"
                              .formatted(action, tdr, eblStatesById.get(tdr)))));
    }

    addOperatorLogEntry(
        "Handling notification with action '%s' and surrenderRequestReference '%s' for eBL with transportDocumentReference '%s' (now in state '%s')"
            .formatted(action, srr, tdr, eblStatesById.get(tdr)));
    return response;
  }
}
