package org.dcsa.conformance.standards.booking;

import static org.dcsa.conformance.standards.booking.party.BookingState.*;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.booking.action.*;
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standards.booking.party.BookingVariant;

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
  private static final String UPATE_BOOKING_SCHEMA_NAME = "UpdateBooking";
  private static final String BOOKING_REF_STATUS_SCHEMA = "BookingRefStatus";
  private static final String CANCEL_SCHEMA_NAME = "bookings_bookingReference_body";
  private static final String BOOKING_NOTIFICATION_SCHEMA_NAME = "BookingNotification";

  public static BookingScenarioListBuilder buildTree(
      BookingComponentFactory componentFactory, String carrierPartyName, String shipperPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);
    return carrier_SupplyScenarioParameters().thenAllPathsFrom(START);
  }

  private BookingScenarioListBuilder thenAllPathsFrom(BookingState bookingState) {
    return thenAllPathsFrom(bookingState, null);
  }

  private BookingScenarioListBuilder thenAllPathsFrom(
      BookingState bookingState, BookingState originalBookingState) {
    return switch (bookingState) {
      case CANCELLED, COMPLETED, DECLINED, REJECTED  -> then(shipper_GetBooking(bookingState));
      case CONFIRMED -> then(
          shipper_GetBooking(bookingState)
              .thenEither(
                  uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED),
                  uc6_carrier_requestUpdateToConfirmedBooking().thenAllPathsFrom(PENDING_AMENDMENT),
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
                  uc2_carrier_requestUpdateToBookingRequest().thenAllPathsFrom(PENDING_UPDATE),
                  uc3_shipper_submitUpdatedBookingRequest()
                      .thenAllPathsFrom(UPDATE_RECEIVED),
                  uc4_carrier_rejectBookingRequest().thenAllPathsFrom(REJECTED),
                  uc5_carrier_confirmBookingRequest().thenAllPathsFrom(CONFIRMED),
                  uc11_shipper_cancelBooking().thenAllPathsFrom(CANCELLED)));
      case START -> thenEither(
        uc1_shipper_SubmitBookingRequest(BookingVariant.REGULAR).thenAllPathsFrom(RECEIVED),
        uc1_shipper_SubmitBookingRequest(BookingVariant.REEFER).thenHappyPathFrom(RECEIVED));
      case PENDING_AMENDMENT -> then(
          shipper_GetBooking(bookingState)
              .thenEither(
                  uc6_carrier_requestUpdateToConfirmedBooking().thenHappyPathFrom(PENDING_AMENDMENT),
                  uc7_shipper_submitBookingAmendment()
                      .thenAllPathsFrom(AMENDMENT_RECEIVED, PENDING_AMENDMENT),
                  uc10_carrier_declineBooking().thenHappyPathFrom(DECLINED),
                  uc11_shipper_cancelBooking().thenHappyPathFrom(CANCELLED)));
      case AMENDMENT_RECEIVED -> then(
        shipper_GetBooking(originalBookingState,AMENDMENT_RECEIVED)
          .thenEither(
            uc6_carrier_requestUpdateToConfirmedBooking().thenHappyPathFrom(PENDING_AMENDMENT),
            uc8a_carrier_approveBookingAmendment().thenAllPathsFrom(AMENDMENT_CONFIRMED,originalBookingState),
            uc8b_carrier_declineBookingAmendment().thenAllPathsFrom(AMENDMENT_DECLINED,originalBookingState),
            uc9_shipper_cancelBookingAmendment().thenAllPathsFrom(AMENDMENT_CANCELLED,originalBookingState),
            uc10_carrier_declineBooking().thenHappyPathFrom(DECLINED)
        )
      );
      case AMENDMENT_CONFIRMED -> then(
        shipper_GetBooking(CONFIRMED,bookingState)
          .thenEither(
            noAction().thenHappyPathFrom(CONFIRMED),
            noAction().thenHappyPathFrom(PENDING_AMENDMENT)
          )
      );
      case AMENDMENT_CANCELLED, AMENDMENT_DECLINED -> then(
        shipper_GetBooking(originalBookingState,bookingState)
          .thenHappyPathFrom(originalBookingState)
      );
    };
  }

  private BookingScenarioListBuilder thenHappyPathFrom(BookingState bookingState) {
    return switch (bookingState) {
      case CANCELLED, COMPLETED, DECLINED, REJECTED -> then(noAction());
      case CONFIRMED, AMENDMENT_CONFIRMED -> then(
          uc12_carrier_confirmBookingCompleted().thenHappyPathFrom(COMPLETED));
      case PENDING_AMENDMENT -> then(
          uc7_shipper_submitBookingAmendment().thenHappyPathFrom(AMENDMENT_RECEIVED));
      case AMENDMENT_RECEIVED -> then(
          uc8a_carrier_approveBookingAmendment().thenHappyPathFrom(CONFIRMED));
      case PENDING_UPDATE -> then(
          uc3_shipper_submitUpdatedBookingRequest().thenHappyPathFrom(UPDATE_RECEIVED));
      case UPDATE_RECEIVED, RECEIVED -> then(
          uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED));
      case START -> then(uc1_shipper_SubmitBookingRequest(BookingVariant.REGULAR).thenHappyPathFrom(RECEIVED));
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

  private static BookingScenarioListBuilder carrier_SupplyScenarioParameters() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction -> new Carrier_SupplyScenarioParametersAction(carrierPartyName));
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

  private static BookingScenarioListBuilder uc1_shipper_SubmitBookingRequest(BookingVariant variant) {
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
            BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
          variant));
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
                componentFactory.getMessageSchemaValidator(BOOKING_API, UPATE_BOOKING_SCHEMA_NAME),
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

  private static BookingScenarioListBuilder uc6_carrier_requestUpdateToConfirmedBooking() {
    return carrierStateChange(UC6_Carrier_RequestUpdateToConfirmedBookingAction::new);
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
                componentFactory.getMessageSchemaValidator(BOOKING_API, UPATE_BOOKING_SCHEMA_NAME),
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
            BOOKING_API, BOOKING_REF_STATUS_SCHEMA)));
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
                    BOOKING_API, BOOKING_REF_STATUS_SCHEMA)));
  }

  private static BookingScenarioListBuilder tbdCarrierAction() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new BookingAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                "TbdCarrierAction",
                500) {
              @Override
              public String getHumanReadablePrompt() {
                return "TBD carrier action";
              }
            }) {};
  }

  private static BookingScenarioListBuilder tbdShipperAction() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new BookingAction(
                shipperPartyName,
                carrierPartyName,
                (BookingAction) previousAction,
                "TbdShipperAction",
                500) {
              @Override
              public String getHumanReadablePrompt() {
                return "TBD shipper action";
              }
            }) {};
  }

  private interface CarrierNotificationUseCase {
    BookingAction newInstance(
        String carrierPartyName,
        String shipperPartyName,
        BookingAction previousAction,
        JsonSchemaValidator requestSchemaValidator);
  }
}
