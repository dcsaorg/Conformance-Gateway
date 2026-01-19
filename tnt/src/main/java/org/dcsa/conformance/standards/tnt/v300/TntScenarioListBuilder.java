package org.dcsa.conformance.standards.tnt.v300;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.tnt.v300.action.ProducerPostTntAction;
import org.dcsa.conformance.standards.tnt.v300.action.TntAction;
import org.dcsa.conformance.standards.tnt.v300.action.TntEventType;

public class TntScenarioListBuilder extends ScenarioListBuilder<TntScenarioListBuilder> {

  private static final ThreadLocal<TntComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalProducerPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalConsumerPartyName = new ThreadLocal<>();

  public TntScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static LinkedHashMap<String, TntScenarioListBuilder> createModuleScenarioListBuilders(
      TntComponentFactory componentFactory, String producerPartyName, String consumerPartyName) {

    threadLocalComponentFactory.set(componentFactory);
    threadLocalProducerPartyName.set(producerPartyName);
    threadLocalConsumerPartyName.set(consumerPartyName);

    return Stream.of(
            Map.entry(
                "POST T&T Events",
                noAction()
                    .thenEither(
                        postTntEvent(TntEventType.SHIPMENT),
                        postTntEvent(TntEventType.TRANSPORT),
                        postTntEvent(TntEventType.EQUIPMENT),
                        postTntEvent(TntEventType.IOT),
                        postTntEvent(TntEventType.REEFER))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, _) -> e1, LinkedHashMap::new));
  }

  private static TntScenarioListBuilder noAction() {
    return new TntScenarioListBuilder(null);
  }

  private static TntScenarioListBuilder postTntEvent(TntEventType eventType) {
    TntComponentFactory componentFactory = threadLocalComponentFactory.get();
    String producerPartyName = threadLocalProducerPartyName.get();
    String consumerPartyName = threadLocalConsumerPartyName.get();
    return new TntScenarioListBuilder(
        previousAction ->
            new ProducerPostTntAction(
                producerPartyName,
                consumerPartyName,
                (TntAction) previousAction,
                eventType,
                componentFactory.getMessageSchemaValidator("PostEventsRequest")));
  }
}
