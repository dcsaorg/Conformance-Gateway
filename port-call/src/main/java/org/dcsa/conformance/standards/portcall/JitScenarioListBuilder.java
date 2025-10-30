package org.dcsa.conformance.standards.portcall;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.portcall.action.PortCallGetAction;
import org.dcsa.conformance.standards.portcall.action.PortCallPostAction;
import org.dcsa.conformance.standards.portcall.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.portcall.model.JitGetType;
import org.dcsa.conformance.standards.portcall.model.JitServiceTypeSelector;

@Slf4j
class JitScenarioListBuilder extends ScenarioListBuilder<JitScenarioListBuilder> {
  private static final ThreadLocal<JitComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  protected JitScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  public static LinkedHashMap<String, JitScenarioListBuilder> createModuleScenarioListBuilders(
      JitComponentFactory componentFactory, String providerPartyName, String consumerPartyName) {

    threadLocalComponentFactory.set(componentFactory);
    threadLocalPublisherPartyName.set(providerPartyName);
    threadLocalSubscriberPartyName.set(consumerPartyName);

    return Stream.of(
            Map.entry(
                "POST-only scenarios (for adopters only supporting POST)",
                noAction().then(postTimestamp())),
            Map.entry(
                "GET-only scenarios (for adopters only supporting GET)",
                noAction().then(supplyScenarioParameters().then(getTimestamps()))))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
  }

  private static JitScenarioListBuilder postTimestamp() {
    return new JitScenarioListBuilder(previousAction -> new PortCallPostAction(previousAction));
  }

  private static JitScenarioListBuilder supplyScenarioParameters(
      @NonNull JitScenarioContext context, JitServiceTypeSelector selector, boolean isFYI) {
    return new JitScenarioListBuilder(
        previousAction -> new SupplyScenarioParametersAction(context, selector, isFYI));
  }

  private static JitScenarioListBuilder getTimestamps(
      JitScenarioContext context,
      List<String> filters,
      boolean requestedByProvider,
      int expectedResults) {
    return new JitScenarioListBuilder(
        previousAction ->
            new PortCallGetAction(
                context,
                previousAction,
                JitGetType.TIMESTAMPS,
                filters,
                requestedByProvider,
                expectedResults));
  }

  private static JitScenarioListBuilder noAction() {
    return new JitScenarioListBuilder(null);
  }
}
