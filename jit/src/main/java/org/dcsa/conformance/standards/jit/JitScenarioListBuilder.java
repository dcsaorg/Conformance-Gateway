package org.dcsa.conformance.standards.jit;

import java.util.LinkedHashMap;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.jit.action.JitPortCallServiceAction;
import org.dcsa.conformance.standards.jit.action.JitTimestampAction;
import org.dcsa.conformance.standards.jit.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.jit.model.JitTimestampType;
import org.dcsa.conformance.standards.jit.model.PortCallServiceType;

@Slf4j
class JitScenarioListBuilder extends ScenarioListBuilder<JitScenarioListBuilder> {

  public static LinkedHashMap<String, JitScenarioListBuilder> createModuleScenarioListBuilders(
      JitComponentFactory componentFactory, String providerPartyName, String consumerPartyName) {
    JitScenarioContext context =
        new JitScenarioContext(providerPartyName, consumerPartyName, componentFactory);

    var scenarioList = new LinkedHashMap<String, JitScenarioListBuilder>();
    scenarioList.put(
        "S-A service types",
        supplyScenarioParameters(context)
            .thenEither(
                portCallService(context, PortCallServiceType.SEA_PASSAGE)
                    .then(sendTimestamp(context, JitTimestampType.ACTUAL)),
                portCallService(context, PortCallServiceType.ALL_FAST)
                    .then(sendTimestamp(context, JitTimestampType.ACTUAL))));

    scenarioList.put(
        "S-ERP-A service types",
        supplyScenarioParameters(context)
            .then(
                portCallService(context, PortCallServiceType.BERTH)
                    .then(
                        sendTimestamp(context, JitTimestampType.ESTIMATED)
                            .then(
                                sendTimestampConsumer(context, JitTimestampType.REQUESTED)
                                    .then(
                                        sendTimestamp(context, JitTimestampType.PLANNED)
                                            .then(
                                                sendTimestamp(
                                                    context, JitTimestampType.ACTUAL)))))));
    return scenarioList;
  }

  private static JitScenarioListBuilder sendTimestamp(
      JitScenarioContext context, JitTimestampType timestampType) {
    return new JitScenarioListBuilder(
        previousAction -> new JitTimestampAction(context, previousAction, timestampType, true));
  }

  private static JitScenarioListBuilder sendTimestampConsumer(
      JitScenarioContext context, JitTimestampType timestampType) {
    return new JitScenarioListBuilder(
        previousAction -> new JitTimestampAction(context, previousAction, timestampType, false));
  }

  private JitScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static JitScenarioListBuilder supplyScenarioParameters(JitScenarioContext context) {
    return new JitScenarioListBuilder(
        previousAction -> new SupplyScenarioParametersAction(context));
  }

  private static JitScenarioListBuilder portCallService(
      JitScenarioContext context, PortCallServiceType serviceType) {
    return new JitScenarioListBuilder(
        previousAction -> new JitPortCallServiceAction(context, previousAction, serviceType));
  }
}
