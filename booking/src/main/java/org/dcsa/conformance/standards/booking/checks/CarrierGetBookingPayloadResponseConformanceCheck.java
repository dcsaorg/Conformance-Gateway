package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class CarrierGetBookingPayloadResponseConformanceCheck extends ActionCheck {


  private static final Set<BookingState> PENDING_CHANGES_STATES = Set.of(
    BookingState.PENDING_UPDATE
  );

  private static final Set<BookingState> CONFIRMED_BOOKING_STATES = Set.of(
    BookingState.CONFIRMED,
    BookingState.COMPLETED
  );

  private static final Set<String> MANDATORY_ON_CONFIRMED_BOOKING = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
    "confirmedEquipments",
    "transportPlan",
    "shipmentCutOffTimes",
    "charges",
    "carrierClauses",
    "termsAndConditions"
  )));

  private static final Function<ObjectNode, Set<String>> ALL_OK = unused -> Collections.emptySet();

  private final BookingState expectedState;

  public CarrierGetBookingPayloadResponseConformanceCheck(UUID matchedExchangeUuid, BookingState bookingState) {
    super(
      "Validate the carrier response",
      BookingRole::isCarrier,
      matchedExchangeUuid,
      HttpMessageType.RESPONSE
    );
    this.expectedState = bookingState;
  }

  @Override
  protected Stream<? extends ConformanceCheck> createSubChecks() {
    List<ConformanceCheck> checks = new ArrayList<>();
    /*
     * Checks for fields that the *Carrier* is responsible for.  That is things like "requestedChanges"
     * or the "transportPlan".  If the Shipper should conditionally provide a field, then that is
     * validation should go somewhere else.
     *
     * As an example, the Carrier does *not* get flagged for not providing "either serviceContractReference
     * OR contractQuotationReference" if the Shipper failed to provide those in the last POST/PUT call
     */
    addSubCheck("Validate bookingState", this::ensureBookingStateIsCorrect, checks::add);
    addSubCheck(
      "Validate requestedChanges is only present on states where it is allowed",
      this::checkPendingUpdates,
      checks::add
    );
    for (var fieldName : MANDATORY_ON_CONFIRMED_BOOKING) {
      addSubCheck(fieldName + " is required for confirmed bookings",
        requiredIfState(CONFIRMED_BOOKING_STATES, fieldName),
        checks::add
      );
    }
    return checks.stream();
  }

  private void addSubCheck(String subtitle, Function<ObjectNode, Set<String>> subCheck, Consumer<ConformanceCheck> addCheck) {
    var check = new ActionCheck(subtitle, this::isRelevantForRole, this.matchedExchangeUuid, this.httpMessageType) {
      @Override
      protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
        ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
        if (exchange == null) return Collections.emptySet();
        var responsePayload =
          exchange
            .getResponse()
            .message()
            .body()
            .getJsonBody();
        if (responsePayload instanceof ObjectNode booking) {
          return subCheck.apply(booking);
        }
        return Set.of("Could not perform the check as the payload was not correct format");
      }
    };
    addCheck.accept(check);
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    // All checks are delegated to sub-checks; nothing to do in here.
    return Collections.emptySet();
  }

  private Set<String> ensureBookingStateIsCorrect(JsonNode responsePayload) {
    String actualState = null;
    if (responsePayload.get("bookingStatus") instanceof TextNode statusNode) {
      actualState = statusNode.asText();
      if (actualState.equals(this.expectedState.wireName())) {
        return Collections.emptySet();
      }
    }
    return Set.of("Expected bookingStatus '%s' but found '%s'"
      .formatted(expectedState.wireName(), actualState));
  }

  private Set<String> checkPendingUpdates(JsonNode responsePayload) {
    var requestedChangesKey = "requestedChanges";
    if (PENDING_CHANGES_STATES.contains(this.expectedState)) {
      return nonEmptyField(responsePayload, requestedChangesKey);
    }
    return fieldIsOmitted(responsePayload, requestedChangesKey);
  }

  private Function<ObjectNode, Set<String>> requiredIfState(Set<BookingState> conditionalInTheseStates, String fieldName) {
    if (conditionalInTheseStates.contains(this.expectedState)) {
      return booking -> nonEmptyField(booking, fieldName);
    }
    return ALL_OK;
  }

  private Set<String> fieldIsOmitted(JsonNode responsePayload, String key) {
    if (responsePayload.has(key) || responsePayload.get(key) != null) {
      return Set.of("The field '%s' must *NOT* be a present for a booking in status '%s'".formatted(
        key, this.expectedState.wireName()
      ));
    }
    return Collections.emptySet();
  }

  private Set<String> nonEmptyField(JsonNode responsePayload, String key) {
    var field = responsePayload.get(key);
    if (field != null) {
      if (field.isTextual() && !field.asText().isBlank()) {
        return Collections.emptySet();
      }
      if (!field.isEmpty() || field.isValueNode()) {
        return Collections.emptySet();
      }
    }
    return Set.of("The field '%s' must be a present and non-empty for a booking in status '%s'".formatted(
      key, this.expectedState.wireName()
    ));
  }
}
