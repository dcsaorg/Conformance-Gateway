package org.dcsa.conformance.standards.eblsurrender.v10;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.eblsurrender.v10.action.SupplyAvailableTdrAction;
import org.dcsa.conformance.standards.eblsurrender.v10.action.SurrenderRequestAction;
import org.dcsa.conformance.standards.eblsurrender.v10.action.SurrenderResponseAction;
import org.dcsa.conformance.standards.eblsurrender.v10.action.VoidAndReissueAction;

@Slf4j
public class EblSurrenderV10ScenarioListBuilder
    extends ScenarioListBuilder<EblSurrenderV10ScenarioListBuilder> {
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPlatformPartyName = new ThreadLocal<>();

  public static EblSurrenderV10ScenarioListBuilder buildTree(
      String carrierPartyName, String platformPartyName) {
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

  private static EblSurrenderV10ScenarioListBuilder supplyAvailableTdrAction() {
    log.debug("EblSurrenderV10ScenarioListBuilder.supplyAvailableTdrAction()");
    String carrierPartyName = threadLocalCarrierPartyName.get();
    return new EblSurrenderV10ScenarioListBuilder(
        noPreviousAction -> new SupplyAvailableTdrAction(carrierPartyName, null));
  }

  private EblSurrenderV10ScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static EblSurrenderV10ScenarioListBuilder requestSurrenderForAmendment(int status) {
    log.debug(
        "EblSurrenderV10ScenarioListBuilder.requestSurrenderForAmendment(%d)".formatted(status));
    return _surrenderRequestBuilder(true, status);
  }

  private static EblSurrenderV10ScenarioListBuilder requestSurrenderForDelivery(int status) {
    log.debug(
        "EblSurrenderV10ScenarioListBuilder.requestSurrenderForDelivery(%d)".formatted(status));
    return _surrenderRequestBuilder(false, status);
  }

  private static EblSurrenderV10ScenarioListBuilder _surrenderRequestBuilder(
      boolean forAmendment, int expectedStatus) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblSurrenderV10ScenarioListBuilder(
        previousAction ->
            new SurrenderRequestAction(
                forAmendment, platformPartyName, carrierPartyName, expectedStatus, previousAction));
  }

  private static EblSurrenderV10ScenarioListBuilder acceptSurrenderRequest(int status) {
    log.debug("EblSurrenderV10ScenarioListBuilder.acceptSurrenderRequest(%d)".formatted(status));
    return _surrenderResponseBuilder(true, status);
  }

  private static EblSurrenderV10ScenarioListBuilder rejectSurrenderRequest(int status) {
    log.debug("EblSurrenderV10ScenarioListBuilder.rejectSurrenderRequest(%d)".formatted(status));
    return _surrenderResponseBuilder(false, status);
  }

  private static EblSurrenderV10ScenarioListBuilder _surrenderResponseBuilder(
      boolean accept, int expectedStatus) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblSurrenderV10ScenarioListBuilder(
        previousAction ->
            new SurrenderResponseAction(
                accept, carrierPartyName, platformPartyName, expectedStatus, previousAction));
  }

  private static EblSurrenderV10ScenarioListBuilder voidAndReissue() {
    log.debug("EblSurrenderV10ScenarioListBuilder.voidAndReissue()");
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblSurrenderV10ScenarioListBuilder(
        previousAction ->
            new VoidAndReissueAction(carrierPartyName, platformPartyName, previousAction));
  }
}
