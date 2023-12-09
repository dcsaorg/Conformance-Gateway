package org.dcsa.conformance.standards.tnt;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.tnt.action.TntAction;
import org.dcsa.conformance.standards.tnt.action.TntGetEventsAction;

@Slf4j
public class TntScenarioListBuilder extends ScenarioListBuilder<TntScenarioListBuilder> {
  private static final ThreadLocal<TntComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  public static TntScenarioListBuilder buildTree(
      TntComponentFactory componentFactory, String publisherPartyName, String subscriberPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalPublisherPartyName.set(publisherPartyName);
    threadLocalSubscriberPartyName.set(subscriberPartyName);
    return noAction().then(getEventsRequest());
  }

  private TntScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static TntScenarioListBuilder noAction() {
    return new TntScenarioListBuilder(null);
  }

  private static TntScenarioListBuilder getEventsRequest() {
    TntComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new TntScenarioListBuilder(
        previousAction ->
            new TntGetEventsAction(
                subscriberPartyName,
                publisherPartyName,
                (TntAction) previousAction,
                componentFactory.getEventSchemaValidators()));
  }
}
