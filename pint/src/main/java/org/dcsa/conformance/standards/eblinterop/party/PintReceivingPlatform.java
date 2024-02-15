package org.dcsa.conformance.standards.eblinterop.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.eblinterop.crypto.SignedNodeSupport.parseSignedNode;
import static org.dcsa.conformance.standards.eblinterop.crypto.SignedNodeSupport.parseSignedNodeNoErrors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.eblinterop.action.ReceiverSupplyScenarioParametersAndStateSetupAction;
import org.dcsa.conformance.standards.eblinterop.action.ScenarioClass;
import org.dcsa.conformance.standards.eblinterop.crypto.Checksums;
import org.dcsa.conformance.standards.eblinterop.crypto.PayloadSigner;
import org.dcsa.conformance.standards.eblinterop.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.eblinterop.crypto.SignatureVerifier;
import org.dcsa.conformance.standards.eblinterop.models.ReceiverScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.TDReceiveState;

@Slf4j
public class PintReceivingPlatform extends ConformanceParty {
  private final Map<String, PintTransferState> eblStatesByTdr = new HashMap<>();

  private final PayloadSigner payloadSigner;

  public PintReceivingPlatform(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      PartyWebClient asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader,
      PayloadSigner payloadSigner) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        persistentMap,
        asyncWebClient,
        orchestratorAuthHeader);
    this.payloadSigner = payloadSigner;
  }

  protected SignatureVerifier getSignatureVerifer() {
    return PayloadSignerFactory.testKeySignatureVerifier();
  }

  @Override
  protected void doReset() {
    eblStatesByTdr.clear();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
      Map.entry(ReceiverSupplyScenarioParametersAndStateSetupAction.class, this::initiateState)
    );
  }


  private void initiateState(JsonNode actionPrompt) {
    log.info("EblInteropSendingPlatform.handleScenarioTypeAction(%s)".formatted(actionPrompt.toPrettyString()));
    var tdr = actionPrompt.required("transportDocumentReference").asText();
    var existing = persistentMap.load(tdr);
    if (existing != null){
      throw new IllegalStateException("Please do not reuse TDRs between scenarios in the conformance test");
    }
    var scenarioClass = ScenarioClass.valueOf(actionPrompt.required("scenarioClass").asText());
    var expectedRecipient = "12345-jane-doe";
    var receivingParameters = new ReceiverScenarioParameters(
      "CARX",
      "Jane Doe",
      scenarioClass == ScenarioClass.INVALID_RECIPIENT ? "12345-invalid" : expectedRecipient,
      "CargoX"
    );

    var tdState = TDReceiveState.newInstance(tdr);
    tdState.setExpectedReceiver(expectedRecipient);
    tdState.save(persistentMap);
    asyncOrchestratorPostPartyInput(
      OBJECT_MAPPER
        .createObjectNode()
        .put("actionId", actionPrompt.required("actionId").asText())
        .set("input", receivingParameters.toJson()));
    addOperatorLogEntry(
      "Finished ScenarioType");
  }


  public ConformanceResponse handleInitiateTransferRequest(ConformanceRequest request) {
    var transferRequest = request.message().body().getJsonBody();
    var td = transferRequest.path("transportDocument");
    var tdr = td.path("transportDocumentReference").asText();
    var receiveState = TDReceiveState.fromPersistentStore(persistentMap, tdr);
    var etc = transferRequest.path("envelopeTransferChain");
    var lastEtcEntry = etc.path(etc.size() - 1);
    var lastEnvelopeTransferChainEntrySignedContentChecksum = Checksums.sha256(lastEtcEntry.asText(""));
    var unsignedPayload = OBJECT_MAPPER.createObjectNode()
      .put("lastEnvelopeTransferChainEntrySignedContentChecksum", lastEnvelopeTransferChainEntrySignedContentChecksum);
    var responseCode = receiveState.recommendedFinishTransferResponse(transferRequest, getSignatureVerifer());
    var envelopeManifest = parseSignedNodeNoErrors(transferRequest.path("envelopeManifestSignedContent"));
    var documentChecksums = createDocumentChecksumArray(envelopeManifest);

    if (responseCode != null) {
      receiveState.updateTransferState(responseCode);
      unsignedPayload.put("responseCode", responseCode.name());
      unsignedPayload.set("receivedAdditionalDocumentChecksums", documentChecksums);

      var signedPayload = payloadSigner.sign(unsignedPayload.toString());
      var signedPayloadJsonNode = TextNode.valueOf(signedPayload);
      receiveState.save(persistentMap);
      return request.createResponse(
        responseCode.getHttpResponseCode(),
        Map.of("API-Version", List.of(apiVersion)),
        new ConformanceMessageBody(signedPayloadJsonNode)
      );
    }
    var envelopeReference = "...";
    var transportDocumentChecksum = Checksums.sha256CanonicalJson(td);
    unsignedPayload
      .put("envelopeReference", envelopeReference)
      .put("lastEnvelopeTransferChainEntrySignedContentChecksum", lastEnvelopeTransferChainEntrySignedContentChecksum)
      .put("transportDocumentChecksum", transportDocumentChecksum);
    unsignedPayload.set("missingAdditionalDocumentChecksums", documentChecksums);
    receiveState.save(persistentMap);
    return request.createResponse(
      201,
      Map.of("API-Version", List.of(apiVersion)),
      new ConformanceMessageBody(unsignedPayload)
    );
  }

  private static ArrayNode createDocumentChecksumArray(JsonNode envelopeManifest) {
    var visualizationChecksum = envelopeManifest.path("eBLVisualisationByCarrier")
      .path("documentChecksum")
      .asText(null);
    var allSupportingDocuments = envelopeManifest.path("supportingDocuments");
    var receivedDocuments = OBJECT_MAPPER.createArrayNode();
    if (visualizationChecksum != null) {
      receivedDocuments.add(visualizationChecksum);
    }
    for (var documentNode : allSupportingDocuments) {
      var documentChecksum = documentNode.path("documentChecksum").asText(null);
      if (documentChecksum != null) {
        receivedDocuments.add(documentChecksum);
      }
    }
    return receivedDocuments;
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {}

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {}

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblInteropPlatform.handleRequest(%s)".formatted(request));

    var url = request.url().replaceFirst("/++$", "");
    if (url.endsWith("/envelopes")) {
      return handleInitiateTransferRequest(request);
    }
    JsonNode jsonRequest = request.message().body().getJsonBody();

    String tdr = jsonRequest.path("transportDocument").path("transportDocumentReference").asText(null);

    ConformanceResponse response;
    if (tdr == null) {
      response =
          request.createResponse(
              400,
              Map.of("Api-Version", List.of(apiVersion)),
              new ConformanceMessageBody(
                  OBJECT_MAPPER
                      .createObjectNode()
                      .put(
                          "message",
                          "Rejecting issuance request as it has no transport document reference")));
    } else if (!eblStatesByTdr.containsKey(tdr)) {
      eblStatesByTdr.put(tdr, PintTransferState.ISSUANCE_REQUESTED);
      response =
          request.createResponse(
              204,
              Map.of("Api-Version", List.of(apiVersion)),
              new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
    } else {
      response =
          request.createResponse(
              409,
              Map.of("Api-Version", List.of(apiVersion)),
              new ConformanceMessageBody(
                  OBJECT_MAPPER
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
