package org.dcsa.conformance.standards.eblissuance.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerWithKey;
import org.dcsa.conformance.standards.eblissuance.action.CarrierScenarioParametersAction;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceRequestAction;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceResponseCode;

@Slf4j
public class EblIssuanceCarrier extends ConformanceParty {
  private final Map<String, EblIssuanceState> eblStatesByTdr = new HashMap<>();
  private final Map<String, String> sirsByTdr = new HashMap<>();
  private final Map<String, String> brsByTdr = new HashMap<>();
  private final PayloadSignerWithKey payloadSigner;

  public EblIssuanceCarrier(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      PartyWebClient webClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader,
      PayloadSignerWithKey payloadSigner) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        persistentMap,
        webClient,
        orchestratorAuthHeader);
    this.payloadSigner = payloadSigner;
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    targetObjectNode.set(
        "eblStatesByTdr",
        StateManagementUtil.storeMap(OBJECT_MAPPER, eblStatesByTdr, EblIssuanceState::name));
    targetObjectNode.set("sirsByTdr", StateManagementUtil.storeMap(OBJECT_MAPPER, sirsByTdr));
    targetObjectNode.set("brsByTdr", StateManagementUtil.storeMap(OBJECT_MAPPER, brsByTdr));
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
    return Map.ofEntries(
      Map.entry(IssuanceRequestAction.class, this::sendIssuanceRequest),
      Map.entry(CarrierScenarioParametersAction.class, this::supplyScenarioParameters)
    );
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info(
      "EblIssuanceCarrier.supplyScenarioParameters(%s)"
        .formatted(actionPrompt.toPrettyString()));
    var carrierScenarioParameters =
        new CarrierScenarioParameters(
          payloadSigner.getPublicKeyInPemFormat());
    asyncOrchestratorPostPartyInput(
      OBJECT_MAPPER
        .createObjectNode()
        .put("actionId", actionPrompt.required("actionId").asText())
        .set("input", carrierScenarioParameters.toJson()));
    addOperatorLogEntry(
      "Submitting CarrierScenarioParameters: %s"
        .formatted(carrierScenarioParameters.toJson().toPrettyString()));
  }

  private void sendIssuanceRequest(JsonNode actionPrompt) {
    log.info("EblIssuanceCarrier.sendIssuanceRequest(%s)".formatted(actionPrompt.toPrettyString()));
    SuppliedScenarioParameters ssp = SuppliedScenarioParameters.fromJson(actionPrompt.get("ssp"));
    var dsp = DynamicScenarioParameters.fromJson(actionPrompt.required("dsp"));
    var eblType = dsp.eblType();
    String tdr =
        actionPrompt.has("tdr")
            ? actionPrompt.path("tdr").asText()
            : UUID.randomUUID().toString().substring(20);
    String sir = sirsByTdr.computeIfAbsent(tdr, ignoredTdr -> UUID.randomUUID().toString());
    String br =
        brsByTdr.computeIfAbsent(tdr, ignoredTdr -> UUID.randomUUID().toString().substring(35));

    boolean isCorrect = actionPrompt.path("isCorrect").asBoolean();
    var isAmended = actionPrompt.path("isAmended").asBoolean(false);
    if (isCorrect) {
      eblStatesByTdr.put(tdr, EblIssuanceState.ISSUANCE_REQUESTED);
    }

    var jsonRequestBody =
      (ObjectNode)JsonToolkit.templateFileToJsonNode(
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
      if (apiVersion.startsWith("2.")) {
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
      ((ObjectNode) jsonRequestBody.path("document").path("documentParties")).remove("issuingParty");
    }
    if (isAmended) {
      var sealObj = (ObjectNode)jsonRequestBody.path("document").path("utilizedTransportEquipments").path(0).path("seals").path(0);
      var sealNumber = sealObj.path("sealNumber").asText("") + "X";
      sealObj.put("sealNumber", sealNumber);
    }

    var tdChecksum = Checksums.sha256CanonicalJson(jsonRequestBody.path("document"));
    var issueToChecksum = Checksums.sha256CanonicalJson(jsonRequestBody.path("issueTo"));
    jsonRequestBody.set("eBLVisualisationByCarrier",getSupportingDocumentObject());
    var eBLVisualisationByCarrier = Checksums.sha256CanonicalJson(jsonRequestBody.path("eBLVisualisationByCarrier"));
    var issuanceManifest = OBJECT_MAPPER.createObjectNode()
        .put("documentChecksum", tdChecksum)
        .put("issueToChecksum", issueToChecksum)
      .put("eBLVisualisationByCarrierChecksum",eBLVisualisationByCarrier);

    jsonRequestBody.put("issuanceManifestSignedContent", payloadSigner.sign(issuanceManifest.toString()));

    syncCounterpartPost(
        "/v%s/ebl-issuance-requests".formatted(apiVersion.charAt(0)),
        jsonRequestBody);

    addOperatorLogEntry(
        "Sent a %s issuance request for eBL with transportDocumentReference '%s' (now in state '%s')"
            .formatted(isCorrect ? "correct" : "incorrect", tdr, eblStatesByTdr.get(tdr)));
  }

  private ObjectNode getSupportingDocumentObject(){
    var document = generateDocument();
    return OBJECT_MAPPER.createObjectNode()
    .put("name","test-iss-document")
      .put("content",document)
      .put("mediatype","application/octet-stream");
  }

  private static byte[] generateDocument() {
    var uuid = UUID.randomUUID();
    var doc = new byte[256 + 16];
    for (int i = 0 ; i < 256 ; i++) {
      // Include every byte so that we are certain that nothing corrupts the transfer.
      doc[i] = (byte)i;
    }
    // Add a UUID to ensure it is unique, such that the testers do not have to "remove" the
    // document between every test.
    putLong(uuid.getMostSignificantBits(), doc, 256);
    putLong(uuid.getLeastSignificantBits(), doc, 256 + 8);
    return doc;
  }

  private static void putLong(long value, byte[] array, int offset) {
    for (int i = 0 ; i < 8 ; i++) {
      array[offset + i] = (byte)((value >>> i) & 0xff);
    }
  }
  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblIssuanceCarrier.handleRequest(%s)".formatted(request));
    JsonNode jsonRequest = request.message().body().getJsonBody();
    String tdr = jsonRequest.path("transportDocumentReference").asText();
    String irc = jsonRequest.path("issuanceResponseCode").asText();

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
          new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
    } else {
      return request.createResponse(
          409,
          Map.of("Api-Version", List.of(apiVersion)),
          new ConformanceMessageBody(
            OBJECT_MAPPER
                  .createObjectNode()
                  .put(
                      "message",
                      "Rejecting '%s' for eBL '%s' because it is in state '%s'"
                          .formatted(irc, tdr, eblStatesByTdr.get(tdr)))));
    }
  }
}
