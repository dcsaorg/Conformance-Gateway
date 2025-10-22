package org.dcsa.conformance.standards.bookingandebl;

import static org.dcsa.conformance.standards.booking.party.BookingState.AMENDMENT_CONFIRMED;
import static org.dcsa.conformance.standards.booking.party.BookingState.AMENDMENT_RECEIVED;
import static org.dcsa.conformance.standards.booking.party.BookingState.CONFIRMED;
import static org.dcsa.conformance.standards.booking.party.BookingState.RECEIVED;
import static org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus.SI_ANY;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.TD_APPROVED;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.TD_DRAFT;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.TD_ISSUED;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.TD_PENDING_SURRENDER_FOR_AMENDMENT;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.TD_SURRENDERED_FOR_AMENDMENT;

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
import org.dcsa.conformance.standards.booking.party.BookingState;
import org.dcsa.conformance.standards.ebl.EblScenarioListBuilder;
import org.dcsa.conformance.standards.ebl.action.CarrierSupplyPayloadAction;
import org.dcsa.conformance.standards.ebl.action.EblAction;
import org.dcsa.conformance.standards.ebl.action.Shipper_GetShippingInstructionsAction;
import org.dcsa.conformance.standards.ebl.action.Shipper_GetTransportDocumentAction;
import org.dcsa.conformance.standards.ebl.action.UC10_Carrier_ProcessSurrenderRequestForAmendmentAction;
import org.dcsa.conformance.standards.ebl.action.UC11_Carrier_voidTDAndIssueAmendedTransportDocumentAction;
import org.dcsa.conformance.standards.ebl.action.UC1_Shipper_SubmitShippingInstructionsAction;
import org.dcsa.conformance.standards.ebl.action.UC6_Carrier_PublishDraftTransportDocumentAction;
import org.dcsa.conformance.standards.ebl.action.UC7_Shipper_ApproveDraftTransportDocumentAction;
import org.dcsa.conformance.standards.ebl.action.UC8_Carrier_IssueTransportDocumentAction;
import org.dcsa.conformance.standards.ebl.action.UC9_Carrier_AwaitSurrenderRequestForAmendmentAction;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;
import org.dcsa.conformance.standardscommons.action.BookingAndEblAction;

