package org.dcsa.conformance.standards.ebl;

import static org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus.*;
import static org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus.SI_RECEIVED;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.standards.ebl.action.*;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.sb.ScenarioManager;
import org.dcsa.conformance.standards.ebl.sb.ScenarioSingleStateStepHandler;
import org.dcsa.conformance.standards.ebl.sb.ScenarioStepHandler;

@RequiredArgsConstructor
public class EBLScenarioBuilder {

  static final String SCENARIO_SUITE_CONFORMANCE_SI_ONLY = "Conformance SI-only";
  static final String SCENARIO_SUITE_CONFORMANCE_TD_ONLY = "Conformance TD-only";
  static final String SCENARIO_SUITE_RI = "Reference Implementation";

  static final Map<String, Consumer<EBLScenarioBuilder>> SCENARIOS =
      Map.ofEntries(
          Map.entry(
              SCENARIO_SUITE_CONFORMANCE_SI_ONLY,
              EBLScenarioBuilder::createConformanceSiOnlyScenarios),
          Map.entry(
              SCENARIO_SUITE_CONFORMANCE_TD_ONLY,
              EBLScenarioBuilder::createConformanceTdOnlyScenarios),
          Map.entry(SCENARIO_SUITE_RI, EBLScenarioBuilder::createConformanceRIScenarios));

  private static final String EBL_API = "api";

  private static final String EBL_NOTIFICATIONS_API = "api";
  private static final String GET_EBL_SCHEMA_NAME = "ShippingInstructions";
  private static final String GET_TD_SCHEMA_NAME = "TransportDocument";
  private static final String POST_EBL_SCHEMA_NAME = "CreateShippingInstructions";
  private static final String PUT_EBL_SCHEMA_NAME = "UpdateShippingInstructions";
  private static final String PATCH_SI_SCHEMA_NAME = "shippinginstructions_documentReference_body";
  private static final String PATCH_TD_SCHEMA_NAME =
      "transportdocuments_transportDocumentReference_body";
  private static final String EBL_REF_STATUS_SCHEMA_NAME = "ShippingInstructionsRefStatus";
  private static final String TD_REF_STATUS_SCHEMA_NAME = "TransportDocumentRefStatus";
  private static final String EBL_SI_NOTIFICATION_SCHEMA_NAME = "ShippingInstructionsNotification";
  private static final String EBL_TD_NOTIFICATION_SCHEMA_NAME = "TransportDocumentNotification";

  private static final ConcurrentHashMap<String, JsonSchemaValidator> SCHEMA_CACHE =
      new ConcurrentHashMap<>();

  private final ScenarioManager scenarioManager;
  private final String standardVersion;
  private final String carrierPartyName;
  private final String shipperPartyName;

  public void createScenarioSuite(String scenarioSuite) {
    var generator = SCENARIOS.get(scenarioSuite);
    if (generator == null) {
      throw new IllegalArgumentException(
          "Invalid scenario suite name '%s'".formatted(scenarioSuite));
    }
    generator.accept(this);
  }

  private void createConformanceSiOnlyScenarios() {
    scenarioManager.openScenarioModule("Supported shipment types scenarios");

    EblScenarioState.initialScenarioState(scenarioManager)
        .branchForEach(ScenarioType.class, this::carrier_SupplyScenarioParameters)
        .then(this::_uc1_get)
        .then(this::_uc14_get)
        .finishScenario();

    scenarioManager.openScenarioModule("Carrier requested update scenarios");
    EblScenarioState.initialScenarioState(scenarioManager)
        .thenStep((s) -> this.carrier_SupplyScenarioParameters(s, ScenarioType.REGULAR_SWB))
        .then(this::_uc1_get)
        .then(this::_uc2_get)
        .then(this::_uc3_andAllSiOnlyPathsFrom)
        .finishScenario();

    scenarioManager.openScenarioModule("Shipper initiated update scenarios");
    EblScenarioState.initialScenarioState(scenarioManager)
        .thenStep((s) -> this.carrier_SupplyScenarioParameters(s, ScenarioType.REGULAR_SWB))
        .then(this::_uc1_get)
        .then(this::_uc3_andAllSiOnlyPathsFrom)
        .finishScenario();
  }

