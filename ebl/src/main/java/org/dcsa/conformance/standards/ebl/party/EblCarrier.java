package org.dcsa.conformance.standards.ebl.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.party.PartyWebClient;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.state.StateManagementUtil;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.ebl.action.*;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.models.CarrierShippingInstructions;

import static org.dcsa.conformance.standards.ebl.party.EblShipper.siFromScenarioType;

@Slf4j
public class EblCarrier extends ConformanceParty {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final Map<String, String> tdrToSir = new HashMap<>();

  protected boolean isShipperNotificationEnabled = true;

  public EblCarrier(
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
    targetObjectNode.set("tdrToSir", StateManagementUtil.storeMap(OBJECT_MAPPER, tdrToSir));
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StateManagementUtil.restoreIntoMap(tdrToSir, sourceObjectNode.get("tdrToSir"));
  }

  @Override
  protected void doReset() {
    tdrToSir.clear();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
      Map.entry(Carrier_SupplyScenarioParametersAction.class, this::supplyScenarioParameters),
      Map.entry(UC2_Carrier_RequestUpdateToShippingInstructionsAction.class, this::requestUpdateToShippingInstructions),
      Map.entry(UC4_Carrier_ProcessUpdateToShippingInstructionsAction.class, this::processUpdatedShippingInstructions),
      Map.entry(UC6_Carrier_PublishDraftTransportDocumentAction.class, this::publishDraftTransportDocument),
      Map.entry(UC8_Carrier_IssueTransportDocumentAction.class, this::issueTransportDocument),
      Map.entry(UC9_Carrier_AwaitSurrenderRequestForAmendmentAction.class, this::notifyOfSurrenderForAmendment),
      Map.entry(UC10_Carrier_ProcessSurrenderRequestForAmendmentAction.class, this::processSurrenderRequestForAmendment),
      Map.entry(UC11v_Carrier_VoidTransportDocumentAction.class, this::voidTransportDocument),
      Map.entry(UC11i_Carrier_IssueAmendedTransportDocumentAction.class, this::issueAmendedTransportDocument),
      Map.entry(UC12_Carrier_AwaitSurrenderRequestForDeliveryAction.class, this::notifyOfSurrenderForDelivery),
      Map.entry(UC13_Carrier_ProcessSurrenderRequestForDeliveryAction.class, this::processSurrenderRequestForDelivery),
      Map.entry(UC14_Carrier_ConfirmShippingInstructionsCompleteAction.class, this::confirmShippingInstructionsComplete),
      Map.entry(UCX_Carrier_TDOnlyProcessOutOfBandUpdateOrAmendmentRequestDraftTransportDocumentAction.class, this::processOutOfBandUpdateOrAmendmentRequestTransportDocumentAction)
    );
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info("Carrier.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));
    var scenarioType = ScenarioType.valueOf(actionPrompt.required("scenarioType").asText());
    CarrierScenarioParameters carrierScenarioParameters =
      switch (scenarioType) {
        case REGULAR_SWB, REGULAR_BOL, REGULAR_SWB_AMF -> new CarrierScenarioParameters(
          "CBR_123_" + scenarioType.name(),
          "Some Commodity Subreference 123",
          null,
          // A "22G1" container - keep aligned with the fixupUtilizedTransportEquipments()
          "NARU3472484",
          null,
          "DKAAR",
          "640510",
          null,
          "Shoes - black, 400 boxes",
          null,
          "Fibreboard boxes",
          "SCR-1234-REGULAR",
          "QR-1234-REGULAR"
        );
        case ACTIVE_REEFER -> new CarrierScenarioParameters(
          "CBR_123_REEFER",
          "Some reefer Commodity Subreference 123",
          null,
          // A "45R1" container - keep aligned with the fixupUtilizedTransportEquipments()
          "KKFU6671914",
          null,
          "DKAAR",
          "04052090",
          null,
          "Dairy products",
          null,
          "Bottles",
          "SCR-1234-REEFER",
          "QR-1234-REEFER"
        );
        case NON_OPERATING_REEFER -> new CarrierScenarioParameters(
          "CBR_123_NON_OPERATING_REEFER",
          "Some reefer Commodity Subreference 123",
          null,
          // A "45R1" container - keep aligned with the fixupUtilizedTransportEquipments()
          "KKFU6671914",
          null,
          "DKAAR",
          "220299",
          null,
          "Non alcoholic beverages, 40,000 cans",
          null,
          "Bottles",
          "SCR-1234-NON_OPERATING_REEFER",
          "QR-1234-NON_OPERATING_REEFER"
        );
        case DG -> new CarrierScenarioParameters(
          "RTM1234567",
          "Some DG Commodity Subreference 123",
          null,
          // A "22GP" container - keep aligned with the fixupUtilizedTransportEquipments()
          "HLXU1234567",
          null,
          "DKAAR",
          "293499",
          null,
          "Environmentally hazardous substance, liquid, N.O.S (Propiconazole)",
          null,
          null,
          "SCR-1234-DG",
          "QR-1234-DG"
        );
        case REGULAR_2C_2U_1E -> new CarrierScenarioParameters(
          "RG-2C-2U-1E",
          "Commodity Subreference 123",
          "Commodity Subreference 456",
          // A "22G1" container - keep aligned with the fixupUtilizedTransportEquipments()
          "MSKU3963442",
          "MSKU7895860",
          "DKAAR",
          "691110",
          "732391",
          "Tableware and kitchenware",
          "Kitchen pots and pans",
          "Fibreboard boxes",
          "SCR-1234-RG2C2U1E",
          "QR-1234-RG2C2U1E"
        );
        case REGULAR_2C_2U_2E -> new CarrierScenarioParameters(
          "RG-2C-2U-1E",
          "Commodity Subreference 123",
          "Commodity Subreference 456",
          // A "22G1" container - keep aligned with the fixupUtilizedTransportEquipments()
          "MSKU3963442",
          "MSKU7895860",
          "DKAAR",
          "691110",
          "732391",
          "Tableware and kitchenware",
          "Kitchen pots and pans",
          "Fibreboard boxes",
          "SCR-1234-RG2C2U2E",
          "QR-1234-RG2C2U2E"
        );
        case REGULAR_SWB_SOC_AND_REFERENCES -> new CarrierScenarioParameters(
          "RG-SOC-REFERENCES",
          "Commodity Subreference 123",
          null,
          null,
          null,
          "DKAAR",
          "691110",
          null,
          "Tableware and kitchenware",
          null,
          "Fibreboard boxes",
          "SCR-1234-RG-SOC-REFERENCES",
          "QR-1234-RG-SOC-REFERENCES"
        );
    };
    asyncOrchestratorPostPartyInput(
      OBJECT_MAPPER
        .createObjectNode()
        .put("actionId", actionPrompt.required("actionId").asText())
        .set("input", carrierScenarioParameters.toJson()));
    addOperatorLogEntry(
      "Provided CarrierScenarioParameters: %s".formatted(carrierScenarioParameters));
  }

