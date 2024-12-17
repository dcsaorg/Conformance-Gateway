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
        "PC - TC - S - V - E - R - P - A",
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
        "PC - TC - S - V - A",
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

    addScenarioGroup4(scenarioList, context);
    addScenarioGroup5(scenarioList, context);
    addScenarioGroup6(scenarioList, context);
    addScenarioGroup7(scenarioList, context);
    addScenarioGroup8(scenarioList, context);
    addScenarioGroup9(scenarioList, context);
    addScenarioGroup10(scenarioList, context);
    addScenarioGroup11(scenarioList, context);
    addScenarioGroup12(scenarioList, context);

    return scenarioList;
  }

  // 4. Scenario group: "PC-TC-S-V-ERP-A in-band ERP variations"
  private static void addScenarioGroup4(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "PC - TC - S - V - E - R - P - A - A",
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

    // TODO: Not sure, to add this one: PC - TC - S - V - E - R - P - P - A

    scenarioList.put(
        "PC - TC - S - V - E - R - P - R - P - A",
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
        "PC - TC - S - V - E - R - P - E - R - P - A",
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
        "PC - TC - S - V - E - R - E - R - P - A",
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
        "PC - TC - S - V - E - R - R - P - A",
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
        "PC - TC - S - V - E - E - R - P - A service types",
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
  }

  // 5. Scenario group: "PC-TC-S-V-ERP-A in-band PC-resend variations"
  private static void addScenarioGroup5(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "PC - PC - TC - S - V - E - R - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            portCall(context)
                                .then(
                                    portCall(context)
                                        .then(
                                            sendPC_TC_PCS_VS(
                                                context,
                                                serviceType,
                                                sendERPTimestamps(
                                                    context,
                                                    sendTimestamp(
                                                        context, JitTimestampType.ACTUAL))))))
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
                                                    serviceCall(context, serviceType)
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
                                            serviceCall(context, serviceType)
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

    scenarioList.put(
        "PC - TC - S - V - E - R - P - PC - A",
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
                                    portCall(context)
                                        .then(sendTimestamp(context, JitTimestampType.ACTUAL)))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S - V - E - R - P - A - PC",
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
                                        .then(portCall(context)))))
                    .toList()));
  }

  // 6. Scenario group: "PC-TC-S-V-ERP-A in-band TC-resend variations"
  private static void addScenarioGroup6(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {

    scenarioList.put(
        "PC - TC - TC - S - V - E - R - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            portCall(context)
                                .then(
                                    terminalCall(context)
                                        .then(
                                            terminalCall(context)
                                                .then(
                                                    serviceCall(context, serviceType)
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
        "PC - TC - S - TC - V - E - R - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            portCall(context)
                                .then(
                                    terminalCall(context)
                                        .then(
                                            serviceCall(context, serviceType)
                                                .then(
                                                    terminalCall(context)
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
        "PC - TC - S - V - TC - E - R - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                terminalCall(context)
                                    .then(
                                        sendERPTimestamps(
                                            context,
                                            sendTimestamp(context, JitTimestampType.ACTUAL)))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S - V - E - TC - R - P - A",
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
                                        terminalCall(context)
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
        "PC - TC - S - V - E - R - TC - P - A",
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
                                                terminalCall(context)
                                                    .then(
                                                        sendTimestamp(
                                                                context, JitTimestampType.PLANNED)
                                                            .then(
                                                                sendTimestamp(
                                                                    context,
                                                                    JitTimestampType.ACTUAL)))))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S- V - E - R - P - TC - A",
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
                                    terminalCall(context)
                                        .then(sendTimestamp(context, JitTimestampType.ACTUAL)))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S - V - E - R - P - A - TC",
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
                                        .then(terminalCall(context)))))
                    .toList()));
  }

  // 7. Scenario group: "PC-TC-S-V-ERP-A in-band S-resend variations"
  private static void addScenarioGroup7(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "PC - TC - S - S - V - E - R - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            portCall(context)
                                .then(
                                    terminalCall(context)
                                        .then(
                                            serviceCall(context, serviceType)
                                                .then(
                                                    serviceCall(context, serviceType)
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
        "PC - TC - S - V - S - E - R - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                serviceCall(context, serviceType)
                                    .then(
                                        sendERPTimestamps(
                                            context,
                                            sendTimestamp(context, JitTimestampType.ACTUAL)))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S - V - E - S - R - P - A",
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
                                        serviceCall(context, serviceType)
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
        "PC - TC - S - V - E - R - S - P - A",
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
                                                serviceCall(context, serviceType)
                                                    .then(
                                                        sendTimestamp(
                                                                context, JitTimestampType.PLANNED)
                                                            .then(
                                                                sendTimestamp(
                                                                    context,
                                                                    JitTimestampType.ACTUAL)))))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S - V - E - R - P - S - A",
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
                                    serviceCall(context, serviceType)
                                        .then(sendTimestamp(context, JitTimestampType.ACTUAL)))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S - V - E - R - P - A - S",
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
                                        .then(serviceCall(context, serviceType)))))
                    .toList()));
  }

  // 8. Scenario group: "PC-TC-S-V-ERP-A in-band V-resend variations"
  private static void addScenarioGroup8(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "PC - TC - S - V - V - E - R - P - A",
        supplyScenarioParameters(context)
            .thenEither(
                PortCallServiceType.getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                vesselStatus(context)
                                    .then(
                                        sendERPTimestamps(
                                            context,
                                            sendTimestamp(context, JitTimestampType.ACTUAL)))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S - V - E - V - R - P - A",
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
                                        vesselStatus(context)
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
        "PC - TC - S - V - E - R - V - P - A",
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
                                                vesselStatus(context)
                                                    .then(
                                                        sendTimestamp(
                                                                context, JitTimestampType.PLANNED)
                                                            .then(
                                                                sendTimestamp(
                                                                    context,
                                                                    JitTimestampType.ACTUAL)))))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S - V - E - R - P - V - A",
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
                                    vesselStatus(context)
                                        .then(sendTimestamp(context, JitTimestampType.ACTUAL)))))
                    .toList()));

    scenarioList.put(
        "PC - TC - S - V - E - R - P - A - V",
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
                                        .then(vesselStatus(context)))))
                    .toList()));
  }

  // 9. Scenario group: "PC-TC-S-V-ERP-A out-of-band variations"
  private static void addScenarioGroup9(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    // TODO
  }

  // 10. Scenario group: "PC-TC-S-V-A variations"
  private static void addScenarioGroup10(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    // TODO
  }

  // 11. Scenario group: "S-ERP-A cancel"
  private static void addScenarioGroup11(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    // TODO
  }

  // 12. Scenario group: "S-ERP-A decline"
  private static void addScenarioGroup12(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    // TODO
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

  private static JitScenarioListBuilder serviceCall(
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
                    serviceCall(context, serviceType)
                        .then(vesselStatus(context).thenEither(thenEither))));
  }
}
