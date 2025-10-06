package org.dcsa.conformance.standards.booking.model;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;
import static org.dcsa.conformance.standards.booking.party.BookingCancellationState.*;
import static org.dcsa.conformance.standards.booking.party.BookingState.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.*;
import java.util.stream.StreamSupport;
import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingState;

public class PersistableCarrierBooking {

  private static final Map<BookingState, Predicate<BookingState>> PREREQUISITE_STATE_FOR_TARGET_STATE = Map.ofEntries(
    Map.entry(CONFIRMED, Set.of(RECEIVED, UPDATE_RECEIVED, CONFIRMED)::contains),
    Map.entry(REJECTED, Set.of(RECEIVED, PENDING_UPDATE, UPDATE_RECEIVED)::contains),
    Map.entry(DECLINED, Set.of(CONFIRMED, PENDING_AMENDMENT, AMENDMENT_RECEIVED)::contains),
    Map.entry(PENDING_UPDATE, Set.of(RECEIVED, PENDING_UPDATE, UPDATE_RECEIVED)::contains),
    Map.entry(PENDING_AMENDMENT, Set.of(CONFIRMED, PENDING_AMENDMENT)::contains),
    Map.entry(COMPLETED, Set.of(CONFIRMED)::contains),
    Map.entry(CANCELLED, Set.of(RECEIVED, UPDATE_RECEIVED, PENDING_UPDATE, CONFIRMED, PENDING_AMENDMENT)::contains)
  );

  private static final Set<BookingState> PREREQUISITE_BOOKING_STATES_FOR_CANCELLATION = Set.of(
    CONFIRMED,
    PENDING_AMENDMENT,
    AMENDMENT_RECEIVED,
    AMENDMENT_CONFIRMED,
    AMENDMENT_DECLINED,
    AMENDMENT_CANCELLED
  );

  private static final Set<BookingState> PREREQUISITE_AMENDMENT_BOOKING_STATES_FOR_CANCELLATION = Set.of(
    AMENDMENT_RECEIVED,
    AMENDMENT_CONFIRMED,
    AMENDMENT_DECLINED,
    AMENDMENT_CANCELLED
  );

  private static final Set<BookingState> MAY_AMEND_STATES = Set.of(
    CONFIRMED,
    PENDING_AMENDMENT
  );

  private static final Set<BookingState> MAY_UPDATE_REQUEST_STATES = Set.of(
    RECEIVED,
    PENDING_UPDATE,
    UPDATE_RECEIVED
  );

  private static final String BOOKING_STATUS = "bookingStatus";
  private static final String AMENDED_BOOKING_STATUS = "amendedBookingStatus";
  private static final String CANCELLATION_BOOKING_STATUS = "bookingCancellationStatus";
  private static final String CARRIER_BOOKING_REQUEST_REFERENCE = "carrierBookingRequestReference";
  private static final String CARRIER_BOOKING_REFERENCE = "carrierBookingReference";
  private static final String SUBSCRIPTION_REFERENCE = "subscriptionReference";
  public static final String FEEDBACKS = "feedbacks";

  private static final String[] METADATA_FIELDS_TO_PRESERVE = {
    CARRIER_BOOKING_REQUEST_REFERENCE,
    CARRIER_BOOKING_REFERENCE,
    BOOKING_STATUS,
    AMENDED_BOOKING_STATUS,
  };



  private static final String BOOKING_DATA_FIELD = "booking";
  private static final String AMENDED_BOOKING_DATA_FIELD = "amendedBooking";

  private final ObjectNode state;

  private PersistableCarrierBooking(ObjectNode state) {
    this.state = state;
  }

  public String getCarrierBookingRequestReference() {
    return getBooking().required(CARRIER_BOOKING_REQUEST_REFERENCE).asText();
  }

  public String getCarrierBookingReference() {
    return getBooking().path(CARRIER_BOOKING_REFERENCE).asText(null);
  }

  public JsonNode getfeedbacks() {
    return getBooking().path(FEEDBACKS);
  }

  public ObjectNode getBooking() {
    return (ObjectNode) state.required(BOOKING_DATA_FIELD);
  }

  private void setBooking(ObjectNode node) {
    state.set(BOOKING_DATA_FIELD, node);
  }

  public Optional<ObjectNode> getAmendedBooking() {
    return Optional.ofNullable((ObjectNode)state.get(AMENDED_BOOKING_DATA_FIELD));
  }

  private void setAmendedBooking(ObjectNode node) {
    state.set(AMENDED_BOOKING_DATA_FIELD, node);
  }

