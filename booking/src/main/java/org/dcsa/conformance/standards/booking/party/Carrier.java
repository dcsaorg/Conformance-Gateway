package org.dcsa.conformance.standards.booking.party;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

@Slf4j
public class Carrier extends ConformanceParty {
  private static final Random RANDOM = new Random();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<String, BookingState> bookingStatesByCbrr = new HashMap<>();
  private final Map<String, String> cbrrToCbr = new HashMap<>();
  private final Map<String, String> cbrToCbrr = new HashMap<>();
  protected boolean isShipperNotificationEnabled = false;

  public Carrier(
      String apiVersion,
      PartyConfiguration partyConfiguration,
      CounterpartConfiguration counterpartConfiguration,
      JsonNodeMap persistentMap,
      Consumer<ConformanceRequest> asyncWebClient,
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
    targetObjectNode.set(
        "bookingStatesByCbrr",
        StateManagementUtil.storeMap(objectMapper, bookingStatesByCbrr, BookingState::name));
    targetObjectNode.set("cbrrToCbr", StateManagementUtil.storeMap(objectMapper, cbrrToCbr));
    targetObjectNode.set("cbrToCbrr", StateManagementUtil.storeMap(objectMapper, cbrToCbrr));
  }

  @Override
  protected void importPartyJsonState(ObjectNode sourceObjectNode) {
    StateManagementUtil.restoreIntoMap(
        bookingStatesByCbrr, sourceObjectNode.get("bookingStatesByCbrr"), BookingState::valueOf);
    StateManagementUtil.restoreIntoMap(cbrrToCbr, sourceObjectNode.get("cbrrToCbr"));
    StateManagementUtil.restoreIntoMap(cbrToCbrr, sourceObjectNode.get("cbrToCbrr"));
  }

  @Override
  protected void doReset() {
    bookingStatesByCbrr.clear();
    cbrrToCbr.clear();
    cbrToCbrr.clear();
  }

