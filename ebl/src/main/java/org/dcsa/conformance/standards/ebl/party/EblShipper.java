package org.dcsa.conformance.standards.ebl.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.ebl.action.Shipper_GetShippingInstructionsAction;
import org.dcsa.conformance.standards.ebl.action.Shipper_GetTransportDocumentAction;
import org.dcsa.conformance.standards.ebl.action.UC1_Shipper_SubmitShippingInstructionsAction;
import org.dcsa.conformance.standards.ebl.action.UC3_Shipper_SubmitUpdatedShippingInstructionsAction;

@Slf4j
public class EblShipper extends ConformanceParty {

  public EblShipper(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      BiConsumer<ConformanceRequest, Consumer<ConformanceResponse>> asyncWebClient,
      Map<String, ? extends Collection<String>> orchestratorAuthHeader) {
    super(
        apiVersion,
        partyConfiguration,
        counterpartConfiguration,
        persistentMap,
        asyncWebClient,
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
      Map.entry(UC3_Shipper_SubmitUpdatedShippingInstructionsAction.class, this::sendUpdatedShippingInstructionsRequest)
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
              Map.entry("DESCRIPTION_OF_GOODS_PLACEHOLDER", carrierScenarioParameters.descriptionOfGoods())
              ));

    asyncCounterpartPost(
      "/v3/shipping-instructions",
      jsonRequestBody,
      conformanceResponse -> {
        JsonNode jsonBody = conformanceResponse.message().body().getJsonBody();
        String shippingInstructionsReference = jsonBody.path("shippingInstructionsReference").asText();
        String shippingInstructionsStatus = jsonBody.path("shippingInstructionsStatus").asText();
        ObjectNode updatedShippingInstructions =
          ((ObjectNode) jsonRequestBody)
            .put("shippingInstructionsStatus", shippingInstructionsStatus)
            .put("shippingInstructionsReference", shippingInstructionsReference);
        persistentMap.save(shippingInstructionsReference, updatedShippingInstructions);
      });

    addOperatorLogEntry(
      "Sent a booking request with the parameters: %s"
        .formatted(carrierScenarioParameters.toJson()));
  }

  private void sendUpdatedShippingInstructionsRequest(JsonNode actionPrompt) {
    log.info("Shipper.sendUpdatedShippingInstructionsRequest(%s)".formatted(actionPrompt.toPrettyString()));

    var sir = actionPrompt.required("sir").asText();
    var si = (ObjectNode) persistentMap.load(sir);

    si.put("transportDocumentTypeCode", "SWB");
    asyncCounterpartPut(
      "/v3/shipping-instructions/%s".formatted(sir),
      si,
      conformanceResponse -> {
        JsonNode jsonBody = conformanceResponse.message().body().getJsonBody();
        String shippingInstructionsStatus = jsonBody.path("shippingInstructionsStatus").asText();
        ObjectNode updatedBooking = si.put("shippingInstructionsStatus", shippingInstructionsStatus);
        persistentMap.save(sir, updatedBooking);
      });

    addOperatorLogEntry(
      "Sent a shipping instructions update with the parameters: %s"
        .formatted(actionPrompt.toPrettyString()));
  }

  private void getShippingInstructionsRequest(JsonNode actionPrompt) {
    log.info("Shipper.getShippingInstructionsRequest(%s)".formatted(actionPrompt.toPrettyString()));
    String sir = actionPrompt.get("sir").asText();
    boolean requestAmendment = actionPrompt.path("amendedContent").asBoolean(false);
    Map<String, List<String>> queryParams = requestAmendment
      ? Map.of("amendedContent", List.of("true"))
      : Collections.emptyMap();

    asyncCounterpartGet("/v3/shipping-instructions/" + sir, queryParams);

    addOperatorLogEntry("Sent a GET request for shipping instructions with SIR: %s".formatted(sir));
  }

  private void getTransportDocument(JsonNode actionPrompt) {
    log.info("Shipper.getTransportDocument(%s)".formatted(actionPrompt.toPrettyString()));
    String tdr = actionPrompt.required("tdr").asText();

    asyncCounterpartGet("/v3/transport-documents/" + tdr);

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