package org.dcsa.conformance.standards.booking;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.util.MapUtils;
import org.dcsa.conformance.standards.booking.action.BookingAction;
import org.dcsa.conformance.standards.booking.action.CarrierSupplyScenarioParametersAction;
import org.dcsa.conformance.standards.booking.action.ShipperGetBookingAction;
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

import static org.dcsa.conformance.standards.booking.party.BookingState.AMENDMENT_RECEIVED;
import static org.dcsa.conformance.standards.booking.party.BookingState.CANCELLED;
import static org.dcsa.conformance.standards.booking.party.BookingState.COMPLETED;
import static org.dcsa.conformance.standards.booking.party.BookingState.CONFIRMED;
import static org.dcsa.conformance.standards.booking.party.BookingState.DECLINED;
import static org.dcsa.conformance.standards.booking.party.BookingState.PENDING_AMENDMENT;
import static org.dcsa.conformance.standards.booking.party.BookingState.PENDING_UPDATE;
import static org.dcsa.conformance.standards.booking.party.BookingState.RECEIVED;
import static org.dcsa.conformance.standards.booking.party.BookingState.REJECTED;

@Slf4j
public class BookingScenarioListBuilder extends ScenarioListBuilder<BookingScenarioListBuilder> {

  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private static final ThreadLocal<BookingComponentFactory> threadLocalComponentFactory = new ThreadLocal<>();
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