  private void createConformanceTdOnlyScenarios() {
    scenarioManager.openScenarioModule("Supported shipment types scenarios");
    EblScenarioState.initialScenarioState(scenarioManager, s -> s.withUsingTDRRefInGetDefault(true))
        .branchForEach(ScenarioType.class, this::carrier_SupplyScenarioParameters)
        .then((s) -> _uc6_get(s, true))
        .then(this::_uc8_get)
        .then(this::_uc12_get)
        .then(this::_uc13_get)
        .finishScenario();

    scenarioManager.openScenarioModule("Shipper interactions with transport document");
    EblScenarioState.initialScenarioState(scenarioManager, s -> s.withUsingTDRRefInGetDefault(true))
        .thenStep((s) -> this.carrier_SupplyScenarioParameters(s, ScenarioType.REGULAR_SWB))
        .then((s) -> _uc6_get(s, true))
      .branchingStep(
        bs -> {
          bs.inlineBranchBuilder().then(this::_uc7_get).then(this::_uc8_get).finishBranch();

          bs.branch(this::_uc8_get);

          bs.inlineBranchBuilder().then(this::_oob_amendment).then(this::_uc8_get).finishBranch();

          bs.inlineBranchBuilder()
            .then(this::_uc8_get)
            .then(this::_oob_amendment)
            .then(this::_uc9_get)
            .then(this::_uc10_get)
            .then(this::_uc11_get)
            .finishBranch();
          return bs.build();
        })
        .then(this::_uc12_get)
        .then(this::_uc13_get)
        .finishScenario();
  }

  private void createConformanceRIScenarios() {
    scenarioManager.openScenarioModule("Supported shipment types scenarios");
    EblScenarioState.initialScenarioState(scenarioManager)
        .branchForEach(ScenarioType.class, this::carrier_SupplyScenarioParameters)
        .then(this::_uc1_get)
        .then(this::_uc14_get)
        .finishScenario();

    // TODO: Port the state sequence diagram
  }

  private EblScenarioState carrier_SupplyScenarioParameters(
      EblScenarioState state, ScenarioType scenarioType) {
    return nextStateBuilder(
            state,
            previousAction ->
                new Carrier_SupplyScenarioParametersAction(carrierPartyName, scenarioType))
        .scenarioType(scenarioType)
        .build();
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc1_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(this::uc1_shipper_submitShippingInstructions)
      .thenStep(this::shipper_GetShippingInstructions);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc2_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(this::uc2_carrier_requestUpdateToShippingInstruction)
      .thenStep(this::shipper_GetShippingInstructions);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc3_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(s -> uc3_shipper_submitUpdatedShippingInstructions(s, s.isUsingTDRRefInGetDefault()))
      .thenStep(this::shipper_GetShippingInstructions);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc4a_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(this::uc4a_carrier_acceptUpdatedShippingInstructions)
      .thenStep(this::shipper_GetShippingInstructions);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc4d_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(this::uc4d_carrier_declineUpdatedShippingInstructions)
      .thenStep(this::shipper_GetShippingInstructions);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc5_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(s -> uc5_shipper_cancelUpdateToShippingInstructions(s, s.isUsingTDRRefInGetDefault()))
      .thenStep(this::shipper_GetShippingInstructions);
  }

