package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

public class CarrierGetBookingPayloadResponseConformanceCheck extends ActionCheck {

  private static final Set<BookingState> PENDING_CHANGES_STATES =
      Set.of(BookingState.PENDING_UPDATE, BookingState.PENDING_AMENDMENT);

   private static final Set<BookingState> CONFIRMED_BOOKING_STATES = Set.of(
    BookingState.CONFIRMED,
    BookingState.COMPLETED,
    BookingState.DECLINED
  );

  private static final Set<BookingState> REASON_STATES = Set.of(
    BookingState.DECLINED,
    BookingState.REJECTED,
    BookingState.CANCELLED
  );

  private static final Set<BookingState> BOOKING_STATES_WHERE_CBR_IS_OPTIONAL = Set.of(
    BookingState.RECEIVED,
    BookingState.REJECTED,
    BookingState.PENDING_UPDATE,
    /* CANCELLED depends on whether cancel happens before CONFIRMED, but the logic does not track prior
     * states. Therefore, we just assume it is optional in CANCELLED here.
     */
    BookingState.CANCELLED
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

  private static final String UNSET_MARKER = "<unset>";

  private final BookingState expectedBookingStatus;
  private final BookingState expectedAmendedBookingStatus;
  private final boolean amendedContent;

  public CarrierGetBookingPayloadResponseConformanceCheck(UUID matchedExchangeUuid, BookingState bookingState) {
    this(matchedExchangeUuid, bookingState, null, false);
  }

  public CarrierGetBookingPayloadResponseConformanceCheck(
    UUID matchedExchangeUuid,
    BookingState bookingState,
    BookingState expectedAmendedBookingStatus,
    boolean amendedContent
  ) {
    super(
      "Validate the carrier response",
      BookingRole::isCarrier,
      matchedExchangeUuid,
      HttpMessageType.RESPONSE
    );
    this.expectedBookingStatus = bookingState;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
    this.amendedContent = amendedContent;
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
    addSubCheck(
      "Validate 'bookingStatus' is correct",
      this::ensureBookingStatusIsCorrect,
      checks::add
    );
    addSubCheck(
      "Validate 'amendedBookingStatus' is correct",
      this::ensureAmendedBookingStatusIsCorrect,
      checks::add
    );
    addSubCheck(
      "Validate 'carrierBookingReference' is conditionally present",
      this::ensureCarrierBookingReferenceCompliance,
      checks::add
    );
    addSubCheck(
      "Validate 'requestedChanges' is only present on states where it is allowed",
      requiredOrExcludedByState(PENDING_CHANGES_STATES, "requestedChanges"),
      checks::add
    );
    addSubCheck(
      "Validate 'reason' is only present on states where it is allowed",
      requiredOrExcludedByState(REASON_STATES, "reason"),
      checks::add
    );
    for (var fieldName : MANDATORY_ON_CONFIRMED_BOOKING) {
      addSubCheck("'" + fieldName + "' is required for confirmed bookings",
        requiredCarrierAttributeIfState(CONFIRMED_BOOKING_STATES, fieldName),
        checks::add
      );
    }
    return checks.stream();
  }

  private void addSubCheck(
      String subtitle,
      Function<ObjectNode, Set<String>> subCheck,
      Consumer<ConformanceCheck> addCheck) {
    var check =
        new ActionCheck(
            subtitle, this::isRelevantForRole, this.matchedExchangeUuid, this.httpMessageType) {
          @Override
          protected Set<String> checkConformance(
              Function<UUID, ConformanceExchange> getExchangeByUuid) {
            ConformanceExchange exchange = getExchangeByUuid.apply(matchedExchangeUuid);
            if (exchange == null) return Collections.emptySet();
            var responsePayload = exchange.getResponse().message().body().getJsonBody();
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

  Set<String> ensureCarrierBookingReferenceCompliance(JsonNode responsePayload) {
    if (BOOKING_STATES_WHERE_CBR_IS_OPTIONAL.contains(expectedBookingStatus)) {
      return Collections.emptySet();
    }
    if (responsePayload.path("carrierBookingReference").isMissingNode()) {
      return Set.of("The 'carrierBookingReference' field was missing");
    }
    return Collections.emptySet();
  }

  private Set<String> ensureBookingStatusIsCorrect(JsonNode responsePayload) {
    String actualState = responsePayload.path("bookingStatus").asText(null);
    String expectedState = expectedBookingStatus.wireName();
    if (Objects.equals(actualState, expectedState)) {
      return Collections.emptySet();
    }
    return Set.of("Expected bookingStatus '%s' but found '%s'"
      .formatted(expectedState, Objects.requireNonNullElse(actualState, UNSET_MARKER)));
  }

  private Set<String> ensureAmendedBookingStatusIsCorrect(JsonNode responsePayload) {
    String actualState = responsePayload.path("amendedBookingStatus").asText(null);
    String expectedState = expectedAmendedBookingStatus != null ? expectedAmendedBookingStatus.wireName() : null;
    if (Objects.equals(actualState, expectedState)) {
      return Collections.emptySet();
    }
    return Set.of("Expected amendedBookingStatus '%s' but found '%s'"
      .formatted(
        Objects.requireNonNullElse(expectedState, UNSET_MARKER),
        Objects.requireNonNullElse(actualState, UNSET_MARKER)));
  }

  private boolean expectedStateMatch(Set<BookingState> states) {
    return expectedStateMatch(states::contains);
  }

  private boolean expectedStateMatch(Predicate<BookingState> check) {
    var state = Objects.requireNonNullElse(expectedAmendedBookingStatus, expectedBookingStatus);
    return check.test(state);
  }

  private Function<ObjectNode, Set<String>> requiredOrExcludedByState(Set<BookingState> conditionalInTheseStates, String fieldName) {
    if (expectedStateMatch(conditionalInTheseStates)) {
      return payload -> nonEmptyField(payload, fieldName);
    }
    return payload -> fieldIsOmitted(payload, fieldName);
  }


  private Function<ObjectNode, Set<String>> requiredCarrierAttributeIfState(Set<BookingState> conditionalInTheseStates, String fieldName) {
    if (!amendedContent && conditionalInTheseStates.contains(this.expectedBookingStatus)) {
      return booking -> nonEmptyField(booking, fieldName);
    }
    return ALL_OK;
  }

  private Set<String> fieldIsOmitted(JsonNode responsePayload, String key) {
    if (responsePayload.has(key) || responsePayload.get(key) != null) {
      return Set.of("The field '%s' must *NOT* be a present for a booking in status '%s'".formatted(
        key, this.expectedBookingStatus.wireName()
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
      key, this.expectedBookingStatus.wireName()
    ));
  }
}