  private void requestUpdateToShippingInstructions(JsonNode actionPrompt) {
    log.info("Carrier.requestUpdateToShippingInstructions(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);

    var si = CarrierShippingInstructions.fromPersistentStore(persistentMap, sir);
    si.requestChangesToShippingInstructions(documentReference, requestedChanges -> requestedChanges.addObject()
      .put("message", "Please perform the changes requested by the Conformance orchestrator")
    );
    si.save(persistentMap);
    generateAndEmitNotificationFromShippingInstructions(actionPrompt, si, true);

    addOperatorLogEntry("Requested update to the shipping instructions with document reference '%s'".formatted(documentReference));
  }


  private void processUpdatedShippingInstructions(JsonNode actionPrompt) {
    log.info("Carrier.processUpdatedShippingInstructions(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    var accept = actionPrompt.required("acceptChanges").asBoolean(true);
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);

    var si = CarrierShippingInstructions.fromPersistentStore(persistentMap, sir);
    if (accept) {
      si.acceptUpdatedShippingInstructions(documentReference);
    } else {
      si.declineUpdatedShippingInstructions(documentReference, "Declined as requested by the Conformance orchestrator");
    }
    si.save(persistentMap);
    generateAndEmitNotificationFromShippingInstructions(actionPrompt, si, true);

    addOperatorLogEntry("Processed update to the shipping instructions with document reference '%s'".formatted(documentReference));
  }



  private void processOutOfBandUpdateOrAmendmentRequestTransportDocumentAction(JsonNode actionPrompt) {
    log.info("Carrier.processOutOfBandUpdateOrAmendmentRequestTransportDocumentAction(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);
    var si = CarrierShippingInstructions.fromPersistentStore(persistentMap, sir);
    var updatedSI = EblShipper.updateShippingInstructions(si.getShippingInstructions());
    si.putShippingInstructions(documentReference, updatedSI);
    si.acceptUpdatedShippingInstructions(documentReference);
    asyncOrchestratorPostPartyInput(
      OBJECT_MAPPER
        .createObjectNode()
        .put("actionId", actionPrompt.required("actionId").asText()));
    addOperatorLogEntry("Process out of band amendment for transport document '%s'".formatted(documentReference));
  }

  private void publishDraftTransportDocument(JsonNode actionPrompt) {
    log.info("Carrier.publishDraftTransportDocument(%s)".formatted(actionPrompt.toPrettyString()));

    var scenarioType = ScenarioType.valueOf(actionPrompt.required("scenarioType").asText());
    var skipSI = actionPrompt.required("skipSI").asBoolean(false);
    String documentReference;
    CarrierShippingInstructions si;
    if (skipSI) {
      var csp = CarrierScenarioParameters.fromJson(actionPrompt.required("csp"));
      var jsonRequestBody = siFromScenarioType(
        scenarioType,
        csp,
        apiVersion
      );
      si = CarrierShippingInstructions.initializeFromShippingInstructionsRequest(jsonRequestBody, apiVersion);
      documentReference = si.getShippingInstructionsReference();
    } else {
      documentReference = actionPrompt.required("documentReference").asText();
      var sir = tdrToSir.getOrDefault(documentReference, documentReference);
      si = CarrierShippingInstructions.fromPersistentStore(persistentMap, sir);
    }
    si.publishDraftTransportDocument(documentReference, scenarioType);
    si.save(persistentMap);
    tdrToSir.put(si.getTransportDocumentReference(), si.getShippingInstructionsReference());
    generateAndEmitNotificationFromTransportDocument(actionPrompt, si, true);

    addOperatorLogEntry("Published draft transport document '%s'".formatted(documentReference));
  }

  private void issueTransportDocument(JsonNode actionPrompt) {
    log.info("Carrier.issueTransportDocument(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);

    var si = CarrierShippingInstructions.fromPersistentStore(persistentMap, sir);
    si.issueTransportDocument(documentReference);
    si.save(persistentMap);
    generateAndEmitNotificationFromTransportDocument(actionPrompt, si, true);

    addOperatorLogEntry("Issued transport document '%s'".formatted(documentReference));
  }

  private void notifyOfSurrenderForAmendment(JsonNode actionPrompt) {
    log.info("Carrier.notifyOfSurrenderForAmendment(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);

    var si = CarrierShippingInstructions.fromPersistentStore(persistentMap, sir);
    si.surrenderForAmendmentRequest(documentReference);
    si.save(persistentMap);
    generateAndEmitNotificationFromTransportDocument(actionPrompt, si, true);

    addOperatorLogEntry("Sent notification for surrender for amendment of transport document with reference '%s'".formatted(documentReference));
  }

  private void notifyOfSurrenderForDelivery(JsonNode actionPrompt) {
    log.info("Carrier.notifyOfSurrenderForDelivery(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);
    var si = CarrierShippingInstructions.fromPersistentStore(persistentMap, sir);
    si.surrenderForDeliveryRequest(documentReference);
    si.save(persistentMap);
    generateAndEmitNotificationFromTransportDocument(actionPrompt, si, true);

    addOperatorLogEntry("Sent notification for surrender for delivery of transport document with reference '%s'".formatted(documentReference));
  }

  private void processSurrenderRequestForAmendment(JsonNode actionPrompt) {
    log.info("Carrier.processSurrenderRequestForAmendment(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);
    var accept = actionPrompt.required("acceptAmendmentRequest").asBoolean(true);

    var si = CarrierShippingInstructions.fromPersistentStore(persistentMap, sir);
    if (accept) {
      si.acceptSurrenderForAmendment(documentReference);
    } else {
      si.rejectSurrenderForAmendment(documentReference);
    }
    si.save(persistentMap);
    generateAndEmitNotificationFromTransportDocument(actionPrompt, si, true);

    addOperatorLogEntry("Processed surrender request for delivery of transport document with reference '%s'".formatted(documentReference));
  }

  private void voidTransportDocument(JsonNode actionPrompt) {
    log.info("Carrier.voidTransportDocument(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);
    var si = CarrierShippingInstructions.fromPersistentStore(persistentMap, sir);
    si.voidTransportDocument(documentReference);
    si.save(persistentMap);
    generateAndEmitNotificationFromTransportDocument(actionPrompt, si, true);

    addOperatorLogEntry("Voided transport document '%s'".formatted(documentReference));
  }

  private void issueAmendedTransportDocument(JsonNode actionPrompt) {
    log.info("Carrier.issueAmendedTransportDocument(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    var scenarioType = ScenarioType.valueOf(actionPrompt.required("scenarioType").asText());
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);

    var si = CarrierShippingInstructions.fromPersistentStore(persistentMap, sir);
    si.issueAmendedTransportDocument(documentReference, scenarioType);
    si.save(persistentMap);
    tdrToSir.put(si.getTransportDocumentReference(), si.getShippingInstructionsReference());
    generateAndEmitNotificationFromTransportDocument(actionPrompt, si, true);

    addOperatorLogEntry("Issued amended transport document '%s'".formatted(documentReference));
  }

  private void processSurrenderRequestForDelivery(JsonNode actionPrompt) {
    log.info("Carrier.processSurrenderRequestForDelivery(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);
    var accept = actionPrompt.required("acceptDeliveryRequest").asBoolean(true);

    var si = CarrierShippingInstructions.fromPersistentStore(persistentMap, sir);
    if (accept) {
      si.acceptSurrenderForDelivery(documentReference);
    } else {
      si.rejectSurrenderForDelivery(documentReference);
    }
    si.save(persistentMap);
    generateAndEmitNotificationFromTransportDocument(actionPrompt, si, true);

    addOperatorLogEntry("Processed surrender request for delivery of transport document with reference '%s'".formatted(documentReference));
  }

  private void confirmShippingInstructionsComplete(JsonNode actionPrompt) {
    log.info("Carrier.confirmShippingInstructionsComplete(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.required("documentReference").asText();
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);

    var si = CarrierShippingInstructions.fromPersistentStore(persistentMap, sir);
    si.confirmShippingInstructionsComplete(documentReference);
    si.save(persistentMap);
    generateAndEmitNotificationFromShippingInstructions(actionPrompt, si, true);

    addOperatorLogEntry("Confirmed shipping instructions with reference %s is complete".formatted(documentReference));
  }

  private void generateAndEmitNotificationFromShippingInstructions(JsonNode actionPrompt, CarrierShippingInstructions shippingInstructions, boolean includeShippingInstructionsReference) {
    var notification =
      ShippingInstructionsNotification.builder()
        .apiVersion(apiVersion)
        .shippingInstructions(shippingInstructions.getShippingInstructions())
        .includeShippingInstructionsReference(includeShippingInstructionsReference)
        .subscriptionReference(shippingInstructions.getSubscriptionReference())
        .build()
        .asJsonNode();
    if (isShipperNotificationEnabled) {
      asyncCounterpartNotification("/v3/shipping-instructions-notifications", notification);
    } else {
      asyncOrchestratorPostPartyInput(
        OBJECT_MAPPER.createObjectNode().put("actionId", actionPrompt.required("actionId").asText()));
    }
  }


  private void generateAndEmitNotificationFromTransportDocument(JsonNode actionPrompt, CarrierShippingInstructions shippingInstructions, boolean includeShippingInstructionsReference) {
    var notification =
      TransportDocumentNotification.builder()
        .apiVersion(apiVersion)
        .transportDocument(shippingInstructions.getTransportDocument().orElseThrow())
        .includeShippingInstructionsReference(includeShippingInstructionsReference)
        .subscriptionReference(shippingInstructions.getSubscriptionReference())
        .build()
        .asJsonNode();
    if (isShipperNotificationEnabled) {
      asyncCounterpartNotification("/v3/transport-document-notifications", notification);
    } else {
      asyncOrchestratorPostPartyInput(
        OBJECT_MAPPER.createObjectNode().put("actionId", actionPrompt.required("actionId").asText()));
    }
  }

  private ConformanceResponse return405(ConformanceRequest request, String... allowedMethods) {
    return request.createResponse(
      405,
      Map.of(
        "Api-Version", List.of(apiVersion), "Allow", List.of(String.join(",", allowedMethods))),
      new ConformanceMessageBody(
        OBJECT_MAPPER
          .createObjectNode()
          .put("message", "Returning 405 because the method was not supported")));
  }

  private ConformanceResponse return400(ConformanceRequest request, String message) {
    return request.createResponse(
      400,
      Map.of("Api-Version", List.of(apiVersion)),
      new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode().put("message", message)));
  }

  private ConformanceResponse return404(ConformanceRequest request) {
    return return404(request, "Returning 404 since the request did not match any known URL");
  }
  private ConformanceResponse return404(ConformanceRequest request, String message) {
    return request.createResponse(
      404,
      Map.of("Api-Version", List.of(apiVersion)),
      new ConformanceMessageBody(
        OBJECT_MAPPER
          .createObjectNode()
          .put("message", message)));
  }

  private ConformanceResponse return409(ConformanceRequest request, String message) {
    return request.createResponse(
      409,
      Map.of("Api-Version", List.of(apiVersion)),
      new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode().put("message", message)));
  }

  private static final Set<String> QUERY_BOOLEAN = Set.of("true", "false");
  private String readAmendedContent(ConformanceRequest request) {
    var queryParams = request.queryParams();
    var operationParams = queryParams.get("updatedContent");
    if (operationParams == null || operationParams.isEmpty()) {
      return "false";
    }
    var operation = operationParams.iterator().next();
    if (operationParams.size() > 1 || !QUERY_BOOLEAN.contains(operation)) {
      return "!INVALID-VALUE!";
    }
    return operation;
  }

  private ConformanceResponse _handleGetShippingInstructionsRequest(ConformanceRequest request, String documentReference) {
    var amendedContentRaw = readAmendedContent(request);
    boolean amendedContent;
    if (amendedContentRaw.equals("true") || amendedContentRaw.equals("false")) {
      amendedContent = amendedContentRaw.equals("true");
    } else {
      return return400(request, "The amendedContent queryParam must be used at most once and" +
        " must be one of true or false");
    }
    // documentReference can either be a SIR or TDR.
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);
    var persistedBookingData = persistentMap.load(sir);

    if (persistedBookingData != null) {
      var si = CarrierShippingInstructions.fromPersistentStore(persistedBookingData);
      JsonNode body;
      if (amendedContent) {
        body = si.getUpdatedShippingInstructions().orElse(null);
        if (body == null) {
          return return404(request, "No amended version of booking with reference: " + documentReference);
        }
      } else {
        body = si.getShippingInstructions();
      }
      ConformanceResponse response =
        request.createResponse(
          200,
          Map.of("Api-Version", List.of(apiVersion)),
          new ConformanceMessageBody(body));
      addOperatorLogEntry(
        "Responded to GET shipping instructions request '%s' (in state '%s')"
          .formatted(documentReference, si.getShippingInstructionsState().wireName()));
      return response;
    }
    return return404(request);
  }

  private ConformanceResponse _handleGetTransportDocument(ConformanceRequest request, String documentReference) {
    // bookingReference can either be a CBR or CBRR.
    var sir = tdrToSir.get(documentReference);
    if (sir == null) {
      return return404(request);
    }
    var persistedSi = persistentMap.load(sir);
    if (persistedSi == null) {
      throw new IllegalStateException("We had a TDR -> SIR mapping, but there is no data related to that reference");
    }
    var si = CarrierShippingInstructions.fromPersistentStore(persistedSi);
    // If the TDR is resolvable, then the document must have a TD.
    var body = si.getTransportDocument().orElseThrow();
    ConformanceResponse response =
      request.createResponse(
        200,
        Map.of("Api-Version", List.of(apiVersion)),
        new ConformanceMessageBody(body));
    addOperatorLogEntry(
      "Responded to GET transport document request '%s' (in state '%s')"
        .formatted(documentReference, si.getShippingInstructionsState().wireName()));
    return response;
  }


  private ConformanceResponse _handlePatchShippingInstructions(ConformanceRequest request, String documentReference) {
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);
    var persistedSi = persistentMap.load(sir);
    if (persistedSi == null) {
      return return404(request);
    }
    var si = CarrierShippingInstructions.fromPersistentStore(persistedSi);
    si.cancelShippingInstructionsUpdate(documentReference);
    si.save(persistentMap);
    var siData = si.getShippingInstructions();
    if (isShipperNotificationEnabled) {
      asyncCounterpartNotification(
          "/v3/shipping-instructions-notifications",
          ShippingInstructionsNotification.builder()
              .apiVersion(apiVersion)
              .shippingInstructions(siData)
              .subscriptionReference(si.getSubscriptionReference())
              .build()
              .asJsonNode());
    }
    return returnShippingInstructionsRefStatusResponse(
      200,
      request,
      siData,
      documentReference
    );
  }

  private ConformanceResponse _handlePatchTransportDocument(ConformanceRequest request, String documentReference) {
    var sir = tdrToSir.get(documentReference);
    if (sir == null) {
      return return404(request);
    }
    var persistedSi = persistentMap.load(sir);
    if (persistedSi == null) {
      throw new IllegalStateException("We had a TDR -> SIR mapping, but there is no data related to that reference");
    }
    var si = CarrierShippingInstructions.fromPersistentStore(persistedSi);
    si.approveDraftTransportDocument(documentReference);
    si.save(persistentMap);
    var td = si.getTransportDocument().orElseThrow();
    if (isShipperNotificationEnabled) {
      asyncCounterpartNotification(
          "/v3/transport-document-notifications",
          TransportDocumentNotification.builder()
              .apiVersion(apiVersion)
              .transportDocument(td)
              .subscriptionReference(si.getSubscriptionReference())
              .build()
              .asJsonNode());
    }
    return returnTransportDocumentRefStatusResponse(
      200,
      request,
      td,
      documentReference
    );
  }

  private ConformanceResponse returnShippingInstructionsRefStatusResponse(
    int responseCode, ConformanceRequest request, ObjectNode shippingInstructions, String documentReference) {
    var sir = shippingInstructions.required("shippingInstructionsReference").asText();
    var siStatus = shippingInstructions.required("shippingInstructionsStatus").asText();
    var statusObject =
      OBJECT_MAPPER
        .createObjectNode()
        .put("shippingInstructionsStatus", siStatus)
        .put("shippingInstructionsReference", sir);
    var tdr = shippingInstructions.path("transportDocumentReference");
    var updatedSiStatus = shippingInstructions.path("updatedShippingInstructionsStatus");
    var reason = shippingInstructions.path("reason");
    if (tdr.isTextual()) {
      statusObject.set("transportDocumentReference", tdr);
    }
    if (updatedSiStatus.isTextual()) {
      statusObject.set("updatedShippingInstructionsStatus", updatedSiStatus);
    }
    if (reason.isTextual()) {
      statusObject.set("reason", reason);
    }
    ConformanceResponse response =
      request.createResponse(
        responseCode,
        Map.of("Api-Version", List.of(apiVersion)),
        new ConformanceMessageBody(statusObject));
    addOperatorLogEntry(
      "Responded %d to %s SI '%s' (resulting state '%s')"
        .formatted(responseCode, request.method(), documentReference, siStatus));
    return response;
  }


  private ConformanceResponse returnTransportDocumentRefStatusResponse(
    int responseCode, ConformanceRequest request, ObjectNode transportDocument, String documentReference) {
    var tdr = transportDocument.required("transportDocumentReference").asText();
    var tdStatus = transportDocument.required("transportDocumentStatus").asText();
    var statusObject =
      OBJECT_MAPPER
        .createObjectNode()
        .put("transportDocumentStatus", tdStatus)
        .put("transportDocumentReference", tdr);
    ConformanceResponse response =
      request.createResponse(
        responseCode,
        Map.of("Api-Version", List.of(apiVersion)),
        new ConformanceMessageBody(statusObject));
    addOperatorLogEntry(
      "Responded %d to %s TD '%s' (resulting state '%s')"
        .formatted(responseCode, request.method(), documentReference, tdStatus));
    return response;
  }

  @SneakyThrows
  private ConformanceResponse _handlePostShippingInstructions(ConformanceRequest request) {
    ObjectNode siPayload =
      (ObjectNode) OBJECT_MAPPER.readTree(request.message().body().getJsonBody().toString());
    var si = CarrierShippingInstructions.initializeFromShippingInstructionsRequest(siPayload, apiVersion);
    si.save(persistentMap);
    if (isShipperNotificationEnabled) {
      asyncCounterpartNotification(
          "/v3/shipping-instructions-notifications",
          ShippingInstructionsNotification.builder()
              .apiVersion(apiVersion)
              .shippingInstructions(si.getShippingInstructions())
              .subscriptionReference(si.getSubscriptionReference())
              .build()
              .asJsonNode());
    }
    return returnShippingInstructionsRefStatusResponse(
      201,
      request,
      siPayload,
      si.getShippingInstructionsReference()
    );
  }


  @SneakyThrows
  private ConformanceResponse _handlePutShippingInstructions(ConformanceRequest request) {
    var url = request.url();
    var documentReference = lastUrlSegment(url);
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);
    var siData = persistentMap.load(sir);
    if (siData == null || siData.isMissingNode()) {
      return return404(request);
    }
    ObjectNode updatedShippingInstructions =
      (ObjectNode) OBJECT_MAPPER.readTree(request.message().body().getJsonBody().toString());
    var si = CarrierShippingInstructions.fromPersistentStore(siData);
    si.putShippingInstructions(sir, updatedShippingInstructions);
    si.save(persistentMap);
    if (isShipperNotificationEnabled) {
      asyncCounterpartNotification(
          "/v3/shipping-instructions-notifications",
          ShippingInstructionsNotification.builder()
              .apiVersion(apiVersion)
              .shippingInstructions(si.getShippingInstructions())
              .subscriptionReference(si.getSubscriptionReference())
              .build()
              .asJsonNode());
    }
    return returnShippingInstructionsRefStatusResponse(200, request, si.getShippingInstructions(), documentReference);
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("Carrier.handleRequest(%s)".formatted(request));
    try {
      var result =
        switch (request.method()) {
          case "GET" -> {
            var url = request.url().replaceAll("/++$", "");
            var lastSegment = lastUrlSegment(url);
            var urlStem = url.substring(0, url.length() - lastSegment.length()).replaceAll("/++$", "");
            if (urlStem.endsWith("/v3/shipping-instructions")) {
              yield _handleGetShippingInstructionsRequest(request, lastSegment);
            }
            if (urlStem.endsWith("/v3/transport-documents")) {
              yield _handleGetTransportDocument(request, lastSegment);
            }
            yield return404(request);
          }
          case "POST" -> {
            var url = request.url();
            if (url.endsWith("/v3/shipping-instructions") || url.endsWith("/v3/shipping-instructions/")) {
              yield _handlePostShippingInstructions(request);
            }
            yield return404(request);
          }
          case "PATCH" -> {
            var url = request.url().replaceAll("/++$", "");
            var lastSegment = lastUrlSegment(url);
            var urlStem = url.substring(0, url.length() - lastSegment.length()).replaceAll("/++$", "");
            if (urlStem.endsWith("/v3/shipping-instructions")) {
              yield _handlePatchShippingInstructions(request, lastSegment);
            }
            if (urlStem.endsWith("/v3/transport-documents")) {
              yield _handlePatchTransportDocument(request, lastSegment);
            }
            yield return404(request);
          }
          case "PUT" -> _handlePutShippingInstructions(request);
          default -> return405(request, "GET", "POST", "PUT", "PATCH");
        };
      addOperatorLogEntry(
        "Responded to request '%s %s' with '%d'"
          .formatted(request.method(), request.url(), result.statusCode()));
      return result;
    } catch (IllegalStateException e) {
      addOperatorLogEntry(
        "Responded to request '%s %s' with '%d'"
          .formatted(request.method(), request.url(), 409));
      return return409(request, e.getMessage());
    }
  }

  private String lastUrlSegment(String url) {
    // ".../foo" and ".../foo/" should be the same
    return url.substring(1 + url.replaceAll("/++$", "").lastIndexOf("/"));
  }


  @SuperBuilder
  private abstract static class DocumentNotification {
    @Builder.Default protected String id = UUID.randomUUID().toString();
    @Builder.Default protected String source = "https://conformance.dcsa.org";

    private String type;
    private String apiVersion;

    private String shippingInstructionsReference;
    private String transportDocumentReference;
    private String shippingInstructionsStatus;
    private String updatedShippingInstructionsStatus;
    private String transportDocumentStatus;
    private String subscriptionReference;
    private String reason;
    @Builder.Default
    private boolean includeShippingInstructionsReference = true;


    private String computedType() {
      if (type != null) {
        return type;
      }
      if (apiVersion != null) {
        var majorVersion = String.valueOf(apiVersion.charAt(0));
        return typePrefix() + ".v" + majorVersion;
      }
      return null;
    }

    protected abstract JsonNode referenceDocument();

    protected abstract String typePrefix();

    protected abstract boolean isSINotification();

    public ObjectNode asJsonNode() {
      var notification = OBJECT_MAPPER.createObjectNode();
      notification.put("specversion", "1.0");
      setIfNotNull(notification, "id", id);
      setIfNotNull(notification, "source", source);
      setIfNotNull(notification, "type", computedType());
      setIfNotNull(notification, "subscriptionReference", subscriptionReference);
      notification.put("time", Instant.now().toString());
      notification.put("datacontenttype", "application/json");

      var data = OBJECT_MAPPER.createObjectNode();
      setDocumentProvidedField(data, "transportDocumentReference", transportDocumentReference);
      if (includeShippingInstructionsReference) {
        setDocumentProvidedField(
          data, "shippingInstructionsReference", shippingInstructionsReference);
      }
      if (isSINotification()) {
        setDocumentProvidedField(data, "shippingInstructionsStatus", shippingInstructionsStatus);
        setDocumentProvidedField(
            data, "updatedShippingInstructionsStatus", updatedShippingInstructionsStatus);
      } else {
        setDocumentProvidedField(data, "transportDocumentStatus", transportDocumentStatus);
      }
      setDocumentProvidedField(data, "reason", reason);
      notification.set("data", data);

      return notification;
    }

    protected void setIfNotNull(ObjectNode node, String key, String value) {
      if (value != null) {
        node.put(key, value);
      }
    }

    protected void setDocumentProvidedField(ObjectNode node, String key, String value) {
      var document = referenceDocument();
      if (value == null && document != null) {
        var v = document.get(key);
        if (v != null) {
          value = v.asText(null);
        }
      }
      setIfNotNull(node, key, value);
    }
  }

  @SuperBuilder
  private static class TransportDocumentNotification extends DocumentNotification {

    private JsonNode transportDocument;

    @Override
    protected JsonNode referenceDocument() {
      return transportDocument;
    }

    @Override
    protected String typePrefix() {
      return "org.dcsa.transport-document-notification";
    }

    @Override
    protected boolean isSINotification() {
      return false;
    }
  }

  @SuperBuilder
  private static class ShippingInstructionsNotification extends DocumentNotification {


    private JsonNode shippingInstructions;

    @Override
    protected JsonNode referenceDocument() {
      return shippingInstructions;
    }

    @Override
    protected String typePrefix() {
      return "org.dcsa.shipping-instructions-notification";
    }

    @Override
    protected boolean isSINotification() {
      return true;
    }
  }
}
