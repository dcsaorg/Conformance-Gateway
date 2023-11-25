package org.dcsa.conformance.standards.booking;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.booking.action.*;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.function.Function;

import static org.dcsa.conformance.standards.booking.party.BookingState.*;

@Slf4j
public class BookingScenarioListBuilder extends ScenarioListBuilder<BookingScenarioListBuilder> {

  private static final ThreadLocal<BookingComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalShipperPartyName = new ThreadLocal<>();

  private static final String BOOKING_API = "api";
  private static final String BOOKING_NOTIFICATIONS_API = "notification";
  private static final String POST_SCHEMA_NAME = "postBooking";
  private static final String GET_BOOKING_SCHEMA_NAME = "getBooking";
  private static final String PUT_SCHEMA_NAME = "putBooking";
  private static final String BOOKING_REF_STATUS_SCHEMA = "bookingRefStatus";
  private static final String CANCEL_SCHEMA_NAME = "bookings_bookingReference_body";
  private static final String BOOKING_NOTIFICATION_SCHEMA_NAME = "BookingNotification";

  public static BookingScenarioListBuilder buildTree(
      BookingComponentFactory componentFactory, String carrierPartyName, String shipperPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);
    if (System.currentTimeMillis() > 0) { // FIXME remove this
      // This is what's been implemented so far and should still work on PR.
      return carrier_SupplyScenarioParameters()
          .then(
              uc1_shipper_SubmitBookingRequest()
                  .then(
                      shipper_GetBooking(RECEIVED)
                          .thenEither(
                              uc5_carrier_confirmBookingRequest()
                                  .then(
                                      shipper_GetBooking(CONFIRMED)
                                          .thenEither(
                                              uc11_carrier_confirmBookingCompleted()
                                                  .then(shipper_GetBooking(COMPLETED)),
                                              uc6_carrier_requestUpdateToConfirmedBooking()
                                                  .then(
                                                      shipper_GetBooking(PENDING_AMENDMENT)
                                                          .then(
                                                              uc7_shipper_submitBookingAmendment()
                                                                  .then(
                                                                      shipper_GetBooking(
                                                                          PENDING_AMENDMENT,
                                                                          AMENDMENT_RECEIVED)))),
                                              uc7_shipper_submitBookingAmendment()
                                                  .then(
                                                      shipper_GetBooking(
                                                          CONFIRMED, AMENDMENT_RECEIVED)),
                                              uc10_carrier_declineBooking()
                                                  .then(shipper_GetBooking(DECLINED)))),
                              uc4_carrier_rejectBookingRequest().then(shipper_GetBooking(REJECTED)),
                              uc12_shipper_cancelBooking().then(shipper_GetBooking(CANCELLED)),
                              uc2_carrier_requestUpdateToBookingRequest()
                                  .then(shipper_GetBooking(PENDING_UPDATE)),
                              uc3_shipper_submitUpdatedBookingRequest()
                                  .then(shipper_GetBooking(PENDING_UPDATE_CONFIRMATION)))));
    }
    return carrier_SupplyScenarioParameters().thenAllPathsFrom(START);
  }

  private BookingScenarioListBuilder thenAllPathsFrom(BookingState bookingState) {
    return thenAllPathsFrom(bookingState, null);
  }

  private BookingScenarioListBuilder thenAllPathsFrom(
      BookingState bookingState, BookingState originalBookingState) {
    return switch (bookingState) {
      case CANCELLED, COMPLETED, DECLINED, REJECTED -> then(shipper_GetBooking(bookingState));
      case CONFIRMED -> then(
          shipper_GetBooking(bookingState)
              .thenEither(
                  uc6_carrier_requestBookingAmendment().thenAllPathsFrom(PENDING_AMENDMENT),
                  uc7_shipper_submitBookingAmendment()
                      .thenAllPathsFrom(AMENDMENT_RECEIVED, CONFIRMED),
                  uc10_carrier_declineBooking().thenAllPathsFrom(DECLINED),
                  uc11_carrier_confirmBookingCompleted().thenAllPathsFrom(COMPLETED),
                  uc12_shipper_cancelBooking().thenHappyPathFrom(CANCELLED)));
      case PENDING_UPDATE -> then(
          shipper_GetBooking(bookingState)
              .thenEither(
                  uc2_carrier_requestUpdateToBookingRequest().thenHappyPathFrom(PENDING_UPDATE),
                  uc3_shipper_submitUpdatedBookingRequest()
                      .thenHappyPathFrom(PENDING_UPDATE_CONFIRMATION),
                  uc4_carrier_rejectBookingRequest().thenHappyPathFrom(REJECTED),
                  uc12_shipper_cancelBooking().thenHappyPathFrom(CANCELLED)));
      case PENDING_UPDATE_CONFIRMATION -> then(
          shipper_GetBooking(bookingState)
              .thenEither(
                  uc2_carrier_requestUpdateToBookingRequest().thenHappyPathFrom(PENDING_UPDATE),
                  uc3_shipper_submitUpdatedBookingRequest()
                      .thenHappyPathFrom(PENDING_UPDATE_CONFIRMATION),
                  uc4_carrier_rejectBookingRequest().thenHappyPathFrom(REJECTED),
                  uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED),
                  uc12_shipper_cancelBooking().thenHappyPathFrom(CANCELLED)));
      case RECEIVED -> then(
          shipper_GetBooking(bookingState)
              .thenEither(
                  uc2_carrier_requestUpdateToBookingRequest().thenAllPathsFrom(PENDING_UPDATE),
                  uc3_shipper_submitUpdatedBookingRequest()
                      .thenAllPathsFrom(PENDING_UPDATE_CONFIRMATION),
                  uc4_carrier_rejectBookingRequest().thenAllPathsFrom(REJECTED),
                  uc5_carrier_confirmBookingRequest().thenAllPathsFrom(CONFIRMED),
                  uc12_shipper_cancelBooking().thenAllPathsFrom(CANCELLED)));
      case START -> then(uc1_shipper_SubmitBookingRequest().thenAllPathsFrom(RECEIVED));
      case PENDING_AMENDMENT -> then(
          shipper_GetBooking(bookingState)
              .thenEither(
                  uc6_carrier_requestBookingAmendment().thenHappyPathFrom(PENDING_AMENDMENT),
                  uc7_shipper_submitBookingAmendment()
                      .thenAllPathsFrom(AMENDMENT_RECEIVED, PENDING_AMENDMENT),
                  uc10_carrier_declineBooking().thenHappyPathFrom(DECLINED),
                  uc12_shipper_cancelBooking().thenHappyPathFrom(CANCELLED)));
      case AMENDMENT_RECEIVED -> then(
          shipper_GetBooking(bookingState)
              .thenEither(
                  uc6_carrier_requestBookingAmendment().thenHappyPathFrom(PENDING_AMENDMENT),
                  uc8a_carrier_approveBookingAmendment().thenHappyPathFrom(CONFIRMED),
                  uc8b_carrier_declineBookingAmendment().thenHappyPathFrom(CONFIRMED),
                  uc9_shipper_cancelBookingAmendment().thenHappyPathFrom(originalBookingState),
                  uc10_carrier_declineBooking().thenHappyPathFrom(DECLINED),
                  uc12_shipper_cancelBooking().thenHappyPathFrom(CANCELLED)));
    };
  }

  private BookingScenarioListBuilder thenHappyPathFrom(BookingState bookingState) {
    return switch (bookingState) {
      case CANCELLED, COMPLETED, DECLINED, REJECTED -> then(noAction());
      case CONFIRMED -> then(uc11_carrier_confirmBookingCompleted().thenHappyPathFrom(COMPLETED));
      case PENDING_AMENDMENT -> then(
          uc7_shipper_submitBookingAmendment().thenHappyPathFrom(AMENDMENT_RECEIVED));
      case AMENDMENT_RECEIVED -> then(
          uc8a_carrier_approveBookingAmendment().thenHappyPathFrom(CONFIRMED));
      case PENDING_UPDATE -> then(
          uc3_shipper_submitUpdatedBookingRequest().thenHappyPathFrom(PENDING_UPDATE_CONFIRMATION));
      case PENDING_UPDATE_CONFIRMATION, RECEIVED -> then(
          uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED));
      case START -> then(uc1_shipper_SubmitBookingRequest().thenHappyPathFrom(RECEIVED));
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
                componentFactory.getMessageSchemaValidator(BOOKING_API, GET_BOOKING_SCHEMA_NAME)));
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
                componentFactory.getMessageSchemaValidator(BOOKING_API, POST_SCHEMA_NAME),
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
                componentFactory.getMessageSchemaValidator(BOOKING_API, PUT_SCHEMA_NAME),
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

  private static BookingScenarioListBuilder uc6_carrier_requestBookingAmendment() {
    return tbdCarrierAction();
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
                componentFactory.getMessageSchemaValidator(BOOKING_API, PUT_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(BOOKING_API, BOOKING_REF_STATUS_SCHEMA),
                componentFactory.getMessageSchemaValidator(
                    BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingScenarioListBuilder uc8a_carrier_approveBookingAmendment() {
    return tbdCarrierAction();
  }

  private static BookingScenarioListBuilder uc8b_carrier_declineBookingAmendment() {
    return tbdCarrierAction();
  }

  private static BookingScenarioListBuilder uc9_shipper_cancelBookingAmendment() {
    return tbdShipperAction();
  }

  private static BookingScenarioListBuilder uc10_carrier_declineBooking() {
    return carrierStateChange(UC10_Carrier_RejectBookingAction::new);
  }

  private static BookingScenarioListBuilder uc11_carrier_confirmBookingCompleted() {
    return carrierStateChange(UC11_Carrier_ConfirmBookingCompletedAction::new);
  }

  private static BookingScenarioListBuilder uc12_shipper_cancelBooking() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC12_Shipper_CancelEntireBookingAction(
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
