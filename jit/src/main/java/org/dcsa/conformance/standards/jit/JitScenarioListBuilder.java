package org.dcsa.conformance.standards.jit;

import static org.dcsa.conformance.standards.jit.model.JitServiceTypeSelector.*;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.*;

import java.util.LinkedHashMap;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.jit.action.JitCancelAction;
import org.dcsa.conformance.standards.jit.action.JitDeclineAction;
import org.dcsa.conformance.standards.jit.action.JitOmitPortCallAction;
import org.dcsa.conformance.standards.jit.action.JitOmitTerminalCallAction;
import org.dcsa.conformance.standards.jit.action.JitPortCallAction;
import org.dcsa.conformance.standards.jit.action.JitPortCallServiceAction;
import org.dcsa.conformance.standards.jit.action.JitTerminalCallAction;
import org.dcsa.conformance.standards.jit.action.JitTimestampAction;
import org.dcsa.conformance.standards.jit.action.JitVesselStatusAction;
import org.dcsa.conformance.standards.jit.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.jit.model.JitServiceTypeSelector;
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
        "1. PC - TC - S - V - E - R - P - A",
        supplyScenarioParameters(context, GIVEN)
            .thenEither(
                getServicesWithERPAndA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                GIVEN,
                                sendERPTimestamps(
                                    context, sendTimestamp(context, JitTimestampType.ACTUAL))))
                    .toList()));

    // 2. Scenario group: "S-A service types"
    scenarioList.put(
        "2. PC - TC - S - V - A",
        supplyScenarioParameters(context, GIVEN)
            .thenEither(
                getServicesHavingOnlyA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_PCS_VS(
                                context,
                                serviceType,
                                GIVEN,
                                sendTimestamp(context, JitTimestampType.ACTUAL)))
                    .toList()));

    addScenarioGroup3(scenarioList, context);
    addScenarioGroup4(scenarioList, context);
    addScenarioGroup5(scenarioList, context);
    addScenarioGroup6(scenarioList, context);
    addScenarioGroup7(scenarioList, context);
    addScenarioGroup8(scenarioList, context);
    addScenarioGroup9(scenarioList, context);
    addScenarioGroup10(scenarioList, context);
    addScenarioGroup11(scenarioList, context);
    addScenarioGroup12(scenarioList, context);

    // Scenario suite: "Secondary sender and receiver conformance"
    addScenarioGroupSecondary1(scenarioList, context);
    addScenarioGroupSecondary2(scenarioList, context);
    addScenarioGroupSecondary3(scenarioList, context);

    return scenarioList;
  }

  private static void addScenarioGroup3(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "3. S-service type with variations (Moves only)",
        supplyScenarioParameters(context, GIVEN)
            .thenEither(
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(serviceCall(context, MOVES, GIVEN).then(vesselStatus(context)))),
                portCall(context)
                    .then(
                        portCall(context)
                            .then(
                                terminalCall(context)
                                    .then(
                                        serviceCall(context, MOVES, GIVEN)
                                            .then(vesselStatus(context))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                portCall(context)
                                    .then(
                                        serviceCall(context, MOVES, GIVEN)
                                            .then(vesselStatus(context))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, MOVES, GIVEN)
                                    .then(portCall(context).then(vesselStatus(context))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, MOVES, GIVEN)
                                    .then(vesselStatus(context).then(portCall(context))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                terminalCall(context)
                                    .then(
                                        serviceCall(context, MOVES, GIVEN)
                                            .then(vesselStatus(context))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, MOVES, GIVEN)
                                    .then(terminalCall(context).then(vesselStatus(context))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, MOVES, GIVEN)
                                    .then(vesselStatus(context).then(terminalCall(context))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, MOVES, GIVEN)
                                    .then(
                                        serviceCall(context, MOVES, GIVEN)
                                            .then(vesselStatus(context))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, MOVES, GIVEN)
                                    .then(
                                        vesselStatus(context)
                                            .then(serviceCall(context, MOVES, GIVEN))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, MOVES, GIVEN)
                                    .then(vesselStatus(context).then(vesselStatus(context))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, MOVES, GIVEN)
                                    .then(vesselStatus(context).then(cancelCall(context))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(serviceCall(context, MOVES, GIVEN).then(cancelCall(context)))),
                sendPC_TC_PCS_VS(context, MOVES, GIVEN, declineCall(context)),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(serviceCall(context, MOVES, GIVEN).then(declineCall(context))))));
  }

  private static void addScenarioGroup4(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {

    // TODO: Not sure, to add this one: PC - TC - S - V - E - R - P - P - A

    scenarioList.put(
        "4. PC-TC-S-V-ERP-A in-band ERP variations",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        sendTimestamp(context, JitTimestampType.ACTUAL)
                            .then(sendTimestamp(context, JitTimestampType.ACTUAL)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        sendTimestamp(context, JitTimestampType.REQUESTED)
                            .then(
                                sendTimestamp(context, JitTimestampType.PLANNED)
                                    .then(sendTimestamp(context, JitTimestampType.ACTUAL))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        sendERPTimestamps(
                            context, sendTimestamp(context, JitTimestampType.ACTUAL)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(
                            sendTimestamp(context, JitTimestampType.REQUESTED)
                                .then(
                                    sendERPTimestamps(
                                        context,
                                        sendTimestamp(context, JitTimestampType.ACTUAL))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(
                            sendTimestamp(context, JitTimestampType.REQUESTED)
                                .then(
                                    sendTimestamp(context, JitTimestampType.REQUESTED)
                                        .then(
                                            sendTimestamp(context, JitTimestampType.PLANNED)
                                                .then(
                                                    sendTimestamp(
                                                        context, JitTimestampType.ACTUAL)))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(
                            sendERPTimestamps(
                                context, sendTimestamp(context, JitTimestampType.ACTUAL))))));
  }

  private static void addScenarioGroup5(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "5. PC-TC-S-V-ERP-A in-band PC-resend variations",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                portCall(context)
                    .then(
                        portCall(context)
                            .then(
                                sendPC_TC_PCS_VS(
                                    context,
                                    null,
                                    FULL_ERP,
                                    sendERPTimestamps(
                                        context,
                                        sendTimestamp(context, JitTimestampType.ACTUAL))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                portCall(context)
                                    .then(
                                        serviceCall(context, null, FULL_ERP)
                                            .then(
                                                vesselStatus(context)
                                                    .then(
                                                        sendERPTimestamps(
                                                            context,
                                                            sendTimestamp(
                                                                context,
                                                                JitTimestampType.ACTUAL))))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, FULL_ERP)
                                    .then(
                                        portCall(context)
                                            .then(
                                                vesselStatus(context)
                                                    .then(
                                                        sendERPTimestamps(
                                                            context,
                                                            sendTimestamp(
                                                                context,
                                                                JitTimestampType.ACTUAL))))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    portCall(context)
                        .then(
                            sendERPTimestamps(
                                context, sendTimestamp(context, JitTimestampType.ACTUAL)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(
                            portCall(context)
                                .then(
                                    sendTimestamp(context, JitTimestampType.REQUESTED)
                                        .then(
                                            sendTimestamp(context, JitTimestampType.PLANNED)
                                                .then(
                                                    sendTimestamp(
                                                        context, JitTimestampType.ACTUAL)))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(
                            portCall(context)
                                .then(
                                    sendTimestamp(context, JitTimestampType.REQUESTED)
                                        .then(
                                            portCall(context)
                                                .then(
                                                    sendTimestamp(context, JitTimestampType.PLANNED)
                                                        .then(
                                                            sendTimestamp(
                                                                context,
                                                                JitTimestampType.ACTUAL))))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        portCall(context).then(sendTimestamp(context, JitTimestampType.ACTUAL)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        sendTimestamp(context, JitTimestampType.ACTUAL).then(portCall(context))))));
  }

  private static void addScenarioGroup6(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {

    scenarioList.put(
        "6. PC-TC-S-V-ERP-A in-band TC-resend variations",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                terminalCall(context)
                                    .then(
                                        serviceCall(context, null, FULL_ERP)
                                            .then(
                                                vesselStatus(context)
                                                    .then(
                                                        sendERPTimestamps(
                                                            context,
                                                            sendTimestamp(
                                                                context,
                                                                JitTimestampType.ACTUAL))))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, FULL_ERP)
                                    .then(
                                        terminalCall(context)
                                            .then(
                                                vesselStatus(context)
                                                    .then(
                                                        sendERPTimestamps(
                                                            context,
                                                            sendTimestamp(
                                                                context,
                                                                JitTimestampType.ACTUAL))))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    terminalCall(context)
                        .then(
                            sendERPTimestamps(
                                context, sendTimestamp(context, JitTimestampType.ACTUAL)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(
                            terminalCall(context)
                                .then(
                                    sendTimestamp(context, JitTimestampType.REQUESTED)
                                        .then(
                                            sendTimestamp(context, JitTimestampType.PLANNED)
                                                .then(
                                                    sendTimestamp(
                                                        context, JitTimestampType.ACTUAL)))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(
                            sendTimestamp(context, JitTimestampType.REQUESTED)
                                .then(
                                    terminalCall(context)
                                        .then(
                                            sendTimestamp(context, JitTimestampType.PLANNED)
                                                .then(
                                                    sendTimestamp(
                                                        context, JitTimestampType.ACTUAL)))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        terminalCall(context)
                            .then(sendTimestamp(context, JitTimestampType.ACTUAL)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        sendTimestamp(context, JitTimestampType.ACTUAL)
                            .then(terminalCall(context))))));
  }

  private static void addScenarioGroup7(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "7. PC-TC-S-V-ERP-A in-band S-resend variations",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, FULL_ERP)
                                    .then(
                                        serviceCall(context, null, FULL_ERP)
                                            .then(
                                                vesselStatus(context)
                                                    .then(
                                                        sendERPTimestamps(
                                                            context,
                                                            sendTimestamp(
                                                                context,
                                                                JitTimestampType.ACTUAL))))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    serviceCall(context, null, FULL_ERP)
                        .then(
                            sendERPTimestamps(
                                context, sendTimestamp(context, JitTimestampType.ACTUAL)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(
                            serviceCall(context, null, FULL_ERP)
                                .then(
                                    sendTimestamp(context, JitTimestampType.REQUESTED)
                                        .then(
                                            sendTimestamp(context, JitTimestampType.PLANNED)
                                                .then(
                                                    sendTimestamp(
                                                        context, JitTimestampType.ACTUAL)))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(
                            sendTimestamp(context, JitTimestampType.REQUESTED)
                                .then(
                                    serviceCall(context, null, FULL_ERP)
                                        .then(
                                            sendTimestamp(context, JitTimestampType.PLANNED)
                                                .then(
                                                    sendTimestamp(
                                                        context, JitTimestampType.ACTUAL)))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        serviceCall(context, null, FULL_ERP)
                            .then(sendTimestamp(context, JitTimestampType.ACTUAL)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        sendTimestamp(context, JitTimestampType.ACTUAL)
                            .then(serviceCall(context, null, FULL_ERP))))));
  }

  private static void addScenarioGroup8(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "8. PC-TC-S-V-ERP-A in-band V-resend variations",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    vesselStatus(context)
                        .then(
                            sendERPTimestamps(
                                context, sendTimestamp(context, JitTimestampType.ACTUAL)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(
                            vesselStatus(context)
                                .then(
                                    sendTimestamp(context, JitTimestampType.REQUESTED)
                                        .then(
                                            sendTimestamp(context, JitTimestampType.PLANNED)
                                                .then(
                                                    sendTimestamp(
                                                        context, JitTimestampType.ACTUAL)))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(
                            sendTimestamp(context, JitTimestampType.REQUESTED)
                                .then(
                                    vesselStatus(context)
                                        .then(
                                            sendTimestamp(context, JitTimestampType.PLANNED)
                                                .then(
                                                    sendTimestamp(
                                                        context, JitTimestampType.ACTUAL)))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        vesselStatus(context)
                            .then(sendTimestamp(context, JitTimestampType.ACTUAL)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        sendTimestamp(context, JitTimestampType.ACTUAL)
                            .then(vesselStatus(context))))));
  }

  // 9. Scenario group: "PC-TC-S-V-ERP-A out-of-band variations"
  private static void addScenarioGroup9(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    // TODO
  }

  private static void addScenarioGroup10(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "10. PC-TC-S-V-A variations",
        supplyScenarioParameters(context, S_A_PATTERN)
            .thenEither(
                portCall(context)
                    .then(
                        sendPC_TC_PCS_VS(
                            context,
                            null,
                            S_A_PATTERN,
                            vesselStatus(context)
                                .then(sendTimestamp(context, JitTimestampType.ACTUAL)))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                portCall(context)
                                    .then(
                                        serviceCall(context, null, S_A_PATTERN)
                                            .then(
                                                vesselStatus(context)
                                                    .then(
                                                        sendTimestamp(
                                                            context, JitTimestampType.ACTUAL)))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, S_A_PATTERN)
                                    .then(
                                        portCall(context)
                                            .then(
                                                vesselStatus(context)
                                                    .then(
                                                        sendTimestamp(
                                                            context, JitTimestampType.ACTUAL)))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    portCall(context).then(sendTimestamp(context, JitTimestampType.ACTUAL))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, JitTimestampType.ACTUAL).then(portCall(context))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                terminalCall(context)
                                    .then(
                                        serviceCall(context, null, S_A_PATTERN)
                                            .then(
                                                vesselStatus(context)
                                                    .then(
                                                        sendTimestamp(
                                                            context, JitTimestampType.ACTUAL)))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, S_A_PATTERN)
                                    .then(
                                        terminalCall(context)
                                            .then(
                                                vesselStatus(context)
                                                    .then(
                                                        sendTimestamp(
                                                            context, JitTimestampType.ACTUAL)))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    terminalCall(context).then(sendTimestamp(context, JitTimestampType.ACTUAL))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, JitTimestampType.ACTUAL).then(terminalCall(context))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, S_A_PATTERN)
                                    .then(
                                        serviceCall(context, null, S_A_PATTERN)
                                            .then(
                                                vesselStatus(context)
                                                    .then(
                                                        sendTimestamp(
                                                            context, JitTimestampType.ACTUAL)))))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    serviceCall(context, null, S_A_PATTERN)
                        .then(sendTimestamp(context, JitTimestampType.ACTUAL))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, JitTimestampType.ACTUAL)
                        .then(serviceCall(context, null, S_A_PATTERN))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    vesselStatus(context).then(sendTimestamp(context, JitTimestampType.ACTUAL))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, JitTimestampType.ACTUAL).then(vesselStatus(context))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, JitTimestampType.ACTUAL)
                        .then(sendTimestamp(context, JitTimestampType.ACTUAL))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, JitTimestampType.ACTUAL).then(cancelCall(context))),
                sendPC_TC_PCS_VS(context, null, S_A_PATTERN, cancelCall(context)),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, S_A_PATTERN).then(cancelCall(context)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, JitTimestampType.ACTUAL).then(declineCall(context))),
                sendPC_TC_PCS_VS(context, null, S_A_PATTERN, declineCall(context)),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, S_A_PATTERN)
                                    .then(declineCall(context))))));
  }

  private static void addScenarioGroup11(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "11. S-ERP-A cancel",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                sendPC_TC_PCS_VS(
                    context, null, FULL_ERP, sendERPTimestamps(context, cancelCall(context))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(
                            sendTimestamp(context, JitTimestampType.REQUESTED)
                                .then(cancelCall(context)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED).then(cancelCall(context))),
                sendPC_TC_PCS_VS(context, null, FULL_ERP, cancelCall(context)),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, FULL_ERP).then(cancelCall(context))))));
  }

  private static void addScenarioGroup12(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "12. S-ERP-A decline",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                sendPC_TC_PCS_VS(
                    context, null, FULL_ERP, sendERPTimestamps(context, declineCall(context))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(
                            sendTimestamp(context, JitTimestampType.REQUESTED)
                                .then(declineCall(context)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED).then(declineCall(context))),
                sendPC_TC_PCS_VS(context, null, FULL_ERP, declineCall(context)),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, FULL_ERP).then(declineCall(context))))));
  }

  // 1. "PC-TC-S-V-ERP-A as FYI messages"
  private static void addScenarioGroupSecondary1(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "1. PC-TC-S-V-ERP-A as FYI messages",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(portCall(context).then(omitPortCall(context)))),
                sendPC_TC_PCS_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, JitTimestampType.ESTIMATED)
                        .then(portCall(context).then(omitTerminalCall(context))))));
    // TODO
  }

  // 2. Scenario group: "S-A as FYI messages"
  private static void addScenarioGroupSecondary2(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    // TODO
  }

  // 3. Scenario group: "S as FYI message"
  private static void addScenarioGroupSecondary3(
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

  private static JitScenarioListBuilder supplyScenarioParameters(
      JitScenarioContext context, JitServiceTypeSelector selector) {
    return new JitScenarioListBuilder(
        previousAction -> new SupplyScenarioParametersAction(context, selector));
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
      JitScenarioContext context,
      PortCallServiceType serviceType,
      JitServiceTypeSelector selector) {
    if (serviceType == null && selector == null) {
      throw new IllegalArgumentException("Either serviceType or selector must be provided");
    }
    return new JitScenarioListBuilder(
        previousAction ->
            new JitPortCallServiceAction(context, previousAction, serviceType, selector));
  }

  private static JitScenarioListBuilder vesselStatus(JitScenarioContext context) {
    return new JitScenarioListBuilder(
        previousAction -> new JitVesselStatusAction(context, previousAction));
  }

  private static JitScenarioListBuilder cancelCall(JitScenarioContext context) {
    return new JitScenarioListBuilder(
        previousAction -> new JitCancelAction(context, previousAction));
  }

  private static JitScenarioListBuilder declineCall(JitScenarioContext context) {
    return new JitScenarioListBuilder(
        previousAction -> new JitDeclineAction(context, previousAction));
  }

  private static JitScenarioListBuilder omitPortCall(JitScenarioContext context) {
    return new JitScenarioListBuilder(
        previousAction -> new JitOmitPortCallAction(context, previousAction));
  }

  private static JitScenarioListBuilder omitTerminalCall(JitScenarioContext context) {
    return new JitScenarioListBuilder(
        previousAction -> new JitOmitTerminalCallAction(context, previousAction));
  }

  private static JitScenarioListBuilder sendPC_TC_PCS_VS(
      JitScenarioContext context,
      PortCallServiceType serviceType,
      JitServiceTypeSelector selector,
      JitScenarioListBuilder... thenEither) {
    return portCall(context)
        .then(
            terminalCall(context)
                .then(
                    serviceCall(context, serviceType, selector)
                        .then(vesselStatus(context).thenEither(thenEither))));
  }
}
