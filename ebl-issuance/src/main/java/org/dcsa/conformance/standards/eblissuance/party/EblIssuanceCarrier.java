package org.dcsa.conformance.standards.eblissuance.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
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
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerFactory;
import org.dcsa.conformance.standards.ebl.crypto.PayloadSignerWithKey;
import org.dcsa.conformance.standards.eblissuance.action.CarrierScenarioParametersAction;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceRequestErrorResponseAction;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceRequestResponseAction;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceResponseCode;

@Slf4j
public class EblIssuanceCarrier extends ConformanceParty {
  private final Map<String, EblIssuanceState> eblStatesByTdr = new HashMap<>();
  private final Map<String, String> sirsByTdr = new HashMap<>();
  private final Map<String, String> brsByTdr = new HashMap<>();
  private static final PayloadSignerWithKey PAYLOAD_SIGNER =PayloadSignerFactory.carrierPayloadSigner();

  public EblIssuanceCarrier(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      PartyWebClient webClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        persistentMap,
        webClient,
        orchestratorAuthHeader);
  }

  private static byte[] generateDocument() {
    String filepath = "/standards/eblissuance/messages/test-iss-document.pdf";
    try (InputStream inputStream = EblIssuanceCarrier.class.getResourceAsStream(filepath)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("File not found: " + filepath);
      }
      RandomAccessReadBuffer randomAccessReadBuffer = new RandomAccessReadBuffer(inputStream);
      try (PDDocument document = Loader.loadPDF(randomAccessReadBuffer);
          ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        // Updating the title of the document
        String uuidHex = UUID.randomUUID().toString();
        String newTitle = "DCSA - " + uuidHex + " shipping";

        PDDocumentInformation info = document.getDocumentInformation();
        info.setTitle(newTitle);
        document.setDocumentInformation(info);
        document.save(outputStream);
        return outputStream.toByteArray();
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Generating document failed: " + filepath, e);
    }
  }

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    targetObjectNode.set(
        "eblStatesByTdr", StateManagementUtil.storeMap(eblStatesByTdr, EblIssuanceState::name));
    targetObjectNode.set("sirsByTdr", StateManagementUtil.storeMap(sirsByTdr));
    targetObjectNode.set("brsByTdr", StateManagementUtil.storeMap(brsByTdr));
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
        Map.entry(IssuanceRequestResponseAction.class, this::sendIssuanceRequest),
        Map.entry(CarrierScenarioParametersAction.class, this::supplyScenarioParameters),
        Map.entry(IssuanceRequestErrorResponseAction.class, this::sendIssuanceRequest));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info(
        "EblIssuanceCarrier.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));
    var carrierScenarioParameters =
        new CarrierScenarioParameters(PAYLOAD_SIGNER.getPublicKeyInPemFormat());
    asyncOrchestratorPostPartyInput(
        actionPrompt.required("actionId").asText(), carrierScenarioParameters.toJson());
    addOperatorLogEntry(
        "Prompt answer for CarrierScenarioParameters: %s"
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

    eblStatesByTdr.put(tdr, EblIssuanceState.ISSUANCE_REQUESTED);

    var jsonRequestBody =
        (ObjectNode)
            JsonToolkit.templateFileToJsonNode(
                "/standards/eblissuance/messages/eblissuance-v%s-request.json"
                    .formatted(apiVersion),
                Map.ofEntries(
                    Map.entry("TRANSPORT_DOCUMENT_REFERENCE_PLACEHOLDER", tdr),
                    Map.entry("SHIPPING_INSTRUCTION_REFERENCE_PLACEHOLDER", sir),
                    Map.entry("BOOKING_REFERENCE_PLACEHOLDER", br),
                    Map.entry(
                        "SEND_TO_PLATFORM_PLACEHOLDER",
                        Objects.requireNonNullElse(ssp.issueToSendToPlatform(), "")),
                    Map.entry(
                        "ISSUE_TO_LEGAL_NAME_PLACEHOLDER",
                        Objects.requireNonNullElse(ssp.issueToPartyName(), "")),
                    Map.entry(
                        "ISSUE_TO_CODE_LIST_PROVIDER",
                        Objects.requireNonNullElse(ssp.issueToCodeListProvider(), "")),
                    Map.entry(
                        "ISSUE_TO_PARTY_CODE_PLACEHOLDER",
                        Objects.requireNonNullElse(ssp.issueToPartyCode(), "")),
                    Map.entry(
                        "ISSUE_TO_CODE_LIST_NAME_PLACEHOLDER",
                        Objects.requireNonNullElse(ssp.issueToCodeListName(), "")),
                    Map.entry(
                        "SHIPPER_LEGAL_NAME_PLACEHOLDER",
                        Objects.requireNonNullElse(ssp.shipperLegalName(), "")),
                    Map.entry(
                        "SHIPPER_CODE_LIST_PROVIDER",
                        Objects.requireNonNullElse(ssp.shipperCodeListProvider(), "")),
                    Map.entry(
                        "SHIPPER_PARTY_CODE_PLACEHOLDER",
                        Objects.requireNonNullElse(ssp.shipperPartyCode(), "")),
                    Map.entry(
                        "SHIPPER_CODE_LIST_NAME_PLACEHOLDER",
                        Objects.requireNonNullElse(ssp.shipperCodeListName(), "")),
                    Map.entry(
                        "CONSIGNEE_LEGAL_NAME_PLACEHOLDER",
                        Objects.requireNonNullElse(ssp.consigneeOrEndorseeLegalName(), "")),
                    Map.entry(
                        "CONSIGNEE_CODE_LIST_PROVIDER",
                        Objects.requireNonNullElse(ssp.consigneeOrEndorseeCodeListProvider(), "")),
                    Map.entry(
                        "CONSIGNEE_PARTY_CODE_PLACEHOLDER",
                        Objects.requireNonNullElse(ssp.consigneeOrEndorseePartyCode(), "")),
                    Map.entry(
                        "CONSIGNEE_CODE_LIST_NAME_PLACEHOLDER",
                        Objects.requireNonNullElse(ssp.consigneeOrEndorseeCodeListName(), "")),
                    Map.entry(
                        "ISSUING_PARTY_LEGAL_NAME_PLACEHOLDER",
                        Objects.requireNonNullElse(ssp.issuingPartyLegalName(), "")),
                    Map.entry(
                        "ISSUING_PARTY_CODE_LIST_PROVIDER",
                        Objects.requireNonNullElse(ssp.issuingPartyCodeListProvider(), "")),
                    Map.entry(
                        "ISSUING_PARTY_PARTY_CODE_PLACEHOLDER",
                        Objects.requireNonNullElse(ssp.issuingPartyPartyCode(), "")),
                    Map.entry(
                        "ISSUING_PARTY_CODE_LIST_NAME_PLACEHOLDER",
                        Objects.requireNonNullElse(ssp.issuingPartyCodeListName(), ""))));

    boolean errorScenario =
        actionPrompt
            .path(IssuanceRequestErrorResponseAction.SEND_NO_ISSUING_PARTY)
            .asBoolean(false);
    if (errorScenario) {
      ((ObjectNode) jsonRequestBody.path("document").path("documentParties"))
          .remove("issuingParty");
    }

    if (eblType.isToOrder()) {
      var td = (ObjectNode) jsonRequestBody.path("document");
      td.put("isToOrder", true);
      if (apiVersion.startsWith("2.")) {
        var documentParties = (ArrayNode) td.path("documentParties");
        var cnIdx = -1;
        for (int i = 0; i < documentParties.size(); i++) {
          if (documentParties.path(i).path("partyFunction").asText("?").equals("CN")) {
            cnIdx = i;
            break;
          }
        }
        if (eblType.isBlankEbl()) {
          documentParties.remove(cnIdx);
        } else {
          ((ObjectNode) documentParties.path(cnIdx)).put("partyFunction", "END");
        }
      } else {
        var documentParties = (ObjectNode) td.path("documentParties");
        if (eblType.isBlankEbl()) {
          documentParties.remove("consignee");
          documentParties.remove("endorsee");
        } else {
          var consignee = documentParties.remove("consignee");
          documentParties.set("endorsee", consignee);
        }
      }
    }

    var tdChecksum = Checksums.sha256CanonicalJson(jsonRequestBody.path("document"));
    var issueToChecksum = Checksums.sha256CanonicalJson(jsonRequestBody.path("issueTo"));
    var eblVisualization = generateDocument();
    jsonRequestBody.set(
        "eBLVisualisationByCarrier", wrapInSupportingDocumentObject(eblVisualization));
    var eBLVisualisationByCarrierChecksum = Checksums.sha256(eblVisualization);
    var issuanceManifest =
        OBJECT_MAPPER
            .createObjectNode()
            .put("documentChecksum", tdChecksum)
            .put("issueToChecksum", issueToChecksum)
            .put("eBLVisualisationByCarrierChecksum", eBLVisualisationByCarrierChecksum);

    jsonRequestBody.put(
        "issuanceManifestSignedContent", PAYLOAD_SIGNER.sign(issuanceManifest.toString()));

    syncCounterpartPut(
        "/v%s/ebl-issuance-requests".formatted(apiVersion.charAt(0)), jsonRequestBody);

    addOperatorLogEntry(
        "Sent an issuance request for eBL with transportDocumentReference '%s' (now in state '%s')"
            .formatted(tdr, eblStatesByTdr.get(tdr)));
  }

  private static ObjectNode wrapInSupportingDocumentObject(byte[] document) {
    return OBJECT_MAPPER
        .createObjectNode()
        .put("name", "test-iss-document")
        .put("content", document)
        .put("mediatype", "application/octet-stream");
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
          "Handled lightweight notification: %s".formatted(request.message().body().getJsonBody()));

      return request.createResponse(
          204,
          Map.of(API_VERSION, List.of(apiVersion)),
          new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));
    } else {
      return request.createResponse(
          409,
          Map.of(API_VERSION, List.of(apiVersion)),
          new ConformanceMessageBody(
              OBJECT_MAPPER
                  .createObjectNode()
                  .put(
                      "message",
                      "Rejecting '%s' for eBL '%s' because it is in state '%s'"
                          .formatted(irc, tdr, eblStatesByTdr.get(tdr)))));
    }
  }

  public static String getCarrierPublicKey() {
    return PAYLOAD_SIGNER.getPublicKeyInPemFormat();
  }
}
