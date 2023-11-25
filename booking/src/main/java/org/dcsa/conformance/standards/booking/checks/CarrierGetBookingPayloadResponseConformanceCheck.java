package org.dcsa.conformance.standards.booking.checks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.check.ConformanceCheck;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.HttpMessageType;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

public class CarrierGetBookingPayloadResponseConformanceCheck extends ActionCheck {

  //  private static final Set<BookingState> PENDING_CHANGES_STATES =
  //      Set.of(BookingState.PENDING_UPDATE, BookingState.PENDING_AMENDMENT);

  private static final Set<BookingState> CONFIRMED_BOOKING_STATES =
      Set.of(BookingState.CONFIRMED, BookingState.COMPLETED, BookingState.DECLINED);

  private static final Set<String> MANDATORY_ON_CONFIRMED_BOOKING =
      Collections.unmodifiableSet(
          new LinkedHashSet<>(
              Arrays.asList(
                  "confirmedEquipments",
                  "transportPlan",
                  "shipmentCutOffTimes",
                  "charges",
                  "carrierClauses",
                  "termsAndConditions")));

  private static final Function<ObjectNode, Set<String>> ALL_OK = unused -> Collections.emptySet();

  private final BookingState expectedBookingStatus;
  private final BookingState expectedAmendedBookingStatus;

  public CarrierGetBookingPayloadResponseConformanceCheck(
      UUID matchedExchangeUuid,
      BookingState expectedBookingStatus,
      BookingState expectedAmendedBookingStatus) {
    super(
        "Validate the carrier response",
        BookingRole::isCarrier,
        matchedExchangeUuid,
        HttpMessageType.RESPONSE);
    this.expectedBookingStatus = expectedBookingStatus;
    this.expectedAmendedBookingStatus = expectedAmendedBookingStatus;
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
    addSubCheck("Validate bookingStatus", this::ensureBookingStatusIsCorrect, checks::add);
    if (expectedAmendedBookingStatus != null) {
      addSubCheck(
          "Validate amendedBookingStatus", this::ensureAmendedBookingStatusIsCorrect, checks::add);
    }
    // FIXME for amendedBookingStatus
    //    addSubCheck(
    //        "Validate requestedChanges is only present on states where it is allowed",
    //        this::checkPendingUpdates,
    //        checks::add);
    //    for (var fieldName : MANDATORY_ON_CONFIRMED_BOOKING) {
    //      addSubCheck(
    //          fieldName + " is required for confirmed bookings",
    //          requiredIfState(CONFIRMED_BOOKING_STATES, fieldName),
    //          checks::add);
    //    }
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

  private Set<String> ensureBookingStatusIsCorrect(JsonNode responsePayload) {
    String actualState = null;
    if (responsePayload.get("bookingStatus") instanceof TextNode statusNode) {
      actualState = statusNode.asText();
      if (actualState.equals(this.expectedBookingStatus.wireName())) {
        return Collections.emptySet();
      }
    }
    return Set.of(
        "Expected bookingStatus '%s' but found '%s'"
            .formatted(expectedBookingStatus.wireName(), actualState));
  }

  private Set<String> ensureAmendedBookingStatusIsCorrect(JsonNode responsePayload) {
    String actualState = null;
    if (responsePayload.get("amendedBookingStatus") instanceof TextNode statusNode) {
      actualState = statusNode.asText();
      if (actualState.equals(this.expectedAmendedBookingStatus.wireName())) {
        return Collections.emptySet();
      }
    }
    return Set.of(
        "Expected amendedBookingStatus '%s' but found '%s'"
            .formatted(expectedAmendedBookingStatus.wireName(), actualState));
  }

  private Set<String> checkPendingUpdates(JsonNode responsePayload) {
    var requestedChangesKey = "requestedChanges";
    // FIXME this needs to take amended status into account
    //    if (PENDING_CHANGES_STATES.contains(this.expectedBookingStatus)) {
    //      return nonEmptyField(responsePayload, requestedChangesKey);
    //    }
    return fieldIsOmitted(responsePayload, requestedChangesKey);
  }

  private Function<ObjectNode, Set<String>> requiredIfState(
      Set<BookingState> conditionalInTheseStates, String fieldName) {
    if (conditionalInTheseStates.contains(this.expectedBookingStatus)) {
      return booking -> nonEmptyField(booking, fieldName);
    }
    return ALL_OK;
  }

  private Set<String> fieldIsOmitted(JsonNode responsePayload, String key) {
    // FIXME to work with amendedBookingStatus
    //    if (responsePayload.has(key) || responsePayload.get(key) != null) {
    //      return Set.of(
    //          "The field '%s' must *NOT* be a present for a booking with %s"
    //              .formatted(key, printableExpectedStatus()));
    //    }
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
    return Set.of(
        "The field '%s' must be a present and non-empty for a booking with %s"
            .formatted(key, printableExpectedStatus()));
  }

  private String printableExpectedStatus() {
    var printableStatus = "bookingStatus='%s'".formatted(expectedBookingStatus.wireName());
    if (expectedAmendedBookingStatus != null) {
      printableStatus +=
          "amendedBookingStatus='%s'".formatted(expectedAmendedBookingStatus.wireName());
    }
    return printableStatus;
  }
}
