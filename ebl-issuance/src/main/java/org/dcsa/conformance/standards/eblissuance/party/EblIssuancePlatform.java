package org.dcsa.conformance.standards.eblissuance.party;

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
import org.dcsa.conformance.core.state.StateManagementUtil;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.eblissuance.action.SupplyScenarioParametersAction;

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

    targetObjectNode.set("tdr2PendingChecksum", StateManagementUtil.storeMap(tdr2PendingChecksum));
    targetObjectNode.set(
        "knownChecksums", StateManagementUtil.storeMap(knownChecksums, String::valueOf));
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StreamSupport.stream(sourceObjectNode.get("eblStatesByTdr").spliterator(), false)
        .forEach(
            entryNode ->
                eblStatesByTdr.put(
                    entryNode.get("key").asText(),
                    EblIssuanceState.valueOf(entryNode.get("value").asText())));

    StateManagementUtil.restoreIntoMap(
        tdr2PendingChecksum, sourceObjectNode.path("tdr2PendingChecksum"));
    StateManagementUtil.restoreIntoMap(
        knownChecksums, sourceObjectNode.path("knownChecksums"), Boolean::valueOf);
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
        Map.entry(SupplyScenarioParametersAction.class, this::supplyScenarioParameters));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info(
        "EblIssuancePlatform.supplyScenarioParameters(%s)"
            .formatted(actionPrompt.toPrettyString()));

    JsonNode code = actionPrompt.path("responseCode");
    if (code != null) {
      persistentMap.save("responseCode", code);
    }

    SuppliedScenarioParameters suppliedScenarioParameters =
        new SuppliedScenarioParameters(
            "DCSA",
            "DCSA issue to party",
            "1234-issue-to",
            "DCSA",
            // These are ignored for blank ones, so we can provide them unconditionally.
            "DCSA Consignee/Endorsee",
            "5678-cn-or-end",
            "DCSA");
    asyncOrchestratorPostPartyInput(
        actionPrompt.required("actionId").asText(), suppliedScenarioParameters.toJson());
    addOperatorLogEntry(
        "Submitting SuppliedScenarioParameters: %s"
            .formatted(suppliedScenarioParameters.toJson().toPrettyString()));
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblIssuancePlatform.handleRequest(%s)".formatted(request));
    JsonNode jsonRequest = request.message().body().getJsonBody();

    var tdr = jsonRequest.path("document").path("transportDocumentReference").asText(null);
    JsonNode responseCodeNode = persistentMap.load("responseCode");

    String irc;
    if (responseCodeNode != null) {
      irc = responseCodeNode.asText();
    } else {
      String value = jsonRequest.path("issueTo").path("sendToPlatform").asText();
      irc =
          switch (value) {
            case "DCSA" -> "ISSU";
            case "DCSB" -> "BREQ";
            case "DCSR" -> "REFU";
            default -> "";
          };
    }
    var checksum = Checksums.sha256CanonicalJson(jsonRequest.path("document"));
    var state = eblStatesByTdr.get(tdr);

    ConformanceResponse response;
    if (tdr == null || !jsonRequest.path("document").path("documentParties").has("issuingParty")) {
      addOperatorLogEntry(
          "Rejecting issuance request for eBL with transportDocumentReference '%s' (invalid)"
              .formatted(tdr));
      return request.createResponse(
          400,
          Map.of(API_VERSION, List.of(apiVersion)),
          new ConformanceMessageBody(
              OBJECT_MAPPER
                  .createObjectNode()
                  .put(
                      "message",
                      "Rejecting issuance request for document '%s' because the issuing party is missing or the TDR could not be resolved"
                          .formatted(tdr))));
    }
    if (state == null
        || state == EblIssuanceState.ISSUED && !knownChecksums.containsKey(checksum)) {
      eblStatesByTdr.put(tdr, EblIssuanceState.ISSUANCE_REQUESTED);
      knownChecksums.put(checksum, Boolean.TRUE);
      tdr2PendingChecksum.put(tdr, checksum);
      response =
          request.createResponse(
              204,
              Map.of(API_VERSION, List.of(apiVersion)),
              new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
    } else {
      String message;
      if (state == EblIssuanceState.ISSUANCE_REQUESTED) {
        message =
            "Rejecting issuance request for document '%s' because there is a pending issuance request for it"
                .formatted(tdr);
      } else {
        message =
            "Rejecting issuance request for document '%s' because has been issued in the past"
                .formatted(tdr);
      }
      response =
          request.createResponse(
              409,
              Map.of(API_VERSION, List.of(apiVersion)),
              new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode().put("message", message)));
    }

    var platformResponse =
        OBJECT_MAPPER
            .createObjectNode()
            .put("transportDocumentReference", tdr)
            .put("issuanceResponseCode", irc);
    if (irc.equals("BREQ")) {
      platformResponse
          .putArray("errors")
          .addObject()
          .put("reason", "Rejected as required by the conformance scenario")
          .put("errorCode", "DCSA-123");
    }
    asyncCounterpartNotification(null, "/v3/ebl-issuance-responses", platformResponse);

    addOperatorLogEntry(
        "Handling issuance request for eBL with transportDocumentReference '%s' (now in state '%s')"
            .formatted(tdr, eblStatesByTdr.get(tdr)));
    return response;
  }
}
