package org.dcsa.conformance.standards.booking;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.util.MapUtils;
import org.dcsa.conformance.standards.booking.action.BookingAction;
import org.dcsa.conformance.standards.booking.action.CarrierSupplyScenarioParametersAction;
import org.dcsa.conformance.standards.booking.action.ShipperGetBookingAction;
import org.dcsa.conformance.standards.booking.action.ShipperGetBookingSkippableAction;
import org.dcsa.conformance.standards.booking.action.UC10_Carrier_DeclineBookingAction;
import org.dcsa.conformance.standards.booking.action.UC11_Shipper_CancelBookingRequestAction;
import org.dcsa.conformance.standards.booking.action.UC12_Carrier_ConfirmBookingCompletedAction;
import org.dcsa.conformance.standards.booking.action.UC13ShipperCancelConfirmedBookingAction;
import org.dcsa.conformance.standards.booking.action.UC14CarrierProcessBookingCancellationAction;
import org.dcsa.conformance.standards.booking.action.UC1_Shipper_SubmitBookingRequestAction;
import org.dcsa.conformance.standards.booking.action.UC2_Carrier_RequestUpdateToBookingRequestAction;
import org.dcsa.conformance.standards.booking.action.UC3_Shipper_SubmitUpdatedBookingRequestAction;
import org.dcsa.conformance.standards.booking.action.UC4_Carrier_RejectBookingRequestAction;
import org.dcsa.conformance.standards.booking.action.UC5_Carrier_ConfirmBookingRequestAction;
import org.dcsa.conformance.standards.booking.action.UC6_Carrier_RequestToAmendConfirmedBookingAction;
import org.dcsa.conformance.standards.booking.action.UC7_Shipper_SubmitBookingAmendment;
import org.dcsa.conformance.standards.booking.action.UC8_Carrier_ProcessAmendmentAction;
import org.dcsa.conformance.standards.booking.action.UC9_Shipper_CancelBookingAmendment;
import org.dcsa.conformance.standards.booking.checks.CarrierStatusScenario;
import org.dcsa.conformance.standards.booking.checks.ScenarioType;
import org.dcsa.conformance.standards.booking.party.BookingCancellationState;
import org.dcsa.conformance.standards.booking.party.BookingRole;
import org.dcsa.conformance.standards.booking.party.BookingState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import static org.dcsa.conformance.standards.booking.party.BookingCancellationState.CANCELLATION_CONFIRMED;
import static org.dcsa.conformance.standards.booking.party.BookingCancellationState.CANCELLATION_RECEIVED;
import static org.dcsa.conformance.standards.booking.party.BookingState.AMENDMENT_CANCELLED;
import static org.dcsa.conformance.standards.booking.party.BookingState.AMENDMENT_CONFIRMED;
import static org.dcsa.conformance.standards.booking.party.BookingState.AMENDMENT_RECEIVED;
import static org.dcsa.conformance.standards.booking.party.BookingState.CANCELLED;
import static org.dcsa.conformance.standards.booking.party.BookingState.COMPLETED;
import static org.dcsa.conformance.standards.booking.party.BookingState.CONFIRMED;
import static org.dcsa.conformance.standards.booking.party.BookingState.DECLINED;
import static org.dcsa.conformance.standards.booking.party.BookingState.PENDING_AMENDMENT;
import static org.dcsa.conformance.standards.booking.party.BookingState.PENDING_UPDATE;
import static org.dcsa.conformance.standards.booking.party.BookingState.RECEIVED;
import static org.dcsa.conformance.standards.booking.party.BookingState.REJECTED;
import static org.dcsa.conformance.standards.booking.party.BookingState.UPDATE_RECEIVED;

@Slf4j
public class BookingScenarioListBuilder extends ScenarioListBuilder<BookingScenarioListBuilder> {

  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private static final ThreadLocal<BookingComponentFactory> threadLocalComponentFactory =
    new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalShipperPartyName = new ThreadLocal<>();
  private static final ThreadLocal<Boolean> threadLocalIsWithNotifications = new ThreadLocal<>();

