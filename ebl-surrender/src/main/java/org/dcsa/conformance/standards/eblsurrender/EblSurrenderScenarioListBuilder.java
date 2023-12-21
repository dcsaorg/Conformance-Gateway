package org.dcsa.conformance.standards.eblsurrender;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.eblsurrender.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.eblsurrender.action.SurrenderRequestAction;
import org.dcsa.conformance.standards.eblsurrender.action.SurrenderResponseAction;
import org.dcsa.conformance.standards.eblsurrender.action.VoidAndReissueAction;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderState;

import java.util.function.Function;

import static org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderState.*;

@Slf4j
public class EblSurrenderScenarioListBuilder
    extends ScenarioListBuilder<EblSurrenderScenarioListBuilder> {
  private static final ThreadLocal<EblSurrenderComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPlatformPartyName = new ThreadLocal<>();

  public static EblSurrenderScenarioListBuilder buildTree(
      EblSurrenderComponentFactory componentFactory,
      String carrierPartyName,
      String platformPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalPlatformPartyName.set(platformPartyName);
    return supplyAvailableTdrAction()
            .thenAllPathsFrom(AVAILABLE_FOR_SURRENDER);
  }

  private EblSurrenderScenarioListBuilder thenAllPathsFrom(
          EblSurrenderState surrenderState) {
    return switch (surrenderState) {
      case AVAILABLE_FOR_SURRENDER -> thenEither(
              requestSurrenderForDelivery(202).thenAllPathsFrom(DELIVERY_SURRENDER_REQUESTED),
              requestSurrenderForAmendment(202).thenAllPathsFrom(AMENDMENT_SURRENDER_REQUESTED),
              acceptSurrenderRequest(409).thenAllHappyPathsFrom(AVAILABLE_FOR_SURRENDER),
              rejectSurrenderRequest(409).thenAllHappyPathsFrom(AVAILABLE_FOR_SURRENDER)
      );
      case DELIVERY_SURRENDER_REQUESTED -> thenEither(
              acceptSurrenderRequest(204).thenAllPathsFrom(SURRENDERED_FOR_DELIVERY),
              rejectSurrenderRequest(204).thenAllHappyPathsFrom(AVAILABLE_FOR_SURRENDER),
              requestSurrenderForAmendment(409).thenAllHappyPathsFrom(DELIVERY_SURRENDER_REQUESTED),
              requestSurrenderForDelivery(409).thenAllHappyPathsFrom(DELIVERY_SURRENDER_REQUESTED)
      );
      case SURRENDERED_FOR_DELIVERY -> thenAnyRequest(409);
      case AMENDMENT_SURRENDER_REQUESTED -> thenEither(
              acceptSurrenderRequest(204).thenAllPathsFrom(SURRENDERED_FOR_AMENDMENT),
              rejectSurrenderRequest(204).thenAllHappyPathsFrom(AVAILABLE_FOR_SURRENDER),
              requestSurrenderForAmendment(409).thenAllHappyPathsFrom(AMENDMENT_SURRENDER_REQUESTED),
              requestSurrenderForDelivery(409).thenAllHappyPathsFrom(AMENDMENT_SURRENDER_REQUESTED)
      );
      case SURRENDERED_FOR_AMENDMENT -> thenEither(
              acceptSurrenderRequest(409).thenAllHappyPathsFrom(SURRENDERED_FOR_AMENDMENT),
              rejectSurrenderRequest(409).thenAllHappyPathsFrom(SURRENDERED_FOR_AMENDMENT),
              requestSurrenderForAmendment(409).thenAllHappyPathsFrom(SURRENDERED_FOR_AMENDMENT),
              requestSurrenderForDelivery(409).thenAllHappyPathsFrom(SURRENDERED_FOR_AMENDMENT),
              voidAndReissue().thenAllHappyPathsFrom(AVAILABLE_FOR_SURRENDER)
      );
    };
  }

  private EblSurrenderScenarioListBuilder thenAllHappyPathsFrom(
          EblSurrenderState surrenderState) {
    var cases = switch (surrenderState) {
      case AVAILABLE_FOR_SURRENDER -> requestSurrenderForDelivery(202).thenAllHappyPathsFrom(DELIVERY_SURRENDER_REQUESTED);
      case DELIVERY_SURRENDER_REQUESTED -> acceptSurrenderRequest(204);
      case SURRENDERED_FOR_DELIVERY -> noAction();
      case AMENDMENT_SURRENDER_REQUESTED -> acceptSurrenderRequest(204)
              .thenAllHappyPathsFrom(SURRENDERED_FOR_AMENDMENT);
      case SURRENDERED_FOR_AMENDMENT -> voidAndReissue().thenAllHappyPathsFrom(AVAILABLE_FOR_SURRENDER);
    };
    return thenEither(cases);
  }

  private EblSurrenderScenarioListBuilder noAction() {
    return new EblSurrenderScenarioListBuilder(null);
  }

  private EblSurrenderScenarioListBuilder thenAnyRequest(int status) {
    return thenEither(
            noAction(),
            requestSurrenderForAmendment(status),
            requestSurrenderForDelivery(status),
            rejectSurrenderRequest(status),
            acceptSurrenderRequest(status)
    );
  }

  private static EblSurrenderScenarioListBuilder supplyAvailableTdrAction() {
    log.debug("EblSurrenderScenarioListBuilder.supplyAvailableTdrAction()");
    String carrierPartyName = threadLocalCarrierPartyName.get();
    return new EblSurrenderScenarioListBuilder(
        noPreviousAction -> new SupplyScenarioParametersAction(carrierPartyName, null));
  }

  private EblSurrenderScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static EblSurrenderScenarioListBuilder requestSurrenderForAmendment(int status) {
    log.debug(
        "EblSurrenderScenarioListBuilder.requestSurrenderForAmendment(%d)".formatted(status));
    return _surrenderRequestBuilder(true, status);
  }

  private static EblSurrenderScenarioListBuilder requestSurrenderForDelivery(int status) {
    log.debug(
        "EblSurrenderScenarioListBuilder.requestSurrenderForDelivery(%d)".formatted(status));
    return _surrenderRequestBuilder(false, status);
  }

  private static EblSurrenderScenarioListBuilder _surrenderRequestBuilder(
      boolean forAmendment, int expectedStatus) {
    EblSurrenderComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblSurrenderScenarioListBuilder(
        previousAction ->
            new SurrenderRequestAction(
                forAmendment,
                platformPartyName,
                carrierPartyName,
                expectedStatus,
                previousAction,
                componentFactory.getMessageSchemaValidator(
                    EblSurrenderRole.CARRIER.getConfigName(), true),
                componentFactory.getMessageSchemaValidator(
                    EblSurrenderRole.CARRIER.getConfigName(), false)));
  }

  private static EblSurrenderScenarioListBuilder acceptSurrenderRequest(int status) {
    log.debug("EblSurrenderScenarioListBuilder.acceptSurrenderRequest(%d)".formatted(status));
    return _surrenderResponseBuilder(true, status);
  }

  private static EblSurrenderScenarioListBuilder rejectSurrenderRequest(int status) {
    log.debug("EblSurrenderScenarioListBuilder.rejectSurrenderRequest(%d)".formatted(status));
    return _surrenderResponseBuilder(false, status);
  }

  private static EblSurrenderScenarioListBuilder _surrenderResponseBuilder(
      boolean accept, int expectedStatus) {
    EblSurrenderComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblSurrenderScenarioListBuilder(
        previousAction ->
            new SurrenderResponseAction(
                accept,
                carrierPartyName,
                platformPartyName,
                expectedStatus,
                previousAction,
                componentFactory.getMessageSchemaValidator(
                    EblSurrenderRole.PLATFORM.getConfigName(), true)));
  }

  private static EblSurrenderScenarioListBuilder voidAndReissue() {
    log.debug("EblSurrenderScenarioListBuilder.voidAndReissue()");
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblSurrenderScenarioListBuilder(
        previousAction ->
            new VoidAndReissueAction(carrierPartyName, platformPartyName, previousAction));
  }
}
