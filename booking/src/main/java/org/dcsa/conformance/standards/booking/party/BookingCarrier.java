package org.dcsa.conformance.standards.booking.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import org.dcsa.conformance.standards.booking.action.*;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.model.PersistableCarrierBooking;

@Slf4j
public class BookingCarrier extends ConformanceParty {

  private static final String MESSAGE = "message";
  private static final String BOOKING_STATUS = "bookingStatus";
  private static final String AMENDED_BOOKING_STATUS = "amendedBookingStatus";
  private static final String CANCEL_AMENDMENT_OPERATION = "cancelAmendment";
  private static final String CANCEL_BOOKING_OPERATION = "cancelBooking";
  private static final String CANCEL_CONFIRMED_BOOKING_OPERATION = "cancelConfirmedBooking";
  private static final String CARRIER_BOOKING_REQUEST_REFERENCE = "carrierBookingRequestReference";
  private static final String CARRIER_BOOKING_REFERENCE = "carrierBookingReference";
  private static final String BOOKING_CANCELLATION_STATUS = "bookingCancellationStatus";
  private static final String BOOKING = "booking";
  private static final String AMENDED_BOOKING = "amendedBooking";
  private static final String DATA = "data";
  public static final String SPECVERSION = "specversion";
  public static final String ID = "id";
  public static final String SOURCE = "source";
  public static final String TYPE = "type";
  public static final String TIME = "time";
  public static final String SUBSCRIPTION_REFERENCE = "subscriptionReference";
  public static final String DATA_CONTENT_TYPE = "datacontenttype";

  private final Map<String, String> cbrrToCbr = new HashMap<>();
  private final Map<String, String> cbrToCbrr = new HashMap<>();

  public static final Set<String> BOOKING_ENDPOINT_PATTERNS = Set.of(".*/v2/bookings(?:/[^/]+)?$");

