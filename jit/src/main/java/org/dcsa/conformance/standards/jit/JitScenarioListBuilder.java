package org.dcsa.conformance.standards.jit;

import java.util.LinkedHashMap;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.jit.action.JitPortCallAction;
import org.dcsa.conformance.standards.jit.action.JitPortCallServiceAction;
import org.dcsa.conformance.standards.jit.action.JitTerminalCallAction;
import org.dcsa.conformance.standards.jit.action.JitTimestampAction;
import org.dcsa.conformance.standards.jit.action.JitVesselStatusAction;
import org.dcsa.conformance.standards.jit.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.jit.model.JitTimestampType;
import org.dcsa.conformance.standards.jit.model.PortCallServiceType;

@Slf4j
class JitScenarioListBuilder extends ScenarioListBuilder<JitScenarioListBuilder> {

  public static LinkedHashMap<String, JitScenarioListBuilder> createModuleScenarioListBuilders(
      JitComponentFactory componentFactory, String providerPartyName, String consumerPartyName) {
    JitScenarioContext context =
        new JitScenarioContext(providerPartyName, consumerPartyName, componentFactory);

    // Scenario suite: "Service negotiation conformance"

    // 1. Scenario group: "PC - TC - S - V - E - R - P - A"
    var scenarioList = new LinkedHashMap<String, JitScenarioListBuilder>();
    scenarioList.put(
        "PC-TC-S-VERP-A service types",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                sendERPTimestamps(
                                    context, sendTimestamp(context, JitTimestampType.ACTUAL))))
                    .toList()));

    // 2. Scenario group: "S-A service types"
    scenarioList.put(
        "PC-TC-S-A service types",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesHavingOnlyA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                sendTimestamp(context, JitTimestampType.ACTUAL)))
                    .toList()));

    // 3. Scenario group: "S service type with variations"
    // TODO: Implement

    // 4. Scenario group: "PC-TC-S-V-ERP-A in-band ERP variations"
    scenarioList.put(
        "PC-TC-S-V-E-R-P-A-A service types",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                sendERPTimestamps(
                                    context,
                                    sendTimestamp(context, JitTimestampType.ACTUAL)
                                        .then(sendTimestamp(context, JitTimestampType.ACTUAL)))))
                    .toList()));

    // Not sure, to add this one: PC - TC - S(service type) - V - E - R - P - P - A

    scenarioList.put(
        "PC-TC-S-V-E-R-P-R-P-A service types",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                sendERPTimestamps(
                                    context,
                                    sendTimestamp(context, JitTimestampType.REQUESTED)
                                        .then(
                                            sendTimestamp(context, JitTimestampType.PLANNED)
                                                .then(
                                                    sendTimestamp(
                                                        context, JitTimestampType.ACTUAL))))))
                    .toList()));

    scenarioList.put(
        "PC-TC-S-V-E-R-P-E-R-P-A service types",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                sendERPTimestamps(
                                    context,
                                    sendERPTimestamps(
                                        context, sendTimestamp(context, JitTimestampType.ACTUAL)))))
                    .toList()));

    scenarioList.put(
        "PC-TC-S-V-E-R-E-R-P-A service types",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                sendTimestamp(context, JitTimestampType.ESTIMATED)
                                    .then(
                                        sendTimestamp(context, JitTimestampType.REQUESTED)
                                            .then(
                                                sendERPTimestamps(
                                                    context,
                                                    sendTimestamp(
                                                        context, JitTimestampType.ACTUAL))))))
                    .toList()));

    scenarioList.put(
        "PC-TC-S-V-E-R-R-P-A service types",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                sendTimestamp(context, JitTimestampType.ESTIMATED)
                                    .then(
                                        sendTimestamp(context, JitTimestampType.REQUESTED)
                                            .then(
                                                sendTimestamp(context, JitTimestampType.REQUESTED)
                                                    .then(
                                                        sendTimestamp(
                                                                context, JitTimestampType.PLANNED)
                                                            .then(
                                                                sendTimestamp(
                                                                    context,
                                                                    JitTimestampType.ACTUAL)))))))
                    .toList()));

    scenarioList.put(
        "PC-TC-S-V-E-E-R-P-A service types",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                sendTimestamp(context, JitTimestampType.ESTIMATED)
                                    .then(
                                        sendERPTimestamps(
                                            context,
                                            sendTimestamp(context, JitTimestampType.ACTUAL)))))
                    .toList()));

    // 5. Scenario group: "PC-TC-S-V-ERP-A in-band PC-resend variations"
    scenarioList.put(
        "PC - PC - TC - S - V - E - R - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            portCall(context)
                                .then(
                                    sendPC_TC_PCS_VS(
                                        context,
                                        serviceType,
                                        sendERPTimestamps(
                                            context,
                                            sendTimestamp(context, JitTimestampType.ACTUAL)))))
                    .toList()));

    scenarioList.put(
        "PC - TC - PC - S - V - E - R - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            portCall(context)
                                .then(
                                    terminalCall(context)
                                        .then(
                                            portCall(context)
                                                .then(
                                                    portCallService(context, serviceType)
                                                        .then(
                                                            vesselStatus(context)
                                                                .then(
                                                                    sendERPTimestamps(
                                                                        context,
                                                                        sendTimestamp(
                                                                            context,
                                                                            JitTimestampType
                                                                                .ACTUAL))))))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S - PC - V - E - R - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            portCall(context)
                                .then(
                                    terminalCall(context)
                                        .then(
                                            portCallService(context, serviceType)
                                                .then(
                                                    portCall(context)
                                                        .then(
                                                            vesselStatus(context)
                                                                .then(
                                                                    sendERPTimestamps(
                                                                        context,
                                                                        sendTimestamp(
                                                                            context,
                                                                            JitTimestampType
                                                                                .ACTUAL))))))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S - V - PC - E - R - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                portCall(context)
                                    .then(
                                        sendERPTimestamps(
                                            context,
                                            sendTimestamp(context, JitTimestampType.ACTUAL)))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S - V - E - PC - R - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                sendTimestamp(context, JitTimestampType.ESTIMATED)
                                    .then(
                                        portCall(context)
                                            .then(
                                                sendTimestamp(context, JitTimestampType.REQUESTED)
                                                    .then(
                                                        sendTimestamp(
                                                                context, JitTimestampType.PLANNED)
                                                            .then(
                                                                sendTimestamp(
                                                                    context,
                                                                    JitTimestampType.ACTUAL)))))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S - V - E - R - PC - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                sendTimestamp(context, JitTimestampType.ESTIMATED)
                                    .then(
                                        portCall(context)
                                            .then(
                                                sendTimestamp(context, JitTimestampType.REQUESTED)
                                                    .then(
                                                        portCall(context)
                                                            .then(
                                                                sendTimestamp(
                                                                        context,
                                                                        JitTimestampType.PLANNED)
                                                                    .then(
                                                                        sendTimestamp(
                                                                            context,
                                                                            JitTimestampType
                                                                                .ACTUAL))))))))
                    .toList()));

    // Scenario: "PC - TC - S(service type) - V - E - R - P - PC - A"
    // Scenario: "PC - TC - S(service type) - V - E - R - P - A - PC"

    // 6. Scenario group: "PC-TC-S-V-ERP-A in-band TC-resend variations"

    return scenarioList;
  }

  private static JitScenarioListBuilder sendTimestamp(
      JitScenarioContext context, JitTimestampType timestampType) {
    return new JitScenarioListBuilder(
        previousAction ->
            new JitTimestampAction(
                context,
                previousAction,
                timestampType,
                timestampType != JitTimestampType.REQUESTED));
  }

  private static JitScenarioListBuilder sendERPTimestamps(
      JitScenarioContext context, JitScenarioListBuilder... thenEither) {
    return sendTimestamp(context, JitTimestampType.ESTIMATED)
        .then(
            sendTimestamp(context, JitTimestampType.REQUESTED)
                .then(sendTimestamp(context, JitTimestampType.PLANNED).thenEither(thenEither)));
  }

  private JitScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static JitScenarioListBuilder supplyScenarioParameters(JitScenarioContext context) {
    return new JitScenarioListBuilder(
        previousAction -> new SupplyScenarioParametersAction(context));
  }

  private static JitScenarioListBuilder portCall(JitScenarioContext context) {
    return new JitScenarioListBuilder(
        previousAction -> new JitPortCallAction(context, previousAction));
  }

  private static JitScenarioListBuilder terminalCall(JitScenarioContext context) {
    return new JitScenarioListBuilder(
        previousAction -> new JitTerminalCallAction(context, previousAction));
  }

  private static JitScenarioListBuilder portCallService(
      JitScenarioContext context, PortCallServiceType serviceType) {
    return new JitScenarioListBuilder(
        previousAction -> new JitPortCallServiceAction(context, previousAction, serviceType));
  }

  private static JitScenarioListBuilder vesselStatus(JitScenarioContext context) {
    return new JitScenarioListBuilder(
        previousAction -> new JitVesselStatusAction(context, previousAction));
  }

  private static JitScenarioListBuilder sendPC_TC_PCS_VS(
      JitScenarioContext context,
      PortCallServiceType serviceType,
      JitScenarioListBuilder... thenEither) {
    return portCall(context)
        .then(
            terminalCall(context)
                .then(
                    portCallService(context, serviceType)
                        .then(vesselStatus(context).thenEither(thenEither))));
  }
}
