package org.dcsa.conformance.standards.booking.model;

import static org.dcsa.conformance.standards.booking.party.BookingState.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.dcsa.conformance.core.state.JsonNodeMap;
import org.dcsa.conformance.standards.booking.party.BookingState;

public class PersistableCarrierBooking {

  private static final String[] METADATA_FIELDS_TO_PRESERVE = {
    "carrierBookingRequestReference",
    "carrierBookingReference",
    "bookingStatus",
    "amendedBookingStatus",
  };

  private static final Map<BookingState, Predicate<BookingState>> PREREQUISITE_STATE_FOR_TARGET_STATE = Map.ofEntries(
    Map.entry(CONFIRMED, Set.of(RECEIVED, PENDING_UPDATE_CONFIRMATION)::contains),
    Map.entry(REJECTED, Set.of(RECEIVED, PENDING_UPDATE, PENDING_UPDATE_CONFIRMATION)::contains),
    Map.entry(DECLINED, Set.of(CONFIRMED, PENDING_AMENDMENT, AMENDMENT_RECEIVED)::contains),
    Map.entry(PENDING_UPDATE, Set.of(RECEIVED, PENDING_UPDATE, PENDING_UPDATE_CONFIRMATION)::contains),
    Map.entry(PENDING_AMENDMENT, Set.of(CONFIRMED, PENDING_AMENDMENT)::contains),
    Map.entry(COMPLETED, Set.of(CONFIRMED)::contains)
  );

  private static final Set<BookingState> NOT_APPLICABLE_FOR_SIMPLE_STATE_CHANGE = Set.of(
    START,
    RECEIVED,
    PENDING_UPDATE_CONFIRMATION,
    AMENDMENT_RECEIVED,
    CANCELLED
  );

  private static final Set<BookingState> MAY_AMEND_STATES = Set.of(
    CONFIRMED,
    PENDING_AMENDMENT
  );

  private static final Set<BookingState> MAY_UPDATE_REQUEST_STATES = Set.of(
    RECEIVED,
    PENDING_UPDATE,
    PENDING_UPDATE_CONFIRMATION
  );

  private static final String BOOKING_STATUS = "bookingStatus";
  private static final String AMENDED_BOOKING_STATUS = "amendedBookingStatus";

  private static final String BOOKING_DATA_FIELD = "booking";
  private static final String AMENDED_BOOKING_DATA_FIELD = "amendedBooking";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final ObjectNode state;

  private PersistableCarrierBooking(ObjectNode state) {
    this.state = state;
  }

  public String getCarrierBookingRequestReference() {
    return getBooking().path("carrierBookingRequestReference").asText();
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

  public void performSimpleStatusChange(String reference, BookingState newState) {
    performSimpleStatusChange(reference, newState, null);
  }

  public void performSimpleStatusChange(String reference, BookingState newState, String reason) {
    // FIXME: Have logic for amendment vs. non-amendment states
    if (NOT_APPLICABLE_FOR_SIMPLE_STATE_CHANGE.contains(newState)) {
      throw new IllegalArgumentException("This state cannot be set via setCarrierStatus");
    }
    var prerequisiteState = PREREQUISITE_STATE_FOR_TARGET_STATE.get(newState);
    if (prerequisiteState == null) {
      throw new IllegalArgumentException("Missing dependency check for state " + newState.wireName());
    }
    checkState(reference, getBookingState(), prerequisiteState);
    changeState(BOOKING_STATUS, newState);
    var booking = getBooking();
    if (reason != null) {
      booking.put("reason", reason);
    } else {
      booking.remove("reason");
    }
  }

  public void cancelEntireBooking(String bookingReference, String reason) {
    checkState(bookingReference, getBookingState(), s -> s != CANCELLED);
    changeState(BOOKING_STATUS, CANCELLED);
    if (reason == null || reason.isBlank()) {
      reason = "Entire booking cancelled by shipper (no reason given)";
    }
    final var cancelReason = reason;
    getBooking().put("reason", cancelReason);
    getAmendedBooking().ifPresent(amendedBooking -> amendedBooking.put(AMENDED_BOOKING_STATUS, CANCELLED.wireName())
      .put("reason", cancelReason));
  }

  public void cancelBookingAmendment(String bookingReference, String reason) {
    checkState(bookingReference, getBookingState(), s -> s == AMENDMENT_RECEIVED);
    changeState(AMENDED_BOOKING_STATUS, CANCELLED);
    if (reason == null || reason.isBlank()) {
      reason = "Amendment cancelled by shipper (no reason given)";
    }
    var amendedBooking = getAmendedBooking().orElseThrow(AssertionError::new);
    amendedBooking.put("reason", reason);
  }

  private void changeState(String attributeName, BookingState newState) {
    getBooking().put(attributeName, newState.wireName());
    getAmendedBooking().ifPresent(amendedBooking -> amendedBooking.put(attributeName, newState.wireName()));
  }

  private static void checkState(
    String reference, BookingState currentState, Predicate<BookingState> expectedState) {
    if (!expectedState.test(currentState)) {
      throw new IllegalStateException(
        "Booking '%s' is in state '%s'".formatted(reference, currentState));
    }
  }

  private void removeRequestedChanges() {
    getBooking().remove("requestedChanges");
    getAmendedBooking().ifPresent(amendedBooking -> amendedBooking.remove("requestedChanges"));
  }

  public void putBooking(String bookingReference, ObjectNode newBookingData) {
    var currentState = getBookingState();
    boolean isAmendment =
      currentState.equals(BookingState.CONFIRMED)
        || currentState.equals(BookingState.PENDING_AMENDMENT);

    checkState(
      bookingReference,
      currentState,
      (isAmendment ? MAY_AMEND_STATES : MAY_UPDATE_REQUEST_STATES)::contains
    );
    if (isAmendment) {
      changeState(AMENDED_BOOKING_STATUS, BookingState.AMENDMENT_RECEIVED);
    } else {
      changeState(BOOKING_STATUS, BookingState.PENDING_UPDATE_CONFIRMATION);
    }
    copyMetadataFields(getBooking(), newBookingData);
    if (isAmendment) {
      setAmendedBooking(newBookingData);
    } else {
      setBooking(newBookingData);
    }
    removeRequestedChanges();
  }

  public BookingState getBookingState() {
    var booking = getBooking();
    var s = booking.path(AMENDED_BOOKING_STATUS);
    if (s.isTextual()) {
      return BookingState.fromWireName(s.asText());
    }
    return BookingState.fromWireName(booking.required(BOOKING_STATUS).asText());
  }

  public static PersistableCarrierBooking initializeFromBookingRequest(ObjectNode bookingRequest) {
    String cbrr = UUID.randomUUID().toString();
    bookingRequest.put("carrierBookingRequestReference", cbrr)
      .put(BOOKING_STATUS, BookingState.RECEIVED.wireName());
    var state = OBJECT_MAPPER.createObjectNode();
    state.set(BOOKING_DATA_FIELD, bookingRequest);
    return new PersistableCarrierBooking(state);
  }

  public static PersistableCarrierBooking fromPersistentStore(JsonNode state) {
    return new PersistableCarrierBooking((ObjectNode) state);
  }

  public JsonNode asPersistentState() {
    return this.state;
  }

  public static PersistableCarrierBooking fromPersistentStore(JsonNodeMap jsonNodeMap, String carrierBookingRequestReference) {
    var data = jsonNodeMap.load(carrierBookingRequestReference);
    if (data == null) {
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

}
