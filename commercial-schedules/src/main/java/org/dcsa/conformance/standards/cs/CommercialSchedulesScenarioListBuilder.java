package org.dcsa.conformance.standards.cs;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;

import java.util.function.Function;

public class CommercialSchedulesScenarioListBuilder extends ScenarioListBuilder<CommercialSchedulesScenarioListBuilder> {

  private static final ThreadLocal<CommercialSchedulesComponentFactory> threadLocalComponentFactory =
    new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();
  protected CommercialSchedulesScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }
}
