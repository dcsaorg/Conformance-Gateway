package org.dcsa.conformance.standards.booking;

import java.util.function.Function;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.booking.action.BookingAction;
import org.dcsa.conformance.standards.booking.action.UC11_Carrier_ConfirmBookingCompletedAction;
import org.dcsa.conformance.standards.booking.action.UC1_Shipper_SubmitBookingRequestAction;
import org.dcsa.conformance.standards.booking.action.UC5_Carrier_ConfirmBookingRequestAction;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;
import static org.dcsa.conformance.standards.booking.party.BookingState.*;

@Slf4j
public class BookingScenarioListBuilder extends ScenarioListBuilder<BookingScenarioListBuilder> {
  private static final ThreadLocal<BookingComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalShipperPartyName = new ThreadLocal<>();

  public static BookingScenarioListBuilder buildTree(
      BookingComponentFactory componentFactory, String carrierPartyName, String shipperPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);
    return noAction().thenAllPathsFrom(START);
  }

  private BookingScenarioListBuilder thenAllPathsFrom(BookingState bookingState) {
    return thenAllPathsFrom(bookingState, null);
  }

  private BookingScenarioListBuilder thenAllPathsFrom(
      BookingState bookingState, BookingState originalBookingState) {
    return switch (bookingState) {
      case CANCELED, COMPLETED, DECLINED, REJECTED -> then(noAction());
      case CONFIRMED -> thenEither(
          uc6_carrier_requestBookingAmendment().thenAllPathsFrom(PENDING_AMENDMENT),
          uc7_shipper_submitBookingAmendment()
              .thenAllPathsFrom(PENDING_AMENDMENT_APPROVAL, CONFIRMED),
          uc10_carrier_declineBooking().thenAllPathsFrom(DECLINED),
          uc11_carrier_confirmBookingCompleted().thenAllPathsFrom(COMPLETED),
          uc12_shipper_cancelBooking().thenHappyPathFrom(CANCELED));
      case PENDING_UPDATE -> thenEither(
          uc2_carrier_requestUpdateToBookingRequest().thenHappyPathFrom(PENDING_UPDATE),
          uc3_shipper_submitUpdatedBookingRequest().thenHappyPathFrom(PENDING_UPDATE_CONFIRMATION),
          uc4_carrier_rejectBookingRequest().thenHappyPathFrom(REJECTED),
          uc12_shipper_cancelBooking().thenHappyPathFrom(CANCELED));
      case PENDING_UPDATE_CONFIRMATION -> thenEither(
          uc2_carrier_requestUpdateToBookingRequest().thenHappyPathFrom(PENDING_UPDATE),
          uc3_shipper_submitUpdatedBookingRequest().thenHappyPathFrom(PENDING_UPDATE_CONFIRMATION),
          uc4_carrier_rejectBookingRequest().thenHappyPathFrom(REJECTED),
          uc5_carrier_confirmBookingRequest().thenHappyPathFrom(CONFIRMED),
          uc12_shipper_cancelBooking().thenHappyPathFrom(CANCELED));
      case RECEIVED -> thenEither(
          uc2_carrier_requestUpdateToBookingRequest().thenAllPathsFrom(PENDING_UPDATE),
          uc3_shipper_submitUpdatedBookingRequest().thenAllPathsFrom(PENDING_UPDATE_CONFIRMATION),
          uc4_carrier_rejectBookingRequest().thenAllPathsFrom(REJECTED),
          uc5_carrier_confirmBookingRequest().thenAllPathsFrom(CONFIRMED),
          uc12_shipper_cancelBooking().thenAllPathsFrom(CANCELED));
      case START -> then(uc1_shipper_SubmitBookingRequest().thenAllPathsFrom(RECEIVED));
      case PENDING_AMENDMENT -> thenEither(
          uc6_carrier_requestBookingAmendment().thenHappyPathFrom(PENDING_AMENDMENT),
          uc7_shipper_submitBookingAmendment()
              .thenAllPathsFrom(PENDING_AMENDMENT_APPROVAL, PENDING_AMENDMENT),
          uc10_carrier_declineBooking().thenHappyPathFrom(DECLINED),
          uc12_shipper_cancelBooking().thenHappyPathFrom(CANCELED));
      case PENDING_AMENDMENT_APPROVAL -> thenEither(
          uc6_carrier_requestBookingAmendment().thenHappyPathFrom(PENDING_AMENDMENT),
          uc8a_carrier_approveBookingAmendment().thenHappyPathFrom(CONFIRMED),
          uc8b_carrier_declineBookingAmendment().thenHappyPathFrom(CONFIRMED),
          uc9_shipper_cancelBookingAmendment().thenHappyPathFrom(originalBookingState),
          uc10_carrier_declineBooking().thenHappyPathFrom(DECLINED),
          uc12_shipper_cancelBooking().thenHappyPathFrom(CANCELED));
    };
  }

  private BookingScenarioListBuilder thenHappyPathFrom(BookingState bookingState) {
    return switch (bookingState) {
      case CANCELED, COMPLETED, DECLINED, REJECTED -> then(noAction());
      case CONFIRMED -> then(uc11_carrier_confirmBookingCompleted().thenHappyPathFrom(COMPLETED));
      case PENDING_AMENDMENT -> then(uc7_shipper_submitBookingAmendment()
          .thenHappyPathFrom(PENDING_AMENDMENT_APPROVAL));
      case PENDING_AMENDMENT_APPROVAL -> then(uc8a_carrier_approveBookingAmendment()
          .thenHappyPathFrom(CONFIRMED));
      case PENDING_UPDATE -> then(uc3_shipper_submitUpdatedBookingRequest()
          .thenHappyPathFrom(PENDING_UPDATE_CONFIRMATION));
      case PENDING_UPDATE_CONFIRMATION, RECEIVED -> then(uc5_carrier_confirmBookingRequest()
          .thenHappyPathFrom(CONFIRMED));
      case START -> then(uc1_shipper_SubmitBookingRequest().thenHappyPathFrom(RECEIVED));
    };
  }

  private BookingScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static BookingScenarioListBuilder noAction() {
    return new BookingScenarioListBuilder(null);
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
                componentFactory.getMessageSchemaValidator(
                    BookingRole.SHIPPER.getConfigName(), true)));
  }

  private static BookingScenarioListBuilder uc2_carrier_requestUpdateToBookingRequest() {
    return tbdCarrierAction();
  }

  private static BookingScenarioListBuilder uc3_shipper_submitUpdatedBookingRequest() {
    return tbdShipperAction();
  }

  private static BookingScenarioListBuilder uc4_carrier_rejectBookingRequest() {
    return tbdCarrierAction();
  }

  private static BookingScenarioListBuilder uc5_carrier_confirmBookingRequest() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC5_Carrier_ConfirmBookingRequestAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    BookingRole.CARRIER.getConfigName(), true)));
  }

  private static BookingScenarioListBuilder uc6_carrier_requestBookingAmendment() {
    return tbdCarrierAction();
  }

  private static BookingScenarioListBuilder uc7_shipper_submitBookingAmendment() {
    return tbdShipperAction();
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
    return tbdCarrierAction();
  }

  private static BookingScenarioListBuilder uc11_carrier_confirmBookingCompleted() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
        previousAction ->
            new UC11_Carrier_ConfirmBookingCompletedAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    BookingRole.CARRIER.getConfigName(), true)));
  }

  private static BookingScenarioListBuilder uc12_shipper_cancelBooking() {
    return tbdShipperAction();
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

              @Override
              protected Supplier<String> getCbrrSupplier() {
                return null;
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

                      @Override
                      protected Supplier<String> getCbrrSupplier() {
                        return null;
                      }
                    }) {};
  }
}
