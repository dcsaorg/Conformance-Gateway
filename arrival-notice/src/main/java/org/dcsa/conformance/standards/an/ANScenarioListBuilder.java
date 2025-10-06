package org.dcsa.conformance.standards.an;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.an.action.ANAction;
import org.dcsa.conformance.standards.an.action.PublisherPostANAction;
import org.dcsa.conformance.standards.an.action.PublisherPostANNotificationAction;
import org.dcsa.conformance.standards.an.action.SubscriberGetANAction;
import org.dcsa.conformance.standards.an.checks.ScenarioType;

public class ANScenarioListBuilder extends ScenarioListBuilder<ANScenarioListBuilder> {

  private static final ThreadLocal<ANComponentFactory> threadLocalComponentFactory =
    new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  private ANScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, ANScenarioListBuilder> createModuleScenarioListBuilders(
      ANComponentFactory anComponentFactory,
      String publisherPartyName,
      String subscriberPartyName) {

    threadLocalComponentFactory.set(anComponentFactory);
    threadLocalPublisherPartyName.set(publisherPartyName);
    threadLocalSubscriberPartyName.set(subscriberPartyName);

    return Stream.of(
            Map.entry(
                "",
                noAction()
                    .thenEither(
                        sendArrivalNotices(ScenarioType.REGULAR).then(getArrivalNotices()),
                        sendArrivalNotices(ScenarioType.FREIGHTED).then(getArrivalNotices()),
                        sendArrivalNotices(ScenarioType.FREE_TIME).then(getArrivalNotices()),
                        sendArrivalNoticesNotification().then(getArrivalNotices()))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static ANScenarioListBuilder sendArrivalNotices(ScenarioType scenarioType) {
    ANComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new ANScenarioListBuilder(
        previousAction ->
            new PublisherPostANAction(
                publisherPartyName,
                subscriberPartyName,
                (ANAction) previousAction,
                scenarioType,
                componentFactory.getMessageSchemaValidator("PostArrivalNoticesRequest")));
  }

  private static ANScenarioListBuilder getArrivalNotices() {
    ANComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new ANScenarioListBuilder(
        previousAction ->
            new SubscriberGetANAction(
                subscriberPartyName,
                publisherPartyName,
                (ANAction) previousAction,
                componentFactory.getMessageSchemaValidator("GetArrivalNoticesResponse")));
  }

  private static ANScenarioListBuilder sendArrivalNoticesNotification() {
    ANComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new ANScenarioListBuilder(
        previousAction ->
            new PublisherPostANNotificationAction(
                publisherPartyName,
                subscriberPartyName,
                (ANAction) previousAction,
                componentFactory.getMessageSchemaValidator("PostArrivalNoticeNotificationsRequest")));
  }

  private static ANScenarioListBuilder noAction() {
    return new ANScenarioListBuilder(null);
  }
}
