package org.dcsa.conformance.standards.booking;

import static org.dcsa.conformance.standards.booking.party.BookingCancellationState.*;
import static org.dcsa.conformance.standards.booking.party.BookingState.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.booking.action.*;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingState;

@Slf4j
class BookingScenarioListBuilder extends ScenarioListBuilder<BookingScenarioListBuilder> {

  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private static final ThreadLocal<BookingComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalShipperPartyName = new ThreadLocal<>();

  private static final String BOOKING_API = "api";
  private static final String BOOKING_NOTIFICATIONS_API = "api";
  private static final String CREATE_BOOKING_SCHEMA_NAME = "CreateBooking";
  private static final String GET_BOOKING_SCHEMA_NAME = "Booking";
  private static final String UPDATE_BOOKING_SCHEMA_NAME = "UpdateBooking";
  private static final String BOOKING_202_RESPONSE_SCHEMA = "CreateBookingResponse";
  private static final String CANCEL_SCHEMA_NAME = "CancelBookingRequest";
  private static final String BOOKING_NOTIFICATION_SCHEMA_NAME = "BookingNotification";

  private BookingScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, BookingScenarioListBuilder> createModuleScenarioListBuilders(
      BookingComponentFactory componentFactory, String carrierPartyName, String shipperPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);

    if (SCENARIO_SUITE_CONFORMANCE.equals(componentFactory.getScenarioSuite())) {
      return createConformanceScenarios(carrierPartyName);
    }

