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
import org.dcsa.conformance.standards.ebl.models.OutOfOrderMessageType;
import org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus;
import org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus;

@Slf4j
public class EblScenarioListBuilder extends ScenarioListBuilder<EblScenarioListBuilder> {

  public static final String SCENARIO_SUITE_CONFORMANCE_SI_ONLY = "Conformance SI-only";
  public static final String SCENARIO_SUITE_CONFORMANCE_TD_ONLY = "Conformance TD-only";
  static final String SCENARIO_SUITE_RI = "Reference Implementation";

  static final Set<String> SCENARIOS = Set.of(
    SCENARIO_SUITE_CONFORMANCE_SI_ONLY,
    SCENARIO_SUITE_CONFORMANCE_TD_ONLY,
    SCENARIO_SUITE_RI
  );

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
    if (SCENARIO_SUITE_RI.equals(componentFactory.getScenarioSuite())) {
      return createReferenceImplementationScenarios();
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
                            .toList()
                            .toArray(new EblScenarioListBuilder[] {}))),
            Map.entry(
                "Shipper interactions with transport document",
                carrierSupplyScenarioParameters(ScenarioType.REGULAR_STRAIGHT_BL)
                    .then(
                        uc6Get(
                            true,
                            uc7Get(uc8Get(uc12Get(uc13Get()))),
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
    return carrierSupplyScenarioParameters(type).then(uc6Get(true, uc8Get(uc12Get(uc13Get()))));
  }

  @SuppressWarnings("unused")
  private static void ignoreSingleValueArgs() {
    uc1Get(SI_ANY);
    uc2Get(SI_ANY);
    uc3Get(SI_ANY, SI_ANY);
    uc4AGet(SI_ANY, SI_ANY);
    uc4DGet(SI_ANY, SI_ANY);
    uc5Get(SI_ANY, SI_ANY);
    uc14Get(SI_ANY);
  }

  private static EblScenarioListBuilder uc3AndAllSiOnlyPathsFrom(
      ShippingInstructionsStatus originalSiState) {
    return uc3Get(
        originalSiState,
        SI_UPDATE_RECEIVED,
        uc2Get(SI_PENDING_UPDATE, uc3Get(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, uc4AUc14())),
        uc3Get(originalSiState, SI_UPDATE_RECEIVED, uc4AUc14()),
        uc4AGet(
            SI_RECEIVED,
            SI_UPDATE_CONFIRMED,
            uc2Get(SI_PENDING_UPDATE, uc3Get(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, uc4AUc14())),
            uc3Get(SI_RECEIVED, SI_UPDATE_RECEIVED, uc4AUc14()),
            uc14Get(SI_COMPLETED)),
        uc4DGet(
            originalSiState,
            SI_UPDATE_DECLINED,
            uc3Get(originalSiState, SI_UPDATE_RECEIVED, uc4AUc14())),
        uc5Get(
            originalSiState,
            SI_UPDATE_CANCELLED,
            uc3Get(originalSiState, SI_UPDATE_RECEIVED, uc4AUc14())));
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

  private static EblScenarioListBuilder uc4AGet(
      ShippingInstructionsStatus originalSiState,
      ShippingInstructionsStatus modifiedSiState,
      EblScenarioListBuilder... thenEither) {
    return uc4ACarrierAcceptUpdatedShippingInstructions()
        .then(
            shipperGetShippingInstructions(originalSiState, modifiedSiState, false)
                .thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc4DGet(
      ShippingInstructionsStatus originalSiState,
      ShippingInstructionsStatus modifiedSiState,
      EblScenarioListBuilder... thenEither) {
    return uc4DCarrierDeclineUpdatedShippingInstructions(originalSiState)
        .then(
            shipperGetShippingInstructions(originalSiState, modifiedSiState, false)
                .thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc4AUc14() {
    return uc4AGet(SI_RECEIVED, SI_UPDATE_CONFIRMED, uc14Get(SI_COMPLETED));
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
    return uc10ACarrierAcceptSurrenderRequestForAmendment()
        .then(shipperGetTransportDocument(TD_SURRENDERED_FOR_AMENDMENT).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc11Get(EblScenarioListBuilder... thenEither) {
    return uc11CarrierVoidTransportDocument()
        .then(
            uc11ICarrierIssueAmendedTransportDocument()
                .then(shipperGetTransportDocument(TD_ISSUED).thenEither(thenEither)));
  }

  private static EblScenarioListBuilder uc12Get(EblScenarioListBuilder... thenEither) {
    return uc12CarrierAwaitSurrenderRequestForDelivery()
        .then(
            shipperGetTransportDocument(TD_PENDING_SURRENDER_FOR_DELIVERY).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc13Get(EblScenarioListBuilder... thenEither) {
    return uc13ACarrierAcceptSurrenderRequestForDelivery()
        .then(shipperGetTransportDocument(TD_SURRENDERED_FOR_DELIVERY).thenEither(thenEither));
  }

  private static EblScenarioListBuilder uc14Get(ShippingInstructionsStatus siState) {
    return uc14CarrierConfirmShippingInstructionsComplete()
        .then(shipperGetShippingInstructions(siState, false));
  }

  private static EblScenarioListBuilder oobAmendment(EblScenarioListBuilder... thenEither) {
    return oobCarrierProcessOutOfBoundTDUpdateRequest().thenEither(thenEither);
  }

  private static LinkedHashMap<String, EblScenarioListBuilder> createReferenceImplementationScenarios() {
    return Stream.of(
            Map.entry(
                "",
                noAction()
                    .thenEither(
                        Arrays.stream(ScenarioType.values())
                            .map(
                                st -> {
                                  if (st == ScenarioType.REGULAR_SWB) {
                                    return carrierSupplyScenarioParameters(st)
                                        .thenAllPathsFrom(SI_START, TD_START, false);
                                  }
                                  return carrierSupplyScenarioParameters(st)
                                      .thenHappyPathFrom(SI_START, TD_START, false);
                                })
                            .toList()
                            .toArray(new EblScenarioListBuilder[] {}))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
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
          throw new IllegalStateException(
              "Cannot use transport document reference when submitting a TD");
        }
        if (transportDocumentStatus != TD_START) {
          throw new IllegalStateException(
              "Cannot use transport document reference when submitting a TD");
        }
        yield then(
            uc1ShipperSubmitShippingInstructions()
                .then(
                    shipperGetShippingInstructions(SI_RECEIVED, false)
                        .thenAllPathsFrom(SI_RECEIVED, transportDocumentStatus, false)));
      }
      case SI_RECEIVED ->
          thenEither(
              uc2CarrierRequestUpdateToShippingInstruction()
                  .then(
                      shipperGetShippingInstructions(SI_PENDING_UPDATE, useTDRef)
                          .thenAllPathsFrom(SI_PENDING_UPDATE, transportDocumentStatus, useTDRef)),
              uc3ShipperSubmitUpdatedShippingInstructions(SI_RECEIVED, useTDRef)
                  .then(
                      shipperGetShippingInstructions(SI_RECEIVED, SI_UPDATE_RECEIVED, useTDRef)
                          .then(
                              shipperGetShippingInstructions(
                                      SI_RECEIVED, SI_UPDATE_RECEIVED, true, useTDRef)
                                  .thenAllPathsFrom(
                                      SI_UPDATE_RECEIVED,
                                      SI_RECEIVED,
                                      transportDocumentStatus,
                                      useTDRef))),
              switch (transportDocumentStatus) {
                case TD_START ->
                    uc6CarrierPublishDraftTransportDocument(false)
                        .then(
                            shipperGetShippingInstructionsRecordTDRef()
                                .then(
                                    shipperGetTransportDocument(TD_DRAFT)
                                        .thenAllPathsFrom(TD_DRAFT)));
                case TD_DRAFT ->
                    uc6CarrierPublishDraftTransportDocument(false)
                        .then(
                            shipperGetShippingInstructionsRecordTDRef()
                                .then(
                                    shipperGetTransportDocument(TD_DRAFT)
                                        .thenHappyPathFrom(TD_DRAFT)));
                case TD_ISSUED ->
                    uc9CarrierAwaitSurrenderRequestForAmendment()
                        .then(shipperGetTransportDocument(TD_PENDING_SURRENDER_FOR_AMENDMENT))
                        .thenHappyPathFrom(TD_PENDING_SURRENDER_FOR_AMENDMENT);
                default ->
                    throw new IllegalStateException(
                        "Unexpected transportDocumentStatus: " + transportDocumentStatus.name());
              },
              aucShipperSendOutOfOrderSIMessage(OutOfOrderMessageType.CANCEL_SI_UPDATE, useTDRef)
                  .then(
                      shipperGetShippingInstructions(shippingInstructionsStatus, useTDRef)
                          .thenHappyPathFrom(
                              shippingInstructionsStatus,
                              memoryState,
                              transportDocumentStatus,
                              useTDRef)));
      case SI_UPDATE_RECEIVED -> {
        if (memoryState == null) {
          throw new IllegalArgumentException(
              shippingInstructionsStatus.name() + " requires a memory state");
        }
        yield thenEither(
            uc2CarrierRequestUpdateToShippingInstruction()
                .then(
                    shipperGetShippingInstructions(SI_PENDING_UPDATE, useTDRef)
                        .thenHappyPathFrom(SI_PENDING_UPDATE, transportDocumentStatus, useTDRef)),
            uc4ACarrierAcceptUpdatedShippingInstructions()
                .then(
                    shipperGetShippingInstructions(SI_RECEIVED, SI_UPDATE_CONFIRMED, useTDRef)
                        .thenEither(
                            noAction()
                                .thenHappyPathFrom(SI_RECEIVED, transportDocumentStatus, useTDRef),
                            aucShipperSendOutOfOrderSIMessage(
                                    OutOfOrderMessageType.CANCEL_SI_UPDATE, useTDRef)
                                .then(
                                    shipperGetShippingInstructions(
                                            SI_RECEIVED, SI_UPDATE_CONFIRMED, useTDRef)
                                        .thenHappyPathFrom(
                                            SI_RECEIVED, transportDocumentStatus, useTDRef)))),
            uc4DCarrierDeclineUpdatedShippingInstructions(memoryState)
                .then(
                    shipperGetShippingInstructions(memoryState, SI_UPDATE_DECLINED, useTDRef)
                        .thenEither(
                            noAction()
                                .thenHappyPathFrom(memoryState, transportDocumentStatus, useTDRef),
                            aucShipperSendOutOfOrderSIMessage(
                                    OutOfOrderMessageType.CANCEL_SI_UPDATE, useTDRef)
                                .then(
                                    shipperGetShippingInstructions(
                                            memoryState, SI_UPDATE_DECLINED, useTDRef)
                                        .thenHappyPathFrom(
                                            memoryState, transportDocumentStatus, useTDRef)))),
            uc5ShipperCancelUpdateToShippingInstructions(memoryState, useTDRef)
                .then(
                    shipperGetShippingInstructions(memoryState, SI_UPDATE_CANCELLED, useTDRef)
                        .thenEither(
                            noAction()
                                .thenHappyPathFrom(memoryState, transportDocumentStatus, useTDRef),
                            aucShipperSendOutOfOrderSIMessage(
                                    OutOfOrderMessageType.CANCEL_SI_UPDATE, useTDRef)
                                .then(
                                    shipperGetShippingInstructions(
                                            memoryState, SI_UPDATE_CANCELLED, useTDRef)
                                        .thenHappyPathFrom(
                                            memoryState, transportDocumentStatus, useTDRef)))));
      }
      case SI_PENDING_UPDATE -> {
        if (transportDocumentStatus != TD_START) {
          yield thenEither(
              uc3ShipperSubmitUpdatedShippingInstructions(SI_PENDING_UPDATE, useTDRef)
                  .then(
                      shipperGetShippingInstructions(
                              SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, useTDRef)
                          .then(
                              shipperGetShippingInstructions(
                                      SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, true, useTDRef)
                                  .thenEither(
                                      noAction()
                                          .thenHappyPathFrom(
                                              SI_PENDING_UPDATE, transportDocumentStatus, useTDRef),
                                      // Special-case: UC2 -> UC3 -> UC5 -> ...
                                      // - Doing thenAllPathsFrom(...) from UC2 would cause UC3 ->
                                      // UC2 -> UC3 ->
                                      // UC2 -> UC3 -> ...
                                      //   patterns (it eventually resolves, but it is unhelpful
                                      // many cases)
                                      // To ensure that UC2 -> UC3 -> UC5 -> ... works properly we
                                      // manually do
                                      // the subtree here.
                                      // Otherwise, we would never test the UC2 -> UC3 -> UC5 -> ...
                                      // flow
                                      // because neither UC2 and UC3
                                      // are considered happy paths.
                                      uc5ShipperCancelUpdateToShippingInstructions(
                                              SI_PENDING_UPDATE, useTDRef)
                                          .then(
                                              shipperGetShippingInstructions(
                                                      SI_PENDING_UPDATE,
                                                      SI_UPDATE_CANCELLED,
                                                      useTDRef)
                                                  .thenEither(
                                                      noAction()
                                                          .thenHappyPathFrom(
                                                              SI_PENDING_UPDATE,
                                                              transportDocumentStatus,
                                                              useTDRef),
                                                      uc3ShipperSubmitUpdatedShippingInstructions(
                                                              SI_PENDING_UPDATE, useTDRef)
                                                          .then(
                                                              shipperGetShippingInstructions(
                                                                      SI_PENDING_UPDATE,
                                                                      SI_UPDATE_RECEIVED,
                                                                      useTDRef)
                                                                  .then(
                                                                      shipperGetShippingInstructions(
                                                                              SI_PENDING_UPDATE,
                                                                              SI_UPDATE_RECEIVED,
                                                                              true,
                                                                              useTDRef)
                                                                          .thenHappyPathFrom(
                                                                              SI_UPDATE_RECEIVED,
                                                                              SI_PENDING_UPDATE,
                                                                              transportDocumentStatus,
                                                                              useTDRef)))))))),
              aucShipperSendOutOfOrderSIMessage(OutOfOrderMessageType.CANCEL_SI_UPDATE, useTDRef)
                  .then(
                      shipperGetShippingInstructions(shippingInstructionsStatus, useTDRef)
                          .thenHappyPathFrom(
                              shippingInstructionsStatus,
                              memoryState,
                              transportDocumentStatus,
                              useTDRef)),
              aucShipperSendOutOfOrderSIMessage(OutOfOrderMessageType.APPROVE_TD, true)
                  .then(
                      shipperGetShippingInstructions(shippingInstructionsStatus, useTDRef)
                          .thenHappyPathFrom(
                              shippingInstructionsStatus,
                              memoryState,
                              transportDocumentStatus,
                              useTDRef)));
        }
        yield thenEither(
            uc3ShipperSubmitUpdatedShippingInstructions(SI_PENDING_UPDATE, useTDRef)
                .then(
                    shipperGetShippingInstructions(SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, useTDRef)
                        .then(
                            shipperGetShippingInstructions(
                                    SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, true, useTDRef)
                                .thenEither(
                                    noAction()
                                        .thenHappyPathFrom(
                                            SI_PENDING_UPDATE, transportDocumentStatus, useTDRef),
                                    // Special-case: UC2 -> UC3 -> UC5 -> ...
                                    // - Doing thenAllPathsFrom(...) from UC2 would cause UC3 -> UC2
                                    // -> UC3 ->
                                    // UC2 -> UC3 -> ...
                                    //   patterns (it eventually resolves, but it is unhelpful many
                                    // cases)
                                    // To ensure that UC2 -> UC3 -> UC5 -> ... works properly we
                                    // manually do
                                    // the subtree here.
                                    // Otherwise, we would never test the UC2 -> UC3 -> UC5 -> ...
                                    // flow
                                    // because neither UC2 and UC3
                                    // are considered happy paths.
                                    uc5ShipperCancelUpdateToShippingInstructions(
                                            SI_PENDING_UPDATE, useTDRef)
                                        .then(
                                            shipperGetShippingInstructions(
                                                    SI_PENDING_UPDATE,
                                                    SI_UPDATE_CANCELLED,
                                                    useTDRef)
                                                .thenEither(
                                                    noAction()
                                                        .thenHappyPathFrom(
                                                            SI_PENDING_UPDATE,
                                                            transportDocumentStatus,
                                                            useTDRef),
                                                    uc3ShipperSubmitUpdatedShippingInstructions(
                                                            SI_PENDING_UPDATE, useTDRef)
                                                        .then(
                                                            shipperGetShippingInstructions(
                                                                    SI_PENDING_UPDATE,
                                                                    SI_UPDATE_RECEIVED,
                                                                    useTDRef)
                                                                .then(
                                                                    shipperGetShippingInstructions(
                                                                            SI_PENDING_UPDATE,
                                                                            SI_UPDATE_RECEIVED,
                                                                            true,
                                                                            useTDRef)
                                                                        .thenHappyPathFrom(
                                                                            SI_UPDATE_RECEIVED,
                                                                            SI_PENDING_UPDATE,
                                                                            transportDocumentStatus,
                                                                            useTDRef)))))))),
            aucShipperSendOutOfOrderSIMessage(OutOfOrderMessageType.CANCEL_SI_UPDATE, useTDRef)
                .then(
                    shipperGetShippingInstructions(shippingInstructionsStatus, useTDRef)
                        .thenHappyPathFrom(
                            shippingInstructionsStatus,
                            memoryState,
                            transportDocumentStatus,
                            useTDRef)));
      }
      case SI_UPDATE_CONFIRMED ->
          thenEither(
              noAction().thenHappyPathFrom(SI_UPDATE_CONFIRMED, transportDocumentStatus, useTDRef),
              // Just to validate that the "Carrier" does not get "stuck"
              uc2CarrierRequestUpdateToShippingInstruction()
                  .then(
                      shipperGetShippingInstructions(SI_PENDING_UPDATE, useTDRef)
                          .thenHappyPathFrom(SI_PENDING_UPDATE, transportDocumentStatus, useTDRef)),
              uc3ShipperSubmitUpdatedShippingInstructions(SI_PENDING_UPDATE, useTDRef)
                  .then(
                      shipperGetShippingInstructions(
                              SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, useTDRef)
                          .then(
                              shipperGetShippingInstructions(
                                      SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, true, useTDRef)
                                  .thenHappyPathFrom(
                                      SI_UPDATE_RECEIVED,
                                      SI_PENDING_UPDATE,
                                      transportDocumentStatus,
                                      useTDRef))));
      case SI_UPDATE_CANCELLED, SI_UPDATE_DECLINED ->
          throw new AssertionError(
              "Please use the black state rather than " + shippingInstructionsStatus.name());
      case SI_ANY -> throw new AssertionError("Not a real/reachable state");
      case SI_COMPLETED ->
          thenEither(
              useTDRef
                  ? aucShipperSendOutOfOrderSIMessage(OutOfOrderMessageType.APPROVE_TD, true)
                      .then(shipperGetShippingInstructions(SI_COMPLETED, true))
                  : noAction(),
              aucShipperSendOutOfOrderSIMessage(OutOfOrderMessageType.SUBMIT_SI_UPDATE, useTDRef)
                  .then(shipperGetShippingInstructions(SI_COMPLETED, useTDRef)),
              aucShipperSendOutOfOrderSIMessage(OutOfOrderMessageType.CANCEL_SI_UPDATE, useTDRef)
                  .then(shipperGetShippingInstructions(SI_COMPLETED, useTDRef)));
    };
  }

  private EblScenarioListBuilder thenHappyPathFrom(
    ShippingInstructionsStatus shippingInstructionsStatus, TransportDocumentStatus transportDocumentStatus, boolean useTDRef) {
    return thenHappyPathFrom(shippingInstructionsStatus, null, transportDocumentStatus, useTDRef);
  }

  private EblScenarioListBuilder thenHappyPathFrom(
      ShippingInstructionsStatus shippingInstructionsStatus, ShippingInstructionsStatus memoryState, TransportDocumentStatus transportDocumentStatus, boolean useTDRef) {
    return switch (shippingInstructionsStatus) {
      case SI_START ->
          then(
              uc1ShipperSubmitShippingInstructions()
                  .then(
                      shipperGetShippingInstructions(SI_RECEIVED, useTDRef)
                          .thenHappyPathFrom(SI_RECEIVED, transportDocumentStatus, useTDRef)));
      case SI_UPDATE_CONFIRMED, SI_RECEIVED ->
          then(
              switch (transportDocumentStatus) {
                case TD_START, TD_DRAFT ->
                    uc6CarrierPublishDraftTransportDocument(false)
                        .then(
                            shipperGetShippingInstructionsRecordTDRef()
                                .then(
                                    shipperGetTransportDocument(TD_DRAFT)
                                        .thenHappyPathFrom(TD_DRAFT)));
                case TD_ISSUED ->
                    uc9CarrierAwaitSurrenderRequestForAmendment()
                        .then(
                            shipperGetTransportDocument(TD_PENDING_SURRENDER_FOR_AMENDMENT)
                                .thenHappyPathFrom(TD_PENDING_SURRENDER_FOR_AMENDMENT));
                default ->
                    throw new IllegalStateException(
                        "Unexpected transportDocumentStatus: " + transportDocumentStatus.name());
              });
      case SI_PENDING_UPDATE ->
          then(
              uc3ShipperSubmitUpdatedShippingInstructions(SI_PENDING_UPDATE, useTDRef)
                  .then(
                      shipperGetShippingInstructions(
                              SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, useTDRef)
                          .then(
                              shipperGetShippingInstructions(
                                      SI_PENDING_UPDATE, SI_UPDATE_RECEIVED, true, useTDRef)
                                  .thenHappyPathFrom(
                                      SI_UPDATE_RECEIVED,
                                      SI_PENDING_UPDATE,
                                      transportDocumentStatus,
                                      useTDRef))));
      case SI_UPDATE_RECEIVED ->
          then(
              uc4ACarrierAcceptUpdatedShippingInstructions()
                  .then(
                      shipperGetShippingInstructions(SI_RECEIVED, SI_UPDATE_CONFIRMED, useTDRef)
                          .thenHappyPathFrom(
                              SI_UPDATE_CONFIRMED, transportDocumentStatus, useTDRef)));
      case SI_COMPLETED -> then(noAction());
      case SI_UPDATE_CANCELLED, SI_UPDATE_DECLINED ->
          throw new AssertionError(
              "Please use the black state rather than " + shippingInstructionsStatus.name());
      case SI_ANY -> throw new AssertionError("Not a real/reachable state");
    };
  }

  private EblScenarioListBuilder thenAllPathsFrom(TransportDocumentStatus transportDocumentStatus) {
    return switch (transportDocumentStatus) {
      case TD_DRAFT ->
          thenEither(
              uc7ShipperApproveDraftTransportDocument()
                  .then(shipperGetTransportDocument(TD_APPROVED).thenAllPathsFrom(TD_APPROVED)),
              uc8CarrierIssueTransportDocument()
                  .then(
                      shipperGetTransportDocument(TD_ISSUED)
                          // Using happy path here as requested in
                          // https://github.com/dcsaorg/Conformance-Gateway/pull/29#discussion_r1421732797
                          .thenHappyPathFrom(TD_ISSUED)),
              uc3ShipperSubmitUpdatedShippingInstructions(SI_RECEIVED, true)
                  .then(
                      shipperGetShippingInstructions(SI_RECEIVED, SI_UPDATE_RECEIVED, true)
                          .then(
                              shipperGetShippingInstructions(
                                      SI_RECEIVED, SI_UPDATE_RECEIVED, true, true)
                                  .thenEither(
                                      uc4ACarrierAcceptUpdatedShippingInstructions()
                                          .then(
                                              shipperGetShippingInstructions(
                                                      SI_RECEIVED, SI_UPDATE_CONFIRMED, true)
                                                  .thenEither(
                                                      noAction()
                                                          .thenHappyPathFrom(
                                                              SI_RECEIVED,
                                                              transportDocumentStatus,
                                                              true),
                                                      aucShipperSendOutOfOrderSIMessage(
                                                              OutOfOrderMessageType
                                                                  .CANCEL_SI_UPDATE,
                                                              true)
                                                          .then(
                                                              shipperGetShippingInstructions(
                                                                      SI_RECEIVED,
                                                                      SI_UPDATE_CONFIRMED,
                                                                      true)
                                                                  .thenHappyPathFrom(
                                                                      SI_RECEIVED,
                                                                      transportDocumentStatus,
                                                                      true)))),
                                      uc2CarrierRequestUpdateToShippingInstruction()
                                          .then(
                                              shipperGetShippingInstructions(
                                                      SI_PENDING_UPDATE, true)
                                                  .thenAllPathsFrom(
                                                      SI_PENDING_UPDATE,
                                                      transportDocumentStatus,
                                                      true)),
                                      uc5ShipperCancelUpdateToShippingInstructions(
                                              SI_RECEIVED, true)
                                          .then(
                                              shipperGetShippingInstructions(
                                                      SI_RECEIVED, SI_UPDATE_CANCELLED, true)
                                                  .thenHappyPathFrom(transportDocumentStatus))))),
              uc3ShipperSubmitUpdatedShippingInstructions(SI_RECEIVED, false)
                  .then(
                      shipperGetShippingInstructions(SI_RECEIVED, SI_UPDATE_RECEIVED, false)
                          .then(
                              shipperGetShippingInstructions(
                                      SI_RECEIVED, SI_UPDATE_RECEIVED, true, false)
                                  .thenEither(
                                      uc4ACarrierAcceptUpdatedShippingInstructions()
                                          .then(
                                              shipperGetShippingInstructions(
                                                      SI_RECEIVED, SI_UPDATE_CONFIRMED, false)
                                                  .thenEither(
                                                      noAction()
                                                          .thenHappyPathFrom(
                                                              SI_RECEIVED,
                                                              transportDocumentStatus,
                                                              false),
                                                      aucShipperSendOutOfOrderSIMessage(
                                                              OutOfOrderMessageType
                                                                  .CANCEL_SI_UPDATE,
                                                              false)
                                                          .then(
                                                              shipperGetShippingInstructions(
                                                                      SI_RECEIVED,
                                                                      SI_UPDATE_CONFIRMED,
                                                                      false)
                                                                  .thenHappyPathFrom(
                                                                      SI_RECEIVED,
                                                                      transportDocumentStatus,
                                                                      false)))),
                                      uc2CarrierRequestUpdateToShippingInstruction()
                                          .then(
                                              shipperGetShippingInstructions(
                                                      SI_PENDING_UPDATE, false)
                                                  .thenAllPathsFrom(
                                                      SI_PENDING_UPDATE,
                                                      transportDocumentStatus,
                                                      false)),
                                      uc4DCarrierDeclineUpdatedShippingInstructions(SI_RECEIVED)
                                          .then(
                                              shipperGetShippingInstructions(
                                                      SI_RECEIVED, SI_UPDATE_DECLINED, false)
                                                  .thenHappyPathFrom(transportDocumentStatus)),
                                      uc5ShipperCancelUpdateToShippingInstructions(
                                              SI_RECEIVED, false)
                                          .then(
                                              shipperGetShippingInstructions(
                                                      SI_RECEIVED, SI_UPDATE_CANCELLED, false)
                                                  .thenHappyPathFrom(transportDocumentStatus))))));
      case TD_APPROVED ->
          then(
              uc8CarrierIssueTransportDocument()
                  .then(shipperGetTransportDocument(TD_ISSUED).thenAllPathsFrom(TD_ISSUED)));
      case TD_ISSUED ->
          thenEither(
              uc3ShipperSubmitUpdatedShippingInstructions(SI_RECEIVED, true)
                  .thenHappyPathFrom(SI_UPDATE_RECEIVED, TD_ISSUED, true),
              uc3ShipperSubmitUpdatedShippingInstructions(SI_RECEIVED, false)
                  .thenHappyPathFrom(SI_UPDATE_RECEIVED, TD_ISSUED, false),
              uc9CarrierAwaitSurrenderRequestForAmendment()
                  .then(
                      shipperGetTransportDocument(TD_PENDING_SURRENDER_FOR_AMENDMENT)
                          .thenAllPathsFrom(TD_PENDING_SURRENDER_FOR_AMENDMENT)),
              uc12CarrierAwaitSurrenderRequestForDelivery()
                  .then(
                      shipperGetTransportDocument(TD_PENDING_SURRENDER_FOR_DELIVERY)
                          .thenAllPathsFrom(TD_PENDING_SURRENDER_FOR_DELIVERY)));
      case TD_PENDING_SURRENDER_FOR_AMENDMENT ->
          thenEither(
              uc10ACarrierAcceptSurrenderRequestForAmendment()
                  .then(
                      shipperGetTransportDocument(TD_SURRENDERED_FOR_AMENDMENT)
                          .thenAllPathsFrom(TD_SURRENDERED_FOR_AMENDMENT)),
              uc10RCarrierRejectSurrenderRequestForAmendment()
                  .then(shipperGetTransportDocument(TD_ISSUED).thenHappyPathFrom(TD_ISSUED)));
      case TD_SURRENDERED_FOR_AMENDMENT ->
          then(
              uc11CarrierVoidTransportDocument()
                  .then(
                      uc11ICarrierIssueAmendedTransportDocument()
                          .then(
                              shipperGetTransportDocument(TD_ISSUED)
                                  .thenHappyPathFrom(TD_ISSUED))));
      case TD_PENDING_SURRENDER_FOR_DELIVERY ->
          thenEither(
              uc13ACarrierAcceptSurrenderRequestForDelivery()
                  .then(
                      shipperGetTransportDocument(TD_SURRENDERED_FOR_DELIVERY)
                          .thenAllPathsFrom(TD_SURRENDERED_FOR_DELIVERY)),
              uc13RCarrierRejectSurrenderRequestForDelivery()
                  .then(shipperGetTransportDocument(TD_ISSUED).thenHappyPathFrom(TD_ISSUED)),
              aucShipperSendOutOfOrderSIMessage(OutOfOrderMessageType.APPROVE_TD, true)
                  .then(
                      shipperGetTransportDocument(TD_PENDING_SURRENDER_FOR_DELIVERY)
                          .thenHappyPathFrom(TD_PENDING_SURRENDER_FOR_DELIVERY))
              // TODO: Should UC3 be allowed here or not?
              );
      case TD_SURRENDERED_FOR_DELIVERY ->
          then(
              uc14CarrierConfirmShippingInstructionsComplete()
                  .thenEither(
                      shipperGetShippingInstructions(SI_COMPLETED, false)
                          .thenAllPathsFrom(SI_COMPLETED, null, false),
                      shipperGetShippingInstructions(SI_COMPLETED, true)
                          .thenAllPathsFrom(SI_COMPLETED, null, true)));
      case TD_START, TD_ANY -> throw new AssertionError("Not a real/reachable state");
      case TD_VOIDED -> then(noAction());
    };
  }

  private EblScenarioListBuilder thenHappyPathFrom(TransportDocumentStatus transportDocumentStatus) {
    return switch (transportDocumentStatus) {
      case TD_DRAFT ->
          thenEither(
              uc8CarrierIssueTransportDocument()
                  .then(shipperGetTransportDocument(TD_ISSUED).thenHappyPathFrom(TD_ISSUED)),
              uc7ShipperApproveDraftTransportDocument()
                  .then(shipperGetTransportDocument(TD_APPROVED).thenHappyPathFrom(TD_APPROVED)));
      case TD_APPROVED ->
          then(
              uc8CarrierIssueTransportDocument()
                  .then(shipperGetTransportDocument(TD_ISSUED).thenHappyPathFrom(TD_ISSUED)));
      case TD_ISSUED ->
          then(
              uc12CarrierAwaitSurrenderRequestForDelivery()
                  .then(
                      shipperGetTransportDocument(TD_PENDING_SURRENDER_FOR_DELIVERY)
                          .thenHappyPathFrom(TD_PENDING_SURRENDER_FOR_DELIVERY)));
      case TD_PENDING_SURRENDER_FOR_AMENDMENT ->
          then(
              uc10ACarrierAcceptSurrenderRequestForAmendment()
                  .then(
                      shipperGetTransportDocument(TD_SURRENDERED_FOR_AMENDMENT)
                          .thenHappyPathFrom(TD_SURRENDERED_FOR_AMENDMENT)));
      case TD_SURRENDERED_FOR_AMENDMENT ->
          then(
              uc11CarrierVoidTransportDocument()
                  .then(
                      uc11ICarrierIssueAmendedTransportDocument()
                          .then(
                              shipperGetTransportDocument(TD_ISSUED)
                                  .thenHappyPathFrom(TD_ISSUED))));
      case TD_PENDING_SURRENDER_FOR_DELIVERY ->
          then(
              uc13ACarrierAcceptSurrenderRequestForDelivery()
                  .then(
                      shipperGetTransportDocument(TD_SURRENDERED_FOR_DELIVERY)
                          .thenHappyPathFrom(TD_SURRENDERED_FOR_DELIVERY)));
      case TD_SURRENDERED_FOR_DELIVERY ->
          then(
              uc14CarrierConfirmShippingInstructionsComplete()
                  .thenEither(
                      shipperGetShippingInstructions(SI_COMPLETED, false)
                          .thenHappyPathFrom(SI_COMPLETED, null, false),
                      shipperGetShippingInstructions(SI_COMPLETED, true)
                          .thenHappyPathFrom(SI_COMPLETED, null, true)));
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

  private static EblScenarioListBuilder uc4ACarrierAcceptUpdatedShippingInstructions() {
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

  private static EblScenarioListBuilder uc4DCarrierDeclineUpdatedShippingInstructions(
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

  private static EblScenarioListBuilder uc10ACarrierAcceptSurrenderRequestForAmendment() {
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

  private static EblScenarioListBuilder uc10RCarrierRejectSurrenderRequestForAmendment() {
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

  private static EblScenarioListBuilder uc11ICarrierIssueAmendedTransportDocument() {
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

  private static EblScenarioListBuilder uc13ACarrierAcceptSurrenderRequestForDelivery() {
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

  private static EblScenarioListBuilder uc13RCarrierRejectSurrenderRequestForDelivery() {
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

  private static EblScenarioListBuilder aucShipperSendOutOfOrderSIMessage(
      OutOfOrderMessageType outOfOrderMessageType, boolean useTDRef) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String shipperPartyName = threadLocalShipperPartyName.get();
    return new EblScenarioListBuilder(
      previousAction -> {
        var schema = switch (outOfOrderMessageType) {
          case SUBMIT_SI_UPDATE -> PUT_EBL_SCHEMA_NAME;
          case CANCEL_SI_UPDATE -> PATCH_SI_SCHEMA_NAME;
          case APPROVE_TD -> PATCH_TD_SCHEMA_NAME;
        };
        return new AUC_Shipper_SendOutOfOrderSIMessageAction(
          carrierPartyName,
          shipperPartyName,
          (EblAction) previousAction,
          outOfOrderMessageType,
          useTDRef,
          resolveMessageSchemaValidator(
            EBL_API, schema));
      });
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
