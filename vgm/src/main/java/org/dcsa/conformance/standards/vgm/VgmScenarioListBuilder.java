package org.dcsa.conformance.standards.vgm;

import java.util.Map;
import java.util.function.UnaryOperator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;

public class VgmScenarioListBuilder extends ScenarioListBuilder<VgmScenarioListBuilder> {

  protected VgmScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, VgmScenarioListBuilder> createModuleScenarioListBuilders(
      VgmComponentFactory anComponentFactory,
      String publisherPartyName,
      String subscriberPartyName) {
    return Map.of();
  }
}
