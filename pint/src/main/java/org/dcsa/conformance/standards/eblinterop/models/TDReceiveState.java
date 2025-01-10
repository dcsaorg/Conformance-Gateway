package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.eblinterop.action.PintResponseCode.*;
import static org.dcsa.conformance.standards.ebl.crypto.SignedNodeSupport.parseSignedNode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.nimbusds.jose.JWSObject;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.ebl.crypto.*;
import org.dcsa.conformance.standards.eblinterop.action.PintResponseCode;
import org.dcsa.conformance.standards.eblinterop.action.ScenarioClass;

public class TDReceiveState {

  private static final String TRANSPORT_DOCUMENT_REFERENCE = "transportDocumentReference";
  private static final String TRANSFER_STATE = "transferState";
  private static final String EXPECTED_RECEIVER = "expectedReceiver";

  private static final String MISSING_DOCUMENTS = "missingDocuments";
  private static final String KNOWN_DOCUMENTS = "knownDocuments";

  private static final String SCENARIO_CLASS = "scenarioClass";
  private static final String ENVELOPE_REFERENCE = "envelopeReference";

  private static final String EXPECTED_SIGNING_CERT_ATTR = "expectedSenderX509Certificate";

  private static final String TRANSFER_CHAIN_ENTRY_HISTORY = "transferChainEntryHistory";

  private final ObjectNode state;

  private TDReceiveState(ObjectNode state) {
    this.state = state;
  }


  public TransferState getTransferState() {
    var v = state.path(TRANSFER_STATE).asText(null);
    if (v == null) {
      return TransferState.NOT_STARTED;
    }
    return TransferState.valueOf(v);
  }

  private void setTransferState(TransferState transferState) {
    state.put(TRANSFER_STATE, transferState.name());
  }


  public String getTransportDocumentReference() {
    return state.path(TRANSPORT_DOCUMENT_REFERENCE).asText();
  }

  public void setExpectedReceiver(JsonNode receiver) {
    this.state.set(EXPECTED_RECEIVER, receiver);
  }

  public Set<String> getKnownDocumentChecksums() {
    return StreamSupport.stream(this.state.path(KNOWN_DOCUMENTS).spliterator(), false)
      .map(JsonNode::asText)
      .collect(Collectors.toUnmodifiableSet());
  }

  public Set<String> getMissingDocumentChecksums() {
    return StreamSupport.stream(this.state.path(MISSING_DOCUMENTS).spliterator(), false)
      .map(JsonNode::asText)
      .collect(Collectors.toUnmodifiableSet());
  }

  public boolean receiveMissingDocument(String checksum) {
    var missingDocumentsRaw = this.state.path(MISSING_DOCUMENTS);
    var known = false;
    if (missingDocumentsRaw.isArray()) {
      var missingDocuments = (ArrayNode)missingDocumentsRaw;
      int idx = -1;
      for (int i = 0 ; i < missingDocuments.size() ; i++) {
        if (missingDocuments.get(i).asText("").equals(checksum)) {
          // In theory, we should also check the size. In practice, we assume that
          // the sha256 checksum is "unbreakable" proof of the size match as well.
          // Which it will be in day-to-day tests until someone breaks sha256 like
          // sha1 was broken (though that will likely not happen for many years and
          // is not really worth the effort to guard against in the conformance
          // scenario as it is impossible for us device a test where the checksums
          // match but the sizes differs - if we could, the sha256 checksum would
          // be broken!)
          idx = i;
          break;
        }
      }
      if (idx > -1) {
        var knownDocumentsRaw = this.state.path(KNOWN_DOCUMENTS);
        ArrayNode knownDocuments;
        if (knownDocumentsRaw.isArray()) {
          knownDocuments = (ArrayNode) knownDocumentsRaw;
        } else {
          knownDocuments = this.state.putArray(KNOWN_DOCUMENTS);
        }
        missingDocuments.remove(idx);
        knownDocuments.add(checksum);
        known = true;
      }
    }
    return known;
  }


