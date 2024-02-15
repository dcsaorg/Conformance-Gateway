package org.dcsa.conformance.standards.eblinterop.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.util.Base64URL;
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
import org.dcsa.conformance.standards.eblinterop.action.PintInitiateAndCloseTransferAction;
import org.dcsa.conformance.standards.eblinterop.action.PintInitiateTransferAction;
import org.dcsa.conformance.standards.eblinterop.action.SenderSupplyScenarioParametersAction;
import org.dcsa.conformance.standards.eblinterop.action.SenderTransmissionClass;
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

  private final Map<String, PintTransferState> eblStatesByTdr = new HashMap<>();

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
      Map.entry(PintInitiateTransferAction.class, this::sendIssuanceRequest)
    );
  }

  private static final char[] TDR_CHARS = (
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    + "abcdefghijklmnopqrstuvwxyz"
    + "0123456789"
    // spaces - cannot come first nor last
    + " "
    // ASCII symbols, also do not count as spaces. TODO, add <> when DT-902 is fixed
    + "-_+!/\\`\"'*^~.:,;(){}[]@$&%"
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

  private void sendIssuanceRequest(JsonNode actionPrompt) {
    log.info("EblInteropSendingPlatform.sendIssuanceRequest(%s)".formatted(actionPrompt.toPrettyString()));
    var dsp = DynamicScenarioParameters.fromJson(actionPrompt.required("dsp"));
    var ssp = SenderScenarioParameters.fromJson(actionPrompt.required("ssp"));
    var tdr = ssp.transportDocumentReference();
    var senderTransmissionClass = SenderTransmissionClass.valueOf(actionPrompt.required("senderTransmissionClass").asText());

    boolean isCorrect = actionPrompt.path("isCorrect").asBoolean();
    if (isCorrect) {
      eblStatesByTdr.put(tdr, PintTransferState.ISSUANCE_REQUESTED);
    }
    var sendingState = TDSendingState.newInstance(ssp.transportDocumentReference(), dsp.documentCount());

    var body = OBJECT_MAPPER.createObjectNode();

    var tdPayload = (ObjectNode)
        JsonToolkit.templateFileToJsonNode(
            "/standards/pint/messages/pint-%s-transport-document.json"
                .formatted(apiVersion),
            Map.of());
    // Manually set TDR. The replacement does raw text subst, which
    // is then parsed as JSON.
    tdPayload.put("transportDocumentReference", tdr);
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

    var signedManifest = payloadSigner.sign(unsignedEnvelopeManifest.toString());
    if (senderTransmissionClass == SenderTransmissionClass.SIGNATURE_ISSUE) {
      signedManifest = mutatePayload(signedManifest);
    }
    body.set("envelopeManifestSignedContent", TextNode.valueOf(signedManifest));
    body.putArray("envelopeTransferChain")
      .add(latestEnvelopeTransferChainEntrySigned);
    sendingState.save(persistentMap);
    this.syncCounterpartPost(
      "/v" + apiVersion.charAt(0) + "/envelopes",
      body
    );

    addOperatorLogEntry(
        "Sent a %s issuance request for eBL with transportDocumentReference '%s' (now in state '%s')"
            .formatted(isCorrect ? "correct" : "incorrect", tdr, eblStatesByTdr.get(tdr)));
  }

  private String mutatePayload(String signedPayload) {
    StringBuilder b = new StringBuilder(signedPayload.length());
    int firstDot = signedPayload.indexOf('.');
    int secondDot = signedPayload.indexOf('.', firstDot + 1);
    if (firstDot == -1 || secondDot == -1) {
      return signedPayload;
    }

    b.append(signedPayload, 0, firstDot + 1);
    var payloadEncoded = signedPayload.substring(firstDot + 1, secondDot);
    var decoded = Base64URL.from(payloadEncoded).decodeToString();
    b.append(Base64URL.encode(decoded + " "));
    b.append(signedPayload, secondDot, signedPayload.length());
    return b.toString();
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
