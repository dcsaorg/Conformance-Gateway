package org.dcsa.conformance.standards.adoption;

import static org.dcsa.conformance.standards.adoption.party.FilterParameter.*;

import java.util.LinkedHashMap;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.check.JsonSchemaValidator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.adoption.action.GetAdoptionStatsAction;
import org.dcsa.conformance.standards.adoption.action.PutAdoptionStatsAction;
import org.dcsa.conformance.standards.adoption.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.adoption.party.FilterParameter;

@Slf4j
class AdoptionScenarioListBuilder extends ScenarioListBuilder<AdoptionScenarioListBuilder> {

  private record ScenarioContext(
      String adopterPartyName, String dcsaPartyName, JsonSchemaValidator validator) {}

  public static LinkedHashMap<String, AdoptionScenarioListBuilder> createModuleScenarioListBuilders(
      AdoptionComponentFactory componentFactory, String adopterPartyName, String dcsaPartyName) {
    ScenarioContext context =
        new ScenarioContext(
            adopterPartyName, dcsaPartyName, componentFactory.getMessageSchemaValidator());
    var scenarioList = new LinkedHashMap<String, AdoptionScenarioListBuilder>();
    scenarioList.put(
        "Get statistics",
        noAction()
            .thenEither(
                supplyScenarioParameters(context, INTERVAL, DATE)
                    .thenEither(getAdoptionStatsRouting(context))));
    scenarioList.put("Put statistics", noAction().thenEither(putAdoptionStatsRouting(context)));

    return scenarioList;
  }

  private AdoptionScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static AdoptionScenarioListBuilder noAction() {
    return new AdoptionScenarioListBuilder(null);
  }

  private static AdoptionScenarioListBuilder supplyScenarioParameters(
      ScenarioContext context, FilterParameter... filterParameters) {
    return new AdoptionScenarioListBuilder(
        previousAction ->
            new SupplyScenarioParametersAction(context.adopterPartyName, filterParameters));
  }

  private static AdoptionScenarioListBuilder getAdoptionStatsRouting(ScenarioContext context) {
    return new AdoptionScenarioListBuilder(
        previousAction ->
            new GetAdoptionStatsAction(
                context.dcsaPartyName,
                context.adopterPartyName,
                previousAction,
                context.validator()));
  }

  private static AdoptionScenarioListBuilder putAdoptionStatsRouting(ScenarioContext context) {
    return new AdoptionScenarioListBuilder(
        previousAction ->
            new PutAdoptionStatsAction(
                context.adopterPartyName,
                context.dcsaPartyName,
                previousAction,
                context.validator()));
  }
}