  public void confirmBookingAmendment(String reference) {
    checkState(reference, getBookingAmendedState(), s -> s == AMENDMENT_RECEIVED);
    changeState(BOOKING_STATUS, CONFIRMED);
    changeState(AMENDED_BOOKING_STATUS, AMENDMENT_CONFIRMED);
    mutateBookingAndAmendment(this::ensureConfirmedBookingHasCarrierFields);
  }

  public void confirmBooking(String reference, Supplier<String> cbrGenerator) {
    var prerequisites = PREREQUISITE_STATE_FOR_TARGET_STATE.get(CONFIRMED);
    checkState(reference, getOriginalBookingState(), prerequisites);
    if (this.getCarrierBookingReference() == null) {
      var newCbr = cbrGenerator.get();
      mutateBookingAndAmendment(b -> b.put(CARRIER_BOOKING_REFERENCE, newCbr));
    }
    changeState(BOOKING_STATUS, CONFIRMED);
    mutateBookingAndAmendment(this::ensureConfirmedBookingHasCarrierFields);
    mutateBookingAndAmendment(b -> b.remove(AMENDED_BOOKING_STATUS));
  }

  private void resetAmendedBookingState() {
    state.remove(AMENDED_BOOKING_DATA_FIELD);
    mutateBookingAndAmendment(b -> b.remove(AMENDED_BOOKING_STATUS));
  }

  private void resetCancellationBookingState() {
    mutateBookingAndAmendment(b -> b.remove(CANCELLATION_BOOKING_STATUS));
  }

  private void ensureConfirmedBookingHasCarrierFields(ObjectNode booking) {
    var clauses = booking.putArray("carrierClauses");
    booking.put("termsAndConditions", termsAndConditions());
    for (var clause : carrierClauses()) {
      clauses.add(clause);
    }
    replaceConfirmedEquipments(booking);
    addCharge(booking);
    generateTransportPlan(booking);
    replaceShipmentCutOffTimes(booking);
    addAdvancedManifestFilings(booking);
  }

  public void declineBooking(String reference) {
    var prerequisites = PREREQUISITE_STATE_FOR_TARGET_STATE.get(DECLINED);
    checkState(reference, getOriginalBookingState(), prerequisites);
    changeState(BOOKING_STATUS, DECLINED);
    if (getAmendedBooking().isPresent()) {
      changeState(AMENDED_BOOKING_STATUS, AMENDMENT_DECLINED);
    }
  }

  public void declineBookingAmendment(String reference) {
    checkState(reference, getBookingAmendedState(), s -> s == AMENDMENT_RECEIVED);
    changeState(AMENDED_BOOKING_STATUS, AMENDMENT_DECLINED);
  }

  public void requestUpdateToBooking(String reference, Consumer<ObjectNode> bookingMutator) {
    var prerequisites = PREREQUISITE_STATE_FOR_TARGET_STATE.get(PENDING_UPDATE);
    checkState(reference, getOriginalBookingState(), prerequisites);
    changeState(BOOKING_STATUS, PENDING_UPDATE);
    mutateBookingAndAmendment(bookingMutator);
  }

  public void rejectBooking(String reference) {
    var prerequisites = PREREQUISITE_STATE_FOR_TARGET_STATE.get(REJECTED);
    checkState(reference, getOriginalBookingState(), prerequisites);
    changeState(BOOKING_STATUS, REJECTED);
  }

  public void confirmBookingCompleted(
      String reference, boolean resetAmendedBooking, boolean resetCancellationBookingState) {
    var prerequisites = PREREQUISITE_STATE_FOR_TARGET_STATE.get(COMPLETED);
    checkState(reference, getOriginalBookingState(), prerequisites);
    changeState(BOOKING_STATUS, COMPLETED);
    if (resetAmendedBooking) {
      resetAmendedBookingState();
    }
    if (resetCancellationBookingState) {
      resetCancellationBookingState();
    }
  }

  public void updateConfirmedBooking(String reference, Consumer<ObjectNode> bookingMutator,boolean resetAmendedBooking) {
    var prerequisites = PREREQUISITE_STATE_FOR_TARGET_STATE.get(PENDING_AMENDMENT);
    checkState(reference, getOriginalBookingState(), prerequisites);
    changeState(BOOKING_STATUS, PENDING_AMENDMENT);
    mutateBookingAndAmendment(bookingMutator);
    if (resetAmendedBooking) {
      resetAmendedBookingState();
    }
  }

  public void cancelBookingRequest(String bookingReference) {
    var prerequisites = PREREQUISITE_STATE_FOR_TARGET_STATE.get(CANCELLED);
    checkState(bookingReference, getOriginalBookingState(), prerequisites);
    changeState(BOOKING_STATUS, CANCELLED);
  }

  public void cancelBookingAmendment(String bookingReference) {
    checkState(bookingReference, getBookingAmendedState(), s -> s == AMENDMENT_RECEIVED);
    changeState(AMENDED_BOOKING_STATUS, AMENDMENT_CANCELLED);
  }

