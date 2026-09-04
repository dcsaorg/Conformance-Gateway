package org.dcsa.conformance.standards.tnt.v300;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.util.MapUtils;
import org.dcsa.conformance.standards.tnt.v300.action.ConsumerGetEventsWithQueryParametersAction;
import org.dcsa.conformance.standards.tnt.v300.action.ConsumerGetEventsWithTypeAction;
import org.dcsa.conformance.standards.tnt.v300.action.ProducerPostEventsAction;
import org.dcsa.conformance.standards.tnt.v300.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.tnt.v300.action.TntAction;
import org.dcsa.conformance.standards.tnt.v300.action.TntEventType;
import org.dcsa.conformance.standards.tnt.v300.party.TntQueryParameters;
import org.dcsa.conformance.standards.tnt.v300.party.TntRole;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class TntScenarioListBuilder extends ScenarioListBuilder<TntScenarioListBuilder> {

  private static final ThreadLocal<TntComponentFactory> threadLocalComponentFactory =
    new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalProducerPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalConsumerPartyName = new ThreadLocal<>();

  public TntScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, TntScenarioListBuilder> createModuleScenarioListBuilders(
    TntComponentFactory componentFactory,
    Set<String> testedPartyRoleNames,
    String producerPartyName,
    String consumerPartyName) {

    threadLocalComponentFactory.set(componentFactory);
    threadLocalProducerPartyName.set(producerPartyName);
    threadLocalConsumerPartyName.set(consumerPartyName);

    Supplier<Map.Entry<String, TntScenarioListBuilder>> postScenarioEntry = () ->
      Map.entry(
        "POST scenarios per event type - alternative required path for event push",
        noAction()
          .thenEither(
            postTntEvents(TntEventType.SHIPMENT),
            postTntEvents(TntEventType.TRANSPORT),
            postTntEvents(TntEventType.EQUIPMENT),
            postTntEvents(TntEventType.IOT),
            postTntEvents(TntEventType.REEFER))
          .asInterchangeableScenarios());

    Map.Entry<String, TntScenarioListBuilder> producerGetByTypeScenarioEntry = Map.entry(
      "GET scenarios per event type - alternative required path for event pull",
      noAction()
        .thenEither(
          getTntEventsByTypeWithOptionalBaseFilters(TntEventType.SHIPMENT),
          getTntEventsByTypeWithOptionalBaseFilters(TntEventType.TRANSPORT),
          getTntEventsByTypeWithOptionalBaseFilters(TntEventType.EQUIPMENT),
          getTntEventsByTypeWithOptionalBaseFilters(TntEventType.IOT),
          getTntEventsByTypeWithOptionalBaseFilters(TntEventType.REEFER))
        .asInterchangeableScenarios());

    Map.Entry<String, TntScenarioListBuilder> consumerGetByTypeScenarioEntry = Map.entry(
      "GET scenarios per event type - alternative required path for event pull",
      noAction()
        .thenEither(
          getTntEvents(TntEventType.SHIPMENT),
          getTntEvents(TntEventType.TRANSPORT),
          getTntEvents(TntEventType.EQUIPMENT),
          getTntEvents(TntEventType.IOT),
          getTntEvents(TntEventType.REEFER))
        .asInterchangeableScenarios());

    Map<String, Map<String, TntScenarioListBuilder>> partyScenariosMap = MapUtils.orderedMap(
      Map.entry(
        TntRole.PRODUCER.getConfigName(),
        MapUtils.orderedMap(
          postScenarioEntry.get(),
          producerGetByTypeScenarioEntry,
          Map.entry("GET scenarios for required query parameter filters - required once per GET endpoint implementation",
            noAction()
              .thenEither(
                supplyScenarioParameters(TntQueryParameters.CBR).then(getTntEvents()),
                supplyScenarioParameters(TntQueryParameters.CBR, TntQueryParameters.ER).then(getTntEvents()),
                supplyScenarioParameters(TntQueryParameters.TDR).then(getTntEvents()),
                supplyScenarioParameters(TntQueryParameters.TDR, TntQueryParameters.ER).then(getTntEvents()),
                supplyScenarioParameters(TntQueryParameters.ER).then(getTntEvents()),
                supplyScenarioParameters(TntQueryParameters.CBR, TntQueryParameters.ET, TntQueryParameters.E_UDT_MIN, TntQueryParameters.E_UDT_MAX).then(getTntEvents()))),
          Map.entry("GET scenario for pagination - optional/report-only",
            noAction()
              .thenEither(
                supplyScenarioParameters(TntQueryParameters.CBR, TntQueryParameters.LIMIT)
                  .then(getTntEvents(true)
                    .then(getTntEvents())))
              .asOptionalReportOnlyScenario()))),
      Map.entry(
        TntRole.CONSUMER.getConfigName(),
        MapUtils.orderedMap(postScenarioEntry.get(), consumerGetByTypeScenarioEntry)));

    if (testedPartyRoleNames.size() > 1) {
      testedPartyRoleNames.forEach(role ->
        partyScenariosMap.getOrDefault(role, Map.of()).values()
          .forEach(builder -> builder.withScenarioTitlePrefix(role + ": ")));
    }
    return MapUtils.mergePartyScenarioModules(partyScenariosMap, testedPartyRoleNames);
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

  private static TntScenarioListBuilder supplyOptionalScenarioParameters(Collection<TntQueryParameters> queryParameters) {
    String producerPartyName = threadLocalProducerPartyName.get();
    return new TntScenarioListBuilder(
      _ -> SupplyScenarioParametersAction.optional(producerPartyName, queryParameters));
  }

  private static TntScenarioListBuilder getTntEventsByTypeWithOptionalBaseFilters(TntEventType eventType) {
    return supplyOptionalScenarioParameters(eventType.applicableBaseFilters())
      .then(getTntEvents(eventType));
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
