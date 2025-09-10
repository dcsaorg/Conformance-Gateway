package org.dcsa.conformance.standards.bookingandebl;

import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;

@Slf4j
public class BookingAndEblScenarioListBuilder
    extends ScenarioListBuilder<BookingAndEblScenarioListBuilder> {

  public static final String SCENARIO_SUITE_CONFORMANCE = "Conformance";

  protected BookingAndEblScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }
}
