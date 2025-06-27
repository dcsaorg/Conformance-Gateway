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
  static final String SCENARIO_SUITE_SI_TD_COMBINED = "SI and TD Combined";

  static final Set<String> SCENARIOS =
      Set.of(
          SCENARIO_SUITE_CONFORMANCE_SI_ONLY,
          SCENARIO_SUITE_CONFORMANCE_TD_ONLY,
          SCENARIO_SUITE_SI_TD_COMBINED);

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

  private static final ConcurrentHashMap<String, JsonSchemaValidator> SCHEMA_CACHE = new ConcurrentHashMap<>();

  public static LinkedHashMap<String, EblScenarioListBuilder> createModuleScenarioListBuilders(
      EblComponentFactory componentFactory, String standardVersion, String carrierPartyName, String shipperPartyName) {
    STANDARD_VERSION.set(standardVersion);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);

    if (SCENARIO_SUITE_CONFORMANCE_SI_ONLY.equals(componentFactory.getScenarioSuite())) {
      return createConformanceSiOnlyScenarios();
    }
    if (SCENARIO_SUITE_CONFORMANCE_TD_ONLY.equals(componentFactory.getScenarioSuite())) {
      return createConformanceTdOnlyScenarios();
    }
    if (SCENARIO_SUITE_SI_TD_COMBINED.equals(componentFactory.getScenarioSuite())) {
      return createSIandTDCombinedScenarios();
    }
    throw new IllegalArgumentException("Invalid scenario suite name '%s'".formatted(componentFactory.getScenarioSuite()));
  }

  private static LinkedHashMap<String, EblScenarioListBuilder> createConformanceSiOnlyScenarios() {
    return Stream.of(
            Map.entry(
                "Supported shipment types scenarios",
                noAction()
                    .thenEither(
                        Arrays.stream(ScenarioType.values())
                            .map(
                                scenarioType ->
                                    carrierSupplyScenarioParameters(scenarioType)
                                        .then(uc1Get(SI_RECEIVED, uc14Get(SI_COMPLETED))))
                            .toList()
                            .toArray(new EblScenarioListBuilder[] {}))),
            Map.entry(
                "Carrier requested update scenarios",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL)
                    .then(
                        uc1Get(
                            SI_RECEIVED,
                            uc2Get(
                                SI_PENDING_UPDATE, uc3AndAllSiOnlyPathsFrom(SI_PENDING_UPDATE))))),
            Map.entry(
                "Shipper initiated update scenarios",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL)
                    .then(uc1Get(SI_RECEIVED, uc3AndAllSiOnlyPathsFrom(SI_RECEIVED)))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static LinkedHashMap<String, EblScenarioListBuilder> createConformanceTdOnlyScenarios() {
    return Stream.of(
            Map.entry(
                "Supported shipment types scenarios",
                noAction()
                    .thenEither(
                        Arrays.stream(ScenarioType.values())
                            .map(EblScenarioListBuilder::buildScenarioForType)
                            .toArray(EblScenarioListBuilder[]::new))),
            Map.entry(
                "Shipper interactions with transport document",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL)
                    .then(
                        uc6Get(
                            true,
                            oobAmendment(uc6Get(false, uc8Get(uc12Get(uc13Get())))),
                            uc8Get(oobAmendment(uc9Get(uc10Get(uc11Get(uc12Get(uc13Get()))))))))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static EblScenarioListBuilder buildScenarioForType(ScenarioType type) {
    if (type.isSWB()) {
      return carrierSupplyScenarioParameters(type).then(uc6Get(true, uc7Get(uc8Get())));
    }
    return carrierSupplyScenarioParameters(type)
        .then(uc6Get(true, uc7Get(uc8Get(uc12Get(uc13Get())))));
  }

  private static EblScenarioListBuilder uc3AndAllSiOnlyPathsFrom(
      ShippingInstructionsStatus originalSiState) {
    return uc3Get(
        originalSiState,
        SI_UPDATE_RECEIVED,
        uc2Get(SI_PENDING_UPDATE, uc3Get(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, uc4aUc14())),
        uc3Get(originalSiState, SI_UPDATE_RECEIVED, uc4aUc14()),
        uc4aGet(
            SI_RECEIVED,
            SI_UPDATE_CONFIRMED,
            uc2Get(SI_PENDING_UPDATE, uc3Get(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, uc4aUc14())),
            uc3Get(SI_RECEIVED, SI_UPDATE_RECEIVED, uc4aUc14()),
            uc14Get(SI_COMPLETED)),
        uc4dGet(
            originalSiState,
            SI_UPDATE_DECLINED,
            uc3Get(originalSiState, SI_UPDATE_RECEIVED, uc4aUc14())),
        uc5Get(
            originalSiState,
            SI_UPDATE_CANCELLED,
            uc3Get(originalSiState, SI_UPDATE_RECEIVED, uc4aUc14())));
  }

  private static EblScenarioListBuilder uc1Get(
      ShippingInstructionsStatus siState, EblScenarioListBuilder... thenEither) {
    return uc1ShipperSubmitShippingInstructions()
        .then(shipperGetShippingInstructions(siState, false).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc2Get(
      ShippingInstructionsStatus siState, EblScenarioListBuilder... thenEither) {
    return uc2CarrierRequestUpdateToShippingInstruction()
        .then(shipperGetShippingInstructions(siState, false).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc3Get(
      ShippingInstructionsStatus originalSiState,
      ShippingInstructionsStatus modifiedSiState,
      EblScenarioListBuilder... thenEither) {
    // Calling both amemded SI GET and original SI GET after a UC3
    return uc3ShipperSubmitUpdatedShippingInstructions(originalSiState, false)
        .then(
            shipperGetShippingInstructions(originalSiState, modifiedSiState, true, false)
                .then(
                    shipperGetShippingInstructions(originalSiState, modifiedSiState, false, false)
                        .thenEither(thenEither)));
  }

  private static EblScenarioListBuilder uc4aGet(
      ShippingInstructionsStatus originalSiState,
      ShippingInstructionsStatus modifiedSiState,
      EblScenarioListBuilder... thenEither) {
    return uc4aCarrierAcceptUpdatedShippingInstructions()
        .then(
            shipperGetShippingInstructions(originalSiState, modifiedSiState, false)
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

  private static EblScenarioListBuilder uc4aUc14() {
    return uc4aGet(SI_RECEIVED, SI_UPDATE_CONFIRMED, uc14Get(SI_COMPLETED));
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
    return uc11CarrierVoidTransportDocument()
        .then(
            uc11iCarrierIssueAmendedTransportDocument()
                .then(shipperGetTransportDocument(TD_ISSUED).thenEither(thenEither)));
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

  private static EblScenarioListBuilder uc14Get(ShippingInstructionsStatus siState) {
    return uc14CarrierConfirmShippingInstructionsComplete()
        .then(shipperGetShippingInstructions(siState, false));
  }

  private static EblScenarioListBuilder oobAmendment(EblScenarioListBuilder... thenEither) {
    return oobCarrierProcessOutOfBoundTDUpdateRequest().thenEither(thenEither);
  }

  private static LinkedHashMap<String, EblScenarioListBuilder> createSIandTDCombinedScenarios() {
    return Stream.of(
            Map.entry(
                "Straight eBL",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL)
                    .then(
                        uc1ShipperSubmitShippingInstructions()
                            .then(
                                shipperGetShippingInstructions(SI_RECEIVED, false)
                                    .then(
                                        uc6CarrierPublishDraftTransportDocument(false)
                                            .then(
                                                shipperGetShippingInstructionsRecordTDRef()
                                                    .then(
                                                        shipperGetTransportDocument(TD_DRAFT)
                                                            .thenEither(
                                                                uc7ShipperApproveDraftTransportDocument()
                                                                    .then(
                                                                        shipperGetTransportDocument(
                                                                                TD_APPROVED)
                                                                            .then(
                                                                                uc8CarrierIssueTransportDocument()
                                                                                    .then(
                                                                                        shipperGetTransportDocument(
                                                                                                TD_ISSUED)
                                                                                            .thenEither(
                                                                                                uc12CarrierAwaitSurrenderRequestForDelivery()
                                                                                                    .then(
                                                                                                        shipperGetTransportDocument(
                                                                                                                TD_PENDING_SURRENDER_FOR_DELIVERY)
                                                                                                            .then(
                                                                                                                uc13aCarrierAcceptSurrenderRequestForDelivery()
                                                                                                                    .then(
                                                                                                                        shipperGetTransportDocument(
                                                                                                                                TD_SURRENDERED_FOR_DELIVERY)
                                                                                                                            .then(
                                                                                                                                uc14CarrierConfirmShippingInstructionsComplete()
                                                                                                                                    .then(
                                                                                                                                        shipperGetShippingInstructions(
                                                                                                                                            SI_COMPLETED,
                                                                                                                                            true)))))),
                                                                                                uc9CarrierAwaitSurrenderRequestForAmendment()
                                                                                                    .then(
                                                                                                        shipperGetTransportDocument(
                                                                                                                TD_PENDING_SURRENDER_FOR_AMENDMENT)
                                                                                                            .then(
                                                                                                                uc10aCarrierAcceptSurrenderRequestForAmendment()
                                                                                                                    .then(
                                                                                                                        shipperGetTransportDocument(
                                                                                                                                TD_SURRENDERED_FOR_AMENDMENT)
                                                                                                                            .then(
                                                                                                                                uc3ShipperSubmitUpdatedShippingInstructions(
                                                                                                                                        SI_RECEIVED,
                                                                                                                                        true)
                                                                                                                                    .then(
                                                                                                                                        shipperGetShippingInstructions(
                                                                                                                                                SI_RECEIVED,
                                                                                                                                                SI_UPDATE_RECEIVED,
                                                                                                                                                true)
                                                                                                                                            .then(
                                                                                                                                                uc4aCarrierAcceptUpdatedShippingInstructions()
                                                                                                                                                    .then(
                                                                                                                                                        shipperGetShippingInstructions(
                                                                                                                                                                SI_RECEIVED,
                                                                                                                                                                SI_UPDATE_CONFIRMED,
                                                                                                                                                                true)
                                                                                                                                                            .then(
                                                                                                                                                                uc11CarrierVoidTransportDocument()
                                                                                                                                                                    .then(
                                                                                                                                                                        shipperGetTransportDocument(
                                                                                                                                                                                TD_VOIDED)
                                                                                                                                                                            .then(
                                                                                                                                                                                uc11iCarrierIssueAmendedTransportDocument()
                                                                                                                                                                                    .then(
                                                                                                                                                                                        shipperGetTransportDocument(
                                                                                                                                                                                                TD_ISSUED)
                                                                                                                                                                                            .then(
                                                                                                                                                                                                uc12CarrierAwaitSurrenderRequestForDelivery()
                                                                                                                                                                                                    .then(
                                                                                                                                                                                                        shipperGetTransportDocument(
                                                                                                                                                                                                                TD_PENDING_SURRENDER_FOR_DELIVERY)
                                                                                                                                                                                                            .then(
                                                                                                                                                                                                                shipperGetTransportDocument(
                                                                                                                                                                                                                        TD_PENDING_SURRENDER_FOR_DELIVERY)
                                                                                                                                                                                                                    .then(
                                                                                                                                                                                                                        uc13aCarrierAcceptSurrenderRequestForDelivery()
                                                                                                                                                                                                                            .then(
                                                                                                                                                                                                                                shipperGetTransportDocument(
                                                                                                                                                                                                                                        TD_SURRENDERED_FOR_DELIVERY)
                                                                                                                                                                                                                                    .then(
                                                                                                                                                                                                                                        uc14CarrierConfirmShippingInstructionsComplete()
                                                                                                                                                                                                                                            .then(
                                                                                                                                                                                                                                                shipperGetShippingInstructions(
                                                                                                                                                                                                                                                    SI_COMPLETED,
                                                                                                                                                                                                                                                    true))))))))))))))))))))))),
                                                                uc3ShipperSubmitUpdatedShippingInstructions(
                                                                        SI_RECEIVED, true)
                                                                    .then(
                                                                        shipperGetShippingInstructions(
                                                                                SI_RECEIVED,
                                                                                SI_UPDATE_RECEIVED,
                                                                                true)
                                                                            .then(
                                                                                uc4aCarrierAcceptUpdatedShippingInstructions()
                                                                                    .then(
                                                                                        shipperGetShippingInstructions(
                                                                                                SI_RECEIVED,
                                                                                                SI_UPDATE_CONFIRMED,
                                                                                                true)
                                                                                            .then(
                                                                                                shipperGetTransportDocument(
                                                                                                        TD_DRAFT)
                                                                                                    .then(
                                                                                                        uc6CarrierPublishDraftTransportDocument(
                                                                                                                false)
                                                                                                            .then(
                                                                                                                shipperGetShippingInstructionsRecordTDRef()
                                                                                                                    .then(
                                                                                                                        shipperGetTransportDocument(
                                                                                                                                TD_DRAFT)
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
                                                                                                                                                                uc12CarrierAwaitSurrenderRequestForDelivery()
                                                                                                                                                                    .then(
                                                                                                                                                                        shipperGetTransportDocument(
                                                                                                                                                                                TD_PENDING_SURRENDER_FOR_DELIVERY)
                                                                                                                                                                            .then(
                                                                                                                                                                                uc13aCarrierAcceptSurrenderRequestForDelivery()
                                                                                                                                                                                    .then(
                                                                                                                                                                                        shipperGetTransportDocument(
                                                                                                                                                                                                TD_SURRENDERED_FOR_DELIVERY)
                                                                                                                                                                                            .then(
                                                                                                                                                                                                uc14CarrierConfirmShippingInstructionsComplete()
                                                                                                                                                                                                    .then(
                                                                                                                                                                                                        shipperGetShippingInstructions(
                                                                                                                                                                                                            SI_COMPLETED,
                                                                                                                                                                                                            false))))))))))))))))))))))))),
            Map.entry(
                "Sea Waybill",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_SWB)
                    .then(
                        uc1ShipperSubmitShippingInstructions()
                            .then(
                                shipperGetShippingInstructions(SI_RECEIVED, false)
                                    .then(
                                        uc6CarrierPublishDraftTransportDocument(false)
                                            .then(
                                                shipperGetShippingInstructionsRecordTDRef()
                                                    .then(
                                                        shipperGetTransportDocument(TD_DRAFT)
                                                            .thenEither(
                                                                uc7ShipperApproveDraftTransportDocument()
                                                                    .then(
                                                                        shipperGetTransportDocument(
                                                                                TD_APPROVED)
                                                                            .thenEither(
                                                                                uc8CarrierIssueTransportDocument()
                                                                                    .then(
                                                                                        shipperGetTransportDocument(
                                                                                            TD_ISSUED)),
                                                                                uc3ShipperSubmitUpdatedShippingInstructions(
                                                                                        SI_RECEIVED,
                                                                                        true)
                                                                                    .then(
                                                                                        shipperGetShippingInstructions(
                                                                                                SI_RECEIVED,
                                                                                                SI_UPDATE_RECEIVED,
                                                                                                true)
                                                                                            .then(
                                                                                                shipperGetTransportDocument(
                                                                                                        TD_APPROVED)
                                                                                                    .then(
                                                                                                        uc4aCarrierAcceptUpdatedShippingInstructions()
                                                                                                            .then(
                                                                                                                shipperGetShippingInstructions(
                                                                                                                        SI_RECEIVED,
                                                                                                                        SI_UPDATE_CONFIRMED,
                                                                                                                        true)
                                                                                                                    .then(
                                                                                                                        uc8CarrierIssueTransportDocument()
                                                                                                                            .then(
                                                                                                                                shipperGetTransportDocument(
                                                                                                                                    TD_ISSUED))))))))),
                                                                uc3ShipperSubmitUpdatedShippingInstructions(
                                                                        SI_RECEIVED, true)
                                                                    .then(
                                                                        shipperGetShippingInstructions(
                                                                                SI_RECEIVED,
                                                                                SI_UPDATE_RECEIVED,
                                                                                true)
                                                                            .then(
                                                                                uc4aCarrierAcceptUpdatedShippingInstructions()
                                                                                    .then(
                                                                                        shipperGetShippingInstructions(
                                                                                                SI_RECEIVED,
                                                                                                SI_UPDATE_CONFIRMED,
                                                                                                true)
                                                                                            .then(
                                                                                                uc6CarrierPublishDraftTransportDocument(
                                                                                                        false)
                                                                                                    .then(
                                                                                                        shipperGetTransportDocument(
                                                                                                                TD_DRAFT)
                                                                                                            .then(
                                                                                                                uc7ShipperApproveDraftTransportDocument()
                                                                                                                    .then(
                                                                                                                        shipperGetTransportDocument(
                                                                                                                                TD_APPROVED)
                                                                                                                            .then(
                                                                                                                                uc8CarrierIssueTransportDocument()
                                                                                                                                    .then(
                                                                                                                                        shipperGetTransportDocument(
                                                                                                                                            TD_ISSUED))))))))))))))))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }


  private EblScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static EblScenarioListBuilder noAction() {
    return new EblScenarioListBuilder(null);
  }

  private static EblScenarioListBuilder carrierSupplyScenarioParameters(ScenarioType scenarioType) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    return new EblScenarioListBuilder(
        previousAction -> new Carrier_SupplyScenarioParametersAction(carrierPartyName, scenarioType));
  }

  private static EblScenarioListBuilder shipperGetShippingInstructions(
      ShippingInstructionsStatus expectedSiStatus, boolean useTDRef) {
    return shipperGetShippingInstructions(expectedSiStatus, null, useTDRef);
  }

  private static EblScenarioListBuilder shipperGetShippingInstructions(
      ShippingInstructionsStatus expectedSiStatus,
      ShippingInstructionsStatus expectedUpdatedSiStatus,
      boolean requestAmendedSI,
      boolean useTDRef) {
    return shipperGetShippingInstructions(
        expectedSiStatus, expectedUpdatedSiStatus, requestAmendedSI, false, useTDRef);
  }

  private static EblScenarioListBuilder shipperGetShippingInstructionsRecordTDRef() {
    return shipperGetShippingInstructions(SI_RECEIVED, SI_ANY, false, true, false);
  }

  private static EblScenarioListBuilder shipperGetShippingInstructions(
      ShippingInstructionsStatus expectedSiStatus,
      ShippingInstructionsStatus expectedUpdatedSiStatus,
      boolean useTDRef) {
    return shipperGetShippingInstructions(
        expectedSiStatus, expectedUpdatedSiStatus, false, false, useTDRef);
  }

  private static EblScenarioListBuilder shipperGetShippingInstructions(
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
    ShippingInstructionsStatus expectedSiStatus,
    boolean useTDRef
  ) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
        previousAction ->
            new UC3ShipperSubmitUpdatedShippingInstructionsAction(
                carrierPartyName,
                shipperPartyName,
                (EblAction) previousAction,
                expectedSiStatus,
                useTDRef,
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
      ShippingInstructionsStatus expectedSIStatus, boolean useTDRef) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction ->
        new UC5_Shipper_CancelUpdateToShippingInstructionsAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          expectedSIStatus,
          useTDRef,
          resolveMessageSchemaValidator(
            EBL_API, PATCH_SI_SCHEMA_NAME),
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

  private static EblScenarioListBuilder uc11CarrierVoidTransportDocument() {
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

  private static EblScenarioListBuilder uc11iCarrierIssueAmendedTransportDocument() {
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
