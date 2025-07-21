package org.dcsa.conformance.standards.eblinterop.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.eblinterop.action.SenderTransmissionClass.*;
import static org.dcsa.conformance.standards.eblinterop.models.TDSendingState.generateTransactionEntry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.nimbusds.jose.util.Base64URL;
import java.util.*;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.ebl.crypto.Checksums;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerWithKey;
import org.dcsa.conformance.standards.eblinterop.action.*;
import org.dcsa.conformance.standards.eblinterop.models.DynamicScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.ReceiverScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.SenderScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.TDSendingState;

@Slf4j
public class PintSendingPlatform extends ConformanceParty {

  private static final PayloadSignerWithKey SENDING_PLATFORM_PAYLOAD_SIGNER =
      PayloadSignerFactory.senderPayloadSigner();
  private static final PayloadSignerWithKey CARRIER_PLATFORM_PAYLOAD_SIGNER =
      PayloadSignerFactory.carrierPayloadSigner();

  public PintSendingPlatform(
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
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {}

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {}

  @Override
  protected void doReset() {}

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(SenderSupplyScenarioParametersAction.class, this::supplyScenarioParameters),
        Map.entry(PintInitiateAndCloseTransferAction.class, this::initiateTransferRequest),
        Map.entry(PintInitiateTransferAction.class, this::initiateTransferRequest),
        Map.entry(PintTransferAdditionalDocumentAction.class, this::transferActionDocument),
        Map.entry(PintRetryTransferAction.class, this::retryTransfer),
        Map.entry(PintRetryTransferAndCloseAction.class, this::retryTransfer),
        Map.entry(PintCloseTransferAction.class, this::finishTransfer),
        Map.entry(PintReceiverValidationAction.class, this::requestReceiverValidation));
  }

  private void requestReceiverValidation(JsonNode actionPrompt) {
    log.info(
        "EblInteropSendingPlatform.requestReceiverValidation(%s)"
            .formatted(actionPrompt.toPrettyString()));
    var dsp = DynamicScenarioParameters.fromJson(actionPrompt.required("dsp"));
    this.syncCounterpartPost(
        "/v" + apiVersion.charAt(0) + "/receiver-validation", dsp.receiverValidation());
    addOperatorLogEntry("Requested receiver validation");
  }

  public static SenderScenarioParameters sendingScenarioParameters() {
    return new SenderScenarioParameters(
        "TDR-12345678",
        "WAVE",
        SENDING_PLATFORM_PAYLOAD_SIGNER.getPublicKeyInPemFormat(),
        CARRIER_PLATFORM_PAYLOAD_SIGNER.getPublicKeyInPemFormat());
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info(
        "EblInteropSendingPlatform.supplyScenarioParameters(%s)"
            .formatted(actionPrompt.toPrettyString()));
    var scenarioParameters = sendingScenarioParameters().toJson();
    addOperatorLogEntry(
        "Prompt answer for supplyScenarioParameters: %s".formatted(scenarioParameters.toString()));
    asyncOrchestratorPostPartyInput(actionPrompt.required("actionId").asText(), scenarioParameters);
  }

  @SneakyThrows
  private byte[] copyDocument(JsonNode jsonNode) {
    return jsonNode.binaryValue().clone();
  }

  private void transferActionDocument(JsonNode actionPrompt) {
    log.info(
        "EblInteropSendingPlatform.transferActionDocument(%s)"
            .formatted(actionPrompt.toPrettyString()));
    var dsp = DynamicScenarioParameters.fromJson(actionPrompt.required("dsp"));
    var ssp = SenderScenarioParameters.fromJson(actionPrompt.required("ssp"));
    var tdr = ssp.transportDocumentReference();
    var senderDocumentTransmissionTypeCode =
        SenderDocumentTransmissionTypeCode.valueOf(
            actionPrompt.required("senderDocumentTransmissionTypeCode").asText());
    var sendingState = TDSendingState.load(persistentMap, tdr);
    var envelopeReference = Objects.requireNonNull(dsp.envelopeReference());
    var document = sendingState.pollPendingDocument();
    var checksum = document.path("checksum").asText("?");
    var content = document.path("content");
    switch (senderDocumentTransmissionTypeCode) {
      case VALID_DOCUMENT -> {
        /* No manipulation */
      }
      case CORRUPTED_DOCUMENT, UNRELATED_DOCUMENT -> {
        var copy = copyDocument(content);
        copy[0] ^= 0x01;
        content = BinaryNode.valueOf(copy);
        if (senderDocumentTransmissionTypeCode
            == SenderDocumentTransmissionTypeCode.UNRELATED_DOCUMENT) {
          checksum = Checksums.sha256(copy);
        }
      }
    }
    var response =
        this.syncCounterpartPut(
            "/v"
                + apiVersion.charAt(0)
                + "/envelopes/"
                + envelopeReference
                + "/additional-documents/"
                + checksum,
            content);
    if (response.statusCode() == 204) {
      sendingState.successfulTransferOfAdditionalDocument(checksum);
      sendingState.save(persistentMap);
    }
    addOperatorLogEntry("Attempted transfer of document: %s".formatted(checksum));
  }

  private void finishTransfer(JsonNode actionPrompt) {
    log.info(
        "EblInteropSendingPlatform.finishTransfer(%s)".formatted(actionPrompt.toPrettyString()));
    var dsp = DynamicScenarioParameters.fromJson(actionPrompt.required("dsp"));
    var envelopeReference = Objects.requireNonNull(dsp.envelopeReference());
    this.syncCounterpartPut(
        "/v" + apiVersion.charAt(0) + "/envelopes/" + envelopeReference + "/finish-transfer", null);
    addOperatorLogEntry("Finished transfer with reference: %s".formatted(envelopeReference));
  }

  private void retryTransfer(JsonNode actionPrompt) {
    log.info(
        "EblInteropSendingPlatform.retryTransfer(%s)".formatted(actionPrompt.toPrettyString()));
    var ssp = SenderScenarioParameters.fromJson(actionPrompt.required("ssp"));
    var tdr = ssp.transportDocumentReference();
    var retryType = RetryType.valueOf(actionPrompt.required("retryType").asText());
    var rsp = ReceiverScenarioParameters.fromJson(actionPrompt.required("rsp"));
    var sendingState = TDSendingState.load(persistentMap, tdr);

    switch (retryType) {
      case RESIGN -> sendingState.resignLatestEntry(SENDING_PLATFORM_PAYLOAD_SIGNER);
      case MANIPULATE ->
          sendingState.manipulateLatestTransaction(SENDING_PLATFORM_PAYLOAD_SIGNER, rsp);
    }

    var body = OBJECT_MAPPER.createObjectNode();
    var tdPayload = loadTransportDocument(tdr);
    body.set("transportDocument", tdPayload);
    body.set("envelopeManifestSignedContent", sendingState.getSignedManifest());
    body.set("envelopeTransferChain", sendingState.getSignedEnvelopeTransferChain());
    var manifestNode = sendingState.getIssuanceManifestNode();
    if (!manifestNode.isMissingNode()) {
      body.set("issuanceManifestSignedContent", manifestNode);
    }
    var response = this.syncCounterpartPost("/v" + apiVersion.charAt(0) + "/envelopes", body);
    var responseBody = response.message().body().getJsonBody();
    sendingState.resetDocumentationState();
    for (var checksumNode : responseBody.path("missingAdditionalDocumentChecksums")) {
      sendingState.registerMissingAdditionalDocument(checksumNode.asText("").toLowerCase());
    }
    sendingState.save(persistentMap);
    addOperatorLogEntry(
        "Re-sent an eBL with transportDocumentReference '%s' with type: %s"
            .formatted(tdr, retryType.name()));
  }

  private ObjectNode loadTransportDocument(String tdr) {
    var tdPayload =
        (ObjectNode)
            JsonToolkit.templateFileToJsonNode(
                "/standards/pint/messages/pint-%s-transport-document.json".formatted(apiVersion),
                Map.of());
    // Manually set TDR. The replacement does raw text subst, which
    // is then parsed as JSON. Since the TDR reference can have any number
    // of special characters, this can then break the parsing.
    return tdPayload.put("transportDocumentReference", tdr);
  }

  private String generateTransactions(
      ObjectNode payload,
      String tdChecksum,
      SenderTransmissionClass senderTransmissionClass,
      ReceiverScenarioParameters rsp) {
    var sendingPlatform = "BOLE";
    var receivingPlatform = rsp.receiverParty().path("eblPlatform").asText("!?");
    var sendingEPUI = "1234";
    var sendingPartyName = "DCSA CTK tester";
    var receiver = rsp.receiverParty();
    if (sendingPlatform.equals(receivingPlatform)) {
      sendingPlatform = "WAVE";
    }
    var transactions = payload.putArray("envelopeTransferChain");
    var action = "ISSU";
    String previousChecksum = null;
    if (senderTransmissionClass == VALID_TRANSFER) {
      var transaction =
          generateTransactionEntry(
              SENDING_PLATFORM_PAYLOAD_SIGNER,
              null,
              tdChecksum,
              action,
              sendingPlatform,
              "DCSA CTK issuer",
              "5432",
              receiver);
      previousChecksum = Checksums.sha256(transaction);
      transactions.add(transaction);
      action = "TRNS";
    }
    if (senderTransmissionClass == WRONG_RECIPIENT_PLATFORM) {
      if (receivingPlatform.equals("WAVE")) {
        receivingPlatform = "BOLE";
      } else {
        receivingPlatform = "WAVE";
      }
      receiver = receiver.deepCopy();
      try {
        var obj = (ObjectNode) receiver;
        obj.put("eblPlatform", receivingPlatform);
      } catch (ClassCastException e) {
        /* ignore */
      }
    }
    var latest =
        generateTransactionEntry(
            SENDING_PLATFORM_PAYLOAD_SIGNER,
            previousChecksum,
            tdChecksum,
            action,
            sendingPlatform,
            sendingPartyName,
            sendingEPUI,
            receiver);
    transactions.add(latest);
    return latest;
  }

  private void initiateTransferRequest(JsonNode actionPrompt) {
    log.info(
        "EblInteropSendingPlatform.sendIssuanceRequest(%s)"
            .formatted(actionPrompt.toPrettyString()));
    var dsp = DynamicScenarioParameters.fromJson(actionPrompt.required("dsp"));
    var ssp = SenderScenarioParameters.fromJson(actionPrompt.required("ssp"));
    var tdr = ssp.transportDocumentReference();

    var senderTransmissionClass =
        SenderTransmissionClass.valueOf(actionPrompt.required("senderTransmissionClass").asText());

    boolean isCorrect = actionPrompt.path("isCorrect").asBoolean();

    var sendingState =
        TDSendingState.newInstance(ssp.transportDocumentReference(), dsp.documentCount());

    var body = OBJECT_MAPPER.createObjectNode();

    var tdPayload = loadTransportDocument(tdr);
    body.set("transportDocument", tdPayload);
    if (!isCorrect && tdPayload.path("transportDocument").has("issuingParty")) {
      ((ObjectNode) tdPayload.path("transportDocument")).remove("issuingParty");
    }

    var rsp = ReceiverScenarioParameters.fromJson(actionPrompt.required("rsp"));
    var tdChecksum = Checksums.sha256CanonicalJson(tdPayload);

    var latestEnvelopeTransferChainEntrySigned =
        generateTransactions(body, tdChecksum, senderTransmissionClass, rsp);
    var unsignedEnvelopeManifest =
        sendingState.generateEnvelopeManifest(
            tdChecksum, Checksums.sha256(latestEnvelopeTransferChainEntrySigned));
    var issuanceManifest =
        OBJECT_MAPPER
            .createObjectNode()
            .put("documentChecksum", tdChecksum)
            // The receiver cannot validate the issueToChecksum anyway.
            .put("issueToChecksum", Checksums.sha256(UUID.randomUUID().toString()));
    var issuanceManifestSignedContentNode =
        TextNode.valueOf(CARRIER_PLATFORM_PAYLOAD_SIGNER.sign(issuanceManifest.toString()));

    JsonNode signedManifest =
        TextNode.valueOf(SENDING_PLATFORM_PAYLOAD_SIGNER.sign(unsignedEnvelopeManifest.toString()));
    sendingState.setSignedManifest(signedManifest);
    if (senderTransmissionClass == SIGNATURE_ISSUE) {
      signedManifest = mutatePayload(signedManifest);
    }
    sendingState.setIssuanceManifestNode(issuanceManifestSignedContentNode);
    body.set("issuanceManifestSignedContent", issuanceManifestSignedContentNode);
    body.set("envelopeManifestSignedContent", signedManifest);
    var envelopeTransferChain = body.path("envelopeTransferChain");
    sendingState.setSignedEnvelopeTransferChain(envelopeTransferChain);
    sendingState.save(persistentMap);
    var response = this.syncCounterpartPost("/v" + apiVersion.charAt(0) + "/envelopes", body);
    var responseBody = response.message().body().getJsonBody();
    for (var checksumNode : responseBody.path("missingAdditionalDocumentChecksums")) {
      sendingState.registerMissingAdditionalDocument(checksumNode.asText("").toLowerCase());
    }

    addOperatorLogEntry("Sent an eBL with transportDocumentReference '%s'".formatted(tdr));
  }

  private JsonNode mutatePayload(JsonNode signedPayloadNode) {
    var signedPayload = signedPayloadNode.asText("");
    StringBuilder b = new StringBuilder(signedPayload.length());
    int firstDot = signedPayload.indexOf('.');
    int secondDot = signedPayload.indexOf('.', firstDot + 1);
    if (firstDot == -1 || secondDot == -1) {
      return signedPayloadNode;
    }

    b.append(signedPayload, 0, firstDot + 1);
    var payloadEncoded = signedPayload.substring(firstDot + 1, secondDot);
    var decoded = Base64URL.from(payloadEncoded).decodeToString();
    b.append(Base64URL.encode(decoded + " "));
    b.append(signedPayload, secondDot, signedPayload.length());
    return TextNode.valueOf(b.toString());
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("EblInteropSendingPlatform.handleRequest(%s)".formatted(request));
    return request.createResponse(
        404,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(
            OBJECT_MAPPER
                .createObjectNode()
                .put(
                    "message",
                    "There are no API endpoints supported. The JWKS one is not supported")));
  }
}
