package org.dcsa.conformance.standards.eblsurrender;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.util.MapUtils;
import org.dcsa.conformance.standards.eblsurrender.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.eblsurrender.action.SurrenderRequestResponseAction;
import org.dcsa.conformance.standards.eblsurrender.party.EblSurrenderRole;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

class EblSurrenderScenarioListBuilder extends ScenarioListBuilder<EblSurrenderScenarioListBuilder> {
  private static final ThreadLocal<EblSurrenderComponentFactory> threadLocalComponentFactory =
    new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPlatformPartyName = new ThreadLocal<>();

  private EblSurrenderScenarioListBuilder(
    Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, EblSurrenderScenarioListBuilder> createModuleScenarioListBuilders(
    EblSurrenderComponentFactory componentFactory,
    Set<String> testedPartyRoleNames,
    String carrierPartyName,
    String platformPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalPlatformPartyName.set(platformPartyName);

    Map<String, Map<String, EblSurrenderScenarioListBuilder>> partyScenariosMap = MapUtils.orderedMap(
      Map.entry(
        EblSurrenderRole.CARRIER.getConfigName(),
        MapUtils.orderedMap(
          Map.entry("Required scenario", supplyAvailableTdrAction().then(requestSurrender(false))),
          Map.entry("Optional (report-only) scenarios", supplyAvailableTdrAction().then(requestSurrender(true)).asOptionalReportOnlyScenario()))),
      Map.entry(
        EblSurrenderRole.PLATFORM.getConfigName(),
        MapUtils.orderedMap(
          Map.entry("Required scenario", requestSurrender(false)),
          Map.entry("Optional (report-only) scenarios", requestSurrender(true).asOptionalReportOnlyScenario())))
    );

    Map<String, EblSurrenderScenarioListBuilder> scenarios = new LinkedHashMap<>();
    testedPartyRoleNames.forEach(party -> scenarios.putAll(partyScenariosMap.get(party)));

    return scenarios;
  }

  private static EblSurrenderScenarioListBuilder supplyAvailableTdrAction() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    return new EblSurrenderScenarioListBuilder(
      previousAction -> new SupplyScenarioParametersAction(carrierPartyName, previousAction));
  }

  private static EblSurrenderScenarioListBuilder requestSurrender(boolean forAmendment) {
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
            EblSurrenderRole.PLATFORM.getConfigName(), true)));
  }
}
