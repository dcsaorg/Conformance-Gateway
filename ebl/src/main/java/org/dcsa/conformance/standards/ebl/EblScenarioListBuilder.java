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
  private static final String GET_EBL_SCHEMA_NAME = "getShippingInstructions";

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
      case SI_RECEIVED -> then(
          uc6_carrier_publishDraftTransportDocument()
              .then(
                  shipper_GetShippingInstructions(SI_RECEIVED, TD_DRAFT)
                      .thenAllPathsFrom(TD_DRAFT)));
      default -> null; // TODO
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
                  shipper_GetShippingInstructions(SI_ANY, TD_ISSUED).thenAllPathsFrom(TD_ISSUED)));
      case TD_ISSUED -> then(noAction()); // TODO
      default -> null; // TODO
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
                null,
                componentFactory.getMessageSchemaValidator(EBL_API, GET_EBL_SCHEMA_NAME),
                false));
  }

  private static EblScenarioListBuilder uc1_shipper_submitShippingInstructions() {
    return noAction();
  }

  private static EblScenarioListBuilder uc6_carrier_publishDraftTransportDocument() {
    return noAction();
  }

  private static EblScenarioListBuilder uc8_carrier_issueTransportDocument() {
    return noAction();
  }
}