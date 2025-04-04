package org.dcsa.conformance.standards.eblinterop.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.eblinterop.action.PintResponseCode.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
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
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerWithKey;
import org.dcsa.conformance.standards.eblinterop.action.*;
import org.dcsa.conformance.standards.eblinterop.models.ReceiverScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.SenderScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.TDReceiveState;

@Slf4j
public class PintReceivingPlatform extends ConformanceParty {

  private static final String EXPECTED_RECEIVER_PARTY_CODE = "12345-jane-doe";
  private static final PayloadSignerWithKey RECEIVING_PLATFORM_PAYLOAD_SIGNER = PayloadSignerFactory.receiverPayloadSigner();

  private final Map<String, String> envelopeReferences = new HashMap<>();

  public PintReceivingPlatform(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      PartyWebClient asyncWebClient,
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
  protected void doReset() {
    envelopeReferences.clear();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
      Map.entry(ReceiverSupplyScenarioParametersAndStateSetupAction.class, this::initiateState),
      Map.entry(ResetScenarioClassAction.class, this::resetScenarioClass),
      Map.entry(SupplyValidationEndpointScenarioParametersAction.class, this::providedReceiverValidationScenarioParameters)
    );
  }

  private void resetScenarioClass(JsonNode actionPrompt) {
    log.info("EblInteropSendingPlatform.resetScenarioClass(%s)".formatted(actionPrompt.toPrettyString()));
    var tdr = actionPrompt.required("transportDocumentReference").asText();
    var existing = persistentMap.load(tdr);
    if (existing == null){
      throw new IllegalStateException("TDR must be known for a resetScenarioClass");
    }
    var scenarioClass = ScenarioClass.valueOf(actionPrompt.required("scenarioClass").asText());

    var tdState = TDReceiveState.fromPersistentStore(persistentMap, tdr);
    tdState.setScenarioClass(scenarioClass);
    tdState.save(persistentMap);
    asyncOrchestratorPostPartyInput(
        actionPrompt.required("actionId").asText(), OBJECT_MAPPER.createObjectNode());
    addOperatorLogEntry(
      "Finished resetScenarioClass");
  }

