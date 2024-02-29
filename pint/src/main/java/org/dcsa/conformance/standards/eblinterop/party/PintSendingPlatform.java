package org.dcsa.conformance.standards.eblinterop.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.nimbusds.jose.util.Base64URL;
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
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.eblinterop.action.*;
import org.dcsa.conformance.standards.eblinterop.crypto.Checksums;
import org.dcsa.conformance.standards.eblinterop.crypto.PayloadSigner;
import org.dcsa.conformance.standards.eblinterop.models.DynamicScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.ReceiverScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.SenderScenarioParameters;
import org.dcsa.conformance.standards.eblinterop.models.TDSendingState;

@Slf4j
public class PintSendingPlatform extends ConformanceParty {

  private static final Random RANDOM = new Random();

  private static final Map<String, String> PLATFORM2CODELISTNAME = Map.ofEntries(
    Map.entry("WAVE", "Wave"),
    Map.entry("CARX", "CargoX"),
    Map.entry("EDOX", "EdoxOnline"),
    Map.entry("IQAX", "IQAX"),
    Map.entry("ESSD", "EssDOCS"),
    Map.entry("BOLE", "Bolero"),
    Map.entry("TRGO", "TradeGO"),
    Map.entry("SECR", "Secro")/*,
    Map.entry("", "GSBN"),
    Map.entry("", "WiseTech")
    */
  );

  private final PayloadSigner payloadSigner;

