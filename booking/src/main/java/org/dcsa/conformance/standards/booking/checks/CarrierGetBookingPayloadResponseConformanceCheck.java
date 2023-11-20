package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
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
    BookingState.COMPLETED,
    BookingState.DECLINED
  );
  private static final JsonPath BOOKING_STATE_PATH = JsonPath.compile("$.bookingStatus");
  private static final JsonPath REQUESTED_CHANGES_PATH = JsonPath.compile("$.requestedChanges");

  private static final Set<String> MANDATORY_ON_CONFIRMED_BOOKING = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
    "confirmedEquipments",
    "transportPlan",
    "shipmentCutOffTimes",
    "charges",
    "carrierClauses",
    "termsAndConditions"
  )));

  private static final Function<DocumentContext, Set<String>> ALL_OK = unused -> Collections.emptySet();

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
    if (CONFIRMED_BOOKING_STATES.contains(this.expectedState)) {
      for (var fieldName : MANDATORY_ON_CONFIRMED_BOOKING) {
        addSubCheck(fieldName + " is mandatory for confirmed bookings (optional otherwise)",
          requiredIfState(CONFIRMED_BOOKING_STATES, JsonPath.compile("$['" + fieldName + "']")),
          checks::add
        );
      }
    }
    return checks.stream();
  }

  private void addSubCheck(String subtitle, Function<DocumentContext, Set<String>> subCheck, Consumer<ConformanceCheck> addCheck) {
    var check = new ActionCheck(subtitle, this::isRelevantForRole, this.matchedExchangeUuid, this.httpMessageType) {
      @Override
      protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
        ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
        if (exchange == null) return Collections.emptySet();
        var booking =
          exchange
            .getResponse()
            .message()
            .body()
            .getJsonPathBody();
        try {
          return subCheck.apply(booking);
        } catch (RuntimeException e){
          return Set.of(e.getLocalizedMessage());
        }
      }
    };
    addCheck.accept(check);
  }

  @Override
  protected Set<String> checkConformance(Function<UUID, ConformanceExchange> getExchangeByUuid) {
    // All checks are delegated to sub-checks; nothing to do in here.
    return Collections.emptySet();
  }

  private Set<String> ensureBookingStateIsCorrect(DocumentContext booking) {
    String actualState = booking.read(BOOKING_STATE_PATH);
    if (Objects.equals(actualState, this.expectedState.wireName())) {
      return Collections.emptySet();
    }
    return Set.of("Expected bookingStatus '%s' but found '%s'"
      .formatted(expectedState.wireName(), actualState));
  }

  private Set<String> checkPendingUpdates(DocumentContext booking) {
    if (PENDING_CHANGES_STATES.contains(this.expectedState)) {
      return nonEmptyField(booking, REQUESTED_CHANGES_PATH);
    }
    return fieldIsOmitted(booking, REQUESTED_CHANGES_PATH);
  }

  private Function<DocumentContext, Set<String>> requiredIfState(Set<BookingState> conditionalInTheseStates, JsonPath jsonPath) {
    if (conditionalInTheseStates.contains(this.expectedState)) {
      return booking -> nonEmptyField(booking, jsonPath);
    }
    return ALL_OK;
  }

  private Set<String> fieldIsOmitted(DocumentContext booking, JsonPath key) {
    var field = booking.read(key);
    if (field != null) {
      return Set.of("The JsonPath %s must *NOT* be a present for a booking in status '%s'".formatted(
        key.getPath(), this.expectedState.wireName()
      ));
    }
    return Collections.emptySet();
  }

  private Set<String> nonEmptyField(DocumentContext booking, JsonPath key) {
    var field = booking.read(key);
    if (field != null) {
      if ((field instanceof Map m && !m.isEmpty()) || (field instanceof Collection c && !c.isEmpty())) {
        return Collections.emptySet();
      }
      if (field instanceof String s && !s.isBlank()) {
        return Collections.emptySet();
      }
    }
    return Set.of("The JsonPath %s must be a present and non-empty for a booking in status '%s'".formatted(
      key.getPath(), this.expectedState.wireName()
    ));
  }
}