  public JsonNode generateSignedResponse(PintResponseCode responseCode, PayloadSigner payloadSigner) {
    var lastEnvelopeTransferChainEntry = lastEnvelopeTransferChainEntry();
    var lastEntryChecksum = Checksums.sha256(lastEnvelopeTransferChainEntry.asText(""));
    var unsignedPayload = OBJECT_MAPPER.createObjectNode()
      .put("lastEnvelopeTransferChainEntrySignedContentChecksum", lastEntryChecksum);

    unsignedPayload.put("responseCode", responseCode.name());

    switch (responseCode) {
      case RECE, DUPE -> {
        var receivedDocuments = unsignedPayload.putArray("receivedAdditionalDocumentChecksums");
        for (var checksum : getKnownDocumentChecksums()) {
          receivedDocuments.add(checksum);
        }
        if (responseCode == DUPE) {
          unsignedPayload.set("duplicateOfAcceptedEnvelopeTransferChainEntrySignedContent", lastEnvelopeTransferChainEntry);
        }
      }
      case MDOC -> {
        var receivedDocuments = unsignedPayload.putArray("missingAdditionalDocumentChecksums");
        for (var checksum : getMissingDocumentChecksums()) {
          receivedDocuments.add(checksum);
        }
      }
      default -> {/* nothing */}
    }
    var signedPayload = payloadSigner.sign(unsignedPayload.toString());
    return TextNode.valueOf(signedPayload);
  }

  public String envelopeReference() {
    var envelopReference = this.state.path(ENVELOPE_REFERENCE).asText(null);
    if (envelopReference == null) {
      envelopReference = UUID.randomUUID().toString();
      this.state.put(ENVELOPE_REFERENCE, envelopReference);
    }
    return envelopReference;
  }

  private boolean isEnvelopeTransferChainValid(JsonNode etc) {
    String expectedChecksum = null;
    for (JsonNode entry : etc) {
      JsonNode parsed;
      try {
          parsed = parseSignedNode(entry);
      } catch (ParseException | JsonProcessingException e) {
          return false;
      }
      var actualChecksum = parsed.path("previousEnvelopeTransferChainEntrySignedContentChecksum").asText(null);
      if (!Objects.equals(expectedChecksum, actualChecksum)) {
        return false;
      }
      expectedChecksum = Checksums.sha256(entry.asText());
    }
    return true;
  }

  public PintResponseCode recommendedFinishTransferResponse(JsonNode initiateRequest, SignatureVerifier signatureVerifier) {
    var etc = initiateRequest.path("envelopeTransferChain");
    var lastEtcEntry = etc.path(etc.size() - 1);
    JsonNode etcEntryParsed, envelopeParsed;
    try {
      etcEntryParsed = parseSignedNode(lastEtcEntry, signatureVerifier);
      envelopeParsed = parseSignedNode(initiateRequest.path("envelopeManifestSignedContent"), signatureVerifier);
    } catch (ParseException | JsonProcessingException e) {
        return PintResponseCode.BENV;
    } catch (CouldNotValidateSignatureException e) {
        return PintResponseCode.BSIG;
    }
    if (!isEnvelopeTransferChainValid(etc)) {
      return PintResponseCode.BENV;
    }
    var transactions = etcEntryParsed.path("transactions");
    var lastTransactionNode = transactions.path(transactions.size() - 1);
    var recipient = lastTransactionNode.path("recipient");
    var expectedReceiver = state.path(EXPECTED_RECEIVER);
    var transferChainEntryHistory = this.state.path(TRANSFER_CHAIN_ENTRY_HISTORY);
    for (int i = 0; i < transferChainEntryHistory.size() ; i++) {
      var transferChainEntry = etc.path(i);
      var expectedTransferChainEntry = transferChainEntryHistory.path(i);
      if (expectedTransferChainEntry.isNull()) {
        // We allow a re-transfer with same history
        if (!transferChainEntry.isMissingNode()) {
          return PintResponseCode.DISE;
        }
        break;
      }
      if (Objects.equals(expectedTransferChainEntry.asText(), transferChainEntry.asText())) {
        continue;
      }
      var lastHistoryNode = transferChainEntryHistory.path(transferChainEntryHistory.size() - 1);
      if (i == transferChainEntryHistory.size() - 2 && lastHistoryNode.isNull()) {
        // Allow resigning of the last entry.
        try {
          var expected = Checksums.sha256(JWSObject.parse(expectedTransferChainEntry.asText()).getPayload().toBytes());
          var actual = Checksums.sha256(JWSObject.parse(transferChainEntry.asText()).getPayload().toBytes());
          if (expected.equals(actual)) {
            continue;
          }
        } catch (ParseException ignored) {
        }
      }
      return PintResponseCode.DISE;
    }
    if (transferChainEntryHistory.size() < transactions.size()) {
      var newTransactionHistory = this.state.putArray(TRANSFER_CHAIN_ENTRY_HISTORY);
      for (var transaction : etc) {
        newTransactionHistory.add(transaction);
      }
    }
    if (!Objects.equals(recipient, expectedReceiver)) {
      return PintResponseCode.BENV;
    }
    var missingDocuments = this.state.putArray(MISSING_DOCUMENTS);
    var knownDocuments = getKnownDocumentChecksums();
    for (var supportingDocumentNode : envelopeParsed.path("supportingDocuments")) {
      var checksum = supportingDocumentNode.path("documentChecksum").asText(null);
      if (checksum == null || knownDocuments.contains(checksum)) {
        continue;
      }
      missingDocuments.add(checksum);
    }
    if (!missingDocuments.isEmpty()) {
      return null;
    }
    return finishTransferCode();
  }