    return MapUtils.mergePartyScenarioModules(partyScenariosMap, testedPartyRoleNames);
  }

  private static Map<String, BookingScenarioListBuilder> carrierConformanceScenarios(String carrierPartyName) {
    var scenarios = new LinkedHashMap<String, BookingScenarioListBuilder>();
    scenarios.put("Required Dry Cargo scenario", carrierRequiredScenario(carrierPartyName, ScenarioType.DRY_CARGO));
    scenarios.put("Additional required Dry Cargo scenarios (execute at least one of these two)", additionalRequiredScenarios(carrierPartyName, ScenarioType.DRY_CARGO));
    scenarios.put("Required Reefer scenario", carrierRequiredScenario(carrierPartyName, ScenarioType.REEFER));
    scenarios.put("Additional required Reefer scenarios (execute at least one of these two)", additionalRequiredScenarios(carrierPartyName, ScenarioType.REEFER));
    scenarios.put("Required Dangerous Goods scenario", carrierRequiredScenario(carrierPartyName, ScenarioType.DG));
    scenarios.put("Additional required Dangerous Goods scenarios (execute at least one of these two)", additionalRequiredScenarios(carrierPartyName, ScenarioType.DG));
    scenarios.put("Optional (report-only) scenarios", carrierOptionalScenarios(carrierPartyName).asOptionalReportOnlyScenario());
    return scenarios;
  }

  private static Map<String, BookingScenarioListBuilder> shipperConformanceScenarios(String carrierPartyName) {
    var scenarios = new LinkedHashMap<String, BookingScenarioListBuilder>();
    scenarios.put("Required Dry Cargo scenario", shipperRequiredScenarios(ScenarioType.DRY_CARGO));
    scenarios.put("Required Reefer container scenario", shipperRequiredScenarios(ScenarioType.REEFER));
    scenarios.put("Required Dangerous Goods scenario", shipperRequiredScenarios(ScenarioType.DG));
    scenarios.put("Optional (report-only) scenarios", shipperOptionalScenarios(carrierPartyName).asOptionalReportOnlyScenario());
    return scenarios;
  }

  private static BookingScenarioListBuilder carrierRequiredScenario(String carrierPartyName, ScenarioType scenarioType) {
    return carrierSupplyScenarioParameters(carrierPartyName, scenarioType)
      .then(uc1ShipperSubmitBookingRequest(false).then(confirmedBookingScenario(false)));
  }

  private static BookingScenarioListBuilder shipperRequiredScenarios(ScenarioType scenarioType) {
    return uc1ShipperSubmitBookingRequest(false, scenarioType)
      .thenEither(
        shipperGetBookingStatusOnly(RECEIVED),
        uc2CarrierRequestUpdateToBookingRequest(false)
          .then(uc3ShipperSubmitUpdatedBookingRequest(false)),
        uc5CarrierConfirmBookingRequest(false)
          .then(
            uc6CarrierRequestToAmendConfirmedBooking(false)
              .then(uc7ShipperSubmitBookingAmendment(false))));
  }

  private static BookingScenarioListBuilder additionalRequiredScenarios(String carrierPartyName, ScenarioType scenarioType) {
    return carrierSupplyScenarioParameters(carrierPartyName, scenarioType)
      .then(
        uc1ShipperSubmitBookingRequest(false)
          .thenEither(
            uc3ShipperSubmitUpdatedBookingRequest(false),
            uc5CarrierConfirmBookingRequest(false)
              .then(uc7ShipperSubmitBookingAmendment(false))));
  }

  private static BookingScenarioListBuilder confirmedBookingScenario(boolean validateSecondaryStatuses) {
    return uc5CarrierConfirmBookingRequest(validateSecondaryStatuses)
      .then(shipperGetBookingStatusOnly(CONFIRMED));
  }

  private static BookingScenarioListBuilder carrierOptionalScenarios(String carrierPartyName) {
    return carrierSupplyScenarioParameters(carrierPartyName, ScenarioType.ANY)
      .then(
        uc1ShipperSubmitBookingRequest()
          .thenEither(
            shipperGetBookingStatusOnly(RECEIVED),
            uc2CarrierRequestUpdateToBookingRequest().then(shipperGetBookingStatusOnly(PENDING_UPDATE)),
            uc4CarrierRejectBookingRequest().then(shipperGetBookingStatusOnly(REJECTED)),
            uc5CarrierConfirmBookingRequest()
              .then(uc6CarrierRequestToAmendConfirmedBooking().then(shipperGetBookingStatusOnly(PENDING_AMENDMENT))),
            processedAmendmentScenario(true),
            processedAmendmentScenario(false),
            retrieveAmendedBookingContentScenario(),
            uc5CarrierConfirmBookingRequest()
              .then(
                uc7ShipperSubmitBookingAmendment(true)
                  .then(uc9ShipperCancelBookingAmendment().then(shipperGetBookingStatusOnly(CONFIRMED)))),
            uc5CarrierConfirmBookingRequest()
              .then(uc10CarrierDeclineBooking().then(shipperGetBookingStatusOnly(DECLINED))),
            uc11ShipperCancelBooking().then(shipperGetBookingStatusOnly(CANCELLED)),
            uc5CarrierConfirmBookingRequest()
              .then(uc12CarrierConfirmBookingCompleted().then(shipperGetBookingStatusOnly(COMPLETED))),
            uc5CarrierConfirmBookingRequest()
              .then(uc13ShipperCancelConfirmedBooking().then(shipperGetBookingStatusOnly(CONFIRMED))),
            processedCancellationScenario(true),
            processedCancellationScenario(false)));
  }

  private static BookingScenarioListBuilder shipperOptionalScenarios(String carrierPartyName) {
    return uc1ShipperSubmitBookingRequest(true, ScenarioType.ANY)
      .thenEither(
        retrieveAmendedBookingContentScenario(),
        uc5CarrierConfirmBookingRequest()
          .then(uc7ShipperSubmitBookingAmendment(true).then(uc9ShipperCancelBookingAmendment())),
        uc5CarrierConfirmBookingRequest().then(uc13ShipperCancelConfirmedBooking()),
        uc11ShipperCancelBooking());
  }

  private static BookingScenarioListBuilder retrieveAmendedBookingContentScenario() {
    return uc5CarrierConfirmBookingRequest()
      .then(
        uc7ShipperSubmitBookingAmendment(true)
          .then(shipperGetBooking(CONFIRMED, AMENDMENT_RECEIVED, null, true)));
  }

  private static BookingScenarioListBuilder processedAmendmentScenario(boolean confirm) {
    return uc5CarrierConfirmBookingRequest()
      .then(
        uc7ShipperSubmitBookingAmendment(true)
          .then(
            uc8CarrierProcessBookingAmendment(confirm)
              .then(shipperGetBookingStatusOnly(CONFIRMED))));
  }

  private static BookingScenarioListBuilder processedCancellationScenario(boolean confirm) {
    BookingState bookingStatus = confirm ? CANCELLED : CONFIRMED;
    return uc5CarrierConfirmBookingRequest()
      .then(
        uc13ShipperCancelConfirmedBooking()
          .then(
            uc14CarrierProcessBookingCancellation(confirm)
              .then(shipperGetBookingStatusOnly(bookingStatus))));
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
          componentFactory.getMessageSchemaValidator(BOOKING_API, CREATE_BOOKING_SCHEMA_NAME)));
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

  private static BookingScenarioListBuilder shipperGetBookingStatusOnly(
    BookingState expectedBookingStatus) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingScenarioListBuilder(
      previousAction ->
        new ShipperGetBookingAction(
          carrierPartyName,
          shipperPartyName,
          (BookingAction) previousAction,
          CarrierStatusScenario.bookingStatusOnly(expectedBookingStatus),
          componentFactory.getMessageSchemaValidator(BOOKING_API, GET_BOOKING_SCHEMA_NAME),
          false));
  }

  private static BookingScenarioListBuilder uc1ShipperSubmitBookingRequest() {
    return uc1ShipperSubmitBookingRequest(true);
  }

  private static BookingScenarioListBuilder uc1ShipperSubmitBookingRequest(
    boolean validateSecondaryStatuses) {
    return uc1ShipperSubmitBookingRequest(validateSecondaryStatuses, null);
  }

  private static BookingScenarioListBuilder uc1ShipperSubmitBookingRequest(
    boolean validateSecondaryStatuses, ScenarioType scenarioType) {
    BookingComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new BookingScenarioListBuilder(
      previousAction -> {
        String actionTitle = "UC1";
        if (scenarioType != null && scenarioType != ScenarioType.ANY) {
          actionTitle = "UC1[%s]".formatted(scenarioType.getDisplayName());
        }
        return new UC1_Shipper_SubmitBookingRequestAction(
          carrierPartyName,
          shipperPartyName,
          (BookingAction) previousAction,
          componentFactory.getMessageSchemaValidator(BOOKING_API, CREATE_BOOKING_SCHEMA_NAME),
          componentFactory.getMessageSchemaValidator(BOOKING_API, BOOKING_202_RESPONSE_SCHEMA),
          componentFactory.getMessageSchemaValidator(BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
          isWithNotifications,
          scenarioType,
          componentFactory.getStandardVersion(),
          actionTitle)
          .withoutSecondaryStatusValidationIf(!validateSecondaryStatuses);
      });
  }

  private static BookingScenarioListBuilder uc2CarrierRequestUpdateToBookingRequest() {
    return uc2CarrierRequestUpdateToBookingRequest(true);
  }

  private static BookingScenarioListBuilder uc2CarrierRequestUpdateToBookingRequest(
    boolean validateSecondaryStatuses) {
    return carrierStateChange(
      UC2_Carrier_RequestUpdateToBookingRequestAction::new, validateSecondaryStatuses);
  }

  private static BookingScenarioListBuilder uc3ShipperSubmitUpdatedBookingRequest(
    boolean validateSecondaryStatuses) {
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
          componentFactory.getMessageSchemaValidator(BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
          isWithNotifications)
          .withoutSecondaryStatusValidationIf(!validateSecondaryStatuses));
  }

  private static BookingScenarioListBuilder uc4CarrierRejectBookingRequest() {
    return carrierStateChange(UC4_Carrier_RejectBookingRequestAction::new);
  }

  private static BookingScenarioListBuilder uc5CarrierConfirmBookingRequest() {
    return uc5CarrierConfirmBookingRequest(true);
  }

  private static BookingScenarioListBuilder uc5CarrierConfirmBookingRequest(
    boolean validateSecondaryStatuses) {
    return carrierStateChange(
      UC5_Carrier_ConfirmBookingRequestAction::new, validateSecondaryStatuses);
  }

  private static BookingScenarioListBuilder uc6CarrierRequestToAmendConfirmedBooking() {
    return uc6CarrierRequestToAmendConfirmedBooking(true);
  }

  private static BookingScenarioListBuilder uc6CarrierRequestToAmendConfirmedBooking(
    boolean validateSecondaryStatuses) {
    return carrierStateChange(
      UC6_Carrier_RequestToAmendConfirmedBookingAction::new, validateSecondaryStatuses);
  }

  private static BookingScenarioListBuilder uc7ShipperSubmitBookingAmendment(
    boolean validateSecondaryStatuses) {
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
          componentFactory.getMessageSchemaValidator(BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
          isWithNotifications)
          .withoutSecondaryStatusValidationIf(!validateSecondaryStatuses));
  }

  private static BookingScenarioListBuilder uc8CarrierProcessBookingAmendment(boolean confirm) {
    return carrierStateChange(
      (carrierPartyName,
       shipperPartyName,
       previousAction,
       requestSchemaValidator,
       isWithNotifications) ->
        new UC8_Carrier_ProcessAmendmentAction(
          carrierPartyName,
          shipperPartyName,
          previousAction,
          requestSchemaValidator,
          isWithNotifications,
          confirm));
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
          componentFactory.getMessageSchemaValidator(BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
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
          componentFactory.getMessageSchemaValidator(BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
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
          componentFactory.getMessageSchemaValidator(BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
          isWithNotifications));
  }

  private static BookingScenarioListBuilder uc14CarrierProcessBookingCancellation(boolean confirm) {
    return carrierStateChange(
      (carrierPartyName,
       shipperPartyName,
       previousAction,
       requestSchemaValidator,
       isWithNotifications) ->
        new UC14CarrierProcessBookingCancellationAction(
          carrierPartyName,
          shipperPartyName,
          previousAction,
          requestSchemaValidator,
          isWithNotifications,
          confirm));
  }

  private static BookingScenarioListBuilder carrierStateChange(
    CarrierNotificationUseCase constructor) {
    return carrierStateChange(constructor, true);
  }

  private static BookingScenarioListBuilder carrierStateChange(
    CarrierNotificationUseCase constructor, boolean validateSecondaryStatuses) {
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
            componentFactory.getMessageSchemaValidator(BOOKING_NOTIFICATIONS_API, BOOKING_NOTIFICATION_SCHEMA_NAME),
            isWithNotifications)
          .withoutSecondaryStatusValidationIf(!validateSecondaryStatuses));
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