  public void cancelConfirmedBooking(String bookingReference) {
    checkState(bookingReference, getBookingCancellationState(), s -> s == CANCELLATION_RECEIVED);
    changeState(CANCELLATION_CONFIRMED);
    changeState(BOOKING_STATUS, CANCELLED);
  }

  public void updateCancelConfirmedBooking(String bookingReference) {
    checkState(bookingReference, getOriginalBookingState(), PREREQUISITE_BOOKING_STATES_FOR_CANCELLATION::contains);
    if(getBookingAmendedState()!=null) {
      checkState(bookingReference, getBookingAmendedState(), PREREQUISITE_AMENDMENT_BOOKING_STATES_FOR_CANCELLATION::contains);
    }
    mutateBookingAndAmendment(b -> b.put(CANCELLATION_BOOKING_STATUS, CANCELLATION_RECEIVED.name()));
  }

  public void declineConfirmedBookingCancellation(String bookingReference) {
    checkState(bookingReference, getBookingCancellationState(), s -> s == CANCELLATION_RECEIVED);
    changeState(CANCELLATION_DECLINED);
  }

  private void changeState(String attributeName, BookingState newState) {
    mutateBookingAndAmendment(b -> b.put(attributeName, newState.name()));
  }

  private void changeState(BookingCancellationState newState) {
    mutateBookingAndAmendment(b -> b.put(CANCELLATION_BOOKING_STATUS, newState.name()));
  }

  private void mutateBookingAndAmendment(Consumer<ObjectNode> mutator) {
    mutator.accept(getBooking());
    getAmendedBooking().ifPresent(mutator);
  }

  private static void checkState(
    String reference, BookingState currentState, Predicate<BookingState> expectedState) {
    if (!expectedState.test(currentState)) {
      throw new IllegalStateException(
        "Booking '%s' is in state '%s'".formatted(reference, currentState));
    }
  }

  private static void checkState(
    String reference, BookingCancellationState currentState, Predicate<BookingCancellationState> expectedState) {
    if (!expectedState.test(currentState)) {
      throw new IllegalStateException(
        "Booking '%s' is in state '%s'".formatted(reference, currentState));
    }
  }

  public void putBooking(String bookingReference, ObjectNode newBookingData) {
    var currentState = getOriginalBookingState();
    var amendedBookingState = getBookingAmendedState();
    boolean isAmendment =
      currentState.equals(BookingState.CONFIRMED)
        || currentState.equals(BookingState.PENDING_AMENDMENT);

    checkState(
      bookingReference,
      currentState,
      (isAmendment ? MAY_AMEND_STATES : MAY_UPDATE_REQUEST_STATES)::contains
    );

    if(amendedBookingState != null && amendedBookingState.equals(AMENDMENT_RECEIVED))  {
      throw new IllegalStateException(
        "Booking '%s' is in Amendment state '%s'".formatted(bookingReference, amendedBookingState));
    }

    if (isAmendment) {
      changeState(AMENDED_BOOKING_STATUS, BookingState.AMENDMENT_RECEIVED);
    } else {
      changeState(BOOKING_STATUS, BookingState.UPDATE_RECEIVED);
    }
    copyMetadataFields(getBooking(), newBookingData);
    if (isAmendment) {
      ensureFeedbacksExist(newBookingData);
      ensureConfirmedBookingHasCarrierFields(newBookingData);
      setAmendedBooking(newBookingData);
    } else {
      setBooking(newBookingData);
    }

  }

  private void ensureFeedbacksExist(ObjectNode booking) {
    booking
        .putArray("feedbacks")
        .addObject()
        .put("severity", "ERROR")
        .put("code", "PROPERTY_VALUE_MUST_CHANGE")
        .put(
            "message",
            "Please change any one of the attributes in the request payload for conformance. For example, change VesselName to 'King of the Seas'")
        .put("jsonPath", "$.vessel.name")
        .put("property", "name");
  }

  public BookingState getOriginalBookingState() {
    var booking = getBooking();
    return BookingState.fromString(booking.required(BOOKING_STATUS).asText());
  }

  public BookingState getBookingAmendedState() {
    var booking = getBooking();
    var s = booking.path(AMENDED_BOOKING_STATUS);
    return !s.asText("").isEmpty()? BookingState.fromString(s.asText()) : null;
  }

  public BookingCancellationState getBookingCancellationState() {
    var booking = getBooking();
    var s = booking.path(CANCELLATION_BOOKING_STATUS);
    return !s.asText("").isEmpty()? BookingCancellationState.fromString(s.asText()) : null;
  }