@Slf4j
public class BookingAndEblScenarioListBuilder
    extends ScenarioListBuilder<BookingAndEblScenarioListBuilder> {

  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private static final ThreadLocal<BookingAndEblComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalShipperPartyName = new ThreadLocal<>();
  private static final ThreadLocal<Boolean> threadLocalIsWithNotifications = new ThreadLocal<>();

  private BookingAndEblScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, BookingAndEblScenarioListBuilder> createModuleScenarioListBuilders(
      BookingAndEblComponentFactory componentFactory,
      boolean isWithNotifications,
      String carrierPartyName,
      String shipperPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);
    threadLocalIsWithNotifications.set(isWithNotifications);

    if (SCENARIO_SUITE_CONFORMANCE.equals(componentFactory.getScenarioSuite())) {
      return createConformanceScenarios(carrierPartyName);
    }

    throw new IllegalArgumentException(
        "Invalid scenario suite name '%s'".formatted(componentFactory.getScenarioSuite()));
  }

  private static Map<String, BookingAndEblScenarioListBuilder> createConformanceScenarios(
      String carrierPartyName) {
    return Stream.of(
            createSeaWaybillScenarios(carrierPartyName),
            createStraightEBLScenarios(carrierPartyName))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static Map.Entry<String, BookingAndEblScenarioListBuilder> createSeaWaybillScenarios(
      String carrierPartyName) {
    return Map.entry(
        "Sea Waybill",
        noAction()
            .thenEither(
                carrierSupplyBookingScenarioParameters(carrierPartyName, ScenarioType.REEFER)
                    .then(
                        uc1BookingGet(
                            uc5BookingGet(
                                carrierEblSupplyScenarioParameters(org.dcsa.conformance.standards.ebl.checks.ScenarioType.REGULAR_SWB)
                                    .then(
                                        uc1SIGet(
                                            uc6TDGetSIGet(
                                                uc7TDGet(
                                                    uc8TDGet(
                                                        uc7BookingGet(
                                                            uc8BookingGet(uc8TDGet())))))))))),
                carrierSupplyBookingScenarioParameters(carrierPartyName, ScenarioType.DG)
                    .then(
                        uc1BookingGet(
                            uc5BookingGet(
                                carrierEblSupplyScenarioParameters(org.dcsa.conformance.standards.ebl.checks.ScenarioType.REGULAR_SWB)
                                    .then(
                                        uc1SIGet(
                                            uc6TDGetSIGet(
                                                uc7TDGet(
                                                    uc8TDGet(
                                                        uc7BookingGet(
                                                            uc8BookingGet(uc8TDGet()))))))))))));
  }

  private static Map.Entry<String, BookingAndEblScenarioListBuilder> createStraightEBLScenarios(
      String carrierPartyName) {
    return Map.entry(
        "Straight eBL",
        noAction()
            .thenEither(
                carrierSupplyBookingScenarioParameters(carrierPartyName, ScenarioType.REEFER)
                    .then(
                        uc1BookingGet(
                            uc5BookingGet(
                                carrierEblSupplyScenarioParameters(org.dcsa.conformance.standards.ebl.checks.ScenarioType.REGULAR_STRAIGHT_BL)
                                    .then(
                                        uc1SIGet(
                                            uc6TDGetSIGet(
                                                uc7TDGet(
                                                    uc8TDGet(
                                                        uc7BookingGet(
                                                            uc8BookingGet(
                                                                uc9TDGet(uc10TDGet(uc11TDGet())))),
                                                        uc9TDGet(
                                                            uc7BookingGet(
                                                                uc8BookingGet(
                                                                    uc10TDGet(uc11TDGet()))),
                                                            uc10TDGet(
                                                                uc7BookingGet(
                                                                    uc8BookingGet(
                                                                        uc11TDGet())))))))))))),
                carrierSupplyBookingScenarioParameters(carrierPartyName, ScenarioType.DG)
                    .then(
                        uc1BookingGet(
                            uc5BookingGet(
                                carrierEblSupplyScenarioParameters(org.dcsa.conformance.standards.ebl.checks.ScenarioType.REGULAR_STRAIGHT_BL)
                                    .then(
                                        uc1SIGet(
                                            uc6TDGetSIGet(
                                                uc7TDGet(
                                                    uc8TDGet(
                                                        uc7BookingGet(
                                                            uc8BookingGet(
                                                                uc9TDGet(uc10TDGet(uc11TDGet())))),
                                                        uc9TDGet(
                                                            uc7BookingGet(
                                                                uc8BookingGet(
                                                                    uc10TDGet(uc11TDGet()))),
                                                            uc10TDGet(
                                                                uc7BookingGet(
                                                                    uc8BookingGet(
                                                                        uc11TDGet()))))))))))))));
  }

  private static BookingAndEblScenarioListBuilder noAction() {
    return new BookingAndEblScenarioListBuilder(null);
  }

  private static BookingAndEblScenarioListBuilder shipperGetBooking(
      BookingState expectedBookingStatus,
      BookingState expectedAmendedBookingStatus,
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
                null,
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
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
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
                    BookingScenarioListBuilder.BOOKING_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static BookingAndEblScenarioListBuilder uc5CarrierConfirmBookingRequest() {
    return carrierStateChange(UC5_Carrier_ConfirmBookingRequestAction::new);
  }

  private static BookingAndEblScenarioListBuilder uc7ShipperSubmitBookingAmendment() {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new UC7_Shipper_SubmitBookingAmendment(
                carrierPartyName,
                shipperPartyName,
                (BookingAndEblAction) previousAction,
                BookingState.CONFIRMED,
                BookingState.AMENDMENT_RECEIVED,
                componentFactory.getBookingMessageSchemaValidator(
                    BookingScenarioListBuilder.UPDATE_BOOKING_SCHEMA_NAME),
                componentFactory.getBookingMessageSchemaValidator(
                    BookingScenarioListBuilder.BOOKING_202_RESPONSE_SCHEMA),
                componentFactory.getBookingMessageSchemaValidator(
                    BookingScenarioListBuilder.BOOKING_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static BookingAndEblScenarioListBuilder uc8aCarrierApproveBookingAmendment() {
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
                BookingState.CONFIRMED,
                BookingState.AMENDMENT_CONFIRMED,
                requestSchemaValidator,
                true,
                isWithNotifications));
  }

  private static BookingAndEblScenarioListBuilder uc1BookingGet(
      BookingAndEblScenarioListBuilder... thenEither) {
    return uc1ShipperSubmitBookingRequest()
        .then(shipperGetBooking(RECEIVED, null, false).thenEither(thenEither));
  }

  private static BookingAndEblScenarioListBuilder uc5BookingGet(
      BookingAndEblScenarioListBuilder... thenEither) {
    return uc5CarrierConfirmBookingRequest()
        .then(shipperGetBooking(CONFIRMED, null, false).thenEither(thenEither));
  }

  private static BookingAndEblScenarioListBuilder uc7BookingGet(
      BookingAndEblScenarioListBuilder... thenEither) {
    return uc7ShipperSubmitBookingAmendment()
        .then(shipperGetBooking(CONFIRMED, AMENDMENT_RECEIVED, true).thenEither(thenEither));
  }

  private static BookingAndEblScenarioListBuilder uc8BookingGet(
      BookingAndEblScenarioListBuilder... thenEither) {
    return uc8aCarrierApproveBookingAmendment()
        .then(shipperGetBooking(CONFIRMED, AMENDMENT_CONFIRMED, false).thenEither(thenEither));
  }

  private static BookingAndEblScenarioListBuilder shipperGetShippingInstructions(
      ShippingInstructionsStatus expectedUpdatedSiStatus, boolean recordTDR) {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new Shipper_GetShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                ShippingInstructionsStatus.SI_RECEIVED,
                expectedUpdatedSiStatus,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.GET_EBL_SCHEMA_NAME),
                false,
                recordTDR,
                false));
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

  private static BookingAndEblScenarioListBuilder carrierEblSupplyScenarioParameters(org.dcsa.conformance.standards.ebl.checks.ScenarioType eblScenarioType) {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String standardVersion = componentFactory.getStandardVersion().split("-\\+-")[1];
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new CarrierSupplyPayloadAction(
                carrierPartyName,
                (BookingAndEblAction) previousAction,
                eblScenarioType,
                standardVersion,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.POST_EBL_SCHEMA_NAME),
                false));
  }

  private static BookingAndEblScenarioListBuilder uc1ShipperSubmitShippingInstructions() {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
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
                    EblScenarioListBuilder.EBL_SI_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static BookingAndEblScenarioListBuilder uc6CarrierPublishDraftTransportDocument() {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new UC6_Carrier_PublishDraftTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.EBL_TD_NOTIFICATION_SCHEMA_NAME),
                false,
                isWithNotifications));
  }

  private static BookingAndEblScenarioListBuilder uc7ShipperApproveDraftTransportDocument() {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new UC7_Shipper_ApproveDraftTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.PATCH_TD_SCHEMA_NAME),
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.EBL_TD_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static BookingAndEblScenarioListBuilder uc8CarrierIssueTransportDocument() {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new UC8_Carrier_IssueTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAndEblAction) previousAction,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.EBL_TD_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static BookingAndEblScenarioListBuilder uc9CarrierAwaitSurrenderRequestForAmendment() {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new UC9_Carrier_AwaitSurrenderRequestForAmendmentAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAndEblAction) previousAction,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.EBL_TD_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static BookingAndEblScenarioListBuilder uc10aCarrierAcceptSurrenderRequestForAmendment() {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new UC10_Carrier_ProcessSurrenderRequestForAmendmentAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAndEblAction) previousAction,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.EBL_TD_NOTIFICATION_SCHEMA_NAME),
                true,
                isWithNotifications));
  }

  private static BookingAndEblScenarioListBuilder
      uc11CarrierVoidTDAndIssueAmendedTransportDocument() {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            new UC11_Carrier_voidTDAndIssueAmendedTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (BookingAndEblAction) previousAction,
                componentFactory.getEblMessageSchemaValidator(
                    EblScenarioListBuilder.EBL_TD_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static BookingAndEblScenarioListBuilder carrierStateChange(
      BookingScenarioListBuilder.CarrierNotificationUseCase constructor) {
    BookingAndEblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new BookingAndEblScenarioListBuilder(
        previousAction ->
            constructor.newInstance(
                carrierPartyName,
                shipperPartyName,
                (BookingAction) previousAction,
                componentFactory.getBookingMessageSchemaValidator(
                    BookingScenarioListBuilder.BOOKING_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static BookingAndEblScenarioListBuilder uc1SIGet(
      BookingAndEblScenarioListBuilder... thenEither) {
    return uc1ShipperSubmitShippingInstructions()
        .then(shipperGetShippingInstructions(null, false).thenEither(thenEither));
  }

  private static BookingAndEblScenarioListBuilder uc6TDGetSIGet(
      BookingAndEblScenarioListBuilder... thenEither) {
    return uc6CarrierPublishDraftTransportDocument()
        .then(
            shipperGetTransportDocument(TD_DRAFT)
                .then(shipperGetShippingInstructions(SI_ANY, true).thenEither(thenEither)));
  }

  private static BookingAndEblScenarioListBuilder uc7TDGet(
      BookingAndEblScenarioListBuilder... thenEither) {
    return uc7ShipperApproveDraftTransportDocument()
        .then(shipperGetTransportDocument(TD_APPROVED).thenEither(thenEither));
  }

  private static BookingAndEblScenarioListBuilder uc8TDGet(
      BookingAndEblScenarioListBuilder... thenEither) {
    return uc8CarrierIssueTransportDocument()
        .then(shipperGetTransportDocument(TD_ISSUED).thenEither(thenEither));
  }

  private static BookingAndEblScenarioListBuilder uc9TDGet(
      BookingAndEblScenarioListBuilder... thenEither) {
    return uc9CarrierAwaitSurrenderRequestForAmendment()
        .then(
            shipperGetTransportDocument(TD_PENDING_SURRENDER_FOR_AMENDMENT).thenEither(thenEither));
  }

  private static BookingAndEblScenarioListBuilder uc10TDGet(
      BookingAndEblScenarioListBuilder... thenEither) {
    return uc10aCarrierAcceptSurrenderRequestForAmendment()
        .then(shipperGetTransportDocument(TD_SURRENDERED_FOR_AMENDMENT).thenEither(thenEither));
  }

  private static BookingAndEblScenarioListBuilder uc11TDGet(
      BookingAndEblScenarioListBuilder... thenEither) {
    return uc11CarrierVoidTDAndIssueAmendedTransportDocument()
        .then(shipperGetTransportDocument(TD_ISSUED).thenEither(thenEither));
  }
}
