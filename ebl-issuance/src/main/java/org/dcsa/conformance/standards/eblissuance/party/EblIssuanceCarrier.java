package org.dcsa.conformance.standards.eblissuance.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.dcsa.conformance.standards.eblissuance.action.IssuanceRequestAction;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceResponseCode;

@Slf4j
public class EblIssuanceCarrier extends ConformanceParty {
  private final Map<String, EblIssuanceState> eblStatesByTdr = new HashMap<>();
  private final Map<String, String> sirsByTdr = new HashMap<>();
  private final Map<String, String> brsByTdr = new HashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public EblIssuanceCarrier(
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
        "eblStatesByTdr",
        StateManagementUtil.storeMap(objectMapper, eblStatesByTdr, EblIssuanceState::name));
    targetObjectNode.set("sirsByTdr", StateManagementUtil.storeMap(objectMapper, sirsByTdr));
    targetObjectNode.set("brsByTdr", StateManagementUtil.storeMap(objectMapper, brsByTdr));
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StateManagementUtil.restoreIntoMap(
        eblStatesByTdr, sourceObjectNode.get("eblStatesByTdr"), EblIssuanceState::valueOf);
    StateManagementUtil.restoreIntoMap(sirsByTdr, sourceObjectNode.get("sirsByTdr"));
    StateManagementUtil.restoreIntoMap(brsByTdr, sourceObjectNode.get("brsByTdr"));
  }

  @Override
  protected void doReset() {
    eblStatesByTdr.clear();
    sirsByTdr.clear();
    brsByTdr.clear();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(Map.entry(IssuanceRequestAction.class, this::sendIssuanceRequest));
  }

  private void sendIssuanceRequest(JsonNode actionPrompt) {
    log.info("EblIssuanceCarrier.sendIssuanceRequest(%s)".formatted(actionPrompt.toPrettyString()));
    SuppliedScenarioParameters ssp = SuppliedScenarioParameters.fromJson(actionPrompt.get("ssp"));
    var dsp = DynamicScenarioParameters.fromJson(actionPrompt.required("dsp"));
    var eblType = dsp.eblType();
    String tdr =
        actionPrompt.has("tdr")
            ? actionPrompt.get("tdr").asText()
            : UUID.randomUUID().toString().substring(20);
    String sir = sirsByTdr.computeIfAbsent(tdr, ignoredTdr -> UUID.randomUUID().toString());
    String br =
        brsByTdr.computeIfAbsent(tdr, ignoredTdr -> UUID.randomUUID().toString().substring(35));

    boolean isCorrect = actionPrompt.get("isCorrect").asBoolean();
    var isAmended = actionPrompt.path("isAmended").asBoolean(false);
    if (isCorrect) {
      eblStatesByTdr.put(tdr, EblIssuanceState.ISSUANCE_REQUESTED);
    }

    var jsonRequestBody =
        JsonToolkit.templateFileToJsonNode(
            "/standards/eblissuance/messages/eblissuance-v%s-request.json"
                .formatted(apiVersion),
            Map.ofEntries(
                Map.entry("TRANSPORT_DOCUMENT_REFERENCE_PLACEHOLDER", tdr),
                Map.entry("SHIPPING_INSTRUCTION_REFERENCE_PLACEHOLDER", sir),
                Map.entry("BOOKING_REFERENCE_PLACEHOLDER", br),
                Map.entry("SEND_TO_PLATFORM_PLACEHOLDER", ssp.sendToPlatform()),
                Map.entry("ISSUE_TO_LEGAL_NAME_PLACEHOLDER", ssp.issueToLegalName()),
                Map.entry("ISSUE_TO_PARTY_CODE_PLACEHOLDER", ssp.issueToPartyCode()),
                Map.entry("ISSUE_TO_CODE_LIST_NAME_PLACEHOLDER", ssp.issueToCodeListName()),
                Map.entry("CONSIGNEE_LEGAL_NAME_PLACEHOLDER", ssp.consigneeOrEndorseeLegalName()),
                Map.entry("CONSIGNEE_PARTY_CODE_PLACEHOLDER", ssp.consigneeOrEndorseePartyCode()),
                Map.entry("CONSIGNEE_CODE_LIST_NAME_PLACEHOLDER", ssp.consigneeOrEndorseeCodeListName())
        ));

    if (eblType.isToOrder()) {
      var td = (ObjectNode)jsonRequestBody.path("document");
      td.put("isToOrder", true);
      if (apiVersion.startsWith("2.") || apiVersion.equals("3.0.0-Beta-1")) {
        var documentParties = (ArrayNode)td.path("documentParties");
        var cnIdx = -1;
        for (int i = 0 ; i < documentParties.size() ; i++) {
          if (documentParties.path(i).path("partyFunction").asText("?").equals("CN")) {
            cnIdx = i;
            break;
          }
        }
        if (eblType.isBlankEbl()) {
          documentParties.remove(cnIdx);
        } else {
          ((ObjectNode)documentParties.path(cnIdx)).put("partyFunction", "END");
        }
      } else {
        var documentParties = (ObjectNode)td.path("documentParties");
        if (eblType.isBlankEbl()) {
          documentParties.remove("consignee");
          documentParties.remove("endorsee");
        } else {
          var consignee = documentParties.remove("consignee");
          documentParties.set("endorsee", consignee);
        }
      }
    }

    if (!isCorrect) {
      ((ObjectNode) jsonRequestBody.get("document")).remove("issuingParty");
    }
    if (isAmended) {
      var sealObj = (ObjectNode)jsonRequestBody.path("document").path("utilizedTransportEquipments").path(0).path("seals").path(0);
      var sealNumber = sealObj.path("sealNumber").asText("") + "X";
      sealObj.put("sealNumber", sealNumber);
    }

    syncCounterpartPost(
        "/v%s/ebl-issuance-requests".formatted(apiVersion.charAt(0)),
        jsonRequestBody);

    addOperatorLogEntry(
        "Sent a %s issuance request for eBL with transportDocumentReference '%s' (now in state '%s')"
            .formatted(isCorrect ? "correct" : "incorrect", tdr, eblStatesByTdr.get(tdr)));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblIssuanceCarrier.handleRequest(%s)".formatted(request));
    JsonNode jsonRequest = request.message().body().getJsonBody();
    String tdr = jsonRequest.get("transportDocumentReference").asText();
    String irc = jsonRequest.get("issuanceResponseCode").asText();

    if (Objects.equals(EblIssuanceState.ISSUANCE_REQUESTED, eblStatesByTdr.get(tdr))) {
      if (Objects.equals(IssuanceResponseCode.ACCEPTED.standardCode, irc)) {
        eblStatesByTdr.put(tdr, EblIssuanceState.ISSUED);
      }

      addOperatorLogEntry(
          "Handling issuance response with issuanceResponseCode '%s' for eBL with transportDocumentReference '%s' (now in state '%s')"
              .formatted(irc, tdr, eblStatesByTdr.get(tdr)));

      return request.createResponse(
          204,
          Map.of("Api-Version", List.of(apiVersion)),
          new ConformanceMessageBody(objectMapper.createObjectNode()));
    } else {
      return request.createResponse(
          409,
          Map.of("Api-Version", List.of(apiVersion)),
          new ConformanceMessageBody(
              objectMapper
                  .createObjectNode()
                  .put(
                      "message",
                      "Rejecting '%s' for eBL '%s' because it is in state '%s'"
                          .formatted(irc, tdr, eblStatesByTdr.get(tdr)))));
    }
  }
}
