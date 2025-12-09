package org.dcsa.conformance.standards.eblissuance.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
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
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceResponseCode;
import org.dcsa.conformance.standards.eblissuance.action.PlatformScenarioParametersAction;

@Slf4j
public class EblIssuancePlatform extends ConformanceParty {
  private final Map<String, EblIssuanceState> eblStatesByTdr = new HashMap<>();
  private final Map<String, String> tdr2PendingChecksum = new HashMap<>();
  private final Map<String, Boolean> knownChecksums = new HashMap<>();

  private String scenarioResponseCode = "";

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
    targetObjectNode.set(
        "responseCode", OBJECT_MAPPER.createObjectNode().put("code", scenarioResponseCode));
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
    scenarioResponseCode = sourceObjectNode.path("responseCode").path("code").asText("");
  }

  @Override
  protected void doReset() {
    eblStatesByTdr.clear();
    tdr2PendingChecksum.clear();
    knownChecksums.clear();
    scenarioResponseCode = "";
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(PlatformScenarioParametersAction.class, this::supplyScenarioParameters));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info(
        "EblIssuancePlatform.supplyScenarioParameters(%s)"
            .formatted(actionPrompt.toPrettyString()));
    scenarioResponseCode = actionPrompt.required("responseCode").asText();

    SuppliedScenarioParameters suppliedScenarioParameters =
        new SuppliedScenarioParameters(
            IssuanceResponseCode.forStandardCode(scenarioResponseCode).sendToPlatform,
            "DCSA issue to party",
            "W3C",
            "1234-issue-to",
            "DCSA",
            "DCSA Shipper",
            "W3C",
            "5677-cn-or-end",
            "DCSA",
            "DCSA Consignee/Endorsee",
            "W3C",
            "5678-cn-or-end",
            "DCSA",
            "DCSA Issuing Party",
            "W3C",
            "5679-cn-or-end",
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

    var document=jsonRequest.path("document");

    var tdr = document.path("transportDocumentReference").asText(null);

    var checksum = document.isMissingNode() ? null : Checksums.sha256CanonicalJson(document);

    var state = eblStatesByTdr.get(tdr);

    ConformanceResponse response;
    if (tdr == null || !document.path("documentParties").has("issuingParty")) {
      addOperatorLogEntry(
          "Rejecting issuance request for eBL with transportDocumentReference '%s' (invalid)"
              .formatted(tdr));
      return return400(
          request,
          "Rejecting issuance request for document '%s' because the issuing party is missing or the TDR could not be resolved"
              .formatted(tdr));
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

    if (scenarioResponseCode.isEmpty()) {
      String value = jsonRequest.path("issueTo").path("sendToPlatform").asText();
      scenarioResponseCode =
          switch (value) {
            case "DCSA" -> "ISSU";
            case "DCSB" -> "BREQ";
            case "DCSR" -> "REFU";
            default -> "ISSU";
          };
    }

    var platformResponse =
        OBJECT_MAPPER
            .createObjectNode()
            .put("transportDocumentReference", tdr)
            .put("issuanceResponseCode", scenarioResponseCode);
    if (scenarioResponseCode.equals("BREQ") || scenarioResponseCode.equals("REFU")) {
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

  private ConformanceResponse return400(ConformanceRequest request, String message) {
    ObjectNode response =
        (ObjectNode)
            JsonToolkit.templateFileToJsonNode(
                "/standards/eblissuance/messages/eblissuance-v3.0.0-error-message.json",
                Map.of(
                    "HTTP_METHOD_PLACEHOLDER",
                    request.method(),
                    "REQUEST_URI_PLACEHOLDER",
                    request.url(),
                    "REFERENCE_PLACEHOLDER",
                    UUID.randomUUID().toString(),
                    "ERROR_DATE_TIME_PLACEHOLDER",
                    LocalDateTime.now().format(JsonToolkit.ISO_8601_DATE_TIME_FORMAT),
                    "ERROR_MESSAGE_PLACEHOLDER",
                    message));

    return request.createResponse(
        400, Map.of(API_VERSION, List.of(apiVersion)), new ConformanceMessageBody(response));
  }
}
