package org.dcsa.conformance.standards.eblsurrender;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.eblsurrender.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.eblsurrender.action.SurrenderRequestResponseAction;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

@Slf4j
class EblSurrenderScenarioListBuilder extends ScenarioListBuilder<EblSurrenderScenarioListBuilder> {
  private static final ThreadLocal<EblSurrenderComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPlatformPartyName = new ThreadLocal<>();

  private EblSurrenderScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static LinkedHashMap<String, EblSurrenderScenarioListBuilder>
      createModuleScenarioListBuilders(
          EblSurrenderComponentFactory componentFactory,
          String carrierPartyName,
          String platformPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalPlatformPartyName.set(platformPartyName);
    return Stream.of(
            Map.entry(
                "Surrender Accepted",
                noAction()
                    .thenEither(
                        supplyAvailableTdrAction("SURR", "Straight eBL")
                            .thenEither(
                                requestSurrenderForDeliveryAnd(true),
                                requestSurrenderForAmendmentAnd(true, false),
                                requestSurrenderForAmendmentAnd(true, true)),
                        supplyAvailableTdrAction("SURR", "Negotiable eBL")
                            .thenEither(
                                requestSurrenderForDeliveryAnd(true),
                                requestSurrenderForAmendmentAnd(true, false),
                                requestSurrenderForAmendmentAnd(true, true)))),
            Map.entry(
                "Surrender Rejected",
                noAction()
                    .thenEither(
                        supplyAvailableTdrAction("SREJ", "Straight eBL")
                            .thenEither(
                                requestSurrenderForDeliveryAnd(false),
                                requestSurrenderForAmendmentAnd(false, false),
                                requestSurrenderForAmendmentAnd(false, true)),
                        supplyAvailableTdrAction("SREJ", "Negotiable eBL")
                            .thenEither(
                                requestSurrenderForDeliveryAnd(false),
                                requestSurrenderForAmendmentAnd(false, false),
                                requestSurrenderForAmendmentAnd(false, true)))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static EblSurrenderScenarioListBuilder noAction() {
    return new EblSurrenderScenarioListBuilder(null);
  }

  private static EblSurrenderScenarioListBuilder supplyAvailableTdrAction(
      String response, String eblType) {
    log.debug("EblSurrenderScenarioListBuilder.supplyAvailableTdrAction()");
    String carrierPartyName = threadLocalCarrierPartyName.get();
    return new EblSurrenderScenarioListBuilder(
        noPreviousAction ->
            new SupplyScenarioParametersAction(carrierPartyName, null, response, eblType));
  }

  private static EblSurrenderScenarioListBuilder requestSurrenderForAmendmentAnd(
      boolean accept, boolean isSWTP) {
    log.debug("EblSurrenderScenarioListBuilder.requestSurrenderForAmendment");

    String titleSuffix = isSWTP ? "AndSwitchToPaper" : "";
    titleSuffix = accept ? titleSuffix + "Accepted" : titleSuffix + "Rejected";
    return _surrenderRequestBuilder(true, accept, "SurrenderForAmendment" + titleSuffix, isSWTP);
  }

  private static EblSurrenderScenarioListBuilder requestSurrenderForDeliveryAnd(boolean accept) {
    log.debug("EblSurrenderScenarioListBuilder.requestSurrenderForDelivery");
    String titleSuffix = accept ? "Accepted" : "Rejected";
    return _surrenderRequestBuilder(false, accept, "SurrenderForDelivery" + titleSuffix, false);
  }

  private static EblSurrenderScenarioListBuilder _surrenderRequestBuilder(
      boolean forAmendment, boolean accept, String title, boolean isSWTP) {
    EblSurrenderComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblSurrenderScenarioListBuilder(
        previousAction ->
            new SurrenderRequestResponseAction(
                forAmendment,
                platformPartyName,
                carrierPartyName,
                204,
                previousAction,
                componentFactory.getMessageSchemaValidator(
                    EblSurrenderRole.CARRIER.getConfigName(), true),
                componentFactory.getMessageSchemaValidator(
                    EblSurrenderRole.PLATFORM.getConfigName(), true),
                accept,
                title,
                isSWTP));
  }
}
