package org.dcsa.conformance.standards.ebl;

import static org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus.*;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.dcsa.conformance.core.util.MapUtils;
import org.dcsa.conformance.standards.ebl.action.*;
import org.dcsa.conformance.standards.ebl.checks.ScenarioType;
import org.dcsa.conformance.standards.ebl.party.AmendedTransportDocumentStatus;
import org.dcsa.conformance.standards.ebl.party.EblRole;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

@Slf4j
public class EblScenarioListBuilder extends ScenarioListBuilder<EblScenarioListBuilder> {

  public static final String SCENARIO_SUITE_CONFORMANCE_SI = "Conformance SI";
  public static final String SCENARIO_SUITE_CONFORMANCE_TD = "Conformance TD";
  static final String SCENARIO_SUITE_CONFORMANCE_TD_AMENDMENTS = "Conformance TD Amendments";
  static final String SCENARIO_SUITE_SI_TD_COMBINED = "Conformance SI + TD";

  static final Set<String> SCENARIO_SUITES =
      Set.of(
          SCENARIO_SUITE_CONFORMANCE_SI,
          SCENARIO_SUITE_CONFORMANCE_TD);

  private static final ThreadLocal<String> STANDARD_VERSION = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalShipperPartyName = new ThreadLocal<>();
  private static final ThreadLocal<Boolean> threadLocalIsWithNotifications = new ThreadLocal<>();

  private static final String EBL_API = "api";

  private static final String EBL_NOTIFICATIONS_API = "api";
  public static final String GET_EBL_SCHEMA_NAME = "ShippingInstructions";
  public static final String GET_TD_SCHEMA_NAME = "TransportDocument";
  public static final String GET_TD_AMENDMENT_SCHEMA_NAME = "TransportDocumentAmend";
  public static final String POST_EBL_SCHEMA_NAME = "CreateShippingInstructions";
  private static final String PUT_EBL_SCHEMA_NAME = "UpdateShippingInstructions";
  private static final String PATCH_SI_SCHEMA_NAME = "CancelShippingInstructionsUpdate";
  public static final String PATCH_TD_SCHEMA_NAME = "ApproveTransportDocument";
  public static final String RESPONSE_POST_SHIPPING_INSTRUCTIONS_SCHEMA_NAME =
      "CreateShippingInstructionsResponse";
  public static final String EBL_SI_NOTIFICATION_SCHEMA_NAME = "ShippingInstructionsNotification";
  public static final String EBL_TD_NOTIFICATION_SCHEMA_NAME = "TransportDocumentNotification";
  private static final String ERROR_RESPONSE_SCHEMA_NAME = "ErrorResponse";

  private static final ConcurrentHashMap<String, JsonSchemaValidator> SCHEMA_CACHE =
      new ConcurrentHashMap<>();

  public static LinkedHashMap<String, EblScenarioListBuilder> createModuleScenarioListBuilders(
      EblComponentFactory componentFactory,
      Set<String> testedPartyRoleNames,
      boolean isWithNotifications,
      String standardVersion,
      String carrierPartyName,
      String shipperPartyName) {
    STANDARD_VERSION.set(standardVersion);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);
    threadLocalIsWithNotifications.set(isWithNotifications);

