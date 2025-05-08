package org.dcsa.conformance.standards.booking;

import static org.dcsa.conformance.standards.booking.model.InvalidBookingMessageType.*;
import static org.dcsa.conformance.standards.booking.party.BookingCancellationState.*;
import static org.dcsa.conformance.standards.booking.party.BookingState.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.booking.action.*;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.model.InvalidBookingMessageType;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingState;

@Slf4j
class BookingScenarioListBuilder extends ScenarioListBuilder<BookingScenarioListBuilder> {
  static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";
  static final String SCENARIO_SUITE_RI = "Reference Implementation";

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

  public static LinkedHashMap<String, BookingScenarioListBuilder> createModuleScenarioListBuilders(
      BookingComponentFactory componentFactory, String carrierPartyName, String shipperPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);

    if (SCENARIO_SUITE_CONFORMANCE.equals(componentFactory.getScenarioSuite())) {
      return createConformanceScenarios(carrierPartyName);
    }
    if (SCENARIO_SUITE_RI.equals(componentFactory.getScenarioSuite())) {
      return createReferenceImplementationScenarios(carrierPartyName);
    }
    throw new IllegalArgumentException("Invalid scenario suite name '%s'".formatted(componentFactory.getScenarioSuite()));
  }

  private static LinkedHashMap<String, BookingScenarioListBuilder> createConformanceScenarios(
      String carrierPartyName) {
    return Stream.of(
            Map.entry(
                "Dry cargo",
                carrier_SupplyScenarioParameters(carrierPartyName, ScenarioType.REGULAR)
                    .then(
                        uc1_shipper_SubmitBookingRequest()
                            .then(
                                shipperGetBooking(RECEIVED)
                                    .thenEither(
                                        uc2_carrier_requestUpdateToBookingRequest()
                                            .then(
                                                shipperGetBooking(PENDING_UPDATE)
                                                    .then(
                                                        uc3_shipper_submitUpdatedBookingRequest(
                                                                UPDATE_RECEIVED)
                                                            .then(
                                                                shipperGetBooking(UPDATE_RECEIVED)
                                                                    .then(
                                                                        uc5_carrier_confirmBookingRequest()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                        CONFIRMED)
                                                                                    .then(
                                                                                        uc12_carrier_confirmBookingCompleted()
                                                                                            .then(
                                                                                                shipperGetBooking(
                                                                                                    COMPLETED)))))))),
                                        uc3_shipper_submitUpdatedBookingRequest(UPDATE_RECEIVED)
                                            .then(
                                                shipperGetBooking(UPDATE_RECEIVED)
                                                    .then(
                                                        uc5_carrier_confirmBookingRequest()
                                                            .then(
                                                                shipperGetBooking(CONFIRMED)
                                                                    .then(
                                                                        uc12_carrier_confirmBookingCompleted()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                    COMPLETED)))))),
                                        uc4_carrier_rejectBookingRequest()
                                            .then(shipperGetBooking(REJECTED)),
                                        uc5_carrier_confirmBookingRequest()
                                            .then(
                                                shipperGetBooking(CONFIRMED)
                                                    .thenEither(
                                                        uc5_carrier_confirmBookingRequest()
                                                            .then(
                                                                shipperGetBooking(CONFIRMED)
                                                                    .then(
                                                                        uc12_carrier_confirmBookingCompleted()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                    COMPLETED)))),
                                                        uc7_shipper_submitBookingAmendment(
                                                                CONFIRMED, AMENDMENT_RECEIVED)
                                                            .then(
                                                                shipperGetBooking(
                                                                        CONFIRMED,
                                                                        AMENDMENT_RECEIVED,
                                                                        null,
                                                                        true)
                                                                    .thenEither(
                                                                        uc8a_carrier_approveBookingAmendment(
                                                                                CONFIRMED,
                                                                                AMENDMENT_CONFIRMED)
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                        CONFIRMED,
                                                                                        AMENDMENT_CONFIRMED)
                                                                                    .then(
                                                                                        uc12_carrier_confirmBookingCompleted()
                                                                                            .then(
                                                                                                shipperGetBooking(
                                                                                                    COMPLETED)))),
                                                                        uc8b_carrier_declineBookingAmendment(
                                                                                CONFIRMED,
                                                                                AMENDMENT_DECLINED)
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                        CONFIRMED,
                                                                                        AMENDMENT_DECLINED)
                                                                                    .then(
                                                                                        uc12_carrier_confirmBookingCompleted()
                                                                                            .then(
                                                                                                shipperGetBooking(
                                                                                                    COMPLETED)))),
                                                                        uc9_shipper_cancelBookingAmendment(
                                                                                CONFIRMED,
                                                                                AMENDMENT_CANCELLED)
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                        CONFIRMED,
                                                                                        AMENDMENT_CANCELLED)
                                                                                    .then(
                                                                                        uc12_carrier_confirmBookingCompleted()
                                                                                            .then(
                                                                                                shipperGetBooking(
                                                                                                    COMPLETED)))))),
                                                        uc10_carrier_declineBooking(null)
                                                            .then(shipperGetBooking(DECLINED)),
                                                        uc12_carrier_confirmBookingCompleted()
                                                            .then(shipperGetBooking(COMPLETED)))),
                                        uc11_shipper_cancelBooking(CANCELLED)
                                            .then(shipperGetBooking(CANCELLED)))))),
            Map.entry(
                "Dangerous goods",
                carrier_SupplyScenarioParameters(carrierPartyName, ScenarioType.DG)
                    .then(
                        uc1_shipper_SubmitBookingRequest()
                            .then(
                                shipperGetBooking(RECEIVED)
                                    .thenEither(
                                        uc3_shipper_submitUpdatedBookingRequest(UPDATE_RECEIVED)
                                            .then(
                                                shipperGetBooking(UPDATE_RECEIVED)
                                                    .then(
                                                        uc5_carrier_confirmBookingRequest()
                                                            .then(
                                                                shipperGetBooking(CONFIRMED)
                                                                    .then(
                                                                        uc12_carrier_confirmBookingCompleted()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                    COMPLETED)))))),
                                        uc5_carrier_confirmBookingRequest()
                                            .then(
                                                shipperGetBooking(CONFIRMED)
                                                    .then(
                                                        uc12_carrier_confirmBookingCompleted()
                                                            .then(
                                                                shipperGetBooking(COMPLETED)))))))),
            Map.entry(
                "Reefer containers",
                carrier_SupplyScenarioParameters(carrierPartyName, ScenarioType.REEFER)
                    .then(
                        uc1_shipper_SubmitBookingRequest()
                            .then(
                                shipperGetBooking(RECEIVED)
                                    .thenEither(
                                        uc3_shipper_submitUpdatedBookingRequest(UPDATE_RECEIVED)
                                            .then(
                                                shipperGetBooking(UPDATE_RECEIVED)
                                                    .then(
                                                        uc5_carrier_confirmBookingRequest()
                                                            .then(
                                                                shipperGetBooking(CONFIRMED)
                                                                    .then(
                                                                        uc12_carrier_confirmBookingCompleted()
                                                                            .then(
                                                                                shipperGetBooking(
                                                                                    COMPLETED)))))),
                                        uc5_carrier_confirmBookingRequest()
                                            .then(
                                                shipperGetBooking(CONFIRMED)
                                                    .then(
                                                        uc12_carrier_confirmBookingCompleted()
                                                            .then(
                                                                shipperGetBooking(COMPLETED)))))))),
            Map.entry(
                "Error Case Scenario (for Carrier)",
                carrier_SupplyScenarioParameters(carrierPartyName, ScenarioType.REGULAR)
                    .then(
                        uc1_shipper_SubmitBookingRequest().then(shipperGetBookingErrorScenario()))),
            Map.entry(
                "Error Case Scenario (for Shipper)",
                carrier_SupplyScenarioParameters(carrierPartyName, ScenarioType.REGULAR)
                    .then(
                        uc1_shipper_SubmitBookingRequest()
                            .then(
                                shipperGetBooking(RECEIVED)
                                    .then(uc2_carrier_invalidBookingRequest())))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static LinkedHashMap<String, BookingScenarioListBuilder>
      createReferenceImplementationScenarios(String carrierPartyName) {
    return Stream.of(
            Map.entry(
                "",
                noAction()
                    .thenEither(
                        carrier_SupplyScenarioParameters(carrierPartyName, ScenarioType.REGULAR)
                            .thenAllPathsFrom(START),
                        carrier_SupplyScenarioParameters(
                                carrierPartyName, ScenarioType.REGULAR_2RE1C)
                            .thenHappyPathFrom(START),
                        carrier_SupplyScenarioParameters(
                                carrierPartyName, ScenarioType.REGULAR_2RE2C)
                            .thenHappyPathFrom(START),
                        carrier_SupplyScenarioParameters(
                                carrierPartyName, ScenarioType.REGULAR_CHO_DEST)
                            .thenHappyPathFrom(START),
                        carrier_SupplyScenarioParameters(
                                carrierPartyName, ScenarioType.REGULAR_CHO_ORIG)
                            .thenHappyPathFrom(START),
                        carrier_SupplyScenarioParameters(
                                carrierPartyName, ScenarioType.REGULAR_NON_OPERATING_REEFER)
                            .thenHappyPathFrom(START),
                        carrier_SupplyScenarioParameters(carrierPartyName, ScenarioType.REEFER)
                            .thenHappyPathFrom(START, ScenarioType.REEFER),
                        carrier_SupplyScenarioParameters(
                                carrierPartyName, ScenarioType.REEFER_TEMP_CHANGE)
                            .thenHappyPathFrom(START),
                        carrier_SupplyScenarioParameters(carrierPartyName, ScenarioType.DG)
                            .thenHappyPathFrom(START, ScenarioType.DG))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }
    private BookingScenarioListBuilder thenAllPathsFrom(BookingState bookingState) {
    return thenAllPathsFrom(bookingState, null);
  }

  private BookingScenarioListBuilder thenAllPathsFrom(
      BookingState bookingState, BookingState originalBookingState) {
    return switch (bookingState) {
      case COMPLETED  -> then(
        shipperGetBooking(bookingState).thenEither(
          noAction().thenHappyPathFrom(COMPLETED),
          auc_shipper_sendInvalidBookingAction(SUBMIT_BOOKING_AMENDMENT)
            .then(shipperGetBooking(COMPLETED)),
          auc_shipper_sendInvalidBookingAction(CANCEL_BOOKING)
            .then(shipperGetBooking(COMPLETED))
        )
      );
      case DECLINED, CANCELLED, REJECTED -> then(
        shipperGetBooking(bookingState).thenEither(
          noAction().thenHappyPathFrom(bookingState),
          auc_shipper_sendInvalidBookingAction(UPDATE_BOOKING)
            .then(shipperGetBooking(bookingState)),
          auc_shipper_sendInvalidBookingAction(SUBMIT_BOOKING_AMENDMENT)
            .then(shipperGetBooking(bookingState)),
          auc_shipper_sendInvalidBookingAction(CANCEL_BOOKING)
            .then(shipperGetBooking(bookingState))
        )
      );
      case CONFIRMED -> then(
        shipperGetBooking(bookingState)
              .thenEither(
                  uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED),
                  uc6_carrier_requestToAmendConfirmedBooking().thenAllPathsFrom(PENDING_AMENDMENT),
                  uc7_shipper_submitBookingAmendment(CONFIRMED, AMENDMENT_RECEIVED)
                      .thenAllPathsFrom(AMENDMENT_RECEIVED, CONFIRMED),
                  uc10_carrier_declineBooking(null).thenAllPathsFrom(DECLINED),
                  uc12_carrier_confirmBookingCompleted().thenAllPathsFrom(COMPLETED),
                  uc13ShipperCancelConfirmedBooking(CONFIRMED,null, CANCELLATION_RECEIVED)
                    .thenAllPathsFrom(CANCELLATION_RECEIVED, CONFIRMED, null)));
      case PENDING_UPDATE -> then(
        shipperGetBooking(bookingState)
              .thenEither(
                  uc2_carrier_requestUpdateToBookingRequest().thenHappyPathFrom(PENDING_UPDATE),
                  uc3_shipper_submitUpdatedBookingRequest(UPDATE_RECEIVED)
                      .thenHappyPathFrom(UPDATE_RECEIVED),
                  uc4_carrier_rejectBookingRequest().thenHappyPathFrom(REJECTED),
                  uc11_shipper_cancelBooking(CANCELLED).thenHappyPathFrom(CANCELLED)));
      case UPDATE_RECEIVED -> then(
        shipperGetBooking(bookingState)
              .thenEither(
                  uc2_carrier_requestUpdateToBookingRequest().thenHappyPathFrom(PENDING_UPDATE),
                  uc3_shipper_submitUpdatedBookingRequest(UPDATE_RECEIVED)
                      .thenHappyPathFrom(UPDATE_RECEIVED),
                  uc4_carrier_rejectBookingRequest().thenHappyPathFrom(REJECTED),
                  uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED),
                  uc11_shipper_cancelBooking(CANCELLED).thenHappyPathFrom(CANCELLED)));
      case RECEIVED -> then(
        shipperGetBooking(bookingState)
              .thenEither(
                auc_shipper_sendInvalidBookingAction(CANCEL_BOOKING_AMENDMENT).then(shipperGetBooking(bookingState)),
                uc2_carrier_requestUpdateToBookingRequest().thenAllPathsFrom(PENDING_UPDATE),
                uc3_shipper_submitUpdatedBookingRequest(UPDATE_RECEIVED)
                    .thenAllPathsFrom(UPDATE_RECEIVED),
                uc4_carrier_rejectBookingRequest().thenAllPathsFrom(REJECTED),
                uc5_carrier_confirmBookingRequest().thenAllPathsFrom(CONFIRMED),
                uc11_shipper_cancelBooking(CANCELLED).thenAllPathsFrom(CANCELLED)
              ));
      case START -> thenEither(
        uc1_shipper_SubmitBookingRequest().thenAllPathsFrom(RECEIVED));
      case PENDING_AMENDMENT -> then(
        shipperGetBooking(bookingState)
              .thenEither(
                uc6_carrier_requestToAmendConfirmedBooking().thenHappyPathFrom(PENDING_AMENDMENT),
                uc10_carrier_declineBooking(null).thenHappyPathFrom(DECLINED),
                uc11_shipper_cancelBooking(CANCELLED).thenHappyPathFrom(CANCELLED),
                uc13ShipperCancelConfirmedBooking(PENDING_AMENDMENT, null, CANCELLATION_RECEIVED).thenAllPathsFrom(CANCELLATION_RECEIVED, PENDING_AMENDMENT,null)));
      case AMENDMENT_RECEIVED -> then(
        originalBookingState.equals(PENDING_AMENDMENT) ?
          shipperGetBooking(PENDING_AMENDMENT, AMENDMENT_RECEIVED,null,true)
          .thenEither(
            uc7_shipper_submitBookingAmendment(CONFIRMED, AMENDMENT_RECEIVED)
              .thenAllPathsFrom(AMENDMENT_RECEIVED, CONFIRMED),
            uc8a_carrier_approveBookingAmendment(PENDING_AMENDMENT, AMENDMENT_CONFIRMED ).thenAllPathsFrom(AMENDMENT_CONFIRMED, PENDING_AMENDMENT),
            uc8b_carrier_declineBookingAmendment(PENDING_AMENDMENT, AMENDMENT_DECLINED ).thenAllPathsFrom(AMENDMENT_DECLINED, PENDING_AMENDMENT),
            uc9_shipper_cancelBookingAmendment(originalBookingState, AMENDMENT_CANCELLED).thenAllPathsFrom(AMENDMENT_CANCELLED, PENDING_AMENDMENT),
            uc10_carrier_declineBooking(AMENDMENT_DECLINED).thenHappyPathFrom(DECLINED),
            uc13ShipperCancelConfirmedBooking(originalBookingState, AMENDMENT_RECEIVED, CANCELLATION_RECEIVED).thenAllPathsFrom(CANCELLATION_RECEIVED, PENDING_AMENDMENT, AMENDMENT_RECEIVED )
        ) :
          shipperGetBooking(CONFIRMED, AMENDMENT_RECEIVED,null,true)
            .thenEither(
              uc6_carrier_requestToAmendConfirmedBooking().thenHappyPathFrom(PENDING_AMENDMENT),
              uc8a_carrier_approveBookingAmendment(CONFIRMED, AMENDMENT_CONFIRMED).thenAllPathsFrom(AMENDMENT_CONFIRMED, CONFIRMED),
              uc8b_carrier_declineBookingAmendment(CONFIRMED, AMENDMENT_DECLINED).thenAllPathsFrom(AMENDMENT_DECLINED, CONFIRMED),
              uc9_shipper_cancelBookingAmendment(CONFIRMED, AMENDMENT_CANCELLED),
              uc10_carrier_declineBooking(AMENDMENT_DECLINED).thenHappyPathFrom(DECLINED),
              uc13ShipperCancelConfirmedBooking(CONFIRMED, AMENDMENT_RECEIVED, CANCELLATION_RECEIVED).thenAllPathsFrom(CANCELLATION_RECEIVED, CONFIRMED, AMENDMENT_RECEIVED )
            )
      );
      case AMENDMENT_CONFIRMED -> then(
        shipperGetBooking(CONFIRMED,bookingState)
          .thenEither(
            auc_shipper_sendInvalidBookingAction(CANCEL_BOOKING_AMENDMENT)
              .then(shipperGetBooking(CONFIRMED,bookingState)),
            uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED),
            uc12_carrier_confirmBookingCompleted().then(shipperGetBooking(COMPLETED)),
            uc13ShipperCancelConfirmedBooking(CONFIRMED, AMENDMENT_CONFIRMED, CANCELLATION_RECEIVED).thenAllPathsFrom(CANCELLATION_RECEIVED, originalBookingState, AMENDMENT_CONFIRMED )
          )
      );
      case AMENDMENT_CANCELLED, AMENDMENT_DECLINED -> then(
        originalBookingState.equals(PENDING_AMENDMENT) ?
          shipperGetBooking(PENDING_AMENDMENT,bookingState).thenEither(
          noAction().thenHappyPathFrom(originalBookingState),
          uc6_carrier_requestToAmendConfirmedBooking().thenHappyPathFrom(originalBookingState),
          auc_shipper_sendInvalidBookingAction(CANCEL_BOOKING_AMENDMENT)
            .then(shipperGetBooking(PENDING_AMENDMENT,bookingState)),
          uc13ShipperCancelConfirmedBooking(PENDING_AMENDMENT, bookingState, CANCELLATION_RECEIVED).thenAllPathsFrom(CANCELLATION_RECEIVED, PENDING_AMENDMENT, bookingState )
        ):
          shipperGetBooking(CONFIRMED,bookingState).thenEither(
          noAction().thenHappyPathFrom(originalBookingState),
          uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED),
          auc_shipper_sendInvalidBookingAction(CANCEL_BOOKING_AMENDMENT)
            .then(shipperGetBooking(CONFIRMED,bookingState)),
          uc13ShipperCancelConfirmedBooking(CONFIRMED, bookingState, CANCELLATION_RECEIVED).thenAllPathsFrom(CANCELLATION_RECEIVED, CONFIRMED, bookingState )
        )
      );
    };
  }

  private BookingScenarioListBuilder thenAllPathsFrom(BookingCancellationState cancellationStatus, BookingState bookingStatus, BookingState amendedBookingStatus) {
    return switch (cancellationStatus) {
      case CANCELLATION_RECEIVED -> then (
        shipperGetBooking(bookingStatus, amendedBookingStatus, CANCELLATION_RECEIVED).thenEither(
          uc14CarrierBookingCancellationConfirmed(bookingStatus, amendedBookingStatus).thenAllPathsFrom(CANCELLATION_CONFIRMED, bookingStatus, amendedBookingStatus),
          uc14CarrierBookingCancellationDeclined(bookingStatus, amendedBookingStatus).thenAllPathsFrom(CANCELLATION_DECLINED, bookingStatus, amendedBookingStatus))
      );
      case CANCELLATION_CONFIRMED -> then (
        shipperGetBooking(bookingStatus, amendedBookingStatus, CANCELLATION_CONFIRMED));
      case CANCELLATION_DECLINED -> then(
        shipperGetBooking(bookingStatus, amendedBookingStatus, CANCELLATION_DECLINED)
      );
    };
  }

  private BookingScenarioListBuilder thenHappyPathFrom(BookingState bookingState) {
    return thenHappyPathFrom(bookingState, null);
  }

  private BookingScenarioListBuilder thenHappyPathFrom(BookingState bookingState, ScenarioType scenarioType) {
    return thenHappyPathFrom(bookingState, null, scenarioType);
  }

  private BookingScenarioListBuilder thenHappyPathFrom(BookingState bookingState, BookingState originalBookingState, ScenarioType scenarioType) {
    return switch (bookingState) {
      case CANCELLED, COMPLETED, DECLINED, REJECTED -> then(shipperGetBooking(bookingState));
      case AMENDMENT_CONFIRMED -> then(
        shipperGetBooking(CONFIRMED).then(
          uc12_carrier_confirmBookingCompleted().thenHappyPathFrom(COMPLETED)));
      case CONFIRMED -> then(
        shipperGetBooking(bookingState).then(
          uc12_carrier_confirmBookingCompleted().thenHappyPathFrom(COMPLETED)));
      case PENDING_AMENDMENT -> then(
        shipperGetBooking(PENDING_AMENDMENT).then(
          uc7_shipper_submitBookingAmendment(PENDING_AMENDMENT, AMENDMENT_RECEIVED).thenHappyPathFrom(AMENDMENT_RECEIVED,PENDING_AMENDMENT,scenarioType)));
      case AMENDMENT_RECEIVED -> then(
        shipperGetBooking(originalBookingState,AMENDMENT_RECEIVED).then(
          uc8a_carrier_approveBookingAmendment(CONFIRMED, AMENDMENT_CONFIRMED ).thenHappyPathFrom(AMENDMENT_CONFIRMED,CONFIRMED,scenarioType)));
      case PENDING_UPDATE -> then(
        shipperGetBooking(PENDING_UPDATE).then(
          uc3_shipper_submitUpdatedBookingRequest(UPDATE_RECEIVED).thenHappyPathFrom(UPDATE_RECEIVED, scenarioType)));
      case UPDATE_RECEIVED -> then(
        shipperGetBooking(bookingState).thenEither(
          uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED, scenarioType)));
      case RECEIVED -> then(
        isScenarioTypeDGorReefer(scenarioType)
          ? shipperGetBooking(bookingState).thenEither
            (
              uc2_carrier_requestUpdateToBookingRequest().thenHappyPathFrom(PENDING_UPDATE, scenarioType),
              uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED, scenarioType)
            ):
          shipperGetBooking(bookingState).then
            (
              uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED, scenarioType)
            )
        );
      case START -> then(uc1_shipper_SubmitBookingRequest().thenHappyPathFrom(RECEIVED, scenarioType));
      case AMENDMENT_DECLINED, AMENDMENT_CANCELLED -> throw new AssertionError(
        "This happyPath from this state requires a context state");
    };
  }

  private boolean isScenarioTypeDGorReefer(ScenarioType scenarioType) {
    return ScenarioType.DG.equals(scenarioType) || ScenarioType.REEFER.equals(scenarioType);
  }

  private BookingScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static BookingScenarioListBuilder noAction() {
    return new BookingScenarioListBuilder(null);
  }

  private static BookingScenarioListBuilder carrier_SupplyScenarioParameters(String carrierPartyName, ScenarioType scenarioType) {
    return new BookingScenarioListBuilder(
        previousAction -> new Carrier_SupplyScenarioParametersAction(carrierPartyName, scenarioType));
  }


  private static BookingScenarioListBuilder shipperGetBooking(BookingState expectedBookingStatus) {
    return shipperGetBooking(expectedBookingStatus, null);
  }

  private static BookingScenarioListBuilder shipperGetBooking(
      BookingState expectedBookingStatus, BookingState expectedAmendedBookingStatus) {
    return shipperGetBooking(expectedBookingStatus, expectedAmendedBookingStatus,null, false);
  }


  //TODO::
  private static BookingScenarioListBuilder shipperGetBooking(
    BookingState expectedBookingStatus, BookingState expectedAmendedBookingStatus, BookingCancellationState bookingCancellationStatus) {
    return shipperGetBooking(expectedBookingStatus, expectedAmendedBookingStatus,null, false);
  }

  private static BookingScenarioListBuilder shipperGetBooking(
    BookingState expectedBookingStatus, BookingState expectedAmendedBookingStatus, BookingCancellationState expectedCancellationState, boolean requestAmendedContent) {
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

  private static BookingScenarioListBuilder uc1_shipper_SubmitBookingRequest() {
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
          componentFactory.getMessageSchemaValidator(BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
          componentFactory.getMessageSchemaValidator(
            BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
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

  private static BookingScenarioListBuilder uc2_carrier_invalidBookingRequest() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC2_Carrier_RequestUpdateToInvalidBookingRequestAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(BOOKING_API, "ErrorResponse")));
  }

  private static BookingScenarioListBuilder uc2_carrier_requestUpdateToBookingRequest() {
    return carrierStateChange(UC2_Carrier_RequestUpdateToBookingRequestAction::new);
  }

  private static BookingScenarioListBuilder uc3_shipper_submitUpdatedBookingRequest(BookingState expectedBookingState) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC3_Shipper_SubmitUpdatedBookingRequestAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                expectedBookingState,
                componentFactory.getMessageSchemaValidator(BOOKING_API, UPDATE_BOOKING_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingScenarioListBuilder uc4_carrier_rejectBookingRequest() {
    return carrierStateChange(UC4_Carrier_RejectBookingRequestAction::new);
  }

  private static BookingScenarioListBuilder uc5_carrier_confirmBookingRequest() {
    return carrierStateChange(UC5_Carrier_ConfirmBookingRequestAction::new);
  }

  private static BookingScenarioListBuilder uc6_carrier_requestToAmendConfirmedBooking() {
    return carrierStateChange(UC6_Carrier_RequestToAmendConfirmedBookingAction::new);
  }

  private static BookingScenarioListBuilder uc7_shipper_submitBookingAmendment(BookingState bookingState, BookingState amendedBookingState) {
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
                amendedBookingState,
                componentFactory.getMessageSchemaValidator(BOOKING_API, UPDATE_BOOKING_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingScenarioListBuilder uc8a_carrier_approveBookingAmendment(BookingState bookingStatus, BookingState amendedBookingStatus) {
    return carrierStateChange((carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator) ->
      new UC8_Carrier_ProcessAmendmentAction(carrierPartyName, shipperPartyName, previousAction, bookingStatus, amendedBookingStatus, requestSchemaValidator, true));
  }

  private static BookingScenarioListBuilder uc8b_carrier_declineBookingAmendment(BookingState bookingStatus, BookingState amendedBookingStatus) {
    return carrierStateChange((carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator) ->
        new UC8_Carrier_ProcessAmendmentAction(carrierPartyName, shipperPartyName, previousAction, bookingStatus, amendedBookingStatus, requestSchemaValidator, false));
  }

  private static BookingScenarioListBuilder uc9_shipper_cancelBookingAmendment(BookingState bookingStatus, BookingState amendedBookingStatus) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
      previousAction ->
        new UC9_Shipper_CancelBookingAmendment(
          carrierPartyName,
          shipperPartyName,
          (BookingAction) previousAction,
          bookingStatus,
          amendedBookingStatus,
          componentFactory.getMessageSchemaValidator(BOOKING_API, CANCEL_SCHEMA_NAME),
          componentFactory.getMessageSchemaValidator(
            BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
          componentFactory.getMessageSchemaValidator(
            BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME))
      );
  }

  private static BookingScenarioListBuilder uc10_carrier_declineBooking(BookingState amendedBookingStatus) {
    return carrierStateChange((carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator) ->
      new UC10_Carrier_DeclineBookingAction(carrierPartyName, shipperPartyName, previousAction, amendedBookingStatus, requestSchemaValidator));
  }

  private static BookingScenarioListBuilder uc12_carrier_confirmBookingCompleted() {
    return carrierStateChange(UC12_Carrier_ConfirmBookingCompletedAction::new);
  }

  private static BookingScenarioListBuilder uc11_shipper_cancelBooking(BookingState expectedBookingStatus) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC11_Shipper_CancelBookingRequestAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                expectedBookingStatus,
                componentFactory.getMessageSchemaValidator(BOOKING_API, CANCEL_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
              componentFactory.getMessageSchemaValidator(
                BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingScenarioListBuilder uc13ShipperCancelConfirmedBooking(BookingState bookingStatus, BookingState amendedBookingStatus, BookingCancellationState cancellationBookingStatus) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
      previousAction ->
        new UC13ShipperCancelConfirmedBookingAction(
          carrierPartyName,
          shipperPartyName,
          (BookingAction) previousAction,
          bookingStatus,
          amendedBookingStatus,
          cancellationBookingStatus,
          componentFactory.getMessageSchemaValidator(BOOKING_API, CANCEL_SCHEMA_NAME),
          componentFactory.getMessageSchemaValidator(
            BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
          componentFactory.getMessageSchemaValidator(
            BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingScenarioListBuilder uc14CarrierBookingCancellationConfirmed(BookingState bookingStatus, BookingState amendedBookingStatus) {
    return carrierStateChange((carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator) ->
      new UC14CarrierProcessBookingCancellationAction(carrierPartyName, shipperPartyName, previousAction, bookingStatus, amendedBookingStatus, requestSchemaValidator, true));
  }

  private static BookingScenarioListBuilder uc14CarrierBookingCancellationDeclined(BookingState bookingStatus, BookingState amendedBookingStatus) {
    return carrierStateChange((carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator) ->
      new UC14CarrierProcessBookingCancellationAction(carrierPartyName, shipperPartyName, previousAction, bookingStatus, amendedBookingStatus, requestSchemaValidator, false));
  }

  private static BookingScenarioListBuilder auc_shipper_sendInvalidBookingAction(InvalidBookingMessageType invalidBookingMessageType) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
      previousAction -> {
        var schema = switch (invalidBookingMessageType) {
          case CANCEL_BOOKING_AMENDMENT, CANCEL_BOOKING -> CANCEL_SCHEMA_NAME;
          case UPDATE_BOOKING, SUBMIT_BOOKING_AMENDMENT ->  UPDATE_BOOKING_SCHEMA_NAME;
        };
        return new AUC_Shipper_SendInvalidBookingAction(
          carrierPartyName,
          shipperPartyName,
          (BookingAction) previousAction,
          invalidBookingMessageType,
          componentFactory.getMessageSchemaValidator(BOOKING_API, schema));
      });
  }

  private interface CarrierNotificationUseCase {
    BookingAction newInstance(
        String carrierPartyName,
        String shipperPartyName,
        BookingAction previousAction,
        JsonSchemaValidator requestSchemaValidator);
  }
}
