package org.dcsa.conformance.standards.eblissuance;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.util.MapUtils;
import org.dcsa.conformance.standards.eblissuance.action.CarrierScenarioParametersAction;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceAction;
import org.dcsa.conformance.standards.eblissuance.action.IssuanceRequestResponseAction;
import org.dcsa.conformance.standards.eblissuance.action.PlatformScenarioParametersAction;
import org.dcsa.conformance.standards.eblissuance.party.EblIssuanceRole;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

class EblIssuanceScenarioListBuilder extends ScenarioListBuilder<EblIssuanceScenarioListBuilder> {

  private static final ThreadLocal<EblIssuanceComponentFactory> threadLocalComponentFactory = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPlatformPartyName = new ThreadLocal<>();

  private EblIssuanceScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, EblIssuanceScenarioListBuilder> createModuleScenarioListBuilders(EblIssuanceComponentFactory componentFactory, Set<String> testedPartyRoleNames, String carrierPartyName, String platformPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalCarrierPartyName.set(carrierPartyName);
    threadLocalPlatformPartyName.set(platformPartyName);

    boolean testsBothRoles = testedPartyRoleNames.containsAll(
      Set.of(EblIssuanceRole.CARRIER.getConfigName(), EblIssuanceRole.PLATFORM.getConfigName()));

    Map<String, Map<String, EblIssuanceScenarioListBuilder>> partyScenariosMap =
      MapUtils.orderedMap(
        Map.entry(
          EblIssuanceRole.CARRIER.getConfigName(),
          MapUtils.orderedMap(
            Map.entry(
              testsBothRoles ? "Carrier required scenario" : "Required scenario",
              carrierScenarioParameters().then(issuanceRequestResponse())))),
        Map.entry(
          EblIssuanceRole.PLATFORM.getConfigName(),
          MapUtils.orderedMap(
            Map.entry(
              testsBothRoles ? "eBL Platform required scenario" : "Required scenario",
              platformScenarioParameters().then(issuanceRequestResponse())))));

    Map<String, EblIssuanceScenarioListBuilder> scenarios = new LinkedHashMap<>();
    testedPartyRoleNames.forEach(party -> scenarios.putAll(partyScenariosMap.get(party)));

    return scenarios;
  }

  private static EblIssuanceScenarioListBuilder platformScenarioParameters() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblIssuanceScenarioListBuilder(
      previousAction ->
        new PlatformScenarioParametersAction(
          platformPartyName,
          carrierPartyName,
          (IssuanceAction) previousAction));
  }

  private static EblIssuanceScenarioListBuilder carrierScenarioParameters() {
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblIssuanceScenarioListBuilder(
      previousAction ->
        new CarrierScenarioParametersAction(
          carrierPartyName, platformPartyName, (IssuanceAction) previousAction));
  }

  private static EblIssuanceScenarioListBuilder issuanceRequestResponse() {
    EblIssuanceComponentFactory componentFactory = threadLocalComponentFactory.get();
    String carrierPartyName = threadLocalCarrierPartyName.get();
    String platformPartyName = threadLocalPlatformPartyName.get();
    return new EblIssuanceScenarioListBuilder(
      previousAction ->
        new IssuanceRequestResponseAction(
          platformPartyName,
          carrierPartyName,
          (IssuanceAction) previousAction,
          componentFactory.getMessageSchemaValidator(
            EblIssuanceRole.PLATFORM.getConfigName(), true, false),
          componentFactory.getMessageSchemaValidator(
            EblIssuanceRole.CARRIER.getConfigName(), true, false),
          componentFactory.getMessageSchemaValidator(
            EblIssuanceRole.CARRIER.getConfigName(), true, true)));
  }

}
