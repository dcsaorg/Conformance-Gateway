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
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.state.StateManagementUtil;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceResponseAction;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceResponseCode;
import org.dcsa.conformance.standards.eblissuance.action.SupplyScenarioParametersAction;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

@Slf4j
public class EblIssuancePlatform extends ConformanceParty {
  private final Map<String, EblIssuanceState> eblStatesByTdr = new HashMap<>();
  private final Map<String, String> tdr2PendingChecksum = new HashMap<>();
  private final Map<String, Boolean> knownChecksums = new HashMap<>();

  public EblIssuancePlatform(
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
    ArrayNode arrayNodeEblStatesById = OBJECT_MAPPER.createArrayNode();
    eblStatesByTdr.forEach(
        (key, value) -> {
          ObjectNode entryNode = OBJECT_MAPPER.createObjectNode();
          entryNode.put("key", key);
          entryNode.put("value", value.name());
          arrayNodeEblStatesById.add(entryNode);
        });
    targetObjectNode.set("eblStatesByTdr", arrayNodeEblStatesById);

    targetObjectNode.set("tdr2PendingChecksum", StateManagementUtil.storeMap(OBJECT_MAPPER, tdr2PendingChecksum));
    targetObjectNode.set("knownChecksums", StateManagementUtil.storeMap(OBJECT_MAPPER, knownChecksums, String::valueOf));
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StreamSupport.stream(sourceObjectNode.get("eblStatesByTdr").spliterator(), false)
        .forEach(
            entryNode ->
                eblStatesByTdr.put(
                    entryNode.get("key").asText(),
                    EblIssuanceState.valueOf(entryNode.get("value").asText())));

    StateManagementUtil.restoreIntoMap(tdr2PendingChecksum, sourceObjectNode.path("tdr2PendingChecksum"));
    StateManagementUtil.restoreIntoMap(knownChecksums, sourceObjectNode.path("knownChecksums"), Boolean::valueOf);
  }

  @Override
  protected void doReset() {
    eblStatesByTdr.clear();
    tdr2PendingChecksum.clear();
    knownChecksums.clear();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(IssuanceResponseAction.class, this::sendIssuanceResponse),
        Map.entry(SupplyScenarioParametersAction.class, this::supplyScenarioParameters));
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
        var checksum = tdr2PendingChecksum.get(tdr);
        knownChecksums.remove(checksum);
      }
    }

    syncCounterpartPost(
        "/v%s/ebl-issuance-responses".formatted(apiVersion.charAt(0)),
      OBJECT_MAPPER
            .createObjectNode()
            .put("transportDocumentReference", tdr)
            .put("issuanceResponseCode", irc));

    addOperatorLogEntry(
        "Sent issuance response with issuanceResponseCode '%s' for eBL with transportDocumentReference '%s'"
            .formatted(irc, tdr));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info(
        "EblIssuancePlatform.supplyScenarioParameters(%s)"
            .formatted(actionPrompt.toPrettyString()));
    SuppliedScenarioParameters suppliedScenarioParameters =
        new SuppliedScenarioParameters(
            "BOLE",
            "DCSA CTK Issue To party",
            "1234-issue-to",
            "Bolero",
            // These are ignored for blank ones, so we can provide them unconditionally.
            "DCSA CTK Consignee/Endorsee",
            "5678-cn-or-end",
            "Bolero");
    asyncOrchestratorPostPartyInput(
      OBJECT_MAPPER
            .createObjectNode()
            .put("actionId", actionPrompt.required("actionId").asText())
            .set("input", suppliedScenarioParameters.toJson()));
    addOperatorLogEntry(
        "Submitting SuppliedScenarioParameters: %s"
            .formatted(suppliedScenarioParameters.toJson().toPrettyString()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblIssuancePlatform.handleRequest(%s)".formatted(request));
    JsonNode jsonRequest = request.message().body().getJsonBody();

    var tdr = jsonRequest.path("document").path("transportDocumentReference").asText(null);
    var checksum = Checksums.sha256CanonicalJson(jsonRequest.path("document"));
    var state = eblStatesByTdr.get(tdr);

    ConformanceResponse response;
    if (tdr == null || !jsonRequest.path("document").has("issuingParty")) {
      addOperatorLogEntry(
        "Rejecting issuance request for eBL with transportDocumentReference '%s' (invalid)"
          .formatted(tdr));
      return request.createResponse(
              400,
              Map.of("Api-Version", List.of(apiVersion)),
              new ConformanceMessageBody(
                OBJECT_MAPPER
                      .createObjectNode()
                      .put(
                          "message",
                          "Rejecting issuance request for document '%s' because the issuing party is missing or the TDR could not be resolved"
                              .formatted(tdr))));
    }
    if (state == null || state == EblIssuanceState.ISSUED && !knownChecksums.containsKey(checksum)) {
      eblStatesByTdr.put(tdr, EblIssuanceState.ISSUANCE_REQUESTED);
      knownChecksums.put(checksum, Boolean.TRUE);
      tdr2PendingChecksum.put(tdr, checksum);
      response =
          request.createResponse(
              204,
              Map.of("Api-Version", List.of(apiVersion)),
              new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
    } else {
      String message;
      if (state == EblIssuanceState.ISSUANCE_REQUESTED) {
        message = "Rejecting issuance request for document '%s' because there is a pending issuance request for it"
          .formatted(tdr);
      } else {
        message = "Rejecting issuance request for document '%s' because has been issued in the past"
          .formatted(tdr);
      }
      response =
          request.createResponse(
              409,
              Map.of("Api-Version", List.of(apiVersion)),
              new ConformanceMessageBody(
                OBJECT_MAPPER
                      .createObjectNode()
                      .put("message", message)));
    }
    addOperatorLogEntry(
        "Handling issuance request for eBL with transportDocumentReference '%s' (now in state '%s')"
            .formatted(tdr, eblStatesByTdr.get(tdr)));
    return response;
  }
}