  public JsonNode lastEnvelopeTransferChainEntry() {
    var transferChainEntryHistory = this.state.path(TRANSFER_CHAIN_ENTRY_HISTORY);
    var ridx = transferChainEntryHistory.size();
    JsonNode lastNode;
    do {
        lastNode = transferChainEntryHistory.path(--ridx);
    } while (ridx >= 0 && lastNode.isNull());
    return lastNode;
  }

  public PintResponseCode finishTransferCode() {
    var responseCode = PintResponseCode.RECE;
    if (!getMissingDocumentChecksums().isEmpty()) {
      return PintResponseCode.MDOC;
    }
    var newTransferHistory = this.state.path(TRANSFER_CHAIN_ENTRY_HISTORY);
    var lastEntry = newTransferHistory.path(newTransferHistory.size() - 1);
    if (!lastEntry.isNull()) {
      if (!newTransferHistory.isArray()) {
        newTransferHistory = this.state.putArray(TRANSFER_CHAIN_ENTRY_HISTORY);
      }
      // We use the null node to mark it as us owning the eBL.
      ((ArrayNode)newTransferHistory).addNull();
    }
    if (this.getTransferState() == TransferState.ACCEPTED) {
      responseCode = PintResponseCode.DUPE;
    }
    return responseCode;
  }

  public void updateTransferState(PintResponseCode code) {
    var state = switch (code){
      case RECE, DUPE -> TransferState.ACCEPTED;
      case BENV, BSIG, DISE -> TransferState.REJECTED;
      case INCD, MDOC, BETR -> TransferState.INCOMPLETE;
    };
    this.setTransferState(state);
  }

  public SignatureVerifier getSignatureVerifierForSenderSignatures() {
    var pem = state.path(EXPECTED_SIGNING_CERT_ATTR).asText();
    return PayloadSignerFactory.verifierFromPemEncodedCertificate(pem, EXPECTED_SIGNING_CERT_ATTR);
  }

  public static TDReceiveState fromPersistentStore(JsonNode state) {
    return new TDReceiveState((ObjectNode) state);
  }

  public static TDReceiveState newInstance(String transportDocumentReference, String senderPublicKeyPEM, ReceiverScenarioParameters receivingParameters) {
    var state = OBJECT_MAPPER.createObjectNode()
      .put(TRANSPORT_DOCUMENT_REFERENCE, transportDocumentReference)
      .put(TRANSFER_STATE, TransferState.NOT_STARTED.name())
      .put(EXPECTED_SIGNING_CERT_ATTR, senderPublicKeyPEM);
    return new TDReceiveState(state);
  }

  public JsonNode asPersistentState() {
    return this.state;
  }

  public static TDReceiveState fromPersistentStore(JsonNodeMap jsonNodeMap, String transportDocumentReference) {
    var data = jsonNodeMap.load(transportDocumentReference);
    if (data == null) {
      throw new IllegalArgumentException("Unknown TD Reference: " + transportDocumentReference);
    }
    return fromPersistentStore(data);
  }

  public void save(JsonNodeMap jsonNodeMap) {
    jsonNodeMap.save(getTransportDocumentReference(), asPersistentState());
  }

  public void setScenarioClass(ScenarioClass scenarioClass) {
    this.state.put(SCENARIO_CLASS, scenarioClass.name());
  }

  public ConformanceResponse cannedResponse(ConformanceRequest conformanceRequest) {
    var scenarioClass = ScenarioClass.valueOf(this.state.path(SCENARIO_CLASS).asText(ScenarioClass.NO_ISSUES.name()));
    if (scenarioClass == ScenarioClass.FAIL_W_503) {
      return conformanceRequest.createResponse(503,
        Map.of("Retry-after", List.of("10")),
        new ConformanceMessageBody("Please retry as directed by the scenario")
      );
    }
    return null;
  }
}
