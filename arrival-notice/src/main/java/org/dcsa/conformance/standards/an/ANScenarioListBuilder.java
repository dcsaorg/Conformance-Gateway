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
import org.dcsa.conformance.standards.an.action.SupplyScenarioParametersAction;
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
                "POST+GET scenarios (for adopters supporting GET and POST)",
                noAction()
                    .thenEither(
                        postArrivalNotices(ScenarioType.REGULAR).then(getArrivalNotices()),
                        postArrivalNotices(ScenarioType.FREIGHTED).then(getArrivalNotices()),
                        postArrivalNotices(ScenarioType.FREE_TIME).then(getArrivalNotices()),
                        postArrivalNoticesNotification().then(getArrivalNotices()))),
            Map.entry(
                "GET-only scenarios (for adopters only supporting GET)",
                noAction()
                    .thenEither(
                        supplyScenarioParameters(ScenarioType.REGULAR).then(getArrivalNotices()),
                        supplyScenarioParameters(ScenarioType.FREIGHTED).then(getArrivalNotices()),
                        supplyScenarioParameters(ScenarioType.FREE_TIME)
                            .then(getArrivalNotices()))),
            Map.entry(
                "POST-only scenarios (for adopters only supporting POST)",
                noAction()
                    .thenEither(
                        postArrivalNotices(ScenarioType.REGULAR),
                        postArrivalNotices(ScenarioType.FREIGHTED),
                        postArrivalNotices(ScenarioType.FREE_TIME),
                        postArrivalNoticesNotification())))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static ANScenarioListBuilder supplyScenarioParameters(ScenarioType scenarioType) {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new ANScenarioListBuilder(
        previousAction -> new SupplyScenarioParametersAction(publisherPartyName, scenarioType));
  }

  private static ANScenarioListBuilder postArrivalNotices(ScenarioType scenarioType) {
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

  private static ANScenarioListBuilder postArrivalNoticesNotification() {
    ANComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new ANScenarioListBuilder(
        previousAction ->
            new PublisherPostANNotificationAction(
                publisherPartyName,
                subscriberPartyName,
                (ANAction) previousAction,
                componentFactory.getMessageSchemaValidator(
                    "PostArrivalNoticeNotificationsRequest")));
  }

  private static ANScenarioListBuilder noAction() {
    return new ANScenarioListBuilder(null);
  }
}
