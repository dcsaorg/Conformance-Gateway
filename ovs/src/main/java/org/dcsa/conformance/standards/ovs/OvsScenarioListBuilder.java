package org.dcsa.conformance.standards.ovs;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.ovs.action.OvsAction;
import org.dcsa.conformance.standards.ovs.action.OvsGetSchedulesAction;
import org.dcsa.conformance.standards.ovs.party.OvsRole;

@Slf4j
public class OvsScenarioListBuilder extends ScenarioListBuilder<OvsScenarioListBuilder> {
  private static final ThreadLocal<OvsComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  public static OvsScenarioListBuilder buildTree(
      OvsComponentFactory componentFactory, String publisherPartyName, String subscriberPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalPublisherPartyName.set(publisherPartyName);
    threadLocalSubscriberPartyName.set(subscriberPartyName);
    return noAction().then(getEventsRequest());
  }

  private OvsScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static OvsScenarioListBuilder noAction() {
    return new OvsScenarioListBuilder(null);
  }

  private static OvsScenarioListBuilder getEventsRequest() {
    OvsComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new OvsScenarioListBuilder(
        previousAction ->
            new OvsGetSchedulesAction(
                subscriberPartyName,
                publisherPartyName,
                (OvsAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    OvsRole.PUBLISHER.getConfigName(), false)));
  }
}
