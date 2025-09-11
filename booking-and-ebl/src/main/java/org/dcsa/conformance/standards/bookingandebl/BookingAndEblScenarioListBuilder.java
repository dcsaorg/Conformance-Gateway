package org.dcsa.conformance.standards.bookingandebl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.booking.action.BookingAction;
import org.dcsa.conformance.standards.booking.action.CarrierSupplyScenarioParametersAction;
import org.dcsa.conformance.standards.booking.action.ShipperGetBookingAction;
import org.dcsa.conformance.standards.booking.action.UC1_Shipper_SubmitBookingRequestAction;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingState;

@Slf4j
public class BookingAndEblScenarioListBuilder
    extends ScenarioListBuilder<BookingAndEblScenarioListBuilder> {

  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private static final ThreadLocal<BookingAndEblComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalShipperPartyName = new ThreadLocal<>();

  private static final String GET_BOOKING_SCHEMA_NAME = "Booking";
  private static final String CREATE_BOOKING_SCHEMA_NAME = "CreateBooking";
  private static final String BOOKING_202_RESPONSE_SCHEMA = "CreateBookingResponse";
  private static final String BOOKING_NOTIFICATION_SCHEMA_NAME = "BookingNotification";

  private BookingAndEblScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, ScenarioListBuilder> createModuleScenarioListBuilders(
      BookingAndEblComponentFactory componentFactory,
      String carrierPartyName,
      String shipperPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);

    if (SCENARIO_SUITE_CONFORMANCE.equals(componentFactory.getScenarioSuite())) {
      return createConformanceScenarios(carrierPartyName);
    }

    throw new IllegalArgumentException(
        "Invalid scenario suite name '%s'".formatted(componentFactory.getScenarioSuite()));
  }

  private static Map<String, ScenarioListBuilder> createConformanceScenarios(
      String carrierPartyName) {
    return Stream.of(
            Map.entry(
                "Dry Cargo",
                carrierSupplyScenarioParameters(carrierPartyName, ScenarioType.REGULAR)
                    .then(
                        uc1ShipperSubmitBookingRequest()
                            .then(shipperGetBooking(BookingState.RECEIVED, null, null, false)))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static BookingAndEblScenarioListBuilder carrierSupplyScenarioParameters(
      String carrierPartyName, ScenarioType scenarioType) {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    var version = componentFactory.getStandardVersion().split("-\\+-")[0];
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new CarrierSupplyScenarioParametersAction(
                carrierPartyName,
                scenarioType,
                version,
                componentFactory.getMessageSchemaValidator(CREATE_BOOKING_SCHEMA_NAME)));
  }

  private static BookingAndEblScenarioListBuilder uc1ShipperSubmitBookingRequest() {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new UC1_Shipper_SubmitBookingRequestAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                componentFactory.getMessageSchemaValidator(CREATE_BOOKING_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getMessageSchemaValidator(BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingAndEblScenarioListBuilder shipperGetBooking(
      BookingState expectedBookingStatus,
      BookingState expectedAmendedBookingStatus,
      BookingCancellationState expectedCancellationState,
      boolean requestAmendedContent) {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new ShipperGetBookingAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                expectedBookingStatus,
                expectedAmendedBookingStatus,
                expectedCancellationState,
                componentFactory.getMessageSchemaValidator(GET_BOOKING_SCHEMA_NAME),
                requestAmendedContent));
  }
}
