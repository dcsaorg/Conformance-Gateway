package org.dcsa.conformance.standards.tnt.v300;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.tnt.v300.action.ConsumerGetEventsWithQueryParametersAction;
import org.dcsa.conformance.standards.tnt.v300.action.ConsumerGetEventsWithTypeAction;
import org.dcsa.conformance.standards.tnt.v300.action.ProducerPostEventsAction;
import org.dcsa.conformance.standards.tnt.v300.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.tnt.v300.action.TntAction;
import org.dcsa.conformance.standards.tnt.v300.action.TntEventType;
import org.dcsa.conformance.standards.tnt.v300.checks.TntQueryParameters;

public class TntScenarioListBuilder extends ScenarioListBuilder<TntScenarioListBuilder> {

  private static final ThreadLocal<TntComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalProducerPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalConsumerPartyName = new ThreadLocal<>();

  public TntScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, TntScenarioListBuilder> createModuleScenarioListBuilders(
      TntComponentFactory componentFactory, String producerPartyName, String consumerPartyName) {

    threadLocalComponentFactory.set(componentFactory);
    threadLocalProducerPartyName.set(producerPartyName);
    threadLocalConsumerPartyName.set(consumerPartyName);

    return Stream.of(
            Map.entry(
                "POST /events - Publish events for each event type",
                noAction()
                    .thenEither(
                        postTntEvents(TntEventType.SHIPMENT),
                        postTntEvents(TntEventType.TRANSPORT),
                        postTntEvents(TntEventType.EQUIPMENT),
                        postTntEvents(TntEventType.IOT),
                        postTntEvents(TntEventType.REEFER))),
            Map.entry(
                "GET /events - Retrieve events by event type",
                noAction()
                    .thenEither(
                        getTntEvents(TntEventType.SHIPMENT),
                        getTntEvents(TntEventType.TRANSPORT),
                        getTntEvents(TntEventType.EQUIPMENT),
                        getTntEvents(TntEventType.IOT),
                        getTntEvents(TntEventType.REEFER))),
            Map.entry(
                "GET /events - Filter events using primary query parameters",
                noAction()
                    .thenEither(
                        supplyScenarioParameters(TntQueryParameters.CBR).then(getTntEvents()),
                        supplyScenarioParameters(TntQueryParameters.CBR, TntQueryParameters.ER)
                            .then(getTntEvents()),
                        supplyScenarioParameters(TntQueryParameters.TDR).then(getTntEvents()),
                        supplyScenarioParameters(TntQueryParameters.TDR, TntQueryParameters.ER)
                            .then(getTntEvents()),
                        supplyScenarioParameters(TntQueryParameters.ER).then(getTntEvents()))),
            Map.entry(
                "GET /events - Filter events using additional query parameters",
                noAction()
                    .thenEither(
                        supplyScenarioParameters(TntQueryParameters.CBR, TntQueryParameters.ET)
                            .then(getTntEvents()),
                        supplyScenarioParameters(
                                TntQueryParameters.CBR, TntQueryParameters.E_UDT_MIN)
                            .then(getTntEvents()),
                        supplyScenarioParameters(
                                TntQueryParameters.CBR, TntQueryParameters.E_UDT_MAX)
                            .then(getTntEvents()),
                        supplyScenarioParameters(
                                TntQueryParameters.CBR,
                                TntQueryParameters.E_UDT_MIN,
                                TntQueryParameters.E_UDT_MAX)
                            .then(getTntEvents()),
                        supplyScenarioParameters(
                                TntQueryParameters.CBR,
                                TntQueryParameters.ET,
                                TntQueryParameters.E_UDT_MIN,
                                TntQueryParameters.E_UDT_MAX)
                            .then(getTntEvents()))),
            Map.entry(
                "GET /events - Validate pagination with cursor-based navigation",
                noAction()
                    .thenEither(
                        supplyScenarioParameters(TntQueryParameters.CBR, TntQueryParameters.LIMIT)
                            .then(getTntEvents(true).then(getTntEvents())))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, _) -> e1, LinkedHashMap::new));
  }

  private static TntScenarioListBuilder noAction() {
    return new TntScenarioListBuilder(null);
  }

  private static TntScenarioListBuilder supplyScenarioParameters(
      TntQueryParameters... queryParameters) {
    String producerPartyName = threadLocalProducerPartyName.get();
    return new TntScenarioListBuilder(
        _ -> new SupplyScenarioParametersAction(producerPartyName, queryParameters));
  }

  private static TntScenarioListBuilder getTntEvents(TntEventType eventType) {
    TntComponentFactory componentFactory = threadLocalComponentFactory.get();
    String producerPartyName = threadLocalProducerPartyName.get();
    String consumerPartyName = threadLocalConsumerPartyName.get();
    return new TntScenarioListBuilder(
        previousAction ->
            new ConsumerGetEventsWithTypeAction(
                consumerPartyName,
                producerPartyName,
                (TntAction) previousAction,
                eventType,
                componentFactory.getMessageSchemaValidator("GetEventsResponse")));
  }

  private static TntScenarioListBuilder getTntEvents() {
    return getTntEvents(false);
  }

  private static TntScenarioListBuilder getTntEvents(boolean hasNextPage) {
    TntComponentFactory componentFactory = threadLocalComponentFactory.get();
    String producerPartyName = threadLocalProducerPartyName.get();
    String consumerPartyName = threadLocalConsumerPartyName.get();
    return new TntScenarioListBuilder(
        previousAction ->
            new ConsumerGetEventsWithQueryParametersAction(
                consumerPartyName,
                producerPartyName,
                (TntAction) previousAction,
                hasNextPage,
                componentFactory.getMessageSchemaValidator("GetEventsResponse")));
  }

  private static TntScenarioListBuilder postTntEvents(TntEventType eventType) {
    TntComponentFactory componentFactory = threadLocalComponentFactory.get();
    String producerPartyName = threadLocalProducerPartyName.get();
    String consumerPartyName = threadLocalConsumerPartyName.get();
    return new TntScenarioListBuilder(
        previousAction ->
            new ProducerPostEventsAction(
                producerPartyName,
                consumerPartyName,
                (TntAction) previousAction,
                eventType,
                componentFactory.getMessageSchemaValidator("PostEventsRequest"),
                componentFactory.getMessageSchemaValidator("PostEventsResponse")));
  }
}