  public BookingCarrier(
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
  public void exportPartyJsonState(ObjectNode targetObjectNode) {
    targetObjectNode.set("cbrrToCbr", StateManagementUtil.storeMap(cbrrToCbr));
    targetObjectNode.set("cbrToCbrr", StateManagementUtil.storeMap(cbrToCbrr));
  }

  @Override
  public void importPartyJsonState(ObjectNode sourceObjectNode) {
    StateManagementUtil.restoreIntoMap(cbrrToCbr, sourceObjectNode.get("cbrrToCbr"));
    StateManagementUtil.restoreIntoMap(cbrToCbrr, sourceObjectNode.get("cbrToCbrr"));
  }

  @Override
  public void doReset() {
    cbrrToCbr.clear();
    cbrToCbrr.clear();
  }

  @Override
  public Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(CarrierSupplyScenarioParametersAction.class, this::supplyScenarioParameters),
        Map.entry(
            UC2_Carrier_RequestUpdateToBookingRequestAction.class,
            this::requestUpdateToBookingRequest),
        Map.entry(UC4_Carrier_RejectBookingRequestAction.class, this::rejectBookingRequest),
        Map.entry(UC5_Carrier_ConfirmBookingRequestAction.class, this::confirmBookingRequest),
        Map.entry(
            UC6_Carrier_RequestToAmendConfirmedBookingAction.class,
            this::requestToAmendConfirmedBooking),
        Map.entry(UC8_Carrier_ProcessAmendmentAction.class, this::processBookingAmendment),
        Map.entry(UC10_Carrier_DeclineBookingAction.class, this::declineBooking),
        Map.entry(UC12_Carrier_ConfirmBookingCompletedAction.class, this::confirmBookingCompleted),
        Map.entry(
            UC14CarrierProcessBookingCancellationAction.class,
            this::processConfirmedBookingCancellation));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    if (log.isInfoEnabled())
      log.info("Carrier.supplyScenarioParameters({})", actionPrompt.toPrettyString());
    var scenarioType = ScenarioType.valueOf(actionPrompt.required("scenarioType").asText());
    ObjectNode bookingPayload = (ObjectNode) getBookingPayload(scenarioType);
    asyncOrchestratorPostPartyInput(actionPrompt.get("actionId").asText(), bookingPayload);
    addOperatorLogEntry(
        "Provided CarrierScenarioParameters: %s".formatted(bookingPayload.toString()));
  }

  private JsonNode getBookingPayload(ScenarioType scenarioType) {
    return JsonToolkit.templateFileToJsonNode(
        "/standards/booking/messages/" + scenarioType.bookingPayload(apiVersion), Map.of());
  }

  private void processBookingAmendment(JsonNode actionPrompt) {
    log.info("Carrier.processBookingAmendment(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.required("cbr").asText();
    String cbrr = actionPrompt.required("cbrr").asText();
    boolean acceptAmendment = actionPrompt.path("acceptAmendment").asBoolean(true);
    addOperatorLogEntry(
        BookingAction.createMessageForUIPrompt(
            "Confirmed the booking amendment for booking", cbr, cbrr));

    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    if (acceptAmendment) {
      persistableCarrierBooking.confirmBookingAmendment(cbr);
    } else {
      persistableCarrierBooking.declineBookingAmendment(cbr);
    }
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
  }

  private void processConfirmedBookingCancellation(JsonNode actionPrompt) {
    log.info(
        "Carrier.processConfirmedBookingCancellation(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.required("cbr").asText();
    boolean isCancellationConfirmed = actionPrompt.path("isCancellationConfirmed").asBoolean(true);
    addOperatorLogEntry(
        BookingAction.createMessageForUIPrompt("Cancellation of Confirmed booking", cbr, null));

    // bookingReference can either be a CBR or CBRR.
    var cbrr = cbrToCbrr.getOrDefault(cbr, cbr);

    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    if (isCancellationConfirmed) {
      persistableCarrierBooking.cancelConfirmedBooking(cbr);
    } else {
      persistableCarrierBooking.declineConfirmedBookingCancellation(cbr);
    }
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
  }

  private void confirmBookingRequest(JsonNode actionPrompt) {
    log.info("Carrier.confirmBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.required("cbrr").asText();
    addOperatorLogEntry(
        BookingAction.createMessageForUIPrompt("Confirmed the booking request", null, cbrr));

    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    var bookingStatus = persistableCarrierBooking.getBooking().get(BOOKING_STATUS).asText();
    if (bookingStatus.equals(BookingState.CONFIRMED.name())) {
      persistableCarrierBooking
          .getBooking()
          .put("importLicenseReference", "importLicenseRefUpdate");
    }
    persistableCarrierBooking.confirmBooking(cbrr, () -> generateAndAssociateCBR(cbrr));
    persistableCarrierBooking.resetCancellationBookingState();
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
    var cbr = persistableCarrierBooking.getCarrierBookingReference();
    assert cbr != null;

    addOperatorLogEntry(
        BookingAction.createMessageForUIPrompt("Confirmed the booking request", cbr, cbrr));
  }

  private void rejectBookingRequest(JsonNode actionPrompt) {
    log.info("Carrier.rejectBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.path("cbr").asText(null);
    String cbrr = actionPrompt.required("cbrr").asText();
    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    persistableCarrierBooking.rejectBooking(cbrr);
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
    addOperatorLogEntry(
        BookingAction.createMessageForUIPrompt("Rejected the booking request", cbr, cbrr));
  }

  private void declineBooking(JsonNode actionPrompt) {
    log.info("Carrier.declineBooking(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.required("cbr").asText();
    String cbrr = actionPrompt.required("cbrr").asText();

    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    persistableCarrierBooking.declineBooking(cbr);
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
    addOperatorLogEntry(BookingAction.createMessageForUIPrompt("Declined the booking", cbr, cbrr));
  }

  private void requestUpdateToBookingRequest(JsonNode actionPrompt) {
    log.info("Carrier.requestUpdateToBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));
    Consumer<ObjectNode> bookingMutator =
        booking ->
            booking
                .putArray(PersistableCarrierBooking.FEEDBACKS)
                .addObject()
                .put("severity", "ERROR")
                .put("code", "PROPERTY_VALUE_MUST_CHANGE")
                .put(
                    "message",
                    "Please change any one of the attributes in the request payload for conformance. For example, change VesselName to 'King of the Seas'")
                .put("jsonPath", "$.vessel.name")
                .put("property", "name");

    String cbr = actionPrompt.path("cbr").asText(null);
    String cbrr = actionPrompt.required("cbrr").asText();
    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);

    persistableCarrierBooking.requestUpdateToBooking(cbrr, bookingMutator);
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
    addOperatorLogEntry(
        BookingAction.createMessageForUIPrompt(
            "Requested update to the booking request", cbr, cbrr));
  }

  private void confirmBookingCompleted(JsonNode actionPrompt) {
    log.info("Carrier.confirmBookingCompleted(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.required("cbrr").asText();
    String cbr = cbrrToCbr.get(cbrr);

    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    persistableCarrierBooking.confirmBookingCompleted(cbrr, true, true);
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, false);

    addOperatorLogEntry(
        BookingAction.createMessageForUIPrompt("Completed the booking request", cbr, cbrr));
  }

  private void requestToAmendConfirmedBooking(JsonNode actionPrompt) {
    log.info("Carrier.requestToAmendConfirmedBooking(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.required("cbrr").asText();
    String cbr = actionPrompt.required("cbr").asText();

    Consumer<ObjectNode> bookingMutator =
        booking ->
            booking
                .putArray(PersistableCarrierBooking.FEEDBACKS)
                .addObject()
                .put("severity", "ERROR")
                .put("code", "PROPERTY_VALUE_MUST_CHANGE")
                .put(
                    "message",
                    "Please change any one of the attributes in the request payload for conformance. For example, change VesselName to 'King of the Seas'")
                .put("jsonPath", "$.vessel.name")
                .put("property", "name");

    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    persistableCarrierBooking.updateConfirmedBooking(cbrr, bookingMutator, true);
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);

    addOperatorLogEntry(
        BookingAction.createMessageForUIPrompt("Requested to amend the booking", cbr, cbrr));
  }

  private String generateAndAssociateCBR(String cbrr) {
    var cbr = UUID.randomUUID().toString().replace("-", "").toUpperCase();
    assert cbrr != null;
    cbrrToCbr.put(cbrr, cbr);
    cbrToCbrr.put(cbr, cbrr);
    return cbr;
  }

  private void generateAndEmitNotificationFromBooking(
      JsonNode actionPrompt,
      PersistableCarrierBooking persistableCarrierBooking,
      boolean includeCbrr) {
    var notification =
        BookingNotification.builder()
            .apiVersion(apiVersion)
            .booking(persistableCarrierBooking.getBooking())
            .amendedBooking(persistableCarrierBooking.getAmendedBooking().orElse(null))
            .feedbacks(
                persistableCarrierBooking.getfeedbacks() != null
                    ? persistableCarrierBooking.getfeedbacks()
                    : OBJECT_MAPPER.createArrayNode())
            .includeCarrierBookingRequestReference(includeCbrr)
            .subscriptionReference(persistableCarrierBooking.getSubscriptionReference())
            .build()
            .asJsonNode();
    asyncCounterpartNotification(
        actionPrompt.required("actionId").asText(), "/v2/booking-notifications", notification);
  }

  private ConformanceResponse return405(ConformanceRequest request, String... allowedMethods) {
    return request.createResponse(
        405,
        Map.of(
            "Api-Version", List.of(apiVersion), "Allow", List.of(String.join(",", allowedMethods))),
        new ConformanceMessageBody(
            OBJECT_MAPPER
                .createObjectNode()
                .put(MESSAGE, "Returning 405 because the method was not supported")));
  }

  private ConformanceResponse return400(ConformanceRequest request, String message) {
    return request.createResponse(
        400,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode().put(MESSAGE, message)));
  }

  public ConformanceResponse return404(ConformanceRequest request) {
    return return404(request, "Returning 404 since the request did not match any known URL");
  }

  private ConformanceResponse return404(ConformanceRequest request, String message) {
    ObjectNode response =
        (ObjectNode)
            JsonToolkit.templateFileToJsonNode(
                "/standards/booking/messages/booking-api-2.0.0-error-message.json",
                Map.of(
                    "HTTP_METHOD_PLACEHOLDER",
                    request.method(),
                    "REQUEST_URI_PLACEHOLDER",
                    request.url(),
                    "REFERENCE_PLACEHOLDER",
                    UUID.randomUUID().toString(),
                    "ERROR_DATE_TIME_PLACEHOLDER",
                    LocalDateTime.now().format(JsonToolkit.ISO_8601_DATE_TIME_FORMAT),
                    "ERROR_MESSAGE_PLACEHOLDER",
                    message));

    return request.createResponse(
        404, Map.of(API_VERSION, List.of(apiVersion)), new ConformanceMessageBody(response));
  }

  private ConformanceResponse return409(ConformanceRequest request, String message) {
    return request.createResponse(
        409,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode().put(MESSAGE, message)));
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
          case "PATCH" -> handlePatchBookingRequest(request);
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
    var requestPayload = request.message();
    if (requestPayload == null || requestPayload.body() == null) {
      return null;
    }
    var cancelJsonBody = requestPayload.body().getJsonBody();

    if (cancelJsonBody.get(BOOKING_STATUS) != null
        && cancelJsonBody.get(AMENDED_BOOKING_STATUS) != null) {
      return "#INVALID";
    }
    if (cancelJsonBody.get(BOOKING_STATUS) != null) {
      return CANCEL_BOOKING_OPERATION;
    }
    if (cancelJsonBody.get(AMENDED_BOOKING_STATUS) != null) {
      return CANCEL_AMENDMENT_OPERATION;
    }
    if (cancelJsonBody.get(BOOKING_CANCELLATION_STATUS) != null) {
      return CANCEL_CONFIRMED_BOOKING_OPERATION;
    }
    return "#INVALID";
  }

  private String readAmendedContent(ConformanceRequest request) {
    var queryParams = request.queryParams();
    var operationParams = queryParams.get("amendedContent");
    if (operationParams == null || operationParams.isEmpty()) {
      return "false";
    }
    var operation = operationParams.iterator().next();
    if (operationParams.size() > 1 || !(operation.equals("true") || operation.equals("false"))) {
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
    var amendedContent=request.queryParams().get("amendedContent");
    if (amendedContent != null && !amendedContent.isEmpty()) {
      persistableCarrierBooking.resetCancellationBookingState();
    }
    persistableCarrierBooking.save(persistentMap);
    var booking = persistableCarrierBooking.getBooking();

    asyncCounterpartNotification(
        null,
        "/v2/booking-notifications",
        BookingNotification.builder()
            .apiVersion(apiVersion)
            .booking(booking)
            .amendedBooking(persistableCarrierBooking.getAmendedBooking().orElse(null))
            .feedbacks(
                persistableCarrierBooking.getfeedbacks() != null
                    ? persistableCarrierBooking.getfeedbacks()
                    : OBJECT_MAPPER.createArrayNode())
            .subscriptionReference(persistableCarrierBooking.getSubscriptionReference())
            .build()
            .asJsonNode());
    return returnEmpty202Response(request, booking, cbrr);
  }

  private ConformanceResponse handlePatchBookingRequest(ConformanceRequest request) {
    var cancelOperation = readCancelOperation(request);
    if (!CANCEL_BOOKING_OPERATION.equals(cancelOperation)
        && !CANCEL_AMENDMENT_OPERATION.equals(cancelOperation)
        && !CANCEL_CONFIRMED_BOOKING_OPERATION.equals(cancelOperation)) {
      return return400(
          request,
          "The message body must specify what to cancel: either 'bookingStatus' OR "
              + "'amendedBookingStatus' OR 'bookingCancellationStatus' attribute should be present.");
    }
    var bookingReference = lastUrlSegment(request.url());
    // bookingReference can either be a CBR or CBRR.
    var cbrr =
        cancelOperation.equals(CANCEL_BOOKING_OPERATION)
            ? bookingReference
            : cbrToCbrr.getOrDefault(bookingReference, bookingReference);
    var bookingData = persistentMap.load(cbrr);
    if (bookingData == null || bookingData.isMissingNode()) {
      return return404(request);
    }
    var persistableCarrierBooking = PersistableCarrierBooking.fromPersistentStore(bookingData);
    try {
      if (cancelOperation.equals(CANCEL_BOOKING_OPERATION)) {
        persistableCarrierBooking.cancelBookingRequest(bookingReference);
      } else if (cancelOperation.equals(CANCEL_AMENDMENT_OPERATION)) {
        persistableCarrierBooking.cancelBookingAmendment(bookingReference);
      } else {
        persistableCarrierBooking.updateCancelConfirmedBooking(bookingReference);
      }
    } catch (IllegalStateException e) {
      return return409(request, "Booking was not in the correct state");
    }
    persistableCarrierBooking.save(persistentMap);

    asyncCounterpartNotification(
        null,
        "/v2/booking-notifications",
        BookingNotification.builder()
            .apiVersion(apiVersion)
            .booking(persistableCarrierBooking.getBooking())
            .amendedBooking(persistableCarrierBooking.getAmendedBooking().orElse(null))
            .feedbacks(
                persistableCarrierBooking.getfeedbacks() != null
                    ? persistableCarrierBooking.getfeedbacks()
                    : OBJECT_MAPPER.createArrayNode())
            .subscriptionReference(persistableCarrierBooking.getSubscriptionReference())
            .build()
            .asJsonNode());

    return returnEmpty202Response(
        request, persistableCarrierBooking.getBooking(), bookingReference);
  }

  private ConformanceResponse returnBookingCBRRResponse(
      ConformanceRequest request, ObjectNode booking, String bookingReference) {
    var cbrr = booking.get(CARRIER_BOOKING_REQUEST_REFERENCE).asText();
    var bookingStatus = booking.get(BOOKING_STATUS).asText();
    var statusObject =
        OBJECT_MAPPER.createObjectNode().put(CARRIER_BOOKING_REQUEST_REFERENCE, cbrr);
    ConformanceResponse response =
        request.createResponse(
            202,
            Map.of("Api-Version", List.of(apiVersion)),
            new ConformanceMessageBody(statusObject));
    addOperatorLogEntry(
        "Responded %d to %s booking '%s' (resulting state '%s')"
            .formatted(202, request.method(), bookingReference, bookingStatus));
    return response;
  }

  private ConformanceResponse returnEmpty202Response(
      ConformanceRequest request, ObjectNode booking, String bookingReference) {
    var bookingStatus = booking.get(BOOKING_STATUS).asText();
    ConformanceResponse response =
        request.createResponse(
            202, Map.of("Api-Version", List.of(apiVersion)), new ConformanceMessageBody(""));
    addOperatorLogEntry(
        "Responded %d to %s booking '%s' (resulting state '%s')"
            .formatted(202, request.method(), bookingReference, bookingStatus));
    return response;
  }

  private ConformanceResponse _handleGetBookingRequest(ConformanceRequest request) {
    var amendedContentRaw = readAmendedContent(request);
    boolean amendedContent;
    if (amendedContentRaw.equals("true") || amendedContentRaw.equals("false")) {
      amendedContent = amendedContentRaw.equals("true");
    } else {
      return return400(
          request,
          "The amendedContent queryParam must be used at most once and"
              + " must be one of true or false");
    }
    var bookingReference = lastUrlSegment(request.url());
    // bookingReference can either be a CBR or CBRR.
    var cbrr = cbrToCbrr.getOrDefault(bookingReference, bookingReference);
    var persistedBookingData = persistentMap.load(cbrr);

    if (persistedBookingData != null) {
      var persistableCarrierBooking =
          PersistableCarrierBooking.fromPersistentStore(persistedBookingData);
      JsonNode body;
      if (amendedContent) {
        body = persistableCarrierBooking.getAmendedBooking().orElse(null);
        if (body == null) {
          return return404(
              request, "No amended version of booking with reference: " + bookingReference);
        }
      } else {
        body = persistableCarrierBooking.getBooking();
      }
      ConformanceResponse response =
          request.createResponse(
              200, Map.of(API_VERSION, List.of(apiVersion)), new ConformanceMessageBody(body));
      addOperatorLogEntry(
          "Responded to GET booking request '%s' (in state '%s')"
              .formatted(bookingReference, persistableCarrierBooking.getOriginalBookingState()));
      return response;
    }
    return return404(request);
  }

  @SneakyThrows
  private ConformanceResponse _handlePostBookingRequest(ConformanceRequest request) {
    ObjectNode bookingRequestPayload =
        (ObjectNode) OBJECT_MAPPER.readTree(request.message().body().getJsonBody().toString());
    var persistableCarrierBooking =
        PersistableCarrierBooking.initializeFromBookingRequest(bookingRequestPayload);
    persistableCarrierBooking.save(persistentMap);

    asyncCounterpartNotification(
        null,
        "/v2/booking-notifications",
        BookingNotification.builder()
            .apiVersion(apiVersion)
            .booking(persistableCarrierBooking.getBooking())
            .amendedBooking(persistableCarrierBooking.getAmendedBooking().orElse(null))
            .subscriptionReference(persistableCarrierBooking.getSubscriptionReference())
            .build()
            .asJsonNode());

    return returnBookingCBRRResponse(
        request,
        bookingRequestPayload,
        persistableCarrierBooking.getCarrierBookingRequestReference());
  }

  @Builder
  private static class BookingNotification {
    @Builder.Default private String id = UUID.randomUUID().toString();
    @Builder.Default private String source = "https://conformance.dcsa.org";
    private String type;
    private String apiVersion;

    private String subscriptionReference;
    private String carrierBookingReference;
    private String carrierBookingRequestReference;
    private String bookingStatus;
    private String amendedBookingStatus;
    private String bookingCancellationStatus;
    private JsonNode feedbacks;
    private JsonNode booking;
    private JsonNode amendedBooking;
    @Builder.Default private boolean includeCarrierBookingRequestReference = true;

    private String computedType() {
      if (type != null) {
        return type;
      }
      if (apiVersion != null) {
        var majorVersion = String.valueOf(apiVersion.charAt(0));
        return "org.dcsa.booking.v" + majorVersion;
      }
      return null;
    }

    public ObjectNode asJsonNode() {
      var notification = OBJECT_MAPPER.createObjectNode();
      // Metadata
      notification.put(SPECVERSION, "1.0");
      putIfNotNull(notification, ID, id);
      putIfNotNull(notification, SOURCE, source);
      putIfNotNull(notification, TYPE, computedType());
      notification.put(TIME, Instant.now().toString());
      notification.put(SUBSCRIPTION_REFERENCE, subscriptionReference);
      notification.put(DATA_CONTENT_TYPE, "application/json");

      // Payload
      var data = OBJECT_MAPPER.createObjectNode();

      setBookingProvidedField(data, BOOKING_STATUS, bookingStatus);
      setBookingProvidedField(data, AMENDED_BOOKING_STATUS, amendedBookingStatus);
      setBookingProvidedField(data, BOOKING_CANCELLATION_STATUS, bookingCancellationStatus);

      setBookingProvidedField(data, CARRIER_BOOKING_REFERENCE, carrierBookingReference);
      if (includeCarrierBookingRequestReference) {
        setBookingProvidedField(
            data, CARRIER_BOOKING_REQUEST_REFERENCE, carrierBookingRequestReference);
      }
      if (feedbacks != null && !feedbacks.isEmpty()) {
        data.set(PersistableCarrierBooking.FEEDBACKS, feedbacks);
      }

      data.set(BOOKING, booking);
      if (amendedBooking != null && !amendedBooking.isEmpty()) {
        data.set(AMENDED_BOOKING, amendedBooking);
      }

      notification.set(DATA, data);
      return notification;
    }

    private void putIfNotNull(ObjectNode node, String key, String value) {
      if (value != null) {
        node.put(key, value);
      }
    }

    private void setBookingProvidedField(ObjectNode node, String key, String value) {
      if (value == null && booking != null) {
        JsonNode fallback = booking.get(key);
        if (fallback != null && !fallback.isNull()) {
          value = fallback.asText(null);
        }
      }
      putIfNotNull(node, key, value);
    }
  }
}
