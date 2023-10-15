package org.dcsa.conformance.standards.eblsurrender;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.eblsurrender.action.SupplyAvailableTdrAction;
import org.dcsa.conformance.standards.eblsurrender.action.SurrenderRequestAction;
import org.dcsa.conformance.standards.eblsurrender.action.SurrenderResponseAction;
import org.dcsa.conformance.standards.eblsurrender.action.VoidAndReissueAction;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

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
        .thenEither(
            requestSurrenderForDelivery(202).then(acceptSurrenderRequest(204)),
            requestSurrenderForDelivery(202)
                .thenEither(
                    acceptSurrenderRequest(204)
                        .thenEither(
                            requestSurrenderForDelivery(409),
                            requestSurrenderForAmendment(409),
                            acceptSurrenderRequest(409),
                            rejectSurrenderRequest(409)),
                    rejectSurrenderRequest(204)
                        .thenEither(
                            requestSurrenderForDelivery(202).then(acceptSurrenderRequest(204)),
                            requestSurrenderForAmendment(202).then(acceptSurrenderRequest(204))),
                    requestSurrenderForDelivery(409).then(acceptSurrenderRequest(204)),
                    requestSurrenderForAmendment(409).then(acceptSurrenderRequest(204))),
            requestSurrenderForAmendment(202).then(acceptSurrenderRequest(204)),
            requestSurrenderForAmendment(202)
                .thenEither(
                    acceptSurrenderRequest(204)
                        .thenEither(
                            requestSurrenderForDelivery(409),
                            requestSurrenderForAmendment(409),
                            acceptSurrenderRequest(409),
                            rejectSurrenderRequest(409),
                            voidAndReissue()
                                .thenEither(
                                    requestSurrenderForDelivery(202)
                                        .then(acceptSurrenderRequest(204)),
                                    requestSurrenderForAmendment(202)
                                        .then(acceptSurrenderRequest(204)),
                                    acceptSurrenderRequest(409),
                                    rejectSurrenderRequest(409))),
                    rejectSurrenderRequest(204)
                        .thenEither(
                            requestSurrenderForDelivery(202).then(acceptSurrenderRequest(204)),
                            requestSurrenderForAmendment(202).then(acceptSurrenderRequest(204))),
                    requestSurrenderForAmendment(409).then(acceptSurrenderRequest(204)),
                    requestSurrenderForDelivery(409).then(acceptSurrenderRequest(204))),
            acceptSurrenderRequest(409)
                .thenEither(
                    requestSurrenderForDelivery(202).then(acceptSurrenderRequest(204)),
                    requestSurrenderForAmendment(202).then(acceptSurrenderRequest(204))),
            rejectSurrenderRequest(409)
                .thenEither(
                    requestSurrenderForDelivery(202).then(acceptSurrenderRequest(204)),
                    requestSurrenderForAmendment(202).then(acceptSurrenderRequest(204))));
  }

  private static EblSurrenderScenarioListBuilder supplyAvailableTdrAction() {
    log.debug("EblSurrenderScenarioListBuilder.supplyAvailableTdrAction()");
    String carrierPartyName = threadLocalCarrierPartyName.get();
    return new EblSurrenderScenarioListBuilder(
        noPreviousAction -> new SupplyAvailableTdrAction(carrierPartyName, null));
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
