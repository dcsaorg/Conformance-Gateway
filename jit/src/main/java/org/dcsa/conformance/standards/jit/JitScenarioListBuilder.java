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
        "S-ERP-A service types",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            portCallService(context, serviceType)
                                .then(
                                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                                        .then(
                                            sendTimestamp(context, JitTimestampType.REQUESTED)
                                                .then(
                                                    sendTimestamp(context, JitTimestampType.PLANNED)
                                                        .then(
                                                            sendTimestamp(
                                                                context,
                                                                JitTimestampType.ACTUAL))))))
                    .toList()
                    .toArray(new JitScenarioListBuilder[] {})));

    scenarioList.put(
        "S-A service types",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesHavingOnlyA().stream()
                    .map(
                        serviceType ->
                            portCallService(context, serviceType)
                                .then(sendTimestamp(context, JitTimestampType.ACTUAL)))
                    .toList()
                    .toArray(new JitScenarioListBuilder[] {})));

    // “S service type with variations” TODO: Implement

    scenarioList.put(
        "S-ERP-A in-band ERP variations - S - E - R - P - A - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .filter(PortCallServiceType::isCommon)
                    .map(
                        serviceType ->
                            portCallService(context, serviceType)
                                .then(
                                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                                        .then(
                                            sendTimestamp(context, JitTimestampType.REQUESTED)
                                                .then(
                                                    sendTimestamp(context, JitTimestampType.PLANNED)
                                                        .then(
                                                            sendTimestamp(
                                                                    context,
                                                                    JitTimestampType.ACTUAL)
                                                                .then(
                                                                    sendTimestamp(
                                                                        context,
                                                                        JitTimestampType
                                                                            .ACTUAL)))))))
                    .toList()
                    .toArray(new JitScenarioListBuilder[] {})));

    scenarioList.put(
        "S-ERP-A in-band ERP variations - S(service type) - E - R - P - R - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .filter(PortCallServiceType::isCommon)
                    .map(
                        serviceType ->
                            portCallService(context, serviceType)
                                .then(
                                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                                        .then(
                                            sendTimestamp(context, JitTimestampType.REQUESTED)
                                                .then(
                                                    sendTimestamp(context, JitTimestampType.PLANNED)
                                                        .then(
                                                            sendTimestamp(
                                                                    context,
                                                                    JitTimestampType.REQUESTED)
                                                                .then(
                                                                    sendTimestamp(
                                                                            context,
                                                                            JitTimestampType
                                                                                .PLANNED)
                                                                        .then(
                                                                            sendTimestamp(
                                                                                context,
                                                                                JitTimestampType
                                                                                    .ACTUAL))))))))
                    .toList()
                    .toArray(new JitScenarioListBuilder[] {})));

    scenarioList.put(
        "S-ERP-A in-band S-resend variations - S - E - R - P - A“",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .filter(PortCallServiceType::isCommon)
                    .map(
                        serviceType ->
                            portCallService(context, serviceType)
                                .then(
                                    portCallService(context, serviceType)
                                        .then(
                                            sendTimestamp(context, JitTimestampType.ESTIMATED)
                                                .then(
                                                    sendTimestamp(
                                                            context, JitTimestampType.REQUESTED)
                                                        .then(
                                                            sendTimestamp(
                                                                    context,
                                                                    JitTimestampType.PLANNED)
                                                                .then(
                                                                    sendTimestamp(
                                                                        context,
                                                                        JitTimestampType
                                                                            .ACTUAL)))))))
                    .toList()
                    .toArray(new JitScenarioListBuilder[] {})));

    return scenarioList;
  }

  private static JitScenarioListBuilder sendTimestamp(
      JitScenarioContext context, JitTimestampType timestampType) {
    if (timestampType == JitTimestampType.REQUESTED) {
      return new JitScenarioListBuilder(
          previousAction ->
              new JitTimestampAction(context, previousAction, JitTimestampType.REQUESTED, false));
    }
    return new JitScenarioListBuilder(
        previousAction -> new JitTimestampAction(context, previousAction, timestampType, true));
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
