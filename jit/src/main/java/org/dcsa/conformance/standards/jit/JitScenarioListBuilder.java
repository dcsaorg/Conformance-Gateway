package org.dcsa.conformance.standards.jit;

import static org.dcsa.conformance.standards.jit.model.JitServiceTypeSelector.*;
import static org.dcsa.conformance.standards.jit.model.JitTimestampType.*;
import static org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.scenario.ConformanceAction;
import org.dcsa.conformance.core.scenario.ScenarioListBuilder;
import org.dcsa.conformance.standards.jit.action.JitCancelAction;
import org.dcsa.conformance.standards.jit.action.JitDeclineAction;
import org.dcsa.conformance.standards.jit.action.JitGetAction;
import org.dcsa.conformance.standards.jit.action.JitOOBTimestampAction;
import org.dcsa.conformance.standards.jit.action.JitOOBTimestampInputAction;
import org.dcsa.conformance.standards.jit.action.JitOmitPortCallAction;
import org.dcsa.conformance.standards.jit.action.JitOmitTerminalCallAction;
import org.dcsa.conformance.standards.jit.action.JitPortCallAction;
import org.dcsa.conformance.standards.jit.action.JitPortCallServiceAction;
import org.dcsa.conformance.standards.jit.action.JitTerminalCallAction;
import org.dcsa.conformance.standards.jit.action.JitTimestampAction;
import org.dcsa.conformance.standards.jit.action.JitVesselStatusAction;
import org.dcsa.conformance.standards.jit.action.JitWrongTimestampAction;
import org.dcsa.conformance.standards.jit.action.SupplyScenarioParametersAction;
import org.dcsa.conformance.standards.jit.checks.JitChecks;
import org.dcsa.conformance.standards.jit.model.JitGetPortCallFilters;
import org.dcsa.conformance.standards.jit.model.JitGetPortCallServiceFilters;
import org.dcsa.conformance.standards.jit.model.JitGetTerminalCallFilters;
import org.dcsa.conformance.standards.jit.model.JitGetTimestampCallFilters;
import org.dcsa.conformance.standards.jit.model.JitGetType;
import org.dcsa.conformance.standards.jit.model.JitServiceTypeSelector;
import org.dcsa.conformance.standards.jit.model.JitTimestampType;
import org.dcsa.conformance.standards.jit.model.PortCallServiceTypeCode;

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
    addScenarioGroup13(scenarioList, context);
    addScenarioGroup14(scenarioList, context);
    addScenarioGroup15and16(scenarioList, context);

    // Scenario suite: "GET endpoint conformance"
    addScenarioGroupGET1(scenarioList, context);

    // Scenario suite: "Secondary sender and receiver conformance"
    addScenarioGroupSecondary1(scenarioList, context);
    addScenarioGroupSecondary2(scenarioList, context);
    addScenarioGroupSecondary3(scenarioList, context);

    return scenarioList;
  }

  private static void addScenarioGroup3(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "3. Moves type with variations",
        supplyScenarioParameters(context, GIVEN)
            .thenEither(
                portCall(context)
                    .then(terminalCall(context).then(serviceCall(context, MOVES, GIVEN))),
                portCall(context)
                    .then(
                        portCall(context)
                            .then(terminalCall(context).then(serviceCall(context, MOVES, GIVEN)))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(portCall(context).then(serviceCall(context, MOVES, GIVEN)))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(serviceCall(context, MOVES, GIVEN).then(portCall(context)))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(terminalCall(context).then(serviceCall(context, MOVES, GIVEN)))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(serviceCall(context, MOVES, GIVEN).then(terminalCall(context)))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, MOVES, GIVEN)
                                    .then(serviceCall(context, MOVES, GIVEN))))));
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
                    sendTimestamp(context, ACTUAL).then(sendTimestamp(context, ACTUAL)))));
  }

  private static void addScenarioGroup11(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "11. S-A cancel",
        supplyScenarioParameters(context, S_A_PATTERN)
            .thenEither(
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
                                serviceCall(context, null, S_A_PATTERN)
                                    .then(cancelCall(context))))));
  }

  private static void addScenarioGroup12(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "12. S-A decline",
        supplyScenarioParameters(context, S_A_PATTERN)
            .thenEither(
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

  private static void addScenarioGroup13(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "13. S-ERP-A cancel",
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

  private static void addScenarioGroup14(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "14. S-ERP-A decline",
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

  private static void addScenarioGroup15and16(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "15. Service Provider handling unexpected Timestamp call",
        noAction()
            .thenEither(
                new JitScenarioListBuilder(
                    previousAction -> new JitWrongTimestampAction(context, previousAction, true))));

    scenarioList.put(
        "16. Service Consumer handling unexpected Timestamp call",
        noAction()
            .thenEither(
                new JitScenarioListBuilder(
                    previousAction ->
                        new JitWrongTimestampAction(context, previousAction, false))));
  }

  private static JitScenarioListBuilder noAction() {
    return new JitScenarioListBuilder(null);
  }

  private static void addScenarioGroupGET1(
      LinkedHashMap<String, JitScenarioListBuilder> scenarioList, JitScenarioContext context) {
    scenarioList.put(
        "1. PC-TC-S-V Service Provider answering GET calls",
        supplyScenarioParameters(context, ANY)
            .thenEither(
                portCall(context).then(getPortCallActions(context, false)),
                portCall(context)
                    .then(terminalCall(context).then(getTerminalCallActions(context, false))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, ANY)
                                    .then(getServiceCallActions(context, false)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    ANY,
                    getAction(
                        context,
                        JitGetType.VESSEL_STATUSES,
                        JitChecks.PORT_CALL_SERVICE_ID,
                        false))));
    scenarioList.put(
        "2. PC-TC-S-V Service Consumer answering GET calls",
        supplyScenarioParameters(context, ANY)
            .thenEither(
                portCall(context).then(getPortCallActions(context, true)),
                portCall(context)
                    .then(terminalCall(context).then(getTerminalCallActions(context, true))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, null, ANY)
                                    .then(getServiceCallActions(context, true)))),
                sendPC_TC_SC_VS(
                    context,
                    null,
                    ANY,
                    getAction(
                        context,
                        JitGetType.VESSEL_STATUSES,
                        JitChecks.PORT_CALL_SERVICE_ID,
                        true))));

    scenarioList.put(
        "3. PC-TC-S-V-A-ERP-A Service Provider answering GET calls",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                sendPC_TC_SC_VS(
                    context,
                    null,
                    FULL_ERP,
                    sendERPTimestamps(
                        context,
                        sendTimestamp(context, ACTUAL)
                            .then(getTimestampActions(context, false))))));

    scenarioList.put(
        "4. PC-TC-S-V-A-ERP-A Service Consumer answering GET calls",
        supplyScenarioParameters(context, FULL_ERP)
            .thenEither(
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendERPTimestamps(
                        context,
                        sendTimestamp(context, ACTUAL).then(getTimestampActions(context, true))))));

    scenarioList.put(
        "5. PC-TC-S-V-A Service Provider answering GET calls",
        supplyScenarioParameters(context, S_A_PATTERN)
            .thenEither(
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, ACTUAL)
                        .then(
                            getTimestamps(
                                    context, JitGetTimestampCallFilters.props().get(3), false, 1)
                                .then(
                                    getTimestamps(
                                            context,
                                            JitGetTimestampCallFilters.props().get(4),
                                            false,
                                            1)
                                        .then(
                                            getTimestamps(
                                                context,
                                                List.of(
                                                    JitGetTimestampCallFilters.props().get(4),
                                                    JitGetTimestampCallFilters.props().get(5)),
                                                false,
                                                1)))))));
    scenarioList.put(
        "6. PC-TC-S-V-A Service Consumer answering GET calls",
        supplyScenarioParameters(context, S_A_PATTERN)
            .thenEither(
                sendPC_TC_SC_VS(
                    context,
                    null,
                    S_A_PATTERN,
                    sendTimestamp(context, ACTUAL)
                        .then(
                            getTimestamps(
                                    context, JitGetTimestampCallFilters.props().get(3), true, 1)
                                .then(
                                    getTimestamps(
                                            context,
                                            JitGetTimestampCallFilters.props().get(4),
                                            true,
                                            1)
                                        .then(
                                            getTimestamps(
                                                context,
                                                List.of(
                                                    JitGetTimestampCallFilters.props().get(4),
                                                    JitGetTimestampCallFilters.props().get(5)),
                                                true,
                                                1)))))));
    scenarioList.put(
        "7. Moves service type Service Provider answering GET calls",
        supplyScenarioParameters(context, GIVEN)
            .thenEither(
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, MOVES, GIVEN)
                                    .then(getServiceCallActions(context, false))))));
    scenarioList.put(
        "8. Moves service type Service Consumer answering GET calls",
        supplyScenarioParameters(context, GIVEN)
            .thenEither(
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, MOVES, GIVEN)
                                    .then(getServiceCallActions(context, true))))));
  }

  private static JitScenarioListBuilder getTimestampActions(
      JitScenarioContext context, boolean requestedByProvider) {
    return getTimestamps(context, JitGetTimestampCallFilters.props().getFirst(), false, 1)
        .then(
            getTimestamps(
                    context, JitGetTimestampCallFilters.props().get(1), requestedByProvider, 1)
                .then(
                    getTimestamps(
                            context,
                            JitGetTimestampCallFilters.props().get(2),
                            requestedByProvider,
                            1)
                        .then(
                            getTimestamps(
                                    context,
                                    JitGetTimestampCallFilters.props().get(3),
                                    requestedByProvider,
                                    1)
                                .then(
                                    getTimestamps(
                                            context,
                                            JitGetTimestampCallFilters.props().get(4),
                                            requestedByProvider,
                                            4)
                                        .then(
                                            getTimestamps(
                                                context,
                                                List.of(
                                                    JitGetTimestampCallFilters.props().get(4),
                                                    JitGetTimestampCallFilters.props().get(5)),
                                                requestedByProvider,
                                                1))))));
  }

  private static JitScenarioListBuilder getPortCallActions(
      JitScenarioContext context, boolean requestedByProvider) {
    return getAction(
            context,
            JitGetType.PORT_CALLS,
            JitGetPortCallFilters.props().getFirst(),
            requestedByProvider)
        .then(
            getAction(
                    context,
                    JitGetType.PORT_CALLS,
                    JitGetPortCallFilters.props().get(1),
                    requestedByProvider)
                .then(
                    getAction(
                            context,
                            JitGetType.PORT_CALLS,
                            JitGetPortCallFilters.props().get(2),
                            requestedByProvider)
                        .then(
                            getAction(
                                    context,
                                    JitGetType.PORT_CALLS,
                                    JitGetPortCallFilters.props().get(3),
                                    requestedByProvider)
                                .then(
                                    getAction(
                                            context,
                                            JitGetType.PORT_CALLS,
                                            JitGetPortCallFilters.props().get(4),
                                            requestedByProvider)
                                        .then(
                                            getAction(
                                                context,
                                                JitGetType.PORT_CALLS,
                                                JitGetPortCallFilters.props().get(5),
                                                requestedByProvider))))));
  }

  private static JitScenarioListBuilder getTerminalCallActions(
      JitScenarioContext context, boolean requestedByProvider) {
    return getAction(
            context,
            JitGetType.TERMINAL_CALLS,
            JitGetTerminalCallFilters.props().getFirst(),
            requestedByProvider)
        .then(
            getAction(
                    context,
                    JitGetType.TERMINAL_CALLS,
                    JitGetTerminalCallFilters.props().get(1),
                    requestedByProvider)
                .then(
                    getAction(
                            context,
                            JitGetType.TERMINAL_CALLS,
                            JitGetTerminalCallFilters.props().get(2),
                            requestedByProvider)
                        .then(
                            getAction(
                                    context,
                                    JitGetType.TERMINAL_CALLS,
                                    JitGetTerminalCallFilters.props().get(3),
                                    requestedByProvider)
                                .then(
                                    getAction(
                                            context,
                                            JitGetType.TERMINAL_CALLS,
                                            JitGetTerminalCallFilters.props().get(4),
                                            requestedByProvider)
                                        .then(
                                            getAction(
                                                    context,
                                                    JitGetType.TERMINAL_CALLS,
                                                    JitGetTerminalCallFilters.props().get(5),
                                                    requestedByProvider)
                                                .then(
                                                    getAction(
                                                            context,
                                                            JitGetType.TERMINAL_CALLS,
                                                            List.of(
                                                                JitGetTerminalCallFilters.props()
                                                                    .get(2),
                                                                JitGetTerminalCallFilters.props()
                                                                    .get(6)),
                                                            requestedByProvider)
                                                        .then(
                                                            getAction(
                                                                    context,
                                                                    JitGetType.TERMINAL_CALLS,
                                                                    List.of(
                                                                        JitGetTerminalCallFilters
                                                                            .props()
                                                                            .get(2),
                                                                        JitGetTerminalCallFilters
                                                                            .props()
                                                                            .get(7)),
                                                                    requestedByProvider)
                                                                .then(
                                                                    getAction(
                                                                            context,
                                                                            JitGetType
                                                                                .TERMINAL_CALLS,
                                                                            List.of(
                                                                                JitGetTerminalCallFilters
                                                                                    .props()
                                                                                    .get(3),
                                                                                JitGetTerminalCallFilters
                                                                                    .props()
                                                                                    .get(8)),
                                                                            requestedByProvider)
                                                                        .then(
                                                                            getAction(
                                                                                context,
                                                                                JitGetType
                                                                                    .TERMINAL_CALLS,
                                                                                List.of(
                                                                                    JitGetTerminalCallFilters
                                                                                        .props()
                                                                                        .get(4),
                                                                                    JitGetTerminalCallFilters
                                                                                        .props()
                                                                                        .get(9)),
                                                                                requestedByProvider))))))))));
  }

  private static JitScenarioListBuilder getServiceCallActions(
      JitScenarioContext context, boolean requestedByProvider) {
    return getAction(
            context,
            JitGetType.PORT_CALL_SERVICES,
            JitGetPortCallServiceFilters.props().getFirst(),
            requestedByProvider)
        .then(
            getAction(
                    context,
                    JitGetType.PORT_CALL_SERVICES,
                    JitGetPortCallServiceFilters.props().get(1),
                    requestedByProvider)
                .then(
                    getAction(
                        context,
                        JitGetType.PORT_CALL_SERVICES,
                        List.of(
                            JitGetPortCallServiceFilters.props().getFirst(),
                            JitGetPortCallServiceFilters.props().get(2)),
                        requestedByProvider)));
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
        "3. Moves as FYI message",
        supplyScenarioParameters(context, GIVEN, true)
            .thenEither(
                portCall(context)
                    .then(terminalCall(context).then(serviceCall(context, MOVES, GIVEN))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(serviceCall(context, MOVES, GIVEN).then(omitPortCall(context)))),
                portCall(context)
                    .then(
                        terminalCall(context)
                            .then(
                                serviceCall(context, MOVES, GIVEN)
                                    .then(omitTerminalCall(context)))),
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
      PortCallServiceTypeCode serviceTypeCode,
      JitServiceTypeSelector selector) {
    if (serviceTypeCode == null && selector == null) {
      throw new IllegalArgumentException("Either serviceTypeCode or selector must be provided");
    }
    return new JitScenarioListBuilder(
        previousAction ->
            new JitPortCallServiceAction(context, previousAction, serviceTypeCode, selector));
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
      PortCallServiceTypeCode serviceType,
      JitServiceTypeSelector selector,
      JitScenarioListBuilder... thenEither) {
    return portCall(context)
        .then(
            terminalCall(context)
                .then(
                    serviceCall(context, serviceType, selector)
                        .then(vesselStatus(context).thenEither(thenEither))));
  }

  private static JitScenarioListBuilder getAction(
      JitScenarioContext context, JitGetType type, String filter, boolean requestedByProvider) {
    return getAction(context, type, Collections.singletonList(filter), requestedByProvider);
  }

  private static JitScenarioListBuilder getAction(
      JitScenarioContext context,
      JitGetType type,
      List<String> filters,
      boolean requestedByProvider) {
    return new JitScenarioListBuilder(
        previousAction ->
            new JitGetAction(context, previousAction, type, filters, requestedByProvider, 1));
  }

  private static JitScenarioListBuilder getTimestamps(
      JitScenarioContext context, String filter, boolean requestedByProvider, int expectedResults) {
    return getTimestamps(
        context, Collections.singletonList(filter), requestedByProvider, expectedResults);
  }

  private static JitScenarioListBuilder getTimestamps(
      JitScenarioContext context,
      List<String> filters,
      boolean requestedByProvider,
      int expectedResults) {
    return new JitScenarioListBuilder(
        previousAction ->
            new JitGetAction(
                context,
                previousAction,
                JitGetType.TIMESTAMPS,
                filters,
                requestedByProvider,
                expectedResults));
  }
}
