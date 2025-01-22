package org.dcsa.conformance.standards.booking.party;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
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
import org.dcsa.conformance.core.traffic.ConformanceMessageBody;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.dcsa.conformance.core.traffic.ConformanceResponse;
import org.dcsa.conformance.standards.booking.action.*;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.model.PersistableCarrierBooking;

@Slf4j
public class BookingCarrier extends ConformanceParty {
  private static final Random RANDOM = new Random();
  private static final String EXAMPLE_CARRIER_SERVICE = "Example Carrier Service";
  private static final String MESSAGE = "message";
  private static final String DKAAR = "DKAAR";
  private static final String DEBRV = "DEBRV";
  private static final String CARRIER_SERVICE = "Carrier Service %d";
  private static final String BOOKING_STATUS = "bookingStatus";
  private static final String CANCEL_AMENDMENT_OPERATION = "cancelAmendment";
  private static final String CANCEL_BOOKING_OPERATION = "cancelBooking";
  private static final String CANCEL_CONFIRMED_BOOKING_OPERATION = "cancelConfirmedBooking";
  private static final String CARRIER_BOOKING_REQUEST_REFERENCE = "carrierBookingRequestReference";
  private static final String CARRIER_BOOKING_REFERENCE = "carrierBookingReference";
  private static final String BOOKING_CANCELLATION_STATUS = "bookingCancellationStatus";

