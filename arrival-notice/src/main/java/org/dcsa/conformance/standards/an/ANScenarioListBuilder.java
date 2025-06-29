package org.dcsa.conformance.standards.an;

import java.util.Map;
import java.util.function.UnaryOperator;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;

public class ANScenarioListBuilder extends ScenarioListBuilder<ANScenarioListBuilder> {

  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  private static final ThreadLocal<ANComponentFactory> threadLocalComponentFactory =
    new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalCarrierPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalShipperPartyName = new ThreadLocal<>();


  private ANScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static <T extends ScenarioListBuilder<T>> Map<String,T> createModuleScenarioListBuilders(ANComponentFactory anComponentFactory, String s, String s1) {
    return null;
  }
}
