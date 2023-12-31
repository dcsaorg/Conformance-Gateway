package org.dcsa.conformance.standards.ebl;

import static org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus.*;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.*;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.ebl.action.*;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

@Slf4j
public class EblScenarioListBuilder extends ScenarioListBuilder<EblScenarioListBuilder> {

  private static final ThreadLocal<EblComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalShipperPartyName = new ThreadLocal<>();

  private static final String EBL_API = "api";

  private static final String EBL_NOTIFICATIONS_API = "notification";
  private static final String GET_EBL_SCHEMA_NAME = "ShippingInstructions";
  private static final String GET_TD_SCHEMA_NAME = "TransportDocument";
  private static final String POST_EBL_SCHEMA_NAME = "CreateShippingInstructions";
  private static final String PUT_EBL_SCHEMA_NAME = "UpdateShippingInstructions";
  private static final String PATCH_SI_SCHEMA_NAME = "shippinginstructions_documentReference_body";
  private static final String PATCH_TD_SCHEMA_NAME = "transportdocuments_transportDocumentReference_body";
  private static final String EBL_REF_STATUS_SCHEMA_NAME = "ShippingInstructionsRefStatus";
  private static final String TD_REF_STATUS_SCHEMA_NAME = "TransportDocumentRefStatus";
  private static final String EBL_SI_NOTIFICATION_SCHEMA_NAME = "ShippingInstructionsNotification";
  private static final String EBL_TD_NOTIFICATION_SCHEMA_NAME = "TransportDocumentNotification";

