package org.dcsa.conformance.standards.ebl.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.party.ConformanceParty;
import org.dcsa.conformance.core.party.CounterpartConfiguration;
import org.dcsa.conformance.core.party.PartyConfiguration;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.core.state.StateManagementUtil;
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.ebl.action.Carrier_SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.ebl.action.UC2_Carrier_RequestUpdateToShippingInstructionsAction;
import org.dcsa.conformance.standards.ebl.models.CarrierShippingInstructions;

@Slf4j
public class EblCarrier extends ConformanceParty {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final Map<String, String> tdrToSir = new HashMap<>();

  ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  protected boolean isShipperNotificationEnabled = true;

  public EblCarrier(
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
      Map.entry(UC2_Carrier_RequestUpdateToShippingInstructionsAction.class, this::requestUpdateToShippingInstructions)
    );
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info("Carrier.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));
    CarrierScenarioParameters carrierScenarioParameters =
      new CarrierScenarioParameters(
        "CARRIER_BOOKING_REFX",
        "COMMODITY_SUBREFERENCE_FOR_REFX",
        "APZU4812090",
        "DKCPH",
        "851712",
        "300 boxes of blue shoes size 47"
        );
    asyncOrchestratorPostPartyInput(
      OBJECT_MAPPER
        .createObjectNode()
        .put("actionId", actionPrompt.get("actionId").asText())
        .set("input", carrierScenarioParameters.toJson()));
    addOperatorLogEntry(
      "Provided CarrierScenarioParameters: %s".formatted(carrierScenarioParameters));
  }

  private void requestUpdateToShippingInstructions(JsonNode actionPrompt) {
    log.info("Carrier.requestUpdateToShippingInstructions(%s)".formatted(actionPrompt.toPrettyString()));

    var documentReference = actionPrompt.get("documentReference").asText();
    var sir = tdrToSir.getOrDefault(documentReference, documentReference);

    var si = CarrierShippingInstructions.fromPersistentStore(persistentMap, sir);
    si.requestChangesToShippingInstructions(documentReference, requestedChanges -> requestedChanges.addObject()
      .put("message", "Please perform the changes requested by the Conformance orchestrator")
    );
    si.save(persistentMap);
    generateAndEmitNotificationFromShippingInstructions(actionPrompt, si, true);

    addOperatorLogEntry("Requested update to the shipping instructions with document reference '%s'".formatted(documentReference));
  }