    if (SCENARIO_SUITE_CONFORMANCE_SI.equals(componentFactory.getScenarioSuite())) {
      return createConformanceSiOnlyScenarios(testedPartyRoleNames, false);
    }
    if (SCENARIO_SUITE_CONFORMANCE_TD.equals(componentFactory.getScenarioSuite())) {
      return createConformanceTdOnlyScenarios(testedPartyRoleNames);
    }
    // Disabled legacy suites (implementation retained for possible future re-enablement):
    // if (SCENARIO_SUITE_SI_TD_COMBINED.equals(componentFactory.getScenarioSuite())) {
    //   return createSIandTDCombinedScenarios(false);
    // }
    // if (SCENARIO_SUITE_CONFORMANCE_TD_AMENDMENTS.equals(componentFactory.getScenarioSuite())) {
    //   return createTDAmendmentScenarios(false);
    // }
    throw new IllegalArgumentException(
        "Invalid scenario suite name '%s'".formatted(componentFactory.getScenarioSuite()));
  }

  private static LinkedHashMap<String, EblScenarioListBuilder> createConformanceSiOnlyScenarios(
      Set<String> testedPartyRoleNames, boolean isTd) {
    var scenarios = new LinkedHashMap<String, EblScenarioListBuilder>();
    boolean includeCarrier = testedPartyRoleNames.contains(EblRole.CARRIER.getConfigName());
    boolean includeShipper = testedPartyRoleNames.contains(EblRole.SHIPPER.getConfigName());
    boolean includeBoth = includeCarrier && includeShipper;

    if (includeCarrier) {
      carrierConformanceSiOnlyScenarios(isTd)
          .forEach(
              (name, builder) ->
                  scenarios.put(includeBoth ? "Carrier - " + name : name, builder));
    }
    if (includeShipper) {
      shipperConformanceSiOnlyScenarios(isTd)
          .forEach(
              (name, builder) ->
                  scenarios.put(includeBoth ? "Shipper - " + name : name, builder));
    }
    return scenarios;
  }

  private static Map<String, EblScenarioListBuilder> carrierConformanceSiOnlyScenarios(boolean isTd) {
    var scenarios = new LinkedHashMap<String, EblScenarioListBuilder>();
    scenarios.put("Required Sea Waybill scenario", requiredSiScenarios(ScenarioType.REGULAR_SWB, isTd));
    scenarios.put("Required Straight B/L scenario", requiredSiScenarios(ScenarioType.REGULAR_STRAIGHT_BL, isTd));
    scenarios.put("Required Negotiable B/L scenario", requiredSiScenarios(ScenarioType.REGULAR_NEGOTIABLE_BL, isTd));
    scenarios.put("Optional (report-only) scenarios", carrierOptionalSiScenarios(isTd).asOptionalReportOnlyScenario());
    return scenarios;
  }

  private static Map<String, EblScenarioListBuilder> shipperConformanceSiOnlyScenarios(boolean isTd) {
    var scenarios = new LinkedHashMap<String, EblScenarioListBuilder>();
    scenarios.put("Required Sea Waybill scenario", shipperRequiredSiScenarios(ScenarioType.REGULAR_SWB, isTd));
    scenarios.put("Required Straight B/L scenario", shipperRequiredSiScenarios(ScenarioType.REGULAR_STRAIGHT_BL, isTd));
    scenarios.put("Required Negotiable B/L scenario", shipperRequiredSiScenarios(ScenarioType.REGULAR_NEGOTIABLE_BL, isTd));
    scenarios.put("Optional (report-only) scenarios", shipperOptionalSiScenarios(isTd).asOptionalReportOnlyScenario());
    return scenarios;
  }

  private static EblScenarioListBuilder requiredSiScenarios(
      ScenarioType scenarioType, boolean isTd) {
    return carrierSupplyScenarioParameters(scenarioType, isTd)
        .then(
            uc1ShipperSubmitShippingInstructions()
                .then(shipperGetShippingInstructions(SI_RECEIVED, false)));
  }

  private static EblScenarioListBuilder shipperRequiredSiScenarios(
      ScenarioType scenarioType, boolean isTd) {
    return uc1ShipperSubmitShippingInstructionsStandalone(scenarioType, uc1TitleFor(scenarioType))
        .then(shipperGetShippingInstructions(SI_RECEIVED, false));
  }

  private static EblScenarioListBuilder carrierOptionalSiScenarios(boolean isTd) {
    return carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL, isTd, false, true)
        .then(
            uc1ShipperSubmitShippingInstructions()
                .thenEither(
                    uc2CarrierRequestedUpdateThenGetPendingPath(),
                    uc3ShipperSubmitUpdateAcceptedPath(),
                    uc4aCarrierConfirmUpdatedShippingInstructionsPath(),
                    uc4dCarrierDeclineUpdatedShippingInstructionsPath(),
                    retrieveUpdatedSiContentPath(),
                    uc5UpdateCancelled2xxPath(),
                    uc16DeclinedSiPath(),
                    uc15CancelledSi2xxPath(),
                    uc14ConfirmSiCompletedPath()));
  }

  private static EblScenarioListBuilder shipperOptionalSiScenarios(boolean isTd) {
    return uc1ShipperSubmitShippingInstructionsStandalone(ScenarioType.REGULAR_SWB)
        .thenEither(
            uc2CarrierRequestedThenShipperUpdatePath(),
            retrieveUpdatedSiContentPath(),
            uc5UpdateCancelled2xxPath(),
            uc15CancelledSi2xxPath());
  }

  private static EblScenarioListBuilder uc2CarrierRequestedUpdateThenGetPendingPath() {
    return uc2CarrierRequestUpdateToShippingInstruction()
        .then(shipperGetShippingInstructionsSkippable(SI_PENDING_UPDATE, false));
  }

  private static EblScenarioListBuilder uc2CarrierRequestedThenShipperUpdatePath() {
    return uc2CarrierRequestUpdateToShippingInstruction()
        .then(uc3ShipperSubmitUpdatedShippingInstructions(SI_PENDING_UPDATE, false));
  }

  private static EblScenarioListBuilder uc3ShipperSubmitUpdateAcceptedPath() {
    return uc3ShipperSubmitUpdatedShippingInstructions(SI_RECEIVED, false);
  }

  private static EblScenarioListBuilder retrieveUpdatedSiContentPath() {
    return uc3ShipperSubmitUpdatedShippingInstructions(SI_RECEIVED, false)
        .then(shipperGetShippingInstructionsSkippable(SI_RECEIVED, SI_UPDATE_RECEIVED, true, false));
  }

  private static EblScenarioListBuilder uc4aCarrierConfirmUpdatedShippingInstructionsPath() {
    return uc3ShipperSubmitUpdatedShippingInstructions(SI_RECEIVED, false)
        .then(
            uc4aCarrierAcceptUpdatedShippingInstructions()
                .then(
                    shipperGetShippingInstructionsSkippable(
                        SI_RECEIVED, SI_UPDATE_CONFIRMED, false)));
  }

  private static EblScenarioListBuilder uc4dCarrierDeclineUpdatedShippingInstructionsPath() {
    return uc3ShipperSubmitUpdatedShippingInstructions(SI_RECEIVED, false)
        .then(
            uc4dCarrierDeclineUpdatedShippingInstructions(SI_RECEIVED)
                .then(
                    shipperGetShippingInstructionsSkippable(
                        SI_RECEIVED, SI_UPDATE_DECLINED, false)));
  }

  private static EblScenarioListBuilder uc5UpdateCancelled2xxPath() {
    return uc3ShipperSubmitUpdatedShippingInstructions(SI_RECEIVED, false)
        .then(uc5ShipperCancelUpdateToShippingInstructions(SI_RECEIVED, false));
  }

  private static EblScenarioListBuilder uc16DeclinedSiPath() {
    return uc16CarrierDeclineShippingInstructions()
        .then(shipperGetShippingInstructionsSkippable(SI_DECLINED, false));
  }

  private static EblScenarioListBuilder uc15CancelledSi2xxPath() {
    return uc15ShipperCancelShippingInstructions();
  }

  private static EblScenarioListBuilder uc14ConfirmSiCompletedPath() {
    return uc14CarrierConfirmShippingInstructionsComplete()
        .then(shipperGetShippingInstructionsSkippable(SI_COMPLETED, false));
  }

  private static boolean isSupportedScenarioType(ScenarioType scenarioType) {
    return scenarioType != ScenarioType.DG
        && scenarioType != ScenarioType.ACTIVE_REEFER
        && scenarioType != ScenarioType.NON_OPERATING_REEFER
        && scenarioType != ScenarioType.REGULAR_NO_COMMODITY_SUBREFERENCE;
  }

  private static LinkedHashMap<String, EblScenarioListBuilder> createConformanceTdOnlyScenarios(
      Set<String> testedPartyRoleNames) {
    Map<String, Map<String, EblScenarioListBuilder>> partyScenarios =
        MapUtils.orderedMap(
            Map.entry(
                EblRole.CARRIER.getConfigName(),
                MapUtils.orderedMap(
                    Map.entry(
                        "Required Sea Waybill scenario",
                        carrierRequiredTdScenario(ScenarioType.REGULAR_SWB)),
                    Map.entry(
                        "Required Straight B/L scenario",
                        carrierRequiredTdScenario(ScenarioType.REGULAR_STRAIGHT_BL)),
                    Map.entry(
                        "Required Negotiable B/L scenario",
                        carrierRequiredTdScenario(ScenarioType.REGULAR_NEGOTIABLE_BL)),
                    Map.entry(
                        "Optional (report-only) scenarios",
                        noAction()
                            .thenEither(
                                carrierGetDirectAmendmentScenario(),
                                carrierProcessDirectAmendmentScenario(true),
                                carrierProcessDirectAmendmentScenario(false),
                                carrierCancelDirectAmendmentScenario())
                            .asOptionalReportOnlyScenario()))),
            Map.entry(
                EblRole.SHIPPER.getConfigName(),
                MapUtils.orderedMap(
                    Map.entry(
                        "Required Sea Waybill scenario",
                        shipperRequiredTdScenario(ScenarioType.REGULAR_SWB)),
                    Map.entry(
                        "Required Straight B/L scenario",
                        shipperRequiredTdScenario(ScenarioType.REGULAR_STRAIGHT_BL)),
                    Map.entry(
                        "Required Negotiable B/L scenario",
                        shipperRequiredTdScenario(ScenarioType.REGULAR_NEGOTIABLE_BL)),
                    Map.entry(
                        "Optional (report-only) scenarios",
                        noAction()
                            .thenEither(
                                shipperGetConfirmedDirectAmendmentScenario(),
                                shipperCancelDirectAmendmentScenario())
                            .asOptionalReportOnlyScenario()))));
    List<String> orderedTestedRoles =
        Stream.of(EblRole.CARRIER, EblRole.SHIPPER)
            .map(EblRole::getConfigName)
            .filter(testedPartyRoleNames::contains)
            .toList();
    return MapUtils.mergePartyScenarioModules(partyScenarios, orderedTestedRoles);
  }

  private static EblScenarioListBuilder carrierRequiredTdScenario(ScenarioType scenarioType) {
    return carrierSupplyScenarioParameters(scenarioType, true)
        .then(
            uc6CarrierPublishDraftTransportDocument(true, scenarioType, false)
                .then(
                    uc7ShipperApproveDraftTransportDocument()
                        .then(
                            uc8CarrierIssueTransportDocument()
                                .then(shipperGetTransportDocument(TD_ISSUED)))));
  }

  private static EblScenarioListBuilder shipperRequiredTdScenario(ScenarioType scenarioType) {
    return uc6CarrierPublishDraftTransportDocument(true, scenarioType, true)
        .then(
            uc7ShipperApproveDraftTransportDocument()
                .then(shipperGetTransportDocument(TD_APPROVED)));
  }

  private static EblScenarioListBuilder carrierGetDirectAmendmentScenario() {
    return carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL, true, true)
        .then(
            uc17ShipperSubmitTransportDocumentAmendment()
                .then(
                    shipperGetTransportDocumentAmendment(
                        AmendedTransportDocumentStatus.AMENDMENT_RECEIVED)));
  }

  private static EblScenarioListBuilder carrierProcessDirectAmendmentScenario(boolean confirm) {
    return carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL, true, true)
        .then(
            uc17ShipperSubmitTransportDocumentAmendment()
                .then(
                    uc19CarrierProcessTransportDocumentAmendment(confirm)
                        .then(
                            shipperGetTransportDocumentAmendment(
                                confirm
                                    ? AmendedTransportDocumentStatus.AMENDMENT_CONFIRMED
                                    : AmendedTransportDocumentStatus.AMENDMENT_DECLINED))));
  }

  private static EblScenarioListBuilder carrierCancelDirectAmendmentScenario() {
    return carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL, true, true)
        .then(
            uc17ShipperSubmitTransportDocumentAmendment()
                .then(uc18ShipperCancelTransportDocumentAmendment()));
  }

  private static EblScenarioListBuilder shipperGetConfirmedDirectAmendmentScenario() {
    return uc17ShipperSubmitTransportDocumentAmendment()
        .then(
            uc19CarrierProcessTransportDocumentAmendment(true)
                .then(
                    shipperGetTransportDocumentAmendment(
                        AmendedTransportDocumentStatus.AMENDMENT_CONFIRMED)));
  }

  private static EblScenarioListBuilder shipperCancelDirectAmendmentScenario() {
    return uc17ShipperSubmitTransportDocumentAmendment()
        .then(uc18ShipperCancelTransportDocumentAmendment());
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
                                                                uc11Get()))))))),
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
                                                                uc11Get())))),
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
                                                                uc11Get()))))))))))),
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

  private static EblScenarioListBuilder buildScenarioForType(ScenarioType type) {
    if (type.isSWB()) {
      return uc6Get(true, type, uc7Get(uc8Get()));
    }
    return uc6Get(true, type, uc7Get(uc8Get(uc12Get(uc13Get()))));
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

  // ── Mandatory GET helpers (used in required scenarios) ──────────────────────

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
    // Calling both amended SI GET and original SI GET after a UC3
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

  // ── Skippable GET helpers (used in optional scenarios) ───────────────────────

  private static EblScenarioListBuilder uc1GetSkippable(
      ShippingInstructionsStatus siState,
      boolean useBothRef,
      EblScenarioListBuilder... thenEither) {
    return uc1ShipperSubmitShippingInstructions()
        .then(shipperGetShippingInstructionsSkippable(siState, useBothRef).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc2GetSkippable(
      ShippingInstructionsStatus siState, EblScenarioListBuilder... thenEither) {
    return uc2CarrierRequestUpdateToShippingInstruction()
        .then(shipperGetShippingInstructionsSkippable(siState, false).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc3GetSkippable(
      ShippingInstructionsStatus originalSiState,
      ShippingInstructionsStatus modifiedSiState,
      boolean useBothRef,
      EblScenarioListBuilder... thenEither) {
    // Calling both amended SI GET and original SI GET after a UC3 — both skippable
    return uc3ShipperSubmitUpdatedShippingInstructions(originalSiState, useBothRef)
        .then(
            shipperGetShippingInstructionsSkippable(originalSiState, modifiedSiState, true, useBothRef)
                .then(
                    shipperGetShippingInstructionsSkippable(
                            originalSiState, modifiedSiState, false, useBothRef)
                        .thenEither(thenEither)));
  }

  private static EblScenarioListBuilder uc4aGetSkippable(
      ShippingInstructionsStatus originalSiState,
      ShippingInstructionsStatus modifiedSiState,
      boolean useBothRef,
      EblScenarioListBuilder... thenEither) {
    return uc4aCarrierAcceptUpdatedShippingInstructions()
        .then(
            shipperGetShippingInstructionsSkippable(originalSiState, modifiedSiState, useBothRef)
                .thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc4aUc14(boolean useBothRef) {
    return uc4aGet(SI_RECEIVED, SI_UPDATE_CONFIRMED, useBothRef, uc14Get(SI_COMPLETED, useBothRef));
  }

  /** Skippable variant of uc4aUc14 — used in optional scenarios */
  private static EblScenarioListBuilder uc4aUc14Skippable(boolean useBothRef) {
    return uc4aGetSkippable(SI_RECEIVED, SI_UPDATE_CONFIRMED, useBothRef, uc14Get(SI_COMPLETED, useBothRef));
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
      boolean skipSI, EblScenarioListBuilder... thenEither) {
    return uc6CarrierPublishDraftTransportDocument(skipSI)
        .then(shipperGetTransportDocument(TD_DRAFT).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc6Get(
      boolean skipSI, ScenarioType scenarioType, EblScenarioListBuilder... thenEither) {
    return uc6CarrierPublishDraftTransportDocument(skipSI, scenarioType)
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
        .then(shipperGetTransportDocument(TD_ISSUED, TD_VOIDED).thenEither(thenEither));
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
    return carrierSupplyScenarioParameters(scenarioType, isTd, false);
  }

  private static EblScenarioListBuilder carrierSupplyScenarioParameters(
      ScenarioType scenarioType, boolean isTd, boolean includeAmendment) {
    return carrierSupplyScenarioParameters(scenarioType, isTd, includeAmendment, false);
  }

  private static EblScenarioListBuilder carrierSupplyScenarioParameters(
      ScenarioType scenarioType, boolean isTd, boolean includeAmendment, boolean allowAnySiType) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String standardVersion = STANDARD_VERSION.get();
    JsonSchemaValidator requestSchemaValidator =
        resolveMessageSchemaValidator(EBL_API, isTd ? GET_TD_SCHEMA_NAME : POST_EBL_SCHEMA_NAME);
    return new EblScenarioListBuilder(
        previousAction ->
            new CarrierSupplyPayloadAction(
                carrierPartyName,
                scenarioType,
                standardVersion,
                requestSchemaValidator,
                isTd,
                includeAmendment,
                allowAnySiType));
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

   private static EblScenarioListBuilder shipperGetShippingInstructionsSkippable(
       ShippingInstructionsStatus expectedSiStatus, boolean useBothRef) {
     return shipperGetShippingInstructionsSkippable(expectedSiStatus, null, useBothRef);
   }

   private static EblScenarioListBuilder shipperGetShippingInstructionsSkippable(
       ShippingInstructionsStatus expectedSiStatus,
       ShippingInstructionsStatus expectedUpdatedSiStatus,
       boolean requestAmendedSI,
       boolean useBothRef) {
     return shipperGetShippingInstructionsSkippable(
         expectedSiStatus, expectedUpdatedSiStatus, requestAmendedSI, false, useBothRef);
   }

   private static EblScenarioListBuilder shipperGetShippingInstructionsSkippable(
       ShippingInstructionsStatus expectedSiStatus,
       ShippingInstructionsStatus expectedUpdatedSiStatus,
       boolean useBothRef) {
     return shipperGetShippingInstructionsSkippable(
         expectedSiStatus, expectedUpdatedSiStatus, false, false, useBothRef);
   }

   private static EblScenarioListBuilder shipperGetShippingInstructionsSkippable(
       ShippingInstructionsStatus expectedSiStatus,
       ShippingInstructionsStatus expectedUpdatedSiStatus,
       boolean requestAmendedSI,
       boolean recordTDR,
       boolean useBothRef) {
     String carrierPartyName = threadLocalCarrierPartyName.get();
     String shipperPartyName = threadLocalShipperPartyName.get();
     return new EblScenarioListBuilder(
         previousAction ->
             new ShipperGetShippingInstructionsSkippableAction(
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
      TransportDocumentStatus... expectedTdStatus) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new Shipper_GetTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                Arrays.stream(expectedTdStatus).toList(),
                resolveMessageSchemaValidator(EBL_API, GET_TD_SCHEMA_NAME)));
  }

  private static EblScenarioListBuilder shipperGetTransportDocumentAmendment(
      AmendedTransportDocumentStatus expectedAmendmentStatus) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new Shipper_GetTransportDocumentAmendmentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(EBL_API, GET_TD_AMENDMENT_SCHEMA_NAME),
                expectedAmendmentStatus));
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
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC1_Shipper_SubmitShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(EBL_API, POST_EBL_SCHEMA_NAME),
                resolveMessageSchemaValidator(
                    EBL_API, RESPONSE_POST_SHIPPING_INSTRUCTIONS_SCHEMA_NAME),
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc1ShipperSubmitShippingInstructionsStandalone(
      ScenarioType scenarioType) {
    return uc1ShipperSubmitShippingInstructionsStandalone(scenarioType, "UC1");
  }

  private static EblScenarioListBuilder uc1ShipperSubmitShippingInstructionsStandalone(
      ScenarioType scenarioType, String actionTitle) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    String standardVersion = STANDARD_VERSION.get();
    return new EblScenarioListBuilder(
        previousAction ->
            {
              var action =
                  new UC1_Shipper_SubmitShippingInstructionsAction(
                      carrierPartyName,
                      shipperPartyName,
                      (EblAction) previousAction,
                      resolveMessageSchemaValidator(EBL_API, POST_EBL_SCHEMA_NAME),
                      resolveMessageSchemaValidator(
                          EBL_API, RESPONSE_POST_SHIPPING_INSTRUCTIONS_SCHEMA_NAME),
                      resolveMessageSchemaValidator(
                          EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
                      isWithNotifications,
                      scenarioType,
                              standardVersion,
                              actionTitle);
                      return action;
            });
  }

          private static String uc1TitleFor(ScenarioType scenarioType) {
            return "UC1[%s]".formatted(scenarioType.tdScopeName());
          }


  private static EblScenarioListBuilder uc3ShipperSubmitUpdatedShippingInstructions(
      ShippingInstructionsStatus expectedSiStatus, boolean useBothRef) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
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
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc2CarrierRequestUpdateToShippingInstruction() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC2_Carrier_RequestUpdateToShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc4aCarrierAcceptUpdatedShippingInstructions() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC4_Carrier_ProcessUpdateToShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                SI_RECEIVED,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
                true,
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc4dCarrierDeclineUpdatedShippingInstructions(
      ShippingInstructionsStatus shippingInstructionsStatus) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC4_Carrier_ProcessUpdateToShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                shippingInstructionsStatus,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
                false,
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc5ShipperCancelUpdateToShippingInstructions(
      ShippingInstructionsStatus expectedSIStatus, boolean useBothRef) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
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
                    EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc6CarrierPublishDraftTransportDocument(boolean skipSI) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC6_Carrier_PublishDraftTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                skipSI,
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc6CarrierPublishDraftTransportDocument(
      boolean skipSI, ScenarioType scenarioType) {
    return uc6CarrierPublishDraftTransportDocument(skipSI, scenarioType, true);
  }

  private static EblScenarioListBuilder uc6CarrierPublishDraftTransportDocument(
      boolean skipSI, ScenarioType scenarioType, boolean includeScenarioTypeInTitle) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC6_Carrier_PublishDraftTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                scenarioType,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                skipSI,
                isWithNotifications,
                includeScenarioTypeInTitle));
  }

  private static EblScenarioListBuilder uc7ShipperApproveDraftTransportDocument() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC7_Shipper_ApproveDraftTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(EBL_API, PATCH_TD_SCHEMA_NAME),
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc8CarrierIssueTransportDocument() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC8_Carrier_IssueTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc17ShipperSubmitTransportDocumentAmendment() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    String standardVersion = STANDARD_VERSION.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC17_Shipper_SubmitTransportDocumentAmendmentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(EBL_API, GET_TD_SCHEMA_NAME),
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications,
                standardVersion));
  }

  private static EblScenarioListBuilder uc18ShipperCancelTransportDocumentAmendment() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC18_Shipper_CancelTransportDocumentAmendmentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc19CarrierProcessTransportDocumentAmendment(
      boolean confirm) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC19_Carrier_ProcessTransportDocumentAmendmentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                confirm,
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc9CarrierAwaitSurrenderRequestForAmendment() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC9_Carrier_AwaitSurrenderRequestForAmendmentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc10aCarrierAcceptSurrenderRequestForAmendment() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC10_Carrier_ProcessSurrenderRequestForAmendmentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                true,
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc11CarrierVoidTDandIssueAmendedTransportDocument() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC11_Carrier_voidTDAndIssueAmendedTransportDocumentAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc12CarrierAwaitSurrenderRequestForDelivery() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC12_Carrier_AwaitSurrenderRequestForDeliveryAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                isWithNotifications));
  }

  private static EblScenarioListBuilder uc13aCarrierAcceptSurrenderRequestForDelivery() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    boolean isWithNotifications = threadLocalIsWithNotifications.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC13_Carrier_ProcessSurrenderRequestForDeliveryAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                resolveMessageSchemaValidator(
                    EBL_NOTIFICATIONS_API, EBL_TD_NOTIFICATION_SCHEMA_NAME),
                true,
                isWithNotifications));
  }

   private static EblScenarioListBuilder uc14CarrierConfirmShippingInstructionsComplete() {
     String carrierPartyName = threadLocalCarrierPartyName.get();
     String shipperPartyName = threadLocalShipperPartyName.get();
     boolean isWithNotifications = threadLocalIsWithNotifications.get();
     return new EblScenarioListBuilder(
         previousAction ->
             new UC14_Carrier_ConfirmShippingInstructionsCompleteAction(
                 carrierPartyName,
                 shipperPartyName,
                 (EblAction) previousAction,
                 resolveMessageSchemaValidator(
                     EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
                 isWithNotifications));
   }

   private static EblScenarioListBuilder uc15ShipperCancelShippingInstructions() {
     String carrierPartyName = threadLocalCarrierPartyName.get();
     String shipperPartyName = threadLocalShipperPartyName.get();
     boolean isWithNotifications = threadLocalIsWithNotifications.get();
     return new EblScenarioListBuilder(
         previousAction ->
             new UC15_Shipper_CancelShippingInstructionsAction(
                 carrierPartyName,
                 shipperPartyName,
                 (EblAction) previousAction,
                 resolveMessageSchemaValidator(EBL_API, PATCH_SI_SCHEMA_NAME),
                 resolveMessageSchemaValidator(
                     EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
                 isWithNotifications));
   }

   private static EblScenarioListBuilder uc16CarrierDeclineShippingInstructions() {
     String carrierPartyName = threadLocalCarrierPartyName.get();
     String shipperPartyName = threadLocalShipperPartyName.get();
     boolean isWithNotifications = threadLocalIsWithNotifications.get();
     return new EblScenarioListBuilder(
         previousAction ->
             new UC16_Carrier_DeclineShippingInstructionsAction(
                 carrierPartyName,
                 shipperPartyName,
                 (EblAction) previousAction,
                 resolveMessageSchemaValidator(
                     EBL_NOTIFICATIONS_API, EBL_SI_NOTIFICATION_SCHEMA_NAME),
                 isWithNotifications));
   }

   private static EblScenarioListBuilder oobCarrierProcessOutOfBoundTDUpdateRequest() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UCX_Carrier_TDOnlyProcessOutOfBandUpdateOrAmendmentRequestDraftTransportDocumentAction(
                carrierPartyName, shipperPartyName, (EblAction) previousAction));
  }

  private static JsonSchemaValidator resolveMessageSchemaValidator(String apiName, String schema) {
    var standardVersion = STANDARD_VERSION.get();
    var schemaKey =
        standardVersion + Character.toString(0x1f) + apiName + Character.toString(0x1f) + schema;
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
