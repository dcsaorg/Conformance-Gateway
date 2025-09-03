package org.dcsa.conformance.standards.ebl;

import static org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus.*;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  public static final String SCENARIO_SUITE_CONFORMANCE_SI_ONLY = "Conformance SI-only";
  public static final String SCENARIO_SUITE_CONFORMANCE_TD_ONLY = "Conformance TD-only";
  static final String SCENARIO_SUITE_CONFORMANCE_TD_AMENDMENTS = "Conformance TD Amendments";
  static final String SCENARIO_SUITE_SI_TD_COMBINED = "Conformance SI + TD";

  static final Set<String> SCENARIO_SUITES =
      Set.of(
          SCENARIO_SUITE_CONFORMANCE_SI_ONLY,
          SCENARIO_SUITE_CONFORMANCE_TD_ONLY,
          SCENARIO_SUITE_SI_TD_COMBINED,
          SCENARIO_SUITE_CONFORMANCE_TD_AMENDMENTS);

  private static final ThreadLocal<String> STANDARD_VERSION = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalShipperPartyName = new ThreadLocal<>();

  private static final String EBL_API = "api";

  private static final String EBL_NOTIFICATIONS_API = "api";
  private static final String GET_EBL_SCHEMA_NAME = "ShippingInstructions";
  private static final String GET_TD_SCHEMA_NAME = "TransportDocument";
  private static final String POST_EBL_SCHEMA_NAME = "CreateShippingInstructions";
  private static final String PUT_EBL_SCHEMA_NAME = "UpdateShippingInstructions";
  private static final String PATCH_SI_SCHEMA_NAME = "CancelShippingInstructionsUpdate";
  private static final String PATCH_TD_SCHEMA_NAME = "ApproveTransportDocument";
  private static final String RESPONSE_POST_SHIPPING_INSTRUCTIONS_SCHEMA_NAME = "CreateShippingInstructionsResponse";
  private static final String EBL_SI_NOTIFICATION_SCHEMA_NAME = "ShippingInstructionsNotification";
  private static final String EBL_TD_NOTIFICATION_SCHEMA_NAME = "TransportDocumentNotification";
  private static final String ERROR_RESPONSE_SCHEMA_NAME = "ErrorResponse";

  private static final ConcurrentHashMap<String, JsonSchemaValidator> SCHEMA_CACHE = new ConcurrentHashMap<>();

  public static LinkedHashMap<String, EblScenarioListBuilder> createModuleScenarioListBuilders(
      EblComponentFactory componentFactory, String standardVersion, String carrierPartyName, String shipperPartyName) {
    STANDARD_VERSION.set(standardVersion);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);

    if (SCENARIO_SUITE_CONFORMANCE_SI_ONLY.equals(componentFactory.getScenarioSuite())) {
      return createConformanceSiOnlyScenarios(false);
    }
    if (SCENARIO_SUITE_CONFORMANCE_TD_ONLY.equals(componentFactory.getScenarioSuite())) {
      return createConformanceTdOnlyScenarios(true);
    }
    if (SCENARIO_SUITE_SI_TD_COMBINED.equals(componentFactory.getScenarioSuite())) {
      return createSIandTDCombinedScenarios(false);
    }
    if (SCENARIO_SUITE_CONFORMANCE_TD_AMENDMENTS.equals(componentFactory.getScenarioSuite())) {
      return createTDAmendmentScenarios(false);
    }
    throw new IllegalArgumentException("Invalid scenario suite name '%s'".formatted(componentFactory.getScenarioSuite()));
  }

  private static LinkedHashMap<String, EblScenarioListBuilder> createConformanceSiOnlyScenarios(
      boolean isTd) {
    return Stream.of(
            Map.entry(
                "Supported shipment types scenarios",
                noAction()
                    .thenEither(
                        Arrays.stream(ScenarioType.values())
                            .map(
                                scenarioType ->
                                    carrierSupplyScenarioParameters(scenarioType, isTd)
                                        .then(
                                            uc1Get(
                                                SI_RECEIVED, false, uc14Get(SI_COMPLETED, false))))
                            .toList()
                            .toArray(new EblScenarioListBuilder[] {}))),
            Map.entry(
                "Carrier requested update scenarios",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL, isTd)
                    .then(
                        uc1Get(
                            SI_RECEIVED,
                            false,
                            uc2Get(
                                SI_PENDING_UPDATE, uc3AndAllSiOnlyPathsFrom(SI_PENDING_UPDATE))))),
            Map.entry(
                "Shipper initiated update scenarios",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL, isTd)
                    .then(uc1Get(SI_RECEIVED, false, uc3AndAllSiOnlyPathsFrom(SI_RECEIVED)))),
            Map.entry(
                "Carrier error response conformance",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL, isTd)
                    .then(
                        uc1ShipperSubmitShippingInstructions()
                            .then(shipperGetShippingInstructionsErrorScenario()))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static LinkedHashMap<String, EblScenarioListBuilder> createConformanceTdOnlyScenarios(
      boolean isTd) {
    return Stream.of(
            Map.entry(
                "Supported shipment types scenarios",
                noAction()
                    .thenEither(
                        Arrays.stream(ScenarioType.values())
                            .filter(scenarioType -> scenarioType != ScenarioType.REGULAR_SWB_AMF)
                            .map(scenarioType -> buildScenarioForType(scenarioType, isTd))
                            .toArray(EblScenarioListBuilder[]::new))),
            Map.entry(
                "Shipper interactions with transport document",
                noAction()
                    .then(
                        uc6Get(
                            true,
                            oobAmendment(uc6Get(false, uc8Get(uc12Get(uc13Get())))),
                            uc8Get(oobAmendment(uc9Get(uc10Get(uc11Get(uc12Get(uc13Get()))))))))),
            Map.entry(
                "Carrier error response conformance",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL, isTd)
                    .then(
                        uc6CarrierPublishDraftTransportDocument(true)
                            .then(shipperGetTransportDocumentErrorScenario()))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static LinkedHashMap<String, EblScenarioListBuilder> createTDAmendmentScenarios(
      boolean isTd) {
    return Stream.of(
            Map.entry(
                "Straight eBL",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL, isTd)
                    .thenEither(
                        uc1Get(
                            SI_RECEIVED,
                            false,
                            uc6Get(
                                false,
                                shipperGetShippingInstructionsRecordTDRef()
                                    .then(
                                        uc7Get(
                                            uc8Get(
                                                uc9Get(
                                                    uc10Get(
                                                        uc3Get(
                                                            SI_RECEIVED,
                                                            SI_UPDATE_RECEIVED,
                                                            true,
                                                            uc4aGet(
                                                                SI_RECEIVED,
                                                                SI_UPDATE_CONFIRMED,
                                                                true,
                                                                uc11Get(
                                                                    uc12Get(
                                                                        uc13Get(
                                                                            uc14Get(
                                                                                SI_COMPLETED,
                                                                                true))))))))))),
                                uc3Get(
                                    SI_RECEIVED,
                                    SI_UPDATE_RECEIVED,
                                    true,
                                    uc4aGet(
                                        SI_RECEIVED,
                                        SI_UPDATE_CONFIRMED,
                                        true,
                                        uc6Get(
                                            false,
                                            shipperGetShippingInstructionsRecordTDRef()
                                                .then(
                                                    uc7Get(
                                                        uc8Get(
                                                            uc12Get(
                                                                uc13Get(
                                                                    uc14Get(
                                                                        SI_COMPLETED,
                                                                        true))))))))))),
                        uc1Get(
                            SI_RECEIVED,
                            false,
                            uc6Get(
                                false,
                                shipperGetShippingInstructionsRecordTDRef()
                                    .then(
                                        uc7Get(
                                            uc8Get(
                                                uc3Get(
                                                    SI_RECEIVED,
                                                    SI_UPDATE_RECEIVED,
                                                    true,
                                                    uc4aGet(
                                                        SI_RECEIVED,
                                                        SI_UPDATE_CONFIRMED,
                                                        true,
                                                        uc9Get(
                                                            uc10Get(
                                                                uc11Get(
                                                                    uc12Get(
                                                                        uc13Get(
                                                                            uc14Get(
                                                                                SI_COMPLETED,
                                                                                true)))))))),
                                                uc9Get(
                                                    uc3Get(
                                                        SI_RECEIVED,
                                                        SI_UPDATE_RECEIVED,
                                                        true,
                                                        uc4aGet(
                                                            SI_RECEIVED,
                                                            SI_UPDATE_CONFIRMED,
                                                            true,
                                                            uc10Get(
                                                                uc11Get(
                                                                    uc12Get(
                                                                        uc13Get(
                                                                            uc14Get(
                                                                                SI_COMPLETED,
                                                                                true))))))))))))))),
            Map.entry(
                "Sea Waybill",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_SWB, isTd)
                    .then(
                        uc1Get(
                            SI_RECEIVED,
                            false,
                            uc6Get(
                                false,
                                shipperGetShippingInstructionsRecordTDRef()
                                    .then(
                                        uc3Get(
                                            SI_RECEIVED,
                                            SI_UPDATE_RECEIVED,
                                            true,
                                            uc4aGet(
                                                SI_RECEIVED,
                                                SI_UPDATE_CONFIRMED,
                                                true,
                                                uc6Get(
                                                    false,
                                                    shipperGetTransportDocument(TD_DRAFT)
                                                        .then(uc7Get(uc8Get())))))),
                                shipperGetShippingInstructionsRecordTDRef()
                                    .then(
                                        uc7Get(
                                            uc8Get(
                                                uc3Get(
                                                    SI_RECEIVED,
                                                    SI_UPDATE_RECEIVED,
                                                    true,
                                                    uc4aGet(
                                                        SI_RECEIVED,
                                                        SI_UPDATE_CONFIRMED,
                                                        true,
                                                        uc8Get()))))))))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static LinkedHashMap<String, EblScenarioListBuilder> createSIandTDCombinedScenarios(
      boolean isTd) {
    return Stream.of(
            Map.entry(
                "Straight eBL",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL, isTd)
                    .then(
                        uc1Get(
                            SI_RECEIVED,
                            false,
                            uc6Get(
                                false,
                                shipperGetShippingInstructionsRecordTDRef()
                                    .then(
                                        uc7Get(
                                            uc8Get(
                                                uc12Get(
                                                    uc13Get(uc14Get(SI_COMPLETED, true)))))))))),
            Map.entry(
                "Sea Waybill",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_SWB, isTd)
                    .then(
                        uc1Get(
                            SI_RECEIVED,
                            false,
                            uc6Get(
                                false,
                                shipperGetShippingInstructionsRecordTDRef()
                                    .then(uc7Get(uc8Get())))))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static EblScenarioListBuilder buildScenarioForType(ScenarioType type, boolean isTd) {
    if (type.isSWB()) {
      return uc6Get(true, uc7Get(uc8Get()));
    }
    return uc6Get(true, uc7Get(uc8Get(uc12Get(uc13Get()))));
  }

  private static EblScenarioListBuilder uc3AndAllSiOnlyPathsFrom(
      ShippingInstructionsStatus originalSiState) {
    return uc3Get(
        originalSiState,
        SI_UPDATE_RECEIVED,
        false,
        uc2Get(
            SI_PENDING_UPDATE,
            uc3Get(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, false, uc4aUc14(false))),
        uc3Get(originalSiState, SI_UPDATE_RECEIVED, false, uc4aUc14(false)),
        uc4aGet(
            SI_RECEIVED,
            SI_UPDATE_CONFIRMED,
            false,
            uc2Get(
                SI_PENDING_UPDATE,
                uc3Get(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, false, uc4aUc14(false))),
            uc3Get(SI_RECEIVED, SI_UPDATE_RECEIVED, false, uc4aUc14(false)),
            uc14Get(SI_COMPLETED, false)),
        uc4dGet(
            originalSiState,
            SI_UPDATE_DECLINED,
            uc3Get(originalSiState, SI_UPDATE_RECEIVED, false, uc4aUc14(false))),
        uc5Get(
            originalSiState,
            SI_UPDATE_CANCELLED,
            uc3Get(originalSiState, SI_UPDATE_RECEIVED, false, uc4aUc14(false))));
  }

  private static EblScenarioListBuilder uc1Get(
      ShippingInstructionsStatus siState,
      boolean useBothRef,
      EblScenarioListBuilder... thenEither) {
    return uc1ShipperSubmitShippingInstructions()
        .then(shipperGetShippingInstructions(siState, useBothRef).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc2Get(
      ShippingInstructionsStatus siState, EblScenarioListBuilder... thenEither) {
    return uc2CarrierRequestUpdateToShippingInstruction()
        .then(shipperGetShippingInstructions(siState, false).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc3Get(
      ShippingInstructionsStatus originalSiState,
      ShippingInstructionsStatus modifiedSiState,
      boolean useBothRef,
      EblScenarioListBuilder... thenEither) {
    // Calling both amemded SI GET and original SI GET after a UC3
    return uc3ShipperSubmitUpdatedShippingInstructions(originalSiState, useBothRef)
        .then(
            shipperGetShippingInstructions(originalSiState, modifiedSiState, true, useBothRef)
                .then(
                    shipperGetShippingInstructions(
                            originalSiState, modifiedSiState, false, useBothRef)
                        .thenEither(thenEither)));
  }

  private static EblScenarioListBuilder uc4aGet(
      ShippingInstructionsStatus originalSiState,
      ShippingInstructionsStatus modifiedSiState,
      boolean useBothRef,
      EblScenarioListBuilder... thenEither) {
    return uc4aCarrierAcceptUpdatedShippingInstructions()
        .then(
            shipperGetShippingInstructions(originalSiState, modifiedSiState, useBothRef)
                .thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc4dGet(
      ShippingInstructionsStatus originalSiState,
      ShippingInstructionsStatus modifiedSiState,
      EblScenarioListBuilder... thenEither) {
    return uc4dCarrierDeclineUpdatedShippingInstructions(originalSiState)
        .then(
            shipperGetShippingInstructions(originalSiState, modifiedSiState, false)
                .thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc4aUc14(boolean useBothRef) {
    return uc4aGet(SI_RECEIVED, SI_UPDATE_CONFIRMED, useBothRef, uc14Get(SI_COMPLETED, useBothRef));
  }

  private static EblScenarioListBuilder uc5Get(
      ShippingInstructionsStatus originalSiState,
      ShippingInstructionsStatus modifiedSiState,
      EblScenarioListBuilder... thenEither) {
    return uc5ShipperCancelUpdateToShippingInstructions(originalSiState, false)
        .then(
            shipperGetShippingInstructions(originalSiState, modifiedSiState, false)
                .thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc6Get(
      boolean start, EblScenarioListBuilder... thenEither) {
    return uc6CarrierPublishDraftTransportDocument(start)
        .then(shipperGetTransportDocument(TD_DRAFT).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc7Get(EblScenarioListBuilder... thenEither) {
    return uc7ShipperApproveDraftTransportDocument()
        .then(shipperGetTransportDocument(TD_APPROVED).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc8Get(EblScenarioListBuilder... thenEither) {
    return uc8CarrierIssueTransportDocument()
        .then(shipperGetTransportDocument(TD_ISSUED).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc9Get(EblScenarioListBuilder... thenEither) {
    return uc9CarrierAwaitSurrenderRequestForAmendment()
        .then(
            shipperGetTransportDocument(TD_PENDING_SURRENDER_FOR_AMENDMENT).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc10Get(EblScenarioListBuilder... thenEither) {
    return uc10aCarrierAcceptSurrenderRequestForAmendment()
        .then(shipperGetTransportDocument(TD_SURRENDERED_FOR_AMENDMENT).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc11Get(EblScenarioListBuilder... thenEither) {
    return uc11CarrierVoidTDandIssueAmendedTransportDocument()
        .then(shipperGetTransportDocument(TD_ISSUED).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc12Get(EblScenarioListBuilder... thenEither) {
    return uc12CarrierAwaitSurrenderRequestForDelivery()
        .then(
            shipperGetTransportDocument(TD_PENDING_SURRENDER_FOR_DELIVERY).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc13Get(EblScenarioListBuilder... thenEither) {
    return uc13aCarrierAcceptSurrenderRequestForDelivery()
        .then(shipperGetTransportDocument(TD_SURRENDERED_FOR_DELIVERY).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc14Get(
      ShippingInstructionsStatus siState, boolean useBothRef) {
    return uc14CarrierConfirmShippingInstructionsComplete()
        .then(shipperGetShippingInstructions(siState, useBothRef));
  }

  private static EblScenarioListBuilder oobAmendment(EblScenarioListBuilder... thenEither) {
    return oobCarrierProcessOutOfBoundTDUpdateRequest().thenEither(thenEither);
  }

  private EblScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static EblScenarioListBuilder noAction() {
    return new EblScenarioListBuilder(null);
  }

  private static EblScenarioListBuilder carrierSupplyScenarioParameters(
      ScenarioType scenarioType, boolean isTd) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String standardVersion = STANDARD_VERSION.get();
    JsonSchemaValidator requestSchemaValidator =
        resolveMessageSchemaValidator(EBL_API, POST_EBL_SCHEMA_NAME);
    return new EblScenarioListBuilder(
        previousAction ->
            new CarrierSupplyPayloadAction(
                carrierPartyName, scenarioType, standardVersion, requestSchemaValidator, isTd));
  }

  private static EblScenarioListBuilder shipperGetShippingInstructions(
      ShippingInstructionsStatus expectedSiStatus, boolean useBothRef) {
    return shipperGetShippingInstructions(expectedSiStatus, null, useBothRef);
  }

  private static EblScenarioListBuilder shipperGetShippingInstructions(
      ShippingInstructionsStatus expectedSiStatus,
      ShippingInstructionsStatus expectedUpdatedSiStatus,
      boolean requestAmendedSI,
      boolean useBothRef) {
    return shipperGetShippingInstructions(
        expectedSiStatus, expectedUpdatedSiStatus, requestAmendedSI, false, useBothRef);
  }

  private static EblScenarioListBuilder shipperGetShippingInstructionsRecordTDRef() {
    return shipperGetShippingInstructions(SI_RECEIVED, SI_ANY, false, true, false);
  }

  private static EblScenarioListBuilder shipperGetShippingInstructions(
      ShippingInstructionsStatus expectedSiStatus,
      ShippingInstructionsStatus expectedUpdatedSiStatus,
      boolean useBothRef) {
    return shipperGetShippingInstructions(
        expectedSiStatus, expectedUpdatedSiStatus, false, false, useBothRef);
  }

  private static EblScenarioListBuilder shipperGetShippingInstructions(
      ShippingInstructionsStatus expectedSiStatus,
      ShippingInstructionsStatus expectedUpdatedSiStatus,
      boolean requestAmendedSI,
      boolean recordTDR,
      boolean useBothRef) {
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
                useBothRef));
  }

  private static EblScenarioListBuilder shipperGetShippingInstructionsErrorScenario() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new ShipperGetShippingInstructionsErrorAction(
                shipperPartyName,
                carrierPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(EBL_API, ERROR_RESPONSE_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder shipperGetTransportDocument(
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

  private static EblScenarioListBuilder shipperGetTransportDocumentErrorScenario() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new ShipperGetTransportDocumentErrorAction(
                shipperPartyName,
                carrierPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(EBL_API, ERROR_RESPONSE_SCHEMA_NAME)));
    }

  private static EblScenarioListBuilder uc1ShipperSubmitShippingInstructions() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC1_Shipper_SubmitShippingInstructionsAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          resolveMessageSchemaValidator(EBL_API, POST_EBL_SCHEMA_NAME),
          resolveMessageSchemaValidator(EBL_API, RESPONSE_POST_SHIPPING_INSTRUCTIONS_SCHEMA_NAME),
          resolveMessageSchemaValidator(EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc3ShipperSubmitUpdatedShippingInstructions(
      ShippingInstructionsStatus expectedSiStatus, boolean useBothRef) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC3ShipperSubmitUpdatedShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                expectedSiStatus,
                useBothRef,
                resolveMessageSchemaValidator(EBL_API, PUT_EBL_SCHEMA_NAME),
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc2CarrierRequestUpdateToShippingInstruction() {
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

  private static EblScenarioListBuilder uc4aCarrierAcceptUpdatedShippingInstructions() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC4_Carrier_ProcessUpdateToShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                SI_RECEIVED,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
              true));
  }

  private static EblScenarioListBuilder uc4dCarrierDeclineUpdatedShippingInstructions(
      ShippingInstructionsStatus shippingInstructionsStatus) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC4_Carrier_ProcessUpdateToShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                shippingInstructionsStatus,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
              false));
  }

  private static EblScenarioListBuilder uc5ShipperCancelUpdateToShippingInstructions(
      ShippingInstructionsStatus expectedSIStatus, boolean useBothRef) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC5_Shipper_CancelUpdateToShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                expectedSIStatus,
                useBothRef,
                resolveMessageSchemaValidator(EBL_API, PATCH_SI_SCHEMA_NAME),
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc6CarrierPublishDraftTransportDocument(boolean skipSI) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC6_Carrier_PublishDraftTransportDocumentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          resolveMessageSchemaValidator(
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
          skipSI));
  }

  private static EblScenarioListBuilder uc7ShipperApproveDraftTransportDocument() {
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
            EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc8CarrierIssueTransportDocument() {
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

  private static EblScenarioListBuilder uc9CarrierAwaitSurrenderRequestForAmendment() {
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

  private static EblScenarioListBuilder uc10aCarrierAcceptSurrenderRequestForAmendment() {
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

  private static EblScenarioListBuilder uc11CarrierVoidTDandIssueAmendedTransportDocument() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC11_Carrier_voidTDAndIssueAmendedTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder uc12CarrierAwaitSurrenderRequestForDelivery() {
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

  private static EblScenarioListBuilder uc13aCarrierAcceptSurrenderRequestForDelivery() {
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

  private static EblScenarioListBuilder uc14CarrierConfirmShippingInstructionsComplete() {
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

  private static EblScenarioListBuilder oobCarrierProcessOutOfBoundTDUpdateRequest() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UCX_Carrier_TDOnlyProcessOutOfBandUpdateOrAmendmentRequestDraftTransportDocumentAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction));
  }


  private static JsonSchemaValidator resolveMessageSchemaValidator(String apiName, String schema) {
    var standardVersion = STANDARD_VERSION.get();
    var schemaKey = standardVersion + Character.toString(0x1f) + apiName + Character.toString(0x1f) + schema;
    var schemaValidator = SCHEMA_CACHE.get(schemaKey);
    if (schemaValidator != null) {
      return schemaValidator;
    }
    String schemaFilePath = "/standards/ebl/schemas/EBL_v%s.yaml".formatted(standardVersion);

    schemaValidator = JsonSchemaValidator.getInstance(schemaFilePath, schema);
    SCHEMA_CACHE.put(schemaKey, schemaValidator);
    return schemaValidator;
  }

}