  private ScenarioStepHandler<EblAction, EblScenarioState> _uc6_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler.then(s -> _uc6_get(s, false));
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc6_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler, boolean skipSISteps) {
    return stepHandler
      .thenStep(s -> uc6_carrier_publishDraftTransportDocument(s, skipSISteps))
      .thenStep(this::shipper_GetTransportDocument);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc7_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(this::uc7_shipper_approveDraftTransportDocument)
      .thenStep(this::shipper_GetTransportDocument);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc8_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(this::uc8_carrier_issueTransportDocument)
      .thenStep(this::shipper_GetTransportDocument);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc9_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(this::uc9_carrier_awaitSurrenderRequestForAmendment)
      .thenStep(this::shipper_GetTransportDocument);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc10_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(this::uc10a_carrier_acceptSurrenderRequestForAmendment)
      .thenStep(this::shipper_GetTransportDocument);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc11_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(this::uc11_carrier_voidTransportDocument)
      .thenStep(this::uc11i_carrier_issueAmendedTransportDocument)
      .thenStep(this::shipper_GetTransportDocument);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc12_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(this::uc12_carrier_awaitSurrenderRequestForDelivery)
      .thenStep(this::shipper_GetTransportDocument);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc13_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(this::uc13a_carrier_acceptSurrenderRequestForDelivery)
      .thenStep(this::shipper_GetTransportDocument);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _uc14_get(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler
      .thenStep(this::uc14_carrier_confirmShippingInstructionsComplete)
      .thenStep(this::shipper_GetShippingInstructions);
  }

  private ScenarioStepHandler<EblAction, EblScenarioState> _uc4a_uc14(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler.then(this::_uc4a_get).then(this::_uc14_get);
  }

  private ScenarioSingleStateStepHandler<EblAction, EblScenarioState> _oob_amendment(ScenarioSingleStateStepHandler<EblAction, EblScenarioState> stepHandler) {
    return stepHandler.thenStep(this::oob_carrier_processOutOfBoundTDUpdateRequest);
  }

  private ScenarioStepHandler<EblAction, EblScenarioState> _uc3_andAllSiOnlyPathsFrom(
      ScenarioStepHandler<EblAction, EblScenarioState> stateChainer) {
    return stateChainer
        .then(this::_uc3_get)
        .branchingStep(
            bs -> {
                bs.inlineBranchBuilder()
                    .then(this::_uc2_get)
                    .then(this::_uc3_get)
                    .then(this::_uc4a_uc14)
                    .finishBranch();

                bs.inlineBranchBuilder()
                    .then(this::_uc4a_get)
                    .then(this::_uc2_get)
                    .then(this::_uc3_get)
                    .then(this::_uc4a_uc14)
                    .finishBranch();

                bs.inlineBranchBuilder()
                    .then(this::_uc4a_get)
                    .then(this::_uc3_get)
                    .then(this::_uc4a_uc14)
                    .finishBranch();


                bs.inlineBranchBuilder()
                  .then(this::_uc4a_get)
                    .then(this::_uc14_get)
                    .finishBranch();

                bs.inlineBranchBuilder()
                    .then(this::_uc4d_get)
                    .then(this::_uc3_get)
                    .then(this::_uc4a_uc14)
                    .finishBranch();

                bs.inlineBranchBuilder()
                    .then(this::_uc5_get)
                    .then(this::_uc3_get)
                    .then(this::_uc4a_uc14)
                    .finishBranch();

                return bs.build();
            });
  }

  private EblScenarioState uc1_shipper_submitShippingInstructions(EblScenarioState state) {
    assert state.getShippingInstructionsStatus() == SI_START;
    return nextStateBuilder(
            state,
            previousAction ->
                new UC1_Shipper_SubmitShippingInstructionsAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(EBL_API, POST_EBL_SCHEMA_NAME),
                    resolveMessageSchemaValidator(EBL_API, EBL_REF_STATUS_SCHEMA_NAME),
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)))
        .shippingInstructionsStatus(SI_RECEIVED)
        .build();
  }

  private EblScenarioState uc2_carrier_requestUpdateToShippingInstruction(EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC2_Carrier_RequestUpdateToShippingInstructionsAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)))
        .shippingInstructionsStatus(SI_PENDING_UPDATE)
        .updatedShippingInstructionsStatus(null)
        .build();
  }

  private EblScenarioState uc3_shipper_submitUpdatedShippingInstructions(
      EblScenarioState state, boolean useTDRef) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC3_Shipper_SubmitUpdatedShippingInstructionsAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    useTDRef,
                    resolveMessageSchemaValidator(EBL_API, PUT_EBL_SCHEMA_NAME),
                    resolveMessageSchemaValidator(EBL_API, EBL_REF_STATUS_SCHEMA_NAME),
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)))
        .updatedShippingInstructionsStatus(SI_UPDATE_RECEIVED)
        .build();
  }

  private EblScenarioState uc4a_carrier_acceptUpdatedShippingInstructions(EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC4_Carrier_ProcessUpdateToShippingInstructionsAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
                    true))
        .shippingInstructionsStatus(SI_RECEIVED)
        .updatedShippingInstructionsStatus(SI_UPDATE_CONFIRMED)
        .build();
  }

  private EblScenarioState uc4d_carrier_declineUpdatedShippingInstructions(EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC4_Carrier_ProcessUpdateToShippingInstructionsAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
                    false))
        .updatedShippingInstructionsStatus(SI_UPDATE_DECLINED)
        .build();
  }

  private EblScenarioState uc5_shipper_cancelUpdateToShippingInstructions(
      EblScenarioState state, boolean useTDRef) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC5_Shipper_CancelUpdateToShippingInstructionsAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    useTDRef,
                    resolveMessageSchemaValidator(EBL_API, PATCH_SI_SCHEMA_NAME),
                    resolveMessageSchemaValidator(EBL_API, EBL_REF_STATUS_SCHEMA_NAME),
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)))
        .updatedShippingInstructionsStatus(SI_UPDATE_CANCELLED)
        .build();
  }

  private EblScenarioState uc6_carrier_publishDraftTransportDocument(
      EblScenarioState state, boolean skipSI) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC6_Carrier_PublishDraftTransportDocumentAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                    skipSI))
        .currentTransportDocumentStatus(TD_DRAFT)
        .build();
  }

  private EblScenarioState uc7_shipper_approveDraftTransportDocument(EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC7_Shipper_ApproveDraftTransportDocumentAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(EBL_API, PATCH_TD_SCHEMA_NAME),
                    resolveMessageSchemaValidator(EBL_API, TD_REF_STATUS_SCHEMA_NAME),
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)))
        .currentTransportDocumentStatus(TD_APPROVED)
        .build();
  }

  private EblScenarioState uc8_carrier_issueTransportDocument(EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC8_Carrier_IssueTransportDocumentAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)))
        .currentTransportDocumentStatus(TD_ISSUED)
        .build();
  }

  private EblScenarioState uc9_carrier_awaitSurrenderRequestForAmendment(EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC9_Carrier_AwaitSurrenderRequestForAmendmentAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)))
        .currentTransportDocumentStatus(TD_PENDING_SURRENDER_FOR_AMENDMENT)
        .build();
  }

  private EblScenarioState uc10a_carrier_acceptSurrenderRequestForAmendment(
      EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC10_Carrier_ProcessSurrenderRequestForAmendmentAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                    true))
        .currentTransportDocumentStatus(TD_SURRENDERED_FOR_AMENDMENT)
        .build();
  }

  private EblScenarioState uc10r_carrier_rejectSurrenderRequestForAmendment(
      EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC10_Carrier_ProcessSurrenderRequestForAmendmentAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                    false))
        .currentTransportDocumentStatus(TD_ISSUED)
        .build();
  }

  private EblScenarioState uc11_carrier_voidTransportDocument(EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC11v_Carrier_VoidTransportDocumentAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)))
        .currentTransportDocumentStatus(TD_VOIDED)
        .build();
  }

  private EblScenarioState uc11i_carrier_issueAmendedTransportDocument(EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC11i_Carrier_IssueAmendedTransportDocumentAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)))
        .currentTransportDocumentStatus(TD_ISSUED)
        .build();
  }

  private EblScenarioState uc12_carrier_awaitSurrenderRequestForDelivery(EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC12_Carrier_AwaitSurrenderRequestForDeliveryAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)))
        .currentTransportDocumentStatus(TD_PENDING_SURRENDER_FOR_DELIVERY)
        .build();
  }

  private EblScenarioState uc13a_carrier_acceptSurrenderRequestForDelivery(EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC13_Carrier_ProcessSurrenderRequestForDeliveryAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                    true))
        .currentTransportDocumentStatus(TD_SURRENDERED_FOR_DELIVERY)
        .build();
  }

  private EblScenarioState uc13r_carrier_rejectSurrenderRequestForDelivery(EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC13_Carrier_ProcessSurrenderRequestForDeliveryAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                    false))
        .currentTransportDocumentStatus(TD_ISSUED)
        .build();
  }

  private EblScenarioState uc14_carrier_confirmShippingInstructionsComplete(
      EblScenarioState state) {
    return nextStateBuilder(
            state,
            previousAction ->
                new UC14_Carrier_ConfirmShippingInstructionsCompleteAction(
                    carrierPartyName,
                    shipperPartyName,
                    previousAction,
                    resolveMessageSchemaValidator(
                        EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)))
        .shippingInstructionsStatus(SI_COMPLETED)
        .updatedShippingInstructionsStatus(null)
        .build();
  }

  private EblScenarioState shipper_GetShippingInstructions(EblScenarioState state) {
    return shipper_GetShippingInstructions(state, false, false, state.isUsingTDRRefInGetDefault());
  }

  private EblScenarioState shipper_GetShippingInstructions(
      EblScenarioState state, boolean requestAmendedSI, boolean recordTDR, boolean useTDRef) {
    return nextState(
        state,
        previousAction ->
            new Shipper_GetShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                previousAction,
                state.getShippingInstructionsStatus(),
                state.getUpdatedShippingInstructionsStatus(),
                resolveMessageSchemaValidator(EBL_API, GET_EBL_SCHEMA_NAME),
                requestAmendedSI,
                recordTDR,
                useTDRef));
  }

  private EblScenarioState shipper_GetTransportDocument(EblScenarioState state) {
    return nextState(
        state,
        previousAction ->
            new Shipper_GetTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                previousAction,
                state.getCurrentTransportDocumentStatus(),
                resolveMessageSchemaValidator(EBL_API, GET_TD_SCHEMA_NAME)));
  }

  private EblScenarioState oob_carrier_processOutOfBoundTDUpdateRequest(EblScenarioState state) {
    return nextState(
        state,
        previousAction ->
            new UCX_Carrier_TDOnlyProcessOutOfBandUpdateOrAmendmentRequestDraftTransportDocumentAction(
                carrierPartyName, shipperPartyName, previousAction));
  }

  private EblScenarioState _onlyHappyPathsFromHere(EblScenarioState state) {
    return nextStateBuilder(
      state,
      null
    ).usingHappyPathOnly(true)
      .build();
  }

  private EblScenarioState nextState(
      EblScenarioState previousState, Function<EblAction, EblAction> actionGenerator) {
    return previousState.toBuilder()
        .conformanceActionGenerator(actionGenerator)
        .previousStepState(previousState)
        .build();
  }

  private EblScenarioState.EblScenarioStateBuilder nextStateBuilder(
      EblScenarioState previousState, Function<EblAction, EblAction> actionGenerator) {
    return previousState.toBuilder()
        .conformanceActionGenerator(actionGenerator)
        .previousStepState(previousState);
  }

  private JsonSchemaValidator resolveMessageSchemaValidator(String apiName, String schema) {
    var schemaKey =
        standardVersion + Character.toString(0x1f) + apiName + Character.toString(0x1f) + schema;
    var schemaValidator = SCHEMA_CACHE.get(schemaKey);
    if (schemaValidator != null) {
      return schemaValidator;
    }
    String schemaFilePath =
        "/standards/ebl/schemas/ebl-%s-%s.json".formatted(apiName, standardVersion.toLowerCase());

    schemaValidator = JsonSchemaValidator.getInstance(schemaFilePath, schema);
    SCHEMA_CACHE.put(schemaKey, schemaValidator);
    return schemaValidator;
  }
}
