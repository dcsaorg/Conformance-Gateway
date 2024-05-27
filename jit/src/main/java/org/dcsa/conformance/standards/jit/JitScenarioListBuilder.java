package org.dcsa.conformance.standards.jit;

import static org.dcsa.conformance.standards.jit.party.JitFilterParameter.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.jit.action.JitGetEventsAction;
import org.dcsa.conformance.standards.jit.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.jit.party.JitFilterParameter;
import org.dcsa.conformance.standards.jit.party.JitRole;

@Slf4j
class JitScenarioListBuilder extends ScenarioListBuilder<JitScenarioListBuilder> {
  private static final ThreadLocal<JitComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  public static LinkedHashMap<String, JitScenarioListBuilder> createModuleScenarioListBuilders(
      JitComponentFactory componentFactory, String publisherPartyName, String subscriberPartyName) {
    threadLocalComponentFactory.set(componentFactory);
    threadLocalPublisherPartyName.set(publisherPartyName);
    threadLocalSubscriberPartyName.set(subscriberPartyName);
    return Stream.of(
            Map.entry(
                "",
                noAction()
                    .thenEither(
                        scenarioWithParameters(TRANSPORT_CALL_ID, LIMIT),
                        scenarioWithParameters(VESSEL_IMO_NUMBER, LIMIT),
                        scenarioWithParameters(CARRIER_SERVICE_CODE, LIMIT),
                        scenarioWithParameters(UN_LOCATION_CODE, LIMIT),
                        scenarioWithParameters(OPERATIONS_EVENT_TYPE_CODE, LIMIT),
                        scenarioWithParameters(
                            EVENT_CREATED_DATE_TIME_GTE, EVENT_CREATED_DATE_TIME_LT, LIMIT))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private JitScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static JitScenarioListBuilder noAction() {
    return new JitScenarioListBuilder(null);
  }

  private static JitScenarioListBuilder scenarioWithParameters(
      JitFilterParameter... jitFilterParameters) {
    return supplyScenarioParameters(jitFilterParameters).then(getEvents());
  }

  private static JitScenarioListBuilder supplyScenarioParameters(
      JitFilterParameter... jitFilterParameters) {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new JitScenarioListBuilder(
        previousAction ->
            new SupplyScenarioParametersAction(publisherPartyName, jitFilterParameters));
  }

  private static JitScenarioListBuilder getEvents() {
    JitComponentFactory componentFactory = threadLocalComponentFactory.get();
    String publisherPartyName = threadLocalPublisherPartyName.get();
    String subscriberPartyName = threadLocalSubscriberPartyName.get();
    return new JitScenarioListBuilder(
        previousAction ->
            new JitGetEventsAction(
                subscriberPartyName,
                publisherPartyName,
                previousAction,
                componentFactory.getMessageSchemaValidator(
                    JitRole.PUBLISHER.getConfigName(), false)));
  }
}