  private static final String BOOKING_API = "api";
  private static final String BOOKING_NOTIFICATIONS_API = "api";
  public static final String CREATE_BOOKING_SCHEMA_NAME = "CreateBooking";
  public static final String GET_BOOKING_SCHEMA_NAME = "Booking";
  public static final String UPDATE_BOOKING_SCHEMA_NAME = "UpdateBooking";
  public static final String BOOKING_202_RESPONSE_SCHEMA = "CreateBookingResponse";
  private static final String CANCEL_SCHEMA_NAME = "CancelBookingRequest";
  public static final String BOOKING_NOTIFICATION_SCHEMA_NAME = "BookingNotification";

  private BookingScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, BookingScenarioListBuilder> createModuleScenarioListBuilders(
    BookingComponentFactory componentFactory,
    Set<String> testedPartyRoleNames,
    boolean isWithNotifications,
    String carrierPartyName,
    String shipperPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);
    threadLocalIsWithNotifications.set(isWithNotifications);

    if (SCENARIO_SUITE_CONFORMANCE.equals(componentFactory.getScenarioSuite())) {
      return createConformanceScenarios(testedPartyRoleNames, carrierPartyName);
    }

    throw new IllegalArgumentException(
      "Invalid scenario suite name '%s'".formatted(componentFactory.getScenarioSuite()));
  }

  private static Map<String, BookingScenarioListBuilder> createConformanceScenarios(Set<String> testedPartyRoleNames, String carrierPartyName) {
    Map<String, Map<String, BookingScenarioListBuilder>> partyScenariosMap = MapUtils.orderedMap(
      Map.entry(BookingRole.CARRIER.getConfigName(), carrierConformanceScenarios(carrierPartyName)),
      Map.entry(BookingRole.SHIPPER.getConfigName(), shipperConformanceScenarios(carrierPartyName))
    );

    var scenarios = new LinkedHashMap<String, BookingScenarioListBuilder>();
    testedPartyRoleNames.forEach(party -> scenarios.putAll(partyScenariosMap.get(party)));

    return scenarios;
  }

  private static Map<String, BookingScenarioListBuilder> carrierConformanceScenarios(
    String carrierPartyName) {
    var scenarios = new LinkedHashMap<String, BookingScenarioListBuilder>();
    scenarios.put("Required Dry Cargo scenario", requiredScenarios(carrierPartyName, ScenarioType.DRY_CARGO));
    scenarios.put("Additional required Dry Cargo scenarios (execute at least one of these two)", additionalRequiredScenarios(carrierPartyName, ScenarioType.DRY_CARGO));
    scenarios.put("Required Reefer container scenario", requiredScenarios(carrierPartyName, ScenarioType.REEFER));
    scenarios.put("Additional required Reefer container scenarios (execute at least one of these two)", additionalRequiredScenarios(carrierPartyName, ScenarioType.REEFER));
    scenarios.put("Required Dangerous Goods scenario", requiredScenarios(carrierPartyName, ScenarioType.DG));
    scenarios.put("Additional required Dangerous Goods scenarios (execute at least one of these two)", additionalRequiredScenarios(carrierPartyName, ScenarioType.DG));
    scenarios.put("Optional (report-only) scenarios", carrierOptionalScenarios(carrierPartyName));
    return scenarios;
  }

  private static Map<String, BookingScenarioListBuilder> shipperConformanceScenarios(
    String carrierPartyName) {
    var scenarios = new LinkedHashMap<String, BookingScenarioListBuilder>();
    scenarios.put("Required Dry Cargo scenario", requiredScenarios(carrierPartyName, ScenarioType.DRY_CARGO));
    scenarios.put("Required Reefer container scenario", requiredScenarios(carrierPartyName, ScenarioType.REEFER));
    scenarios.put("Required Dangerous Goods scenario", requiredScenarios(carrierPartyName, ScenarioType.DG));
    scenarios.put("Optional (report-only) scenarios", shipperOptionalScenarios(carrierPartyName));
    return scenarios;
  }

  private static BookingScenarioListBuilder requiredScenarios(String carrierPartyName, ScenarioType scenarioType) {
    return carrierSupplyScenarioParameters(carrierPartyName, scenarioType)
      .then(
        uc1ShipperSubmitBookingRequest()
          .then(
            shipperGetBooking(RECEIVED)
              .thenEither(
                confirmedBookingScenario())));
  }

  private static BookingScenarioListBuilder additionalRequiredScenarios(String carrierPartyName, ScenarioType scenarioType) {
    return carrierSupplyScenarioParameters(carrierPartyName, scenarioType)
      .then(
        uc1ShipperSubmitBookingRequest()
          .then(
            shipperGetBookingSkippable(RECEIVED)
              .thenEither(
                updatedBookingScenario(),
                processedAmendmentScenario())));
  }

  private static BookingScenarioListBuilder confirmedBookingScenario() {
    return uc5CarrierConfirmBookingRequest().then(shipperGetBookingSkippable(CONFIRMED));
  }

  private static BookingScenarioListBuilder updatedBookingScenario() {
    return uc3ShipperSubmitUpdatedBookingRequest().then(shipperGetBookingSkippable(UPDATE_RECEIVED));
  }

  private static BookingScenarioListBuilder processedAmendmentScenario() {
    return uc5CarrierConfirmBookingRequest()
      .then(
        shipperGetBookingSkippable(CONFIRMED)
          .then(
            uc7ShipperSubmitBookingAmendment()
              .then(
                shipperGetBookingSkippable(CONFIRMED, AMENDMENT_RECEIVED, null, false)
                  .then(
                    uc8CarrierProcessBookingAmendment()
                      .then(
                        shipperGetBookingSkippable(
                          CarrierStatusScenario.from(CONFIRMED, AMENDMENT_CONFIRMED, null), false))))));
  }

  private static BookingScenarioListBuilder carrierOptionalScenarios(String carrierPartyName) {
    return carrierSupplyScenarioParameters(carrierPartyName, ScenarioType.ANY)
      .then(
        uc1ShipperSubmitBookingRequest()
          .then(
            shipperGetBookingSkippable(RECEIVED)
              .thenEither(
                uc11ShipperCancelBooking().then(shipperGetBookingSkippable(CANCELLED)),
                retrieveAmendedBookingContentScenario(),
                uc2CarrierRequestUpdateToBookingRequest()
                  .then(shipperGetBookingSkippable(PENDING_UPDATE)),
                uc4CarrierRejectBookingRequest().then(shipperGetBookingSkippable(REJECTED)),
                confirmedBookingOptionalScenarios())));
  }

  private static BookingScenarioListBuilder shipperOptionalScenarios(String carrierPartyName) {
    return carrierSupplyScenarioParameters(carrierPartyName, ScenarioType.ANY)
      .then(
        uc1ShipperSubmitBookingRequest()
          .then(
            shipperGetBookingSkippable(RECEIVED)
              .thenEither(
                uc11ShipperCancelBooking().then(shipperGetBookingSkippable(CANCELLED)),
                updatedBookingScenario(),
                processedAmendmentScenario(),
                retrieveAmendedBookingContentScenario(),
                requestedBookingUpdateScenario(),
                shipperConfirmedBookingCancellationScenario(),
                shipperAmendmentCancellationScenario(),
                carrierRequestedAmendmentScenario())));
  }

  private static BookingScenarioListBuilder requestedBookingUpdateScenario() {
    return uc2CarrierRequestUpdateToBookingRequest()
      .then(shipperGetBookingSkippable(PENDING_UPDATE)
        .then(updatedBookingScenario()));
  }

  private static BookingScenarioListBuilder carrierRequestedAmendmentScenario() {
    return uc5CarrierConfirmBookingRequest()
      .then(
        shipperGetBookingSkippable(CONFIRMED)
          .then(
            uc6CarrierRequestToAmendConfirmedBooking()
              .then(
                shipperGetBookingSkippable(PENDING_AMENDMENT)
                  .then(
                    uc7ShipperSubmitBookingAmendment()
                      .then(shipperGetBookingSkippable(CONFIRMED, AMENDMENT_RECEIVED, null, false))))));
  }

  private static BookingScenarioListBuilder shipperConfirmedBookingCancellationScenario() {
    return uc5CarrierConfirmBookingRequest()
      .then(shipperGetBookingSkippable(CONFIRMED).then(confirmedBookingCancellationScenario()));
  }

  private static BookingScenarioListBuilder shipperAmendmentCancellationScenario() {
    return uc5CarrierConfirmBookingRequest()
      .then(shipperGetBookingSkippable(CONFIRMED).then(amendmentCancellationScenario()));
  }

  private static BookingScenarioListBuilder confirmedBookingOptionalScenarios() {
    return uc5CarrierConfirmBookingRequest()
      .then(
        shipperGetBookingSkippable(CONFIRMED)
          .thenEither(
            confirmedBookingCancellationScenario(),
            amendmentCancellationScenario(),
            uc10CarrierDeclineBooking().then(shipperGetBookingSkippable(DECLINED)),
            uc6CarrierRequestToAmendConfirmedBooking()
              .then(shipperGetBookingSkippable(PENDING_AMENDMENT)),
            uc12CarrierConfirmBookingCompleted().then(shipperGetBookingSkippable(COMPLETED))));
  }

  private static BookingScenarioListBuilder confirmedBookingCancellationScenario() {
    return uc13ShipperCancelConfirmedBooking()
      .then(
        shipperGetBookingSkippable(CONFIRMED, null, CANCELLATION_RECEIVED, false)
          .then(
            uc14CarrierProcessBookingCancellation()
              .then(
                shipperGetBookingSkippable(
                  CarrierStatusScenario.from(CANCELLED, null, CANCELLATION_CONFIRMED), false))));
  }

  private static BookingScenarioListBuilder amendmentCancellationScenario() {
    return uc7ShipperSubmitBookingAmendment()
      .then(
        shipperGetBookingSkippable(CONFIRMED, AMENDMENT_RECEIVED, null, false)
          .then(
            uc9ShipperCancelBookingAmendment()
              .then(
                shipperGetBookingSkippable(CONFIRMED, AMENDMENT_CANCELLED, null, false))));
  }

  private static BookingScenarioListBuilder retrieveAmendedBookingContentScenario() {
    return uc5CarrierConfirmBookingRequest()
      .then(
        shipperGetBookingSkippable(CONFIRMED)
          .then(
            uc7ShipperSubmitBookingAmendment()
              .then(
                shipperGetBookingSkippable(CONFIRMED, AMENDMENT_RECEIVED, null, false)
                  .then(shipperGetBookingSkippable(CONFIRMED, AMENDMENT_RECEIVED, null, true)))));
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
    return shipperGetBooking(expectedBookingStatus, null, null, false);
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

  private static BookingScenarioListBuilder shipperGetBookingSkippable(BookingState expectedBookingStatus) {
    return shipperGetBookingSkippable(expectedBookingStatus, null, null, false);
  }

  private static BookingScenarioListBuilder shipperGetBookingSkippable(
    BookingState expectedBookingStatus,
    BookingState expectedAmendedBookingStatus,
    BookingCancellationState expectedCancellationState,
    boolean requestAmendedContent) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
      previousAction ->
        new ShipperGetBookingSkippableAction(
          carrierPartyName,
          shipperPartyName,
          (BookingAction) previousAction,
          expectedBookingStatus,
          expectedAmendedBookingStatus,
          expectedCancellationState,
          componentFactory.getMessageSchemaValidator(BOOKING_API, GET_BOOKING_SCHEMA_NAME),
          requestAmendedContent));
  }

  private static BookingScenarioListBuilder shipperGetBookingSkippable(
    CarrierStatusScenario carrierStatusScenario, boolean requestAmendedContent) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
      previousAction ->
        new ShipperGetBookingSkippableAction(
          carrierPartyName,
          shipperPartyName,
          (BookingAction) previousAction,
          carrierStatusScenario,
          componentFactory.getMessageSchemaValidator(BOOKING_API, GET_BOOKING_SCHEMA_NAME),
          requestAmendedContent));
  }

  private static BookingScenarioListBuilder uc1ShipperSubmitBookingRequest() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
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
            BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
          isWithNotifications));
  }

  private static BookingScenarioListBuilder uc2CarrierRequestUpdateToBookingRequest() {
    return carrierStateChange(UC2_Carrier_RequestUpdateToBookingRequestAction::new);
  }

  private static BookingScenarioListBuilder uc3ShipperSubmitUpdatedBookingRequest() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
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
            BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
          isWithNotifications));
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

  private static BookingScenarioListBuilder uc7ShipperSubmitBookingAmendment() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new BookingScenarioListBuilder(
      previousAction ->
        new UC7_Shipper_SubmitBookingAmendment(
          carrierPartyName,
          shipperPartyName,
          (BookingAction) previousAction,
          BookingState.CONFIRMED,
          BookingState.AMENDMENT_RECEIVED,
          componentFactory.getMessageSchemaValidator(BOOKING_API, UPDATE_BOOKING_SCHEMA_NAME),
          componentFactory.getMessageSchemaValidator(
            BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
          componentFactory.getMessageSchemaValidator(
            BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
          isWithNotifications));
  }

  private static BookingScenarioListBuilder uc8CarrierProcessBookingAmendment() {
    return carrierStateChange(UC8_Carrier_ProcessAmendmentAction::new);
  }

  private static BookingScenarioListBuilder uc9ShipperCancelBookingAmendment() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
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
            BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
          isWithNotifications));
  }

  private static BookingScenarioListBuilder uc10CarrierDeclineBooking() {
    return carrierStateChange(
      (carrierPartyName,
       shipperPartyName,
       previousAction,
       requestSchemaValidator,
       isWithNotifications) ->
        new UC10_Carrier_DeclineBookingAction(
          carrierPartyName,
          shipperPartyName,
          previousAction,
          null,
          requestSchemaValidator,
          isWithNotifications));
  }

  private static BookingScenarioListBuilder uc11ShipperCancelBooking() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
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
            BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
          isWithNotifications));
  }

  private static BookingScenarioListBuilder uc12CarrierConfirmBookingCompleted() {
    return carrierStateChange(UC12_Carrier_ConfirmBookingCompletedAction::new);
  }

  private static BookingScenarioListBuilder uc13ShipperCancelConfirmedBooking() {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
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
            BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
          isWithNotifications));
  }

  private static BookingScenarioListBuilder uc14CarrierProcessBookingCancellation() {
    return carrierStateChange(UC14CarrierProcessBookingCancellationAction::new);
  }

  private static BookingScenarioListBuilder carrierStateChange(
    CarrierNotificationUseCase constructor) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new BookingScenarioListBuilder(
      previousAction ->
        constructor.newInstance(
          carrierPartyName,
          shipperPartyName,
          (BookingAction) previousAction,
          componentFactory.getMessageSchemaValidator(
            BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
          isWithNotifications));
  }

  public interface CarrierNotificationUseCase {
    BookingAction newInstance(
      String carrierPartyName,
      String shipperPartyName,
      BookingAction previousAction,
      JsonSchemaValidator requestSchemaValidator,
      boolean isWithNotifications);
  }
}
