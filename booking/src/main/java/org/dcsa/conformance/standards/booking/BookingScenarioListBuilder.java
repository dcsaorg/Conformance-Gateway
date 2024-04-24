package org.dcsa.conformance.standards.booking;

import static org.dcsa.conformance.standards.booking.model.InvalidBookingMessageType.*;
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
import org.dcsa.conformance.standards.booking.party.BookingState;

@Slf4j
public class BookingScenarioListBuilder extends ScenarioListBuilder<BookingScenarioListBuilder> {

  private static final ThreadLocal<BookingComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalShipperPartyName = new ThreadLocal<>();
  private static final String BOOKING_API = "api";
  private static final String BOOKING_NOTIFICATIONS_API = "notification";
  private static final String CREATE_BOOKING_SCHEMA_NAME = "CreateBooking";
  private static final String GET_BOOKING_SCHEMA_NAME = "Booking";
  private static final String UPDATE_BOOKING_SCHEMA_NAME = "UpdateBooking";
  private static final String BOOKING_REF_STATUS_SCHEMA = "BookingRefStatus";
  private static final String CANCEL_SCHEMA_NAME = "bookings_bookingReference_body";
  private static final String BOOKING_NOTIFICATION_SCHEMA_NAME = "BookingNotification";

  public static LinkedHashMap<String, BookingScenarioListBuilder> createModuleScenarioListBuilders(
      BookingComponentFactory componentFactory, String carrierPartyName, String shipperPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);
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
                            .thenHappyPathFrom(START),
                        carrier_SupplyScenarioParameters(
                                carrierPartyName, ScenarioType.REEFER_TEMP_CHANGE)
                            .thenHappyPathFrom(START),
                        carrier_SupplyScenarioParameters(carrierPartyName, ScenarioType.DG)
                            .thenHappyPathFrom(START))))
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
        shipper_GetBooking(bookingState).thenEither(
          noAction().thenHappyPathFrom(COMPLETED),
          auc_shipper_sendInvalidBookingAction(SUBMIT_BOOKING_AMENDMENT)
            .then(shipper_GetBooking(COMPLETED)),
          auc_shipper_sendInvalidBookingAction(CANCEL_BOOKING)
            .then(shipper_GetBooking(COMPLETED))
        )
      );
      case DECLINED, CANCELLED, REJECTED -> then(
        shipper_GetBooking(bookingState).thenEither(
          noAction().thenHappyPathFrom(bookingState),
          auc_shipper_sendInvalidBookingAction(UPDATE_BOOKING)
            .then(shipper_GetBooking(bookingState)),
          auc_shipper_sendInvalidBookingAction(SUBMIT_BOOKING_AMENDMENT)
            .then(shipper_GetBooking(bookingState)),
          auc_shipper_sendInvalidBookingAction(CANCEL_BOOKING)
            .then(shipper_GetBooking(bookingState))
        )
      );
      case CONFIRMED -> then(
          shipper_GetBooking(bookingState)
              .thenEither(
                  uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED),
                  uc6_carrier_requestToAmendConfirmedBooking().thenAllPathsFrom(PENDING_AMENDMENT),
                  uc7_shipper_submitBookingAmendment()
                      .thenAllPathsFrom(AMENDMENT_RECEIVED, CONFIRMED),
                  uc10_carrier_declineBooking().thenAllPathsFrom(DECLINED),
                  uc12_carrier_confirmBookingCompleted().thenAllPathsFrom(COMPLETED),
                  uc11_shipper_cancelBooking().thenHappyPathFrom(CANCELLED)));
      case PENDING_UPDATE -> then(
          shipper_GetBooking(bookingState)
              .thenEither(
                  uc2_carrier_requestUpdateToBookingRequest().thenHappyPathFrom(PENDING_UPDATE),
                  uc3_shipper_submitUpdatedBookingRequest()
                      .thenHappyPathFrom(UPDATE_RECEIVED),
                  uc4_carrier_rejectBookingRequest().thenHappyPathFrom(REJECTED),
                  uc11_shipper_cancelBooking().thenHappyPathFrom(CANCELLED)));
      case UPDATE_RECEIVED -> then(
          shipper_GetBooking(bookingState)
              .thenEither(
                  uc2_carrier_requestUpdateToBookingRequest().thenHappyPathFrom(PENDING_UPDATE),
                  uc3_shipper_submitUpdatedBookingRequest()
                      .thenHappyPathFrom(UPDATE_RECEIVED),
                  uc4_carrier_rejectBookingRequest().thenHappyPathFrom(REJECTED),
                  uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED),
                  uc11_shipper_cancelBooking().thenHappyPathFrom(CANCELLED)));
      case RECEIVED -> then(
          shipper_GetBooking(bookingState)
              .thenEither(
                auc_shipper_sendInvalidBookingAction(CANCEL_BOOKING_AMENDMENT).then(shipper_GetBooking(bookingState)),
                uc2_carrier_requestUpdateToBookingRequest().thenAllPathsFrom(PENDING_UPDATE),
                uc3_shipper_submitUpdatedBookingRequest()
                    .thenAllPathsFrom(UPDATE_RECEIVED),
                uc4_carrier_rejectBookingRequest().thenAllPathsFrom(REJECTED),
                uc5_carrier_confirmBookingRequest().thenAllPathsFrom(CONFIRMED),
                uc11_shipper_cancelBooking().thenAllPathsFrom(CANCELLED)));
      case START -> thenEither(
        uc1_shipper_SubmitBookingRequest().thenAllPathsFrom(RECEIVED));
      case PENDING_AMENDMENT -> then(
          shipper_GetBooking(bookingState)
              .thenEither(
                uc6_carrier_requestToAmendConfirmedBooking().thenHappyPathFrom(PENDING_AMENDMENT),
                uc7_shipper_submitBookingAmendment()
                    .thenAllPathsFrom(AMENDMENT_RECEIVED, PENDING_AMENDMENT),
                uc10_carrier_declineBooking().thenHappyPathFrom(DECLINED),
                uc11_shipper_cancelBooking().thenHappyPathFrom(CANCELLED)));
      case AMENDMENT_RECEIVED -> then(
        shipper_GetBooking(originalBookingState,AMENDMENT_RECEIVED)
          .thenEither(
            uc6_carrier_requestToAmendConfirmedBooking().thenHappyPathFrom(PENDING_AMENDMENT),
            uc8a_carrier_approveBookingAmendment().thenAllPathsFrom(AMENDMENT_CONFIRMED,originalBookingState),
            uc8b_carrier_declineBookingAmendment().thenAllPathsFrom(AMENDMENT_DECLINED,originalBookingState),
            uc9_shipper_cancelBookingAmendment().thenAllPathsFrom(AMENDMENT_CANCELLED,originalBookingState),
            uc10_carrier_declineBooking().thenHappyPathFrom(DECLINED)
        )
      );
      case AMENDMENT_CONFIRMED -> then(
        shipper_GetBooking(CONFIRMED,bookingState)
          .thenEither(
            auc_shipper_sendInvalidBookingAction(CANCEL_BOOKING_AMENDMENT)
              .then(shipper_GetBooking(CONFIRMED,bookingState)),
            uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED)
          )
      );
      case AMENDMENT_CANCELLED, AMENDMENT_DECLINED -> then(
        originalBookingState.equals(PENDING_AMENDMENT) ?
        shipper_GetBooking(PENDING_AMENDMENT,bookingState).thenEither(
          noAction().thenHappyPathFrom(originalBookingState),
          uc6_carrier_requestToAmendConfirmedBooking().thenHappyPathFrom(originalBookingState),
          auc_shipper_sendInvalidBookingAction(CANCEL_BOOKING_AMENDMENT)
            .then(shipper_GetBooking(PENDING_AMENDMENT,bookingState))
        ):
        shipper_GetBooking(CONFIRMED,bookingState).thenEither(
          noAction().thenHappyPathFrom(originalBookingState),
          uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED),
          auc_shipper_sendInvalidBookingAction(CANCEL_BOOKING_AMENDMENT)
            .then(shipper_GetBooking(CONFIRMED,bookingState))
        )
      );
    };
  }

  private BookingScenarioListBuilder thenHappyPathFrom(BookingState bookingState) {
    return thenHappyPathFrom(bookingState, null);
  }

  private BookingScenarioListBuilder thenHappyPathFrom(BookingState bookingState, BookingState originalBookingState) {
    return switch (bookingState) {
      case CANCELLED, COMPLETED, DECLINED, REJECTED -> then(shipper_GetBooking(bookingState));
      case AMENDMENT_CONFIRMED -> then(
        shipper_GetBooking(CONFIRMED).then(
          uc12_carrier_confirmBookingCompleted().thenHappyPathFrom(COMPLETED,null)));
      case CONFIRMED -> then(
        shipper_GetBooking(bookingState).then(
          uc12_carrier_confirmBookingCompleted().thenHappyPathFrom(COMPLETED,null)));
      case PENDING_AMENDMENT -> then(
        shipper_GetBooking(PENDING_AMENDMENT).then(
          uc7_shipper_submitBookingAmendment().thenHappyPathFrom(AMENDMENT_RECEIVED,PENDING_AMENDMENT)));
      case AMENDMENT_RECEIVED -> then(
        shipper_GetBooking(originalBookingState,AMENDMENT_RECEIVED).then(
          uc8a_carrier_approveBookingAmendment().thenHappyPathFrom(AMENDMENT_CONFIRMED,CONFIRMED)));
      case PENDING_UPDATE -> then(
        shipper_GetBooking(PENDING_UPDATE).then(
          uc3_shipper_submitUpdatedBookingRequest().thenHappyPathFrom(UPDATE_RECEIVED,null)));
      case UPDATE_RECEIVED, RECEIVED -> then(
        shipper_GetBooking(bookingState).then(
          uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED,null)));
      case START -> then(uc1_shipper_SubmitBookingRequest().thenHappyPathFrom(RECEIVED,null));
      case AMENDMENT_DECLINED, AMENDMENT_CANCELLED -> throw new AssertionError(
        "This happyPath from this state requires a context state");
    };
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


  private static BookingScenarioListBuilder shipper_GetBooking(BookingState expectedBookingStatus) {
    return shipper_GetBooking(expectedBookingStatus, null);
  }

  private static BookingScenarioListBuilder shipper_GetBooking(
      BookingState expectedBookingStatus, BookingState expectedAmendedBookingStatus) {
    return shipper_GetBooking(expectedBookingStatus, expectedAmendedBookingStatus, false);
  }

  private static BookingScenarioListBuilder shipper_GetBooking(
    BookingState expectedBookingStatus, BookingState expectedAmendedBookingStatus, boolean requestAmendedContent) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
      previousAction ->
        new Shipper_GetBookingAction(
          carrierPartyName,
          shipperPartyName,
          (BookingAction) previousAction,
          expectedBookingStatus,
          expectedAmendedBookingStatus,
          componentFactory.getMessageSchemaValidator(BOOKING_API, GET_BOOKING_SCHEMA_NAME),
          requestAmendedContent));
  }

  private static BookingScenarioListBuilder shipper_GetAmendedBooking404() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
      previousAction ->
        new Shipper_GetAmendedBooking404Action(
          carrierPartyName,
          shipperPartyName,
          (BookingAction) previousAction));
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
          componentFactory.getMessageSchemaValidator(BOOKING_API, BOOKING_REF_STATUS_SCHEMA),
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

  private static BookingScenarioListBuilder uc2_carrier_requestUpdateToBookingRequest() {
    return carrierStateChange(UC2_Carrier_RequestUpdateToBookingRequestAction::new);
  }

  private static BookingScenarioListBuilder uc3_shipper_submitUpdatedBookingRequest() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC3_Shipper_SubmitUpdatedBookingRequestAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                componentFactory.getMessageSchemaValidator(BOOKING_API, UPDATE_BOOKING_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(BOOKING_API, BOOKING_REF_STATUS_SCHEMA),
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

  private static BookingScenarioListBuilder uc7_shipper_submitBookingAmendment() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC7_Shipper_SubmitBookingAmendment(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                componentFactory.getMessageSchemaValidator(BOOKING_API, UPDATE_BOOKING_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(BOOKING_API, BOOKING_REF_STATUS_SCHEMA),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingScenarioListBuilder uc8a_carrier_approveBookingAmendment() {
    return carrierStateChange((carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator) ->
      new UC8_Carrier_ProcessAmendmentAction(carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator, true));
  }

  private static BookingScenarioListBuilder uc8b_carrier_declineBookingAmendment() {
    return carrierStateChange((carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator) ->
        new UC8_Carrier_ProcessAmendmentAction(carrierPartyName, shipperPartyName, previousAction, requestSchemaValidator, false));
  }

  private static BookingScenarioListBuilder uc9_shipper_cancelBookingAmendment() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
      previousAction ->
        new UC9_Shipper_CancelBookingAmendment(
          carrierPartyName,
          shipperPartyName,
          (BookingAction) previousAction,
          componentFactory.getMessageSchemaValidator(BOOKING_API, CANCEL_SCHEMA_NAME),
          componentFactory.getMessageSchemaValidator(
            BOOKING_API, BOOKING_REF_STATUS_SCHEMA),
          componentFactory.getMessageSchemaValidator(
            BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME))
      );
  }

  private static BookingScenarioListBuilder uc10_carrier_declineBooking() {
    return carrierStateChange(UC10_Carrier_DeclineBookingAction::new);
  }

  private static BookingScenarioListBuilder uc12_carrier_confirmBookingCompleted() {
    return carrierStateChange(UC12_Carrier_ConfirmBookingCompletedAction::new);
  }

  private static BookingScenarioListBuilder uc11_shipper_cancelBooking() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC11_Shipper_CancelEntireBookingAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                componentFactory.getMessageSchemaValidator(BOOKING_API, CANCEL_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_API, BOOKING_REF_STATUS_SCHEMA),
              componentFactory.getMessageSchemaValidator(
                BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
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
