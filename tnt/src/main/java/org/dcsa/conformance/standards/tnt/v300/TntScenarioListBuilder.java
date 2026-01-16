package org.dcsa.conformance.standards.tnt.v300;

import java.util.LinkedHashMap;
import java.util.function.UnaryOperator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;

public class TntScenarioListBuilder extends ScenarioListBuilder<TntScenarioListBuilder> {

  public TntScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static LinkedHashMap<String, TntScenarioListBuilder> createModuleScenarioListBuilders(
      TntComponentFactory componentFactory, String producerPartyName, String consumerPartyName) {
    return new LinkedHashMap<>();
  }
}
