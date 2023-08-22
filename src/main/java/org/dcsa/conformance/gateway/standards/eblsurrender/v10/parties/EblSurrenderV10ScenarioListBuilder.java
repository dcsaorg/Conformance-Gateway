package org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties;

import java.util.function.Function;

import org.dcsa.conformance.gateway.check.ActionCheck;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.dcsa.conformance.gateway.scenarios.ScenarioListBuilder;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.check.SurrenderRequestCheck;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.check.SurrenderResponseCheck;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.check.VoidAndReissueCheck;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SupplyAvailableTdrAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SurrenderRequestAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SurrenderResponseAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.VoidAndReissueAction;

public class EblSurrenderV10ScenarioListBuilder extends ScenarioListBuilder {
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPlatformPartyName = new ThreadLocal<>();

  public static ScenarioListBuilder buildTree(String carrierPartyName, String platformPartyName) {
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalPlatformPartyName.set(platformPartyName);
    return supplyAvailableTdrAction()
        .thenEither(
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
    return new EblSurrenderV10ScenarioListBuilder(
        noPreviousAction -> new SupplyAvailableTdrAction(threadLocalCarrierPartyName.get(), null),
        noPreviousCheck -> new ActionCheck("Scenarios", null));
  }

  private EblSurrenderV10ScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder,
      Function<ActionCheck, ActionCheck> checkBuilder) {
    super(actionBuilder, checkBuilder);
  }

  private static EblSurrenderV10ScenarioListBuilder requestSurrenderForAmendment(int status) {
    return _surrenderRequestBuilder(true, status);
  }

  private static EblSurrenderV10ScenarioListBuilder requestSurrenderForDelivery(int status) {
    return _surrenderRequestBuilder(false, status);
  }

  private static EblSurrenderV10ScenarioListBuilder _surrenderRequestBuilder(
      boolean forAmendment, int expectedStatus) {
    return new EblSurrenderV10ScenarioListBuilder(
        previousAction ->
            new SurrenderRequestAction(
                forAmendment,
                threadLocalPlatformPartyName.get(),
                threadLocalCarrierPartyName.get(),
                expectedStatus,
                previousAction),
        previousCheck -> new SurrenderRequestCheck("TODO", previousCheck));
  }

  private static EblSurrenderV10ScenarioListBuilder acceptSurrenderRequest(int status) {
    return _surrenderResponseBuilder(true, status);
  }

  private static EblSurrenderV10ScenarioListBuilder rejectSurrenderRequest(int status) {
    return _surrenderResponseBuilder(false, status);
  }

  private static EblSurrenderV10ScenarioListBuilder _surrenderResponseBuilder(
      boolean accept, int expectedStatus) {
    return new EblSurrenderV10ScenarioListBuilder(
        previousAction ->
            new SurrenderResponseAction(
                accept,
                threadLocalCarrierPartyName.get(),
                threadLocalPlatformPartyName.get(),
                expectedStatus,
                previousAction),
        previousCheck -> new SurrenderResponseCheck("TODO", previousCheck));
  }

  private static EblSurrenderV10ScenarioListBuilder voidAndReissue() {
    return new EblSurrenderV10ScenarioListBuilder(
        previousAction ->
            new VoidAndReissueAction(
                threadLocalCarrierPartyName.get(),
                threadLocalPlatformPartyName.get(),
                previousAction),
        previousCheck -> new VoidAndReissueCheck("TODO", previousCheck));
  }
}
