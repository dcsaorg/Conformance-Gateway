package org.dcsa.conformance.standards.portcall;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.portcall.action.PortCallAction;
import org.dcsa.conformance.standards.portcall.action.PublisherPostPortCallEventsAction;
import org.dcsa.conformance.standards.portcall.action.SubscriberGetPortCallEventsAction;
import org.dcsa.conformance.standards.portcall.action.SupplyScenarioParametersAction;

public class PortCallScenarioListBuilder extends ScenarioListBuilder<PortCallScenarioListBuilder> {

  private static final ThreadLocal<PortCallComponentFactory> threadLocalComponentFactory =
    new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  private PortCallScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, PortCallScenarioListBuilder> createModuleScenarioListBuilders(PortCallComponentFactory portCallComponentFactory, String publisherPartyName,
                                                                                          String subscriberPartyName) {
    threadLocalComponentFactory.set(portCallComponentFactory);
    threadLocalPublisherPartyName.set(publisherPartyName);
    threadLocalSubscriberPartyName.set(subscriberPartyName);

    return Stream.of(
        Map.entry(
          "POST-only scenarios (for adopters only supporting POST)",
          noAction()
            .then(
              postPortCallEvents())),
        Map.entry(
          "GET-only scenarios (for adopters only supporting GET)",
          noAction()
            .then(
              supplyScenarioParameters().then(getPortCallEvents()))))
      .collect(
        Collectors.toMap(
          Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));


  }

  private static PortCallScenarioListBuilder supplyScenarioParameters() {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new PortCallScenarioListBuilder(
      previousAction -> new SupplyScenarioParametersAction(publisherPartyName));
  }

  private static PortCallScenarioListBuilder postPortCallEvents() {
    PortCallComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new PortCallScenarioListBuilder(
        previousAction ->
            new PublisherPostPortCallEventsAction(
                publisherPartyName,
                subscriberPartyName,
                (PortCallAction) previousAction,
                componentFactory.getMessageSchemaValidator("PostEventsRequest")));
  }

  private static PortCallScenarioListBuilder getPortCallEvents() {
    PortCallComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new PortCallScenarioListBuilder(
      previousAction ->
        new SubscriberGetPortCallEventsAction(
          subscriberPartyName,
          publisherPartyName,
          (PortCallAction) previousAction,
          componentFactory.getMessageSchemaValidator("GetEventsResponse")));
  }



  private static PortCallScenarioListBuilder noAction() {
    return new PortCallScenarioListBuilder(null);
  }
}
