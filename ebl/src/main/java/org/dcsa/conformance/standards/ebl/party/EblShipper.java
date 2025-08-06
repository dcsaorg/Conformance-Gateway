package org.dcsa.conformance.standards.ebl.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
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
import org.dcsa.conformance.standards.ebl.action.*;
import org.dcsa.conformance.standards.ebl.models.OutOfOrderMessageType;

@Slf4j
public class EblShipper extends ConformanceParty {

  public EblShipper(
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

  @Override
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    // no state to export
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    // no state to import
  }

  @Override
  protected void doReset() {
    // no state to reset
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
      Map.entry(UC1_Shipper_SubmitShippingInstructionsAction.class, this::sendShippingInstructionsRequest),
      Map.entry(Shipper_GetShippingInstructionsAction.class, this::getShippingInstructionsRequest),
      Map.entry(Shipper_GetTransportDocumentAction.class, this::getTransportDocument),
      Map.entry(AUC_Shipper_SendOutOfOrderSIMessageAction.class, this::sendOutOfOrderMessage),
      Map.entry(UC3ShipperSubmitUpdatedShippingInstructionsAction.class, this::sendUpdatedShippingInstructionsRequest),
      Map.entry(UC5_Shipper_CancelUpdateToShippingInstructionsAction.class, this::cancelUpdateToShippingInstructions),
      Map.entry(UC7_Shipper_ApproveDraftTransportDocumentAction.class, this::approveDraftTransportDocument),
      Map.entry(ShipperGetShippingInstructionsErrorAction.class, this::getShippingInstructionsRequest)
    );
  }

  private void sendShippingInstructionsRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendShippingInstructionsRequest(%s)".formatted(actionPrompt.toPrettyString()));

    JsonNode siPayload = actionPrompt.get(CarrierSupplyPayloadAction.CARRIER_PAYLOAD);

    ConformanceResponse conformanceResponse = syncCounterpartPost("/v3/shipping-instructions", siPayload);

    JsonNode jsonBody = conformanceResponse.message().body().getJsonBody();
    String shippingInstructionsReference = jsonBody.path("shippingInstructionsReference").asText();
    ObjectNode updatedShippingInstructions = ((ObjectNode) siPayload)
        .put("shippingInstructionsReference", shippingInstructionsReference);
    persistentMap.save(shippingInstructionsReference, updatedShippingInstructions);

    addOperatorLogEntry(
        "Sent a shipping instructions request with the parameters: %s"
            .formatted(siPayload));
  }

  private void sendUpdatedShippingInstructionsRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendUpdatedShippingInstructionsRequest(%s)".formatted(actionPrompt.toPrettyString()));
    var sir = actionPrompt.required("sir").asText();
    var documentReference = actionPrompt.required("documentReference").asText();
    var updatedSI = sendUpdatedShippingInstructions(sir, documentReference);
    persistentMap.save(documentReference, updatedSI);
    addOperatorLogEntry(
      "Sent a shipping instructions update with the parameters: %s"
        .formatted(actionPrompt.toPrettyString()));
  }

  private ObjectNode sendUpdatedShippingInstructions(String sir, String documentReference) {
    var si = updateShippingInstructions((ObjectNode) persistentMap.load(sir));
    var siWithoutStatus = si.deepCopy();
    siWithoutStatus.remove("shippingInstructionsStatus");

    ConformanceResponse conformanceResponse = syncCounterpartPut("/v3/shipping-instructions/%s".formatted(
      URLEncoder.encode(documentReference, StandardCharsets.UTF_8)
    ), siWithoutStatus);

    JsonNode jsonBody = conformanceResponse.message().body().getJsonBody();
    String shippingInstructionsStatus = jsonBody.path("shippingInstructionsStatus").asText();
    return si.put("shippingInstructionsStatus", shippingInstructionsStatus);
  }

  static ObjectNode updateShippingInstructions(ObjectNode si) {
    var seal = si.required("utilizedTransportEquipments").required(0).required("seals").path(0);
    var newSealNumber =
        UUID.randomUUID()
            .toString()
            .substring(0, 8)
            .toUpperCase(); // adding a different seal number for each UC3
    ((ObjectNode)seal).put("number", newSealNumber);
    return si;
  }

  private void sendCancellationToUpdatedShippingInstructions(String documentReference) {
    var approvePayload = OBJECT_MAPPER.createObjectNode()
      .put("updatedShippingInstructionsStatus", ShippingInstructionsStatus.SI_UPDATE_CANCELLED.wireName());

    syncCounterpartPatch(
      "/v3/shipping-instructions/%s".formatted(URLEncoder.encode(documentReference, StandardCharsets.UTF_8)),
      Collections.emptyMap(),
      approvePayload);

  }

  private void cancelUpdateToShippingInstructions(JsonNode actionPrompt) {
    log.info("Shipper.cancelUpdateToShippingInstructions(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    sendCancellationToUpdatedShippingInstructions(documentReference);
    addOperatorLogEntry(
      "Cancelled update to shipping instructions the parameters: %s"
        .formatted(actionPrompt.toPrettyString()));
  }

  private void sendOutOfOrderMessage(JsonNode actionPrompt) {
    var outOfOrderMessageType = OutOfOrderMessageType.valueOf(actionPrompt.required("outOfOrderMessageType").asText("<?>"));
    var documentReference = actionPrompt.required("documentReference").asText();
    switch (outOfOrderMessageType) {
      case CANCEL_SI_UPDATE -> sendCancellationToUpdatedShippingInstructions(documentReference);
      case SUBMIT_SI_UPDATE -> sendUpdatedShippingInstructions(
        actionPrompt.required("sir").asText("<?>"),
        documentReference
      );
      case APPROVE_TD -> sendApproveDraftTransportDocument(documentReference);
      default -> throw new AssertionError("Missing case for " + outOfOrderMessageType.name());
    }
  }

  private void sendApproveDraftTransportDocument(String documentReference) {
    var approvePayload = OBJECT_MAPPER.createObjectNode()
      .put("transportDocumentStatus", TransportDocumentStatus.TD_APPROVED.wireName());

    syncCounterpartPatch(
      "/v3/transport-documents/%s".formatted(URLEncoder.encode(documentReference, StandardCharsets.UTF_8)),
      Collections.emptyMap(),
      approvePayload);
  }

  private void approveDraftTransportDocument(JsonNode actionPrompt) {
    log.info("Shipper.approveDraftTransportDocument(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    sendApproveDraftTransportDocument(documentReference);

    addOperatorLogEntry(
      "Approved transport document the parameters: %s"
        .formatted(actionPrompt.toPrettyString()));
  }


  private void getShippingInstructionsRequest(JsonNode actionPrompt) {
    log.info("Shipper.getShippingInstructionsRequest(%s)".formatted(actionPrompt.toPrettyString()));
    String documentReference = actionPrompt.get("documentReference").asText();
    boolean requestAmendment = actionPrompt.path("amendedContent").asBoolean(false);
    boolean errorScenario = actionPrompt.path(ShipperGetShippingInstructionsErrorAction.SEND_INVALID_FACILITY_CODE).asBoolean(false);

    Map<String, List<String>> queryParams = requestAmendment
      ? Map.of("updatedContent", List.of("true"))
      : Collections.emptyMap();

    if (errorScenario) {
      documentReference = "NON-EXISTING-SI";
    }

    syncCounterpartGet("/v3/shipping-instructions/" + URLEncoder.encode(documentReference, StandardCharsets.UTF_8), queryParams);

    addOperatorLogEntry("Sent a GET request for shipping instructions with documentReference: %s".formatted(documentReference));
  }

  private void getTransportDocument(JsonNode actionPrompt) {
    log.info("Shipper.getTransportDocument(%s)".formatted(actionPrompt.toPrettyString()));
    String tdr = actionPrompt.required("tdr").asText();

    syncCounterpartGet("/v3/transport-documents/" + URLEncoder.encode(tdr, StandardCharsets.UTF_8), Collections.emptyMap());

    addOperatorLogEntry("Sent a GET request for transport document with TDR: %s".formatted(tdr));
  }


  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("Shipper.handleRequest(%s)".formatted(request));

    ConformanceResponse response =
        request.createResponse(
            204,
            Map.of(API_VERSION, List.of(apiVersion)),
            new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode()));

    addOperatorLogEntry(
        "Handled lightweight notification: %s".formatted(request.message().body().getJsonBody()));
    return response;
  }


}