  public PintSendingPlatform(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      PartyWebClient asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader,
      PayloadSigner payloadSigner
  ) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        persistentMap,
        asyncWebClient,
        orchestratorAuthHeader);
    this.payloadSigner = payloadSigner;
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
      Map.entry(PintInitiateAndCloseTransferAction.class, this::sendIssuanceRequest),
      Map.entry(PintInitiateTransferAction.class, this::sendIssuanceRequest),
      Map.entry(PintInitiateTransferUnsignedErrorAction.class, this::sendIssuanceRequest),
      Map.entry(PintTransferAdditionalDocumentAction.class, this::transferActionDocument),
      Map.entry(PintRetryTransferAction.class, this::retryTransfer),
      Map.entry(PintRetryTransferAndCloseAction.class, this::retryTransfer),
      Map.entry(PintCloseTransferAction.class, this::finishTransfer)
    );
  }

  private static final char[] TDR_CHARS = (
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    + "abcdefghijklmnopqrstuvwxyz"
    + "0123456789"
    // spaces - cannot come first nor last
    + " "
    // ASCII symbols, also do not count as spaces.
    + "-_+!/\\`\"'*^~.:,;(){}[]<>@$&%"
    // Spec says "\S+(\s+\S+)*". Unicode smiley and other non-ASCII symbols count as "not space".
    + "✉½¤§"
  ).toCharArray();

  private String generateTDR() {
    var tdrChars = new StringBuilder(20);
    // The breaker is sticky to limit how many times we will
    // poll the random.
    // Also, you might be tempted to think this is a good way
    // to generate passwords/keys. You would be wrong in that
    // case as this generator has bias that is not policy
    // defined, and you do not want that.
    int breakerLimit = 20;
    for (int i = 0 ; i < 19 ; i++) {
      var c = TDR_CHARS[RANDOM.nextInt(TDR_CHARS.length)];
      if ((i < 1 || i > 17)) {
        while (Character.isSpaceChar(c) && breakerLimit-- > 0) {
          c = TDR_CHARS[RANDOM.nextInt(TDR_CHARS.length)];
        }

        if (Character.isSpaceChar(c)) {
          // In the unlikely even that random keeps pulling
          // a space, we just pick the first letter and move
          // on. This ensures we will not hang forever with
          // a slight bias towards "A". But it is not a
          // password/key, so the bias is of no consequence.
          c = TDR_CHARS[0];
          assert !Character.isSpaceChar(c);
        }
      }
      tdrChars.append(c);
    }
    return tdrChars.toString();
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info("EblInteropSendingPlatform.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));
    var tdr = generateTDR();
    var scenarioParameters = new SenderScenarioParameters(tdr);
    asyncOrchestratorPostPartyInput(
      OBJECT_MAPPER
        .createObjectNode()
        .put("actionId", actionPrompt.required("actionId").asText())
        .set("input", scenarioParameters.toJson()));
    addOperatorLogEntry(
      "Provided ScenarioParameters: %s".formatted(scenarioParameters));
  }

  private void transferActionDocument(JsonNode actionPrompt) {
    log.info("EblInteropSendingPlatform.transferActionDocument(%s)".formatted(actionPrompt.toPrettyString()));
    var dsp = DynamicScenarioParameters.fromJson(actionPrompt.required("dsp"));
    var ssp = SenderScenarioParameters.fromJson(actionPrompt.required("ssp"));
    var tdr = ssp.transportDocumentReference();
    var sendingState = TDSendingState.load(persistentMap, tdr);
    var envelopeReference = Objects.requireNonNull(dsp.envelopeReference());
    var document = sendingState.pollPendingDocument();
    var checksum = document.path("checksum").asText("?");
    var response = this.syncCounterpartPut(
      "/v" + apiVersion.charAt(0) + "/envelopes/" + envelopeReference + "/additional-documents/" + checksum,
      document.path("content")
    );
    if (response.statusCode() == 204) {
      sendingState.successfulTransferOfAdditionalDocument(checksum);
      sendingState.save(persistentMap);
    }
    addOperatorLogEntry(
      "Attempted transfer of document: %s".formatted(checksum));
  }

  private void finishTransfer(JsonNode actionPrompt) {
    log.info("EblInteropSendingPlatform.finishTransfer(%s)".formatted(actionPrompt.toPrettyString()));
    var dsp = DynamicScenarioParameters.fromJson(actionPrompt.required("dsp"));
    var envelopeReference = Objects.requireNonNull(dsp.envelopeReference());
    this.syncCounterpartPut(
      "/v" + apiVersion.charAt(0) + "/envelopes/" + envelopeReference + "/finish-transfer",
      null
    );
    addOperatorLogEntry(
      "Finished transfer with reference: %s".formatted(envelopeReference));
  }

  private void retryTransfer(JsonNode actionPrompt) {
    log.info("EblInteropSendingPlatform.retryTransfer(%s)".formatted(actionPrompt.toPrettyString()));
    var ssp = SenderScenarioParameters.fromJson(actionPrompt.required("ssp"));
    var tdr = ssp.transportDocumentReference();
    var sendingState = TDSendingState.load(persistentMap, tdr);

    var body = OBJECT_MAPPER.createObjectNode();
    var tdPayload = loadTDR(tdr);
    body.set("transportDocument", tdPayload);
    body.set("envelopeManifestSignedContent", sendingState.getSignedManifest());
    body.set("envelopeTransferChain", sendingState.getSignedEnvelopeTransferChain());
    var response = this.syncCounterpartPost(
      "/v" + apiVersion.charAt(0) + "/envelopes",
      body
    );
    var responseBody = response.message().body().getJsonBody();
    sendingState.resetDocumentationState();
    for (var checksumNode : responseBody.path("missingAdditionalDocumentChecksums")) {
      sendingState.registerMissingAdditionalDocument(checksumNode.asText("").toLowerCase());
    }
    sendingState.save(persistentMap);
    addOperatorLogEntry(
      "Re-sent an eBL with transportDocumentReference '%s'".formatted(tdr));
  }

  private ObjectNode loadTDR(String tdr) {
    var tdPayload = (ObjectNode)
      JsonToolkit.templateFileToJsonNode(
        "/standards/pint/messages/pint-%s-transport-document.json"
          .formatted(apiVersion),
        Map.of());
    // Manually set TDR. The replacement does raw text subst, which
    // is then parsed as JSON. Since the TDR reference can have any number
    // of special characters, this can then break the parsing.
    return tdPayload.put("transportDocumentReference", tdr);
  }

  private void sendIssuanceRequest(JsonNode actionPrompt) {
    log.info("EblInteropSendingPlatform.sendIssuanceRequest(%s)".formatted(actionPrompt.toPrettyString()));
    var dsp = DynamicScenarioParameters.fromJson(actionPrompt.required("dsp"));
    var ssp = SenderScenarioParameters.fromJson(actionPrompt.required("ssp"));
    var tdr = ssp.transportDocumentReference();
    var senderTransmissionClass = SenderTransmissionClass.valueOf(actionPrompt.required("senderTransmissionClass").asText());

    boolean isCorrect = actionPrompt.path("isCorrect").asBoolean();

    var sendingState = TDSendingState.newInstance(ssp.transportDocumentReference(), dsp.documentCount());

    var body = OBJECT_MAPPER.createObjectNode();

    var tdPayload = loadTDR(tdr);
    body.set("transportDocument", tdPayload);
    if (!isCorrect && tdPayload.path("transportDocument").has("issuingParty")) {
      ((ObjectNode) tdPayload.path("transportDocument")).remove("issuingParty");
    }
    var rsp = ReceiverScenarioParameters.fromJson(actionPrompt.required("rsp"));
    var tdChecksum = Checksums.sha256CanonicalJson(tdPayload);
    var sendingPlatform = "BOLE";
    var receivingPlatform = rsp.eblPlatform();
    var sendingEPUI = "1234";
    var sendingLegalName = "DCSA CTK tester";
    var receivingEPUI = rsp.receiverEPUI();
    var receivingLegalName = rsp.receiverLegalName();
    var receiverCodeListName = rsp.receiverEPUICodeListName();
    var latestEnvelopeTransferChainUnsigned = OBJECT_MAPPER.createObjectNode()
      .put("eblPlatform", sendingPlatform)
      .put("transportDocumentChecksum", tdChecksum)
      .putNull("previousEnvelopeTransferChainEntrySignedContentChecksum");
    var issuingParty = OBJECT_MAPPER.createObjectNode()
      .put("eblPlatform", sendingPlatform)
      .put("legalName", sendingLegalName);
    issuingParty.putArray("partyCodes")
      .addObject()
      .put("partyCode", sendingEPUI)
      .put("codeListProvider", "EPUI")
      .put("codeListName", PLATFORM2CODELISTNAME.get(sendingPlatform));
    var issueToParty = OBJECT_MAPPER.createObjectNode()
      .put("eblPlatform", receivingPlatform)
      .put("legalName", receivingLegalName);
    issueToParty.putArray("partyCodes")
      .addObject()
      .put("partyCode", receivingEPUI)
      .put("codeListProvider", "EPUI")
      .put("codeListName", receiverCodeListName);
    var transaction = latestEnvelopeTransferChainUnsigned
      .putArray("transactions")
      .addObject()
      .put("action", "ISSU")
      .put("timestamp", Instant.now().toEpochMilli());
    transaction.set("actor", issuingParty);
    transaction.set("recipient", issueToParty);

    var latestEnvelopeTransferChainEntrySigned = payloadSigner.sign(latestEnvelopeTransferChainUnsigned.toString());
    var unsignedEnvelopeManifest = sendingState.generateEnvelopeManifest(tdChecksum, Checksums.sha256(latestEnvelopeTransferChainEntrySigned));

    JsonNode signedManifest = TextNode.valueOf(payloadSigner.sign(unsignedEnvelopeManifest.toString()));
    sendingState.setSignedManifest(signedManifest);
    if (senderTransmissionClass == SenderTransmissionClass.SIGNATURE_ISSUE) {
      signedManifest = mutatePayload(signedManifest);
    }
    body.set("envelopeManifestSignedContent", signedManifest);
    var envelopeTransferChain = body.putArray("envelopeTransferChain")
      .add(latestEnvelopeTransferChainEntrySigned);
    sendingState.setSignedEnvelopeTransferChain(envelopeTransferChain);
    sendingState.save(persistentMap);
    var response = this.syncCounterpartPost(
      "/v" + apiVersion.charAt(0) + "/envelopes",
      body
    );
    var responseBody = response.message().body().getJsonBody();
    for (var checksumNode : responseBody.path("missingAdditionalDocumentChecksums")) {
      sendingState.registerMissingAdditionalDocument(checksumNode.asText("").toLowerCase());
    }

    addOperatorLogEntry(
        "Sent an eBL with transportDocumentReference '%s'".formatted(tdr));
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
      Map.of("Api-Version", List.of(apiVersion)),
      new ConformanceMessageBody(
        OBJECT_MAPPER
          .createObjectNode()
          .put(
            "message",
            "There are no API endpoints supported. The JWKS one is not supported")));
  }
}