  public static PersistableCarrierBooking initializeFromBookingRequest(ObjectNode bookingRequest) {
    String cbrr = UUID.randomUUID().toString();
    bookingRequest.put(CARRIER_BOOKING_REQUEST_REFERENCE, cbrr)
      .put(BOOKING_STATUS, BookingState.RECEIVED.name());
    var state = OBJECT_MAPPER.createObjectNode();
    state.put(SUBSCRIPTION_REFERENCE,UUID.randomUUID().toString());
    state.set(BOOKING_DATA_FIELD, bookingRequest);
    return new PersistableCarrierBooking(state);
  }

  public String getSubscriptionReference() {
    return this.state.required(SUBSCRIPTION_REFERENCE).asText("");
  }

  public static PersistableCarrierBooking fromPersistentStore(JsonNode state) {
    return new PersistableCarrierBooking((ObjectNode) state);
  }

  public JsonNode asPersistentState() {
    return this.state;
  }

  public static PersistableCarrierBooking fromPersistentStore(JsonNodeMap jsonNodeMap, String carrierBookingRequestReference) {
    var data = jsonNodeMap.load(carrierBookingRequestReference);
    if (data == null || data.isMissingNode() || !data.isObject()) {
      throw new IllegalArgumentException("Unknown CBRR: " + carrierBookingRequestReference);
    }
    return fromPersistentStore(data);
  }

  public void save(JsonNodeMap jsonNodeMap) {
    jsonNodeMap.save(getCarrierBookingRequestReference(), asPersistentState());
  }

  private void copyMetadataFields(JsonNode originalBooking, ObjectNode updatedBooking) {
    for (String field : METADATA_FIELDS_TO_PRESERVE) {
      var previousValue = originalBooking.path(field);
      if (previousValue != null && previousValue.isTextual()){
        updatedBooking.put(field, previousValue.asText());
      } else {
        updatedBooking.remove(field);
      }
    }
  }


  private String extractUnLocationCode(JsonNode locationNode) {
    if (locationNode != null) {
      var loc = locationNode.path("location");
      var unloc = loc.path("UNLocationCode").asText("");
      if (!unloc.isBlank()) {
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
    var receiptTypeAtOrigin = booking.path("receiptTypeAtOrigin").asText("");
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
    addShipmentCutOff(shipmentCutOffTimes, "EFC", oneWeekPrior);

    if("CFS".equals(receiptTypeAtOrigin)) {
      addShipmentCutOff(shipmentCutOffTimes, "LCO", oneWeekPrior);
    }
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

  private void replaceConfirmedEquipments(ObjectNode booking) {
    if (booking.get("requestedEquipments") instanceof ArrayNode requestedEquipments
      && !requestedEquipments.isEmpty()) {
      var commoditySubReferenceSequence = 1;
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
        var commoditiesNode = (ArrayNode) requestedEquipment.get("commodities");
        if (commoditiesNode != null && commoditiesNode.isArray()) {
          for(var commodity: commoditiesNode) {
            ((ObjectNode)commodity).put("commoditySubReference", "COM"+commoditySubReferenceSequence++);
          }
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

  private void addCharge(ObjectNode booking) {
    ArrayNode charges;
    if (booking.get("charges") instanceof ArrayNode chargeNode) {
      charges = chargeNode;
    } else {
      charges = booking.putArray("charges");
    }
    if (charges.isEmpty()) {
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
        .put("chargeName", "Fictive amendment fee")
        .put("currencyAmount", 1f)
        .put("currencyCode", "EUR")
        .put("paymentTermCode", "COL")
        .put("calculationBasis", "For the concrete amendment")
        .put("unitPrice", 1f)
        .put("quantity", 1);
    }
  }

  private void addAdvancedManifestFilings(ObjectNode booking) {
    ArrayNode advanceManifestFilings;
    if (booking.get("advanceManifestFilings") instanceof ArrayNode advanceManifestFilingsNode) {
      advanceManifestFilings = advanceManifestFilingsNode;
    } else {
      advanceManifestFilings = booking.putArray("advanceManifestFilings");
    }
    if (advanceManifestFilings.isEmpty()) {
      advanceManifestFilings
        .addObject()
        .put("manifestTypeCode", "ACI")
        .put("countryCode", "EG");
    }
  }


  private record LocationBuilder<T>(ObjectNode location, Function<ObjectNode, T> onCompletion) {

    // TODO: Add Address here at some point

    public T unlocation(String unlocationCode) {
      location.put("UNLocationCode", unlocationCode);
      return endLocation();
    }

    public T facility(String unlocationCode, String facilityCode, String facilityCodeListProvider) {
      location
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

    private TransportPlanStepBuilder setStringField(String fieldName, String value) {
      this.transportPlanStep.put(fieldName, value);
      return this;
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