  @Override
  protected Map<Class<? extends ConformanceAction>, Consumer<JsonNode>> getActionPromptHandlers() {
    return Map.ofEntries(
        Map.entry(Carrier_SupplyScenarioParametersAction.class, this::supplyScenarioParameters),
        Map.entry(UC2_Carrier_RequestUpdateToBookingRequestAction.class, this::requestUpdateToBookingRequest),
        Map.entry(UC4_Carrier_RejectBookingRequestAction.class, this::rejectBookingRequest),
        Map.entry(UC5_Carrier_ConfirmBookingRequestAction.class, this::confirmBookingRequest),
        Map.entry(UC6_Carrier_RequestUpdateToConfirmedBookingAction.class, this::requestUpdateToConfirmedBooking),
        Map.entry(UC10_Carrier_RejectBookingAction.class, this::declineBooking),
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
        objectMapper
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
        Set.of(BookingState.RECEIVED, BookingState.PENDING_UPDATE_CONFIRMATION),
        ReferenceState.GENERATE_IF_MISSING,
        true,
        booking -> {
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
    if (booking.get("shipmentLocations") instanceof ArrayNode shipmentLocations && !shipmentLocations.isEmpty()) {
      var polNode = StreamSupport.stream(shipmentLocations.spliterator(), false)
        .filter(o -> o.path("locationTypeCode").asText("").equals("POL"))
        .findFirst()
        .orElse(null);
      var podNode = StreamSupport.stream(shipmentLocations.spliterator(), false)
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
    if (booking.get("transportPlan") instanceof ArrayNode transportPlan && !transportPlan.isEmpty()) {
      var plannedDepartureDateNode = transportPlan.path(0).path("plannedDepartureDate");
      if (plannedDepartureDateNode.isTextual()) {
        try {
          var plannedDepartureDate = LocalDate.parse(plannedDepartureDateNode.asText());
          firstTransportActionByCarrier = plannedDepartureDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        } catch (IllegalArgumentException ignored) {
          // We have a fallback already.
        }
      }
    }

    var oneWeekPrior = firstTransportActionByCarrier.minusWeeks(1).toString();
    var twoWeeksPrior = firstTransportActionByCarrier.minusWeeks(2).toString();

    addShipmentCutOff(shipmentCutOffTimes, "DCO", oneWeekPrior);
    addShipmentCutOff(shipmentCutOffTimes, "VCO", oneWeekPrior);
    addShipmentCutOff(shipmentCutOffTimes, "FCO", oneWeekPrior);
    addShipmentCutOff(shipmentCutOffTimes, "LCO", oneWeekPrior);
    addShipmentCutOff(shipmentCutOffTimes, "EFC", oneWeekPrior);

    // It would be impossible if ECP was the same time as the others, so we give another
    // week for that one.
    addShipmentCutOff(shipmentCutOffTimes, "ECP", twoWeeksPrior);
  }

  private void addShipmentCutOff(ArrayNode shipmentCutOffTimes, String cutOffDateTimeCode, String cutOffDateTime) {
    shipmentCutOffTimes.addObject()
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
      charges.addObject()
        .put("chargeName", "Fictive booking fee")
        .put("currencyAmount", 1f)
        .put("currencyCode", "EUR")
        .put("paymentTermCode", "PRE")
        .put("calculationBasis", "For the entire booking")
        .put("unitPrice", 1f)
        .put("quantity", 1);
    } else {
      charges.addObject()
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
    if (booking.get("requestedEquipments") instanceof ArrayNode requestedEquipments && !requestedEquipments.isEmpty()) {
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
        confirmedEquipments.addObject()
          .put("ISOEquipmentCode", equipmentCode)
          .put("units", units);
      }
    } else {
      // It is required even if we got nothing to go on.
      booking.putArray("confirmedEquipments")
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
        Set.of(
            BookingState.RECEIVED,
            BookingState.PENDING_UPDATE,
            BookingState.PENDING_UPDATE_CONFIRMATION),
        ReferenceState.PROVIDE_IF_EXIST,
        true);
    addOperatorLogEntry("Rejected the booking request with CBRR '%s'".formatted(cbrr));
  }

  private void declineBooking(JsonNode actionPrompt) {
    log.info("Carrier.declineBooking(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.get("cbr").asText();

    processAndEmitNotificationForStateTransition(
      actionPrompt,
      BookingState.DECLINED,
      Set.of(
        BookingState.CONFIRMED,
        BookingState.PENDING_AMENDMENT,
        BookingState.PENDING_AMENDMENT_APPROVAL),
      ReferenceState.PROVIDE_IF_EXIST,
      false);
    addOperatorLogEntry("Declined the booking with CBR '%s'".formatted(cbr));
  }

  private void requestUpdateToBookingRequest(JsonNode actionPrompt) {
    log.info("Carrier.requestUpdateToBookingRequest(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.get("cbrr").asText();

    processAndEmitNotificationForStateTransition(
      actionPrompt,
      BookingState.PENDING_UPDATE,
      Set.of(BookingState.RECEIVED, BookingState.PENDING_UPDATE, BookingState.PENDING_UPDATE_CONFIRMATION),
      ReferenceState.PROVIDE_IF_EXIST,
      true,
      booking -> booking.putArray("requestedChanges")
        .addObject()
        .put("message", "Please perform the changes requested by the Conformance orchestrator"));
    addOperatorLogEntry("Requested update to the booking request with CBRR '%s'".formatted(cbrr));
  }

  private void confirmBookingCompleted(JsonNode actionPrompt) {
    log.info("Carrier.confirmBookingCompleted(%s)".formatted(actionPrompt.toPrettyString()));

    String cbrr = actionPrompt.get("cbrr").asText();
    String cbr = cbrrToCbr.get(cbrr);

    processAndEmitNotificationForStateTransition(
        actionPrompt,
        BookingState.COMPLETED,
        Set.of(BookingState.CONFIRMED),
        ReferenceState.MUST_EXIST,
        false);
    addOperatorLogEntry("Completed the booking request with CBR '%s'".formatted(cbr));
  }

  private void requestUpdateToConfirmedBooking(JsonNode actionPrompt) {
    log.info("Carrier.requestUpdateToConfirmedBooking(%s)".formatted(actionPrompt.toPrettyString()));

    String cbr = actionPrompt.get("cbr").asText();

    processAndEmitNotificationForStateTransition(
      actionPrompt,
      BookingState.PENDING_AMENDMENT,
      Set.of(BookingState.CONFIRMED, BookingState.PENDING_AMENDMENT),
      ReferenceState.PROVIDE_IF_EXIST,
      true,
      booking -> booking.putArray("requestedChanges")
        .addObject()
        .put("message", "Please perform the changes requested by the Conformance orchestrator"));
    addOperatorLogEntry("Requested update to the booking with CBR '%s'".formatted(cbr));
  }

  private void processAndEmitNotificationForStateTransition(
    JsonNode actionPrompt,
    BookingState targetState,
    Set<BookingState> expectedState,
    ReferenceState cbrHandling,
    boolean includeCbrr) {
    processAndEmitNotificationForStateTransition(
      actionPrompt,
      targetState,
      expectedState,
      cbrHandling,
      includeCbrr,
      null
    );
  }

  private void processAndEmitNotificationForStateTransition(
      JsonNode actionPrompt,
      BookingState targetState,
      Set<BookingState> expectedState,
      ReferenceState cbrHandling,
      boolean includeCbrr,
      Consumer<ObjectNode> bookingMutator) {
    String cbrr = actionPrompt.get("cbrr").asText();
    String cbr = cbrrToCbr.get(cbrr);
    BookingState currentState = bookingStatesByCbrr.get(cbrr);
    boolean isCorrect = actionPrompt.path("isCorrect").asBoolean(true);
    if (!expectedState.contains(currentState)) {
      throw new IllegalStateException(
          "Booking '%s' is in state '%s'".formatted(cbrr, currentState));
    }

    if (isCorrect) {
      var booking = (ObjectNode)persistentMap.load(cbrr);
      boolean generatedCBR = false;
      bookingStatesByCbrr.put(cbrr, targetState);
      switch (cbrHandling) {
        case MUST_EXIST -> {
          if (cbr == null) {
            throw new IllegalStateException(
                "Booking '%s' did not have a carrier booking reference and must have one"
                    .formatted(cbrr));
          }
        }
        case GENERATE_IF_MISSING -> {
          if (cbr == null) {
            cbr = UUID.randomUUID().toString().replace("-", "").toUpperCase();
            cbrrToCbr.put(cbrr, cbr);
            cbrToCbrr.put(cbr, cbrr);
            generatedCBR = true;
          }
        }
        case PROVIDE_IF_EXIST -> {
          /* Do nothing */
        }
      }
      if (!includeCbrr && cbr == null) {
        throw new IllegalArgumentException(
            "If includeCbrr is false, then cbrHandling must ensure"
                + " that a carrierBookingReference is provided");
      }

      if (booking != null) {
        booking.put("bookingStatus", targetState.wireName());
        if (generatedCBR) {
          booking.put("carrierBookingReference", cbr);
        }
        if (bookingMutator != null) {
          bookingMutator.accept(booking);
        }
        persistentMap.save(cbrr, booking);
      }
    }
    var notification =
        BookingNotification.builder()
            .apiVersion(apiVersion)
            .carrierBookingRequestReference(includeCbrr ? cbrr : null)
            .carrierBookingReference(cbr)
            .bookingStatus(targetState.wireName())
            .build()
            .asJsonNode();

    if (!isCorrect) {
      notification.remove("bookingStatus");
    }

    if (isShipperNotificationEnabled) {
      asyncCounterpartPost("/v2/booking-notifications", notification);
    } else {
      asyncOrchestratorPostPartyInput(
          objectMapper.createObjectNode().put("actionId", actionPrompt.get("actionId").asText()));
    }
  }

  @Override
  public ConformanceResponse handleRequest(ConformanceRequest request) {
    log.info("Carrier.handleRequest(%s)".formatted(request));
    if (request.method().equals("GET")) {
      return _handleGetBookingRequest(request);
    } else if (request.method().equals("POST") && request.url().endsWith("/v2/bookings")) {
      return _handlePostBookingRequest(request);
    } else {
      ConformanceResponse response =
          request.createResponse(
              409,
              Map.of("Api-Version", List.of(apiVersion)),
              new ConformanceMessageBody(
                  objectMapper
                      .createObjectNode()
                      .put(
                          "message",
                          "Rejecting '%s' for Booking '%s' because it is in state '%s'"
                              .formatted("TODO", "TODO", "TODO"))));
      addOperatorLogEntry(
          "Handling booking request '%s' (now in state '%s')".formatted("TODO", "TODO"));
      return response;
    }
  }

  private String lastUrlSegment(String url) {
    // ".../foo" and ".../foo/" should be the same
    return url.substring(1 + url.replaceAll("/++$", "").lastIndexOf("/"));
  }

  private ConformanceResponse _handleGetBookingRequest(ConformanceRequest request) {
    var bookingReference = lastUrlSegment(request.url());
    // bookingReference can either be a CBR or CBRR.
    var cbrr = cbrToCbrr.getOrDefault(bookingReference, bookingReference);
    var booking = persistentMap.load(cbrr);
    ConformanceResponse response =
        request.createResponse(
            200,
            Map.of("Api-Version", List.of(apiVersion)),
            new ConformanceMessageBody(booking));
    addOperatorLogEntry(
        "Responded to GET booking request '%s' (in state '%s')"
            .formatted(bookingReference, booking.get("bookingStatus")));
    return response;
  }

  @SneakyThrows
  private ConformanceResponse _handlePostBookingRequest(ConformanceRequest request) {
    String cbrr = UUID.randomUUID().toString();
    BookingState bookingState = BookingState.RECEIVED;
    bookingStatesByCbrr.put(cbrr, bookingState);
    ConformanceResponse response =
        request.createResponse(
            201,
            Map.of("Api-Version", List.of(apiVersion)),
            new ConformanceMessageBody(
                objectMapper
                    .createObjectNode()
                    .put("carrierBookingRequestReference", cbrr)
                    .put("bookingStatus", bookingState.wireName())));

    ObjectNode booking =
        (ObjectNode) objectMapper.readTree(request.message().body().getJsonBody().toString());
    booking.put("carrierBookingRequestReference", cbrr);
    booking.put("bookingStatus", bookingState.wireName());
    persistentMap.save(cbrr, booking);

    addOperatorLogEntry(
        "Accepted booking request '%s' (now in state '%s')"
            .formatted(cbrr, bookingState.wireName()));
    return response;
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
      ObjectMapper objectMapper = new ObjectMapper();
      var notification = objectMapper.createObjectNode();
      setIfNotNull(notification, "id", id);
      setIfNotNull(notification, "source", source);
      setIfNotNull(notification, "type", computedType());
      var data = objectMapper.createObjectNode();

      setIfNotNull(notification, "carrierBookingReference", carrierBookingReference);
      setIfNotNull(notification, "carrierBookingRequestReference", carrierBookingRequestReference);
      setIfNotNull(notification, "bookingStatus", bookingStatus);

      if (!data.isEmpty()) {
        notification.putObject("data").put("datacontenttype", "application/json");
      }
      return notification;
    }

    private void setIfNotNull(ObjectNode node, String key, String value) {
      if (value != null) {
        node.put(key, value);
      }
    }
  }

  private record LocationBuilder<T>(ObjectNode location, Function<ObjectNode, T> onCompletion) {

    // TODO: Add Address here at some point

      public T unlocation(String unlocationCode) {
        location.put("locationType", "UNLO")
          .put("UNLocationCode", unlocationCode);
        return endLocation();
      }

      public T facility(String unlocationCode, String facilityCode, String facilityCodeListProvider) {
        location.put("locationType", "FACI")
          .put("UNLocationCode", unlocationCode)
          .put("facilityCode", facilityCode)
          .put("facilityCodeListProvider", facilityCodeListProvider);
        return endLocation();
      }

      private T endLocation() {
        return this.onCompletion.apply(location);
      }
    }

    private record TransportPlanStepBuilder(TransportPlanBuilder parentBuilder, ObjectNode transportPlanStep) {
      public LocationBuilder<TransportPlanStepBuilder> loadLocation() {
        return new LocationBuilder<>(transportPlanStep.putObject("loadLocation"), (ignored -> this));
      }

      public LocationBuilder<TransportPlanStepBuilder> dischargeLocation() {
        return new LocationBuilder<>(transportPlanStep.putObject("dischargeLocation"), (ignored -> this));
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
      var step = transportPlan.addObject()
          // Yes, this is basically the array index (+1), but it is required, so here goes.
          .put("transportPlanStageSequenceNumber", sequenceNumber++);
      return new TransportPlanStepBuilder(this, step);
    }

    public JsonNode build() {
      // just to match the pattern really
      return transportPlan;
    }
  }

  private enum ReferenceState {
    MUST_EXIST,
    PROVIDE_IF_EXIST,
    GENERATE_IF_MISSING,
    ;
  }

  private static List<String> carrierClauses() {
    return List.of(
      "Per terms and conditions (see the termsAndConditions field), this is not a real booking.",
      "A real booking would probably have more legal text here."
    );
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
