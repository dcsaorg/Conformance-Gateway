package org.dcsa.conformance.standards.bookingandebl;

import static org.dcsa.conformance.standards.booking.party.BookingState.AMENDMENT_CONFIRMED;
import static org.dcsa.conformance.standards.booking.party.BookingState.AMENDMENT_RECEIVED;
import static org.dcsa.conformance.standards.booking.party.BookingState.CONFIRMED;
import static org.dcsa.conformance.standards.ebl.checks.ScenarioType.REGULAR_SWB;
import static org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus.SI_ANY;
import static org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus.SI_RECEIVED;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.TD_APPROVED;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.TD_DRAFT;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.TD_ISSUED;

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
import org.dcsa.conformance.standards.ebl.EblScenarioListBuilder;
import org.dcsa.conformance.standards.booking.action.BookingAndEblAction;
import org.dcsa.conformance.standards.ebl.action.CarrierSupplyPayloadAction;
import org.dcsa.conformance.standards.ebl.action.EblAction;
import org.dcsa.conformance.standards.ebl.action.Shipper_GetShippingInstructionsAction;
import org.dcsa.conformance.standards.ebl.action.Shipper_GetTransportDocumentAction;
import org.dcsa.conformance.standards.ebl.action.UC1_Shipper_SubmitShippingInstructionsAction;
import org.dcsa.conformance.standards.ebl.action.UC6_Carrier_PublishDraftTransportDocumentAction;
import org.dcsa.conformance.standards.ebl.action.UC7_Shipper_ApproveDraftTransportDocumentAction;
import org.dcsa.conformance.standards.ebl.action.UC8_Carrier_IssueTransportDocumentAction;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

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
                carrierSupplyBookingScenarioParameters(carrierPartyName, ScenarioType.REGULAR)
                    .then(
                        uc1ShipperSubmitBookingRequest()
                            .then(
                                shipperGetBooking(BookingState.RECEIVED, null, null, false)
                                    .then(
                                        uc5CarrierConfirmBookingRequest()
                                            .then(
                                                shipperGetBooking(CONFIRMED, null, null, false)
                                                    .then(
                                                        carrierEblSupplyScenarioParameters(
                                                                REGULAR_SWB, false)
                                                            .then(
                                                                uc1ShipperSubmitShippingInstructions()
                                                                    .then(
                                                                        shipperGetShippingInstructions(
                                                                                ShippingInstructionsStatus
                                                                                    .SI_RECEIVED,
                                                                                null,
                                                                                false,
                                                                                false,
                                                                                false)
                                                                            .then(
                                                                                uc6CarrierPublishDraftTransportDocument(
                                                                                        false)
                                                                                    .then(
                                                                                        shipperGetTransportDocument(
                                                                                                TD_DRAFT)
                                                                                            .then(
                                                                                                shipperGetShippingInstructions(
                                                                                                        SI_RECEIVED,
                                                                                                        SI_ANY,
                                                                                                        false,
                                                                                                        true,
                                                                                                        false)
                                                                                                    .then(
                                                                                                        uc7ShipperApproveDraftTransportDocument()
                                                                                                            .then(
                                                                                                                shipperGetTransportDocument(
                                                                                                                        TD_APPROVED)
                                                                                                                    .then(
                                                                                                                        uc8CarrierIssueTransportDocument()
                                                                                                                            .then(
                                                                                                                                shipperGetTransportDocument(
                                                                                                                                        TD_ISSUED)
                                                                                                                                    .then(
                                                                                                                                        uc7ShipperSubmitBookingAmendment(
                                                                                                                                                BookingState
                                                                                                                                                    .CONFIRMED)
                                                                                                                                            .then(
                                                                                                                                                shipperGetBooking(
                                                                                                                                                        CONFIRMED,
                                                                                                                                                        AMENDMENT_RECEIVED,
                                                                                                                                                        null,
                                                                                                                                                        true)
                                                                                                                                                    .then(
                                                                                                                                                        uc8aCarrierApproveBookingAmendment()
                                                                                                                                                            .then(
                                                                                                                                                                shipperGetBooking(
                                                                                                                                                                        CONFIRMED,
                                                                                                                                                                        AMENDMENT_CONFIRMED,
                                                                                                                                                                        null,
                                                                                                                                                                        false)
                                                                                                                                                                    .then(
                                                                                                                                                                        uc8CarrierIssueTransportDocument())))))))))))))))))))))
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
                componentFactory.getBookingMessageSchemaValidator(
                    BookingScenarioListBuilder.GET_BOOKING_SCHEMA_NAME),
                requestAmendedContent));
  }

  private static BookingAndEblScenarioListBuilder carrierSupplyBookingScenarioParameters(
      String carrierPartyName, ScenarioType scenarioType) {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    var version = componentFactory.getStandardVersion().split("-\\+-")[0];
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new CarrierSupplyScenarioParametersAction(
                carrierPartyName,
                scenarioType,
                version,
                componentFactory.getBookingMessageSchemaValidator(
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
                componentFactory.getBookingMessageSchemaValidator(
                    BookingScenarioListBuilder.CREATE_BOOKING_SCHEMA_NAME),
                componentFactory.getBookingMessageSchemaValidator(
                    BookingScenarioListBuilder.BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getBookingMessageSchemaValidator(
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
                (BookingAndEblAction) previousAction,
                bookingState,
                BookingState.AMENDMENT_RECEIVED,
                componentFactory.getBookingMessageSchemaValidator(
                    BookingScenarioListBuilder.UPDATE_BOOKING_SCHEMA_NAME),
                componentFactory.getBookingMessageSchemaValidator(
                    BookingScenarioListBuilder.BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getBookingMessageSchemaValidator(
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

  private static BookingAndEblScenarioListBuilder shipperGetShippingInstructions(
      ShippingInstructionsStatus expectedSiStatus,
      ShippingInstructionsStatus expectedUpdatedSiStatus,
      boolean requestAmendedSI,
      boolean recordTDR,
      boolean useBothRef) {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new Shipper_GetShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                expectedSiStatus,
                expectedUpdatedSiStatus,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.GET_EBL_SCHEMA_NAME),
                requestAmendedSI,
                recordTDR,
                useBothRef));
  }

  private static BookingAndEblScenarioListBuilder shipperGetTransportDocument(
      TransportDocumentStatus expectedTdStatus) {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new Shipper_GetTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                expectedTdStatus,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.GET_TD_SCHEMA_NAME)));
  }

  private static BookingAndEblScenarioListBuilder carrierEblSupplyScenarioParameters(
      org.dcsa.conformance.standards.ebl.checks.ScenarioType scenarioType, boolean isTd) {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String standardVersion = componentFactory.getStandardVersion().split("-\\+-")[1];
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new CarrierSupplyPayloadAction(
                carrierPartyName,
                (BookingAndEblAction) previousAction,
                scenarioType,
                standardVersion,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.POST_EBL_SCHEMA_NAME),
                isTd));
  }

  private static BookingAndEblScenarioListBuilder uc1ShipperSubmitShippingInstructions() {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new UC1_Shipper_SubmitShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.POST_EBL_SCHEMA_NAME),
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.RESPONSE_POST_SHIPPING_INSTRUCTIONS_SCHEMA_NAME),
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingAndEblScenarioListBuilder uc6CarrierPublishDraftTransportDocument(
      boolean skipSI) {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new UC6_Carrier_PublishDraftTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.EBL_TD_NOTIFICATION_SCHEMA_NAME),
                skipSI));
  }

  private static BookingAndEblScenarioListBuilder uc7ShipperApproveDraftTransportDocument() {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new UC7_Shipper_ApproveDraftTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.PATCH_TD_SCHEMA_NAME),
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }

  private static BookingAndEblScenarioListBuilder uc8CarrierIssueTransportDocument() {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new UC8_Carrier_IssueTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAndEblAction) previousAction,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.EBL_TD_NOTIFICATION_SCHEMA_NAME)));
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
                componentFactory.getBookingMessageSchemaValidator(
                    BookingScenarioListBuilder.BOOKING_NOTIFICATION_SCHEMA_NAME)));
  }
}