  public static EblScenarioListBuilder buildTree(
      EblComponentFactory componentFactory, String carrierPartyName, String shipperPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);
    return noAction().thenEither(
      carrier_SupplyScenarioParameters(ScenarioType.REGULAR).thenAllPathsFrom(SI_START),
      carrier_SupplyScenarioParameters(ScenarioType.REEFER).thenHappyPathFrom(SI_START),
      carrier_SupplyScenarioParameters(ScenarioType.DG).thenHappyPathFrom(SI_START)
    );
  }

  private EblScenarioListBuilder thenAllPathsFrom(
    ShippingInstructionsStatus shippingInstructionsStatus) {
    return thenAllPathsFrom(shippingInstructionsStatus, null);
  }

  private EblScenarioListBuilder thenAllPathsFrom(
      ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus memoryState) {
    return switch (shippingInstructionsStatus) {
      case SI_START -> then(
          uc1_shipper_submitShippingInstructions()
              .then(
                  shipper_GetShippingInstructions(SI_RECEIVED, TD_START)
                      .thenAllPathsFrom(SI_RECEIVED)));
      case SI_RECEIVED -> thenEither(
          uc2_carrier_requestUpdateToShippingInstruction()
              .then(
                  shipper_GetShippingInstructions(SI_PENDING_UPDATE, TD_START)
                      .thenAllPathsFrom(SI_PENDING_UPDATE)),
          uc3_shipper_submitUpdatedShippingInstructions()
              .then(
                  shipper_GetShippingInstructions(SI_RECEIVED, SI_UPDATE_RECEIVED, TD_START)
                      .thenAllPathsFrom(SI_UPDATE_RECEIVED, SI_RECEIVED)),
          uc6_carrier_publishDraftTransportDocument()
              .then(
                  shipper_GetShippingInstructions(SI_RECEIVED, SI_ANY, TD_DRAFT, true)
                      .then(shipper_GetTransportDocument(TD_DRAFT).thenAllPathsFrom(TD_DRAFT))));
      case SI_UPDATE_RECEIVED -> {
        if (memoryState == null) {
          throw new IllegalArgumentException(
              shippingInstructionsStatus.name() + " requires a memory state");
        }
        yield thenEither(
            uc2_carrier_requestUpdateToShippingInstruction()
                .then(
                    shipper_GetShippingInstructions(SI_PENDING_UPDATE, TD_START)
                        .thenHappyPathFrom(SI_PENDING_UPDATE)),
            uc4a_carrier_acceptUpdatedShippingInstructions()
                .then(
                    shipper_GetShippingInstructions(SI_RECEIVED, SI_UPDATE_CONFIRMED, TD_START)
                        .thenHappyPathFrom(SI_RECEIVED)),
            uc4d_carrier_declineUpdatedShippingInstructions()
                .then(
                    shipper_GetShippingInstructions(memoryState, SI_UPDATE_DECLINED, TD_START)
                        .thenHappyPathFrom(memoryState)),
            uc5_shipper_cancelUpdateToShippingInstructions()
              .then(
                shipper_GetShippingInstructions(memoryState, SI_UPDATE_CANCELLED, TD_START)
                  .thenHappyPathFrom(memoryState)));
      }
      case SI_PENDING_UPDATE -> then(
          uc3_shipper_submitUpdatedShippingInstructions()
              .then(
                  shipper_GetShippingInstructions(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, TD_START)
                    .thenEither(
                      noAction().thenHappyPathFrom(SI_PENDING_UPDATE),
                      // Special-case: UC2 -> UC3 -> UC5 -> ...
                      // - Doing thenAllPathsFrom(...) from UC2 would cause UC3 -> UC2 -> UC3 -> UC2 -> UC3 -> ...
                      //   patterns (it eventually resolves, but it is unhelpful many cases)
                      // To ensure that UC2 -> UC3 -> UC5 -> ... works properly we manually the subtree here.
                      // Otherwise, we would never test the UC2 -> UC3 -> UC5 -> ... flow because neither UC2 and UC5
                      // are considered happy paths.
                      uc5_shipper_cancelUpdateToShippingInstructions()
                        .then(
                          shipper_GetShippingInstructions(SI_PENDING_UPDATE, SI_UPDATE_CANCELLED, TD_START)
                            .thenEither(
                              noAction().thenHappyPathFrom(SI_PENDING_UPDATE),
                              uc3_shipper_submitUpdatedShippingInstructions().then(
                                shipper_GetShippingInstructions(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, TD_START)
                                  .thenHappyPathFrom(SI_UPDATE_RECEIVED, SI_PENDING_UPDATE)))))));
      case SI_UPDATE_CONFIRMED -> thenEither(
        noAction().thenHappyPathFrom(SI_UPDATE_CONFIRMED),
        // Just to validate that the "Carrier" does not get "stuck"
        uc2_carrier_requestUpdateToShippingInstruction()
          .then(
            shipper_GetShippingInstructions(SI_PENDING_UPDATE, TD_START)
              .thenHappyPathFrom(SI_PENDING_UPDATE)),
        uc3_shipper_submitUpdatedShippingInstructions()
          .then(
            shipper_GetShippingInstructions(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, TD_START)
              .thenHappyPathFrom(SI_UPDATE_RECEIVED, SI_PENDING_UPDATE)));
      case SI_UPDATE_CANCELLED, SI_UPDATE_DECLINED -> throw new AssertionError("Please use the black state rather than " + shippingInstructionsStatus.name());
      case SI_ANY -> throw new AssertionError("Not a real/reachable state");
      case SI_COMPLETED -> then(noAction());
    };
  }

  private EblScenarioListBuilder thenHappyPathFrom(
    ShippingInstructionsStatus shippingInstructionsStatus) {
    return thenHappyPathFrom(shippingInstructionsStatus, null);
  }

  private EblScenarioListBuilder thenHappyPathFrom(
      ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus memoryState) {
    return switch (shippingInstructionsStatus) {
      case SI_START -> then(
          uc1_shipper_submitShippingInstructions()
              .then(
                  shipper_GetShippingInstructions(SI_RECEIVED, TD_START)
                      .thenHappyPathFrom(SI_RECEIVED)));
      case SI_UPDATE_CONFIRMED, SI_RECEIVED -> then(
          uc6_carrier_publishDraftTransportDocument()
              .then(
                  shipper_GetShippingInstructions(SI_RECEIVED, SI_ANY, TD_DRAFT, true)
                      .then(shipper_GetTransportDocument(TD_DRAFT).thenHappyPathFrom(TD_DRAFT))));
      case SI_PENDING_UPDATE -> then(
            uc3_shipper_submitUpdatedShippingInstructions()
                .then(
                    shipper_GetShippingInstructions(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, TD_START)
                        .thenHappyPathFrom(SI_UPDATE_RECEIVED, SI_PENDING_UPDATE)));
      case SI_UPDATE_RECEIVED -> then(
        uc4a_carrier_acceptUpdatedShippingInstructions()
          .then(
            shipper_GetShippingInstructions(SI_RECEIVED, SI_UPDATE_CONFIRMED, TD_START)
              .thenHappyPathFrom(SI_UPDATE_CONFIRMED)));
      case SI_COMPLETED -> then(noAction());
      case SI_UPDATE_CANCELLED, SI_UPDATE_DECLINED -> throw new AssertionError(
          "Please use the black state rather than DECLINED");
      case SI_ANY -> throw new AssertionError("Not a real/reachable state");
    };
  }

  private EblScenarioListBuilder thenAllPathsFrom(TransportDocumentStatus transportDocumentStatus) {
    return switch (transportDocumentStatus) {
      case TD_DRAFT -> thenEither(
          uc8_carrier_issueTransportDocument()
              .then(
                  shipper_GetTransportDocument(TD_ISSUED)
                    // Using happy path here as requested in
                    // https://github.com/dcsaorg/Conformance-Gateway/pull/29#discussion_r1421732797
                    .thenHappyPathFrom(TD_ISSUED)),
          uc7_shipper_approveDraftTransportDocument()
            .then(shipper_GetTransportDocument(TD_APPROVED)
              .thenAllPathsFrom(TD_APPROVED))
        );
      case TD_APPROVED -> then(
        uc8_carrier_issueTransportDocument()
          .then(
            shipper_GetTransportDocument(TD_ISSUED)
              .thenAllPathsFrom(TD_ISSUED))
      );
      case TD_ISSUED -> thenEither(
        // TODO: We should have uc3 + uc4a + uc9 for an SI based amendment in parallel with UC9
        //  (without uc3 + uc4a, which is basically a booking based amendment)
        uc9_carrier_awaitSurrenderRequestForAmendment()
          .then(shipper_GetTransportDocument(TD_PENDING_SURRENDER_FOR_AMENDMENT)
            .thenAllPathsFrom(TD_PENDING_SURRENDER_FOR_AMENDMENT)),
        uc12_carrier_awaitSurrenderRequestForDelivery()
          .then(shipper_GetTransportDocument(TD_PENDING_SURRENDER_FOR_DELIVERY)
            .thenAllPathsFrom(TD_PENDING_SURRENDER_FOR_DELIVERY))
      );
      case TD_PENDING_SURRENDER_FOR_AMENDMENT -> thenEither(
          uc10a_carrier_acceptSurrenderRequestForAmendment().then(
            shipper_GetTransportDocument(TD_SURRENDERED_FOR_AMENDMENT)
              .thenAllPathsFrom(TD_SURRENDERED_FOR_AMENDMENT)
          ),
          uc10r_carrier_rejectSurrenderRequestForAmendment().then(
            shipper_GetTransportDocument(TD_ISSUED)
              .thenHappyPathFrom(TD_ISSUED)
          )
      );
      case TD_SURRENDERED_FOR_AMENDMENT -> then(
        uc11_carrier_voidTransportDocument()
          .then(uc11i_carrier_issueAmendedTransportDocument()
            .then(shipper_GetTransportDocument(TD_ISSUED)
              .thenHappyPathFrom(TD_ISSUED))));
      case TD_PENDING_SURRENDER_FOR_DELIVERY -> thenEither(
        uc13a_carrier_acceptSurrenderRequestForDelivery().then(
          shipper_GetTransportDocument(TD_SURRENDERED_FOR_DELIVERY)
            .thenAllPathsFrom(TD_SURRENDERED_FOR_DELIVERY)
        ),
        uc13r_carrier_rejectSurrenderRequestForDelivery().then(
          shipper_GetTransportDocument(TD_ISSUED)
            .thenHappyPathFrom(TD_ISSUED)
        )
      );
      case TD_SURRENDERED_FOR_DELIVERY -> thenHappyPathFrom(transportDocumentStatus);
      case TD_START, TD_ANY -> throw new AssertionError("Not a real/reachable state");
      case TD_VOIDED -> then(noAction());
    };
  }

  private EblScenarioListBuilder thenHappyPathFrom(TransportDocumentStatus transportDocumentStatus) {
    return switch (transportDocumentStatus) {
      case TD_DRAFT -> thenEither(
        uc8_carrier_issueTransportDocument()
          .then(
            shipper_GetTransportDocument(TD_ISSUED)
              .thenHappyPathFrom(TD_ISSUED)),
        uc7_shipper_approveDraftTransportDocument()
          .then(shipper_GetTransportDocument(TD_APPROVED)
            .thenHappyPathFrom(TD_APPROVED)));
      case TD_APPROVED -> then(
        uc8_carrier_issueTransportDocument()
          .then(
            shipper_GetTransportDocument(TD_ISSUED)
              .thenHappyPathFrom(TD_ISSUED))
      );
      case TD_ISSUED -> then(
        uc12_carrier_awaitSurrenderRequestForDelivery()
          .then(shipper_GetTransportDocument(TD_PENDING_SURRENDER_FOR_DELIVERY)
            .thenHappyPathFrom(TD_PENDING_SURRENDER_FOR_DELIVERY))
      );
      case TD_PENDING_SURRENDER_FOR_AMENDMENT -> then(
        uc10a_carrier_acceptSurrenderRequestForAmendment().then(
          shipper_GetTransportDocument(TD_SURRENDERED_FOR_AMENDMENT)
            .thenHappyPathFrom(TD_SURRENDERED_FOR_AMENDMENT)
        )
      );
      case TD_SURRENDERED_FOR_AMENDMENT -> then(
        uc11_carrier_voidTransportDocument()
          .then(uc11i_carrier_issueAmendedTransportDocument()
            .then(shipper_GetTransportDocument(TD_ISSUED)
              .thenHappyPathFrom(TD_ISSUED))));
      case TD_PENDING_SURRENDER_FOR_DELIVERY -> then(
        uc13a_carrier_acceptSurrenderRequestForDelivery().then(
          shipper_GetTransportDocument(TD_SURRENDERED_FOR_DELIVERY)
            .thenHappyPathFrom(TD_SURRENDERED_FOR_DELIVERY)
        )
      );
      case TD_SURRENDERED_FOR_DELIVERY -> then(
        uc14_carrier_confirmShippingInstructionsComplete().then(
          shipper_GetShippingInstructions(SI_COMPLETED, TD_SURRENDERED_FOR_DELIVERY)
        )
      );
      case TD_START, TD_ANY -> throw new AssertionError("Not a real/reachable state");
      case TD_VOIDED -> then(noAction());
    };
  }

  private EblScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static EblScenarioListBuilder noAction() {
    return new EblScenarioListBuilder(null);
  }

  private static EblScenarioListBuilder carrier_SupplyScenarioParameters(ScenarioType scenarioType) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    return new EblScenarioListBuilder(
        previousAction -> new Carrier_SupplyScenarioParametersAction(carrierPartyName, scenarioType));
  }


  private static EblScenarioListBuilder shipper_GetShippingInstructions(
    ShippingInstructionsStatus expectedSiStatus, TransportDocumentStatus expectedTdStatus) {
    return shipper_GetShippingInstructions(expectedSiStatus, null, expectedTdStatus);
  }

  private static EblScenarioListBuilder shipper_GetShippingInstructions(
    ShippingInstructionsStatus expectedSiStatus, ShippingInstructionsStatus expectedUpdatedSiStatus, TransportDocumentStatus expectedTdStatus, boolean recordTDR) {
    return shipper_GetShippingInstructions(expectedSiStatus, expectedUpdatedSiStatus, expectedTdStatus, false, recordTDR);
  }

  private static EblScenarioListBuilder shipper_GetShippingInstructions(
    ShippingInstructionsStatus expectedSiStatus,
    ShippingInstructionsStatus expectedUpdatedSiStatus,
    TransportDocumentStatus expectedTdStatus) {
    return shipper_GetShippingInstructions(expectedSiStatus, expectedUpdatedSiStatus, expectedTdStatus, false, false);
  }

  private static EblScenarioListBuilder shipper_GetShippingInstructions(
    ShippingInstructionsStatus expectedSiStatus,
    ShippingInstructionsStatus expectedUpdatedSiStatus,
    TransportDocumentStatus expectedTdStatus,
    boolean requestAmendedSI,
    boolean recordTDR) {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new Shipper_GetShippingInstructionsAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          expectedSiStatus,
          expectedUpdatedSiStatus,
          componentFactory.getMessageSchemaValidator(EBL_API, GET_EBL_SCHEMA_NAME),
          requestAmendedSI,
          recordTDR));
  }

  private static EblScenarioListBuilder shipper_GetTransportDocument(
    TransportDocumentStatus expectedTdStatus) {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new Shipper_GetTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                expectedTdStatus,
                componentFactory.getMessageSchemaValidator(EBL_API, GET_TD_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc1_shipper_submitShippingInstructions() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC1_Shipper_SubmitShippingInstructionsAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          componentFactory.getMessageSchemaValidator(EBL_API, POST_EBL_SCHEMA_NAME),
          componentFactory.getMessageSchemaValidator(EBL_API, EBL_REF_STATUS_SCHEMA_NAME),
          componentFactory.getMessageSchemaValidator(EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc3_shipper_submitUpdatedShippingInstructions() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC3_Shipper_SubmitUpdatedShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                componentFactory.getMessageSchemaValidator(EBL_API, PUT_EBL_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(EBL_API, EBL_REF_STATUS_SCHEMA_NAME),
                componentFactory.getMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc2_carrier_requestUpdateToShippingInstruction() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC2_Carrier_RequestUpdateToShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc4a_carrier_acceptUpdatedShippingInstructions() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC4_Carrier_ProcessUpdateToShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
              true));
  }

  private static EblScenarioListBuilder uc4d_carrier_declineUpdatedShippingInstructions() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC4_Carrier_ProcessUpdateToShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
              false));
  }

  private static EblScenarioListBuilder uc5_shipper_cancelUpdateToShippingInstructions() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC5_Shipper_CancelUpdateToShippingInstructionsAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          componentFactory.getMessageSchemaValidator(
            EBL_API, PATCH_SI_SCHEMA_NAME),
          componentFactory.getMessageSchemaValidator(
            EBL_API, EBL_REF_STATUS_SCHEMA_NAME),
          componentFactory.getMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc6_carrier_publishDraftTransportDocument() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC6_Carrier_PublishDraftTransportDocumentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          componentFactory.getMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc7_shipper_approveDraftTransportDocument() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC7_Shipper_ApproveDraftTransportDocumentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          componentFactory.getMessageSchemaValidator(
            EBL_API, PATCH_TD_SCHEMA_NAME),
          componentFactory.getMessageSchemaValidator(
            EBL_API, TD_REF_STATUS_SCHEMA_NAME),
          componentFactory.getMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc8_carrier_issueTransportDocument() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC8_Carrier_IssueTransportDocumentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          componentFactory.getMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc9_carrier_awaitSurrenderRequestForAmendment() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC9_Carrier_AwaitSurrenderRequestForAmendmentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          componentFactory.getMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc10a_carrier_acceptSurrenderRequestForAmendment() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC10_Carrier_ProcessSurrenderRequestForAmendmentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          componentFactory.getMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
          true));
  }

  private static EblScenarioListBuilder uc10r_carrier_rejectSurrenderRequestForAmendment() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC10_Carrier_ProcessSurrenderRequestForAmendmentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          componentFactory.getMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
          false));
  }

  private static EblScenarioListBuilder uc11_carrier_voidTransportDocument() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
            previousAction ->
                new UC11v_Carrier_VoidTransportDocumentAction(
                    carrierPartyName,
                    shipperPartyName,
                    (EblAction) previousAction,
                    componentFactory.getMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
    }

  private static EblScenarioListBuilder uc11i_carrier_issueAmendedTransportDocument() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC11i_Carrier_IssueAmendedTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }



  private static EblScenarioListBuilder uc12_carrier_awaitSurrenderRequestForDelivery() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC12_Carrier_AwaitSurrenderRequestForDeliveryAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc13a_carrier_acceptSurrenderRequestForDelivery() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC13_Carrier_ProcessSurrenderRequestForDeliveryAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          componentFactory.getMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
          true));
  }

  private static EblScenarioListBuilder uc13r_carrier_rejectSurrenderRequestForDelivery() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC13_Carrier_ProcessSurrenderRequestForDeliveryAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                false));
  }


  private static EblScenarioListBuilder uc14_carrier_confirmShippingInstructionsComplete() {
    EblComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC14_Carrier_ConfirmShippingInstructionsCompleteAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          componentFactory.getMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }
}
