package org.dcsa.conformance.end;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;

public class EndorsementChainScenarioListBuilder
    extends ScenarioListBuilder<EndorsementChainScenarioListBuilder> {
  protected EndorsementChainScenarioListBuilder(
      Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static final ThreadLocal<EndorsementChainComponentFactory> threadLocalComponentFactory =
      new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalPublisherPartyName = new ThreadLocal<>();
  private static final ThreadLocal<String> threadLocalSubscriberPartyName = new ThreadLocal<>();

  public static Map<String, EndorsementChainScenarioListBuilder> createModuleScenarioListBuilders(
      EndorsementChainComponentFactory endComponentChainFactory,
      String providerPartyName,
      String carrierPartyName) {

    threadLocalComponentFactory.set(endComponentChainFactory);
    threadLocalPublisherPartyName.set(providerPartyName);
    threadLocalSubscriberPartyName.set(carrierPartyName);

    Map<String, EndorsementChainScenarioListBuilder> map = new LinkedHashMap<>();

    map.put(
        "GET-only scenarios (for adopters only supporting GET)",
        noAction()
            .thenEither(
                supplyScenarioParameters().then(getEndorsementChain()),
                supplyScenarioParameters().then(getEndorsementChain()),
                supplyScenarioParameters().then(getEndorsementChain())));
    return map;
  }

  private static EndorsementChainScenarioListBuilder noAction() {
    return new EndorsementChainScenarioListBuilder(null);
  }

  private static EndorsementChainScenarioListBuilder supplyScenarioParameters() {
    String publisherPartyName = threadLocalPublisherPartyName.get();
    return new EndorsementChainScenarioListBuilder(
        previousAction -> new SupplyScenarioParametersAction(publisherPartyName, scenarioType));
  }

  private static EndorsementChainScenarioListBuilder getEndorsementChain() {
    return null;
  }
}