  private void generateAndEmitNotificationFromShippingInstructions(JsonNode actionPrompt, CarrierShippingInstructions shippingInstructions, boolean includeShippingInstructionsReference) {
    var notification =
      ShippingInstructionsNotification.builder()
        .apiVersion(apiVersion)
        .shippingInstructions(shippingInstructions.getShippingInstructions())
        .includeShippingInstructionsReference(includeShippingInstructionsReference)
        .build()
        .asJsonNode();
    if (isShipperNotificationEnabled) {
      asyncCounterpartPost("/v3/shipping-instructions-notifications", notification);
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

  private String readAmendedContent(ConformanceRequest request) {
    var queryParams = request.queryParams();
    var operationParams = queryParams.get("amendedContent");
    if (operationParams == null || operationParams.isEmpty()) {
      return "false";
    }
    var operation = operationParams.iterator().next();
    if (operationParams.size() > 1
      || !(operation.equals("true") || operation.equals("false"))) {
      return "!INVALID-VALUE!";
    }
    return operation;
  }

  private ConformanceResponse _handleGetShippingInstructionsRequest(ConformanceRequest request) {
    var amendedContentRaw = readAmendedContent(request);
    boolean amendedContent;
    if (amendedContentRaw.equals("true") || amendedContentRaw.equals("false")) {
      amendedContent = amendedContentRaw.equals("true");
    } else {
      return return400(request, "The amendedContent queryParam must be used at most once and" +
        " must be one of true or false");
    }
    var bookingReference = lastUrlSegment(request.url());
    // bookingReference can either be a CBR or CBRR.
    var cbrr = tdrToSir.getOrDefault(bookingReference, bookingReference);
    var persistedBookingData = persistentMap.load(cbrr);


    if (persistedBookingData != null) {
      var persistableCarrierBooking = CarrierShippingInstructions.fromPersistentStore(persistedBookingData);
      JsonNode body;
      if (amendedContent) {
        body = persistableCarrierBooking.getUpdatedShippingInstructions().orElse(null);
        if (body == null) {
          return return404(request, "No amended version of booking with reference: " + bookingReference);
        }
      } else {
        body = persistableCarrierBooking.getShippingInstructions();
      }
      ConformanceResponse response =
        request.createResponse(
          200,
          Map.of("Api-Version", List.of(apiVersion)),
          new ConformanceMessageBody(body));
      addOperatorLogEntry(
        "Responded to GET booking request '%s' (in state '%s')"
          .formatted(bookingReference, persistableCarrierBooking.getShippingInstructionsState().wireName()));
      return response;
    }
    return return404(request);
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

  @SneakyThrows
  private ConformanceResponse _handlePostShippingInstructions(ConformanceRequest request) {
    ObjectNode siPayload =
      (ObjectNode) OBJECT_MAPPER.readTree(request.message().body().getJsonBody().toString());
    var si = CarrierShippingInstructions.initializeFromShippingInstructionsRequest(siPayload);
    si.save(persistentMap);
    if (isShipperNotificationEnabled) {
      executor.schedule(
        () ->
          asyncCounterpartPost(
            "/v3/shipping-instructions-notifications",
            ShippingInstructionsNotification.builder()
              .apiVersion(apiVersion)
              .shippingInstructions(si.getShippingInstructions())
              .build()
              .asJsonNode()),
        1,
        TimeUnit.SECONDS);
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
    if (isShipperNotificationEnabled) {
      executor.schedule(
        () ->
          asyncCounterpartPost(
            "/v3/shipping-instructions-notifications",
            ShippingInstructionsNotification.builder()
              .apiVersion(apiVersion)
              .shippingInstructions(si.getShippingInstructions())
              .build()
              .asJsonNode()),
        1,
        TimeUnit.SECONDS);
    }
    return returnShippingInstructionsRefStatusResponse(200, request, si.getShippingInstructions(), documentReference);
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("Carrier.handleRequest(%s)".formatted(request));
    var result =
      switch (request.method()) {
        case "GET" -> _handleGetShippingInstructionsRequest(request);
        case "POST" -> {
          var url = request.url();
          if (url.endsWith("/v3/shipping-instructions") || url.endsWith("/v3/shipping-instructions/")) {
            yield _handlePostShippingInstructions(request);
          }
          yield return404(request);
        }
        // case "PATCH" -> _handlePatchRequest(request);
        case "PUT" -> _handlePutShippingInstructions(request);
        default -> return405(request, "GET", "POST", "PUT", "PATCH");
      };
    addOperatorLogEntry(
      "Responded to request '%s %s' with '%d'"
        .formatted(request.method(), request.url(), result.statusCode()));
    return result;
  }

  private String lastUrlSegment(String url) {
    // ".../foo" and ".../foo/" should be the same
    return url.substring(1 + url.replaceAll("/++$", "").lastIndexOf("/"));
  }


  @Builder
  private static class ShippingInstructionsNotification {
    @Builder.Default private String id = UUID.randomUUID().toString();
    @Builder.Default private String source = "https://conformance.dcsa.org";
    private String type;
    private String apiVersion;

    private String shippingInstructionsReference;
    private String transportDocumentReference;
    private String shippingInstructionsStatus;
    private String updatedShippingInstructionsStatus;
    private String reason;

    private JsonNode shippingInstructions;
    @Builder.Default
    private boolean includeShippingInstructionsReference = true;

    private String computedType() {
      if (type != null) {
        return type;
      }
      if (apiVersion != null) {
        var majorVersion = String.valueOf(apiVersion.charAt(0));
        return "org.dcsa.shipping-instructions-notification.v" + majorVersion;
      }
      return null;
    }

    public ObjectNode asJsonNode() {
      var notification = OBJECT_MAPPER.createObjectNode();
      notification.put("specversion", "1.0");
      setIfNotNull(notification, "id", id);
      setIfNotNull(notification, "source", source);
      setIfNotNull(notification, "type", computedType());
      notification.put("time", Instant.now().toString());
      notification.put("datacontenttype", "application/json");

      var data = OBJECT_MAPPER.createObjectNode();
      setBookingProvidedField(data, "transportDocumentReference", transportDocumentReference);
      if (includeShippingInstructionsReference) {
        setBookingProvidedField(
          data, "shippingInstructionsReference", shippingInstructionsReference);
      }
      setBookingProvidedField(data, "shippingInstructionsStatus", shippingInstructionsStatus);
      setBookingProvidedField(data, "updatedShippingInstructionsStatus", updatedShippingInstructionsStatus);
      setBookingProvidedField(data, "reason", reason);
      notification.set("data", data);

      return notification;
    }

    private void setIfNotNull(ObjectNode node, String key, String value) {
      if (value != null) {
        node.put(key, value);
      }
    }

    private void setBookingProvidedField(ObjectNode node, String key, String value) {
      if (value == null && shippingInstructions != null) {
        var v = shippingInstructions.get(key);
        if (v != null) {
          value = v.asText(null);
        }
      }
      setIfNotNull(node, key, value);
    }
  }
}
