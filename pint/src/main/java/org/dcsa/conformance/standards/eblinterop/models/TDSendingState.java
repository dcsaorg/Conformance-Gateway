package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;

import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.eblinterop.crypto.Checksums;

public class TDSendingState {

  private static final String TRANSPORT_DOCUMENT_REFERENCE = "transportDocumentReference";
  private static final String DOCUMENTS = "documents";
  private static final String MISSING_DOCUMENTS = "missingDocuments";
  private static final String REMAINING_DOCUMENTS = "remainingDocuments";

  private final ObjectNode state;

  private TDSendingState(ObjectNode state) {
    this.state = state;
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
        .put("content", document);
    }
    state.put(TRANSPORT_DOCUMENT_REFERENCE, transportDocumentReference);
    return new TDSendingState(state);
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

  public void save(JsonNodeMap jsonNodeMap) {
    jsonNodeMap.save(getTransportDocumentReference(), asPersistentState());
  }
}
