package org.dcsa.conformance.standards.ebl;

import static org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus.*;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.ebl.action.*;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

@Slf4j
public class EblScenarioListBuilder extends ScenarioListBuilder<EblScenarioListBuilder> {

  private static final ThreadLocal<String> STANDARD_VERSION = new ThreadLocal<>();
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

  private static final ConcurrentHashMap<String, JsonSchemaValidator> SCHEMA_CACHE = new ConcurrentHashMap<>();

  public static EblScenarioListBuilder buildTree(
      String standardVersion, String carrierPartyName, String shipperPartyName) {
    STANDARD_VERSION.set(standardVersion);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);
    return noAction().thenEither(
      carrier_SupplyScenarioParameters(ScenarioType.REGULAR_SWB).thenAllPathsFrom(SI_START, TD_START, false),
      carrier_SupplyScenarioParameters(ScenarioType.REGULAR_BOL).thenHappyPathFrom(SI_START, TD_START, false),
      carrier_SupplyScenarioParameters(ScenarioType.ACTIVE_REEFER).thenHappyPathFrom(SI_START, TD_START, false),
      carrier_SupplyScenarioParameters(ScenarioType.NON_OPERATING_REEFER).thenHappyPathFrom(SI_START, TD_START, false),
      carrier_SupplyScenarioParameters(ScenarioType.DG).thenHappyPathFrom(SI_START, TD_START, false),
      carrier_SupplyScenarioParameters(ScenarioType.REGULAR_2C_2U_1E).thenHappyPathFrom(SI_START, TD_START, false),
      carrier_SupplyScenarioParameters(ScenarioType.REGULAR_2C_2U_2E).thenHappyPathFrom(SI_START, TD_START, false),
      carrier_SupplyScenarioParameters(ScenarioType.REGULAR_SWB_SOC_AND_REFERENCES).thenHappyPathFrom(SI_START, TD_START, false),
      carrier_SupplyScenarioParameters(ScenarioType.REGULAR_SWB_AMF).thenHappyPathFrom(SI_START, TD_START, false)
    );
  }

  private EblScenarioListBuilder thenAllPathsFrom(
    ShippingInstructionsStatus shippingInstructionsStatus, TransportDocumentStatus transportDocumentStatus, boolean useTDRef) {
    return thenAllPathsFrom(shippingInstructionsStatus, null, transportDocumentStatus, useTDRef);
  }

  private EblScenarioListBuilder thenAllPathsFrom(
      ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus memoryState, TransportDocumentStatus transportDocumentStatus, boolean useTDRef) {
    return switch (shippingInstructionsStatus) {
      case SI_START -> {
        if (useTDRef) {
          throw new IllegalStateException("Cannot use transport document reference when submitting a TD");
        }
        if (transportDocumentStatus != TD_START) {
          throw new IllegalStateException("Cannot use transport document reference when submitting a TD");
        }
        yield then(
            uc1_shipper_submitShippingInstructions()
                .then(
                    shipper_GetShippingInstructions(SI_RECEIVED, false)
                        .thenAllPathsFrom(SI_RECEIVED, transportDocumentStatus, false)));
      }
      case SI_RECEIVED -> thenEither(
          uc2_carrier_requestUpdateToShippingInstruction()
              .then(
                  shipper_GetShippingInstructions(SI_PENDING_UPDATE, useTDRef)
                      .thenAllPathsFrom(SI_PENDING_UPDATE, transportDocumentStatus, useTDRef)),
          uc3_shipper_submitUpdatedShippingInstructions(useTDRef)
              .then(
                  shipper_GetShippingInstructions(SI_RECEIVED, SI_UPDATE_RECEIVED, useTDRef)
                      .thenAllPathsFrom(SI_UPDATE_RECEIVED, SI_RECEIVED, transportDocumentStatus, useTDRef)),
        switch (transportDocumentStatus) {
          case TD_START -> uc6_carrier_publishDraftTransportDocument().then(
            shipper_GetShippingInstructionsRecordTDRef()
              .then(shipper_GetTransportDocument(TD_DRAFT).thenAllPathsFrom(TD_DRAFT)));
          case TD_DRAFT -> uc6_carrier_publishDraftTransportDocument().then(
            shipper_GetShippingInstructionsRecordTDRef()
              .then(shipper_GetTransportDocument(TD_DRAFT).thenHappyPathFrom(TD_DRAFT)));
          case TD_ISSUED -> uc9_carrier_awaitSurrenderRequestForAmendment().then(
            shipper_GetTransportDocument(TD_PENDING_SURRENDER_FOR_AMENDMENT)
          ).thenHappyPathFrom(TD_PENDING_SURRENDER_FOR_AMENDMENT);
          default -> throw new IllegalStateException("Unexpected transportDocumentStatus: " + transportDocumentStatus.name());
        });
      case SI_UPDATE_RECEIVED -> {
        if (memoryState == null) {
          throw new IllegalArgumentException(
              shippingInstructionsStatus.name() + " requires a memory state");
        }
        yield thenEither(
            uc2_carrier_requestUpdateToShippingInstruction()
                .then(
                    shipper_GetShippingInstructions(SI_PENDING_UPDATE, useTDRef)
                        .thenHappyPathFrom(SI_PENDING_UPDATE, transportDocumentStatus, useTDRef)),
            uc4a_carrier_acceptUpdatedShippingInstructions()
                .then(
                    shipper_GetShippingInstructions(SI_RECEIVED, SI_UPDATE_CONFIRMED, useTDRef)
                        .thenHappyPathFrom(SI_RECEIVED, transportDocumentStatus, useTDRef)),
            uc4d_carrier_declineUpdatedShippingInstructions()
                .then(
                    shipper_GetShippingInstructions(memoryState, SI_UPDATE_DECLINED, useTDRef)
                        .thenHappyPathFrom(memoryState, transportDocumentStatus, useTDRef)),
            uc5_shipper_cancelUpdateToShippingInstructions()
                .then(
                    shipper_GetShippingInstructions(memoryState, SI_UPDATE_CANCELLED, useTDRef)
                        .thenHappyPathFrom(memoryState, transportDocumentStatus, useTDRef)));
      }
      case SI_PENDING_UPDATE -> then(
          uc3_shipper_submitUpdatedShippingInstructions(useTDRef)
              .then(
                  shipper_GetShippingInstructions(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, useTDRef)
                      .thenEither(
                          noAction().thenHappyPathFrom(SI_PENDING_UPDATE, transportDocumentStatus, useTDRef),
                          // Special-case: UC2 -> UC3 -> UC5 -> ...
                          // - Doing thenAllPathsFrom(...) from UC2 would cause UC3 -> UC2 -> UC3 ->
                          // UC2 -> UC3 -> ...
                          //   patterns (it eventually resolves, but it is unhelpful many cases)
                          // To ensure that UC2 -> UC3 -> UC5 -> ... works properly we manually do
                          // the subtree here.
                          // Otherwise, we would never test the UC2 -> UC3 -> UC5 -> ... flow
                          // because neither UC2 and UC3
                          // are considered happy paths.
                          uc5_shipper_cancelUpdateToShippingInstructions()
                              .then(
                                  shipper_GetShippingInstructions(
                                          SI_PENDING_UPDATE, SI_UPDATE_CANCELLED, useTDRef)
                                      .thenEither(
                                          noAction().thenHappyPathFrom(SI_PENDING_UPDATE, transportDocumentStatus, useTDRef),
                                          uc3_shipper_submitUpdatedShippingInstructions(useTDRef)
                                              .then(
                                                  shipper_GetShippingInstructions(
                                                          SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, useTDRef)
                                                      .thenHappyPathFrom(
                                                          SI_UPDATE_RECEIVED,
                                                          SI_PENDING_UPDATE,
                                                          transportDocumentStatus,
                                                          useTDRef)))))));
      case SI_UPDATE_CONFIRMED -> thenEither(
          noAction().thenHappyPathFrom(SI_UPDATE_CONFIRMED, transportDocumentStatus, useTDRef),
          // Just to validate that the "Carrier" does not get "stuck"
          uc2_carrier_requestUpdateToShippingInstruction()
              .then(
                  shipper_GetShippingInstructions(SI_PENDING_UPDATE, useTDRef)
                      .thenHappyPathFrom(SI_PENDING_UPDATE, transportDocumentStatus, useTDRef)),
          uc3_shipper_submitUpdatedShippingInstructions(useTDRef)
              .then(
                  shipper_GetShippingInstructions(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, useTDRef)
                      .thenHappyPathFrom(SI_UPDATE_RECEIVED, SI_PENDING_UPDATE, transportDocumentStatus,  useTDRef)));
      case SI_UPDATE_CANCELLED, SI_UPDATE_DECLINED -> throw new AssertionError(
          "Please use the black state rather than " + shippingInstructionsStatus.name());
      case SI_ANY -> throw new AssertionError("Not a real/reachable state");
      case SI_COMPLETED -> then(noAction());
    };
  }

  private EblScenarioListBuilder thenHappyPathFrom(
    ShippingInstructionsStatus shippingInstructionsStatus, TransportDocumentStatus transportDocumentStatus, boolean useTDRef) {
    return thenHappyPathFrom(shippingInstructionsStatus, null, transportDocumentStatus, useTDRef);
  }

  private EblScenarioListBuilder thenHappyPathFrom(
      ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus memoryState, TransportDocumentStatus transportDocumentStatus, boolean useTDRef) {
    return switch (shippingInstructionsStatus) {
      case SI_START -> then(
          uc1_shipper_submitShippingInstructions()
              .then(
                  shipper_GetShippingInstructions(SI_RECEIVED, useTDRef)
                      .thenHappyPathFrom(SI_RECEIVED, transportDocumentStatus, useTDRef)));
      case SI_UPDATE_CONFIRMED, SI_RECEIVED -> then(
        switch (transportDocumentStatus) {
          case TD_START, TD_DRAFT -> uc6_carrier_publishDraftTransportDocument().then(
            shipper_GetShippingInstructionsRecordTDRef()
              .then(shipper_GetTransportDocument(TD_DRAFT).thenHappyPathFrom(TD_DRAFT)));
          case TD_ISSUED -> uc9_carrier_awaitSurrenderRequestForAmendment().then(
            shipper_GetTransportDocument(TD_PENDING_SURRENDER_FOR_AMENDMENT)
              .thenHappyPathFrom(TD_PENDING_SURRENDER_FOR_AMENDMENT));
          default -> throw new IllegalStateException("Unexpected transportDocumentStatus: " + transportDocumentStatus.name());
        });
      case SI_PENDING_UPDATE -> then(
            uc3_shipper_submitUpdatedShippingInstructions(false)
                .then(
                    shipper_GetShippingInstructions(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, useTDRef)
                        .thenHappyPathFrom(SI_UPDATE_RECEIVED, SI_PENDING_UPDATE, transportDocumentStatus, useTDRef)));
      case SI_UPDATE_RECEIVED -> then(
        uc4a_carrier_acceptUpdatedShippingInstructions()
          .then(
            shipper_GetShippingInstructions(SI_RECEIVED, SI_UPDATE_CONFIRMED, useTDRef)
              .thenHappyPathFrom(SI_UPDATE_CONFIRMED, transportDocumentStatus, useTDRef)));
      case SI_COMPLETED -> then(noAction());
      case SI_UPDATE_CANCELLED, SI_UPDATE_DECLINED -> throw new AssertionError(
          "Please use the black state rather than " + shippingInstructionsStatus.name());
      case SI_ANY -> throw new AssertionError("Not a real/reachable state");
    };
  }

  private EblScenarioListBuilder thenAllPathsFrom(TransportDocumentStatus transportDocumentStatus) {
    return switch (transportDocumentStatus) {
      case TD_DRAFT -> thenEither(
        uc7_shipper_approveDraftTransportDocument()
          .then(shipper_GetTransportDocument(TD_APPROVED)
            .thenAllPathsFrom(TD_APPROVED)),
          uc8_carrier_issueTransportDocument()
              .then(
                  shipper_GetTransportDocument(TD_ISSUED)
                    // Using happy path here as requested in
                    // https://github.com/dcsaorg/Conformance-Gateway/pull/29#discussion_r1421732797
                    .thenHappyPathFrom(TD_ISSUED)),
          uc3_shipper_submitUpdatedShippingInstructions(true)
            .thenHappyPathFrom(SI_UPDATE_RECEIVED, TD_DRAFT, true),
        uc3_shipper_submitUpdatedShippingInstructions(false)
          .thenHappyPathFrom(SI_UPDATE_RECEIVED, TD_DRAFT, false)
      );
      case TD_APPROVED -> then(
        uc8_carrier_issueTransportDocument()
          .then(
            shipper_GetTransportDocument(TD_ISSUED)
              .thenAllPathsFrom(TD_ISSUED))
      );
      case TD_ISSUED -> thenEither(
        uc3_shipper_submitUpdatedShippingInstructions(true)
          .thenHappyPathFrom(SI_UPDATE_RECEIVED, TD_ISSUED, true),
        uc3_shipper_submitUpdatedShippingInstructions(false)
          .thenHappyPathFrom(SI_UPDATE_RECEIVED, TD_ISSUED, false),
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
        uc14_carrier_confirmShippingInstructionsComplete().thenEither(
          shipper_GetShippingInstructions(SI_COMPLETED, false),
          shipper_GetShippingInstructions(SI_COMPLETED, true)
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


  private static EblScenarioListBuilder shipper_GetShippingInstructions(ShippingInstructionsStatus expectedSiStatus,
                                                                        boolean useTDRef) {
    return shipper_GetShippingInstructions(expectedSiStatus, null, useTDRef);
  }

  private static EblScenarioListBuilder shipper_GetShippingInstructionsRecordTDRef() {
    return shipper_GetShippingInstructions(SI_RECEIVED, SI_ANY, false, true, false);
  }

  private static EblScenarioListBuilder shipper_GetShippingInstructions(
    ShippingInstructionsStatus expectedSiStatus,
    ShippingInstructionsStatus expectedUpdatedSiStatus,
    boolean useTDRef
  ) {
    return shipper_GetShippingInstructions(expectedSiStatus, expectedUpdatedSiStatus, false, false, useTDRef);
  }


  private static EblScenarioListBuilder shipper_GetShippingInstructions(
    ShippingInstructionsStatus expectedSiStatus,
    ShippingInstructionsStatus expectedUpdatedSiStatus,
    boolean requestAmendedSI,
    boolean recordTDR,
    boolean useTDRef) {
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
          resolveMessageSchemaValidator(EBL_API, GET_EBL_SCHEMA_NAME),
          requestAmendedSI,
          recordTDR,
          useTDRef));
  }

  private static EblScenarioListBuilder shipper_GetTransportDocument(
    TransportDocumentStatus expectedTdStatus) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new Shipper_GetTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                expectedTdStatus,
                resolveMessageSchemaValidator(EBL_API, GET_TD_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc1_shipper_submitShippingInstructions() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC1_Shipper_SubmitShippingInstructionsAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          resolveMessageSchemaValidator(EBL_API, POST_EBL_SCHEMA_NAME),
          resolveMessageSchemaValidator(EBL_API, EBL_REF_STATUS_SCHEMA_NAME),
          resolveMessageSchemaValidator(EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc3_shipper_submitUpdatedShippingInstructions(boolean useTDRef) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC3_Shipper_SubmitUpdatedShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                useTDRef,
                resolveMessageSchemaValidator(EBL_API, PUT_EBL_SCHEMA_NAME),
                resolveMessageSchemaValidator(EBL_API, EBL_REF_STATUS_SCHEMA_NAME),
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc2_carrier_requestUpdateToShippingInstruction() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC2_Carrier_RequestUpdateToShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc4a_carrier_acceptUpdatedShippingInstructions() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC4_Carrier_ProcessUpdateToShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
              true));
  }

  private static EblScenarioListBuilder uc4d_carrier_declineUpdatedShippingInstructions() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC4_Carrier_ProcessUpdateToShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
              false));
  }

  private static EblScenarioListBuilder uc5_shipper_cancelUpdateToShippingInstructions() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC5_Shipper_CancelUpdateToShippingInstructionsAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          resolveMessageSchemaValidator(
            EBL_API, PATCH_SI_SCHEMA_NAME),
          resolveMessageSchemaValidator(
            EBL_API, EBL_REF_STATUS_SCHEMA_NAME),
          resolveMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc6_carrier_publishDraftTransportDocument() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC6_Carrier_PublishDraftTransportDocumentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          resolveMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc7_shipper_approveDraftTransportDocument() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC7_Shipper_ApproveDraftTransportDocumentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          resolveMessageSchemaValidator(
            EBL_API, PATCH_TD_SCHEMA_NAME),
          resolveMessageSchemaValidator(
            EBL_API, TD_REF_STATUS_SCHEMA_NAME),
          resolveMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc8_carrier_issueTransportDocument() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC8_Carrier_IssueTransportDocumentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          resolveMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc9_carrier_awaitSurrenderRequestForAmendment() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC9_Carrier_AwaitSurrenderRequestForAmendmentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          resolveMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc10a_carrier_acceptSurrenderRequestForAmendment() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC10_Carrier_ProcessSurrenderRequestForAmendmentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          resolveMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
          true));
  }

  private static EblScenarioListBuilder uc10r_carrier_rejectSurrenderRequestForAmendment() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC10_Carrier_ProcessSurrenderRequestForAmendmentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          resolveMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
          false));
  }

  private static EblScenarioListBuilder uc11_carrier_voidTransportDocument() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
            previousAction ->
                new UC11v_Carrier_VoidTransportDocumentAction(
                    carrierPartyName,
                    shipperPartyName,
                    (EblAction) previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
    }

  private static EblScenarioListBuilder uc11i_carrier_issueAmendedTransportDocument() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC11i_Carrier_IssueAmendedTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }



  private static EblScenarioListBuilder uc12_carrier_awaitSurrenderRequestForDelivery() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC12_Carrier_AwaitSurrenderRequestForDeliveryAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc13a_carrier_acceptSurrenderRequestForDelivery() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC13_Carrier_ProcessSurrenderRequestForDeliveryAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          resolveMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
          true));
  }

  private static EblScenarioListBuilder uc13r_carrier_rejectSurrenderRequestForDelivery() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC13_Carrier_ProcessSurrenderRequestForDeliveryAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                false));
  }


  private static EblScenarioListBuilder uc14_carrier_confirmShippingInstructionsComplete() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC14_Carrier_ConfirmShippingInstructionsCompleteAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          resolveMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }

  private static JsonSchemaValidator resolveMessageSchemaValidator(String apiName, String schema) {
    var standardVersion = STANDARD_VERSION.get();
    var schemaKey = standardVersion + Character.toString(0x1f) + apiName + Character.toString(0x1f) + schema;
    var schemaValidator = SCHEMA_CACHE.get(schemaKey);
    if (schemaValidator != null) {
      return schemaValidator;
    }
    String schemaFilePath = "/standards/ebl/schemas/ebl-%s-%s.json"
      .formatted(apiName, standardVersion.toLowerCase());

    schemaValidator = JsonSchemaValidator.getInstance(schemaFilePath, schema);
    SCHEMA_CACHE.put(schemaKey, schemaValidator);
    return schemaValidator;
  }

}
