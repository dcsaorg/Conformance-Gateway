package org.dcsa.conformance.standards.an;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.an.action.ArrivalNoticeGetNotificationAction;
import org.dcsa.conformance.standards.an.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.an.party.ArrivalNoticeFilterParameter;
import org.dcsa.conformance.standards.an.party.ArrivalNoticeRole;

import static org.dcsa.conformance.standards.an.party.ArrivalNoticeFilterParameter.TRANSPORT_DOCUMENT_REFERENCE;

@Slf4j
public class ArrivalNoticeScenarioListBuilder extends ScenarioListBuilder<ArrivalNoticeScenarioListBuilder> {
  private static final ThreadLocal<ArrivalNoticeComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  public static LinkedHashMap<String, ArrivalNoticeScenarioListBuilder> createModuleScenarioListBuilders(
    ArrivalNoticeComponentFactory componentFactory, String publisherPartyName, String subscriberPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalPublisherPartyName.set(publisherPartyName);
    threadLocalSubscriberPartyName.set(subscriberPartyName);
    return Stream.of(
            Map.entry(
                "Arrival Notices",
                noAction()
                    .thenEither(
                        scenarioWithParameters(TRANSPORT_DOCUMENT_REFERENCE))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private ArrivalNoticeScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static ArrivalNoticeScenarioListBuilder noAction() {
    return new ArrivalNoticeScenarioListBuilder(null);
  }

  private static ArrivalNoticeScenarioListBuilder scenarioWithParameters(
    ArrivalNoticeFilterParameter... anFilterParameters) {
    return supplyScenarioParameters(anFilterParameters).then(getArrivalNotice());
  }

  private static ArrivalNoticeScenarioListBuilder supplyScenarioParameters(
    ArrivalNoticeFilterParameter... anFilterParameters) {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new ArrivalNoticeScenarioListBuilder(
        previousAction ->
            new SupplyScenarioParametersAction(publisherPartyName, anFilterParameters));
  }

  private static ArrivalNoticeScenarioListBuilder getArrivalNotice() {
    ArrivalNoticeComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new ArrivalNoticeScenarioListBuilder(
        previousAction ->
            new ArrivalNoticeGetNotificationAction(
                subscriberPartyName,
                publisherPartyName,
                previousAction,
                componentFactory.getMessageSchemaValidator()));
  }
}
