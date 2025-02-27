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
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.ebl.action.*;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.models.OutOfOrderMessageType;

@Slf4j
public class EblShipper extends ConformanceParty {

  private static final String CONSIGNMENT_ITEMS = "consignmentItems";

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
      Map.entry(UC7_Shipper_ApproveDraftTransportDocumentAction.class, this::approveDraftTransportDocument)
    );
  }

  static ObjectNode siFromScenarioType(ScenarioType scenarioType, CarrierScenarioParameters carrierScenarioParameters, String apiVersion) {

    var jsonRequestBody =
        (ObjectNode)
            JsonToolkit.templateFileToJsonNode(
                "/standards/ebl/messages/" + scenarioType.shipperTemplate(apiVersion),
                Map.ofEntries(
                    Map.entry(
                        "CARRIER_BOOKING_REFERENCE_PLACEHOLDER",
                        carrierScenarioParameters.carrierBookingReference()),
                    Map.entry(
                        "COMMODITY_SUBREFERENCE_PLACEHOLDER",
                        Objects.requireNonNullElse(
                            carrierScenarioParameters.commoditySubReference(), "")),
                    Map.entry(
                        "COMMODITY_SUBREFERENCE_2_PLACEHOLDER",
                        Objects.requireNonNullElse(
                            carrierScenarioParameters.commoditySubReference2(), "")),
                    Map.entry(
                        "EQUIPMENT_REFERENCE_PLACEHOLDER",
                        carrierScenarioParameters.equipmentReference()),
                    Map.entry(
                        "EQUIPMENT_REFERENCE_2_PLACEHOLDER",
                        Objects.requireNonNullElse(
                            carrierScenarioParameters.equipmentReference2(), "")),
                    Map.entry(
                        "INVOICE_PAYABLE_AT_UNLOCATION_CODE",
                        carrierScenarioParameters.invoicePayableAtUNLocationCode()),
                    Map.entry(
                        "CONSIGNMENT_ITEM_HS_CODE",
                        carrierScenarioParameters.consignmentItemHSCode()),
                    Map.entry(
                        "CONSIGNMENT_ITEM_2_HS_CODE",
                        Objects.requireNonNullElse(
                            carrierScenarioParameters.consignmentItem2HSCode(), "")),
                    Map.entry(
                        "DESCRIPTION_OF_GOODS_PLACEHOLDER",
                        carrierScenarioParameters.descriptionOfGoods()),
                    Map.entry(
                        "DESCRIPTION_OF_GOODS_2_PLACEHOLDER",
                        Objects.requireNonNullElse(
                            carrierScenarioParameters.descriptionOfGoods2(), "")),
                    Map.entry(
                        "OUTER_PACKAGING_DESCRIPTION_PLACEHOLDER",
                        Objects.requireNonNullElse(
                            carrierScenarioParameters.outerPackagingDescription(), "")),
                    Map.entry(
                        "TRANSPORT_DOCUMENT_TYPE_CODE_PLACEHOLDER",
                        scenarioType.transportDocumentTypeCode())));

    removeEmptyFields(jsonRequestBody, scenarioType, carrierScenarioParameters);
    // Cannot substitute this because it is a boolean
    if (!scenarioType.isToOrder()) {
      // Cannot substitute this because it is a full element
      var parties = (ObjectNode) jsonRequestBody.path("documentParties");
      parties
          .putObject("consignee")
          .put("partyName", "DCSA CTK Consignee")
          .putArray("identifyingCodes")
          .addObject()
          .put("codeListProvider", "W3C")
          .put("partyCode", "MSK")
          .put("codeListName", "DID")
          .putArray("partyContactDetails")
          .addObject()
          .put("name", "DCSA another test person")
          .put("email", "no-reply@dcsa-consignee.example.org");
    }
    if (scenarioType.transportDocumentTypeCode().equals("BOL")) {
      JsonNode documentParties = jsonRequestBody.path("documentParties");
      if (!documentParties.isMissingNode() && !documentParties.path("issueTo").isMissingNode()) {
        var issueToParty = (ObjectNode) documentParties.path("issueTo");
        issueToParty.put("sendToPlatform", "CARX");
      }
    }
    return jsonRequestBody;
  }

  private static void removeEmptyFields(
      ObjectNode jsonRequestBody,
      ScenarioType scenarioType,
      CarrierScenarioParameters carrierScenarioParameters) {

    if (carrierScenarioParameters.commoditySubReference() == null
        || carrierScenarioParameters.commoditySubReference().isEmpty()) {
      ((ObjectNode) jsonRequestBody.withArray(CONSIGNMENT_ITEMS).get(0))
          .remove("commoditySubReference");
    }
    if ((scenarioType.equals(ScenarioType.REGULAR_2C_2U_2E)
            || scenarioType.equals(ScenarioType.REGULAR_2C_2U_1E))
        && (carrierScenarioParameters.commoditySubReference2() == null
            || carrierScenarioParameters.commoditySubReference2().isEmpty())) {
      ((ObjectNode) jsonRequestBody.withArray(CONSIGNMENT_ITEMS).get(1))
          .remove("commoditySubReference");
    }

    if (carrierScenarioParameters.outerPackagingDescription() == null
        || carrierScenarioParameters.outerPackagingDescription().isEmpty()
            && scenarioType.equals(ScenarioType.DG)) {
      jsonRequestBody
          .withArray(CONSIGNMENT_ITEMS)
          .forEach(
              consignmentItem ->
                  consignmentItem
                      .withArray("cargoItems")
                      .forEach(cargoItem -> ((ObjectNode) cargoItem).remove("outerPackaging")));
    }
  }

  private void sendShippingInstructionsRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendShippingInstructionsRequest(%s)".formatted(actionPrompt.toPrettyString()));

    CarrierScenarioParameters carrierScenarioParameters =
      CarrierScenarioParameters.fromJson(actionPrompt.get("csp"));
    var scenarioType = ScenarioType.valueOf(actionPrompt.required("scenarioType").asText());
    var jsonRequestBody = siFromScenarioType(
      scenarioType,
      carrierScenarioParameters,
      apiVersion
    );
    ConformanceResponse conformanceResponse = syncCounterpartPost("/v3/shipping-instructions", jsonRequestBody);

    JsonNode jsonBody = conformanceResponse.message().body().getJsonBody();
    String shippingInstructionsReference = jsonBody.path("shippingInstructionsReference").asText();
    ObjectNode updatedShippingInstructions = jsonRequestBody
        .put("shippingInstructionsReference", shippingInstructionsReference);
    persistentMap.save(shippingInstructionsReference, updatedShippingInstructions);

    addOperatorLogEntry(
        "Sent a shipping instructions request with the parameters: %s"
            .formatted(carrierScenarioParameters.toJson()));
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
    Map<String, List<String>> queryParams = requestAmendment
      ? Map.of("updatedContent", List.of("true"))
      : Collections.emptyMap();

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
