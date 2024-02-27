package org.dcsa.conformance.standards.eblinterop.models;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.eblinterop.crypto.SignedNodeSupport.parseSignedNode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.eblinterop.action.PintResponseCode;
import org.dcsa.conformance.standards.eblinterop.action.ScenarioClass;
import org.dcsa.conformance.standards.eblinterop.crypto.CouldNotValidateSignatureException;
import org.dcsa.conformance.standards.eblinterop.crypto.SignatureVerifier;

public class TDReceiveState {

  private static final String TRANSPORT_DOCUMENT_REFERENCE = "transportDocumentReference";
  private static final String TRANSFER_STATE = "transferState";
  private static final String EXPECTED_RECEIVER = "expectedReceiver";

  private static final String MISSING_DOCUMENTS = "missingDocuments";
  private static final String KNOWN_DOCUMENTS = "knownDocuments";

  private static final String SCENARIO_CLASS = "scenarioClass";
  private static final String ENVELOPE_REFERENCE = "envelopeReference";

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

  public void setExpectedReceiver(String receiverEPUI) {
    this.state.put(EXPECTED_RECEIVER, receiverEPUI);
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

  public void receiveMissingDocument(String checksum) {
    var missingDocumentsRaw = this.state.path(MISSING_DOCUMENTS);
    if (missingDocumentsRaw.isArray()) {
      var missingDocuments = (ArrayNode)missingDocumentsRaw;
      int idx = -1;
      for (int i = 0 ; i < missingDocuments.size() ; i++) {
        if (missingDocuments.get(i).asText("").equals(checksum)) {
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
      }
    }
  }


  public String envelopeReference() {
    var envelopReference = this.state.path(ENVELOPE_REFERENCE).asText(null);
    if (envelopReference == null) {
      envelopReference = UUID.randomUUID().toString();
      this.state.put(ENVELOPE_REFERENCE, envelopReference);
    }
    return envelopReference;
  }

  public PintResponseCode recommendedFinishTransferResponse(JsonNode initiateRequest, SignatureVerifier signatureVerifer) {
    var etc = initiateRequest.path("envelopeTransferChain");
    var lastEtcEntry = etc.path(etc.size() - 1);
    JsonNode etcEntryParsed, envelopeParsed;
    try {
      etcEntryParsed = parseSignedNode(lastEtcEntry, signatureVerifer);
      envelopeParsed = parseSignedNode(initiateRequest.path("envelopeManifestSignedContent"), signatureVerifer);
    } catch (ParseException | JsonProcessingException e) {
        return PintResponseCode.BENV;
    } catch (CouldNotValidateSignatureException e) {
        return PintResponseCode.BSIG;
    }
    var transactions = etcEntryParsed.path("transactions");
    var lastTransactionNode = transactions.path(transactions.size() - 1);
    var recipient = lastTransactionNode.path("recipient");
    var recipientPartyCodes = recipient.path("partyCodes");
    var expectedReceiver = state.path(EXPECTED_RECEIVER).asText();
    boolean hasExpectedCode = false;
    for (var partyCodeNode : recipientPartyCodes) {
      if (!partyCodeNode.path("codeListProvider").asText("").equals("EPUI")) {
        continue;
      }
      if (partyCodeNode.path("partyCode").asText("").equals(expectedReceiver)) {
        hasExpectedCode = true;
        break;
      }
    }

    if (!hasExpectedCode) {
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

  public PintResponseCode finishTransferCode() {
    var responseCode = PintResponseCode.RECE;
    if (!getMissingDocumentChecksums().isEmpty()) {
      return PintResponseCode.MDOC;
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

  public static TDReceiveState fromPersistentStore(JsonNode state) {
    return new TDReceiveState((ObjectNode) state);
  }

  public static TDReceiveState newInstance(String transportDocumentReference) {
    var state = OBJECT_MAPPER.createObjectNode()
      .put(TRANSPORT_DOCUMENT_REFERENCE, transportDocumentReference)
      .put(TRANSFER_STATE, TransferState.NOT_STARTED.name());
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