  private void providedReceiverValidationScenarioParameters(JsonNode actionPrompt) {
    log.info("EblInteropReceivingPlatform.providedReceiverValidationScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));
    asyncOrchestratorPostPartyInput(
      actionPrompt.required("actionId").asText(), SupplyValidationEndpointScenarioParametersAction.getJsonForPrompt());
    addOperatorLogEntry(
      "Finished providedReceiverValidationScenarioParameters");
  }

  private void initiateState(JsonNode actionPrompt) {
    log.info("EblInteropSendingPlatform.handleScenarioTypeAction(%s)".formatted(actionPrompt.toPrettyString()));
    var ssp = SenderScenarioParameters.fromJson(actionPrompt.get("ssp"));
    var tdr = ssp.transportDocumentReference();
    var scenarioClass = ScenarioClass.valueOf(actionPrompt.required("scenarioClass").asText());
    var receivingParameters = getReceiverScenarioParameters(ssp.eblPlatform(), scenarioClass);
    var tdState = TDReceiveState.newInstance(tdr, ssp.sendersX509SigningCertificateInPEMFormat());
    tdState.setExpectedReceiver(
      generateReceiverParty(
        ssp.eblPlatform(),
        scenarioClass == ScenarioClass.INVALID_RECIPIENT ? ScenarioClass.NO_ISSUES : scenarioClass,
        EXPECTED_RECEIVER_PARTY_CODE
      )
    );
    tdState.setScenarioClass(scenarioClass);
    tdState.save(persistentMap);
    var receivingParametersJson = receivingParameters.toJson();
    addOperatorLogEntry(
      "Prompt answer for initiateState: %s".formatted(receivingParametersJson));
    asyncOrchestratorPostPartyInput(
        actionPrompt.required("actionId").asText(), receivingParametersJson);
    addOperatorLogEntry(
      "Finished ScenarioType setup");
  }

  private static JsonNode generateReceiverParty(String sendingPlatform, ScenarioClass scenarioClass, String expectedRecipient) {
    String platform;
    if ("CARX".equals(sendingPlatform)) {
      platform = "BOLE";
    } else {
      platform = "CARX";
    }
    var partyCode = OBJECT_MAPPER.createObjectNode()
      .put("partyCode", scenarioClass == ScenarioClass.INVALID_RECIPIENT ? "12345-invalid" : expectedRecipient)
      .put("codeListProvider", platform);
    var receiverParty = OBJECT_MAPPER.createObjectNode();
    receiverParty.put("partyName", "Jane Doe")
      .put("eblPlatform", platform)
      .putArray("identifyingCodes")
      .add(partyCode);
    return receiverParty;
  }

  public static ReceiverScenarioParameters getReceiverScenarioParameters(String sendingPlatform, ScenarioClass scenarioClass) {
    var receiverParty = generateReceiverParty(sendingPlatform, scenarioClass, EXPECTED_RECEIVER_PARTY_CODE);

    return new ReceiverScenarioParameters(
      receiverParty,
      RECEIVING_PLATFORM_PAYLOAD_SIGNER.getPublicKeyInPemFormat()
    );
  }


  public ConformanceResponse handleInitiateTransferRequest(ConformanceRequest request) {
    var transferRequest = request.message().body().getJsonBody();
    var td = transferRequest.path("transportDocument");
    var tdr = td.path("transportDocumentReference").asText();
    var receiveState = TDReceiveState.fromPersistentStoreIfPresent(persistentMap, tdr).orElse(null);
    if (receiveState == null) {
      receiveState = TDReceiveState.newInstance("", null);
      var responseBody = receiveState.generateSignedResponse(BENV, RECEIVING_PLATFORM_PAYLOAD_SIGNER);
      return request.createResponse(
        BENV.getHttpResponseCode(),
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(responseBody)
      );
    }
    var cannedResponse = receiveState.cannedResponse(request);
    if (cannedResponse != null) {
      return cannedResponse;
    }
    var etc = transferRequest.path("envelopeTransferChain");
    var lastEtcEntry = etc.path(etc.size() - 1);
    var lastEnvelopeTransferChainEntrySignedContentChecksum = Checksums.sha256(lastEtcEntry.asText(""));
    var unsignedPayload = OBJECT_MAPPER.createObjectNode()
      .put("lastEnvelopeTransferChainEntrySignedContentChecksum", lastEnvelopeTransferChainEntrySignedContentChecksum);
    var responseCode = receiveState.recommendedFinishTransferResponse(transferRequest, receiveState.getSignatureVerifierForSenderSignatures());

    if (responseCode != null) {
      receiveState.updateTransferState(responseCode);
      unsignedPayload.put("responseCode", responseCode.name());
      var signedPayloadJsonNode = receiveState.generateSignedResponse(responseCode, RECEIVING_PLATFORM_PAYLOAD_SIGNER);
      receiveState.save(persistentMap);
      return request.createResponse(
        responseCode.getHttpResponseCode(),
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(signedPayloadJsonNode)
      );
    }
    var envelopeReference = receiveState.envelopeReference();
    this.envelopeReferences.put(envelopeReference, tdr);
    var transportDocumentChecksum = Checksums.sha256CanonicalJson(td);
    unsignedPayload
      .put("envelopeReference", envelopeReference)
      .put("lastEnvelopeTransferChainEntrySignedContentChecksum", lastEnvelopeTransferChainEntrySignedContentChecksum)
      .put("transportDocumentChecksum", transportDocumentChecksum);
    var missingChecksums = unsignedPayload.putArray("missingAdditionalDocumentChecksums");
    for (var checksum : receiveState.getMissingDocumentChecksums()) {
      missingChecksums.add(checksum);
    }
    receiveState.save(persistentMap);
    return request.createResponse(
      201,
      Map.of(API_VERSION, List.of(apiVersion)),
      new ConformanceMessageBody(unsignedPayload)
    );
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    targetObjectNode.set(
      "envelopeReferences",
      StateManagementUtil.storeMap(envelopeReferences));
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StateManagementUtil.restoreIntoMap(
      envelopeReferences, sourceObjectNode.get("envelopeReferences"));
  }

  private static final Pattern ENVELOPE_ADDITIONAL_DOCUMENT_PATTERN = Pattern.compile(
    "/envelopes/([^/]++)/additional-documents/([0-9a-fA-F]+)/?$"
  );

  private static final Pattern FINISH_TRANSFER_PATTERN = Pattern.compile(
    "/envelopes/([^/]++)/finish-transfer/?$"
  );

  public ConformanceResponse handleEnvelopeRequest(ConformanceRequest request) {
    var eadm = ENVELOPE_ADDITIONAL_DOCUMENT_PATTERN.matcher(request.url());

    if (eadm.find()) {
      var envelopeReference = eadm.group(1);
      var checksum = eadm.group(2);
      var tdr = this.envelopeReferences.get(envelopeReference);
      if (tdr != null) {
        var receiveState = TDReceiveState.fromPersistentStore(persistentMap, tdr);
        var cannedResponse = receiveState.cannedResponse(request);
        if (cannedResponse != null) {
          return cannedResponse;
        }

        String computedChecksum = "";
        try {
          computedChecksum = Checksums.sha256(request.message().body().getJsonBody().binaryValue());
        } catch (Exception ignored) {
          // Will just fail the checksum check below
        }
        if (!Objects.equals(checksum, computedChecksum) || !receiveState.receiveMissingDocument(checksum)) {
          var payload = receiveState.generateSignedResponse(INCD, RECEIVING_PLATFORM_PAYLOAD_SIGNER);
          return request.createResponse(
            INCD.getHttpResponseCode(),
            Map.of(API_VERSION, List.of(apiVersion)),
            new ConformanceMessageBody(payload)
          );
        }
        receiveState.save(persistentMap);
        return request.createResponse(
          204,
          Map.of(API_VERSION, List.of(apiVersion)),
          new ConformanceMessageBody("")
        );
      }
      return request.createResponse(404,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(
          OBJECT_MAPPER
            .createObjectNode()
            .put(
              "message",
              "Unknown envelope reference")));
    }
    var ftpm = FINISH_TRANSFER_PATTERN.matcher(request.url());
    if (ftpm.find()) {
      var envelopeReference = ftpm.group(1);
      var tdr = this.envelopeReferences.get(envelopeReference);
      var receiveState = TDReceiveState.fromPersistentStore(persistentMap, tdr);
      var cannedResponse = receiveState.cannedResponse(request);
      if (cannedResponse != null) {
        return cannedResponse;
      }
      var responseCode = receiveState.finishTransferCode();
      receiveState.updateTransferState(responseCode);
      var signedPayloadJsonNode = receiveState.generateSignedResponse(responseCode, RECEIVING_PLATFORM_PAYLOAD_SIGNER);
      receiveState.save(persistentMap);
      return request.createResponse(
        responseCode.getHttpResponseCode(),
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(signedPayloadJsonNode)
      );
    }

    return request.createResponse(404,
      Map.of(API_VERSION, List.of(apiVersion)),
      new ConformanceMessageBody(
        OBJECT_MAPPER
          .createObjectNode()
          .put(
            "message",
            "Unknown endpoint")));
  }

  private ConformanceResponse handleReceiverValidation(ConformanceRequest request) {
    return request.createResponse(
      200,
      Map.of(API_VERSION, List.of(apiVersion)),
      new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode().put("partyName", "Name of Test Party"))
    );
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblInteropPlatform.handleRequest(%s)".formatted(request));

    var url = request.url().replaceFirst("/++$", "");
    ConformanceResponse response;
    if (url.endsWith("/receiver-validation")) {
      response = handleReceiverValidation(request);
    } else if (url.endsWith("/envelopes")) {
      response = handleInitiateTransferRequest(request);
    } else if(url.contains("/envelopes/")) {
      response = handleEnvelopeRequest(request);
    } else {
      response = request.createResponse(
        404,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(
          OBJECT_MAPPER
            .createObjectNode()
            .put(
              "message",
              "Unknown endpoint")));
    }
    addOperatorLogEntry("Handling a request");
    return response;
  }
}
