package org.dcsa.conformance.standards.bookingandebl;

import static org.dcsa.conformance.standards.booking.party.BookingState.CONFIRMED;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.booking.BookingScenarioListBuilder;
import org.dcsa.conformance.standards.booking.action.BookingAction;
import org.dcsa.conformance.standards.booking.action.CarrierSupplyScenarioParametersAction;
import org.dcsa.conformance.standards.booking.action.ShipperGetBookingAction;
import org.dcsa.conformance.standards.booking.action.UC1_Shipper_SubmitBookingRequestAction;
import org.dcsa.conformance.standards.booking.action.UC5_Carrier_ConfirmBookingRequestAction;
import org.dcsa.conformance.standards.booking.action.UC7_Shipper_SubmitBookingAmendment;
import org.dcsa.conformance.standards.booking.action.UC8_Carrier_ProcessAmendmentAction;
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

  private BookingAndEblScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, BookingAndEblScenarioListBuilder> createModuleScenarioListBuilders(
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

  private static Map<String, BookingAndEblScenarioListBuilder> createConformanceScenarios(
      String carrierPartyName) {
    return Stream.of(
            Map.entry(
                "Dry Cargo",
                carrierSupplyScenarioParameters(carrierPartyName, ScenarioType.REGULAR)
                    .then(
                        uc1ShipperSubmitBookingRequest()
                            .then(
                                shipperGetBooking(BookingState.RECEIVED, null, null, false)
                                    .then(
                                        uc5CarrierConfirmBookingRequest()
                                            .then(
                                                shipperGetBooking(
                                                    CONFIRMED, null, null, false)))))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
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
                componentFactory.getMessageSchemaValidator(
                    BookingScenarioListBuilder.GET_BOOKING_SCHEMA_NAME),
                requestAmendedContent));
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
                componentFactory.getMessageSchemaValidator(
                    BookingScenarioListBuilder.CREATE_BOOKING_SCHEMA_NAME)));
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
                componentFactory.getMessageSchemaValidator(
                    BookingScenarioListBuilder.CREATE_BOOKING_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(
                    BookingScenarioListBuilder.BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getMessageSchemaValidator(
                    BookingScenarioListBuilder.BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingAndEblScenarioListBuilder uc5CarrierConfirmBookingRequest() {
    return carrierStateChange(UC5_Carrier_ConfirmBookingRequestAction::new);
  }

  private static BookingAndEblScenarioListBuilder uc7ShipperSubmitBookingAmendment(
      BookingState bookingState) {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new UC7_Shipper_SubmitBookingAmendment(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                bookingState,
                BookingState.AMENDMENT_RECEIVED,
                componentFactory.getMessageSchemaValidator(
                    BookingScenarioListBuilder.UPDATE_BOOKING_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(
                    BookingScenarioListBuilder.BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getMessageSchemaValidator(
                    BookingScenarioListBuilder.BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingAndEblScenarioListBuilder uc8aCarrierApproveBookingAmendment() {
    return carrierStateChange(
        (carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator) ->
            new UC8_Carrier_ProcessAmendmentAction(
                carrierPartyName,
                shipperPartyName,
                previousAction,
                BookingState.CONFIRMED,
                BookingState.AMENDMENT_CONFIRMED,
                requestSchemaValidator,
                true));
  }

  private static BookingAndEblScenarioListBuilder carrierStateChange(
      BookingScenarioListBuilder.CarrierNotificationUseCase constructor) {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            constructor.newInstance(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    BookingScenarioListBuilder.BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }
}