  private final Map<String, String> cbrrToCbr = new HashMap<>();
  private final Map<String, String> cbrToCbrr = new HashMap<>();


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
  protected void exportPartyJsonState(ObjectNode targetObjectNode) {
    targetObjectNode.set("cbrrToCbr", StateManagementUtil.storeMap(cbrrToCbr));
    targetObjectNode.set("cbrToCbrr", StateManagementUtil.storeMap(cbrToCbrr));
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
            UC6_Carrier_RequestToAmendConfirmedBookingAction.class,
            this::requestToAmendConfirmedBooking),
        Map.entry(UC8_Carrier_ProcessAmendmentAction.class, this::processBookingAmendment),
        Map.entry(UC10_Carrier_DeclineBookingAction.class, this::declineBooking),
        Map.entry(UC12_Carrier_ConfirmBookingCompletedAction.class, this::confirmBookingCompleted),
        Map.entry(UC14CarrierProcessBookingCancellationAction.class, this::processConfirmedBookingCancellation));
  }

  private void supplyScenarioParameters(JsonNode actionPrompt) {
    if (log.isInfoEnabled())
      log.info("Carrier.supplyScenarioParameters({})", actionPrompt.toPrettyString());
    var scenarioType = ScenarioType.valueOf(actionPrompt.required("scenarioType").asText());
    CarrierScenarioParameters carrierScenarioParameters = getCarrierScenarioParameters(scenarioType);
    asyncOrchestratorPostPartyInput(
        actionPrompt.get("actionId").asText(), carrierScenarioParameters.toJson());
    addOperatorLogEntry(
        "Provided CarrierScenarioParameters: %s".formatted(carrierScenarioParameters));
  }

  public static CarrierScenarioParameters getCarrierScenarioParameters(ScenarioType scenarioType) {
    return switch (scenarioType) {
      case REGULAR, REGULAR_SHIPPER_OWNED ->
          new CarrierScenarioParameters(
              "SCR-1234-REGULAR",
              EXAMPLE_CARRIER_SERVICE,
              "402E",
              CARRIER_SERVICE.formatted(RANDOM.nextInt(999999)),
              "640510",
              "Shoes - black, 400 boxes",
              null,
              null,
              DKAAR,
              DEBRV);
      case REGULAR_2RE1C, REGULAR_2RE2C ->
          new CarrierScenarioParameters(
              "SCR-1234-REGULAR-2REC",
              EXAMPLE_CARRIER_SERVICE,
              "402E",
              CARRIER_SERVICE.formatted(RANDOM.nextInt(999999)),
              "630260",
              "Tableware and kitchenware",
              "691010",
              "Kitchen pots and pans",
              DKAAR,
              DEBRV);
      case REGULAR_CHO_DEST ->
          new CarrierScenarioParameters(
              "SCR-1234-REGULAR-CHO-DEST",
              EXAMPLE_CARRIER_SERVICE,
              "402E",
              CARRIER_SERVICE.formatted(RANDOM.nextInt(999999)),
              "640510",
              "Shoes - black, 400 boxes",
              null,
              null,
              DKAAR,
              "USGBO");
      case REGULAR_CHO_ORIG ->
          new CarrierScenarioParameters(
              "SCR-1234-REGULAR-CHO-ORIG",
              EXAMPLE_CARRIER_SERVICE,
              "402E",
              CARRIER_SERVICE.formatted(RANDOM.nextInt(999999)),
              "640510",
              "Shoes - black, 400 boxes",
              null,
              null,
              DKAAR,
              DKAAR);
      case REGULAR_NON_OPERATING_REEFER ->
          new CarrierScenarioParameters(
              "SCR-1234-NON-OPERATING-REEFER",
              EXAMPLE_CARRIER_SERVICE,
              "402E",
              CARRIER_SERVICE.formatted(RANDOM.nextInt(999999)),
              "220291",
              "Non alcoholic beverages",
              null,
              null,
              DKAAR,
              DEBRV);
      case REEFER, REEFER_TEMP_CHANGE ->
          new CarrierScenarioParameters(
              "SCR-1234-REEFER",
              EXAMPLE_CARRIER_SERVICE,
              "402E",
              CARRIER_SERVICE.formatted(RANDOM.nextInt(999999)),
              "04052090",
              "Dairy products",
              null,
              null,
              DKAAR,
              DEBRV);
      case DG ->
          new CarrierScenarioParameters(
              "SCR-1234-DG",
              EXAMPLE_CARRIER_SERVICE,
              "403W",
              "TA1",
              "293499",
              "Environmentally hazardous substance, liquid, N.O.S (Propiconazole)",
              null,
              null,
              DKAAR,
              DEBRV);
    };
  }

  private void processBookingAmendment(JsonNode actionPrompt) {
    log.info("Carrier.processBookingAmendment(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.required("cbr").asText();
    String cbrr = actionPrompt.required("cbrr").asText();
    boolean acceptAmendment = actionPrompt.path("acceptAmendment").asBoolean(true);
    addOperatorLogEntry(
        "Confirmed the booking amendment for booking with CBR '%s' and CBRR '%s'"
            .formatted(cbr, cbrr));

    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    if (acceptAmendment) {
      persistableCarrierBooking.confirmBookingAmendment(cbr);
    } else {
      persistableCarrierBooking.declineBookingAmendment(
          cbr);
    }
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
  }

  private void processConfirmedBookingCancellation(JsonNode actionPrompt) {
    log.info("Carrier.processConfirmedBookingCancellation(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.required("cbr").asText();
    boolean isCancellationConfirmed = actionPrompt.path("isCancellationConfirmed").asBoolean(true);
    addOperatorLogEntry(
      "Cancellation of Confirmed booking with CBR '%s'"
        .formatted(cbr));

    // bookingReference can either be a CBR or CBRR.
    var cbrr = cbrToCbrr.getOrDefault(cbr, cbr);

    var persistableCarrierBooking =
      PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    if (isCancellationConfirmed) {
      persistableCarrierBooking.cancelConfirmedBooking(cbr);
    } else {
      persistableCarrierBooking.declineConfirmedBookingCancellation(
        cbr);
    }
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
  }

  private void confirmBookingRequest(JsonNode actionPrompt) {
    log.info("Carrier.confirmBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.required("cbrr").asText();
    addOperatorLogEntry("Confirmed the booking request with CBRR '%s'".formatted(cbrr));

    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    var bookingStatus = persistableCarrierBooking.getBooking().get(BOOKING_STATUS).asText();
    if (bookingStatus.equals(BookingState.CONFIRMED.name())) {
      persistableCarrierBooking
          .getBooking()
          .put("importLicenseReference", "importLicenseRefUpdate");
    }
    persistableCarrierBooking.confirmBooking(cbrr, () -> generateAndAssociateCBR(cbrr));
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
    var cbr = persistableCarrierBooking.getCarrierBookingReference();
    assert cbr != null;

    addOperatorLogEntry(
        "Confirmed the booking request with CBR '%s' and CBRR '%s'".formatted(cbr, cbrr));
  }

  private void rejectBookingRequest(JsonNode actionPrompt) {
    log.info("Carrier.rejectBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.path("cbr").asText(null);
    String cbrr = actionPrompt.required("cbrr").asText();
    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    persistableCarrierBooking.rejectBooking(
        cbrr);
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
    addOperatorLogEntry(
        "Rejected the booking request with CBR '%s' and CBRR '%s'".formatted(cbr, cbrr));
  }

  private void declineBooking(JsonNode actionPrompt) {
    log.info("Carrier.declineBooking(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.required("cbr").asText();
    String cbrr = actionPrompt.required("cbrr").asText();

    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    persistableCarrierBooking.declineBooking(
        cbr);
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);
    addOperatorLogEntry("Declined the booking with CBR '%s' and CBRR '%s'".formatted(cbr, cbrr));
  }

  private void requestUpdateToBookingRequest(JsonNode actionPrompt) {
    log.info("Carrier.requestUpdateToBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));
    Consumer<ObjectNode> bookingMutator =
        booking ->
            booking
                .putArray("feedbacks")
                .addObject()
                .put("severity", "ERROR")
                .put("code", "PROPERTY_VALUE_MUST_CHANGE")
                .put(
                  MESSAGE,
                    "Please perform the changes requested by the Conformance orchestrator");
    String cbr = actionPrompt.path("cbr").asText(null);
    String cbrr = actionPrompt.required("cbrr").asText();
    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);

    persistableCarrierBooking.requestUpdateToBooking(cbrr, bookingMutator);
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);

    addOperatorLogEntry("Requested update to the booking request with CBR '%s' and CBRR '%s'".formatted(cbr, cbrr));
  }

  private void confirmBookingCompleted(JsonNode actionPrompt) {
    log.info("Carrier.confirmBookingCompleted(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.required("cbrr").asText();
    String cbr = cbrrToCbr.get(cbrr);

    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    persistableCarrierBooking.confirmBookingCompleted(cbrr, true);
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, false);

    addOperatorLogEntry("Completed the booking request with CBR '%s' and CBRR '%s'".formatted(cbr, cbrr));
  }

  private void requestToAmendConfirmedBooking(JsonNode actionPrompt) {
    log.info("Carrier.requestToAmendConfirmedBooking(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.required("cbrr").asText();
    String cbr = actionPrompt.required("cbr").asText();

    Consumer<ObjectNode> bookingMutator =
        booking ->
            booking
                .putArray("feedbacks")
                .addObject()
                .put("severity", "ERROR")
                .put("code", "PROPERTY_VALUE_MUST_CHANGE")
                .put(
                  MESSAGE,
                    "Please perform the changes requested by the Conformance orchestrator");

    var persistableCarrierBooking =
        PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    persistableCarrierBooking.updateConfirmedBooking(cbrr, bookingMutator, true);
    persistableCarrierBooking.save(persistentMap);
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, true);

    addOperatorLogEntry("Requested to amend the booking with CBR '%s' and CBRR '%s'".formatted(cbr, cbrr));
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
    generateAndEmitNotificationFromBooking(actionPrompt, persistableCarrierBooking, includeCbrr,false);
  }

  private void generateAndEmitNotificationFromBooking(
      JsonNode actionPrompt,
      PersistableCarrierBooking persistableCarrierBooking,
      boolean includeCbrr,
      boolean includeCbr) {
    var notification =
        BookingNotification.builder()
            .apiVersion(apiVersion)
            .booking(persistableCarrierBooking.getBooking())
            .includeCarrierBookingRequestReference(includeCbrr)
            .includeCarrierBookingReference(includeCbr)
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

  private ConformanceResponse return404(ConformanceRequest request) {
    return return404(request, "Returning 404 since the request did not match any known URL");
  }

  private ConformanceResponse return404(ConformanceRequest request, String message) {
    return request.createResponse(
        404,
        Map.of(API_VERSION, List.of(apiVersion)),
        new ConformanceMessageBody(OBJECT_MAPPER.createObjectNode().put(MESSAGE, message)));
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
    var requestPayload = request.message();
    if (requestPayload == null || requestPayload.body() == null ) {
      return null;
    }
    var cancelJsonBody = requestPayload.body().getJsonBody();

    if (cancelJsonBody.get(BOOKING_STATUS) != null && cancelJsonBody.get("amendedBookingStatus") != null) {
      return "#INVALID";
    }
    if (cancelJsonBody.get(BOOKING_STATUS) != null) {
      return CANCEL_BOOKING_OPERATION;
    }
    if(cancelJsonBody.get("amendedBookingStatus") != null ) {
      return CANCEL_AMENDMENT_OPERATION;
    }
    if(cancelJsonBody.get(BOOKING_CANCELLATION_STATUS) != null ) {
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
    persistableCarrierBooking.save(persistentMap);
    var booking = persistableCarrierBooking.getBooking();

    asyncCounterpartNotification(
        null,
        "/v2/booking-notifications",
        BookingNotification.builder()
            .apiVersion(apiVersion)
            .booking(booking)
            .subscriptionReference(persistableCarrierBooking.getSubscriptionReference())
            .build()
            .asJsonNode());
    return returnEmpty202Response( request, booking, cbrr);
  }

  private ConformanceResponse _handlePatchBookingRequest(ConformanceRequest request) {
    var cancelOperation = readCancelOperation(request);
    if (!cancelOperation.equals(CANCEL_BOOKING_OPERATION)
      && !cancelOperation.equals(CANCEL_AMENDMENT_OPERATION)
      && !cancelOperation.equals(CANCEL_CONFIRMED_BOOKING_OPERATION)) {
      return return400(
          request,
          "The 'operation' query parameter must be given exactly one and have"
              + " value either 'cancelBooking' OR 'cancelAmendment' OR 'cancelConfirmedBooking'");
    }
    var bookingReference = lastUrlSegment(request.url());
    // bookingReference can either be a CBR or CBRR.
    var cbrr = cancelOperation.equals(CANCEL_BOOKING_OPERATION) ? bookingReference
      : cbrToCbrr.getOrDefault(bookingReference, bookingReference);
    var bookingData = persistentMap.load(cbrr);
    if (bookingData == null || bookingData.isMissingNode()) {
      return return404(request);
    }
    var persistableCarrierBooking = PersistableCarrierBooking.fromPersistentStore(bookingData);
    try {
      if (cancelOperation.equals(CANCEL_BOOKING_OPERATION)) {
        persistableCarrierBooking.cancelBookingRequest(bookingReference);
      } else if (cancelOperation.equals(CANCEL_AMENDMENT_OPERATION)){
        persistableCarrierBooking.cancelBookingAmendment(bookingReference);
      }
      else {
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
            .subscriptionReference(persistableCarrierBooking.getSubscriptionReference())
            .build()
            .asJsonNode());

    return returnEmpty202Response(
         request, persistableCarrierBooking.getBooking(), bookingReference);
  }

  private ConformanceResponse returnBookingCBRRResponse(ConformanceRequest request, ObjectNode booking, String bookingReference) {
    var cbrr = booking.get(CARRIER_BOOKING_REQUEST_REFERENCE).asText();
    var bookingStatus = booking.get(BOOKING_STATUS).asText();
    var statusObject =
      OBJECT_MAPPER
        .createObjectNode()
        .put(CARRIER_BOOKING_REQUEST_REFERENCE, cbrr);
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

  private ConformanceResponse returnEmpty202Response(ConformanceRequest request, ObjectNode booking, String bookingReference) {
    var bookingStatus = booking.get(BOOKING_STATUS).asText();
    ConformanceResponse response =
      request.createResponse(
        202,
        Map.of("Api-Version", List.of(apiVersion)),
        new ConformanceMessageBody(""));
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
              .formatted(
                  bookingReference,
                  persistableCarrierBooking.getOriginalBookingState()));
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


    private JsonNode booking;
    @Builder.Default private boolean includeCarrierBookingRequestReference = true;
    @Builder.Default private boolean includeCarrierBookingReference = false;

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
      notification.put("specversion", "1.0");
      setIfNotNull(notification, "id", id);
      setIfNotNull(notification, "source", source);
      setIfNotNull(notification, "type", computedType());
      notification.put("time", Instant.now().toString());
      notification.put("subscriptionReference", subscriptionReference);
      notification.put("datacontenttype", "application/json");

      var data = OBJECT_MAPPER.createObjectNode();
      setBookingProvidedField(data, CARRIER_BOOKING_REFERENCE, carrierBookingReference);
      if (includeCarrierBookingRequestReference) {
        setBookingProvidedField(
            data, CARRIER_BOOKING_REQUEST_REFERENCE, carrierBookingRequestReference);
      }
      if (includeCarrierBookingReference) {
        setBookingProvidedField(
          data, CARRIER_BOOKING_REFERENCE, carrierBookingReference);
      }
      setBookingProvidedField(data, BOOKING_STATUS, bookingStatus);
      setBookingProvidedField(data, "amendedBookingStatus", amendedBookingStatus);
      setBookingProvidedField(data, BOOKING_CANCELLATION_STATUS, bookingCancellationStatus);
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
