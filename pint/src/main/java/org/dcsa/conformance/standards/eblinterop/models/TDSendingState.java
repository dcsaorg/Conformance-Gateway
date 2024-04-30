package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.eblinterop.crypto.SignedNodeSupport.parseSignedNode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.nimbusds.jose.JWSObject;
import lombok.SneakyThrows;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.eblinterop.crypto.Checksums;
import org.dcsa.conformance.standards.eblinterop.crypto.PayloadSigner;

public class TDSendingState {

  private static final String TRANSPORT_DOCUMENT_REFERENCE = "transportDocumentReference";
  private static final String DOCUMENTS = "documents";
  private static final String SIGNED_MANIFEST = "signedManifest";
  private static final String ENVELOPE_TRANSFER_CHAIN = "envelopeTransferChain";


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

  private final ObjectNode state;

  private TDSendingState(ObjectNode state) {
    this.state = state;
    if (state == null) {
      throw new IllegalArgumentException();
    }
  }

  public static TDSendingState newInstance(String transportDocumentReference, int documentCount) {
    var state = OBJECT_MAPPER.createObjectNode();
    var documents = state.putArray(DOCUMENTS);
    // We just generate all of them as additional documents because that is easier.
    // TODO: Add random chance for one of them being the visualization
    for (var i = 0 ; i < documentCount ; i++) {
      var document = generateDocument();
      documents.addObject()
        .put("checksum", Checksums.sha256(document))
        .put("content", document)
        .put("pendingTransfer", Boolean.TRUE);
    }
    state.put(TRANSPORT_DOCUMENT_REFERENCE, transportDocumentReference);
    return new TDSendingState(state);
  }

  public void resetDocumentationState() {
    for (var doc : state.path(DOCUMENTS)) {
      ((ObjectNode)doc).put("pendingTransfer", Boolean.FALSE);
    }
  }


  public void registerMissingAdditionalDocument(String checksum) {
    for (var doc : state.path(DOCUMENTS)) {
      if ( Objects.equals(checksum, doc.path("checksum").asText(""))) {
        ((ObjectNode)doc).put("pendingTransfer", Boolean.TRUE);
      }
    }
  }


  public void successfulTransferOfAdditionalDocument(String checksum) {
    for (var doc : state.path(DOCUMENTS)) {
      if ( Objects.equals(checksum, doc.path("checksum").asText(""))) {
        ((ObjectNode)doc).put("pendingTransfer", Boolean.FALSE);
      }
    }
  }

  public JsonNode pollPendingDocument() {
    for (var doc : state.path(DOCUMENTS)) {
      if (doc.path("pendingTransfer").asBoolean(false)) {
        return doc;
      }
    }
    return OBJECT_MAPPER.missingNode();
  }

  public void setSignedManifest(JsonNode signedManifest) {
    state.set(SIGNED_MANIFEST, signedManifest);
  }

  public JsonNode getSignedManifest() {
    return state.path(SIGNED_MANIFEST);
  }

  public void setSignedEnvelopeTransferChain(JsonNode signedManifest) {
    state.set(ENVELOPE_TRANSFER_CHAIN, signedManifest);
  }

  public JsonNode getSignedEnvelopeTransferChain() {
    return state.path(ENVELOPE_TRANSFER_CHAIN);
  }



  public static String platform2CodeListName(String platform) {
    // The default is not valid, but it only happens with unknown platforms and null would be even worse
    return PLATFORM2CODELISTNAME.getOrDefault(platform, platform);
  }

  public static ObjectNode generateTransaction(String action, String sendingPlatform, String sendingPartyName, String sendingEPUI, String receivingPlatform, String receivingPartyName, String receivingEPUI, String receivingCodeListName) {
    var actor = OBJECT_MAPPER.createObjectNode()
      .put("eblPlatform", sendingPlatform)
      .put("partyName", sendingPartyName);
    actor.putArray("partyCodes")
      .addObject()
      .put("partyCode", sendingEPUI)
      .put("codeListProvider", "EPUI")
      .put("codeListName", platform2CodeListName(sendingPlatform));
    var recipient = OBJECT_MAPPER.createObjectNode()
      .put("eblPlatform", receivingPlatform)
      .put("partyName", receivingPartyName);
    recipient.putArray("partyCodes")
      .addObject()
      .put("partyCode", receivingEPUI)
      .put("codeListProvider", "EPUI")
      .put("codeListName", receivingCodeListName);
    var transaction = OBJECT_MAPPER.createObjectNode()
      .put("action", action)
      .put("timestamp", Instant.now().toEpochMilli());
    transaction.set("actor", actor);
    transaction.set("recipient", recipient);
    return transaction;
  }

