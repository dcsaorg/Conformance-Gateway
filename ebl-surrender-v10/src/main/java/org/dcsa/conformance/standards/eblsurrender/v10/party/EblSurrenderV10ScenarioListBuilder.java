package org.dcsa.conformance.standards.eblsurrender.v10.party;

import java.util.LinkedList;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.ActionCheck;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.eblsurrender.v10.EblSurrenderV10Role;
import org.dcsa.conformance.standards.eblsurrender.v10.check.SurrenderRequestCheck;
import org.dcsa.conformance.standards.eblsurrender.v10.check.SurrenderResponseCheck;
import org.dcsa.conformance.standards.eblsurrender.v10.check.VoidAndReissueCheck;
import org.dcsa.conformance.standards.eblsurrender.v10.scenario.SupplyAvailableTdrAction;
import org.dcsa.conformance.standards.eblsurrender.v10.scenario.SurrenderRequestAction;
import org.dcsa.conformance.standards.eblsurrender.v10.scenario.SurrenderResponseAction;
import org.dcsa.conformance.standards.eblsurrender.v10.scenario.VoidAndReissueAction;
import org.dcsa.conformance.core.traffic.ConformanceExchange;

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
    log.info("EblSurrenderV10ScenarioListBuilder.supplyAvailableTdrAction()");
    String carrierPartyName = threadLocalCarrierPartyName.get();
    return new EblSurrenderV10ScenarioListBuilder(
        noPreviousAction -> new SupplyAvailableTdrAction(carrierPartyName, null),
        noPreviousCheck ->
            new ActionCheck("Scenario handling", null) {
              @Override
              public Stream<LinkedList<ConformanceExchange>> relevantExchangeListsStream() {
                return null;
              }

              @Override
              public boolean isRelevantForRole(String roleName) {
                return childrenStream().anyMatch(child -> child.isRelevantForRole(roleName))
                    || (EblSurrenderV10Role.isCarrier(roleName) && hasNoChildren());
              }
            });
  }

  private EblSurrenderV10ScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder,
      Function<ActionCheck, ActionCheck> checkBuilder) {
    super(actionBuilder, checkBuilder);
  }

  private static EblSurrenderV10ScenarioListBuilder requestSurrenderForAmendment(int status) {
    log.info(
        "EblSurrenderV10ScenarioListBuilder.requestSurrenderForAmendment(%d)".formatted(status));
    return _surrenderRequestBuilder(true, status);
  }

  private static EblSurrenderV10ScenarioListBuilder requestSurrenderForDelivery(int status) {
    log.info(
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
                forAmendment, platformPartyName, carrierPartyName, expectedStatus, previousAction),
        previousCheck ->
            new SurrenderRequestCheck(
                "%s - %s %d"
                    .formatted(
                        previousCheck.getTitle(), forAmendment ? "AREQ" : "SREQ", expectedStatus),
                previousCheck,
                forAmendment,
                expectedStatus));
  }

  private static EblSurrenderV10ScenarioListBuilder acceptSurrenderRequest(int status) {
    log.info("EblSurrenderV10ScenarioListBuilder.acceptSurrenderRequest(%d)".formatted(status));
    return _surrenderResponseBuilder(true, status);
  }

  private static EblSurrenderV10ScenarioListBuilder rejectSurrenderRequest(int status) {
    log.info("EblSurrenderV10ScenarioListBuilder.rejectSurrenderRequest(%d)".formatted(status));
    return _surrenderResponseBuilder(false, status);
  }

  private static EblSurrenderV10ScenarioListBuilder _surrenderResponseBuilder(
      boolean accept, int expectedStatus) {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblSurrenderV10ScenarioListBuilder(
        previousAction ->
            new SurrenderResponseAction(
                accept, carrierPartyName, platformPartyName, expectedStatus, previousAction),
        previousCheck ->
            new SurrenderResponseCheck(
                "%s - %s %d"
                    .formatted(previousCheck.getTitle(), accept ? "SURR" : "SREJ", expectedStatus),
                previousCheck,
                accept,
                expectedStatus));
  }

  private static EblSurrenderV10ScenarioListBuilder voidAndReissue() {
    log.info("EblSurrenderV10ScenarioListBuilder.voidAndReissue()");
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblSurrenderV10ScenarioListBuilder(
        previousAction ->
            new VoidAndReissueAction(carrierPartyName, platformPartyName, previousAction),
        previousCheck ->
            new VoidAndReissueCheck(
                "%s - Void & Reissue".formatted(previousCheck.getTitle()), previousCheck));
  }
}