    throw new IllegalArgumentException(
        "Invalid scenario suite name '%s'".formatted(componentFactory.getScenarioSuite()));
  }

  private static Map<String, BookingScenarioListBuilder> createConformanceScenarios(
      String carrierPartyName) {
    return Stream.of(
            Map.entry(
                "Dry cargo",
                carrierSupplyScenarioParameters(carrierPartyName, ScenarioType.REGULAR)
                    .then(
                        uc1ShipperSubmitBookingRequest()
                            .then(
                                shipperGetBooking(RECEIVED)
                                    .thenEither(
                                        uc2CarrierRequestUpdateToBookingRequest()
                                            .then(
                                                shipperGetBooking(PENDING_UPDATE)
                                                    .then(
                                                        uc3ShipperSubmitUpdatedBookingRequest()
                                                            .then(
                                                                shipperGetBooking(UPDATE_RECEIVED)
                                                                    .then(
                                                                        uc5CarrierConfirmBookingRequest()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                        CONFIRMED)
                                                                                    .then(
                                                                                        uc12CarrierConfirmBookingCompleted()
                                                                                            .then(
                                                                                                shipperGetBooking(
                                                                                                    COMPLETED)))))))),
                                        uc3ShipperSubmitUpdatedBookingRequest()
                                            .then(
                                                shipperGetBooking(UPDATE_RECEIVED)
                                                    .then(
                                                        uc5CarrierConfirmBookingRequest()
                                                            .then(
                                                                shipperGetBooking(CONFIRMED)
                                                                    .thenEither(
                                                                        uc12CarrierConfirmBookingCompleted()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                    COMPLETED)),
                                                                        uc13ShipperCancelConfirmedBooking()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                        CONFIRMED,
                                                                                        null,
                                                                                        CANCELLATION_RECEIVED,
                                                                                        false)
                                                                                    .thenEither(
                                                                                        uc14aCarrierBookingCancellationConfirmed()
                                                                                            .then(
                                                                                                shipperGetBooking(
                                                                                                    CANCELLED,
                                                                                                    null,
                                                                                                    CANCELLATION_CONFIRMED,
                                                                                                    false)),
                                                                                        uc14bCarrierBookingCancellationDeclined()
                                                                                            .then(
                                                                                                shipperGetBooking(
                                                                                                        CONFIRMED,
                                                                                                        null,
                                                                                                        CANCELLATION_DECLINED,
                                                                                                        false)
                                                                                                    .then(
                                                                                                        uc12CarrierConfirmBookingCompleted()
                                                                                                            .then(
                                                                                                                shipperGetBooking(
                                                                                                                    COMPLETED)))))))))),
                                        uc4CarrierRejectBookingRequest()
                                            .then(shipperGetBooking(REJECTED)),
                                        uc5CarrierConfirmBookingRequest()
                                            .then(
                                                shipperGetBooking(CONFIRMED)
                                                    .thenEither(
                                                        uc5CarrierConfirmBookingRequest()
                                                            .then(
                                                                shipperGetBooking(CONFIRMED)
                                                                    .then(
                                                                        uc12CarrierConfirmBookingCompleted()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                    COMPLETED)))),
                                                        uc6CarrierRequestToAmendConfirmedBooking()
                                                            .then(
                                                                shipperGetBooking(PENDING_AMENDMENT)
                                                                    .then(
                                                                        uc7ShipperSubmitBookingAmendment(
                                                                                PENDING_AMENDMENT)
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                        PENDING_AMENDMENT,
                                                                                        AMENDMENT_RECEIVED,
                                                                                        null,
                                                                                        true)
                                                                                    .thenEither(
                                                                                        uc8aCarrierApproveBookingAmendment()
                                                                                            .then(
                                                                                                shipperGetBooking(
                                                                                                        CONFIRMED,
                                                                                                        AMENDMENT_CONFIRMED)
                                                                                                    .then(
                                                                                                        uc12CarrierConfirmBookingCompleted()
                                                                                                            .then(
                                                                                                                shipperGetBooking(
                                                                                                                    COMPLETED)))))))),
                                                        uc7ShipperSubmitBookingAmendment(CONFIRMED)
                                                            .then(
                                                                shipperGetBooking(
                                                                        CONFIRMED,
                                                                        AMENDMENT_RECEIVED,
                                                                        null,
                                                                        true)
                                                                    .thenEither(
                                                                        uc8aCarrierApproveBookingAmendment()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                        CONFIRMED,
                                                                                        AMENDMENT_CONFIRMED)
                                                                                    .then(
                                                                                        uc12CarrierConfirmBookingCompleted()
                                                                                            .then(
                                                                                                shipperGetBooking(
                                                                                                    COMPLETED)))),
                                                                        uc8bCarrierDeclineBookingAmendment()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                        CONFIRMED,
                                                                                        AMENDMENT_DECLINED)
                                                                                    .then(
                                                                                        uc12CarrierConfirmBookingCompleted()
                                                                                            .then(
                                                                                                shipperGetBooking(
                                                                                                    COMPLETED)))),
                                                                        uc9ShipperCancelBookingAmendment()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                        CONFIRMED,
                                                                                        AMENDMENT_CANCELLED)
                                                                                    .then(
                                                                                        uc12CarrierConfirmBookingCompleted()
                                                                                            .then(
                                                                                                shipperGetBooking(
                                                                                                    COMPLETED)))))),
                                                        uc10CarrierDeclineBooking()
                                                            .then(shipperGetBooking(DECLINED)),
                                                        uc12CarrierConfirmBookingCompleted()
                                                            .then(shipperGetBooking(COMPLETED)))),
                                        uc11ShipperCancelBooking()
                                            .then(shipperGetBooking(CANCELLED)))))),
            Map.entry(
                "Routing Reference",
                carrierSupplyScenarioParameters(carrierPartyName, ScenarioType.ROUTING_REFERENCE)
                    .then(
                        uc1ShipperSubmitBookingRequest()
                            .then(
                                shipperGetBooking(RECEIVED)
                                    .thenEither(
                                        uc3ShipperSubmitUpdatedBookingRequest()
                                            .then(
                                                shipperGetBooking(UPDATE_RECEIVED)
                                                    .then(
                                                        uc5CarrierConfirmBookingRequest()
                                                            .then(
                                                                shipperGetBooking(CONFIRMED)
                                                                    .then(
                                                                        uc12CarrierConfirmBookingCompleted()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                    COMPLETED)))))),
                                        uc5CarrierConfirmBookingRequest()
                                            .then(
                                                shipperGetBooking(CONFIRMED)
                                                    .then(
                                                        uc12CarrierConfirmBookingCompleted()
                                                            .then(
                                                                shipperGetBooking(COMPLETED)))))))),
            Map.entry(
                "Store Door at origin",
                carrierSupplyScenarioParameters(carrierPartyName, ScenarioType.STORE_DOOR_AT_ORIGIN)
                    .then(
                        uc1ShipperSubmitBookingRequest()
                            .then(
                                shipperGetBooking(RECEIVED)
                                    .thenEither(
                                        uc3ShipperSubmitUpdatedBookingRequest()
                                            .then(
                                                shipperGetBooking(UPDATE_RECEIVED)
                                                    .then(
                                                        uc5CarrierConfirmBookingRequest()
                                                            .then(
                                                                shipperGetBooking(CONFIRMED)
                                                                    .then(
                                                                        uc12CarrierConfirmBookingCompleted()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                    COMPLETED)))))),
                                        uc5CarrierConfirmBookingRequest()
                                            .then(
                                                shipperGetBooking(CONFIRMED)
                                                    .then(
                                                        uc12CarrierConfirmBookingCompleted()
                                                            .then(
                                                                shipperGetBooking(COMPLETED)))))))),
            Map.entry(
                "Store Door at destination",
                carrierSupplyScenarioParameters(carrierPartyName, ScenarioType.STORE_DOOR_AT_DESTINATION)
                    .then(
                        uc1ShipperSubmitBookingRequest()
                            .then(
                                shipperGetBooking(RECEIVED)
                                    .thenEither(
                                        uc3ShipperSubmitUpdatedBookingRequest()
                                            .then(
                                                shipperGetBooking(UPDATE_RECEIVED)
                                                    .then(
                                                        uc5CarrierConfirmBookingRequest()
                                                            .then(
                                                                shipperGetBooking(CONFIRMED)
                                                                    .then(
                                                                        uc12CarrierConfirmBookingCompleted()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                    COMPLETED)))))),
                                        uc5CarrierConfirmBookingRequest()
                                            .then(
                                                shipperGetBooking(CONFIRMED)
                                                    .then(
                                                        uc12CarrierConfirmBookingCompleted()
                                                            .then(
                                                                shipperGetBooking(COMPLETED)))))))),
            Map.entry(
                "Dangerous goods",
                carrierSupplyScenarioParameters(carrierPartyName, ScenarioType.DG)
                    .then(
                        uc1ShipperSubmitBookingRequest()
                            .then(
                                shipperGetBooking(RECEIVED)
                                    .thenEither(
                                        uc3ShipperSubmitUpdatedBookingRequest()
                                            .then(
                                                shipperGetBooking(UPDATE_RECEIVED)
                                                    .then(
                                                        uc5CarrierConfirmBookingRequest()
                                                            .then(
                                                                shipperGetBooking(CONFIRMED)
                                                                    .then(
                                                                        uc12CarrierConfirmBookingCompleted()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                    COMPLETED)))))),
                                        uc5CarrierConfirmBookingRequest()
                                            .then(
                                                shipperGetBooking(CONFIRMED)
                                                    .then(
                                                        uc12CarrierConfirmBookingCompleted()
                                                            .then(
                                                                shipperGetBooking(COMPLETED)))))))),
            Map.entry(
                "Reefer containers",
                carrierSupplyScenarioParameters(carrierPartyName, ScenarioType.REEFER)
                    .then(
                        uc1ShipperSubmitBookingRequest()
                            .then(
                                shipperGetBooking(RECEIVED)
                                    .thenEither(
                                        uc3ShipperSubmitUpdatedBookingRequest()
                                            .then(
                                                shipperGetBooking(UPDATE_RECEIVED)
                                                    .then(
                                                        uc5CarrierConfirmBookingRequest()
                                                            .then(
                                                                shipperGetBooking(CONFIRMED)
                                                                    .then(
                                                                        uc12CarrierConfirmBookingCompleted()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                    COMPLETED)))))),
                                        uc5CarrierConfirmBookingRequest()
                                            .then(
                                                shipperGetBooking(CONFIRMED)
                                                    .then(
                                                        uc12CarrierConfirmBookingCompleted()
                                                            .then(
                                                                shipperGetBooking(COMPLETED)))))))),
            Map.entry(
                "Non Reefer containers",
                carrierSupplyScenarioParameters(carrierPartyName, ScenarioType.NON_OPERATING_REEFER)
                    .then(
                        uc1ShipperSubmitBookingRequest()
                            .then(
                                shipperGetBooking(RECEIVED)
                                    .thenEither(
                                        uc3ShipperSubmitUpdatedBookingRequest()
                                            .then(
                                                shipperGetBooking(UPDATE_RECEIVED)
                                                    .then(
                                                        uc5CarrierConfirmBookingRequest()
                                                            .then(
                                                                shipperGetBooking(CONFIRMED)
                                                                    .then(
                                                                        uc12CarrierConfirmBookingCompleted()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                    COMPLETED)))))),
                                        uc5CarrierConfirmBookingRequest()
                                            .then(
                                                shipperGetBooking(CONFIRMED)
                                                    .then(
                                                        uc12CarrierConfirmBookingCompleted()
                                                            .then(
                                                                shipperGetBooking(COMPLETED)))))))),
            Map.entry(
                "Carrier error response conformance",
                carrierSupplyScenarioParameters(carrierPartyName, ScenarioType.REGULAR)
                    .then(uc1ShipperSubmitBookingRequest().then(shipperGetBookingErrorScenario()))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static BookingScenarioListBuilder carrierSupplyScenarioParameters(
      String carrierPartyName, ScenarioType scenarioType) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new CarrierSupplyScenarioParametersAction(
                carrierPartyName,
                scenarioType,
                componentFactory.getStandardVersion(),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_API, CREATE_BOOKING_SCHEMA_NAME)));
  }

  private static BookingScenarioListBuilder shipperGetBooking(BookingState expectedBookingStatus) {
    return shipperGetBooking(expectedBookingStatus, null);
  }

  private static BookingScenarioListBuilder shipperGetBooking(
      BookingState expectedBookingStatus, BookingState expectedAmendedBookingStatus) {
    return shipperGetBooking(expectedBookingStatus, expectedAmendedBookingStatus, null, false);
  }

  private static BookingScenarioListBuilder shipperGetBooking(
      BookingState expectedBookingStatus,
      BookingState expectedAmendedBookingStatus,
      BookingCancellationState expectedCancellationState,
      boolean requestAmendedContent) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new ShipperGetBookingAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                expectedBookingStatus,
                expectedAmendedBookingStatus,
                expectedCancellationState,
                componentFactory.getMessageSchemaValidator(BOOKING_API, GET_BOOKING_SCHEMA_NAME),
                requestAmendedContent));
  }

  private static BookingScenarioListBuilder shipperGetBookingErrorScenario() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new ShipperGetBookingErrorScenarioAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                componentFactory.getMessageSchemaValidator(BOOKING_API, "ErrorResponse")));
  }

  private static BookingScenarioListBuilder uc1ShipperSubmitBookingRequest() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC1_Shipper_SubmitBookingRequestAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                componentFactory.getMessageSchemaValidator(BOOKING_API, CREATE_BOOKING_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingScenarioListBuilder uc2CarrierRequestUpdateToBookingRequest() {
    return carrierStateChange(UC2_Carrier_RequestUpdateToBookingRequestAction::new);
  }

  private static BookingScenarioListBuilder uc3ShipperSubmitUpdatedBookingRequest() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC3_Shipper_SubmitUpdatedBookingRequestAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                BookingState.UPDATE_RECEIVED,
                componentFactory.getMessageSchemaValidator(BOOKING_API, UPDATE_BOOKING_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingScenarioListBuilder uc4CarrierRejectBookingRequest() {
    return carrierStateChange(UC4_Carrier_RejectBookingRequestAction::new);
  }

  private static BookingScenarioListBuilder uc5CarrierConfirmBookingRequest() {
    return carrierStateChange(UC5_Carrier_ConfirmBookingRequestAction::new);
  }

  private static BookingScenarioListBuilder uc6CarrierRequestToAmendConfirmedBooking() {
    return carrierStateChange(UC6_Carrier_RequestToAmendConfirmedBookingAction::new);
  }

  private static BookingScenarioListBuilder uc7ShipperSubmitBookingAmendment(
      BookingState bookingState) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC7_Shipper_SubmitBookingAmendment(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                bookingState,
                BookingState.AMENDMENT_RECEIVED,
                componentFactory.getMessageSchemaValidator(BOOKING_API, UPDATE_BOOKING_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingScenarioListBuilder uc8aCarrierApproveBookingAmendment() {
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

  private static BookingScenarioListBuilder uc8bCarrierDeclineBookingAmendment() {
    return carrierStateChange(
        (carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator) ->
            new UC8_Carrier_ProcessAmendmentAction(
                carrierPartyName,
                shipperPartyName,
                previousAction,
                BookingState.CONFIRMED,
                BookingState.AMENDMENT_DECLINED,
                requestSchemaValidator,
                false));
  }

  private static BookingScenarioListBuilder uc9ShipperCancelBookingAmendment() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC9_Shipper_CancelBookingAmendment(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                BookingState.CONFIRMED,
                BookingState.AMENDMENT_CANCELLED,
                componentFactory.getMessageSchemaValidator(BOOKING_API, CANCEL_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingScenarioListBuilder uc10CarrierDeclineBooking() {
    return carrierStateChange(
        (carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator) ->
            new UC10_Carrier_DeclineBookingAction(
                carrierPartyName, shipperPartyName, previousAction, null, requestSchemaValidator));
  }

  private static BookingScenarioListBuilder uc11ShipperCancelBooking() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC11_Shipper_CancelBookingRequestAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                BookingState.CANCELLED,
                componentFactory.getMessageSchemaValidator(BOOKING_API, CANCEL_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingScenarioListBuilder uc12CarrierConfirmBookingCompleted() {
    return carrierStateChange(UC12_Carrier_ConfirmBookingCompletedAction::new);
  }

  private static BookingScenarioListBuilder uc13ShipperCancelConfirmedBooking() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC13ShipperCancelConfirmedBookingAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                BookingState.CONFIRMED,
                null,
                BookingCancellationState.CANCELLATION_RECEIVED,
                componentFactory.getMessageSchemaValidator(BOOKING_API, CANCEL_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingScenarioListBuilder uc14aCarrierBookingCancellationConfirmed() {
    return carrierStateChange(
        (carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator) ->
            new UC14CarrierProcessBookingCancellationAction(
                carrierPartyName,
                shipperPartyName,
                previousAction,
                BookingState.CANCELLED,
                null,
                requestSchemaValidator,
                true));
  }

  private static BookingScenarioListBuilder uc14bCarrierBookingCancellationDeclined() {
    return carrierStateChange(
        (carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator) ->
            new UC14CarrierProcessBookingCancellationAction(
                carrierPartyName,
                shipperPartyName,
                previousAction,
                BookingState.CONFIRMED,
                null,
                requestSchemaValidator,
                false));
  }

  private static BookingScenarioListBuilder carrierStateChange(
      CarrierNotificationUseCase constructor) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            constructor.newInstance(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private interface CarrierNotificationUseCase {
    BookingAction newInstance(
        String carrierPartyName,
        String shipperPartyName,
        BookingAction previousAction,
        JsonSchemaValidator requestSchemaValidator);
  }
}
