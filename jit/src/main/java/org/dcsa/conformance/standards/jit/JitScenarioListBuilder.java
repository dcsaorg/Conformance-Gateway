package org.dcsa.conformance.standards.jit;

import static org.dcsa.conformance.standards.jit.model.JitServiceTypeSelector.*;
import static org.dcsa.conformance.standards.jit.model.JitTimestampType.*;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceType.*;

import java.util.LinkedHashMap;
import java.util.function.Function;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.jit.action.JitCancelAction;
import org.dcsa.conformance.standards.jit.action.JitDeclineAction;
import org.dcsa.conformance.standards.jit.action.JitOOBTimestampAction;
import org.dcsa.conformance.standards.jit.action.JitOOBTimestampInputAction;
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
                            sendPC_TC_SC_VS(
                                context,
                                serviceType,
                                GIVEN,
                                sendERPTimestamps(context, sendTimestamp(context, ACTUAL))))
                    .toList()));

    // 2. Scenario group: "S-A service types"
    scenarioList.put(
        "2. PC - TC - S - V - A",
        supplyScenarioParameters(context, GIVEN)
            .thenEither(
                getServicesHavingOnlyA().stream()
                    .map(
                        serviceType ->
                            sendPC_TC_SC_VS(
                                context, serviceType, GIVEN, sendTimestamp(context, ACTUAL)))
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
                sendPC_TC_SC_VS(context, MOVES, GIVEN, declineCall(context)),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(serviceCall(context, MOVES, GIVEN).then(declineCall(context))))));
  }

  private static void addScenarioGroup4(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {

    scenarioList.put(
        "4. PC-TC-S-V-ERP-A in-band ERP variations",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        sendTimestamp(context, ACTUAL).then(sendTimestamp(context, ACTUAL)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        sendTimestamp(context, REQUESTED)
                            .then(
                                sendTimestamp(context, PLANNED)
                                    .then(sendTimestamp(context, ACTUAL))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context, sendERPTimestamps(context, sendTimestamp(context, ACTUAL)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendTimestamp(context, REQUESTED)
                                .then(sendERPTimestamps(context, sendTimestamp(context, ACTUAL))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendTimestamp(context, REQUESTED)
                                .then(
                                    sendTimestamp(context, REQUESTED)
                                        .then(
                                            sendTimestamp(context, PLANNED)
                                                .then(sendTimestamp(context, ACTUAL)))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(sendERPTimestamps(context, sendTimestamp(context, ACTUAL))))));
  }

  private static void addScenarioGroup5(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "5. PC-TC-S-V-ERP-A in-band PC-resend variations",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                portCall(context)
                    .then(
                        sendPC_TC_SC_VS(
                            context,
                            null,
                            FULL_ERP,
                            sendERPTimestamps(context, sendTimestamp(context, ACTUAL)))),
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
                                                            sendTimestamp(context, ACTUAL))))))),
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
                                                            sendTimestamp(context, ACTUAL))))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    portCall(context)
                        .then(sendERPTimestamps(context, sendTimestamp(context, ACTUAL)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            portCall(context)
                                .then(
                                    sendTimestamp(context, REQUESTED)
                                        .then(
                                            sendTimestamp(context, PLANNED)
                                                .then(sendTimestamp(context, ACTUAL)))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            portCall(context)
                                .then(
                                    sendTimestamp(context, REQUESTED)
                                        .then(
                                            portCall(context)
                                                .then(
                                                    sendTimestamp(context, PLANNED)
                                                        .then(sendTimestamp(context, ACTUAL))))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context, portCall(context).then(sendTimestamp(context, ACTUAL)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context, sendTimestamp(context, ACTUAL).then(portCall(context))))));
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
                                                            sendTimestamp(context, ACTUAL))))))),
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
                                                            sendTimestamp(context, ACTUAL))))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    terminalCall(context)
                        .then(sendERPTimestamps(context, sendTimestamp(context, ACTUAL)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            terminalCall(context)
                                .then(
                                    sendTimestamp(context, REQUESTED)
                                        .then(
                                            sendTimestamp(context, PLANNED)
                                                .then(sendTimestamp(context, ACTUAL)))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendTimestamp(context, REQUESTED)
                                .then(
                                    terminalCall(context)
                                        .then(
                                            sendTimestamp(context, PLANNED)
                                                .then(sendTimestamp(context, ACTUAL)))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context, terminalCall(context).then(sendTimestamp(context, ACTUAL)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context, sendTimestamp(context, ACTUAL).then(terminalCall(context))))));
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
                                                            sendTimestamp(context, ACTUAL))))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    serviceCall(context, null, FULL_ERP)
                        .then(sendERPTimestamps(context, sendTimestamp(context, ACTUAL)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            serviceCall(context, null, FULL_ERP)
                                .then(
                                    sendTimestamp(context, REQUESTED)
                                        .then(
                                            sendTimestamp(context, PLANNED)
                                                .then(sendTimestamp(context, ACTUAL)))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendTimestamp(context, REQUESTED)
                                .then(
                                    serviceCall(context, null, FULL_ERP)
                                        .then(
                                            sendTimestamp(context, PLANNED)
                                                .then(sendTimestamp(context, ACTUAL)))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        serviceCall(context, null, FULL_ERP).then(sendTimestamp(context, ACTUAL)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        sendTimestamp(context, ACTUAL)
                            .then(serviceCall(context, null, FULL_ERP))))));
  }

  private static void addScenarioGroup8(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "8. PC-TC-S-V-ERP-A in-band V-resend variations",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    vesselStatus(context)
                        .then(sendERPTimestamps(context, sendTimestamp(context, ACTUAL)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            vesselStatus(context)
                                .then(
                                    sendTimestamp(context, REQUESTED)
                                        .then(
                                            sendTimestamp(context, PLANNED)
                                                .then(sendTimestamp(context, ACTUAL)))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendTimestamp(context, REQUESTED)
                                .then(
                                    vesselStatus(context)
                                        .then(
                                            sendTimestamp(context, PLANNED)
                                                .then(sendTimestamp(context, ACTUAL)))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context, vesselStatus(context).then(sendTimestamp(context, ACTUAL)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context, sendTimestamp(context, ACTUAL).then(vesselStatus(context))))));
  }

  private static void addScenarioGroup9(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "9. PC-TC-S-V-ERP-A out-of-band variations",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendOOBTimestamp(context, ESTIMATED)
                        .then(
                            sendTimestamp(context, REQUESTED)
                                .then(
                                    sendTimestamp(context, PLANNED)
                                        .then(sendTimestamp(context, ACTUAL))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendOOBTimestamp(context, REQUESTED)
                                .then(
                                    sendTimestamp(context, PLANNED)
                                        .then(sendTimestamp(context, ACTUAL))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendTimestamp(context, REQUESTED)
                                .then(
                                    sendOOBTimestamp(context, PLANNED)
                                        .then(sendTimestamp(context, ACTUAL))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendOOBTimestamp(context, ESTIMATED)
                        .then(
                            sendOOBTimestamp(context, REQUESTED)
                                .then(
                                    sendTimestamp(context, PLANNED)
                                        .then(sendTimestamp(context, ACTUAL))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendOOBTimestamp(context, REQUESTED)
                                .then(
                                    sendOOBTimestamp(context, PLANNED)
                                        .then(sendTimestamp(context, ACTUAL))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendOOBTimestamp(context, ESTIMATED)
                        .then(
                            sendOOBTimestamp(context, REQUESTED)
                                .then(
                                    sendOOBTimestamp(context, PLANNED)
                                        .then(sendTimestamp(context, ACTUAL)))))));
  }

  private static void addScenarioGroup10(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "10. PC-TC-S-V-A variations",
        supplyScenarioParameters(context, S_A_PATTERN)
            .thenEither(
                portCall(context)
                    .then(
                        sendPC_TC_SC_VS(
                            context,
                            null,
                            S_A_PATTERN,
                            vesselStatus(context).then(sendTimestamp(context, ACTUAL)))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                portCall(context)
                                    .then(
                                        serviceCall(context, null, S_A_PATTERN)
                                            .then(
                                                vesselStatus(context)
                                                    .then(sendTimestamp(context, ACTUAL)))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, S_A_PATTERN)
                                    .then(
                                        portCall(context)
                                            .then(
                                                vesselStatus(context)
                                                    .then(sendTimestamp(context, ACTUAL)))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    portCall(context).then(sendTimestamp(context, ACTUAL))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, ACTUAL).then(portCall(context))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                terminalCall(context)
                                    .then(
                                        serviceCall(context, null, S_A_PATTERN)
                                            .then(
                                                vesselStatus(context)
                                                    .then(sendTimestamp(context, ACTUAL)))))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, S_A_PATTERN)
                                    .then(
                                        terminalCall(context)
                                            .then(
                                                vesselStatus(context)
                                                    .then(sendTimestamp(context, ACTUAL)))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    terminalCall(context).then(sendTimestamp(context, ACTUAL))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, ACTUAL).then(terminalCall(context))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, S_A_PATTERN)
                                    .then(
                                        serviceCall(context, null, S_A_PATTERN)
                                            .then(
                                                vesselStatus(context)
                                                    .then(sendTimestamp(context, ACTUAL)))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    serviceCall(context, null, S_A_PATTERN).then(sendTimestamp(context, ACTUAL))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, ACTUAL).then(serviceCall(context, null, S_A_PATTERN))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    vesselStatus(context).then(sendTimestamp(context, ACTUAL))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, ACTUAL).then(vesselStatus(context))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, ACTUAL).then(sendTimestamp(context, ACTUAL))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, ACTUAL).then(cancelCall(context))),
                sendPC_TC_SC_VS(context, null, S_A_PATTERN, cancelCall(context)),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, S_A_PATTERN).then(cancelCall(context)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, ACTUAL).then(declineCall(context))),
                sendPC_TC_SC_VS(context, null, S_A_PATTERN, declineCall(context)),
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
                sendPC_TC_SC_VS(
                    context, null, FULL_ERP, sendERPTimestamps(context, cancelCall(context))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(sendTimestamp(context, REQUESTED).then(cancelCall(context)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED).then(cancelCall(context))),
                sendPC_TC_SC_VS(context, null, FULL_ERP, cancelCall(context)),
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
                sendPC_TC_SC_VS(
                    context, null, FULL_ERP, sendERPTimestamps(context, declineCall(context))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(sendTimestamp(context, REQUESTED).then(declineCall(context)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED).then(declineCall(context))),
                sendPC_TC_SC_VS(context, null, FULL_ERP, declineCall(context)),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, FULL_ERP).then(declineCall(context))))));
  }

  private static void addScenarioGroupSecondary1(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "1. PC-TC-S-V-ERP-A as FYI messages",
        supplyScenarioParameters(context, FULL_ERP, true)
            .thenEither(
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendTimestampFYI(context, REQUESTED)
                                .then(
                                    sendTimestamp(context, PLANNED)
                                        .then(sendTimestamp(context, ACTUAL))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendTimestampFYI(context, REQUESTED)
                                .then(sendTimestamp(context, PLANNED).then(cancelCall(context))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendTimestampFYI(context, REQUESTED)
                                .then(
                                    sendTimestamp(context, PLANNED)
                                        .then(declineCallFYI(context))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendTimestampFYI(context, REQUESTED)
                                .then(
                                    sendTimestamp(context, PLANNED).then(omitPortCall(context))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendTimestampFYI(context, REQUESTED)
                                .then(
                                    sendTimestamp(context, PLANNED)
                                        .then(omitTerminalCall(context))))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(sendTimestampFYI(context, REQUESTED).then(omitPortCall(context)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED)
                        .then(
                            sendTimestampFYI(context, REQUESTED).then(omitTerminalCall(context)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED).then(omitPortCall(context))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendTimestamp(context, ESTIMATED).then(omitTerminalCall(context))),
                sendPC_TC_SC_VS(context, null, FULL_ERP, omitPortCall(context)),
                sendPC_TC_SC_VS(context, null, FULL_ERP, omitTerminalCall(context)),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, FULL_ERP).then(omitPortCall(context)))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, FULL_ERP)
                                    .then(omitTerminalCall(context)))),
                portCall(context).then(terminalCall(context).then(omitPortCall(context))),
                portCall(context).then(terminalCall(context).then(omitTerminalCall(context))),
                portCall(context).then(omitPortCall(context))));
  }

  private static void addScenarioGroupSecondary2(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "2. S-A as FYI messages",
        supplyScenarioParameters(context, S_A_PATTERN, true)
            .thenEither(
                sendPC_TC_SC_VS(context, null, S_A_PATTERN, sendTimestamp(context, ACTUAL)),
                sendPC_TC_SC_VS(context, null, S_A_PATTERN, omitPortCall(context)),
                sendPC_TC_SC_VS(context, null, S_A_PATTERN, omitTerminalCall(context)),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, S_A_PATTERN)
                                    .then(omitPortCall(context)))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, S_A_PATTERN)
                                    .then(omitTerminalCall(context)))),
                portCall(context).then(terminalCall(context).then(omitPortCall(context))),
                portCall(context).then(terminalCall(context).then(omitTerminalCall(context))),
                portCall(context).then(omitPortCall(context))));
  }

  private static void addScenarioGroupSecondary3(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "3. S(Moves) as FYI message",
        supplyScenarioParameters(context, GIVEN, true)
            .thenEither(
                sendPC_TC_SC_VS(context, MOVES, null),
                sendPC_TC_SC_VS(context, MOVES, null, omitPortCall(context)),
                sendPC_TC_SC_VS(context, MOVES, null, omitTerminalCall(context)),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(serviceCall(context, MOVES, null).then(omitPortCall(context)))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, MOVES, null).then(omitTerminalCall(context)))),
                portCall(context).then(terminalCall(context).then(omitPortCall(context))),
                portCall(context).then(terminalCall(context).then(omitTerminalCall(context))),
                portCall(context).then(omitPortCall(context))));
  }

  private static JitScenarioListBuilder sendTimestamp(
      JitScenarioContext context, JitTimestampType timestampType) {
    return new JitScenarioListBuilder(
        previousAction ->
            new JitTimestampAction(
                context, previousAction, timestampType, timestampType != REQUESTED));
  }

  private static JitScenarioListBuilder sendTimestampFYI(
      JitScenarioContext context, JitTimestampType timestampType) {
    return new JitScenarioListBuilder(
        previousAction -> new JitTimestampAction(context, previousAction, timestampType, true));
  }

  private static JitScenarioListBuilder sendOOBTimestamp(
      JitScenarioContext context, JitTimestampType timestampType) {
    if (timestampType == REQUESTED) {
      return new JitScenarioListBuilder(
          previousAction -> new JitOOBTimestampInputAction(context, previousAction, timestampType));
    }
    return new JitScenarioListBuilder(
        previousAction -> new JitOOBTimestampAction(context, previousAction, timestampType, true));
  }

  private static JitScenarioListBuilder sendERPTimestamps(
      JitScenarioContext context, JitScenarioListBuilder... thenEither) {
    return sendTimestamp(context, ESTIMATED)
        .then(
            sendTimestamp(context, REQUESTED)
                .then(sendTimestamp(context, PLANNED).thenEither(thenEither)));
  }

  private JitScenarioListBuilder(Function<ConformanceAction, ConformanceAction> actionBuilder) {
    super(actionBuilder);
  }

  private static JitScenarioListBuilder supplyScenarioParameters(
      JitScenarioContext context, JitServiceTypeSelector selector) {
    return supplyScenarioParameters(context, selector, false);
  }

  private static JitScenarioListBuilder supplyScenarioParameters(
      @NonNull JitScenarioContext context, JitServiceTypeSelector selector, boolean isFYI) {
    return new JitScenarioListBuilder(
        previousAction -> new SupplyScenarioParametersAction(context, selector, isFYI));
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
        previousAction -> new JitDeclineAction(context, previousAction, false));
  }

  private static JitScenarioListBuilder declineCallFYI(JitScenarioContext context) {
    return new JitScenarioListBuilder(
        previousAction -> new JitDeclineAction(context, previousAction, true));
  }

  private static JitScenarioListBuilder omitPortCall(JitScenarioContext context) {
    return new JitScenarioListBuilder(
        previousAction -> new JitOmitPortCallAction(context, previousAction));
  }

  private static JitScenarioListBuilder omitTerminalCall(JitScenarioContext context) {
    return new JitScenarioListBuilder(
        previousAction -> new JitOmitTerminalCallAction(context, previousAction));
  }

  private static JitScenarioListBuilder sendPC_TC_SC_VS(
      @NonNull JitScenarioContext context,
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
