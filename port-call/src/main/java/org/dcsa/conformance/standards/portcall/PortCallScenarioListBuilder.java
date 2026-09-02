package org.dcsa.conformance.standards.portcall;

import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.core.util.MapUtils;
import org.dcsa.conformance.standards.portcall.action.GetPortCallEventsAction;
import org.dcsa.conformance.standards.portcall.action.PortCallAction;
import org.dcsa.conformance.standards.portcall.action.PostPortCallEventsAction;
import org.dcsa.conformance.standards.portcall.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.portcall.party.PortCallFilterParameter;
import org.dcsa.conformance.standards.portcall.party.PortCallRole;
import org.dcsa.conformance.standards.portcall.party.ScenarioType;

import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public class PortCallScenarioListBuilder extends ScenarioListBuilder<PortCallScenarioListBuilder> {

  private static final ThreadLocal<PortCallComponentFactory> threadLocalComponentFactory = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalProducerPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalConsumerPartyName = new ThreadLocal<>();

  private PortCallScenarioListBuilder(UnaryOperator<ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static Map<String, PortCallScenarioListBuilder> createModuleScenarioListBuilders(
    PortCallComponentFactory portCallComponentFactory, Set<String> testedPartyRoleNames, String producerPartyName, String consumerPartyName) {

    threadLocalComponentFactory.set(portCallComponentFactory);
    threadLocalProducerPartyName.set(producerPartyName);
    threadLocalConsumerPartyName.set(consumerPartyName);

    Map<String, Map<String, PortCallScenarioListBuilder>> partyScenariosMap =
      MapUtils.orderedMap(
        Map.entry(
          PortCallRole.PRODUCER.getConfigName(),
          MapUtils.orderedMap(
            Map.entry(
              "POST scenarios - alternative required path for event push",
              noAction()
                .thenEither(
                  postPortCallEvents(ScenarioType.TIMESTAMP),
                  postPortCallEvents(ScenarioType.MOVE_FORECAST))),
            Map.entry(
              "GET scenarios - alternative required path for event pull",
              noAction()
                .thenEither(
                  supplyScenarioParameters(ScenarioType.TIMESTAMP)
                    .then(getPortCallEvents()),
                  supplyScenarioParameters(ScenarioType.MOVE_FORECAST)
                    .then(getPortCallEvents()))),
            Map.entry(
              "GET scenario for pagination - optional/report-only",
              supplyScenarioParameters(PortCallFilterParameter.PORT_CALL_SERVICE_TYPE_CODE, PortCallFilterParameter.LIMIT)
                .then(getPortCallEvents(true).then(getPortCallEvents()))
                .asOptionalReportOnlyScenario()))),
        Map.entry(
          PortCallRole.CONSUMER.getConfigName(),
          MapUtils.orderedMap(
            Map.entry(
              "POST scenarios - alternative required path for event push",
              noAction()
                .thenEither(
                  postPortCallEvents(ScenarioType.TIMESTAMP),
                  postPortCallEvents(ScenarioType.MOVE_FORECAST))),
            Map.entry(
              "GET scenarios - alternative required path for event pull",
              noAction()
                .thenEither(
                  getPortCallEvents(ScenarioType.TIMESTAMP),
                  getPortCallEvents(ScenarioType.MOVE_FORECAST))))));

      if (testedPartyRoleNames.size() > 1) {
        testedPartyRoleNames.forEach(role ->
          partyScenariosMap.getOrDefault(role, Map.of()).values()
            .forEach(builder -> builder.withScenarioTitlePrefix(role + ": ")));
      }
    return MapUtils.mergePartyScenarioModules(partyScenariosMap, testedPartyRoleNames);
  }

  private static PortCallScenarioListBuilder supplyScenarioParameters(ScenarioType scenarioType) {
    String producerPartyName = threadLocalProducerPartyName.get();
    return new PortCallScenarioListBuilder(
      previousAction -> new SupplyScenarioParametersAction(producerPartyName, scenarioType));
  }

  private static PortCallScenarioListBuilder supplyScenarioParameters(
    PortCallFilterParameter... filterParameters) {
    String producerPartyName = threadLocalProducerPartyName.get();
    return new PortCallScenarioListBuilder(
      previousAction ->
        new SupplyScenarioParametersAction(producerPartyName, filterParameters));
  }

  private static PortCallScenarioListBuilder postPortCallEvents(
    ScenarioType scenarioType) {
    PortCallComponentFactory componentFactory = threadLocalComponentFactory.get();
    String producerPartyName = threadLocalProducerPartyName.get();
    String consumerPartyName = threadLocalConsumerPartyName.get();
    return new PortCallScenarioListBuilder(
      previousAction ->
        new PostPortCallEventsAction(
          producerPartyName,
          consumerPartyName,
          (PortCallAction) previousAction,
          scenarioType,
          componentFactory.getMessageSchemaValidator("PostEventsRequest")));
  }

  private static PortCallScenarioListBuilder getPortCallEvents() {
    return getPortCallEvents(false);
  }

  private static PortCallScenarioListBuilder getPortCallEvents(boolean hasNextPage) {
    PortCallComponentFactory componentFactory = threadLocalComponentFactory.get();
    String producerPartyName = threadLocalProducerPartyName.get();
    String consumerPartyName = threadLocalConsumerPartyName.get();
    return new PortCallScenarioListBuilder(
      previousAction ->
        new GetPortCallEventsAction(
          consumerPartyName,
          producerPartyName,
          (PortCallAction) previousAction,
          componentFactory.getMessageSchemaValidator("GetEventsResponse"),
          hasNextPage));
  }

  private static PortCallScenarioListBuilder getPortCallEvents(ScenarioType scenarioType) {
    PortCallComponentFactory componentFactory = threadLocalComponentFactory.get();
    String producerPartyName = threadLocalProducerPartyName.get();
    String consumerPartyName = threadLocalConsumerPartyName.get();
    return new PortCallScenarioListBuilder(
      previousAction ->
        new GetPortCallEventsAction(
          consumerPartyName,
          producerPartyName,
          (PortCallAction) previousAction,
          componentFactory.getMessageSchemaValidator("GetEventsResponse"),
          scenarioType));
  }

  private static PortCallScenarioListBuilder noAction() {
    return new PortCallScenarioListBuilder(null);
  }
}
