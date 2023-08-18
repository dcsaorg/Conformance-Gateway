package org.dcsa.conformance.gateway.standards.eblsurrender.v10.parties;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;
import org.dcsa.conformance.gateway.scenarios.ConformanceAction;
import org.dcsa.conformance.gateway.scenarios.ScenarioListBuilder;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SupplyAvailableTdrAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SurrenderRequestAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.SurrenderResponseAction;
import org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios.VoidAndReissueAction;

public class EblSurrenderV10ScenarioListBuilder extends ScenarioListBuilder {

  public EblSurrenderV10ScenarioListBuilder(String carrierPartyName, String platformPartyName) {
    this(
        null,
        carrierPartyName,
        platformPartyName,
        (noPreviousActions) -> new SupplyAvailableTdrAction(carrierPartyName));
  }

  @Override
  protected ScenarioListBuilder buildTree() {
    this.thenEither(
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
                        requestSurrenderForDelivery(202).acceptSurrenderRequest(204),
                        requestSurrenderForAmendment(202).acceptSurrenderRequest(204)),
                requestSurrenderForDelivery(409).acceptSurrenderRequest(204),
                requestSurrenderForAmendment(409).acceptSurrenderRequest(204)),
        requestSurrenderForAmendment(202)
            .thenEither(
                acceptSurrenderRequest(204)
                    .thenEither(
                        requestSurrenderForDelivery(409),
                        requestSurrenderForAmendment(409),
                        acceptSurrenderRequest(409),
                        rejectSurrenderRequest(409),
                        voidAndReissue()
                            .requestSurrenderForDelivery(202)
                            .acceptSurrenderRequest(204)),
                rejectSurrenderRequest(204)
                    .thenEither(
                        requestSurrenderForDelivery(202).acceptSurrenderRequest(204),
                        requestSurrenderForAmendment(202).acceptSurrenderRequest(204)),
                requestSurrenderForAmendment(409).acceptSurrenderRequest(204),
                requestSurrenderForDelivery(409).acceptSurrenderRequest(204)),
        acceptSurrenderRequest(409)
            .thenEither(
                requestSurrenderForDelivery(202).acceptSurrenderRequest(204),
                requestSurrenderForAmendment(202).acceptSurrenderRequest(204)),
        rejectSurrenderRequest(409)
            .thenEither(
                requestSurrenderForDelivery(202).acceptSurrenderRequest(204),
                requestSurrenderForAmendment(202).acceptSurrenderRequest(204)));
    return this;
  }

  private final String carrierPartyName;
  private final String platformPartyName;

  private EblSurrenderV10ScenarioListBuilder(
      EblSurrenderV10ScenarioListBuilder parent,
      String carrierPartyName,
      String platformPartyName,
      Function<LinkedList<ConformanceAction>, ConformanceAction> actionBuilder) {
    super(parent, actionBuilder);
    this.carrierPartyName = carrierPartyName;
    this.platformPartyName = platformPartyName;
  }

  private EblSurrenderV10ScenarioListBuilder requestSurrenderForAmendment(int status) {
    return _surrenderRequestBuilder(true, status);
  }

  private EblSurrenderV10ScenarioListBuilder requestSurrenderForDelivery(int status) {
    return _surrenderRequestBuilder(false, status);
  }

  private EblSurrenderV10ScenarioListBuilder _surrenderRequestBuilder(
      boolean forAmendment, int expectedStatus) {
    return addChildIfFirst(
        new EblSurrenderV10ScenarioListBuilder(
            this,
            carrierPartyName,
            platformPartyName,
            (previousActions) ->
                new SurrenderRequestAction(
                    forAmendment,
                    _getTdrSupplier(previousActions),
                    platformPartyName,
                    carrierPartyName,
                    expectedStatus)));
  }

  private EblSurrenderV10ScenarioListBuilder acceptSurrenderRequest(int status) {
    return _surrenderResponseBuilder(true, status);
  }

  private EblSurrenderV10ScenarioListBuilder rejectSurrenderRequest(int status) {
    return _surrenderResponseBuilder(false, status);
  }

  private EblSurrenderV10ScenarioListBuilder _surrenderResponseBuilder(
      boolean accept, int expectedStatus) {
    return addChildIfFirst(
        new EblSurrenderV10ScenarioListBuilder(
            this,
            carrierPartyName,
            platformPartyName,
            (previousActions) ->
                new SurrenderResponseAction(
                    accept,
                    _getLatestSrrSupplier(previousActions),
                    _getTdrSupplier(previousActions),
                    carrierPartyName,
                    platformPartyName,
                    expectedStatus)));
  }

  private Supplier<String> _getTdrSupplier(List<ConformanceAction> previousActions) {
    return ((SupplyAvailableTdrAction) previousActions.get(0)).getTdrSupplier();
  }

  private Supplier<String> _getLatestSrrSupplier(LinkedList<ConformanceAction> previousActions) {
    return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                previousActions.descendingIterator(), Spliterator.ORDERED),
            false)
        .filter(action -> action instanceof SurrenderRequestAction)
        .map(action -> (SurrenderRequestAction) action)
        .findFirst()
        .orElseThrow()
        .getSrrSupplier();
  }

  private EblSurrenderV10ScenarioListBuilder voidAndReissue() {
    return addChildIfFirst(
        new EblSurrenderV10ScenarioListBuilder(
            this,
            carrierPartyName,
            platformPartyName,
            (previousActions) ->
                new VoidAndReissueAction(
                    _getTdrSupplier(previousActions), carrierPartyName, platformPartyName)));
  }
}