  public static String generateTransactionEntry(PayloadSigner payloadSigner, String previousEnvelopeTransferChainEntrySignedContentChecksum, String tdChecksum, String action, String sendingPlatform, String sendingPartyName, String sendingEPUI, String receivingPlatform, String receivingPartyName, String receivingEPUI, String receivingCodeListName) {
    var latestEnvelopeTransferChainUnsigned = OBJECT_MAPPER.createObjectNode()
      .put("eblPlatform", sendingPlatform)
      .put("transportDocumentChecksum", tdChecksum)
      .put("previousEnvelopeTransferChainEntrySignedContentChecksum", previousEnvelopeTransferChainEntrySignedContentChecksum);

    latestEnvelopeTransferChainUnsigned
      .putArray("transactions")
      .add(generateTransaction(
        action,
        sendingPlatform,
        sendingPartyName,
        sendingEPUI,
        receivingPlatform,
        receivingPartyName,
        receivingEPUI,
        receivingCodeListName
      ));


    return payloadSigner.sign(latestEnvelopeTransferChainUnsigned.toString());
  }

  @SneakyThrows
  public void resignLatestEntry(PayloadSigner payloadSigner) {
    var chain = this.state.path(ENVELOPE_TRANSFER_CHAIN);
    var lastSigned = chain.path(chain.size() - 1);
    var lastUnsigned = JWSObject.parse(lastSigned.asText());
    var lastResigned = payloadSigner.sign(lastUnsigned.getPayload().toString());
    ((ArrayNode)chain).set(chain.size() - 1, lastResigned);
  }

  @SneakyThrows
  public void manipulateLatestTransaction(PayloadSigner payloadSigner, ReceiverScenarioParameters rsp) {
    var chain = this.state.path(ENVELOPE_TRANSFER_CHAIN);
    var lastSigned = chain.path(chain.size() - 1);
    var last = parseSignedNode(lastSigned);
    var sendingPlatform = "BOLE";
    var receivingPlatform = rsp.eblPlatform();
    var sendingEPUI = "1234";
    var sendingPartyName = "DCSA CTK tester";
    var receivingEPUI = rsp.receiverEPUI();
    var receivingPartyName = rsp.receiverPartyName();
    var receiverCodeListName = rsp.receiverEPUICodeListName();
    if (sendingPlatform.equals(receivingPlatform)) {
      sendingPlatform = "WAVE";
    }
    var newTransactionEntry = generateTransaction(
      "TRNS",
      sendingPlatform,
      sendingPartyName,
      sendingEPUI,
      sendingPlatform,
      receivingPartyName,
      receivingEPUI,
      receiverCodeListName
    );

    ((ArrayNode)last.path("transactions")).insert(0, newTransactionEntry);

    var newSigned = payloadSigner.sign(last.toString());
    ((ArrayNode)chain).set(chain.size() - 1, newSigned);
  }

  public ObjectNode generateEnvelopeManifest(String transportDocumentChecksum, String lastEnvelopeTransferChainEntrySignedContentChecksum) {
    var unsignedEnvelopeManifest = OBJECT_MAPPER.createObjectNode()
      .put("transportDocumentChecksum", transportDocumentChecksum)
      .put("lastEnvelopeTransferChainEntrySignedContentChecksum", lastEnvelopeTransferChainEntrySignedContentChecksum);
    var docs = unsignedEnvelopeManifest.putArray("supportingDocuments");
    int docNo = 0;
    for (var documentNode : this.state.path(DOCUMENTS)) {
      var checksum = documentNode.path("checksum");
      var document = ((BinaryNode)documentNode.path("content")).binaryValue();
      var docNode = docs.addObject();
      docNode.put("name", "test-document-" + (++docNo) + ".bin")
        .put("size", document.length)
        .put("mediaType", "application/octet-stream")
        .set("documentChecksum", checksum);
    }
    return unsignedEnvelopeManifest;
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


  public String getTransportDocumentReference() {
    return state.path(TRANSPORT_DOCUMENT_REFERENCE).asText();
  }

  public JsonNode asPersistentState() {
    return this.state;
  }

  public static TDSendingState load(JsonNodeMap jsonNodeMap, String tdr) {
    return new TDSendingState((ObjectNode) jsonNodeMap.load(tdr));
  }

  public void save(JsonNodeMap jsonNodeMap) {
    jsonNodeMap.save(getTransportDocumentReference(), asPersistentState());
  }

}
