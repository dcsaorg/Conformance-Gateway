package org.dcsa.conformance.standards.booking.party;

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
import org.dcsa.conformance.standards.booking.action.*;
import org.dcsa.conformance.standards.booking.model.PersistableCarrierBooking;

@Slf4j
public class Carrier extends ConformanceParty {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private static final Random RANDOM = new Random();
  private final Map<String, String> cbrrToCbr = new HashMap<>();
  private final Map<String, String> cbrToCbrr = new HashMap<>();
  protected boolean isShipperNotificationEnabled = true;

  public Carrier(
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
    targetObjectNode.set("cbrrToCbr", StateManagementUtil.storeMap(OBJECT_MAPPER, cbrrToCbr));
    targetObjectNode.set("cbrToCbrr", StateManagementUtil.storeMap(OBJECT_MAPPER, cbrToCbrr));
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StateManagementUtil.restoreIntoMap(cbrrToCbr, sourceObjectNode.get("cbrrToCbr"));
    StateManagementUtil.restoreIntoMap(cbrToCbrr, sourceObjectNode.get("cbrToCbrr"));
  }

  @Override
  protected void doReset() {
    cbrrToCbr.clear();
    cbrToCbrr.clear();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(Carrier_SupplyScenarioParametersAction.class, this::supplyScenarioParameters),
        Map.entry(
            UC2_Carrier_RequestUpdateToBookingRequestAction.class,
            this::requestUpdateToBookingRequest),
        Map.entry(UC4_Carrier_RejectBookingRequestAction.class, this::rejectBookingRequest),
        Map.entry(UC5_Carrier_ConfirmBookingRequestAction.class, this::confirmBookingRequest),
        Map.entry(
            UC6_Carrier_RequestUpdateToConfirmedBookingAction.class,
            this::requestUpdateToConfirmedBooking),
        Map.entry(UC8_Carrier_ProcessAmendmentAction.class, this::processBookingAmendment),
        Map.entry(UC10_Carrier_DeclineBookingAction.class, this::declineBooking),
        Map.entry(UC12_Carrier_ConfirmBookingCompletedAction.class, this::confirmBookingCompleted));
  }

  private char computeVesselIMONumberCheckDigit(String vesselIMONumberSansCheckDigit) {
    int sum = 0;
    assert vesselIMONumberSansCheckDigit.length() == 6;
    for (int i = 0; i < 6; i++) {
      char c = vesselIMONumberSansCheckDigit.charAt(i);
      assert c >= '0' && c <= '9';
      sum += (7 - i) * Character.getNumericValue(c);
    }
    String s = String.valueOf(sum);
    return s.charAt(s.length() - 1);
  }

  private String generateSchemaValidVesselIMONumber() {
    var vesselIMONumberSansCheckDigit = "%06d".formatted(RANDOM.nextInt(999999));
    var checkDigit = computeVesselIMONumberCheckDigit(vesselIMONumberSansCheckDigit);
    return vesselIMONumberSansCheckDigit + checkDigit;
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    log.info("Carrier.supplyScenarioParameters(%s)".formatted(actionPrompt.toPrettyString()));
    List<String> validHsCodeAndCommodityType = generateValidCommodityTypeAndHSCodes();
    CarrierScenarioParameters carrierScenarioParameters =
        new CarrierScenarioParameters(
            "Carrier Service %d".formatted(RANDOM.nextInt(999999)),
            generateSchemaValidVesselIMONumber(),
          "service name",
          validHsCodeAndCommodityType.get(0),
          validHsCodeAndCommodityType.get(1),
          generateValidPolUNLocationCode(),
          generateValidPodUNLocationCode());
    asyncOrchestratorPostPartyInput(
        OBJECT_MAPPER
            .createObjectNode()
            .put("actionId", actionPrompt.get("actionId").asText())
            .set("input", carrierScenarioParameters.toJson()));
    addOperatorLogEntry(
        "Provided CarrierScenarioParameters: %s".formatted(carrierScenarioParameters));
  }


  private String generateValidPolUNLocationCode()  {
    List<String> validUnLocationCode = Arrays.asList("DEHAM", "BEANR", "NLRTM", "ESVLC", "ESALG", "SGSIN", "HKHKG");
    return validUnLocationCode.get(RANDOM.nextInt(validUnLocationCode.size()));
  }

  private String generateValidPodUNLocationCode()  {
    List<String> validUnLocationCode = Arrays.asList("DEBRV", "CNSGH", "JPTYO", "AEAUH", "AEJEA", "AEKHL", "AEKLF");
    return validUnLocationCode.get(RANDOM.nextInt(validUnLocationCode.size()));
  }

  private List<String> generateValidCommodityTypeAndHSCodes()  {
    Map<Integer, List<String>>  mapHSCodesAndCommodityType = Map.of(
      0,Arrays.asList("411510", "Leather"),
      1,Arrays.asList("843420", "Dairy machinery"),
      2,Arrays.asList("721911", "Stainless steel"),
      3,Arrays.asList("730110", "Iron or steel")
    );
    return mapHSCodesAndCommodityType.get(RANDOM.nextInt(mapHSCodesAndCommodityType.size()));
  }

  private void processBookingAmendment(JsonNode actionPrompt) {
    log.info("Carrier.processBookingAmendment(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.get("cbr").asText();
    String cbrr = actionPrompt.get("cbrr").asText();
    boolean acceptAmendment = actionPrompt.path("acceptAmendment").asBoolean(true);
    addOperatorLogEntry(
      "Confirmed the booking amendment for booking with CBR '%s'".formatted(cbr));

    var persistableCarrierBooking = PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    if (acceptAmendment) {
      persistableCarrierBooking.confirmBookingAmendment(cbr, null);
    } else {
      persistableCarrierBooking.declineBookingAmendment(cbr, "Declined as required by the conformance scenario");
    }
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
  }

  private void confirmBookingRequest(JsonNode actionPrompt) {
    log.info("Carrier.confirmBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.get("cbrr").asText();
    addOperatorLogEntry(
      "Confirmed the booking request with CBRR '%s'".formatted(cbrr));

    var persistableCarrierBooking = PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    var bookingStatus = persistableCarrierBooking.getBooking().get("bookingStatus").asText();
    if (bookingStatus.equals(BookingState.CONFIRMED.wireName())) {
      persistableCarrierBooking.getBooking().put("importLicenseReference","importLicenseRefUpdate");
    }
    persistableCarrierBooking.confirmBooking(cbrr, () -> generateAndAssociateCBR(cbrr), null);
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
    var cbr = persistableCarrierBooking.getCarrierBookingReference();
    assert cbr != null;

    addOperatorLogEntry(
        "Confirmed the booking request with CBRR '%s' with CBR '%s'".formatted(cbrr, cbr));
  }

  private void rejectBookingRequest(JsonNode actionPrompt) {
    log.info("Carrier.rejectBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.get("cbrr").asText();

    processAndEmitNotificationForStateTransition(
        actionPrompt,
        BookingState.REJECTED,
        true,
        booking -> booking.put("reason", "Rejected as required by the conformance scenario"),
      false
    );
    addOperatorLogEntry("Rejected the booking request with CBRR '%s'".formatted(cbrr));
  }

  private void declineBooking(JsonNode actionPrompt) {
    log.info("Carrier.declineBooking(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.get("cbr").asText();
    String cbrr = actionPrompt.get("cbrr").asText();
    addOperatorLogEntry(
      "Confirmed the booking request with CBR '%s'".formatted(cbr));

    var persistableCarrierBooking = PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    persistableCarrierBooking.declineBooking(cbr, "Declined as required by the conformance scenario");
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
    addOperatorLogEntry("Declined the booking with CBR '%s'".formatted(cbr));
  }

  private void requestUpdateToBookingRequest(JsonNode actionPrompt) {
    log.info("Carrier.requestUpdateToBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.get("cbrr").asText();

    processAndEmitNotificationForStateTransition(
        actionPrompt,
        BookingState.PENDING_UPDATE,
        true,
        booking ->
            booking
                .putArray("requestedChanges")
                .addObject()
                .put(
                    "message",
                    "Please perform the changes requested by the Conformance orchestrator"),
      false);
    addOperatorLogEntry("Requested update to the booking request with CBRR '%s'".formatted(cbrr));
  }

  private void confirmBookingCompleted(JsonNode actionPrompt) {
    log.info("Carrier.confirmBookingCompleted(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.get("cbrr").asText();
    String cbr = cbrrToCbr.get(cbrr);


    processAndEmitNotificationForStateTransition(
        actionPrompt,
        BookingState.COMPLETED);
    addOperatorLogEntry("Completed the booking request with CBR '%s'".formatted(cbr));
  }

  private void requestUpdateToConfirmedBooking(JsonNode actionPrompt) {
    log.info(
        "Carrier.requestUpdateToConfirmedBooking(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.get("cbr").asText();


    processAndEmitNotificationForStateTransition(
        actionPrompt,
        BookingState.PENDING_AMENDMENT,
        true,
        booking ->
            booking
                .putArray("requestedChanges")
                .addObject()
                .put(
                    "message",
                    "Please perform the changes requested by the Conformance orchestrator"),
      true);
    addOperatorLogEntry("Requested update to the booking with CBR '%s'".formatted(cbr));
  }

  private void processAndEmitNotificationForStateTransition(
      JsonNode actionPrompt,
      BookingState targetState) {
    processAndEmitNotificationForStateTransition(
        actionPrompt, targetState, false, null, false);
  }

  private String generateAndAssociateCBR(String cbrr) {
    var cbr = UUID.randomUUID().toString().replace("-", "").toUpperCase();
    assert cbrr != null;
    cbrrToCbr.put(cbrr, cbr);
    cbrToCbrr.put(cbr, cbrr);
    return cbr;
  }

  private void processAndEmitNotificationForStateTransition(
      JsonNode actionPrompt,
      BookingState targetState,
      boolean includeCbrr,
      Consumer<ObjectNode> bookingMutator,
      boolean resetAmendedBookingState) {
    String cbrr = actionPrompt.get("cbrr").asText();
    var peristableCarrierBooking = PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    peristableCarrierBooking.performSimpleStatusChange(cbrr, targetState, resetAmendedBookingState);
    var booking = peristableCarrierBooking.getBooking();
    if (bookingMutator != null) {
      bookingMutator.accept(booking);
    }
    peristableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, peristableCarrierBooking, includeCbrr);
  }

  private void generateAndEmitNotificationFromBooking(JsonNode actionPrompt, PersistableCarrierBooking persistableCarrierBooking, boolean includeCbrr) {
    var notification =
      BookingNotification.builder()
        .apiVersion(apiVersion)
        .booking(persistableCarrierBooking.getBooking())
        .includeCarrierBookingRequestReference(includeCbrr)
        .build()
        .asJsonNode();
    if (isShipperNotificationEnabled) {
      asyncCounterpartPostNotification("/v2/booking-notifications", notification);
    } else {
      asyncOrchestratorPostPartyInput(
        OBJECT_MAPPER.createObjectNode().put("actionId", actionPrompt.get("actionId").asText()));
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

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("Carrier.handleRequest(%s)".formatted(request));
    var result =
        switch (request.method()) {
          case "GET" -> _handleGetBookingRequest(request);
          case "POST" -> {
            var url = request.url();
            if (url.endsWith("/v2/bookings") || url.endsWith("/v2/bookings/")) {
              yield _handlePostBookingRequest(request);
            }
            yield return404(request);
          }
          case "PATCH" -> _handlePatchBookingRequest(request);
          case "PUT" -> _handlePutBookingRequest(request);
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

  private String readCancelOperation(ConformanceRequest request) {
    var queryParams = request.queryParams();
    var operationParams = queryParams.get("operation");
    if (operationParams == null || operationParams.isEmpty()) {
      return null;
    }
    var operation = operationParams.iterator().next();
    if (operationParams.size() > 1
        || !(operation.equals("cancelBooking") || operation.equals("cancelAmendment"))) {
      return "!INVALID-VALUE!";
    }
    return operation;
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


  @SneakyThrows
  private ConformanceResponse _handlePutBookingRequest(ConformanceRequest request) {
    // bookingReference can either be a CBR or CBRR.
    var bookingReference = lastUrlSegment(request.url());
    var cbrr = cbrToCbrr.getOrDefault(bookingReference, bookingReference);
    var bookingData = persistentMap.load(cbrr);
    if (bookingData == null || bookingData.isMissingNode()) {
      return return404(request);
    }
    ObjectNode updatedBookingRequest =
      (ObjectNode) OBJECT_MAPPER.readTree(request.message().body().getJsonBody().toString());
    var persistableCarrierBooking = PersistableCarrierBooking.fromPersistentStore(bookingData);
    try {
      persistableCarrierBooking.putBooking(bookingReference, updatedBookingRequest);
    } catch (IllegalStateException e) {
      return return409(request, "Invalid state");
    }
    // readTree(json.toString()) because we want a deep copy
    persistableCarrierBooking.save(persistentMap);
    var booking = persistableCarrierBooking.getBooking();

    if (isShipperNotificationEnabled) {
      executor.schedule(
          () ->
              asyncCounterpartPostNotification(
                  "/v2/booking-notifications",
                  BookingNotification.builder()
                      .apiVersion(apiVersion)
                      .booking(booking)
                      .build()
                      .asJsonNode()),
          100,
          TimeUnit.MILLISECONDS);
    }
    return returnBookingStatusResponse(200, request, booking, cbrr);
  }

  private ConformanceResponse _handlePatchBookingRequest(ConformanceRequest request) {
    var cancelOperation = readCancelOperation(request);
    if (cancelOperation == null) {
      return return400(request, "Missing mandatory 'operation' query parameter");
    }
    if (!cancelOperation.equals("cancelBooking") && !cancelOperation.equals("cancelAmendment")) {
      return return400(
          request,
          "The 'operation' query parameter must be given exactly one and have"
              + " value either 'cancelBooking' OR 'cancelAmendment'");
    }
    var bookingReference = lastUrlSegment(request.url());
    // bookingReference can either be a CBR or CBRR.
    var cbrr = cbrToCbrr.getOrDefault(bookingReference, bookingReference);
    var bookingData = persistentMap.load(cbrr);
    if (bookingData == null || bookingData.isMissingNode()) {
      return return404(request);
    }
    var persistableCarrierBooking = PersistableCarrierBooking.fromPersistentStore(bookingData);
    var reason = request.message().body().getJsonBody().path("reason").asText(null);
    try {
      if (cancelOperation.equals("cancelBooking")) {
        persistableCarrierBooking.cancelEntireBooking(bookingReference, reason);
      } else {
        persistableCarrierBooking.cancelBookingAmendment(bookingReference, reason);
      }
    } catch (IllegalStateException e) {
      return return409(request, "Booking was not in the correct state");
    }

    return returnBookingStatusResponse(
      200,
      request,
      persistableCarrierBooking.getBooking(),
      bookingReference
    );
  }

  private ConformanceResponse returnBookingStatusResponse(
      int responseCode, ConformanceRequest request, ObjectNode booking, String bookingReference) {
    var cbrr = booking.get("carrierBookingRequestReference").asText();
    var bookingStatus = booking.get("bookingStatus").asText();
    var statusObject =
        OBJECT_MAPPER
            .createObjectNode()
            .put("bookingStatus", bookingStatus)
            .put("carrierBookingRequestReference", cbrr);
    var cbr = booking.get("carrierBookingReference");
    var amendedBookingStatus = booking.get("amendedBookingStatus");
    var reason = booking.get("reason");
    if (cbr != null) {
      statusObject.set("carrierBookingReference", cbr);
    }
    if (amendedBookingStatus != null) {
      statusObject.set("amendedBookingStatus", amendedBookingStatus);
    }
    if (reason != null) {
      statusObject.set("reason", reason);
    }
    ConformanceResponse response =
        request.createResponse(
            responseCode,
            Map.of("Api-Version", List.of(apiVersion)),
            new ConformanceMessageBody(statusObject));
    addOperatorLogEntry(
        "Responded %d to %s booking '%s' (resulting state '%s')"
            .formatted(responseCode, request.method(), bookingReference, bookingStatus));
    return response;
  }

  private ConformanceResponse _handleGetBookingRequest(ConformanceRequest request) {
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
    var cbrr = cbrToCbrr.getOrDefault(bookingReference, bookingReference);
    var persistedBookingData = persistentMap.load(cbrr);


    if (persistedBookingData != null) {
      var persistableCarrierBooking = PersistableCarrierBooking.fromPersistentStore(persistedBookingData);
      JsonNode body;
      if (amendedContent) {
        body = persistableCarrierBooking.getAmendedBooking().orElse(null);
        if (body == null) {
          return return404(request, "No amended version of booking with reference: " + bookingReference);
        }
      } else {
        body = persistableCarrierBooking.getBooking();
      }
      ConformanceResponse response =
          request.createResponse(
              200,
            Map.of("Api-Version", List.of(apiVersion)),
            new ConformanceMessageBody(body));
      addOperatorLogEntry(
          "Responded to GET booking request '%s' (in state '%s')"
              .formatted(bookingReference, persistableCarrierBooking.getOriginalBookingState().wireName()));
      return response;
    }
    return return404(request);
  }

  @SneakyThrows
  private ConformanceResponse _handlePostBookingRequest(ConformanceRequest request) {
    ObjectNode bookingRequestPayload =
        (ObjectNode) OBJECT_MAPPER.readTree(request.message().body().getJsonBody().toString());
    var persistableCarrierBooking = PersistableCarrierBooking.initializeFromBookingRequest(bookingRequestPayload);
    persistableCarrierBooking.save(persistentMap);
    if (isShipperNotificationEnabled) {
      executor.schedule(
          () ->
              asyncCounterpartPostNotification(
                  "/v2/booking-notifications",
                  BookingNotification.builder()
                      .apiVersion(apiVersion)
                      .booking(persistableCarrierBooking.getBooking())
                      .build()
                      .asJsonNode()),
          100,
          TimeUnit.MILLISECONDS);
    }
    return returnBookingStatusResponse(
      201,
      request,
      bookingRequestPayload,
      persistableCarrierBooking.getCarrierBookingRequestReference()
    );
  }

  @Builder
  private static class BookingNotification {
    @Builder.Default private String id = UUID.randomUUID().toString();
    @Builder.Default private String source = "https://conformance.dcsa.org";
    private String type;
    private String apiVersion;

    private String carrierBookingReference;
    private String carrierBookingRequestReference;
    private String bookingStatus;
    private String amendedBookingStatus;
    private String reason;

    private JsonNode booking;
    @Builder.Default
    private boolean includeCarrierBookingRequestReference = true;

    private String computedType() {
      if (type != null) {
        return type;
      }
      if (apiVersion != null) {
        var majorVersion = String.valueOf(apiVersion.charAt(0));
        return "org.dcsa.bookingnotification.v" + majorVersion;
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
      setBookingProvidedField(data, "carrierBookingReference", carrierBookingReference);
      if (includeCarrierBookingRequestReference) {
        setBookingProvidedField(
            data, "carrierBookingRequestReference", carrierBookingRequestReference);
      }
      setBookingProvidedField(data, "bookingStatus", bookingStatus);
      setBookingProvidedField(data, "amendedBookingStatus", amendedBookingStatus);
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
      if (value == null && booking != null) {
        var v = booking.get(key);
        if (v != null) {
          value = v.asText(null);
        }
      }
      setIfNotNull(node, key, value);
    }
  }
}
