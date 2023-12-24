package org.dcsa.conformance.standards.ebl.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.dcsa.conformance.standards.ebl.action.*;

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
      Map.entry(UC3_Shipper_SubmitUpdatedShippingInstructionsAction.class, this::sendUpdatedShippingInstructionsRequest),
      Map.entry(UC5_Shipper_CancelUpdateToShippingInstructionsAction.class, this::cancelUpdateToShippingInstructions),
      Map.entry(UC7_Shipper_ApproveDraftTransportDocumentAction.class, this::approveDraftTransportDocument)
    );
  }


  private void sendShippingInstructionsRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendShippingInstructionsRequest(%s)".formatted(actionPrompt.toPrettyString()));

    CarrierScenarioParameters carrierScenarioParameters =
      CarrierScenarioParameters.fromJson(actionPrompt.get("csp"));

    JsonNode jsonRequestBody =
        JsonToolkit.templateFileToJsonNode(
            "/standards/ebl/messages/ebl-api-v30-request.json",
            Map.ofEntries(
                Map.entry(
                    "CARRIER_BOOKING_REFERENCE_PLACEHOLDER",
                    carrierScenarioParameters.carrierBookingReference()),
                Map.entry(
                    "COMMODITY_SUBREFERENCE_PLACEHOLDER",
                    carrierScenarioParameters.commoditySubreference()),
                Map.entry(
                    "EQUIPMENT_REFERENCE_PLACEHOLDER",
                    carrierScenarioParameters.equipmentReference()),
              Map.entry("INVOICE_PAYABLE_AT_UNLOCATION_CODE", carrierScenarioParameters.invoicePayableAtUNLocationCode()),
              Map.entry("CONSIGNMENT_ITEM_HS_CODE", carrierScenarioParameters.consignmentItemHSCode()),
              Map.entry("DESCRIPTION_OF_GOODS_PLACEHOLDER", carrierScenarioParameters.descriptionOfGoods()),
              Map.entry("SERVICE_CONTRACT_REFERENCE_PLACEHOLDER", carrierScenarioParameters.serviceContractReference())
              ));

    ConformanceResponse conformanceResponse = syncCounterpartPost("/v3/shipping-instructions", jsonRequestBody);

    JsonNode jsonBody = conformanceResponse.message().body().getJsonBody();
    String shippingInstructionsReference = jsonBody.path("shippingInstructionsReference").asText();
    ObjectNode updatedShippingInstructions =
      ((ObjectNode) jsonRequestBody)
        .put("shippingInstructionsReference", shippingInstructionsReference);
    persistentMap.save(shippingInstructionsReference, updatedShippingInstructions);

    addOperatorLogEntry(
      "Sent a booking request with the parameters: %s"
        .formatted(carrierScenarioParameters.toJson()));
  }

  private void sendUpdatedShippingInstructionsRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendUpdatedShippingInstructionsRequest(%s)".formatted(actionPrompt.toPrettyString()));

    var sir = actionPrompt.required("sir").asText();
    var si = (ObjectNode) persistentMap.load(sir);

    var pcd = si.required("partyContactDetails").required(0);
    var newName = "DCSA test person2";
    if (pcd.required("name").asText("").equals(newName)) {
      newName = "DCSA test person";
    }
    ((ObjectNode)pcd).put("name", newName);

    ConformanceResponse conformanceResponse = syncCounterpartPut("/v3/shipping-instructions/%s".formatted(sir), si);

    JsonNode jsonBody = conformanceResponse.message().body().getJsonBody();
    String shippingInstructionsStatus = jsonBody.path("shippingInstructionsStatus").asText();
    ObjectNode updatedBooking = si.put("shippingInstructionsStatus", shippingInstructionsStatus);
    persistentMap.save(sir, updatedBooking);

    addOperatorLogEntry(
      "Sent a shipping instructions update with the parameters: %s"
        .formatted(actionPrompt.toPrettyString()));
  }

  private void cancelUpdateToShippingInstructions(JsonNode actionPrompt) {
    log.info("Shipper.cancelUpdateToShippingInstructions(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    var approvePayload = new ObjectMapper().createObjectNode()
      .put("updatedShippingInstructionsStatus", ShippingInstructionsStatus.SI_UPDATE_CANCELLED.wireName());

    syncCounterpartPatch(
      "/v3/shipping-instructions/%s".formatted(documentReference),
      Collections.emptyMap(),
      approvePayload);

    addOperatorLogEntry(
      "Approved transport document the parameters: %s"
        .formatted(actionPrompt.toPrettyString()));
  }

  private void approveDraftTransportDocument(JsonNode actionPrompt) {
    log.info("Shipper.approveDraftTransportDocument(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    var approvePayload = new ObjectMapper().createObjectNode()
      .put("transportDocumentStatus", TransportDocumentStatus.TD_APPROVED.wireName());

    syncCounterpartPatch(
      "/v3/transport-documents/%s".formatted(documentReference),
      Collections.emptyMap(),
      approvePayload);

    addOperatorLogEntry(
      "Approved transport document the parameters: %s"
        .formatted(actionPrompt.toPrettyString()));
  }


  private void getShippingInstructionsRequest(JsonNode actionPrompt) {
    log.info("Shipper.getShippingInstructionsRequest(%s)".formatted(actionPrompt.toPrettyString()));
    String sir = actionPrompt.get("sir").asText();
    boolean requestAmendment = actionPrompt.path("amendedContent").asBoolean(false);
    Map<String, List<String>> queryParams = requestAmendment
      ? Map.of("amendedContent", List.of("true"))
      : Collections.emptyMap();

    syncCounterpartGet("/v3/shipping-instructions/" + sir, queryParams);

    addOperatorLogEntry("Sent a GET request for shipping instructions with SIR: %s".formatted(sir));
  }

  private void getTransportDocument(JsonNode actionPrompt) {
    log.info("Shipper.getTransportDocument(%s)".formatted(actionPrompt.toPrettyString()));
    String tdr = actionPrompt.required("tdr").asText();

    syncCounterpartGet("/v3/transport-documents/" + tdr, Collections.emptyMap());

    addOperatorLogEntry("Sent a GET request for transport document with TDR: %s".formatted(tdr));
  }


  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("Shipper.handleRequest(%s)".formatted(request));

    ConformanceResponse response =
        request.createResponse(
            204,
            Map.of("Api-Version", List.of(apiVersion)),
            new ConformanceMessageBody(new ObjectMapper().createObjectNode()));

    addOperatorLogEntry(
        "Handled lightweight notification: %s".formatted(request.message().body().getJsonBody()));
    return response;
  }
}
