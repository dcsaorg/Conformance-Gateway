package org.dcsa.conformance.standards.booking.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
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
        Map.entry(UC10_Carrier_DeclineBookingAction.class, this::declineBooking),
        Map.entry(UC11_Carrier_ConfirmBookingCompletedAction.class, this::confirmBookingCompleted));
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
    CarrierScenarioParameters carrierScenarioParameters =
        new CarrierScenarioParameters(
            "Carrier Service %d".formatted(RANDOM.nextInt(999999)),
            generateSchemaValidVesselIMONumber());
    asyncOrchestratorPostPartyInput(
        OBJECT_MAPPER
            .createObjectNode()
            .put("actionId", actionPrompt.get("actionId").asText())
            .set("input", carrierScenarioParameters.toJson()));
    addOperatorLogEntry(
        "Provided CarrierScenarioParameters: %s".formatted(carrierScenarioParameters));
  }

  private void confirmBookingRequest(JsonNode actionPrompt) {
    log.info("Carrier.confirmBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.get("cbrr").asText();
    processAndEmitNotificationForStateTransition(
        actionPrompt,
        BookingState.CONFIRMED,
        true,
        booking -> {
          ensureBookingHasCBR(booking);
          var clauses = booking.putArray("carrierClauses");
          booking.put("termsAndConditions", termsAndConditions());
          for (var clause : carrierClauses()) {
            clauses.add(clause);
          }
          replaceConfirmedEquipments(booking);
          addCharge(booking);
          generateTransportPlan(booking);
          replaceShipmentCutOffTimes(booking);
        });
    // processAndEmitNotificationForStateTransition will insert a CBR for the cbrr if needed,
    // so this lookup has to happen after.
    String cbr = cbrrToCbr.get(cbrr);

    addOperatorLogEntry(
        "Confirmed the booking request with CBRR '%s' with CBR '%s'".formatted(cbrr, cbr));
  }

  private String extractUnLocationCode(JsonNode locationNode) {
    if (locationNode != null) {
      var loc = locationNode.path("location");
      var locType = loc.path("locationType").asText("");
      var unloc = loc.path("UNLocationCode").asText("");
      if ((locType.equals("UNLO") || locType.equals("FACI")) && !unloc.isBlank()) {
        return unloc;
      }
    }
    return null;
  }

  private void generateTransportPlan(ObjectNode booking) {
    // Default values if we cannot "guessimate" a better default.
    LocalDate departureDate = LocalDate.now().plusMonths(1);
    LocalDate arrivalDate = departureDate.plusWeeks(2);
    var loadLocation = "NLRTM";
    var dischargeLocation = "DKCPH";
    if (booking.get("shipmentLocations") instanceof ArrayNode shipmentLocations
        && !shipmentLocations.isEmpty()) {
      var polNode =
          StreamSupport.stream(shipmentLocations.spliterator(), false)
              .filter(o -> o.path("locationTypeCode").asText("").equals("POL"))
              .findFirst()
              .orElse(null);
      var podNode =
          StreamSupport.stream(shipmentLocations.spliterator(), false)
              .filter(o -> o.path("locationTypeCode").asText("").equals("POD"))
              .findFirst()
              .orElse(null);

      loadLocation = Objects.requireNonNullElse(extractUnLocationCode(polNode), loadLocation);
      dischargeLocation = Objects.requireNonNullElse(extractUnLocationCode(podNode), loadLocation);
    }

    /*
     * TODO: At some point there should be:
     *
     *  * Vessel information
     *  * Pre carriage steps
     *  * Onward carriage steps
     */
    new TransportPlanBuilder(booking)
        .addTransportLeg()
        .transportPlanStage("MNC")
        .loadLocation()
        .unlocation(loadLocation)
        .dischargeLocation()
        .unlocation(dischargeLocation)
        .plannedDepartureDate(departureDate.toString())
        .plannedArrivalDate(arrivalDate.toString());
  }

  private void replaceShipmentCutOffTimes(ObjectNode booking) {
    var shipmentCutOffTimes = booking.putArray("shipmentCutOffTimes");
    var firstTransportActionByCarrier = OffsetDateTime.now().plusMonths(1);
    if (booking.get("transportPlan") instanceof ArrayNode transportPlan
        && !transportPlan.isEmpty()) {
      var plannedDepartureDateNode = transportPlan.path(0).path("plannedDepartureDate");
      if (plannedDepartureDateNode.isTextual()) {
        try {
          var plannedDepartureDate = LocalDate.parse(plannedDepartureDateNode.asText());
          firstTransportActionByCarrier =
              plannedDepartureDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        } catch (IllegalArgumentException ignored) {
          // We have a fallback already.
        }
      }
    }

    var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    var oneWeekPrior = formatter.format(firstTransportActionByCarrier.minusWeeks(1));
    var twoWeeksPrior = formatter.format(firstTransportActionByCarrier.minusWeeks(2));

    addShipmentCutOff(shipmentCutOffTimes, "DCO", oneWeekPrior);
    addShipmentCutOff(shipmentCutOffTimes, "VCO", oneWeekPrior);
    addShipmentCutOff(shipmentCutOffTimes, "FCO", oneWeekPrior);
    addShipmentCutOff(shipmentCutOffTimes, "LCO", oneWeekPrior);
    addShipmentCutOff(shipmentCutOffTimes, "EFC", oneWeekPrior);

    // It would be impossible if ECP was the same time as the others, so we give another
    // week for that one.
    addShipmentCutOff(shipmentCutOffTimes, "ECP", twoWeeksPrior);
  }

  private void addShipmentCutOff(
      ArrayNode shipmentCutOffTimes, String cutOffDateTimeCode, String cutOffDateTime) {
    shipmentCutOffTimes
        .addObject()
        .put("cutOffDateTimeCode", cutOffDateTimeCode)
        .put("cutOffDateTime", cutOffDateTime);
  }

  private void addCharge(ObjectNode booking) {
    ArrayNode charges;
    if (booking.get("charges") instanceof ArrayNode chargeNode) {
      charges = chargeNode;
    } else {
      charges = booking.putArray("charges");
    }
    if (!charges.isEmpty()) {
      charges
          .addObject()
          .put("chargeName", "Fictive booking fee")
          .put("currencyAmount", 1f)
          .put("currencyCode", "EUR")
          .put("paymentTermCode", "PRE")
          .put("calculationBasis", "For the entire booking")
          .put("unitPrice", 1f)
          .put("quantity", 1);
    } else {
      charges
          .addObject()
          .put("chargeName", "Fictive admentment fee")
          .put("currencyAmount", 1f)
          .put("currencyCode", "EUR")
          .put("paymentTermCode", "COL")
          .put("calculationBasis", "For the concrete amendment")
          .put("unitPrice", 1f)
          .put("quantity", 1);
    }
  }

  private void replaceConfirmedEquipments(ObjectNode booking) {
    if (booking.get("requestedEquipments") instanceof ArrayNode requestedEquipments
        && !requestedEquipments.isEmpty()) {
      var confirmedEquipments = booking.putArray("confirmedEquipments");
      for (var requestedEquipment : requestedEquipments) {
        var equipmentCodeNode = requestedEquipment.get("ISOEquipmentCode");
        var unitsNode = requestedEquipment.get("units");
        var equipmentCode = "22GP";
        var units = 1L;
        if (equipmentCodeNode.isTextual()) {
          equipmentCode = equipmentCodeNode.asText();
        }
        if (unitsNode.canConvertToLong()) {
          units = Math.min(unitsNode.longValue(), 1L);
        }
        confirmedEquipments.addObject().put("ISOEquipmentCode", equipmentCode).put("units", units);
      }
    } else {
      // It is required even if we got nothing to go on.
      booking
          .putArray("confirmedEquipments")
          .addObject()
          .put("ISOEquipmentCode", "22GP")
          .put("units", 1);
    }
  }

  private void rejectBookingRequest(JsonNode actionPrompt) {
    log.info("Carrier.rejectBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.get("cbrr").asText();

    processAndEmitNotificationForStateTransition(
        actionPrompt,
        BookingState.REJECTED,
        true,
        booking -> booking.put("reason", "Rejected as required by the conformance scenario")
    );
    addOperatorLogEntry("Rejected the booking request with CBRR '%s'".formatted(cbrr));
  }

  private void declineBooking(JsonNode actionPrompt) {
    log.info("Carrier.declineBooking(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.get("cbr").asText();

    processAndEmitNotificationForStateTransition(
      actionPrompt,
      BookingState.DECLINED,
      false,
      booking -> booking.put("reason", "Declined as required by the conformance scenario")
    );
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
                    "Please perform the changes requested by the Conformance orchestrator"));
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
                    "Please perform the changes requested by the Conformance orchestrator"));
    addOperatorLogEntry("Requested update to the booking with CBR '%s'".formatted(cbr));
  }

  private void checkState(
      String reference, BookingState currentState, Predicate<BookingState> expectedState) {
    if (!expectedState.test(currentState)) {
      throw new IllegalStateException(
          "Booking '%s' is in state '%s'".formatted(reference, currentState));
    }
  }

  private void processAndEmitNotificationForStateTransition(
      JsonNode actionPrompt,
      BookingState targetState) {
    processAndEmitNotificationForStateTransition(
        actionPrompt, targetState, false, null);
  }

  private void ensureBookingHasCBR(ObjectNode booking) {
    if (!booking.has("carrierBookingReference")) {
      var cbrr = booking.required("carrierBookingRequestReference").asText();
      var cbr = UUID.randomUUID().toString().replace("-", "").toUpperCase();
      assert cbrr != null;
      cbrrToCbr.put(cbrr, cbr);
      cbrToCbrr.put(cbr, cbrr);
      booking.put("carrierBookingReference", cbr);
    }
  }

  private void processAndEmitNotificationForStateTransition(
      JsonNode actionPrompt,
      BookingState targetState,
      boolean includeCbrr,
      Consumer<ObjectNode> bookingMutator) {
    String cbrr = actionPrompt.get("cbrr").asText();
    var peristableCarrierBooking = PersistableCarrierBooking.fromPersistentStore(persistentMap, cbrr);
    peristableCarrierBooking.performSimpleStatusChange(cbrr, targetState);
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
      asyncCounterpartPost("/v2/booking-notifications", notification);
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
              asyncCounterpartPost(
                  "/v2/booking-notifications",
                  BookingNotification.builder()
                      .apiVersion(apiVersion)
                      .booking(booking)
                      .build()
                      .asJsonNode()),
          1,
          TimeUnit.SECONDS);
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
              .formatted(bookingReference, persistableCarrierBooking.getBookingState().wireName()));
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
              asyncCounterpartPost(
                  "/v2/booking-notifications",
                  BookingNotification.builder()
                      .apiVersion(apiVersion)
                      .booking(persistableCarrierBooking.getBooking())
                      .build()
                      .asJsonNode()),
          1,
          TimeUnit.SECONDS);
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

  private record LocationBuilder<T>(ObjectNode location, Function<ObjectNode, T> onCompletion) {

    // TODO: Add Address here at some point

    public T unlocation(String unlocationCode) {
      location.put("locationType", "UNLO").put("UNLocationCode", unlocationCode);
      return endLocation();
    }

    public T facility(String unlocationCode, String facilityCode, String facilityCodeListProvider) {
      location
          .put("locationType", "FACI")
          .put("UNLocationCode", unlocationCode)
          .put("facilityCode", facilityCode)
          .put("facilityCodeListProvider", facilityCodeListProvider);
      return endLocation();
    }

    private T endLocation() {
      return this.onCompletion.apply(location);
    }
  }

  private record TransportPlanStepBuilder(
      TransportPlanBuilder parentBuilder, ObjectNode transportPlanStep) {
    public LocationBuilder<TransportPlanStepBuilder> loadLocation() {
      return new LocationBuilder<>(transportPlanStep.putObject("loadLocation"), (ignored -> this));
    }

    public LocationBuilder<TransportPlanStepBuilder> dischargeLocation() {
      return new LocationBuilder<>(
          transportPlanStep.putObject("dischargeLocation"), (ignored -> this));
    }

    public TransportPlanStepBuilder plannedArrivalDate(String plannedArrivalDate) {
      return setStringField("plannedArrivalDate", plannedArrivalDate);
    }

    public TransportPlanStepBuilder plannedDepartureDate(String plannedDepartureDate) {
      return setStringField("plannedDepartureDate", plannedDepartureDate);
    }

    public TransportPlanStepBuilder transportPlanStage(String transportPlanStage) {
      return setStringField("transportPlanStage", transportPlanStage);
    }

    public TransportPlanStepBuilder modeOfTransport(String modeOfTransport) {
      return setStringField("modeOfTransport", modeOfTransport);
    }

    public TransportPlanStepBuilder vesselName(String vesselName) {
      return setStringField("vesselName", vesselName);
    }

    public TransportPlanStepBuilder vesselIMONumber(String vesselIMONumber) {
      return setStringField("vesselIMONumber", vesselIMONumber);
    }

    private TransportPlanStepBuilder setStringField(String fieldName, String value) {
      this.transportPlanStep.put(fieldName, value);
      return this;
    }

    public TransportPlanStepBuilder nextTransportLeg() {
      return parentBuilder.addTransportLeg();
    }

    public JsonNode buildTransportPlan() {
      return parentBuilder.build();
    }
  }

  private static class TransportPlanBuilder {

    private final ArrayNode transportPlan;
    private int sequenceNumber = 1;

    TransportPlanBuilder(ObjectNode booking) {
      this.transportPlan = booking.putArray("transportPlan");
    }

    private TransportPlanStepBuilder addTransportLeg() {
      var step =
          transportPlan
              .addObject()
              // Yes, this is basically the array index (+1), but it is required, so here goes.
              .put("transportPlanStageSequenceNumber", sequenceNumber++);
      return new TransportPlanStepBuilder(this, step);
    }

    public JsonNode build() {
      // just to match the pattern really
      return transportPlan;
    }
  }

  private static List<String> carrierClauses() {
    return List.of(
        "Per terms and conditions (see the termsAndConditions field), this is not a real booking.",
        "A real booking would probably have more legal text here.");
  }

  private static String termsAndConditions() {
    return """
            You agree that this booking exist is name only for the sake of
            testing your conformance with the DCSA BKG API. This booking is NOT backed
            by a real booking with ANY carrier and NONE of the requested services will be
            carried out in real life.

            Unless required by applicable law or agreed to in writing, DCSA provides
            this JSON data on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
            ANY KIND, either express or implied, including, without limitation, any
            warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY,
            or FITNESS FOR A PARTICULAR PURPOSE. You are solely responsible for
            determining the appropriateness of using or redistributing this JSON
            data and assume any risks associated with Your usage of this data.

            In no event and under no legal theory, whether in tort (including negligence),
            contract, or otherwise, unless required by applicable law (such as deliberate
            and grossly negligent acts) or agreed to in writing, shall DCSA be liable to
            You for damages, including any direct, indirect, special, incidental, or
            consequential damages of any character arising as a result of this terms or conditions
            or out of the use or inability to use the provided JSON data (including but not limited
            to damages for loss of goodwill, work stoppage, computer failure or malfunction, or any
            and all other commercial damages or losses), even if DCSA has been advised of the
            possibility of such damages.
            """;
  }
}
