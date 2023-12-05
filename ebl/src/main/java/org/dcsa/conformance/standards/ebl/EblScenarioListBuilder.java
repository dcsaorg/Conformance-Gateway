package org.dcsa.conformance.standards.ebl;

import static org.dcsa.conformance.standards.ebl.party.ShippingInstructionsStatus.*;
import static org.dcsa.conformance.standards.ebl.party.TransportDocumentStatus.*;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.ebl.action.*;
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
  private static final String GET_EBL_SCHEMA_NAME = "ShippingInstructionsResponse";
  private static final String GET_TD_SCHEMA_NAME = "getTransportDocument";
  private static final String POST_EBL_SCHEMA_NAME = "ShippingInstructionsRequest";
  private static final String PUT_EBL_SCHEMA_NAME = "ShippingInstructionsUpdate";
  private static final String EBL_REF_STATUS_SCHEMA_NAME = "ShippingInstructionsRefStatus";
  private static final String EBL_SI_NOTIFICATION_SCHEMA_NAME = "ShippingInstructionsNotification";
  private static final String EBL_TD_NOTIFICATION_SCHEMA_NAME = "TransportDocumentNotification";

  public static EblScenarioListBuilder buildTree(
      EblComponentFactory componentFactory, String carrierPartyName, String shipperPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalShipperPartyName.set(shipperPartyName);
    return carrier_SupplyScenarioParameters().thenAllPathsFrom(SI_START);
  }

  private EblScenarioListBuilder thenAllPathsFrom(
      ShippingInstructionsStatus shippingInstructionsStatus) {
    return switch (shippingInstructionsStatus) {
      case SI_START -> then(
          uc1_shipper_submitShippingInstructions()
              .then(
                  shipper_GetShippingInstructions(SI_RECEIVED, TD_START)
                      .thenAllPathsFrom(SI_RECEIVED)));
      case SI_RECEIVED -> thenEither(
          uc2_carrier_requestUpdateToShippingInstruction()
            .then(shipper_GetShippingInstructions(SI_PENDING_UPDATE, TD_START)
              .thenHappyPathFrom(SI_PENDING_UPDATE)),
          uc3_shipper_submitUpdatedShippingInstructions()
            .then(
              shipper_GetShippingInstructions(SI_RECEIVED, SI_UPDATE_RECEIVED, TD_START)
                .then(
                  uc2_carrier_requestUpdateToShippingInstruction()
                    .then(shipper_GetShippingInstructions(SI_PENDING_UPDATE, TD_START)
                      .thenHappyPathFrom(SI_PENDING_UPDATE)))),
          uc6_carrier_publishDraftTransportDocument()
              .then(
                  shipper_GetShippingInstructions(SI_RECEIVED, TD_DRAFT, true)
                    .then(shipper_GetTransportDocument(TD_DRAFT)
                      .thenAllPathsFrom(TD_DRAFT))));
      default -> then(noAction()); // TODO
    };
  }

  private EblScenarioListBuilder thenHappyPathFrom(
      ShippingInstructionsStatus shippingInstructionsStatus) {
    return then(noAction()); // TODO
  }

  private EblScenarioListBuilder thenAllPathsFrom(TransportDocumentStatus transportDocumentStatus) {
    return switch (transportDocumentStatus) {
      case TD_DRAFT -> then(
          uc8_carrier_issueTransportDocument()
              .then(
                  shipper_GetTransportDocument(TD_ISSUED)
                    .thenAllPathsFrom(TD_ISSUED)));
      case TD_ISSUED -> then(noAction()); // TODO
      default -> then(noAction()); // TODO
    };
  }

  private EblScenarioListBuilder thenHappyPathFrom(TransportDocumentStatus tdStatus) {
    return then(noAction()); // TODO
  }

  private EblScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static EblScenarioListBuilder noAction() {
    return new EblScenarioListBuilder(null);
  }

  private static EblScenarioListBuilder carrier_SupplyScenarioParameters() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    return new EblScenarioListBuilder(
        previousAction -> new Carrier_SupplyScenarioParametersAction(carrierPartyName));
  }


  private static EblScenarioListBuilder shipper_GetShippingInstructions(
    ShippingInstructionsStatus expectedSiStatus, TransportDocumentStatus expectedTdStatus) {
    return shipper_GetShippingInstructions(expectedSiStatus, null, expectedTdStatus);
  }

  private static EblScenarioListBuilder shipper_GetShippingInstructions(
    ShippingInstructionsStatus expectedSiStatus, TransportDocumentStatus expectedTdStatus, boolean recordTDR) {
    return shipper_GetShippingInstructions(expectedSiStatus, null, expectedTdStatus, false, recordTDR);
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
}
